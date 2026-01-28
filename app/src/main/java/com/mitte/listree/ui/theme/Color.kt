package com.mitte.listree.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class LisTreeColors(
    val topAppBarContainer: Color,
    val topAppBarTitle: Color,
    val listMetaCount: Color,
    val groupCardContainer: Color,
    val groupCardContent: Color,
    val singleCardContainer: Color,
    val singleCardContent: Color,
    val itemContainer: Color,
    val itemContent: Color,
    val listItemContainer: Color,
    val sectionContainer: Color,
    val sectionContent: Color,
    val strikethrough: Color,
    val deleteAction: Color,
    val deletedCardContainer: Color
)

val LightLisTreeColors = LisTreeColors(
    topAppBarContainer = Color(0xFF4A6A8A),
    topAppBarTitle = Color(0xFFFFFFFF),
    listMetaCount = Color(0xFF616161),
    groupCardContainer = Color(0xFFBBBBBB),
    groupCardContent = Color(0xFF212121),
    singleCardContainer = Color(0xFFDDDDDD),
    singleCardContent = Color(0xFF212121),
    itemContainer = Color(0xFFEEEEEE),
    itemContent = Color(0xFF212121),
    listItemContainer = Color(0xFFEEEEEE),
    sectionContainer = Color(0xFF212121),
    sectionContent = Color(0xFF212121),
    strikethrough = Color(0xFFAA5A64),
    deleteAction = Color(0xFFAA5A64),
    deletedCardContainer = Color(0xFFC0C0C0)
)

val DarkLisTreeColors = LisTreeColors(
    topAppBarContainer = Color(0xFF3A4A5D),
    topAppBarTitle = Color(0xFFDCE4EC),
    listMetaCount = Color(0xFFB0C0D0),
    groupCardContainer = Color(0xFF282828),
    groupCardContent = Color(0xFFDCE4EC),
    singleCardContainer = Color(0xFF3B3B3B),
    singleCardContent = Color(0xFFDCE4EC),
    itemContainer = Color(0xFF525252),
    itemContent = Color(0xFFDCE4EC),
    listItemContainer = Color(0xFF525252),
    sectionContainer = Color(0xFF462801),
    sectionContent = Color(0xFFEEEEEE),
    strikethrough = Color(0xFFF44336),
    deleteAction = Color(0xFFF44336),
    deletedCardContainer = Color(0xFF424242)
)
