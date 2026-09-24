package com.example.wallpaperapp.source

import android.util.Log
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

data class PaintingsFilterSelection(
    val centuryCategory: String? = null,
    val movementCategory: String? = null,
    val artistCategory: String? = null,
)

data class PaintingsPage(
    val wallpapers: List<Wallpaper>,
    val hasMore: Boolean,
)

@Singleton
class SourceCatalog @Inject constructor() {
    private val wikimediaSource = WikimediaApiSource()
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

    suspend fun paintingsFeed(
        filters: PaintingsFilterSelection,
        page: Int,
        pageSize: Int = 50,
        randomize: Boolean = false,
    ): PaintingsPage = wikimediaSource.fetchPaintings(filters, page, pageSize, randomize)

    private fun builtInSources(): List<WallpaperSource> = listOf(
        DemoWallpaperSource(),
        WallhavenDemoSource(),
        wikimediaSource,
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

        suspend fun fetchPaintings(
            filters: PaintingsFilterSelection,
            page: Int,
            pageSize: Int = 50,
            randomize: Boolean = false,
        ): PaintingsPage {
            val safePage = page.coerceAtLeast(1)
            val offset = (safePage - 1) * pageSize
            val srsearch = buildPaintingsSearch(filters)
            val encodedSearch = URLEncoder.encode(srsearch, "UTF-8")
            val sort = if (randomize) "random" else "relevance"
            val url = "https://commons.wikimedia.org/w/api.php" +
                "?action=query&generator=search&gsrsearch=$encodedSearch" +
                "&gsrnamespace=6&gsrlimit=$pageSize&gsroffset=$offset" +
                "&gsrsort=$sort" +
                "&prop=imageinfo&iiprop=url|size|mime|extmetadata" +
                "&iiurlwidth=800&iiextmetadatalanguage=en" +
                "&format=json&origin=*"

            return runCatching {
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 10_000
                    readTimeout = 10_000
                    instanceFollowRedirects = true
                    setRequestProperty(
                        "User-Agent",
                        "WallpaperApp/1.0 (https://github.com/Aho-Janai/Wallpaperapp)"
                    )
                }

                val responseCode = connection.responseCode
                if (responseCode !in 200..299) {
                    val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() }
                    connection.disconnect()
                    error("Wikimedia API returned HTTP $responseCode: ${errorBody?.take(300)}")
                }

                val body = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()
                parsePaintingsResponse(body)
            }.onFailure { e ->
                Log.e("WikimediaApiSource", "fetchPaintings failed (page=$page, filters=$filters)", e)
            }.getOrDefault(PaintingsPage(emptyList(), hasMore = false))
        }

        private fun buildPaintingsSearch(filters: PaintingsFilterSelection): String {
            val clauses = mutableListOf("incategory:\"Paintings\"")
            filters.centuryCategory?.let { clauses += "incategory:\"$it\"" }
            filters.movementCategory?.let { clauses += "incategory:\"$it\"" }
            filters.artistCategory?.let { clauses += "incategory:\"$it\"" }
            return clauses.joinToString(" ")
        }

        private fun parsePaintingsResponse(json: String): PaintingsPage {
            val root = JSONObject(json)
            val query = root.optJSONObject("query") ?: return PaintingsPage(emptyList(), hasMore = false)
            val pages = query.optJSONObject("pages") ?: return PaintingsPage(emptyList(), hasMore = false)
            val keys = pages.names() ?: return PaintingsPage(emptyList(), hasMore = false)

            val wallpapers = (0 until keys.length()).mapNotNull { index ->
                val key = keys.getString(index)
                val page = pages.optJSONObject(key) ?: return@mapNotNull null
                val infos = page.optJSONArray("imageinfo") ?: return@mapNotNull null
                val info = infos.optJSONObject(0) ?: return@mapNotNull null
                val mime = info.optString("mime")
                if (!mime.startsWith("image/")) return@mapNotNull null

                val thumb = info.optString("thumburl", "").ifBlank { info.optString("url", "") }
                val full = info.optString("url", "").ifBlank { thumb }
                if (full.isBlank()) return@mapNotNull null

                val artistRaw = info.optJSONObject("extmetadata")
                    ?.optJSONObject("Artist")
                    ?.optString("value")
                val artist = artistRaw
                    ?.replace(Regex("(?s)<[^>]*>"), " ")
                    ?.replace(Regex("\\s+"), " ")
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }

                val title = page.optString("title", "").removePrefix("File:").trim()
                val descriptionUrl = info.optString("descriptionurl", "")

                Wallpaper(
                    id = "wikimedia-${page.optLong("pageid", 0L)}",
                    sourceId = WIKIMEDIA_SOURCE_ID,
                    thumbnailUrl = thumb,
                    fullResUrl = full,
                    width = info.optInt("width", 0).takeIf { it > 0 },
                    height = info.optInt("height", 0).takeIf { it > 0 },
                    tags = listOfNotNull(
                        title.ifBlank { "Wikimedia painting" },
                        "wikimedia",
                        "painting",
                    ),
                    artist = artist,
                    attributionText = artist ?: "Wikimedia Commons",
                    attributionUrl = descriptionUrl.ifBlank { null },
                )
            }

            val hasMore = root.has("continue")
            return PaintingsPage(wallpapers, hasMore)
        }

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
                    setRequestProperty(
                        "User-Agent",
                        "WallpaperApp/1.0 (https://github.com/Aho-Janai/Wallpaperapp)"
                    )
                }

                val responseCode = connection.responseCode
                if (responseCode !in 200..299) {
                    val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() }
                    connection.disconnect()
                    error("Wikimedia API returned HTTP $responseCode: ${errorBody?.take(300)}")
                }
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()
                parseResponse(body)
            }.onFailure { e ->
                Log.e("WikimediaApiSource", "Wikimedia fetch failed for query='$query' page=$page", e)
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
