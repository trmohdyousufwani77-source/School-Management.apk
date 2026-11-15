package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.TextbookAllocationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TextbookDao {
    @Query("SELECT * FROM textbook_allocations ORDER BY className ASC, subject ASC, studentName ASC")
    fun getAllAllocations(): Flow<List<TextbookAllocationEntity>>

    @Query("SELECT * FROM textbook_allocations ORDER BY returnDueDate ASC")
    suspend fun getAllAllocationsSync(): List<TextbookAllocationEntity>

    @Query("SELECT * FROM textbook_allocations WHERE className = :className ORDER BY subject ASC, studentName ASC")
    fun getAllocationsForClass(className: Int): Flow<List<TextbookAllocationEntity>>

    @Query("SELECT * FROM textbook_allocations WHERE className = :className AND section = :section ORDER BY subject ASC, studentName ASC")
    fun getAllocationsForClassAndSection(className: Int, section: String): Flow<List<TextbookAllocationEntity>>

    @Query("SELECT * FROM textbook_allocations WHERE studentId = :studentId ORDER BY subject ASC")
    fun getAllocationsForStudent(studentId: String): Flow<List<TextbookAllocationEntity>>

    @Query("SELECT * FROM textbook_allocations WHERE id = :id LIMIT 1")
    suspend fun getAllocationById(id: Long): TextbookAllocationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllocation(allocation: TextbookAllocationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllocations(allocations: List<TextbookAllocationEntity>)

    @Update
    suspend fun updateAllocation(allocation: TextbookAllocationEntity)

    @Query("UPDATE textbook_allocations SET status = :status WHERE id = :id")
    suspend fun updateAllocationStatus(id: Long, status: String)

    @Query(
        "UPDATE textbook_allocations SET status = :status, bookCondition = :condition, returnDueDate = :returnDueDate, returnedDate = :returnedDate, remarks = :remarks WHERE id = :id"
    )
    suspend fun updateInventoryDetails(
        id: Long,
        status: String,
        condition: String,
        returnDueDate: String,
        returnedDate: String,
        remarks: String
    )

    @Query("SELECT COUNT(*) FROM textbook_allocations WHERE status = 'RECEIVED'")
    suspend fun getDistributedCount(): Int

    @Query("SELECT COUNT(*) FROM textbook_allocations WHERE status != 'RECEIVED'")
    suspend fun getPendingCount(): Int
}
