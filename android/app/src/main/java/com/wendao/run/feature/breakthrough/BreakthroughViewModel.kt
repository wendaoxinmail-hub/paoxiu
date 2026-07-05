package com.wendao.run.feature.breakthrough

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wendao.run.core.run.RunRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BreakthroughUiState(
    val realmLabel: String? = null,
    val adventureTitle: String? = null,
)

@HiltViewModel
class BreakthroughViewModel @Inject constructor(
    private val runRepository: RunRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BreakthroughUiState())
    val uiState: StateFlow<BreakthroughUiState> = _uiState.asStateFlow()

    fun load(runId: String) {
        viewModelScope.launch {
            val run = runRepository.getRun(runId)
            _uiState.value = BreakthroughUiState(
                realmLabel = run?.realmLabelAfter,
                adventureTitle = run?.adventureTitle,
            )
        }
    }
}
