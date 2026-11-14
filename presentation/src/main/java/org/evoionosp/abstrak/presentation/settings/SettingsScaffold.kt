package org.evoionosp.abstrak.presentation.settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.evoionosp.abstrak.presentation.designsystem.component.header.CollapsingTitleBarHeight
import org.evoionosp.abstrak.presentation.designsystem.component.header.CollapsingTitleHeader
import org.evoionosp.abstrak.presentation.designsystem.component.header.CollapsingTitleHeaderMaxHeight
import org.evoionosp.abstrak.presentation.designsystem.component.header.rememberCollapsingHeaderState
import org.evoionosp.abstrak.presentation.designsystem.theme.AbstrakSpacing
import org.evoionosp.abstrak.presentation.designsystem.theme.screenBackgroundBrush

@Composable
internal fun SettingsScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    miniTrailPadding: Dp = 0.dp,
    content: LazyListScope.() -> Unit,
) {
    val listState = rememberLazyListState()
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val header =
        rememberCollapsingHeaderState(
            listState = listState,
            minHeight = CollapsingTitleBarHeight + statusBarHeight,
            maxHeight = CollapsingTitleHeaderMaxHeight,
        )
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(screenBackgroundBrush())
                .nestedScroll(header.nestedScrollConnection),
    ) {
        val headerHeight = with(LocalDensity.current) { header.heightPx.toDp() }
        val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    top = headerHeight + 8.dp,
                    start = AbstrakSpacing.content,
                    end = AbstrakSpacing.content,
                    bottom = navigationBarHeight + 24.dp + miniTrailPadding,
                ),
            verticalArrangement = Arrangement.spacedBy(AbstrakSpacing.item),
            content = content,
        )
        CollapsingTitleHeader(
            title = title,
            collapseFraction = header.collapseFraction,
            height = headerHeight,
            onBack = onBack,
        )
    }
}

@Composable
internal fun SettingsIntro(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
