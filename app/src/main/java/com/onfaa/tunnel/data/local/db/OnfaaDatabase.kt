package com.onfaa.tunnel.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ProfileEntity::class],
    version = 2,
    exportSchema = false
)
abstract class OnfaaDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao

    companion object {
        const val NAME = "onfaa_profiles.db"

        /**
         * v1 stored only display fields (id, name, protocol, endpoint,
         * isFavorite, notes). v2 adds the encrypted engine payload, the
         * security mode and timestamps. Rows are recreated explicitly instead
         * of using ALTER TABLE DEFAULT, so the resulting schema is exactly the
         * one Room validates (no default-value mismatch).
         *
         * Migrated rows keep their display data but have an empty payload:
         * they are surfaced as "incomplete" and the user must re-import the
         * link, because the credential was never persisted by v1.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `server_configs_new` (" +
                        "`id` TEXT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`protocol` TEXT NOT NULL, " +
                        "`endpoint` TEXT NOT NULL, " +
                        "`security` TEXT NOT NULL, " +
                        "`payloadCipher` TEXT NOT NULL, " +
                        "`isFavorite` INTEGER NOT NULL, " +
                        "`notes` TEXT, " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "INSERT OR REPLACE INTO `server_configs_new` " +
                        "(`id`,`name`,`protocol`,`endpoint`,`security`,`payloadCipher`," +
                        "`isFavorite`,`notes`,`createdAt`,`updatedAt`) " +
                        "SELECT `id`,`name`,`protocol`,`endpoint`,'none',''," +
                        "`isFavorite`,`notes`,0,0 FROM `server_configs`"
                )
                db.execSQL("DROP TABLE `server_configs`")
                db.execSQL("ALTER TABLE `server_configs_new` RENAME TO `server_configs`")
            }
        }

        fun build(context: Context): OnfaaDatabase =
            Room.databaseBuilder(context.applicationContext, OnfaaDatabase::class.java, NAME)
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
