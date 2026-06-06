package com.xiaoyuezhu.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoyuezhu.app.domain.model.Class
import com.xiaoyuezhu.app.domain.model.Paper
import com.xiaoyuezhu.app.domain.repository.ClassRepository
import com.xiaoyuezhu.app.domain.repository.PaperRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class HomeUiState(
    val papers: List<Paper> = emptyList(),
    val classes: List<Class> = emptyList(),
    val paperCount: Int = 0,
    val classCount: Int = 0,
    val showCreateClassDialog: Boolean = false,
    val newClassName: String = "",
    val showDeletePaperDialog: Boolean = false,
    val selectedDeletePaperId: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val paperRepository: PaperRepository,
    private val classRepository: ClassRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                paperRepository.getAllPapers(),
                classRepository.getAllClasses()
            ) { papers, classes ->
                HomeUiState(
                    papers = papers,
                    classes = classes,
                    paperCount = papers.size,
                    classCount = classes.size
                )
            }.collect { base ->
                // Preserve dialog state when data refreshes
                _uiState.update { current ->
                    current.copy(
                        papers = base.papers,
                        classes = base.classes,
                        paperCount = base.paperCount,
                        classCount = base.classCount
                    )
                }
            }
        }
    }

    fun showCreateClassDialog() {
        _uiState.update { it.copy(showCreateClassDialog = true, newClassName = "") }
    }

    fun hideCreateClassDialog() {
        _uiState.update { it.copy(showCreateClassDialog = false) }
    }

    fun updateNewClassName(name: String) {
        _uiState.update { it.copy(newClassName = name) }
    }

    fun createClass() {
        val name = _uiState.value.newClassName.trim()
        if (name.isBlank()) return
        viewModelScope.launch {
            try {
                classRepository.saveClass(Class(name = name))
                Timber.i("创建班级: $name")
                _uiState.update { it.copy(showCreateClassDialog = false, newClassName = "") }
            } catch (e: Exception) {
                Timber.e(e, "创建班级失败")
            }
        }
    }

    // ── Delete paper ──

    fun showDeletePaperDialog() {
        _uiState.update { it.copy(showDeletePaperDialog = true, selectedDeletePaperId = null) }
    }

    fun hideDeletePaperDialog() {
        _uiState.update { it.copy(showDeletePaperDialog = false, selectedDeletePaperId = null) }
    }

    fun selectPaperToDelete(paperId: String) {
        _uiState.update { it.copy(selectedDeletePaperId = paperId) }
    }

    fun confirmDeletePaper() {
        val paperId = _uiState.value.selectedDeletePaperId ?: return
        viewModelScope.launch {
            try {
                paperRepository.deletePaper(paperId)
                Timber.i("答题卡已删除: $paperId")
            } catch (e: Exception) {
                Timber.e(e, "删除答题卡失败")
            }
        }
        _uiState.update { it.copy(showDeletePaperDialog = false, selectedDeletePaperId = null) }
    }
}
