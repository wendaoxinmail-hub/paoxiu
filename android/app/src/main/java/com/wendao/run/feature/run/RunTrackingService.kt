package com.wendao.run.feature.run

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.wendao.run.BuildConfig
import com.wendao.run.MainActivity
import com.wendao.run.R
import com.wendao.run.core.database.entity.RunRecordEntity
import com.wendao.run.core.location.BaiduRunLocationTracker
import com.wendao.run.core.location.PaoxiuLocationLog
import com.wendao.run.core.location.RunLocationCoordinator
import com.wendao.run.core.location.RunLocationUpdate
import com.wendao.run.core.location.SpiritRootDemoRoute
import com.wendao.run.core.location.SystemRunLocationSource
import com.wendao.run.core.location.isNetworkFix
import com.wendao.run.core.location.hasFineLocationPermission
import com.wendao.run.core.location.isLocationEnabled
import com.wendao.run.core.run.RunGeoUtils
import com.wendao.run.core.run.RunRepository
import com.wendao.run.core.run.RunSessionStatus
import com.wendao.run.core.run.RunSessionStore
import com.wendao.run.core.run.ActiveRunState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.NonCancellable

@AndroidEntryPoint
class RunTrackingService : Service() {

    @Inject lateinit var trackLocationSource: SystemRunLocationSource
    @Inject lateinit var baiduLocationTracker: BaiduRunLocationTracker
    @Inject lateinit var runRepository: RunRepository
    @Inject lateinit var runSessionStore: RunSessionStore

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var timerJob: Job? = null
    private var syntheticRouteJob: Job? = null
    private var runId: String? = null
    private var runType: String = RunRecordEntity.RUN_TYPE_NORMAL
    private var startedAt: Long = 0L
    private var durationSec: Long = 0L
    private var paused = false
    private var locationFixCount = 0
    private val ingestMutex = Mutex()
    @Volatile private var isStopping = false
    private var locationCoordinator: RunLocationCoordinator? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                runType = intent.getStringExtra(EXTRA_RUN_TYPE) ?: RunRecordEntity.RUN_TYPE_NORMAL
                serviceScope.launch { startTracking() }
            }
            ACTION_PAUSE -> serviceScope.launch { pauseTracking() }
            ACTION_RESUME -> serviceScope.launch { resumeTracking() }
            ACTION_STOP -> serviceScope.launch { stopTracking(finish = true) }
        }
        return START_STICKY
    }

    private suspend fun startTracking() {
        if (runId != null) return

        runId = runRepository.startRun(runType)
        startedAt = System.currentTimeMillis()
        durationSec = 0L
        paused = false

        runSessionStore.clearTrackPoints()
        locationFixCount = 0
        publishState(RunSessionStatus.RUNNING)
        startForegroundWithLocationType(buildNotification("修炼中…"))
        PaoxiuLocationLog.service(
            "startTracking",
            "build=${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE}) " +
                "runId=$runId finePerm=${hasFineLocationPermission()} gpsOn=${isLocationEnabled()}",
        )
        startTimer()
        startLocation()
        startSyntheticSpiritRootRouteIfNeeded()
    }

    private suspend fun pauseTracking() {
        if (runId == null || paused) return
        paused = true
        runId?.let { runRepository.setRunStatus(it, RunRecordEntity.STATUS_PAUSED) }
        trackLocationSource.stop()
        baiduLocationTracker.stop()
        locationCoordinator?.stop()
        locationCoordinator = null
        runRepository.markTrackingBreak()
        publishState(RunSessionStatus.PAUSED)
        updateNotification("修炼暂停")
    }

    private suspend fun resumeTracking() {
        if (runId == null || !paused) return
        paused = false
        runId?.let { runRepository.setRunStatus(it, RunRecordEntity.STATUS_ACTIVE) }
        runRepository.markTrackingBreak()
        startLocation()
        publishState(RunSessionStatus.RUNNING)
        updateNotification("修炼中…")
    }

    private suspend fun stopTracking(finish: Boolean) {
        val currentRunId = runId ?: return
        isStopping = true
        timerJob?.cancel()
        timerJob = null
        syntheticRouteJob?.cancel()
        syntheticRouteJob = null
        trackLocationSource.stop()
        baiduLocationTracker.stop()
        locationCoordinator?.stop()
        locationCoordinator = null

        if (finish) {
            withContext(NonCancellable) {
                runCatching {
                    runRepository.markRunFinished(currentRunId, durationSec)
                }.onFailure { e ->
                    PaoxiuLocationLog.w("markRunFinished failed runId=$currentRunId", e)
                }
            }
            serviceScope.launch { runRepository.syncFinishedRun(currentRunId) }
        }

        runSessionStore.update(null)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        runId = null
    }

    /** 跑步定位：仅系统 GPS，与 Keep 一致，避免百度/系统双源漂移 */
    private fun startLocation() {
        val currentRunId = runId ?: return
        if (!hasFineLocationPermission()) {
            PaoxiuLocationLog.service("startLocation skipped", "no fine permission")
            return
        }
        isStopping = false
        locationCoordinator?.stop()
        locationCoordinator = null
        baiduLocationTracker.stop()
        PaoxiuLocationLog.service("startLocation", "mode=system-gps-only")
        trackLocationSource.start { update ->
            if (paused || isStopping) return@start
            serviceScope.launch { publishDisplayLocation(update) }
            onTrackLocation(currentRunId, update, "system")
        }
    }

    private fun onTrackLocation(runId: String, update: RunLocationUpdate, source: String) {
        if (isStopping) return
        locationFixCount += 1
        serviceScope.launch {
            if (!isStopping) ingestLocationFix(runId, update, source)
        }
    }

    private suspend fun publishDisplayLocation(update: RunLocationUpdate) {
        runSessionStore.patch { current ->
            current.copy(
                displayLat = update.lat,
                displayLng = update.lng,
                accuracyM = update.accuracyM,
            )
        }
    }

    private suspend fun ingestLocationFix(runId: String, update: RunLocationUpdate, source: String) {
        ingestMutex.withLock {
            val snapshot = runRepository.processLocationUpdate(
                runId = runId,
                lat = update.lat,
                lng = update.lng,
                accuracyM = update.accuracyM,
                recordedAt = update.recordedAt,
                isNetworkFix = update.isNetworkFix(),
            )
            val pointCount = runRepository.getPointCount(runId)
            val prev = runSessionStore.activeRun.value
            if (snapshot != null) {
                PaoxiuLocationLog.ingest(
                    accepted = snapshot.accepted,
                    rawLat = snapshot.rawLat,
                    rawLng = snapshot.rawLng,
                    accuracyM = update.accuracyM,
                    pointCount = pointCount,
                    rejectReason = snapshot.rejectReason.name,
                    segmentM = snapshot.segmentM,
                    distanceM = snapshot.distanceM,
                )
                if (snapshot.accepted && snapshot.segmentM > 0) {
                    runSessionStore.appendTrackPoint(snapshot.rawLat, snapshot.rawLng)
                }
                publishState(
                    status = if (paused) RunSessionStatus.PAUSED else RunSessionStatus.RUNNING,
                    distanceM = snapshot.distanceM,
                    currentSpeedKmh = snapshot.instantSpeedKmh.takeIf { it > 0.2f }
                        ?: snapshot.derivedSpeedKmh.takeIf { it > 0.2f },
                    rollingPaceSecPerKm = snapshot.rollingPaceSecPerKm,
                    avgPaceSecPerKm = RunGeoUtils.computePaceSecPerKm(snapshot.distanceM, durationSec),
                    movingDurationSec = snapshot.movingDurationSec,
                    lastLat = if (snapshot.accepted) snapshot.rawLat else prev?.lastLat,
                    lastLng = if (snapshot.accepted) snapshot.rawLng else prev?.lastLng,
                    displayLat = update.lat,
                    displayLng = update.lng,
                    accuracyM = update.accuracyM,
                    locationFixCount = locationFixCount,
                )
            }
        }
    }

    private fun startSyntheticSpiritRootRouteIfNeeded() {
        if (runType != RunRecordEntity.RUN_TYPE_SPIRIT_ROOT) return
        if (!com.wendao.run.core.util.EmulatorUtils.isEmulator()) return
        val currentRunId = runId ?: return
        syntheticRouteJob?.cancel()
        syntheticRouteJob = serviceScope.launch {
            delay(3_000)
            if (runRepository.getPointCount(currentRunId) > 0) return@launch
            for ((index, point) in SpiritRootDemoRoute.points.withIndex()) {
                if (!isActive || paused) return@launch
                val run = runRepository.getRun(currentRunId) ?: return@launch
                if (run.distanceM >= 1_000.0) return@launch
                runRepository.processLocationUpdate(
                    runId = currentRunId,
                    lat = point.lat,
                    lng = point.lng,
                    accuracyM = 8f,
                    recordedAt = System.currentTimeMillis() + index * 4_000L,
                    isNetworkFix = false,
                )
                runSessionStore.appendTrackPoint(point.lat, point.lng)
                publishState(
                    status = if (paused) RunSessionStatus.PAUSED else RunSessionStatus.RUNNING,
                    currentSpeedKmh = 10f,
                    lastLat = point.lat,
                    lastLng = point.lng,
                    displayLat = point.lat,
                    displayLng = point.lng,
                )
                delay(4_000)
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isActive) {
                delay(1000)
                if (!paused) {
                    durationSec += 1
                    runId?.let { runRepository.tickDuration(it, durationSec) }
                    publishState(if (paused) RunSessionStatus.PAUSED else RunSessionStatus.RUNNING)
                }
            }
        }
    }

    private suspend fun publishState(
        status: RunSessionStatus,
        distanceM: Double? = null,
        currentSpeedKmh: Float? = null,
        rollingPaceSecPerKm: Double? = null,
        avgPaceSecPerKm: Double? = null,
        movingDurationSec: Long? = null,
        lastLat: Double? = null,
        lastLng: Double? = null,
        displayLat: Double? = null,
        displayLng: Double? = null,
        accuracyM: Float? = null,
        locationFixCount: Int? = null,
        locationHint: String? = null,
    ) {
        val currentRunId = runId ?: return
        val run = runRepository.getRun(currentRunId) ?: return
        val pointCount = runRepository.getPointCount(currentRunId)
        val prev = runSessionStore.activeRun.value
        val speed = currentSpeedKmh ?: prev?.currentSpeedKmh ?: 0f
        val lat = lastLat ?: prev?.lastLat
        val lng = lastLng ?: prev?.lastLng
        val mapLat = displayLat ?: prev?.displayLat ?: lat
        val mapLng = displayLng ?: prev?.displayLng ?: lng
        val hint = locationHint ?: prev?.locationHint
        runSessionStore.update(
            ActiveRunState(
                runId = currentRunId,
                status = status,
                startedAt = startedAt,
                durationSec = durationSec,
                distanceM = distanceM ?: prev?.distanceM ?: run.distanceM,
                currentSpeedKmh = speed,
                pointCount = pointCount,
                runType = run.runType,
                lastLat = lat,
                lastLng = lng,
                displayLat = mapLat,
                displayLng = mapLng,
                accuracyM = accuracyM ?: prev?.accuracyM,
                rollingPaceSecPerKm = rollingPaceSecPerKm ?: prev?.rollingPaceSecPerKm,
                avgPaceSecPerKm = avgPaceSecPerKm ?: prev?.avgPaceSecPerKm,
                movingDurationSec = movingDurationSec ?: prev?.movingDurationSec ?: 0L,
                locationFixCount = locationFixCount ?: prev?.locationFixCount ?: 0,
                locationHint = hint,
            ),
        )
        if (mapLat != null && mapLng != null) {
            PaoxiuLocationLog.publish(mapLat, mapLng, pointCount, accuracyM)
        }
    }

    private fun startForegroundWithLocationType(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(content: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(content))
    }

    private fun buildNotification(content: String): Notification {
        createChannel()
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "修炼定位",
            NotificationManager.IMPORTANCE_LOW,
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        locationCoordinator?.stop()
        locationCoordinator = null
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.wendao.run.run.START"
        const val ACTION_PAUSE = "com.wendao.run.run.PAUSE"
        const val ACTION_RESUME = "com.wendao.run.run.RESUME"
        const val ACTION_STOP = "com.wendao.run.run.STOP"
        const val EXTRA_RUN_TYPE = "run_type"

        private const val CHANNEL_ID = "run_tracking"
        private const val NOTIFICATION_ID = 1001

        fun startIntent(context: Context, runType: String = RunRecordEntity.RUN_TYPE_NORMAL): Intent =
            Intent(context, RunTrackingService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_RUN_TYPE, runType)
            }

        fun pauseIntent(context: Context): Intent =
            Intent(context, RunTrackingService::class.java).apply { action = ACTION_PAUSE }

        fun resumeIntent(context: Context): Intent =
            Intent(context, RunTrackingService::class.java).apply { action = ACTION_RESUME }

        fun stopIntent(context: Context): Intent =
            Intent(context, RunTrackingService::class.java).apply { action = ACTION_STOP }
    }
}
