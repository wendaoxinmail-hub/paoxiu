package com.wendao.run.feature.spiritroot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wendao.run.core.data.SpiritRootRepository
import com.wendao.run.core.network.model.SpiritRootResultDto
import com.wendao.run.core.run.RunRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SpiritRootResultUiState(
    val loading: Boolean = true,
    val result: SpiritRootResultDto? = null,
    val error: String? = null,
)

@HiltViewModel
class SpiritRootResultViewModel @Inject constructor(
    private val spiritRootRepository: SpiritRootRepository,
    private val runRepository: RunRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SpiritRootResultUiState())
    val uiState: StateFlow<SpiritRootResultUiState> = _uiState.asStateFlow()

    fun detect(runId: String) {
        viewModelScope.launch {
            _uiState.value = SpiritRootResultUiState(loading = true)
            var syncError: Throwable? = null
            var synced = false
            for (attempt in 0 until 3) {
                runRepository.syncRun(runId)
                    .onSuccess { synced = true }
                    .onFailure { error -> syncError = error }
                if (synced) break
                if (attempt < 2) {
                    kotlinx.coroutines.delay(500L * (attempt + 1))
                }
            }
            if (!synced) {
                _uiState.value = SpiritRootResultUiState(
                    loading = false,
                    error = syncError?.message ?: "修炼记录同步失败，请稍后重试",
                )
                return@launch
            }
            runRepository.syncPendingRuns()
            spiritRootRepository.detect(runId)
                .onSuccess { result ->
                    _uiState.value = SpiritRootResultUiState(loading = false, result = result)
                }
                .onFailure { error ->
                    _uiState.value = SpiritRootResultUiState(
                        loading = false,
                        error = error.message ?: "灵根觉醒失败，请稍后重试",
                    )
                }
        }
    }
}
