package com.xiaoyuezhu.app.domain.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Class(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
