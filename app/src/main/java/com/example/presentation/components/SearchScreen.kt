package com.example.presentation.components

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Hardware
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.dao.BoxWithCounts
import com.example.data.local.entity.BoxEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.ItemEntity

enum class BoxSortOption(val label: String, val icon: ImageVector) {
    RELEVANCE("Por relevancia", Icons.Default.AutoAwesome),
    RECENT("Más recientes", Icons.Default.Schedule),
    NAME_AZ("Nombre (A - Z)", Icons.Default.SortByAlpha),
    CAPACITY("Capacidad (% lleno)", Icons.Default.PieChart)
}

private fun getCategoryIcon(name: String): ImageVector {
    val lower = name.lowercase()
    return when {
        lower.contains("herramienta") || lower.contains("tool") -> Icons.Default.Build
        lower.contains("tecno") || lower.contains("audio") || lower.contains("electr") -> Icons.Default.Hardware
        lower.contains("arte") || lower.contains("tinta") || lower.contains("pintur") -> Icons.Default.Palette
        lower.contains("textil") || lower.contains("ropa") -> Icons.Default.Checkroom
        lower.contains("camp") || lower.contains("viaje") -> Icons.Default.Hiking
        lower.contains("doc") || lower.contains("papel") -> Icons.Default.Description
        else -> Icons.Default.Category
    }
}

/**
 * Screen 2: Inventario - Búsqueda Expandida & Filtros Avanzados AMOLED
 * Strict 2-column grid layout with contextual box creation and tune filters.
 */
