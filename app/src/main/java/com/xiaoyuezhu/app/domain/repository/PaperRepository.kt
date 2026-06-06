package com.xiaoyuezhu.app.domain.repository

import com.xiaoyuezhu.app.domain.model.Paper
import kotlinx.coroutines.flow.Flow

interface PaperRepository {
    fun getAllPapers(): Flow<List<Paper>>
    suspend fun getPaperById(id: String): Paper?
    suspend fun savePaper(paper: Paper): String
    suspend fun deletePaper(id: String)
    suspend fun getPaperCount(): Int
}
