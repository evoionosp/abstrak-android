package org.evoionosp.abstrak.presentation.player.sheets
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first
import org.evoionosp.abstrak.domain.audiobook.model.AudiobookChapter
import org.evoionosp.abstrak.presentation.R
import org.evoionosp.abstrak.presentation.designsystem.component.icon.PlayingEqualizerIcon
import org.evoionosp.abstrak.presentation.ui.chapter.ChapterRow
import org.evoionosp.abstrak.presentation.ui.chapter.ChapterRowDefaults
import org.evoionosp.abstrak.presentation.ui.format.toDurationLabel

private val SHEET_HORIZONTAL_PADDING = 24.dp

internal fun chapterListScrollTarget(
    currentIndex: Int,
    chapterCount: Int,
    fullyVisible: List<Int>,
): Int? {
    if (currentIndex !in 0 until chapterCount || currentIndex in fullyVisible) return null
    return (currentIndex - 1).coerceAtLeast(0)
}

internal fun chapterProgressLabel(
    chapters: List<AudiobookChapter>,
    index: Int,
    positionSeconds: Double,
    totalSeconds: Double,
): String? {
    val chapter = chapters.getOrNull(index) ?: return null
    val endSeconds =
        chapter.endInSeconds?.toDouble()
            ?: chapters.getOrNull(index + 1)?.startInSeconds?.toDouble()
            ?: totalSeconds
    val lengthSeconds = endSeconds - chapter.startInSeconds
    if (lengthSeconds <= 0.0) return null
    val elapsedSeconds = (positionSeconds - chapter.startInSeconds).coerceIn(0.0, lengthSeconds)
    val elapsed = elapsedSeconds.toLong().toDurationLabel()
    val length = lengthSeconds.toLong().toDurationLabel()
    return "${chapter.startInSeconds.toDurationLabel()} · $elapsed\u202F/\u202F$length"
}

@Composable
internal fun ChaptersSheet(
    chapters: List<AudiobookChapter>,
    currentChapterIndex: Int,
    positionSeconds: Double,
    totalSeconds: Double,
    isPlaying: Boolean,
    onPlayChapter: (AudiobookChapter) -> Unit,
    onDismiss: () -> Unit,
) {
    PlayerBottomSheet(
        onDismiss = onDismiss,
        title = stringResource(R.string.now_playing_chapters),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
        titlePadding = PaddingValues(horizontal = SHEET_HORIZONTAL_PADDING),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        if (chapters.isEmpty()) {
            Text(
                text = stringResource(R.string.now_playing_chapters_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = SHEET_HORIZONTAL_PADDING),
            )
        } else {
            val listState = rememberLazyListState()
            LaunchedEffect(listState) {
                val layout = snapshotFlow { listState.layoutInfo }.first { it.visibleItemsInfo.isNotEmpty() }
                val fullyVisible =
                    layout.visibleItemsInfo
                        .filter {
                            it.offset >= layout.viewportStartOffset &&
                                it.offset + it.size <= layout.viewportEndOffset
                        }.map { it.index }
                chapterListScrollTarget(currentChapterIndex, chapters.size, fullyVisible)?.let {
                    listState.scrollToItem(it)
                }
            }
            val currentProgress = chapterProgressLabel(chapters, currentChapterIndex, positionSeconds, totalSeconds)
            LazyColumn(
                state = listState,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 320.dp, max = 500.dp),
            ) {
                itemsIndexed(
                    items = chapters,
                    contentType = { _, _ -> "chapter" },
                ) { index, chapter ->
                    val isCurrent = index == currentChapterIndex
                    ChapterRow(
                        chapter = chapter,
                        isCurrent = isCurrent,
                        onPlay = { onPlayChapter(chapter) },
                        contentPadding = ChapterRowDefaults.contentPadding(horizontal = SHEET_HORIZONTAL_PADDING),
                        supporting = if (isCurrent) currentProgress else null,
                        trailing =
                            if (isCurrent) {
                                { PlayingEqualizerIcon(isAnimating = isPlaying) }
                            } else {
                                null
                            },
                    )
                }
            }
        }
    }
}
