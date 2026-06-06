package com.xiaoyuezhu.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.xiaoyuezhu.app.domain.model.Class

@Entity(tableName = "classes")
data class ClassEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "created_at") val createdAt: Long
) {
    fun toDomain(): Class = Class(id = id, name = name, createdAt = createdAt)

    companion object {
        fun fromDomain(cls: Class): ClassEntity = ClassEntity(
            id = cls.id,
            name = cls.name,
            createdAt = cls.createdAt
        )
    }
}
