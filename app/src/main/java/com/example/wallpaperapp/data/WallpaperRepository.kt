package com.example.wallpaperapp.data

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WallpaperRepository @Inject constructor() {
    fun wallpapers(): List<Wallpaper> = demoWallpapers

    companion object {
        val demoWallpapers = listOf(
            Wallpaper(
                id = "1",
                title = "Aurora nights",
                imageUrl = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?auto=format&fit=crop&w=900&q=80",
                sourceName = "Unsplash",
                aspectRatio = 0.72f
            ),
            Wallpaper(
                id = "2",
                title = "Forest mist",
                imageUrl = "https://images.unsplash.com/photo-1493246507139-91e8fad9978e?auto=format&fit=crop&w=900&q=80",
                sourceName = "Unsplash",
                aspectRatio = 1.3f
            ),
            Wallpaper(
                id = "3",
                title = "Coastal calm",
                imageUrl = "https://images.unsplash.com/photo-1501785888041-af3ef285b470?auto=format&fit=crop&w=900&q=80",
                sourceName = "Unsplash",
                aspectRatio = 0.8f
            ),
            Wallpaper(
                id = "4",
                title = "Sunset glow",
                imageUrl = "https://images.unsplash.com/photo-1470770841072-f978cf4d019e?auto=format&fit=crop&w=900&q=80",
                sourceName = "Pexels",
                aspectRatio = 1.45f
            ),
            Wallpaper(
                id = "5",
                title = "Golden dunes",
                imageUrl = "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=900&q=80",
                sourceName = "Wallhaven",
                aspectRatio = 0.77f
            ),
            Wallpaper(
                id = "6",
                title = "City horizon",
                imageUrl = "https://images.unsplash.com/photo-1514565131-fce0801e5785?auto=format&fit=crop&w=900&q=80",
                sourceName = "Unsplash",
                aspectRatio = 1.2f
            ),
            Wallpaper(
                id = "7",
                title = "Minimal abstract",
                imageUrl = "https://images.unsplash.com/photo-1519681393784-d120267933ba?auto=format&fit=crop&w=900&q=80",
                sourceName = "Pexels",
                aspectRatio = 0.7f
            ),
            Wallpaper(
                id = "8",
                title = "Low light studio",
                imageUrl = "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=900&q=80",
                sourceName = "Wallhaven",
                aspectRatio = 1.52f
            ),
            Wallpaper(
                id = "9",
                title = "Desert air",
                imageUrl = "https://images.unsplash.com/photo-1470770841072-f978cf4d019e?auto=format&fit=crop&w=900&q=80",
                sourceName = "Unsplash",
                aspectRatio = 0.9f
            ),
            Wallpaper(
                id = "10",
                title = "Quiet lake",
                imageUrl = "https://images.unsplash.com/photo-1501785888041-af3ef285b470?auto=format&fit=crop&w=900&q=80",
                sourceName = "Wallhaven",
                aspectRatio = 1.38f
            ),
            Wallpaper(
                id = "11",
                title = "Night sky",
                imageUrl = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?auto=format&fit=crop&w=900&q=80",
                sourceName = "Unsplash",
                aspectRatio = 0.68f
            ),
            Wallpaper(
                id = "12",
                title = "Mountain air",
                imageUrl = "https://images.unsplash.com/photo-1493246507139-91e8fad9978e?auto=format&fit=crop&w=900&q=80",
                sourceName = "Pexels",
                aspectRatio = 1.55f
            )
        )
    }
}
