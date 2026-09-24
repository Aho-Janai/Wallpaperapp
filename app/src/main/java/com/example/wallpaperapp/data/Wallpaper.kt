package com.example.wallpaperapp.data

data class Wallpaper(
    val id: String,
    val sourceId: String,
    val thumbnailUrl: String,
    val fullResUrl: String,
    val width: Int? = null,
    val height: Int? = null,
    val tags: List<String> = emptyList(),
    val artist: String? = null,
    val attributionText: String? = null,
    val attributionUrl: String? = null,
) {
    val title: String
        get() = tags.firstOrNull() ?: "Wallpaper"

    val sourceName: String
        get() = sourceId

    val aspectRatio: Float
        get() = if (width != null && height != null && height != 0) width.toFloat() / height.toFloat() else 1f

    fun aspectRatioLabel(): String? {
        val w = width ?: return null
        val h = height ?: return null
        val g = gcd(w, h)
        return "${w / g}:${h / g}"
    }

    fun matchesAbstractSearchFilter(): Boolean {
        if (tags.isEmpty()) return true
        return tags.any { tag -> tag.contains("abstract", ignoreCase = true) }
    }

    fun containsAiHeuristicMarker(): Boolean {
        val combined = (tags + listOf(title)).joinToString(" ").lowercase()
        return listOf("ai", "midjourney", "stable diffusion", "dall-e", "generated").any { marker ->
            combined.contains(marker, ignoreCase = true)
        }
    }

    private tailrec fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
}

