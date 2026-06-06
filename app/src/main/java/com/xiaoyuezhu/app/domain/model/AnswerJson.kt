package com.xiaoyuezhu.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class AnswerItemJson(
    val questionIndex: Int,
    val selectedOptions: List<String> = emptyList()
)

@Serializable
data class StudentAnswerJson(
    val studentId: String = "",
    val answers: List<AnswerItemJson> = emptyList()
)
