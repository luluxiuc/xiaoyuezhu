package com.xiaoyuezhu.app.data.repository

import com.xiaoyuezhu.app.data.db.dao.ClassDao
import com.xiaoyuezhu.app.data.db.dao.StudentDao
import com.xiaoyuezhu.app.data.db.entity.ClassEntity
import com.xiaoyuezhu.app.data.db.entity.StudentEntity
import com.xiaoyuezhu.app.domain.model.Class
import com.xiaoyuezhu.app.domain.model.Student
import com.xiaoyuezhu.app.domain.repository.ClassRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClassRepositoryImpl @Inject constructor(
    private val classDao: ClassDao,
    private val studentDao: StudentDao
) : ClassRepository {

    override fun getAllClasses(): Flow<List<Class>> =
        classDao.getAllClasses().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getClassById(id: String): Class? =
        classDao.getClassById(id)?.toDomain()

    override suspend fun saveClass(cls: Class): String {
        try {
            classDao.insert(ClassEntity.fromDomain(cls))
            Timber.i("班级已保存: ${cls.name}")
            return cls.id
        } catch (e: Exception) {
            Timber.e(e, "班级保存失败")
            throw e
        }
    }

    override suspend fun deleteClass(id: String) {
        try {
            classDao.deleteById(id)
            Timber.i("班级已删除: $id")
        } catch (e: Exception) {
            Timber.e(e, "班级删除失败")
            throw e
        }
    }

    override suspend fun getStudentsByClass(classId: String): List<Student> =
        studentDao.getStudentsByClassOnce(classId).map { it.toDomain() }

    override fun getStudentsByClassFlow(classId: String): Flow<List<Student>> =
        studentDao.getStudentsByClass(classId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun addStudents(classId: String, students: List<Student>) {
        val entities = students.map { StudentEntity.fromDomain(it) }
        studentDao.insertAll(entities)
        Timber.i("批量导入${students.size}名学生到班级$classId")
    }

    override suspend fun addStudent(classId: String, student: Student) {
        studentDao.insert(StudentEntity.fromDomain(student))
        Timber.i("添加学生: ${student.id} ${student.name}")
    }

    override suspend fun deleteStudent(classId: String, studentId: String) {
        studentDao.deleteStudent(classId, studentId)
        Timber.i("删除学生: $studentId")
    }

    override suspend fun getStudentCount(classId: String): Int =
        studentDao.getCountByClass(classId)

    override suspend fun getNextStudentId(classId: String): String {
        val count = studentDao.getCountByClass(classId)
        return String.format("%02d", count + 1)
    }
}
