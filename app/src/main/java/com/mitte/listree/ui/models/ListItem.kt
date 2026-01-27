package com.mitte.listree.ui.models

data class ListItem(
    val id: String,
    val name: String,
    val isChecked: Boolean = false,
    val isHeader: Boolean = false,
    val order: Int = 0
)
