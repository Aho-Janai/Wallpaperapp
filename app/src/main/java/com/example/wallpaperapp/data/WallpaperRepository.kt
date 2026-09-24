package com.example.wallpaperapp.data

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WallpaperRepository @Inject constructor(
    private val savedItemDao: SavedItemDao,
) {
    fun wallpapers(): List<Wallpaper> = demoWallpapers

    suspend fun getSavedItems(kind: SaveKind? = null): List<SavedItemEntity> =
        if (kind == null) savedItemDao.getAll() else savedItemDao.getByKind(kind)

    suspend fun saveWallpaper(item: SavedItemEntity): Long = savedItemDao.insert(item)

    companion object {
        val demoWallpapers = listOf(
            Wallpaper(
                id = "1",
                sourceId = "wikimedia-commons",
                thumbnailUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/e/ec/Mona_Lisa%2C_by_Leonardo_da_Vinci%2C_from_C2RMF_retouched.jpg/800px-Mona_Lisa%2C_by_Leonardo_da_Vinci%2C_from_C2RMF_retouched.jpg",
                fullResUrl = "https://upload.wikimedia.org/wikipedia/commons/e/ec/Mona_Lisa%2C_by_Leonardo_da_Vinci%2C_from_C2RMF_retouched.jpg",
                width = 1524,
                height = 1878,
                tags = listOf("portrait", "renaissance", "masterpiece"),
                artist = "Leonardo da Vinci",
                attributionText = "Mona Lisa by Leonardo da Vinci",
                attributionUrl = "https://commons.wikimedia.org/wiki/File:Mona_Lisa,_by_Leonardo_da_Vinci,_from_C2RMF_retouched.jpg"
            ),
            Wallpaper(
                id = "2",
                sourceId = "wikimedia-commons",
                thumbnailUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/e/ee/The_Starry_Night_-_Google_Art_Project.jpg/800px-The_Starry_Night_-_Google_Art_Project.jpg",
                fullResUrl = "https://upload.wikimedia.org/wikipedia/commons/e/ee/The_Starry_Night_-_Google_Art_Project.jpg",
                width = 1280,
                height = 1024,
                tags = listOf("landscape", "post-impressionism", "night"),
                artist = "Vincent van Gogh",
                attributionText = "The Starry Night by Vincent van Gogh",
                attributionUrl = "https://commons.wikimedia.org/wiki/File:The_Starry_Night_-_Google_Art_Project.jpg"
            ),
            Wallpaper(
                id = "3",
                sourceId = "wikimedia-commons",
                thumbnailUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/9/9d/Salvador_Dali_-_The_Persistence_of_Memory.jpg/800px-Salvador_Dali_-_The_Persistence_of_Memory.jpg",
                fullResUrl = "https://upload.wikimedia.org/wikipedia/commons/9/9d/Salvador_Dali_-_The_Persistence_of_Memory.jpg",
                width = 1200,
                height = 900,
                tags = listOf("surrealism", "dream", "abstract"),
                artist = "Salvador Dalí",
                attributionText = "The Persistence of Memory by Salvador Dalí",
                attributionUrl = "https://commons.wikimedia.org/wiki/File:Salvador_Dali_-_The_Persistence_of_Memory.jpg"
            ),
            Wallpaper(
                id = "4",
                sourceId = "wikimedia-commons",
                thumbnailUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/0/0b/La_nascita_di_venere.jpg/800px-La_nascita_di_venere.jpg",
                fullResUrl = "https://upload.wikimedia.org/wikipedia/commons/0/0b/La_nascita_di_venere.jpg",
                width = 1331,
                height = 1600,
                tags = listOf("mythology", "classical", "beauty"),
                artist = "Sandro Botticelli",
                attributionText = "The Birth of Venus by Sandro Botticelli",
                attributionUrl = "https://commons.wikimedia.org/wiki/File:La_nascita_di_venere.jpg"
            ),
            Wallpaper(
                id = "5",
                sourceId = "wikimedia-commons",
                thumbnailUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/0/01/The_Great_Wave_off_Kanagawa.jpg/800px-The_Great_Wave_off_Kanagawa.jpg",
                fullResUrl = "https://upload.wikimedia.org/wikipedia/commons/0/01/The_Great_Wave_off_Kanagawa.jpg",
                width = 1200,
                height = 800,
                tags = listOf("wave", "japanese", "print"),
                artist = "Katsushika Hokusai",
                attributionText = "The Great Wave off Kanagawa by Katsushika Hokusai",
                attributionUrl = "https://commons.wikimedia.org/wiki/File:The_Great_Wave_off_Kanagawa.jpg"
            ),
            Wallpaper(
                id = "6",
                sourceId = "wikimedia-commons",
                thumbnailUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/5/5c/The_Scream.jpg/800px-The_Scream.jpg",
                fullResUrl = "https://upload.wikimedia.org/wikipedia/commons/5/5c/The_Scream.jpg",
                width = 1200,
                height = 972,
                tags = listOf("expressionism", "emotion", "iconic"),
                artist = "Edvard Munch",
                attributionText = "The Scream by Edvard Munch",
                attributionUrl = "https://commons.wikimedia.org/wiki/File:The_Scream.jpg"
            )
        )
    }
}
