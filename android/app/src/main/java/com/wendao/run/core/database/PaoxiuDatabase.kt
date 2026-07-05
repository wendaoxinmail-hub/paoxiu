package com.wendao.run.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.wendao.run.core.database.dao.RunDao
import com.wendao.run.core.database.dao.SyncQueueDao
import com.wendao.run.core.database.entity.RunRecordEntity
import com.wendao.run.core.database.entity.SyncQueueEntity
import com.wendao.run.core.database.entity.TrackPointEntity

@Database(
    entities = [
        SyncQueueEntity::class,
        RunRecordEntity::class,
        TrackPointEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class PaoxiuDatabase : RoomDatabase() {
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun runDao(): RunDao
}
