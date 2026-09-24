package com.example.wallpaperapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SavedItemDao {
    @Query("SELECT * FROM saved_items ORDER BY savedAt DESC")
    suspend fun getAll(): List<SavedItemEntity>

    @Query("SELECT * FROM saved_items WHERE kind = :kind ORDER BY savedAt DESC")
    suspend fun getByKind(kind: SaveKind): List<SavedItemEntity>

    @Insert
    suspend fun insert(item: SavedItemEntity): Long
}
