package com.mitte.shopper

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.mitte.shopper.data.ShoppingRepository
import com.mitte.shopper.ui.models.ListType
import com.mitte.shopper.ui.models.ShoppingItem
import com.mitte.shopper.ui.models.ShoppingList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ShoppingViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ShoppingRepository(application)

    private val _shoppingLists = MutableStateFlow<List<ShoppingList>>(repository.getShoppingLists())
    val shoppingLists: StateFlow<List<ShoppingList>> = _shoppingLists.asStateFlow()

    private fun getAllLists(lists: List<ShoppingList>): List<ShoppingList> {
        return lists + lists.flatMap { getAllLists(it.subLists ?: emptyList()) }
    }

    fun getListById(listId: Int): ShoppingList? {
        return getAllLists(shoppingLists.value).firstOrNull { it.id == listId }
    }

    fun toggleExpanded(listId: Int) {
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
            val updatedList = mapList(currentLists)
            repository.saveShoppingLists(updatedList)
            updatedList
        }
    }

    fun addList(listName: String) {
        _shoppingLists.update { currentLists ->
            val newId = (getAllLists(currentLists).maxOfOrNull { it.id } ?: 0) + 1

            val newList = ShoppingList(
                id = newId,
                name = listName.trim(),
                type = ListType.ITEM_LIST,
                items = emptyList(),
                subLists = emptyList()
            )

            val updatedList = currentLists + newList
            repository.saveShoppingLists(updatedList)
            updatedList
        }
    }

    fun addGroupList(listName: String) {
        _shoppingLists.update { currentLists ->
            val newId = (getAllLists(currentLists).maxOfOrNull { it.id } ?: 0) + 1

            val newList = ShoppingList(
                id = newId,
                name = listName.trim(),
                type = ListType.GROUP_LIST,
                items = null,
                subLists = emptyList()
            )

            val updatedList = currentLists + newList
            repository.saveShoppingLists(updatedList)
            updatedList
        }
    }

    fun addSubGroup(parentGroupId: Int, subGroupName: String) {
        _shoppingLists.update { currentLists ->
            val newId = (getAllLists(currentLists).maxOfOrNull { it.id } ?: 0) + 1

            val newSubGroup = ShoppingList(
                id = newId,
                name = subGroupName.trim(),
                type = ListType.GROUP_LIST,
                items = null,
                subLists = emptyList(),
                parentId = parentGroupId
            )

            fun mapList(lists: List<ShoppingList>): List<ShoppingList> {
                return lists.map { list ->
                    if (list.id == parentGroupId) {
                        list.copy(subLists = (list.subLists ?: emptyList()) + newSubGroup)
                    } else {
                        list.copy(subLists = list.subLists?.let { mapList(it) })
                    }
                }
            }
            val updatedList = mapList(currentLists)
            repository.saveShoppingLists(updatedList)
            updatedList
        }
    }

    fun addSubList(parentGroupId: Int, subListName: String) {
        _shoppingLists.update { currentLists ->
            val newId = (getAllLists(currentLists).maxOfOrNull { it.id } ?: 0) + 1
            val newList = ShoppingList(
                id = newId,
                name = subListName.trim(),
                type = ListType.ITEM_LIST,
                items = emptyList(),
                subLists = emptyList(),
                parentId = parentGroupId
            )

            fun mapList(lists: List<ShoppingList>): List<ShoppingList> {
                return lists.map { list ->
                    if (list.id == parentGroupId) {
                        list.copy(subLists = (list.subLists ?: emptyList()) + newList)
                    } else {
                        list.copy(subLists = list.subLists?.let { mapList(it) })
                    }
                }
            }

            val updatedList = mapList(currentLists)
            repository.saveShoppingLists(updatedList)
            updatedList
        }
    }

    fun deleteList(listId: Int) {
        _shoppingLists.update { currentLists ->
            val updatedList = currentLists.filterNot { it.id == listId }
                .map { list ->
                    list.copy(subLists = list.subLists?.filterNot { subList -> subList.id == listId })
                }
            repository.saveShoppingLists(updatedList)
            updatedList
        }
    }

    fun editListName(listId: Int, newName: String) {
        _shoppingLists.update { currentLists ->
            val updatedList = currentLists.map { list ->
                if (list.id == listId) {
                    list.copy(name = newName.trim())
                } else if (list.subLists?.any { it.id == listId } == true) {
                    list.copy(subLists = list.subLists.map { subList ->
                        if (subList.id == listId) {
                            subList.copy(name = newName.trim())
                        } else {
                            subList
                        }
                    })
                } else {
                    list
                }
            }
            repository.saveShoppingLists(updatedList)
            updatedList
        }
    }

    fun moveList(from: Int, to: Int) {
        _shoppingLists.update { currentLists ->
            val mutableList = currentLists.toMutableList()
            mutableList.add(to, mutableList.removeAt(from))

            val updatedList = mutableList.toList()
            repository.saveShoppingLists(updatedList)
            updatedList
        }
    }

    fun moveSubList(parentListId: Int, from: Int, to: Int) {
        _shoppingLists.update { currentLists ->
            fun mapList(lists: List<ShoppingList>): List<ShoppingList> {
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
                        list.copy(subLists = list.subLists?.let { mapList(it) })
                    }
                }
            }

            val updatedLists = mapList(currentLists)
            repository.saveShoppingLists(updatedLists)
            updatedLists
        }
    }

    fun moveSubListToNewGroup(subListId: Int, fromGroupId: Int, toGroupId: Int) {
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
                        val movedList = subListToMove?.copy(parentId = toGroupId)
                        list.copy(subLists = (list.subLists ?: emptyList()) + movedList!!)
                    } else {
                        list.copy(subLists = list.subLists?.let { addList(it) })
                    }
                }
            }

            val listsWithRemoved = removeList(currentLists)
            val listsWithAdded = addList(listsWithRemoved)

            repository.saveShoppingLists(listsWithAdded)
            listsWithAdded
        }
    }

    fun moveItem(listId: Int, from: Int, to: Int) {
        _shoppingLists.update { currentLists ->
            val updatedList = currentLists.map { list ->
                if (list.id == listId) {
                    val mutableItems = list.items?.toMutableList()
                    if (mutableItems != null) {
                        mutableItems.add(to, mutableItems.removeAt(from))
                        list.copy(items = mutableItems.toList())
                    } else {
                        list
                    }
                } else {
                    list.copy(subLists = list.subLists?.map { subList ->
                        if (subList.id == listId) {
                            val mutableItems = subList.items?.toMutableList()
                            if (mutableItems != null) {
                                mutableItems.add(to, mutableItems.removeAt(from))
                                subList.copy(items = mutableItems.toList())
                            } else {
                                subList
                            }
                        } else {
                            subList
                        }
                    })
                }
            }
            repository.saveShoppingLists(updatedList)
            updatedList
        }
    }

    fun toggleChecked(listId: Int, itemToToggle: ShoppingItem) {
        _shoppingLists.update { currentLists ->
            val updatedList = currentLists.map { list ->
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
                    list.copy(subLists = list.subLists?.map { subList ->
                        if (subList.id == listId) {
                            val updatedItems = subList.items?.map { item ->
                                if (item.id == itemToToggle.id) {
                                    item.copy(isChecked = !item.isChecked)
                                } else {
                                    item
                                }
                            }
                            subList.copy(items = updatedItems)
                        } else {
                            subList
                        }
                    })
                }
            }
            repository.saveShoppingLists(updatedList)
            updatedList
        }
    }

    fun addItem(listId: Int, itemName: String) {
        _shoppingLists.update { currentLists ->
            val updatedList = currentLists.map { list ->
                if (list.id == listId) {
                    val newId = (list.items?.maxOfOrNull { it.id } ?: (listId * 1000)) + 1
                    val newItem = ShoppingItem(id = newId, name = itemName.trim())
                    list.copy(items = (list.items ?: emptyList()) + newItem)
                } else {
                    list.copy(subLists = list.subLists?.map { subList ->
                        if (subList.id == listId) {
                            val newId = (subList.items?.maxOfOrNull { it.id } ?: (listId * 1000)) + 1
                            val newItem = ShoppingItem(id = newId, name = itemName.trim())
                            subList.copy(items = (subList.items ?: emptyList()) + newItem)
                        } else {
                            subList
                        }
                    })
                }
            }
            repository.saveShoppingLists(updatedList)
            updatedList
        }
    }

    fun editItemName(listId: Int, itemId: Int, newItemName: String) {
        _shoppingLists.update { currentLists ->
            val updatedList = currentLists.map { list ->
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
                    list.copy(subLists = list.subLists?.map { subList ->
                        if (subList.id == listId) {
                            val updatedItems = subList.items?.map { item ->
                                if (item.id == itemId) {
                                    item.copy(name = newItemName.trim())
                                } else {
                                    item
                                }
                            }
                            subList.copy(items = updatedItems)
                        } else {
                            subList
                        }
                    })
                }
            }
            repository.saveShoppingLists(updatedList)
            updatedList
        }
    }

    fun removeItem(listId: Int, itemToRemove: ShoppingItem) {
        _shoppingLists.update { currentLists ->
            val updatedList = currentLists.map { list ->
                if (list.id == listId) {
                    val updatedItems = list.items?.filterNot { it.id == itemToRemove.id }
                    list.copy(items = updatedItems)
                } else {
                    list.copy(subLists = list.subLists?.map { subList ->
                        if (subList.id == listId) {
                            subList.copy(items = subList.items?.filterNot { it.id == itemToRemove.id })
                        } else {
                            subList
                        }
                    })
                }
            }
            repository.saveShoppingLists(updatedList)
            updatedList
        }
    }
}
