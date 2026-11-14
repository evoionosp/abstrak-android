package org.evoionosp.abstrak.presentation.ui.chapter
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.evoionosp.abstrak.domain.audiobook.model.AudiobookChapter
import org.evoionosp.abstrak.presentation.ui.format.toDurationLabel

@Composable
fun ChapterRow(
    chapter: AudiobookChapter,
    isCurrent: Boolean,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = ChapterRowDefaults.contentPadding(),
    supporting: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onPlay)
                .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = chapter.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = supporting ?: chapter.startInSeconds.toDurationLabel(),
                style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(12.dp))
            trailing()
        }
    }
}

fun LazyListScope.chapterItems(
    chapters: List<AudiobookChapter>,
    currentIndex: Int,
    onPlay: (AudiobookChapter) -> Unit,
    contentPadding: PaddingValues = ChapterRowDefaults.contentPadding(),
    trailing: (@Composable (index: Int) -> Unit)? = null,
) {
    itemsIndexed(
        items = chapters,
        contentType = { _, _ -> "chapter" },
    ) { index, chapter ->
        ChapterRow(
            chapter = chapter,
            isCurrent = index == currentIndex,
            onPlay = { onPlay(chapter) },
            contentPadding = contentPadding,
            trailing = trailing?.let { { it(index) } },
        )
    }
}

object ChapterRowDefaults {
    fun contentPadding(horizontal: Dp = 16.dp): PaddingValues = PaddingValues(horizontal = horizontal, vertical = 8.dp)
}
