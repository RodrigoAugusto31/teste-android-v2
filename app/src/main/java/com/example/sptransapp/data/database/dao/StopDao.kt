package com.example.sptransapp.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.sptransapp.data.database.entity.StopEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StopDao {
    @Query("SELECT * FROM stops WHERE lineRelationCode = :lineCode")
    fun getStopsByLine(lineCode: Int): Flow<List<StopEntity>>

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertAll(stops: List<StopEntity>)
}
