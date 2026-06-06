package com.xiaoyuezhu.app.ui.classui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoyuezhu.app.domain.model.*
import com.xiaoyuezhu.app.domain.repository.ClassRepository
import com.xiaoyuezhu.app.domain.repository.GradeRepository
import com.xiaoyuezhu.app.domain.repository.PaperRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ClassListUiState(
    val classes: List<Class> = emptyList(),
    val showCreateDialog: Boolean = false,
    val newClassName: String = "",
    val papers: List<Paper> = emptyList(),
    val selectedClassId: String? = null,
    val selectedPaperId: String? = null,
    val showSelectDialog: Boolean = false
)

@HiltViewModel
class ClassViewModel @Inject constructor(
    private val classRepository: ClassRepository,
    private val paperRepository: PaperRepository,
    private val gradeRepository: GradeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ClassListUiState())
    val uiState: StateFlow<ClassListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            classRepository.getAllClasses().collect { classes ->
                _uiState.update { it.copy(classes = classes) }
            }
        }
        viewModelScope.launch {
            paperRepository.getAllPapers().collect { papers ->
                _uiState.update { it.copy(papers = papers) }
            }
        }
    }

    fun showCreateDialog() = _uiState.update { it.copy(showCreateDialog = true, newClassName = "") }

    fun hideCreateDialog() = _uiState.update { it.copy(showCreateDialog = false) }

    fun updateNewClassName(name: String) = _uiState.update { it.copy(newClassName = name) }

    fun createClass() {
        val name = _uiState.value.newClassName.trim()
        if (name.isBlank()) return
        viewModelScope.launch {
            classRepository.saveClass(Class(name = name))
            _uiState.update { it.copy(showCreateDialog = false, newClassName = "") }
        }
    }

    fun deleteClass(id: String) {
        viewModelScope.launch { classRepository.deleteClass(id) }
    }

    fun showSelectDialog() {
        _uiState.update { it.copy(showSelectDialog = true) }
    }

    fun hideSelectDialog() {
        _uiState.update { it.copy(showSelectDialog = false, selectedClassId = null, selectedPaperId = null) }
    }

    fun selectClass(id: String) = _uiState.update { it.copy(selectedClassId = id) }
    fun selectPaper(id: String) = _uiState.update { it.copy(selectedPaperId = id) }
}

// ClassDetailState
data class ClassDetailUiState(
    val className: String = "",
    val students: List<Student> = emptyList(),
    val exams: List<Exam> = emptyList(),
    val showAddStudent: Boolean = false,
    val newStudentNames: String = "",
    val editingStudentId: String? = null,
    val editingStudentName: String = ""
)

@HiltViewModel
class ClassDetailViewModel @Inject constructor(
    private val classRepository: ClassRepository,
    private val gradeRepository: GradeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ClassDetailUiState())
    val uiState: StateFlow<ClassDetailUiState> = _uiState.asStateFlow()

    private var classId: String = ""

    fun loadClass(id: String) {
        classId = id
        viewModelScope.launch {
            val cls = classRepository.getClassById(id)
            _uiState.update { it.copy(className = cls?.name ?: "") }
        }
        viewModelScope.launch {
            classRepository.getStudentsByClassFlow(id).collect { students ->
                _uiState.update { it.copy(students = students) }
            }
        }
        viewModelScope.launch {
            gradeRepository.getExamsByClass(id).collect { exams ->
                _uiState.update { it.copy(exams = exams) }
            }
        }
    }

    fun showAddStudent() = _uiState.update { it.copy(showAddStudent = true, newStudentNames = "") }

    fun hideAddStudent() = _uiState.update { it.copy(showAddStudent = false) }

    fun updateNewStudentNames(names: String) = _uiState.update { it.copy(newStudentNames = names) }

    fun addStudents() {
        val names = _uiState.value.newStudentNames
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (names.isEmpty()) return

        viewModelScope.launch {
            val existingCount = classRepository.getStudentCount(classId)
            val students = names.mapIndexed { index, name ->
                val studentId = String.format("%02d", existingCount + index + 1)
                Student(id = studentId, name = name, classId = classId)
            }
            classRepository.addStudents(classId, students)
            _uiState.update { it.copy(showAddStudent = false, newStudentNames = "") }
        }
    }

    fun startEditStudent(student: Student) {
        _uiState.update { it.copy(editingStudentId = student.id, editingStudentName = student.name) }
    }

    fun updateEditingStudentName(name: String) {
        _uiState.update { it.copy(editingStudentName = name) }
    }

    fun saveEditStudent() {
        val id = _uiState.value.editingStudentId ?: return
        val name = _uiState.value.editingStudentName.trim()
        if (name.isBlank()) return
        viewModelScope.launch {
            classRepository.addStudent(classId, Student(id = id, name = name, classId = classId))
            _uiState.update { it.copy(editingStudentId = null, editingStudentName = "") }
        }
    }

    fun cancelEdit() = _uiState.update { it.copy(editingStudentId = null, editingStudentName = "") }

    fun deleteStudent(studentId: String) {
        viewModelScope.launch { classRepository.deleteStudent(classId, studentId) }
    }
}
