package com.xiaoyuezhu.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.xiaoyuezhu.app.domain.model.Paper

@Entity(tableName = "papers")
data class PaperEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "correct_answer_json") val correctAnswerJson: String,
    @ColumnInfo(name = "template_json") val templateJson: String,
    @ColumnInfo(name = "master_json") val masterJson: String,
    @ColumnInfo(name = "image_path") val imagePath: String,
    @ColumnInfo(name = "created_at") val createdAt: Long
) {
    fun toDomain(): Paper = Paper(
        id = id, title = title,
        correctAnswerJson = correctAnswerJson,
        templateJson = templateJson,
        masterJson = masterJson,
        imagePath = imagePath,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(paper: Paper): PaperEntity = PaperEntity(
            id = paper.id, title = paper.title,
            correctAnswerJson = paper.correctAnswerJson,
            templateJson = paper.templateJson,
            masterJson = paper.masterJson,
            imagePath = paper.imagePath,
            createdAt = paper.createdAt
        )
    }
}
