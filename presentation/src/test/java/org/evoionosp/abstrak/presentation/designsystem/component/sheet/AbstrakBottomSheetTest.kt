package org.evoionosp.abstrak.presentation.designsystem.component.sheet
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AbstrakBottomSheetTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `a sheet left half open in portrait opens again after turning to landscape`() {
        var isLandscape = false
        val restoration = StateRestorationTester(composeRule)
        restoration.setContent {
            val configuration =
                Configuration(LocalConfiguration.current).apply {
                    orientation =
                        if (isLandscape) Configuration.ORIENTATION_LANDSCAPE else Configuration.ORIENTATION_PORTRAIT
                }
            CompositionLocalProvider(LocalConfiguration provides configuration) {
                AbstrakBottomSheet(onDismiss = {}, title = TITLE) {
                    Spacer(modifier = Modifier.fillMaxWidth().height(2_000.dp))
                }
            }
        }
        val halfOpenTop = composeRule.onNodeWithText(TITLE).getUnclippedBoundsInRoot().top
        assertTrue("expected the sheet half open, its title at $halfOpenTop", halfOpenTop > 150.dp)
        isLandscape = true
        restoration.emulateSavedInstanceStateRestore()
        val landscapeTop = composeRule.onNodeWithText(TITLE).getUnclippedBoundsInRoot().top
        assertTrue("expected the sheet fully open, its title at $landscapeTop", landscapeTop < halfOpenTop)
    }

    private companion object {
        const val TITLE = "Sheet title"
    }
}
