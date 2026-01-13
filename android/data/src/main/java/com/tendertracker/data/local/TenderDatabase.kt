package com.tendertracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.tendertracker.data.local.dao.ChatMessageDao
import com.tendertracker.data.local.dao.SyncMetadataDao
import com.tendertracker.data.local.dao.TenderDao
import com.tendertracker.data.local.entity.ChatMessageEntity
import com.tendertracker.data.local.entity.SyncMetadataEntity
import com.tendertracker.data.local.entity.TenderEntity

@Database(
    entities = [
        TenderEntity::class,
        ChatMessageEntity::class,
        SyncMetadataEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class TenderDatabase : RoomDatabase() {
    abstract fun tenderDao(): TenderDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun syncMetadataDao(): SyncMetadataDao

    companion object {
        const val DATABASE_NAME = "tender_tracker_db"
    }
}
