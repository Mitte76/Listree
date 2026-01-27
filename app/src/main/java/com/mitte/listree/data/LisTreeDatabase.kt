package com.mitte.listree.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase


@Database(
    entities = [LisTreeList::class, LisTreeItem::class, ListShare::class],
    version = 4,
    exportSchema = false
)
abstract class LisTreeDatabase : RoomDatabase() {

    abstract fun lisTreeDao(): LisTreeDao

    companion object {
        @Volatile
        private var INSTANCE: LisTreeDatabase? = null

        fun getDatabase(context: Context): LisTreeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LisTreeDatabase::class.java,
                    "listree_database"

                )
                    .fallbackToDestructiveMigration(false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
