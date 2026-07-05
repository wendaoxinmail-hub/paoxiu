package com.wendao.run.core.location

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.baidu.location.BDAbstractLocationListener
import com.baidu.location.BDLocation
import com.baidu.location.LocationClient
import com.baidu.location.LocationClientOption

/**
 * 地图页专用定位（独立 LocationClient），不与跑步前台服务的 tracker 抢同一个 client。
 */
class MapViewLocationController(context: Context) {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private var client: LocationClient? = null
    private var callback: ((RunLocationUpdate) -> Unit)? = null

    fun start(onLocation: (RunLocationUpdate) -> Unit) {
        stop()
        callback = onLocation
        val locationClient = LocationClient(appContext)
        locationClient.registerLocationListener(object : BDAbstractLocationListener() {
            override fun onReceiveLocation(location: BDLocation?) {
                val update = location?.toRunLocationUpdate() ?: return
                mainHandler.post { callback?.invoke(update) }
            }
        })
        locationClient.locOption = LocationClientOption().apply {
            locationMode = LocationClientOption.LocationMode.Hight_Accuracy
            setCoorType("gcj02")
            setScanSpan(SCAN_SPAN_MS)
            isOpenGps = true
            setIsNeedAddress(false)
            isOnceLocation = false
        }
        try {
            locationClient.start()
            client = locationClient
        } catch (_: Exception) {
            stop()
        }
    }

    fun stop() {
        try {
            client?.stop()
        } catch (_: Exception) {
            // ignore
        }
        client = null
        callback = null
    }

    private fun BDLocation.toRunLocationUpdate(): RunLocationUpdate? {
        if (locType == BDLocation.TypeServerError || locType == BDLocation.TypeOffLineLocationFail) {
            return null
        }
        if (latitude == 0.0 && longitude == 0.0) return null
        val accuracy = radius.coerceAtLeast(0f)
        if (accuracy > MAX_ACCURACY_M) return null
        return RunLocationUpdate(
            lat = latitude,
            lng = longitude,
            accuracyM = accuracy,
            speedKmh = 0f,
            recordedAt = System.currentTimeMillis(),
            locType = locType,
        )
    }

    companion object {
        private const val SCAN_SPAN_MS = 1000
        private const val MAX_ACCURACY_M = 60f
    }
}