@Composable
fun SearchScreen(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    allBoxes: List<BoxEntity>,
    allZones: List<String>,
    categories: List<CategoryEntity>,
    boxItemPhotos: Map<String, List<String>>,
    itemsInInventory: List<ItemEntity>,
    onBoxClick: (String) -> Unit,
    onItemClick: (ItemEntity) -> Unit,
    onNewBoxWithFilters: (zone: String?) -> Unit,
    onCloseSearch: () -> Unit,
    onMoveBox: (BoxEntity) -> Unit,
    onEditBox: (BoxEntity) -> Unit,
    onDeleteBox: (BoxEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedZone by remember { mutableStateOf<String?>(null) }
    var selectedCategories by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedStatus by remember { mutableStateOf<String?>(null) } // "Frecuente", "En préstamo", "Casi lleno (>80%)"
    var selectedSort by remember { mutableStateOf(BoxSortOption.RELEVANCE) }
    var isSortMenuOpen by remember { mutableStateOf(false) }

    // Active filters badge count on tune button
    val activeFilterBadgeCount = remember(selectedZone, selectedCategories, selectedStatus) {
        (if (selectedZone != null) 1 else 0) + selectedCategories.size + (if (selectedStatus != null) 1 else 0)
    }

    // Filter boxes
    val matchingBoxes = remember(
        allBoxes,
        searchQuery,
        selectedZone,
        selectedCategories,
        selectedStatus,
        selectedSort,
        itemsInInventory
    ) {
        val query = searchQuery.trim().lowercase()

        val filtered = allBoxes.filter { box ->
            // 1. Text Query
            val matchesQuery = if (query.isBlank()) {
                true
            } else {
                box.name.lowercase().contains(query) ||
                    (box.zone?.lowercase()?.contains(query) == true) ||
                    (box.description?.lowercase()?.contains(query) == true) ||
                    itemsInInventory.any {
                        it.boxId == box.id && (
                            it.name.lowercase().contains(query) ||
                            (it.description?.lowercase()?.contains(query) == true) ||
                            it.tags.lowercase().contains(query)
                        )
                    }
            }

            // 2. Zone Filter
            val matchesZone = if (selectedZone == null) {
                true
            } else if (selectedZone == "Sin zona asignada") {
                box.zone.isNullOrBlank() || box.zone.equals("null", ignoreCase = true)
            } else {
                box.zone?.equals(selectedZone, ignoreCase = true) == true
            }

            // 3. Category Filter
            val matchesCategory = if (selectedCategories.isEmpty()) {
                true
            } else {
                val boxItems = itemsInInventory.filter { it.boxId == box.id }
                boxItems.any { it.categoryId in selectedCategories }
            }

            // 4. Status Filter
            val matchesStatus = when (selectedStatus) {
                "Frecuente" -> {
                    val count = itemsInInventory.count { it.boxId == box.id }
                    count >= 5
                }
                "En préstamo" -> {
                    itemsInInventory.any { it.boxId == box.id && it.isLoaned }
                }
                "Casi lleno (>80%)" -> {
                    val count = itemsInInventory.count { it.boxId == box.id }
                    count >= 8
                }
                else -> true
            }

            matchesQuery && matchesZone && matchesCategory && matchesStatus
        }

        // Apply Sorting
        when (selectedSort) {
            BoxSortOption.RELEVANCE -> {
                if (query.isBlank()) {
                    filtered
                } else {
                    filtered.sortedWith(
                        compareByDescending<BoxEntity> { it.name.lowercase().contains(query) }
                            .thenByDescending { box -> itemsInInventory.count { it.boxId == box.id } }
                    )
                }
            }
            BoxSortOption.RECENT -> filtered.sortedByDescending { it.createdAt }
            BoxSortOption.NAME_AZ -> filtered.sortedBy { it.name.lowercase() }
            BoxSortOption.CAPACITY -> filtered.sortedByDescending { box -> itemsInInventory.count { it.boxId == box.id } }
        }
    }

    // Matching Items (if search query or categories are active)
    val matchingItems = remember(searchQuery, selectedCategories, itemsInInventory) {
        val query = searchQuery.trim().lowercase()
        if (query.isBlank() && selectedCategories.isEmpty()) {
            emptyList()
        } else {
            itemsInInventory.filter { item ->
                val matchesQuery = query.isBlank() ||
                    item.name.lowercase().contains(query) ||
                    (item.description?.lowercase()?.contains(query) == true) ||
                    item.tags.lowercase().contains(query)
                val matchesCat = selectedCategories.isEmpty() || item.categoryId in selectedCategories
                matchesQuery && matchesCat
            }
        }
    }

    val distinctZonesCount = remember(matchingBoxes) {
        matchingBoxes.mapNotNull { it.zone?.takeIf { z -> z.isNotBlank() && !z.equals("null", ignoreCase = true) } }
            .distinct().size
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // =================================================================
            // HEADER BAR: Search Input + Tune Button + Stats Summary
            // =================================================================
            Surface(
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                tonalElevation = 3.dp,
                shadowElevation = 4.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_header_bar")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Top Input Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Back Button
                        IconButton(
                            onClick = onCloseSearch,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                .testTag("btn_close_search")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Search Text Field Container
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(modifier = Modifier.weight(1f)) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "Taladro, cables o ropa...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    BasicTextField(
                                        value = searchQuery,
                                        onValueChange = onQueryChange,
                                        textStyle = TextStyle(
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                        keyboardActions = KeyboardActions(onSearch = { }),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("search_text_input")
                                    )
                                }
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { onQueryChange("") },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Limpiar",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Tune Button (With active filters badge)
                        Box(contentAlignment = Alignment.TopEnd) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(
                                    1.dp,
                                    if (activeFilterBadgeCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        // Toggles or scrolls to filter section
                                    }
                                    .testTag("btn_tune_advanced_filters")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = "Filtros avanzados",
                                        tint = if (activeFilterBadgeCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            if (activeFilterBadgeCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                        .testTag("tune_active_badge"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$activeFilterBadgeCount",
                                        color = Color(0xFF2F1500),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }

                    // Summary & Clear Filters Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Text(
                                text = "${matchingBoxes.size} cajas encontradas",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (distinctZonesCount > 0) {
                                Text(
                                    text = "en $distinctZonesCount zonas",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            text = "Limpiar filtros",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable {
                                    onQueryChange("")
                                    selectedZone = null
                                    selectedCategories = emptySet()
                                    selectedStatus = null
                                }
                                .testTag("btn_clear_filters")
                        )
                    }
                }
            }

            // =================================================================
            // MAIN 2-COLUMN GRID WITH FILTERS & RESULTS
            // =================================================================
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("search_results_grid")
            ) {
                // Section 1: FILTRAR POR ZONA (Full-width row)
                item(span = { GridItemSpan(2) }) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "FILTRAR POR ZONA",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = if (selectedZone == null) "Todas (${allBoxes.size})" else selectedZone!!,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Todas pill
                            val isAllSelected = selectedZone == null
                            Surface(
                                shape = RoundedCornerShape(9999.dp),
                                color = if (isAllSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(
                                    1.dp,
                                    if (isAllSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(9999.dp))
                                    .clickable { selectedZone = null }
                                    .testTag("filter_zone_all")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.GridView,
                                        contentDescription = null,
                                        tint = if (isAllSelected) Color(0xFF2F1500) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "Todas (${allBoxes.size})",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isAllSelected) Color(0xFF2F1500) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Individual Zones
                            allZones.forEach { zoneName ->
                                val isSelected = selectedZone == zoneName
                                val count = allBoxes.count { it.zone?.equals(zoneName, ignoreCase = true) == true }
                                Surface(
                                    shape = RoundedCornerShape(9999.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(9999.dp))
                                        .clickable {
                                            selectedZone = if (isSelected) null else zoneName
                                        }
                                        .testTag("filter_zone_$zoneName")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = if (isSelected) Color(0xFF2F1500) else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = "$zoneName ($count)",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color(0xFF2F1500) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 2: CATEGORÍAS & ETIQUETAS CARD (Full-width card)
                item(span = { GridItemSpan(2) }) {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("card_categories_filter")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Category,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Categorías & Etiquetas",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                if (selectedCategories.isNotEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(9999.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                                    ) {
                                        Text(
                                            text = "${selectedCategories.size} seleccionadas",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }

                            // Category chips row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                categories.forEach { cat ->
                                    val isSelected = selectedCategories.contains(cat.id)
                                    val catItemCount = itemsInInventory.count { it.categoryId == cat.id }
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                        ),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                selectedCategories = if (isSelected) {
                                                    selectedCategories - cat.id
                                                } else {
                                                    selectedCategories + cat.id
                                                }
                                            }
                                            .testTag("filter_cat_${cat.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = getCategoryIcon(cat.name),
                                                contentDescription = null,
                                                tint = if (isSelected) Color(0xFF2F1500) else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Text(
                                                text = "${cat.name} ($catItemCount)",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) Color(0xFF2F1500) else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color(0xFF2F1500),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Estado / Status quick filter row
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Estado:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp
                                )

                                val statusOptions = listOf(
                                    Pair("Frecuente", Color(0xFF10B981)),
                                    Pair("En préstamo", Color(0xFF3B82F6)),
                                    Pair("Casi lleno (>80%)", Color(0xFFF59E0B))
                                )

                                statusOptions.forEach { (statusName, statusColor) ->
                                    val isSelected = selectedStatus == statusName
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) statusColor.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant,
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) statusColor else MaterialTheme.colorScheme.outlineVariant
                                        ),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                selectedStatus = if (isSelected) null else statusName
                                            }
                                            .testTag("filter_status_$statusName")
                                    ) {
                                        Text(
                                            text = statusName,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) statusColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 3: COINCIDENCIAS ENCONTRADAS HEADER & SORT DROPDOWN
                item(span = { GridItemSpan(2) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Coincidencias encontradas",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(9999.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Text(
                                    text = "${matchingBoxes.size} cajas",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Sort Dropdown Button
                        Box {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { isSortMenuOpen = !isSortMenuOpen }
                                    .testTag("sort_dropdown_toggle")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FilterList,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = selectedSort.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 12.sp
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = isSortMenuOpen,
                                onDismissRequest = { isSortMenuOpen = false }
                            ) {
                                BoxSortOption.entries.forEach { option ->
                                    val isSelected = selectedSort == option
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = option.label,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = option.icon,
                                                contentDescription = null,
                                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        },
                                        trailingIcon = if (isSelected) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        } else null,
                                        onClick = {
                                            selectedSort = option
                                            isSortMenuOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 4: 2-COLUMN GRID OF MATCHING BOXES
                items(matchingBoxes, key = { it.id }) { box ->
                    val itemCount = itemsInInventory.count { it.boxId == box.id }
                    BoxGridCard(
                        boxWithCounts = BoxWithCounts(box, 0, itemCount),
                        itemPhotos = boxItemPhotos[box.id] ?: emptyList(),
                        onClick = { onBoxClick(box.id) },
                        onMove = { onMoveBox(box) },
                        onEdit = { onEditBox(box) },
                        onDelete = { onDeleteBox(box) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Section 5: CONTEXTUAL CARD: "Nueva Caja (Con estos filtros)"
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(230.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                onNewBoxWithFilters(selectedZone)
                            }
                            .testTag("btn_new_box_with_filters")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Nueva Caja",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Con estos filtros",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Section 6: MATCHING ITEMS (If found)
                if (matchingItems.isNotEmpty()) {
                    item(span = { GridItemSpan(2) }) {
                        Text(
                            text = "Objetos coincidentes (${matchingItems.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 14.dp, bottom = 4.dp)
                        )
                    }

                    items(matchingItems, key = { "item_${it.id}" }, span = { GridItemSpan(2) }) { item ->
                        val cat = categories.find { it.id == item.categoryId }
                        val box = allBoxes.find { it.id == item.boxId }

                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onItemClick(item) }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                ItemCard(
                                    item = item,
                                    category = cat,
                                    onQuantityChange = { },
                                    onMove = { },
                                    onEdit = { },
                                    onDelete = { },
                                    onLoan = { },
                                    onReturnLoan = { },
                                    onClick = { onItemClick(item) }
                                )
                                if (box != null) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "Ubicación: ${box.name}${if (!box.zone.isNullOrBlank()) " (${box.zone})" else ""}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Spacer at bottom so content is never covered by bottom bar or FAB
                item(span = { GridItemSpan(2) }) {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }

        // Floating Action Button at Bottom Right (above dock)
        FloatingActionButton(
            onClick = { onNewBoxWithFilters(selectedZone) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color(0xFF2F1500),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 76.dp, end = 16.dp)
                .testTag("btn_fab_new_box_search")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Nueva Caja",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }
    }
}
