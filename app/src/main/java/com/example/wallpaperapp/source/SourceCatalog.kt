package com.example.wallpaperapp.source

import com.example.wallpaperapp.data.Wallpaper
import com.example.wallpaperapp.data.WallpaperRepository
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.json.JSONObject

private const val DEMO_SOURCE_ID = "demo-trending"
private const val WALLHAVEN_SOURCE_ID = "wallhaven"
private const val WIKIMEDIA_SOURCE_ID = "wikimedia-commons"

data class ExtensionDescriptor(
    val id: String,
    val sourceId: String,
    val name: String,
    val packageName: String,
    val developer: String = "Wallpaper App",
    val apiVersion: Int = 1,
    val isTrusted: Boolean = false,
    val isEnabled: Boolean = true,
    val isOfficialApi: Boolean = true,
) {
    val status: String
        get() = when {
            !isTrusted -> "untrusted"
            !isEnabled -> "disabled"
            apiVersion != 1 -> "incompatible"
            else -> "trusted"
        }
}

data class SourceStatus(
    val id: String,
    val displayName: String,
    val isEnabled: Boolean,
    val isOfficialApi: Boolean,
    val type: String,
)

@Singleton
class SourceCatalog @Inject constructor() {
    private var extensionSources: List<WallpaperSource> = emptyList()
    private val sourceEnabledState = linkedMapOf<String, Boolean>()

    val sources: List<WallpaperSource>
        get() = buildList {
            addAll(builtInSources())
            addAll(extensionSources)
        }

    fun reviewSources(): List<SourceStatus> = sources.map { source ->
        SourceStatus(
            id = source.id,
            displayName = source.displayName,
            isEnabled = isSourceEnabled(source.id),
            isOfficialApi = source.isOfficialApi,
            type = if (source is ExtensionWallpaperSource) "extension" else "built-in",
        )
    }

    fun isSourceEnabled(sourceId: String): Boolean = sourceEnabledState[sourceId] ?: true

    fun setSourceEnabled(sourceId: String, enabled: Boolean) {
        sourceEnabledState[sourceId] = enabled
    }

    fun registerExtension(descriptor: ExtensionDescriptor) {
        val next = ExtensionWallpaperSource(descriptor)
        extensionSources = extensionSources
            .filterNot { it.id == descriptor.sourceId }
            .plus(next)
    }

    fun unregisterExtension(sourceId: String) {
        extensionSources = extensionSources.filterNot { it.id == sourceId }
    }

    fun setExtensionEnabled(sourceId: String, enabled: Boolean) {
        setSourceEnabled(sourceId, enabled)
    }

    fun allSourceIdsAreUnique(): Boolean = sources.map { it.id }.distinct().size == sources.size

