package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "textbook_allocations",
    indices = [
        Index(value = ["className", "section"]),
        Index(value = ["studentId"]),
        Index(value = ["status"]),
        Index(value = ["bookCondition"])
    ]
)
data class TextbookAllocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val className: Int,
    val section: String,
    val subject: String,
    val bookTitle: String,
    val studentId: String,
    val studentName: String,
    val status: String, // RECEIVED, NOT_AVAILABLE, NOT_RECEIVED, RETURNED
    val distributionDate: String = "2026-04-10",
    val returnDueDate: String = "2027-03-15",
    val returnedDate: String = "",
    val bookCondition: String = "GOOD", // NEW, GOOD, FAIR, DAMAGED, LOST
    val bookCode: String = "",
    val remarks: String = ""
)
