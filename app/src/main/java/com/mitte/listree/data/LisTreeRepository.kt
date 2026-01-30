package com.mitte.listree.data

import android.content.Context
import kotlinx.coroutines.ExperimentalCoroutinesApi
import com.mitte.listree.ui.models.TreeList as UiShoppingList
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
}
