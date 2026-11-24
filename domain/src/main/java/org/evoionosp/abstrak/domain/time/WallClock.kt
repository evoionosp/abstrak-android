package org.evoionosp.abstrak.domain.time

/** The current wall-clock time, injected so rules that depend on it can be tested. */
fun interface WallClock {
    fun nowMillis(): Long

    companion object {
        val System: WallClock = WallClock { java.lang.System.currentTimeMillis() }
    }
}
