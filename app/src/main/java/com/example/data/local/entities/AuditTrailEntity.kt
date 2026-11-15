package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_trail")
data class AuditTrailEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val actionType: String, // LEAVE_APPROVED, LEAVE_REJECTED, ATTENDANCE_BULK_MARKED, ATTENDANCE_REVERTED, VACUUM_EXECUTED, STUDENTS_ARCHIVED, PIN_AUTHENTICATED, SYNC_EXECUTED
    val actor: String,
    val targetId: String = "",
    val details: String
)

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entityType: String,
    val entityId: String,
    val changeType: String, // INSERT, UPDATE, DELETE
    val payloadJson: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)
