package com.mitte.shopper.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// Custom Semantic Colors
@Immutable
data class ShopperColors(
    val topAppBarContainer: Color,
    val topAppBarTitle: Color,
    val listMetaCount: Color,
    val groupCardContainer: Color,
    val groupCardContent: Color,
    val singleCardContainer: Color,
    val singleCardContent: Color,
    val itemContainer: Color,
    val itemContent: Color,
    val strikethrough: Color
)

// A discrete and calming slate blue/grey palette
val LightShopperColors = ShopperColors(
    topAppBarContainer = Color(0xFF4A6A8A),      // Deep, muted blue
    topAppBarTitle = Color(0xFFFFFFFF),          // White
    listMetaCount = Color(0xFF616161),           // Dark grey for good contrast
    groupCardContainer = Color(0xFFBBBBBB),      // A bit darker blue
    groupCardContent = Color(0xFF212121),         // High-contrast dark text
    singleCardContainer = Color(0xFFDDDDDD),     // Light blue background
    singleCardContent = Color(0xFF212121),         // High-contrast dark text
    itemContainer = Color(0xFFEEEEEE),           // Light grey
    itemContent = Color(0xFF212121),               // High-contrast dark text
    strikethrough = Color(0xFFAA5A64)            // Strikethrough color
)

val DarkShopperColors = ShopperColors(
    topAppBarContainer = Color(0xFF3A4A5D),      // Muted slate blue
    topAppBarTitle = Color(0xFFDCE4EC),          // Light, almost-white blue-grey
    listMetaCount = Color(0xFFB0C0D0),           // Desaturated, light blue-grey
    groupCardContainer = Color(0xFF282828),      // A distinct, lighter slate for groups
    groupCardContent = Color(0xFFDCE4EC),         // Light text color
    singleCardContainer = Color(0xFF3B3B3B),     // Main dark background
    singleCardContent = Color(0xFFDCE4EC),         // Light text color
    itemContainer = Color(0xFF525252),           // Slightly lighter cards for items
    itemContent = Color(0xFFDCE4EC),               // Light text color
    strikethrough = Color(0xFFF44336)            // Strikethrough color
)
