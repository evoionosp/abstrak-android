package org.evoionosp.abstrak.presentation.ui.catalog
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.evoionosp.abstrak.presentation.designsystem.theme.AbstrakSpacing
import org.evoionosp.abstrak.presentation.ui.book.BookCover
import org.evoionosp.abstrak.presentation.ui.model.AudiobookUiModel

@Composable
internal fun AudiobookListRow(
    audiobook: AudiobookUiModel,
    detail: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onClickLabel: String? = null,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    selected: Boolean = false,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
    contentPadding: PaddingValues =
        if (trailing == null) {
            AudiobookListRowDefaults.ContentPadding
        } else {
            AudiobookListRowDefaults.ContentPaddingWithTrailing
        },
    coverShape: Shape = AudiobookListRowDefaults.CoverShape,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                .combinedClickable(
                    onClickLabel = onClickLabel,
                    role = Role.Button,
                    onLongClickLabel = onLongClickLabel,
                    onLongClick = onLongClick,
                    onClick = onClick,
                ).padding(contentPadding)
                .heightIn(min = AudiobookListRowDefaults.Height)
                .alpha(if (enabled) 1f else UnsupportedBookDefaults.CONTENT_ALPHA),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .height(AudiobookListRowDefaults.Height)
                    .aspectRatio(AudiobookListRowDefaults.COVER_ASPECT_RATIO)
                    .clip(coverShape),
        ) {
            BookCover(
                coverUrl = audiobook.coverUrl,
                title = audiobook.title,
                author = audiobook.author,
                modifier = Modifier.fillMaxSize(),
                colorFilter = if (enabled) null else UnsupportedBookDefaults.CoverFilter,
            )
            if (selected) {
                SelectedCoverMark()
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = audiobook.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = audiobook.author,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        trailing?.invoke()
    }
}

@Composable
private fun SelectedCoverMark() {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(32.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

internal object AudiobookListRowDefaults {
    val Height = 84.dp
    const val COVER_ASPECT_RATIO = 0.72f
    val CoverShape = RoundedCornerShape(8.dp)
    val ContentPadding = PaddingValues(horizontal = AbstrakSpacing.catalog, vertical = 8.dp)
    val ContentPaddingWithTrailing =
        PaddingValues(start = AbstrakSpacing.catalog, end = 4.dp, top = 8.dp, bottom = 8.dp)
}
