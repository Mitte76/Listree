package com.mitte.listree.data.converters

import androidx.room.TypeConverter
import com.mitte.listree.ui.models.ListType

class ListTypeConverter {
    @TypeConverter
    fun fromListType(listType: ListType?): String? {
        return listType?.name
    }

    @TypeConverter
    fun toListType(name: String?): ListType? {
        return name?.let { ListType.valueOf(it) }
    }
}
