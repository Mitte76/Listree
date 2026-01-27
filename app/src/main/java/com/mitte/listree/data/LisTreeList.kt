package com.mitte.listree.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.util.UUID

enum class ListType {
    ITEM_LIST,
    GROUP_LIST
}

@Entity(tableName = "shopping_lists")
data class LisTreeList(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val ownerId: String,
    val name: String,
    val type: ListType? = ListType.ITEM_LIST,
    val parentId: String? = null,
    val lastModified: Long = System.currentTimeMillis(),
    val order: Int = 0
)

data class ShoppingListWithItems(
    @Embedded val lisTreeList: LisTreeList,
    @Relation(
        parentColumn = "id",
        entityColumn = "listId"
    )
    val items: List<LisTreeItem>
)
