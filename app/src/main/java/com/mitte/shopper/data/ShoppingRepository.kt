package com.mitte.shopper.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class ShoppingRepository(context: Context) {

    private val shoppingDao = ShoppingDatabase.getDatabase(context).shoppingDao()

    fun getShoppingLists(): Flow<List<ShoppingList>> {
        return shoppingDao.getShoppingLists()
    }

    suspend fun saveShoppingLists(lists: List<ShoppingList>) {
        shoppingDao.deleteAllShoppingItems()
        shoppingDao.deleteAllShoppingLists()
        lists.forEach {
            saveListRecursively(it, null)
        }
    }

    private suspend fun saveListRecursively(list: ShoppingList, parentId: String?) {
        shoppingDao.insertShoppingList(list.copy(parentId = parentId))
    }

    fun getShoppingItems(listId: String): Flow<List<ShoppingItem>> {
        return shoppingDao.getShoppingItems(listId)
    }

    suspend fun saveShoppingItem(item: ShoppingItem) {
        shoppingDao.insertShoppingItem(item)
    }

    suspend fun deleteShoppingItem(item: ShoppingItem) {
        shoppingDao.deleteShoppingItem(item)
    }
}
