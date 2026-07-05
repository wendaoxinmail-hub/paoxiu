package com.wendao.run.core.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.wendao.run.core.run.RunGeoUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keep 式跑步定位：单通道 GPS 优先（WGS84→GCJ02），Fused 兜底；与计距/地图同源。
 */
@Singleton
class SystemRunLocationSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val locationManager = context.getSystemService(LocationManager::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var callback: ((RunLocationUpdate) -> Unit)? = null
    private var listener: LocationListener? = null
    private var activeProvider: String? = null

    private var lastRawLat: Double? = null
    private var lastRawLng: Double? = null
    private var lastRawAt: Long? = null
    /** 整段跑步锁定：华为等机型 GPS 已是 GCJ02，避免逐点切换导致漂移 */
    private var coordUseRaw: Boolean? = null

    @SuppressLint("MissingPermission")
    fun start(onLocation: (RunLocationUpdate) -> Unit) {
        stop()
        if (!context.hasFineLocationPermission()) {
            PaoxiuLocationLog.reject("system.start", "no ACCESS_FINE_LOCATION")
            return
        }
        callback = onLocation
        val provider = selectProvider() ?: run {
            PaoxiuLocationLog.w("system.start no GPS/Fused provider available")
            return
        }
        activeProvider = provider

        val locListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                emit(location)
            }

            override fun onProviderDisabled(disabledProvider: String) {
                PaoxiuLocationLog.w("system provider disabled: $disabledProvider")
            }

            override fun onProviderEnabled(enabledProvider: String) {
                PaoxiuLocationLog.i("system provider enabled: $enabledProvider")
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
        }
        listener = locListener

        try {
            locationManager?.requestLocationUpdates(
                provider,
                MIN_TIME_MS,
                MIN_DISTANCE_M,
                locListener,
                Looper.getMainLooper(),
            )
            PaoxiuLocationLog.i("system.start provider=$provider interval=${MIN_TIME_MS}ms dist=${MIN_DISTANCE_M}m")
            locationManager?.getLastKnownLocation(provider)?.let { cached ->
                PaoxiuLocationLog.i("system.start seed lastKnown ($provider)")
                emit(cached)
            }
        } catch (e: SecurityException) {
            PaoxiuLocationLog.w("system.start denied for $provider", e)
        } catch (e: Exception) {
            PaoxiuLocationLog.w("system.start failed for $provider", e)
        }
    }

    fun stop() {
        listener?.let { locListener ->
            try {
                locationManager?.removeUpdates(locListener)
            } catch (_: Exception) {
                // ignore
            }
        }
        listener = null
        callback = null
        activeProvider = null
        lastRawLat = null
        lastRawLng = null
        lastRawAt = null
        coordUseRaw = null
    }

    /** 户外计距优先 GPS（WGS84 可正确转 GCJ02）；Fused 在部分国产机前台服务中回调不稳定 */
    private fun selectProvider(): String? {
        val gps = LocationManager.GPS_PROVIDER
        val fused = LocationManager.FUSED_PROVIDER
        if (locationManager?.isProviderEnabled(gps) == true) return gps
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            locationManager?.isProviderEnabled(fused) == true
        ) {
            return fused
        }
        return null
    }

    private fun emit(location: Location) {
        val provider = location.provider ?: activeProvider ?: "unknown"
        if (provider == LocationManager.NETWORK_PROVIDER) {
            PaoxiuLocationLog.i("system.drop network provider fix")
            return
        }
        val (lat, lng) = toRunCoordinate(
            provider = provider,
            lat = location.latitude,
            lng = location.longitude,
        )
        if (!isValidCoordinate(lat, lng)) {
            PaoxiuLocationLog.reject("system[$provider]", "invalid ($lat,$lng)")
            return
        }
        val accuracy = LocationAccuracyUtils.normalizeAccuracyM(location.accuracy)
        if (accuracy > LocationAccuracyUtils.MAX_RUN_ACCURACY_M) {
            PaoxiuLocationLog.reject("system[$provider]", "accuracy=${accuracy}m")
            return
        }

        val now = location.time.takeIf { it > 0L } ?: System.currentTimeMillis()
        val speedKmh = when {
            location.hasSpeed() && location.speed >= 0f -> location.speed * 3.6f
            else -> deriveSpeedKmh(lat, lng, now)
        }
        PaoxiuLocationLog.accept("system[$provider]", lat, lng, accuracy)
        val update = RunLocationUpdate(
            lat = lat,
            lng = lng,
            accuracyM = accuracy,
            speedKmh = speedKmh,
            recordedAt = now,
            locType = SYSTEM_LOC_TYPE,
            provider = provider,
        )
        mainHandler.post { callback?.invoke(update) }
    }

    /**
     * 首点默认 WGS84→GCJ02；第二点起比较 raw/gcj 哪条更连贯并锁定整段跑步。
     */
    private fun toRunCoordinate(provider: String, lat: Double, lng: Double): Pair<Double, Double> {
        if (provider != LocationManager.GPS_PROVIDER &&
            provider != LocationManager.PASSIVE_PROVIDER
        ) {
            return lat to lng
        }
        val gcj = wgs84ToGcj02(lat, lng)
        val prevLat = lastRawLat
        val prevLng = lastRawLng
        if (prevLat == null || prevLng == null) {
            val shift = RunGeoUtils.haversineMeters(lat, lng, gcj.first, gcj.second)
            if (shift > 150.0) {
                coordUseRaw = true
                PaoxiuLocationLog.i("system.coordMode RAW(GCJ02-native) first-fix")
                return lat to lng
            }
            return gcj
        }
        if (coordUseRaw == null) {
            val rawJump = RunGeoUtils.haversineMeters(prevLat, prevLng, lat, lng)
            val gcjJump = RunGeoUtils.haversineMeters(prevLat, prevLng, gcj.first, gcj.second)
            coordUseRaw = rawJump <= gcjJump
            PaoxiuLocationLog.i(
                "system.coordMode ${if (coordUseRaw == true) "RAW(GCJ02-native)" else "WGS84->GCJ02"}",
            )
        }
        return if (coordUseRaw == true) lat to lng else gcj
    }

    private fun deriveSpeedKmh(lat: Double, lng: Double, recordedAt: Long): Float {
        val prevLat = lastRawLat
        val prevLng = lastRawLng
        val prevAt = lastRawAt
        lastRawLat = lat
        lastRawLng = lng
        lastRawAt = recordedAt
        if (prevLat == null || prevLng == null || prevAt == null) return 0f
        val elapsedMs = recordedAt - prevAt
        if (elapsedMs <= 0) return 0f
        return RunGeoUtils.deriveSpeedKmh(lat, lng, prevLat, prevLng, elapsedMs)
    }

    private fun isValidCoordinate(lat: Double, lng: Double): Boolean {
        if (!lat.isFinite() || !lng.isFinite()) return false
        if (kotlin.math.abs(lat) < 1e-6 && kotlin.math.abs(lng) < 1e-6) return false
        return lat in -90.0..90.0 && lng in -180.0..180.0
    }

    companion object {
        const val SYSTEM_LOC_TYPE = -100
        private const val MIN_TIME_MS = 1_000L
        private const val MIN_DISTANCE_M = 0f
    }
}

