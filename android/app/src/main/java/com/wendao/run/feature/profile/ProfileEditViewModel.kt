package com.wendao.run.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wendao.run.core.data.AuthRepository
import com.wendao.run.core.data.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileEditUiState(
    val daoName: String = "",
    val saving: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

@HiltViewModel
class ProfileEditViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userProfileRepository: UserProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileEditUiState())
    val uiState: StateFlow<ProfileEditUiState> = _uiState.asStateFlow()

    init {
        _uiState.value = ProfileEditUiState(daoName = authRepository.profile.value?.daoName.orEmpty())
    }

    fun updateDaoName(value: String) {
        _uiState.value = _uiState.value.copy(daoName = value, error = null)
    }

    fun save(onSuccess: () -> Unit) {
        val name = _uiState.value.daoName.trim()
        if (name.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "道号不能为空")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(saving = true, error = null)
            userProfileRepository.updateProfile(name)
                .onSuccess {
                    authRepository.refreshProfile()
                    _uiState.value = _uiState.value.copy(saving = false, message = "已保存")
                    onSuccess()
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(saving = false, error = it.message)
                }
        }
    }
}
