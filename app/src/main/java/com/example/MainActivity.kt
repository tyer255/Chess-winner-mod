package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.CameraChessScreen
import com.example.ui.screens.ChessLearningScreen
import com.example.ui.screens.ColorConfigurationScreen
import com.example.ui.screens.DigitalChessAndAnalysisScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ImportExportScreen
import com.example.ui.screens.SavedGamesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.CharcoalCard
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DeepCyanContainer
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NavySurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.AppDestination
import com.example.viewmodel.ChessWinnerViewModel
import com.example.viewmodel.SessionLaunchTarget
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // STEP 1: Configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val chessViewModel: ChessWinnerViewModel = viewModel()
                ChessWinnerApp(viewModel = chessViewModel)
            }
        }
    }
}

@Composable
fun ChessWinnerApp(
    viewModel: ChessWinnerViewModel,
    modifier: Modifier = Modifier
) {
    val currentDestination by viewModel.currentDestination.collectAsStateWithLifecycle()
    val boardState by viewModel.boardState.collectAsStateWithLifecycle()
    val selectedSquare by viewModel.selectedSquare.collectAsStateWithLifecycle()
    val legalMoves by viewModel.legalMovesForSelected.collectAsStateWithLifecycle()
    val pendingPromotions by viewModel.pendingPromotionMoves.collectAsStateWithLifecycle()
    val orientation by viewModel.boardOrientation.collectAsStateWithLifecycle()
    val engineReady by viewModel.stockfishEngine.isReady.collectAsStateWithLifecycle()
    val engineAnalysis by viewModel.engineAnalysis.collectAsStateWithLifecycle()
    val tacticalExplanation by viewModel.tacticalExplanation.collectAsStateWithLifecycle()
    val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()
    val savedGames by viewModel.savedGames.collectAsStateWithLifecycle()
    val learningProgress by viewModel.learningProgress.collectAsStateWithLifecycle()
    val systemInitReport by viewModel.systemInitReport.collectAsStateWithLifecycle()
    val playAgainstEngine by viewModel.playAgainstEngine.collectAsStateWithLifecycle()

    val aiCoachingText by viewModel.aiCoachingText.collectAsStateWithLifecycle()
    val isAiCoachingLoading by viewModel.isAiCoachingLoading.collectAsStateWithLifecycle()
    val aiCoachingError by viewModel.aiCoachingError.collectAsStateWithLifecycle()

    val visionScanMode by viewModel.visionScanMode.collectAsStateWithLifecycle()
    val visionResult by viewModel.visionAnalysisResult.collectAsStateWithLifecycle()
    val calibratedCorners by viewModel.calibratedCorners.collectAsStateWithLifecycle()
    val isCameraPaused by viewModel.isCameraPaused.collectAsStateWithLifecycle()
    val useBackCamera by viewModel.useBackCamera.collectAsStateWithLifecycle()

    val activeLearningTab by viewModel.activeLearningTab.collectAsStateWithLifecycle()
    val selectedCheckmateLesson by viewModel.selectedCheckmateLesson.collectAsStateWithLifecycle()
    val checkmateFeedback by viewModel.checkmateExerciseFeedback.collectAsStateWithLifecycle()
    val selectedBlunderItem by viewModel.selectedBlunderItem.collectAsStateWithLifecycle()
    val selectedOpeningLesson by viewModel.selectedOpeningLesson.collectAsStateWithLifecycle()
    val openingStepIndex by viewModel.openingMoveStepIndex.collectAsStateWithLifecycle()

    val editorSquares by viewModel.editorSquares.collectAsStateWithLifecycle()
    val editorActiveColor by viewModel.editorActiveColor.collectAsStateWithLifecycle()
    val editorSelectedBrush by viewModel.editorSelectedBrush.collectAsStateWithLifecycle()
    val toastMessage by viewModel.statusBannerToast.collectAsStateWithLifecycle()

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(3200L)
            viewModel.dismissToast()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            NavigationBar(
                containerColor = NavySurface,
                windowInsets = WindowInsets.navigationBars
            ) {
                NavigationBarItem(
                    selected = currentDestination == AppDestination.HOME,
                    onClick = { viewModel.navigateTo(AppDestination.HOME) },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyanAccent,
                        selectedTextColor = CyanAccent,
                        indicatorColor = DeepCyanContainer,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("nav_home")
                )
                NavigationBarItem(
                    selected = currentDestination == AppDestination.DIGITAL_BOARD,
                    onClick = {
                        viewModel.openColorConfigurationForSession(SessionLaunchTarget.DIGITAL_CHESSBOARD)
                    },
                    icon = { Icon(Icons.Default.GridOn, contentDescription = "Digital Board") },
                    label = { Text("Play") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyanAccent,
                        selectedTextColor = CyanAccent,
                        indicatorColor = DeepCyanContainer,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("nav_play")
                )
                NavigationBarItem(
                    selected = currentDestination == AppDestination.CAMERA_CHESS,
                    onClick = {
                        viewModel.openColorConfigurationForSession(SessionLaunchTarget.CAMERA_CHESS)
                    },
                    icon = { Icon(Icons.Default.CameraAlt, contentDescription = "Camera Chess") },
                    label = { Text("Camera") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyanAccent,
                        selectedTextColor = CyanAccent,
                        indicatorColor = DeepCyanContainer,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("nav_camera")
                )
                NavigationBarItem(
                    selected = currentDestination == AppDestination.POSITION_ANALYSIS,
                    onClick = { viewModel.navigateTo(AppDestination.POSITION_ANALYSIS) },
                    icon = { Icon(Icons.Default.Analytics, contentDescription = "Analysis") },
                    label = { Text("Analysis") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyanAccent,
                        selectedTextColor = CyanAccent,
                        indicatorColor = DeepCyanContainer,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("nav_analysis")
                )
                NavigationBarItem(
                    selected = currentDestination == AppDestination.CHESS_LEARNING,
                    onClick = { viewModel.navigateTo(AppDestination.CHESS_LEARNING) },
                    icon = { Icon(Icons.Default.School, contentDescription = "Learn") },
                    label = { Text("Learn") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyanAccent,
                        selectedTextColor = CyanAccent,
                        indicatorColor = DeepCyanContainer,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("nav_learn")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentDestination) {
                AppDestination.HOME -> {
                    HomeScreen(
                        engineReady = engineReady,
                        savedGamesCount = savedGames.size,
                        onSelectSessionTarget = { target ->
                            viewModel.openColorConfigurationForSession(target)
                        },
                        onNavigate = { dest -> viewModel.navigateTo(dest) }
                    )
                }

                AppDestination.COLOR_CONFIG -> {
                    ColorConfigurationScreen(
                        report = systemInitReport,
                        onSelectColor = { viewModel.selectPlayerColor(it) },
                        onStartInitialization = { camGranted ->
                            viewModel.runSystemInitializationAndStart(camGranted)
                        },
                        onContinueToSession = { viewModel.proceedAfterConfigurationAllSet() },
                        onSwitchToDigitalFallback = {
                            viewModel.openColorConfigurationForSession(SessionLaunchTarget.DIGITAL_CHESSBOARD)
                        },
                        onBack = { viewModel.navigateBack() }
                    )
                }

                AppDestination.DIGITAL_BOARD -> {
                    DigitalChessAndAnalysisScreen(
                        isAnalysisFocusMode = false,
                        boardState = boardState,
                        orientation = orientation,
                        selectedSquare = selectedSquare,
                        legalMoves = legalMoves,
                        pendingPromotionMoves = pendingPromotions,
                        engineAnalysis = engineAnalysis,
                        tacticalExplanation = tacticalExplanation,
                        userSettings = userSettings,
                        playAgainstEngine = playAgainstEngine,
                        aiCoachingText = aiCoachingText,
                        isAiCoachingLoading = isAiCoachingLoading,
                        aiCoachingError = aiCoachingError,
                        onSquareTapped = { viewModel.onSquareTapped(it) },
                        onPieceDragged = { from, to -> viewModel.onPieceDragged(from, to) },
                        onSelectPromotion = { viewModel.onPromotionPieceSelected(it) },
                        onCancelPromotion = { viewModel.onPromotionCancelled() },
                        onUndoMove = { viewModel.undoMove() },
                        onRestartGame = { viewModel.restartGame() },
                        onFlipBoard = { viewModel.flipBoardOrientation() },
                        onTogglePlayAgainstEngine = { viewModel.togglePlayAgainstEngine(it) },
                        onPlayRecommendedMove = { viewModel.playRecommendedBestMove() },
                        onTriggerEngineAnalysis = { viewModel.stockfishEngine.analyzePosition(boardState) },
                        onStopEngineAnalysis = { viewModel.stockfishEngine.stopAnalysis() },
                        onSaveGame = { title -> viewModel.saveCurrentGameToDatabase(title, "Digital Chessboard") },
                        onRequestHighThinkingCoaching = { viewModel.requestHighThinkingAiExplanation() },
                        onShowToast = { viewModel.showToast(it) },
                        onBack = { viewModel.navigateBack() }
                    )
                }

                AppDestination.POSITION_ANALYSIS -> {
                    DigitalChessAndAnalysisScreen(
                        isAnalysisFocusMode = true,
                        boardState = boardState,
                        orientation = orientation,
                        selectedSquare = selectedSquare,
                        legalMoves = legalMoves,
                        pendingPromotionMoves = pendingPromotions,
                        engineAnalysis = engineAnalysis,
                        tacticalExplanation = tacticalExplanation,
                        userSettings = userSettings,
                        playAgainstEngine = false,
                        aiCoachingText = aiCoachingText,
                        isAiCoachingLoading = isAiCoachingLoading,
                        aiCoachingError = aiCoachingError,
                        onSquareTapped = { viewModel.onSquareTapped(it) },
                        onPieceDragged = { from, to -> viewModel.onPieceDragged(from, to) },
                        onSelectPromotion = { viewModel.onPromotionPieceSelected(it) },
                        onCancelPromotion = { viewModel.onPromotionCancelled() },
                        onUndoMove = { viewModel.undoMove() },
                        onRestartGame = { viewModel.restartGame() },
                        onFlipBoard = { viewModel.flipBoardOrientation() },
                        onTogglePlayAgainstEngine = {},
                        onPlayRecommendedMove = { viewModel.playRecommendedBestMove() },
                        onTriggerEngineAnalysis = { viewModel.stockfishEngine.analyzePosition(boardState) },
                        onStopEngineAnalysis = { viewModel.stockfishEngine.stopAnalysis() },
                        onSaveGame = { title -> viewModel.saveCurrentGameToDatabase(title, "Position Analysis") },
                        onRequestHighThinkingCoaching = { viewModel.requestHighThinkingAiExplanation() },
                        onShowToast = { viewModel.showToast(it) },
                        onBack = { viewModel.navigateBack() }
                    )
                }

                AppDestination.CAMERA_CHESS -> {
                    CameraChessScreen(
                        scanMode = visionScanMode,
                        visionResult = visionResult,
                        calibratedCorners = calibratedCorners,
                        isCameraPaused = isCameraPaused,
                        useBackCamera = useBackCamera,
                        orientation = orientation,
                        boardState = boardState,
                        engineAnalysis = engineAnalysis,
                        userSettings = userSettings,
                        onSetScanMode = { viewModel.setVisionScanMode(it) },
                        onCameraLifecycleState = { ready, err -> viewModel.onCameraLifecycleState(ready, err) },
                        onFrameLuminanceAvailable = { bytes, w, h ->
                            viewModel.processCameraLuminanceFrame(bytes, w, h)
                        },
                        onTogglePause = { viewModel.toggleCameraPause() },
                        onToggleCameraLens = { viewModel.toggleCameraLens() },
                        onFlipBoardOrientation = { viewModel.flipBoardOrientation() },
                        onUpdateCorners = { viewModel.updateCornerCalibration(it) },
                        onConfirmCalibration = { viewModel.confirmBoardPieceCalibration() },
                        onLoadStandardStartingBoard = { viewModel.loadStandardStartingBoardInCamera() },
                        onManualPieceOverride = { sq, piece ->
                            viewModel.applyManualPieceOverrideInCamera(sq, piece)
                        },
                        onOpenInFullAnalysis = { viewModel.openRecognizedCameraPositionInAnalysis() },
                        onBack = { viewModel.navigateBack() }
                    )
                }

                AppDestination.IMPORT_EXPORT -> {
                    ImportExportScreen(
                        currentBoardState = boardState,
                        editorSquares = editorSquares,
                        editorActiveColor = editorActiveColor,
                        editorSelectedBrush = editorSelectedBrush,
                        onImportFen = { viewModel.importFenString(it) },
                        onImportPgn = { viewModel.importPgnString(it) },
                        onImportScreenshotBitmap = { viewModel.processImportedScreenshotBitmap(it) },
                        onSelectEditorBrush = { viewModel.selectEditorBrush(it) },
                        onTapEditorSquare = { viewModel.tapEditorSquare(it) },
                        onSetEditorActiveColor = { viewModel.setEditorActiveColor(it) },
                        onClearEditorBoard = { viewModel.clearEditorBoard() },
                        onResetEditorBoard = { viewModel.resetEditorBoardToStart() },
                        onValidateEditorBoard = { viewModel.validateCurrentEditorBoard() },
                        onApplyEditorBoardAndAnalyze = { viewModel.applyEditorBoardAndAnalyze() },
                        onShowToast = { viewModel.showToast(it) },
                        onOpenAnalysis = { viewModel.navigateTo(AppDestination.POSITION_ANALYSIS) },
                        onBack = { viewModel.navigateBack() }
                    )
                }

                AppDestination.SAVED_GAMES -> {
                    SavedGamesScreen(
                        savedGames = savedGames,
                        onOpenGame = { viewModel.openSavedGame(it) },
                        onDeleteGame = { viewModel.deleteSavedGame(it) },
                        onSaveCurrentBoard = { title -> viewModel.saveCurrentGameToDatabase(title, "Saved Session") },
                        onShowToast = { viewModel.showToast(it) },
                        onBack = { viewModel.navigateBack() }
                    )
                }

                AppDestination.CHESS_LEARNING -> {
                    ChessLearningScreen(
                        activeTab = activeLearningTab,
                        boardState = boardState,
                        orientation = orientation,
                        selectedSquare = selectedSquare,
                        legalMoves = legalMoves,
                        engineAnalysis = engineAnalysis,
                        tacticalExplanation = tacticalExplanation,
                        selectedCheckmateLesson = selectedCheckmateLesson,
                        checkmateFeedback = checkmateFeedback,
                        selectedBlunderItem = selectedBlunderItem,
                        selectedOpeningLesson = selectedOpeningLesson,
                        openingStepIndex = openingStepIndex,
                        learningProgress = learningProgress,
                        userSettings = userSettings,
                        aiCoachingText = aiCoachingText,
                        isAiCoachingLoading = isAiCoachingLoading,
                        aiCoachingError = aiCoachingError,
                        onSelectTab = { viewModel.selectLearningTab(it) },
                        onSquareTapped = { viewModel.onSquareTapped(it) },
                        onPieceDragged = { from, to -> viewModel.onPieceDragged(from, to) },
                        onSelectCheckmateLesson = { viewModel.loadCheckmateLesson(it) },
                        onPlayRecommendedMove = { viewModel.playRecommendedBestMove() },
                        onSelectBlunderCase = { viewModel.loadBlunderReviewCase(it) },
                        onPlayBlunderMove = { viewModel.playBlunderMoveOnBoard(it) },
                        onPlayBestAlternativeMove = { viewModel.playBestAlternativeMoveOnBoard(it) },
                        onSelectOpeningLesson = { viewModel.loadOpeningLesson(it) },
                        onStepOpeningMove = { viewModel.stepOpeningLessonToMove(it) },
                        onRequestHighThinkingCoaching = { viewModel.requestHighThinkingAiExplanation() },
                        onBack = { viewModel.navigateBack() }
                    )
                }

                AppDestination.SETTINGS -> {
                    SettingsScreen(
                        settings = userSettings,
                        savedGamesCount = savedGames.size,
                        onUpdateDefaultColor = { viewModel.updateDefaultPlayerColor(it) },
                        onUpdateEngineDepth = { viewModel.updateEngineDepth(it) },
                        onUpdateEngineTimeMs = { viewModel.updateEngineTimeMs(it) },
                        onUpdateMultiPv = { viewModel.updateMultiPvLines(it) },
                        onUpdateBoardTheme = { viewModel.updateBoardTheme(it) },
                        onUpdatePieceStyle = { viewModel.updatePieceStyle(it) },
                        onUpdateSoundEnabled = { viewModel.updateSoundEnabled(it) },
                        onUpdateCameraThrottleMs = { viewModel.updateCameraThrottleMs(it) },
                        onUpdateUseBackCamera = { viewModel.updateUseBackCameraDefault(it) },
                        onUpdateShowOverlayArrows = { viewModel.updateShowOverlayArrows(it) },
                        onUpdateShowOverlayCoords = { viewModel.updateShowOverlayCoordinates(it) },
                        onClearAllSavedGames = { viewModel.clearAllSavedGames() },
                        onClearEngineHash = {
                            viewModel.stockfishEngine.clearHash()
                            viewModel.showToast("Cleared Stockfish transposition table.")
                        },
                        onBack = { viewModel.navigateBack() }
                    )
                }
            }

            // Floating Toast / Status Notification Banner
            AnimatedVisibility(
                visible = toastMessage != null,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CharcoalCard,
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, CyanAccent, RoundedCornerShape(12.dp))
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = toastMessage.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            modifier = Modifier.align(Alignment.CenterStart)
                        )
                        TextButton(
                            onClick = { viewModel.dismissToast() },
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            Text("OK", color = CyanAccent)
                        }
                    }
                }
            }
        }
    }
}
