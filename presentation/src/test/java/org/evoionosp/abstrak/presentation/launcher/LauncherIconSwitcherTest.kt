package org.evoionosp.abstrak.presentation.launcher
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import org.evoionosp.abstrak.domain.settings.AppIcon
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LauncherIconSwitcherTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val packageManager = context.packageManager
    private val switcher = LauncherIconSwitcher(context)

    @Test
    fun `every icon has a launcher entry that opens MainActivity`() {
        AppIcon.entries.forEach { icon ->
            val entry = packageManager.getActivityInfo(componentOf(icon), PackageManager.MATCH_DISABLED_COMPONENTS)
            assertEquals("org.evoionosp.abstrak.presentation.MainActivity", entry.targetActivity)
        }
    }

    @Test
    fun `the launcher shows the default icon until one is applied`() {
        assertEquals(AppIcon.DEFAULT, switcher.current())
        assertEquals(listOf(AppIcon.DEFAULT), launcherEntries())
    }

    @Test
    fun `applying an icon leaves its entry as the only one on the launcher`() {
        switcher.apply(AppIcon.FOREST_DARK)
        assertEquals(AppIcon.FOREST_DARK, switcher.current())
        assertEquals(listOf(AppIcon.FOREST_DARK), launcherEntries())
        assertEquals(PackageManager.COMPONENT_ENABLED_STATE_DISABLED, stateOf(AppIcon.ISLAND_LIGHT))
    }

    @Test
    fun `switching icons again keeps exactly one entry, the default included`() {
        switcher.apply(AppIcon.ISLAND_DARK)
        switcher.apply(AppIcon.FOREST_LIGHT)
        assertEquals(listOf(AppIcon.FOREST_LIGHT), launcherEntries())
        switcher.apply(AppIcon.ISLAND_LIGHT)
        assertEquals(listOf(AppIcon.ISLAND_LIGHT), launcherEntries())
        assertEquals(AppIcon.ISLAND_LIGHT, switcher.current())
    }

    @Test
    fun `applying the icon already shown changes nothing`() {
        switcher.apply(AppIcon.DEFAULT)
        AppIcon.entries.forEach { assertEquals(PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, stateOf(it)) }
    }

    private fun launcherEntries(): List<AppIcon> {
        val launch = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(context.packageName)
        val names = packageManager.queryIntentActivities(launch, 0).map { it.activityInfo.name }
        return AppIcon.entries.filter { LauncherIconSwitcher.launcherEntryName(it) in names }
    }

    private fun stateOf(icon: AppIcon): Int = packageManager.getComponentEnabledSetting(componentOf(icon))

    private fun componentOf(icon: AppIcon) = ComponentName(context.packageName, LauncherIconSwitcher.launcherEntryName(icon))
}
