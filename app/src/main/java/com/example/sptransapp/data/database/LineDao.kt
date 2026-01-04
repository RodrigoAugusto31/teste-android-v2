package com.example.sptransapp.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LineDao {

    @Query("SELECT * FROM favorite_lines")
    fun getAllFavorites(): Flow<List<LineEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(line: LineEntity)

    @Delete
    suspend fun delete(line: LineEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_lines WHERE lineCode = :id LIMIT 1)")
    fun isFavorite(id: Int): Flow<Boolean>
}
