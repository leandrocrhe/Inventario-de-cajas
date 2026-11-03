package com.example.presentation.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.entity.BoxEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.ItemEntity

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditItemDialog(
    itemToEdit: ItemEntity?,
    currentBox: BoxEntity?,
    allBoxes: List<BoxEntity>,
    categories: List<CategoryEntity>,
    onSave: (name: String, description: String?, quantity: Int, categoryId: String?, tags: String, photoUri: String?, targetBoxId: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(itemToEdit?.name ?: "") }
    var description by remember { mutableStateOf(itemToEdit?.description ?: "") }
    var quantity by remember { mutableIntStateOf(itemToEdit?.quantity ?: 1) }
    var selectedCategoryId by remember { mutableStateOf(itemToEdit?.categoryId) }
    var tags by remember { mutableStateOf(itemToEdit?.tags ?: "") }
    var photoUri by remember { mutableStateOf(itemToEdit?.primaryPhotoUri) }

    // If currentBox is null (e.g. from search or root), let user select box
    var selectedBoxId by remember {
        mutableStateOf(itemToEdit?.boxId ?: currentBox?.id ?: allBoxes.firstOrNull()?.id)
    }
    var boxDropdownExpanded by remember { mutableStateOf(false) }

    val quickTags = listOf("Frágil", "Cables", "Herramienta", "Documento", "Invierno", "Repuesto", "Nuevo")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = if (itemToEdit == null) "Nuevo Objeto" else "Editar Objeto",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (currentBox != null) {
                    Text(
                        text = "Guardar en: ${currentBox.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // If no current box is set, select box
                if (currentBox == null) {
                    val activeBoxName = allBoxes.find { it.id == selectedBoxId }?.name ?: "Selecciona una caja"
                    ExposedDropdownMenuBox(
                        expanded = boxDropdownExpanded,
                        onExpandedChange = { boxDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = activeBoxName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Caja de destino *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = boxDropdownExpanded) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = boxDropdownExpanded,
                            onDismissRequest = { boxDropdownExpanded = false }
                        ) {
                            allBoxes.forEach { b ->
                                DropdownMenuItem(
                                    text = { Text(b.name) },
                                    onClick = {
                                        selectedBoxId = b.id
                                        boxDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del objeto *") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("item_name_input")
                )

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Detalles, modelo o características") },
                    minLines = 2,
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("item_desc_input")
                )

                // Quantity selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cantidad de unidades:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        FilledIconButton(
                            onClick = { if (quantity > 1) quantity-- },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Menos", modifier = Modifier.size(16.dp))
                        }

                        Text(
                            text = "$quantity",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )

                        FilledIconButton(
                            onClick = { quantity++ },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Más",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Category chips
                Column {
                    Text(
                        text = "Categoría",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { cat ->
                            val isSelected = selectedCategoryId == cat.id
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedCategoryId = if (isSelected) null else cat.id
                                },
                                label = { Text(cat.name) }
                            )
                        }
                    }
                }

                // Tags
                Column {
                    OutlinedTextField(
                        value = tags,
                        onValueChange = { tags = it },
                        label = { Text("Etiquetas (separadas por coma)") },
                        placeholder = { Text("ej. Cables, HDMI, Video") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        quickTags.forEach { qTag ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.clickable {
                                    tags = if (tags.isBlank()) qTag else "$tags, $qTag"
                                }
                            ) {
                                Text(
                                    text = "+ $qTag",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Photo picker & camera capture section
                PhotoPickerSection(
                    photoUri = photoUri,
                    onPhotoSelected = { photoUri = it },
                    title = "Foto del objeto (Cámara o Galería)",
                    prefixTag = "item_photo"
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(name, description, quantity, selectedCategoryId, tags, photoUri, selectedBoxId)
                },
                enabled = name.isNotBlank() && selectedBoxId != null,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("save_item_button")
            ) {
                Text(if (itemToEdit == null) "Guardar Objeto" else "Guardar Cambios")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
