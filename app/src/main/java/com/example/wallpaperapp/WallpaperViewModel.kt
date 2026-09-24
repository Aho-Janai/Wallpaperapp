package com.example.wallpaperapp

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wallpaperapp.data.SaveKind
import com.example.wallpaperapp.data.SavedItemEntity
import com.example.wallpaperapp.data.SettingsStore
import com.example.wallpaperapp.data.Wallpaper
import com.example.wallpaperapp.data.WallpaperRepository
import com.example.wallpaperapp.source.PaintingsFilterSelection
import com.example.wallpaperapp.source.SourceCatalog
import com.example.wallpaperapp.source.SourceStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class WallpaperViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val repository: WallpaperRepository,
    private val sourceCatalog: SourceCatalog,
    private val settingsStore: SettingsStore,
) : ViewModel() {

    private val _feed = MutableStateFlow(emptyList<Wallpaper>())
    val feed: StateFlow<List<Wallpaper>> = _feed.asStateFlow()

    private val _savedItems = MutableStateFlow<List<SavedItemEntity>>(emptyList())
    val savedItems: StateFlow<List<SavedItemEntity>> = _savedItems.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Wallpaper>>(emptyList())
    val searchResults: StateFlow<List<Wallpaper>> = _searchResults.asStateFlow()

    private val _sourceStatuses = MutableStateFlow<List<SourceStatus>>(emptyList())
    val sourceStatuses: StateFlow<List<SourceStatus>> = _sourceStatuses.asStateFlow()

    private val _paintingsFilters = MutableStateFlow(PaintingsFilterSelection())
    val paintingsFilters: StateFlow<PaintingsFilterSelection> = _paintingsFilters.asStateFlow()

    private var paintingsPage = 1
    private var paintingsHasMore = true

    private val _isRefreshingPaintings = MutableStateFlow(false)
    val isRefreshingPaintings: StateFlow<Boolean> = _isRefreshingPaintings.asStateFlow()

    private val _isLoadingMorePaintings = MutableStateFlow(false)
    val isLoadingMorePaintings: StateFlow<Boolean> = _isLoadingMorePaintings.asStateFlow()

    val hapticsEnabled = settingsStore.hapticsEnabledFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = true,
    )

    val dynamicColorsEnabled = settingsStore.dynamicColorsEnabledFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = true,
    )

    init {
        refreshSavedItems()
        refreshFeed()
        refreshSources()
    }

    fun refreshFeed() {
        viewModelScope.launch {
            paintingsPage = 1
            val result = sourceCatalog.paintingsFeed(_paintingsFilters.value, page = 1)
            _feed.value = result.wallpapers
            paintingsHasMore = result.hasMore
            refreshSources()
        }
    }

    fun updatePaintingsFilters(filters: PaintingsFilterSelection) {
        _paintingsFilters.value = filters
        refreshFeed()
    }

    fun loadMorePaintings() {
        if (_isLoadingMorePaintings.value || !paintingsHasMore) return
        viewModelScope.launch {
            _isLoadingMorePaintings.value = true
            val nextPage = paintingsPage + 1
            val result = sourceCatalog.paintingsFeed(_paintingsFilters.value, page = nextPage)
            val existingIds = _feed.value.map { it.id }.toHashSet()
            _feed.value = _feed.value + result.wallpapers.filterNot { it.id in existingIds }
            paintingsPage = nextPage
            paintingsHasMore = result.hasMore
            _isLoadingMorePaintings.value = false
        }
    }

    fun pullToRefreshPaintings() {
        viewModelScope.launch {
            _isRefreshingPaintings.value = true
            paintingsPage = 1
            val result = sourceCatalog.paintingsFeed(
                filters = _paintingsFilters.value,
                page = 1,
                randomize = true,
            )
            _feed.value = result.wallpapers
            paintingsHasMore = result.hasMore
            _isRefreshingPaintings.value = false
        }
    }

    fun refreshSources() {
        _sourceStatuses.value = sourceCatalog.reviewSources()
    }

    fun searchWallpapers(query: String) {
        viewModelScope.launch {
            val trimmed = query.trim()
            _searchResults.value = if (trimmed.isBlank()) {
                emptyList()
            } else {
                sourceCatalog.searchEnabledSources(query = trimmed, limit = 50)
            }
        }
    }

    fun clearSearch() {
        _searchResults.value = emptyList()
        refreshFeed()
    }

    fun setSourceEnabled(sourceId: String, enabled: Boolean) {
        sourceCatalog.setSourceEnabled(sourceId, enabled)
        refreshSources()
        refreshFeed()
    }

    fun refreshSavedItems() {
        viewModelScope.launch {
            _savedItems.value = repository.getSavedItems()
        }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setHapticsEnabled(enabled)
        }
    }

    fun setDynamicColorsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setDynamicColorsEnabled(enabled)
        }
    }

    fun saveWallpaper(wallpaper: Wallpaper, kind: SaveKind) {
        viewModelScope.launch {
            val item = SavedItemEntity(
                wallpaperId = wallpaper.id,
                sourceId = wallpaper.sourceId,
                kind = kind,
                thumbnailUrl = wallpaper.thumbnailUrl,
                fullResUrl = wallpaper.fullResUrl,
                width = wallpaper.width,
                height = wallpaper.height,
                artist = wallpaper.artist,
            )
            repository.saveWallpaper(item)
            refreshSavedItems()
        }
    }

    suspend fun applyWallpaper(wallpaper: Wallpaper, kind: SaveKind = SaveKind.SET): Boolean = withContext(Dispatchers.IO) {
        val bitmap = runCatching {
            val urlString = wallpaper.fullResUrl.ifBlank { wallpaper.thumbnailUrl }
            loadBitmapFromUrl(urlString)
        }.getOrNull() ?: return@withContext false

        val safeBitmap = if (bitmap.config == Bitmap.Config.HARDWARE) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            bitmap
        }

        val wallpaperManager = WallpaperManager.getInstance(appContext)
        if (!wallpaperManager.isWallpaperSupported || !wallpaperManager.isSetWallpaperAllowed) {
            Log.w("WallpaperViewModel", "Wallpaper setting is not supported or allowed on this device")
            return@withContext false
        }

        try {
            when (kind) {
                SaveKind.WALLPAPER -> wallpaperManager.setBitmap(safeBitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                SaveKind.LOCKSCREEN -> wallpaperManager.setBitmap(safeBitmap, null, true, WallpaperManager.FLAG_LOCK)
                SaveKind.SET -> {
                    wallpaperManager.setBitmap(safeBitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                    wallpaperManager.setBitmap(safeBitmap, null, true, WallpaperManager.FLAG_LOCK)
                }
            }
            true
        } catch (error: Exception) {
            Log.e("WallpaperViewModel", "Failed to apply wallpaper for ${wallpaper.id}", error)
            false
        }
    }

    private fun loadBitmapFromUrl(urlString: String): Bitmap? {
        val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
            instanceFollowRedirects = true
        }

        return try {
            connection.inputStream.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (error: Exception) {
            Log.e("WallpaperViewModel", "Failed to download wallpaper bitmap", error)
            null
        } finally {
            connection.disconnect()
        }
    }
}
