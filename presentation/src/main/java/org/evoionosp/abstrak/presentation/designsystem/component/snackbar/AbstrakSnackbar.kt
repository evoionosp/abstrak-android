package org.evoionosp.abstrak.presentation.designsystem.component.snackbar
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.evoionosp.abstrak.presentation.R
import org.evoionosp.abstrak.presentation.designsystem.theme.AbstrakExtendedColors
import org.evoionosp.abstrak.presentation.designsystem.theme.extendedColors

enum class SnackbarType(
    val icon: ImageVector,
    @param:StringRes val label: Int,
) {
    INFO(Icons.Outlined.Info, R.string.snackbar_info),
    SUCCESS(Icons.Outlined.CheckCircle, R.string.snackbar_success),
    WARNING(Icons.Outlined.WarningAmber, R.string.snackbar_warning),
    ERROR(Icons.Outlined.ErrorOutline, R.string.snackbar_error),
}

data class AbstrakSnackbarVisuals(
    override val message: String,
    val type: SnackbarType = SnackbarType.INFO,
    override val duration: SnackbarDuration = SnackbarDuration.Short,
) : SnackbarVisuals {
    override val actionLabel: String? get() = null
    override val withDismissAction: Boolean get() = false
}

@Immutable
data class SnackbarColors(
    val container: Color,
    val content: Color,
)

fun snackbarColors(
    type: SnackbarType,
    scheme: ColorScheme,
    extended: AbstrakExtendedColors,
): SnackbarColors =
    when (type) {
        SnackbarType.INFO -> SnackbarColors(container = scheme.inverseSurface, content = scheme.inverseOnSurface)
        SnackbarType.SUCCESS -> SnackbarColors(container = extended.success, content = extended.onSuccess)
        SnackbarType.WARNING -> SnackbarColors(container = extended.warning, content = extended.onWarning)
        SnackbarType.ERROR -> SnackbarColors(container = scheme.error, content = scheme.onError)
    }

@Composable
fun AbstrakSnackbar(
    data: SnackbarData,
    modifier: Modifier = Modifier,
) {
    val type = (data.visuals as? AbstrakSnackbarVisuals)?.type ?: SnackbarType.INFO
    val colors = snackbarColors(type, MaterialTheme.colorScheme, MaterialTheme.extendedColors)
    Snackbar(
        modifier = modifier.padding(12.dp),
        containerColor = colors.container,
        contentColor = colors.content,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = type.icon,
                contentDescription = stringResource(type.label),
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = data.visuals.message)
        }
    }
}
