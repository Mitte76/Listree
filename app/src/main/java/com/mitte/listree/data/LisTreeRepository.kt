package com.mitte.listree.data

import android.content.Context
import com.mitte.listree.ui.models.TreeList as UiShoppingList
import kotlinx.coroutines.flow.Flow

class LisTreeRepository(context: Context) {

    private val shoppingDao = LisTreeDatabase.getDatabase(context).lisTreeDao()

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
        val dataList = LisTreeList(list.id, "ownerId", list.name, list.type?.let { ListType.valueOf(it.name) }, parentId, order = list.order)
        shoppingDao.upsertShoppingList(dataList)
        list.items?.forEach { item ->
            val dataItem = LisTreeItem(item.id, list.id, item.name, item.isChecked, item.isHeader, order = item.order)
            shoppingDao.upsertShoppingItem(dataItem)
        }
        list.subLists?.forEach { subList ->
            saveListRecursively(subList, list.id)
        }
    }
}
