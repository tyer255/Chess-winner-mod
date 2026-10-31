package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `app launches with Chess Winner branding and navigates through Color Selection`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Chess Winner", context.getString(R.string.app_name))

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("home_screen").assertIsDisplayed()
        composeTestRule.onNodeWithText("CHESS WINNER").assertIsDisplayed()

        // Navigate to Start Chess Analysis -> Opens SELECT YOUR COLOR
        composeTestRule.onNodeWithTag("home_start_analysis").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("color_config_screen").assertIsDisplayed()
        composeTestRule.onNodeWithText("SELECT YOUR COLOR").assertIsDisplayed()

        // Select Black Pieces and press Start
        composeTestRule.onNodeWithTag("select_black_pieces").performClick()
        composeTestRule.onNodeWithTag("start_session_button").performClick()
        composeTestRule.waitForIdle()

        // Verify "Configuration All Set" banner appears after module checks pass,
        // and explicitly reports that automatic TFLite piece recognition is incomplete while manual calibration is active
        composeTestRule.onNodeWithTag("configuration_all_set_banner").assertIsDisplayed()
        composeTestRule.onNodeWithText("Configuration All Set").assertIsDisplayed()

        val vmReport = composeTestRule.activity.let { activity ->
            androidx.lifecycle.ViewModelProvider(activity)[com.example.viewmodel.ChessWinnerViewModel::class.java]
                .systemInitReport.value
        }
        assertEquals(7, vmReport.modules.size)
        org.junit.Assert.assertTrue(vmReport.boardGeometryDetectionReady)
        org.junit.Assert.assertFalse("Trained TFLite model is missing so trainedPieceRecognitionReady must be false", vmReport.trainedPieceRecognitionReady)
        org.junit.Assert.assertFalse("Full automatic recognition must not be reported ready when model is missing", vmReport.isFullAutomaticRecognitionReady)
        org.junit.Assert.assertTrue(vmReport.manualCalibrationAvailable)
        org.junit.Assert.assertTrue(vmReport.stockfishReady)
        org.junit.Assert.assertTrue(vmReport.chessRulesReady)
        org.junit.Assert.assertTrue(vmReport.databaseReady)
    }
}
