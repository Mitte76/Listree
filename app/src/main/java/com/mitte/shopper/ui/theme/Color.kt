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
    val groupCardContent: Color
)

val LightShopperColors = ShopperColors(
    topAppBarContainer = Color(0xFF006A66),
    topAppBarTitle = Color(0xFFFFFFFF),
    listMetaCount = Color(0xFF4A6361),
    groupCardContainer = Color(0xFFF0F0F0), // Solid Neutral Grey
    groupCardContent = Color(0xFF3F4948)
)

val DarkShopperColors = ShopperColors(
    topAppBarContainer = Color(0xFF4CDAD4),
    topAppBarTitle = Color(0xFF003735),
    listMetaCount = Color(0xFFB0CCC9),
    groupCardContainer = Color(0xFF303030), // Solid Neutral Dark Grey
    groupCardContent = Color(0xFFBEC9C7)
)
