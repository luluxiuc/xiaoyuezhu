package com.xiaoyuezhu.app.di

import android.content.Context
import androidx.room.Room
import com.xiaoyuezhu.app.data.db.AppDatabase
import com.xiaoyuezhu.app.data.db.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "xiaoyuezhu.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides fun providePaperDao(db: AppDatabase): PaperDao = db.paperDao()
    @Provides fun provideClassDao(db: AppDatabase): ClassDao = db.classDao()
    @Provides fun provideStudentDao(db: AppDatabase): StudentDao = db.studentDao()
    @Provides fun provideExamDao(db: AppDatabase): ExamDao = db.examDao()
    @Provides fun provideGradeDao(db: AppDatabase): GradeDao = db.gradeDao()
}
