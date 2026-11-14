package org.evoionosp.abstrak.presentation.player
import org.evoionosp.abstrak.domain.audiobook.model.AudiobookChapter

sealed interface PlayerAction {
    data object TogglePlayPause : PlayerAction

    data object SeekBackward : PlayerAction

    data object SeekForward : PlayerAction

    data class SeekTo(
        val positionMs: Long,
    ) : PlayerAction

    data object PreviousChapter : PlayerAction

    data object NextChapter : PlayerAction

    data class PlayChapter(
        val chapter: AudiobookChapter,
    ) : PlayerAction

    data class SetSpeed(
        val speed: Float,
    ) : PlayerAction

    data class ArmSleepTimer(
        val minutes: Int,
    ) : PlayerAction

    data object ArmSleepEndOfChapter : PlayerAction

    data class SaveSleepTimerMinutes(
        val minutes: Int,
    ) : PlayerAction

    data object CancelSleepTimer : PlayerAction
}
