package com.xiaoyuezhu.app.data.repository

import com.xiaoyuezhu.app.core.log.AppLogger
import com.xiaoyuezhu.app.data.db.dao.PaperDao
import com.xiaoyuezhu.app.data.db.entity.PaperEntity
import com.xiaoyuezhu.app.domain.model.Paper
import com.xiaoyuezhu.app.domain.repository.PaperRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaperRepositoryImpl @Inject constructor(
    private val paperDao: PaperDao
) : PaperRepository {

    override fun getAllPapers(): Flow<List<Paper>> =
        paperDao.getAllPapers().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getPaperById(id: String): Paper? =
        paperDao.getPaperById(id)?.toDomain()

    override suspend fun savePaper(paper: Paper): String {
        try {
            paperDao.insert(PaperEntity.fromDomain(paper))
            Timber.i("答题卡已保存: ${paper.title}")
            return paper.id
        } catch (e: Exception) {
            Timber.e(e, "答题卡保存失败")
            throw e
        }
    }

    override suspend fun deletePaper(id: String) {
        try {
            val paper = paperDao.getPaperById(id)
            // Delete image file
            if (paper != null) {
                val imageFile = java.io.File(paper.imagePath)
                if (imageFile.exists()) imageFile.delete()
            }
            paperDao.deleteById(id)
            Timber.i("答题卡已删除: $id")
        } catch (e: Exception) {
            Timber.e(e, "答题卡删除失败")
            throw e
        }
    }

    override suspend fun getPaperCount(): Int = paperDao.getCount()
}
