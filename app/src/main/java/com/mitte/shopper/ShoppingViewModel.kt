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

    fun getListById(listId: Int): ShoppingList? {
        return shoppingLists.value.firstOrNull { it.id == listId } 
            ?: shoppingLists.value.flatMap { it.subLists ?: emptyList() }.firstOrNull { it.id == listId }
    }

    fun toggleExpanded(listId: Int) {
        _shoppingLists.update { currentLists ->
            val updatedList = currentLists.map { list ->
                if (list.id == listId) {
                    list.copy(isExpanded = !list.isExpanded)
                } else {
                    list
                }
            }
            repository.saveShoppingLists(updatedList)
            updatedList
        }
    }

    fun addList(listName: String) {
        _shoppingLists.update { currentLists ->
            val allLists = currentLists.flatMap { it.subLists ?: emptyList() } + currentLists
            val newId = (allLists.maxOfOrNull { it.id } ?: 0) + 1

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
            val allLists = currentLists.flatMap { it.subLists ?: emptyList() } + currentLists
            val newId = (allLists.maxOfOrNull { it.id } ?: 0) + 1

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

    fun addSubList(parentGroupId: Int, subListName: String) {
        _shoppingLists.update { currentLists ->
            val allLists = currentLists.flatMap { it.subLists ?: emptyList() } + currentLists
            val newId = (allLists.maxOfOrNull { it.id } ?: 0) + 1
            val updatedList = currentLists.map { list ->
                if (list.id == parentGroupId) {
                    val newList = ShoppingList(
                        id = newId,
                        name = subListName.trim(),
                        type = ListType.ITEM_LIST,
                        items = emptyList(),
                        subLists = emptyList()
                    )
                    list.copy(subLists = (list.subLists ?: emptyList()) + newList)
                } else {
                    list
                }
            }
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
                    list.copy(subLists = list.subLists?.map { subList ->
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
            val updatedLists = currentLists.map { list ->
                if (list.id == parentListId) {
                    val mutableSubLists = list.subLists?.toMutableList()
                    if (mutableSubLists != null) {
                        mutableSubLists.add(to, mutableSubLists.removeAt(from))
                        list.copy(subLists = mutableSubLists.toList())
                    } else {
                        list
                    }
                } else {
                    list
                }
            }
            repository.saveShoppingLists(updatedLists)
            updatedLists
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
