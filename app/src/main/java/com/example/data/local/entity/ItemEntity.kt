package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "items",
    foreignKeys = [
        ForeignKey(
            entity = BoxEntity::class,
            parentColumns = ["id"],
            childColumns = ["box_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("box_id"),
        Index("category_id"),
        Index("name"),
        Index("is_loaned")
    ]
)
data class ItemEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String? = null,
    @ColumnInfo(name = "box_id")
    val boxId: String,
    @ColumnInfo(name = "category_id")
    val categoryId: String? = null,
    @ColumnInfo(name = "primary_photo_uri")
    val primaryPhotoUri: String? = null,
    @ColumnInfo(name = "quantity")
    val quantity: Int = 1,
    @ColumnInfo(name = "tags")
    val tags: String = "",
    @ColumnInfo(name = "is_loaned")
    val isLoaned: Boolean = false,
    @ColumnInfo(name = "loaned_to")
    val loanedTo: String? = null,
    @ColumnInfo(name = "loaned_at")
    val loanedAt: Long? = null,
    @ColumnInfo(name = "notes")
    val notes: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
