package com.mitte.listree.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mitte.listree.ui.theme.LisTreeColors
import androidx.core.content.edit
import kotlin.reflect.full.memberProperties

class ThemePersistence(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveTheme(themeName: String, colors: LisTreeColors) {
        val json = gson.toJson(colors)
        sharedPreferences.edit { putString("theme_$themeName", json) }
    }

    fun loadTheme(themeName: String, defaultColors: LisTreeColors): LisTreeColors {
        val json = sharedPreferences.getString("theme_$themeName", null)

        val mergedColors = if (json != null) {
            try {
                val type = object : TypeToken<Map<String, Any>>() {}.type
                val defaultColorsMap = gson.fromJson<Map<String, Any>>(gson.toJson(defaultColors), type)
                val savedColorsMap = gson.fromJson<Map<String, Any>>(json, type)

                val mergedMap = defaultColorsMap.toMutableMap()
                mergedMap.putAll(savedColorsMap)

                val finalMap = mergedMap.filterKeys { key ->
                    LisTreeColors::class.memberProperties.any { it.name == key }
                }

                gson.fromJson(gson.toJson(finalMap), LisTreeColors::class.java)
            } catch (_: Exception) {
                defaultColors
            }
        } else {
            defaultColors
        }

        saveTheme(themeName, mergedColors)
        return mergedColors
    }

    fun saveColor(themeName: String, colorName: String, color: Color, defaultColors: LisTreeColors) {
        val theme = loadTheme(themeName, defaultColors)
        val newTheme = when (colorName) {
            "topAppBarContainer" -> theme.copy(topAppBarContainer = color)
            "topAppBarTitle" -> theme.copy(topAppBarTitle = color)
            "listMetaCount" -> theme.copy(listMetaCount = color)
            "groupCardContainer" -> theme.copy(groupCardContainer = color)
            "groupCardContent" -> theme.copy(groupCardContent = color)
            "groupCardDeletedContainer" -> theme.copy(groupCardDeletedContainer = color)
            "groupCardDeletedContent" -> theme.copy(groupCardDeletedContent = color)
            "singleCardContainer" -> theme.copy(singleCardContainer = color)
            "singleCardContent" -> theme.copy(singleCardContent = color)
            "singleCardDeletedContainer" -> theme.copy(singleCardDeletedContainer = color)
            "singleCardDeletedContent" -> theme.copy(singleCardDeletedContent = color)
            "headerItemContainer" -> theme.copy(headerItemContainer = color)
            "headerItemContent" -> theme.copy(headerItemContent = color)
            "headerItemDeletedContainer" -> theme.copy(headerItemDeletedContainer = color)
            "headerItemDeletedContent" -> theme.copy(headerItemDeletedContent = color)
            "normalItemContainer" -> theme.copy(normalItemContainer = color)
            "normalItemContent" -> theme.copy(normalItemContent = color)
            "normalItemCheckedContainer" -> theme.copy(normalItemCheckedContainer = color)
            "normalItemCheckedContent" -> theme.copy(normalItemCheckedContent = color)
            "normalItemDeletedContainer" -> theme.copy(normalItemDeletedContainer = color)
            "normalItemDeletedContent" -> theme.copy(normalItemDeletedContent = color)
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
