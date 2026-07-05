package com.wendao.run.feature.run

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wendao.run.core.database.entity.RunRecordEntity
import com.wendao.run.core.run.RunGeoUtils
import com.wendao.run.core.data.AuthRepository
import com.wendao.run.core.run.RunRepository
import com.wendao.run.core.run.TrackEffectResolver
import com.wendao.run.core.run.TrackVisualEffect
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RunSummaryUiState(
    val loading: Boolean = true,
    val run: RunRecordEntity? = null,
    val trackPoints: List<com.wendao.run.core.run.RunTrackPoint> = emptyList(),
    val trackEffect: TrackVisualEffect = TrackVisualEffect.Default,
)

@HiltViewModel
class RunSummaryViewModel @Inject constructor(
    private val runRepository: RunRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RunSummaryUiState())
    val uiState: StateFlow<RunSummaryUiState> = _uiState.asStateFlow()

    fun load(runId: String) {
        viewModelScope.launch {
            _uiState.value = RunSummaryUiState(loading = true)
            val run = runRepository.getRun(runId)
            val points = runRepository.getTrackPoints(runId)
            _uiState.value = RunSummaryUiState(loading = false, run = run, trackPoints = points)
            runRepository.syncPendingRuns()
            val refreshed = runRepository.getRun(runId)
            val profile = authRepository.refreshProfile().getOrNull()
                ?: authRepository.profile.value
            val trackEffect = TrackEffectResolver.resolve(
                spiritRoot = profile?.spiritRoot,
                weaponId = profile?.equippedWeaponId,
                armorId = profile?.equippedArmorId,
                accessoryId = profile?.equippedAccessoryId,
            )
            _uiState.value = RunSummaryUiState(
                loading = false,
                run = refreshed ?: run,
                trackPoints = points,
                trackEffect = trackEffect,
            )
        }
    }
}

fun RunRecordEntity.syncStatusLabel(): String = when (syncStatus) {
    RunRecordEntity.SYNC_SYNCED -> "已同步云端"
    RunRecordEntity.SYNC_FAILED -> "待重试同步"
    else -> "同步中…"
}

fun RunRecordEntity.summaryDistance(): String = RunGeoUtils.formatDistance(distanceM)

fun RunRecordEntity.summaryDuration(): String = RunGeoUtils.formatDuration(durationSec)

fun RunRecordEntity.summaryPace(): String = RunGeoUtils.formatPace(avgPaceSecPerKm)
