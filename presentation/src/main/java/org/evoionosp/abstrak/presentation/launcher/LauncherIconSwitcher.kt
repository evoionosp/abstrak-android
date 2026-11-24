package org.evoionosp.abstrak.presentation.launcher
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import org.evoionosp.abstrak.domain.settings.AppIcon

/**
 * Puts an app icon on the launcher. Each icon has its own launcher entry, an activity-alias of
 * MainActivity declared in AndroidManifest.xml; this enables the chosen one and disables the rest.
 *
 * Switching makes the launcher redraw the icon, and on some devices closes the app or drops it
 * from Recents, so it is meant to run while the app is in the background.
 */
class LauncherIconSwitcher
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) {
        /** The icon whose launcher entry is enabled. */
        fun current(): AppIcon = AppIcon.entries.firstOrNull(::isEnabled) ?: AppIcon.DEFAULT

        /** Makes [icon] the app's only launcher entry. Does nothing when it already is. */
        fun apply(icon: AppIcon) {
            val changes =
                AppIcon.entries
                    .filter { isEnabled(it) != (it == icon) }
                    // Enable the new entry before disabling the old one, so there is always one.
                    .sortedByDescending { it == icon }
            changes.forEach { entry ->
                context.packageManager.setComponentEnabledSetting(
                    componentOf(entry),
                    if (entry == icon) {
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                    } else {
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                    },
                    PackageManager.DONT_KILL_APP,
                )
            }
        }

        private fun isEnabled(icon: AppIcon): Boolean =
            when (context.packageManager.getComponentEnabledSetting(componentOf(icon))) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true

                // Never switched: as declared in the manifest, where only the default is enabled.
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> icon == AppIcon.DEFAULT

                else -> false
            }

        private fun componentOf(icon: AppIcon) = ComponentName(context.packageName, launcherEntryName(icon))

        companion object {
            /** The class name of [icon]'s activity-alias in AndroidManifest.xml. */
            fun launcherEntryName(icon: AppIcon): String =
                "org.evoionosp.abstrak.presentation.launcher." +
                    when (icon) {
                        AppIcon.ISLAND_LIGHT -> "IslandLight"
                        AppIcon.ISLAND_DARK -> "IslandDark"
                        AppIcon.FOREST_LIGHT -> "ForestLight"
                        AppIcon.FOREST_DARK -> "ForestDark"
                    }
        }
    }
