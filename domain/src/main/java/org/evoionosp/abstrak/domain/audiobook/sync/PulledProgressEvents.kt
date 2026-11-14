package org.evoionosp.abstrak.domain.audiobook.sync
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.evoionosp.abstrak.domain.audiobook.model.PlaybackProgress

data class PulledProgress(
    val audiobookId: String,
    val progress: PlaybackProgress,
)

@Singleton
class PulledProgressEvents
    @Inject
    constructor() {
        private val events = MutableSharedFlow<PulledProgress>(extraBufferCapacity = EVENT_BUFFER)
        val pulled: SharedFlow<PulledProgress> = events.asSharedFlow()

        fun publish(pulled: PulledProgress) {
            events.tryEmit(pulled)
        }

        private companion object {
            const val EVENT_BUFFER = 16
        }
    }
