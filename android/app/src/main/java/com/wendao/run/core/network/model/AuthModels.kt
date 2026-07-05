package com.wendao.run.core.network.model

import kotlinx.serialization.Serializable

@Serializable
data class GuestLoginRequest(
    val deviceId: String,
    val daoName: String? = null,
)

@Serializable
data class WeChatLoginRequest(
    val code: String,
    val daoName: String? = null,
)

@Serializable
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val user: UserDto,
)

@Serializable
data class UserDto(
    val id: Long,
    val daoName: String,
    val spiritStones: Long,
    val realmLabel: String,
    val spiritRoot: String? = null,
    val spiritRootTestCompleted: Boolean = false,
)
