package org.evoionosp.abstrak.playback

enum class PlaybackFailure {
    OFFLINE,
    SERVER,
    NO_AUDIO,
    NOT_FOUND,
    UNKNOWN,
}

sealed interface PlaybackNotice {
    data class CouldNotPlay(
        val failure: PlaybackFailure,
    ) : PlaybackNotice

    data class Stopped(
        val failure: PlaybackFailure,
    ) : PlaybackNotice

    data class MovedToServerProgress(
        val positionSeconds: Double,
    ) : PlaybackNotice
}
