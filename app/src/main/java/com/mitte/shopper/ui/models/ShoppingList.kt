package com.mitte.shopper.ui.models

enum class ListType {
    ITEM_LIST,
    GROUP_LIST
}

data class ShoppingList(
    val id: Int,
    val name: String,
    val type: ListType? = ListType.ITEM_LIST,
    val items: List<ShoppingItem>? = emptyList(),
    val subLists: List<ShoppingList>? = emptyList()
)