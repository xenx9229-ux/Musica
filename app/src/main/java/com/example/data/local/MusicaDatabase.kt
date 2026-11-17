package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        DownloadedTrackEntity::class,
        LocalPlaylistEntity::class,
        LocalPlaylistTrackEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MusicaDatabase : RoomDatabase() {
    abstract fun musicaDao(): MusicaDao

    companion object {
        @Volatile
        private var INSTANCE: MusicaDatabase? = null

        fun getInstance(context: Context): MusicaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MusicaDatabase::class.java,
                    "musica_offline_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
