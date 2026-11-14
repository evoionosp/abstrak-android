package org.evoionosp.abstrak.presentation.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.evoionosp.abstrak.domain.auth.model.AuthError
import org.evoionosp.abstrak.domain.auth.model.LoginData
import org.evoionosp.abstrak.domain.auth.model.LoginResult
import org.evoionosp.abstrak.domain.auth.repository.AuthRepository
import org.evoionosp.abstrak.domain.auth.usecase.LoginUseCase
import org.evoionosp.abstrak.domain.server.model.ServerCheckResult
import org.evoionosp.abstrak.domain.server.model.ServerError
import org.evoionosp.abstrak.domain.server.model.ServerStatus
import org.evoionosp.abstrak.domain.server.repository.ServerRepository
import org.evoionosp.abstrak.domain.server.usecase.ServerHealthCheckUseCase
import org.evoionosp.abstrak.domain.server.usecase.ServerPingUseCase
import org.evoionosp.abstrak.domain.session.LoginSession
import org.evoionosp.abstrak.domain.session.SessionStore
import org.evoionosp.abstrak.domain.session.usecase.ObserveLastServerUrlUseCase
import org.evoionosp.abstrak.domain.session.usecase.SaveSessionUseCase
import org.evoionosp.abstrak.presentation.R
import org.evoionosp.abstrak.presentation.designsystem.component.snackbar.SnackbarType
import org.evoionosp.abstrak.presentation.ui.message.SnackbarController
import org.evoionosp.abstrak.presentation.ui.message.SnackbarMessage
import org.evoionosp.abstrak.presentation.ui.message.UiText
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelServerCheckTest {
    private val dispatcher = StandardTestDispatcher()
    private val snackbar = SnackbarController()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `unresolvable server url shows the server not found error`() =
        runTest(dispatcher) {
            val viewModel = viewModel(pingResult = ServerCheckResult.Failure(ServerError.SERVER_NOT_FOUND))
            advanceUntilIdle()
            val messages = mutableListOf<SnackbarMessage>()
            val collection = launch { snackbar.messages.collect { messages.add(it) } }
            viewModel.onBaseUrlChange("audiobooks.evoionosp.com")
            viewModel.checkLoginSetup()
            advanceUntilIdle()
            collection.cancel()
            advanceUntilIdle()
            assertEquals(
                listOf(SnackbarMessage(UiText.Resource(R.string.error_server_not_found), SnackbarType.ERROR)),
                messages,
            )
        }

    @Test
    fun `successful login keeps progress visible and shows no message`() =
        runTest(dispatcher) {
            val viewModel =
                viewModel(
                    pingResult = ServerCheckResult.Failure(ServerError.UNKNOWN),
                    loginResult =
                        LoginResult.Success(
                            LoginData(accessToken = "token", refreshToken = null, userId = "u1"),
                        ),
                )
            advanceUntilIdle()
            val messages = mutableListOf<SnackbarMessage>()
            val collection = launch { snackbar.messages.collect { messages.add(it) } }
            viewModel.onUsernameChange("user")
            viewModel.onPasswordChange("pass")
            viewModel.login()
            advanceUntilIdle()
            collection.cancel()
            advanceUntilIdle()
            assertEquals(emptyList<SnackbarMessage>(), messages)
            assertEquals(true, viewModel.uiState.value.isLoggingIn)
        }

    @Test
    fun `failed login restores the button and shows the error`() =
        runTest(dispatcher) {
            val viewModel =
                viewModel(
                    pingResult = ServerCheckResult.Failure(ServerError.UNKNOWN),
                    loginResult = LoginResult.Failure(AuthError.HTTP),
                )
            advanceUntilIdle()
            val messages = mutableListOf<SnackbarMessage>()
            val collection = launch { snackbar.messages.collect { messages.add(it) } }
            viewModel.onUsernameChange("user")
            viewModel.onPasswordChange("wrong")
            viewModel.login()
            advanceUntilIdle()
            collection.cancel()
            advanceUntilIdle()
            assertEquals(
                listOf(SnackbarMessage(UiText.Resource(R.string.error_login_failed), SnackbarType.ERROR)),
                messages,
            )
            assertEquals(false, viewModel.uiState.value.isLoggingIn)
        }

    private fun viewModel(
        pingResult: ServerCheckResult<Boolean>,
        loginResult: LoginResult = LoginResult.Failure(AuthError.UNEXPECTED),
    ): AuthViewModel {
        val sessionStore = ServerCheckFakeSessionStore()
        val serverRepository = ServerCheckFakeServerRepository(pingResult)
        val authRepository = ServerCheckFakeAuthRepository(loginResult)
        return AuthViewModel(
            serverPingUseCase = ServerPingUseCase(serverRepository),
            serverHealthCheckUseCase = ServerHealthCheckUseCase(serverRepository),
            loginUseCase = LoginUseCase(authRepository),
            saveSessionUseCase = SaveSessionUseCase(sessionStore),
            observeLastServerUrlUseCase = ObserveLastServerUrlUseCase(sessionStore),
            snackbar = snackbar,
        )
    }
}

private class ServerCheckFakeSessionStore : SessionStore {
    private val sessionFlow = MutableStateFlow<LoginSession?>(null)
    private val lastServerFlow = MutableStateFlow<String?>(null)
    override val session: Flow<LoginSession?> = sessionFlow
    override val lastServerUrl: Flow<String?> = lastServerFlow

    override suspend fun saveSession(session: LoginSession): LoginSession {
        sessionFlow.value = session
        lastServerFlow.value = session.baseUrl
        return session
    }

    override suspend fun clearSession() {
        sessionFlow.value = null
    }
}

private class ServerCheckFakeServerRepository(
    private val pingResult: ServerCheckResult<Boolean>,
) : ServerRepository {
    override suspend fun ping(baseUrl: String): ServerCheckResult<Boolean> = pingResult

    override suspend fun getStatus(baseUrl: String): ServerCheckResult<ServerStatus> = ServerCheckResult.Failure(ServerError.UNKNOWN)

    override suspend fun healthCheck(baseUrl: String): ServerCheckResult<Boolean> = ServerCheckResult.Failure(ServerError.UNKNOWN)
}

private class ServerCheckFakeAuthRepository(
    private val loginResult: LoginResult,
) : AuthRepository {
    override suspend fun login(
        baseUrl: String,
        username: String,
        password: String,
    ): LoginResult = loginResult

    override suspend fun refreshSession(
        baseUrl: String,
        refreshToken: String,
    ): LoginResult = LoginResult.Failure(AuthError.UNEXPECTED)
}
