package com.onfaa.tunnel.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ServerConfigDao {
    @Query("SELECT * FROM server_configs ORDER BY name ASC")
    fun observeAll(): Flow<List<ServerConfigEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ServerConfigEntity)

    @Query("DELETE FROM server_configs WHERE id = :id")
    suspend fun deleteById(id: String)
}
