package com.wendao.run.core.network.model

import kotlinx.serialization.Serializable

@Serializable
data class UserProfileDto(
    val id: Long,
    val daoName: String,
    val spiritStones: Long,
    val realmLabel: String,
    val realmXp: Long = 0,
    val xpToNext: Long = 0,
    val spiritRoot: String? = null,
    val spiritRootLabel: String? = null,
    val spiritRootTestCompleted: Boolean = false,
    val spiritRootConfidence: Double = 0.0,
    val activeTechniqueId: String? = null,
    val equippedWeaponId: String? = null,
    val equippedArmorId: String? = null,
    val sectId: Long? = null,
    val mentorUserId: Long? = null,
    val grottoLevel: Int = 1,
    val spiritBeastLevel: Int = 0,
    val spiritBeastName: String? = null,
    val runStreakDays: Int = 0,
    val mentorDaoName: String? = null,
    val equippedAccessoryId: String? = null,
    val grottoName: String? = null,
)

@Serializable
data class UpdateProfileRequest(
    val daoName: String? = null,
)

@Serializable
data class DetectSpiritRootRequest(
    val clientRunId: String,
)

@Serializable
data class SpiritRootResultDto(
    val spiritRoot: String,
    val spiritRootLabel: String,
    val confidence: Double,
    val description: String,
    val realmLabel: String,
    val daoName: String,
    val message: String,
)
