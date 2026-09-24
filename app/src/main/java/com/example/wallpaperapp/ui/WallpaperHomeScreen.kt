package com.example.wallpaperapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import com.example.wallpaperapp.WallpaperViewModel
import com.example.wallpaperapp.data.SaveKind
import com.example.wallpaperapp.data.SavedItemEntity
import com.example.wallpaperapp.data.Wallpaper
import com.example.wallpaperapp.source.PaintingsFilterOptions
import com.example.wallpaperapp.source.PaintingsFilterSelection
import com.example.wallpaperapp.source.SourceStatus
import kotlinx.coroutines.launch

private data class WallpaperFilters(
    val selectedColors: Set<String> = emptySet(),
    val selectedSizes: Set<String> = emptySet(),
    val selectedSources: Set<String> = emptySet(),
)

private data class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector,
) {
    companion object {
        val Home = BottomNavItem("home", "Home", Icons.Default.Home)
        val Discover = BottomNavItem("discover", "Discover", Icons.Default.Search)
        val Saved = BottomNavItem("saved", "Saved", Icons.Default.Star)
        val Settings = BottomNavItem("settings", "Settings", Icons.Default.Settings)
        val entries = listOf(Home, Discover, Saved, Settings)
    }
}

@Composable
fun WallpaperAppScreen(
    modifier: Modifier = Modifier,
    viewModel: WallpaperViewModel = hiltViewModel(),
) {
    val feed by viewModel.feed.collectAsStateWithLifecycle()
    val savedItems by viewModel.savedItems.collectAsStateWithLifecycle()
    val hapticsEnabled by viewModel.hapticsEnabled.collectAsStateWithLifecycle()
    val dynamicColorsEnabled by viewModel.dynamicColorsEnabled.collectAsStateWithLifecycle()
    val sourceStatuses by viewModel.sourceStatuses.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val paintingsFilters by viewModel.paintingsFilters.collectAsStateWithLifecycle()
    val isRefreshingPaintings by viewModel.isRefreshingPaintings.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var filters by remember { mutableStateOf(WallpaperFilters()) }
    var filterCategory by remember { mutableStateOf<String?>(null) }
    var savedFilter by rememberSaveable { mutableStateOf<SaveKind?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val visibleSavedItems = when (savedFilter) {
        null -> savedItems
        SaveKind.WALLPAPER -> savedItems.filter { it.kind == SaveKind.WALLPAPER }
        SaveKind.LOCKSCREEN -> savedItems.filter { it.kind == SaveKind.LOCKSCREEN }
        SaveKind.SET -> savedItems.filter { it.kind == SaveKind.SET }
    }
    val filteredFeed = remember(feed, filters) { applyWallpaperFilters(feed, filters) }
    val activeFeed = remember(searchQuery, filteredFeed, searchResults) {
        if (searchQuery.isBlank()) filteredFeed else searchResults
    }
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (currentRoute in listOf(BottomNavItem.Home.route, BottomNavItem.Discover.route, BottomNavItem.Saved.route, BottomNavItem.Settings.route)) {
                FloatingNavBar(
                    currentRoute = currentRoute ?: BottomNavItem.Home.route,
                    onNavigate = { route ->
                        if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(BottomNavItem.Home.route) {
                HomeScreen(
                    wallpapers = activeFeed,
                    filters = filters,
                    paintingsFilters = paintingsFilters,
                    searchQuery = searchQuery,
                    isRefreshingPaintings = isRefreshingPaintings,
                    onSearchQueryChange = { query ->
                        searchQuery = query
                        if (query.isBlank()) {
                            viewModel.clearSearch()
                        } else {
                            viewModel.searchWallpapers(query)
                        }
                    },
                    onOpenFilter = { category ->
                        if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        filterCategory = category
                    },
                    onUpdateFilters = { filters = it },
                    onUpdatePaintingsFilters = { viewModel.updatePaintingsFilters(it) },
                    onWallpaperClick = { wallpaper ->
                        if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        navController.navigate("detail/${wallpaper.id}")
                    },
                    onApplyWallpaper = { wallpaper ->
                        scope.launch {
                            val result = viewModel.applyWallpaper(wallpaper, SaveKind.WALLPAPER)
                            if (result) snackbarHostState.showSnackbar("Home wallpaper applied")
                            else snackbarHostState.showSnackbar("Wallpaper could not be applied")
                        }
                    },
                    onLoadMore = viewModel::loadMorePaintings,
                    onRefresh = viewModel::pullToRefreshPaintings,
                )
            }

            composable(BottomNavItem.Discover.route) {
                DiscoverScreen(
                    wallpapers = activeFeed,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { query ->
                        searchQuery = query
                        if (query.isBlank()) {
                            viewModel.clearSearch()
                        } else {
                            viewModel.searchWallpapers(query)
                        }
                    },
                    onWallpaperClick = { wallpaper -> navController.navigate("detail/${wallpaper.id}") },
                    onApplyWallpaper = { wallpaper ->
                        scope.launch {
                            val result = viewModel.applyWallpaper(wallpaper, SaveKind.WALLPAPER)
                            if (result) snackbarHostState.showSnackbar("Wallpaper applied")
                            else snackbarHostState.showSnackbar("Wallpaper could not be applied")
                        }
                    },
                )
            }

            composable(BottomNavItem.Saved.route) {
                SavedScreen(
                    items = visibleSavedItems,
                    activeFilter = savedFilter,
                    onFilterSelected = { savedFilter = it },
                    onWallpaperClick = { item -> navController.navigate("detail/${item.wallpaperId}") },
                )
            }

            composable(BottomNavItem.Settings.route) {
                SettingsScreen(
                    hapticsEnabled = hapticsEnabled,
                    dynamicColorsEnabled = dynamicColorsEnabled,
                    sourceStatuses = sourceStatuses,
                    onHapticsChanged = { viewModel.setHapticsEnabled(it) },
                    onDynamicColorsChanged = { viewModel.setDynamicColorsEnabled(it) },
                    onSourceToggled = { sourceId, enabled ->
                        viewModel.setSourceEnabled(sourceId, enabled)
                    },
                )
            }

            composable(
                route = "detail/{wallpaperId}",
                arguments = listOf(navArgument("wallpaperId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val wallpaperId = backStackEntry.arguments?.getString("wallpaperId")
                val wallpaper = feed.firstOrNull { it.id == wallpaperId }
                    ?: visibleSavedItems.firstOrNull { it.wallpaperId == wallpaperId }?.toWallpaper()
                if (wallpaper != null) {
                    WallpaperDetailScreen(
                        wallpaper = wallpaper,
                        onBack = { navController.popBackStack() },
                        onApply = { kind ->
                            scope.launch {
                                val result = viewModel.applyWallpaper(wallpaper, kind)
                                if (result) snackbarHostState.showSnackbar("Wallpaper applied")
                                else snackbarHostState.showSnackbar("Wallpaper could not be applied")
                            }
                        },
                        onSave = { kind ->
                            viewModel.saveWallpaper(wallpaper, kind)
                            scope.launch {
                                snackbarHostState.showSnackbar("Saved to ${kind.name.lowercase()}")
                            }
                        },
                    )
                }
            }
        }
    }

    filterCategory?.let { category ->
        if (category in listOf("Century", "Movement", "Artist")) {
            PaintingsFilterPickerDialog(
                category = category,
                paintingsFilters = paintingsFilters,
                onDismiss = { filterCategory = null },
                onUpdated = {
                    viewModel.updatePaintingsFilters(it)
                    filterCategory = null
                },
            )
        } else {
            FilterPickerDialog(
                category = category,
                filters = filters,
                sourceStatuses = sourceStatuses,
                onDismiss = { filterCategory = null },
                onUpdated = {
                    filters = it
                    filterCategory = null
                },
            )
        }
    }
}

@Composable
private fun FloatingNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        tonalElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            BottomNavItem.entries.forEach { item ->
                val selected = currentRoute == item.route
                NavigationBarItem(
                    selected = selected,
                    onClick = { onNavigate(item.route) },
                    icon = { Icon(imageVector = item.icon, contentDescription = item.title) },
                    label = { Text(item.title) },
                    alwaysShowLabel = false,
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun HomeScreen(
    wallpapers: List<Wallpaper>,
    filters: WallpaperFilters,
    paintingsFilters: PaintingsFilterSelection,
    searchQuery: String,
    isRefreshingPaintings: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onOpenFilter: (String) -> Unit,
    onUpdateFilters: (WallpaperFilters) -> Unit,
    onUpdatePaintingsFilters: (PaintingsFilterSelection) -> Unit,
    onWallpaperClick: (Wallpaper) -> Unit,
    onApplyWallpaper: (Wallpaper) -> Unit,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
) {
    val heroWallpapers = wallpapers.take(4)
    val gridState = rememberLazyStaggeredGridState()

    LaunchedEffect(gridState, wallpapers.size) {
        snapshotFlow {
            gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
        }.collect { lastVisibleIndex ->
            if (lastVisibleIndex != null && lastVisibleIndex >= wallpapers.size - 10) {
                onLoadMore()
            }
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshingPaintings,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyVerticalStaggeredGrid(
            state = gridState,
            columns = StaggeredGridCells.Adaptive(minSize = 180.dp),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalItemSpacing = 12.dp,
            modifier = Modifier.fillMaxSize(),
        ) {
            item(span = StaggeredGridItemSpan.FullLine) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Find your next wallpaper.",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                    )

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("Search wallpapers") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onSearchQueryChange(searchQuery) }),
                    )

                    Text(
                        text = "Featured",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(heroWallpapers) { wallpaper ->
                            FeaturedWallpaperCard(
                                wallpaper = wallpaper,
                                onClick = { onWallpaperClick(wallpaper) },
                                onApply = { onApplyWallpaper(wallpaper) },
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val filterEntries = listOf(
                            "Color" to filters.selectedColors.isNotEmpty(),
                            "Size" to filters.selectedSizes.isNotEmpty(),
                            "Source" to filters.selectedSources.isNotEmpty(),
                            "Century" to (paintingsFilters.centuryCategory != null),
                            "Movement" to (paintingsFilters.movementCategory != null),
                            "Artist" to (paintingsFilters.artistCategory != null),
                        )
                        filterEntries.forEach { (category, selected) ->
                            FilterChip(
                                selected = selected,
                                onClick = { onOpenFilter(category) },
                                label = { Text(category) },
                            )
                        }
                    }
                }
            }

            item(span = StaggeredGridItemSpan.FullLine) {
                Text(
                    text = "For you",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            items(wallpapers, key = { it.id }) { wallpaper ->
                WallpaperThumbnailCard(
                    wallpaper = wallpaper,
                    onClick = { onWallpaperClick(wallpaper) },
                )
            }
        }
    }
}

@Composable
private fun WallpaperThumbnailCard(
    wallpaper: Wallpaper,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column {
            AsyncImage(
                model = wallpaper.thumbnailUrl,
                contentDescription = wallpaper.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
            )
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = wallpaper.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = wallpaper.sourceName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun FeaturedWallpaperCard(
    wallpaper: Wallpaper,
    onClick: () -> Unit,
    onApply: () -> Unit,
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier
            .width(260.dp)
            .height(360.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = wallpaper.thumbnailUrl,
                contentDescription = wallpaper.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.18f)),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = wallpaper.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = wallpaper.sourceName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
            Button(
                onClick = onApply,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
            ) {
                Text("Apply")
            }
        }
    }
}

@Composable
private fun DiscoverScreen(
    wallpapers: List<Wallpaper>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onWallpaperClick: (Wallpaper) -> Unit,
    onApplyWallpaper: (Wallpaper) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "Discover",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
            )
        }
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Search available sources") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearchQueryChange(searchQuery) }),
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Trending", "Abstract", "Nature", "Dark").forEach { tag ->
                    FilterChip(selected = tag == "Trending", onClick = {}, label = { Text(tag) })
                }
            }
        }
        items(wallpapers.take(6)) { wallpaper ->
            WallpaperListRow(
                wallpaper = wallpaper,
                onClick = { onWallpaperClick(wallpaper) },
                onApply = { onApplyWallpaper(wallpaper) },
            )
        }
    }
}

