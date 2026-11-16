package com.onfaa.tunnel.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ServerConfigEntity::class],
    version = 1,
    exportSchema = false
)
abstract class OnfaaDatabase : RoomDatabase() {
    abstract fun serverConfigDao(): ServerConfigDao
}
