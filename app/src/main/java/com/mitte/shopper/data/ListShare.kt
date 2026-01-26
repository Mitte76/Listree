package com.mitte.shopper.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "list_shares",
    primaryKeys = ["listId", "userId"],
    indices = [Index(value = ["userId"])],
    foreignKeys = [
        ForeignKey(
            entity = ShoppingList::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ListShare(
    val listId: String,
    val userId: String
)
