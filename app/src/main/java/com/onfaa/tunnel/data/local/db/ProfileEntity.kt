package com.onfaa.tunnel.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted server profile.
 *
 * [payloadCipher] holds the AES-GCM envelope of the Xray outbound produced by
 * libXray: the credential (UUID, keys) is never stored in clear text and is
 * never logged. [name] and [endpoint] are non-secret display fields.
 */
@Entity(tableName = "server_configs")
data class ProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val protocol: String,
    val endpoint: String,
    val security: String,
    val payloadCipher: String,
    val isFavorite: Boolean,
    val notes: String?,
    val createdAt: Long,
    val updatedAt: Long
)
