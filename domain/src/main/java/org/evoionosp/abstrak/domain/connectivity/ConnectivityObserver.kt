package org.evoionosp.abstrak.domain.connectivity
import kotlinx.coroutines.flow.Flow

interface ConnectivityObserver {
    fun observe(): Flow<Boolean>

    fun isConnected(): Boolean
}
