package com.xiaoyuezhu.app.data.db.dao

import androidx.room.*
import com.xiaoyuezhu.app.data.db.entity.ExamEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamDao {
    @Query("SELECT * FROM exams WHERE class_id = :classId ORDER BY created_at DESC")
    fun getExamsByClass(classId: String): Flow<List<ExamEntity>>

    @Query("SELECT * FROM exams WHERE id = :id")
    suspend fun getExamById(id: String): ExamEntity?

    @Query("SELECT * FROM exams WHERE class_id = :classId AND paper_id = :paperId LIMIT 1")
    suspend fun findByClassAndPaper(classId: String, paperId: String): ExamEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(exam: ExamEntity)

    @Update
    suspend fun update(exam: ExamEntity)

    @Query("DELETE FROM exams WHERE id = :id")
    suspend fun deleteById(id: String)
}
