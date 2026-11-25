package com.example.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MergeType
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.util.TagStat
import com.example.domain.util.TagUtils

/**
 * Secondary Bottom Sheet for reactive bulk tag management:
 * - Lists active tags across the Room inventory with associated object counts
 * - Bulk Rename / Merge: atomically updates all items using the tag without leaving duplicates
 * - Bulk Delete: strips the tag from all items in a Room transaction without deleting items
 * - Styled with AMOLED #14110E / #181512 surfaces, #2C2622 borders, and #FFB77C accents.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagManagementBottomSheet(
    tagStats: List<TagStat>,
    onRenameTag: (oldTag: String, newTag: String) -> Unit,
    onDeleteTag: (tagName: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    var filterQuery by remember { mutableStateOf("") }
    var editingTag by remember { mutableStateOf<TagStat?>(null) }
    var renameInput by remember { mutableStateOf("") }
    var renameError by remember { mutableStateOf<String?>(null) }
    var tagToDelete by remember { mutableStateOf<TagStat?>(null) }

    val filteredTags = remember(tagStats, filterQuery) {
        val q = filterQuery.trim().lowercase()
        if (q.isBlank()) tagStats else tagStats.filter { it.name.lowercase().contains(q) }
    }

    fun openRenameForm(stat: TagStat) {
        editingTag = stat
        renameInput = stat.name
        renameError = null
    }

    fun closeRenameForm() {
        editingTag = null
        renameInput = ""
        renameError = null
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
        modifier = modifier.testTag("tag_management_sheet")
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
                            imageVector = Icons.Default.Tag,
                            contentDescription = null,
                            tint = Color(0xFFFFB77C),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Gestión de Etiquetas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${tagStats.size} etiqueta${if (tagStats.size == 1) "" else "s"} en el inventario",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFA89F95)
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("btn_close_tag_sheet")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar gestión de etiquetas",
                        tint = Color(0xFFA89F95)
                    )
                }
            }

            // Inline Rename / Merge Form
            AnimatedVisibility(
                visible = editingTag != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                val target = editingTag
                if (target != null) {
                    val normalizedPreview = TagUtils.normalizeTag(renameInput)
                    val mergeTarget = remember(normalizedPreview, tagStats, target.name) {
                        if (normalizedPreview.isBlank() || normalizedPreview.equals(target.name, ignoreCase = true)) {
                            null
                        } else {
                            tagStats.find { it.name.equals(normalizedPreview, ignoreCase = true) }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF181512),
                        border = BorderStroke(1.dp, Color(0xFFFFB77C).copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                            .testTag("tag_rename_form_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Renombrar: #${target.name}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFB77C)
                                )

                                IconButton(
                                    onClick = { closeRenameForm() },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Cancelar edición",
                                        tint = Color(0xFFA89F95),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = renameInput,
                                onValueChange = {
                                    renameInput = it.replace(",", "")
                                    renameError = null
                                },
                                label = { Text("Nuevo nombre de etiqueta *") },
                                placeholder = { Text("Ej. Cables HDMI") },
                                singleLine = true,
                                isError = renameError != null,
                                supportingText = renameError?.let { { Text(it, color = Color(0xFFEF4444)) } },
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
                                    .testTag("input_rename_tag")
                            )

                            if (mergeTarget != null) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF261D17),
                                    border = BorderStroke(1.dp, Color(0xFFFFB77C).copy(alpha = 0.45f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.MergeType,
                                            contentDescription = null,
                                            tint = Color(0xFFFFB77C),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Ya existe «${mergeTarget.name}» (${mergeTarget.itemCount} obj). Se fusionarán automáticamente sin duplicados.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFFECE0DA)
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "Se actualizará automáticamente en los ${target.itemCount} objeto(s) asociados.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFA89F95)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { closeRenameForm() }) {
                                    Text("Cancelar", color = Color(0xFFA89F95))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        val clean = TagUtils.normalizeTag(renameInput)
                                        if (clean.isBlank()) {
                                            renameError = "El nombre no puede estar vacío"
                                            return@Button
                                        }
                                        if (clean != target.name) {
                                            onRenameTag(target.name, clean)
                                        }
                                        closeRenameForm()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFFFB77C),
                                        contentColor = Color(0xFF2B1705)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("btn_save_tag_rename")
                                ) {
                                    Text(
                                        text = if (mergeTarget != null) "Fusionar" else "Guardar",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Quick Search Filter when tags exist
            if (tagStats.isNotEmpty()) {
                OutlinedTextField(
                    value = filterQuery,
                    onValueChange = { filterQuery = it },
                    placeholder = { Text("Filtrar etiquetas...", color = Color(0xFFA89F95)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color(0xFFFFB77C),
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = if (filterQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { filterQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Limpiar filtro",
                                    tint = Color(0xFFA89F95),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF181512),
                        unfocusedContainerColor = Color(0xFF181512),
                        focusedBorderColor = Color(0xFFFFB77C),
                        unfocusedBorderColor = Color(0xFF2C2622),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color(0xFFECE0DA)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("input_filter_tags_sheet")
                )
            }

            if (filteredTags.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF181512),
                    border = BorderStroke(1.dp, Color(0xFF2C2622)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tag,
                            contentDescription = null,
                            tint = Color(0xFF6B5E55),
                            modifier = Modifier.size(34.dp)
                        )
                        Text(
                            text = if (tagStats.isEmpty()) {
                                "Aún no hay etiquetas guardadas en tus objetos."
                            } else {
                                "Ninguna etiqueta coincide con «$filterQuery»."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFA89F95)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredTags, key = { it.name.lowercase() }) { stat ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF181512),
                            border = BorderStroke(1.dp, Color(0xFF2C2622)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("tag_stat_row_${stat.name}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFF231B15))
                                            .border(1.dp, Color(0xFF2C2622), RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "#",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFFB77C)
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stat.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${stat.itemCount} objeto${if (stat.itemCount == 1) "" else "s"} asociado${if (stat.itemCount == 1) "" else "s"}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFA89F95),
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    IconButton(
                                        onClick = { openRenameForm(stat) },
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("btn_edit_tag_${stat.name}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Renombrar etiqueta ${stat.name}",
                                            tint = Color(0xFFFFB77C),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { tagToDelete = stat },
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("btn_delete_tag_${stat.name}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Eliminar etiqueta ${stat.name}",
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

    // Confirmation Dialog for Bulk Tag Deletion
    if (tagToDelete != null) {
        val target = tagToDelete!!
        AlertDialog(
            onDismissRequest = { tagToDelete = null },
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
                    Text("¿Eliminar etiqueta «#${target.name}»?")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Esta etiqueta se quitará de ${target.itemCount} objeto(s) del inventario.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF261D17),
                        border = BorderStroke(1.dp, Color(0xFFFFB77C).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Tus objetos y cajas se mantendrán intactos; únicamente se eliminará esta etiqueta de sus fichas.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFECE0DA),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteTag(target.name)
                        tagToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("confirm_delete_tag_button")
                ) {
                    Text("Eliminar Etiqueta", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { tagToDelete = null }) {
                    Text("Cancelar", color = Color(0xFFA89F95))
                }
            }
        )
    }
}
