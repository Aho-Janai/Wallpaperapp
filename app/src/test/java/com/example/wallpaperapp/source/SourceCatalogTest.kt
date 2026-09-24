package com.example.wallpaperapp.source

import com.example.wallpaperapp.data.Wallpaper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceCatalogTest {
    @Test
    fun catalog_usesUniqueStableSourceIds() {
        val catalog = SourceCatalog()
        val ids = catalog.sources.map { it.id }

        assertEquals(ids.size, ids.distinct().size)
        assertTrue(ids.contains("demo-trending"))
        assertTrue(ids.contains("wallhaven"))
        assertTrue(ids.contains("wikimedia-commons"))
    }

    @Test
    fun extensionDescriptor_keepsAttributionAndLicenseMetadata() {
        val wallpaper = Wallpaper(
            id = "wikimedia-1",
            sourceId = "wikimedia-commons",
            thumbnailUrl = "https://example.com/thumb.jpg",
            fullResUrl = "https://example.com/full.jpg",
            width = 2204,
            height = 1142,
            tags = listOf("nature"),
            artist = "Forest Service Alaska Region, USDA",
            attributionText = "CC BY 2.0",
            attributionUrl = "https://commons.wikimedia.org/wiki/File:Madson_Mountain,_Alaska.jpg",
        )

        assertEquals("wikimedia-commons", wallpaper.sourceId)
        assertEquals("CC BY 2.0", wallpaper.attributionText)
        assertTrue(wallpaper.attributionUrl!!.contains("commons.wikimedia.org"))
    }

    @Test
    fun wikimediaSource_parsesCommonsApiResults() {
        val json = """
            {
              "query": {
                "pages": {
                  "112544910": {
                    "pageid": 112544910,
                    "ns": 6,
                    "title": "File:Abstract pattern on a tree stump.jpg",
                    "imagerepository": "local",
                    "imageinfo": [{
                      "thumburl": "https://thumb.wikimedia.org/wikipedia/commons/thumb/9/9b/Abstract_pattern_on_a_tree_stump.jpg/960px-Abstract_pattern_on_a_tree_stump.jpg",
                      "url": "https://upload.wikimedia.org/wikipedia/commons/9/9b/Abstract_pattern_on_a_tree_stump.jpg",
                      "descriptionurl": "https://commons.wikimedia.org/wiki/File:Abstract_pattern_on_a_tree_stump.jpg",
                      "width": 6000,
                      "height": 4000,
                      "mime": "image/jpeg"
                    }]
                  }
                }
              }
            }
        """.trimIndent()

        val wallpapers = SourceCatalog.WikimediaApiSource.parseResponse(json)

        assertTrue(wallpapers.isNotEmpty())
        assertEquals("wikimedia-commons", wallpapers.first().sourceId)
        assertTrue(wallpapers.first().thumbnailUrl.contains("thumb.wikimedia.org"))
        assertTrue(wallpapers.first().fullResUrl.contains("upload.wikimedia.org"))
    }

    @Test
    fun searchEnabledSources_returnsResultsAcrossCatalogSources() = kotlinx.coroutines.runBlocking {
        val catalog = SourceCatalog()
        catalog.setSourceEnabled("demo-trending", true)
        catalog.setSourceEnabled("wallhaven", true)
        catalog.setSourceEnabled("wikimedia-commons", true)

        val results = catalog.searchEnabledSources("abstract", limit = 10)

        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.sourceId.isNotBlank() })
    }
}
