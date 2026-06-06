package com.xiaoyuezhu.app.di

import com.xiaoyuezhu.app.data.repository.*
import com.xiaoyuezhu.app.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPaperRepository(impl: PaperRepositoryImpl): PaperRepository

    @Binds
    @Singleton
    abstract fun bindClassRepository(impl: ClassRepositoryImpl): ClassRepository

    @Binds
    @Singleton
    abstract fun bindGradeRepository(impl: GradeRepositoryImpl): GradeRepository
}
