package org.evoionosp.abstrak.domain.audiobook.playback
import org.evoionosp.abstrak.domain.audiobook.model.PlaybackProgress

/**
 * Most recently updated wins, by [PlaybackProgress.lastUpdateMillis] on both sides.
 *
 * The device's time is when the position was saved here, and every save sends that time to the
 * server as lastUpdate, so a record this device wrote compares on this device's own clock. A
 * record another client wrote carries that client's clock, or the server's when the client sent
 * none; skew between those clocks can only decide updates made within the skew of each other.
 */
object ProgressMerge {
    /**
     * The more recently updated of [device] and [server]. The device wins only when strictly
     * newer: a tie, or an undated device record, keeps the server's, so an older device position
     * is never sent over a more recent server one.
     */
    fun newest(
        device: PlaybackProgress?,
        server: PlaybackProgress?,
    ): PlaybackProgress? = if (device != null && isDeviceNewer(device, server)) device else server

    fun isDeviceNewer(
        device: PlaybackProgress,
        server: PlaybackProgress?,
    ): Boolean = server == null || device.lastUpdateMillis > server.lastUpdateMillis
}
