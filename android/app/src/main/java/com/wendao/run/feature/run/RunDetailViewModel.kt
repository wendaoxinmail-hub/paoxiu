package com.wendao.run.feature.run

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wendao.run.core.data.AuthRepository
import com.wendao.run.core.database.entity.RunRecordEntity
import com.wendao.run.core.run.KeepRunStats
import com.wendao.run.core.run.RunAnalytics
import com.wendao.run.core.run.RunAnalyticsEngine
import com.wendao.run.core.run.RunKeepMetrics
import com.wendao.run.core.run.RunRepository
import com.wendao.run.core.run.RunTrackPoint
import com.wendao.run.core.run.TrackEffectResolver
import com.wendao.run.core.run.TrackVisualEffect
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RunDetailUiState(
    val loading: Boolean = true,
    val run: RunRecordEntity? = null,
    val trackPoints: List<RunTrackPoint> = emptyList(),
    val analytics: RunAnalytics? = null,
    val keepStats: KeepRunStats? = null,
    val trackEffect: TrackVisualEffect = TrackVisualEffect.Default,
)

@HiltViewModel
class RunDetailViewModel @Inject constructor(
    private val runRepository: RunRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RunDetailUiState())
    val uiState: StateFlow<RunDetailUiState> = _uiState.asStateFlow()

    fun load(runId: String) {
        viewModelScope.launch {
            _uiState.value = RunDetailUiState(loading = true)
            val run = runRepository.getRun(runId)
            val pointEntities = runRepository.getTrackPointEntities(runId)
            val points = pointEntities.map { RunTrackPoint(it.lat, it.lng) }
            val previous = run?.finishedAt?.let { runRepository.getPreviousFinishedRun(it) }
            val analytics = run?.let {
                RunAnalyticsEngine.analyze(it, pointEntities, previous)
            }
            val keepStats = run?.let { RunKeepMetrics.compute(it, pointEntities) }
            val profile = authRepository.refreshProfile().getOrNull()
                ?: authRepository.profile.value
            val trackEffect = TrackEffectResolver.resolve(
                spiritRoot = profile?.spiritRoot,
                weaponId = profile?.equippedWeaponId,
                armorId = profile?.equippedArmorId,
                accessoryId = profile?.equippedAccessoryId,
            )
            _uiState.value = RunDetailUiState(
                loading = false,
                run = run,
                trackPoints = points,
                analytics = analytics,
                keepStats = keepStats,
                trackEffect = trackEffect,
            )
        }
    }
}
