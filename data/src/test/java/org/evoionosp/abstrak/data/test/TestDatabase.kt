package org.evoionosp.abstrak.data.test
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.evoionosp.abstrak.data.library.local.db.AbstrakDatabase

object TestDatabase {
    fun create(): AbstrakDatabase {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return Room
            .inMemoryDatabaseBuilder(context, AbstrakDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }
}
