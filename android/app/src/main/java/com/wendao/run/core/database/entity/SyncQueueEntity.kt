package com.wendao.run.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val payloadType: String,
    val payloadJson: String,
    val createdAt: Long,
    val retryCount: Int = 0,
)
