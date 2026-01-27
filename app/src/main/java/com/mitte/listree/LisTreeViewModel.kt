package com.mitte.listree

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mitte.listree.data.LisTreeRepository
import com.mitte.listree.ui.models.ListType
import com.mitte.listree.ui.models.ListItem
import com.mitte.listree.ui.models.TreeList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class LisTreeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LisTreeRepository(application)

    private val _treeLists = MutableStateFlow<List<TreeList>>(emptyList())
    val treeLists: StateFlow<List<TreeList>> = _treeLists.asStateFlow()

    init {
        viewModelScope.launch {
            val allListsWithItems = repository.getShoppingLists().first()
            val allUiLists = allListsWithItems.map { dataListWithItems ->
                val dataList = dataListWithItems.lisTreeList
                val uiItems = dataListWithItems.items.map { dataItem ->
                    ListItem(
                        id = dataItem.id,
                        name = dataItem.name,
                        isChecked = dataItem.isChecked,
                        isHeader = dataItem.isHeader,
                        order = dataItem.order
                    )
                }
                TreeList(
                    id = dataList.id,
                    name = dataList.name,
                    type = dataList.type?.let { ListType.valueOf(it.name) },
                    parentId = dataList.parentId,
                    items = uiItems.sortedBy { it.order },
                    subLists = emptyList(),
                    order = dataList.order
                )
            }

            fun buildTree(parentId: String?): List<TreeList> {
                return allUiLists
                    .filter { it.parentId == parentId }
                    .sortedBy { it.order }
                    .map { it.copy(subLists = buildTree(it.id)) }
            }

            _treeLists.value = buildTree(null)
        }
    }

    private fun saveLists() {
        viewModelScope.launch {
            repository.saveShoppingLists(_treeLists.value)
        }
    }

    private fun getAllLists(lists: List<TreeList>): List<TreeList> {
        return lists + lists.flatMap { getAllLists(it.subLists ?: emptyList()) }
    }

    fun getListById(listId: String): TreeList? {
        return getAllLists(treeLists.value).firstOrNull { it.id == listId }
    }

    fun toggleExpanded(listId: String) {
        _treeLists.update { currentLists ->
            fun mapList(list: List<TreeList>): List<TreeList> {
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
        _treeLists.update { currentLists ->
            val newList = TreeList(
                id = UUID.randomUUID().toString(),
                name = listName.trim(),
                type = ListType.ITEM_LIST,
                items = emptyList(),
                subLists = emptyList(),
                order = currentLists.size
            )
            currentLists + newList
        }
        saveLists()
    }

    fun addGroupList(listName: String) {
        _treeLists.update { currentLists ->
            val newList = TreeList(
                id = UUID.randomUUID().toString(),
                name = listName.trim(),
                type = ListType.GROUP_LIST,
                items = null,
                subLists = emptyList(),
                order = currentLists.size
            )
            currentLists + newList
        }
        saveLists()
    }

    private fun mapAndAddSubGroup(
        lists: List<TreeList>,
        parentGroupId: String,
        newSubGroup: TreeList
    ): List<TreeList> {
        return lists.map { list ->
            if (list.id == parentGroupId) {
                list.copy(subLists = ((list.subLists ?: emptyList()) + newSubGroup).sortedBy { it.order })
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
        _treeLists.update { currentLists ->
            val parentList = getListById(parentGroupId)
            val newSubGroup = TreeList(
                id = UUID.randomUUID().toString(),
                name = subGroupName.trim(),
                type = ListType.GROUP_LIST,
                items = null,
                subLists = emptyList(),
                parentId = parentGroupId,
                order = parentList?.subLists?.size ?: 0
            )
            mapAndAddSubGroup(currentLists, parentGroupId, newSubGroup)
        }
        saveLists()
    }

    private fun mapAndAddSubList(
        lists: List<TreeList>,
        parentGroupId: String,
        newList: TreeList
    ): List<TreeList> {
        return lists.map { list ->
            if (list.id == parentGroupId) {
                list.copy(subLists = ((list.subLists ?: emptyList()) + newList).sortedBy { it.order })
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
        _treeLists.update { currentLists ->
            val parentList = getListById(parentGroupId)
            val newList = TreeList(
                id = UUID.randomUUID().toString(),
                name = subListName.trim(),
                type = ListType.ITEM_LIST,
                items = emptyList(),
                subLists = emptyList(),
                parentId = parentGroupId,
                order = parentList?.subLists?.size ?: 0
            )
            mapAndAddSubList(currentLists, parentGroupId, newList)
        }
        saveLists()
    }

    fun deleteList(listId: String) {
        _treeLists.update { currentLists ->
            fun removeRecursively(lists: List<TreeList>): List<TreeList> {
                val filteredList = lists.filterNot { it.id == listId }
                return filteredList.map { list ->
                    list.copy(subLists = list.subLists?.let { removeRecursively(it) })
                }
            }
            removeRecursively(currentLists)
        }
        saveLists()
    }

    private fun mapAndEditListName(
        lists: List<TreeList>,
        listId: String,
        newName: String
    ): List<TreeList> {
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
        _treeLists.update { currentLists ->
            mapAndEditListName(currentLists, listId, newName)
        }
        saveLists()
    }

    fun moveList(from: Int, to: Int) {
        _treeLists.update { currentLists ->
            val mutableList = currentLists.toMutableList()
            mutableList.add(to, mutableList.removeAt(from))
            mutableList.mapIndexed { index, list -> list.copy(order = index) }.toList()
        }
        saveLists()
    }

    private fun mapAndMoveSubList(
        lists: List<TreeList>,
        parentListId: String,
        from: Int,
        to: Int
    ): List<TreeList> {
        return lists.map { list ->
            if (list.id == parentListId) {
                val mutableSubLists = list.subLists?.toMutableList()
                if (mutableSubLists != null) {
                    mutableSubLists.add(to, mutableSubLists.removeAt(from))
                    list.copy(subLists = mutableSubLists.mapIndexed { index, subList -> subList.copy(order = index) }.toList())
                } else {
                    list
                }
            } else {
                list.copy(subLists = list.subLists?.let { mapAndMoveSubList(it, parentListId, from, to) })
            }
        }
    }

    fun moveSubList(parentListId: String, from: Int, to: Int) {
        _treeLists.update { currentLists ->
            mapAndMoveSubList(currentLists, parentListId, from, to)
        }
        saveLists()
    }

    fun moveList(listId: String, newParentId: String?) {
        _treeLists.update { currentLists ->
            val listToMove = getAllLists(currentLists).find { it.id == listId } ?: return@update currentLists
            val oldParentId = listToMove.parentId

            if (oldParentId == newParentId) return@update currentLists // Nothing to do

            // --- REMOVAL STEP ---
            val listRemovedTree = if (oldParentId == null) {
                currentLists
                    .filterNot { it.id == listId }
                    .mapIndexed { index, list -> list.copy(order = index) }
            } else {
                fun removeRecursively(lists: List<TreeList>): List<TreeList> {
                    return lists.map { list ->
                        if (list.id == oldParentId) {
                            val newSubLists = list.subLists
                                ?.filterNot { it.id == listId }
                                ?.mapIndexed { index, subList -> subList.copy(order = index) }
                            list.copy(subLists = newSubLists)
                        } else {
                            list.copy(subLists = list.subLists?.let { removeRecursively(it) })
                        }
                    }
                }
                removeRecursively(currentLists)
            }

            // --- ADDITION STEP ---
            val listAddedTree = if (newParentId == null) {
                val newOrder = listRemovedTree.size
                listRemovedTree + listToMove.copy(parentId = null, order = newOrder)
            } else {
                fun addRecursively(lists: List<TreeList>): List<TreeList> {
                    return lists.map { list ->
                        if (list.id == newParentId) {
                            val newOrder = list.subLists?.size ?: 0
                            val updatedList = listToMove.copy(parentId = newParentId, order = newOrder)
                            list.copy(subLists = ((list.subLists ?: emptyList()) + updatedList))
                        } else {
                            list.copy(subLists = list.subLists?.let { addRecursively(it) })
                        }
                    }
                }
                addRecursively(listRemovedTree)
            }
            listAddedTree
        }
        saveLists()
    }

    private fun mapAndMoveItem(
        lists: List<TreeList>,
        listId: String,
        from: Int,
        to: Int
    ): List<TreeList> {
        return lists.map { list ->
            if (list.id == listId) {
                val sortedItems = list.items?.sortedBy { it.order }
                val mutableItems = sortedItems?.toMutableList()
                if (mutableItems != null) {
                    mutableItems.add(to, mutableItems.removeAt(from))
                    val updatedItems = mutableItems.mapIndexed { index, item -> item.copy(order = index) }
                    list.copy(items = updatedItems)
                } else {
                    list
                }
            } else {
                list.copy(subLists = list.subLists?.let { mapAndMoveItem(it, listId, from, to) })
            }
        }
    }

    fun moveItem(listId: String, from: Int, to: Int) {
        _treeLists.update { currentLists ->
            mapAndMoveItem(currentLists, listId, from, to)
        }
        saveLists()
    }

    private fun mapAndToggleChecked(
        lists: List<TreeList>,
        listId: String,
        itemToToggle: ListItem
    ): List<TreeList> {
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

    fun toggleChecked(listId: String, itemToToggle: ListItem) {
        _treeLists.update { currentLists ->
            mapAndToggleChecked(currentLists, listId, itemToToggle)
        }
        saveLists()
    }

    private fun mapAndAddItem(
        lists: List<TreeList>,
        listId: String,
        newItem: ListItem
    ): List<TreeList> {
        return lists.map { list ->
            if (list.id == listId) {
                list.copy(items = ((list.items ?: emptyList()) + newItem).sortedBy { it.order })
            } else {
                list.copy(subLists = list.subLists?.let { mapAndAddItem(it, listId, newItem) })
            }
        }
    }

    fun addItem(listId: String, itemName: String, isHeader: Boolean = false) {
        _treeLists.update { currentLists ->
            val parentList = getListById(listId)
            val nextOrder = (parentList?.items?.map { it.order }?.maxOrNull() ?: -1) + 1
            val newItem = ListItem(
                id = UUID.randomUUID().toString(),
                name = itemName.trim(),
                isHeader = isHeader,
                order = nextOrder
            )
            mapAndAddItem(currentLists, listId, newItem)
        }
        saveLists()
    }

    private fun mapAndEditItemName(
        lists: List<TreeList>,
        listId: String,
        itemId: String,
        newItemName: String
    ): List<TreeList> {
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
        _treeLists.update { currentLists ->
            mapAndEditItemName(currentLists, listId, itemId, newItemName)
        }
        saveLists()
    }

    private fun mapAndRemoveItem(
        lists: List<TreeList>,
        listId: String,
        itemToRemove: ListItem
    ): List<TreeList> {
        return lists.map { list ->
            if (list.id == listId) {
                val updatedItems = list.items
                    ?.filterNot { it.id == itemToRemove.id }
                    ?.sortedBy { it.order }
                    ?.mapIndexed { index, item -> item.copy(order = index) }
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

    fun removeItem(listId: String, itemToRemove: ListItem) {
        _treeLists.update { currentLists ->
            mapAndRemoveItem(currentLists, listId, itemToRemove)
        }
        saveLists()
    }
}
