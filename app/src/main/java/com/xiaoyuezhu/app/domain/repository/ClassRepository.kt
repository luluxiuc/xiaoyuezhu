package com.xiaoyuezhu.app.domain.repository

import com.xiaoyuezhu.app.domain.model.Class
import com.xiaoyuezhu.app.domain.model.Student
import kotlinx.coroutines.flow.Flow

interface ClassRepository {
    fun getAllClasses(): Flow<List<Class>>
    suspend fun getClassById(id: String): Class?
    suspend fun saveClass(cls: Class): String
    suspend fun deleteClass(id: String)

    suspend fun getStudentsByClass(classId: String): List<Student>
    fun getStudentsByClassFlow(classId: String): Flow<List<Student>>
    suspend fun addStudents(classId: String, students: List<Student>)
    suspend fun addStudent(classId: String, student: Student)
    suspend fun deleteStudent(classId: String, studentId: String)
    suspend fun getStudentCount(classId: String): Int
    suspend fun getNextStudentId(classId: String): String
}
