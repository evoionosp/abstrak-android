package org.evoionosp.abstrak.domain.audiobook.playback
import org.evoionosp.abstrak.domain.audiobook.model.PlaybackProgress

object ProgressMerge {
    fun isDeviceAhead(
        device: PlaybackProgress,
        server: PlaybackProgress?,
    ): Boolean =
        when {
            server == null -> true
            device.isFinished -> !server.isFinished
            server.isFinished -> false
            else -> device.currentTimeSeconds > server.currentTimeSeconds
        }

    fun furthest(
        device: PlaybackProgress?,
        server: PlaybackProgress?,
    ): PlaybackProgress? = if (device != null && isDeviceAhead(device, server)) device else server
}
