package com.mitte.shopper.ui.models

data class ShoppingItem(
    val id: Int,
    val name: String,
    var isChecked: Boolean = false,
    var isHeader: Boolean = false
)
