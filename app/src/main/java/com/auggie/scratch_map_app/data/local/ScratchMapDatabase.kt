package com.auggie.scratch_map_app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.auggie.scratch_map_app.data.local.dao.AchievementDefinitionDao
import com.auggie.scratch_map_app.data.local.dao.RegionDao
import com.auggie.scratch_map_app.data.local.dao.ScratchTileDao
import com.auggie.scratch_map_app.data.local.dao.TrailDao
import com.auggie.scratch_map_app.data.local.dao.WaypointDao
import com.auggie.scratch_map_app.data.local.entity.AchievementDefinitionEntity
import com.auggie.scratch_map_app.data.local.entity.AchievementUnlockEntity
import com.auggie.scratch_map_app.data.local.entity.RegionEntity
import com.auggie.scratch_map_app.data.local.entity.ScratchTileEntity
import com.auggie.scratch_map_app.data.local.entity.TrailCheckpointEntity
import com.auggie.scratch_map_app.data.local.entity.TrailEntity
import com.auggie.scratch_map_app.data.local.entity.WaypointEntity


@Database(
    entities = [
        ScratchTileEntity::class,
        WaypointEntity::class,
        RegionEntity::class,
        AchievementDefinitionEntity::class,
        TrailEntity::class,
        TrailCheckpointEntity::class,
        AchievementUnlockEntity::class
    ],
    version = 11,
    exportSchema = true
)

abstract class ScratchMapDatabase : RoomDatabase() {
    abstract fun scratchTileDao(): ScratchTileDao
    abstract fun waypointDao(): WaypointDao
    abstract fun regionDao(): RegionDao
    abstract fun achievementDefinitionDao(): AchievementDefinitionDao
    abstract fun trailDao(): TrailDao

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

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE regions ADD COLUMN boundary_json TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS trails (
                        id TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL,
                        difficulty TEXT NOT NULL,
                        estimated_distance_km REAL NOT NULL,
                        is_completed INTEGER NOT NULL
                    )
                """
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS trail_checkpoints (
                        id TEXT PRIMARY KEY NOT NULL,
                        trail_id TEXT NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL,
                        latitude REAL NOT NULL,
                        longitude REAL NOT NULL,
                        checkpoint_order INTEGER NOT NULL,
                        unlock_radius_meters INTEGER NOT NULL,
                        is_reached INTEGER NOT NULL,
                        reached_at_millis INTEGER
                    )
                """
                )
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE scratch_tiles ADD COLUMN synced_to_cloud INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE achievement_definitions ADD COLUMN is_regional INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE achievement_definitions ADD COLUMN category TEXT")
                db.execSQL("ALTER TABLE achievement_definitions ADD COLUMN stages_json TEXT NOT NULL DEFAULT '[]'")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE achievement_definitions ADD COLUMN region_id TEXT")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS achievement_definitions (
                        id TEXT PRIMARY KEY NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT NOT NULL,
                        icon_name TEXT NOT NULL,
                        target_value INTEGER NOT NULL,
                        metric_type TEXT NOT NULL
                    )
                """
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE waypoints ADD COLUMN image_url TEXT NOT NULL DEFAULT ''")
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