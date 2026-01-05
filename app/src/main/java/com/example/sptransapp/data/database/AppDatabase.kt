package com.example.sptransapp.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.sptransapp.data.database.dao.CorridorDao
import com.example.sptransapp.data.database.dao.LineDao
import com.example.sptransapp.data.database.dao.StopDao
import com.example.sptransapp.data.database.entity.CorridorEntity
import com.example.sptransapp.data.database.entity.LineEntity
import com.example.sptransapp.data.database.entity.StopEntity

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
