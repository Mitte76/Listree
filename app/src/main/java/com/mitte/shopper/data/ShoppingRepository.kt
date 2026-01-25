package com.mitte.shopper.data
import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mitte.shopper.ui.models.ShoppingItem
import com.mitte.shopper.ui.models.ShoppingList
import androidx.core.content.edit

private const val PREFS_NAME = "ShoppingListPrefs"
private const val KEY_SHOPPING_LISTS = "shopping_lists"

class ShoppingRepository(context: Context) {

    private val gson = Gson()
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveShoppingLists(lists: List<ShoppingList>) {
        val jsonString = gson.toJson(lists)
        prefs.edit { putString(KEY_SHOPPING_LISTS, jsonString) }
    }


    fun getShoppingLists(): List<ShoppingList> {
        val jsonString = prefs.getString(KEY_SHOPPING_LISTS, null)
        val lists = if (jsonString != null) {
            val type = object : TypeToken<List<ShoppingList>>() {}.type
            gson.fromJson(jsonString, type)
        } else {
            getInitialLists()
        }
        return migrateShoppingLists(lists)
    }

    private fun migrateShoppingLists(lists: List<ShoppingList>): List<ShoppingList> {
        val migratedLists = lists.map { list ->
            if (list.type == com.mitte.shopper.ui.models.ListType.GROUP_LIST && list.subLists != null) {
                list.copy(subLists = list.subLists.map { subList ->
                    if (subList.parentId == null) {
                        subList.copy(parentId = list.id)
                    } else {
                        subList
                    }
                })
            } else {
                list
            }
        }
        saveShoppingLists(migratedLists)
        return migratedLists
    }

    private fun getInitialLists(): List<ShoppingList> {
        return listOf(
            ShoppingList(
                id = 1,
                name = "Welcome",
                items = emptyList()
            ),
        )
    }
}