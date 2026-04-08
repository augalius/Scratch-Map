package com.auggie.scratch_map_app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.auggie.scratch_map_app.data.local.dao.RegionDao
import com.auggie.scratch_map_app.data.local.dao.ScratchTileDao
import com.auggie.scratch_map_app.data.local.dao.WaypointDao
import com.auggie.scratch_map_app.data.local.entity.AchievementUnlockEntity
import com.auggie.scratch_map_app.data.local.entity.RegionEntity
import com.auggie.scratch_map_app.data.local.entity.ScratchTileEntity
import com.auggie.scratch_map_app.data.local.entity.WaypointEntity


@Database(
    entities = [
        ScratchTileEntity::class,
        WaypointEntity::class,
        RegionEntity::class,
        AchievementUnlockEntity::class
    ],
    version = 4,
    exportSchema = true
)

abstract class ScratchMapDatabase : RoomDatabase() {
    abstract fun scratchTileDao(): ScratchTileDao
    abstract fun waypointDao(): WaypointDao
    abstract fun regionDao(): RegionDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS waypoints")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS waypoints (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        string_id TEXT NOT NULL,
                        name TEXT NOT NULL,
                        latitude REAL NOT NULL,
                        longitude REAL NOT NULL,
                        description TEXT NOT NULL DEFAULT '',
                        category TEXT NOT NULL DEFAULT 'OTHER',
                        unlock_radius_meters INTEGER NOT NULL DEFAULT 50,
                        icon TEXT NOT NULL DEFAULT '',
                        is_unlocked INTEGER NOT NULL DEFAULT 0,
                        unlocked_at_millis INTEGER,
                        created_at_millis INTEGER NOT NULL DEFAULT 0
                    )
                """
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_waypoints_string_id ON waypoints (string_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_waypoints_latitude_longitude ON waypoints (latitude, longitude)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_waypoints_category ON waypoints (category)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_waypoints_is_unlocked ON waypoints (is_unlocked)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS regions (
                        id TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL DEFAULT '',
                        min_lat REAL NOT NULL,
                        max_lat REAL NOT NULL,
                        min_lng REAL NOT NULL,
                        max_lng REAL NOT NULL,
                        goal_tiles INTEGER NOT NULL
                    )
                """
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS achievement_unlocks (
                        achievement_id TEXT PRIMARY KEY NOT NULL,
                        unlocked_at_millis INTEGER NOT NULL
                    )
                """
                )
            }
        }
    }
}