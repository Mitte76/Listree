package com.mitte.listree.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import com.google.gson.Gson
import com.mitte.listree.ui.theme.LisTreeColors

class ThemePersistence(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveTheme(themeName: String, colors: LisTreeColors) {
        val json = gson.toJson(colors)
        sharedPreferences.edit().putString("theme_$themeName", json).apply()
    }

    fun loadTheme(themeName: String): LisTreeColors? {
        val json = sharedPreferences.getString("theme_$themeName", null)
        return gson.fromJson(json, LisTreeColors::class.java)
    }

    fun saveColor(themeName: String, colorName: String, color: Color) {
        val theme = loadTheme(themeName) ?: return
        val newTheme = when (colorName) {
            "topAppBarContainer" -> theme.copy(topAppBarContainer = color)
            "topAppBarTitle" -> theme.copy(topAppBarTitle = color)
            "listMetaCount" -> theme.copy(listMetaCount = color)
            "groupCardContainer" -> theme.copy(groupCardContainer = color)
            "groupCardContent" -> theme.copy(groupCardContent = color)
            "singleCardContainer" -> theme.copy(singleCardContainer = color)
            "singleCardContent" -> theme.copy(singleCardContent = color)
            "itemContainer" -> theme.copy(itemContainer = color)
            "itemContent" -> theme.copy(itemContent = color)
            "listItemContainer" -> theme.copy(listItemContainer = color)
            "sectionContainer" -> theme.copy(sectionContainer = color)
            "sectionContent" -> theme.copy(sectionContent = color)
            "strikethrough" -> theme.copy(strikethrough = color)
            "deleteAction" -> theme.copy(deleteAction = color)
            "deletedCardContainer" -> theme.copy(deletedCardContainer = color)
            "deletedCardContent" -> theme.copy(deletedCardContent = color)
            "checkedItemContent" -> theme.copy(checkedItemContent = color)
            "undoRowContent" -> theme.copy(undoRowContent = color)
            "undoRowBackground" -> theme.copy(undoRowBackground = color)
            else -> theme
        }
        saveTheme(themeName, newTheme)
    }
}