package com.mitte.shopper.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class ListType {
    ITEM_LIST,
    GROUP_LIST
}

@Entity(tableName = "shopping_lists")
data class ShoppingList(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val ownerId: String,
    val name: String,
    val type: ListType? = ListType.ITEM_LIST,
    val parentId: String? = null,
    val lastModified: Long = System.currentTimeMillis()
)
