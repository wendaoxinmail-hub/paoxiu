package com.wendao.run.core.network

import com.wendao.run.core.network.model.FinishRunRequest
import com.wendao.run.core.network.model.FinishRunResponse
import com.wendao.run.core.network.model.RunSummaryDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface RunApi {
    @POST("api/v1/runs/finish")
    suspend fun finishRun(@Body request: FinishRunRequest): FinishRunResponse

    @GET("api/v1/runs/recent")
    suspend fun recentRuns(@Query("limit") limit: Int = 10): List<RunSummaryDto>
}
