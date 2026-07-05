package com.wendao.run.feature.technique

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wendao.run.core.data.GameRepository
import com.wendao.run.core.network.model.TechniqueDto
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TechniqueDetailUiState(
    val loading: Boolean = true,
    val technique: TechniqueDto? = null,
    val error: String? = null,
    val message: String? = null,
    val actionInProgress: Boolean = false,
)

@HiltViewModel
class TechniqueDetailViewModel @Inject constructor(
    private val gameRepository: GameRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TechniqueDetailUiState())
    val uiState: StateFlow<TechniqueDetailUiState> = _uiState.asStateFlow()

    fun load(techniqueId: String) {
        viewModelScope.launch {
            _uiState.value = TechniqueDetailUiState(loading = true)
            gameRepository.technique(techniqueId).fold(
                onSuccess = { technique ->
                    _uiState.value = TechniqueDetailUiState(loading = false, technique = technique)
                },
                onFailure = { detailError ->
                    // 详情接口不可用时，回退到列表数据（字段一致）
                    gameRepository.techniques().fold(
                        onSuccess = { list ->
                            val cached = list.find { it.id == techniqueId }
                            if (cached != null) {
                                _uiState.value = TechniqueDetailUiState(loading = false, technique = cached)
                            } else {
                                _uiState.value = TechniqueDetailUiState(
                                    loading = false,
                                    error = detailError.message ?: "加载失败",
                                )
                            }
                        },
                        onFailure = {
                            _uiState.value = TechniqueDetailUiState(
                                loading = false,
                                error = detailError.message ?: "加载失败",
                            )
                        },
                    )
                },
            )
        }
    }

    fun comprehend() {
        val id = _uiState.value.technique?.id ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actionInProgress = true, error = null)
            gameRepository.comprehendTechnique(id).fold(
                onSuccess = { updated ->
                    _uiState.value = _uiState.value.copy(
                        actionInProgress = false,
                        technique = updated,
                        message = "已领悟「${updated.name}」",
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        actionInProgress = false,
                        error = e.message ?: "领悟失败",
                    )
                },
            )
        }
    }

    fun equip() {
        val id = _uiState.value.technique?.id ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actionInProgress = true, error = null)
            gameRepository.equipTechnique(id).fold(
                onSuccess = { updated ->
                    _uiState.value = _uiState.value.copy(
                        actionInProgress = false,
                        technique = updated,
                        message = "已装备「${updated.name}」",
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        actionInProgress = false,
                        error = e.message ?: "装备失败",
                    )
                },
            )
        }
    }
}
