package org.evoionosp.abstrak.appicon
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.evoionosp.abstrak.domain.settings.AppSettingsStore
import org.evoionosp.abstrak.presentation.launcher.LauncherIconSwitcher

/**
 * Puts the app icon chosen in Settings on the launcher whenever the app goes to the background,
 * and for any change saved while it is there. The home screen is out of sight while the app is
 * open, and switching launcher entries can close the app on some devices, so nothing changes
 * while it is in front, however often the choice changes. A process started without ever coming
 * to the front (a background job) changes nothing either.
 */
@Singleton
class AppIconCoordinator
    @Inject
    constructor(
        @param:Named("application_scope") private val scope: CoroutineScope,
        private val appSettingsStore: AppSettingsStore,
        private val launcherIconSwitcher: LauncherIconSwitcher,
    ) {
        private val inBackground = MutableStateFlow(false)
        private var started = false

        fun start(processLifecycle: Lifecycle = ProcessLifecycleOwner.get().lifecycle) {
            if (started) return
            started = true
            processLifecycle.addObserver(
                object : DefaultLifecycleObserver {
                    override fun onStart(owner: LifecycleOwner) {
                        inBackground.value = false
                    }

                    override fun onStop(owner: LifecycleOwner) {
                        inBackground.value = true
                    }
                },
            )
            scope.launch {
                combine(
                    inBackground,
                    appSettingsStore.settings.map { it.appIcon }.distinctUntilChanged(),
                ) { background, icon ->
                    icon.takeIf { background }
                }.filterNotNull()
                    .collect(launcherIconSwitcher::apply)
            }
        }
    }
