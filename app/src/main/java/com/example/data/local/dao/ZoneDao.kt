package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.ZoneEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ZoneDao {
    @Query("SELECT * FROM zones ORDER BY name ASC")
    fun getAllZones(): Flow<List<ZoneEntity>>

    @Query("SELECT * FROM zones ORDER BY name ASC")
    suspend fun getAllZonesSync(): List<ZoneEntity>

    @Query("SELECT name FROM zones ORDER BY name ASC")
    fun getAllZoneNames(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertZone(zone: ZoneEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertZones(zones: List<ZoneEntity>)

    @Query("DELETE FROM zones WHERE name = :name")
    suspend fun deleteZone(name: String)

    @Query("UPDATE zones SET name = :newName WHERE name = :oldName")
    suspend fun renameZone(oldName: String, newName: String)

    @Query("DELETE FROM zones")
    suspend fun deleteAllZones()
}
