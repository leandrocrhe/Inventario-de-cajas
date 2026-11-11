package com.example.data.backup

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.entity.BoxEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.ItemEntity
import com.example.data.local.entity.ZoneEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipInputStream

data class ImportResult(
    val success: Boolean,
    val message: String,
    val boxesCount: Int = 0,
    val itemsCount: Int = 0,
    val photosRestoredCount: Int = 0
)

class BackupManager(private val database: AppDatabase) {

    private val boxDao = database.boxDao()
    private val itemDao = database.itemDao()
    private val categoryDao = database.categoryDao()
    private val zoneDao = database.zoneDao()

    /**
     * Exports full inventory as a JSON string, optionally embedding photos encoded in Base64
     * if a Context is provided.
     */
    suspend fun exportJson(
        context: Context? = null,
        onProgress: ((String) -> Unit)? = null
    ): String = withContext(Dispatchers.IO) {
        onProgress?.invoke("Recuperando datos de la base de datos...")
        val boxes = boxDao.getAllBoxesSync()
        val items = itemDao.getAllItemsSync()
        val categories = categoryDao.getAllCategoriesSync()
        val zones = zoneDao.getAllZonesSync()

        val rootJson = JSONObject().apply {
            put("app", "Inventario de Cajas")
            put("version", 3)
            put("exportedAt", System.currentTimeMillis())

            // Categories
            val catArray = JSONArray()
            categories.forEach { cat ->
                catArray.put(JSONObject().apply {
                    put("id", cat.id)
                    put("name", cat.name)
                    put("iconName", cat.iconName)
                    put("colorHex", cat.colorHex)
                })
            }
            put("categories", catArray)

            // Zones
            val zoneArray = JSONArray()
            zones.forEach { zone ->
                zoneArray.put(JSONObject().apply {
                    put("name", zone.name)
                    put("colorHex", zone.colorHex)
                })
            }
            put("zones", zoneArray)

            // Boxes
            val boxArray = JSONArray()
            boxes.forEachIndexed { index, box ->
                onProgress?.invoke("Procesando cajas (${index + 1}/${boxes.size})...")
                val boxObj = JSONObject().apply {
                    put("id", box.id)
                    put("name", box.name)
                    put("description", box.description ?: "")
                    put("parentBoxId", box.parentBoxId ?: JSONObject.NULL)
                    put("colorHex", box.colorHex)
                    put("primaryPhotoUri", box.primaryPhotoUri ?: JSONObject.NULL)
                    put("zone", box.zone ?: JSONObject.NULL)
                    put("sortOrder", box.sortOrder)
                    put("createdAt", box.createdAt)

                    // Embed photo in Base64 if available
                    if (context != null && !box.primaryPhotoUri.isNullOrBlank()) {
                        val base64 = encodeImageToBase64(context, box.primaryPhotoUri)
                        if (!base64.isNullOrBlank()) {
                            put("photoBase64", base64)
                        }
                    }
                }
                boxArray.put(boxObj)
            }
            put("boxes", boxArray)

            // Items
            val itemArray = JSONArray()
            items.forEachIndexed { index, item ->
                onProgress?.invoke("Procesando objetos e imágenes (${index + 1}/${items.size})...")
                val itemObj = JSONObject().apply {
                    put("id", item.id)
                    put("name", item.name)
                    put("description", item.description ?: "")
                    put("boxId", item.boxId)
                    put("categoryId", item.categoryId ?: JSONObject.NULL)
                    put("primaryPhotoUri", item.primaryPhotoUri ?: JSONObject.NULL)
                    put("quantity", item.quantity)
                    put("tags", item.tags)
                    put("isLoaned", item.isLoaned)
                    put("loanedTo", item.loanedTo ?: JSONObject.NULL)
                    put("loanedAt", item.loanedAt ?: JSONObject.NULL)
                    put("notes", item.notes ?: JSONObject.NULL)
                    put("createdAt", item.createdAt)

                    // Embed photo in Base64 if available
                    if (context != null && !item.primaryPhotoUri.isNullOrBlank()) {
                        val base64 = encodeImageToBase64(context, item.primaryPhotoUri)
                        if (!base64.isNullOrBlank()) {
                            put("photoBase64", base64)
                        }
                    }
                }
                itemArray.put(itemObj)
            }
            put("items", itemArray)
        }

        onProgress?.invoke("Generando estructura JSON final...")
        rootJson.toString(2)
    }

