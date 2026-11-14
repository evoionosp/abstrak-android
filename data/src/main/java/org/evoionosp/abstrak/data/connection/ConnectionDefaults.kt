package org.evoionosp.abstrak.data.connection
import android.os.Build

internal fun defaultUserAgent(): String =
    "Mozilla/5.0 (Linux; Android ${Build.VERSION.RELEASE}; K) " +
        "AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/130.0.6723.106 Mobile Safari/537.36"
