package org.evoionosp.abstrak.presentation.auth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.evoionosp.abstrak.presentation.R
import org.evoionosp.abstrak.presentation.designsystem.component.button.LoadingButton
import org.evoionosp.abstrak.presentation.designsystem.component.button.SegmentedChoiceRow
import org.evoionosp.abstrak.presentation.designsystem.component.card.TonalCard
import org.evoionosp.abstrak.presentation.designsystem.theme.AbstrakShapes
import org.evoionosp.abstrak.presentation.designsystem.theme.AbstrakSpacing

@Composable
fun ConnectToServerCard(
    state: AuthUiState,
    onAction: (AuthAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val protocols = listOf(stringResource(R.string.https_protocol), stringResource(R.string.http_protocol))
    val locale = LocalLocale.current.platformLocale
    val submit = {
        keyboardController?.hide()
        onAction(AuthAction.CheckServer)
    }
    TonalCard(
        modifier = modifier.fillMaxWidth(),
        shadowElevation = 3.dp,
        contentPadding = PaddingValues(AbstrakSpacing.content),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.connect_to_server),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        SegmentedChoiceRow(
            options = protocols,
            isSelected = { state.protocol.equals(it, ignoreCase = true) },
            onSelect = { onAction(AuthAction.ProtocolChange(it)) },
            label = { it.uppercase(locale) },
        )
        OutlinedTextField(
            value = state.baseUrl,
            onValueChange = { onAction(AuthAction.UrlChange(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.server_url_label)) },
            placeholder = { Text(stringResource(R.string.server_url_placeholder)) },
            shape = AbstrakShapes.field,
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done,
                ),
            keyboardActions =
                KeyboardActions(
                    onDone = { if (!state.isChecking) submit() },
                ),
        )
        LoadingButton(
            text = stringResource(R.string.check_server),
            loadingText = stringResource(R.string.checking_server),
            isLoading = state.isChecking,
            onClick = submit,
            enabled = !state.isChecking,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
