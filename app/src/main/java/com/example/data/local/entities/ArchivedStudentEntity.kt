package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "archived_students")
data class ArchivedStudentEntity(
    @PrimaryKey val uuid: String,
    val admissionNumber: String,
    val finalRollNumber: Int,
    val nameEn: String,
    val nameUr: String,
    val completedClass: Int,
    val graduationSession: String,
    val fatherName: String,
    val contact: String,
    val maskedAadhaar: String,
    val archiveReason: String = "Graduated / Transferred",
    val archivedTimestamp: Long = System.currentTimeMillis()
)
