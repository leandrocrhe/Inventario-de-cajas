package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "zones")
data class ZoneEntity(
    @PrimaryKey
    val name: String,
    @ColumnInfo(name = "color_hex")
    val colorHex: String = "#F59E0B",
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
