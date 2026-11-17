package com.onfaa.tunnel.data.local.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.onfaa.tunnel.core.security.SecretCipher
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Room v1 schema, reduced to the columns that existed before Phase 4. It is
 * declared here (test sources only) so the v1 -> v2 migration can be exercised
 * against a real Room-created database without shipping deprecated entities in
 * the application.
 */
@Entity(tableName = "server_configs")
data class LegacyServerConfig(
    @PrimaryKey val id: String,
    val name: String,
    val protocol: String,
    val endpoint: String,
    val isFavorite: Boolean,
    val notes: String?
)

@Dao
interface LegacyServerConfigDao {
    @Insert
    suspend fun insert(entity: LegacyServerConfig)

    @Query("SELECT * FROM server_configs")
    suspend fun all(): List<LegacyServerConfig>
}

@Database(entities = [LegacyServerConfig::class], version = 1, exportSchema = false)
abstract class LegacyServerConfigDatabase : RoomDatabase() {
    abstract fun legacyDao(): LegacyServerConfigDao
}

@RunWith(AndroidJUnit4::class)
class ProfileStorageInstrumentedTest {

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val migrationDbName = "profile-migration-test.db"

    @After
    fun removeDatabases() {
        context.deleteDatabase(migrationDbName)
    }

    @Test
    fun migrationFromVersion1PreservesRowsAndAddsCredentialColumns() = runBlocking {
        context.deleteDatabase(migrationDbName)

        val legacy = Room.databaseBuilder(
            context,
            LegacyServerConfigDatabase::class.java,
            migrationDbName
        ).build()
        legacy.legacyDao().insert(
            LegacyServerConfig(
                id = "legacy-1",
                name = "Ancien profil",
                protocol = "VLESS",
                endpoint = "192.168.1.10:443",
                isFavorite = true,
                notes = "note v1"
            )
        )
        legacy.close()

        val database = Room.databaseBuilder(context, OnfaaDatabase::class.java, migrationDbName)
            .addMigrations(OnfaaDatabase.MIGRATION_1_2)
            .build()

        val row = database.profileDao().findById("legacy-1")
        assertNotNull("the v1 row must survive the migration", row)
        assertEquals("Ancien profil", row!!.name)
        assertEquals("VLESS", row.protocol)
        assertEquals("192.168.1.10:443", row.endpoint)
        assertTrue(row.isFavorite)
        assertEquals("note v1", row.notes)
        assertEquals("none", row.security)
        assertEquals("", row.payloadCipher)
        assertEquals(1, database.profileDao().count())
        database.close()
    }

    @Test
    fun daoCrudKeepsTheCredentialOnlyInTheEncryptedColumn() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(context, OnfaaDatabase::class.java).build()
        val dao = database.profileDao()
        val credential = "123e4567-e89b-12d3-a456-426614174000"

        dao.upsert(
            ProfileEntity(
                id = "profile-1",
                name = "Serveur de test",
                protocol = "VLESS",
                endpoint = "10.10.0.1:443",
                security = "tls",
                payloadCipher = SecretCipher.encrypt(
                    """{"protocol":"vless","settings":{"id":"$credential"}}"""
                ),
                isFavorite = false,
                notes = null,
                createdAt = 1L,
                updatedAt = 1L
            )
        )

        val stored = dao.findById("profile-1")
        assertNotNull(stored)
        assertFalse(stored!!.payloadCipher.contains(credential))
        assertFalse(stored.endpoint.contains(credential))

        assertEquals(1, dao.rename("profile-1", "Renommé", 2L))
        assertEquals("Renommé", dao.findById("profile-1")!!.name)

        assertEquals(1, dao.setFavorite("profile-1", true, 3L))
        assertTrue(dao.findById("profile-1")!!.isFavorite)

        dao.deleteById("profile-1")
        assertNull(dao.findById("profile-1"))
        assertEquals(0, dao.count())
        database.close()
    }

    @Test
    fun secretCipherRoundTripsAndRejectsTamperedPayload() {
        val plain = """{"protocol":"vmess","settings":{"id":"secret-uuid","address":"a.example"}}"""
        val encrypted = SecretCipher.encrypt(plain)

        assertFalse(encrypted.contains("secret-uuid"))
        assertEquals(plain, SecretCipher.decrypt(encrypted))

        val tampered = encrypted.dropLast(4) + "AAAA"
        assertThrows(SecretCipher.SecretUnavailable::class.java) {
            SecretCipher.decrypt(tampered)
        }
    }
}
