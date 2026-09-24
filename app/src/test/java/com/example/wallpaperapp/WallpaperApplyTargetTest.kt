package com.example.wallpaperapp

import android.app.WallpaperManager
import com.example.wallpaperapp.data.SaveKind
import org.junit.Assert.assertEquals
import org.junit.Test

class WallpaperApplyTargetTest {
    @Test
    fun applyTarget_forSaveKind_matchesWallpaperManagerFlags() {
        assertEquals(WallpaperManager.FLAG_SYSTEM, WallpaperApplyTarget.fromKind(SaveKind.WALLPAPER))
        assertEquals(WallpaperManager.FLAG_LOCK, WallpaperApplyTarget.fromKind(SaveKind.LOCKSCREEN))
        assertEquals(0, WallpaperApplyTarget.fromKind(SaveKind.SET))
    }
}
