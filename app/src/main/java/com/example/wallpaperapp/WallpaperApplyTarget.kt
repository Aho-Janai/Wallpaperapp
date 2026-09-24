package com.example.wallpaperapp

import android.app.WallpaperManager
import com.example.wallpaperapp.data.SaveKind

object WallpaperApplyTarget {
    fun fromKind(kind: SaveKind): Int = when (kind) {
        SaveKind.WALLPAPER -> WallpaperManager.FLAG_SYSTEM
        SaveKind.LOCKSCREEN -> WallpaperManager.FLAG_LOCK
        SaveKind.SET -> 0
    }
}
