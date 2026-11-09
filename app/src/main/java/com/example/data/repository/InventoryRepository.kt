package com.example.data.repository

import android.content.Context
import com.example.data.backup.BackupManager
import com.example.data.backup.ImportResult
import java.io.InputStream
import java.io.OutputStream
import com.example.data.local.AppDatabase
import com.example.data.local.dao.BoxWithCounts
import com.example.data.local.entity.BoxEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.ItemEntity
import com.example.data.local.entity.SearchHistoryEntity
import com.example.data.local.entity.ZoneEntity
import com.example.data.local.relation.BoxHierarchy
import com.example.data.local.relation.BoxWithChildren
import com.example.data.local.relation.BoxWithItems
import com.example.data.local.relation.ItemWithBox
import com.example.domain.usecase.MoveBoxResult
import com.example.domain.usecase.MoveBoxUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

data class CategoryStat(
    val category: CategoryEntity,
    val itemsCount: Int,
    val unitsCount: Int,
    val percentage: Float
)

class InventoryRepository(private val database: AppDatabase) {

    private val boxDao = database.boxDao()
    private val itemDao = database.itemDao()
    private val categoryDao = database.categoryDao()
    private val zoneDao = database.zoneDao()
    private val searchHistoryDao = database.searchHistoryDao()
    private val moveBoxUseCase = MoveBoxUseCase(boxDao)
    private val backupManager = BackupManager(database)
    private val pdfExportHelper = com.example.data.backup.PdfExportHelper(database)

    // Flow queries
    fun getRootBoxesWithCounts(): Flow<List<BoxWithCounts>> = boxDao.getRootBoxesWithCounts()

    fun getChildBoxesWithCounts(parentId: String): Flow<List<BoxWithCounts>> =
        boxDao.getChildBoxesWithCounts(parentId)

    fun getBoxHierarchy(boxId: String): Flow<BoxHierarchy?> = boxDao.getBoxHierarchy(boxId)

    fun getBoxWithItems(boxId: String): Flow<BoxWithItems?> = boxDao.getBoxWithItems(boxId)

    fun getBoxWithChildren(boxId: String): Flow<BoxWithChildren?> = boxDao.getBoxWithChildren(boxId)

    fun getRootBoxesHierarchy(): Flow<List<BoxHierarchy>> = boxDao.getRootBoxesHierarchy()

    fun getItemWithBox(itemId: String): Flow<ItemWithBox?> = itemDao.getItemWithBox(itemId)

    fun getAllItemsWithBox(): Flow<List<ItemWithBox>> = itemDao.getAllItemsWithBox()

    fun getBoxFlowById(id: String): Flow<BoxEntity?> = boxDao.getBoxFlowById(id)

    suspend fun getBoxById(id: String): BoxEntity? = boxDao.getBoxById(id)

    fun getAllBoxes(): Flow<List<BoxEntity>> = boxDao.getAllBoxes()

    suspend fun getAllBoxesSync(): List<BoxEntity> = boxDao.getAllBoxesSync()

    fun getItemsInBox(boxId: String): Flow<List<ItemEntity>> = itemDao.getItemsInBox(boxId)

    fun getAllCategories(): Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    fun getLoanedItems(): Flow<List<ItemEntity>> = itemDao.getLoanedItems()

    fun getTotalBoxesCount(): Flow<Int> = boxDao.getTotalBoxesCount()

    fun getTotalItemsCount(): Flow<Int> = itemDao.getTotalItemsCount()

    fun getTotalItemUnitsCount(): Flow<Int?> = itemDao.getTotalItemUnitsCount()

    fun getTotalLoanedCount(): Flow<Int> = itemDao.getTotalLoanedCount()

    fun searchItems(query: String, categoryId: String? = null): Flow<List<ItemEntity>> =
        itemDao.searchItems(query, categoryId)

    fun searchBoxes(query: String): Flow<List<BoxEntity>> = boxDao.searchBoxes(query)

    // Search History
    fun getRecentSearches(limit: Int = 15): Flow<List<SearchHistoryEntity>> =
        searchHistoryDao.getRecentSearches(limit)

