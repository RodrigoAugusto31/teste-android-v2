package com.example.sptransapp.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CorridorDao {
    @Query("SELECT * FROM corridors")
    fun getAllCorridors(): Flow<List<CorridorEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(corridors: List<CorridorEntity>)
}