@Composable
private fun SavedScreen(
    items: List<SavedItemEntity>,
    activeFilter: SaveKind?,
    onFilterSelected: (SaveKind?) -> Unit,
    onWallpaperClick: (SavedItemEntity) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "Your collection",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(null, SaveKind.WALLPAPER, SaveKind.LOCKSCREEN, SaveKind.SET).forEach { kind ->
                    val label = when (kind) {
                        null -> "All"
                        SaveKind.WALLPAPER -> "Home"
                        SaveKind.LOCKSCREEN -> "Lock"
                        SaveKind.SET -> "Both"
                    }
                    FilterChip(
                        selected = activeFilter == kind,
                        onClick = { onFilterSelected(kind) },
                        label = { Text(label) },
                    )
                }
            }
        }
        if (items.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Nothing saved yet", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("Find something you love and keep it here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(items, key = { it.wallpaperId }) { item ->
                WallpaperListRow(
                    wallpaper = item.toWallpaper(),
                    onClick = { onWallpaperClick(item) },
                    onApply = {},
                )
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    hapticsEnabled: Boolean,
    dynamicColorsEnabled: Boolean,
    sourceStatuses: List<SourceStatus>,
    onHapticsChanged: (Boolean) -> Unit,
    onDynamicColorsChanged: (Boolean) -> Unit,
    onSourceToggled: (String, Boolean) -> Unit,
) {
    var showSourceReviewDialog by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "Make it yours",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
            )
        }
        item {
            SettingToggleRow(
                title = "Haptic feedback",
                subtitle = "Subtle touch responses",
                checked = hapticsEnabled,
                onCheckedChange = onHapticsChanged,
            )
        }
        item {
            SettingToggleRow(
                title = "Dynamic colors",
                subtitle = "Wallpaper-derived palette",
                checked = dynamicColorsEnabled,
                onCheckedChange = onDynamicColorsChanged,
            )
        }
        item {
            SettingCard(
                title = "Extensions",
                subtitle = "${sourceStatuses.size} sources available",
                iconColor = MaterialTheme.colorScheme.primary,
                onClick = { showSourceReviewDialog = true },
            )
        }
        item {
            SettingCard(
                title = "Storage",
                subtitle = "Current cache: 0 MB",
                iconColor = MaterialTheme.colorScheme.secondary,
                onClick = {},
            )
        }
    }

    if (showSourceReviewDialog) {
        SourceReviewDialog(
            sourceStatuses = sourceStatuses,
            onDismiss = { showSourceReviewDialog = false },
            onToggleSource = onSourceToggled,
        )
    }
}

