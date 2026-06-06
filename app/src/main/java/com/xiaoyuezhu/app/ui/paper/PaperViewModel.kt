package com.xiaoyuezhu.app.ui.paper

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoyuezhu.app.domain.model.Paper
import com.xiaoyuezhu.app.domain.repository.PaperRepository
import com.xiaoyuezhu.app.engine.PaperGenerator
import com.xiaoyuezhu.app.engine.PaperSpec
import com.xiaoyuezhu.app.engine.GenerateResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

data class PaperEditorState(
    val paperId: String = "",
    val title: String = "",
    val questionCount: Int = 20,
    val questionCountText: String = "20",
    val optionCount: Int = 4,
    val totalScore: Double = 100.0,
    val correctAnswers: Map<Int, List<String>> = emptyMap(),
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val generateResult: GenerateResult? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class PaperViewModel @Inject constructor(
    private val paperRepository: PaperRepository,
    private val paperGenerator: PaperGenerator,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(PaperEditorState())
    val state: StateFlow<PaperEditorState> = _state.asStateFlow()

    fun loadPaper(paperId: String) {
        if (paperId == "new") {
            _state.update { it.copy(paperId = "new") }
            return
        }
        viewModelScope.launch {
            val paper = paperRepository.getPaperById(paperId)
            if (paper != null) {
                try {
                    // Parse saved correct answers JSON
                    val answerItems = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                        .decodeFromString<List<com.xiaoyuezhu.app.domain.model.AnswerItemJson>>(paper.correctAnswerJson)
                    val answerMap = answerItems.associate { it.questionIndex to it.selectedOptions }
                    val savedCount = answerItems.size

                    // Parse template JSON to get optionCount
                    val template = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                        .decodeFromString<com.xiaoyuezhu.app.domain.model.TemplateJson>(paper.templateJson)
                    val savedOptionCount = template.optionCount

                    _state.update {
                        it.copy(
                            paperId = paper.id,
                            title = paper.title,
                            questionCount = savedCount,
                            questionCountText = savedCount.toString(),
                            optionCount = savedOptionCount,
                            correctAnswers = answerMap,
                            generateResult = GenerateResult(paper.templateJson, paper.correctAnswerJson, paper.imagePath)
                        )
                    }
                    Timber.i("答题卡已加载: title=${paper.title}, questions=$savedCount, options=$savedOptionCount")
                } catch (e: Exception) {
                    // Fallback: just load basic info
                    _state.update { it.copy(paperId = paper.id, title = paper.title) }
                    Timber.e(e, "恢复答题卡数据失败")
                }
            }
        }
    }

    fun updateTitle(title: String) { _state.update { it.copy(title = title) } }

    fun updateQuestionCount(count: Int) {
        _state.update {
            it.copy(
                questionCount = count,
                questionCountText = count.toString()
            )
        }
    }

    fun updateQuestionCountText(text: String) {
        val count = text.toIntOrNull()
        if (count != null) {
            val clamped = count.coerceIn(1, 200)
            _state.update {
                it.copy(
                    questionCountText = text,
                    questionCount = clamped
                )
            }
        } else {
            // Allow empty field or partial input (user is still typing)
            _state.update { it.copy(questionCountText = text) }
        }
    }

    fun updateOptionCount(count: Int) { _state.update { it.copy(optionCount = count) } }

    fun toggleAnswer(questionIndex: Int, option: String) {
        _state.update { state ->
            val current = state.correctAnswers[questionIndex]?.toMutableList() ?: mutableListOf()
            if (current.contains(option)) current.remove(option)
            else current.add(option)
            state.copy(correctAnswers = state.correctAnswers + (questionIndex to current))
        }
    }

    fun savePaper() {
        val st = _state.value
        if (st.title.isBlank()) {
            _state.update { it.copy(errorMessage = "请输入答题卡名称") }
            return
        }

        _state.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val spec = PaperSpec(
                    title = st.title,
                    questionCount = st.questionCount,
                    optionCount = st.optionCount,
                    totalScore = st.totalScore
                )

                val correctAnswers = (0 until st.questionCount).map { i ->
                    st.correctAnswers[i] ?: emptyList()
                }

                val result = withContext(Dispatchers.IO) {
                    paperGenerator.generate(context, spec, correctAnswers)
                }

                val paper = Paper(
                    id = if (st.paperId == "new") java.util.UUID.randomUUID().toString() else st.paperId,
                    title = st.title,
                    correctAnswerJson = result.correctAnswerJson,
                    templateJson = result.templateJson,
                    imagePath = result.imagePath
                )

                paperRepository.savePaper(paper)
                _state.update {
                    it.copy(
                        isSaving = false,
                        isSaved = true,
                        generateResult = result,
                        paperId = paper.id
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "保存答题卡失败")
                _state.update {
                    it.copy(isSaving = false, errorMessage = "保存失败: ${e.message}")
                }
            }
        }
    }

    fun clearError() { _state.update { it.copy(errorMessage = null) } }
}
