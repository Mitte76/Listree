package com.mitte.shopper.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingDao {

    @Transaction
    @Query("SELECT * FROM shopping_lists ORDER BY `order` ASC")
    fun getShoppingListsWithItems(): Flow<List<ShoppingListWithItems>>

    @Query("SELECT * FROM shopping_lists WHERE parentId = :listId ORDER BY `order` ASC")
    fun getSubLists(listId: String): Flow<List<ShoppingList>>

    @Query("SELECT * FROM shopping_items WHERE listId = :listId ORDER BY `order` ASC")
    fun getShoppingItems(listId: String): Flow<List<ShoppingItem>>

    @Upsert
    suspend fun upsertShoppingList(list: ShoppingList)

    @Update
    suspend fun updateShoppingList(list: ShoppingList)

    @Delete
    suspend fun deleteShoppingList(list: ShoppingList)

    @Query("DELETE FROM shopping_lists")
    suspend fun deleteAllShoppingLists()

    @Upsert
    suspend fun upsertShoppingItem(item: ShoppingItem)

    @Update
    suspend fun updateShoppingItem(item: ShoppingItem)

    @Delete
    suspend fun deleteShoppingItem(item: ShoppingItem)

    @Query("DELETE FROM shopping_items")
    suspend fun deleteAllShoppingItems()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertListShare(listShare: ListShare)

    @Delete
    suspend fun deleteListShare(listShare: ListShare)

    @Query("SELECT * FROM shopping_lists WHERE id IN (SELECT listId FROM list_shares WHERE userId = :userId)")
    fun getSharedLists(userId: String): Flow<List<ShoppingList>>
}
