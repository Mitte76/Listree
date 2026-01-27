package com.mitte.listree.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mitte.listree.data.converters.ListTypeConverter

@Database(entities = [LisTreeList::class, LisTreeItem::class, ListShare::class], version = 3, exportSchema = false)
@TypeConverters(ListTypeConverter::class)
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
                    "shopping_database"
//                    "listree_database"

                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
