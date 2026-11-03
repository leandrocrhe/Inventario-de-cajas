package com.example.presentation.components

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.dao.BoxWithCounts
import com.example.data.local.entity.BoxEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.ItemEntity
import com.example.presentation.InventoryViewModel

@Composable
fun SearchScreen(
    viewModel: InventoryViewModel,
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    selectedCategory: String?,
    onCategorySelect: (String?) -> Unit,
    searchResultsItems: List<ItemEntity>,
    searchResultsBoxes: List<BoxEntity>,
    categories: List<CategoryEntity>,
    onBoxClick: (String) -> Unit,
    onItemClick: (ItemEntity) -> Unit,
    onCloseSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Map to cache box paths for items
    val boxPaths = remember { mutableStateMapOf<String, String>() }

    // Resolve paths for search items
    LaunchedEffect(searchResultsItems) {
        val uniqueBoxIds = searchResultsItems.map { it.boxId }.distinct()
        for (boxId in uniqueBoxIds) {
            if (!boxPaths.containsKey(boxId)) {
                boxPaths[boxId] = viewModel.getBoxBreadcrumbPath(boxId)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 8.dp)
    ) {
        // Search bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onCloseSearch,
                modifier = Modifier.testTag("close_search_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Cerrar búsqueda"
                )
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onQueryChange,
                placeholder = { Text("Buscar por nombre, descripción o etiqueta…") },
                singleLine = true,
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Limpiar")
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("search_text_field")
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Category filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onCategorySelect(null) },
                label = { Text("Todas") }
            )
            categories.forEach { cat ->
                FilterChip(
                    selected = selectedCategory == cat.id,
                    onClick = { onCategorySelect(cat.id) },
                    label = { Text(cat.name) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Results list
        if (searchQuery.isBlank() && selectedCategory == null) {
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
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Escribe para buscar cualquier caja u objeto",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (searchResultsItems.isEmpty() && searchResultsBoxes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No se encontraron resultados para «$searchQuery»",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Boxes found
                if (searchResultsBoxes.isNotEmpty()) {
                    item {
                        Text(
                            text = "Cajas encontradas (${searchResultsBoxes.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                        )
                    }

                    items(searchResultsBoxes) { box ->
                        BoxCard(
                            boxWithCounts = BoxWithCounts(box, 0, 0),
                            onClick = { onBoxClick(box.id) },
                            onMove = { viewModel.openMoveBoxDialog(box) },
                            onEdit = { viewModel.openEditBoxDialog(box) },
                            onDelete = { viewModel.requestDeleteBox(BoxWithCounts(box, 0, 0)) }
                        )
                    }
                }

                // Items found
                if (searchResultsItems.isNotEmpty()) {
                    item {
                        Text(
                            text = "Objetos encontrados (${searchResultsItems.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }

                    items(searchResultsItems) { item ->
                        val cat = categories.find { it.id == item.categoryId }
                        val path = boxPaths[item.boxId] ?: "Cargando ubicación…"

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                // Item card content
                                ItemCard(
                                    item = item,
                                    category = cat,
                                    onQuantityChange = { delta -> viewModel.adjustItemQuantity(item, delta) },
                                    onMove = { viewModel.openMoveItemDialog(item) },
                                    onEdit = { viewModel.openEditItemDialog(item) },
                                    onDelete = { viewModel.requestDeleteItem(item) },
                                    onLoan = { viewModel.openLoanDialog(item) },
                                    onReturnLoan = { viewModel.returnItem(item) }
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Breadcrumb Path chip: clicking jumps to container box!
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                    modifier = Modifier
                                        .fillMaxWidth()
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
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
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
    }
}
