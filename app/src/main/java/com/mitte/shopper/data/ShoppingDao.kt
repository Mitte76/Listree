package com.mitte.shopper.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingDao {

    @Query("SELECT * FROM shopping_lists WHERE parentId IS NULL")
    fun getShoppingLists(): Flow<List<ShoppingList>>

    @Query("SELECT * FROM shopping_lists WHERE parentId = :listId")
    fun getSubLists(listId: String): Flow<List<ShoppingList>>

    @Query("SELECT * FROM shopping_items WHERE listId = :listId")
    fun getShoppingItems(listId: String): Flow<List<ShoppingItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingList(list: ShoppingList)

    @Update
    suspend fun updateShoppingList(list: ShoppingList)

    @Delete
    suspend fun deleteShoppingList(list: ShoppingList)

    @Query("DELETE FROM shopping_lists")
    suspend fun deleteAllShoppingLists()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingItem(item: ShoppingItem)

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
