package com.xiaoyuezhu.app.engine

data class PaperSpec(
    val title: String = "答题卡",
    val questionCount: Int = 20,
    val optionCount: Int = 4,
    val questionsPerRow: Int = 4,
    val totalScore: Double = 100.0,
    val studentIdDigits: Int = 2
)

data class GenerateResult(
    val templateJson: String,
    val correctAnswerJson: String,
    val imagePath: String
)
