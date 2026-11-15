package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entities.StudentVoiceMemoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VoiceMemoDao {
    @Query("SELECT * FROM student_voice_memos ORDER BY recordedTimestamp DESC")
    fun getAllVoiceMemos(): Flow<List<StudentVoiceMemoEntity>>

    @Query("SELECT * FROM student_voice_memos WHERE studentUuid = :studentUuid ORDER BY recordedTimestamp DESC")
    fun getVoiceMemosForStudent(studentUuid: String): Flow<List<StudentVoiceMemoEntity>>

    @Query("SELECT * FROM student_voice_memos WHERE studentUuid = :studentUuid ORDER BY recordedTimestamp DESC")
    suspend fun getVoiceMemosForStudentSync(studentUuid: String): List<StudentVoiceMemoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoiceMemo(memo: StudentVoiceMemoEntity): Long

    @Query("DELETE FROM student_voice_memos WHERE id = :memoId")
    suspend fun deleteVoiceMemoById(memoId: Long)
}
