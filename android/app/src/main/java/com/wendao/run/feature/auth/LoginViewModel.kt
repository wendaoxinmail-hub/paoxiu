package com.wendao.run.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wendao.run.core.auth.WeChatAuthManager
import com.wendao.run.core.auth.WeChatAuthResult
import com.wendao.run.core.auth.WeChatAuthResultBus
import com.wendao.run.core.data.AuthRepository
import com.wendao.run.core.data.SessionStore
import com.wendao.run.core.network.model.UserDto
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val loading: Boolean = false,
    val loggedIn: Boolean = false,
    val user: UserDto? = null,
    val error: String? = null,
    val weChatConfigured: Boolean = false,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionStore: SessionStore,
    private val weChatAuthManager: WeChatAuthManager,
) : ViewModel() {

    fun needsStoryIntro(): Boolean = !sessionStore.hasSeenStoryIntro()

    private val _uiState = MutableStateFlow(LoginUiState(weChatConfigured = weChatAuthManager.isConfigured()))
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            WeChatAuthResultBus.results.collect { result ->
                when (result) {
                    is WeChatAuthResult.Success -> loginWithWeChatCode(result.code)
                    is WeChatAuthResult.Error -> _uiState.update { it.copy(loading = false, error = result.message) }
                }
            }
        }
        if (authRepository.hasSession()) {
            viewModelScope.launch {
                _uiState.update { it.copy(loading = true) }
                authRepository.refreshProfile()
                    .onSuccess { profile ->
                        _uiState.update {
                            it.copy(
                                loading = false,
                                loggedIn = true,
                                user = profile.toUserDto(),
                            )
                        }
                    }
                    .onFailure {
                        authRepository.clearSession()
                        _uiState.update {
                            it.copy(
                                loading = false,
                                loggedIn = false,
                                error = "登录已过期，请重新入道",
                            )
                        }
                    }
            }
        }
    }

    fun loginAsGuest() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            authRepository.loginAsGuest()
                .onSuccess { user ->
                    _uiState.update { it.copy(loading = false, loggedIn = true, user = user) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(loading = false, error = error.message ?: "入道失败")
                    }
                }
        }
    }

    fun loginWithWeChat() {
        when {
            !weChatAuthManager.isConfigured() -> {
                _uiState.update { it.copy(error = "请先在 local.properties 配置 WECHAT_APP_ID") }
            }
            !weChatAuthManager.isWeChatInstalled() -> {
                _uiState.update { it.copy(error = "未安装微信，请先安装微信客户端") }
            }
            !weChatAuthManager.sendAuthRequest() -> {
                _uiState.update { it.copy(error = "无法唤起微信，请检查 AppID 与签名") }
            }
            else -> _uiState.update { it.copy(loading = true, error = null) }
        }
    }

    private fun loginWithWeChatCode(code: String) {
        viewModelScope.launch {
            authRepository.loginWithWeChat(code)
                .onSuccess { user ->
                    _uiState.update { it.copy(loading = false, loggedIn = true, user = user) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(loading = false, error = error.message ?: "微信入道失败")
                    }
                }
        }
    }
}

private fun com.wendao.run.core.network.model.UserProfileDto.toUserDto(): UserDto =
    UserDto(
        id = id,
        daoName = daoName,
        spiritStones = spiritStones,
        realmLabel = realmLabel,
        spiritRoot = spiritRoot,
        spiritRootTestCompleted = spiritRootTestCompleted,
    )
