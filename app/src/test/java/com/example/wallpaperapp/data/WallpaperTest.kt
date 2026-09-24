package com.example.wallpaperapp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WallpaperTest {
    @Test
    fun aspectRatioLabel_usesReducedFraction() {
        val wallpaper = Wallpaper(
            id = "1",
            sourceId = "wallhaven",
            thumbnailUrl = "https://example.com/thumb.jpg",
            fullResUrl = "https://example.com/full.jpg",
            width = 1920,
            height = 1080,
            tags = listOf("abstract", "minimal")
        )

        assertEquals("16:9", wallpaper.aspectRatioLabel())
    }

    @Test
    fun abstractFiltering_detectsAbstractTagCaseInsensitive() {
        val wallpaper = Wallpaper(
            id = "2",
            sourceId = "demo",
            thumbnailUrl = "https://example.com/thumb.jpg",
            fullResUrl = "https://example.com/full.jpg",
            width = 200,
            height = 300,
            tags = listOf("COLORFUL", "ABSTRACT")
        )

        assertTrue(wallpaper.matchesAbstractSearchFilter())
    }
}
