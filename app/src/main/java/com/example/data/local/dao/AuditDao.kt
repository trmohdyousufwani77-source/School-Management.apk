package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entities.AuditTrailEntity
import com.example.data.local.entities.SyncQueueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditDao {
    @Query("SELECT * FROM audit_trail ORDER BY timestamp DESC LIMIT 100")
    fun getAllAuditLogs(): Flow<List<AuditTrailEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudit(audit: AuditTrailEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncQueue(queue: SyncQueueEntity)

    @Query("SELECT COUNT(*) FROM sync_queue WHERE isSynced = 0")
    fun getPendingSyncCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE isSynced = 0")
    suspend fun getPendingSyncCount(): Int

    @Query("SELECT * FROM sync_queue WHERE isSynced = 0 ORDER BY timestamp ASC")
    suspend fun getPendingSyncQueueItems(): List<SyncQueueEntity>

    @Query("UPDATE sync_queue SET isSynced = 1 WHERE isSynced = 0")
    suspend fun markAllSynced()

    @Query("DELETE FROM sync_queue WHERE isSynced = 1")
    suspend fun purgeSyncedItems()
}
