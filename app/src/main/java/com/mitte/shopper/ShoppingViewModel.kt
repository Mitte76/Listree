package com.mitte.shopper

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mitte.shopper.data.ShoppingRepository
import com.mitte.shopper.ui.models.ListType
import com.mitte.shopper.ui.models.ShoppingItem
import com.mitte.shopper.ui.models.ShoppingList
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(FlowPreview::class)
class ShoppingViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ShoppingRepository(application)

    private val _shoppingLists = MutableStateFlow<List<ShoppingList>>(emptyList())
    val shoppingLists: StateFlow<List<ShoppingList>> = _shoppingLists.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getShoppingLists().collect { dataLists ->
                _shoppingLists.value = dataLists.map { dataList ->
                    ShoppingList(
                        id = dataList.id,
                        name = dataList.name,
                        type = dataList.type?.let { type -> ListType.valueOf(type.name) },
                        parentId = dataList.parentId
                    )
                }
            }
        }

        _shoppingLists
            .debounce(300)
            .distinctUntilChanged()
            .onEach { uiLists ->
                val dataLists = uiLists.map { uiList ->
                    com.mitte.shopper.data.ShoppingList(
                        id = uiList.id,
                        ownerId = "user1",
                        name = uiList.name,
                        type = uiList.type?.let { type -> com.mitte.shopper.data.ListType.valueOf(type.name) },
                        parentId = uiList.parentId
                    )
                }
                repository.saveShoppingLists(dataLists)
            }
            .launchIn(viewModelScope)
    }

    private fun getAllLists(lists: List<ShoppingList>): List<ShoppingList> {
        return lists + lists.flatMap { getAllLists(it.subLists ?: emptyList()) }
    }

    fun getListById(listId: String): ShoppingList? {
        return getAllLists(shoppingLists.value).firstOrNull { it.id == listId }
    }

    fun toggleExpanded(listId: String) {
        _shoppingLists.update { currentLists ->
            fun mapList(list: List<ShoppingList>): List<ShoppingList> {
                return list.map { item ->
                    if (item.id == listId) {
                        item.copy(isExpanded = !item.isExpanded)
                    } else {
                        item.copy(subLists = item.subLists?.let { mapList(it) })
                    }
                }
            }
            mapList(currentLists)
        }
    }

    fun addList(listName: String) {
        _shoppingLists.update { currentLists ->
            val newList = ShoppingList(
                id = UUID.randomUUID().toString(),
                name = listName.trim(),
                type = ListType.ITEM_LIST,
                items = emptyList(),
                subLists = emptyList()
            )
            currentLists + newList
        }
    }

    fun addGroupList(listName: String) {
        _shoppingLists.update { currentLists ->
            val newList = ShoppingList(
                id = UUID.randomUUID().toString(),
                name = listName.trim(),
                type = ListType.GROUP_LIST,
                items = null,
                subLists = emptyList()
            )
            currentLists + newList
        }
    }

    private fun mapAndAddSubGroup(
        lists: List<ShoppingList>,
        parentGroupId: String,
        newSubGroup: ShoppingList
    ): List<ShoppingList> {
        return lists.map { list ->
            if (list.id == parentGroupId) {
                list.copy(subLists = (list.subLists ?: emptyList()) + newSubGroup)
            } else {
                list.copy(subLists = list.subLists?.let {
                    mapAndAddSubGroup(
                        it,
                        parentGroupId,
                        newSubGroup
                    )
                })
            }
        }
    }

    fun addSubGroup(parentGroupId: String, subGroupName: String) {
        _shoppingLists.update { currentLists ->
            val newSubGroup = ShoppingList(
                id = UUID.randomUUID().toString(),
                name = subGroupName.trim(),
                type = ListType.GROUP_LIST,
                items = null,
                subLists = emptyList(),
                parentId = parentGroupId
            )
            mapAndAddSubGroup(currentLists, parentGroupId, newSubGroup)
        }
    }

    private fun mapAndAddSubList(
        lists: List<ShoppingList>,
        parentGroupId: String,
        newList: ShoppingList
    ): List<ShoppingList> {
        return lists.map { list ->
            if (list.id == parentGroupId) {
                list.copy(subLists = (list.subLists ?: emptyList()) + newList)
            } else {
                list.copy(
                    subLists = list.subLists?.let {
                        mapAndAddSubList(
                            it,
                            parentGroupId,
                            newList
                        )
                    }
                )
            }
        }
    }

    fun addSubList(parentGroupId: String, subListName: String) {
        _shoppingLists.update { currentLists ->
            val newList = ShoppingList(
                id = UUID.randomUUID().toString(),
                name = subListName.trim(),
                type = ListType.ITEM_LIST,
                items = emptyList(),
                subLists = emptyList(),
                parentId = parentGroupId
            )
            mapAndAddSubList(currentLists, parentGroupId, newList)
        }
    }

    fun deleteList(listId: String) {
        _shoppingLists.update { currentLists ->
            fun removeRecursively(lists: List<ShoppingList>): List<ShoppingList> {
                val filteredList = lists.filterNot { it.id == listId }
                return filteredList.map { list ->
                    list.copy(subLists = list.subLists?.let { removeRecursively(it) })
                }
            }
            removeRecursively(currentLists)
        }
    }

    private fun mapAndEditListName(
        lists: List<ShoppingList>,
        listId: String,
        newName: String
    ): List<ShoppingList> {
        return lists.map { list ->
            if (list.id == listId) {
                list.copy(name = newName.trim())
            } else {
                list.copy(
                    subLists = list.subLists?.let {
                        mapAndEditListName(
                            it,
                            listId,
                            newName
                        )
                    }
                )
            }
        }
    }

    fun editListName(listId: String, newName: String) {
        _shoppingLists.update { currentLists ->
            mapAndEditListName(currentLists, listId, newName)
        }
    }

    fun moveList(from: Int, to: Int) {
        _shoppingLists.update { currentLists ->
            val mutableList = currentLists.toMutableList()
            mutableList.add(to, mutableList.removeAt(from))
            mutableList.toList()
        }
    }

    private fun mapAndMoveSubList(
        lists: List<ShoppingList>,
        parentListId: String,
        from: Int,
        to: Int
    ): List<ShoppingList> {
        return lists.map { list ->
            if (list.id == parentListId) {
                val mutableSubLists = list.subLists?.toMutableList()
                if (mutableSubLists != null) {
                    mutableSubLists.add(to, mutableSubLists.removeAt(from))
                    list.copy(subLists = mutableSubLists.toList())
                } else {
                    list
                }
            } else {
                list.copy(subLists = list.subLists?.let { mapAndMoveSubList(it, parentListId, from, to) })
            }
        }
    }

    fun moveSubList(parentListId: String, from: Int, to: Int) {
        _shoppingLists.update { currentLists ->
            mapAndMoveSubList(currentLists, parentListId, from, to)
        }
    }

    fun moveSubListToNewGroup(subListId: String, fromGroupId: String, toGroupId: String) {
        _shoppingLists.update { currentLists ->
            var subListToMove: ShoppingList? = null

            fun removeList(lists: List<ShoppingList>): List<ShoppingList> {
                return lists.map { list ->
                    if (list.id == fromGroupId) {
                        subListToMove = list.subLists?.firstOrNull { it.id == subListId }
                        list.copy(subLists = list.subLists?.filterNot { it.id == subListId })
                    } else {
                        list.copy(subLists = list.subLists?.let { removeList(it) })
                    }
                }
            }

            fun addList(lists: List<ShoppingList>): List<ShoppingList> {
                return lists.map { list ->
                    if (list.id == toGroupId) {
                        subListToMove?.let { movedList ->
                            list.copy(subLists = (list.subLists ?: emptyList()) + movedList.copy(parentId = toGroupId))
                        } ?: list
                    } else {
                        list.copy(subLists = list.subLists?.let { addList(it) })
                    }
                }
            }

            val listsWithRemoved = removeList(currentLists)
            addList(listsWithRemoved)
        }
    }

    private fun mapAndMoveItem(
        lists: List<ShoppingList>,
        listId: String,
        from: Int,
        to: Int
    ): List<ShoppingList> {
        return lists.map { list ->
            if (list.id == listId) {
                val mutableItems = list.items?.toMutableList()
                if (mutableItems != null) {
                    mutableItems.add(to, mutableItems.removeAt(from))
                    list.copy(items = mutableItems.toList())
                } else {
                    list
                }
            } else {
                list.copy(subLists = list.subLists?.let { mapAndMoveItem(it, listId, from, to) })
            }
        }
    }

    fun moveItem(listId: String, from: Int, to: Int) {
        _shoppingLists.update { currentLists ->
            mapAndMoveItem(currentLists, listId, from, to)
        }
    }

    private fun mapAndToggleChecked(
        lists: List<ShoppingList>,
        listId: String,
        itemToToggle: ShoppingItem
    ): List<ShoppingList> {
        return lists.map { list ->
            if (list.id == listId) {
                val updatedItems = list.items?.map { item ->
                    if (item.id == itemToToggle.id) {
                        item.copy(isChecked = !item.isChecked)
                    } else {
                        item
                    }
                }
                list.copy(items = updatedItems)
            } else {
                list.copy(
                    subLists = list.subLists?.let {
                        mapAndToggleChecked(
                            it,
                            listId,
                            itemToToggle
                        )
                    }
                )
            }
        }
    }

    fun toggleChecked(listId: String, itemToToggle: ShoppingItem) {
        _shoppingLists.update { currentLists ->
            mapAndToggleChecked(currentLists, listId, itemToToggle)
        }
    }

    private fun mapAndAddItem(
        lists: List<ShoppingList>,
        listId: String,
        newItem: ShoppingItem
    ): List<ShoppingList> {
        return lists.map { list ->
            if (list.id == listId) {
                list.copy(items = (list.items ?: emptyList()) + newItem)
            } else {
                list.copy(subLists = list.subLists?.let { mapAndAddItem(it, listId, newItem) })
            }
        }
    }

    fun addItem(listId: String, itemName: String, isHeader: Boolean = false) {
        _shoppingLists.update { currentLists ->
            val newItem = ShoppingItem(
                id = UUID.randomUUID().toString(),
                name = itemName.trim(),
                isHeader = isHeader
            )
            mapAndAddItem(currentLists, listId, newItem)
        }
    }

    private fun mapAndEditItemName(
        lists: List<ShoppingList>,
        listId: String,
        itemId: String,
        newItemName: String
    ): List<ShoppingList> {
        return lists.map { list ->
            if (list.id == listId) {
                val updatedItems = list.items?.map { item ->
                    if (item.id == itemId) {
                        item.copy(name = newItemName.trim())
                    } else {
                        item
                    }
                }
                list.copy(items = updatedItems)
            } else {
                list.copy(
                    subLists = list.subLists?.let {
                        mapAndEditItemName(
                            it,
                            listId,
                            itemId,
                            newItemName
                        )
                    }
                )
            }
        }
    }

    fun editItemName(listId: String, itemId: String, newItemName: String) {
        _shoppingLists.update { currentLists ->
            mapAndEditItemName(currentLists, listId, itemId, newItemName)
        }
    }

    private fun mapAndRemoveItem(
        lists: List<ShoppingList>,
        listId: String,
        itemToRemove: ShoppingItem
    ): List<ShoppingList> {
        return lists.map { list ->
            if (list.id == listId) {
                val updatedItems = list.items?.filterNot { it.id == itemToRemove.id }
                list.copy(items = updatedItems)
            } else {
                list.copy(
                    subLists = list.subLists?.let {
                        mapAndRemoveItem(
                            it,
                            listId,
                            itemToRemove
                        )
                    }
                )
            }
        }
    }

    fun removeItem(listId: String, itemToRemove: ShoppingItem) {
        _shoppingLists.update { currentLists ->
            mapAndRemoveItem(currentLists, listId, itemToRemove)
        }
    }
}
