package com.wendao.run.core.location

import android.util.Log
import com.baidu.location.BDLocation
import com.wendao.run.BuildConfig

/**
 * 真机定位诊断日志。过滤命令：
 * adb logcat -s PaoxiuLocation
 */
object PaoxiuLocationLog {

    const val TAG = "PaoxiuLocation"

    fun i(message: String) {
        runCatching { Log.i(TAG, message) }
    }

    fun w(message: String, error: Throwable? = null) {
        runCatching {
            if (error != null) Log.w(TAG, message, error) else Log.w(TAG, message)
        }
    }

    fun appInit(context: android.content.Context) {
        val key = BuildConfig.BAIDU_MAP_API_KEY
        val masked = when {
            key.length <= 8 -> "(empty or too short)"
            else -> "${key.take(4)}…${key.takeLast(4)} (len=${key.length})"
        }
        i(
            "app init pkg=${context.packageName} " +
                "baiduKey=$masked finePerm=${context.hasFineLocationPermission()} " +
                "gpsOn=${context.isLocationEnabled()}",
        )
    }

    fun permission(state: String, fine: Boolean, gpsOn: Boolean) {
        i("permission state=$state fine=$fine gpsOn=$gpsOn")
    }

    fun service(event: String, detail: String = "") {
        i("service $event${if (detail.isNotEmpty()) " | $detail" else ""}")
    }

    fun map(event: String, detail: String = "") {
        i("map $event${if (detail.isNotEmpty()) " | $detail" else ""}")
    }

    fun ingest(
        accepted: Boolean,
        rawLat: Double,
        rawLng: Double,
        accuracyM: Float,
        pointCount: Int,
        rejectReason: String = "",
        segmentM: Double = 0.0,
        distanceM: Double = 0.0,
    ) {
        i(
            "ingest accepted=$accepted reason=$rejectReason seg=${"%.1f".format(segmentM)}m " +
                "dist=${"%.1f".format(distanceM)}m raw=($rawLat,$rawLng) acc=${accuracyM.toInt()}m " +
                "points=$pointCount",
        )
    }

    fun publish(lastLat: Double?, lastLng: Double?, pointCount: Int, accuracyM: Float?) {
        i(
            "publish last=(${lastLat ?: "null"},${lastLng ?: "null"}) " +
                "points=$pointCount acc=${accuracyM?.toInt()?.let { "${it}m" } ?: "null"}",
        )
    }

    /** 百度原始回调（过滤前） */
    fun rawBaidu(source: String, location: BDLocation?) {
        if (location == null) {
            w("baidu[$source] callback location=null")
            return
        }
        i(
            "baidu[$source] type=${location.locType}(${locTypeName(location.locType)}) " +
                "lat=${location.latitude} lng=${location.longitude} " +
                "radius=${location.radius}m " +
                "desc=${location.locationDescribe.orEmpty()} " +
                "error=${location.locTypeDescription.orEmpty()}",
        )
    }

    fun reject(source: String, reason: String) {
        w("reject[$source] $reason")
    }

    fun accept(source: String, lat: Double, lng: Double, accuracyM: Float) {
        i("accept[$source] ($lat,$lng) acc=${accuracyM.toInt()}m")
    }

    private fun locTypeName(type: Int): String = when (type) {
        BDLocation.TypeGpsLocation -> "GPS"
        BDLocation.TypeNetWorkLocation -> "Network"
        BDLocation.TypeOffLineLocation -> "Offline"
        BDLocation.TypeServerError -> "ServerError"
        BDLocation.TypeNetWorkException -> "NetException"
        BDLocation.TypeCriteriaException -> "CriteriaException"
        BDLocation.TypeOffLineLocationFail -> "OfflineFail"
        else -> "Unknown($type)"
    }
}