    suspend fun searchEnabledSources(
        query: String = "abstract",
        page: Int = 1,
        limit: Int = 50,
    ): List<Wallpaper> = coroutineScope {
        val activeSources = sources.filter { source -> isSourceEnabled(source.id) }
        val results = activeSources.map { source ->
            async {
                val searchResults = if (query.isBlank()) source.getPopular(page) else source.search(query, page)
                searchResults.filter { wallpaper ->
                    if (query.equals("abstract", ignoreCase = true) && wallpaper.tags.isNotEmpty()) {
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

    suspend fun abstractFeed(page: Int = 1, limit: Int = 50): List<Wallpaper> =
        searchEnabledSources(query = "abstract", page = page, limit = limit)

    private fun builtInSources(): List<WallpaperSource> = listOf(
        DemoWallpaperSource(),
        WallhavenDemoSource(),
        WikimediaApiSource(),
    )

    class DemoWallpaperSource : ApiWallpaperSource() {
        override val id: String = DEMO_SOURCE_ID
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
        override val id: String = WALLHAVEN_SOURCE_ID
        override val displayName: String = "Wallhaven"

        override suspend fun getPopular(page: Int): List<Wallpaper> = WallpaperRepository.demoWallpapers

        override suspend fun search(query: String, page: Int): List<Wallpaper> =
            WallpaperRepository.demoWallpapers.filter {
                it.sourceId == WALLHAVEN_SOURCE_ID || it.tags.any { tag -> tag.contains(query, ignoreCase = true) }
            }
    }

    class WikimediaApiSource : ApiWallpaperSource() {
        override val id: String = WIKIMEDIA_SOURCE_ID
        override val displayName: String = "Wikimedia Commons"

        override suspend fun getPopular(page: Int): List<Wallpaper> = fetchWikimedia(query = "abstract", page = page)

        override suspend fun search(query: String, page: Int): List<Wallpaper> = fetchWikimedia(query = query, page = page)

        private suspend fun fetchWikimedia(query: String, page: Int): List<Wallpaper> {
            val encoded = URLEncoder.encode(query.ifBlank { "abstract" }, "UTF-8")
            val offset = (page - 1) * 10
            val url = "https://commons.wikimedia.org/w/api.php?action=query&generator=search&gsrsearch=$encoded&gsrnamespace=6&gsrlimit=10&gsroffset=$offset&prop=imageinfo&iiprop=url|size|mime&iiurlwidth=800&format=json&origin=*"
            return runCatching {
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 10_000
                    readTimeout = 10_000
                    instanceFollowRedirects = true
                }

                val body = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()
                parseResponse(body)
            }.getOrDefault(emptyList())
        }

        companion object {
            fun parseResponse(json: String): List<Wallpaper> {
                val root = JSONObject(json)
                val query = root.optJSONObject("query") ?: return emptyList()
                val pages = query.optJSONObject("pages") ?: return emptyList()
                val keys = pages.names() ?: return emptyList()

                return (0 until keys.length()).mapNotNull { index ->
                    val key = keys.getString(index)
                    val page = pages.optJSONObject(key) ?: return@mapNotNull null
                    val infos = page.optJSONArray("imageinfo") ?: return@mapNotNull null
                    val info = infos.optJSONObject(0) ?: return@mapNotNull null
                    val thumb = info.optString("thumburl", "").ifBlank { info.optString("url", "") }
                    val full = info.optString("url", "").ifBlank { thumb }
                    val width = info.optInt("width", 0)
                    val height = info.optInt("height", 0)
                    val title = page.optString("title", "")
                    val descriptionUrl = info.optString("descriptionurl", "")
                    val tags = listOfNotNull(
                        title.substringAfter("File:").substringBeforeLast(".").replace("_", " ").takeIf { it.isNotBlank() },
                        if (descriptionUrl.contains("commons.wikimedia.org")) "wikimedia" else null,
                    )

                    Wallpaper(
                        id = "wikimedia-${page.optLong("pageid", 0L)}",
                        sourceId = WIKIMEDIA_SOURCE_ID,
                        thumbnailUrl = thumb,
                        fullResUrl = full,
                        width = width,
                        height = height,
                        tags = tags,
                        artist = null,
                        attributionText = if (descriptionUrl.isNotBlank()) "Wikimedia Commons" else null,
                        attributionUrl = descriptionUrl.ifBlank { null },
                    )
                }
            }
        }
    }
}

private data class ExtensionWallpaperSource(
    private val descriptor: ExtensionDescriptor,
    var enabled: Boolean = true,
) : ApiWallpaperSource() {
    override val id: String = descriptor.sourceId
    override val displayName: String = descriptor.name
    override val isOfficialApi: Boolean = descriptor.isOfficialApi

    override suspend fun getPopular(page: Int): List<Wallpaper> = WallpaperRepository.demoWallpapers.filter {
        it.sourceId == descriptor.sourceId || it.sourceId == WALLHAVEN_SOURCE_ID || it.sourceId == WIKIMEDIA_SOURCE_ID
    }

    override suspend fun search(query: String, page: Int): List<Wallpaper> = WallpaperRepository.demoWallpapers.filter {
        it.sourceId == descriptor.sourceId ||
            it.tags.any { tag -> tag.contains(query, ignoreCase = true) } ||
            it.attributionText?.contains(query, ignoreCase = true) == true
    }
}
