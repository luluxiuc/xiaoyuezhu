package com.xiaoyuezhu.app.data.db.dao

import androidx.room.*
import com.xiaoyuezhu.app.data.db.entity.PaperEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaperDao {
    @Query("SELECT * FROM papers ORDER BY created_at DESC")
    fun getAllPapers(): Flow<List<PaperEntity>>

    @Query("SELECT * FROM papers WHERE id = :id")
    suspend fun getPaperById(id: String): PaperEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(paper: PaperEntity)

    @Update
    suspend fun update(paper: PaperEntity)

    @Query("DELETE FROM papers WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM papers")
    suspend fun getCount(): Int
}
