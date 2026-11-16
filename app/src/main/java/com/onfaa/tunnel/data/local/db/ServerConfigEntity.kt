package com.onfaa.tunnel.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "server_configs")
data class ServerConfigEntity(
    @PrimaryKey val id: String,
    val name: String,
    val protocol: String,
    val endpoint: String,
    val isFavorite: Boolean = false,
    val notes: String? = null
)
