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
import com.example.wallpaperapp.data.Wallpaper
import com.example.wallpaperapp.data.WallpaperRepository
import com.example.wallpaperapp.source.SourceRegistry
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class WallpaperViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val repository: WallpaperRepository,
    private val sourceRegistry: SourceRegistry,
) : ViewModel() {

    private val _feed = MutableStateFlow(emptyList<Wallpaper>())
    val feed: StateFlow<List<Wallpaper>> = _feed.asStateFlow()

    private val _savedItems = MutableStateFlow<List<SavedItemEntity>>(emptyList())
    val savedItems: StateFlow<List<SavedItemEntity>> = _savedItems.asStateFlow()

    init {
        refreshSavedItems()
        refreshFeed()
    }

    fun refreshFeed() {
        viewModelScope.launch {
            _feed.value = sourceRegistry.abstractFeed()
        }
    }

    fun refreshSavedItems() {
        viewModelScope.launch {
            _savedItems.value = repository.getSavedItems()
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
