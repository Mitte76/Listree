package com.mitte.shopper.ui.models

data class ShoppingItem(
    val id: String,
    val name: String,
    val isChecked: Boolean = false,
    val isHeader: Boolean = false
)
