package com.xiaoyuezhu.app.data.db.dao

import androidx.room.*
import com.xiaoyuezhu.app.data.db.entity.StudentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {
    @Query("SELECT * FROM students WHERE class_id = :classId ORDER BY id ASC")
    fun getStudentsByClass(classId: String): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE class_id = :classId ORDER BY id ASC")
    suspend fun getStudentsByClassOnce(classId: String): List<StudentEntity>

    @Query("SELECT * FROM students WHERE class_id = :classId AND id = :studentId")
    suspend fun getStudent(classId: String, studentId: String): StudentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(students: List<StudentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(student: StudentEntity)

    @Update
    suspend fun update(student: StudentEntity)

    @Query("DELETE FROM students WHERE class_id = :classId AND id = :studentId")
    suspend fun deleteStudent(classId: String, studentId: String)

    @Query("DELETE FROM students WHERE class_id = :classId")
    suspend fun deleteAllByClass(classId: String)

    @Query("SELECT COUNT(*) FROM students WHERE class_id = :classId")
    suspend fun getCountByClass(classId: String): Int
}
