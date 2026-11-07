package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "boxes",
    foreignKeys = [
        ForeignKey(
            entity = BoxEntity::class,
            parentColumns = ["id"],
            childColumns = ["parent_box_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("parent_box_id"),
        Index("name"),
        Index("zone")
    ]
)
data class BoxEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String? = null,
    @ColumnInfo(name = "parent_box_id")
    val parentBoxId: String? = null,
    @ColumnInfo(name = "primary_photo_uri")
    val primaryPhotoUri: String? = null,
    @ColumnInfo(name = "color_hex")
    val colorHex: String = "#F59E0B",
    @ColumnInfo(name = "zone")
    val zone: String? = null,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
