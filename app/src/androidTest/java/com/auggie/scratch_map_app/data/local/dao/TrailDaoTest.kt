package com.auggie.scratch_map_app.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.auggie.scratch_map_app.data.local.ScratchMapDatabase
import com.auggie.scratch_map_app.data.local.dao.TrailDao
import com.auggie.scratch_map_app.data.local.entity.TrailCheckpointEntity
import com.auggie.scratch_map_app.data.local.entity.TrailEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrailDaoTest {

    private lateinit var db: ScratchMapDatabase
    private lateinit var dao: TrailDao

    private val trailA = TrailEntity(
        id = "trail_a", name = "Gediminas Hill Trail",
        description = "Historic hill walk", difficulty = "EASY",
        estimatedDistanceKm = 1.2
    )

    private val trailB = TrailEntity(
        id = "trail_b", name = "Vilnius Old Town Loop",
        description = "City centre loop", difficulty = "MEDIUM",
        estimatedDistanceKm = 3.5
    )

    private fun checkpoint(id: String, trailId: String, order: Int) = TrailCheckpointEntity(
        id = id, trailId = trailId, name = "Point $id",
        latitude = 54.6872 + order * 0.001,
        longitude = 25.2797 + order * 0.001,
        checkpointOrder = order
    )

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ScratchMapDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.trailDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertTrail_canBeRetrievedById() = runTest {
        dao.insertTrail(trailA)
        val result = dao.getById("trail_a")
        assertNotNull(result)
        assertEquals("Gediminas Hill Trail", result!!.name)
    }




    @Test
    fun markTrailCompleted_setsIsCompletedTrue() = runTest {
        dao.insertTrail(trailA)
        dao.markTrailCompleted("trail_a")
        val result = dao.getById("trail_a")
        assertTrue(result!!.isCompleted)
    }

    @Test
    fun insertSameTrailTwice_doesNotCreateDuplicate() = runTest {
        dao.insertTrail(trailA)
        dao.insertTrail(trailA)
        assertEquals(1, dao.getLockedTrails().size)
    }

    @Test
    fun insertCheckpoints_canBeRetrieved() = runTest {
        dao.insertTrail(trailA)
        dao.insertCheckpoints(listOf(checkpoint("cp1", "trail_a", 1), checkpoint("cp2", "trail_a", 2)))
        val checkpoints = dao.getCheckpoints("trail_a")
        assertEquals(2, checkpoints.size)
    }

    @Test
    fun checkpoints_areOrderedByCheckpointOrder() = runTest {
        dao.insertTrail(trailA)
        dao.insertCheckpoints(listOf(checkpoint("cp2", "trail_a", 2), checkpoint("cp1", "trail_a", 1)))
        val checkpoints = dao.getCheckpoints("trail_a")
        assertEquals(1, checkpoints[0].checkpointOrder)
        assertEquals(2, checkpoints[1].checkpointOrder)
    }

    @Test
    fun markCheckpointReached_setsIsReachedTrue() = runTest {
        dao.insertTrail(trailA)
        dao.insertCheckpoints(listOf(checkpoint("cp1", "trail_a", 1)))
        dao.markCheckpointReached("cp1")
        val cp = dao.getCheckpoints("trail_a")[0]
        assertTrue(cp.isReached)
    }

    @Test
    fun markAllCheckpointsReached_setsAllReachedForTrail() = runTest {
        dao.insertTrail(trailA)
        dao.insertCheckpoints(listOf(
            checkpoint("cp1", "trail_a", 1),
            checkpoint("cp2", "trail_a", 2)
        ))
        dao.markAllCheckpointsReached("trail_a")
        val checkpoints = dao.getCheckpoints("trail_a")
        assertTrue(checkpoints.all { it.isReached })
    }

    @Test
    fun markAllCheckpointsReached_onlyAffectsTargetTrail() = runTest {
        dao.insertTrail(trailA)
        dao.insertTrail(trailB)
        dao.insertCheckpoints(listOf(
            checkpoint("cpA", "trail_a", 1),
            checkpoint("cpB", "trail_b", 1)
        ))
        dao.markAllCheckpointsReached("trail_a")
        val bCheckpoints = dao.getCheckpoints("trail_b")
        assertFalse(bCheckpoints[0].isReached)
    }

    @Test
    fun deleteNotIn_removesTrailsNotInList() = runTest {
        dao.insertTrail(trailA)
        dao.insertTrail(trailB)
        dao.deleteNotIn(listOf("trail_a"))
        assertNull(dao.getById("trail_b"))
        assertNotNull(dao.getById("trail_a"))
    }

    @Test
    fun getById_returnsNullForUnknownId() = runTest {
        assertNull(dao.getById("nonexistent"))
    }

}
