package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.BoxEntity
import kotlinx.coroutines.flow.Flow

data class BoxWithCounts(
    @Embedded val box: BoxEntity,
    val childBoxesCount: Int,
    val itemsCount: Int
)

@Dao
interface BoxDao {

    @Query("""
        SELECT b.*, 
            (SELECT COUNT(*) FROM boxes sub WHERE sub.parent_box_id = b.id) AS childBoxesCount,
            (SELECT COUNT(*) FROM items it WHERE it.box_id = b.id) AS itemsCount
        FROM boxes b 
        WHERE b.parent_box_id IS NULL 
        ORDER BY b.sort_order ASC, b.name ASC
    """)
    fun getRootBoxesWithCounts(): Flow<List<BoxWithCounts>>

    @Query("""
        SELECT b.*, 
            (SELECT COUNT(*) FROM boxes sub WHERE sub.parent_box_id = b.id) AS childBoxesCount,
            (SELECT COUNT(*) FROM items it WHERE it.box_id = b.id) AS itemsCount
        FROM boxes b 
        WHERE b.parent_box_id = :parentId 
        ORDER BY b.sort_order ASC, b.name ASC
    """)
    fun getChildBoxesWithCounts(parentId: String): Flow<List<BoxWithCounts>>

    @Query("SELECT * FROM boxes WHERE id = :id LIMIT 1")
    suspend fun getBoxById(id: String): BoxEntity?

    @Query("SELECT * FROM boxes WHERE id = :id LIMIT 1")
    fun getBoxFlowById(id: String): Flow<BoxEntity?>

    @Query("SELECT * FROM boxes ORDER BY name ASC")
    fun getAllBoxes(): Flow<List<BoxEntity>>

    @Query("SELECT * FROM boxes")
    suspend fun getAllBoxesSync(): List<BoxEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBox(box: BoxEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBoxes(boxes: List<BoxEntity>)

    @Update
    suspend fun updateBox(box: BoxEntity)

    @Delete
    suspend fun deleteBox(box: BoxEntity)

    @Query("DELETE FROM boxes WHERE id = :boxId")
    suspend fun deleteBoxById(boxId: String)

    @Query("SELECT COUNT(*) FROM boxes")
    fun getTotalBoxesCount(): Flow<Int>

    @Query("""
        SELECT DISTINCT zone_name FROM (
            SELECT name AS zone_name FROM zones WHERE TRIM(name) != ''
            UNION
            SELECT zone AS zone_name FROM boxes WHERE zone IS NOT NULL AND TRIM(zone) != ''
        ) ORDER BY zone_name ASC
    """)
    fun getAllZones(): Flow<List<String>>

    @Query("UPDATE boxes SET zone = :newName WHERE zone = :oldName")
    suspend fun renameZoneInBoxes(oldName: String, newName: String)

    @Query("UPDATE boxes SET zone = NULL WHERE zone = :name")
    suspend fun clearZoneInBoxes(name: String)

    @Query("SELECT * FROM boxes WHERE name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR zone LIKE '%' || :query || '%'")
    fun searchBoxes(query: String): Flow<List<BoxEntity>>
}
