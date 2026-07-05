package com.wendao.run.core.network

import com.wendao.run.core.network.model.CreateSectRequest
import com.wendao.run.core.network.model.EquipmentDto
import com.wendao.run.core.network.model.GrottoStateDto
import com.wendao.run.core.network.model.MarketBuyResultDto
import com.wendao.run.core.network.model.MarketItemDto
import com.wendao.run.core.network.model.RankingEntryDto
import com.wendao.run.core.network.model.RenameGrottoRequest
import com.wendao.run.core.network.model.SectDailyTaskDto
import com.wendao.run.core.network.model.SectDetailDto
import com.wendao.run.core.network.model.SectSummaryDto
import com.wendao.run.core.network.model.SetMentorRequest
import com.wendao.run.core.network.model.TechniqueDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface GameApi {
    @GET("api/v1/techniques")
    suspend fun techniques(): List<TechniqueDto>

    @GET("api/v1/techniques/{id}")
    suspend fun technique(@Path("id") id: String): TechniqueDto

    @POST("api/v1/techniques/{id}/comprehend")
    suspend fun comprehendTechnique(@Path("id") id: String): TechniqueDto

    @POST("api/v1/techniques/{id}/buy")
    suspend fun buyTechnique(@Path("id") id: String): TechniqueDto

    @POST("api/v1/techniques/{id}/equip")
    suspend fun equipTechnique(@Path("id") id: String): TechniqueDto

    @GET("api/v1/equipment")
    suspend fun equipment(): List<EquipmentDto>

    @POST("api/v1/equipment/{id}/buy")
    suspend fun buyEquipment(@Path("id") id: String): EquipmentDto

    @POST("api/v1/equipment/{id}/equip")
    suspend fun equipEquipment(@Path("id") id: String): EquipmentDto

    @GET("api/v1/grotto")
    suspend fun grotto(): GrottoStateDto

    @POST("api/v1/grotto/harvest")
    suspend fun harvestGrotto(): GrottoStateDto

    @POST("api/v1/grotto/upgrade")
    suspend fun upgradeGrotto(): GrottoStateDto

    @POST("api/v1/grotto/feed-beast")
    suspend fun feedBeast(): GrottoStateDto

    @POST("api/v1/grotto/retreat")
    suspend fun retreat(): GrottoStateDto

    @PATCH("api/v1/grotto/name")
    suspend fun renameGrotto(@Body request: RenameGrottoRequest): GrottoStateDto

    @GET("api/v1/sects")
    suspend fun sects(): List<SectSummaryDto>

    @GET("api/v1/sects/mine")
    suspend fun mySect(): SectDetailDto

    @POST("api/v1/sects")
    suspend fun createSect(@Body request: CreateSectRequest): SectDetailDto

    @POST("api/v1/sects/{id}/join")
    suspend fun joinSect(@Path("id") id: Long): SectDetailDto

    @POST("api/v1/sects/mentor")
    suspend fun setMentor(@Body request: SetMentorRequest): SectDetailDto

    @GET("api/v1/rankings/weekly")
    suspend fun weeklyRanking(@Query("limit") limit: Int = 20): List<RankingEntryDto>

    @GET("api/v1/market")
    suspend fun market(): List<MarketItemDto>

    @POST("api/v1/market/{id}/buy")
    suspend fun buyMarket(@Path("id") id: String): MarketBuyResultDto

    @GET("api/v1/sects/tasks/daily")
    suspend fun dailySectTask(): SectDailyTaskDto

    @POST("api/v1/sects/tasks/daily/claim")
    suspend fun claimDailySectTask(): SectDailyTaskDto
}
