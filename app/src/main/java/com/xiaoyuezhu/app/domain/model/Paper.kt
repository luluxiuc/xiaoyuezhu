package com.xiaoyuezhu.app.domain.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Paper(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val correctAnswerJson: String = "[]",
    val templateJson: String = "{}",
    val masterJson: String = "",
    val imagePath: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
