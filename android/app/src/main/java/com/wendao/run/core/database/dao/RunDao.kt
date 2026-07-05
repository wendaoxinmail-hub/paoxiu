package com.wendao.run.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.wendao.run.core.database.entity.RunRecordEntity
import com.wendao.run.core.database.entity.TrackPointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RunDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRun(run: RunRecordEntity)

    @Update
    suspend fun updateRun(run: RunRecordEntity)

    @Insert
    suspend fun insertPoints(points: List<TrackPointEntity>)

    @Query("SELECT * FROM run_records WHERE id = :runId LIMIT 1")
    suspend fun getRun(runId: String): RunRecordEntity?

    @Query("SELECT * FROM track_points WHERE runId = :runId ORDER BY recordedAt ASC")
    suspend fun getPoints(runId: String): List<TrackPointEntity>

    @Query("SELECT COUNT(*) FROM track_points WHERE runId = :runId")
    suspend fun countPoints(runId: String): Int

    @Query(
        """
        SELECT * FROM run_records
        WHERE status = :status
        ORDER BY startedAt DESC
        LIMIT 1
        """,
    )
    suspend fun getLatestByStatus(status: String): RunRecordEntity?

    @Query(
        """
        SELECT * FROM run_records
        WHERE status = :status
        ORDER BY startedAt DESC
        LIMIT 1
        """,
    )
    fun observeLatestByStatus(status: String): Flow<RunRecordEntity?>

    @Query(
        """
        SELECT * FROM run_records
        WHERE status = 'FINISHED'
        ORDER BY finishedAt DESC
        LIMIT :limit
        """,
    )
    fun observeRecentFinished(limit: Int): Flow<List<RunRecordEntity>>

    @Query(
        """
        SELECT * FROM run_records
        WHERE status = 'FINISHED'
        ORDER BY finishedAt DESC
        """,
    )
    fun observeAllFinished(): Flow<List<RunRecordEntity>>

    @Query(
        """
        SELECT * FROM run_records
        WHERE status = 'FINISHED' AND finishedAt IS NOT NULL AND finishedAt < :beforeFinishedAt
        ORDER BY finishedAt DESC
        LIMIT 1
        """,
    )
    suspend fun getPreviousFinished(beforeFinishedAt: Long): RunRecordEntity?

    @Query(
        """
        SELECT * FROM run_records
        WHERE syncStatus = :syncStatus
        ORDER BY finishedAt ASC
        LIMIT :limit
        """,
    )
    suspend fun getRunsPendingSync(syncStatus: String, limit: Int): List<RunRecordEntity>
}
