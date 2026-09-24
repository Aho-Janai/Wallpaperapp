package com.example.wallpaperapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.wallpaperapp.ui.WallpaperAppScreen
import com.example.wallpaperapp.ui.theme.WallpaperAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WallpaperAppTheme {
                WallpaperAppScreen(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
