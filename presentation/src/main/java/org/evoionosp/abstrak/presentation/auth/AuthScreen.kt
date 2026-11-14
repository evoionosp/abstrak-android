package org.evoionosp.abstrak.presentation.auth
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.evoionosp.abstrak.presentation.R
import org.evoionosp.abstrak.presentation.designsystem.theme.AbstrakSpacing
import org.evoionosp.abstrak.presentation.designsystem.theme.screenBackgroundBrush

sealed interface AuthAction {
    data class ProtocolChange(
        val protocol: String,
    ) : AuthAction

    data class UrlChange(
        val url: String,
    ) : AuthAction

    data object CheckServer : AuthAction

    data object EditServer : AuthAction

    data class UsernameChange(
        val username: String,
    ) : AuthAction

    data class PasswordChange(
        val password: String,
    ) : AuthAction

    data object Login : AuthAction
}

@Composable
fun AuthRoute(
    onOpenSettings: () -> Unit,
    onOpenServerConnection: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AuthScreen(
        state = state,
        onAction = { action ->
            when (action) {
                is AuthAction.ProtocolChange -> viewModel.onProtocolChange(action.protocol)
                is AuthAction.UrlChange -> viewModel.onBaseUrlChange(action.url)
                AuthAction.CheckServer -> viewModel.checkLoginSetup()
                AuthAction.EditServer -> viewModel.clearServerState()
                is AuthAction.UsernameChange -> viewModel.onUsernameChange(action.username)
                is AuthAction.PasswordChange -> viewModel.onPasswordChange(action.password)
                AuthAction.Login -> viewModel.login()
            }
        },
        onOpenSettings = onOpenSettings,
        onOpenServerConnection = onOpenServerConnection,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    state: AuthUiState,
    onAction: (AuthAction) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenServerConnection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.ime)
                .navigationBarsPadding()
                .background(screenBackgroundBrush())
                .verticalScroll(rememberScrollState()),
    ) {
        TopAppBar(
            title = { Text(text = stringResource(R.string.auth_app_title)) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            actions = {
                FilledTonalIconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.padding(end = 8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = stringResource(R.string.settings_icon_desc),
                    )
                }
            },
        )
        Column(
            modifier = Modifier.padding(horizontal = AbstrakSpacing.content, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(AbstrakSpacing.item),
        ) {
            WelcomeHeader()
            if (state.serverStatus == null) {
                ConnectToServerCard(state = state, onAction = onAction)
            } else {
                ServerStatusCard(state = state, onEdit = { onAction(AuthAction.EditServer) })
            }
            if (state.showLoginFields) {
                CredentialsCard(state = state, onAction = onAction)
            } else {
                Text(
                    text = stringResource(R.string.auth_server_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
            if (state.serverStatus == null) {
                TextButton(
                    onClick = onOpenServerConnection,
                    contentPadding = PaddingValues(horizontal = 4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.auth_trouble_connecting),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline,
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
