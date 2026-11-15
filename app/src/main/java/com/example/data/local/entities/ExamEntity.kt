package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exams")
data class ExamEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String, // e.g. "2026-2027"
    val term: String, // UNIT_TEST_1, UNIT_TEST_2, TERM_1, TERM_2
    val className: Int, // 1 to 8
    val subjectNameEn: String,
    val subjectNameUr: String,
    val maxMarks: Int = 100,
    val passMarks: Int = 33,
    val examDate: String = "2026-10-15"
)
