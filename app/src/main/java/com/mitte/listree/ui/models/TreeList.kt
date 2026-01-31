package com.mitte.listree.ui.models

enum class ListType {
    ITEM_LIST,
    GROUP_LIST
}

data class TreeList(
    override val id: String,
    override val name: String,
    val type: ListType? = ListType.ITEM_LIST,
    val children: List<ListContent> = emptyList(),
    val isExpanded: Boolean = false,
    val parentId: String? = null,
    override val order: Int = 0,
    override val lastModified: Long = System.currentTimeMillis(),
    override val deleted: Boolean = false
) : ListContent
