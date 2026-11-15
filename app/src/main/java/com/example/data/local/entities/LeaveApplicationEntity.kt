package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "leave_applications")
data class LeaveApplicationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val applicantId: String,
    val applicantName: String,
    val applicantRole: String = "TEACHER", // STUDENT, PARENT, TEACHER
    val studentUuid: String = "",
    val className: Int = 0,
    val section: String = "",
    val guardianName: String = "",
    val guardianPhone: String = "",
    val leaveType: String, // CASUAL, MEDICAL, EARNED, FAMILY_EVENT, EMERGENCY
    val startDate: String, // YYYY-MM-DD
    val endDate: String, // YYYY-MM-DD
    val daysCount: Int,
    val reason: String,
    val attachmentReceiptPath: String = "", // Local image URI or path to receipt/medical certificate
    val status: String = "PENDING", // PENDING, APPROVED, REJECTED
    val reviewNotes: String = "",
    val reviewedBy: String = "",
    val appliedTimestamp: Long = System.currentTimeMillis(),
    val reviewedTimestamp: Long? = null
)
