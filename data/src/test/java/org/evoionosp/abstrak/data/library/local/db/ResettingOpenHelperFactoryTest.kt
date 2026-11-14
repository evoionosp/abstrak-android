package org.evoionosp.abstrak.data.library.local.db
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.evoionosp.abstrak.data.audiobook.local.entity.PendingProgressEntity
import org.evoionosp.abstrak.data.library.local.entity.LibraryEntity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ResettingOpenHelperFactoryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @After
    fun tearDown() {
        context.deleteDatabase(NAME)
    }

    @Test
    fun `a database that fits opens with its data`() =
        runTest {
            createWithLibrary()
            val database = abstrakDatabase()
            assertEquals(listOf("lib-1"), database.libraryDao().getLibraries().map { it.id })
            database.close()
        }

    @Test
    fun `plain Room refuses a schema changed in place at the same version`() {
        createWithLibrary()
        editFile { execSQL("UPDATE room_master_table SET identity_hash = 'stale'") }
        val database = abstrakDatabase(factory = null)
        val failure = assertThrows(IllegalStateException::class.java) { database.openHelper.writableDatabase }
        assertTrue(failure.message.orEmpty().startsWith("Room cannot verify the data integrity"))
        database.close()
    }

    @Test
    fun `a schema changed in place at the same version starts over instead of crashing`() =
        runTest {
            createWithLibrary()
            editFile {
                execSQL("DROP TABLE pending_progress")
                execSQL("UPDATE room_master_table SET identity_hash = 'stale'")
            }
            val database = abstrakDatabase()
            assertTrue(database.libraryDao().getLibraries().isEmpty())
            database.pendingProgressDao().upsert(queued())
            assertEquals(1, database.pendingProgressDao().getAll().size)
            database.libraryDao().upsertLibraries(listOf(library()))
            database.close()
            val reopened = abstrakDatabase()
            assertEquals(1, reopened.libraryDao().getLibraries().size)
            reopened.close()
        }

    @Test
    fun `a file Room can't validate starts over`() =
        runTest {
            editFile(create = true) {
                version = 1
                execSQL("CREATE TABLE legacy (id INTEGER PRIMARY KEY)")
            }
            val database = abstrakDatabase()
            assertTrue(database.libraryDao().getLibraries().isEmpty())
            assertFalse("legacy" in tableNames(database))
            database.close()
        }

    @Test
    fun `a migration Room can't find starts over`() =
        runTest {
            createWithLibrary()
            editFile { version = 5 }
            val database = abstrakDatabase(destructiveFallback = false)
            assertTrue(database.libraryDao().getLibraries().isEmpty())
            assertEquals(1, database.openHelper.writableDatabase.version)
            database.close()
        }

    @Test
    fun `an upgrade that throws starts over`() {
        FrameworkSQLiteOpenHelperFactory()
            .create(SupportSQLiteOpenHelper.Configuration(context, NAME, FakeCallback(version = 1, createTable = "old")))
            .apply { writableDatabase }
            .close()
        val helper =
            ResettingOpenHelperFactory().create(
                SupportSQLiteOpenHelper.Configuration(context, NAME, FakeCallback(version = 2, createTable = "fresh")),
            )
        val db = helper.writableDatabase
        assertEquals(2, db.version)
        val tables = db.query("SELECT name FROM sqlite_master WHERE type = 'table'").use { it.names() }
        assertTrue("fresh" in tables)
        assertFalse("old" in tables)
        helper.close()
    }

    private fun abstrakDatabase(
        factory: SupportSQLiteOpenHelper.Factory? = ResettingOpenHelperFactory(),
        destructiveFallback: Boolean = true,
    ): AbstrakDatabase =
        Room
            .databaseBuilder(context, AbstrakDatabase::class.java, NAME)
            .allowMainThreadQueries()
            .apply { if (destructiveFallback) fallbackToDestructiveMigration(dropAllTables = true) }
            .apply { if (factory != null) openHelperFactory(factory) }
            .build()

    private fun createWithLibrary() {
        val database = abstrakDatabase()
        database.openHelper.writableDatabase.execSQL(
            "INSERT INTO libraries (id, name, displayOrder, isSelected) VALUES ('lib-1', 'Books', 0, 1)",
        )
        database.close()
    }

    private fun editFile(
        create: Boolean = false,
        edit: SQLiteDatabase.() -> Unit,
    ) {
        val flags = SQLiteDatabase.OPEN_READWRITE or if (create) SQLiteDatabase.CREATE_IF_NECESSARY else 0
        val path = context.getDatabasePath(NAME).apply { parentFile?.mkdirs() }.path
        SQLiteDatabase.openDatabase(path, null, flags).use { it.edit() }
    }

    private fun tableNames(database: AbstrakDatabase): Set<String> =
        database.openHelper.writableDatabase
            .query("SELECT name FROM sqlite_master WHERE type = 'table'")
            .use { it.names() }

    private fun android.database.Cursor.names(): Set<String> = buildSet { while (moveToNext()) add(getString(0)) }

    private class FakeCallback(
        version: Int,
        private val createTable: String,
    ) : SupportSQLiteOpenHelper.Callback(version) {
        override fun onCreate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE $createTable (id INTEGER PRIMARY KEY)")
        }

        override fun onUpgrade(
            db: SupportSQLiteDatabase,
            oldVersion: Int,
            newVersion: Int,
        ): Unit = throw SQLiteException("broken migration from $oldVersion to $newVersion")
    }

    private fun library() = LibraryEntity(id = "lib-2", name = "More", displayOrder = 1, isSelected = false)

    private fun queued() =
        PendingProgressEntity(
            audiobookId = "book-1",
            libraryId = "lib-1",
            currentTimeSeconds = 60.0,
            durationSeconds = 300.0,
            isFinished = false,
            savedAtMillis = 1L,
        )

    private companion object {
        const val NAME = "resetting-factory-test.db"
    }
}
