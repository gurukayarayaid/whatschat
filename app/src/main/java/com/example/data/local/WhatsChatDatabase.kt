package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * WhatsChatDatabase:
 * High-performance local SQLite database via Room.
 * Uses Write-Ahead Logging (WAL) for concurrent read/write throughput and seamless offline caching.
 */
@Database(
    entities = [ConversationEntity::class, MessageEntity::class, ContactEntity::class],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class WhatsChatDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun contactDao(): ContactDao

    companion object {
        @Volatile
        private var INSTANCE: WhatsChatDatabase? = null

        fun getInstance(context: Context): WhatsChatDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WhatsChatDatabase::class.java,
                    "whatschat_database.db"
                )
                    .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

