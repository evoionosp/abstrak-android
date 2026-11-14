package org.evoionosp.abstrak.presentation.player
import org.evoionosp.abstrak.playback.PlaybackFailure
import org.evoionosp.abstrak.playback.PlaybackNotice
import org.evoionosp.abstrak.presentation.R
import org.evoionosp.abstrak.presentation.designsystem.component.snackbar.SnackbarType
import org.evoionosp.abstrak.presentation.ui.message.SnackbarMessage
import org.evoionosp.abstrak.presentation.ui.message.UiText
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackNoticeMessagesTest {
    @Test
    fun `a book that couldn't play is an error saying why`() {
        val expected =
            mapOf(
                PlaybackFailure.OFFLINE to R.string.player_error_offline,
                PlaybackFailure.SERVER to R.string.player_error_server,
                PlaybackFailure.NO_AUDIO to R.string.player_error_no_audio,
                PlaybackFailure.NOT_FOUND to R.string.player_error_not_found,
                PlaybackFailure.UNKNOWN to R.string.player_error_unknown,
            )
        for ((failure, text) in expected) {
            assertEquals(error(text), PlaybackNotice.CouldNotPlay(failure).toSnackbarMessage())
        }
    }

    @Test
    fun `playback that stopped is an error saying why`() {
        val expected =
            mapOf(
                PlaybackFailure.OFFLINE to R.string.player_error_offline,
                PlaybackFailure.SERVER to R.string.player_stopped_server,
                PlaybackFailure.UNKNOWN to R.string.player_stopped_unknown,
            )
        for ((failure, text) in expected) {
            assertEquals(error(text), PlaybackNotice.Stopped(failure).toSnackbarMessage())
        }
    }

    @Test
    fun `a move to where another device got to is news with the position`() {
        assertEquals(
            SnackbarMessage(UiText.Resource(R.string.player_moved_to_server_progress, listOf("1:30:05")), SnackbarType.INFO),
            PlaybackNotice.MovedToServerProgress(5_405.6).toSnackbarMessage(),
        )
    }

    private fun error(text: Int) = SnackbarMessage(UiText.Resource(text), SnackbarType.ERROR)
}
