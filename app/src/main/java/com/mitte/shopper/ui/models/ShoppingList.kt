package com.mitte.shopper.ui.models

data class ShoppingList(
    val id: Int,
    val name: String,
    val items: List<ShoppingItem>
)