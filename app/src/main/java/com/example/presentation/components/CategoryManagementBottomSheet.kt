package com.example.presentation.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Hardware
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.ItemEntity
import com.example.data.repository.InventoryRepository

val CATEGORY_COLORS = listOf(
    "#FFB77C", // Warm Amber
    "#3B82F6", // Sky Blue
    "#10B981", // Emerald Green
    "#EC4899", // Rose Pink
    "#F59E0B", // Gold Amber
    "#8B5CF6", // Purple
    "#06B6D4", // Teal Cyan
    "#EF4444", // Red Coral
    "#64748B", // Slate
    "#A855F7"  // Violet
)

val CATEGORY_ICON_OPTIONS = listOf(
    Pair("category", Icons.Default.Category),
    Pair("build", Icons.Default.Build),
    Pair("hardware", Icons.Default.Hardware),
    Pair("palette", Icons.Default.Palette),
    Pair("checkroom", Icons.Default.Checkroom),
    Pair("hiking", Icons.Default.Hiking),
    Pair("description", Icons.Default.Description),
    Pair("home", Icons.Default.Home),
    Pair("inventory", Icons.Default.Inventory2),
    Pair("tag", Icons.Default.Tag),
    Pair("schedule", Icons.Default.Schedule),
    Pair("settings", Icons.Default.Settings)
)

fun getCategoryIconByTag(iconName: String, fallbackName: String = ""): ImageVector {
    val key = iconName.trim().lowercase()
    val match = CATEGORY_ICON_OPTIONS.find { it.first.equals(key, ignoreCase = true) }
    if (match != null) return match.second

    val lowerName = fallbackName.lowercase()
    return when {
        lowerName.contains("herramienta") || lowerName.contains("tool") -> Icons.Default.Build
        lowerName.contains("tecno") || lowerName.contains("audio") || lowerName.contains("electr") -> Icons.Default.Hardware
        lowerName.contains("arte") || lowerName.contains("tinta") || lowerName.contains("pintur") -> Icons.Default.Palette
        lowerName.contains("ropa") || lowerName.contains("textil") || lowerName.contains("invierno") -> Icons.Default.Checkroom
        lowerName.contains("camp") || lowerName.contains("montaña") || lowerName.contains("viaje") -> Icons.Default.Hiking
        lowerName.contains("doc") || lowerName.contains("papel") || lowerName.contains("factura") -> Icons.Default.Description
        lowerName.contains("hogar") || lowerName.contains("casa") -> Icons.Default.Home
        else -> Icons.Default.Category
    }
}

