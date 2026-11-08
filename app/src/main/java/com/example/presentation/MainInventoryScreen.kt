package com.example.presentation

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.AutoAwesomeMosaic
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.dao.BoxWithCounts
import com.example.data.local.entity.BoxEntity
import com.example.data.local.entity.ItemEntity
import com.example.presentation.components.BackupDialog
import com.example.presentation.components.BoxCard
import com.example.presentation.components.BoxGridCard
import com.example.presentation.components.BreadcrumbBar
import com.example.presentation.components.DeleteConfirmDialog
import com.example.presentation.components.EditBoxDialog
import com.example.presentation.components.EditItemDialog
import com.example.presentation.components.ItemCard
import com.example.presentation.components.ItemGridCard
import com.example.presentation.components.LoanDialog
import com.example.presentation.components.LoansSheet
import com.example.presentation.components.MoveBoxDialog
import com.example.presentation.components.MoveItemDialog
import com.example.presentation.components.ObjectDetailSheet
import com.example.presentation.components.SettingsView
import com.example.presentation.components.StatsDashboardDialog
import com.example.presentation.components.ZonesView
import com.example.presentation.components.parseColorSafe

enum class AppBottomTab {
    CAJAS,
    ZONAS,
    PRESTAMOS,
    AJUSTES
}

enum class GroupingFilterMode {
    POR_ZONAS,
    CATEGORIAS,
    RECIENTES
}

enum class DateFilterMode(val label: String) {
    RECENT("Recientes"),
    LAST_7_DAYS("7 días"),
    LAST_30_DAYS("30 días"),
    TODAY("Hoy")
}

