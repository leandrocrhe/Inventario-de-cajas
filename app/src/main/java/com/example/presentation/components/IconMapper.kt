package com.example.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AllInbox
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Hardware
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Microwave
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.graphics.vector.ImageVector

data class CategoryIconOption(
    val key: String,
    val label: String,
    val themeGroup: String,
    val vector: ImageVector
)

/**
 * Curated, duplicate-free Material Symbols icon catalog tailored for inventory categories.
 * Excludes sports/leisure and medical/first-aid icons.
 */
object IconMapper {

    val curatedIconOptions: List<CategoryIconOption> = listOf(
        // General / Base (preserved without duplicating themed icons)
        CategoryIconOption("category", "General", "General", Icons.Default.Category),
        CategoryIconOption("palette", "Arte y Color", "General", Icons.Default.Palette),
        CategoryIconOption("tag", "Etiqueta", "General", Icons.Default.Tag),
        CategoryIconOption("schedule", "Temporal", "General", Icons.Default.Schedule),

        // Herramientas y Taller
        CategoryIconOption("build", "Herramientas", "Herramientas y Taller", Icons.Default.Build),
        CategoryIconOption("hardware", "Ferretería", "Herramientas y Taller", Icons.Default.Hardware),
        CategoryIconOption("bolt", "Eléctrico", "Herramientas y Taller", Icons.Default.Bolt),
        CategoryIconOption("construction", "Taller y Obra", "Herramientas y Taller", Icons.Default.Construction),
        CategoryIconOption("settings", "Mecánica", "Herramientas y Taller", Icons.Default.Settings),
        CategoryIconOption("tune", "Ajustes / Calibración", "Herramientas y Taller", Icons.Default.Tune),
        CategoryIconOption("precision_manufacturing", "Maquinaria", "Herramientas y Taller", Icons.Default.PrecisionManufacturing),

        // Tecnología, Cables y Electrónica
        CategoryIconOption("memory", "Electrónica", "Tecnología y Electrónica", Icons.Default.Memory),
        CategoryIconOption("cable", "Cables", "Tecnología y Electrónica", Icons.Default.Cable),
        CategoryIconOption("devices", "Dispositivos", "Tecnología y Electrónica", Icons.Default.Devices),
        CategoryIconOption("smartphone", "Móviles", "Tecnología y Electrónica", Icons.Default.Smartphone),
        CategoryIconOption("router", "Redes y Router", "Tecnología y Electrónica", Icons.Default.Router),
        CategoryIconOption("power", "Energía y Enchufes", "Tecnología y Electrónica", Icons.Default.Power),
        CategoryIconOption("headphones", "Auriculares", "Tecnología y Electrónica", Icons.Default.Headphones),
        CategoryIconOption("speaker", "Audio y Altavoces", "Tecnología y Electrónica", Icons.Default.Speaker),

        // Documentos y Oficina
        CategoryIconOption("description", "Documentos", "Documentos y Oficina", Icons.Default.Description),
        CategoryIconOption("article", "Notas / Artículos", "Documentos y Oficina", Icons.AutoMirrored.Filled.Article),
        CategoryIconOption("folder", "Carpetas", "Documentos y Oficina", Icons.Default.Folder),
        CategoryIconOption("folder_special", "Archivo Especial", "Documentos y Oficina", Icons.Default.FolderSpecial),
        CategoryIconOption("receipt_long", "Facturas / Tickets", "Documentos y Oficina", Icons.AutoMirrored.Filled.ReceiptLong),
        CategoryIconOption("badge", "Tarjetas / ID", "Documentos y Oficina", Icons.Default.Badge),
        CategoryIconOption("menu_book", "Manuales", "Documentos y Oficina", Icons.AutoMirrored.Filled.MenuBook),
        CategoryIconOption("book", "Libros", "Documentos y Oficina", Icons.Default.Book),

        // Hogar, Ropa y Almacenamiento
        CategoryIconOption("home", "Hogar", "Hogar y Almacenamiento", Icons.Default.Home),
        CategoryIconOption("checkroom", "Ropa y Textil", "Hogar y Almacenamiento", Icons.Default.Checkroom),
        CategoryIconOption("kitchen", "Cocina", "Hogar y Almacenamiento", Icons.Default.Kitchen),
        CategoryIconOption("microwave", "Electrodomésticos", "Hogar y Almacenamiento", Icons.Default.Microwave),
        CategoryIconOption("inventory_2", "Almacenamiento", "Hogar y Almacenamiento", Icons.Default.Inventory2),
        CategoryIconOption("package_2", "Paquetes y Cajas", "Hogar y Almacenamiento", Icons.Default.AllInbox),
        CategoryIconOption("lightbulb", "Iluminación", "Hogar y Almacenamiento", Icons.Default.Lightbulb),
        CategoryIconOption("cleaning_services", "Limpieza", "Hogar y Almacenamiento", Icons.Default.CleaningServices)
    ).distinctBy { it.key.lowercase() }

    private val iconMap: Map<String, ImageVector> = curatedIconOptions.associate {
        it.key.lowercase() to it.vector
    }

    private val legacyAliasMap: Map<String, ImageVector> = mapOf(
        "inventory" to Icons.Default.Inventory2,
        "hiking" to Icons.Default.Hiking,
        "all_inbox" to Icons.Default.AllInbox
    )

    fun getOption(iconName: String): CategoryIconOption? {
        val key = iconName.trim().lowercase()
        return curatedIconOptions.find { it.key.equals(key, ignoreCase = true) }
    }

    fun getCategoryIcon(iconName: String, fallbackName: String = ""): ImageVector {
        val key = iconName.trim().lowercase()
        iconMap[key]?.let { return it }
        legacyAliasMap[key]?.let { return it }

        val lowerName = fallbackName.trim().lowercase()
        return when {
            lowerName.contains("cable") || lowerName.contains("conex") -> Icons.Default.Cable
            lowerName.contains("herramienta") || lowerName.contains("tool") || lowerName.contains("taller") -> Icons.Default.Build
            lowerName.contains("electr") || lowerName.contains("tecno") || lowerName.contains("chip") -> Icons.Default.Memory
            lowerName.contains("audio") || lowerName.contains("sonido") -> Icons.Default.Headphones
            lowerName.contains("arte") || lowerName.contains("tinta") || lowerName.contains("pintur") -> Icons.Default.Palette
            lowerName.contains("ropa") || lowerName.contains("textil") || lowerName.contains("invierno") -> Icons.Default.Checkroom
            lowerName.contains("cocina") -> Icons.Default.Kitchen
            lowerName.contains("doc") || lowerName.contains("papel") || lowerName.contains("factura") -> Icons.Default.Description
            lowerName.contains("libro") || lowerName.contains("manual") -> Icons.AutoMirrored.Filled.MenuBook
            lowerName.contains("hogar") || lowerName.contains("casa") -> Icons.Default.Home
            lowerName.contains("limpieza") -> Icons.Default.CleaningServices
            lowerName.contains("luz") || lowerName.contains("ilumin") -> Icons.Default.Lightbulb
            else -> Icons.Default.Category
        }
    }
}
