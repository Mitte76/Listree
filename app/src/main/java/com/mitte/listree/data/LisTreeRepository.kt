package com.mitte.listree.data

import android.content.Context
import com.mitte.listree.ui.models.TreeList as UiShoppingList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

class LisTreeRepository(context: Context) {

    private val shoppingDao = LisTreeDatabase.getDatabase(context).lisTreeDao()

    fun getShoppingLists(showDeleted: Boolean): Flow<List<ShoppingListWithItems>> {
        return shoppingDao.getShoppingLists().flatMapLatest { lists ->
            val itemFlows = lists.map { list ->
                if (showDeleted) {
                    shoppingDao.getShoppingItemsWithDeleted(list.id)
                } else {
                    shoppingDao.getShoppingItems(list.id)
                }
            }

            // When there are no item flows, emit the lists with empty items directly.
            if (itemFlows.isEmpty()) {
                flowOf(lists.map { ShoppingListWithItems(it, emptyList()) })
            } else {
                // Combine the flows of items for all lists into a single flow of a list of items.
                combine(itemFlows) { itemsArray ->
                    lists.map { list ->
                        // Find the corresponding items for the current list.
                        // This assumes the order of lists and itemsArray is consistent.
                        val listItems = itemsArray.firstOrNull { it.isNotEmpty() && it.first().listId == list.id } ?: emptyList()
                        ShoppingListWithItems(list, listItems)
                    }
                }
            }
        }
    }


    suspend fun saveShoppingLists(lists: List<UiShoppingList>) {
        lists.forEach { list ->
            saveListRecursively(list, null)
        }
    }

    private suspend fun saveListRecursively(list: UiShoppingList, parentId: String?) {
        // TODO: The ownerId is hardcoded here. This should be replaced with the actual owner's ID.
        val dataList = LisTreeList(
            id = list.id,
            ownerId = "ownerId",
            name = list.name,
            type = list.type?.let { ListType.valueOf(it.name) },
            parentId = parentId,
            lastModified = list.lastModified,
            order = list.order,
            deleted = list.deleted
        )
        shoppingDao.upsertShoppingList(dataList)
        list.items?.forEach { item ->
            val dataItem = LisTreeItem(
                id = item.id,
                listId = list.id,
                name = item.name,
                isChecked = item.isChecked,
                isHeader = item.isHeader,
                lastModified = item.lastModified,
                order = item.order,
                deleted = item.deleted
            )
            shoppingDao.upsertShoppingItem(dataItem)
        }
        list.subLists?.forEach { subList ->
            saveListRecursively(subList, list.id)
        }
    }

    suspend fun removeItem(itemId: String) {
        shoppingDao.removeItem(itemId, System.currentTimeMillis())
    }
}
