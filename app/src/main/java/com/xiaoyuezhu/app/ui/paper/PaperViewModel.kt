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
    /** Per-question option counts: question index → count (default 4) */
    val optionCounts: Map<Int, Int> = (0 until 20).associateWith { 4 },
    /** Per-question scores: question index → score (default 5.0 each for 100 total) */
    val questionScores: Map<Int, Double> = (0 until 20).associateWith { 5.0 },
    /** User-set correct answers: question index → list of selected options */
    val correctAnswers: Map<Int, List<String>> = emptyMap(),
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val generateResult: GenerateResult? = null,
    val errorMessage: String? = null
) {
    /** Calculated total score from sum of all question scores */
    val totalScore: Double
        get() = questionScores.values.sum()
}

@HiltViewModel
class PaperViewModel @Inject constructor(
    private val paperRepository: PaperRepository,
    private val paperGenerator: PaperGenerator,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(PaperEditorState())
    val state: StateFlow<PaperEditorState> = _state.asStateFlow()

    private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

    fun loadPaper(paperId: String) {
        if (paperId == "new") {
            _state.update { it.copy(paperId = "new") }
            return
        }
        viewModelScope.launch {
            val paper = paperRepository.getPaperById(paperId)
            if (paper != null) {
                try {
                    val answerItems = json.decodeFromString<List<com.xiaoyuezhu.app.domain.model.AnswerItemJson>>(paper.correctAnswerJson)
                    val answerMap = answerItems.associate { it.questionIndex to it.selectedOptions }
                    val savedCount = answerItems.size

                    val template = json.decodeFromString<com.xiaoyuezhu.app.domain.model.TemplateJson>(paper.templateJson)
                    val optionCounts = if (template.optionCounts.isNotEmpty()) {
                        template.optionCounts.mapIndexed { i, c -> i to c }.toMap()
                    } else {
                        (0 until savedCount).associateWith { template.optionCount }
                    }
                    val questionScores = if (template.questionScores.isNotEmpty()) {
                        template.questionScores.mapIndexed { i, s -> i to s }.toMap()
                    } else {
                        (0 until savedCount).associateWith { 100.0 / savedCount }
                    }

                    _state.update {
                        it.copy(
                            paperId = paper.id, title = paper.title,
                            questionCount = savedCount, questionCountText = savedCount.toString(),
                            optionCounts = optionCounts, questionScores = questionScores,
                            correctAnswers = answerMap,
                            generateResult = GenerateResult(paper.templateJson, paper.correctAnswerJson, paper.imagePath)
                        )
                    }
                    Timber.i("答题卡已加载: title=${paper.title}, questions=$savedCount")
                } catch (e: Exception) {
                    _state.update { it.copy(paperId = paper.id, title = paper.title) }
                    Timber.e(e, "恢复答题卡数据失败")
                }
            }
        }
    }

    fun updateTitle(title: String) { _state.update { it.copy(title = title) } }

    fun updateQuestionCount(count: Int) {
        val clamped = count.coerceIn(1, 200)
        _state.update { state ->
            // Preserve existing configs for existing questions, add defaults for new ones
            val newOptionCounts = (0 until clamped).associate { i ->
                i to (state.optionCounts[i] ?: 4)
            }
            val newScores = (0 until clamped).associate { i ->
                i to (state.questionScores[i] ?: 5.0)
            }
            state.copy(
                questionCount = clamped,
                questionCountText = clamped.toString(),
                optionCounts = newOptionCounts,
                questionScores = newScores
            )
        }
    }

    fun updateQuestionCountText(text: String) {
        val count = text.toIntOrNull()
        if (count != null) updateQuestionCount(count)
        else _state.update { it.copy(questionCountText = text) }
    }

    fun setQuestionOptionCount(questionIndex: Int, count: Int) {
        val clamped = count.coerceIn(2, 8)
        _state.update { state ->
            val newMap = state.optionCounts.toMutableMap()
            newMap[questionIndex] = clamped
            // Clear answers that reference now-removed options
            val labels = ('A'..'Z').toList()
            val validLabels = labels.take(clamped).map { it.toString() }.toSet()
            val updatedAnswers = state.correctAnswers.toMutableMap()
            val current = updatedAnswers[questionIndex]
            if (current != null) {
                updatedAnswers[questionIndex] = current.filter { it in validLabels }
            }
            state.copy(optionCounts = newMap, correctAnswers = updatedAnswers)
        }
    }

    fun setQuestionScore(questionIndex: Int, score: Double) {
        _state.update { state ->
            val newScores = state.questionScores.toMutableMap()
            newScores[questionIndex] = score.coerceAtLeast(0.0)
            state.copy(questionScores = newScores)
        }
    }

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
                    optionCounts = (0 until st.questionCount).map { st.optionCounts[it] ?: 4 },
                    questionScores = (0 until st.questionCount).map { st.questionScores[it] ?: 5.0 }
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
                    it.copy(isSaving = false, isSaved = true,
                        generateResult = result, paperId = paper.id)
                }
            } catch (e: Exception) {
                Timber.e(e, "保存答题卡失败")
                _state.update { it.copy(isSaving = false,
                    errorMessage = "保存失败: ${e.message}") }
            }
        }
    }

    fun clearError() { _state.update { it.copy(errorMessage = null) } }
}
