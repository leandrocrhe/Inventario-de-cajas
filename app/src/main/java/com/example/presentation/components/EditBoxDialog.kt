package com.example.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import com.example.data.local.entity.BoxEntity

val PRESET_BOX_COLORS = listOf(
    "#F59E0B", // Amber (General / Tools)
    "#3B82F6", // Blue (Tech / Audio)
    "#10B981", // Emerald (Home / Garden)
    "#A855F7", // Purple (Documents / Studio)
    "#EF4444", // Red (Fragile / Winter)
    "#C26343", // Terracotta (Workshop)
    "#14B8A6", // Teal (Clothes / Seasonal)
    "#64748B"  // Slate (Warehouse)
)

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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(parseColorSafe(selectedColor).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = null,
                        tint = parseColorSafe(selectedColor),
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

                // Color picker
                Column {
                    Text(
                        text = "Etiqueta semántica de color",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        PRESET_BOX_COLORS.forEach { hex ->
                            val color = parseColorSafe(hex)
                            val isSelected = selectedColor.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = hex }
                                    .testTag("color_preset_$hex"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                    )
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
