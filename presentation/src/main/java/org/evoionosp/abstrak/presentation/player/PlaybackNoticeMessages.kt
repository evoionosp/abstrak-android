package org.evoionosp.abstrak.presentation.player
import androidx.annotation.StringRes
import org.evoionosp.abstrak.playback.PlaybackFailure
import org.evoionosp.abstrak.playback.PlaybackNotice
import org.evoionosp.abstrak.presentation.R
import org.evoionosp.abstrak.presentation.designsystem.component.snackbar.SnackbarType
import org.evoionosp.abstrak.presentation.ui.format.toDurationLabel
import org.evoionosp.abstrak.presentation.ui.message.SnackbarMessage
import org.evoionosp.abstrak.presentation.ui.message.UiText

internal fun PlaybackNotice.toSnackbarMessage(): SnackbarMessage =
    when (this) {
        is PlaybackNotice.CouldNotPlay -> {
            SnackbarMessage(UiText.Resource(failure.couldNotPlayText()), SnackbarType.ERROR)
        }

        is PlaybackNotice.Stopped -> {
            SnackbarMessage(UiText.Resource(failure.stoppedText()), SnackbarType.ERROR)
        }

        is PlaybackNotice.MovedToServerProgress -> {
            val position = positionSeconds.toLong().toDurationLabel()
            val text = UiText.Resource(R.string.player_moved_to_server_progress, listOf(position))
            SnackbarMessage(text, SnackbarType.INFO)
        }
    }

@StringRes
private fun PlaybackFailure.couldNotPlayText(): Int =
    when (this) {
        PlaybackFailure.OFFLINE -> R.string.player_error_offline
        PlaybackFailure.SERVER -> R.string.player_error_server
        PlaybackFailure.NO_AUDIO -> R.string.player_error_no_audio
        PlaybackFailure.NOT_FOUND -> R.string.player_error_not_found
        PlaybackFailure.UNKNOWN -> R.string.player_error_unknown
    }

@StringRes
private fun PlaybackFailure.stoppedText(): Int =
    when (this) {
        PlaybackFailure.OFFLINE -> R.string.player_error_offline
        PlaybackFailure.SERVER -> R.string.player_stopped_server
        PlaybackFailure.NO_AUDIO, PlaybackFailure.NOT_FOUND, PlaybackFailure.UNKNOWN -> R.string.player_stopped_unknown
    }
