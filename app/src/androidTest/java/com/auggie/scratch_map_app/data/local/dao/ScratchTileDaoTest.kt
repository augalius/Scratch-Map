package com.auggie.scratch_map_app.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.auggie.scratch_map_app.data.local.ScratchMapDatabase
import com.auggie.scratch_map_app.data.local.dao.ScratchTileDao
import com.auggie.scratch_map_app.data.local.entity.ScratchTileEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScratchTileDaoTest {

    private lateinit var db: ScratchMapDatabase
    private lateinit var dao: ScratchTileDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ScratchMapDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.scratchTileDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertTiles_increasesCount() = runTest {
        val tiles = listOf(
            ScratchTileEntity(100, 200, 16, 54.6872, 25.2797),
            ScratchTileEntity(101, 200, 16, 54.6873, 25.2798)
        )
        dao.insertTiles(tiles)
        assertEquals(2L, dao.getCount())
    }

    @Test
    fun insertSameTileTwice_doesNotCreateDuplicate() = runTest {
        val tile = ScratchTileEntity(100, 200, 16, 54.6872, 25.2797)
        dao.insertTiles(listOf(tile))
        dao.insertTiles(listOf(tile))
        assertEquals(1L, dao.getCount())
    }

    @Test
    fun insertTiles_returnsMinusOneForDuplicates() = runTest {
        val tile = ScratchTileEntity(100, 200, 16, 54.6872, 25.2797)
        dao.insertTiles(listOf(tile))
        val rowIds = dao.insertTiles(listOf(tile))
        assertEquals(-1L, rowIds[0])
    }

    @Test
    fun tilesWithDifferentZoomLevels_areStoredSeparately() = runTest {
        val tile16 = ScratchTileEntity(100, 200, 16, 54.6872, 25.2797)
        val tile17 = ScratchTileEntity(100, 200, 17, 54.6872, 25.2797)
        dao.insertTiles(listOf(tile16, tile17))
        assertEquals(2L, dao.getCount())
    }

    @Test
    fun emptyDatabase_hasZeroCount() = runTest {
        assertEquals(0L, dao.getCount())
    }
}
