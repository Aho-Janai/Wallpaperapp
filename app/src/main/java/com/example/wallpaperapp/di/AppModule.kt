package com.example.wallpaperapp.di

import android.content.Context
import androidx.room.Room
import com.example.wallpaperapp.data.SavedItemDao
import com.example.wallpaperapp.data.WallpaperDatabase
import com.example.wallpaperapp.source.SourceCatalog
import com.example.wallpaperapp.source.SourceRegistry
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideWallpaperDatabase(@ApplicationContext context: Context): WallpaperDatabase =
        Room.databaseBuilder(
            context,
            WallpaperDatabase::class.java,
            "wallpaper_app_db"
        ).build()

    @Provides
    @Singleton
    fun provideSavedItemDao(database: WallpaperDatabase): SavedItemDao = database.savedItemDao()

    @Provides
    @Singleton
    fun provideSourceCatalog(): SourceCatalog = SourceCatalog()

    @Provides
    @Singleton
    fun provideSourceRegistry(sourceCatalog: SourceCatalog): SourceRegistry = SourceRegistry(sourceCatalog)
}