    suspend fun recordSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isNotBlank()) {
            withContext(Dispatchers.IO) {
                searchHistoryDao.insertSearch(
                    SearchHistoryEntity(query = trimmed, timestamp = System.currentTimeMillis())
                )
            }
        }
    }

    suspend fun deleteSearch(query: String) {
        withContext(Dispatchers.IO) {
            searchHistoryDao.deleteSearch(query)
        }
    }

    suspend fun clearSearchHistory() {
        withContext(Dispatchers.IO) {
            searchHistoryDao.clearAllHistory()
        }
    }

    // Breadcrumb calculation
    suspend fun getBreadcrumbs(boxId: String): List<BoxEntity> = withContext(Dispatchers.IO) {
        val allBoxes = boxDao.getAllBoxesSync().associateBy { it.id }
        val breadcrumbs = mutableListOf<BoxEntity>()
        var current: BoxEntity? = allBoxes[boxId]

        while (current != null) {
            breadcrumbs.add(0, current)
            current = current.parentBoxId?.let { allBoxes[it] }
        }
        breadcrumbs
    }

    // Category Distribution Stats
    suspend fun getCategoryStats(): List<CategoryStat> = withContext(Dispatchers.IO) {
        val items = itemDao.getAllItemsSync()
        val categories = categoryDao.getAllCategoriesSync()
        val totalUnits = items.sumOf { it.quantity }.coerceAtLeast(1)

        val itemsByCat = items.groupBy { it.categoryId }

        val stats = mutableListOf<CategoryStat>()
        for (cat in categories) {
            val catItems = itemsByCat[cat.id] ?: emptyList()
            val units = catItems.sumOf { it.quantity }
            val count = catItems.size
            if (count > 0) {
                stats.add(
                    CategoryStat(
                        category = cat,
                        itemsCount = count,
                        unitsCount = units,
                        percentage = (units.toFloat() / totalUnits) * 100f
                    )
                )
            }
        }
        stats.sortedByDescending { it.unitsCount }
    }

    fun getAllZones(): Flow<List<String>> = boxDao.getAllZones()
    fun getAllZoneEntities(): Flow<List<ZoneEntity>> = zoneDao.getAllZones()

    suspend fun createZone(name: String, colorHex: String = "#F59E0B") = withContext(Dispatchers.IO) {
        val trimmed = name.trim()
        if (trimmed.isNotBlank()) {
            zoneDao.insertZone(ZoneEntity(name = trimmed, colorHex = colorHex))
        }
    }

    suspend fun renameZone(oldName: String, newName: String) = withContext(Dispatchers.IO) {
        val trimmed = newName.trim()
        if (trimmed.isNotBlank() && trimmed != oldName) {
            zoneDao.renameZone(oldName, trimmed)
            boxDao.renameZoneInBoxes(oldName, trimmed)
        }
    }

    suspend fun deleteZone(name: String) = withContext(Dispatchers.IO) {
        zoneDao.deleteZone(name)
        boxDao.clearZoneInBoxes(name)
    }

    // CRUD Box
    suspend fun createBox(
        name: String,
        description: String?,
        parentBoxId: String?,
        primaryPhotoUri: String?,
        colorHex: String,
        zone: String? = null
    ): String = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val trimmedZone = zone?.takeIf { it.isNotBlank() }?.trim()
        val box = BoxEntity(
            id = id,
            name = name.trim(),
            description = description?.takeIf { it.isNotBlank() }?.trim(),
            parentBoxId = parentBoxId,
            primaryPhotoUri = primaryPhotoUri,
            colorHex = colorHex,
            zone = trimmedZone
        )
        boxDao.insertBox(box)
        trimmedZone?.let { z ->
            zoneDao.insertZone(ZoneEntity(name = z, colorHex = colorHex))
        }
        id
    }

    suspend fun updateBox(box: BoxEntity) = withContext(Dispatchers.IO) {
        boxDao.updateBox(box)
        box.zone?.takeIf { it.isNotBlank() }?.trim()?.let { z ->
            zoneDao.insertZone(ZoneEntity(name = z, colorHex = box.colorHex))
        }
    }

    suspend fun deleteBox(boxId: String) = withContext(Dispatchers.IO) {
        boxDao.deleteBoxById(boxId)
    }

    suspend fun moveBox(boxId: String, targetParentBoxId: String?): MoveBoxResult =
        withContext(Dispatchers.IO) {
            moveBoxUseCase.execute(boxId, targetParentBoxId)
        }

    fun canMoveBox(boxId: String, targetParentBoxId: String?, allBoxes: List<BoxEntity>): Pair<Boolean, String?> =
        moveBoxUseCase.isValidMove(boxId, targetParentBoxId, allBoxes)

    // CRUD Item
    suspend fun createItem(
        name: String,
        description: String?,
        boxId: String,
        categoryId: String?,
        primaryPhotoUri: String?,
        quantity: Int,
        tags: String
    ): String = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val item = ItemEntity(
            id = id,
            name = name.trim(),
            description = description?.takeIf { it.isNotBlank() }?.trim(),
            boxId = boxId,
            categoryId = categoryId,
            primaryPhotoUri = primaryPhotoUri,
            quantity = quantity.coerceAtLeast(1),
            tags = tags.trim()
        )
        itemDao.insertItem(item)
        id
    }

    suspend fun updateItem(item: ItemEntity) = withContext(Dispatchers.IO) {
        itemDao.updateItem(item)
    }

    suspend fun deleteItem(itemId: String) = withContext(Dispatchers.IO) {
        itemDao.deleteItemById(itemId)
    }

    suspend fun updateItemQuantity(itemId: String, quantity: Int) = withContext(Dispatchers.IO) {
        if (quantity <= 0) {
            itemDao.deleteItemById(itemId)
        } else {
            itemDao.updateQuantity(itemId, quantity)
        }
    }

    suspend fun moveItem(itemId: String, targetBoxId: String) = withContext(Dispatchers.IO) {
        itemDao.moveItem(itemId, targetBoxId)
    }

    // Loans
    suspend fun loanItem(itemId: String, loanedTo: String, notes: String?) = withContext(Dispatchers.IO) {
        itemDao.loanItem(
            itemId = itemId,
            loanedTo = loanedTo.trim(),
            loanedAt = System.currentTimeMillis(),
            notes = notes?.trim()
        )
    }

    suspend fun returnLoanedItem(itemId: String) = withContext(Dispatchers.IO) {
        itemDao.returnLoanedItem(itemId)
    }

    // Backup & Export
    suspend fun exportJson(context: Context? = null, onProgress: ((String) -> Unit)? = null): String =
        backupManager.exportJson(context, onProgress)

    suspend fun exportJsonToStream(
        context: Context,
        outputStream: OutputStream,
        onProgress: ((String) -> Unit)? = null
    ) = backupManager.exportJsonToStream(context, outputStream, onProgress)

    suspend fun exportCsv(): String = backupManager.exportCsv()

    suspend fun exportCsvToStream(outputStream: OutputStream) =
        backupManager.exportCsvToStream(outputStream)

    suspend fun exportPdfToStream(outputStream: OutputStream) =
        pdfExportHelper.generatePdf(outputStream)

    suspend fun importJson(
        content: String,
        context: Context? = null,
        onProgress: ((String) -> Unit)? = null
    ): ImportResult = backupManager.importJson(content, context, onProgress)

    suspend fun importJsonFromStream(
        context: Context,
        inputStream: InputStream,
        onProgress: ((String) -> Unit)? = null
    ): ImportResult = backupManager.importJsonFromStream(context, inputStream, onProgress)

    // Initial seeding
    suspend fun seedInitialDataIfNeeded() = withContext(Dispatchers.IO) {
        val existingBoxes = boxDao.getAllBoxesSync()
        if (existingBoxes.isNotEmpty()) return@withContext

        // Default categories
        val catElec = CategoryEntity("cat_elec", "Electrónica", "memory", "#3B82F6")
        val catHerr = CategoryEntity("cat_herr", "Herramientas", "build", "#F59E0B")
        val catRopa = CategoryEntity("cat_ropa", "Ropa", "checkroom", "#EC4899")
        val catDocs = CategoryEntity("cat_docs", "Documentos", "description", "#10B981")
        val catHogar = CategoryEntity("cat_hogar", "Hogar", "home", "#8B5CF6")
        val catCables = CategoryEntity("cat_cables", "Cables y Conexiones", "cable", "#06B6D4")

        categoryDao.insertCategories(listOf(catElec, catHerr, catRopa, catDocs, catHogar, catCables))

        val defaultZones = listOf(
            ZoneEntity("Trastero", "#F59E0B"),
            ZoneEntity("Taller", "#EF4444"),
            ZoneEntity("Estudio", "#8B5CF6"),
            ZoneEntity("Dormitorio", "#EC4899")
        )
        zoneDao.insertZones(defaultZones)

        val boxTrastero = BoxEntity(
            id = "box_trastero",
            name = "Trastero Principal",
            description = "Estantería metálica del fondo, balda 2",
            parentBoxId = null,
            colorHex = "#F59E0B",
            zone = "Trastero"
        )
        val boxHerramientas = BoxEntity(
            id = "box_herramientas",
            name = "Herramientas Taller",
            description = "Maletín de llaves, tornillería y taladro",
            parentBoxId = "box_trastero",
            colorHex = "#EF4444",
            zone = "Taller"
        )
        val boxCables = BoxEntity(
            id = "box_cables",
            name = "Cables & Conexiones",
            description = "HDMI, USB-C, adaptadores y regletas",
            parentBoxId = "box_trastero",
            colorHex = "#3B82F6",
            zone = "Taller"
        )
        val boxArmario = BoxEntity(
            id = "box_armario",
            name = "Armario Estudio",
            description = "Cajonera blanca junto al escritorio",
            parentBoxId = null,
            colorHex = "#8B5CF6",
            zone = "Estudio"
        )
        val boxArchivo = BoxEntity(
            id = "box_archivo",
            name = "Archivo Fiscal & Facturas",
            description = "Carpetas 2024-2026 ordenadas alfabéticamente",
            parentBoxId = "box_armario",
            colorHex = "#10B981",
            zone = "Estudio"
        )
        val boxRopa = BoxEntity(
            id = "box_ropa",
            name = "Ropa Temporada Invierno",
            description = "Abrigos de montaña, guantes y bufandas al vacío",
            parentBoxId = null,
            colorHex = "#EC4899",
            zone = "Dormitorio"
        )

        boxDao.insertBoxes(listOf(boxTrastero, boxHerramientas, boxCables, boxArmario, boxArchivo, boxRopa))

        val items = listOf(
            ItemEntity(
                id = UUID.randomUUID().toString(),
                name = "Juego destornilladores precisión",
                description = "Puntas Torx, Phillips y planas en estuche negro",
                boxId = "box_herramientas",
                categoryId = "cat_herr",
                quantity = 1,
                tags = "Bricolaje, Reparación"
            ),
            ItemEntity(
                id = UUID.randomUUID().toString(),
                name = "Taladro percutor 18V",
                description = "Con 2 baterías y cargador rápido",
                boxId = "box_herramientas",
                categoryId = "cat_herr",
                quantity = 1,
                tags = "Eléctrico, Taller",
                isLoaned = true,
                loanedTo = "Carlos (Vecino)",
                loanedAt = System.currentTimeMillis() - 86400000L * 2,
                notes = "Prometió devolverlo este viernes"
            ),
            ItemEntity(
                id = UUID.randomUUID().toString(),
                name = "Cable HDMI 2.1 4K 120Hz (2 metros)",
                description = "Trenzado de nylon ultra resistente",
                boxId = "box_cables",
                categoryId = "cat_cables",
                quantity = 3,
                tags = "Monitor, TV, Video"
            ),
            ItemEntity(
                id = UUID.randomUUID().toString(),
                name = "Adaptador USB-C a Ethernet Gigabit",
                description = "Aluminio gris espacial",
                boxId = "box_cables",
                categoryId = "cat_elec",
                quantity = 2,
                tags = "Red, Portátil"
            ),
            ItemEntity(
                id = UUID.randomUUID().toString(),
                name = "Escrituras y contratos de alquiler",
                description = "Sobre ignífugo etiquetado 'Casa'",
                boxId = "box_archivo",
                categoryId = "cat_docs",
                quantity = 1,
                tags = "Importante, Legal"
            ),
            ItemEntity(
                id = UUID.randomUUID().toString(),
                name = "Abrigo térmico de plumas",
                description = "Talla L, impermeable con capucha",
                boxId = "box_ropa",
                categoryId = "cat_ropa",
                quantity = 1,
                tags = "Montaña, Nieve"
            ),
            ItemEntity(
                id = UUID.randomUUID().toString(),
                name = "Juego de sábanas térmicas de franela",
                description = "Para cama de 150 cm",
                boxId = "box_ropa",
                categoryId = "cat_hogar",
                quantity = 2,
                tags = "Invierno, Cama"
            )
        )

        itemDao.insertItems(items)
    }
}
