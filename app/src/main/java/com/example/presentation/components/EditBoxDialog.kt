package com.example.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.BoxEntity

data class SemanticBoxColor(
    val hex: String,
    val colorName: String,
    val meaning: String,
    val shortBadge: String
) {
    val headerDisplayLabel: String
        get() = "$colorName · $meaning"
}

val SEMANTIC_BOX_COLORS = listOf(
    SemanticBoxColor("#F59E0B", "Ámbar", "General / Taller", "Taller"),
    SemanticBoxColor("#3B82F6", "Azul", "Tecnología", "Tecno"),
    SemanticBoxColor("#10B981", "Esmeralda", "Hogar / Almacén", "Hogar"),
    SemanticBoxColor("#8B5CF6", "Púrpura", "Creativo", "Creativo"),
    SemanticBoxColor("#EF4444", "Coral", "Frágil / Prioritario", "Frágil"),
    SemanticBoxColor("#14B8A6", "Teal", "Archivos / Oficina", "Archivos"),
    SemanticBoxColor("#EC4899", "Magenta", "Textil / Personal", "Textil"),
    SemanticBoxColor("#C26343", "Terracota", "Bricolaje / Obra", "Obra"),
    SemanticBoxColor("#6366F1", "Índigo", "Audio / Estudio", "Estudio"),
    SemanticBoxColor("#64748B", "Pizarra", "Varios / Reserva", "Varios")
)

val PRESET_BOX_COLORS = SEMANTIC_BOX_COLORS.map { it.hex }

fun resolveSemanticBoxColor(hex: String?): SemanticBoxColor {
    val normalized = hex?.trim()?.uppercase().orEmpty()
    val canonicalHex = when (normalized) {
        "#A855F7" -> "#8B5CF6" // Legacy purple alias -> Púrpura Creativo
        else -> normalized
    }
    return SEMANTIC_BOX_COLORS.firstOrNull { it.hex.equals(canonicalHex, ignoreCase = true) }
        ?: SemanticBoxColor(
            hex = hex?.takeIf { it.isNotBlank() } ?: "#F59E0B",
            colorName = "Personalizado",
            meaning = "Etiqueta",
            shortBadge = "Caja"
        )
}

