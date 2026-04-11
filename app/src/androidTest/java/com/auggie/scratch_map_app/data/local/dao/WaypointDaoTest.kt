package com.auggie.scratch_map_app.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.auggie.scratch_map_app.data.local.ScratchMapDatabase
import com.auggie.scratch_map_app.data.local.dao.WaypointDao
import com.auggie.scratch_map_app.data.local.entity.WaypointEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WaypointDaoTest {

    private lateinit var db: ScratchMapDatabase
    private lateinit var dao: WaypointDao

    private val church = WaypointEntity(
        stringId = "church_1", name = "St. Anne's Church",
        latitude = 54.6861, longitude = 25.2900,
        category = "CHURCH", unlockRadiusMeters = 50
    )

    private val monument = WaypointEntity(
        stringId = "monument_1", name = "Gediminas Tower",
        latitude = 54.6872, longitude = 25.2797,
        category = "MONUMENT", unlockRadiusMeters = 100
    )

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ScratchMapDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.waypointDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertWaypoints_canBeRetrievedAsLocked() = runTest {
        dao.insertAll(listOf(church, monument))
        val locked = dao.getLockedWaypoints()
        assertEquals(2, locked.size)
    }

    @Test
    fun newWaypoints_areLockedByDefault() = runTest {
        dao.insertAll(listOf(church))
        val locked = dao.getLockedWaypoints()
        assertFalse(locked[0].isUnlocked)
    }

    @Test
    fun markUnlocked_removesWaypointFromLockedList() = runTest {
        dao.insertAll(listOf(church, monument))
        val id = dao.getLockedWaypoints().first { it.stringId == "church_1" }.id
        dao.markUnlocked(id)
        val locked = dao.getLockedWaypoints()
        assertEquals(1, locked.size)
        assertEquals("monument_1", locked[0].stringId)
    }

    @Test
    fun markUnlocked_setsIsUnlockedToTrue() = runTest {
        dao.insertAll(listOf(church))
        val id = dao.getLockedWaypoints()[0].id
        dao.markUnlocked(id)
        val byStringId = dao.getByStringId("church_1")
        assertNotNull(byStringId)
        assertTrue(byStringId!!.isUnlocked)
    }

    @Test
    fun insertSameWaypointTwice_doesNotCreateDuplicate() = runTest {
        dao.insertAll(listOf(church))
        dao.insertAll(listOf(church))
        assertEquals(1, dao.getLockedWaypoints().size)
    }

    @Test
    fun getByStringId_returnsCorrectWaypoint() = runTest {
        dao.insertAll(listOf(church, monument))
        val result = dao.getByStringId("monument_1")
        assertNotNull(result)
        assertEquals("Gediminas Tower", result!!.name)
    }

    @Test
    fun getByStringId_returnsNullForUnknownId() = runTest {
        val result = dao.getByStringId("does_not_exist")
        assertNull(result)
    }


    @Test
    fun emptyDatabase_returnsEmptyLockedList() = runTest {
        assertTrue(dao.getLockedWaypoints().isEmpty())
    }
}
