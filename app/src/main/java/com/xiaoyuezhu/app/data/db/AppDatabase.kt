package com.xiaoyuezhu.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.xiaoyuezhu.app.data.db.dao.*
import com.xiaoyuezhu.app.data.db.entity.*

@Database(
    entities = [
        PaperEntity::class,
        ClassEntity::class,
        StudentEntity::class,
        ExamEntity::class,
        GradeEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun paperDao(): PaperDao
    abstract fun classDao(): ClassDao
    abstract fun studentDao(): StudentDao
    abstract fun examDao(): ExamDao
    abstract fun gradeDao(): GradeDao
}
