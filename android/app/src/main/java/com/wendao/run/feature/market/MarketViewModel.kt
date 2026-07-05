package com.wendao.run.feature.market

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wendao.run.core.data.AuthRepository
import com.wendao.run.core.data.GameRepository
import com.wendao.run.core.network.model.MarketItemDto
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MarketUiState(
    val loading: Boolean = true,
    val items: List<MarketItemDto> = emptyList(),
    val spiritStones: Long = 0,
    val message: String? = null,
    val error: String? = null,
)

@HiltViewModel
class MarketViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MarketUiState())
    val uiState: StateFlow<MarketUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            authRepository.refreshProfile()
            val stones = authRepository.profile.value?.spiritStones ?: 0
            gameRepository.market()
                .onSuccess { items ->
                    _uiState.value = MarketUiState(loading = false, items = items, spiritStones = stones)
                }
                .onFailure { _uiState.value = MarketUiState(loading = false, error = it.message) }
        }
    }

    fun buy(id: String) {
        viewModelScope.launch {
            gameRepository.buyMarket(id)
                .onSuccess { result ->
                    _uiState.value = _uiState.value.copy(
                        message = result.message,
                        spiritStones = result.spiritStones,
                    )
                    refresh()
                }
                .onFailure { _uiState.value = _uiState.value.copy(error = it.message) }
        }
    }
}
