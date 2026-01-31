package com.mitte.listree.ui.models

data class ListItem(
    override val id: String,
    override val name: String,
    val isChecked: Boolean = false,
    val isHeader: Boolean = false,
    override val order: Int = 0,
    override val lastModified: Long = System.currentTimeMillis(),
    override val deleted: Boolean = false
) : ListContent