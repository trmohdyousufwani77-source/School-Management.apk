package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entities.AttendanceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance WHERE date = :date")
    fun getAttendanceByDate(date: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE date = :date")
    suspend fun getAttendanceByDateSync(date: String): List<AttendanceEntity>

    @Query("SELECT * FROM attendance WHERE studentId = :studentId ORDER BY date DESC")
    fun getAttendanceForStudent(studentId: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE studentId = :studentId ORDER BY date ASC")
    suspend fun getAttendanceForStudentSync(studentId: String): List<AttendanceEntity>

    @Query("SELECT * FROM attendance WHERE studentId = :studentId ORDER BY date DESC LIMIT :limit")
    fun getRecentAttendanceForStudent(studentId: String, limit: Int = 30): Flow<List<AttendanceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAttendance(records: List<AttendanceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSingleAttendance(record: AttendanceEntity)

    @Query("DELETE FROM attendance WHERE date = :date AND studentId IN (:studentIds)")
    suspend fun deleteAttendanceForDateAndStudents(date: String, studentIds: List<String>): Int

    @Query("SELECT COUNT(*) FROM attendance")
    suspend fun getTotalAttendanceRecordsCount(): Int

    @Query("SELECT * FROM attendance ORDER BY date DESC")
    suspend fun getAllAttendanceSync(): List<AttendanceEntity>

    @Query("SELECT * FROM attendance ORDER BY date ASC")
    fun getAllAttendanceFlow(): Flow<List<AttendanceEntity>>
}
