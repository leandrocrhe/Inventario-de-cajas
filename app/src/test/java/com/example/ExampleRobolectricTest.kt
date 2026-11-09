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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
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
}
