package com.xiaoyuezhu.app.data.db.dao

import androidx.room.*
import com.xiaoyuezhu.app.data.db.entity.ClassEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassDao {
    @Query("SELECT * FROM classes ORDER BY created_at DESC")
    fun getAllClasses(): Flow<List<ClassEntity>>

    @Query("SELECT * FROM classes WHERE id = :id")
    suspend fun getClassById(id: String): ClassEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cls: ClassEntity)

    @Update
    suspend fun update(cls: ClassEntity)

    @Query("DELETE FROM classes WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM classes")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM students WHERE class_id = :classId")
    suspend fun getStudentCount(classId: String): Int
}
