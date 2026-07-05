package com.wendao.run.core.location

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat

fun Context.findComponentActivity(): ComponentActivity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is ComponentActivity) return current
        current = current.baseContext
    }
    return null
}

/** 跑步/地图需要精确位置，仅 coarse 不够。 */
fun Context.hasFineLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED

fun Context.hasLocationPermission(): Boolean = hasFineLocationPermission()

fun Context.isLocationEnabled(): Boolean {
    val manager = getSystemService(LocationManager::class.java) ?: return false
    return manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
        manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
}

fun Context.openAppSettings() {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
}

fun Context.readLastKnownLocation(): RunLocationUpdate? {
    if (!hasLocationPermission()) return null
    val manager = getSystemService(LocationManager::class.java) ?: return null
    val providers = listOfNotNull(
        LocationManager.GPS_PROVIDER,
        LocationManager.NETWORK_PROVIDER,
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            LocationManager.FUSED_PROVIDER
        } else {
            null
        },
    )
    for (provider in providers) {
        try {
            val loc = manager.getLastKnownLocation(provider) ?: continue
            if (loc.latitude == 0.0 && loc.longitude == 0.0) continue
            val (lat, lng) = androidLocationToGcj02(provider, loc.latitude, loc.longitude)
            return RunLocationUpdate(
                lat = lat,
                lng = lng,
                accuracyM = loc.accuracy.coerceAtLeast(0f),
                speedKmh = loc.speed.takeIf { it >= 0f }?.times(3.6f) ?: 0f,
                recordedAt = loc.time,
                locType = -1,
            )
        } catch (_: SecurityException) {
            return null
        }
    }
    return null
}

fun Int.dpToPx(context: Context): Int =
    (this * context.resources.displayMetrics.density).toInt()
