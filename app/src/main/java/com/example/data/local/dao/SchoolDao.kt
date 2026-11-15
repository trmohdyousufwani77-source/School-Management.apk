package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entities.SchoolProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SchoolDao {
    @Query("SELECT * FROM school_profile WHERE id = 1 LIMIT 1")
    fun getSchoolProfile(): Flow<SchoolProfileEntity?>

    @Query("SELECT * FROM school_profile WHERE id = 1 LIMIT 1")
    suspend fun getSchoolProfileSync(): SchoolProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(profile: SchoolProfileEntity)
}
