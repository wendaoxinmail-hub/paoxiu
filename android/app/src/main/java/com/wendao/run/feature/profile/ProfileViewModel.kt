package com.wendao.run.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wendao.run.core.data.AuthRepository
import com.wendao.run.core.data.GameRepository
import com.wendao.run.core.network.model.EquipmentDto
import com.wendao.run.core.network.model.TechniqueDto
import com.wendao.run.core.network.model.UserProfileDto
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val gameRepository: GameRepository,
) : ViewModel() {

    val profile: StateFlow<UserProfileDto?> = authRepository.profile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _techniques = MutableStateFlow<List<TechniqueDto>>(emptyList())
    val techniques: StateFlow<List<TechniqueDto>> = _techniques.asStateFlow()

    private val _equipment = MutableStateFlow<List<EquipmentDto>>(emptyList())
    val equipment: StateFlow<List<EquipmentDto>> = _equipment.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            authRepository.refreshProfile()
            gameRepository.techniques().onSuccess { _techniques.value = it }
            gameRepository.equipment().onSuccess { _equipment.value = it }
        }
    }

    fun logout() {
        authRepository.clearSession()
    }
}
