package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "school_profile")
data class SchoolProfileEntity(
    @PrimaryKey val id: Int = 1,
    val schoolNameEn: String = "Govt. Model Middle School",
    val schoolNameUr: String = "گورنمنٹ ماڈل مڈل اسکول",
    val udiseCode: String = "01010408201",
    val villageOrTown: String = "Sopore Central",
    val block: String = "Zaingeer",
    val district: String = "Baramulla",
    val state: String = "Jammu & Kashmir",
    val pinCode: String = "193201",
    val headmasterName: String = "Mohd. Yousuf Wani",
    val headmasterPhone: String = "+91 94190 00000",
    val headmasterEmail: String = "hm.gmms@jk.gov.in",
    val currentSession: String = "2026-2027",
    val totalWorkingDays: Int = 220,
    val schoolCategory: String = "Primary & Middle (Classes 1–8)",
    val logoAssetPath: String = "ic_school_logo",
    val isSetupComplete: Boolean = true
)
