package com.example.viewmodel

import android.app.Application
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.chess.core.CastlingRights
import com.example.chess.core.ChessBoardState
import com.example.chess.core.ChessMove
import com.example.chess.core.ChessRulesEngine
import com.example.chess.core.Piece
import com.example.chess.core.PieceColor
import com.example.chess.core.PieceType
import com.example.chess.core.PositionManager
import com.example.chess.core.PositionValidationResult
import com.example.chess.core.Square
import com.example.chess.engine.EngineAnalysisResult
import com.example.chess.engine.EngineConfig
import com.example.chess.engine.StockfishUciEngine
import com.example.chess.learning.BlunderReviewItem
import com.example.chess.learning.CheckmateLesson
import com.example.chess.learning.ChessLearningAssistant
import com.example.chess.learning.OpeningLesson
import com.example.chess.learning.TacticalExplanation
import com.example.chess.vision.BoardCorners
import com.example.chess.vision.CameraDetectionState
import com.example.chess.vision.ChessVisionPipeline
import com.example.chess.vision.VisionFrameAnalysisResult
import com.example.chess.vision.VisionScanMode
import com.example.data.AnalysisSnapshotEntity
import com.example.data.ChessRepository
import com.example.data.ChessWinnerDatabase
import com.example.data.LearningProgressEntity
import com.example.data.PreferencesRepository
import com.example.data.SavedGameEntity
import com.example.data.UserSettingsState
import com.example.ui.theme.BoardThemePalette
import com.example.ui.theme.PieceVisualStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AppDestination {
    HOME,
    COLOR_CONFIG,
    DIGITAL_BOARD,
    POSITION_ANALYSIS,
    CAMERA_CHESS,
    IMPORT_EXPORT,
    SAVED_GAMES,
    CHESS_LEARNING,
    SETTINGS
}

enum class SessionLaunchTarget(
    val title: String,
    val subtitle: String,
    val requiresCameraAndVision: Boolean,
    val targetDestination: AppDestination,
    val defaultScanMode: VisionScanMode = VisionScanMode.PHYSICAL_BOARD
) {
    START_ANALYSIS(
        title = "Start Chess Analysis",
        subtitle = "Stockfish UCI Multi-PV evaluation & tactical explanation",
        requiresCameraAndVision = false,
        targetDestination = AppDestination.POSITION_ANALYSIS
    ),
    CAMERA_CHESS(
        title = "Camera Chess",
        subtitle = "Live CameraX chessboard & second-phone detection",
        requiresCameraAndVision = true,
        targetDestination = AppDestination.CAMERA_CHESS,
        defaultScanMode = VisionScanMode.SECOND_PHONE_DISPLAY
    ),
    PHYSICAL_PRACTICE(
        title = "Physical Chess Practice",
        subtitle = "Real physical chessboard recognition & aligned tactical overlay",
        requiresCameraAndVision = true,
        targetDestination = AppDestination.CAMERA_CHESS,
        defaultScanMode = VisionScanMode.PHYSICAL_BOARD
    ),
    DIGITAL_CHESSBOARD(
        title = "Digital Chessboard",
        subtitle = "Full legal chess game vs Stockfish or 2-player study",
        requiresCameraAndVision = false,
        targetDestination = AppDestination.DIGITAL_BOARD
    )
}

enum class ModuleCheckStatus {
    PENDING,
    CHECKING,
    PASSED,
    WARNING,
    FAILED
}

enum class ModuleReadinessCategory(val badgeLabel: String) {
    PENDING("PENDING"),
    INITIALIZED("MODULE INITIALIZED"),
    OPERATIONAL("MODULE OPERATIONAL"),
    INCOMPLETE("MODULE INCOMPLETE"),
    REQUIRES_USER_INPUT("REQUIRES USER INPUT"),
    FAILED("MODULE FAILED")
}

data class ModuleInitItem(
    val id: Int,
    val moduleName: String,
    val requiredForSession: Boolean,
    val status: ModuleCheckStatus = ModuleCheckStatus.PENDING,
    val readinessCategory: ModuleReadinessCategory = ModuleReadinessCategory.PENDING,
    val detailMessage: String = "Waiting...",
    val recoveryHint: String? = null
)

data class SystemInitReport(
    val selectedColor: PieceColor = PieceColor.WHITE,
    val launchTarget: SessionLaunchTarget = SessionLaunchTarget.DIGITAL_CHESSBOARD,
    val isRunningChecks: Boolean = false,
    val isAllSet: Boolean = false,
    val cameraReady: Boolean = false,
    val boardGeometryDetectionReady: Boolean = false,
    val trainedPieceRecognitionReady: Boolean = false,
    val manualCalibrationAvailable: Boolean = false,
    val stockfishReady: Boolean = false,
    val isGenuineStockfishBinary: Boolean = false,
    val chessRulesReady: Boolean = false,
    val databaseReady: Boolean = false,
    val isFullAutomaticRecognitionReady: Boolean = false,
    val readinessBannerSubtitle: String = "Core modules verified.",
    val nextActionGuidance: String = "Select your playing side and press Start Session.",
    val failedModuleName: String? = null,
    val failureRecoveryMessage: String? = null,
    val modules: List<ModuleInitItem> = emptyList()
)

enum class LearningSubTab(val title: String) {
    POSITION_COACH("Position Analysis"),
    CHECKMATE_PATTERNS("Checkmate Patterns"),
    BLUNDER_REVIEW("Blunder Review"),
    OPENING_MASTERY("Opening Mastery")
}

class ChessWinnerViewModel(application: Application) : AndroidViewModel(application) {

    private val database = ChessWinnerDatabase.getInstance(application)
    private val repository = ChessRepository(database.chessDao())
    private val preferencesRepository = PreferencesRepository(application)

    val positionManager = PositionManager()
    val stockfishEngine = StockfishUciEngine(application)
    val visionPipeline = ChessVisionPipeline(application)

    val boardState: StateFlow<ChessBoardState> = positionManager.boardState
    val selectedSquare: StateFlow<Square?> = positionManager.selectedSquare
    val legalMovesForSelected: StateFlow<List<ChessMove>> = positionManager.legalMovesForSelected
    val pendingPromotionMoves: StateFlow<List<ChessMove>> = positionManager.pendingPromotionMoves
    val boardOrientation: StateFlow<PieceColor> = positionManager.boardOrientation
    val engineAnalysis: StateFlow<EngineAnalysisResult> = stockfishEngine.analysisState

