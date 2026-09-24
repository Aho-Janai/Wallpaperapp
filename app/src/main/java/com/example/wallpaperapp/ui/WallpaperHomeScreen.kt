package com.example.wallpaperapp.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.wallpaperapp.WallpaperViewModel
import com.example.wallpaperapp.data.SaveKind
import com.example.wallpaperapp.data.SavedItemEntity
import com.example.wallpaperapp.data.Wallpaper
import kotlinx.coroutines.launch

private data class WallpaperFilters(
    val selectedColors: Set<String> = emptySet(),
    val selectedSizes: Set<String> = emptySet(),
    val selectedSources: Set<String> = emptySet(),
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WallpaperAppScreen(
    modifier: Modifier = Modifier,
    viewModel: WallpaperViewModel = hiltViewModel(),
) {
    val feed by viewModel.feed.collectAsStateWithLifecycle()
    val savedItems by viewModel.savedItems.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedWallpaper by remember { mutableStateOf<Wallpaper?>(null) }
    var wallpaperToApply by remember { mutableStateOf<Wallpaper?>(null) }
    var wallpaperToSave by remember { mutableStateOf<Wallpaper?>(null) }
    var savedFilter by rememberSaveable { mutableStateOf<SaveKind?>(null) }
    var filters by remember { mutableStateOf(WallpaperFilters()) }
    var showSettings by remember { mutableStateOf(false) }
    var filterCategory by remember { mutableStateOf<String?>(null) }
    val visibleSavedItems = when (savedFilter) {
        null -> savedItems
        SaveKind.WALLPAPER -> savedItems.filter { it.kind == SaveKind.WALLPAPER }
        SaveKind.LOCKSCREEN -> savedItems.filter { it.kind == SaveKind.LOCKSCREEN }
        SaveKind.SET -> savedItems.filter { it.kind == SaveKind.SET }
    }
    val filteredFeed = remember(feed, filters) { applyWallpaperFilters(feed, filters) }

    Box(modifier = modifier.background(MaterialTheme.colorScheme.background)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            pageSpacing = 0.dp,
        ) { page ->
            when (page) {
                0 -> HomeFeed(
                    wallpapers = filteredFeed,
                    filters = filters,
                    onWallpaperClick = { selectedWallpaper = it },
                    onOpenFilter = { filterCategory = it },
                    onUpdateFilters = { filters = it },
                )
                1 -> SavedFeed(
                    items = visibleSavedItems,
                    activeFilter = savedFilter,
                    onFilterSelected = { savedFilter = it },
                    onItemClick = { selectedWallpaper = it.toWallpaper() },
                )
            }
        }

        FloatingTabBar(
            selectedPage = pagerState.currentPage,
            onTabSelected = { index ->
                scope.launch { pagerState.animateScrollToPage(index) }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
        )

        FloatingActionButton(
            onClick = {
                val luckyWallpaper = filteredFeed.randomOrNull()
                if (luckyWallpaper == null) {
                    scope.launch { snackbarHostState.showSnackbar("No wallpapers available right now") }
                } else {
                    wallpaperToApply = luckyWallpaper
                    selectedWallpaper = luckyWallpaper
                }
            },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 32.dp, bottom = 96.dp),
        ) {
            Text("⭐")
        }

        FloatingActionButton(
            onClick = { showSettings = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 32.dp, bottom = 96.dp),
        ) {
            Text("⚙")
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    selectedWallpaper?.let { wallpaper ->
        ModalBottomSheet(onDismissRequest = { selectedWallpaper = null }) {
            WallpaperActionSheet(
                wallpaper = wallpaper,
                onApply = {
                    wallpaperToApply = wallpaper
                    selectedWallpaper = null
                },
                onSave = {
                    wallpaperToSave = wallpaper
                    selectedWallpaper = null
                },
            )
        }
    }

    wallpaperToApply?.let { wallpaper ->
        ApplyChoiceDialog(
            wallpaper = wallpaper,
            onDismiss = { wallpaperToApply = null },
            onApply = { kind ->
                scope.launch {
                    val success = viewModel.applyWallpaper(wallpaper, kind)
                    wallpaperToApply = null
                    if (success) {
                        val label = when (kind) {
                            SaveKind.WALLPAPER -> "Home wallpaper set"
                            SaveKind.LOCKSCREEN -> "Lock screen set"
                            SaveKind.SET -> "Home + lock set"
                        }
                        snackbarHostState.showSnackbar(label)
                    } else {
                        snackbarHostState.showSnackbar("Wallpaper could not be applied on this device")
                    }
                }
            },
        )
    }

    wallpaperToSave?.let { wallpaper ->
        SaveChoiceDialog(
            wallpaper = wallpaper,
            onDismiss = { wallpaperToSave = null },
            onSave = { kind ->
                viewModel.saveWallpaper(wallpaper, kind)
                wallpaperToSave = null
                scope.launch {
                    val label = when (kind) {
                        SaveKind.WALLPAPER -> "Saved as wallpaper"
                        SaveKind.LOCKSCREEN -> "Saved as lock screen"
                        SaveKind.SET -> "Saved as set"
                    }
                    snackbarHostState.showSnackbar(label)
                }
            },
        )
    }

    if (showSettings) {
        SettingsSheet(onDismiss = { showSettings = false })
    }

    filterCategory?.let { category ->
        FilterPickerDialog(
            category = category,
            filters = filters,
            onDismiss = { filterCategory = null },
            onUpdated = {
                filters = it
                filterCategory = null
            },
        )
    }
}

@Composable
private fun HomeFeed(
    wallpapers: List<Wallpaper>,
    filters: WallpaperFilters,
    onWallpaperClick: (Wallpaper) -> Unit,
    onOpenFilter: (String) -> Unit,
    onUpdateFilters: (WallpaperFilters) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Wallpapers",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(
                "Color" to filters.selectedColors.isNotEmpty(),
                "Size" to filters.selectedSizes.isNotEmpty(),
                "Source" to filters.selectedSources.isNotEmpty(),
            ).forEach { (category, selected) ->
                FilterChip(
                    selected = selected,
                    onClick = { onOpenFilter(category) },
                    label = { Text(category) },
                )
            }
        }

        if (wallpapers.isEmpty()) {
            Text(
                text = "No wallpapers match these filters.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
            )
        } else {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Adaptive(minSize = 180.dp),
                contentPadding = PaddingValues(bottom = 80.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalItemSpacing = 10.dp,
                modifier = Modifier.fillMaxSize(),
            ) {
                items(wallpapers, key = { it.id }) { wallpaper ->
                    WallpaperThumbnailCard(
                        wallpaper = wallpaper,
                        onClick = { onWallpaperClick(wallpaper) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedFeed(
    items: List<SavedItemEntity>,
    activeFilter: SaveKind?,
    onFilterSelected: (SaveKind?) -> Unit,
    onItemClick: (SavedItemEntity) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Text(
            text = "Saved",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 12.dp),
        ) {
            listOf(null, SaveKind.WALLPAPER, SaveKind.LOCKSCREEN, SaveKind.SET).forEach { kind ->
                val label = when (kind) {
                    null -> "All"
                    SaveKind.WALLPAPER -> "Wallpapers"
                    SaveKind.LOCKSCREEN -> "Lock Screens"
                    SaveKind.SET -> "Sets"
                }
                TextButton(
                    onClick = { onFilterSelected(kind) },
                    modifier = Modifier.background(
                        if (activeFilter == kind) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                        RoundedCornerShape(50),
                    ),
                ) {
                    Text(label)
                }
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(items, key = { it.wallpaperId }) { item ->
                SavedItemRow(
                    item = item,
                    onClick = { onItemClick(item) },
                )
            }
        }
    }
}

@Composable
private fun FloatingTabBar(
    selectedPage: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = { onTabSelected(0) },
                modifier = Modifier.background(
                    if (selectedPage == 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(50),
                ),
            ) {
                Text("Home")
            }
            TextButton(
                onClick = { onTabSelected(1) },
                modifier = Modifier.background(
                    if (selectedPage == 1) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(50),
                ),
            ) {
                Text("♥")
            }
        }
    }
}

@Composable
private fun FilterPickerDialog(
    category: String,
    filters: WallpaperFilters,
    onDismiss: () -> Unit,
    onUpdated: (WallpaperFilters) -> Unit,
) {
    val options = when (category) {
        "Color" -> listOf("blue", "teal", "red", "green", "orange")
        "Size" -> listOf("HD", "QHD", "4K+")
        "Source" -> listOf("demo", "wallhaven", "unsplash")
        else -> emptyList()
    }

    val selectedValues = when (category) {
        "Color" -> filters.selectedColors
        "Size" -> filters.selectedSizes
        "Source" -> filters.selectedSources
        else -> emptySet<String>()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(category) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                options.forEach { option ->
                    FilterChip(
                        selected = option in selectedValues,
                        onClick = {
                            val next: Set<String> = when (category) {
                                "Color" -> filters.selectedColors.toMutableSet().apply {
                                    if (contains(option)) remove(option) else add(option)
                                }.toSet()
                                "Size" -> filters.selectedSizes.toMutableSet().apply {
                                    if (contains(option)) remove(option) else add(option)
                                }.toSet()
                                "Source" -> filters.selectedSources.toMutableSet().apply {
                                    if (contains(option)) remove(option) else add(option)
                                }.toSet()
                                else -> emptySet()
                            }

                            onUpdated(
                                when (category) {
                                    "Color" -> filters.copy(selectedColors = next)
                                    "Size" -> filters.copy(selectedSizes = next)
                                    "Source" -> filters.copy(selectedSources = next)
                                    else -> filters
                                },
                            )
                        },
                        label = { Text(option) },
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDismiss) { Text("←") }
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }

            SettingCard(title = "Sources", subtitle = "Demo, Wallhaven, Unsplash", iconColor = MaterialTheme.colorScheme.primary)
            SettingCard(title = "Appearance", subtitle = "System default", iconColor = MaterialTheme.colorScheme.secondary)
            SettingCard(title = "Content Filters", subtitle = "SFW-only by default", iconColor = MaterialTheme.colorScheme.tertiary)
            SettingCard(title = "Storage", subtitle = "Current cache: 0 MB • Clear cache", iconColor = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun SettingCard(title: String, subtitle: String, iconColor: Color) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(iconColor, RoundedCornerShape(10.dp)),
            )
            Spacer(modifier = Modifier.size(12.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
private fun KindBadge(kind: SaveKind) {
    Surface(
        shape = RoundedCornerShape(50),
        color = when (kind) {
            SaveKind.WALLPAPER -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            SaveKind.LOCKSCREEN -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
            SaveKind.SET -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
        },
    ) {
        Text(
            text = when (kind) {
                SaveKind.WALLPAPER -> "Wallpaper"
                SaveKind.LOCKSCREEN -> "Lock"
                SaveKind.SET -> "Set"
            },
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun WallpaperThumbnailCard(
    wallpaper: Wallpaper,
    onClick: () -> Unit,
) {
    val cardHeight = (220f / wallpaper.aspectRatio).coerceIn(140f, 320f)
    Card(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        AsyncImage(
            model = wallpaper.thumbnailUrl,
            contentDescription = wallpaper.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(cardHeight.dp)
                .clip(RoundedCornerShape(18.dp)),
        )
    }
}

@Composable
private fun SavedItemRow(
    item: SavedItemEntity,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AsyncImage(
                model = item.thumbnailUrl,
                contentDescription = item.wallpaperId,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )
            Column(modifier = Modifier.weight(1f)) {
                KindBadge(item.kind)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = item.sourceId,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = item.artist ?: "Unknown artist",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun WallpaperActionSheet(
    wallpaper: Wallpaper,
    onApply: () -> Unit,
    onSave: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AsyncImage(
            model = wallpaper.thumbnailUrl,
            contentDescription = wallpaper.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp)
                .clip(RoundedCornerShape(20.dp)),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            KindBadge(SaveKind.WALLPAPER)
            wallpaper.aspectRatioLabel()?.let { KindBadge(SaveKind.SET) }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = wallpaper.width?.let { w -> wallpaper.height?.let { h -> "$w×$h" } } ?: "Unknown size",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = wallpaper.sourceId,
                style = MaterialTheme.typography.bodyMedium,
            )
            wallpaper.artist?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            wallpaper.aspectRatioLabel()?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        Button(
            onClick = onApply,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Apply Set")
        }

        OutlinedButton(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Save Set")
        }
    }
}

@Composable
private fun ApplyChoiceDialog(
    wallpaper: Wallpaper,
    onDismiss: () -> Unit,
    onApply: (SaveKind) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Apply wallpaper") },
        text = { Text("Choose where to set this image.") },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onApply(SaveKind.WALLPAPER) }) { Text("Home") }
                TextButton(onClick = { onApply(SaveKind.LOCKSCREEN) }) { Text("Lock") }
                TextButton(onClick = { onApply(SaveKind.SET) }) { Text("Both") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun SaveChoiceDialog(
    wallpaper: Wallpaper,
    onDismiss: () -> Unit,
    onSave: (SaveKind) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save wallpaper") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Wallpaper")
                Text("Lock Screen")
                Text("Both (Set)")
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onSave(SaveKind.WALLPAPER) }) { Text("Wallpaper") }
                TextButton(onClick = { onSave(SaveKind.LOCKSCREEN) }) { Text("Lock Screen") }
                TextButton(onClick = { onSave(SaveKind.SET) }) { Text("Both (Set)") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

private fun SavedItemEntity.toWallpaper(): Wallpaper = Wallpaper(
    id = wallpaperId,
    sourceId = sourceId,
    thumbnailUrl = thumbnailUrl,
    fullResUrl = fullResUrl,
    width = width,
    height = height,
    artist = artist,
)

private fun applyWallpaperFilters(wallpapers: List<Wallpaper>, filters: WallpaperFilters): List<Wallpaper> {
    return wallpapers.filter { wallpaper ->
        val anyColor = wallpaper.tags.any { tag ->
            filters.selectedColors.any { color -> tag.contains(color, ignoreCase = true) }
        }
        val colorMatch = if (filters.selectedColors.isEmpty()) true else anyColor
        val sizeMatch = if (filters.selectedSizes.isEmpty()) true else filters.selectedSizes.any { bucket ->
            val width = wallpaper.width ?: return@any false
            val height = wallpaper.height ?: return@any false
            when (bucket) {
                "HD" -> width * height >= 1920 * 1080
                "QHD" -> width * height >= 2560 * 1440
                "4K+" -> width * height >= 3840 * 2160
                else -> true
            }
        }
        val sourceMatch = if (filters.selectedSources.isEmpty()) true else filters.selectedSources.contains(wallpaper.sourceId)
        colorMatch && sizeMatch && sourceMatch
    }
}
