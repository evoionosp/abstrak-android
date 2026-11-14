package org.evoionosp.abstrak.data.connection
import android.content.Context
import android.content.SharedPreferences

internal object ConnectionPreferences {
    fun create(context: Context): SharedPreferences =
        context.getSharedPreferences("connection_store", Context.MODE_PRIVATE)
}
