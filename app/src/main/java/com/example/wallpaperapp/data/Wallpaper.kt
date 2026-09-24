package com.example.wallpaperapp.data

data class Wallpaper(
    val id: String,
    val title: String,
    val imageUrl: String,
    val sourceName: String,
    val aspectRatio: Float = 1f
)
