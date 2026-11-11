package com.example

import com.example.data.local.dao.BoxDao
import com.example.data.local.dao.BoxWithCounts
import com.example.data.local.entity.BoxEntity
import com.example.data.local.relation.BoxHierarchy
import com.example.data.local.relation.BoxWithChildren
import com.example.data.local.relation.BoxWithItems
import com.example.domain.usecase.MoveBoxResult
import com.example.domain.usecase.MoveBoxUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    private class FakeBoxDao(private val boxes: MutableList<BoxEntity>) : BoxDao {
        override fun getRootBoxesWithCounts(): Flow<List<BoxWithCounts>> = flowOf(emptyList())
        override fun getChildBoxesWithCounts(parentId: String): Flow<List<BoxWithCounts>> = flowOf(emptyList())
        override fun getBoxHierarchy(boxId: String): Flow<BoxHierarchy?> = flowOf(
            boxes.find { it.id == boxId }?.let { b ->
                BoxHierarchy(
                    box = b,
                    childBoxes = boxes.filter { it.parentBoxId == boxId },
                    items = emptyList()
                )
            }
        )
        override fun getBoxWithItems(boxId: String): Flow<BoxWithItems?> = flowOf(
            boxes.find { it.id == boxId }?.let { b ->
                BoxWithItems(box = b, items = emptyList())
            }
        )
        override fun getBoxWithChildren(boxId: String): Flow<BoxWithChildren?> = flowOf(
            boxes.find { it.id == boxId }?.let { b ->
                BoxWithChildren(box = b, childBoxes = boxes.filter { it.parentBoxId == boxId })
            }
        )
        override fun getRootBoxesHierarchy(): Flow<List<BoxHierarchy>> = flowOf(
            boxes.filter { it.parentBoxId == null }.map { b ->
                BoxHierarchy(
                    box = b,
                    childBoxes = boxes.filter { it.parentBoxId == b.id },
                    items = emptyList()
                )
            }
        )
        override suspend fun getBoxById(id: String): BoxEntity? = boxes.find { it.id == id }
        override fun getBoxFlowById(id: String): Flow<BoxEntity?> = flowOf(boxes.find { it.id == id })
        override fun getAllBoxes(): Flow<List<BoxEntity>> = flowOf(boxes)
        override suspend fun getAllBoxesSync(): List<BoxEntity> = boxes.toList()
        override suspend fun insertBox(box: BoxEntity) { boxes.add(box) }
        override suspend fun insertBoxes(boxes: List<BoxEntity>) { this.boxes.addAll(boxes) }
        override suspend fun updateBox(box: BoxEntity) {
            val idx = boxes.indexOfFirst { it.id == box.id }
            if (idx >= 0) boxes[idx] = box
        }
        override suspend fun deleteBox(box: BoxEntity) { boxes.removeIf { it.id == box.id } }
        override suspend fun deleteBoxById(boxId: String) { boxes.removeIf { it.id == boxId } }
        override suspend fun deleteAllBoxes() { boxes.clear() }
        override fun getTotalBoxesCount(): Flow<Int> = flowOf(boxes.size)
        override fun searchBoxes(query: String): Flow<List<BoxEntity>> = flowOf(boxes)
        override fun getAllZones(): Flow<List<String>> = flowOf(boxes.mapNotNull { it.zone?.takeIf { z -> z.isNotBlank() && !z.equals("null", ignoreCase = true) } }.distinct())
        override suspend fun renameZoneInBoxes(oldName: String, newName: String) {
            boxes.indices.forEach { i ->
                if (boxes[i].zone == oldName) {
                    boxes[i] = boxes[i].copy(zone = newName)
                }
            }
        }
        override suspend fun clearZoneInBoxes(name: String) {
            boxes.indices.forEach { i ->
                if (boxes[i].zone == name) {
                    boxes[i] = boxes[i].copy(zone = null)
                }
            }
        }
    }

    @Test
    fun testMoveBoxCyclePrevention() = runBlocking {
        // Hierarchy:
        // BoxA (root)
        //   -> BoxB (child of A)
        //       -> BoxC (child of B)
        // BoxD (root)
        val boxA = BoxEntity(id = "A", name = "Box A", parentBoxId = null)
        val boxB = BoxEntity(id = "B", name = "Box B", parentBoxId = "A")
        val boxC = BoxEntity(id = "C", name = "Box C", parentBoxId = "B")
        val boxD = BoxEntity(id = "D", name = "Box D", parentBoxId = null)

        val fakeDao = FakeBoxDao(mutableListOf(boxA, boxB, boxC, boxD))
        val useCase = MoveBoxUseCase(fakeDao)

        // 1. Moving Box A into itself is rejected
        val selfMove = useCase.execute("A", "A")
        assertTrue(selfMove is MoveBoxResult.Error)

        // 2. Moving Box A into child Box B creates a cycle -> rejected
        val childMove = useCase.execute("A", "B")
        assertTrue(childMove is MoveBoxResult.Error)

        // 3. Moving Box A into grandchild Box C creates a cycle -> rejected
        val grandchildMove = useCase.execute("A", "C")
        assertTrue(grandchildMove is MoveBoxResult.Error)

        // 4. Moving Box C to root is valid
        val toRoot = useCase.execute("C", null)
        assertTrue(toRoot is MoveBoxResult.Success)

        // 5. Moving Box B into Box D (sibling tree) is valid
        val toSibling = useCase.execute("B", "D")
        assertTrue(toSibling is MoveBoxResult.Success)
    }

    @Test
    fun testSearchHistoryLogic() {
        val history = mutableListOf<String>()

        fun addQuery(q: String) {
            val trimmed = q.trim()
            if (trimmed.isNotBlank()) {
                history.remove(trimmed)
                history.add(0, trimmed)
            }
        }

        addQuery("cables")
        addQuery("herramientas")
        addQuery("cables") // should move to top

        assertEquals(2, history.size)
        assertEquals("cables", history[0])
        assertEquals("herramientas", history[1])

        history.remove("cables")
        assertEquals(1, history.size)
        assertEquals("herramientas", history[0])

        history.clear()
        assertTrue(history.isEmpty())
    }

    @Test
    fun testBoxZoneAssignment() = runBlocking {
        val boxWithExistingZone = BoxEntity(id = "1", name = "Tornillos", zone = "Taller")
        val boxWithNewZone = BoxEntity(id = "2", name = "Navidad", zone = "Desván")
        val boxWithoutZone = BoxEntity(id = "3", name = "Varios", zone = null)

        val fakeDao = FakeBoxDao(mutableListOf(boxWithExistingZone, boxWithNewZone, boxWithoutZone))
        val zones = mutableListOf<String>()
        fakeDao.getAllZones().collect { zones.addAll(it) }

        assertEquals(2, zones.size)
        assertTrue(zones.contains("Taller"))
        assertTrue(zones.contains("Desván"))
        assertFalse(zones.contains("Varios"))
    }

    @Test
    fun testZoneRenameAndClear() = runBlocking {
        val box1 = BoxEntity(id = "1", name = "Tornillos", zone = "Taller")
        val box2 = BoxEntity(id = "2", name = "Cables", zone = "Taller")
        val fakeDao = FakeBoxDao(mutableListOf(box1, box2))

        fakeDao.renameZoneInBoxes("Taller", "Garaje")
        val box1Updated = fakeDao.getBoxById("1")
        val box2Updated = fakeDao.getBoxById("2")
        assertEquals("Garaje", box1Updated?.zone)
        assertEquals("Garaje", box2Updated?.zone)

        fakeDao.clearZoneInBoxes("Garaje")
        val box1Cleared = fakeDao.getBoxById("1")
        assertEquals(null, box1Cleared?.zone)
    }

    @Test
    fun testThemeModeSelection() {
        fun resolveTheme(mode: com.example.ui.theme.ThemeMode, systemDark: Boolean): Boolean = when (mode) {
            com.example.ui.theme.ThemeMode.SYSTEM -> systemDark
            com.example.ui.theme.ThemeMode.DARK -> true
            com.example.ui.theme.ThemeMode.LIGHT -> false
        }

        // When mode is SYSTEM, it follows system dark/light state
        assertTrue(resolveTheme(com.example.ui.theme.ThemeMode.SYSTEM, systemDark = true))
        assertFalse(resolveTheme(com.example.ui.theme.ThemeMode.SYSTEM, systemDark = false))

        // When mode is DARK, it is always dark
        assertTrue(resolveTheme(com.example.ui.theme.ThemeMode.DARK, systemDark = true))
        assertTrue(resolveTheme(com.example.ui.theme.ThemeMode.DARK, systemDark = false))

        // When mode is LIGHT, it is always light
        assertFalse(resolveTheme(com.example.ui.theme.ThemeMode.LIGHT, systemDark = true))
        assertFalse(resolveTheme(com.example.ui.theme.ThemeMode.LIGHT, systemDark = false))
    }
}
