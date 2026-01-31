package com.mitte.listree.ui.models

sealed interface ListContent {
    val id: String
    val name: String
    val order: Int
    val lastModified: Long
    val deleted: Boolean
}
