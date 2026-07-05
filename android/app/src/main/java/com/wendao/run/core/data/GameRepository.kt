package com.wendao.run.core.data

import com.wendao.run.core.network.GameApi
import com.wendao.run.core.network.model.CreateSectRequest
import com.wendao.run.core.network.model.EquipmentDto
import com.wendao.run.core.network.model.GrottoStateDto
import com.wendao.run.core.network.model.MarketBuyResultDto
import com.wendao.run.core.network.model.MarketItemDto
import com.wendao.run.core.network.model.RankingEntryDto
import com.wendao.run.core.network.model.SectDailyTaskDto
import com.wendao.run.core.network.model.SectDetailDto
import com.wendao.run.core.network.model.SectSummaryDto
import com.wendao.run.core.network.model.SetMentorRequest
import com.wendao.run.core.network.model.TechniqueDto
import javax.inject.Inject
import javax.inject.Singleton
import java.io.IOException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException

@Singleton
class GameRepository @Inject constructor(
    private val gameApi: GameApi,
    private val authRepository: AuthRepository,
) {
    suspend fun techniques(): Result<List<TechniqueDto>> = apiCall { gameApi.techniques() }

    suspend fun technique(id: String): Result<TechniqueDto> = apiCall { gameApi.technique(id) }

    suspend fun comprehendTechnique(id: String): Result<TechniqueDto> = apiCall {
        gameApi.comprehendTechnique(id).also { authRepository.refreshProfile() }
    }

    suspend fun buyTechnique(id: String): Result<TechniqueDto> = comprehendTechnique(id)

    suspend fun equipTechnique(id: String): Result<TechniqueDto> = apiCall {
        gameApi.equipTechnique(id).also { authRepository.refreshProfile() }
    }

    suspend fun equipment(): Result<List<EquipmentDto>> = apiCall { gameApi.equipment() }

    suspend fun buyEquipment(id: String): Result<EquipmentDto> = apiCall {
        gameApi.buyEquipment(id).also { authRepository.refreshProfile() }
    }

    suspend fun equipEquipment(id: String): Result<EquipmentDto> = apiCall {
        gameApi.equipEquipment(id).also { authRepository.refreshProfile() }
    }

    suspend fun grotto(): Result<GrottoStateDto> = apiCall { gameApi.grotto() }

    suspend fun harvestGrotto(): Result<GrottoStateDto> = apiCall {
        gameApi.harvestGrotto().also { authRepository.refreshProfile() }
    }

    suspend fun upgradeGrotto(): Result<GrottoStateDto> = apiCall {
        gameApi.upgradeGrotto().also { authRepository.refreshProfile() }
    }

    suspend fun feedBeast(): Result<GrottoStateDto> = apiCall {
        gameApi.feedBeast().also { authRepository.refreshProfile() }
    }

    suspend fun retreat(): Result<GrottoStateDto> = apiCall {
        gameApi.retreat().also { authRepository.refreshProfile() }
    }

    suspend fun renameGrotto(name: String): Result<GrottoStateDto> = apiCall {
        gameApi.renameGrotto(com.wendao.run.core.network.model.RenameGrottoRequest(name))
            .also { authRepository.refreshProfile() }
    }

    suspend fun sects(): Result<List<SectSummaryDto>> = apiCall { gameApi.sects() }

    suspend fun mySect(): Result<SectDetailDto> = apiCall { gameApi.mySect() }

    suspend fun createSect(name: String, description: String): Result<SectDetailDto> = apiCall {
        gameApi.createSect(CreateSectRequest(name, description)).also { authRepository.refreshProfile() }
    }

    suspend fun joinSect(id: Long): Result<SectDetailDto> = apiCall {
        gameApi.joinSect(id).also { authRepository.refreshProfile() }
    }

    suspend fun setMentor(mentorUserId: Long): Result<SectDetailDto> = apiCall {
        gameApi.setMentor(SetMentorRequest(mentorUserId)).also { authRepository.refreshProfile() }
    }

    suspend fun weeklyRanking(): Result<List<RankingEntryDto>> = apiCall {
        gameApi.weeklyRanking()
    }

    suspend fun market(): Result<List<MarketItemDto>> = apiCall { gameApi.market() }

    suspend fun buyMarket(id: String): Result<MarketBuyResultDto> = apiCall {
        gameApi.buyMarket(id).also { authRepository.refreshProfile() }
    }

    suspend fun dailySectTask(): Result<SectDailyTaskDto> = apiCall { gameApi.dailySectTask() }

    suspend fun claimDailySectTask(): Result<SectDailyTaskDto> = apiCall {
        gameApi.claimDailySectTask().also { authRepository.refreshProfile() }
    }

    private suspend fun <T> apiCall(block: suspend () -> T): Result<T> = runCatching {
        block()
    }.mapError { it.toReadableApiError() }

    private fun Throwable.toReadableApiError(): Throwable {
        if (this is IOException) {
            return RuntimeException("网络连接失败，请检查网络或稍后重试", this)
        }
        if (this !is HttpException) return this
        val code = code()
        val body = response()?.errorBody()?.string().orEmpty()
        val message = runCatching {
            Json.parseToJsonElement(body).jsonObject["message"]?.jsonPrimitive?.content
        }.getOrNull()?.takeIf { it.isNotBlank() }
        val readable = message ?: when (code) {
            401 -> "未登录或登录已过期"
            404 -> "服务端未找到该资源，请确认服务器已更新"
            in 500..599 -> "服务器异常($code)，请稍后重试"
            else -> "请求失败($code)"
        }
        return RuntimeException(readable, this)
    }

    private fun <T> Result<T>.mapError(transform: (Throwable) -> Throwable): Result<T> =
        fold(
            onSuccess = { Result.success(it) },
            onFailure = { Result.failure(transform(it)) },
        )
}
