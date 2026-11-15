package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "teachers")
data class TeacherEntity(
    @PrimaryKey val employeeId: String,
    val name: String,
    val designation: String, // Headmaster, TGT Science, PRT Urdu, etc.
    val contactPhone: String,
    val email: String,
    val assignedClasses: String, // Comma-separated: "Class 3-A, Class 4-A"
    val assignedSubjects: String, // Comma-separated: "Mathematics, Urdu"
    val employmentStatus: String = "Active", // Active, On Leave, Deputed
    val clQuotaTotal: Int = 12,
    val clQuotaUsed: Int = 2,
    val mlQuotaTotal: Int = 10,
    val mlQuotaUsed: Int = 1,
    val elQuotaTotal: Int = 15,
    val elQuotaUsed: Int = 0
)
