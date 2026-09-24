package com.example.wallpaperapp.source

import com.example.wallpaperapp.data.Wallpaper
import javax.inject.Singleton

@Singleton
class SourceRegistry constructor(
    private val sourceCatalog: SourceCatalog = SourceCatalog(),
) {
    val sources: List<WallpaperSource>
        get() = sourceCatalog.sources

    suspend fun abstractFeed(page: Int = 1, limit: Int = 50): List<Wallpaper> =
        sourceCatalog.abstractFeed(page, limit)
}
