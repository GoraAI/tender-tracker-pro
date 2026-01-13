package com.tendertracker.app.di

import android.content.Context
import androidx.room.Room
import com.tendertracker.data.local.TenderDatabase
import com.tendertracker.data.local.dao.ChatMessageDao
import com.tendertracker.data.local.dao.SyncMetadataDao
import com.tendertracker.data.local.dao.TenderDao
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
    fun provideDatabase(
        @ApplicationContext context: Context
    ): TenderDatabase {
        return Room.databaseBuilder(
            context,
            TenderDatabase::class.java,
            TenderDatabase.DATABASE_NAME
        ).build()
    }

    @Provides
    fun provideTenderDao(database: TenderDatabase): TenderDao {
        return database.tenderDao()
    }

    @Provides
    fun provideChatMessageDao(database: TenderDatabase): ChatMessageDao {
        return database.chatMessageDao()
    }

    @Provides
    fun provideSyncMetadataDao(database: TenderDatabase): SyncMetadataDao {
        return database.syncMetadataDao()
    }
}
