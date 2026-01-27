package com.mitte.listree.ui.models

enum class ListType {
    ITEM_LIST,
    GROUP_LIST
}

data class ShoppingList(
    val id: String,
    val name: String,
    val type: ListType? = ListType.ITEM_LIST,
    val items: List<ShoppingItem>? = emptyList(),
    val subLists: List<ShoppingList>? = emptyList(),
    val isExpanded: Boolean = false,
    val parentId: String? = null,
    val order: Int = 0
)
