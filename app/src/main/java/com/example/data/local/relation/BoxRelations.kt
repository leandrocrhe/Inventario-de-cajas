package com.example.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.data.local.entity.BoxEntity
import com.example.data.local.entity.ItemEntity

/**
 * 1-to-many relationship: A parent Box containing nested child Boxes.
 */
data class BoxWithChildren(
    @Embedded
    val box: BoxEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "parent_box_id"
    )
    val childBoxes: List<BoxEntity>
)

/**
 * 1-to-many relationship: A Box containing objects/items.
 */
data class BoxWithItems(
    @Embedded
    val box: BoxEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "box_id"
    )
    val items: List<ItemEntity>
)

/**
 * Complete hierarchical container representation:
 * A Box with both its nested child Boxes and its contained Items.
 */
data class BoxHierarchy(
    @Embedded
    val box: BoxEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "parent_box_id"
    )
    val childBoxes: List<BoxEntity>,

    @Relation(
        parentColumn = "id",
        entityColumn = "box_id"
    )
    val items: List<ItemEntity>
)

/**
 * An Item with its containing parent Box.
 */
data class ItemWithBox(
    @Embedded
    val item: ItemEntity,

    @Relation(
        parentColumn = "box_id",
        entityColumn = "id"
    )
    val box: BoxEntity
)
