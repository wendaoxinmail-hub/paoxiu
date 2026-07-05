package com.wendao.run.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.wendao.run.core.database.entity.SyncQueueEntity

@Dao
interface SyncQueueDao {
    @Insert
    suspend fun enqueue(item: SyncQueueEntity)

    @Query("SELECT * FROM sync_queue ORDER BY createdAt ASC LIMIT :limit")
    suspend fun peek(limit: Int = 20): List<SyncQueueEntity>

    @Query("DELETE FROM sync_queue WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)
}
