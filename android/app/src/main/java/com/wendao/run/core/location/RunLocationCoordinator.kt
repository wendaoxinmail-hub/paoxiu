package com.wendao.run.core.location

import com.wendao.run.core.run.RunGeoUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 跑步定位协调：同一时刻只有一个 SDK 驱动地图+计距，避免双源漂移。
 *
 * 1. 先启百度（GCJ02 与底图一致）
 * 2. 5s 内无百度回调 → 切系统 GPS（互斥，停百度）
 * 3. 百度恢复后不抢回（防 mid-run 坐标跳变）
 */
class RunLocationCoordinator(
    private val baiduTracker: BaiduRunLocationTracker,
    private val systemSource: SystemRunLocationSource,
    private val scope: CoroutineScope,
    private val onDisplay: (RunLocationUpdate) -> Unit,
    private val onIngest: (RunLocationUpdate, source: String) -> Unit,
    private val onReanchor: () -> Unit,
) {

    enum class ActiveSource { NONE, BAIDU, SYSTEM }

    private var activeSource = ActiveSource.NONE
    private var lastBaiduAtMs = 0L
    private var watchdogJob: Job? = null

    fun start() {
        stop()
        activeSource = ActiveSource.BAIDU
        PaoxiuLocationLog.service("locationCoordinator", "start mode=baidu-first")
        baiduTracker.start(
            onLocation = { update -> onBaiduFix(update) },
            replayCachedFix = false,
        )
        scope.launch {
            delay(BAIDU_BOOTSTRAP_MS)
            if (lastBaiduAtMs == 0L && activeSource == ActiveSource.BAIDU) {
                PaoxiuLocationLog.service("locationCoordinator", "baidu silent ${BAIDU_BOOTSTRAP_MS}ms -> system")
                switchToSystem(reanchor = false)
            }
        }
        watchdogJob = scope.launch {
            while (isActive) {
                delay(WATCHDOG_INTERVAL_MS)
                if (activeSource != ActiveSource.BAIDU || lastBaiduAtMs == 0L) continue
                val silentMs = System.currentTimeMillis() - lastBaiduAtMs
                if (silentMs >= BAIDU_STALL_MS) {
                    PaoxiuLocationLog.service("locationCoordinator", "baidu stall ${silentMs}ms -> system")
                    switchToSystem(reanchor = true)
                }
            }
        }
    }

    fun stop() {
        watchdogJob?.cancel()
        watchdogJob = null
        baiduTracker.stop()
        systemSource.stop()
        activeSource = ActiveSource.NONE
        lastBaiduAtMs = 0L
    }

    private fun onBaiduFix(update: RunLocationUpdate) {
        lastBaiduAtMs = System.currentTimeMillis()
        if (activeSource == ActiveSource.SYSTEM) return
        activeSource = ActiveSource.BAIDU
        onDisplay(update)
        onIngest(update, "baidu")
    }

    private fun switchToSystem(reanchor: Boolean) {
        if (activeSource == ActiveSource.SYSTEM) return
        baiduTracker.stop()
        if (reanchor) onReanchor()
        activeSource = ActiveSource.SYSTEM
        systemSource.start { update ->
            onDisplay(update)
            onIngest(update, "system")
        }
    }

    companion object {
        private const val BAIDU_BOOTSTRAP_MS = 5_000L
        private const val BAIDU_STALL_MS = 15_000L
        private const val WATCHDOG_INTERVAL_MS = 5_000L
    }
}