@Composable
private fun WallpaperListRow(
    wallpaper: Wallpaper,
    onClick: () -> Unit,
    onApply: () -> Unit,
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = wallpaper.thumbnailUrl,
                contentDescription = wallpaper.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(18.dp)),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(wallpaper.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(wallpaper.sourceName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = wallpaper.width?.let { w -> "$w×${wallpaper.height ?: 0}" } ?: "Resolution unknown",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(onClick = onApply, shape = CircleShape) { Text("Apply") }
        }
    }
}

@Composable
private fun WallpaperDetailScreen(
    wallpaper: Wallpaper,
    onBack: () -> Unit,
    onApply: (SaveKind) -> Unit,
    onSave: (SaveKind) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            AsyncImage(
                model = wallpaper.fullResUrl,
                contentDescription = wallpaper.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(440.dp),
            )
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onBack()
                },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(wallpaper.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(wallpaper.sourceName, style = MaterialTheme.typography.bodyMedium)
                wallpaper.width?.let { w ->
                    Text("${w}×${wallpaper.height}", style = MaterialTheme.typography.bodyMedium)
                }
            }
            wallpaper.artist?.let {
                Text("by $it", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onApply(SaveKind.WALLPAPER) }, shape = CircleShape) { Text("Apply") }
                OutlinedButton(onClick = { onSave(SaveKind.WALLPAPER) }, shape = CircleShape) { Text("Save") }
            }
            if (!wallpaper.attributionText.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Attribution", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(wallpaper.attributionText ?: "", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
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
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun SettingCard(
    title: String,
    subtitle: String,
    iconColor: Color,
    onClick: () -> Unit = {},
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.clickable(onClick = onClick),
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
            Spacer(Modifier.size(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SourceReviewDialog(
    sourceStatuses: List<SourceStatus>,
    onDismiss: () -> Unit,
    onToggleSource: (String, Boolean) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sources") },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(sourceStatuses, key = { it.id }) { source ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(source.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = "${source.type} • ${if (source.isOfficialApi) "official API" else "scraped"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = source.isEnabled,
                                onCheckedChange = { enabled -> onToggleSource(source.id, enabled) },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        },
    )
}

@Composable
private fun PaintingsFilterPickerDialog(
    category: String,
    paintingsFilters: PaintingsFilterSelection,
    onDismiss: () -> Unit,
    onUpdated: (PaintingsFilterSelection) -> Unit,
) {
    val options = when (category) {
        "Century" -> PaintingsFilterOptions.centuries
        "Movement" -> PaintingsFilterOptions.movements
        "Artist" -> PaintingsFilterOptions.artists
        else -> emptyList()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(category) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                options.forEach { (label, value) ->
                    val selected = when (category) {
                        "Century" -> paintingsFilters.centuryCategory == value
                        "Movement" -> paintingsFilters.movementCategory == value
                        "Artist" -> paintingsFilters.artistCategory == value
                        else -> false
                    }
                    FilterChip(
                        selected = selected,
                        onClick = {
                            val next = when (category) {
                                "Century" -> paintingsFilters.copy(
                                    centuryCategory = if (selected) null else value,
                                )
                                "Movement" -> paintingsFilters.copy(
                                    movementCategory = if (selected) null else value,
                                )
                                "Artist" -> paintingsFilters.copy(
                                    artistCategory = if (selected) null else value,
                                )
                                else -> paintingsFilters
                            }
                            onUpdated(next)
                        },
                        label = { Text(label) },
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

@Composable
private fun FilterPickerDialog(
    category: String,
    filters: WallpaperFilters,
    sourceStatuses: List<SourceStatus>,
    onDismiss: () -> Unit,
    onUpdated: (WallpaperFilters) -> Unit,
) {
    val options = when (category) {
        "Color" -> listOf("blue", "teal", "red", "green", "orange")
        "Size" -> listOf("HD", "QHD", "4K+")
        "Source" -> sourceStatuses.map { it.id }
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
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                options.forEach { option ->
                    FilterChip(
                        selected = option in selectedValues,
                        onClick = {
                            val next = when (category) {
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

private fun SavedItemEntity.toWallpaper(): Wallpaper = Wallpaper(
    id = wallpaperId,
    sourceId = sourceId,
    thumbnailUrl = thumbnailUrl,
    fullResUrl = fullResUrl,
    width = width,
    height = height,
    artist = artist,
    attributionText = null,
    attributionUrl = null,
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
