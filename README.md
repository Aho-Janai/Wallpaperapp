# Wallpaper App

A starter Android wallpaper app built with Kotlin, Jetpack Compose, and a Pinterest-inspired masonry feed.

## Stack

- Kotlin
- Jetpack Compose
- Material 3
- Coil for image loading
- Hilt for dependency injection
- ViewModel + StateFlow

## Features included

- Pinterest-style staggered image grid
- Demo wallpaper data source
- Source abstraction for future API/scraped providers
- App shell ready for adding favorites, settings, and search

## Running the app

1. Install JDK 17.
2. Ensure the Android SDK is available.
3. Open the project in Android Studio or run Gradle from the repo root.

## Extending it

The `WallpaperSource` abstraction lives in `app/src/main/java/com/example/wallpaperapp/source/WallpaperSource.kt` and is intended to be extended with real provider implementations such as Wallhaven, Unsplash, or a Jsoup scraper.
