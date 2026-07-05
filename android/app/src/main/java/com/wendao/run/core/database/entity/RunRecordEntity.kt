package com.wendao.run.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "run_records")
data class RunRecordEntity(
    @PrimaryKey val id: String,
    val userId: Long,
    val startedAt: Long,
    val finishedAt: Long? = null,
    val durationSec: Long = 0,
    val distanceM: Double = 0.0,
    val avgPaceSecPerKm: Double? = null,
    val maxSpeedKmh: Double = 0.0,
    val status: String = STATUS_ACTIVE,
    val syncStatus: String = SYNC_PENDING,
    val serverRunId: Long? = null,
    val runType: String = RUN_TYPE_NORMAL,
    val rejectReason: String? = null,
    val rewardXp: Long? = null,
    val rewardStones: Long? = null,
    val adventureTitle: String? = null,
    val adventureDescription: String? = null,
    val realmLabelAfter: String? = null,
    val leveledUp: Boolean = false,
) {
    companion object {
        const val STATUS_ACTIVE = "ACTIVE"
        const val STATUS_PAUSED = "PAUSED"
        const val STATUS_FINISHED = "FINISHED"

        const val SYNC_PENDING = "PENDING"
        const val SYNC_SYNCED = "SYNCED"
        const val SYNC_FAILED = "FAILED"

        const val RUN_TYPE_NORMAL = "NORMAL"
        const val RUN_TYPE_SPIRIT_ROOT = "SPIRIT_ROOT"
    }
}
