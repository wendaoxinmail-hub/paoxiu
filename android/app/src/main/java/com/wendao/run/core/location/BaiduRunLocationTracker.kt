package com.wendao.run.core.location

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.baidu.location.BDAbstractLocationListener
import com.baidu.location.BDLocation
import com.baidu.location.LocationClient
import com.baidu.location.LocationClientOption
import com.wendao.run.core.run.RunGeoUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class RunLocationUpdate(
    val lat: Double,
    val lng: Double,
    val accuracyM: Float,
    val speedKmh: Float,
    val recordedAt: Long,
    val locType: Int,
    /** android LocationManager provider 或 "baidu" */
    val provider: String = "unknown",
)

@Singleton
class BaiduRunLocationTracker @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private var locationClient: LocationClient? = null
    private var callback: ((RunLocationUpdate) -> Unit)? = null
    private var lastGpsBaiduAtMs = 0L
    private var lastBaiduFix: RunLocationUpdate? = null
    private var lastAuthError: String? = null
    private var lastRawLat: Double? = null
    private var lastRawLng: Double? = null
    private var lastRawAt: Long? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    /** 最近一次有效百度定位（GCJ02），供地图定位按钮即时回退。 */
    fun peekLastFix(): RunLocationUpdate? {
        val fix = lastBaiduFix ?: return null
        if (System.currentTimeMillis() - fix.recordedAt > CACHE_TTL_MS) return null
        return fix
    }

    /** 百度 AK 鉴权失败等错误（供 UI 展示） */
    fun peekAuthError(): String? = lastAuthError

    fun start(
        onLocation: (RunLocationUpdate) -> Unit,
        onAuthError: ((String) -> Unit)? = null,
        replayCachedFix: Boolean = true,
    ) {
        stop()
        if (!context.hasFineLocationPermission()) {
            PaoxiuLocationLog.reject("tracker.start", "no ACCESS_FINE_LOCATION")
            return
        }
        PaoxiuLocationLog.i("tracker.start scan=${SCAN_SPAN_MS}ms replayCache=$replayCachedFix")
        callback = onLocation
        if (replayCachedFix) {
            peekLastFix()?.let { cached ->
                PaoxiuLocationLog.i("tracker.start emit cached fix (${cached.lat},${cached.lng})")
                mainHandler.post { callback?.invoke(cached) }
            }
        }

        val client = LocationClient(context.applicationContext)
        client.registerLocationListener(object : BDAbstractLocationListener() {
            override fun onReceiveLocation(location: BDLocation?) {
                PaoxiuLocationLog.rawBaidu("continuous", location)
                val update = location?.toRunLocationUpdate("continuous")
                if (update != null) {
                    rememberFix(update)
                    mainHandler.post { callback?.invoke(update) }
                } else {
                    peekAuthError()?.let { err ->
                        mainHandler.post { onAuthError?.invoke(err) }
                    }
                }
            }

            override fun onLocDiagnosticMessage(locType: Int, diagnosticType: Int, diagnosticMessage: String?) {
                PaoxiuLocationLog.w(
                    "baidu[continuous] diagnostic locType=$locType type=$diagnosticType msg=$diagnosticMessage",
                )
            }
        })

        val option = LocationClientOption().apply {
            locationMode = LocationClientOption.LocationMode.Hight_Accuracy
            setCoorType("gcj02")
            setScanSpan(SCAN_SPAN_MS)
            isOpenGps = true
            setIsNeedAddress(false)
            setIsNeedAltitude(false)
            setIsNeedLocationDescribe(false)
            setLocationNotify(true)
            setIgnoreKillProcess(true)
        }
        client.locOption = option
        try {
            client.start()
            locationClient = client
            PaoxiuLocationLog.i("tracker.start LocationClient started")
        } catch (e: Exception) {
            PaoxiuLocationLog.w("tracker.start LocationClient.start failed", e)
        }
    }

    fun stop() {
        if (locationClient != null) {
            PaoxiuLocationLog.i("tracker.stop")
        }
        locationClient?.stop()
        locationClient = null
        callback = null
        lastGpsBaiduAtMs = 0L
        lastRawLat = null
        lastRawLng = null
        lastRawAt = null
    }

    /**
     * 单次定位：优先缓存 → 百度 once；不使用系统 lastKnownLocation。
     */
    fun requestSingleFix(onResult: (RunLocationUpdate?) -> Unit) {
        if (!context.hasFineLocationPermission()) {
            PaoxiuLocationLog.reject("singleFix", "no ACCESS_FINE_LOCATION")
            mainHandler.post { onResult(null) }
            return
        }
        peekLastFix()?.let { cached ->
            PaoxiuLocationLog.i("singleFix hit cache (${cached.lat},${cached.lng})")
            mainHandler.post { onResult(cached) }
            return
        }

        PaoxiuLocationLog.i("singleFix start once location timeout=${SINGLE_FIX_TIMEOUT_MS}ms")
        var delivered = false
        fun deliver(update: RunLocationUpdate?) {
            if (delivered) return
            delivered = true
            if (update == null) {
                PaoxiuLocationLog.w("singleFix failed (null result)")
            } else {
                PaoxiuLocationLog.accept("singleFix", update.lat, update.lng, update.accuracyM)
            }
            mainHandler.post {
                update?.let { rememberFix(it) }
                onResult(update)
            }
        }

        val client = LocationClient(context.applicationContext)
        val listener = object : BDAbstractLocationListener() {
            override fun onReceiveLocation(location: BDLocation?) {
                PaoxiuLocationLog.rawBaidu("singleFix", location)
                client.unRegisterLocationListener(this)
                client.stop()
                deliver(location?.toRunLocationUpdate("singleFix"))
            }

            override fun onLocDiagnosticMessage(locType: Int, diagnosticType: Int, diagnosticMessage: String?) {
                PaoxiuLocationLog.w(
                    "baidu[singleFix] diagnostic locType=$locType type=$diagnosticType msg=$diagnosticMessage",
                )
            }
        }
        client.registerLocationListener(listener)
        val option = LocationClientOption().apply {
            locationMode = LocationClientOption.LocationMode.Hight_Accuracy
            setCoorType("gcj02")
            setScanSpan(0)
            isOpenGps = true
            setIsNeedAddress(false)
            isOnceLocation = true
        }
        client.locOption = option
        try {
            client.start()
        } catch (e: Exception) {
            PaoxiuLocationLog.w("singleFix LocationClient.start failed", e)
            deliver(null)
            return
        }

        mainHandler.postDelayed({
            if (delivered) return@postDelayed
            PaoxiuLocationLog.w("singleFix timeout after ${SINGLE_FIX_TIMEOUT_MS}ms")
            try {
                client.unRegisterLocationListener(listener)
                client.stop()
            } catch (_: Exception) {
                // ignore
            }
            deliver(peekLastFix())
        }, SINGLE_FIX_TIMEOUT_MS)
    }

    private fun rememberFix(update: RunLocationUpdate) {
        lastBaiduFix = update
    }

    private fun BDLocation.toRunLocationUpdate(source: String): RunLocationUpdate? {
        val typeDesc = locTypeDescription.orEmpty()
        if (typeDesc.contains("unlegal", ignoreCase = true) ||
            typeDesc.contains("key", ignoreCase = true) && typeDesc.contains("check", ignoreCase = true)
        ) {
            PaoxiuLocationLog.reject(source, "AK auth failed: $typeDesc")
            lastAuthError = "百度地图 Key 鉴权失败，请在控制台核对包名与 SHA1"
            return null
        }
        if (locType == 505 || locType == BDLocation.TypeServerError || locType == BDLocation.TypeOffLineLocationFail) {
            PaoxiuLocationLog.reject(source, "locType=$locType error=$typeDesc")
            return null
        }
        if (!isValidGcjCoordinate(latitude, longitude)) {
            PaoxiuLocationLog.reject(source, "invalid lat/lng ($latitude,$longitude)")
            return null
        }
        if (locType == BDLocation.TypeGpsLocation) {
            lastGpsBaiduAtMs = System.currentTimeMillis()
        }
        val accuracy = LocationAccuracyUtils.normalizeAccuracyM(radius.coerceAtLeast(0f))
        if (accuracy > MAX_ACCURACY_M) {
            PaoxiuLocationLog.reject(source, "accuracy ${accuracy}m > max ${MAX_ACCURACY_M}m")
            return null
        }

        lastAuthError = null
        val now = System.currentTimeMillis()
        val speedKmh = computeInstantSpeedKmh(latitude, longitude, now)
        PaoxiuLocationLog.accept(source, latitude, longitude, accuracy)
        return RunLocationUpdate(
            lat = latitude,
            lng = longitude,
            accuracyM = accuracy,
            speedKmh = speedKmh,
            recordedAt = now,
            locType = locType,
            provider = "baidu",
        )
    }

    private fun isValidGcjCoordinate(lat: Double, lng: Double): Boolean {
        if (!lat.isFinite() || !lng.isFinite()) return false
        if (kotlin.math.abs(lat) < 1e-6 && kotlin.math.abs(lng) < 1e-6) return false
        if (lat !in -90.0..90.0 || lng !in -180.0..180.0) return false
        return true
    }

    private fun computeInstantSpeedKmh(lat: Double, lng: Double, recordedAt: Long): Float {
        val prevLat = lastRawLat
        val prevLng = lastRawLng
        val prevAt = lastRawAt
        lastRawLat = lat
        lastRawLng = lng
        lastRawAt = recordedAt
        if (prevLat == null || prevLng == null || prevAt == null) return 0f
        val elapsedMs = recordedAt - prevAt
        if (elapsedMs <= 0) return 0f
        val segmentM = RunGeoUtils.haversineMeters(prevLat, prevLng, lat, lng)
        return (segmentM / elapsedMs * 3_600.0).toFloat().coerceAtLeast(0f)
    }

    companion object {
        private const val SCAN_SPAN_MS = 1000
        private const val SINGLE_FIX_TIMEOUT_MS = 8_000L
        private const val CACHE_TTL_MS = 120_000L
        private const val MAX_ACCURACY_M = 100f
    }
}
