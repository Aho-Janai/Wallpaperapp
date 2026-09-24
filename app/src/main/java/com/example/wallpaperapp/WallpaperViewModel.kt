package com.example.wallpaperapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wallpaperapp.data.SaveKind
import com.example.wallpaperapp.data.SavedItemEntity
import com.example.wallpaperapp.data.Wallpaper
import com.example.wallpaperapp.data.WallpaperRepository
import com.example.wallpaperapp.source.SourceRegistry
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class WallpaperViewModel @Inject constructor(
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

    fun applyWallpaper(wallpaper: Wallpaper) {
        // Placeholder for the full-res apply flow; the UI can show a loading/error state.
    }
}
