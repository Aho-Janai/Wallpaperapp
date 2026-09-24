package com.example.wallpaperapp.source

import com.example.wallpaperapp.data.Wallpaper

interface WallpaperSource {
    val id: String
    val displayName: String
    suspend fun getPopular(page: Int): List<Wallpaper>
    suspend fun search(query: String, page: Int): List<Wallpaper>
}

abstract class ApiWallpaperSource : WallpaperSource {
    override val id: String
        get() = javaClass.simpleName
}

abstract class ScrapedWallpaperSource : WallpaperSource {
    override val id: String
        get() = javaClass.simpleName
}
