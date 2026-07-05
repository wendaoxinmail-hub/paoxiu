package com.wendao.run.feature.grotto

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wendao.run.core.data.AuthRepository
import com.wendao.run.core.data.GameRepository
import com.wendao.run.core.network.model.EquipmentDto
import com.wendao.run.core.network.model.GrottoStateDto
import com.wendao.run.core.network.model.TechniqueDto
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GrottoUiState(
    val loading: Boolean = true,
    val grotto: GrottoStateDto? = null,
    val techniques: List<TechniqueDto> = emptyList(),
    val equipment: List<EquipmentDto> = emptyList(),
    val grottoNameInput: String = "",
    val spiritRoot: String? = null,
    val message: String? = null,
    val error: String? = null,
)

@HiltViewModel
class GrottoViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GrottoUiState())
    val uiState: StateFlow<GrottoUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            val grotto = gameRepository.grotto().getOrNull()
            val techniques = gameRepository.techniques().getOrDefault(emptyList())
            val equipment = gameRepository.equipment().getOrDefault(emptyList())
            authRepository.refreshProfile()
            val spiritRoot = authRepository.profile.value?.spiritRoot
            _uiState.value = GrottoUiState(
                loading = false,
                grotto = grotto,
                techniques = techniques,
                equipment = equipment,
                grottoNameInput = grotto?.grottoName.orEmpty(),
                spiritRoot = spiritRoot,
            )
        }
    }

    fun updateGrottoName(value: String) {
        _uiState.value = _uiState.value.copy(grottoNameInput = value)
    }

    fun renameGrotto() = action { gameRepository.renameGrotto(_uiState.value.grottoNameInput.trim()) }

    fun harvest() = action { gameRepository.harvestGrotto() }

    fun upgrade() = action { gameRepository.upgradeGrotto() }

    fun feedBeast() = action { gameRepository.feedBeast() }

    fun retreat() = action { gameRepository.retreat() }

    fun buyTechnique(id: String) = action {
        gameRepository.buyTechnique(id)
        gameRepository.techniques()
    }

    fun equipTechnique(id: String) = action {
        gameRepository.equipTechnique(id)
        gameRepository.techniques()
    }

    fun buyEquipment(id: String) = action {
        gameRepository.buyEquipment(id)
        gameRepository.equipment()
    }

    fun equipEquipment(id: String) = action {
        gameRepository.equipEquipment(id)
        gameRepository.equipment()
    }

    private fun action(block: suspend () -> Result<*>) {
        viewModelScope.launch {
            block()
                .onSuccess {
                    refresh()
                    _uiState.value = _uiState.value.copy(
                        message = (it as? GrottoStateDto)?.lastActionMessage ?: "操作成功",
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(error = error.message)
                }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null, error = null)
    }

    private fun <T> Result<T>.getOrDefault(default: T): T = getOrElse { default }
}
