package com.mitte.shopper

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.mitte.shopper.data.ShoppingRepository
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

    fun addList(listName: String) {
        _shoppingLists.update { currentLists ->
            val newId = (currentLists.maxOfOrNull { it.id } ?: 0) + 1

            val newList = ShoppingList(
                id = newId,
                name = listName.trim(),
                items = emptyList()
            )

            val updatedList = currentLists + newList
            repository.saveShoppingLists(updatedList)
            updatedList
        }
    }

    fun deleteList(listId: Int) {
        _shoppingLists.update { currentLists ->
            // Filter out the list that matches the given id
            val updatedList = currentLists.filterNot { it.id == listId }
            repository.saveShoppingLists(updatedList) // <-- SAVE TO PREFS
            updatedList
        }
    }

    fun editListName(listId: Int, newName: String) {
        _shoppingLists.update { currentLists ->
            val updatedList = currentLists.map { list ->
                // Find the list to edit and update its name
                if (list.id == listId) {
                    list.copy(name = newName.trim())
                } else {
                    list
                }
            }
            repository.saveShoppingLists(updatedList) // <-- SAVE TO PREFS
            updatedList
        }
    }

    fun moveList(from: Int, to: Int) {
        _shoppingLists.update { currentLists ->
            // Create a mutable copy to perform the move operation
            val mutableList = currentLists.toMutableList()
            mutableList.add(to, mutableList.removeAt(from))

            // The updated list that will be saved
            val updatedList = mutableList.toList()
            repository.saveShoppingLists(updatedList) // <-- SAVE TO PREFS
            updatedList
        }
    }

    fun moveItem(listId: Int, from: Int, to: Int) {
        _shoppingLists.update { currentLists ->
            val updatedList = currentLists.map { list ->
                if (list.id == listId) {
                    val mutableItems = list.items.toMutableList()
                    mutableItems.add(to, mutableItems.removeAt(from))
                    list.copy(items = mutableItems.toList())
                } else {
                    list
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
                    val updatedItems = list.items.map { item ->
                        if (item.id == itemToToggle.id) {
                            item.copy(isChecked = !item.isChecked)
                        } else {
                            item
                        }
                    }
                    list.copy(items = updatedItems)
                } else {
                    list
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
                    // Create a unique ID for the new item.
                    // This is a simple approach; for a real app, consider UUIDs.
                    val newId = (list.items.maxOfOrNull { it.id } ?: (listId * 1000)) + 1
                    val newItem = ShoppingItem(id = newId, name = itemName.trim())
                    // Add the new item to this list's items
                    list.copy(items = list.items + newItem)
                } else {
                    list
                }
            }
            repository.saveShoppingLists(updatedList) // <-- SAVE TO PREFS
            updatedList
        }
    }

    fun editItemName(listId: Int, itemId: Int, newItemName: String) {
        _shoppingLists.update { currentLists ->
            val updatedList = currentLists.map { list ->
                if (list.id == listId) {
                    val updatedItems = list.items.map { item ->
                        if (item.id == itemId) {
                            // Edit the name of the matching item
                            item.copy(name = newItemName.trim())
                        } else {
                            item
                        }
                    }
                    list.copy(items = updatedItems)
                } else {
                    list
                }
            }
            repository.saveShoppingLists(updatedList) // <-- SAVE TO PREFS
            updatedList
        }
    }

    fun removeItem(listId: Int, itemToRemove: ShoppingItem) {
        _shoppingLists.update { currentLists ->
            val updatedList = currentLists.map { list ->
                if (list.id == listId) {
                    val updatedItems = list.items.filterNot { it.id == itemToRemove.id }
                    list.copy(items = updatedItems)
                } else {
                    list
                }
            }
            repository.saveShoppingLists(updatedList)
            updatedList
        }
    }

}