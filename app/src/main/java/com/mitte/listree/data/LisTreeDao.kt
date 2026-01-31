package com.mitte.listree.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface LisTreeDao {

    @Query("SELECT * FROM shopping_lists WHERE deleted = 0 ORDER BY `order` ASC")
    fun getShoppingLists(): Flow<List<LisTreeList>>

    @Query("SELECT * FROM shopping_lists ORDER BY `order` ASC")
    fun getShoppingListsWithDeleted(): Flow<List<LisTreeList>>

    @Query("SELECT * FROM shopping_items WHERE listId = :listId AND deleted = 0 ORDER BY `order` ASC")
    fun getShoppingItems(listId: String): Flow<List<LisTreeItem>>

    @Query("SELECT * FROM shopping_items WHERE listId = :listId ORDER BY `order` ASC")
    fun getShoppingItemsWithDeleted(listId: String): Flow<List<LisTreeItem>>

    @Upsert
    suspend fun upsertShoppingList(list: LisTreeList)

    @Upsert
    suspend fun upsertShoppingItem(item: LisTreeItem)

    @Query("DELETE FROM shopping_items WHERE deleted = 1")
    suspend fun deleteMarkedItems()

    @Query("DELETE FROM shopping_lists WHERE deleted = 1")
    suspend fun deleteMarkedLists()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertListShare(listShare: ListShare)

    @Delete
    suspend fun deleteListShare(listShare: ListShare)

    @Query("SELECT * FROM shopping_lists WHERE id IN (SELECT listId FROM list_shares WHERE userId = :userId)")
    fun getSharedLists(userId: String): Flow<List<LisTreeList>>
}
