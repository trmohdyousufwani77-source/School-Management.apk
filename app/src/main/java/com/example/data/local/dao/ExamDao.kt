package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entities.ExamEntity
import com.example.data.local.entities.StudentGradeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamDao {
    @Query("SELECT * FROM exams WHERE sessionId = :sessionId AND className = :className ORDER BY examDate ASC")
    fun getExamsForClass(sessionId: String, className: Int): Flow<List<ExamEntity>>

    @Query("SELECT * FROM exams WHERE sessionId = :sessionId AND className = :className ORDER BY examDate ASC")
    suspend fun getExamsForClassSync(sessionId: String, className: Int): List<ExamEntity>

    @Query("SELECT * FROM exams WHERE sessionId = :sessionId AND className = :className AND term = :term ORDER BY id ASC")
    suspend fun getExamsForClassAndTermSync(sessionId: String, className: Int, term: String): List<ExamEntity>

    @Query("SELECT * FROM exams ORDER BY id ASC")
    fun getAllExams(): Flow<List<ExamEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: ExamEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExams(exams: List<ExamEntity>): List<Long>

    @Query("SELECT * FROM student_grades WHERE studentId = :studentId AND sessionId = :sessionId")
    fun getGradesForStudent(studentId: String, sessionId: String): Flow<List<StudentGradeEntity>>

    @Query("SELECT * FROM student_grades WHERE studentId = :studentId AND sessionId = :sessionId")
    suspend fun getGradesForStudentSync(studentId: String, sessionId: String): List<StudentGradeEntity>

    @Query("SELECT * FROM student_grades WHERE examId = :examId")
    suspend fun getGradesForExam(examId: Long): List<StudentGradeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGrade(grade: StudentGradeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGrades(grades: List<StudentGradeEntity>)
}
