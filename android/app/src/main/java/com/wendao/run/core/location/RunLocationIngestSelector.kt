package com.wendao.run.core.location

import android.location.LocationManager
import com.baidu.location.BDLocation

/**
 * 跑步计距：系统 GPS/Fused 为主；百度仅在系统长时间无回调时兜底计距（不驱动地图）。
 */
class RunLocationIngestSelector(
    private val onReanchor: () -> Unit,
    private val onIngest: (RunLocationUpdate, source: String) -> Unit,
) {

    private var lastSystemAtMs = 0L
    private var baiduFallbackActive = false

    fun reset() {
        lastSystemAtMs = 0L
        baiduFallbackActive = false
    }

    /** 系统定位：始终参与计距 */
    fun onSystem(update: RunLocationUpdate) {
        lastSystemAtMs = System.currentTimeMillis()
        baiduFallbackActive = false
        onIngest(update, "system")
    }

    /** 百度：仅当系统超过 [FALLBACK_AFTER_MS] 无回调时兜底 */
    fun onBaidu(update: RunLocationUpdate) {
        val silentMs = if (lastSystemAtMs > 0L) {
            System.currentTimeMillis() - lastSystemAtMs
        } else {
            Long.MAX_VALUE
        }
        if (silentMs < FALLBACK_AFTER_MS) {
            PaoxiuLocationLog.i("baidu skipped (system active ${silentMs}ms ago)")
            return
        }
        if (!baiduFallbackActive) {
            baiduFallbackActive = true
            PaoxiuLocationLog.i("baidu fallback ON after ${silentMs}ms system silence, reanchor")
            onReanchor()
        }
        onIngest(update, "baidu-fallback")
    }

    companion object {
        private const val FALLBACK_AFTER_MS = 8_000L
    }
}

fun RunLocationUpdate.isGpsFix(): Boolean =
    locType == BDLocation.TypeGpsLocation

fun RunLocationUpdate.isSystemNetworkProvider(): Boolean =
    provider == LocationManager.NETWORK_PROVIDER

fun RunLocationUpdate.isNetworkFix(): Boolean = when {
    provider == LocationManager.NETWORK_PROVIDER -> true
    locType == BDLocation.TypeNetWorkLocation -> true
    locType == BDLocation.TypeOffLineLocation -> true
    locType == BDLocation.TypeGpsLocation -> false
    provider == "baidu" -> locType != BDLocation.TypeGpsLocation
    else -> false
}
