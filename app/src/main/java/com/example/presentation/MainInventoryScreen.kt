package com.example.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AssignmentInd
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.presentation.components.BackupDialog
import com.example.presentation.components.BoxCard
import com.example.presentation.components.BoxGridCard
import com.example.presentation.components.BreadcrumbBar
import com.example.presentation.components.DeleteConfirmDialog
import com.example.presentation.components.EditBoxDialog
import com.example.presentation.components.EditItemDialog
import com.example.presentation.components.ItemCard
import com.example.presentation.components.ItemGridCard
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import com.example.presentation.components.LoanDialog
import com.example.presentation.components.LoansSheet
import com.example.presentation.components.MoveBoxDialog
import com.example.presentation.components.MoveItemDialog
import com.example.presentation.components.SearchScreen
import com.example.presentation.components.StatsDashboardDialog
import com.example.presentation.components.parseColorSafe

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MainInventoryScreen(
    viewModel: InventoryViewModel = viewModel()
) {
    val isGridView by viewModel.isGridView.collectAsState()
    val currentBoxId by viewModel.currentBoxId.collectAsState()
    val currentBox by viewModel.currentBox.collectAsState()
    val breadcrumbs by viewModel.breadcrumbs.collectAsState()
    val boxesWithCounts by viewModel.currentBoxesWithCounts.collectAsState()
    val itemsInBox by viewModel.currentItems.collectAsState()
    val allBoxes by viewModel.allBoxes.collectAsState()
    val categories by viewModel.allCategories.collectAsState()

    val isSearchActive by viewModel.isSearchActive.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategoryFilter by viewModel.selectedCategoryFilter.collectAsState()
    val searchResultsItems by viewModel.searchResultsItems.collectAsState()
    val searchResultsBoxes by viewModel.searchResultsBoxes.collectAsState()

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
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Todo, 1: Subcajas, 2: Objetos
    var fabMenuExpanded by remember { mutableStateOf(false) }

    // Intercept back button for nested box navigation and search
    BackHandler(enabled = isSearchActive || currentBoxId != null) {
        viewModel.navigateBack()
    }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        topBar = {
            if (!isSearchActive) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("header_container")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                    ) {
                        // Fila principal: cabecera con flex-wrap: wrap y min-widths
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalArrangement = Arrangement.Center
                        ) {
                            // Elemento hijo: Bloque de identidad y título (flex-shrink: 0, min-width para evitar desbordes)
                            Row(
                                modifier = Modifier
                                    .widthIn(min = 190.dp)
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (currentBoxId != null) {
                                    IconButton(
                                        onClick = { viewModel.navigateBack() },
                                        modifier = Modifier.testTag("nav_back_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Atrás",
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                } else {
                                    Image(
                                        painter = painterResource(com.example.R.drawable.cajas_app_logo_1793724857669),
                                        contentDescription = "Logo Inventario",
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                }

                                Column(
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Text(
                                        text = currentBox?.name ?: "Inventario de Cajas",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (currentBoxId == null) {
                                            "$totalBoxesCount cajas · $totalItemsCount objetos (${totalItemUnitsCount ?: 0} uds)"
                                        } else {
                                            "${boxesWithCounts.size} subcajas · ${itemsInBox.size} objetos"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Elemento hijo: Bloque de botones de acción (flex-shrink: 0, min-width)
                            Row(
                                modifier = Modifier
                                    .widthIn(min = 96.dp)
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End
                            ) {
                                // Botón de Búsqueda rápida
                                IconButton(
                                    onClick = { viewModel.setSearchActive(true) },
                                    modifier = Modifier.testTag("search_icon_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Buscar",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Botón para alternar Vista Cuadrícula / Lista
                                IconButton(
                                    onClick = { viewModel.toggleViewMode() },
                                    modifier = Modifier.testTag("toggle_view_mode_button")
                                ) {
                                    Icon(
                                        imageVector = if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                                        contentDescription = if (isGridView) "Vista lista" else "Vista cuadrícula",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Fila de herramientas y estado en chips interactivos organizados
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Chip de Préstamos con Badge reactivo
                            FilterChip(
                                selected = totalLoanedCount > 0,
                                onClick = { viewModel.openLoansSheet() },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Préstamos")
                                        if (totalLoanedCount > 0) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Badge(
                                                containerColor = MaterialTheme.colorScheme.primary,
                                                contentColor = MaterialTheme.colorScheme.onPrimary
                                            ) {
                                                Text("$totalLoanedCount")
                                            }
                                        }
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.AssignmentInd,
                                        contentDescription = "Préstamos",
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                modifier = Modifier.testTag("loans_button")
                            )

                            // Chip de Estadísticas
                            AssistChip(
                                onClick = { viewModel.openStatsDialog() },
                                label = { Text("Estadísticas") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.PieChart,
                                        contentDescription = "Estadísticas",
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                modifier = Modifier.testTag("stats_button")
                            )

                            // Chip de Copias de seguridad
                            AssistChip(
                                onClick = { viewModel.openBackupDialog() },
                                label = { Text("Copias de seguridad") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Storage,
                                        contentDescription = "Copias de seguridad",
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                modifier = Modifier.testTag("backup_button")
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (!isSearchActive) {
                Box {
                    if (currentBoxId == null) {
                        ExtendedFloatingActionButton(
                            onClick = { viewModel.openCreateBoxDialog() },
                            icon = { Icon(Icons.Default.Add, contentDescription = null) },
                            text = { Text("Nueva Caja") },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.testTag("add_root_box_fab")
                        )
                    } else {
                        FloatingActionButton(
                            onClick = { fabMenuExpanded = true },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.testTag("box_add_menu_fab")
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
        if (isSearchActive) {
            SearchScreen(
                viewModel = viewModel,
                searchQuery = searchQuery,
                onQueryChange = { viewModel.setSearchQuery(it) },
                selectedCategory = selectedCategoryFilter,
                onCategorySelect = { viewModel.setCategoryFilter(it) },
                searchResultsItems = searchResultsItems,
                searchResultsBoxes = searchResultsBoxes,
                categories = categories,
                onBoxClick = { boxId -> viewModel.openBox(boxId) },
                onItemClick = { item -> viewModel.openEditItemDialog(item) },
                onCloseSearch = { viewModel.setSearchActive(false) },
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Breadcrumbs header row
                BreadcrumbBar(
                    breadcrumbs = breadcrumbs,
                    onRootClick = { viewModel.navigateToRoot() },
                    onCrumbClick = { box -> viewModel.openBox(box.id) }
                )

                // Active box banner + segment tabs
                if (currentBox != null) {
                    val activeBox = currentBox!!
                    val accentColor = remember(activeBox.colorHex) { parseColorSafe(activeBox.colorHex) }

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
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
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (!activeBox.description.isNullOrBlank()) {
                                    Text(
                                        text = activeBox.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            IconButton(onClick = { viewModel.openEditBoxDialog(activeBox) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar detalles")
                            }
                        }
                    }

                    // Content Tabs (Todo / Subcajas / Objetos)
                    PrimaryTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Todo (${boxesWithCounts.size + itemsInBox.size})") }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Subcajas (${boxesWithCounts.size})") }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("Objetos (${itemsInBox.size})") }
                        )
                    }
                }

                val showBoxes = selectedTab == 0 || selectedTab == 1 || currentBoxId == null
                val showItems = currentBoxId != null && (selectedTab == 0 || selectedTab == 2)

                if (boxesWithCounts.isEmpty() && (!showItems || itemsInBox.isEmpty())) {
                    // Empty state
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (currentBoxId == null) {
                                Image(
                                    painter = painterResource(com.example.R.drawable.cajas_app_logo_1793724857669),
                                    contentDescription = "Logo",
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Inventory2,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                    modifier = Modifier.size(72.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (currentBoxId == null) "No tienes cajas todavía" else "Esta caja está vacía",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (currentBoxId == null) {
                                    "Pulsa «Nueva Caja» para empezar a organizar tu inventario."
                                } else {
                                    "Añade objetos o subcajas usando el botón «+»."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else if (isGridView) {
                    // -------------------------------------------------------------
                    // GRID VIEW (2 COLUMNS)
                    // -------------------------------------------------------------
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 88.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Section: Subcajas
                        if (showBoxes && boxesWithCounts.isNotEmpty()) {
                            item(span = { GridItemSpan(2) }) {
                                Text(
                                    text = if (currentBoxId == null) "Mis Cajas (${boxesWithCounts.size})" else "Subcajas (${boxesWithCounts.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                                )
                            }

                            items(boxesWithCounts, key = { it.box.id }) { bwc ->
                                BoxGridCard(
                                    boxWithCounts = bwc,
                                    onClick = { viewModel.openBox(bwc.box.id) },
                                    onMove = { viewModel.openMoveBoxDialog(bwc.box) },
                                    onEdit = { viewModel.openEditBoxDialog(bwc.box) },
                                    onDelete = { viewModel.requestDeleteBox(bwc) }
                                )
                            }
                        }

                        // Section: Objetos
                        if (showItems && itemsInBox.isNotEmpty()) {
                            item(span = { GridItemSpan(2) }) {
                                Text(
                                    text = "Objetos contenidos (${itemsInBox.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                                )
                            }

                            items(itemsInBox, key = { it.id }) { item ->
                                val category = categories.find { it.id == item.categoryId }
                                ItemGridCard(
                                    item = item,
                                    category = category,
                                    onQuantityChange = { delta -> viewModel.adjustItemQuantity(item, delta) },
                                    onMove = { viewModel.openMoveItemDialog(item) },
                                    onEdit = { viewModel.openEditItemDialog(item) },
                                    onDelete = { viewModel.requestDeleteItem(item) },
                                    onLoan = { viewModel.openLoanDialog(item) },
                                    onReturnLoan = { viewModel.returnItem(item) }
                                )
                            }
                        }
                    }
                } else {
                    // -------------------------------------------------------------
                    // LIST VIEW
                    // -------------------------------------------------------------
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Section: Subcajas
                        if (showBoxes && boxesWithCounts.isNotEmpty()) {
                            item {
                                Text(
                                    text = if (currentBoxId == null) "Mis Cajas (${boxesWithCounts.size})" else "Subcajas (${boxesWithCounts.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                            }

                            items(boxesWithCounts, key = { it.box.id }) { bwc ->
                                BoxCard(
                                    boxWithCounts = bwc,
                                    onClick = { viewModel.openBox(bwc.box.id) },
                                    onMove = { viewModel.openMoveBoxDialog(bwc.box) },
                                    onEdit = { viewModel.openEditBoxDialog(bwc.box) },
                                    onDelete = { viewModel.requestDeleteBox(bwc) }
                                )
                            }
                        }

                        // Section: Objetos
                        if (showItems && itemsInBox.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Objetos contenidos (${itemsInBox.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                                )
                            }

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
                                    onReturnLoan = { viewModel.returnItem(item) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (isBoxDialogOpen) {
        EditBoxDialog(
            boxToEdit = boxToEdit,
            currentParentBox = currentBox,
            onSave = { name, desc, color, photo ->
                viewModel.saveBox(name, desc, color, photo)
            },
            onDismiss = { viewModel.dismissBoxDialog() }
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
