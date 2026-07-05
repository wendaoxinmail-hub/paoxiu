package com.wendao.run.core.network

import com.wendao.run.core.network.model.AuthResponse
import com.wendao.run.core.network.model.GuestLoginRequest
import com.wendao.run.core.network.model.WeChatLoginRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("api/v1/auth/guest")
    suspend fun guestLogin(@Body request: GuestLoginRequest): AuthResponse

    @POST("api/v1/auth/wechat")
    suspend fun wechatLogin(@Body request: WeChatLoginRequest): AuthResponse
}
