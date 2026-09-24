package com.example.wallpaperapp.source

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SourceRegistry @Inject constructor() {
    val sources: List<WallpaperSource> = listOf(DemoWallpaperSource())

    class DemoWallpaperSource : ApiWallpaperSource() {
        override val id: String = "demo"
        override val displayName: String = "Trending"

        override suspend fun getPopular(page: Int): List<com.example.wallpaperapp.data.Wallpaper> =
            com.example.wallpaperapp.data.WallpaperRepository.demoWallpapers

        override suspend fun search(query: String, page: Int): List<com.example.wallpaperapp.data.Wallpaper> =
            com.example.wallpaperapp.data.WallpaperRepository.demoWallpapers.filter {
                it.title.contains(query, ignoreCase = true) || it.sourceName.contains(query, ignoreCase = true)
            }
    }
}
