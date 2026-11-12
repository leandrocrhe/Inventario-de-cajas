package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.ItemEntity
import com.example.data.local.relation.ItemWithBox
import kotlinx.coroutines.flow.Flow

data class CategoryCount(
    val categoryId: String?,
    val count: Int
)

@Dao
interface ItemDao {

    @Query("SELECT * FROM items WHERE box_id = :boxId ORDER BY name ASC")
    fun getItemsInBox(boxId: String): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: String): ItemEntity?

    @Query("SELECT * FROM items WHERE id = :id LIMIT 1")
    fun getItemFlowById(id: String): Flow<ItemEntity?>

    @Query("SELECT * FROM items")
    suspend fun getAllItemsSync(): List<ItemEntity>

    // --- Relational Queries ---

    @Transaction
    @Query("SELECT * FROM items WHERE id = :itemId LIMIT 1")
    fun getItemWithBox(itemId: String): Flow<ItemWithBox?>

    @Transaction
    @Query("SELECT * FROM items ORDER BY name ASC")
    fun getAllItemsWithBox(): Flow<List<ItemWithBox>>

    @Query("SELECT * FROM items WHERE primary_photo_uri IS NOT NULL AND primary_photo_uri != ''")
    fun getAllItemsWithPhotos(): Flow<List<ItemEntity>>

    @Query("SELECT primary_photo_uri FROM items WHERE box_id = :boxId AND primary_photo_uri IS NOT NULL AND primary_photo_uri != ''")
    fun getItemPhotosInBox(boxId: String): Flow<List<String>>

    // --- Searches & Filters ---

    @Query("""
        SELECT * FROM items 
        WHERE (:query = '' OR name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%')
          AND (:categoryId IS NULL OR category_id = :categoryId)
        ORDER BY name ASC
    """)
    fun searchItems(query: String, categoryId: String? = null): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE category_id = :categoryId ORDER BY name ASC")
    fun getItemsByCategoryId(categoryId: String): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE is_loaned = 1 ORDER BY loaned_at DESC")
    fun getLoanedItems(): Flow<List<ItemEntity>>

    @Query("SELECT COUNT(*) FROM items")
    fun getTotalItemsCount(): Flow<Int>

    @Query("SELECT SUM(quantity) FROM items")
    fun getTotalItemUnitsCount(): Flow<Int?>

    @Query("SELECT COUNT(*) FROM items WHERE is_loaned = 1")
    fun getTotalLoanedCount(): Flow<Int>

    // --- Mutators ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ItemEntity>)

    @Update
    suspend fun updateItem(item: ItemEntity)

    @Delete
    suspend fun deleteItem(item: ItemEntity)

    @Query("DELETE FROM items WHERE id = :itemId")
    suspend fun deleteItemById(itemId: String)

    @Query("DELETE FROM items")
    suspend fun deleteAllItems()

    @Query("UPDATE items SET quantity = :quantity WHERE id = :itemId")
    suspend fun updateQuantity(itemId: String, quantity: Int)

    @Query("UPDATE items SET box_id = :targetBoxId WHERE id = :itemId")
    suspend fun moveItem(itemId: String, targetBoxId: String)

    @Query("UPDATE items SET is_loaned = 1, loaned_to = :loanedTo, loaned_at = :loanedAt, notes = :notes WHERE id = :itemId")
    suspend fun loanItem(itemId: String, loanedTo: String, loanedAt: Long, notes: String?)

    @Query("UPDATE items SET is_loaned = 0, loaned_to = NULL, loaned_at = NULL, notes = NULL WHERE id = :itemId")
    suspend fun returnLoanedItem(itemId: String)

    @Query("UPDATE items SET category_id = :newCategoryId WHERE category_id = :oldCategoryId")
    suspend fun reassignCategory(oldCategoryId: String, newCategoryId: String)

    @Query("SELECT COUNT(*) FROM items WHERE category_id = :categoryId")
    suspend fun getItemsCountByCategoryId(categoryId: String): Int
}
