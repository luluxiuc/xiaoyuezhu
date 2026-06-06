package com.xiaoyuezhu.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.xiaoyuezhu.app.domain.model.Student

@Entity(
    tableName = "students",
    primaryKeys = ["id", "class_id"],
    foreignKeys = [
        ForeignKey(
            entity = ClassEntity::class,
            parentColumns = ["id"],
            childColumns = ["class_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("class_id")]
)
data class StudentEntity(
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "class_id") val classId: String
) {
    fun toDomain(): Student = Student(id = id, name = name, classId = classId)

    companion object {
        fun fromDomain(student: Student): StudentEntity = StudentEntity(
            id = student.id,
            name = student.name,
            classId = student.classId
        )
    }
}
