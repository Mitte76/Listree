package com.mitte.shopper.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "shopping_items",
    foreignKeys = [
        ForeignKey(
            entity = ShoppingList::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ShoppingItem(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val listId: String,
    val name: String,
    var isChecked: Boolean = false,
    var isHeader: Boolean = false,
    val lastModified: Long = System.currentTimeMillis(),
    val order: Int = 0
)
