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
 * Keep 式跑步定位：GPS/Fused 为主，GPS 长时间无回调时用 NETWORK 兜底。
 */
@Singleton
class SystemRunLocationSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val locationManager = context.getSystemService(LocationManager::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var callback: ((RunLocationUpdate) -> Unit)? = null
    private var primaryListener: LocationListener? = null
    private var networkListener: LocationListener? = null
    private var activeProvider: String? = null

    private var lastRawLat: Double? = null
    private var lastRawLng: Double? = null
    private var lastRawAt: Long? = null
    private var lastConvertedLat: Double? = null
    private var lastConvertedLng: Double? = null
    private var lastGpsFixAtMs = 0L
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
        lastGpsFixAtMs = 0L

        val listener = object : LocationListener {
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
        primaryListener = listener

        val provider = selectPrimaryProvider()
        if (provider != null) {
            activeProvider = provider
            try {
                locationManager?.requestLocationUpdates(
                    provider,
                    MIN_TIME_MS,
                    MIN_DISTANCE_M,
                    listener,
                    Looper.getMainLooper(),
                )
                PaoxiuLocationLog.i(
                    "system.start provider=$provider interval=${MIN_TIME_MS}ms dist=${MIN_DISTANCE_M}m",
                )
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

        if (locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true) {
            networkListener = listener
            try {
                locationManager?.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    NETWORK_MIN_TIME_MS,
                    NETWORK_MIN_DISTANCE_M,
                    listener,
                    Looper.getMainLooper(),
                )
                PaoxiuLocationLog.i("system.start network fallback listener ON")
            } catch (e: SecurityException) {
                PaoxiuLocationLog.w("system.start network denied", e)
            } catch (e: Exception) {
                PaoxiuLocationLog.w("system.start network failed", e)
            }
        }
    }

    fun stop() {
        primaryListener?.let { locListener ->
            try {
                locationManager?.removeUpdates(locListener)
            } catch (_: Exception) {
                // ignore
            }
        }
        networkListener?.let { locListener ->
            if (locListener !== primaryListener) {
                try {
                    locationManager?.removeUpdates(locListener)
                } catch (_: Exception) {
                    // ignore
                }
            }
        }
        primaryListener = null
        networkListener = null
        callback = null
        activeProvider = null
        lastRawLat = null
        lastRawLng = null
        lastRawAt = null
        lastConvertedLat = null
        lastConvertedLng = null
        lastGpsFixAtMs = 0L
        coordUseRaw = null
    }

    private fun selectPrimaryProvider(): String? {
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
            val gpsEnabled = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
            val gpsRecent = lastGpsFixAtMs > 0L &&
                System.currentTimeMillis() - lastGpsFixAtMs < GPS_STALL_MS
            if (gpsEnabled && gpsRecent) {
                PaoxiuLocationLog.i("system.drop network (gps active ${System.currentTimeMillis() - lastGpsFixAtMs}ms ago)")
                return
            }
            PaoxiuLocationLog.i("system.use network fallback")
        } else if (provider == LocationManager.GPS_PROVIDER ||
            provider == LocationManager.FUSED_PROVIDER
        ) {
            lastGpsFixAtMs = System.currentTimeMillis()
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
        val rawAccuracy = location.accuracy
        if (LocationAccuracyUtils.isOverMaxAccuracy(rawAccuracy)) {
            PaoxiuLocationLog.reject("system[$provider]", "accuracy=${rawAccuracy}m")
            return
        }
        val accuracy = LocationAccuracyUtils.normalizeAccuracyM(rawAccuracy)

        val now = location.time.takeIf { it > 0L } ?: System.currentTimeMillis()
        val speedKmh = when {
            location.hasSpeed() && location.speed >= 0f -> location.speed * 3.6f
            else -> deriveSpeedKmh(lat, lng, now)
        }
        lastRawLat = location.latitude
        lastRawLng = location.longitude
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
        val prevLat = lastConvertedLat
        val prevLng = lastConvertedLng
        val prevAt = lastRawAt
        lastConvertedLat = lat
        lastConvertedLng = lng
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
        private const val NETWORK_MIN_TIME_MS = 5_000L
        private const val NETWORK_MIN_DISTANCE_M = 10f
        /** GPS 在此时间内有回调则忽略 NETWORK，避免双源漂移 */
        private const val GPS_STALL_MS = 15_000L
    }
}
