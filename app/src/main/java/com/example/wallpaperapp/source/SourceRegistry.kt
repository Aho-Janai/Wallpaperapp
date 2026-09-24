package com.example.wallpaperapp.source

import com.example.wallpaperapp.data.Wallpaper
import com.example.wallpaperapp.data.WallpaperRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

@Singleton
class SourceRegistry @Inject constructor() {
    val sources: List<WallpaperSource> = listOf(
        DemoWallpaperSource(),
        WallhavenDemoSource(),
        UnsplashDemoSource(),
    )

    suspend fun abstractFeed(page: Int = 1, limit: Int = 50): List<Wallpaper> = coroutineScope {
        val results = sources.map { source ->
            async {
                source.search(query = "abstract", page = page)
                    .filter { wallpaper ->
                        if (wallpaper.tags.isNotEmpty()) {
                            val hasAbstractTag = wallpaper.tags.any { it.contains("abstract", ignoreCase = true) }
                            if (!hasAbstractTag) return@filter false
                        }
                        if (source.id.equals("scraped", ignoreCase = true)) {
                            return@filter !wallpaper.containsAiHeuristicMarker()
                        }
                        true
                    }
            }
        }

        results.awaitAll()
            .flatten()
            .distinctBy { it.id }
            .shuffled()
            .take(limit)
    }

    class DemoWallpaperSource : ApiWallpaperSource() {
        override val id: String = "demo"
        override val displayName: String = "Trending"

        override suspend fun getPopular(page: Int): List<Wallpaper> = WallpaperRepository.demoWallpapers

        override suspend fun search(query: String, page: Int): List<Wallpaper> =
            WallpaperRepository.demoWallpapers.filter {
                it.tags.any { tag -> tag.contains(query, ignoreCase = true) } ||
                    it.sourceId.contains(query, ignoreCase = true) ||
                    it.title.contains(query, ignoreCase = true)
            }
    }

    class WallhavenDemoSource : ApiWallpaperSource() {
        override val id: String = "wallhaven"
        override val displayName: String = "Wallhaven"

        override suspend fun getPopular(page: Int): List<Wallpaper> = WallpaperRepository.demoWallpapers

        override suspend fun search(query: String, page: Int): List<Wallpaper> =
            WallpaperRepository.demoWallpapers.filter {
                it.sourceId == "wallhaven" || it.tags.any { tag -> tag.contains(query, ignoreCase = true) }
            }
    }

    class UnsplashDemoSource : ApiWallpaperSource() {
        override val id: String = "unsplash"
        override val displayName: String = "Unsplash"

        override suspend fun getPopular(page: Int): List<Wallpaper> = WallpaperRepository.demoWallpapers

        override suspend fun search(query: String, page: Int): List<Wallpaper> =
            WallpaperRepository.demoWallpapers.filter {
                it.sourceId == "unsplash" || it.tags.any { tag -> tag.contains(query, ignoreCase = true) }
            }
    }
}
