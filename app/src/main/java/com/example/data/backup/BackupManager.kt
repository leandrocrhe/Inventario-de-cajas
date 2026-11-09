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
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
     */
    suspend fun importJsonFromStream(
        context: Context,
        inputStream: InputStream,
        onProgress: ((String) -> Unit)? = null
    ): ImportResult = withContext(Dispatchers.IO) {
        try {
            onProgress?.invoke("Leyendo archivo de respaldo...")
            val content = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            importJson(content, context, onProgress)
        } catch (e: Exception) {
            ImportResult(
                success = false,
                message = "Error de lectura de archivo: ${e.localizedMessage ?: "No se pudo leer el archivo"}"
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
        onProgress: ((String) -> Unit)? = null
    ): ImportResult = withContext(Dispatchers.IO) {
        try {
            onProgress?.invoke("Validando estructura del archivo...")
            val root = JSONObject(jsonContent)

            // Validate header
            val appIdentifier = root.optString("app", "")
            if (!appIdentifier.contains("Inventario", ignoreCase = true) && !root.has("boxes")) {
                return@withContext ImportResult(
                    success = false,
                    message = "El archivo seleccionado no es un respaldo compatible con Inventario de Cajas."
                )
            }

            var photosRestoredCount = 0

            // 1. Categories
            val parsedCategories = mutableListOf<CategoryEntity>()
            if (root.has("categories")) {
                val catArray = root.getJSONArray("categories")
                for (i in 0 until catArray.length()) {
                    val obj = catArray.getJSONObject(i)
                    parsedCategories.add(
                        CategoryEntity(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            iconName = obj.optString("iconName", "category"),
                            colorHex = obj.optString("colorHex", "#64748B")
                        )
                    )
                }
            }

            // 2. Zones
            val parsedZones = mutableListOf<ZoneEntity>()
            if (root.has("zones")) {
                val zoneArray = root.getJSONArray("zones")
                for (i in 0 until zoneArray.length()) {
                    val obj = zoneArray.getJSONObject(i)
                    val zoneName = obj.getString("name").trim()
                    if (zoneName.isNotBlank()) {
                        parsedZones.add(
                            ZoneEntity(
                                name = zoneName,
                                colorHex = obj.optString("colorHex", "#F59E0B")
                            )
                        )
                    }
                }
            }

            // 3. Boxes
            val parsedBoxes = mutableListOf<BoxEntity>()
            if (root.has("boxes")) {
                val boxArray = root.getJSONArray("boxes")
                for (i in 0 until boxArray.length()) {
                    onProgress?.invoke("Restaurando cajas y fotos (${i + 1}/${boxArray.length()})...")
                    val obj = boxArray.getJSONObject(i)
                    val id = obj.getString("id")
                    val parentId = if (obj.isNull("parentBoxId") || obj.optString("parentBoxId").isBlank()) {
                        null
                    } else {
                        obj.getString("parentBoxId")
                    }

                    // Extract image from Base64 if available
                    var restoredPhotoUri: String? = null
                    if (context != null && obj.has("photoBase64") && !obj.isNull("photoBase64")) {
                        val base64 = obj.getString("photoBase64")
                        val savedUri = decodeBase64ToInternalFile(context, base64, "box", id)
                        if (savedUri != null) {
                            restoredPhotoUri = savedUri
                            photosRestoredCount++
                        }
                    }

                    if (restoredPhotoUri == null && !obj.isNull("primaryPhotoUri")) {
                        val rawUri = obj.optString("primaryPhotoUri").takeIf { it.isNotBlank() }
                        // Check if file still exists locally or keep URI
                        restoredPhotoUri = rawUri
                    }

                    val zoneVal = if (obj.isNull("zone")) null else obj.optString("zone").takeIf { it.isNotBlank() }
                    if (zoneVal != null && parsedZones.none { it.name.equals(zoneVal, ignoreCase = true) }) {
                        parsedZones.add(ZoneEntity(name = zoneVal, colorHex = obj.optString("colorHex", "#F59E0B")))
                    }

                    parsedBoxes.add(
                        BoxEntity(
                            id = id,
                            name = obj.getString("name"),
                            description = obj.optString("description").takeIf { it.isNotBlank() },
                            parentBoxId = parentId,
                            colorHex = obj.optString("colorHex", "#F59E0B"),
                            primaryPhotoUri = restoredPhotoUri,
                            zone = zoneVal,
                            sortOrder = obj.optInt("sortOrder", 0),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
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

            // 5. Items
            val parsedItems = mutableListOf<ItemEntity>()
            if (root.has("items")) {
                val itemArray = root.getJSONArray("items")
                for (i in 0 until itemArray.length()) {
                    onProgress?.invoke("Restaurando objetos y fotos (${i + 1}/${itemArray.length()})...")
                    val obj = itemArray.getJSONObject(i)
                    val id = obj.getString("id")
                    val catId = if (obj.isNull("categoryId") || obj.optString("categoryId").isBlank()) null else obj.getString("categoryId")
                    val isLoaned = obj.optBoolean("isLoaned", false)
                    val loanedTo = if (obj.isNull("loanedTo")) null else obj.optString("loanedTo").takeIf { it.isNotBlank() }
                    val loanedAt = if (obj.isNull("loanedAt")) null else obj.optLong("loanedAt")
                    val notes = if (obj.isNull("notes")) null else obj.optString("notes").takeIf { it.isNotBlank() }

                    // Extract image from Base64 if available
                    var restoredPhotoUri: String? = null
                    if (context != null && obj.has("photoBase64") && !obj.isNull("photoBase64")) {
                        val base64 = obj.getString("photoBase64")
                        val savedUri = decodeBase64ToInternalFile(context, base64, "item", id)
                        if (savedUri != null) {
                            restoredPhotoUri = savedUri
                            photosRestoredCount++
                        }
                    }

                    if (restoredPhotoUri == null && !obj.isNull("primaryPhotoUri")) {
                        restoredPhotoUri = obj.optString("primaryPhotoUri").takeIf { it.isNotBlank() }
                    }

                    parsedItems.add(
                        ItemEntity(
                            id = id,
                            name = obj.getString("name"),
                            description = obj.optString("description").takeIf { it.isNotBlank() },
                            boxId = obj.getString("boxId"),
                            categoryId = catId,
                            primaryPhotoUri = restoredPhotoUri,
                            quantity = obj.optInt("quantity", 1).coerceAtLeast(1),
                            tags = obj.optString("tags", ""),
                            isLoaned = isLoaned,
                            loanedTo = loanedTo,
                            loanedAt = loanedAt,
                            notes = notes,
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            // 6. Transactional Room Database Restoration
            onProgress?.invoke("Guardando cambios en la base de datos...")
            database.withTransaction {
                if (parsedCategories.isNotEmpty()) {
                    categoryDao.insertCategories(parsedCategories)
                }
                if (parsedZones.isNotEmpty()) {
                    zoneDao.insertZones(parsedZones)
                }
                if (parsedBoxes.isNotEmpty()) {
                    boxDao.insertBoxes(parsedBoxes)
                }
                if (parsedItems.isNotEmpty()) {
                    itemDao.insertItems(parsedItems)
                }
            }

            val photoMsg = if (photosRestoredCount > 0) " con $photosRestoredCount fotos restauradas" else ""
            ImportResult(
                success = true,
                message = "Restauración exitosa: ${parsedBoxes.size} cajas y ${parsedItems.size} objetos recuperados$photoMsg.",
                boxesCount = parsedBoxes.size,
                itemsCount = parsedItems.size,
                photosRestoredCount = photosRestoredCount
            )
        } catch (e: Exception) {
            ImportResult(
                success = false,
                message = "Error al procesar el archivo: ${e.localizedMessage ?: "Estructura inválida o archivo corrupto"}"
            )
        }
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
