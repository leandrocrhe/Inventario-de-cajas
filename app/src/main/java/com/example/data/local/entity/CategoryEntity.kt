package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    indices = [Index(value = ["name"], unique = true)]
)
data class CategoryEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    @ColumnInfo(name = "icon_name")
    val iconName: String = "category",
    @ColumnInfo(name = "color_hex")
    val colorHex: String = "#64748B"
)
