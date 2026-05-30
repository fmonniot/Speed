package eu.monniot.speed

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import eu.monniot.speed.data.RaceDatabase
import eu.monniot.speed.data.Session
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        Intents.init()
        // Insert a dummy session to test navigation to details
        val db = RaceDatabase.getDatabase(composeTestRule.activity)
        runBlocking {
            db.dataPointDao().insertSession(
                Session(
                    sessionId = "test-session-123",
                    startTimeMs = System.currentTimeMillis(),
                    endTimeMs = System.currentTimeMillis() + 1000,
                    pointCount = 10,
                    maxSpeedMs = 25.0f,
                    notes = "Test Session"
                )
            )
        }
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun testBottomNavigation() {
        // Start at Race screen
        composeTestRule.onNodeWithText("START RACE").assertIsDisplayed()

        // Navigate to Sessions
        composeTestRule.onNodeWithContentDescription("Sessions").performClick()
        composeTestRule.onNodeWithText("Past Sessions").assertIsDisplayed()

        // Navigate to Settings
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.onNodeWithText("Recording Settings").assertIsDisplayed()

        // Navigate back to Race
        composeTestRule.onNodeWithContentDescription("Race").performClick()
        composeTestRule.onNodeWithText("START RACE").assertIsDisplayed()
    }

    @Test
    fun testSessionDetailAndExportNavigation() {
        // Navigate to Sessions
        composeTestRule.onNodeWithContentDescription("Sessions").performClick()
        
        // Wait for the session to appear and click it
        composeTestRule.onNodeWithText("ID: test-session-123").performClick()
        
        // Verify we are on the Detail screen
        composeTestRule.onNodeWithText("Session Details").assertIsDisplayed()
        composeTestRule.onNodeWithText("Test Session").assertIsDisplayed()
        
        // Test Export Action
        composeTestRule.onNodeWithContentDescription("Export CSV").performClick()
        
        // Verify that the share intent was sent
        intended(hasAction(Intent.ACTION_CHOOSER))
        
        // Go back
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.onNodeWithText("Past Sessions").assertIsDisplayed()
    }
}
