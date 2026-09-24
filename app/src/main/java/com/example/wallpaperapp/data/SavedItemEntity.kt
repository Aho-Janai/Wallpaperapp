package com.example.wallpaperapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_items")
data class SavedItemEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val wallpaperId: String,
    val sourceId: String,
    val kind: SaveKind,
    val savedAt: Long = System.currentTimeMillis(),
    val thumbnailUrl: String,
    val fullResUrl: String,
    val width: Int? = null,
    val height: Int? = null,
    val artist: String? = null,
)
