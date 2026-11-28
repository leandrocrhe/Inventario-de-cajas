package com.example

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.converter.Converters
import com.example.data.local.entity.BoxEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.ItemEntity
import com.example.data.local.entity.ZoneEntity
import com.example.data.local.relation.BoxHierarchy
import com.example.data.local.relation.ItemWithBox
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlinx.coroutines.flow.first
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun readStringFromContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Inventario de Cajas", appName)
    }

    @Test
    fun testRoomTypeConverters() {
        val converters = Converters()

        // 1. Date <-> Timestamp converter
        val now = System.currentTimeMillis()
        val date = Date(now)
        val timestamp = converters.dateToTimestamp(date)
        assertEquals(now, timestamp)
        val backToDate = converters.fromTimestamp(timestamp)
        assertEquals(date, backToDate)
        assertNull(converters.dateToTimestamp(null))
        assertNull(converters.fromTimestamp(null))

        // 2. List<String> <-> JSON String converter
        val tags = listOf("Herramientas", "Bricolaje", "Taller")
        val jsonStr = converters.fromStringList(tags)
        assertNotNull(jsonStr)
        val backToTags = converters.toStringList(jsonStr)
        assertEquals(tags, backToTags)
        assertNull(converters.fromStringList(null))
        assertNull(converters.toStringList(null))

        // 3. Uri <-> String converter
        val photoUri = Uri.parse("file:///data/user/0/com.example/cache/camera_photos/test_box.jpg")
        val uriString = converters.fromUri(photoUri)
        assertEquals("file:///data/user/0/com.example/cache/camera_photos/test_box.jpg", uriString)
        val backToUri = converters.toUri(uriString)
        assertEquals(photoUri, backToUri)
        assertNull(converters.fromUri(null))
        assertNull(converters.toUri(null))
    }

    @Test
    fun testBoxAndItemHierarchicalRelationsAndImagePreservation() {
        val parentPhoto = "content://media/external/images/media/101"
        val childPhoto = "content://media/external/images/media/102"
        val itemPhoto = "content://media/external/images/media/103"

        // Parent Box (Container)
        val parentBox = BoxEntity(
            id = "parent_box_1",
            name = "Trastero Balda 1",
            description = "Balda superior del trastero",
            parentBoxId = null,
            primaryPhotoUri = parentPhoto,
            zone = "Trastero"
        )

        // Nested Child Box (Hierarchy)
        val childBox = BoxEntity(
            id = "child_box_1",
            name = "Caja Tornillería",
            description = "Subcaja con tornillos y tuercas",
            parentBoxId = "parent_box_1",
            primaryPhotoUri = childPhoto,
            zone = "Trastero"
        )

        // Item in Container
        val item = ItemEntity(
            id = "item_1",
            name = "Tornillos M4 20mm",
            boxId = "child_box_1",
            primaryPhotoUri = itemPhoto,
            quantity = 50,
            tags = "Tornillería, Metal"
        )

        // Verify relational representations
        val hierarchy = BoxHierarchy(
            box = parentBox,
            childBoxes = listOf(childBox),
            items = emptyList()
        )
        val itemWithBox = ItemWithBox(
            item = item,
            box = childBox
        )

        // Verify container-object hierarchy
        assertEquals("parent_box_1", hierarchy.box.id)
        assertEquals(1, hierarchy.childBoxes.size)
        assertEquals("child_box_1", hierarchy.childBoxes[0].id)
        assertEquals("parent_box_1", hierarchy.childBoxes[0].parentBoxId)

        assertEquals("item_1", itemWithBox.item.id)
        assertEquals("child_box_1", itemWithBox.box.id)
        assertEquals(itemWithBox.item.boxId, itemWithBox.box.id)

        // Verify image persistence
        assertEquals(parentPhoto, hierarchy.box.primaryPhotoUri)
        assertEquals(childPhoto, hierarchy.childBoxes[0].primaryPhotoUri)
        assertEquals(itemPhoto, itemWithBox.item.primaryPhotoUri)
    }

    @Test
    fun testBackupManagerExportAndImportWithBase64Images() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.local.AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val backupManager = com.example.data.backup.BackupManager(db)

        // Setup a temporary photo file
        val photosDir = java.io.File(context.cacheDir, "test_photos").apply { mkdirs() }
        val testPhotoFile = java.io.File(photosDir, "test_box_image.jpg")
        testPhotoFile.writeBytes(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 0x00, 0x10))

        val cat = CategoryEntity("cat_1", "Herramientas", "build", "#F59E0B")
        val zone = ZoneEntity("Taller", "#EF4444")
        val box = BoxEntity(
            id = "box_1",
            name = "Caja Herramientas",
            description = "Taller",
            primaryPhotoUri = Uri.fromFile(testPhotoFile).toString(),
            zone = "Taller"
        )
        val item = ItemEntity(
            id = "item_1",
            name = "Martillo",
            boxId = "box_1",
            categoryId = "cat_1",
            primaryPhotoUri = Uri.fromFile(testPhotoFile).toString(),
            quantity = 2
        )

        db.categoryDao().insertCategory(cat)
        db.zoneDao().insertZone(zone)
        db.boxDao().insertBox(box)
        db.itemDao().insertItem(item)

        // 1. Export JSON with Context
        val jsonOutput = backupManager.exportJson(context)
        assertNotNull(jsonOutput)
        val root = org.json.JSONObject(jsonOutput)
        assertEquals("Inventario de Cajas", root.getString("app"))
        assertEquals(3, root.getInt("version"))
        assertEquals(1, root.getJSONArray("boxes").length())
        assertEquals(1, root.getJSONArray("items").length())
        assertEquals(1, root.getJSONArray("zones").length())
        assertEquals(1, root.getJSONArray("categories").length())

        // 2. Clear Database
        db.itemDao().deleteItemById("item_1")
        db.boxDao().deleteBoxById("box_1")
        assertEquals(0, db.boxDao().getAllBoxesSync().size)
        assertEquals(0, db.itemDao().getAllItemsSync().size)

        // 3. Import JSON with Context
        val importResult = backupManager.importJson(jsonOutput, context)
        assertTrue(importResult.success)
        assertEquals(1, importResult.boxesCount)
        assertEquals(1, importResult.itemsCount)

        // 4. Verify entities restored in Room
        val restoredBoxes = db.boxDao().getAllBoxesSync()
        val restoredItems = db.itemDao().getAllItemsSync()
        assertEquals(1, restoredBoxes.size)
        assertEquals("Caja Herramientas", restoredBoxes[0].name)
        assertEquals(1, restoredItems.size)
        assertEquals("Martillo", restoredItems[0].name)

        db.close()
    }

    @Test
    fun testBackupManagerRejectsCircularReference() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.local.AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val backupManager = com.example.data.backup.BackupManager(db)

        val circularJson = """
            {
              "app": "Inventario de Cajas",
              "version": 3,
              "boxes": [
                { "id": "A", "name": "Box A", "parentBoxId": "B" },
                { "id": "B", "name": "Box B", "parentBoxId": "A" }
              ],
              "items": []
            }
        """.trimIndent()

        val result = backupManager.importJson(circularJson, context)
        org.junit.Assert.assertFalse(result.success)
        assertTrue(result.message.contains("circular", ignoreCase = true))

        db.close()
    }

    @Test
    fun testBackupManagerRestoresReverseHierarchyTopologically() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.local.AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val backupManager = com.example.data.backup.BackupManager(db)

        // Child box appears BEFORE parent box in the JSON array
        val reverseHierarchyJson = """
            {
              "app": "Inventario de Cajas",
              "version": 3,
              "boxes": [
                { "id": "child_box_1", "name": "Caja Hija", "parentBoxId": "parent_box_1" },
                { "id": "parent_box_1", "name": "Caja Padre", "parentBoxId": null }
              ],
              "items": [
                { "id": "item_in_child", "name": "Cable HDMI", "boxId": "child_box_1", "quantity": 1 }
              ]
            }
        """.trimIndent()

        val result = backupManager.importJson(reverseHierarchyJson, context)
        assertTrue(result.success)
        assertEquals(2, result.boxesCount)
        assertEquals(1, result.itemsCount)
        assertTrue(result.message.contains("Copia de seguridad restaurada con éxito", ignoreCase = true))

        val boxesInDb = db.boxDao().getAllBoxesSync()
        val itemsInDb = db.itemDao().getAllItemsSync()
        assertEquals(2, boxesInDb.size)
        assertEquals(1, itemsInDb.size)

        db.close()
    }

    @Test
    fun testBackupManagerHandlesCorruptedOrEmptyJsonGracefully() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.local.AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val backupManager = com.example.data.backup.BackupManager(db)

        // 1. Corrupt JSON
        val corruptResult = backupManager.importJson("{ broken json ...", context)
        assertFalse(corruptResult.success)
        assertTrue(corruptResult.message.contains("inválido", ignoreCase = true))

        // 2. Empty JSON
        val emptyResult = backupManager.importJson("", context)
        assertFalse(emptyResult.success)
        assertTrue(emptyResult.message.contains("vacío", ignoreCase = true))

        db.close()
    }

    @Test
    fun testBackupManagerImportsCsvCatalogWithHierarchicalBoxes() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.local.AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val backupManager = com.example.data.backup.BackupManager(db)

        val csvContent = """
            ID,Nombre,Cantidad,Categoria,Ruta_Caja,Etiquetas,Prestado,Prestado_A,Fecha_Prestamo,Descripcion,Notas
            item_csv_1,"Taladro Percutor",1,Herramientas,"Taller > Estante A > Herramientas Pesadas","taller, eléctrico",SI,"Carlos Gómez",2026-11-10 10:00,"Taladro Bosch 750W","Revisar brocas"
            item_csv_2,"Cinta Aislante",5,Electricidad,"Taller > Estante B","cables, manual",NO,"",,"Cinta negra 20m",""
        """.trimIndent()

        val result = backupManager.importCsv(csvContent)
        assertTrue(result.success)
        assertEquals(2, result.itemsCount)
        assertTrue(result.message.contains("CSV", ignoreCase = true))

        // Verify boxes were created hierarchically
        val boxes = db.boxDao().getAllBoxesSync()
        val boxNames = boxes.map { it.name }.toSet()
        assertTrue(boxNames.contains("Taller"))
        assertTrue(boxNames.contains("Estante A"))
        assertTrue(boxNames.contains("Herramientas Pesadas"))
        assertTrue(boxNames.contains("Estante B"))

        // Verify parent-child relations
        val rootBox = boxes.find { it.name == "Taller" }
        assertNotNull(rootBox)
        assertEquals(null, rootBox?.parentBoxId)

        val estanteA = boxes.find { it.name == "Estante A" }
        assertNotNull(estanteA)
        assertEquals(rootBox?.id, estanteA?.parentBoxId)

        val pesadas = boxes.find { it.name == "Herramientas Pesadas" }
        assertNotNull(pesadas)
        assertEquals(estanteA?.id, pesadas?.parentBoxId)

        // Verify items
        val items = db.itemDao().getAllItemsSync()
        assertEquals(2, items.size)
        val taladro = items.find { it.id == "item_csv_1" }
        assertNotNull(taladro)
        assertEquals("Taladro Percutor", taladro?.name)
        assertEquals(pesadas?.id, taladro?.boxId)
        assertEquals(true, taladro?.isLoaned)
        assertEquals("Carlos Gómez", taladro?.loanedTo)

        // Verify categories
        val categories = db.categoryDao().getAllCategoriesSync()
        val catNames = categories.map { it.name }
        assertTrue(catNames.contains("Herramientas"))
        assertTrue(catNames.contains("Electricidad"))

        db.close()
    }

    @Test
    fun testBackupManagerStreamAutoDetectsCsvAndImportsSuccessfully() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.local.AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val backupManager = com.example.data.backup.BackupManager(db)

        val csvData = """
            Nombre,Cantidad,Categoria,Ruta_Caja
            "Multímetro Digital",2,Electrónica,"Laboratorio > Mesa 1"
        """.trimIndent()

        val inputStream = java.io.ByteArrayInputStream(csvData.toByteArray(Charsets.UTF_8))
        val result = backupManager.importJsonFromStream(context, inputStream)
        assertTrue(result.success)
        assertEquals(1, result.itemsCount)

        val items = db.itemDao().getAllItemsSync()
        assertEquals(1, items.size)
        assertEquals("Multímetro Digital", items[0].name)
        assertEquals(2, items[0].quantity)

        val boxes = db.boxDao().getAllBoxesSync()
        val mesa = boxes.find { it.name == "Mesa 1" }
        assertNotNull(mesa)
        assertEquals(mesa?.id, items[0].boxId)

        db.close()
    }

    @Test
    fun testBackupManagerDoesNotCreateNullZoneForOrphanBoxes() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.local.AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val backupManager = com.example.data.backup.BackupManager(db)

        val jsonWithNullZone = """
            {
              "app": "Inventario de Cajas",
              "version": 3,
              "boxes": [
                {
                  "id": "box_orphan_1",
                  "name": "Caja Sin Zona",
                  "zone": null,
                  "parentBoxId": null
                },
                {
                  "id": "box_orphan_2",
                  "name": "Caja Literal Null",
                  "zone": "null",
                  "parentBoxId": null
                },
                {
                  "id": "box_with_real_zone",
                  "name": "Caja en Taller",
                  "zone": "Taller",
                  "parentBoxId": null
                }
              ],
              "items": []
            }
        """.trimIndent()

        val result = backupManager.importJson(jsonWithNullZone, context)
        assertTrue(result.success)
        assertEquals(3, result.boxesCount)

        val allBoxes = db.boxDao().getAllBoxesSync()
        assertEquals(3, allBoxes.size)

        val orphan1 = allBoxes.find { it.id == "box_orphan_1" }
        assertNotNull(orphan1)
        assertNull(orphan1?.zone)

        val orphan2 = allBoxes.find { it.id == "box_orphan_2" }
        assertNotNull(orphan2)
        assertNull(orphan2?.zone)

        val withZone = allBoxes.find { it.id == "box_with_real_zone" }
        assertNotNull(withZone)
        assertEquals("Taller", withZone?.zone)

        // Ensure no "null" zone entity was created
        val allZones = db.zoneDao().getAllZonesSync()
        assertTrue(allZones.none { it.name.equals("null", ignoreCase = true) })
        assertEquals(1, allZones.size)
        assertEquals("Taller", allZones[0].name)

        db.close()
    }

    @Test
    fun testCuratedIconMapperHasNoDuplicatesAndResolvesAllInventorySymbols() {
        val options = com.example.presentation.components.IconMapper.curatedIconOptions
        val keys = options.map { it.key.lowercase() }
        assertEquals("Icon keys must have zero duplicates", keys.size, keys.distinct().size)

        val vectors = options.map { it.vector }
        assertEquals("Icon vectors must have zero duplicates", vectors.size, vectors.distinct().size)

        // Ensure sports/leisure/health icons are excluded from selector
        assertFalse(keys.contains("hiking"))
        assertFalse(keys.contains("medical_services"))
        assertFalse(keys.contains("sports_soccer"))

        val requiredKeys = listOf(
            "build", "hardware", "bolt", "construction", "settings", "tune", "precision_manufacturing",
            "memory", "cable", "devices", "smartphone", "router", "power", "headphones", "speaker",
            "description", "article", "folder", "folder_special", "receipt_long", "badge", "menu_book", "book",
            "home", "checkroom", "kitchen", "microwave", "inventory_2", "package_2", "lightbulb", "cleaning_services"
        )
        requiredKeys.forEach { key ->
            assertTrue("Missing required icon key: $key", keys.contains(key))
            val resolved = com.example.presentation.components.IconMapper.getCategoryIcon(key)
            assertNotNull(resolved)
        }
    }

    @Test
    fun testTagUtilsNormalizationAntiDuplicateAndDynamicSuggestions() {
        // Normalization: trims & collapses internal spaces
        assertEquals("Cables HDMI", com.example.domain.util.TagUtils.normalizeTag("   Cables   HDMI  "))

        // Case-insensitive deduplication on parse & add
        val parsed = com.example.domain.util.TagUtils.parseTags("  cables , Cables,  HDMI ,  hdmi  ,   Audio   Pro ")
        assertEquals(listOf("cables", "HDMI", "Audio Pro"), parsed)

        val afterAddDuplicate = com.example.domain.util.TagUtils.addTag(parsed, "CABLES")
        assertEquals(parsed, afterAddDuplicate)

        val afterAddNew = com.example.domain.util.TagUtils.addTag(parsed, "  Video   4K ")
        assertEquals(listOf("cables", "HDMI", "Audio Pro", "Video 4K"), afterAddNew)

        val afterRemove = com.example.domain.util.TagUtils.removeTag(afterAddNew, "hdmi")
        assertEquals(listOf("cables", "Audio Pro", "Video 4K"), afterRemove)

        // Dynamic suggestions fallback when no tags exist
        val fallback = com.example.domain.util.TagUtils.computeSuggestedTags(emptyList())
        assertEquals(com.example.domain.util.TagUtils.DEFAULT_FALLBACK_TAGS, fallback)

        // Dynamic suggestions sorted by usage frequency
        val items = listOf(
            ItemEntity("1", "A", boxId = "b1", tags = "Taller, Eléctrico"),
            ItemEntity("2", "B", boxId = "b1", tags = "Taller, Manual"),
            ItemEntity("3", "C", boxId = "b1", tags = "Eléctrico")
        )
        val stats = com.example.domain.util.TagUtils.computeTagStats(items)
        assertEquals(3, stats.size)
        assertEquals("Eléctrico", stats[0].name)
        assertEquals(2, stats[0].itemCount)
        assertEquals("Taller", stats[1].name)
        assertEquals(2, stats[1].itemCount)
        assertEquals("Manual", stats[2].name)
        assertEquals(1, stats[2].itemCount)
    }

    @Test
    fun testRepositoryAtomicBulkRenameMergeAndDeleteTags() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.local.AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repo = com.example.data.repository.InventoryRepository(db)

        db.boxDao().insertBox(BoxEntity(id = "box_1", name = "Caja Test"))
        db.itemDao().insertItems(
            listOf(
                ItemEntity(id = "i1", name = "Cable A", boxId = "box_1", tags = "cables, HDMI"),
                ItemEntity(id = "i2", name = "Cable B", boxId = "box_1", tags = "Cables, Cableado"),
                ItemEntity(id = "i3", name = "Martillo", boxId = "box_1", tags = "Taller")
            )
        )

        // 1. Bulk rename "cables" -> "Cableado" (should merge without duplicate "Cableado" on i2)
        val renameResult = repo.renameTagGlobally("cables", "  Cableado  ")
        assertTrue(renameResult.isSuccess)
        assertEquals(2, renameResult.getOrNull())

        val i1AfterRename = db.itemDao().getItemById("i1")!!
        val i2AfterRename = db.itemDao().getItemById("i2")!!
        val i3AfterRename = db.itemDao().getItemById("i3")!!

        assertEquals("Cableado, HDMI", i1AfterRename.tags)
        assertEquals("Cableado", i2AfterRename.tags)
        assertEquals("Taller", i3AfterRename.tags)

        // 2. Bulk delete "Cableado" (items must remain in DB, only the tag is removed)
        val deleteResult = repo.deleteTagGlobally("cableado")
        assertTrue(deleteResult.isSuccess)
        assertEquals(2, deleteResult.getOrNull())

        val allItemsAfterDelete = db.itemDao().getAllItemsSync()
        assertEquals(3, allItemsAfterDelete.size)
        assertEquals("HDMI", db.itemDao().getItemById("i1")!!.tags)
        assertEquals("", db.itemDao().getItemById("i2")!!.tags)
        assertEquals("Taller", db.itemDao().getItemById("i3")!!.tags)

        db.close()
    }

    @Test
    fun testRepositorySortsBoxesAndItemsByCreatedDateNameAndMostRecent() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.local.AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repo = com.example.data.repository.InventoryRepository(db)

        // Insert root boxes with intentionally non-alphabetical creation timestamps
        val boxZetaOldest = BoxEntity(id = "b_zeta", name = "Zeta Caja", createdAt = 1_000L, zone = "Zona 1")
        val boxAlfaMiddle = BoxEntity(id = "b_alfa", name = "alfa Caja", createdAt = 2_000L, zone = "Zona 1")
        val boxBetaNewest = BoxEntity(id = "b_beta", name = "Beta Caja", createdAt = 3_000L, zone = "Zona 2")
        db.boxDao().insertBoxes(listOf(boxZetaOldest, boxAlfaMiddle, boxBetaNewest))

        // Insert child boxes inside b_alfa
        val childZ = BoxEntity(id = "c_z", name = "Zeta Sub", parentBoxId = "b_alfa", createdAt = 1_000L)
        val childA = BoxEntity(id = "c_a", name = "Alfa Sub", parentBoxId = "b_alfa", createdAt = 3_000L)
        val childM = BoxEntity(id = "c_m", name = "Medio Sub", parentBoxId = "b_alfa", createdAt = 2_000L)
        db.boxDao().insertBoxes(listOf(childZ, childA, childM))

        // Insert items inside b_alfa
        val itemZ = ItemEntity(id = "it_z", name = "Zapatillas", boxId = "b_alfa", createdAt = 1_000L)
        val itemA = ItemEntity(id = "it_a", name = "auriculares", boxId = "b_alfa", createdAt = 2_000L)
        val itemM = ItemEntity(id = "it_m", name = "Martillo", boxId = "b_alfa", createdAt = 3_000L)
        db.itemDao().insertItems(listOf(itemZ, itemA, itemM))

        // 1. Root boxes sorting
        val rootByCreated = repo.getRootBoxesWithCounts(com.example.domain.model.InventorySortOption.CREATED_DATE).first()
        assertEquals(listOf("b_zeta", "b_alfa", "b_beta"), rootByCreated.map { it.box.id })

        val rootByName = repo.getRootBoxesWithCounts(com.example.domain.model.InventorySortOption.NAME_ASC).first()
        assertEquals(listOf("b_alfa", "b_beta", "b_zeta"), rootByName.map { it.box.id })

        val rootByRecent = repo.getRootBoxesWithCounts(com.example.domain.model.InventorySortOption.MOST_RECENT).first()
        assertEquals(listOf("b_beta", "b_alfa", "b_zeta"), rootByRecent.map { it.box.id })

        // 2. Child boxes sorting
        val childByCreated = repo.getChildBoxesWithCounts("b_alfa", com.example.domain.model.InventorySortOption.CREATED_DATE).first()
        assertEquals(listOf("c_z", "c_m", "c_a"), childByCreated.map { it.box.id })

        val childByName = repo.getChildBoxesWithCounts("b_alfa", com.example.domain.model.InventorySortOption.NAME_ASC).first()
        assertEquals(listOf("c_a", "c_m", "c_z"), childByName.map { it.box.id })

        val childByRecent = repo.getChildBoxesWithCounts("b_alfa", com.example.domain.model.InventorySortOption.MOST_RECENT).first()
        assertEquals(listOf("c_a", "c_m", "c_z"), childByRecent.map { it.box.id })

        // 3. Items in box sorting
        val itemsByCreated = repo.getItemsInBox("b_alfa", com.example.domain.model.InventorySortOption.CREATED_DATE).first()
        assertEquals(listOf("it_z", "it_a", "it_m"), itemsByCreated.map { it.id })

        val itemsByName = repo.getItemsInBox("b_alfa", com.example.domain.model.InventorySortOption.NAME_ASC).first()
        assertEquals(listOf("it_a", "it_m", "it_z"), itemsByName.map { it.id })

        val itemsByRecent = repo.getItemsInBox("b_alfa", com.example.domain.model.InventorySortOption.MOST_RECENT).first()
        assertEquals(listOf("it_m", "it_a", "it_z"), itemsByRecent.map { it.id })

        // 4. Global items search sorting
        val allByRecent = repo.searchItems("", sortOption = com.example.domain.model.InventorySortOption.MOST_RECENT).first()
        assertEquals(listOf("it_m", "it_a", "it_z"), allByRecent.map { it.id })

        db.close()
    }

    @Test
    fun testSemanticBoxColorPaletteAndPersistence() = kotlinx.coroutines.runBlocking {
        val colors = com.example.presentation.components.SEMANTIC_BOX_COLORS
        assertTrue(colors.size >= 8)
        assertEquals(colors.size, colors.map { it.hex.uppercase() }.distinct().size)
        colors.forEach {
            assertTrue(it.colorName.isNotBlank())
            assertTrue(it.meaning.isNotBlank())
            assertTrue(it.shortBadge.isNotBlank())
        }

        // Known semantic meanings
        val purple = com.example.presentation.components.resolveSemanticBoxColor("#8B5CF6")
        assertEquals("Púrpura", purple.colorName)
        assertEquals("Creativo", purple.meaning)
        assertEquals("Púrpura · Creativo", purple.headerDisplayLabel)

        val legacyPurple = com.example.presentation.components.resolveSemanticBoxColor("#a855f7")
        assertEquals("Púrpura", legacyPurple.colorName)
        assertEquals("Creativo", legacyPurple.meaning)

        val fallback = com.example.presentation.components.resolveSemanticBoxColor("#123456")
        assertEquals("Personalizado", fallback.colorName)

        // Room persistence of selected semantic colorHex
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.local.AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val box = BoxEntity(id = "box_sem", name = "Cables Estudio", colorHex = "#3B82F6", zone = "Estudio")
        db.boxDao().insertBox(box)
        val loaded = db.boxDao().getBoxById("box_sem")
        assertNotNull(loaded)
        assertEquals("#3B82F6", loaded?.colorHex)
        assertEquals("Tecnología", com.example.presentation.components.resolveSemanticBoxColor(loaded?.colorHex).meaning)
        db.close()
    }

    @Test
    fun testSemanticColorAutoScrollHelpers() {
        // Not in visibleItemsInfo -> not visible
        assertFalse(
            com.example.presentation.components.isColorSwatchFullyVisible(
                itemOffset = null,
                itemSize = null,
                viewportEndOffset = 300,
                afterContentPadding = 10
            )
        )

        // Partially clipped on the left (offset < 0) -> not visible
        assertFalse(
            com.example.presentation.components.isColorSwatchFullyVisible(
                itemOffset = -12,
                itemSize = 48,
                viewportEndOffset = 300,
                afterContentPadding = 10
            )
        )

        // Partially clipped on the right (48dp item starting at 260, safeEnd = 290) -> not visible
        assertFalse(
            com.example.presentation.components.isColorSwatchFullyVisible(
                itemOffset = 260,
                itemSize = 48,
                viewportEndOffset = 300,
                afterContentPadding = 10
            )
        )

        // Completely within visible safe viewport -> visible
        assertTrue(
            com.example.presentation.components.isColorSwatchFullyVisible(
                itemOffset = 60,
                itemSize = 48,
                viewportEndOffset = 300,
                afterContentPadding = 10
            )
        )

        // Centering target calculation: viewport=260, item=48, stride=60 -> (260-48)/2 / 60 = 1 item before
        assertEquals(
            7,
            com.example.presentation.components.computeTargetScrollIndexForSwatch(
                selectedIndex = 8,
                totalCount = 10,
                viewportWidth = 260,
                itemSize = 48,
                itemStride = 60
            )
        )
        assertEquals(
            0,
            com.example.presentation.components.computeTargetScrollIndexForSwatch(
                selectedIndex = 0,
                totalCount = 10,
                viewportWidth = 260,
                itemSize = 48,
                itemStride = 60
            )
        )
    }

    @Test
    fun testBoxIconCatalogPersistenceAndMigration(): Unit = kotlinx.coroutines.runBlocking {
        // 1. Verify 6 primary + 6 additional box icons without duplicates
        val primary = com.example.presentation.components.IconMapper.primaryBoxIcons
        val additional = com.example.presentation.components.IconMapper.additionalBoxIcons
        assertEquals(6, primary.size)
        assertEquals(6, additional.size)
        assertEquals("inventory_2", primary.first().key)

        val allKeys = (primary + additional).map { it.key }
        assertEquals(12, allKeys.distinct().size)
        allKeys.forEach { key ->
            assertNotNull(com.example.presentation.components.IconMapper.getBoxIcon(key))
        }
        // Fallback resolves cleanly to standard inventory_2 icon
        assertEquals(
            com.example.presentation.components.IconMapper.getBoxIcon("inventory_2"),
            com.example.presentation.components.IconMapper.getBoxIcon("unknown_icon")
        )

        // 2. Verify Room persistence + BackupManager export/import roundtrip
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.local.AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repo = com.example.data.repository.InventoryRepository(db)
        val createdId = repo.createBox(
            name = "Caja Audio",
            description = "Micrófonos",
            parentBoxId = null,
            primaryPhotoUri = null,
            colorHex = "#3B82F6",
            zone = "Estudio",
            iconName = "headphones"
        )
        val loaded = db.boxDao().getBoxById(createdId)
        assertNotNull(loaded)
        assertEquals("headphones", loaded?.iconName)

        val backupManager = com.example.data.backup.BackupManager(db)
        val exportedJson = backupManager.exportJson(context)
        db.boxDao().deleteAllBoxes()
        val importResult = backupManager.importJson(exportedJson, context)
        assertTrue(importResult.success)
        val restored = db.boxDao().getBoxById(createdId)
        assertEquals("headphones", restored?.iconName)
        db.close()

        // 3. Verify MIGRATION_5_6 adds icon_name column without losing existing rows
        val dbFile = context.getDatabasePath("migration_test_5_6.db")
        if (dbFile.exists()) dbFile.delete()
        val rawSqlite = android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        rawSqlite.execSQL(
            """
            CREATE TABLE IF NOT EXISTS boxes (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                description TEXT,
                parent_box_id TEXT,
                primary_photo_uri TEXT,
                color_hex TEXT NOT NULL,
                zone TEXT,
                sort_order INTEGER NOT NULL,
                created_at INTEGER NOT NULL,
                FOREIGN KEY(parent_box_id) REFERENCES boxes(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        rawSqlite.execSQL("CREATE INDEX IF NOT EXISTS index_boxes_parent_box_id ON boxes(parent_box_id)")
        rawSqlite.execSQL("CREATE INDEX IF NOT EXISTS index_boxes_name ON boxes(name)")
        rawSqlite.execSQL("CREATE INDEX IF NOT EXISTS index_boxes_zone ON boxes(zone)")
        rawSqlite.execSQL(
            """
            CREATE TABLE IF NOT EXISTS categories (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                icon_name TEXT NOT NULL,
                color_hex TEXT NOT NULL
            )
            """.trimIndent()
        )
        rawSqlite.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_categories_name ON categories(name)")
        rawSqlite.execSQL(
            """
            CREATE TABLE IF NOT EXISTS items (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                description TEXT,
                box_id TEXT NOT NULL,
                category_id TEXT,
                primary_photo_uri TEXT,
                quantity INTEGER NOT NULL,
                tags TEXT NOT NULL,
                is_loaned INTEGER NOT NULL,
                loaned_to TEXT,
                loaned_at INTEGER,
                notes TEXT,
                created_at INTEGER NOT NULL,
                FOREIGN KEY(box_id) REFERENCES boxes(id) ON DELETE CASCADE,
                FOREIGN KEY(category_id) REFERENCES categories(id) ON DELETE SET NULL
            )
            """.trimIndent()
        )
        rawSqlite.execSQL("CREATE INDEX IF NOT EXISTS index_items_box_id ON items(box_id)")
        rawSqlite.execSQL("CREATE INDEX IF NOT EXISTS index_items_category_id ON items(category_id)")
        rawSqlite.execSQL("CREATE INDEX IF NOT EXISTS index_items_name ON items(name)")
        rawSqlite.execSQL("CREATE INDEX IF NOT EXISTS index_items_is_loaned ON items(is_loaned)")
        rawSqlite.execSQL(
            """
            CREATE TABLE IF NOT EXISTS search_history (
                `query` TEXT NOT NULL PRIMARY KEY,
                timestamp INTEGER NOT NULL
            )
            """.trimIndent()
        )
        rawSqlite.execSQL(
            """
            CREATE TABLE IF NOT EXISTS zones (
                name TEXT NOT NULL PRIMARY KEY,
                color_hex TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        rawSqlite.execSQL(
            "INSERT INTO boxes (id, name, description, parent_box_id, primary_photo_uri, color_hex, zone, sort_order, created_at) " +
                "VALUES ('old_box_1', 'Caja Legacy', 'Desc', NULL, NULL, '#F59E0B', 'Taller', 0, 1000)"
        )
        rawSqlite.version = 5
        rawSqlite.close()

        val migratedDb = androidx.room.Room.databaseBuilder(
            context,
            com.example.data.local.AppDatabase::class.java,
            "migration_test_5_6.db"
        )
            .addMigrations(com.example.data.local.AppDatabase.MIGRATION_5_6, com.example.data.local.AppDatabase.MIGRATION_6_7)
            .allowMainThreadQueries()
            .build()
        val migratedBox = migratedDb.boxDao().getBoxById("old_box_1")
        assertNotNull(migratedBox)
        assertEquals("Caja Legacy", migratedBox?.name)
        assertEquals("inventory_2", migratedBox?.iconName)
        assertFalse(migratedBox?.isFragile ?: true)
        migratedDb.close()
        dbFile.delete()
        Unit
    }

    @Test
    fun testFragileBoxPersistenceBackupAndMigration6To7(): Unit = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.local.AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repo = com.example.data.repository.InventoryRepository(db)

        // 1. Default isFragile == false, explicit isFragile == true
        val normalId = repo.createBox(
            name = "Caja Libros",
            description = "Novelas",
            parentBoxId = null,
            primaryPhotoUri = null,
            colorHex = "#10B981",
            zone = "Estudio"
        )
        val fragileId = repo.createBox(
            name = "Cristalería",
            description = "Copas de vino",
            parentBoxId = null,
            primaryPhotoUri = null,
            colorHex = "#EF4444",
            zone = "Cocina",
            iconName = "inventory_2",
            isFragile = true
        )

        val normalBox = db.boxDao().getBoxById(normalId)
        val fragileBox = db.boxDao().getBoxById(fragileId)
        assertNotNull(normalBox)
        assertNotNull(fragileBox)
        assertFalse(normalBox!!.isFragile)
        assertTrue(fragileBox!!.isFragile)

        // Update toggling fragile on and off
        repo.updateBox(normalBox.copy(isFragile = true))
        assertTrue(db.boxDao().getBoxById(normalId)!!.isFragile)
        repo.updateBox(normalBox.copy(isFragile = false))
        assertFalse(db.boxDao().getBoxById(normalId)!!.isFragile)

        // 2. BackupManager JSON export / import roundtrip preserves isFragile
        val backupManager = com.example.data.backup.BackupManager(db)
        val exportedJson = backupManager.exportJson(context)
        db.boxDao().deleteAllBoxes()
        val importResult = backupManager.importJson(exportedJson, context)
        assertTrue(importResult.success)
        assertFalse(db.boxDao().getBoxById(normalId)!!.isFragile)
        assertTrue(db.boxDao().getBoxById(fragileId)!!.isFragile)
        db.close()

        // 3. Verify MIGRATION_6_7 adds is_fragile column without losing existing v6 rows
        val dbFile = context.getDatabasePath("migration_test_6_7.db")
        if (dbFile.exists()) dbFile.delete()
        val rawSqlite = android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        rawSqlite.execSQL(
            """
            CREATE TABLE IF NOT EXISTS boxes (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                description TEXT,
                parent_box_id TEXT,
                primary_photo_uri TEXT,
                color_hex TEXT NOT NULL,
                icon_name TEXT NOT NULL DEFAULT 'inventory_2',
                zone TEXT,
                sort_order INTEGER NOT NULL,
                created_at INTEGER NOT NULL,
                FOREIGN KEY(parent_box_id) REFERENCES boxes(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        rawSqlite.execSQL("CREATE INDEX IF NOT EXISTS index_boxes_parent_box_id ON boxes(parent_box_id)")
        rawSqlite.execSQL("CREATE INDEX IF NOT EXISTS index_boxes_name ON boxes(name)")
        rawSqlite.execSQL("CREATE INDEX IF NOT EXISTS index_boxes_zone ON boxes(zone)")
        rawSqlite.execSQL(
            """
            CREATE TABLE IF NOT EXISTS categories (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                icon_name TEXT NOT NULL,
                color_hex TEXT NOT NULL
            )
            """.trimIndent()
        )
        rawSqlite.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_categories_name ON categories(name)")
        rawSqlite.execSQL(
            """
            CREATE TABLE IF NOT EXISTS items (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                description TEXT,
                box_id TEXT NOT NULL,
                category_id TEXT,
                primary_photo_uri TEXT,
                quantity INTEGER NOT NULL,
                tags TEXT NOT NULL,
                is_loaned INTEGER NOT NULL,
                loaned_to TEXT,
                loaned_at INTEGER,
                notes TEXT,
                created_at INTEGER NOT NULL,
                FOREIGN KEY(box_id) REFERENCES boxes(id) ON DELETE CASCADE,
                FOREIGN KEY(category_id) REFERENCES categories(id) ON DELETE SET NULL
            )
            """.trimIndent()
        )
        rawSqlite.execSQL("CREATE INDEX IF NOT EXISTS index_items_box_id ON items(box_id)")
        rawSqlite.execSQL("CREATE INDEX IF NOT EXISTS index_items_category_id ON items(category_id)")
        rawSqlite.execSQL("CREATE INDEX IF NOT EXISTS index_items_name ON items(name)")
        rawSqlite.execSQL("CREATE INDEX IF NOT EXISTS index_items_is_loaned ON items(is_loaned)")
        rawSqlite.execSQL(
            """
            CREATE TABLE IF NOT EXISTS search_history (
                `query` TEXT NOT NULL PRIMARY KEY,
                timestamp INTEGER NOT NULL
            )
            """.trimIndent()
        )
        rawSqlite.execSQL(
            """
            CREATE TABLE IF NOT EXISTS zones (
                name TEXT NOT NULL PRIMARY KEY,
                color_hex TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        rawSqlite.execSQL(
            "INSERT INTO boxes (id, name, description, parent_box_id, primary_photo_uri, color_hex, icon_name, zone, sort_order, created_at) " +
                "VALUES ('v6_box_1', 'Caja v6', 'Desc v6', NULL, NULL, '#3B82F6', 'build', 'Taller', 0, 2000)"
        )
        rawSqlite.version = 6
        rawSqlite.close()

        val migratedDb = androidx.room.Room.databaseBuilder(
            context,
            com.example.data.local.AppDatabase::class.java,
            "migration_test_6_7.db"
        )
            .addMigrations(com.example.data.local.AppDatabase.MIGRATION_6_7)
            .allowMainThreadQueries()
            .build()
        val migratedBox = migratedDb.boxDao().getBoxById("v6_box_1")
        assertNotNull(migratedBox)
        assertEquals("Caja v6", migratedBox?.name)
        assertEquals("build", migratedBox?.iconName)
        assertFalse(migratedBox?.isFragile ?: true)
        migratedDb.close()
        dbFile.delete()
        Unit
    }
}
