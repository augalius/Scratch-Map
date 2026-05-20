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
        TrailCheckpointEntity::class
    ],
    version = 19,
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

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS trails_new (
                        id TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL,
                        difficulty TEXT NOT NULL,
                        estimated_distance_km REAL NOT NULL,
                        is_completed INTEGER NOT NULL
                    )
                """
                )
                db.execSQL("INSERT OR IGNORE INTO trails_new SELECT id, name, description, difficulty, estimated_distance_km, is_completed FROM trails")
                db.execSQL("DROP TABLE trails")
                db.execSQL("ALTER TABLE trails_new RENAME TO trails")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS trail_checkpoints_new (
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
                db.execSQL("INSERT OR IGNORE INTO trail_checkpoints_new SELECT id, trail_id, name, description, latitude, longitude, checkpoint_order, unlock_radius_meters, is_reached, reached_at_millis FROM trail_checkpoints")
                db.execSQL("DROP TABLE trail_checkpoints")
                db.execSQL("ALTER TABLE trail_checkpoints_new RENAME TO trail_checkpoints")
            }
        }

        val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS trail_checkpoints_new (
                        id TEXT PRIMARY KEY NOT NULL,
                        trail_id TEXT NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL DEFAULT '',
                        latitude REAL NOT NULL,
                        longitude REAL NOT NULL,
                        checkpoint_order INTEGER NOT NULL DEFAULT 0,
                        unlock_radius_meters INTEGER NOT NULL DEFAULT 50,
                        is_reached INTEGER NOT NULL DEFAULT 0,
                        question TEXT NOT NULL DEFAULT '',
                        answer TEXT NOT NULL DEFAULT '',
                        is_pending_answer INTEGER NOT NULL DEFAULT 0
                    )
                """
                )
                db.execSQL("INSERT OR IGNORE INTO trail_checkpoints_new SELECT id, trail_id, name, description, latitude, longitude, checkpoint_order, unlock_radius_meters, is_reached, question, answer, is_pending_answer FROM trail_checkpoints")
                db.execSQL("DROP TABLE trail_checkpoints")
                db.execSQL("ALTER TABLE trail_checkpoints_new RENAME TO trail_checkpoints")
            }
        }

        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS waypoints_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        string_id TEXT NOT NULL,
                        name TEXT NOT NULL,
                        latitude REAL NOT NULL,
                        longitude REAL NOT NULL,
                        description TEXT NOT NULL DEFAULT '',
                        category TEXT NOT NULL DEFAULT 'OTHER',
                        unlock_radius_meters INTEGER NOT NULL DEFAULT 200,
                        image_url TEXT NOT NULL DEFAULT '',
                        is_unlocked INTEGER NOT NULL DEFAULT 0
                    )
                """
                )
                db.execSQL("INSERT OR IGNORE INTO waypoints_new SELECT id, string_id, name, latitude, longitude, description, category, unlock_radius_meters, image_url, is_unlocked FROM waypoints")
                db.execSQL("DROP TABLE waypoints")
                db.execSQL("ALTER TABLE waypoints_new RENAME TO waypoints")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_waypoints_string_id ON waypoints (string_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_waypoints_latitude_longitude ON waypoints (latitude, longitude)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_waypoints_category ON waypoints (category)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_waypoints_is_unlocked ON waypoints (is_unlocked)")
            }
        }

        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS scratch_tiles_new (
                        tile_x INTEGER NOT NULL,
                        tile_y INTEGER NOT NULL,
                        zoom_level INTEGER NOT NULL,
                        center_lat REAL NOT NULL,
                        center_lng REAL NOT NULL,
                        synced_to_cloud INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY (tile_x, tile_y, zoom_level)
                    )
                """
                )
                db.execSQL("INSERT OR IGNORE INTO scratch_tiles_new SELECT tile_x, tile_y, zoom_level, center_lat, center_lng, synced_to_cloud FROM scratch_tiles")
                db.execSQL("DROP TABLE scratch_tiles")
                db.execSQL("ALTER TABLE scratch_tiles_new RENAME TO scratch_tiles")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_scratch_tiles_center_lat_center_lng ON scratch_tiles (center_lat, center_lng)")
            }
        }

        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS achievement_unlocks")
            }
        }

        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE trails ADD COLUMN is_unlocked INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE trails ADD COLUMN unlock_radius_meters INTEGER NOT NULL DEFAULT 100")
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE trail_checkpoints ADD COLUMN is_pending_answer INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE trail_checkpoints ADD COLUMN question TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE trail_checkpoints ADD COLUMN answer TEXT NOT NULL DEFAULT ''")
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
