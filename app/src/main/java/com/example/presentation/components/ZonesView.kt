package com.example.presentation.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.BoxEntity

private val ZONE_COLOR_PALETTE = listOf(
    "#F59E0B", // Amber
    "#EF4444", // Red
    "#3B82F6", // Blue
    "#10B981", // Emerald
    "#8B5CF6", // Purple
    "#EC4899", // Pink
    "#14B8A6", // Teal
    "#06B6D4"  // Cyan
)

/**
 * Pantalla Administrar Zonas sincronizada 100% con las zonas del modal 'Nueva Caja'.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ZonesView(
    zones: List<String>,
    allBoxes: List<BoxEntity>,
    totalItemsCount: Int,
    onZoneClick: (zoneName: String) -> Unit,
    onBoxClick: (boxId: String) -> Unit,
    onCreateZone: (name: String, colorHex: String) -> Unit,
    onRenameZone: (oldName: String, newName: String) -> Unit,
    onDeleteZone: (name: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isCreateDialogOpen by remember { mutableStateOf(false) }
    var zoneToRename by remember { mutableStateOf<String?>(null) }
    var zoneToDelete by remember { mutableStateOf<String?>(null) }
    val cleanZones = remember(zones) { zones.filter { it.isNotBlank() && !it.equals("null", ignoreCase = true) } }
    val unassignedBoxes = remember(allBoxes) { allBoxes.filter { it.zone.isNullOrBlank() || it.zone.equals("null", ignoreCase = true) } }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("zones_view"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Toolbar: Title & Action
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Administrar Zonas",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${zones.size} zonas configuradas · ${allBoxes.size} cajas organizadas",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFA6978A)
                    )
                }

                Button(
                    onClick = { isCreateDialogOpen = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color(0xFF2F1500)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("create_zone_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AddLocation,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Nueva Zona",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Zone Metrics Bento Card
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF14110E),
                border = BorderStroke(1.dp, Color(0xFF28211B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Estado de Almacenamiento",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(9999.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "100% Organizado",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF10B981),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Metric 1: Zonas Activas
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF1B1510),
                            border = BorderStroke(1.dp, Color(0xFF2C221A)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${zones.size}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Zonas activas",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF8E8175),
                                    fontSize = 10.sp
                                )
                            }
                        }

                        // Metric 2: Cajas Ubicadas
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF1B1510),
                            border = BorderStroke(1.dp, Color(0xFF2C221A)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${allBoxes.size}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Cajas totales",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF8E8175),
                                    fontSize = 10.sp
                                )
                            }
                        }

                        // Metric 3: Objetos
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF1B1510),
                            border = BorderStroke(1.dp, Color(0xFF2C221A)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "$totalItemsCount",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                                Text(
                                    text = "Objetos",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF8E8175),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Zones Stack List
        if (cleanZones.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF14110E),
                    border = BorderStroke(1.dp, Color(0xFF28211B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = Color(0xFF8E8175),
                            modifier = Modifier.size(46.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No hay zonas creadas todavía",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Crea zonas como «Trastero», «Taller» o «Garaje» para organizar todas tus cajas.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF8E8175),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { isCreateDialogOpen = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Crear Primera Zona", color = Color(0xFF2F1500), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            items(cleanZones, key = { it }) { zoneName ->
                val boxesInZone = remember(allBoxes, zoneName) {
                    allBoxes.filter { it.zone.equals(zoneName, ignoreCase = true) }
                }
                val zoneColor = remember(zoneName, boxesInZone) {
                    boxesInZone.firstOrNull()?.let { parseColorSafe(it.colorHex) }
                        ?: run {
                            val hash = kotlin.math.abs(zoneName.hashCode())
                            parseColorSafe(ZONE_COLOR_PALETTE[hash % ZONE_COLOR_PALETTE.size])
                        }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF14110E),
                    border = BorderStroke(1.dp, Color(0xFF28211B)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onZoneClick(zoneName) }
                        .testTag("zone_card_$zoneName")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 14.dp)
                    ) {
                        // Header de la Zona
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                // Color Pill / Icono
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1E1915))
                                        .border(1.dp, zoneColor.copy(alpha = 0.4f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = zoneColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = zoneName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (boxesInZone.isEmpty()) "Sin cajas asignadas todavía" else "${boxesInZone.size} cajas asignadas",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFA6978A)
                                    )
                                }
                            }

                            // Botones de acción (Editar / Eliminar)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { zoneToRename = zoneName },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .testTag("edit_zone_$zoneName")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Editar zona",
                                        tint = Color(0xFFA6978A),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { zoneToDelete = zoneName },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .testTag("delete_zone_$zoneName")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Eliminar zona",
                                        tint = Color(0xFFEF4444).copy(alpha = 0.8f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Cajas dentro de la zona (chips interactivos)
                        if (boxesInZone.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                boxesInZone.forEach { box ->
                                    val bColor = remember(box.colorHex) { parseColorSafe(box.colorHex) }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF1E1814),
                                        border = BorderStroke(1.dp, Color(0xFF32261E)),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { onBoxClick(box.id) }
                                            .testTag("box_chip_${box.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(bColor)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = box.name,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFFECE0DA),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
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

        // Cajas sin zona asignada (si las hubiera)
        if (unassignedBoxes.isNotEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF181512),
                    border = BorderStroke(1.dp, Color(0xFF2C221A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = Color(0xFFA89F95),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Cajas sin zona asignada (${unassignedBoxes.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFECE0DA)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            unassignedBoxes.forEach { box ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF221A15),
                                    border = BorderStroke(1.dp, Color(0xFF382B22)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onBoxClick(box.id) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = box.name,
                                            style = MaterialTheme.typography.labelSmall,
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

        // Storage classification banner
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1F1510),
                border = BorderStroke(1.dp, Color(0xFF3E281C)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF93370D)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Consejo de Organización",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Las zonas creadas aquí aparecen automáticamente al crear cajas en '+ Nueva Caja'.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFD48960)
                        )
                    }
                }
            }
        }
    }

    // Modal: Crear Nueva Zona
    if (isCreateDialogOpen) {
        var newZoneName by remember { mutableStateOf("") }
        var selectedColor by remember { mutableStateOf(ZONE_COLOR_PALETTE[0]) }

        AlertDialog(
            onDismissRequest = { isCreateDialogOpen = false },
            title = {
                Text(
                    text = "Nueva Zona o Estancia",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Crea una zona para agrupar cajas (ej. Trastero, Taller, Estudio, Buhardilla).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = newZoneName,
                        onValueChange = { newZoneName = it },
                        label = { Text("Nombre de la zona *") },
                        placeholder = { Text("ej. Garaje, Buhardilla, Terraza…") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_zone_name_input")
                    )

                    // Color Picker
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Color identificador",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ZONE_COLOR_PALETTE.forEach { hex ->
                                val color = remember(hex) { parseColorSafe(hex) }
                                val isSelected = selectedColor == hex
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) Color.White else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedColor = hex }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = newZoneName.trim()
                        if (trimmed.isNotBlank()) {
                            onCreateZone(trimmed, selectedColor)
                            isCreateDialogOpen = false
                        }
                    },
                    enabled = newZoneName.isNotBlank(),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("confirm_create_zone_button")
                ) {
                    Text("Crear Zona")
                }
            },
            dismissButton = {
                TextButton(onClick = { isCreateDialogOpen = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Modal: Renombrar Zona
    zoneToRename?.let { currentZoneName ->
        var updatedName by remember { mutableStateOf(currentZoneName) }

        AlertDialog(
            onDismissRequest = { zoneToRename = null },
            title = {
                Text(
                    text = "Renombrar Zona",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "El nuevo nombre se actualizará en todas las cajas asignadas a esta zona.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = updatedName,
                        onValueChange = { updatedName = it },
                        label = { Text("Nombre de la zona") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("rename_zone_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = updatedName.trim()
                        if (trimmed.isNotBlank() && trimmed != currentZoneName) {
                            onRenameZone(currentZoneName, trimmed)
                        }
                        zoneToRename = null
                    },
                    enabled = updatedName.isNotBlank() && updatedName.trim() != currentZoneName,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("confirm_rename_zone_button")
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { zoneToRename = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Modal: Eliminar Zona
    zoneToDelete?.let { zoneName ->
        AlertDialog(
            onDismissRequest = { zoneToDelete = null },
            title = {
                Text(
                    text = "¿Eliminar zona «$zoneName»?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Esta acción quitará la zona «$zoneName» de la lista. Las cajas asociadas no se eliminarán, pero quedarán sin zona asignada.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteZone(zoneName)
                        zoneToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("confirm_delete_zone_button")
                ) {
                    Text("Eliminar Zona")
                }
            },
            dismissButton = {
                TextButton(onClick = { zoneToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
