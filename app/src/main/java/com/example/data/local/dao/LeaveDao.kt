package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.LeaveApplicationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LeaveDao {
    @Query("SELECT * FROM leave_applications ORDER BY appliedTimestamp DESC")
    fun getAllLeaves(): Flow<List<LeaveApplicationEntity>>

    @Query("SELECT * FROM leave_applications ORDER BY appliedTimestamp DESC")
    suspend fun getAllLeavesSync(): List<LeaveApplicationEntity>

    @Query("SELECT * FROM leave_applications WHERE status = :status ORDER BY appliedTimestamp DESC")
    fun getLeavesByStatus(status: String): Flow<List<LeaveApplicationEntity>>

    @Query("SELECT * FROM leave_applications WHERE applicantRole IN (:roles) ORDER BY appliedTimestamp DESC")
    fun getLeavesByApplicantRoles(roles: List<String>): Flow<List<LeaveApplicationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLeave(leave: LeaveApplicationEntity): Long

    @Update
    suspend fun updateLeave(leave: LeaveApplicationEntity)

    @Query("SELECT * FROM leave_applications WHERE id = :id LIMIT 1")
    suspend fun getLeaveById(id: Long): LeaveApplicationEntity?
}
