package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.TeacherEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TeacherDao {
    @Query("SELECT * FROM teachers ORDER BY designation ASC, name ASC")
    fun getAllTeachers(): Flow<List<TeacherEntity>>

    @Query("SELECT * FROM teachers WHERE employeeId = :id LIMIT 1")
    suspend fun getTeacherById(id: String): TeacherEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeacher(teacher: TeacherEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeachers(teachers: List<TeacherEntity>)

    @Update
    suspend fun updateTeacher(teacher: TeacherEntity)
}
