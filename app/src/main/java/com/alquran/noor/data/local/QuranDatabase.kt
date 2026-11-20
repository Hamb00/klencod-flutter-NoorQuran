package com.alquran.noor.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [SurahEntity::class, AyahEntity::class, BookmarkEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class QuranDatabase : RoomDatabase() {
    abstract fun dao(): QuranDao

    companion object {
        fun create(context: Context): QuranDatabase =
            Room.databaseBuilder(context, QuranDatabase::class.java, "quran.db").build()
    }
}
