package com.mitte.listree.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class PreferencesManager(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("listree_prefs", Context.MODE_PRIVATE)

    fun getShowDeleted(): Boolean {
        return sharedPreferences.getBoolean(KEY_SHOW_DELETED, false)
    }

    fun setShowDeleted(show: Boolean) {
        sharedPreferences.edit { putBoolean(KEY_SHOW_DELETED, show) }
    }

    fun getExpandedListIds(): Set<String> {
        return sharedPreferences.getStringSet(KEY_EXPANDED_LIST_IDS, emptySet()) ?: emptySet()
    }

    fun saveExpandedListIds(ids: Set<String>) {
        sharedPreferences.edit { putStringSet(KEY_EXPANDED_LIST_IDS, ids) }
    }

    companion object {
        private const val KEY_SHOW_DELETED = "show_deleted"
        private const val KEY_EXPANDED_LIST_IDS = "expanded_list_ids"
    }
}
