package com.auggie.scratch_map_app.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.auggie.scratch_map_app.data.local.ScratchMapDatabase
import com.auggie.scratch_map_app.data.local.dao.RegionDao
import com.auggie.scratch_map_app.data.local.entity.RegionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RegionDaoTest {

    private lateinit var db: ScratchMapDatabase
    private lateinit var dao: RegionDao

    private val oldTown = RegionEntity(
        id = "vilnius_old_town", name = "Old Town",
        minLat = 54.67, maxLat = 54.70,
        minLng = 25.27, maxLng = 25.30,
        goalTiles = 200
    )

    private val newTown = RegionEntity(
        id = "vilnius_new_town", name = "New Town",
        minLat = 54.68, maxLat = 54.72,
        minLng = 25.26, maxLng = 25.31,
        goalTiles = 300
    )

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ScratchMapDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.regionDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAll_regionsCanBeObserved() = runTest {
        dao.insertAll(listOf(oldTown, newTown))
        val regions = dao.observeAll().first()
        assertEquals(2, regions.size)
    }

    @Test
    fun observeAll_returnsRegionsOrderedByName() = runTest {
        dao.insertAll(listOf(newTown, oldTown))
        val regions = dao.observeAll().first()
        assertEquals("New Town", regions[0].name)
        assertEquals("Old Town", regions[1].name)
    }

    @Test
    fun insertAll_withReplace_updatesExistingRegion() = runTest {
        dao.insertAll(listOf(oldTown))
        val updated = oldTown.copy(name = "Old Town Updated", goalTiles = 999)
        dao.insertAll(listOf(updated))
        val regions = dao.observeAll().first()
        assertEquals(1, regions.size)
        assertEquals("Old Town Updated", regions[0].name)
        assertEquals(999, regions[0].goalTiles)
    }

    @Test
    fun deleteNotIn_removesRegionsNotInList() = runTest {
        dao.insertAll(listOf(oldTown, newTown))
        dao.deleteNotIn(listOf("vilnius_old_town"))
        val regions = dao.observeAll().first()
        assertEquals(1, regions.size)
        assertEquals("vilnius_old_town", regions[0].id)
    }

    @Test
    fun deleteNotIn_keepsAllWhenAllIdsProvided() = runTest {
        dao.insertAll(listOf(oldTown, newTown))
        dao.deleteNotIn(listOf("vilnius_old_town", "vilnius_new_town"))
        assertEquals(2, dao.observeAll().first().size)
    }

    @Test
    fun deleteNotIn_removesAllWhenEmptyListProvided() = runTest {
        dao.insertAll(listOf(oldTown, newTown))
        dao.deleteNotIn(emptyList())
        assertTrue(dao.observeAll().first().isEmpty())
    }

    @Test
    fun insertAll_preservesBoundaryJson() = runTest {
        val withBoundary = oldTown.copy(boundaryJson = """[[54.67,25.27],[54.70,25.30]]""")
        dao.insertAll(listOf(withBoundary))
        val result = dao.observeAll().first()[0]
        assertEquals("""[[54.67,25.27],[54.70,25.30]]""", result.boundaryJson)
    }

    @Test
    fun emptyDatabase_returnsEmptyList() = runTest {
        assertTrue(dao.observeAll().first().isEmpty())
    }
}
