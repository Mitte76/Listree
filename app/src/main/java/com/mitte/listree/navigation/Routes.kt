package com.mitte.listree.navigation

object Routes {
    const val LIST_ID = "{listId}"
    const val SHOPPING_LISTS = "shoppingLists"
    const val SHOPPING_ITEMS = "shoppingItems/$LIST_ID"
    const val SETTINGS = "settings"
    const val THEME_EDITOR = "themeEditor"
    const val COMPONENT_THEME_EDITOR = "componentThemeEditor/{componentName}"
    const val TODO = "todo"
    const val CALENDAR = "calendar"
}
