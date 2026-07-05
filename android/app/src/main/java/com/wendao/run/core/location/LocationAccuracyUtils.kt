package com.wendao.run.core.location

/**
 * Android 定位常返回 accuracy=0（未知精度），直接丢弃会导致整条计距链路无输入。
 */
object LocationAccuracyUtils {

    /** 未知精度时的保守估计（米），用于滤噪阈值 */
    const val UNKNOWN_ACCURACY_M = 18f

    const val MAX_RUN_ACCURACY_M = 100f

    /** 原始精度超过上限则拒绝该点（不做 clamp 后再放行） */
    fun isOverMaxAccuracy(raw: Float, maxM: Float = MAX_RUN_ACCURACY_M): Boolean =
        raw.isFinite() && raw > 0f && raw > maxM

    fun normalizeAccuracyM(raw: Float): Float {
        if (!raw.isFinite() || raw <= 0f) return UNKNOWN_ACCURACY_M
        return raw.coerceAtMost(MAX_RUN_ACCURACY_M)
    }
}
