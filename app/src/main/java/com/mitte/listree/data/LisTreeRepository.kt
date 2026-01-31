package com.mitte.listree.data

import android.content.Context
import android.util.Log
import com.mitte.listree.ui.models.ListType
import com.mitte.listree.ui.models.ListItem as UiListItem
import com.mitte.listree.ui.models.TreeList as UiShoppingList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

class LisTreeRepository(context: Context) {

    private val shoppingDao = LisTreeDatabase.getDatabase(context).lisTreeDao()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getShoppingLists(showDeleted: Boolean): Flow<List<ShoppingListWithItems>> {
        val listsFlow = if (showDeleted) {
            shoppingDao.getShoppingListsWithDeleted()
        } else {
            shoppingDao.getShoppingLists()
        }

        return listsFlow.flatMapLatest { lists ->
            if (lists.isEmpty()) {
                return@flatMapLatest flowOf(emptyList())
            }

            val itemFlows = lists.map { list ->
                if (showDeleted) {
                    shoppingDao.getShoppingItemsWithDeleted(list.id)
                } else {
                    shoppingDao.getShoppingItems(list.id)
                }
            }

            combine(itemFlows) { itemsArrays ->
                lists.mapIndexed { index, list ->
                    ShoppingListWithItems(list, itemsArrays[index])
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

        list.children.forEach { child ->
            when (child) {
                is UiShoppingList -> {
                    saveListRecursively(child, list.id)
                }
                is UiListItem -> {
                    if (list.type == ListType.GROUP_LIST) {
                        Log.w("LisTreeRepository", "Attempted to save an item directly to a group list. This is not allowed. Item: ${child.name}, Group: ${list.name}")
                    } else {
                        val dataItem = LisTreeItem(
                            id = child.id,
                            listId = list.id,
                            name = child.name,
                            isChecked = child.isChecked,
                            isHeader = child.isHeader,
                            lastModified = child.lastModified,
                            order = child.order,
                            deleted = child.deleted
                        )
                        shoppingDao.upsertShoppingItem(dataItem)
                    }
                }
            }
        }
    }

    suspend fun permanentlyClearDeleted() {
        shoppingDao.deleteMarkedItems()
        shoppingDao.deleteMarkedLists()
    }
}
