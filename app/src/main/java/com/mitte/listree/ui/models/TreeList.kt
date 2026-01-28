package com.mitte.listree.ui.models

enum class ListType {
    ITEM_LIST,
    GROUP_LIST
}

data class TreeList(
    val id: String,
    val name: String,
    val type: ListType? = ListType.ITEM_LIST,
    val items: List<ListItem>? = emptyList(),
    val subLists: List<TreeList>? = emptyList(),
    val isExpanded: Boolean = false,
    val parentId: String? = null,
    val order: Int = 0,
    val lastModified: Long = System.currentTimeMillis(),
    val deleted: Boolean = false
)
