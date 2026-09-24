package com.example.wallpaperapp

import androidx.lifecycle.ViewModel
import com.example.wallpaperapp.data.Wallpaper
import com.example.wallpaperapp.data.WallpaperRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class WallpaperViewModel @Inject constructor(
    repository: WallpaperRepository,
) : ViewModel() {

    private val _wallpapers = MutableStateFlow(repository.wallpapers())
    val wallpapers: StateFlow<List<Wallpaper>> = _wallpapers.asStateFlow()
}
