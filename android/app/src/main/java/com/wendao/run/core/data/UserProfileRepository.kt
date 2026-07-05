package com.wendao.run.core.data

import com.wendao.run.core.network.UserApi
import com.wendao.run.core.network.model.UpdateProfileRequest
import com.wendao.run.core.network.model.UserProfileDto
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException

@Singleton
class UserProfileRepository @Inject constructor(
    private val userApi: UserApi,
) {
    suspend fun updateProfile(daoName: String): Result<UserProfileDto> = runCatching {
        userApi.updateProfile(UpdateProfileRequest(daoName = daoName))
    }.mapError()

    private fun <T> Result<T>.mapError(): Result<T> = fold(
        onSuccess = { Result.success(it) },
        onFailure = { Result.failure(it.toReadableApiError()) },
    )

    private fun Throwable.toReadableApiError(): Throwable {
        if (this !is HttpException) return this
        val body = response()?.errorBody()?.string().orEmpty()
        val message = runCatching {
            Json.parseToJsonElement(body).jsonObject["message"]?.jsonPrimitive?.content
        }.getOrNull()
        return RuntimeException(message?.takeIf { it.isNotBlank() } ?: "保存失败", this)
    }
}
