package org.evoionosp.abstrak.data.library.local.db
import android.util.Log
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory

class ResettingOpenHelperFactory(
    private val delegate: SupportSQLiteOpenHelper.Factory = FrameworkSQLiteOpenHelperFactory(),
) : SupportSQLiteOpenHelper.Factory {
    override fun create(configuration: SupportSQLiteOpenHelper.Configuration): SupportSQLiteOpenHelper =
        delegate.create(
            SupportSQLiteOpenHelper.Configuration(
                context = configuration.context,
                name = configuration.name,
                callback = ResettingCallback(configuration.callback),
                useNoBackupDirectory = configuration.useNoBackupDirectory,
                allowDataLossOnRecovery = configuration.allowDataLossOnRecovery,
            ),
        )
}

internal class ResettingCallback(
    private val room: SupportSQLiteOpenHelper.Callback,
) : SupportSQLiteOpenHelper.Callback(room.version) {
    override fun onConfigure(db: SupportSQLiteDatabase) = room.onConfigure(db)

    override fun onCreate(db: SupportSQLiteDatabase) = startOverIfFailing(db) { room.onCreate(db) }

    override fun onUpgrade(
        db: SupportSQLiteDatabase,
        oldVersion: Int,
        newVersion: Int,
    ) = startOverIfFailing(db) { room.onUpgrade(db, oldVersion, newVersion) }

    override fun onDowngrade(
        db: SupportSQLiteDatabase,
        oldVersion: Int,
        newVersion: Int,
    ) = startOverIfFailing(db) { room.onDowngrade(db, oldVersion, newVersion) }

    override fun onOpen(db: SupportSQLiteDatabase) {
        try {
            room.onOpen(db)
        } catch (failure: Exception) {
            Log.w(TAG, "The database doesn't fit the app's schema; starting it over", failure)
            db.execSQL("PRAGMA foreign_keys = OFF")
            db.beginTransaction()
            try {
                db.dropEverything()
                room.onCreate(db)
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
            room.onOpen(db)
        }
    }

    override fun onCorruption(db: SupportSQLiteDatabase) = room.onCorruption(db)

    private inline fun startOverIfFailing(
        db: SupportSQLiteDatabase,
        step: () -> Unit,
    ) {
        try {
            step()
        } catch (failure: Exception) {
            Log.w(TAG, "Creating or migrating the database failed; starting it over", failure)
            db.dropEverything()
            room.onCreate(db)
        }
    }

    private companion object {
        const val TAG = "AbstrakDatabase"
    }
}

internal fun SupportSQLiteDatabase.dropEverything() {
    val objects =
        buildList {
            query("SELECT type, name FROM sqlite_master WHERE type IN ('table', 'view')").use { cursor ->
                while (cursor.moveToNext()) {
                    val name = cursor.getString(1)
                    if (!name.startsWith("sqlite_") && name != "android_metadata") add(cursor.getString(0) to name)
                }
            }
        }
    objects.sortedByDescending { (type, _) -> type == "view" }.forEach { (type, name) ->
        execSQL("DROP ${if (type == "view") "VIEW" else "TABLE"} IF EXISTS `$name`")
    }
}
