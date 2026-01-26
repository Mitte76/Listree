package com.mitte.shopper.data.converters

import androidx.room.TypeConverter
import com.mitte.shopper.ui.models.ListType

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
