package com.mitte.listree

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mitte.listree.data.LisTreeRepository
import com.mitte.listree.data.PreferencesManager
import com.mitte.listree.ui.models.ListItem
import com.mitte.listree.ui.models.ListType
import com.mitte.listree.ui.models.TreeList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class LisTreeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LisTreeRepository(application)
    private val preferencesManager = PreferencesManager(application)

    private val _treeLists = MutableStateFlow<List<TreeList>>(emptyList())
    val treeLists: StateFlow<List<TreeList>> = _treeLists.asStateFlow()

    private val _showDeleted = MutableStateFlow(preferencesManager.getShowDeleted())
    val showDeleted: StateFlow<Boolean> = _showDeleted.asStateFlow()

    init {
        loadLists()
    }

    fun setShowDeleted(show: Boolean) {
        preferencesManager.setShowDeleted(show)
        _showDeleted.value = show
        loadLists() // Reload to apply filter
    }

    private fun loadLists() {
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
                        order = dataItem.order,
                        lastModified = dataItem.lastModified,
                        deleted = dataItem.deleted
                    )
                }
                TreeList(
                    id = dataList.id,
                    name = dataList.name,
                    type = dataList.type?.let { ListType.valueOf(it.name) },
                    parentId = dataList.parentId,
                    items = uiItems.sortedBy { it.order },
                    subLists = emptyList(),
                    order = dataList.order,
                    lastModified = dataList.lastModified,
                    deleted = dataList.deleted
                )
            }

            fun buildTree(parentId: String?): List<TreeList> {
                return allUiLists
                    .filter { it.parentId == parentId && (!it.deleted || _showDeleted.value) }
                    .sortedBy { it.order }
                    .map { it.copy(subLists = buildTree(it.id)) }
            }

            _treeLists.value = buildTree(null)
        }
    }

    fun exportData(context: Context, uri: Uri?): String? {
        val gson = Gson()
        val jsonString = gson.toJson(treeLists.value)

        if (uri != null) { // For older Android versions via file picker
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(jsonString.toByteArray())
                }
                return uri.path // This might not be a user-friendly path
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        } else { // For modern Android versions with MediaStore
            val timeStamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
            val fileName = "$timeStamp.json"

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/LisTreeBackup")
                }
            }

            return try {
                val contentUri = MediaStore.Files.getContentUri("external")
                context.contentResolver.insert(contentUri, contentValues)?.let { newUri ->
                    context.contentResolver.openOutputStream(newUri)?.use { outputStream ->
                        outputStream.write(jsonString.toByteArray())
                    }
                    val path = "${Environment.DIRECTORY_DOWNLOADS}/LisTreeBackup/$fileName"
                    path
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    fun importData(context: Context, uri: Uri) {
        val gson = Gson()
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    val jsonString = reader.readText()
                    val type = object : TypeToken<List<TreeList>>() {}.type
                    val importedLists: List<TreeList> = gson.fromJson(jsonString, type)

                    val existingLists = _treeLists.value
                    val mergedToplevelLists = mergeLists(existingLists, importedLists, null)

                    _treeLists.value = mergedToplevelLists
                    saveLists()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun mergeLists(
        existingLists: List<TreeList>,
        importedLists: List<TreeList>,
        parentId: String?
    ): List<TreeList> {
        val mutableExisting = existingLists.toMutableList()

        importedLists.forEach { importedList ->
            val existingList = mutableExisting.find { it.name == importedList.name && it.parentId == parentId }

            if (existingList == null) {
                // --- NEW LIST ---
                val idMap = mutableMapOf<String, String>()
                val newList = generateNewIds(importedList, parentId, idMap)
                mutableExisting.add(newList)
            } else {
                // --- MERGE CHILDREN --- (Only for groups)
                if (existingList.type == ListType.GROUP_LIST && importedList.subLists != null) {
                    val mergedChildren = mergeLists(
                        existingList.subLists ?: emptyList(),
                        importedList.subLists,
                        existingList.id
                    )
                    val listIndex = mutableExisting.indexOf(existingList)
                    if (listIndex != -1) {
                        mutableExisting[listIndex] = existingList.copy(subLists = mergedChildren)
                    }
                }

                if (existingList.type == ListType.ITEM_LIST && importedList.items != null) {
                    val mergedItems = mergeItems(
                        existingList.items ?: emptyList(),
                        importedList.items
                    )
                    val listIndex = mutableExisting.indexOf(existingList)
                    if (listIndex != -1) {
                        mutableExisting[listIndex] = existingList.copy(items = mergedItems)
                    }
                }
            }
        }
        return mutableExisting.mapIndexed { index, list -> list.copy(order = index) }
    }

    private fun mergeItems(existingItems: List<ListItem>, importedItems: List<ListItem>): List<ListItem> {
        val mutableExisting = existingItems.toMutableList()
        importedItems.forEach { importedItem ->
            val existingItem = mutableExisting.find { it.name == importedItem.name }
            if (existingItem == null) {
                mutableExisting.add(importedItem.copy(id = UUID.randomUUID().toString()))
            }
        }
        return mutableExisting.mapIndexed { index, item -> item.copy(order = index) }
    }

    private fun generateNewIds(treeList: TreeList, newParentId: String?, idMap: MutableMap<String, String>): TreeList {
        val oldId = treeList.id
        val newId = UUID.randomUUID().toString()
        idMap[oldId] = newId

        val newSubLists = treeList.subLists?.map { subList ->
            generateNewIds(subList, newId, idMap)
        }

        val newItems = treeList.items?.map { item ->
            item.copy(id = UUID.randomUUID().toString())
        }

        return treeList.copy(
            id = newId,
            parentId = newParentId,
            subLists = newSubLists,
            items = newItems
        )
    }


    private fun saveLists() {
        viewModelScope.launch {
            repository.saveShoppingLists(_treeLists.value)
        }
    }

    private fun getAllLists(lists: List<TreeList>): List<TreeList> {
        return lists.flatMap { list ->
            listOf(list) + getAllLists(list.subLists ?: emptyList())
        }
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
                list.copy(subLists = ((list.subLists ?: emptyList()) + newSubGroup).sortedBy { it.order }, lastModified = System.currentTimeMillis())
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
                list.copy(subLists = ((list.subLists ?: emptyList()) + newList).sortedBy { it.order }, lastModified = System.currentTimeMillis())
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

    private fun mapAndDeleteList(lists: List<TreeList>, listId: String): List<TreeList> {
        return lists.map { list ->
            if (list.id == listId) {
                list.copy(deleted = true, lastModified = System.currentTimeMillis())
            } else {
                list.copy(subLists = list.subLists?.let { mapAndDeleteList(it, listId) })
            }
        }
    }

    fun deleteList(listId: String) {
        _treeLists.update { currentLists ->
            mapAndDeleteList(currentLists, listId)
        }
        saveLists()
    }

    private fun mapAndUndeleteList(lists: List<TreeList>, listId: String): List<TreeList> {
        return lists.map { list ->
            if (list.id == listId) {
                list.copy(deleted = false, lastModified = System.currentTimeMillis())
            } else {
                list.copy(subLists = list.subLists?.let { mapAndUndeleteList(it, listId) })
            }
        }
    }

    fun undeleteList(listId: String) {
        _treeLists.update { currentLists ->
            mapAndUndeleteList(currentLists, listId)
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
                list.copy(name = newName.trim(), lastModified = System.currentTimeMillis())
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
                    list.copy(subLists = mutableSubLists.mapIndexed { index, subList -> subList.copy(order = index) }.toList(), lastModified = System.currentTimeMillis())
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
                currentLists.filterNot { it.id == listId }
                    .mapIndexed { index, list -> list.copy(order = index) }
            } else {
                fun removeRecursively(lists: List<TreeList>): List<TreeList> {
                    return lists.map { list ->
                        if (list.id == oldParentId) {
                            val newSubLists = list.subLists?.filterNot { it.id == listId }
                                ?.mapIndexed { index, subList -> subList.copy(order = index) }
                            list.copy(subLists = newSubLists, lastModified = System.currentTimeMillis())
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
                            list.copy(subLists = ((list.subLists ?: emptyList()) + updatedList), lastModified = System.currentTimeMillis())
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
                    list.copy(items = updatedItems, lastModified = System.currentTimeMillis())
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
                        item.copy(isChecked = !item.isChecked, lastModified = System.currentTimeMillis())
                    } else {
                        item
                    }
                }
                list.copy(items = updatedItems, lastModified = System.currentTimeMillis())
            } else {
                list.copy(subLists = list.subLists?.let {
                    mapAndToggleChecked(
                        it,
                        listId,
                        itemToToggle
                    )
                })
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
                list.copy(items = ((list.items ?: emptyList()) + newItem).sortedBy { it.order }, lastModified = System.currentTimeMillis())
            } else {
                list.copy(subLists = list.subLists?.let { mapAndAddItem(it, listId, newItem) })
            }
        }
    }

    fun addItem(listId: String, itemName: String, isHeader: Boolean = false) {
        _treeLists.update { currentLists ->
            val parentList = getListById(listId)
            val nextOrder = (parentList?.items?.maxOfOrNull { it.order } ?: -1) + 1
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
                        item.copy(name = newItemName.trim(), lastModified = System.currentTimeMillis())
                    } else {
                        item
                    }
                }
                list.copy(items = updatedItems, lastModified = System.currentTimeMillis())
            } else {
                list.copy(subLists = list.subLists?.let {
                    mapAndEditItemName(
                        it,
                        listId,
                        itemId,
                        newItemName
                    )
                })
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
                val updatedItems = list.items?.map { item ->
                    if (item.id == itemToRemove.id) {
                        item.copy(deleted = true, lastModified = System.currentTimeMillis())
                    } else {
                        item
                    }
                }
                list.copy(items = updatedItems, lastModified = System.currentTimeMillis())
            } else {
                list.copy(subLists = list.subLists?.let {
                    mapAndRemoveItem(
                        it,
                        listId,
                        itemToRemove
                    )
                })
            }
        }
    }

    fun removeItem(listId: String, itemToRemove: ListItem) {
        _treeLists.update { currentLists ->
            mapAndRemoveItem(currentLists, listId, itemToRemove)
        }
        saveLists()
    }

    private fun mapAndUndeleteItem(
        lists: List<TreeList>,
        listId: String,
        itemToUndelete: ListItem
    ): List<TreeList> {
        return lists.map { list ->
            if (list.id == listId) {
                val updatedItems = list.items?.map { item ->
                    if (item.id == itemToUndelete.id) {
                        item.copy(deleted = false, lastModified = System.currentTimeMillis())
                    } else {
                        item
                    }
                }
                list.copy(items = updatedItems, lastModified = System.currentTimeMillis())
            } else {
                list.copy(subLists = list.subLists?.let {
                    mapAndUndeleteItem(
                        it,
                        listId,
                        itemToUndelete
                    )
                })
            }
        }
    }

    fun undeleteItem(listId: String, itemToUndelete: ListItem) {
        _treeLists.update { currentLists ->
            mapAndUndeleteItem(currentLists, listId, itemToUndelete)
        }
        saveLists()
    }
}
