package com.xiaoyuezhu.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Student(
    val id: String,
    val name: String,
    val classId: String
)
