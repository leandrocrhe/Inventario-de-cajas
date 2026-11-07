package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.BoxDao
import com.example.data.local.dao.CategoryDao
import com.example.data.local.dao.ItemDao
import com.example.data.local.dao.SearchHistoryDao
import com.example.data.local.dao.ZoneDao
import com.example.data.local.entity.BoxEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.ItemEntity
import com.example.data.local.entity.SearchHistoryEntity
import com.example.data.local.entity.ZoneEntity

@Database(
    entities = [
        BoxEntity::class,
        ItemEntity::class,
        CategoryEntity::class,
        SearchHistoryEntity::class,
        ZoneEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun boxDao(): BoxDao
    abstract fun itemDao(): ItemDao
    abstract fun categoryDao(): CategoryDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun zoneDao(): ZoneDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cajas_inventory.db"
                )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