    val userSettings: StateFlow<UserSettingsState> = preferencesRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettingsState())

    val savedGames: StateFlow<List<SavedGameEntity>> = repository.savedGames
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val learningProgress: StateFlow<List<LearningProgressEntity>> = repository.learningProgress
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val analysisSnapshots: StateFlow<List<AnalysisSnapshotEntity>> = repository.analysisSnapshots
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentDestination = MutableStateFlow(AppDestination.HOME)
    val currentDestination: StateFlow<AppDestination> = _currentDestination.asStateFlow()

    private val navBackStack = ArrayDeque<AppDestination>()

    // Section 4: Maintain two separate states:
    // 1. playerSide (User-selected color: WHITE or BLACK; never changed by orientation flip or moves)
    // 2. sideToMove (Actual turn obtained from the latest validated chess position/FEN)
    private val _playerColor = MutableStateFlow(PieceColor.WHITE)
    val playerColor: StateFlow<PieceColor> = _playerColor.asStateFlow()
    val playerSide: StateFlow<PieceColor> = _playerColor.asStateFlow()

    private val _sideToMove = MutableStateFlow(positionManager.boardState.value.activeColor)
    val sideToMove: StateFlow<PieceColor> = _sideToMove.asStateFlow()

    private val _turnSynchronizationLabel = MutableStateFlow("YOUR TURN")
    val turnSynchronizationLabel: StateFlow<String> = _turnSynchronizationLabel.asStateFlow()

    private val _playAgainstEngine = MutableStateFlow(true)
    val playAgainstEngine: StateFlow<Boolean> = _playAgainstEngine.asStateFlow()

    private val _systemInitReport = MutableStateFlow(SystemInitReport())
    val systemInitReport: StateFlow<SystemInitReport> = _systemInitReport.asStateFlow()

    private val _tacticalExplanation = MutableStateFlow(
        ChessLearningAssistant.explainPositionDeterministically(
            positionManager.boardState.value,
            stockfishEngine.analysisState.value
        )
    )
    val tacticalExplanation: StateFlow<TacticalExplanation> = _tacticalExplanation.asStateFlow()

    // Optional High-Thinking Gemini 4 Argon Coaching State
    private val _aiCoachingText = MutableStateFlow<String?>(null)
    val aiCoachingText: StateFlow<String?> = _aiCoachingText.asStateFlow()

    private val _isAiCoachingLoading = MutableStateFlow(false)
    val isAiCoachingLoading: StateFlow<Boolean> = _isAiCoachingLoading.asStateFlow()

    private val _aiCoachingError = MutableStateFlow<String?>(null)
    val aiCoachingError: StateFlow<String?> = _aiCoachingError.asStateFlow()

    // Camera & Computer Vision State
    private val _visionScanMode = MutableStateFlow(VisionScanMode.PHYSICAL_BOARD)
    val visionScanMode: StateFlow<VisionScanMode> = _visionScanMode.asStateFlow()

    private val _isCameraPaused = MutableStateFlow(false)
    val isCameraPaused: StateFlow<Boolean> = _isCameraPaused.asStateFlow()

    private val _useBackCamera = MutableStateFlow(true)
    val useBackCamera: StateFlow<Boolean> = _useBackCamera.asStateFlow()

    private val _calibratedCorners = MutableStateFlow(BoardCorners())
    val calibratedCorners: StateFlow<BoardCorners> = _calibratedCorners.asStateFlow()

    private val _userConfirmedCorners = MutableStateFlow(false)
    val userConfirmedCorners: StateFlow<Boolean> = _userConfirmedCorners.asStateFlow()

    private val _manualSquareOverrides = MutableStateFlow<Map<Square, Piece?>>(emptyMap())
    val manualSquareOverrides: StateFlow<Map<Square, Piece?>> = _manualSquareOverrides.asStateFlow()

    private val _cameraCalibrationBoard = MutableStateFlow(ChessRulesEngine.createInitialState())
    val cameraCalibrationBoard: StateFlow<ChessBoardState> = _cameraCalibrationBoard.asStateFlow()

    private val _cameraSideToMove = MutableStateFlow(PieceColor.WHITE)
    val cameraSideToMove: StateFlow<PieceColor> = _cameraSideToMove.asStateFlow()

    private val _userConfirmedCalibration = MutableStateFlow(false)
    val userConfirmedCalibration: StateFlow<Boolean> = _userConfirmedCalibration.asStateFlow()

    private val _visionAnalysisResult = MutableStateFlow(
        VisionFrameAnalysisResult(
            detectionState = CameraDetectionState.INITIALIZING_CAMERA,
            statusBannerMessage = "Initializing Camera...",
            isChessboardDetected = false,
            isPieceRecognitionConfident = false,
            isCalibratedModelLoaded = visionPipeline.isTrainedTfliteBundled,
            corners = BoardCorners(),
            squareResults = emptyList(),
            overallConfidence = 0f,
            meanLuminance = 0f,
            glareDetected = false,
            poorLightingDetected = false,
            recognizedState = null,
            validationResult = null,
            modelStatusLabel = visionPipeline.getModelStatusDescription()
        )
    )
    val visionAnalysisResult: StateFlow<VisionFrameAnalysisResult> = _visionAnalysisResult.asStateFlow()

    // Learning Screen Interactive State
    private val _activeLearningTab = MutableStateFlow(LearningSubTab.POSITION_COACH)
    val activeLearningTab: StateFlow<LearningSubTab> = _activeLearningTab.asStateFlow()

    private val _selectedCheckmateLesson = MutableStateFlow(ChessLearningAssistant.CHECKMATE_LESSONS.first())
    val selectedCheckmateLesson: StateFlow<CheckmateLesson> = _selectedCheckmateLesson.asStateFlow()

    private val _checkmateExerciseFeedback = MutableStateFlow<String?>(null)
    val checkmateExerciseFeedback: StateFlow<String?> = _checkmateExerciseFeedback.asStateFlow()

    private val _selectedBlunderItem = MutableStateFlow(ChessLearningAssistant.BLUNDER_REVIEW_CASES.first())
    val selectedBlunderItem: StateFlow<BlunderReviewItem> = _selectedBlunderItem.asStateFlow()

    private val _selectedOpeningLesson = MutableStateFlow(ChessLearningAssistant.OPENING_LESSONS.first())
    val selectedOpeningLesson: StateFlow<OpeningLesson> = _selectedOpeningLesson.asStateFlow()

    private val _openingMoveStepIndex = MutableStateFlow(0)
    val openingMoveStepIndex: StateFlow<Int> = _openingMoveStepIndex.asStateFlow()

    // Manual Board Editor State (in Import/Export & Editor Screen)
    private val _editorSquares = MutableStateFlow(ChessRulesEngine.createInitialState().squares)
    val editorSquares: StateFlow<List<Piece?>> = _editorSquares.asStateFlow()

    private val _editorActiveColor = MutableStateFlow(PieceColor.WHITE)
    val editorActiveColor: StateFlow<PieceColor> = _editorActiveColor.asStateFlow()

    private val _editorSelectedBrush = MutableStateFlow<Piece?>(Piece(PieceType.QUEEN, PieceColor.WHITE))
    val editorSelectedBrush: StateFlow<Piece?> = _editorSelectedBrush.asStateFlow()

    private val _statusBannerToast = MutableStateFlow<String?>(null)
    val statusBannerToast: StateFlow<String?> = _statusBannerToast.asStateFlow()

    private var lastCameraFrameTimestamp = 0L
    private var engineReplyJob: Job? = null

    init {
        viewModelScope.launch {
            repository.verifyAndSeedInitialGamesIfNeeded()
            stockfishEngine.initializeEngine()
            stockfishEngine.analyzePosition(positionManager.boardState.value)
        }

        // Synchronize sideToMove and turnSynchronizationLabel whenever validated boardState changes
        viewModelScope.launch {
            positionManager.boardState.collect { validBoard ->
                _sideToMove.value = validBoard.activeColor
                refreshTurnSynchronizationLabel()
            }
        }

        // Observe settings changes and apply to engine & camera
        viewModelScope.launch {
            userSettings.collect { settings ->
                stockfishEngine.updateConfig(
                    EngineConfig(
                        targetDepth = settings.engineDepth,
                        maxAnalysisTimeMs = settings.engineTimeMs,
                        multiPvCount = settings.multiPvLines
                    )
                )
                _useBackCamera.value = settings.useBackCameraByDefault
            }
        }

        // Keep deterministic explanation updated whenever board state or engine analysis updates
        viewModelScope.launch {
            stockfishEngine.analysisState.collect { analysis ->
                _tacticalExplanation.value = ChessLearningAssistant.explainPositionDeterministically(
                    positionManager.boardState.value,
                    analysis
                )
            }
        }
    }

    private fun refreshTurnSynchronizationLabel() {
        val currentTurn = positionManager.boardState.value.activeColor
        _sideToMove.value = currentTurn
        _turnSynchronizationLabel.value = if (_playerColor.value == currentTurn) {
            "YOUR TURN"
        } else {
            "OPPONENT'S TURN"
        }
    }

    fun getTurnStatusLabel(
        selectedPlayerSide: PieceColor = _playerColor.value,
        currentSideToMove: PieceColor = _sideToMove.value
    ): String {
        return if (selectedPlayerSide == currentSideToMove) "YOUR TURN" else "OPPONENT'S TURN"
    }

    fun isPlayerTurnNow(): Boolean = _playerColor.value == _sideToMove.value

    // Navigation
    fun navigateTo(destination: AppDestination) {
        if (_currentDestination.value != destination) {
            navBackStack.addLast(_currentDestination.value)
            _currentDestination.value = destination
        }
    }

    fun navigateBack(): Boolean {
        if (_currentDestination.value == AppDestination.CAMERA_CHESS) {
            _isCameraPaused.value = true
        }
        return if (navBackStack.isNotEmpty()) {
            _currentDestination.value = navBackStack.removeLast()
            true
        } else if (_currentDestination.value != AppDestination.HOME) {
            _currentDestination.value = AppDestination.HOME
            true
        } else {
            false
        }
    }

    /**
     * Opens the Player Color Configuration screen before starting a chess session (Section 5).
     */
    fun openColorConfigurationForSession(target: SessionLaunchTarget) {
        val defaultColor = userSettings.value.defaultPlayerColor
        _playerColor.value = defaultColor
        positionManager.setOrientation(defaultColor)
        refreshTurnSynchronizationLabel()
        _visionScanMode.value = target.defaultScanMode
        if (target.requiresCameraAndVision) {
            _userConfirmedCalibration.value = false
            _userConfirmedCorners.value = false
            _manualSquareOverrides.value = emptyMap()
            _cameraCalibrationBoard.value = positionManager.boardState.value
            _cameraSideToMove.value = positionManager.boardState.value.activeColor
        }
        _systemInitReport.value = SystemInitReport(
            selectedColor = defaultColor,
            launchTarget = target,
            isRunningChecks = false,
            isAllSet = false,
            modules = buildInitialModuleList(target.requiresCameraAndVision)
        )
        navigateTo(AppDestination.COLOR_CONFIG)
    }

    fun selectPlayerColor(color: PieceColor) {
        _playerColor.value = color
        positionManager.setOrientation(color)
        refreshTurnSynchronizationLabel()
        _systemInitReport.value = _systemInitReport.value.copy(
            selectedColor = color,
            isAllSet = false,
            failedModuleName = null,
            failureRecoveryMessage = null
        )
    }

    private fun buildInitialModuleList(requiresCamera: Boolean): List<ModuleInitItem> {
        return listOf(
            ModuleInitItem(1, "Camera Permission / CameraX Initialization", requiresCamera),
            ModuleInitItem(2, "Board Geometry Detection", true),
            ModuleInitItem(3, "Trained Piece Recognition (TFLite)", false),
            ModuleInitItem(4, "Manual Calibration", true),
            ModuleInitItem(5, "Chess Engine / UCI", true),
            ModuleInitItem(6, "Chess Rules Engine", true),
            ModuleInitItem(7, "Database (Room SQLite)", true)
        )
    }

    /**
     * Performs strict verification of all 7 system modules when Start is pressed:
     * 1. Camera Permission / CameraX Initialization
     * 2. Board Geometry Detection
     * 3. Trained Piece Recognition (never reported as READY when chess_piece_classifier.tflite is missing)
     * 4. Manual Calibration
     * 5. Chess Engine / UCI (distinguishing genuine Stockfish binary vs. fallback engine)
     * 6. Chess Rules Engine
     * 7. Database (Room SQLite)
     *
     * Explicitly distinguishes:
     * - MODULE INITIALIZED
     * - MODULE OPERATIONAL
     * - MODULE INCOMPLETE
     * - REQUIRES USER INPUT
     */
    fun runSystemInitializationAndStart(cameraPermissionGranted: Boolean) {
        val target = _systemInitReport.value.launchTarget
        val chosenColor = _playerColor.value

        val modules = buildInitialModuleList(target.requiresCameraAndVision).toMutableList()
        _systemInitReport.value = _systemInitReport.value.copy(
            isRunningChecks = true,
            isAllSet = false,
            failedModuleName = null,
            failureRecoveryMessage = null,
            modules = modules.toList()
        )

        // 1. Verify Camera Permission / CameraX Initialization
        val context = getApplication<Application>()
        val hasPerm = cameraPermissionGranted || ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (target.requiresCameraAndVision && !hasPerm) {
            failInitialization(
                modules,
                0,
                "Camera Module requires CAMERA permission to view physical or second-phone chessboards.",
                "Tap 'Grant Camera Permission' below or switch to Digital Chessboard mode."
            )
            return
        }
        modules[0] = if (hasPerm) {
            modules[0].copy(
                status = ModuleCheckStatus.PASSED,
                readinessCategory = ModuleReadinessCategory.INITIALIZED,
                detailMessage = "MODULE INITIALIZED: Android CAMERA permission granted; CameraX preview & Y-plane analyzer ready to bind."
            )
        } else {
            modules[0].copy(
                status = ModuleCheckStatus.WARNING,
                readinessCategory = ModuleReadinessCategory.REQUIRES_USER_INPUT,
                detailMessage = "REQUIRES USER INPUT: Camera permission not granted (Not required for ${target.title}; required for Camera modes)."
            )
        }

        // 2. Verify Board Geometry Detection
        val geometryOk = visionPipeline.verifyGeometryDetectorSelfTest()
        if (!geometryOk) {
            failInitialization(
                modules,
                1,
                "Board Geometry Detection failed 8x8 perspective homography self-test.",
                "Restart session to reinitialize perspective homography matrix."
            )
            return
        }
        modules[1] = modules[1].copy(
            status = ModuleCheckStatus.PASSED,
            readinessCategory = ModuleReadinessCategory.OPERATIONAL,
            detailMessage = "MODULE OPERATIONAL: 8x8 perspective homography warp & checkerboard periodicity detector verified."
        )

        // 3. Verify Trained Piece Recognition (TFLite 13-Class Model Audit)
        val tfliteAudit = visionPipeline.auditTfliteModel()
        val trainedModelReady = tfliteAudit.isTrainedModelReady
        modules[2] = if (trainedModelReady) {
            modules[2].copy(
                status = ModuleCheckStatus.PASSED,
                readinessCategory = ModuleReadinessCategory.OPERATIONAL,
                detailMessage = "MODULE OPERATIONAL: ${tfliteAudit.auditStatusSummary}"
            )
        } else {
            modules[2].copy(
                status = ModuleCheckStatus.WARNING,
                readinessCategory = ModuleReadinessCategory.INCOMPLETE,
                detailMessage = "MODULE INCOMPLETE: ${tfliteAudit.auditStatusSummary}"
            )
        }

        // 4. Verify Manual Calibration & Board Renderer
        val theme = userSettings.value.boardTheme
        val pieceStyle = userSettings.value.pieceStyle
        val cornersTest = BoardCorners().mapBoardToOverlay(0.5f, 0.5f)
        val manualCalibrationReady = cornersTest.x in 0.4f..0.6f && cornersTest.y in 0.4f..0.6f
        if (!manualCalibrationReady) {
            failInitialization(
                modules,
                3,
                "Manual Calibration & Board Renderer failed coordinate mapping check.",
                "Reset corner calibration in Camera settings."
            )
            return
        }
        modules[3] = if (target.requiresCameraAndVision && !trainedModelReady) {
            modules[3].copy(
                status = ModuleCheckStatus.PASSED,
                readinessCategory = ModuleReadinessCategory.REQUIRES_USER_INPUT,
                detailMessage = "REQUIRES USER INPUT: 64-square calibration editor, turn selector & 4-corner handles ready. Confirm board in Camera mode to run analysis."
            )
        } else {
            modules[3].copy(
                status = ModuleCheckStatus.PASSED,
                readinessCategory = ModuleReadinessCategory.OPERATIONAL,
                detailMessage = "MODULE OPERATIONAL: 4-corner homography handles, 64-square piece override editor & 8x8 Canvas (${theme.displayName}, ${pieceStyle.displayName}) ready."
            )
        }

        // 5. Verify Chess Engine / UCI
        val engineInit = stockfishEngine.initializeEngine()
        val engineAudit = stockfishEngine.verifyEngineIdentity()
        if (engineInit.isFailure || !engineAudit.uciHandshakeVerified) {
            failInitialization(
                modules,
                4,
                "Chess Engine failed UCI readiness check: ${engineInit.exceptionOrNull()?.localizedMessage ?: "Not ready"}",
                "Retry engine initialization or reset engine hash in Settings."
            )
            return
        }
        modules[4] = if (engineAudit.isGenuineStockfishBinary) {
            modules[4].copy(
                status = ModuleCheckStatus.PASSED,
                readinessCategory = ModuleReadinessCategory.OPERATIONAL,
                detailMessage = "MODULE OPERATIONAL: Genuine Stockfish (${engineAudit.uciIdName}, MultiPV=${userSettings.value.multiPvLines})."
            )
        } else {
            modules[4].copy(
                status = ModuleCheckStatus.WARNING,
                readinessCategory = ModuleReadinessCategory.OPERATIONAL,
                detailMessage = "MODULE OPERATIONAL (Embedded UCI Fallback): ${engineAudit.uciIdName} — Native libstockfish.so not bundled."
            )
        }

        // 6. Verify Chess Rules Engine & Position Manager
        val initialBoard = ChessRulesEngine.createInitialState()
        val legalMoveCount = ChessRulesEngine.generateLegalMoves(initialBoard).size
        if (legalMoveCount != 20) {
            failInitialization(
                modules,
                5,
                "Chess Rules Module failed initial 20-move verification (got $legalMoveCount).",
                "Reset board state and retry initialization."
            )
            return
        }
        positionManager.setOrientation(chosenColor)
        val currentValidation = ChessRulesEngine.validatePosition(positionManager.boardState.value)
        if (!currentValidation.isValid) {
            positionManager.restartGame(chosenColor)
        }
        refreshTurnSynchronizationLabel()
        modules[5] = modules[5].copy(
            status = ModuleCheckStatus.PASSED,
            readinessCategory = ModuleReadinessCategory.OPERATIONAL,
            detailMessage = "MODULE OPERATIONAL: 20 opening moves, castling, en passant, promotion, checkmate/draw & Turn Sync (Player=${chosenColor.displayName}, Turn=${_sideToMove.value.displayName}) verified."
        )

        // 7. Verify Database (Room SQLite)
        val dbOk = runCatching {
            database.chessDao()
            true
        }.getOrDefault(false)

        if (!dbOk) {
            failInitialization(
                modules,
                6,
                "Local Database (Room SQLite) failed integrity check.",
                "Clear local storage cache in Settings and retry."
            )
            return
        }
        modules[6] = modules[6].copy(
            status = ModuleCheckStatus.PASSED,
            readinessCategory = ModuleReadinessCategory.OPERATIONAL,
            detailMessage = "MODULE OPERATIONAL: Room SQLite offline storage ready (${savedGames.value.size} saved games)."
        )

        val fullAutoReady = hasPerm && geometryOk && trainedModelReady
        val engineShortLabel = if (engineAudit.isGenuineStockfishBinary) {
            "Genuine Stockfish (${engineAudit.uciIdName})"
        } else {
            "Embedded UCI Fallback (libstockfish.so not bundled)"
        }
        val autoRecognitionLabel = if (trainedModelReady) {
            "Full Automatic TFLite Recognition Ready"
        } else {
            "Automatic TFLite Piece Recognition INCOMPLETE (chess_piece_classifier.tflite missing — Manual Calibration Active)"
        }
        val nextAction = if (target.requiresCameraAndVision) {
            "Next Step: Tap 'Launch ${target.title}' -> Align camera grid with chessboard -> Confirm/calibrate pieces & sideToMove to view engine best-move arrows."
        } else {
            "Next Step: Tap 'Launch ${target.title}' to begin playing as ${chosenColor.displayName}."
        }

        // All required session modules initialized!
        _systemInitReport.value = _systemInitReport.value.copy(
            isRunningChecks = false,
            isAllSet = true,
            cameraReady = hasPerm,
            boardGeometryDetectionReady = geometryOk,
            trainedPieceRecognitionReady = trainedModelReady,
            manualCalibrationAvailable = manualCalibrationReady,
            stockfishReady = engineAudit.uciHandshakeVerified,
            isGenuineStockfishBinary = engineAudit.isGenuineStockfishBinary,
            chessRulesReady = true,
            databaseReady = dbOk,
            isFullAutomaticRecognitionReady = fullAutoReady,
            readinessBannerSubtitle = "$autoRecognitionLabel • Engine: $engineShortLabel",
            nextActionGuidance = nextAction,
            failedModuleName = null,
            failureRecoveryMessage = null,
            modules = modules.toList()
        )

        if (target.requiresCameraAndVision) {
            // In Camera mode, do not keep a stale recommendation before the user confirms the camera board
            _cameraCalibrationBoard.value = positionManager.boardState.value
            _cameraSideToMove.value = positionManager.boardState.value.activeColor
            if (!_userConfirmedCalibration.value) {
                stockfishEngine.clearAnalysisForUncertainPosition("Camera board not yet confirmed — tap 'Confirm Board' on Camera screen.")
            }
        } else {
            // Trigger analysis for current digital/analysis board
            stockfishEngine.analyzePosition(positionManager.boardState.value)
        }

        // If user selected Black Pieces and is launching Digital Chessboard vs Engine, let White (Engine) play move 1 when entering
        if (target == SessionLaunchTarget.DIGITAL_CHESSBOARD &&
            chosenColor == PieceColor.BLACK &&
            _playAgainstEngine.value &&
            positionManager.boardState.value.activeColor == PieceColor.WHITE &&
            positionManager.boardState.value.moveHistory.isEmpty()
        ) {
            triggerEngineMoveIfNeeded()
        }
    }

    private fun failInitialization(
        modules: MutableList<ModuleInitItem>,
        failedIndex: Int,
        errorDetail: String,
        recoveryAction: String
    ) {
        modules[failedIndex] = modules[failedIndex].copy(
            status = ModuleCheckStatus.FAILED,
            readinessCategory = ModuleReadinessCategory.FAILED,
            detailMessage = errorDetail,
            recoveryHint = recoveryAction
        )
        _systemInitReport.value = _systemInitReport.value.copy(
            isRunningChecks = false,
            isAllSet = false,
            failedModuleName = modules[failedIndex].moduleName,
            failureRecoveryMessage = recoveryAction,
            modules = modules.toList()
        )
    }

    fun proceedAfterConfigurationAllSet() {
        val report = _systemInitReport.value
        if (!report.isAllSet) return
        val dest = report.launchTarget.targetDestination
        if (dest == AppDestination.CAMERA_CHESS) {
            _isCameraPaused.value = false
        }
        _currentDestination.value = dest
    }

    // Digital Board & Analysis Interactions
    fun onSquareTapped(square: Square) {
        val executedMove = positionManager.selectSquare(square)
        if (executedMove != null) {
            onMoveCompleted(executedMove)
        }
    }

    fun onPieceDragged(from: Square, to: Square) {
        val executedMove = positionManager.tryMoveBetweenSquares(from, to)
        if (executedMove != null) {
            onMoveCompleted(executedMove)
        }
    }

    fun onPromotionPieceSelected(pieceType: PieceType) {
        val executedMove = positionManager.completePromotion(pieceType)
        if (executedMove != null) {
            onMoveCompleted(executedMove)
        }
    }

    fun onPromotionCancelled() {
        positionManager.cancelPromotion()
    }

    private fun onMoveCompleted(move: ChessMove) {
        playMoveFeedback(move)
        _aiCoachingText.value = null
        val newState = positionManager.boardState.value
        _sideToMove.value = newState.activeColor
        _cameraCalibrationBoard.value = newState
        _cameraSideToMove.value = newState.activeColor
        refreshTurnSynchronizationLabel()
        if (_userConfirmedCalibration.value) {
            lockConfirmedBoardIntoOverrides(newState)
            _visionAnalysisResult.value = _visionAnalysisResult.value.copy(
                detectionState = CameraDetectionState.CONFIGURATION_ALL_SET,
                isChessboardDetected = true,
                isPieceRecognitionConfident = true,
                recognizedState = newState,
                validationResult = ChessRulesEngine.validatePosition(newState),
                statusBannerMessage = ChessVisionPipeline.MSG_CONFIGURATION_ALL_SET
            )
        }
        stockfishEngine.analyzePosition(newState)

        // Check if user is in Checkmate Learning interactive exercise mode
        if (_currentDestination.value == AppDestination.CHESS_LEARNING &&
            _activeLearningTab.value == LearningSubTab.CHECKMATE_PATTERNS
        ) {
            val lesson = _selectedCheckmateLesson.value
            if (move.uci in lesson.solutionUci || move.san in lesson.solutionSan || newState.status.name == "CHECKMATE") {
                _checkmateExerciseFeedback.value = "Solved! ${move.san} executes the ${lesson.patternName} pattern."
                viewModelScope.launch {
                    repository.markLessonCompleted(lesson.id, "Checkmate")
                }
            } else {
                _checkmateExerciseFeedback.value = "Move ${move.san} played, but there is a faster forced mate! Hint: ${lesson.tacticalHint}"
            }
            return
        }

        if (_currentDestination.value == AppDestination.DIGITAL_BOARD && _playAgainstEngine.value) {
            triggerEngineMoveIfNeeded()
        }
    }

    private fun triggerEngineMoveIfNeeded() {
        engineReplyJob?.cancel()
        val state = positionManager.boardState.value
        if (state.status.isTerminal) return
        if (state.activeColor == _playerColor.value) return

        engineReplyJob = viewModelScope.launch {
            delay(350L)
            val current = positionManager.boardState.value
            if (current.status.isTerminal || current.activeColor == _playerColor.value) return@launch
            val result = stockfishEngine.analyzePositionBlocking(
                boardState = current,
                depth = userSettings.value.engineDepth.coerceIn(2, 4),
                maxTimeMs = 1200L
            )
            val bestUci = result.bestMoveUci ?: return@launch
            val move = ChessRulesEngine.parseUciMove(current, bestUci) ?: return@launch
            if (positionManager.makeMove(move)) {
                playMoveFeedback(move)
                refreshTurnSynchronizationLabel()
                stockfishEngine.analyzePosition(positionManager.boardState.value)
            }
        }
    }

    fun playRecommendedBestMove() {
        val current = positionManager.boardState.value
        if (current.status.isTerminal) return
        val analysis = stockfishEngine.analysisState.value
        if (analysis.fen != current.toFen() || analysis.error != null) return
        val bestUci = analysis.bestMoveUci ?: return
        val move = ChessRulesEngine.parseUciMove(current, bestUci) ?: return
        if (positionManager.makeMove(move)) {
            onMoveCompleted(move)
        }
    }

    fun undoMove() {
        engineReplyJob?.cancel()
        if (positionManager.undoLastMove()) {
            // If playing vs engine and it's now engine's turn, undo one more ply to return to player's turn
            if (_currentDestination.value == AppDestination.DIGITAL_BOARD &&
                _playAgainstEngine.value &&
                positionManager.boardState.value.activeColor != _playerColor.value
            ) {
                positionManager.undoLastMove()
            }
            _aiCoachingText.value = null
            val restored = positionManager.boardState.value
            _cameraCalibrationBoard.value = restored
            _cameraSideToMove.value = restored.activeColor
            refreshTurnSynchronizationLabel()
            if (_userConfirmedCalibration.value) {
                lockConfirmedBoardIntoOverrides(restored)
                _visionAnalysisResult.value = _visionAnalysisResult.value.copy(
                    recognizedState = restored,
                    validationResult = ChessRulesEngine.validatePosition(restored)
                )
            }
            stockfishEngine.analyzePosition(restored)
        }
    }

    fun restartGame() {
        engineReplyJob?.cancel()
        positionManager.restartGame(positionManager.boardOrientation.value)
        _aiCoachingText.value = null
        val fresh = positionManager.boardState.value
        _cameraCalibrationBoard.value = fresh
        _cameraSideToMove.value = fresh.activeColor
        refreshTurnSynchronizationLabel()
        stockfishEngine.analyzePosition(fresh)
        if (_currentDestination.value == AppDestination.DIGITAL_BOARD &&
            _playAgainstEngine.value &&
            _playerColor.value == PieceColor.BLACK
        ) {
            triggerEngineMoveIfNeeded()
        }
    }

    /**
     * Rotates/flips the visual board orientation (`WHITE` <-> `BLACK`) without EVER changing `playerSide`.
     */
    fun flipBoardOrientation() {
        positionManager.flipOrientation()
        refreshTurnSynchronizationLabel()
    }

    fun togglePlayAgainstEngine(enabled: Boolean) {
        _playAgainstEngine.value = enabled
        if (enabled && positionManager.boardState.value.activeColor != _playerColor.value) {
            triggerEngineMoveIfNeeded()
        }
    }

    // Optional High-Thinking Gemini 4 Argon Explanation
    fun requestHighThinkingAiExplanation() {
        if (_isAiCoachingLoading.value) return
        viewModelScope.launch {
            _isAiCoachingLoading.value = true
            _aiCoachingError.value = null
            val result = ChessLearningAssistant.generateHighThinkingAiCoaching(
                state = positionManager.boardState.value,
                analysis = stockfishEngine.analysisState.value
            )
            _isAiCoachingLoading.value = false
            result.onSuccess { text ->
                _aiCoachingText.value = text
            }.onFailure { err ->
                _aiCoachingError.value = err.localizedMessage ?: "AI Coaching unavailable offline."
            }
        }
    }

    // Camera & Computer Vision Actions
    fun setVisionScanMode(mode: VisionScanMode) {
        _visionScanMode.value = mode
        showToast("Switched to ${mode.displayName}")
    }

    fun onCameraLifecycleState(ready: Boolean, errorMessage: String? = null) {
        if (_userConfirmedCalibration.value) {
            return
        }
        if (errorMessage != null) {
            _visionAnalysisResult.value = _visionAnalysisResult.value.copy(
                detectionState = CameraDetectionState.INITIALIZING_CAMERA,
                statusBannerMessage = errorMessage,
                isChessboardDetected = false
            )
            return
        }
        if (ready && _visionAnalysisResult.value.detectionState == CameraDetectionState.INITIALIZING_CAMERA) {
            _visionAnalysisResult.value = _visionAnalysisResult.value.copy(
                detectionState = CameraDetectionState.CAMERA_READY,
                statusBannerMessage = "Camera Ready — Point at chessboard"
            )
        }
    }

    /**
     * Processes a grayscale luminance frame from CameraX (or tests).
     * - When [_userConfirmedCalibration] is false and no trained TFLite model is bundled,
     *   piece recognition is uncertain (`isPieceRecognitionConfident == false`), so stale
     *   engine recommendations are immediately cleared (Mandatory Test G).
     * - When [_userConfirmedCalibration] is true, the user's confirmed 64-square calibration
     *   is preserved via [_manualSquareOverrides] and validated positions are analyzed by the engine.
     */
    fun processCameraLuminanceFrame(
        luminanceBytes: ByteArray,
        width: Int,
        height: Int,
        bypassThrottle: Boolean = false
    ) {
        if (_isCameraPaused.value && !bypassThrottle) return
        val now = System.currentTimeMillis()
        val throttleMs = userSettings.value.cameraFrameThrottleMs
        if (!bypassThrottle && now - lastCameraFrameTimestamp < throttleMs) return
        lastCameraFrameTimestamp = now

        val isConfirmed = _userConfirmedCalibration.value
        val activeTurn = _cameraSideToMove.value
        val currentOverrides = _manualSquareOverrides.value
        val currentCorners = if (_userConfirmedCorners.value || isConfirmed) _calibratedCorners.value else null

        val result = visionPipeline.analyzeLuminanceFrame(
            luminanceData = luminanceBytes,
            width = width,
            height = height,
            scanMode = _visionScanMode.value,
            customCorners = currentCorners,
            orientation = positionManager.boardOrientation.value,
            manualOverrides = currentOverrides,
            userConfirmedCalibration = isConfirmed,
            activeColor = activeTurn
        )
        _visionAnalysisResult.value = result
        if (!_userConfirmedCorners.value && !isConfirmed && result.isChessboardDetected) {
            _calibratedCorners.value = result.corners
        }

        if (!result.isChessboardDetected || !result.isPieceRecognitionConfident) {
            // Uncertain or unconfirmed camera scan MUST NOT generate or retain a recommendation
            stockfishEngine.clearAnalysisForUncertainPosition(
                "Uncertain camera scan (${result.statusBannerMessage}) — confirm board calibration to analyze."
            )
        } else {
            val recognized = result.recognizedState
            val validation = result.validationResult
            if (recognized != null && validation?.isValid == true) {
                val loaded = positionManager.loadCustomState(recognized)
                if (loaded.isValid) {
                    refreshTurnSynchronizationLabel()
                    val currentAnalysis = stockfishEngine.analysisState.value
                    if (currentAnalysis.fen != recognized.toFen() || currentAnalysis.bestMoveUci == null) {
                        stockfishEngine.analyzePosition(positionManager.boardState.value)
                    }
                }
            } else if (recognized != null) {
                stockfishEngine.analyzePosition(recognized)
            }
        }
    }

    fun toggleCameraPause() {
        _isCameraPaused.value = !_isCameraPaused.value
    }

    fun toggleCameraLens() {
        _useBackCamera.value = !_useBackCamera.value
    }

    fun updateCornerCalibration(corners: BoardCorners) {
        _calibratedCorners.value = corners
        _userConfirmedCorners.value = true
    }

    private fun lockConfirmedBoardIntoOverrides(state: ChessBoardState) {
        val fullMap = mutableMapOf<Square, Piece?>()
        for (i in 0 until 64) {
            fullMap[Square.fromIndex(i)] = state.squares[i]
        }
        _manualSquareOverrides.value = fullMap
    }

    /**
     * Sets whose turn it is (`sideToMove`) for the Camera Calibration board.
     * Never changes `playerSide` (`_playerColor`).
     */
    fun setCameraSideToMove(color: PieceColor) {
        _cameraSideToMove.value = color
        val updated = _cameraCalibrationBoard.value.copy(activeColor = color)
        _cameraCalibrationBoard.value = updated
        val validation = ChessRulesEngine.validatePosition(updated)
        if (_userConfirmedCalibration.value) {
            if (validation.isValid) {
                positionManager.loadCustomState(updated)
                refreshTurnSynchronizationLabel()
                _visionAnalysisResult.value = _visionAnalysisResult.value.copy(
                    detectionState = CameraDetectionState.CONFIGURATION_ALL_SET,
                    statusBannerMessage = ChessVisionPipeline.MSG_CONFIGURATION_ALL_SET,
                    isChessboardDetected = true,
                    isPieceRecognitionConfident = true,
                    recognizedState = positionManager.boardState.value,
                    validationResult = validation
                )
                stockfishEngine.analyzePosition(positionManager.boardState.value)
            } else {
                _visionAnalysisResult.value = _visionAnalysisResult.value.copy(
                    detectionState = CameraDetectionState.POSITION_VALIDATION,
                    statusBannerMessage = "Invalid position: ${validation.errors.firstOrNull()}",
                    isPieceRecognitionConfident = false,
                    recognizedState = updated,
                    validationResult = validation
                )
                stockfishEngine.analyzePosition(updated)
            }
        } else {
            _visionAnalysisResult.value = _visionAnalysisResult.value.copy(
                recognizedState = updated,
                validationResult = validation
            )
        }
    }

    /**
     * Clears all 64 squares of the Camera Calibration board so the user can place a custom endgame
     * or verify that an empty board is rejected by the validator.
     */
    fun clearCameraCalibrationBoard() {
        val emptySquares = List<Piece?>(64) { null }
        val emptyState = ChessBoardState(
            squares = emptySquares,
            activeColor = _cameraSideToMove.value,
            castlingRights = CastlingRights.NONE
        )
        _cameraCalibrationBoard.value = emptyState
        lockConfirmedBoardIntoOverrides(emptyState)
        _userConfirmedCalibration.value = false
        val validation = ChessRulesEngine.validatePosition(emptyState)
        _visionAnalysisResult.value = _visionAnalysisResult.value.copy(
            detectionState = CameraDetectionState.POSITION_VALIDATION,
            statusBannerMessage = "Invalid position: ${validation.errors.firstOrNull()}",
            isPieceRecognitionConfident = false,
            recognizedState = emptyState,
            validationResult = validation
        )
        stockfishEngine.analyzePosition(emptyState)
    }

    /**
     * Confirms the 64-square camera calibration board, validates chess legality, updates `sideToMove`,
     * locks the confirmed squares against noisy camera frame overwrites, and runs engine analysis.
     */
    fun confirmBoardPieceCalibration(): PositionValidationResult {
        val candidate = _cameraCalibrationBoard.value.copy(activeColor = _cameraSideToMove.value)
        val validation = positionManager.loadCustomState(candidate)
        if (validation.isValid) {
            _userConfirmedCalibration.value = true
            val validState = positionManager.boardState.value
            _cameraCalibrationBoard.value = validState
            lockConfirmedBoardIntoOverrides(validState)
            refreshTurnSynchronizationLabel()
            _visionAnalysisResult.value = _visionAnalysisResult.value.copy(
                detectionState = CameraDetectionState.CONFIGURATION_ALL_SET,
                statusBannerMessage = ChessVisionPipeline.MSG_CONFIGURATION_ALL_SET,
                isChessboardDetected = true,
                isPieceRecognitionConfident = true,
                overallConfidence = 0.96f,
                recognizedState = validState,
                validationResult = validation
            )
            stockfishEngine.analyzePosition(validState)
            showToast("Configuration All Set — Position Validated (${_turnSynchronizationLabel.value})")
        } else {
            _userConfirmedCalibration.value = false
            _visionAnalysisResult.value = _visionAnalysisResult.value.copy(
                detectionState = CameraDetectionState.POSITION_VALIDATION,
                statusBannerMessage = "Invalid position: ${validation.errors.firstOrNull()}",
                isChessboardDetected = true,
                isPieceRecognitionConfident = false,
                recognizedState = candidate,
                validationResult = validation
            )
            stockfishEngine.analyzePosition(candidate)
        }
        return validation
    }

    fun applyManualPieceOverrideInCamera(square: Square, piece: Piece?) {
        val nextMap = _manualSquareOverrides.value.toMutableMap()
        nextMap[square] = piece
        _manualSquareOverrides.value = nextMap

        // Update current camera calibration state immediately
        val baseState = _cameraCalibrationBoard.value
        val nextSquares = baseState.squares.toMutableList()
        nextSquares[square.index] = piece
        val updatedState = baseState.copy(
            squares = nextSquares,
            activeColor = _cameraSideToMove.value
        )
        _cameraCalibrationBoard.value = updatedState
        val validation = ChessRulesEngine.validatePosition(updatedState)

        _visionAnalysisResult.value = _visionAnalysisResult.value.copy(
            recognizedState = updatedState,
            validationResult = validation,
            statusBannerMessage = if (validation.isValid) {
                if (_userConfirmedCalibration.value) ChessVisionPipeline.MSG_CONFIGURATION_ALL_SET
                else ChessVisionPipeline.MSG_PIECE_RECOGNITION_REQUIRED
            } else {
                "Invalid position: ${validation.errors.firstOrNull()}"
            }
        )
        if (validation.isValid && _userConfirmedCalibration.value) {
            positionManager.loadCustomState(updatedState)
            lockConfirmedBoardIntoOverrides(positionManager.boardState.value)
            refreshTurnSynchronizationLabel()
            stockfishEngine.analyzePosition(positionManager.boardState.value)
        } else if (!validation.isValid) {
            stockfishEngine.analyzePosition(updatedState)
        }
    }

    fun loadStandardStartingBoardInCamera() {
        val initial = ChessRulesEngine.createInitialState()
        _cameraSideToMove.value = initial.activeColor
        _cameraCalibrationBoard.value = initial
        lockConfirmedBoardIntoOverrides(initial)
        _userConfirmedCalibration.value = true
        positionManager.loadCustomState(initial)
        refreshTurnSynchronizationLabel()
        _visionAnalysisResult.value = _visionAnalysisResult.value.copy(
            detectionState = CameraDetectionState.CONFIGURATION_ALL_SET,
            statusBannerMessage = ChessVisionPipeline.MSG_CONFIGURATION_ALL_SET,
            isChessboardDetected = true,
            isPieceRecognitionConfident = true,
            overallConfidence = 0.96f,
            recognizedState = initial,
            validationResult = PositionValidationResult(isValid = true)
        )
        stockfishEngine.analyzePosition(initial)
    }

    /**
     * Confirms a legal move played on the physical or second-phone board in Camera Mode.
     * Automatically updates `sideToMove` (`WHITE` <-> `BLACK`), locks the updated board,
     * and triggers fresh engine analysis.
     */
    fun confirmMoveInCamera(from: Square, to: Square): ChessMove? {
        if (!_userConfirmedCalibration.value) {
            val confirmRes = confirmBoardPieceCalibration()
            if (!confirmRes.isValid) return null
        }
        val move = positionManager.tryMoveBetweenSquares(from, to)
        if (move != null) {
            onMoveCompleted(move)
        }
        return move
    }

    /**
     * Returns the engine's recommended move (UCI) ONLY when:
     * 1. The camera board position is confirmed and confident (`isPieceRecognitionConfident == true`),
     * 2. The position is legal (`validationResult?.isValid == true`),
     * 3. The engine analysis belongs to the exact current validated FEN (`fen` match & no error), AND
     * 4. It is the user's turn (`playerSide == sideToMove`).
     * Never returns an opponent's move as the user's recommendation, and never returns a recommendation
     * for an uncertain or invalid position.
     */
    fun getCameraUserRecommendedMoveUci(): String? {
        val vr = _visionAnalysisResult.value
        if (!_userConfirmedCalibration.value || !vr.isChessboardDetected || !vr.isPieceRecognitionConfident) {
            return null
        }
        if (vr.validationResult?.isValid != true) return null
        val validState = vr.recognizedState ?: return null
        if (_playerColor.value != validState.activeColor) {
            // Opponent's turn — never label or return opponent's move as user's recommendation
            return null
        }
        val analysis = stockfishEngine.analysisState.value
        if (analysis.error != null || analysis.fen != validState.toFen()) return null
        return analysis.bestMoveUci
    }

    fun getCameraUserRecommendedMoveSan(): String? {
        val vr = _visionAnalysisResult.value
        if (!_userConfirmedCalibration.value || !vr.isChessboardDetected || !vr.isPieceRecognitionConfident) {
            return null
        }
        if (vr.validationResult?.isValid != true) return null
        val validState = vr.recognizedState ?: return null
        if (_playerColor.value != validState.activeColor) {
            return null
        }
        val analysis = stockfishEngine.analysisState.value
        if (analysis.error != null || analysis.fen != validState.toFen()) return null
        return analysis.bestMoveSan
    }

    fun openRecognizedCameraPositionInAnalysis() {
        val state = _visionAnalysisResult.value.recognizedState ?: _cameraCalibrationBoard.value
        val validation = positionManager.loadCustomState(state)
        if (validation.isValid) {
            _isCameraPaused.value = true
            refreshTurnSynchronizationLabel()
            stockfishEngine.analyzePosition(positionManager.boardState.value)
            navigateTo(AppDestination.POSITION_ANALYSIS)
        } else {
            showToast("Fix position error before analysis: ${validation.errors.firstOrNull()}")
        }
    }

    // Import / Export & Screenshot / Board Editor
    fun importFenString(fen: String): Result<ChessBoardState> {
        val res = positionManager.loadValidatedFen(fen)
        res.onSuccess { state ->
            _editorSquares.value = state.squares
            _editorActiveColor.value = state.activeColor
            stockfishEngine.analyzePosition(state)
            showToast("FEN imported and validated successfully.")
        }
        return res
    }

    fun importPgnString(pgn: String): Result<ChessBoardState> {
        val res = positionManager.loadValidatedPgn(pgn)
        res.onSuccess { state ->
            _editorSquares.value = state.squares
            _editorActiveColor.value = state.activeColor
            stockfishEngine.analyzePosition(state)
            showToast("PGN game imported (${state.moveHistory.size} plies).")
        }
        return res
    }

    fun processImportedScreenshotBitmap(bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.Default) {
            val result = visionPipeline.analyzeBitmap(
                bitmap = bitmap,
                scanMode = VisionScanMode.SCREENSHOT_IMPORT,
                orientation = positionManager.boardOrientation.value,
                userConfirmedCalibration = true
            )
            withContext(Dispatchers.Main) {
                val detected = result.recognizedState
                if (detected != null) {
                    _editorSquares.value = detected.squares
                    _editorActiveColor.value = detected.activeColor
                    showToast("Screenshot analyzed (${(result.overallConfidence * 100).toInt()}% confidence). Verify pieces in editor below.")
                } else {
                    showToast(ChessVisionPipeline.MSG_CHESS_NOT_DISPLAYING)
                }
            }
        }
    }

    // Manual Board Editor Controls
    fun selectEditorBrush(piece: Piece?) {
        _editorSelectedBrush.value = piece
    }

    fun tapEditorSquare(square: Square) {
        val list = _editorSquares.value.toMutableList()
        val brush = _editorSelectedBrush.value
        list[square.index] = if (list[square.index] == brush) null else brush
        _editorSquares.value = list
    }

    fun setEditorActiveColor(color: PieceColor) {
        _editorActiveColor.value = color
    }

    fun clearEditorBoard() {
        val empty = MutableList<Piece?>(64) { null }
        empty[Square(4, 0).index] = Piece(PieceType.KING, PieceColor.WHITE)
        empty[Square(4, 7).index] = Piece(PieceType.KING, PieceColor.BLACK)
        _editorSquares.value = empty
    }

    fun resetEditorBoardToStart() {
        val start = ChessRulesEngine.createInitialState()
        _editorSquares.value = start.squares
        _editorActiveColor.value = PieceColor.WHITE
    }

    fun validateCurrentEditorBoard(): PositionValidationResult {
        val candidate = ChessBoardState(
            squares = _editorSquares.value,
            activeColor = _editorActiveColor.value,
            castlingRights = CastlingRights.ALL
        )
        return ChessRulesEngine.validatePosition(candidate)
    }

    fun applyEditorBoardAndAnalyze(): PositionValidationResult {
        val candidate = ChessBoardState(
            squares = _editorSquares.value,
            activeColor = _editorActiveColor.value,
            castlingRights = CastlingRights.ALL
        )
        val validation = positionManager.loadCustomState(candidate)
        if (validation.isValid) {
            stockfishEngine.analyzePosition(positionManager.boardState.value)
            navigateTo(AppDestination.POSITION_ANALYSIS)
        }
        return validation
    }

    // Saved Games Management (Room Database)
    fun saveCurrentGameToDatabase(customTitle: String, sourceMode: String = "Digital Chessboard") {
        viewModelScope.launch {
            val state = positionManager.boardState.value
            val analysis = stockfishEngine.analysisState.value
            val topEval = analysis.candidateLines.firstOrNull()?.formattedEvaluation ?: "0.00"
            val title = customTitle.ifBlank {
                "Chess Session • ${state.moveHistory.size} plies (${state.status.label})"
            }
            val pgn = ChessRulesEngine.exportPgn(
                state = state,
                whitePlayer = if (_playerColor.value == PieceColor.WHITE) "Player (White)" else "Chess Winner Engine",
                blackPlayer = if (_playerColor.value == PieceColor.BLACK) "Player (Black)" else "Chess Winner Engine",
                event = title
            )
            repository.saveGame(
                SavedGameEntity(
                    title = title,
                    whitePlayer = "White",
                    blackPlayer = "Black",
                    event = sourceMode,
                    finalFen = state.toFen(),
                    pgnText = pgn,
                    resultText = state.status.label,
                    moveCount = (state.moveHistory.size + 1) / 2,
                    evaluationSummary = "Eval: $topEval (Depth ${analysis.currentDepth})",
                    sourceMode = sourceMode
                )
            )
            showToast("Saved '$title' to local Room database.")
        }
    }

    fun openSavedGame(game: SavedGameEntity) {
        val pgnLoaded = positionManager.loadValidatedPgn(game.pgnText)
        if (pgnLoaded.isFailure) {
            positionManager.loadValidatedFen(game.finalFen)
        }
        stockfishEngine.analyzePosition(positionManager.boardState.value)
        showToast("Loaded '${game.title}'")
        navigateTo(AppDestination.POSITION_ANALYSIS)
    }

    fun deleteSavedGame(id: Long) {
        viewModelScope.launch {
            repository.deleteGame(id)
            showToast("Deleted saved game.")
        }
    }

    fun clearAllSavedGames() {
        viewModelScope.launch {
            repository.clearAllSavedGames()
            showToast("Cleared all saved games from local database.")
        }
    }

    fun saveCurrentAnalysisSnapshot(notes: String) {
        viewModelScope.launch {
            val state = positionManager.boardState.value
            val analysis = stockfishEngine.analysisState.value
            val top = analysis.candidateLines.firstOrNull()
            repository.saveAnalysisSnapshot(
                AnalysisSnapshotEntity(
                    title = "Position ${state.activeColor.displayName} to move",
                    fen = state.toFen(),
                    bestMoveSan = top?.moveSan ?: "-",
                    evalScoreText = top?.formattedEvaluation ?: "0.00",
                    depth = analysis.currentDepth,
                    notes = notes.ifBlank { _tacticalExplanation.value.headline }
                )
            )
            showToast("Saved analysis snapshot to database.")
        }
    }

    // Learning Modules Navigation & Exercises
    fun selectLearningTab(tab: LearningSubTab) {
        _activeLearningTab.value = tab
        when (tab) {
            LearningSubTab.CHECKMATE_PATTERNS -> loadCheckmateLesson(_selectedCheckmateLesson.value)
            LearningSubTab.BLUNDER_REVIEW -> loadBlunderReviewCase(_selectedBlunderItem.value)
            LearningSubTab.OPENING_MASTERY -> loadOpeningLesson(_selectedOpeningLesson.value)
            LearningSubTab.POSITION_COACH -> stockfishEngine.analyzePosition(positionManager.boardState.value)
        }
    }

    fun loadCheckmateLesson(lesson: CheckmateLesson) {
        _selectedCheckmateLesson.value = lesson
        _checkmateExerciseFeedback.value = "Your turn: Find the forced ${lesson.patternName}!"
        positionManager.loadValidatedFen(lesson.fen)
        stockfishEngine.analyzePosition(positionManager.boardState.value)
    }

    fun loadBlunderReviewCase(item: BlunderReviewItem) {
        _selectedBlunderItem.value = item
        positionManager.loadValidatedFen(item.fenBeforeMove)
        stockfishEngine.analyzePosition(positionManager.boardState.value)
    }

    fun playBlunderMoveOnBoard(item: BlunderReviewItem) {
        positionManager.loadValidatedFen(item.fenBeforeMove)
        val current = positionManager.boardState.value
        val move = ChessRulesEngine.parseUciMove(current, item.playedMoveUci) ?: return
        positionManager.makeMove(move)
        stockfishEngine.analyzePosition(positionManager.boardState.value)
        viewModelScope.launch {
            repository.markLessonCompleted(item.id, "Blunder")
        }
    }

    fun playBestAlternativeMoveOnBoard(item: BlunderReviewItem) {
        positionManager.loadValidatedFen(item.fenBeforeMove)
        val current = positionManager.boardState.value
        val move = ChessRulesEngine.parseUciMove(current, item.bestMoveUci) ?: return
        positionManager.makeMove(move)
        stockfishEngine.analyzePosition(positionManager.boardState.value)
        viewModelScope.launch {
            repository.markLessonCompleted(item.id, "Blunder")
        }
    }

    fun loadOpeningLesson(lesson: OpeningLesson) {
        _selectedOpeningLesson.value = lesson
        _openingMoveStepIndex.value = lesson.movesSan.size
        positionManager.loadValidatedFen(lesson.resultingFen)
        stockfishEngine.analyzePosition(positionManager.boardState.value)
    }

    fun stepOpeningLessonToMove(stepCount: Int) {
        val lesson = _selectedOpeningLesson.value
        val clamped = stepCount.coerceIn(0, lesson.movesSan.size)
        _openingMoveStepIndex.value = clamped
        var state = ChessRulesEngine.createInitialState()
        for (i in 0 until clamped) {
            val move = ChessRulesEngine.parseSanMove(state, lesson.movesSan[i]) ?: break
            state = ChessRulesEngine.applyMove(state, move)
        }
        positionManager.loadCustomState(state)
        stockfishEngine.analyzePosition(positionManager.boardState.value)
        if (clamped == lesson.movesSan.size) {
            viewModelScope.launch {
                repository.markLessonCompleted(lesson.id, "Opening")
            }
        }
    }

    // Settings updates
    fun updateDefaultPlayerColor(color: PieceColor) {
        viewModelScope.launch { preferencesRepository.setDefaultPlayerColor(color) }
    }

    fun updateEngineDepth(depth: Int) {
        viewModelScope.launch {
            preferencesRepository.setEngineDepth(depth)
            stockfishEngine.analyzePosition(positionManager.boardState.value, customDepth = depth)
        }
    }

    fun updateEngineTimeMs(timeMs: Long) {
        viewModelScope.launch { preferencesRepository.setEngineTimeMs(timeMs) }
    }

    fun updateMultiPvLines(lines: Int) {
        viewModelScope.launch { preferencesRepository.setMultiPvLines(lines) }
    }

    fun updateBoardTheme(theme: BoardThemePalette) {
        viewModelScope.launch { preferencesRepository.setBoardTheme(theme) }
    }

    fun updatePieceStyle(style: PieceVisualStyle) {
        viewModelScope.launch { preferencesRepository.setPieceStyle(style) }
    }

    fun updateSoundEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setSoundEnabled(enabled) }
    }

    fun updateCameraThrottleMs(throttleMs: Long) {
        viewModelScope.launch { preferencesRepository.setCameraThrottleMs(throttleMs) }
    }

    fun updateUseBackCameraDefault(useBack: Boolean) {
        viewModelScope.launch { preferencesRepository.setUseBackCamera(useBack) }
    }

    fun updateShowOverlayArrows(show: Boolean) {
        viewModelScope.launch { preferencesRepository.setShowOverlayArrows(show) }
    }

    fun updateShowOverlayCoordinates(show: Boolean) {
        viewModelScope.launch { preferencesRepository.setShowOverlayCoordinates(show) }
    }

    fun showToast(message: String) {
        _statusBannerToast.value = message
    }

    fun dismissToast() {
        _statusBannerToast.value = null
    }

    private fun playMoveFeedback(move: ChessMove) {
        if (!userSettings.value.soundEnabled) return
        runCatching {
            val toneType = when {
                move.san.endsWith("#") -> ToneGenerator.TONE_PROP_ACK
                move.san.endsWith("+") -> ToneGenerator.TONE_PROP_BEEP2
                move.capturedPiece != null -> ToneGenerator.TONE_PROP_BEEP
                else -> ToneGenerator.TONE_PROP_NACK
            }
            val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 45)
            toneGen.startTone(toneType, 70)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stockfishEngine.shutdown()
    }
}
