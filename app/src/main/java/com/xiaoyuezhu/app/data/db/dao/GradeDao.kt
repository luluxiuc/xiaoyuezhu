package com.xiaoyuezhu.app.data.db.dao

import androidx.room.*
import com.xiaoyuezhu.app.data.db.entity.GradeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GradeDao {
    @Query("SELECT * FROM grades WHERE exam_id = :examId ORDER BY student_id ASC")
    fun getGradesByExam(examId: String): Flow<List<GradeEntity>>

    @Query("SELECT * FROM grades WHERE exam_id = :examId ORDER BY student_id ASC")
    suspend fun getGradesByExamOnce(examId: String): List<GradeEntity>

    @Query("SELECT * FROM grades WHERE exam_id = :examId AND student_id = :studentId LIMIT 1")
    suspend fun findByExamAndStudent(examId: String, studentId: String): GradeEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(grade: GradeEntity)

    @Update
    suspend fun update(grade: GradeEntity)

    @Query("DELETE FROM grades WHERE exam_id = :examId AND student_id = :studentId")
    suspend fun deleteByExamAndStudent(examId: String, studentId: String)

    @Query("SELECT COUNT(*) FROM grades WHERE exam_id = :examId")
    suspend fun getCountByExam(examId: String): Int

    @Query("SELECT * FROM grades WHERE exam_id NOT IN (SELECT id FROM exams)")
    suspend fun findOrphans(): List<GradeEntity>

    @Delete
    suspend fun deleteAll(grades: List<GradeEntity>)
}
