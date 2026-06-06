package com.xiaoyuezhu.app.data.repository

import com.xiaoyuezhu.app.core.log.AppLogger
import com.xiaoyuezhu.app.data.db.dao.*
import com.xiaoyuezhu.app.data.db.entity.*
import com.xiaoyuezhu.app.domain.model.*
import com.xiaoyuezhu.app.domain.repository.GradeRepository
import com.xiaoyuezhu.app.domain.repository.SaveGradeResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GradeRepositoryImpl @Inject constructor(
    private val examDao: ExamDao,
    private val gradeDao: GradeDao,
    private val classDao: ClassDao,
    private val paperDao: PaperDao,
    private val studentDao: StudentDao
) : GradeRepository {

    override suspend fun saveGrade(
        classId: String,
        paperId: String,
        paperTitle: String,
        studentId: String,
        score: Double,
        totalScore: Double,
        studentAnswerJson: String,
        correctCount: Int,
        wrongCount: Int,
        blankCount: Int
    ): SaveGradeResult {
        return try {
            // Step 1: Validate class exists
            val cls = classDao.getClassById(classId)
            if (cls == null) {
                Timber.e("班级不存在: $classId")
                return SaveGradeResult.Error("班级不存在")
            }

            // Step 2: Validate paper exists
            val paper = paperDao.getPaperById(paperId)
            if (paper == null) {
                Timber.e("答题卡不存在: $paperId")
                return SaveGradeResult.Error("答题卡不存在")
            }

            // Step 3: Validate student exists in class
            val student = studentDao.getStudent(classId, studentId)
            if (student == null) {
                Timber.w("学号$studentId 不存在于班级 ${cls.name}")
                return SaveGradeResult.Error("学号 $studentId 不存在于该班级")
            }

            // Step 4: Ensure Exam exists (auto-create on first grade)
            var exam = examDao.findByClassAndPaper(classId, paperId)
            if (exam == null) {
                val totalStudents = studentDao.getCountByClass(classId)
                exam = ExamEntity(
                    id = UUID.randomUUID().toString(),
                    classId = classId,
                    paperId = paperId,
                    paperTitle = paperTitle,
                    status = "SCANNING",
                    gradedCount = 0,
                    totalStudents = totalStudents,
                    createdAt = System.currentTimeMillis()
                )
                examDao.insert(exam)
                Timber.i("自动创建考试记录: classId=$classId, paperId=$paperId")
            }

            // Step 5: Check duplicate
            val existing = gradeDao.findByExamAndStudent(exam.id, studentId)
            if (existing != null) {
                Timber.w("学号$studentId 已有成绩记录，examId=${exam.id}")
                return SaveGradeResult.Conflict(existing.toDomain())
            }

            // Step 6: Write Grade
            val grade = GradeEntity(
                id = UUID.randomUUID().toString(),
                examId = exam.id,
                studentId = studentId,
                score = score,
                totalScore = totalScore,
                studentAnswerJson = studentAnswerJson,
                correctCount = correctCount,
                wrongCount = wrongCount,
                blankCount = blankCount,
                createdAt = System.currentTimeMillis()
            )
            gradeDao.insert(grade)

            // Step 7: Update exam progress
            val newGradedCount = gradeDao.getCountByExam(exam.id)
            val newStatus = if (newGradedCount >= exam.totalStudents) "COMPLETED" else "SCANNING"
            examDao.update(exam.copy(gradedCount = newGradedCount, status = newStatus))

            Timber.i("成绩已保存: examId=${exam.id}, studentId=$studentId, score=$score/$totalScore")
            SaveGradeResult.Success(grade.toDomain())

        } catch (e: Exception) {
            Timber.e(e, "成绩保存失败: classId=$classId, studentId=$studentId")
            SaveGradeResult.Error(e.message ?: "未知错误")
        }
    }

    override fun getExamsByClass(classId: String): Flow<List<Exam>> =
        examDao.getExamsByClass(classId).map { entities -> entities.map { it.toDomain() } }

    override fun getGradesByExam(examId: String): Flow<List<Grade>> =
        gradeDao.getGradesByExam(examId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun getExamById(examId: String): Exam? =
        examDao.getExamById(examId)?.toDomain()

    override suspend fun checkIntegrity() {
        try {
            val orphans = gradeDao.findOrphans()
            if (orphans.isNotEmpty()) {
                Timber.w("发现${orphans.size}条孤儿成绩记录，已自动清理")
                gradeDao.deleteAll(orphans)
            }
        } catch (e: Exception) {
            Timber.e(e, "数据完整性检查失败")
        }
    }
}
