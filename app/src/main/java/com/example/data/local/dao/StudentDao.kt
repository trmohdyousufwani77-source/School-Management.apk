package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.ArchivedStudentEntity
import com.example.data.local.entities.StudentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {
    @Query("SELECT * FROM students WHERE status != 'ARCHIVED' ORDER BY className ASC, section ASC, rollNumber ASC")
    fun getAllActiveStudents(): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE status != 'ARCHIVED' ORDER BY className ASC, section ASC, rollNumber ASC")
    suspend fun getAllActiveStudentsSync(): List<StudentEntity>

    @Query("SELECT * FROM students WHERE className = :className AND section = :section AND status != 'ARCHIVED' ORDER BY rollNumber ASC")
    fun getStudentsByClassAndSection(className: Int, section: String): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE className = :className AND section = :section AND status != 'ARCHIVED' ORDER BY rollNumber ASC")
    suspend fun getStudentsByClassAndSectionSync(className: Int, section: String): List<StudentEntity>

    @Query("SELECT * FROM students WHERE uuid = :uuid LIMIT 1")
    suspend fun getStudentByUuid(uuid: String): StudentEntity?

    @Query("""
        SELECT * FROM students 
        WHERE status != 'ARCHIVED' 
        AND (nameEn LIKE '%' || :query || '%' 
             OR nameUr LIKE '%' || :query || '%' 
             OR admissionNumber LIKE '%' || :query || '%' 
             OR CAST(rollNumber AS TEXT) LIKE '%' || :query || '%')
        ORDER BY className ASC, rollNumber ASC
    """)
    fun searchStudents(query: String): Flow<List<StudentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: StudentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudents(students: List<StudentEntity>)

    @Update
    suspend fun updateStudent(student: StudentEntity)

    @Delete
    suspend fun deleteStudent(student: StudentEntity)

    @Query("SELECT COUNT(*) FROM students WHERE status != 'ARCHIVED'")
    suspend fun getActiveStudentCount(): Int

    // Archiving
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArchivedStudents(archived: List<ArchivedStudentEntity>)

    @Query("SELECT * FROM archived_students ORDER BY archivedTimestamp DESC")
    fun getAllArchivedStudents(): Flow<List<ArchivedStudentEntity>>

    @Query("DELETE FROM students WHERE className = :className AND academicSession = :session")
    suspend fun purgeStudentsByClassAndSession(className: Int, session: String): Int
}
