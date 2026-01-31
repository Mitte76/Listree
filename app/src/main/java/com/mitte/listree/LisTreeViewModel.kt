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
import com.mitte.listree.ui.models.ListContent
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
            val allListsWithItems = repository.getShoppingLists(_showDeleted.value).first()
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
                    children = uiItems.sortedBy { it.order },
                    order = dataList.order,
                    lastModified = dataList.lastModified,
                    deleted = dataList.deleted
                )
            }

            val expandedIds = preferencesManager.getExpandedListIds()

            fun buildTree(parentId: String?): List<TreeList> {
                return allUiLists
                    .filter { it.parentId == parentId }
                    .map { list ->
                        val subLists = buildTree(list.id)
                        val items = list.children.filterIsInstance<ListItem>()
                        val combinedChildren = (subLists + items).sortedBy { it.order }
                        list.copy(
                            isExpanded = list.id in expandedIds,
                            children = combinedChildren
                        )
                    }
                    .sortedBy { it.order }
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
                // --- MERGE CHILDREN ---
                val mergedChildren = mergeLists(
                    existingList.children.filterIsInstance<TreeList>(),
                    importedList.children.filterIsInstance<TreeList>(),
                    existingList.id
                )
                val mergedItems = mergeItems(
                    existingList.children.filterIsInstance<ListItem>(),
                    importedList.children.filterIsInstance<ListItem>()
                )
                val listIndex = mutableExisting.indexOf(existingList)
                if (listIndex != -1) {
                    mutableExisting[listIndex] = existingList.copy(children = mergedChildren + mergedItems)
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

        val newChildren = treeList.children.map { child ->
            when (child) {
                is TreeList -> generateNewIds(child, newId, idMap)
                is ListItem -> child.copy(id = UUID.randomUUID().toString())
            }
        }

        return treeList.copy(
            id = newId,
            parentId = newParentId,
            children = newChildren
        )
    }


    private fun saveLists() {
        viewModelScope.launch {
            repository.saveShoppingLists(_treeLists.value)
        }
    }

    private fun getAllLists(lists: List<TreeList>): List<TreeList> {
        return lists.flatMap { list ->
            listOf(list) + getAllLists(list.children.filterIsInstance<TreeList>())
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
                        item.copy(children = item.children.map { if (it is TreeList) mapList(listOf(it)).first() else it })
                    }
                }
            }
            mapList(currentLists)
        }
        val expandedIds = getAllLists(_treeLists.value).filter { it.isExpanded }.map { it.id }.toSet()
        preferencesManager.saveExpandedListIds(expandedIds)
    }

    fun addList(name: String, parentId: String? = null) {
        val newList = TreeList(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            type = ListType.ITEM_LIST,
            parentId = parentId,
            order = if (parentId == null) _treeLists.value.size else getListById(parentId)?.children?.size ?: 0
        )
        _treeLists.update { currentLists ->
            if (parentId == null) {
                currentLists + newList
            } else {
                mapAndAddChild(currentLists, parentId, newList)
            }
        }
        saveLists()
    }

    fun addGroup(name: String, parentId: String? = null) {
        val newGroup = TreeList(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            type = ListType.GROUP_LIST,
            parentId = parentId,
            order = if (parentId == null) _treeLists.value.size else getListById(parentId)?.children?.size ?: 0
        )
        _treeLists.update { currentLists ->
            if (parentId == null) {
                currentLists + newGroup
            } else {
                mapAndAddChild(currentLists, parentId, newGroup)
            }
        }
        saveLists()
    }

    private fun mapAndAddChild(lists: List<TreeList>, parentId: String, child: ListContent): List<TreeList> {
        return lists.map { list ->
            if (list.id == parentId) {
                list.copy(children = (list.children + child).sortedBy { it.order }, lastModified = System.currentTimeMillis())
            } else {
                list.copy(children = (list.children.filterIsInstance<ListItem>()) + mapAndAddChild(list.children.filterIsInstance<TreeList>(), parentId, child))
            }
        }
    }

    fun addItem(listId: String, itemName: String, isHeader: Boolean = false) {
        _treeLists.update { currentLists ->
            val parentList = getListById(listId)
            val nextOrder = (parentList?.children?.maxOfOrNull { it.order } ?: -1) + 1
            val newItem = ListItem(
                id = UUID.randomUUID().toString(),
                name = itemName.trim(),
                isHeader = isHeader,
                order = nextOrder
            )
            mapAndAddChild(currentLists, listId, newItem)
        }
        saveLists()
    }

    private fun mapAndDeleteList(lists: List<TreeList>, listId: String): List<TreeList> {
        return lists.map { list ->
            if (list.id == listId) {
                list.copy(deleted = true, lastModified = System.currentTimeMillis())
            } else {
                list.copy(children = mapAndDeleteList(list.children.filterIsInstance<TreeList>(), listId))
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
                list.copy(children = mapAndUndeleteList(list.children.filterIsInstance<TreeList>(), listId))
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
                list.copy(children = mapAndEditListName(list.children.filterIsInstance<TreeList>(), listId, newName))
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

    fun moveSubList(parentListId: String, from: Int, to: Int) {
        _treeLists.update { currentLists ->
            mapAndMoveSubList(currentLists, parentListId, from, to)
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
                val mutableChildren = list.children.toMutableList()
                mutableChildren.add(to, mutableChildren.removeAt(from))
                list.copy(children = mutableChildren.mapIndexed { index, child ->
                    when(child) {
                        is TreeList -> child.copy(order = index)
                        is ListItem -> child.copy(order = index)
                    }
                }.toList(), lastModified = System.currentTimeMillis())
            } else {
                list.copy(children = list.children.map { if (it is TreeList) mapAndMoveSubList(listOf(it), parentListId, from, to).first() else it })
            }
        }
    }

    fun moveListToNewParent(listId: String, newParentId: String?) {
        _treeLists.update { currentLists ->
            val listToMove = getAllLists(currentLists).find { it.id == listId } ?: return@update currentLists
            val oldParentId = listToMove.parentId

            if (oldParentId == newParentId) return@update currentLists // Nothing to do

            val listRemovedTree = if (oldParentId == null) {
                currentLists.filterNot { it.id == listId }
                    .mapIndexed { index, list -> list.copy(order = index) }
            } else {
                fun removeRecursively(lists: List<TreeList>): List<TreeList> {
                    return lists.map { list ->
                        if (list.id == oldParentId) {
                            val newChildren = list.children.filterNot { it.id == listId }
                                .mapIndexed { index, child ->
                                    when (child) {
                                        is TreeList -> child.copy(order = index)
                                        is ListItem -> child.copy(order = index)
                                    }
                                }
                            list.copy(children = newChildren, lastModified = System.currentTimeMillis())
                        } else {
                            list.copy(children = list.children.map { if (it is TreeList) removeRecursively(listOf(it)).first() else it })
                        }
                    }
                }
                removeRecursively(currentLists)
            }

            val listAddedTree = if (newParentId == null) {
                val newOrder = listRemovedTree.size
                listRemovedTree + listToMove.copy(parentId = null, order = newOrder)
            } else {
                fun addRecursively(lists: List<TreeList>): List<TreeList> {
                    return lists.map { list ->
                        if (list.id == newParentId) {
                            val newOrder = list.children.size
                            val updatedList = listToMove.copy(parentId = newParentId, order = newOrder)
                            list.copy(children = (list.children + updatedList), lastModified = System.currentTimeMillis())
                        } else {
                            list.copy(children = list.children.map { if (it is TreeList) addRecursively(listOf(it)).first() else it })
                        }
                    }
                }
                addRecursively(listRemovedTree)
            }
            listAddedTree
        }
        saveLists()
    }

    fun moveItem(listId: String, from: Int, to: Int) {
        _treeLists.update { currentLists ->
            mapAndMoveItem(currentLists, listId, from, to)
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
                val mutableChildren = list.children.toMutableList()
                mutableChildren.add(to, mutableChildren.removeAt(from))
                val updatedChildren = mutableChildren.mapIndexed { index, item ->
                    when (item) {
                        is ListItem -> item.copy(order = index)
                        is TreeList -> item.copy(order = index)
                    }
                }
                list.copy(children = updatedChildren, lastModified = System.currentTimeMillis())
            } else {
                list.copy(children = list.children.map { if (it is TreeList) mapAndMoveItem(listOf(it), listId, from, to).first() else it })
            }
        }
    }

    private fun mapAndToggleChecked(lists: List<TreeList>, listId: String, itemId: String): List<TreeList> {
        return lists.map { list ->
            if (list.id == listId) {
                val updatedChildren = list.children.map { item ->
                    if (item.id == itemId && item is ListItem) {
                        item.copy(isChecked = !item.isChecked, lastModified = System.currentTimeMillis())
                    } else {
                        item
                    }
                }
                list.copy(children = updatedChildren, lastModified = System.currentTimeMillis())
            } else {
                list.copy(children = list.children.map { if (it is TreeList) mapAndToggleChecked(listOf(it), listId, itemId).first() else it })
            }
        }
    }

    fun toggleChecked(listId: String, itemId: String) {
        _treeLists.update { currentLists ->
            mapAndToggleChecked(currentLists, listId, itemId)
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
                val updatedChildren = list.children.map { item ->
                    if (item.id == itemId && item is ListItem) {
                        item.copy(name = newItemName.trim(), lastModified = System.currentTimeMillis())
                    } else {
                        item
                    }
                }
                list.copy(children = updatedChildren, lastModified = System.currentTimeMillis())
            } else {
                list.copy(children = list.children.map { if (it is TreeList) mapAndEditItemName(listOf(it), listId, itemId, newItemName).first() else it })
            }
        }
    }

    fun editItemName(listId: String, itemId: String, newItemName: String) {
        _treeLists.update { currentLists ->
            mapAndEditItemName(currentLists, listId, itemId, newItemName)
        }
        saveLists()
    }

    private fun mapAndRemoveItem(lists: List<TreeList>, listId: String, itemId: String): List<TreeList> {
        return lists.map { list ->
            if (list.id == listId) {
                val updatedChildren = list.children.map { item ->
                    if (item.id == itemId) {
                        when (item) {
                            is ListItem -> item.copy(deleted = true, lastModified = System.currentTimeMillis())
                            is TreeList -> item.copy(deleted = true, lastModified = System.currentTimeMillis()) // Or handle recursively
                        }
                    } else {
                        item
                    }
                }
                list.copy(children = updatedChildren, lastModified = System.currentTimeMillis())
            } else {
                list.copy(children = list.children.map { if (it is TreeList) mapAndRemoveItem(listOf(it), listId, itemId).first() else it })
            }
        }
    }

    fun removeItem(listId: String, itemId: String) {
        _treeLists.update { currentLists ->
            mapAndRemoveItem(currentLists, listId, itemId)
        }
        saveLists()
    }

    private fun mapAndUndeleteItem(lists: List<TreeList>, listId: String, itemId: String): List<TreeList> {
        return lists.map { list ->
            if (list.id == listId) {
                val updatedChildren = list.children.map { item ->
                    if (item.id == itemId) {
                        when (item) {
                            is ListItem -> item.copy(deleted = false, lastModified = System.currentTimeMillis())
                            is TreeList -> item.copy(deleted = false, lastModified = System.currentTimeMillis()) // Or handle recursively
                        }
                    } else {
                        item
                    }
                }
                list.copy(children = updatedChildren, lastModified = System.currentTimeMillis())
            } else {
                list.copy(children = list.children.map { if (it is TreeList) mapAndUndeleteItem(listOf(it), listId, itemId).first() else it })
            }
        }
    }

    fun undeleteItem(listId: String, itemId: String) {
        _treeLists.update { currentLists ->
            mapAndUndeleteItem(currentLists, listId, itemId)
        }
        saveLists()
    }

    fun clearDeletedItems() {
        viewModelScope.launch {
            repository.permanentlyClearDeleted()
            loadLists()
        }
    }
}