    /**
     * Exports full inventory directly into an OutputStream (Storage Access Framework).
     */
    suspend fun exportJsonToStream(
        context: Context,
        outputStream: OutputStream,
        onProgress: ((String) -> Unit)? = null
    ) = withContext(Dispatchers.IO) {
        val jsonString = exportJson(context, onProgress)
        onProgress?.invoke("Escribiendo archivo de respaldo en almacenamiento...")
        outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.write(jsonString)
            writer.flush()
        }
    }

    /**
     * Exports catalog to CSV string.
     */
    suspend fun exportCsv(): String = withContext(Dispatchers.IO) {
        val boxes = boxDao.getAllBoxesSync().associateBy { it.id }
        val items = itemDao.getAllItemsSync()
        val categories = categoryDao.getAllCategoriesSync().associateBy { it.id }

        fun getPath(boxId: String): String {
            val path = mutableListOf<String>()
            var curr: BoxEntity? = boxes[boxId]
            while (curr != null) {
                path.add(0, curr.name)
                curr = curr.parentBoxId?.let { boxes[it] }
            }
            return if (path.isEmpty()) "Sin caja" else path.joinToString(" > ")
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        val sb = StringBuilder()
        sb.append("ID,Nombre,Cantidad,Categoria,Ruta_Caja,Etiquetas,Prestado,Prestado_A,Fecha_Prestamo,Descripcion,Notas\n")

        items.forEach { item ->
            val catName = item.categoryId?.let { categories[it]?.name } ?: "General"
            val path = getPath(item.boxId)
            val isLoanedStr = if (item.isLoaned) "SI" else "NO"
            val loanedToStr = item.loanedTo ?: ""
            val loanedDateStr = item.loanedAt?.let { dateFormat.format(Date(it)) } ?: ""

            sb.append(escapeCsv(item.id)).append(",")
            sb.append(escapeCsv(item.name)).append(",")
            sb.append(item.quantity).append(",")
            sb.append(escapeCsv(catName)).append(",")
            sb.append(escapeCsv(path)).append(",")
            sb.append(escapeCsv(item.tags)).append(",")
            sb.append(isLoanedStr).append(",")
            sb.append(escapeCsv(loanedToStr)).append(",")
            sb.append(escapeCsv(loanedDateStr)).append(",")
            sb.append(escapeCsv(item.description ?: "")).append(",")
            sb.append(escapeCsv(item.notes ?: "")).append("\n")
        }

        sb.toString()
    }

    /**
     * Exports catalog directly into an OutputStream (Storage Access Framework).
     */
    suspend fun exportCsvToStream(outputStream: OutputStream) = withContext(Dispatchers.IO) {
        val csv = exportCsv()
        outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.write(csv)
            writer.flush()
        }
    }

    private fun escapeCsv(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    /**
     * Imports inventory from an InputStream (Storage Access Framework).
     * Automatically detects plain JSON (.json) or compressed backups (.zip) containing JSON and photos.
     */
    suspend fun importJsonFromStream(
        context: Context,
        inputStream: InputStream,
        onProgress: ((String) -> Unit)? = null
    ): ImportResult = withContext(Dispatchers.IO) {
        try {
            onProgress?.invoke("Leyendo archivo de respaldo...")
            val buffered = BufferedInputStream(inputStream)
            buffered.mark(1024)
            val header = ByteArray(4)
            val bytesRead = buffered.read(header)
            buffered.reset()

            val isZip = bytesRead >= 4 &&
                    header[0] == 0x50.toByte() &&
                    header[1] == 0x4B.toByte() &&
                    header[2] == 0x03.toByte() &&
                    header[3] == 0x04.toByte()

            if (isZip) {
                onProgress?.invoke("Descomprimiendo archivo de respaldo ZIP...")
                var jsonContent: String? = null
                val extractedPhotosMap = mutableMapOf<String, String>()
                val photosDir = File(context.filesDir, "inventory_photos").apply { mkdirs() }

                val zipStream = ZipInputStream(buffered)
                var entry = zipStream.nextEntry
                while (entry != null) {
                    val entryName = entry.name.replace("\\", "/")
                    if (!entry.isDirectory) {
                        if (entryName.endsWith(".json", ignoreCase = true) && jsonContent == null) {
                            jsonContent = zipStream.bufferedReader(Charsets.UTF_8).readText()
                        } else if (entryName.endsWith(".jpg", ignoreCase = true) ||
                            entryName.endsWith(".jpeg", ignoreCase = true) ||
                            entryName.endsWith(".png", ignoreCase = true) ||
                            entryName.endsWith(".webp", ignoreCase = true)
                        ) {
                            val cleanName = File(entryName).name
                            val targetFile = File(photosDir, "restored_${System.currentTimeMillis()}_$cleanName")
                            targetFile.outputStream().use { out ->
                                zipStream.copyTo(out)
                            }
                            val restoredUri = Uri.fromFile(targetFile).toString()
                            extractedPhotosMap[entryName] = restoredUri
                            extractedPhotosMap[cleanName] = restoredUri
                        }
                    }
                    zipStream.closeEntry()
                    entry = zipStream.nextEntry
                }

                if (jsonContent.isNullOrBlank()) {
                    return@withContext ImportResult(
                        success = false,
                        message = "El archivo ZIP no contiene ningún archivo de catálogo .json válido."
                    )
                }

                importJson(jsonContent, context, onProgress, extractedPhotosMap)
            } else {
                val rawText = buffered.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val cleanText = rawText.trim().removePrefix("\uFEFF").trim()
                if (cleanText.isBlank()) {
                    return@withContext ImportResult(
                        success = false,
                        message = "El archivo seleccionado está vacío."
                    )
                }

                val isJson = cleanText.startsWith("{") || cleanText.startsWith("[")
                val isCsv = !isJson && (
                    cleanText.lines().firstOrNull()?.let { line ->
                        line.contains(",") || line.contains(";") ||
                        line.contains("Nombre", ignoreCase = true) ||
                        line.contains("Ruta_Caja", ignoreCase = true) ||
                        line.contains("ID", ignoreCase = true)
                    } == true
                )

                if (isCsv) {
                    importCsv(cleanText, onProgress)
                } else if (isJson) {
                    importJson(cleanText, context, onProgress)
                } else {
                    // Try parsing as CSV first; if not recognized, attempt JSON
                    val csvResult = importCsv(cleanText, onProgress)
                    if (csvResult.success) {
                        csvResult
                    } else {
                        importJson(cleanText, context, onProgress)
                    }
                }
            }
        } catch (e: Exception) {
            ImportResult(
                success = false,
                message = "Error de lectura de archivo: ${e.localizedMessage ?: "No se pudo leer el archivo"}"
            )
        }
    }

    /**
     * Parses RFC-4180 compliant CSV text into a matrix of rows and columns,
     * supporting escaped quotes and comma or semicolon delimiters.
     */
    private fun parseCsvRows(csvText: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val currentField = StringBuilder()
        val currentRow = mutableListOf<String>()
        var inQuotes = false
        var i = 0
        val len = csvText.length

        // Detect delimiter based on first line
        var delimiter = ','
        val firstLineEnd = csvText.indexOf('\n').let { if (it == -1) len else it }
        val firstLine = csvText.substring(0, firstLineEnd)
        if (firstLine.count { it == ';' } > firstLine.count { it == ',' }) {
            delimiter = ';'
        }

        while (i < len) {
            val c = csvText[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < len && csvText[i + 1] == '"') {
                        currentField.append('"')
                        i++ // Skip escaped quote
                    } else {
                        inQuotes = false
                    }
                } else {
                    currentField.append(c)
                }
            } else {
                when (c) {
                    '"' -> {
                        inQuotes = true
                    }
                    delimiter -> {
                        currentRow.add(currentField.toString().trim())
                        currentField.clear()
                    }
                    '\r' -> {
                        // Ignore carriage return
                    }
                    '\n' -> {
                        currentRow.add(currentField.toString().trim())
                        currentField.clear()
                        if (currentRow.any { it.isNotBlank() }) {
                            rows.add(currentRow.toList())
                        }
                        currentRow.clear()
                    }
                    else -> {
                        currentField.append(c)
                    }
                }
            }
            i++
        }
        if (currentField.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(currentField.toString().trim())
            if (currentRow.any { it.isNotBlank() }) {
                rows.add(currentRow.toList())
            }
        }
        return rows
    }

    /**
     * Imports flat CSV catalog into Room transactionally, resolving and creating
     * hierarchical boxes from "Ruta_Caja" (> separator) and associating categories.
     */
    suspend fun importCsv(
        csvContent: String,
        onProgress: ((String) -> Unit)? = null
    ): ImportResult = withContext(Dispatchers.IO) {
        try {
            onProgress?.invoke("Analizando estructura de archivo CSV...")
            val cleanCsv = csvContent.trim().removePrefix("\uFEFF").trim()
            if (cleanCsv.isBlank()) {
                return@withContext ImportResult(
                    success = false,
                    message = "El archivo CSV está vacío."
                )
            }

            val allRows = parseCsvRows(cleanCsv)
            if (allRows.isEmpty()) {
                return@withContext ImportResult(
                    success = false,
                    message = "No se encontraron filas con datos en el archivo CSV."
                )
            }

            // Determine if the first row is a header
            val firstRow = allRows[0]
            val isHeader = firstRow.any { col ->
                val lower = col.lowercase().trim()
                lower == "nombre" || lower == "name" || lower == "id" ||
                lower == "ruta_caja" || lower == "cantidad" || lower == "categoria" ||
                lower == "tags" || lower == "etiquetas"
            }

            // Default column positions
            var colId = 0
            var colName = 1
            var colQuantity = 2
            var colCategory = 3
            var colBoxPath = 4
            var colTags = 5
            var colLoaned = 6
            var colLoanedTo = 7
            var colLoanedDate = 8
            var colDescription = 9
            var colNotes = 10

            val dataRows = if (isHeader) {
                firstRow.forEachIndexed { index, headerName ->
                    val h = headerName.lowercase().trim().replace(" ", "_")
                        .replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u")
                    when {
                        h == "id" || h == "codigo" -> colId = index
                        h == "nombre" || h == "name" || h == "objeto" || h == "item" || h == "articulo" -> colName = index
                        h == "cantidad" || h == "quantity" || h == "qty" || h == "unidades" -> colQuantity = index
                        h == "categoria" || h == "category" || h == "cat" -> colCategory = index
                        h.contains("ruta") || h.contains("caja") || h.contains("box") || h.contains("path") -> colBoxPath = index
                        h == "etiquetas" || h == "tags" -> colTags = index
                        h == "prestado" || h == "loaned" || h == "is_loaned" -> colLoaned = index
                        h.contains("prestado_a") || h.contains("destinatario") || h.contains("loaned_to") -> colLoanedTo = index
                        h.contains("fecha") || h.contains("date") -> colLoanedDate = index
                        h.contains("descripcion") || h.contains("description") || h == "desc" -> colDescription = index
                        h.contains("nota") || h.contains("observaciones") -> colNotes = index
                    }
                }
                allRows.drop(1)
            } else {
                allRows
            }

            if (dataRows.isEmpty()) {
                return@withContext ImportResult(
                    success = false,
                    message = "El archivo CSV solo contenía encabezados y ninguna fila de datos."
                )
            }

            onProgress?.invoke("Cargando inventario actual para vincular cajas y categorías...")
            val existingBoxes = boxDao.getAllBoxesSync().toMutableList()
            val existingCategories = categoryDao.getAllCategoriesSync().toMutableList()
            val existingZones = zoneDao.getAllZonesSync().toMutableList()

            val newBoxesToInsert = mutableListOf<BoxEntity>()
            val newCategoriesToInsert = mutableListOf<CategoryEntity>()
            val newZonesToInsert = mutableListOf<ZoneEntity>()
            val itemsToInsert = mutableListOf<ItemEntity>()

            // Lookup maps
            val boxLookup = mutableMapOf<Pair<String, String?>, BoxEntity>()
            existingBoxes.forEach { box ->
                boxLookup[Pair(box.name.lowercase().trim(), box.parentBoxId)] = box
            }

            val categoryLookup = mutableMapOf<String, CategoryEntity>()
            existingCategories.forEach { cat ->
                categoryLookup[cat.name.lowercase().trim()] = cat
            }

            val zoneLookup = mutableSetOf<String>()
            existingZones.forEach { zone ->
                zoneLookup.add(zone.name.lowercase().trim())
            }

            // Helper to get or create boxes along hierarchical path (separated by >)
            fun resolveBoxForPath(pathStr: String): String {
                val cleanPath = pathStr.trim()
                val rawSegments = if (cleanPath.contains(">")) {
                    cleanPath.split(Regex("\\s*>\\s*"))
                } else if (cleanPath.contains("/")) {
                    cleanPath.split(Regex("\\s*/\\s*"))
                } else {
                    listOf(cleanPath)
                }.map { it.trim() }.filter { it.isNotBlank() }

                val segments = if (rawSegments.isEmpty() ||
                    rawSegments.size == 1 && (rawSegments[0].equals("Sin caja", ignoreCase = true) ||
                            rawSegments[0].equals("None", ignoreCase = true) ||
                            rawSegments[0].equals("General", ignoreCase = true) ||
                            rawSegments[0].isBlank())
                ) {
                    listOf("General")
                } else {
                    rawSegments
                }

                var currentParentId: String? = null
                for (segment in segments) {
                    val key = Pair(segment.lowercase(), currentParentId)
                    var box = boxLookup[key]
                    if (box == null) {
                        val newId = UUID.randomUUID().toString()
                        box = BoxEntity(
                            id = newId,
                            name = segment,
                            description = "Caja creada automáticamente desde importación CSV",
                            parentBoxId = currentParentId,
                            colorHex = "#F59E0B",
                            zone = if (currentParentId == null && segment != "General") segment else null,
                            createdAt = System.currentTimeMillis()
                        )
                        boxLookup[key] = box
                        newBoxesToInsert.add(box)
                        existingBoxes.add(box)

                        box.zone?.let { z ->
                            val zKey = z.lowercase()
                            if (!zoneLookup.contains(zKey)) {
                                zoneLookup.add(zKey)
                                newZonesToInsert.add(ZoneEntity(name = z, colorHex = "#F59E0B"))
                            }
                        }
                    }
                    currentParentId = box.id
                }

                return currentParentId ?: run {
                    val defaultBox = existingBoxes.firstOrNull() ?: run {
                        val genBox = BoxEntity(id = UUID.randomUUID().toString(), name = "General")
                        newBoxesToInsert.add(genBox)
                        existingBoxes.add(genBox)
                        genBox
                    }
                    defaultBox.id
                }
            }

            // Helper to get or create category
            fun resolveCategory(catNameRaw: String): String? {
                val trimmed = catNameRaw.trim()
                if (trimmed.isBlank() || trimmed.equals("Sin categoría", ignoreCase = true) || trimmed.equals("Ninguna", ignoreCase = true)) {
                    return null
                }
                val key = trimmed.lowercase()
                var cat = categoryLookup[key]
                if (cat == null) {
                    cat = CategoryEntity(
                        id = UUID.randomUUID().toString(),
                        name = trimmed,
                        iconName = "category",
                        colorHex = "#64748B"
                    )
                    categoryLookup[key] = cat
                    newCategoriesToInsert.add(cat)
                    existingCategories.add(cat)
                }
                return cat.id
            }

            val dateFormats = listOf(
                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()),
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
                SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()),
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            )

            fun parseLoanDate(rawDate: String): Long? {
                val d = rawDate.trim()
                if (d.isBlank()) return null
                d.toLongOrNull()?.let { return it }
                for (format in dateFormats) {
                    try {
                        return format.parse(d)?.time
                    } catch (_: Exception) {}
                }
                return null
            }

            onProgress?.invoke("Procesando ${dataRows.size} filas del catálogo CSV...")
            for (row in dataRows) {
                val rawName = row.getOrNull(colName)?.trim()
                if (rawName.isNullOrBlank()) continue

                val rawId = row.getOrNull(colId)?.trim()
                val itemId = if (!rawId.isNullOrBlank()) rawId else UUID.randomUUID().toString()

                val qtyStr = row.getOrNull(colQuantity)?.trim() ?: "1"
                val quantity = qtyStr.toIntOrNull()?.coerceAtLeast(1) ?: 1

                val catName = row.getOrNull(colCategory) ?: ""
                val categoryId = resolveCategory(catName)

                val boxPath = row.getOrNull(colBoxPath) ?: ""
                val targetBoxId = resolveBoxForPath(boxPath)

                val tags = row.getOrNull(colTags)?.trim() ?: ""

                val loanedStr = row.getOrNull(colLoaned)?.trim() ?: ""
                val isLoaned = loanedStr.equals("SI", ignoreCase = true) ||
                        loanedStr.equals("SÍ", ignoreCase = true) ||
                        loanedStr.equals("TRUE", ignoreCase = true) ||
                        loanedStr == "1" ||
                        loanedStr.equals("YES", ignoreCase = true)

                val loanedTo = row.getOrNull(colLoanedTo)?.trim()?.takeIf { it.isNotBlank() }
                val rawLoanDate = row.getOrNull(colLoanedDate) ?: ""
                val loanedAt = if (isLoaned) parseLoanDate(rawLoanDate) ?: System.currentTimeMillis() else null

                val description = row.getOrNull(colDescription)?.trim()?.takeIf { it.isNotBlank() }
                val notes = row.getOrNull(colNotes)?.trim()?.takeIf { it.isNotBlank() }

                itemsToInsert.add(
                    ItemEntity(
                        id = itemId,
                        name = rawName,
                        description = description,
                        boxId = targetBoxId,
                        categoryId = categoryId,
                        primaryPhotoUri = null,
                        quantity = quantity,
                        tags = tags,
                        isLoaned = isLoaned,
                        loanedTo = loanedTo,
                        loanedAt = loanedAt,
                        notes = notes,
                        createdAt = System.currentTimeMillis()
                    )
                )
            }

            if (itemsToInsert.isEmpty()) {
                return@withContext ImportResult(
                    success = false,
                    message = "El archivo CSV no contiene objetos con nombres válidos para importar."
                )
            }

            // Atomic Room Database Transaction
            onProgress?.invoke("Guardando cambios masivos en la base de datos...")
            database.withTransaction {
                if (newCategoriesToInsert.isNotEmpty()) {
                    categoryDao.insertCategories(newCategoriesToInsert)
                }
                if (newZonesToInsert.isNotEmpty()) {
                    zoneDao.insertZones(newZonesToInsert)
                }
                if (newBoxesToInsert.isNotEmpty()) {
                    // Sort topological if any parent-child in new boxes
                    val sortedNewBoxes = mutableListOf<BoxEntity>()
                    val insertedIds = mutableSetOf<String>()
                    val pending = newBoxesToInsert.toMutableList()
                    var prog = true
                    while (pending.isNotEmpty() && prog) {
                        prog = false
                        val it = pending.iterator()
                        while (it.hasNext()) {
                            val b = it.next()
                            val p = b.parentBoxId
                            if (p == null || !newBoxesToInsert.any { it.id == p } || insertedIds.contains(p)) {
                                sortedNewBoxes.add(b)
                                insertedIds.add(b.id)
                                it.remove()
                                prog = true
                            }
                        }
                    }
                    sortedNewBoxes.addAll(pending)
                    boxDao.insertBoxes(sortedNewBoxes)
                }
                itemDao.insertItems(itemsToInsert)
            }

            val totalBoxesInvolved = (newBoxesToInsert.map { it.id } + itemsToInsert.map { it.boxId }).distinct().size
            ImportResult(
                success = true,
                message = "Catálogo CSV importado con éxito: ${itemsToInsert.size} objetos y $totalBoxesInvolved cajas procesadas (${dataRows.size} filas analizadas).",
                boxesCount = totalBoxesInvolved,
                itemsCount = itemsToInsert.size
            )
        } catch (e: Exception) {
            ImportResult(
                success = false,
                message = "Error al procesar el archivo CSV: ${e.localizedMessage ?: "Formato de archivo o datos inválidos"}"
            )
        }
    }

    /**
     * Validates and imports inventory from JSON content into Room transactionally,
     * extracting Base64 images to the app's internal persistent storage.
     */
    suspend fun importJson(
        jsonContent: String,
        context: Context? = null,
        onProgress: ((String) -> Unit)? = null,
        extractedPhotosMap: Map<String, String> = emptyMap()
    ): ImportResult = withContext(Dispatchers.IO) {
        try {
            onProgress?.invoke("Validando estructura del archivo...")
            val cleanJson = jsonContent.trim().removePrefix("\uFEFF").trim()
            if (cleanJson.isBlank()) {
                return@withContext ImportResult(
                    success = false,
                    message = "El contenido del archivo está vacío o no es legible."
                )
            }

            val rootObj: JSONObject?
            val rootArray: JSONArray?

            if (cleanJson.startsWith("[")) {
                rootObj = null
                rootArray = try {
                    JSONArray(cleanJson)
                } catch (e: Exception) {
                    return@withContext ImportResult(
                        success = false,
                        message = "Formato JSON inválido: ${e.localizedMessage ?: "Estructura no válida"}"
                    )
                }
            } else {
                rootArray = null
                rootObj = try {
                    JSONObject(cleanJson)
                } catch (e: Exception) {
                    return@withContext ImportResult(
                        success = false,
                        message = "Formato JSON inválido: ${e.localizedMessage ?: "Estructura no válida"}"
                    )
                }
            }

            var photosRestoredCount = extractedPhotosMap.size

            // 1. Categories
            val parsedCategories = mutableListOf<CategoryEntity>()
            val catArray = rootObj?.optJSONArray("categories") ?: rootObj?.optJSONArray("categorias")
            if (catArray != null) {
                for (i in 0 until catArray.length()) {
                    val obj = catArray.optJSONObject(i) ?: continue
                    val id = obj.optString("id").takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
                    val name = obj.optString("name").takeIf { it.isNotBlank() } ?: obj.optString("nombre", "Categoría")
                    val iconName = obj.optString("iconName").takeIf { it.isNotBlank() } ?: obj.optString("icono", "category")
                    val colorHex = obj.optString("colorHex").takeIf { it.isNotBlank() } ?: obj.optString("color", "#64748B")
                    parsedCategories.add(
                        CategoryEntity(
                            id = id,
                            name = name,
                            iconName = iconName,
                            colorHex = colorHex
                        )
                    )
                }
            }

            // 2. Zones
            val parsedZones = mutableListOf<ZoneEntity>()
            val zoneArray = rootObj?.optJSONArray("zones") ?: rootObj?.optJSONArray("zonas")
            if (zoneArray != null) {
                for (i in 0 until zoneArray.length()) {
                    val obj = zoneArray.optJSONObject(i) ?: continue
                    val rawZoneName = when {
                        !obj.isNull("name") -> obj.optString("name").trim()
                        !obj.isNull("nombre") -> obj.optString("nombre").trim()
                        else -> null
                    }
                    val zoneName = rawZoneName?.takeIf {
                        it.isNotBlank() && !it.equals("null", ignoreCase = true) && !it.equals("undefined", ignoreCase = true)
                    }
                    if (!zoneName.isNullOrBlank() && parsedZones.none { it.name.equals(zoneName, ignoreCase = true) }) {
                        val color = when {
                            !obj.isNull("colorHex") -> obj.optString("colorHex").trim()
                            !obj.isNull("color") -> obj.optString("color").trim()
                            else -> null
                        }?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) } ?: "#F59E0B"
                        parsedZones.add(ZoneEntity(name = zoneName, colorHex = color))
                    }
                }
            }

            // 3. Boxes & Items
            val parsedBoxes = mutableListOf<BoxEntity>()
            val parsedItems = mutableListOf<ItemEntity>()

            val boxArray = rootArray ?: (rootObj?.optJSONArray("boxes") ?: rootObj?.optJSONArray("cajas"))
            if (boxArray != null) {
                for (i in 0 until boxArray.length()) {
                    onProgress?.invoke("Procesando cajas (${i + 1}/${boxArray.length()})...")
                    val obj = boxArray.optJSONObject(i) ?: continue
                    val id = obj.optString("id").takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) } ?: UUID.randomUUID().toString()
                    val name = when {
                        !obj.isNull("name") -> obj.optString("name").trim()
                        !obj.isNull("nombre") -> obj.optString("nombre").trim()
                        else -> null
                    }?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) } ?: "Caja"
                    val rawParentId = obj.optString("parentBoxId").takeIf { it.isNotBlank() }
                        ?: obj.optString("parent_box_id").takeIf { it.isNotBlank() }
                        ?: obj.optString("parentId").takeIf { it.isNotBlank() }
                    val parentId = if (rawParentId.isNullOrBlank() || rawParentId.equals("null", ignoreCase = true)) null else rawParentId

                    // Photo resolution
                    var restoredPhotoUri: String? = null
                    if (context != null && obj.has("photoBase64") && !obj.isNull("photoBase64")) {
                        val base64 = obj.getString("photoBase64")
                        val savedUri = decodeBase64ToInternalFile(context, base64, "box", id)
                        if (savedUri != null) {
                            restoredPhotoUri = savedUri
                            photosRestoredCount++
                        }
                    }

                    if (restoredPhotoUri == null) {
                        val rawUri = obj.optString("primaryPhotoUri").takeIf { it.isNotBlank() }
                            ?: obj.optString("photoUri").takeIf { it.isNotBlank() }
                            ?: obj.optString("foto").takeIf { it.isNotBlank() }
                        if (rawUri != null && !rawUri.equals("null", ignoreCase = true)) {
                            restoredPhotoUri = extractedPhotosMap[rawUri]
                                ?: extractedPhotosMap[File(rawUri).name]
                                ?: rawUri
                        }
                    }

                    // Strict handling of zone: null or "null" -> explicit null
                    val rawZone = when {
                        !obj.isNull("zone") -> obj.optString("zone").trim()
                        !obj.isNull("zona") -> obj.optString("zona").trim()
                        else -> null
                    }
                    val zoneVal = rawZone?.takeIf {
                        it.isNotBlank() &&
                        !it.equals("null", ignoreCase = true) &&
                        !it.equals("none", ignoreCase = true) &&
                        !it.equals("undefined", ignoreCase = true) &&
                        !it.equals("sin zona", ignoreCase = true) &&
                        !it.equals("sin zona asignada", ignoreCase = true)
                    }

                    if (zoneVal != null && parsedZones.none { it.name.equals(zoneVal, ignoreCase = true) }) {
                        val boxColor = when {
                            !obj.isNull("colorHex") -> obj.optString("colorHex").trim()
                            !obj.isNull("color") -> obj.optString("color").trim()
                            else -> null
                        }?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) } ?: "#F59E0B"
                        parsedZones.add(ZoneEntity(name = zoneVal, colorHex = boxColor))
                    }

                    val rawDesc = when {
                        !obj.isNull("description") -> obj.optString("description").trim()
                        !obj.isNull("descripcion") -> obj.optString("descripcion").trim()
                        else -> null
                    }?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }

                    val rawColor = when {
                        !obj.isNull("colorHex") -> obj.optString("colorHex").trim()
                        !obj.isNull("color") -> obj.optString("color").trim()
                        else -> null
                    }?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) } ?: "#F59E0B"

                    parsedBoxes.add(
                        BoxEntity(
                            id = id,
                            name = name,
                            description = rawDesc,
                            parentBoxId = parentId,
                            colorHex = rawColor,
                            primaryPhotoUri = restoredPhotoUri,
                            zone = zoneVal,
                            sortOrder = obj.optInt("sortOrder", i),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )

                    // Check for nested items inside this box
                    val innerItems = obj.optJSONArray("items") ?: obj.optJSONArray("objetos") ?: obj.optJSONArray("articulos")
                    if (innerItems != null) {
                        for (j in 0 until innerItems.length()) {
                            val itemObj = innerItems.optJSONObject(j) ?: continue
                            parseSingleItem(itemObj, id, context, extractedPhotosMap, parsedItems) {
                                photosRestoredCount++
                            }
                        }
                    }
                }
            }

            // Top-level Items
            val itemArray = rootObj?.optJSONArray("items") ?: rootObj?.optJSONArray("objetos") ?: rootObj?.optJSONArray("articulos")
            if (itemArray != null) {
                for (i in 0 until itemArray.length()) {
                    onProgress?.invoke("Procesando objetos (${i + 1}/${itemArray.length()})...")
                    val obj = itemArray.optJSONObject(i) ?: continue
                    val boxId = obj.optString("boxId").takeIf { it.isNotBlank() }
                        ?: obj.optString("box_id").takeIf { it.isNotBlank() }
                        ?: obj.optString("cajaId", "")
                    parseSingleItem(obj, boxId, context, extractedPhotosMap, parsedItems) {
                        photosRestoredCount++
                    }
                }
            }

            // Compatibility check
            if (parsedBoxes.isEmpty() && parsedItems.isEmpty() && parsedCategories.isEmpty()) {
                return@withContext ImportResult(
                    success = false,
                    message = "El archivo no contiene cajas ni objetos válidos para restaurar."
                )
            }

            // 4. Cycle prevention verification in parsed boxes
            val boxMap = parsedBoxes.associateBy { it.id }
            for (box in parsedBoxes) {
                var current: String? = box.parentBoxId
                val visited = mutableSetOf(box.id)
                while (current != null) {
                    if (visited.contains(current)) {
                        return@withContext ImportResult(
                            success = false,
                            message = "El archivo contiene una referencia circular inválida en la caja: ${box.name}"
                        )
                    }
                    visited.add(current)
                    current = boxMap[current]?.parentBoxId
                }
            }

            // 5. Topological Sort for SQLite Foreign Key Integrity
            // Root boxes (parentBoxId == null or not in backup) must be inserted before children
            val sortedBoxes = mutableListOf<BoxEntity>()
            val insertedBoxIds = mutableSetOf<String>()
            val pendingBoxes = parsedBoxes.toMutableList()

            var madeProgress = true
            while (pendingBoxes.isNotEmpty() && madeProgress) {
                madeProgress = false
                val it = pendingBoxes.iterator()
                while (it.hasNext()) {
                    val b = it.next()
                    val pId = b.parentBoxId
                    if (pId == null || !boxMap.containsKey(pId) || insertedBoxIds.contains(pId)) {
                        // If parent not in backup, convert to root box to avoid FK error
                        val safeBox = if (pId != null && !boxMap.containsKey(pId)) b.copy(parentBoxId = null) else b
                        sortedBoxes.add(safeBox)
                        insertedBoxIds.add(safeBox.id)
                        it.remove()
                        madeProgress = true
                    }
                }
            }
            // Add any remaining boxes with parent cleared to ensure no crash
            for (remaining in pendingBoxes) {
                val safeBox = remaining.copy(parentBoxId = null)
                sortedBoxes.add(safeBox)
                insertedBoxIds.add(safeBox.id)
            }

            // 6. Validate Items Foreign Keys
            val categoryIds = parsedCategories.map { it.id }.toSet()
            val validItems = parsedItems.map { item ->
                var safeBoxId = item.boxId
                if (!insertedBoxIds.contains(safeBoxId) && sortedBoxes.isNotEmpty()) {
                    safeBoxId = sortedBoxes.first().id
                }
                var safeCatId = item.categoryId
                if (safeCatId != null && !categoryIds.contains(safeCatId)) {
                    safeCatId = null
                }
                item.copy(boxId = safeBoxId, categoryId = safeCatId)
            }

            // 7. Atomic Room Database Restoration in Transaction
            onProgress?.invoke("Guardando cambios en la base de datos...")
            database.withTransaction {
                // Wipe previous data cleanly to avoid duplicate keys or orphaned rows
                itemDao.deleteAllItems()
                boxDao.deleteAllBoxes()
                categoryDao.deleteAllCategories()
                zoneDao.deleteAllZones()

                if (parsedCategories.isNotEmpty()) {
                    categoryDao.insertCategories(parsedCategories)
                }
                if (parsedZones.isNotEmpty()) {
                    zoneDao.insertZones(parsedZones)
                }
                if (sortedBoxes.isNotEmpty()) {
                    boxDao.insertBoxes(sortedBoxes)
                }
                if (validItems.isNotEmpty()) {
                    itemDao.insertItems(validItems)
                }
            }

            val photoMsg = if (photosRestoredCount > 0) " y $photosRestoredCount fotos" else ""
            ImportResult(
                success = true,
                message = "Copia de seguridad restaurada con éxito: ${sortedBoxes.size} cajas y ${validItems.size} objetos actualizados$photoMsg.",
                boxesCount = sortedBoxes.size,
                itemsCount = validItems.size,
                photosRestoredCount = photosRestoredCount
            )
        } catch (e: Exception) {
            ImportResult(
                success = false,
                message = "Error al procesar el archivo: ${e.localizedMessage ?: "Estructura inválida o archivo corrupto"}"
            )
        }
    }

    private fun parseSingleItem(
        obj: JSONObject,
        boxId: String,
        context: Context?,
        extractedPhotosMap: Map<String, String>,
        targetList: MutableList<ItemEntity>,
        onPhotoRestored: () -> Unit
    ) {
        val id = obj.optString("id").takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) } ?: UUID.randomUUID().toString()
        val name = when {
            !obj.isNull("name") -> obj.optString("name").trim()
            !obj.isNull("nombre") -> obj.optString("nombre").trim()
            else -> null
        }?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) } ?: "Objeto"

        val desc = when {
            !obj.isNull("description") -> obj.optString("description").trim()
            !obj.isNull("descripcion") -> obj.optString("descripcion").trim()
            else -> null
        }?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }

        val catId = when {
            !obj.isNull("categoryId") -> obj.optString("categoryId").trim()
            !obj.isNull("category_id") -> obj.optString("category_id").trim()
            !obj.isNull("categoriaId") -> obj.optString("categoriaId").trim()
            else -> null
        }?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }

        var restoredPhotoUri: String? = null
        if (context != null && obj.has("photoBase64") && !obj.isNull("photoBase64")) {
            val base64 = obj.getString("photoBase64")
            val savedUri = decodeBase64ToInternalFile(context, base64, "item", id)
            if (savedUri != null) {
                restoredPhotoUri = savedUri
                onPhotoRestored()
            }
        }

        if (restoredPhotoUri == null) {
            val rawUri = obj.optString("primaryPhotoUri").takeIf { it.isNotBlank() }
                ?: obj.optString("photoUri").takeIf { it.isNotBlank() }
                ?: obj.optString("foto").takeIf { it.isNotBlank() }
            if (rawUri != null && !rawUri.equals("null", ignoreCase = true)) {
                restoredPhotoUri = extractedPhotosMap[rawUri]
                    ?: extractedPhotosMap[File(rawUri).name]
                    ?: rawUri
            }
        }

        val quantity = obj.optInt("quantity", obj.optInt("cantidad", 1)).coerceAtLeast(1)
        val tags = when {
            !obj.isNull("tags") -> obj.optString("tags").trim()
            !obj.isNull("etiquetas") -> obj.optString("etiquetas").trim()
            else -> ""
        }.takeIf { !it.equals("null", ignoreCase = true) } ?: ""

        val isLoaned = obj.optBoolean("isLoaned", obj.optBoolean("prestado", false))
        val loanedTo = when {
            !obj.isNull("loanedTo") -> obj.optString("loanedTo").trim()
            !obj.isNull("prestado_a") -> obj.optString("prestado_a").trim()
            else -> null
        }?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }

        val loanedAt = if (obj.isNull("loanedAt") && obj.isNull("fecha_prestamo")) null else obj.optLong("loanedAt", obj.optLong("fecha_prestamo"))
        val notes = when {
            !obj.isNull("notes") -> obj.optString("notes").trim()
            !obj.isNull("notas") -> obj.optString("notas").trim()
            else -> null
        }?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }

        targetList.add(
            ItemEntity(
                id = id,
                name = name,
                description = desc,
                boxId = boxId,
                categoryId = catId,
                primaryPhotoUri = restoredPhotoUri,
                quantity = quantity,
                tags = tags,
                isLoaned = isLoaned,
                loanedTo = loanedTo,
                loanedAt = loanedAt,
                notes = notes,
                createdAt = obj.optLong("createdAt", System.currentTimeMillis())
            )
        )
    }

    /**
     * Reads and compresses image file/content into Base64 (max 1280px dimension to avoid memory bloat).
     */
    private fun encodeImageToBase64(context: Context, uriString: String?): String? {
        if (uriString.isNullOrBlank()) return null
        return try {
            val uri = Uri.parse(uriString)
            val bytes = if (uri.scheme == "file") {
                val file = File(uri.path ?: return null)
                if (file.exists()) file.readBytes() else null
            } else {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            } ?: return null

            if (bytes.isEmpty()) return null

            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

            val maxDim = maxOf(options.outWidth, options.outHeight)
            var sampleSize = 1
            if (maxDim > 1280) {
                while (maxDim / (sampleSize * 2) >= 1280) {
                    sampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
                ?: return Base64.encodeToString(bytes, Base64.NO_WRAP)

            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos)
            val compressedBytes = baos.toByteArray()
            bitmap.recycle()
            Base64.encodeToString(compressedBytes, Base64.NO_WRAP)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Decodes Base64 data into a persistent local file in the app's internal files directory.
     */
    private fun decodeBase64ToInternalFile(
        context: Context,
        base64Str: String?,
        prefix: String,
        id: String
    ): String? {
        if (base64Str.isNullOrBlank()) return null
        return try {
            val bytes = Base64.decode(base64Str, Base64.DEFAULT)
            if (bytes == null || bytes.isEmpty()) return null

            val photosDir = File(context.filesDir, "inventory_photos").apply { mkdirs() }
            val cleanId = id.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val fileName = "${prefix}_${cleanId}_${System.currentTimeMillis()}.jpg"
            val targetFile = File(photosDir, fileName)
            targetFile.writeBytes(bytes)
            Uri.fromFile(targetFile).toString()
        } catch (_: Exception) {
            null
        }
    }
}
