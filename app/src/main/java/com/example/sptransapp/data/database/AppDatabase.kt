package com.example.sptransapp.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [LineEntity::class, StopEntity::class, CorridorEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun lineDao(): LineDao

    abstract fun stopDao(): StopDao

    abstract fun corridorDao(): CorridorDao
}
