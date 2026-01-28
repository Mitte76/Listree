package com.mitte.listree.data

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("listree_prefs", Context.MODE_PRIVATE)

    fun getShowDeleted(): Boolean {
        return sharedPreferences.getBoolean(KEY_SHOW_DELETED, false)
    }

    fun setShowDeleted(show: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_SHOW_DELETED, show).apply()
    }

    companion object {
        private const val KEY_SHOW_DELETED = "show_deleted"
    }
}
