package com.wendao.run.core.network.model

import kotlinx.serialization.Serializable

@Serializable
data class RunRewardDto(
    val xpGained: Long = 0,
    val stonesGained: Long = 0,
    val realmLabel: String? = null,
    val leveledUp: Boolean = false,
    val adventureTitle: String? = null,
    val adventureDescription: String? = null,
)

@Serializable
data class TechniqueDto(
    val id: String,
    val name: String,
    val description: String,
    val tier: String = "basic",
    val unlockRealm: String = "",
    val practiceMethod: String = "",
    val practiceCondition: String = "",
    val practiceFlow: String = "",
    val masteryEffect: String = "",
    val price: Long,
    val requiredGrottoLevel: Int,
    val basicFreeComprehend: Boolean = false,
    val freeComprehend: Boolean = false,
    val xpMultiplier: Double,
    val stoneMultiplier: Double,
    val owned: Boolean,
    val active: Boolean,
    val proficiency: Int = 0,
    val canComprehend: Boolean = false,
) {
    val isBasic: Boolean get() = tier == "basic"
    val tierLabel: String get() = if (isBasic) "基础功法" else "高阶功法"
}

@Serializable
data class EquipmentDto(
    val id: String,
    val slot: String,
    val name: String,
    val description: String,
    val price: Long,
    val requiredGrottoLevel: Int,
    val xpMultiplier: Double,
    val stoneMultiplier: Double,
    val owned: Boolean,
    val equipped: Boolean,
)

@Serializable
data class GrottoStateDto(
    val grottoLevel: Int = 1,
    val realmXp: Long = 0,
    val fieldReadyAt: String? = null,
    val canHarvest: Boolean = false,
    val spiritBeastLevel: Int = 0,
    val spiritBeastName: String? = null,
    val grottoName: String? = null,
    val upgradeCost: Long = 0,
    val lastActionReward: Long = 0,
    val lastActionMessage: String? = null,
)

@Serializable
data class SectSummaryDto(
    val id: Long,
    val name: String,
    val description: String,
    val memberCount: Int,
)

@Serializable
data class SectMemberDto(
    val userId: Long,
    val daoName: String,
    val realmLabel: String,
    val role: String,
    val mentorUserId: Long? = null,
)

@Serializable
data class SectDetailDto(
    val id: Long,
    val name: String,
    val description: String,
    val memberCount: Int,
    val leaderUserId: Long,
    val members: List<SectMemberDto>,
)

@Serializable
data class CreateSectRequest(
    val name: String,
    val description: String,
)

@Serializable
data class SetMentorRequest(
    val mentorUserId: Long,
)

@Serializable
data class RankingEntryDto(
    val rank: Int,
    val userId: Long,
    val daoName: String,
    val distanceMeters: Double,
)

@Serializable
data class MarketItemDto(
    val id: String,
    val type: String,
    val name: String,
    val description: String,
    val price: Long,
    val requiredGrottoLevel: Int,
    val owned: Boolean = false,
    val inventoryCount: Int = 0,
)

@Serializable
data class MarketBuyResultDto(
    val itemId: String,
    val message: String,
    val spiritStones: Long,
    val peiyuanPillCount: Int = 0,
)

@Serializable
data class RenameGrottoRequest(
    val name: String,
)

@Serializable
data class SectDailyTaskDto(
    val targetMeters: Double,
    val contributionMeters: Double,
    val completed: Boolean,
    val claimed: Boolean,
    val rewardStones: Long,
    val rewardXp: Long,
    val message: String? = null,
)
