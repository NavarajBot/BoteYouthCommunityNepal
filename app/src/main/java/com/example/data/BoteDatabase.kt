package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        Article::class,
        Resource::class,
        FaqItem::class,
        Registration::class,
        ForumPost::class,
        BoteUser::class,
        ScholarshipOpportunity::class,
        AppUpdate::class,
        BoteDonation::class
    ],
    version = 6,
    exportSchema = false
)
abstract class BoteDatabase : RoomDatabase() {
    abstract fun boteDao(): BoteDao

    companion object {
        @Volatile
        private var INSTANCE: BoteDatabase? = null

        fun getDatabase(context: Context): BoteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BoteDatabase::class.java,
                    "bote_community_database"
                )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
