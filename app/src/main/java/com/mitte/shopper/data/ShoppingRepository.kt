package com.mitte.shopper.data

import android.content.Context
import com.mitte.shopper.ui.models.ShoppingList as UiShoppingList
import kotlinx.coroutines.flow.Flow

class ShoppingRepository(context: Context) {

    private val shoppingDao = ShoppingDatabase.getDatabase(context).shoppingDao()

    fun getShoppingLists(): Flow<List<ShoppingListWithItems>> {
        return shoppingDao.getShoppingListsWithItems()
    }

    suspend fun saveShoppingLists(lists: List<UiShoppingList>) {
        lists.forEach { list ->
            saveListRecursively(list, null)
        }
    }

    private suspend fun saveListRecursively(list: UiShoppingList, parentId: String?) {
        // TODO: The ownerId is hardcoded here. This should be replaced with the actual owner's ID.
        val dataList = ShoppingList(list.id, "ownerId", list.name, list.type?.let { com.mitte.shopper.data.ListType.valueOf(it.name) }, parentId, order = list.order)
        shoppingDao.upsertShoppingList(dataList)
        list.items?.forEach { item ->
            val dataItem = ShoppingItem(item.id, list.id, item.name, item.isChecked, item.isHeader, order = item.order)
            shoppingDao.upsertShoppingItem(dataItem)
        }
        list.subLists?.forEach { subList ->
            saveListRecursively(subList, list.id)
        }
    }

    fun getShoppingItems(listId: String): Flow<List<ShoppingItem>> {
        return shoppingDao.getShoppingItems(listId)
    }

    suspend fun saveShoppingItem(item: ShoppingItem) {
        shoppingDao.upsertShoppingItem(item)
    }

    suspend fun deleteShoppingItem(item: ShoppingItem) {
        shoppingDao.deleteShoppingItem(item)
    }
}
