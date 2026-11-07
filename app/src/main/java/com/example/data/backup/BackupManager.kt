package com.example.data.backup

import com.example.data.local.AppDatabase
import com.example.data.local.entity.BoxEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.ItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ImportResult(
    val success: Boolean,
    val message: String,
    val boxesCount: Int = 0,
    val itemsCount: Int = 0
)

class BackupManager(private val database: AppDatabase) {

    private val boxDao = database.boxDao()
    private val itemDao = database.itemDao()
    private val categoryDao = database.categoryDao()

    suspend fun exportJson(): String = withContext(Dispatchers.IO) {
        val boxes = boxDao.getAllBoxesSync()
        val items = itemDao.getAllItemsSync()
        val categories = categoryDao.getAllCategoriesSync()

        val rootJson = JSONObject().apply {
            put("app", "Inventario de Cajas")
            put("version", 2)
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

            // Boxes
            val boxArray = JSONArray()
            boxes.forEach { box ->
                boxArray.put(JSONObject().apply {
                    put("id", box.id)
                    put("name", box.name)
                    put("description", box.description ?: "")
                    put("parentBoxId", box.parentBoxId ?: JSONObject.NULL)
                    put("colorHex", box.colorHex)
                    put("primaryPhotoUri", box.primaryPhotoUri ?: JSONObject.NULL)
                    put("zone", box.zone ?: JSONObject.NULL)
                    put("sortOrder", box.sortOrder)
                    put("createdAt", box.createdAt)
                })
            }
            put("boxes", boxArray)

            // Items
            val itemArray = JSONArray()
            items.forEach { item ->
                itemArray.put(JSONObject().apply {
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
                })
            }
            put("items", itemArray)
        }

        rootJson.toString(2)
    }

    suspend fun exportCsv(): String = withContext(Dispatchers.IO) {
        val boxes = boxDao.getAllBoxesSync().associateBy { it.id }
        val items = itemDao.getAllItemsSync()
        val categories = categoryDao.getAllCategoriesSync().associateBy { it.id }

        // Helper to resolve box breadcrumb path
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
        // CSV Header
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

    private fun escapeCsv(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    suspend fun importJson(jsonContent: String): ImportResult = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonContent)

            // Import categories
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

            // Import boxes
            val parsedBoxes = mutableListOf<BoxEntity>()
            if (root.has("boxes")) {
                val boxArray = root.getJSONArray("boxes")
                for (i in 0 until boxArray.length()) {
                    val obj = boxArray.getJSONObject(i)
                    val parentId = if (obj.isNull("parentBoxId") || obj.optString("parentBoxId").isBlank()) {
                        null
                    } else {
                        obj.getString("parentBoxId")
                    }
                    parsedBoxes.add(
                        BoxEntity(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            description = obj.optString("description").takeIf { it.isNotBlank() },
                            parentBoxId = parentId,
                            colorHex = obj.optString("colorHex", "#F59E0B"),
                            primaryPhotoUri = if (obj.isNull("primaryPhotoUri")) null else obj.optString("primaryPhotoUri").takeIf { it.isNotBlank() },
                            zone = if (obj.isNull("zone")) null else obj.optString("zone").takeIf { it.isNotBlank() },
                            sortOrder = obj.optInt("sortOrder", 0),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            // Verify cycle prevention in parsed boxes!
            val boxMap = parsedBoxes.associateBy { it.id }
            for (box in parsedBoxes) {
                var current: String? = box.parentBoxId
                val visited = mutableSetOf(box.id)
                while (current != null) {
                    if (visited.contains(current)) {
                        return@withContext ImportResult(
                            success = false,
                            message = "El archivo contiene una referencia circular en la caja: ${box.name}"
                        )
                    }
                    visited.add(current)
                    current = boxMap[current]?.parentBoxId
                }
            }

            // Import items
            val parsedItems = mutableListOf<ItemEntity>()
            if (root.has("items")) {
                val itemArray = root.getJSONArray("items")
                for (i in 0 until itemArray.length()) {
                    val obj = itemArray.getJSONObject(i)
                    val catId = if (obj.isNull("categoryId") || obj.optString("categoryId").isBlank()) null else obj.getString("categoryId")
                    val isLoaned = obj.optBoolean("isLoaned", false)
                    val loanedTo = if (obj.isNull("loanedTo")) null else obj.optString("loanedTo").takeIf { it.isNotBlank() }
                    val loanedAt = if (obj.isNull("loanedAt")) null else obj.optLong("loanedAt")
                    val notes = if (obj.isNull("notes")) null else obj.optString("notes").takeIf { it.isNotBlank() }

                    parsedItems.add(
                        ItemEntity(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            description = obj.optString("description").takeIf { it.isNotBlank() },
                            boxId = obj.getString("boxId"),
                            categoryId = catId,
                            primaryPhotoUri = if (obj.isNull("primaryPhotoUri")) null else obj.optString("primaryPhotoUri").takeIf { it.isNotBlank() },
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

            // Insert into Room
            if (parsedCategories.isNotEmpty()) {
                categoryDao.insertCategories(parsedCategories)
            }
            if (parsedBoxes.isNotEmpty()) {
                boxDao.insertBoxes(parsedBoxes)
            }
            if (parsedItems.isNotEmpty()) {
                itemDao.insertItems(parsedItems)
            }

            ImportResult(
                success = true,
                message = "Importación completada: ${parsedBoxes.size} cajas y ${parsedItems.size} objetos restaurados.",
                boxesCount = parsedBoxes.size,
                itemsCount = parsedItems.size
            )
        } catch (e: Exception) {
            ImportResult(
                success = false,
                message = "Error al procesar el archivo JSON: ${e.localizedMessage ?: "Formato inválido"}"
            )
        }
    }
}
