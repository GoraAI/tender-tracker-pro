package com.tendertracker.app.di

import com.tendertracker.data.repository.AnalyticsRepositoryImpl
import com.tendertracker.data.repository.ApplicationRepositoryImpl
import com.tendertracker.data.repository.AuthRepositoryImpl
import com.tendertracker.data.repository.ChatRepositoryImpl
import com.tendertracker.data.repository.CriteriaRepositoryImpl
import com.tendertracker.data.repository.SettingsRepositoryImpl
import com.tendertracker.data.repository.TenderRepositoryImpl
import com.tendertracker.domain.repository.AnalyticsRepository
import com.tendertracker.domain.repository.ApplicationRepository
import com.tendertracker.domain.repository.AuthRepository
import com.tendertracker.domain.repository.ChatRepository
import com.tendertracker.domain.repository.CriteriaRepository
import com.tendertracker.domain.repository.SettingsRepository
import com.tendertracker.domain.repository.TenderRepository
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
    abstract fun bindTenderRepository(
        impl: TenderRepositoryImpl
    ): TenderRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(
        impl: ChatRepositoryImpl
    ): ChatRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        impl: SettingsRepositoryImpl
    ): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindAnalyticsRepository(
        impl: AnalyticsRepositoryImpl
    ): AnalyticsRepository

    @Binds
    @Singleton
    abstract fun bindCriteriaRepository(
        impl: CriteriaRepositoryImpl
    ): CriteriaRepository

    @Binds
    @Singleton
    abstract fun bindApplicationRepository(
        impl: ApplicationRepositoryImpl
    ): ApplicationRepository
}
