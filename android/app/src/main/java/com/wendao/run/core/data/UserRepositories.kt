package com.wendao.run.core.data

import android.content.Context
import com.wendao.run.core.network.AuthApi
import com.wendao.run.core.network.SpiritRootApi
import com.wendao.run.core.network.UserApi
import com.wendao.run.core.network.model.DetectSpiritRootRequest
import com.wendao.run.core.network.model.GuestLoginRequest
import com.wendao.run.core.network.model.SpiritRootResultDto
import com.wendao.run.core.network.model.UserDto
import com.wendao.run.core.network.model.UserProfileDto
import com.wendao.run.core.network.model.WeChatLoginRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authApi: AuthApi,
    private val sessionStore: SessionStore,
    private val userApi: UserApi,
) {
    private val _profile = MutableStateFlow<UserProfileDto?>(null)
    val profile: StateFlow<UserProfileDto?> = _profile.asStateFlow()

    fun hasSession(): Boolean = sessionStore.hasSession()

    suspend fun loginAsGuest(): Result<UserDto> = runCatching {
        val deviceId = sessionStore.getOrCreateDeviceId(context)
        val response = authApi.guestLogin(GuestLoginRequest(deviceId = deviceId))
        sessionStore.saveSession(
            accessToken = response.accessToken,
            refreshToken = response.refreshToken,
            userId = response.user.id,
        )
        refreshProfile()
        response.user
    }

    suspend fun loginWithWeChat(code: String, daoName: String? = null): Result<UserDto> = runCatching {
        val response = authApi.wechatLogin(WeChatLoginRequest(code = code, daoName = daoName))
        sessionStore.saveSession(
            accessToken = response.accessToken,
            refreshToken = response.refreshToken,
            userId = response.user.id,
        )
        refreshProfile()
        response.user
    }

    suspend fun refreshProfile(): Result<UserProfileDto> = runCatching {
        if (!hasSession()) error("未登录")
        val profile = userApi.me()
        _profile.value = profile
        profile
    }.onFailure { error ->
        if (error is HttpException && error.code() == 401) {
            clearSession()
        }
    }

    fun clearProfile() {
        _profile.value = null
    }

    fun clearSession() {
        sessionStore.clearSession()
        clearProfile()
    }
}

@Singleton
class SpiritRootRepository @Inject constructor(
    private val spiritRootApi: SpiritRootApi,
    private val authRepository: AuthRepository,
) {
    suspend fun detect(clientRunId: String): Result<SpiritRootResultDto> = runCatching {
        val result = spiritRootApi.detect(DetectSpiritRootRequest(clientRunId))
        authRepository.refreshProfile()
        result
    }.mapError { it.toReadableApiError("灵根觉醒失败，请稍后重试") }
}

private fun <T> Result<T>.mapError(transform: (Throwable) -> Throwable): Result<T> =
    fold(
        onSuccess = { Result.success(it) },
        onFailure = { Result.failure(transform(it)) },
    )

private fun Throwable.toReadableApiError(fallback: String): Throwable {
    if (this !is HttpException) return this
    val body = response()?.errorBody()?.string().orEmpty()
    if (body.isBlank()) return RuntimeException(fallback, this)
    val message = runCatching {
        Json.parseToJsonElement(body).jsonObject["message"]?.jsonPrimitive?.content
    }.getOrNull()
    return RuntimeException(message?.takeIf { it.isNotBlank() } ?: fallback, this)
}
