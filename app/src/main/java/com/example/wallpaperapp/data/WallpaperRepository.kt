package com.example.wallpaperapp.data

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WallpaperRepository @Inject constructor(
    private val savedItemDao: SavedItemDao,
) {
    fun wallpapers(): List<Wallpaper> = demoWallpapers

    suspend fun getSavedItems(kind: SaveKind? = null): List<SavedItemEntity> =
        if (kind == null) savedItemDao.getAll() else savedItemDao.getByKind(kind)

    suspend fun saveWallpaper(item: SavedItemEntity): Long = savedItemDao.insert(item)

    companion object {
        val demoWallpapers = listOf(
            Wallpaper(
                id = "1",
                sourceId = "wallhaven",
                thumbnailUrl = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?auto=format&fit=crop&w=900&q=80",
                fullResUrl = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?auto=format&fit=crop&w=2000&q=80",
                width = 1920,
                height = 1080,
                tags = listOf("abstract", "modern", "minimal"),
                artist = "Nora Hale",
                attributionText = "Photo by Nora Hale",
                attributionUrl = "https://unsplash.com"
            ),
            Wallpaper(
                id = "2",
                sourceId = "demo",
                thumbnailUrl = "https://images.unsplash.com/photo-1493246507139-91e8fad9978e?auto=format&fit=crop&w=900&q=80",
                fullResUrl = "https://images.unsplash.com/photo-1493246507139-91e8fad9978e?auto=format&fit=crop&w=2000&q=80",
                width = 1200,
                height = 1800,
                tags = listOf("abstract", "forest"),
                artist = "Milo Stone",
                attributionText = "Photo by Milo Stone",
                attributionUrl = "https://unsplash.com"
            ),
            Wallpaper(
                id = "3",
                sourceId = "unsplash",
                thumbnailUrl = "https://images.unsplash.com/photo-1501785888041-af3ef285b470?auto=format&fit=crop&w=900&q=80",
                fullResUrl = "https://images.unsplash.com/photo-1501785888041-af3ef285b470?auto=format&fit=crop&w=2000&q=80",
                width = 1600,
                height = 900,
                tags = listOf("coastal", "abstract"),
                artist = "Lina Cross",
                attributionText = "Photo by Lina Cross",
                attributionUrl = "https://unsplash.com"
            ),
            Wallpaper(
                id = "4",
                sourceId = "demo",
                thumbnailUrl = "https://images.unsplash.com/photo-1470770841072-f978cf4d019e?auto=format&fit=crop&w=900&q=80",
                fullResUrl = "https://images.unsplash.com/photo-1470770841072-f978cf4d019e?auto=format&fit=crop&w=2000&q=80",
                width = 1440,
                height = 960,
                tags = listOf("sunset", "abstract"),
                artist = "Rae Ellison",
                attributionText = "Photo by Rae Ellison",
                attributionUrl = "https://pexels.com"
            ),
            Wallpaper(
                id = "5",
                sourceId = "wallhaven",
                thumbnailUrl = "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=900&q=80",
                fullResUrl = "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=2000&q=80",
                width = 1600,
                height = 1067,
                tags = listOf("desert", "abstract"),
                artist = "Hugo Vale",
                attributionText = "Photo by Hugo Vale",
                attributionUrl = "https://wallhaven.cc"
            ),
            Wallpaper(
                id = "6",
                sourceId = "unsplash",
                thumbnailUrl = "https://images.unsplash.com/photo-1514565131-fce0801e5785?auto=format&fit=crop&w=900&q=80",
                fullResUrl = "https://images.unsplash.com/photo-1514565131-fce0801e5785?auto=format&fit=crop&w=2000&q=80",
                width = 2100,
                height = 1400,
                tags = listOf("city", "abstract"),
                artist = "Tara Mendez",
                attributionText = "Photo by Tara Mendez",
                attributionUrl = "https://unsplash.com"
            )
        )
    }
}
