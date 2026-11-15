package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "students",
    indices = [
        Index(value = ["admissionNumber"], unique = true),
        Index(value = ["className", "section"]),
        Index(value = ["rollNumber"])
    ]
)
data class StudentEntity(
    @PrimaryKey val uuid: String,
    val admissionNumber: String,
    val rollNumber: Int,
    val nameEn: String,
    val nameUr: String,
    val className: Int, // 1 to 8
    val section: String, // A, B, C
    val gender: String, // Male, Female, Other
    val dob: String, // YYYY-MM-DD
    val category: String, // GENERAL, SC, ST, OBC
    val fatherName: String,
    val motherName: String,
    val primaryContact: String,
    val whatsAppNumber: String,
    val residentialAddress: String,
    val status: String = "ACTIVE", // ACTIVE, TRANSFERRED, ARCHIVED
    
    // UIDAI Compliance Guardrails - No Raw Aadhaar
    val aadhaarReferenceToken: String,
    val maskedAadhaar: String, // Format: XXXX-XXXX-1234
    val aadhaarStatus: String, // VERIFIED, PENDING, FAILED, EXEMPTION
    val consentRecorded: Boolean = true,
    val consentTimestamp: Long = System.currentTimeMillis(),
    
    val academicSession: String = "2026-2027",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
