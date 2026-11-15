package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "attendance",
    primaryKeys = ["studentId", "date"],
    indices = [
        Index(value = ["date"]),
        Index(value = ["studentId"])
    ]
)
data class AttendanceEntity(
    val studentId: String,
    val date: String, // Format: YYYY-MM-DD
    val status: String, // PRESENT, ABSENT, LATE, LEAVE
    val recordedBy: String = "HM01",
    val recordedTimestamp: Long = System.currentTimeMillis(),
    val remarks: String = ""
)
