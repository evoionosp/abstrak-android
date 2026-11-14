package org.evoionosp.abstrak.data.session
import android.content.Context
import android.content.SharedPreferences

internal object SessionPreferences {
    fun create(context: Context): SharedPreferences =
        context.getSharedPreferences("session_store", Context.MODE_PRIVATE)
}
