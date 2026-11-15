package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "student_voice_memos")
data class StudentVoiceMemoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val studentUuid: String,
    val studentName: String,
    val tag: String = "Behavioral",
    val noteText: String = "",
    val filePath: String,
    val durationSeconds: Int,
    val recordedBy: String = "Teacher",
    val recordedTimestamp: Long = System.currentTimeMillis()
)
