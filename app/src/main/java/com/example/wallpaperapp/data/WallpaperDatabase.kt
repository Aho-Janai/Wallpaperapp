package com.example.wallpaperapp.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class SaveKindConverter {
    @TypeConverter
    fun fromSaveKind(kind: SaveKind): String = kind.name

    @TypeConverter
    fun toSaveKind(value: String): SaveKind = SaveKind.valueOf(value)
}

@Database(
    entities = [SavedItemEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(SaveKindConverter::class)
abstract class WallpaperDatabase : RoomDatabase() {
    abstract fun savedItemDao(): SavedItemDao
}
