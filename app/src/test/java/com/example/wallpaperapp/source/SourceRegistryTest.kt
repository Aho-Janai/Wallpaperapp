package com.example.wallpaperapp.source

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class SourceRegistryTest {
    @Test
    fun abstractFeed_deduplicatesWallpaperIds() = runBlocking {
        val registry = SourceRegistry()

        val feed = registry.abstractFeed()

        assertEquals(feed.size, feed.map { it.id }.distinct().size)
    }
}