/**
 * Secondary Bottom Sheet for full dynamic category management:
 * - Create new categories with custom name, color, and Material Symbol icon
 * - Edit / Rename existing categories (retains items relation via unique ID)
 * - Safe Delete with warning and automatic reassignment to "Sin categoría"
 * - Styled with AMOLED #181512 container, #2C2622 borders, and #FFB77C amber accents.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementBottomSheet(
    categories: List<CategoryEntity>,
    itemsInInventory: List<ItemEntity>,
    onCreateCategory: (name: String, colorHex: String, iconName: String) -> Unit,
    onUpdateCategory: (id: String, name: String, colorHex: String, iconName: String) -> Unit,
    onDeleteCategory: (categoryId: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    var isFormVisible by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var categoryToDelete by remember { mutableStateOf<CategoryEntity?>(null) }

    var formName by remember { mutableStateOf("") }
    var formColor by remember { mutableStateOf(CATEGORY_COLORS[0]) }
    var formIcon by remember { mutableStateOf(CATEGORY_ICON_OPTIONS[0].first) }
    var formError by remember { mutableStateOf<String?>(null) }

    fun openCreateForm() {
        editingCategory = null
        formName = ""
        formColor = CATEGORY_COLORS[0]
        formIcon = CATEGORY_ICON_OPTIONS[0].first
        formError = null
        isFormVisible = true
    }

    fun openEditForm(category: CategoryEntity) {
        editingCategory = category
        formName = category.name
        formColor = category.colorHex
        formIcon = category.iconName
        formError = null
        isFormVisible = true
    }

    fun closeForm() {
        isFormVisible = false
        editingCategory = null
        formName = ""
        formError = null
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF14110E),
        contentColor = Color(0xFFECE0DA),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Surface(
                modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                color = Color(0xFF382E26),
                shape = RoundedCornerShape(9999.dp)
            ) {
                Box(modifier = Modifier.size(width = 38.dp, height = 4.dp))
            }
        },
        modifier = modifier.testTag("category_management_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF231B15))
                            .border(1.dp, Color(0xFF2C2622), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = null,
                            tint = Color(0xFFFFB77C),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Gestión de Categorías",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${categories.size} categorías registradas",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFA89F95)
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("btn_close_category_sheet")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = Color(0xFFA89F95)
                    )
                }
            }

            // Button "+ Nueva Categoría" or Header Toggle
            if (!isFormVisible) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF231B15),
                    border = BorderStroke(1.dp, Color(0xFFFFB77C).copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { openCreateForm() }
                        .testTag("btn_add_category_header")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color(0xFFFFB77C),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Crear Nueva Categoría",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFB77C)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // Form: Create or Edit Category
            AnimatedVisibility(
                visible = isFormVisible,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF181512),
                    border = BorderStroke(1.dp, Color(0xFFFFB77C).copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                        .testTag("category_form_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (editingCategory == null) "Nueva Categoría" else "Editar: ${editingCategory!!.name}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFB77C)
                            )

                            IconButton(
                                onClick = { closeForm() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancelar",
                                    tint = Color(0xFFA89F95),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Category Name TextField
                        OutlinedTextField(
                            value = formName,
                            onValueChange = {
                                formName = it
                                formError = null
                            },
                            label = { Text("Nombre de la categoría *") },
                            placeholder = { Text("Ej. Electrónica, Pinturas...") },
                            singleLine = true,
                            isError = formError != null,
                            supportingText = formError?.let { { Text(it, color = Color(0xFFEF4444)) } },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF221C18),
                                unfocusedContainerColor = Color(0xFF1B1713),
                                focusedBorderColor = Color(0xFFFFB77C),
                                unfocusedBorderColor = Color(0xFF382E26),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color(0xFFECE0DA)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_category_name")
                        )

                        // Color Palette Selector
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Color de la categoría:",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFA89F95),
                                fontWeight = FontWeight.Medium
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CATEGORY_COLORS.forEach { hex ->
                                    val color = parseColorSafe(hex)
                                    val isSelected = formColor.equals(hex, ignoreCase = true)
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .border(
                                                width = if (isSelected) 3.dp else 1.dp,
                                                color = if (isSelected) Color.White else Color(0xFF2C2622),
                                                shape = CircleShape
                                            )
                                            .clickable { formColor = hex }
                                            .testTag("color_picker_$hex"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Icon Selector
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Icono representativo (Material Symbols):",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFA89F95),
                                fontWeight = FontWeight.Medium
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CATEGORY_ICON_OPTIONS.forEach { (iconKey, vector) ->
                                    val isSelected = formIcon.equals(iconKey, ignoreCase = true)
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) Color(0xFFFFB77C).copy(alpha = 0.25f) else Color(0xFF241D17),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) Color(0xFFFFB77C) else Color(0xFF382E26)
                                        ),
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { formIcon = iconKey }
                                            .testTag("icon_picker_$iconKey")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = vector,
                                                contentDescription = iconKey,
                                                tint = if (isSelected) Color(0xFFFFB77C) else Color(0xFFA89F95),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Save & Cancel Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { closeForm() }) {
                                Text("Cancelar", color = Color(0xFFA89F95))
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = {
                                    val trimmed = formName.trim()
                                    if (trimmed.isBlank()) {
                                        formError = "El nombre no puede estar vacío"
                                        return@Button
                                    }
                                    val duplicate = categories.any {
                                        it.id != editingCategory?.id && it.name.equals(trimmed, ignoreCase = true)
                                    }
                                    if (duplicate) {
                                        formError = "Ya existe una categoría con este nombre"
                                        return@Button
                                    }

                                    if (editingCategory != null) {
                                        onUpdateCategory(editingCategory!!.id, trimmed, formColor, formIcon)
                                    } else {
                                        onCreateCategory(trimmed, formColor, formIcon)
                                    }
                                    closeForm()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFFB77C),
                                    contentColor = Color(0xFF2B1705)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("btn_save_category")
                            ) {
                                Text(
                                    text = if (editingCategory == null) "Crear Categoría" else "Guardar Cambios",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Categories List
            Text(
                text = "Categorías disponibles:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFA89F95),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories, key = { it.id }) { category ->
                    val isDefaultSinCat = category.id == InventoryRepository.DEFAULT_CATEGORY_ID ||
                        category.name.equals(InventoryRepository.DEFAULT_CATEGORY_NAME, ignoreCase = true)

                    val itemsCount = remember(category.id, itemsInInventory) {
                        itemsInInventory.count { it.categoryId == category.id }
                    }

                    val catColor = remember(category.colorHex) {
                        parseColorSafe(category.colorHex, Color(0xFF64748B))
                    }
                    val iconVector = remember(category.iconName, category.name) {
                        getCategoryIconByTag(category.iconName, category.name)
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF181512),
                        border = BorderStroke(1.dp, Color(0xFF2C2622)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("category_item_${category.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left: Color & Icon & Name & Counter
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(catColor.copy(alpha = 0.2f))
                                        .border(1.dp, catColor.copy(alpha = 0.45f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = iconVector,
                                        contentDescription = null,
                                        tint = catColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = category.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        if (isDefaultSinCat) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFF28221D),
                                                border = BorderStroke(1.dp, Color(0xFF483A30))
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Shield,
                                                        contentDescription = null,
                                                        tint = Color(0xFFFFB77C),
                                                        modifier = Modifier.size(10.dp)
                                                    )
                                                    Text(
                                                        text = "Resguardo",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFFFFB77C)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Text(
                                        text = "$itemsCount objeto${if (itemsCount == 1) "" else "s"} asociado${if (itemsCount == 1) "" else "s"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFA89F95)
                                    )
                                }
                            }

                            // Right: Edit & Delete Action Buttons
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = { openEditForm(category) },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .testTag("btn_edit_cat_${category.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Editar categoría",
                                        tint = Color(0xFFFFB77C),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                if (!isDefaultSinCat) {
                                    IconButton(
                                        onClick = { categoryToDelete = category },
                                        modifier = Modifier
                                            .size(34.dp)
                                            .testTag("btn_delete_cat_${category.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Eliminar categoría",
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
        }
    }

    // Confirmation Alert for Safe Deletion
    if (categoryToDelete != null) {
        val targetCat = categoryToDelete!!
        val affectedItemsCount = itemsInInventory.count { it.categoryId == targetCat.id }

        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            containerColor = Color(0xFF181512),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFECE0DA),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = Color(0xFFEF4444)
                    )
                    Text("¿Eliminar categoría «${targetCat.name}»?")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Esta acción eliminará la categoría «${targetCat.name}» de forma permanente.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF261D17),
                        border = BorderStroke(1.dp, Color(0xFFFFB77C).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "🛡️ Integridad de datos:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFB77C)
                            )
                            Text(
                                text = "Los $affectedItemsCount objeto(s) asociados NO serán borrados. Se reasignarán automáticamente a la categoría de resguardo «Sin categoría».",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFECE0DA)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCategory(targetCat.id)
                        categoryToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("confirm_delete_cat_button")
                ) {
                    Text("Eliminar y Reasignar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text("Cancelar", color = Color(0xFFA89F95))
                }
            }
        )
    }
}
