package com.wendao.run.core.run

import com.wendao.run.core.database.dao.RunDao
import com.wendao.run.core.database.dao.SyncQueueDao
import com.wendao.run.core.database.entity.RunRecordEntity
import com.wendao.run.core.database.entity.SyncQueueEntity
import com.wendao.run.core.database.entity.TrackPointEntity
import com.wendao.run.core.data.SessionStore
import com.wendao.run.core.network.RunApi
import com.wendao.run.core.network.model.FinishRunRequest
import com.wendao.run.core.network.model.TrackPointDto
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 跑步记录仓库：本地 Room 为主，联网后同步至服务端反作弊与奖励结算。
 *
 * 同步策略：finish 后立即尝试上传；失败则写入队列并重试。
 */
@Singleton
class RunRepository @Inject constructor(
    private val runDao: RunDao,
    private val syncQueueDao: SyncQueueDao,
    private val runApi: RunApi,
    private val sessionStore: SessionStore,
    private val json: Json,
) {

    private val trackingEngine = RunTrackingEngine()

    fun observeRecentFinished(limit: Int = 5): Flow<List<RunRecordEntity>> =
        runDao.observeRecentFinished(limit)

    suspend fun getActiveRun(): RunRecordEntity? =
        runDao.getLatestByStatus(RunRecordEntity.STATUS_ACTIVE)
            ?: runDao.getLatestByStatus(RunRecordEntity.STATUS_PAUSED)

    suspend fun getRun(runId: String): RunRecordEntity? = runDao.getRun(runId)

    suspend fun getPointCount(runId: String): Int = runDao.countPoints(runId)

    suspend fun getTrackPoints(runId: String): List<RunTrackPoint> =
        runDao.getPoints(runId).map { RunTrackPoint(it.lat, it.lng) }

    suspend fun getTrackPointEntities(runId: String): List<TrackPointEntity> =
        runDao.getPoints(runId)

    suspend fun getPreviousFinishedRun(beforeFinishedAt: Long): RunRecordEntity? =
        runDao.getPreviousFinished(beforeFinishedAt)

    fun observeAllFinished(): Flow<List<RunRecordEntity>> = runDao.observeAllFinished()

    suspend fun startRun(runType: String = RunRecordEntity.RUN_TYPE_NORMAL): String {
        val userId = sessionStore.getUserId()
            ?: throw IllegalStateException("未登录，无法开始修炼")
        val runId = UUID.randomUUID().toString()
        runDao.insertRun(
            RunRecordEntity(
                id = runId,
                userId = userId,
                startedAt = System.currentTimeMillis(),
                runType = runType,
            ),
        )
        trackingEngine.reset()
        return runId
    }

    suspend fun setRunStatus(runId: String, status: String) {
        val run = runDao.getRun(runId) ?: return
        runDao.updateRun(run.copy(status = status))
    }

    fun markTrackingBreak() {
        trackingEngine.markBreak()
    }

    /**
     * Keep 式定位 ingest：平滑坐标 + 位移速度 + 滚动/移动配速。
     * 每次更新都返回快照（含未入库的展示坐标）；仅 accepted 时写入轨迹点。
     */
    suspend fun processLocationUpdate(
        runId: String,
        lat: Double,
        lng: Double,
        accuracyM: Float,
        recordedAt: Long = System.currentTimeMillis(),
        isNetworkFix: Boolean = false,
    ): TrackingSnapshot? {
        val run = runDao.getRun(runId) ?: return null
        val maxAccuracy = 100f
        val snapshot = trackingEngine.ingest(
            lat, lng, accuracyM, recordedAt, maxAccuracy, isNetworkFix,
        )

        if (snapshot.accepted) {
            runDao.insertPoints(
                listOf(
                    TrackPointEntity(
                        runId = runId,
                        lat = snapshot.rawLat,
                        lng = snapshot.rawLng,
                        recordedAt = recordedAt,
                        accuracyM = accuracyM,
                        speedKmh = snapshot.derivedSpeedKmh,
                    ),
                ),
            )
            runDao.updateRun(
                run.copy(
                    distanceM = snapshot.distanceM,
                    maxSpeedKmh = snapshot.maxSpeedKmh,
                ),
            )
        }
        return snapshot
    }

    suspend fun tickDuration(runId: String, durationSec: Long) {
        val run = runDao.getRun(runId) ?: return
        runDao.updateRun(run.copy(durationSec = durationSec))
    }

    suspend fun markRunFinished(runId: String, durationSec: Long): RunRecordEntity {
        val run = runDao.getRun(runId) ?: throw IllegalStateException("修炼记录不存在")
        if (run.status == RunRecordEntity.STATUS_FINISHED) {
            return run
        }
        val avgPace = RunGeoUtils.computePaceSecPerKm(run.distanceM, durationSec.coerceAtLeast(1))
        val finished = run.copy(
            finishedAt = System.currentTimeMillis(),
            durationSec = durationSec,
            avgPaceSecPerKm = avgPace,
            status = RunRecordEntity.STATUS_FINISHED,
        )
        runDao.updateRun(finished)
        trackingEngine.reset()
        return runDao.getRun(runId) ?: finished
    }

    suspend fun finishRun(runId: String, durationSec: Long): RunRecordEntity {
        val finished = markRunFinished(runId, durationSec)
        syncRunInternal(finished)
        return runDao.getRun(runId) ?: finished
    }

    /** 收功后异步上传，避免阻塞前台服务结束与页面跳转。 */
    suspend fun syncFinishedRun(runId: String) {
        val run = runDao.getRun(runId) ?: return
        if (run.status != RunRecordEntity.STATUS_FINISHED) return
        syncRunInternal(run)
    }

    suspend fun syncPendingRuns() {
        processSyncQueue()
        val pending = runDao.getRunsPendingSync(RunRecordEntity.SYNC_PENDING, 10)
        val failed = runDao.getRunsPendingSync(RunRecordEntity.SYNC_FAILED, 10)
        (pending + failed).distinctBy { it.id }.forEach { syncRunInternal(it) }
    }

    suspend fun waitUntilFinished(runId: String, timeoutMs: Long = 15_000): RunRecordEntity? {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val run = runDao.getRun(runId)
            if (run?.status == RunRecordEntity.STATUS_FINISHED) return run
            kotlinx.coroutines.delay(100)
        }
        return runDao.getRun(runId)
    }

    suspend fun syncRun(runId: String): Result<Unit> = runCatching {
        val run = runDao.getRun(runId) ?: error("本地修炼记录不存在")
        if (run.status != RunRecordEntity.STATUS_FINISHED) {
            error("修炼记录尚未结束，请稍后再试")
        }
        if (run.runType == RunRecordEntity.RUN_TYPE_SPIRIT_ROOT && run.distanceM < 800.0) {
            error("灵根试炼需跑满约 1 公里（当前 ${RunGeoUtils.formatDistance(run.distanceM)}）")
        }
        syncRunInternal(run)
        val synced = runDao.getRun(runId)
        if (synced?.syncStatus != RunRecordEntity.SYNC_SYNCED) {
            error("修炼记录尚未同步到云端，请检查网络后重试")
        }
    }

    suspend fun processSyncQueue() {
        val items = syncQueueDao.peek(20)
        for (item in items) {
            if (item.payloadType != PAYLOAD_TYPE_FINISH_RUN) continue
            try {
                val request = json.decodeFromString<FinishRunRequest>(item.payloadJson)
                val response = runApi.finishRun(request)
                syncQueueDao.deleteByIds(listOf(item.id))
                runDao.getRun(request.clientRunId)?.let { run ->
                    runDao.updateRun(
                        run.copy(
                            syncStatus = RunRecordEntity.SYNC_SYNCED,
                            serverRunId = response.serverRunId,
                            rejectReason = response.rejectReason,
                            rewardXp = response.rewards?.xpGained,
                            rewardStones = response.rewards?.stonesGained,
                            adventureTitle = response.rewards?.adventureTitle,
                            adventureDescription = response.rewards?.adventureDescription,
                            realmLabelAfter = response.rewards?.realmLabel,
                            leveledUp = response.rewards?.leveledUp == true,
                        ),
                    )
                }
            } catch (_: Exception) {
                // keep in queue for retry
            }
        }
    }

    private suspend fun syncRunInternal(run: RunRecordEntity) {
        if (run.status != RunRecordEntity.STATUS_FINISHED) return
        val points = runDao.getPoints(run.id)
        val request = buildFinishRequest(run, points)
        try {
            val response = runApi.finishRun(request)
            runDao.updateRun(
                run.copy(
                    syncStatus = RunRecordEntity.SYNC_SYNCED,
                    serverRunId = response.serverRunId,
                    rejectReason = response.rejectReason,
                    rewardXp = response.rewards?.xpGained,
                    rewardStones = response.rewards?.stonesGained,
                    adventureTitle = response.rewards?.adventureTitle,
                    adventureDescription = response.rewards?.adventureDescription,
                    realmLabelAfter = response.rewards?.realmLabel,
                    leveledUp = response.rewards?.leveledUp == true,
                ),
            )
        } catch (error: Exception) {
            enqueueSync(run.id, request)
            runDao.updateRun(run.copy(syncStatus = RunRecordEntity.SYNC_FAILED))
        }
    }

    private suspend fun enqueueSync(runId: String, request: FinishRunRequest) {
        syncQueueDao.enqueue(
            SyncQueueEntity(
                payloadType = PAYLOAD_TYPE_FINISH_RUN,
                payloadJson = json.encodeToString(request),
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    private fun buildFinishRequest(
        run: RunRecordEntity,
        points: List<TrackPointEntity>,
    ): FinishRunRequest {
        val startedAt = Instant.ofEpochMilli(run.startedAt).toString()
        val finishedAt = Instant.ofEpochMilli(run.finishedAt ?: System.currentTimeMillis()).toString()
        return FinishRunRequest(
            clientRunId = run.id,
            startedAt = startedAt,
            finishedAt = finishedAt,
            durationSeconds = run.durationSec.toInt(),
            distanceMeters = run.distanceM,
            avgPaceSecPerKm = run.avgPaceSecPerKm ?: 0.0,
            maxSpeedKmh = run.maxSpeedKmh,
            runType = run.runType,
            points = points.map {
                TrackPointDto(
                    lat = it.lat,
                    lng = it.lng,
                    recordedAt = Instant.ofEpochMilli(it.recordedAt).toString(),
                    accuracyM = it.accuracyM,
                    speedKmh = it.speedKmh,
                )
            },
        )
    }

    companion object {
        const val PAYLOAD_TYPE_FINISH_RUN = "run_finish"
    }
}
