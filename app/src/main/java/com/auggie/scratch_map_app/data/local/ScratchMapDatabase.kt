package com.auggie.scratch_map_app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.auggie.scratch_map_app.data.local.dao.ScratchTileDao
import com.auggie.scratch_map_app.data.local.entity.ScratchTileEntity


@Database(
    entities = [
        ScratchTileEntity::class
    ],
    version = 1,
    exportSchema = true
)

abstract class ScratchMapDatabase : RoomDatabase() {
    abstract fun scratchTileDao(): ScratchTileDao
}
