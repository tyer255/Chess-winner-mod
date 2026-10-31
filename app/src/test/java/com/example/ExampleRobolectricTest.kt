package com.example

import android.Manifest
import android.app.Application
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import com.example.chess.core.Piece
import com.example.chess.core.PieceColor
import com.example.chess.core.PieceType
import com.example.chess.core.Square
import com.example.viewmodel.ChessWinnerViewModel
import com.example.viewmodel.ModuleReadinessCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun grantCameraPermissionForRobolectric() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.CAMERA)
    }

    private fun waitForEngineBestMove(viewModel: ChessWinnerViewModel, expectedFen: String) {
        composeTestRule.waitUntil(timeoutMillis = 5000L) {
            val state = viewModel.engineAnalysis.value
            !state.isAnalyzing && state.fen == expectedFen && state.bestMoveUci != null && state.error == null
        }
    }

    @Test
    fun `app launches with Chess Winner branding and verifies 7-module configuration truth (Test L)`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Chess Winner", context.getString(R.string.app_name))

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("home_screen").assertIsDisplayed()
        composeTestRule.onNodeWithText("CHESS WINNER").assertIsDisplayed()

        // Navigate to Camera Chess Mode -> Opens SELECT YOUR COLOR
        composeTestRule.onNodeWithTag("home_camera_chess").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("color_config_screen").assertIsDisplayed()
        composeTestRule.onNodeWithText("SELECT YOUR COLOR").assertIsDisplayed()

        // Select Black Pieces and press Start
        composeTestRule.onNodeWithTag("select_black_pieces").performClick()
        composeTestRule.onNodeWithTag("start_session_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("configuration_all_set_banner").assertIsDisplayed()
        composeTestRule.onNodeWithText("Configuration All Set").assertIsDisplayed()

        val vm = ViewModelProvider(composeTestRule.activity)[ChessWinnerViewModel::class.java]
        val vmReport = vm.systemInitReport.value
        assertEquals(7, vmReport.modules.size)
        assertTrue(vmReport.cameraReady)
        assertTrue(vmReport.boardGeometryDetectionReady)
        // Mandatory Test L: Missing TFLite weights must never be reported as trained recognition READY
        assertFalse("Trained TFLite model is missing so trainedPieceRecognitionReady must be false", vmReport.trainedPieceRecognitionReady)
        assertFalse("Full automatic recognition must not be reported ready when model is missing", vmReport.isFullAutomaticRecognitionReady)
        assertEquals(ModuleReadinessCategory.INCOMPLETE, vmReport.modules[2].readinessCategory)
        assertEquals(ModuleReadinessCategory.REQUIRES_USER_INPUT, vmReport.modules[3].readinessCategory)
        assertTrue(vmReport.manualCalibrationAvailable)
        assertTrue(vmReport.stockfishReady)
        assertTrue(vmReport.chessRulesReady)
        assertTrue(vmReport.databaseReady)
        assertTrue(vmReport.nextActionGuidance.isNotBlank())
    }

    @Test
    fun `Mandatory Tests A through K - Turn synchronization, orientation independence, uncertain scan suppression, manual correction, engine analysis UI, and invalid position rejection`() {
        val vm = ViewModelProvider(composeTestRule.activity)[ChessWinnerViewModel::class.java]

        // Enter Camera Chess Mode via Configuration with WHITE selected
        composeTestRule.onNodeWithTag("home_camera_chess").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("select_white_pieces").performClick()
        composeTestRule.onNodeWithTag("start_session_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("enter_session_after_init_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("camera_chess_screen").assertIsDisplayed()

        // --- Mandatory Test G: Uncertain camera scan must not generate a recommendation ---
        val checkerFrame = ByteArray(128 * 128)
        for (y in 0 until 128) {
            for (x in 0 until 128) {
                val isLight = ((x / 16) + (y / 16)) % 2 == 0
                checkerFrame[y * 128 + x] = (if (isLight) 220 else 40).toByte()
            }
        }
        vm.processCameraLuminanceFrame(checkerFrame, 128, 128, bypassThrottle = true)
        composeTestRule.waitForIdle()
        assertFalse("Uncertain scan must have isPieceRecognitionConfident = false", vm.visionAnalysisResult.value.isPieceRecognitionConfident)
        assertNull("Uncertain camera scan must not produce user recommendation", vm.getCameraUserRecommendedMoveUci())
        assertNull("Uncertain camera scan must clear engine bestMoveUci", vm.engineAnalysis.value.bestMoveUci)

        // --- Confirm starting board calibration in Camera Mode ---
        val validation = vm.confirmBoardPieceCalibration()
        assertTrue("Confirmed starting board must be valid", validation.isValid)
        composeTestRule.waitForIdle()

        // --- Mandatory Test A: WHITE selected + White to move = YOUR TURN ---
        assertEquals(PieceColor.WHITE, vm.playerSide.value)
        assertEquals(PieceColor.WHITE, vm.sideToMove.value)
        assertEquals("YOUR TURN", vm.getTurnStatusLabel())
        composeTestRule.onNodeWithTag("camera_chess_screen")
            .performScrollToNode(hasTestTag("camera_turn_sync_card"))
        composeTestRule.onNodeWithTag("camera_turn_status_badge").assertTextContains("YOUR TURN")

        // --- Mandatory Test I & J: A validated position must reach the engine and results must reach the camera analysis UI ---
        val initialFen = vm.boardState.value.toFen()
        waitForEngineBestMove(vm, initialFen)
        composeTestRule.waitForIdle()

        val whiteRecommendedUci = vm.getCameraUserRecommendedMoveUci()
        val whiteRecommendedSan = vm.getCameraUserRecommendedMoveSan()
        assertNotNull("Validated WHITE turn position must have recommended UCI move", whiteRecommendedUci)
        assertNotNull("Validated WHITE turn position must have recommended SAN move", whiteRecommendedSan)
        composeTestRule.onNodeWithTag("camera_chess_screen")
            .performScrollToNode(hasTestTag("camera_engine_analysis_card"))
        composeTestRule.onNodeWithTag("camera_recommended_move_text").assertIsDisplayed()
        composeTestRule.onNodeWithTag("camera_evaluation_text").assertIsDisplayed()

        // Confirm a subsequent live camera frame does NOT wipe out the confirmed board or recommendation
        vm.processCameraLuminanceFrame(checkerFrame, 128, 128, bypassThrottle = true)
        composeTestRule.waitForIdle()
        assertTrue("Confirmed calibration must stay confident across subsequent frames", vm.visionAnalysisResult.value.isPieceRecognitionConfident)
        assertNotNull("Recommendation must persist across subsequent camera frames", vm.getCameraUserRecommendedMoveUci())

        // --- Mandatory Test E: Rotating board orientation must not change playerSide ---
        val orientationBefore = vm.boardOrientation.value
        vm.flipBoardOrientation()
        composeTestRule.waitForIdle()
        assertFalse("Board orientation must flip", orientationBefore == vm.boardOrientation.value)
        assertEquals("Rotating board orientation must NEVER change playerSide", PieceColor.WHITE, vm.playerSide.value)
        assertEquals("Rotating board orientation must NEVER change sideToMove", PieceColor.WHITE, vm.sideToMove.value)

        // --- Mandatory Test F & B: A confirmed legal move must update sideToMove; WHITE selected + Black to move = OPPONENT'S TURN ---
        val e2 = Square.fromAlgebraic("e2")!!
        val e4 = Square.fromAlgebraic("e4")!!
        val playedMove = vm.confirmMoveInCamera(e2, e4)
        assertNotNull("Legal move e2e4 must be confirmed in camera mode", playedMove)
        composeTestRule.waitForIdle()

        assertEquals("Confirmed legal move must update sideToMove to BLACK", PieceColor.BLACK, vm.sideToMove.value)
        assertEquals("Player side must remain WHITE", PieceColor.WHITE, vm.playerSide.value)
        assertEquals("OPPONENT'S TURN", vm.getTurnStatusLabel())
        composeTestRule.onNodeWithTag("camera_chess_screen")
            .performScrollToNode(hasTestTag("camera_turn_sync_card"))
        composeTestRule.onNodeWithTag("camera_turn_status_badge").assertTextContains("OPPONENT'S TURN")
        assertNull("Opponent's move must NEVER be returned as the user's recommendation", vm.getCameraUserRecommendedMoveUci())

        // --- Mandatory Test C & D: BLACK selected + White to move = OPPONENT'S TURN; BLACK selected + Black to move = YOUR TURN ---
        vm.selectPlayerColor(PieceColor.BLACK)
        composeTestRule.waitForIdle()
        // Right now sideToMove is BLACK (after 1. e4) and playerSide is BLACK -> Test D: YOUR TURN!
        assertEquals(PieceColor.BLACK, vm.playerSide.value)
        assertEquals(PieceColor.BLACK, vm.sideToMove.value)
        assertEquals("YOUR TURN", vm.getTurnStatusLabel())
        composeTestRule.onNodeWithTag("camera_chess_screen")
            .performScrollToNode(hasTestTag("camera_turn_sync_card"))
        composeTestRule.onNodeWithTag("camera_turn_status_badge").assertTextContains("YOUR TURN")

        val afterE4Fen = vm.boardState.value.toFen()
        waitForEngineBestMove(vm, afterE4Fen)
        composeTestRule.waitForIdle()
        assertNotNull("BLACK player on Black's turn must receive a recommended move", vm.getCameraUserRecommendedMoveUci())
        composeTestRule.onNodeWithTag("camera_chess_screen")
            .performScrollToNode(hasTestTag("camera_engine_analysis_card"))
        composeTestRule.onNodeWithTag("camera_recommended_move_text").assertIsDisplayed()

        // Switch sideToMove to WHITE while BLACK is selected -> Test C: OPPONENT'S TURN!
        vm.setCameraSideToMove(PieceColor.WHITE)
        composeTestRule.waitForIdle()
        assertEquals(PieceColor.BLACK, vm.playerSide.value)
        assertEquals(PieceColor.WHITE, vm.sideToMove.value)
        assertEquals("OPPONENT'S TURN", vm.getTurnStatusLabel())
        composeTestRule.onNodeWithTag("camera_chess_screen")
            .performScrollToNode(hasTestTag("camera_turn_sync_card"))
        composeTestRule.onNodeWithTag("camera_turn_status_badge").assertTextContains("OPPONENT'S TURN")
        assertNull("BLACK player on White's turn must not receive White's move as their recommendation", vm.getCameraUserRecommendedMoveUci())

        // --- Mandatory Test K: An invalid position must not produce a best-move recommendation ---
        vm.clearCameraCalibrationBoard()
        composeTestRule.waitForIdle()
        assertFalse("Cleared empty board must be invalid", vm.visionAnalysisResult.value.validationResult?.isValid == true)
        assertNull("Invalid position must not return user recommendation", vm.getCameraUserRecommendedMoveUci())
        assertNull("Invalid position must clear engine bestMoveUci", vm.engineAnalysis.value.bestMoveUci)

        // --- Mandatory Test H: Manual square correction must produce a validated position when the correction is legal ---
        vm.selectPlayerColor(PieceColor.WHITE)
        vm.setCameraSideToMove(PieceColor.WHITE)
        vm.applyManualPieceOverrideInCamera(Square.fromAlgebraic("e1")!!, Piece(PieceType.KING, PieceColor.WHITE))
        vm.applyManualPieceOverrideInCamera(Square.fromAlgebraic("d1")!!, Piece(PieceType.ROOK, PieceColor.WHITE))
        vm.applyManualPieceOverrideInCamera(Square.fromAlgebraic("g8")!!, Piece(PieceType.KING, PieceColor.BLACK))
        val manualConfirmResult = vm.confirmBoardPieceCalibration()
        assertTrue("Manual square correction with 1 White King, 1 White Rook, and 1 Black King must validate", manualConfirmResult.isValid)
        composeTestRule.waitForIdle()

        val customFen = vm.boardState.value.toFen()
        waitForEngineBestMove(vm, customFen)
        composeTestRule.waitForIdle()
        assertNotNull("Validated manually corrected position must produce an engine recommendation", vm.getCameraUserRecommendedMoveUci())
    }
}