@Composable
fun EditBoxDialog(
    boxToEdit: BoxEntity?,
    currentParentBox: BoxEntity?,
    existingZones: List<String> = emptyList(),
    initialZone: String? = null,
    onSave: (name: String, description: String?, colorHex: String, photoUri: String?, zone: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(boxToEdit?.name ?: "") }
    var description by remember { mutableStateOf(boxToEdit?.description ?: "") }
    val initialCleanZone = (boxToEdit?.zone ?: initialZone)?.trim()?.takeIf {
        it.isNotBlank() && !it.equals("null", ignoreCase = true) && !it.equals("cajas sin zona asignada", ignoreCase = true) && !it.equals("sin estancia asignada", ignoreCase = true)
    } ?: ""
    var zone by remember { mutableStateOf(initialCleanZone) }
    var selectedColor by remember { mutableStateOf(boxToEdit?.colorHex ?: "#F59E0B") }
    var photoUri by remember { mutableStateOf(boxToEdit?.primaryPhotoUri) }

    val selectedSemantic = remember(selectedColor) { resolveSemanticBoxColor(selectedColor) }
    val selectedColorValue = remember(selectedColor) { parseColorSafe(selectedColor) }
    val colorListState = rememberLazyListState()

    LaunchedEffect(boxToEdit?.id) {
        val initialIdx = SEMANTIC_BOX_COLORS.indexOfFirst {
            it.hex.equals(selectedSemantic.hex, ignoreCase = true)
        }
        if (initialIdx > 3) {
            colorListState.scrollToItem((initialIdx - 1).coerceAtLeast(0))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(selectedColorValue.copy(alpha = 0.2f))
                        .border(1.dp, selectedColorValue.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = null,
                        tint = selectedColorValue,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (boxToEdit == null) "Nueva Caja" else "Editar Caja",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (boxToEdit == null && currentParentBox != null) {
                        Text(
                            text = "Dentro de: ${currentParentBox.name}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
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
                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre de la caja *") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("box_name_input")
                )

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción o ubicación (ej. Trastero / Balda 2)") },
                    minLines = 2,
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("box_desc_input")
                )

                // Zona / Ubicación (asignar existente o crear nueva)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Zona o Estancia",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (zone.isNotBlank()) {
                            TextButton(onClick = { zone = "" }) {
                                Text("Quitar zona", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    val validExistingZones = remember(existingZones) {
                        existingZones.filter { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                    }

                    // Chips de zonas existentes
                    if (validExistingZones.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            validExistingZones.forEach { z ->
                                val isSelected = zone.equals(z, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { zone = if (isSelected) "" else z },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Place,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    label = { Text(z) },
                                    modifier = Modifier.testTag("zone_chip_$z")
                                )
                            }
                        }
                    }

                    // Campo para seleccionar o escribir una nueva zona
                    OutlinedTextField(
                        value = zone,
                        onValueChange = { zone = it },
                        label = { Text("Asignar o crear nueva zona") },
                        placeholder = { Text("ej. Garaje, Trastero, Taller, Desván…") },
                        leadingIcon = {
                            Icon(Icons.Default.Place, contentDescription = null)
                        },
                        trailingIcon = {
                            if (zone.isNotBlank()) {
                                IconButton(onClick = { zone = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Limpiar")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("box_zone_input")
                    )
                }

                // Semantic Color Selector (Strict circular geometry + dynamic header meaning + LazyRow)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Color de etiqueta semántica",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(9999.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, selectedColorValue.copy(alpha = 0.5f)),
                            modifier = Modifier.testTag("selected_semantic_color_badge")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(selectedColorValue)
                                )
                                Text(
                                    text = selectedSemantic.headerDisplayLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LazyRow(
                            state = colorListState,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("semantic_color_lazy_row")
                        ) {
                            items(
                                items = SEMANTIC_BOX_COLORS,
                                key = { it.hex }
                            ) { semanticOption ->
                                val swatchColor = parseColorSafe(semanticOption.hex)
                                val isSelected = selectedColor.equals(semanticOption.hex, ignoreCase = true) ||
                                    (semanticOption.hex == "#8B5CF6" && selectedColor.equals("#A855F7", ignoreCase = true))
                                val ringColor by animateColorAsState(
                                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                    label = "swatch_ring_color"
                                )
                                val checkTint = if (swatchColor.luminance() > 0.62f) {
                                    Color(0xFF1A1410)
                                } else {
                                    Color.White
                                }

                                // 48.dp x 48.dp strict square touch target clipped to CircleShape
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .clickable(
                                            role = Role.RadioButton,
                                            onClickLabel = "${semanticOption.colorName}: ${semanticOption.meaning}"
                                        ) {
                                            selectedColor = semanticOption.hex
                                        }
                                        .semantics {
                                            selected = isSelected
                                            contentDescription = "${semanticOption.colorName}: ${semanticOption.meaning}"
                                        }
                                        .testTag("color_preset_${semanticOption.hex}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Outer highlight ring (44.dp x 44.dp perfect circle)
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) swatchColor.copy(alpha = 0.16f) else Color.Transparent
                                            )
                                            .border(
                                                width = if (isSelected) 2.dp else 0.dp,
                                                color = ringColor,
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // Inner color swatch (34.dp x 34.dp perfect circle)
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(swatchColor)
                                                .border(
                                                    width = if (isSelected) 1.5.dp else 1.dp,
                                                    color = if (isSelected) {
                                                        Color.White.copy(alpha = 0.9f)
                                                    } else {
                                                        MaterialTheme.colorScheme.outlineVariant
                                                    },
                                                    shape = CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = checkTint,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Photo picker & camera capture section
                PhotoPickerSection(
                    photoUri = photoUri,
                    onPhotoSelected = { photoUri = it },
                    title = "Foto de la caja (Cámara o Galería)",
                    prefixTag = "box_photo"
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanZ = zone.trim().takeIf {
                        it.isNotBlank() && !it.equals("null", ignoreCase = true) && !it.equals("cajas sin zona asignada", ignoreCase = true)
                    }
                    onSave(name.trim(), description.trim().takeIf { it.isNotBlank() }, selectedColor, photoUri, cleanZ)
                },
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("save_box_button")
            ) {
                Text(
                    text = if (boxToEdit == null) "Crear Caja" else "Guardar Cambios",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
