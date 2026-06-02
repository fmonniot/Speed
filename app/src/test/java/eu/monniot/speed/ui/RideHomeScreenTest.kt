package eu.monniot.speed.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import eu.monniot.speed.data.Units
import eu.monniot.speed.service.ServiceState
import eu.monniot.speed.ui.theme.RaceLoggerTheme
import eu.monniot.speed.util.LocalUnits
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Robolectric Compose test — runs in the JVM unit suite (no device). Establishes the pattern for
 * screen-level behaviour tests: render a real screen with controlled state and assert on
 * conditional rendering and click callbacks.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class RideHomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setScreen(state: ServiceState, onStop: () -> Unit = {}) {
        composeRule.setContent {
            RaceLoggerTheme {
                CompositionLocalProvider(LocalUnits provides Units.METRIC) {
                    RideHomeScreen(
                        serviceState = state,
                        gpsRateHz = 5,
                        lastRide = null,
                        thisWeekCount = 0,
                        lifetimeDistanceM = 0f,
                        onRecord = {},
                        onStop = onStop,
                        onOpenSummary = {},
                        onOpenTrips = {},
                        onOpenStats = {},
                    )
                }
            }
        }
    }

    @Test
    fun stopChip_isVisible_whenSensorsEnabled() {
        setScreen(ServiceState(isSensorsEnabled = true))
        composeRule.onNodeWithContentDescription("Stop sensors").assertIsDisplayed()
    }

    @Test
    fun stopChip_isHidden_whenSensorsDisabled() {
        setScreen(ServiceState(isSensorsEnabled = false))
        composeRule.onNodeWithContentDescription("Stop sensors").assertDoesNotExist()
    }

    @Test
    fun stopChip_firesOnStop_whenClicked() {
        var stopped = false
        setScreen(ServiceState(isSensorsEnabled = true)) { stopped = true }
        composeRule.onNodeWithContentDescription("Stop sensors").performClick()
        assertTrue("onStop should be invoked when the Stop chip is clicked", stopped)
    }
}
