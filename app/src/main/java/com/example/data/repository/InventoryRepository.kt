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
import androidx.room.withTransaction
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

    companion object {
        const val DEFAULT_CATEGORY_ID = "cat_default_sin_categoria"
        const val DEFAULT_CATEGORY_NAME = "Sin categoría"
        const val DEFAULT_CATEGORY_COLOR = "#64748B"
        const val DEFAULT_CATEGORY_ICON = "category"
    }

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

    fun getAllItemsWithPhotos(): Flow<List<ItemEntity>> = itemDao.getAllItemsWithPhotos()

    fun getItemPhotosInBox(boxId: String): Flow<List<String>> = itemDao.getItemPhotosInBox(boxId)

    fun getAllCategories(): Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    suspend fun ensureDefaultCategoryExists(): CategoryEntity = withContext(Dispatchers.IO) {
        val existingById = categoryDao.getCategoryById(DEFAULT_CATEGORY_ID)
        if (existingById != null) return@withContext existingById

        val existingByName = categoryDao.getAllCategoriesSync()
            .find { it.name.equals(DEFAULT_CATEGORY_NAME, ignoreCase = true) }
        if (existingByName != null) return@withContext existingByName

        val defaultCat = CategoryEntity(
            id = DEFAULT_CATEGORY_ID,
            name = DEFAULT_CATEGORY_NAME,
            iconName = DEFAULT_CATEGORY_ICON,
            colorHex = DEFAULT_CATEGORY_COLOR
        )
        categoryDao.insertCategory(defaultCat)
        defaultCat
    }

    suspend fun createCategory(
        name: String,
        colorHex: String = "#F59E0B",
        iconName: String = "category"
    ): Result<CategoryEntity> = withContext(Dispatchers.IO) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("El nombre de la categoría no puede estar vacío"))
        }
        val existing = categoryDao.getAllCategoriesSync()
        if (existing.any { it.name.equals(cleanName, ignoreCase = true) }) {
            return@withContext Result.failure(IllegalArgumentException("Ya existe una categoría con el nombre «$cleanName»"))
        }

        val newCat = CategoryEntity(
            id = UUID.randomUUID().toString(),
            name = cleanName,
            iconName = iconName.ifBlank { "category" },
            colorHex = colorHex.ifBlank { "#F59E0B" }
        )
        categoryDao.insertCategory(newCat)
        Result.success(newCat)
    }

    suspend fun updateCategory(
        id: String,
        name: String,
        colorHex: String,
        iconName: String
    ): Result<CategoryEntity> = withContext(Dispatchers.IO) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("El nombre de la categoría no puede estar vacío"))
        }
        val current = categoryDao.getCategoryById(id)
            ?: return@withContext Result.failure(IllegalArgumentException("No se encontró la categoría seleccionada"))

        val existing = categoryDao.getAllCategoriesSync()
        if (existing.any { it.id != id && it.name.equals(cleanName, ignoreCase = true) }) {
            return@withContext Result.failure(IllegalArgumentException("Ya existe otra categoría con el nombre «$cleanName»"))
        }

        val updated = current.copy(
            name = cleanName,
            colorHex = colorHex.ifBlank { current.colorHex },
            iconName = iconName.ifBlank { current.iconName }
        )
        categoryDao.updateCategory(updated)
        Result.success(updated)
    }

    suspend fun deleteCategory(categoryId: String): Result<Int> = withContext(Dispatchers.IO) {
        if (categoryId == DEFAULT_CATEGORY_ID) {
            return@withContext Result.failure(IllegalArgumentException("No se puede eliminar la categoría de resguardo «Sin categoría»"))
        }
        val current = categoryDao.getCategoryById(categoryId)
            ?: return@withContext Result.failure(IllegalArgumentException("Categoría no encontrada"))

        if (current.name.equals(DEFAULT_CATEGORY_NAME, ignoreCase = true)) {
            return@withContext Result.failure(IllegalArgumentException("No se puede eliminar la categoría de resguardo «Sin categoría»"))
        }

        database.withTransaction {
            val defaultCat = ensureDefaultCategoryExists()
            val affectedCount = itemDao.getItemsCountByCategoryId(categoryId)
            itemDao.reassignCategory(categoryId, defaultCat.id)
            categoryDao.deleteCategoryById(categoryId)
            Result.success(affectedCount)
        }
    }

    suspend fun getCategoryItemCount(categoryId: String): Int = withContext(Dispatchers.IO) {
        itemDao.getItemsCountByCategoryId(categoryId)
    }

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

    suspend fun importCsv(
        content: String,
        onProgress: ((String) -> Unit)? = null
    ): ImportResult = backupManager.importCsv(content, onProgress)

    // Initial seeding
    suspend fun seedInitialDataIfNeeded() = withContext(Dispatchers.IO) {
        val existingBoxes = boxDao.getAllBoxesSync()
        if (existingBoxes.isNotEmpty()) return@withContext

        // Default categories
        val catDefault = CategoryEntity(DEFAULT_CATEGORY_ID, DEFAULT_CATEGORY_NAME, DEFAULT_CATEGORY_ICON, DEFAULT_CATEGORY_COLOR)
        val catElec = CategoryEntity("cat_elec", "Electrónica", "memory", "#3B82F6")
        val catHerr = CategoryEntity("cat_herr", "Herramientas", "build", "#F59E0B")
        val catRopa = CategoryEntity("cat_ropa", "Ropa", "checkroom", "#EC4899")
        val catDocs = CategoryEntity("cat_docs", "Documentos", "description", "#10B981")
        val catHogar = CategoryEntity("cat_hogar", "Hogar", "home", "#8B5CF6")
        val catCables = CategoryEntity("cat_cables", "Cables y Conexiones", "cable", "#06B6D4")

        categoryDao.insertCategories(listOf(catDefault, catElec, catHerr, catRopa, catDocs, catHogar, catCables))

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
            primaryPhotoUri = "https://lh3.googleusercontent.com/aida-public/AB6AXuCAew30R10pnSi74P-3gS6FwxtvW-QjepoKz-jmgBPY7DEP6iLOcG22hxX-a7ucTOuIJRk8UcE3AlZn5inE-aj_ZNiwctuFLmLHLZ-Ww0Lb_ftR7vjeOR3nOu9rGWaAhD5KP3vsvVpsFAxjlKl4rW74diWttEVxo644O_BGFDMSIoaThon86W0Nt2NGary8tJDNlD-6vLsjqYp725yQ1MZpgVeW5SWHPVjIMPIzrU_y",
            zone = "Taller"
        )
        val boxCables = BoxEntity(
            id = "box_cables",
            name = "Periféricos & Audio",
            description = "HDMI, USB-C, adaptadores y regletas",
            parentBoxId = "box_trastero",
            colorHex = "#3B82F6",
            primaryPhotoUri = "https://lh3.googleusercontent.com/aida-public/AB6AXuBUj9geDQBhgmf756qfUrtwUrHfkAMA0Y4KzQpon1nLUhaR-0ZzXAthPQjmJif3yaLxfa7jhI6-ImK1pLEh4ipmJc358uK3Pg_4J6eO9Ts_zdp-lmqu6yM_k0-x6tqHsXahTQ59wE2op6s0YNFVpKjlpzuXEaOFxC0qdUt11OCFCPeiAETg2FixYBL2i_AsR61Zo1nw_JwKiNCVZW37Fj7ScbegI-pTw9NuWZxH_e3r",
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
                primaryPhotoUri = "https://lh3.googleusercontent.com/aida-public/AB6AXuDwk-ILGGFWq3RT5sQFnxGlTmu3h3-Jc4RrZ5EHsKkbtSxfHr8sQvkSTtikkvmMKrHUXRFIbWrWM5Tb2Ip6Eb99QMfSZlTTyAJB_Wo6roNl7UPlDxPMqXXx6Sxm-uWtL73MCCouFWRCu2Dp6sRDSaHzYPzrGWU9ItCVU8LxU5tXeaDNcDOg36H_0jBBPiuVzamwVUYRe9_Wse0IaRDbfoUpccEJ8VvUPui2ZghaBlb_",
                quantity = 1,
                tags = "Bricolaje, Reparación"
            ),
            ItemEntity(
                id = UUID.randomUUID().toString(),
                name = "Taladro percutor 18V",
                description = "Con 2 baterías y cargador rápido",
                boxId = "box_herramientas",
                categoryId = "cat_herr",
                primaryPhotoUri = "https://lh3.googleusercontent.com/aida-public/AB6AXuD7_R_cqUvDyQULOuhwA-_1iZ3Qg4CUoIJyo3a75MYSw3S9qE8mDCEcToxW7FrtJAFsR70CC_z4kKcLHosxvOrQSoNk5Xj_ZZoGI-XfS13C2RGCPkBsoEGfHgZLm6T8wxpB7ladlvEixFzqvzrwxx-bKHpBeYSsegtZ-T2oYVMNUu9QWXyjAUnEKU67Q3gyEJe9igzp8hShkpkHJiAp1rA5E_m-L-HZVLFJGHDvnFpF",
                quantity = 1,
                tags = "Eléctrico, Taller",
                isLoaned = true,
                loanedTo = "Carlos (Vecino)",
                loanedAt = System.currentTimeMillis() - 86400000L * 2,
                notes = "Prometió devolverlo este viernes"
            ),
            ItemEntity(
                id = UUID.randomUUID().toString(),
                name = "Juego de llaves fijas",
                description = "Medidas métricas 6-22 mm",
                boxId = "box_herramientas",
                categoryId = "cat_herr",
                primaryPhotoUri = "https://lh3.googleusercontent.com/aida-public/AB6AXuBagPPcK6hdlm9dcF3-HJVYgmmT6FSTL8cDb0XLafqQSb0Qo0B_pvGJv0KKk22x6OcWP0AGVw7bIYHduSm2B9iA0gPGXqX0W3hWEkGWaMJck8o9GorUAxkrtn_7ZxoQEX5daNJmauHnnhBz1X0u_6gizq7SFUtU3VHzBG3Fz_J9X-SsIDTLBHQw8nXhzoaaVrn2hR8SvZ1g__3xbyI207CnP9Oy8SyKRobiRzuxJxFX",
                quantity = 1,
                tags = "Herramientas"
            ),
            ItemEntity(
                id = UUID.randomUUID().toString(),
                name = "Metro láser digital",
                description = "Alcance 50m con Bluetooth",
                boxId = "box_herramientas",
                categoryId = "cat_herr",
                primaryPhotoUri = "https://lh3.googleusercontent.com/aida-public/AB6AXuAL7Mcvd2YzqEMtQnhQ3HdekW2O6fW_gVf-k-bjcFRTbAlXkr4SIiNiw0MWO3_5pRPYGO9BcG7mf4hPqN0tHFzsxELgR9K3vvn7NNigj1VACZUKgwRiqwylcAamoMTDkk7WSx98C4wtvm0ZSGnJ2bWpKrGNoabbRozOU6g6SyNcEJeiW0nSYQyxSPwh_3nW28H-xTpB-y963XWPwy4FUJwoc0GlQXy_W_dP4eaVlhI0",
                quantity = 1,
                tags = "Medición"
            ),
            ItemEntity(
                id = UUID.randomUUID().toString(),
                name = "Soldador estaño 60W",
                description = "Temperatura regulable con soporte",
                boxId = "box_herramientas",
                categoryId = "cat_herr",
                primaryPhotoUri = "https://lh3.googleusercontent.com/aida-public/AB6AXuA4Cc6nV-Wjrrg-O9Zq-G0J-Vkfz2Y2yn2dVC3pKE9kSeUHx9NPsbGsGIgxStAKxj0iRLW7vBLQX9_kVSEYeVsSbh1kQ5vwsSM53XH_DeBn6BAEaT6gVRyHkT7jgUzDMRFFWENtj5urshpaMVH_JYGGCiBuVJTi8nXuQo1rt4_MDQ61Gkn1UEv7XFiZR8aZ9mGMSVEBH3g0yxnNO3tem01ffTNhSyrG3fSWLSca3WWE",
                quantity = 1,
                tags = "Electrónica"
            ),
            ItemEntity(
                id = UUID.randomUUID().toString(),
                name = "Cable HDMI 2.1 4K 120Hz (2 metros)",
                description = "Trenzado de nylon ultra resistente",
                boxId = "box_cables",
                categoryId = "cat_cables",
                primaryPhotoUri = "https://lh3.googleusercontent.com/aida-public/AB6AXuC5c_eotg_L7-SgM5x8QHAoGRvckDezg3GNUL-IdDcSzxT5T1GJDje_bzcQBzhKQs6VS8DYor4R1A-KzUy2V5ckrEJkj8GuqOeYD5ETAIzejOOVPSDbikFD9SDoYBhziHdHskHpO0xm0sagCbaeUUW2-ip1pxDesTui6-mvhYNyovqcw7xOdzlC7OdT2o3FW3C86r2c7s2xQGaoMpznpUwhR1tXIlCIotT4b6MANPmF",
                quantity = 3,
                tags = "Monitor, TV, Video"
            ),
            ItemEntity(
                id = UUID.randomUUID().toString(),
                name = "Switches Mecánicos y Teclado",
                description = "Switches lubricados y keycaps PBT",
                boxId = "box_cables",
                categoryId = "cat_elec",
                primaryPhotoUri = "https://lh3.googleusercontent.com/aida-public/AB6AXuAnHsmtGkAHoP61NzA5uVby1Y5fKg2Dbs1ZdgQw2LsX_TU5xh1s9JNrgAjMWrJKnMeGDGceyT25TGOw4XzM7t9bIA4cwKT9WgOUtQfiajY9JipJ5jHcd_TivSZ_Lp1XK79cOHCycp0Xp_tiPYqMY0ullnetyP547Y2h9qjPRNN1rcXLLNCpsv8MG5qY-jsOXWVXwFy3JpFRetvPM4IVy1q6nz6XuBNu3bUzqGf5a6PX",
                quantity = 1,
                tags = "Teclado, PC"
            ),
            ItemEntity(
                id = UUID.randomUUID().toString(),
                name = "Pinceles & Rotuladores Caligrafía",
                description = "Pelo sintético de precisión",
                boxId = "box_cables",
                categoryId = "cat_elec",
                primaryPhotoUri = "https://lh3.googleusercontent.com/aida-public/AB6AXuCR34FFaXdllXOspcft5Lsc1Y_AACpTlvLROB9FHKJxt9UqG7w_Axy7XKp3HQUCMoJpbcDQYHRZjRzrZ6rPi-4PltN2FQVpsz0yOTw0lLIWcHR6AC3doY0rnvYyr4LLpY0CcFnSb286KpDFC8_faHUTPfPuiqgWwHhCSo62yNDluxyYEQMPBsO_CPdkQjyLz4ArenYuevyOWA_eI7Gd2l1aJVeN2YqRF0-3tFK8Ne2z",
                quantity = 4,
                tags = "Arte"
            ),
            ItemEntity(
                id = UUID.randomUUID().toString(),
                name = "Paleta Mezclas Cerámica",
                description = "Superficie blanca esmaltada",
                boxId = "box_cables",
                categoryId = "cat_elec",
                primaryPhotoUri = "https://lh3.googleusercontent.com/aida-public/AB6AXuCcu-_HZ6sZHG6IKCBiKXNo2JZNNR0XjicyoF4SoEXLvIke9G_OI5OYV2FmrVmvAgZ2UEwJsJ7GsYkGc0viayBqjZA308XW2y3QWJTIPSoJFzI95SVsCAE7URWI4MWRBN3u-Q22P1WwCkXjX8VITnlLs8DTtvhREoJMg46PMa5NQZbGrrya3y4y8DWB1IoMr1FteX3Djy6JKrzDHBf7VD4aTMKp3nbhTbaVp6a0HbM9",
                quantity = 1,
                tags = "Arte"
            ),
            ItemEntity(
                id = UUID.randomUUID().toString(),
                name = "Cuadernos Acuarela Grano Fino",
                description = "Papel 300g 100% algodón",
                boxId = "box_cables",
                categoryId = "cat_elec",
                primaryPhotoUri = "https://lh3.googleusercontent.com/aida-public/AB6AXuBvNIZTdt6d1k4sFVLbeJwB4TxDvZkY3Rl4NHQADwU0ScvcB-U_EcDalFrtijg3DmQFXf7qhTVqMFZsrSbVAjse4zuCUu9CWPF-QqVa4itniDDKIHmuNvamF9m-Kv_dUNIhdFeoVPx8SfTXezDIJVAQWMHI0YaRoUnVmpukF2YKeCpAhxydjqb-5xS7_BLjNEZ2CilmdnLG8Kha5jr_4pJo6EsAcaYnqc78j8FGcRfb",
                quantity = 2,
                tags = "Arte"
            ),
            ItemEntity(
                id = UUID.randomUUID().toString(),
                name = "Organizador Cables Velcro",
                description = "Bobina 5 metros reutilizable",
                boxId = "box_cables",
                categoryId = "cat_cables",
                primaryPhotoUri = "https://lh3.googleusercontent.com/aida-public/AB6AXuDwk-ILGGFWq3RT5sQFnxGlTmu3h3-Jc4RrZ5EHsKkbtSxfHr8sQvkSTtikkvmMKrHUXRFIbWrWM5Tb2Ip6Eb99QMfSZlTTyAJB_Wo6roNl7UPlDxPMqXXx6Sxm-uWtL73MCCouFWRCu2Dp6sRDSaHzYPzrGWU9ItCVU8LxU5tXeaDNcDOg36H_0jBBPiuVzamwVUYRe9_Wse0IaRDbfoUpccEJ8VvUPui2ZghaBlb_",
                quantity = 1,
                tags = "Organización"
            ),
            ItemEntity(
                id = UUID.randomUUID().toString(),
                name = "Adaptador USB-C a Ethernet Gigabit",
                description = "Aluminio gris espacial",
                boxId = "box_cables",
                categoryId = "cat_elec",
                primaryPhotoUri = "https://lh3.googleusercontent.com/aida-public/AB6AXuCxwIWmOX9ha0pNj9uGYeX2-Tl8Ai02PTQFpikmNWaEmRWmiWEOqdxaKKB9Y5I_aApeQLHL3pjpITug21aDRPCP3UDpoffUg4G0NeVYV8vY00u9_918gTjibDzVa5Ocve7X5ixuOA56Tl4beMy47bk11k-dhIoSGv1W_4q-NE5JK-iSwSW9bqRVPqOJj3qMzz62ZSlKYC42g6ReIF8Lr-0KTBs_QvKx_OSiwXNe5m78",
                quantity = 2,
                tags = "Red, Portátil"
            ),
            ItemEntity(
                id = UUID.randomUUID().toString(),
                name = "Escrituras y contratos de alquiler",
                description = "Sobre ignífugo etiquetado 'Casa'",
                boxId = "box_archivo",
                categoryId = "cat_docs",
                primaryPhotoUri = "https://lh3.googleusercontent.com/aida-public/AB6AXuBUj9geDQBhgmf756qfUrtwUrHfkAMA0Y4KzQpon1nLUhaR-0ZzXAthPQjmJif3yaLxfa7jhI6-ImK1pLEh4ipmJc358uK3Pg_4J6eO9Ts_zdp-lmqu6yM_k0-x6tqHsXahTQ59wE2op6s0YNFVpKjlpzuXEaOFxC0qdUt11OCFCPeiAETg2FixYBL2i_AsR61Zo1nw_JwKiNCVZW37Fj7ScbegI-pTw9NuWZxH_e3r",
                quantity = 1,
                tags = "Importante, Legal"
            ),
            ItemEntity(
                id = UUID.randomUUID().toString(),
                name = "Certificados de garantía electrodomésticos",
                description = "Carpeta azul de anillas",
                boxId = "box_archivo",
                categoryId = "cat_docs",
                primaryPhotoUri = "https://lh3.googleusercontent.com/aida-public/AB6AXuCAew30R10pnSi74P-3gS6FwxtvW-QjepoKz-jmgBPY7DEP6iLOcG22hxX-a7ucTOuIJRk8UcE3AlZn5inE-aj_ZNiwctuFLmLHLZ-Ww0Lb_ftR7vjeOR3nOu9rGWaAhD5KP3vsvVpsFAxjlKl4rW74diWttEVxo644O_BGFDMSIoaThon86W0Nt2NGary8tJDNlD-6vLsjqYp725yQ1MZpgVeW5SWHPVjIMPIzrU_y",
                quantity = 1,
                tags = "Garantías, Hogar"
            ),
            ItemEntity(
                id = UUID.randomUUID().toString(),
                name = "Abrigo térmico de plumas",
                description = "Talla L, impermeable con capucha",
                boxId = "box_ropa",
                categoryId = "cat_ropa",
                primaryPhotoUri = "https://lh3.googleusercontent.com/aida-public/AB6AXuCR34FFaXdllXOspcft5Lsc1Y_AACpTlvLROB9FHKJxt9UqG7w_Axy7XKp3HQUCMoJpbcDQYHRZjRzrZ6rPi-4PltN2FQVpsz0yOTw0lLIWcHR6AC3doY0rnvYyr4LLpY0CcFnSb286KpDFC8_faHUTPfPuiqgWwHhCSo62yNDluxyYEQMPBsO_CPdkQjyLz4ArenYuevyOWA_eI7Gd2l1aJVeN2YqRF0-3tFK8Ne2z",
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
