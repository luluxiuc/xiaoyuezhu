package com.xiaoyuezhu.app.engine

data class PaperSpec(
    val title: String = "答题卡",
    val questionCount: Int = 20,
    val questionsPerRow: Int = 4,
    val studentIdDigits: Int = 2,
    /** Per-question option counts, indexed by question. Default: 4 for each. */
    val optionCounts: List<Int> = List(questionCount) { 4 },
    /** Per-question scores, indexed by question. Default: equal split of 100. */
    val questionScores: List<Double> = List(questionCount) { 100.0 / questionCount }
) {
    /** Total score derived from sum of per-question scores */
    val totalScore: Double get() = questionScores.sum()
}

data class GenerateResult(
    val templateJson: String,
    val correctAnswerJson: String,
    val imagePath: String
)