/**
 * Main Inventory Screen matching 'inventario_cuadr_cula_por_zonas_foto_portada_1'
 * from Google Stitch AI Design System
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainInventoryScreen(
    viewModel: InventoryViewModel = viewModel()
) {
    val context = LocalContext.current
    val isGridView by viewModel.isGridView.collectAsState()
    val currentBoxId by viewModel.currentBoxId.collectAsState()
    val currentBox by viewModel.currentBox.collectAsState()
    val breadcrumbs by viewModel.breadcrumbs.collectAsState()
    val rootBoxesWithCounts by viewModel.currentBoxesWithCounts.collectAsState()
    val itemsInBox by viewModel.currentItems.collectAsState()
    val allBoxes by viewModel.allBoxes.collectAsState()
    val categories by viewModel.allCategories.collectAsState()
    val allZones by viewModel.allZones.collectAsState()

    val isSearchActive by viewModel.isSearchActive.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategoryFilter by viewModel.selectedCategoryFilter.collectAsState()
    val searchResultsItems by viewModel.searchResultsItems.collectAsState()
    val searchResultsBoxes by viewModel.searchResultsBoxes.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()

    val totalBoxesCount by viewModel.totalBoxesCount.collectAsState()
    val totalItemsCount by viewModel.totalItemsCount.collectAsState()
    val totalItemUnitsCount by viewModel.totalItemUnitsCount.collectAsState()
    val totalLoanedCount by viewModel.totalLoanedCount.collectAsState()
    val loanedItems by viewModel.loanedItems.collectAsState()
    val categoryStats by viewModel.categoryStats.collectAsState()

    val isBoxDialogOpen by viewModel.isBoxDialogOpen.collectAsState()
    val boxToEdit by viewModel.boxToEdit.collectAsState()
    val isItemDialogOpen by viewModel.isItemDialogOpen.collectAsState()
    val itemToEdit by viewModel.itemToEdit.collectAsState()

    val boxToMove by viewModel.boxToMove.collectAsState()
    val itemToMove by viewModel.itemToMove.collectAsState()
    val itemToLoan by viewModel.itemToLoan.collectAsState()

    val isStatsDialogOpen by viewModel.isStatsDialogOpen.collectAsState()
    val isLoansSheetOpen by viewModel.isLoansSheetOpen.collectAsState()
    val isBackupDialogOpen by viewModel.isBackupDialogOpen.collectAsState()

    val deleteTarget by viewModel.deleteTarget.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var currentBottomTab by remember { mutableStateOf(AppBottomTab.CAJAS) }
    var groupingFilter by remember { mutableStateOf(GroupingFilterMode.POR_ZONAS) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Todo, 1: Subcajas, 2: Objetos
    var fabMenuExpanded by remember { mutableStateOf(false) }
    var isSearchBarVisible by remember { mutableStateOf(false) }
    var showCategoryFilterRow by remember { mutableStateOf(false) }
    var selectedDateFilter by remember { mutableStateOf<DateFilterMode?>(null) }

    // Map of collapsed state per zone name
    val collapsedZones = remember { mutableStateMapOf<String, Boolean>() }
    var allZonesCollapsed by remember { mutableStateOf(false) }
    var createBoxInitialZone by remember { mutableStateOf<String?>(null) }

    // Selected item for rich Object Detail Sheet (Stitch AI Modal 2)
    var selectedItemForDetail by remember { mutableStateOf<ItemEntity?>(null) }

    // Map to cache box paths for search results and item badges
    val boxPaths = remember { mutableStateMapOf<String, String>() }
    LaunchedEffect(searchResultsItems, itemsInBox) {
        val allVisibleItemBoxes = (searchResultsItems.map { it.boxId } + itemsInBox.map { it.boxId }).distinct()
        for (boxId in allVisibleItemBoxes) {
            if (!boxPaths.containsKey(boxId)) {
                boxPaths[boxId] = viewModel.getBoxBreadcrumbPath(boxId)
            }
        }
    }

    // Intercept back button for nested navigation, tabs, search, and detail modal
    BackHandler(enabled = selectedItemForDetail != null || isSearchActive || searchQuery.isNotBlank() || isSearchBarVisible || selectedCategoryFilter != null || selectedDateFilter != null || currentBottomTab != AppBottomTab.CAJAS || currentBoxId != null) {
        when {
            selectedItemForDetail != null -> {
                selectedItemForDetail = null
            }
            selectedDateFilter != null || selectedCategoryFilter != null -> {
                selectedDateFilter = null
                viewModel.setCategoryFilter(null)
            }
            searchQuery.isNotBlank() -> {
                viewModel.setSearchQuery("")
                viewModel.setSearchActive(false)
            }
            isSearchBarVisible || isSearchActive -> {
                isSearchBarVisible = false
                viewModel.setSearchActive(false)
            }
            currentBottomTab != AppBottomTab.CAJAS -> {
                currentBottomTab = AppBottomTab.CAJAS
            }
            else -> {
                viewModel.navigateBack()
            }
        }
    }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    val isFiltering = searchQuery.isNotBlank() || isSearchActive || selectedCategoryFilter != null || selectedDateFilter != null

    Scaffold(
        topBar = {
            // =====================================================================
            // STITCH AI EXACT HEADER (inventario_cuadr_cula_por_zonas_foto_portada_1)
            // =====================================================================
            Surface(
                color = Color.Black.copy(alpha = 0.92f),
                tonalElevation = 3.dp,
                shadowElevation = 4.dp,
                border = BorderStroke(1.dp, Color(0xFF2C2622).copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("header_container")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                ) {
                    // Header Bar (h-16 in prototype)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left Identity
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            if (currentBoxId != null || isFiltering || currentBottomTab != AppBottomTab.CAJAS) {
                                IconButton(
                                    onClick = {
                                        if (isFiltering) {
                                            viewModel.setSearchQuery("")
                                            viewModel.setSearchActive(false)
                                            isSearchBarVisible = false
                                        } else if (currentBottomTab != AppBottomTab.CAJAS) {
                                            currentBottomTab = AppBottomTab.CAJAS
                                        } else {
                                            viewModel.navigateBack()
                                        }
                                    },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF181512))
                                        .border(1.dp, Color(0xFF2C2622), CircleShape)
                                        .testTag("nav_back_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Atrás",
                                        tint = Color(0xFFECE0DA),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF241B14))
                                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Inventory2,
                                        contentDescription = "Logo",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = when {
                                        isFiltering -> "Búsqueda"
                                        currentBottomTab == AppBottomTab.ZONAS -> "Administrar Zonas"
                                        currentBottomTab == AppBottomTab.PRESTAMOS -> "Gestión de Préstamos"
                                        currentBottomTab == AppBottomTab.AJUSTES -> "Configuración"
                                        else -> currentBox?.name ?: "Inventario de Cajas"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = when {
                                        isFiltering -> "${searchResultsBoxes.size} cajas · ${searchResultsItems.size} objetos"
                                        currentBottomTab == AppBottomTab.ZONAS -> "${allZones.size} zonas · $totalBoxesCount cajas"
                                        currentBottomTab == AppBottomTab.PRESTAMOS -> "$totalLoanedCount en préstamo"
                                        currentBottomTab == AppBottomTab.AJUSTES -> "Preferencias y respaldos"
                                        currentBoxId == null -> "Cajas"
                                        else -> "${rootBoxesWithCounts.size} subcajas · ${itemsInBox.size} objetos"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFA89F95),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Right Action Controls matching prototype: View Switcher Pill + Search + Filter
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (currentBottomTab == AppBottomTab.CAJAS) {
                                // View Switcher Capsule (Mosaico / Cuadrícula / Lista)
                                Surface(
                                    shape = RoundedCornerShape(9999.dp),
                                    color = Color(0xFF181512),
                                    border = BorderStroke(1.dp, Color(0xFF2C2622)),
                                    modifier = Modifier.padding(end = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Cuadrícula con portada (Active Amber in design)
                                        IconButton(
                                            onClick = { if (!isGridView) viewModel.toggleViewMode() },
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(if (isGridView) MaterialTheme.colorScheme.primary else Color.Transparent)
                                                .testTag("toggle_view_mode_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.GridView,
                                                contentDescription = "Cuadrícula con portada",
                                                tint = if (isGridView) Color(0xFF2F1500) else Color(0xFFA89F95),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        // Lista compacta
                                        IconButton(
                                            onClick = { if (isGridView) viewModel.toggleViewMode() },
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(if (!isGridView) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ViewList,
                                                contentDescription = "Lista compacta",
                                                tint = if (!isGridView) Color(0xFF2F1500) else Color(0xFFA89F95),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                // Search Button
                                IconButton(
                                    onClick = {
                                        isSearchBarVisible = !isSearchBarVisible
                                        if (isSearchBarVisible) viewModel.setSearchActive(true)
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Buscar",
                                        tint = if (isSearchBarVisible || isSearchActive) MaterialTheme.colorScheme.primary else Color(0xFFA89F95),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Filter Button (Tune)
                                IconButton(
                                    onClick = { showCategoryFilterRow = !showCategoryFilterRow },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = "Filtros",
                                        tint = if (showCategoryFilterRow || selectedCategoryFilter != null) MaterialTheme.colorScheme.primary else Color(0xFFA89F95),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Search input & Filter Chips below it
                    AnimatedVisibility(
                        visible = isSearchBarVisible || isFiltering || showCategoryFilterRow,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                        ) {
                            // Search input field with subtle border-radius and background-color
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = {
                                        viewModel.setSearchQuery(it)
                                        if (it.isNotBlank()) {
                                            viewModel.setSearchActive(true)
                                        }
                                    },
                                    placeholder = {
                                        Text(
                                            text = if (currentBox != null) "Buscar en «${currentBox!!.name}»..." else "Buscar caja, objeto o etiqueta...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFFA89F95),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    singleLine = true,
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = "Buscar",
                                            tint = if (searchQuery.isNotBlank()) MaterialTheme.colorScheme.primary else Color(0xFFA89F95),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    trailingIcon = {
                                        if (searchQuery.isNotEmpty()) {
                                            IconButton(
                                                onClick = {
                                                    viewModel.setSearchQuery("")
                                                    viewModel.setSearchActive(false)
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Clear,
                                                    contentDescription = "Limpiar",
                                                    tint = Color(0xFFA89F95),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFF241E19),
                                        unfocusedContainerColor = Color(0xFF1E1814),
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = Color(0xFF382E26),
                                        focusedTextColor = Color(0xFFFBF8F5),
                                        unfocusedTextColor = Color(0xFFECE0DA),
                                        cursorColor = MaterialTheme.colorScheme.primary
                                    ),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(
                                        onSearch = {
                                            if (searchQuery.isNotBlank()) {
                                                viewModel.recordSearchQuery(searchQuery)
                                            }
                                        }
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("search_text_field")
                                )
                            }

                            // Row of filter chips below search input with scale and ripple animation
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                    .testTag("header_filter_chips_row"),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Filter: "Todas"
                                val isAllSelected = selectedCategoryFilter == null && selectedDateFilter == null
                                val allScale by animateFloatAsState(
                                    targetValue = if (isAllSelected) 1.05f else 1.0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    ),
                                    label = "scale_all"
                                )
                                FilterChip(
                                    selected = isAllSelected,
                                    onClick = {
                                        viewModel.setCategoryFilter(null)
                                        selectedDateFilter = null
                                    },
                                    shape = RoundedCornerShape(9999.dp),
                                    label = { Text("Todas", style = MaterialTheme.typography.labelSmall) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = Color(0xFF1E1814),
                                        labelColor = Color(0xFFECE0DA),
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = Color(0xFF2F1500)
                                    ),
                                    modifier = Modifier
                                        .graphicsLayer {
                                            scaleX = allScale
                                            scaleY = allScale
                                        }
                                        .testTag("filter_chip_all")
                                )

                                // Date filter chips
                                DateFilterMode.entries.forEach { dateMode ->
                                    val isSelected = selectedDateFilter == dateMode
                                    val scale by animateFloatAsState(
                                        targetValue = if (isSelected) 1.05f else 1.0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        ),
                                        label = "scale_date_${dateMode.name}"
                                    )
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            selectedDateFilter = if (isSelected) null else dateMode
                                        },
                                        shape = RoundedCornerShape(9999.dp),
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Schedule,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        },
                                        label = { Text(dateMode.label, style = MaterialTheme.typography.labelSmall) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            containerColor = Color(0xFF1E1814),
                                            labelColor = Color(0xFFECE0DA),
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = Color(0xFF2F1500)
                                        ),
                                        modifier = Modifier
                                            .graphicsLayer {
                                                scaleX = scale
                                                scaleY = scale
                                            }
                                            .testTag("filter_chip_date_${dateMode.name}")
                                    )
                                }

                                // Category filter chips
                                categories.forEach { cat ->
                                    val isSelected = selectedCategoryFilter == cat.id
                                    val scale by animateFloatAsState(
                                        targetValue = if (isSelected) 1.05f else 1.0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        ),
                                        label = "scale_cat_${cat.id}"
                                    )
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            viewModel.setCategoryFilter(if (isSelected) null else cat.id)
                                        },
                                        shape = RoundedCornerShape(9999.dp),
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Category,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        },
                                        label = { Text(cat.name, style = MaterialTheme.typography.labelSmall) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            containerColor = Color(0xFF1E1814),
                                            labelColor = Color(0xFFECE0DA),
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = Color(0xFF2F1500)
                                        ),
                                        modifier = Modifier
                                            .graphicsLayer {
                                                scaleX = scale
                                                scaleY = scale
                                            }
                                            .testTag("filter_chip_cat_${cat.id}")
                                    )
                                }
                            }
                        }
                    }

                    // Breadcrumb Bar if inside a subbox
                    if (currentBoxId != null || breadcrumbs.isNotEmpty()) {
                        BreadcrumbBar(
                            breadcrumbs = breadcrumbs,
                            onRootClick = {
                                if (isFiltering) {
                                    viewModel.setSearchQuery("")
                                    viewModel.setSearchActive(false)
                                }
                                viewModel.navigateToRoot()
                            },
                            onCrumbClick = { box ->
                                if (isFiltering) {
                                    viewModel.setSearchQuery("")
                                    viewModel.setSearchActive(false)
                                }
                                viewModel.openBox(box.id)
                            }
                        )
                    }
                }
            }
        },
        bottomBar = {
            // =====================================================================
            // BOTTOM DOCK (Matching line 370 of inventario_cuadr_cula_por_zonas)
            // =====================================================================
            Surface(
                color = Color.Black.copy(alpha = 0.92f),
                border = BorderStroke(1.dp, Color(0xFF2C2622).copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .height(64.dp)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tab 1: Cajas (active by default)
                    val isCajasActive = currentBottomTab == AppBottomTab.CAJAS
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                if (isFiltering) {
                                    viewModel.setSearchQuery("")
                                    viewModel.setSearchActive(false)
                                    isSearchBarVisible = false
                                }
                                currentBottomTab = AppBottomTab.CAJAS
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = "Cajas",
                            tint = if (isCajasActive) MaterialTheme.colorScheme.primary else Color(0xFFA89F95),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Cajas",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isCajasActive) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCajasActive) MaterialTheme.colorScheme.primary else Color(0xFFA89F95),
                            fontSize = 11.sp
                        )
                    }

                    // Tab 2: Zonas
                    val isZonasActive = currentBottomTab == AppBottomTab.ZONAS
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { currentBottomTab = AppBottomTab.ZONAS }
                            .padding(vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridView,
                            contentDescription = "Zonas",
                            tint = if (isZonasActive) MaterialTheme.colorScheme.primary else Color(0xFFA89F95),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Zonas",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isZonasActive) FontWeight.Bold else FontWeight.Normal,
                            color = if (isZonasActive) MaterialTheme.colorScheme.primary else Color(0xFFA89F95),
                            fontSize = 11.sp
                        )
                    }

                    // Tab 3: Préstamos
                    val isPrestamosActive = currentBottomTab == AppBottomTab.PRESTAMOS
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { currentBottomTab = AppBottomTab.PRESTAMOS }
                            .padding(vertical = 4.dp)
                    ) {
                        Box {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = "Préstamos",
                                tint = if (isPrestamosActive) MaterialTheme.colorScheme.primary else Color(0xFFA89F95),
                                modifier = Modifier.size(22.dp)
                            )
                            if (totalLoanedCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFF59E0B))
                                        .align(Alignment.TopEnd)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Préstamos",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isPrestamosActive) FontWeight.Bold else FontWeight.Normal,
                            color = if (isPrestamosActive) MaterialTheme.colorScheme.primary else Color(0xFFA89F95),
                            fontSize = 11.sp
                        )
                    }

                    // Tab 4: Ajustes
                    val isAjustesActive = currentBottomTab == AppBottomTab.AJUSTES
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { currentBottomTab = AppBottomTab.AJUSTES }
                            .padding(vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Ajustes",
                            tint = if (isAjustesActive) MaterialTheme.colorScheme.primary else Color(0xFFA89F95),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Ajustes",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isAjustesActive) FontWeight.Bold else FontWeight.Normal,
                            color = if (isAjustesActive) MaterialTheme.colorScheme.primary else Color(0xFFA89F95),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (currentBottomTab == AppBottomTab.CAJAS) {
                Box {
                    if (currentBoxId == null && !isFiltering) {
                        Button(
                            onClick = { viewModel.openCreateBoxDialog() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color(0xFF2F1500)
                            ),
                            shape = RoundedCornerShape(18.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                            modifier = Modifier
                                .height(52.dp)
                                .testTag("add_root_box_fab")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Nueva Caja", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    } else {
                        FloatingActionButton(
                            onClick = { fabMenuExpanded = true },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color(0xFF2F1500),
                            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .padding(bottom = 8.dp)
                                .testTag("box_add_menu_fab")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Añadir a la caja")
                        }

                        DropdownMenu(
                            expanded = fabMenuExpanded,
                            onDismissRequest = { fabMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Nuevo Objeto") },
                                leadingIcon = { Icon(Icons.Default.PostAdd, contentDescription = null) },
                                onClick = {
                                    fabMenuExpanded = false
                                    viewModel.openCreateItemDialog()
                                },
                                modifier = Modifier.testTag("menu_add_item")
                            )
                            DropdownMenuItem(
                                text = { Text("Nueva Subcaja") },
                                leadingIcon = { Icon(Icons.Default.Inventory2, contentDescription = null) },
                                onClick = {
                                    fabMenuExpanded = false
                                    viewModel.openCreateBoxDialog()
                                },
                                modifier = Modifier.testTag("menu_add_subbox")
                            )
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentBottomTab) {
                // =============================================================
                // TAB 1: ZONAS
                // =============================================================
                AppBottomTab.ZONAS -> {
                    ZonesView(
                        zones = allZones,
                        allBoxes = allBoxes,
                        totalItemsCount = totalItemsCount,
                        onZoneClick = { zoneName ->
                            viewModel.setSearchQuery(zoneName)
                            viewModel.setSearchActive(true)
                            currentBottomTab = AppBottomTab.CAJAS
                        },
                        onBoxClick = { boxId ->
                            viewModel.openBox(boxId)
                            currentBottomTab = AppBottomTab.CAJAS
                        },
                        onCreateZone = { name, colorHex ->
                            viewModel.createZone(name, colorHex)
                        },
                        onRenameZone = { oldName, newName ->
                            viewModel.renameZone(oldName, newName)
                        },
                        onDeleteZone = { name ->
                            viewModel.deleteZone(name)
                        }
                    )
                }

                // =============================================================
                // TAB 2: PRÉSTAMOS
                // =============================================================
                AppBottomTab.PRESTAMOS -> {
                    LoansSheet(
                        loanedItems = loanedItems,
                        onReturnItem = { item -> viewModel.returnItem(item) },
                        onDismiss = { currentBottomTab = AppBottomTab.CAJAS }
                    )
                }

                // =============================================================
                // TAB 3: AJUSTES
                // =============================================================
                AppBottomTab.AJUSTES -> {
                    SettingsView(
                        onOpenBackup = { viewModel.openBackupDialog() },
                        onOpenStats = { viewModel.openStatsDialog() }
                    )
                }

                // =============================================================
                // TAB 0: CAJAS (MAIN CUADRÍCULA POR ZONAS CON FOTO PORTADA)
                // =============================================================
                AppBottomTab.CAJAS -> {
                    if (isFiltering) {
                        val now = System.currentTimeMillis()
                        val dayMs = 24L * 60 * 60 * 1000
                        val displayedBoxes = remember(searchResultsBoxes, selectedDateFilter) {
                            when (selectedDateFilter) {
                                DateFilterMode.TODAY -> searchResultsBoxes.filter { now - it.createdAt < dayMs }
                                DateFilterMode.LAST_7_DAYS -> searchResultsBoxes.filter { now - it.createdAt < 7 * dayMs }
                                DateFilterMode.LAST_30_DAYS -> searchResultsBoxes.filter { now - it.createdAt < 30 * dayMs }
                                DateFilterMode.RECENT -> searchResultsBoxes.sortedByDescending { it.createdAt }
                                null -> searchResultsBoxes
                            }
                        }
                        val displayedItems = remember(searchResultsItems, selectedDateFilter) {
                            when (selectedDateFilter) {
                                DateFilterMode.TODAY -> searchResultsItems.filter { now - it.createdAt < dayMs }
                                DateFilterMode.LAST_7_DAYS -> searchResultsItems.filter { now - it.createdAt < 7 * dayMs }
                                DateFilterMode.LAST_30_DAYS -> searchResultsItems.filter { now - it.createdAt < 30 * dayMs }
                                DateFilterMode.RECENT -> searchResultsItems.sortedByDescending { it.createdAt }
                                null -> searchResultsItems
                            }
                        }

                        // Search / Filter Results
                        if (searchQuery.isBlank() && selectedCategoryFilter == null && selectedDateFilter == null) {
                            if (recentSearches.isNotEmpty()) {
                                LazyColumn(
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    item {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.History,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Búsquedas recientes",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                            TextButton(onClick = { viewModel.clearSearchHistory() }) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteSweep,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Borrar historial", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }

                                    items(recentSearches, key = { it.query }) { item ->
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color(0xFF14110E),
                                            border = BorderStroke(1.dp, Color(0xFF2C2622)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    viewModel.setSearchQuery(item.query)
                                                    viewModel.recordSearchQuery(item.query)
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    modifier = Modifier.weight(1f),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.History,
                                                        contentDescription = null,
                                                        tint = Color(0xFFA89F95),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Text(
                                                        text = item.query,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = Color.White,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { viewModel.deleteRecentSearch(item.query) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Clear,
                                                        contentDescription = "Eliminar de historial",
                                                        tint = Color(0xFFA89F95),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                            modifier = Modifier.size(56.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "Escribe para buscar cajas u objetos",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = Color(0xFFA89F95)
                                        )
                                    }
                                }
                            }
                        } else if (displayedItems.isEmpty() && displayedBoxes.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (searchQuery.isNotBlank()) "No se encontraron coincidencias para «$searchQuery»" else "No se encontraron elementos con los filtros seleccionados",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color(0xFFA89F95)
                                )
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                // Cajas encontradas
                                if (displayedBoxes.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = "Cajas encontradas (${displayedBoxes.size})",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                                        )
                                    }

                                    items(displayedBoxes, key = { it.id }) { box ->
                                        BoxGridCard(
                                            boxWithCounts = BoxWithCounts(box, 0, 0),
                                            onClick = {
                                                if (searchQuery.isNotBlank()) viewModel.recordSearchQuery(searchQuery)
                                                viewModel.setSearchQuery("")
                                                viewModel.setSearchActive(false)
                                                isSearchBarVisible = false
                                                viewModel.openBox(box.id)
                                            },
                                            onMove = { viewModel.openMoveBoxDialog(box) },
                                            onEdit = { viewModel.openEditBoxDialog(box) },
                                            onDelete = { viewModel.requestDeleteBox(BoxWithCounts(box, 0, 0)) }
                                        )
                                    }
                                }

                                // Objetos encontrados
                                if (displayedItems.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = "Objetos encontrados (${displayedItems.size})",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                        )
                                    }

                                    items(displayedItems, key = { it.id }) { item ->
                                        val cat = categories.find { it.id == item.categoryId }
                                        val path = boxPaths[item.boxId] ?: "Cargando ubicación…"

                                        Card(
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF12100E)),
                                            border = BorderStroke(1.dp, Color(0xFF2C2622)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { selectedItemForDetail = item }
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp)) {
                                                ItemCard(
                                                    item = item,
                                                    category = cat,
                                                    onQuantityChange = { delta -> viewModel.adjustItemQuantity(item, delta) },
                                                    onMove = { viewModel.openMoveItemDialog(item) },
                                                    onEdit = { viewModel.openEditItemDialog(item) },
                                                    onDelete = { viewModel.requestDeleteItem(item) },
                                                    onLoan = { viewModel.openLoanDialog(item) },
                                                    onReturnLoan = { viewModel.returnItem(item) },
                                                    onClick = { selectedItemForDetail = item }
                                                )

                                                Spacer(modifier = Modifier.height(8.dp))

                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = Color(0xFF241B14),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            viewModel.setSearchQuery("")
                                                            viewModel.setSearchActive(false)
                                                            isSearchBarVisible = false
                                                            viewModel.openBox(item.boxId)
                                                        }
                                                        .testTag("jump_to_box_${item.boxId}")
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.LocationOn,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = "Ubicación: $path",
                                                            style = MaterialTheme.typography.labelMedium,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else if (currentBoxId == null) {
                        // =============================================================
                        // VISTA PRINCIPAL RAÍZ: CUADRÍCULA POR ZONAS (Stitch AI Layout)
                        // =============================================================
                        val zonePalette = remember {
                            listOf(
                                Color(0xFFF59E0B), // Amber
                                Color(0xFF3B82F6), // Blue
                                Color(0xFF10B981), // Emerald
                                Color(0xFFA855F7), // Purple
                                Color(0xFFEF4444), // Red
                                Color(0xFF14B8A6), // Teal
                                Color(0xFFC26343), // Terracotta
                                Color(0xFFEAB308), // Yellow
                                Color(0xFF6366F1)  // Indigo
                            )
                        }

                        val zoneGroups = remember(rootBoxesWithCounts, allBoxes, allZones) {
                            val assignedZones = rootBoxesWithCounts.mapNotNull { it.box.zone?.trim()?.takeIf { z -> z.isNotEmpty() } }.distinct()
                            val allKnownZones = (allZones + assignedZones).distinct().sorted()

                            val map = linkedMapOf<String, MutableList<BoxWithCounts>>()
                            allKnownZones.forEach { z -> map[z] = mutableListOf() }
                            val unassigned = mutableListOf<BoxWithCounts>()

                            rootBoxesWithCounts.forEach { bwc ->
                                val z = bwc.box.zone?.trim()
                                if (!z.isNullOrBlank()) {
                                    map.getOrPut(z) { mutableListOf() }.add(bwc)
                                } else {
                                    unassigned.add(bwc)
                                }
                            }

                            val result = mutableListOf<Pair<String, List<BoxWithCounts>>>()
                            for ((zName, boxes) in map) {
                                if (boxes.isNotEmpty() || allZones.contains(zName)) {
                                    result.add(zName to boxes)
                                }
                            }
                            if (unassigned.isNotEmpty()) {
                                result.add("Sin estancia asignada" to unassigned)
                            }
                            result
                        }

                        val activeZonesCount = remember(zoneGroups) { zoneGroups.count { it.second.isNotEmpty() } }

                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Fila 1: Filtros de Vista (Por Zonas / Categorías / Recientes) + Botón Colapsar Todo
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .weight(1f)
                                            .horizontalScroll(rememberScrollState())
                                    ) {
                                        // "Por Zonas" chip
                                        Surface(
                                            shape = RoundedCornerShape(9999.dp),
                                            color = if (groupingFilter == GroupingFilterMode.POR_ZONAS) MaterialTheme.colorScheme.primary else Color(0xFF181512),
                                            border = if (groupingFilter == GroupingFilterMode.POR_ZONAS) null else BorderStroke(1.dp, Color(0xFF2C2622)),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(9999.dp))
                                                .clickable { groupingFilter = GroupingFilterMode.POR_ZONAS }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PhotoLibrary,
                                                    contentDescription = null,
                                                    tint = if (groupingFilter == GroupingFilterMode.POR_ZONAS) Color(0xFF2F1500) else Color(0xFFA89F95),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Por Zonas",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (groupingFilter == GroupingFilterMode.POR_ZONAS) Color(0xFF2F1500) else Color(0xFFA89F95)
                                                )
                                            }
                                        }

                                        // "Categorías" chip
                                        Surface(
                                            shape = RoundedCornerShape(9999.dp),
                                            color = if (groupingFilter == GroupingFilterMode.CATEGORIAS) MaterialTheme.colorScheme.primary else Color(0xFF181512),
                                            border = if (groupingFilter == GroupingFilterMode.CATEGORIAS) null else BorderStroke(1.dp, Color(0xFF2C2622)),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(9999.dp))
                                                .clickable {
                                                    groupingFilter = GroupingFilterMode.CATEGORIAS
                                                    showCategoryFilterRow = true
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Category,
                                                    contentDescription = null,
                                                    tint = if (groupingFilter == GroupingFilterMode.CATEGORIAS) Color(0xFF2F1500) else Color(0xFFA89F95),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Categorías",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = if (groupingFilter == GroupingFilterMode.CATEGORIAS) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (groupingFilter == GroupingFilterMode.CATEGORIAS) Color(0xFF2F1500) else Color(0xFFA89F95)
                                                )
                                            }
                                        }

                                        // "Recientes" chip
                                        Surface(
                                            shape = RoundedCornerShape(9999.dp),
                                            color = if (groupingFilter == GroupingFilterMode.RECIENTES) MaterialTheme.colorScheme.primary else Color(0xFF181512),
                                            border = if (groupingFilter == GroupingFilterMode.RECIENTES) null else BorderStroke(1.dp, Color(0xFF2C2622)),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(9999.dp))
                                                .clickable { groupingFilter = GroupingFilterMode.RECIENTES }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Schedule,
                                                    contentDescription = null,
                                                    tint = if (groupingFilter == GroupingFilterMode.RECIENTES) Color(0xFF2F1500) else Color(0xFFA89F95),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Recientes",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = if (groupingFilter == GroupingFilterMode.RECIENTES) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (groupingFilter == GroupingFilterMode.RECIENTES) Color(0xFF2F1500) else Color(0xFFA89F95)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Botón Contraer / Expandir todas las zonas
                                    IconButton(
                                        onClick = {
                                            allZonesCollapsed = !allZonesCollapsed
                                            zoneGroups.forEach { (zoneName, _) ->
                                                collapsedZones[zoneName] = allZonesCollapsed
                                            }
                                        },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF181512))
                                            .border(1.dp, Color(0xFF2C2622), CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = if (allZonesCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                                            contentDescription = "Expandir o contraer todas las zonas",
                                            tint = if (allZonesCollapsed) MaterialTheme.colorScheme.primary else Color(0xFFA89F95),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            // Fila 2: Subtotal y Reordenar
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "$totalBoxesCount Cajas",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "· $activeZonesCount ${if (activeZonesCount == 1) "Zona activa" else "Zonas activas"}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFA89F95)
                                        )
                                    }

                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { currentBottomTab = AppBottomTab.ZONAS }
                                            .padding(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Gestionar",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Icon(
                                            imageVector = Icons.Default.SwapVert,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            // Estado vacío si no hay cajas
                            if (rootBoxesWithCounts.isEmpty()) {
                                item {
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = Color(0xFF14110E),
                                        border = BorderStroke(1.dp, Color(0xFF2C2622)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(32.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Inventory2,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                                modifier = Modifier.size(56.dp)
                                            )
                                            Spacer(modifier = Modifier.height(14.dp))
                                            Text(
                                                text = "No tienes cajas todavía",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "Pulsa «Nueva Caja» para empezar a organizar tu inventario por zonas.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color(0xFFA89F95),
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }

                            // =============================================================
                            // ZONE ACCORDIONS: Cada zona agrupa sus cajas
                            // =============================================================
                            zoneGroups.forEach { (zoneName, displayBoxes) ->
                                item(key = "zone_$zoneName") {
                                    val isCollapsed = collapsedZones[zoneName] ?: false
                                    val zoneColorIndex = (Math.abs(zoneName.hashCode()) % zonePalette.size)
                                    val zoneTagColor = zonePalette[zoneColorIndex]

                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Header de Zona Acordeón (Nombre de la Zona/Estancia)
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = Color.Transparent,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .clickable {
                                                    collapsedZones[zoneName] = !isCollapsed
                                                }
                                                .padding(horizontal = 4.dp, vertical = 4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    // Badge de Zona
                                                    Box(
                                                        modifier = Modifier
                                                            .size(34.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(0xFF1E1915))
                                                            .border(1.dp, Color(0xFF2C2622), CircleShape),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.LocationOn,
                                                            contentDescription = null,
                                                            tint = zoneTagColor,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(10.dp))

                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            Text(
                                                                text = zoneName,
                                                                style = MaterialTheme.typography.titleMedium,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color.White,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                            Surface(
                                                                shape = RoundedCornerShape(9999.dp),
                                                                color = Color(0xFF1C1713),
                                                                border = BorderStroke(1.dp, Color(0xFF2C2622))
                                                            ) {
                                                                Text(
                                                                    text = "${displayBoxes.size} ${if (displayBoxes.size == 1) "caja" else "cajas"}",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = Color(0xFFECE0DA),
                                                                    fontSize = 10.sp,
                                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                                )
                                                            }
                                                        }
                                                        Text(
                                                            text = if (displayBoxes.isEmpty()) "Sin cajas asignadas todavía" else "${displayBoxes.size} ${if (displayBoxes.size == 1) "caja en esta estancia" else "cajas en esta estancia"}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = Color(0xFFA89F95),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }

                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    val rotation by animateFloatAsState(targetValue = if (isCollapsed) -90f else 0f)
                                                    Icon(
                                                        imageVector = Icons.Default.ExpandMore,
                                                        contentDescription = if (isCollapsed) "Expandir" else "Contraer",
                                                        tint = Color(0xFFA89F95),
                                                        modifier = Modifier
                                                            .size(20.dp)
                                                            .rotate(rotation)
                                                    )
                                                }
                                            }
                                        }

                                        // Contenido Expandido: Cuadrícula de 2 columnas de cajas con foto portada
                                        AnimatedVisibility(
                                            visible = !isCollapsed,
                                            enter = fadeIn(),
                                            exit = fadeOut()
                                        ) {
                                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                // Grilla en filas de 2 elementos
                                                val chunkedBoxes = displayBoxes.chunked(2)
                                                chunkedBoxes.forEach { rowBoxes ->
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                    ) {
                                                        rowBoxes.forEach { bwc ->
                                                            Box(modifier = Modifier.weight(1f)) {
                                                                BoxGridCard(
                                                                    boxWithCounts = bwc,
                                                                    onClick = { viewModel.openBox(bwc.box.id) },
                                                                    onMove = { viewModel.openMoveBoxDialog(bwc.box) },
                                                                    onEdit = { viewModel.openEditBoxDialog(bwc.box) },
                                                                    onDelete = { viewModel.requestDeleteBox(bwc) }
                                                                )
                                                            }
                                                        }
                                                        // Si es fila impar, dejamos la tarjeta de asignar caja en el segundo hueco
                                                        if (rowBoxes.size == 1) {
                                                            Surface(
                                                                shape = RoundedCornerShape(16.dp),
                                                                color = Color(0xFF12100E),
                                                                border = BorderStroke(1.dp, Color(0xFF2C2622)),
                                                                modifier = Modifier
                                                                    .weight(1f)
                                                                    .clip(RoundedCornerShape(16.dp))
                                                                    .clickable {
                                                                        createBoxInitialZone = if (zoneName == "Sin estancia asignada") null else zoneName
                                                                        viewModel.openCreateBoxDialog()
                                                                    }
                                                            ) {
                                                                Column(
                                                                    modifier = Modifier
                                                                        .fillMaxWidth()
                                                                        .padding(16.dp),
                                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                                    verticalArrangement = Arrangement.Center
                                                                ) {
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .size(46.dp)
                                                                            .clip(CircleShape)
                                                                            .background(Color(0xFF1E1915))
                                                                            .border(1.dp, Color(0xFF2C2622), CircleShape),
                                                                        contentAlignment = Alignment.Center
                                                                    ) {
                                                                        Icon(
                                                                            imageVector = Icons.Default.Add,
                                                                            contentDescription = "Asignar",
                                                                            tint = MaterialTheme.colorScheme.primary,
                                                                            modifier = Modifier.size(24.dp)
                                                                        )
                                                                    }
                                                                    Spacer(modifier = Modifier.height(8.dp))
                                                                    Text(
                                                                        text = "Asignar Caja",
                                                                        style = MaterialTheme.typography.titleSmall,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = Color.White
                                                                    )
                                                                    Text(
                                                                        text = "Añadir a $zoneName",
                                                                        style = MaterialTheme.typography.labelSmall,
                                                                        color = Color(0xFFA89F95),
                                                                        fontSize = 10.sp,
                                                                        maxLines = 1,
                                                                        overflow = TextOverflow.Ellipsis
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                                // Si el número de cajas es par (por ejemplo 0 o 2 o 4), añadimos una fila con botón "+ Añadir caja a esta zona"
                                                if (displayBoxes.size % 2 == 0) {
                                                    Surface(
                                                        shape = RoundedCornerShape(14.dp),
                                                        color = Color(0xFF12100E),
                                                        border = BorderStroke(1.dp, Color(0xFF2C2622)),
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(14.dp))
                                                            .clickable {
                                                                createBoxInitialZone = if (zoneName == "Sin estancia asignada") null else zoneName
                                                                viewModel.openCreateBoxDialog()
                                                            }
                                                    ) {
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                                            horizontalArrangement = Arrangement.Center,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Add,
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Text(
                                                                text = "Añadir caja a $zoneName",
                                                                style = MaterialTheme.typography.labelMedium,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = Color.White
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // =============================================================
                        // VISTA DENTRO DE UNA CAJA (Navegación con subcajas / objetos)
                        // =============================================================
                        val activeBox = currentBox
                        if (activeBox == null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.material3.CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        } else {
                            val accentColor = remember(activeBox.colorHex) { parseColorSafe(activeBox.colorHex) }

                            LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Banner de caja activa
                            item {
                                Card(
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF14110E)),
                                    border = BorderStroke(1.dp, Color(0xFF28211B)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(46.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(accentColor.copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Inventory2,
                                                    contentDescription = null,
                                                    tint = accentColor,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = activeBox.name,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (!activeBox.description.isNullOrBlank()) {
                                                    Text(
                                                        text = activeBox.description,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = Color(0xFFA89F95),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }

                                            IconButton(onClick = { viewModel.openEditBoxDialog(activeBox) }) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Editar detalles",
                                                    tint = Color(0xFFA89F95)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Quick Action Bar
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = { viewModel.openMoveBoxDialog(activeBox) },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFF201811),
                                                    contentColor = MaterialTheme.colorScheme.primary
                                                ),
                                                border = BorderStroke(1.dp, Color(0xFF3E2C1E)),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(40.dp)
                                            ) {
                                                Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Mover de Zona", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                            }

                                            IconButton(
                                                onClick = {
                                                    val shareIntent = Intent().apply {
                                                        action = Intent.ACTION_SEND
                                                        type = "text/plain"
                                                        putExtra(Intent.EXTRA_TEXT, "Caja: ${activeBox.name}\n${activeBox.description ?: ""}\nContenido: ${itemsInBox.size} objetos")
                                                    }
                                                    context.startActivity(Intent.createChooser(shareIntent, "Compartir caja"))
                                                },
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(Color(0xFF201811))
                                                    .border(1.dp, Color(0xFF3E2C1E), RoundedCornerShape(12.dp))
                                            ) {
                                                Icon(Icons.Default.Share, contentDescription = "Compartir", tint = Color(0xFFECE0DA), modifier = Modifier.size(18.dp))
                                            }

                                            Button(
                                                onClick = { viewModel.openCreateItemDialog() },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.primary,
                                                    contentColor = Color(0xFF2F1500)
                                                ),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.height(40.dp)
                                            ) {
                                                Icon(Icons.Default.AddBox, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("+ Objeto", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            // Primary Tab Row (Todo / Subcajas / Objetos)
                            item {
                                PrimaryTabRow(
                                    selectedTabIndex = selectedTab,
                                    containerColor = Color.Black,
                                    contentColor = MaterialTheme.colorScheme.primary
                                ) {
                                    Tab(
                                        selected = selectedTab == 0,
                                        onClick = { selectedTab = 0 },
                                        text = { Text("Todo (${rootBoxesWithCounts.size + itemsInBox.size})", fontWeight = FontWeight.SemiBold) }
                                    )
                                    Tab(
                                        selected = selectedTab == 1,
                                        onClick = { selectedTab = 1 },
                                        text = { Text("Subcajas (${rootBoxesWithCounts.size})", fontWeight = FontWeight.SemiBold) }
                                    )
                                    Tab(
                                        selected = selectedTab == 2,
                                        onClick = { selectedTab = 2 },
                                        text = { Text("Objetos (${itemsInBox.size})", fontWeight = FontWeight.SemiBold) }
                                    )
                                }
                            }

                            val showBoxes = selectedTab == 0 || selectedTab == 1
                            val showItems = selectedTab == 0 || selectedTab == 2

                            // Subcajas de esta caja
                            if (showBoxes && rootBoxesWithCounts.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "Subcajas (${rootBoxesWithCounts.size})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                val chunkedSubBoxes = rootBoxesWithCounts.chunked(2)
                                chunkedSubBoxes.forEach { rowBoxes ->
                                    item {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            rowBoxes.forEach { bwc ->
                                                Box(modifier = Modifier.weight(1f)) {
                                                    BoxGridCard(
                                                        boxWithCounts = bwc,
                                                        onClick = { viewModel.openBox(bwc.box.id) },
                                                        onMove = { viewModel.openMoveBoxDialog(bwc.box) },
                                                        onEdit = { viewModel.openEditBoxDialog(bwc.box) },
                                                        onDelete = { viewModel.requestDeleteBox(bwc) }
                                                    )
                                                }
                                            }
                                            if (rowBoxes.size == 1) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }

                            // Objetos contenidos en esta caja
                            if (showItems && itemsInBox.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "Objetos en esta caja (${itemsInBox.size})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                if (isGridView) {
                                    val chunkedItems = itemsInBox.chunked(2)
                                    chunkedItems.forEach { rowItems ->
                                        item {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                rowItems.forEach { item ->
                                                    val category = categories.find { it.id == item.categoryId }
                                                    Box(modifier = Modifier.weight(1f)) {
                                                        ItemGridCard(
                                                            item = item,
                                                            category = category,
                                                            onQuantityChange = { delta -> viewModel.adjustItemQuantity(item, delta) },
                                                            onMove = { viewModel.openMoveItemDialog(item) },
                                                            onEdit = { viewModel.openEditItemDialog(item) },
                                                            onDelete = { viewModel.requestDeleteItem(item) },
                                                            onLoan = { viewModel.openLoanDialog(item) },
                                                            onReturnLoan = { viewModel.returnItem(item) },
                                                            onClick = { selectedItemForDetail = item }
                                                        )
                                                    }
                                                }
                                                if (rowItems.size == 1) {
                                                    Spacer(modifier = Modifier.weight(1f))
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    items(itemsInBox, key = { it.id }) { item ->
                                        val category = categories.find { it.id == item.categoryId }
                                        ItemCard(
                                            item = item,
                                            category = category,
                                            onQuantityChange = { delta -> viewModel.adjustItemQuantity(item, delta) },
                                            onMove = { viewModel.openMoveItemDialog(item) },
                                            onEdit = { viewModel.openEditItemDialog(item) },
                                            onDelete = { viewModel.requestDeleteItem(item) },
                                            onLoan = { viewModel.openLoanDialog(item) },
                                            onReturnLoan = { viewModel.returnItem(item) },
                                            onClick = { selectedItemForDetail = item }
                                        )
                                    }
                                }
                            }

                            if (rootBoxesWithCounts.isEmpty() && itemsInBox.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = Icons.Default.Inventory2,
                                                contentDescription = null,
                                                tint = Color(0xFFA89F95),
                                                modifier = Modifier.size(48.dp)
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(
                                                text = "Esta caja está vacía",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Añade objetos o subcajas usando el botón «+».",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFFA89F95)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                }
            }
        }
    }

    // Modal 2: Ficha Detallada de Objeto (Stitch AI Modal)
    selectedItemForDetail?.let { item ->
        val cat = categories.find { it.id == item.categoryId }
        val locationPath = boxPaths[item.boxId] ?: currentBox?.name ?: "Inventario"
        ObjectDetailSheet(
            item = item,
            category = cat,
            boxLocationPath = locationPath,
            onQuantityChange = { delta -> viewModel.adjustItemQuantity(item, delta) },
            onMove = { viewModel.openMoveItemDialog(item) },
            onEdit = { viewModel.openEditItemDialog(item) },
            onDelete = { viewModel.requestDeleteItem(item) },
            onLoan = { viewModel.openLoanDialog(item) },
            onReturnLoan = { viewModel.returnItem(item) },
            onDismiss = { selectedItemForDetail = null }
        )
    }

    // Diálogos y Modales CRUD
    if (isBoxDialogOpen) {
        EditBoxDialog(
            boxToEdit = boxToEdit,
            currentParentBox = currentBox,
            existingZones = allZones,
            initialZone = createBoxInitialZone,
            onSave = { name, desc, color, photo, zone ->
                viewModel.saveBox(name, desc, color, photo, zone)
                createBoxInitialZone = null
            },
            onDismiss = {
                createBoxInitialZone = null
                viewModel.dismissBoxDialog()
            }
        )
    }

    if (isItemDialogOpen) {
        EditItemDialog(
            itemToEdit = itemToEdit,
            currentBox = currentBox,
            allBoxes = allBoxes,
            categories = categories,
            onSave = { name, desc, qty, catId, tags, photo, targetBoxId ->
                viewModel.saveItem(name, desc, qty, catId, tags, photo, targetBoxId)
            },
            onDismiss = { viewModel.dismissItemDialog() }
        )
    }

    boxToMove?.let { box ->
        MoveBoxDialog(
            boxToMove = box,
            allBoxes = allBoxes,
            validator = { bId, targetParentId -> viewModel.canMoveBoxTo(bId, targetParentId) },
            onConfirm = { targetParentId -> viewModel.executeMoveBox(box.id, targetParentId) },
            onDismiss = { viewModel.dismissMoveBoxDialog() }
        )
    }

    itemToMove?.let { item ->
        MoveItemDialog(
            itemToMove = item,
            allBoxes = allBoxes,
            onConfirm = { targetBoxId -> viewModel.executeMoveItem(item.id, targetBoxId) },
            onDismiss = { viewModel.dismissMoveItemDialog() }
        )
    }

    itemToLoan?.let { item ->
        LoanDialog(
            item = item,
            onConfirm = { to, notes -> viewModel.confirmLoan(item, to, notes) },
            onDismiss = { viewModel.dismissLoanDialog() }
        )
    }

    if (isLoansSheetOpen) {
        LoansSheet(
            loanedItems = loanedItems,
            onReturnItem = { item -> viewModel.returnItem(item) },
            onDismiss = { viewModel.dismissLoansSheet() }
        )
    }

    if (isStatsDialogOpen) {
        StatsDashboardDialog(
            totalBoxes = totalBoxesCount,
            totalItems = totalItemsCount,
            totalUnits = totalItemUnitsCount ?: 0,
            totalLoaned = totalLoanedCount,
            categoryStats = categoryStats,
            onDismiss = { viewModel.dismissStatsDialog() }
        )
    }

    if (isBackupDialogOpen) {
        BackupDialog(
            onExportJson = { viewModel.getExportJsonString() },
            onExportCsv = { viewModel.getExportCsvString() },
            onImportJson = { json -> viewModel.importBackup(json) },
            onDismiss = { viewModel.dismissBackupDialog() }
        )
    }

    deleteTarget?.let { target ->
        DeleteConfirmDialog(
            target = target,
            onConfirm = { viewModel.confirmDelete() },
            onDismiss = { viewModel.dismissDeleteDialog() }
        )
    }
}
