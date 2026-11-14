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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import org.evoionosp.abstrak.presentation.R
import org.evoionosp.abstrak.presentation.designsystem.component.button.LoadingButton
import org.evoionosp.abstrak.presentation.designsystem.component.card.TonalCard
import org.evoionosp.abstrak.presentation.designsystem.theme.AbstrakShapes
import org.evoionosp.abstrak.presentation.designsystem.theme.AbstrakSpacing

@Composable
fun CredentialsCard(
    state: AuthUiState,
    onAction: (AuthAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val canLogin = !state.isChecking && !state.isLoggingIn
    TonalCard(
        modifier = modifier.fillMaxWidth(),
        shadowElevation = 3.dp,
        contentPadding = PaddingValues(AbstrakSpacing.content),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.auth_credentials_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        OutlinedTextField(
            value = state.username,
            onValueChange = { onAction(AuthAction.UsernameChange(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.username_label)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            shape = AbstrakShapes.field,
        )
        OutlinedTextField(
            value = state.password,
            onValueChange = { onAction(AuthAction.PasswordChange(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.password_label)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
            keyboardActions =
                KeyboardActions(
                    onDone = {
                        keyboardController?.hide()
                        if (canLogin) onAction(AuthAction.Login)
                    },
                ),
            shape = AbstrakShapes.field,
        )
        LoadingButton(
            text = stringResource(R.string.login_button),
            loadingText = stringResource(R.string.logging_in),
            isLoading = state.isLoggingIn,
            onClick = {
                keyboardController?.hide()
                onAction(AuthAction.Login)
            },
            enabled = canLogin,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
