package com.wendao.run.core.network

import com.wendao.run.core.network.model.DetectSpiritRootRequest
import com.wendao.run.core.network.model.SpiritRootResultDto
import com.wendao.run.core.network.model.UpdateProfileRequest
import com.wendao.run.core.network.model.UserProfileDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST

interface UserApi {
    @GET("api/v1/users/me")
    suspend fun me(): UserProfileDto

    @PATCH("api/v1/users/me")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): UserProfileDto
}

interface SpiritRootApi {
    @POST("api/v1/spirit-root/detect")
    suspend fun detect(@Body request: DetectSpiritRootRequest): SpiritRootResultDto
}
