package com.mitte.listree.data

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
interface LisTreeDao {

    @Transaction
    @Query("SELECT * FROM shopping_lists ORDER BY `order` ASC")
    fun getShoppingListsWithItems(): Flow<List<ShoppingListWithItems>>

    @Query("SELECT * FROM shopping_lists WHERE parentId = :listId ORDER BY `order` ASC")
    fun getSubLists(listId: String): Flow<List<LisTreeList>>

    @Query("SELECT * FROM shopping_items WHERE listId = :listId ORDER BY `order` ASC")
    fun getShoppingItems(listId: String): Flow<List<LisTreeItem>>

    @Upsert
    suspend fun upsertShoppingList(list: LisTreeList)

    @Update
    suspend fun updateShoppingList(list: LisTreeList)

    @Delete
    suspend fun deleteShoppingList(list: LisTreeList)

    @Query("DELETE FROM shopping_lists")
    suspend fun deleteAllShoppingLists()

    @Upsert
    suspend fun upsertShoppingItem(item: LisTreeItem)

    @Update
    suspend fun updateShoppingItem(item: LisTreeItem)

    @Delete
    suspend fun deleteShoppingItem(item: LisTreeItem)

    @Query("UPDATE shopping_items SET deleted = 1, lastModified = :timestamp WHERE id = :itemId")
    suspend fun removeItem(itemId: String, timestamp: Long)

    @Query("DELETE FROM shopping_items")
    suspend fun deleteAllShoppingItems()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertListShare(listShare: ListShare)

    @Delete
    suspend fun deleteListShare(listShare: ListShare)

    @Query("SELECT * FROM shopping_lists WHERE id IN (SELECT listId FROM list_shares WHERE userId = :userId)")
    fun getSharedLists(userId: String): Flow<List<LisTreeList>>
}
