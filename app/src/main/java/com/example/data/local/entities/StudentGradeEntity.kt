package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "student_grades",
    indices = [
        Index(value = ["examId", "studentId"], unique = true),
        Index(value = ["studentId"]),
        Index(value = ["sessionId"])
    ]
)
data class StudentGradeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val examId: Long,
    val studentId: String,
    val sessionId: String,
    val marksObtained: Double,
    val isAbsent: Boolean = false,
    val gradeBand: String = "B+", // A+, A, B+, B, C, D, E
    val enteredBy: String = "T01",
    val remarks: String = "Satisfactory",
    val timestamp: Long = System.currentTimeMillis()
)
