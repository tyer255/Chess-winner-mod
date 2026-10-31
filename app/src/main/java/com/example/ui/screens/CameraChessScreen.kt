package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Paint
import android.graphics.Typeface
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.chess.core.ChessBoardState
import com.example.chess.core.ChessRulesEngine
import com.example.chess.core.Piece
import com.example.chess.core.PieceColor
import com.example.chess.core.PieceType
import com.example.chess.core.Square
import com.example.chess.engine.EngineAnalysisResult
import com.example.chess.vision.BoardCorners
import com.example.chess.vision.CameraDetectionState
import com.example.chess.vision.ChessVisionPipeline
import com.example.chess.vision.NormalizedPoint
import com.example.chess.vision.VisionFrameAnalysisResult
import com.example.chess.vision.VisionScanMode
import com.example.data.UserSettingsState
import com.example.ui.components.InteractiveChessboard
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CharcoalBorder
import com.example.ui.theme.CharcoalCard
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DeepCyanContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.NavyBackground
import com.example.ui.theme.NavySurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.concurrent.Executors
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@Composable
fun CameraChessScreen(
    scanMode: VisionScanMode,
    visionResult: VisionFrameAnalysisResult,
    calibratedCorners: BoardCorners,
    isCameraPaused: Boolean,
    useBackCamera: Boolean,
    orientation: PieceColor,
    playerSide: PieceColor,
    sideToMove: PieceColor,
    turnStatusLabel: String,
    cameraCalibrationBoard: ChessBoardState,
    cameraSideToMove: PieceColor,
    userRecommendedMoveUci: String?,
    userRecommendedMoveSan: String?,
    boardState: ChessBoardState,
    engineAnalysis: EngineAnalysisResult,
    userSettings: UserSettingsState,
    onSetScanMode: (VisionScanMode) -> Unit,
    onCameraLifecycleState: (Boolean, String?) -> Unit,
    onFrameLuminanceAvailable: (ByteArray, Int, Int) -> Unit,
    onTogglePause: () -> Unit,
    onToggleCameraLens: () -> Unit,
    onFlipBoardOrientation: () -> Unit,
    onUpdateCorners: (BoardCorners) -> Unit,
    onConfirmCalibration: () -> Unit,
    onLoadStandardStartingBoard: () -> Unit,
    onClearCalibrationBoard: () -> Unit,
    onSetCameraSideToMove: (PieceColor) -> Unit,
    onManualPieceOverride: (Square, Piece?) -> Unit,
    onConfirmMoveInCamera: (Square, Square) -> Unit,
    onPlayRecommendedMoveInCamera: () -> Unit,
    onOpenInFullAnalysis: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            onCameraLifecycleState(false, "Camera permission denied — Grant permission or use manual calibration")
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var showManualCorrectionPanel by remember { mutableStateOf(false) }
    var selectedCorrectionBrush by remember {
        mutableStateOf<Piece?>(Piece(PieceType.QUEEN, PieceColor.WHITE))
    }
    var moveTrackingSourceSquare by remember { mutableStateOf<Square?>(null) }

    val activeBoardForSync = visionResult.recognizedState ?: cameraCalibrationBoard
    val effectiveSideToMove = if (visionResult.isPieceRecognitionConfident && visionResult.validationResult?.isValid == true) {
        sideToMove
    } else {
        cameraSideToMove
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(NavyBackground)
            .testTag("camera_chess_screen"),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Header Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 620.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("camera_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                    Column {
                        Text(
                            text = scanMode.displayName,
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = scanMode.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }

                Row {
                    IconButton(
                        onClick = onTogglePause,
                        modifier = Modifier.testTag("camera_pause_resume_button")
                    ) {
                        Icon(
                            imageVector = if (isCameraPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (isCameraPaused) "Resume Camera" else "Pause Camera",
                            tint = CyanAccent
                        )
                    }
                    IconButton(
                        onClick = onToggleCameraLens,
                        modifier = Modifier.testTag("camera_switch_lens_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cameraswitch,
                            contentDescription = "Switch Front/Back Camera",
                            tint = CyanAccent
                        )
                    }
                }
            }
        }

        // 2. Scan Mode Selector (Physical Chessboard vs Second Mobile Phone Display)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 620.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = scanMode == VisionScanMode.PHYSICAL_BOARD,
                    onClick = { onSetScanMode(VisionScanMode.PHYSICAL_BOARD) },
                    label = { Text("Physical Chessboard") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("mode_physical_board_chip")
                )
                FilterChip(
                    selected = scanMode == VisionScanMode.SECOND_PHONE_DISPLAY,
                    onClick = { onSetScanMode(VisionScanMode.SECOND_PHONE_DISPLAY) },
                    label = { Text("2nd Phone Display") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("mode_second_phone_chip")
                )
            }
        }

        // 3. Detection State & Status Banner
        item {
            DetectionStatusBanner(
                visionResult = visionResult,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 620.dp)
            )
        }

        // 4. Player Side & Turn Synchronization Card
        item {
            PlayerTurnSyncCard(
                playerSide = playerSide,
                sideToMove = effectiveSideToMove,
                turnStatusLabel = turnStatusLabel,
                onSetSideToMove = onSetCameraSideToMove,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 620.dp)
            )
        }

        // 5. Live Camera Engine Analysis & Move Recommendation Card
        item {
            CameraEngineAnalysisCard(
                visionResult = visionResult,
                playerSide = playerSide,
                sideToMove = effectiveSideToMove,
                turnStatusLabel = turnStatusLabel,
                userRecommendedMoveUci = userRecommendedMoveUci,
                userRecommendedMoveSan = userRecommendedMoveSan,
                engineAnalysis = engineAnalysis,
                onPlayRecommendedMove = onPlayRecommendedMoveInCamera,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 620.dp)
            )
        }

        // 6. Live CameraX Preview + Aligned 8x8 Perspective Overlay
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF050914))
                    .border(1.5.dp, CyanAccent.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .testTag("camera_preview_container")
            ) {
                if (hasCameraPermission && !isCameraPaused) {
                    CameraXLivePreview(
                        useBackCamera = useBackCamera,
                        onCameraReady = { ready, err -> onCameraLifecycleState(ready, err) },
                        onLuminanceFrame = onFrameLuminanceAvailable,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (!hasCameraPermission) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Permission Required",
                            tint = CrimsonError,
                            modifier = Modifier.size(42.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Camera Permission Required",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Grant CAMERA permission to scan physical or second-phone chessboards, or use the manual calibration tools below.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black),
                            modifier = Modifier.testTag("request_camera_permission_button")
                        ) {
                            Text("Grant Camera Permission")
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0B132B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Camera Preview Paused • Overlay Calibration Active",
                            style = MaterialTheme.typography.titleMedium,
                            color = CyanAccent
                        )
                    }
                }

                // Perspective-Aligned 8x8 Board Overlay (Corners, Grid, Coordinates, and Validated User Turn Best-Move Arrow)
                CameraBoardOverlayCanvas(
                    corners = calibratedCorners,
                    orientation = orientation,
                    visionResult = visionResult,
                    showCoordinates = userSettings.showOverlayCoordinates,
                    recommendedMoveUci = if (userSettings.showOverlayArrows) userRecommendedMoveUci else null,
                    onUpdateCorners = onUpdateCorners,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // 7. 7-Step Camera Detection Pipeline Progress Strip
        item {
            CameraPipelineStepsRow(
                currentState = visionResult.detectionState,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 620.dp)
            )
        }

        // 8. Calibration, Manual Piece Correction & Live Move Sync Controls
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 620.dp)
                    .border(1.dp, CharcoalBorder, RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CharcoalCard)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "BOARD CALIBRATION & LIVE MOVE SYNCHRONIZATION",
                        style = MaterialTheme.typography.labelLarge,
                        color = CyanAccent
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = visionResult.modelStatusLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                moveTrackingSourceSquare = null
                                onConfirmCalibration()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldSuccess,
                                contentColor = Color(0xFF022C22)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("confirm_camera_calibration_button")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Confirm Board", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                moveTrackingSourceSquare = null
                                onLoadStandardStartingBoard()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("calibrate_starting_board_button")
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reset Start Grid")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                moveTrackingSourceSquare = null
                                showManualCorrectionPanel = !showManualCorrectionPanel
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("toggle_manual_correction_button")
                        ) {
                            Icon(
                                imageVector = if (showManualCorrectionPanel) Icons.Default.TouchApp else Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (showManualCorrectionPanel) "Track Legal Moves" else "Correct Pieces")
                        }

                        OutlinedButton(
                            onClick = onFlipBoardOrientation,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("camera_flip_orientation_button")
                        ) {
                            Icon(Icons.Default.FlipCameraAndroid, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Flip View (${orientation.displayName})")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Interactive Synchronized 8x8 Board (supports either Legal Move Tracking or 64-Square Piece Brush Correction)
                    val legalMovesFromSelected = remember(activeBoardForSync, moveTrackingSourceSquare, showManualCorrectionPanel) {
                        val src = moveTrackingSourceSquare
                        if (!showManualCorrectionPanel && src != null && visionResult.validationResult?.isValid == true) {
                            ChessRulesEngine.generateLegalMovesFromSquare(activeBoardForSync, src)
                        } else {
                            emptyList()
                        }
                    }

                    if (showManualCorrectionPanel) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PIECE CORRECTION PALETTE (Tap square to set piece)",
                                style = MaterialTheme.typography.labelMedium,
                                color = AmberWarning
                            )
                            OutlinedButton(
                                onClick = onClearCalibrationBoard,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("camera_clear_board_button")
                            ) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear Board", fontSize = 12.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        PieceBrushSelectorRow(
                            selectedBrush = selectedCorrectionBrush,
                            onSelectBrush = { selectedCorrectionBrush = it }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    } else {
                        Text(
                            text = "LIVE MOVE SYNC BOARD: Tap a ${effectiveSideToMove.displayName} piece and destination square as moves are played on the 2nd phone or physical board.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    InteractiveChessboard(
                        boardState = activeBoardForSync,
                        orientation = orientation,
                        selectedSquare = if (showManualCorrectionPanel) null else moveTrackingSourceSquare,
                        legalMoves = legalMovesFromSelected,
                        recommendedMoveUci = userRecommendedMoveUci,
                        boardTheme = userSettings.boardTheme,
                        pieceStyle = userSettings.pieceStyle,
                        showBestMoveArrow = userSettings.showOverlayArrows && userRecommendedMoveUci != null,
                        onSquareTapped = { tappedSquare ->
                            if (showManualCorrectionPanel) {
                                onManualPieceOverride(tappedSquare, selectedCorrectionBrush)
                            } else {
                                val currentSrc = moveTrackingSourceSquare
                                if (currentSrc == null) {
                                    val pieceAtSq = activeBoardForSync.pieceAt(tappedSquare)
                                    if (pieceAtSq != null && pieceAtSq.color == activeBoardForSync.activeColor) {
                                        moveTrackingSourceSquare = tappedSquare
                                    }
                                } else if (currentSrc == tappedSquare) {
                                    moveTrackingSourceSquare = null
                                } else {
                                    val isLegalDest = legalMovesFromSelected.any { move -> move.to == tappedSquare }
                                    if (isLegalDest) {
                                        moveTrackingSourceSquare = null
                                        onConfirmMoveInCamera(currentSrc, tappedSquare)
                                    } else {
                                        val pieceAtSq = activeBoardForSync.pieceAt(tappedSquare)
                                        moveTrackingSourceSquare = if (pieceAtSq != null && pieceAtSq.color == activeBoardForSync.activeColor) {
                                            tappedSquare
                                        } else {
                                            null
                                        }
                                    }
                                }
                            }
                        },
                        onPieceDragged = { from, to ->
                            if (!showManualCorrectionPanel) {
                                moveTrackingSourceSquare = null
                                onConfirmMoveInCamera(from, to)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 400.dp)
                            .align(Alignment.CenterHorizontally)
                            .testTag("camera_sync_board")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = onOpenInFullAnalysis,
                        enabled = visionResult.validationResult?.isValid != false,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanAccent,
                            contentColor = Color(0xFF04131D)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_camera_board_in_analysis_button")
                    ) {
                        Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Open Recognized Position in Stockfish Analysis",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerTurnSyncCard(
    playerSide: PieceColor,
    sideToMove: PieceColor,
    turnStatusLabel: String,
    onSetSideToMove: (PieceColor) -> Unit,
    modifier: Modifier = Modifier
) {
    val isUserTurn = playerSide == sideToMove
    val badgeColor = if (isUserTurn) EmeraldSuccess else AmberWarning
    val badgeBg = if (isUserTurn) Color(0xFF062E22) else Color(0xFF2E1F05)

    Card(
        modifier = modifier
            .border(1.dp, CharcoalBorder, RoundedCornerShape(14.dp))
            .testTag("camera_turn_sync_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CharcoalCard)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Player Side: ${playerSide.name}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.testTag("camera_player_side_text")
                    )
                    Text(
                        text = "Side to Move: ${sideToMove.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CyanAccent,
                        modifier = Modifier.testTag("camera_side_to_move_text")
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeBg,
                    modifier = Modifier
                        .border(1.5.dp, badgeColor, RoundedCornerShape(8.dp))
                ) {
                    Text(
                        text = turnStatusLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("camera_turn_status_badge")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active Turn on Board:",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                FilterChip(
                    selected = sideToMove == PieceColor.WHITE,
                    onClick = { onSetSideToMove(PieceColor.WHITE) },
                    label = { Text("White to Move") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("camera_set_white_turn_chip")
                )
                FilterChip(
                    selected = sideToMove == PieceColor.BLACK,
                    onClick = { onSetSideToMove(PieceColor.BLACK) },
                    label = { Text("Black to Move") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("camera_set_black_turn_chip")
                )
            }
        }
    }
}

@Composable
private fun CameraEngineAnalysisCard(
    visionResult: VisionFrameAnalysisResult,
    playerSide: PieceColor,
    sideToMove: PieceColor,
    turnStatusLabel: String,
    userRecommendedMoveUci: String?,
    userRecommendedMoveSan: String?,
    engineAnalysis: EngineAnalysisResult,
    onPlayRecommendedMove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isBoardValidatedAndConfirmed =
        visionResult.isChessboardDetected &&
            visionResult.isPieceRecognitionConfident &&
            visionResult.validationResult?.isValid == true

    val isInvalidPosition = visionResult.validationResult?.isValid == false
    val isUserTurn = playerSide == sideToMove
    val currentValidFen = visionResult.recognizedState?.toFen()
    val isEngineMatched = isBoardValidatedAndConfirmed &&
        currentValidFen != null &&
        engineAnalysis.fen == currentValidFen &&
        engineAnalysis.error == null

    val borderColor = when {
        isInvalidPosition -> CrimsonError
        !isBoardValidatedAndConfirmed -> AmberWarning
        isUserTurn -> EmeraldSuccess
        else -> CyanAccent
    }

    Card(
        modifier = modifier
            .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
            .testTag("camera_engine_analysis_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = NavySurface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LIVE CAMERA ENGINE ANALYSIS",
                    style = MaterialTheme.typography.labelLarge,
                    color = CyanAccent
                )
                if (isEngineMatched) {
                    val evalFormatted = if (engineAnalysis.mateInMoves != null) {
                        val m = engineAnalysis.mateInMoves
                        if (m > 0) "+M$m" else "-M${kotlin.math.abs(m)}"
                    } else {
                        val pawns = engineAnalysis.evaluationCentipawns / 100.0
                        val sign = if (pawns > 0) "+" else ""
                        String.format("%s%.2f", sign, pawns)
                    }
                    Text(
                        text = "Eval: $evalFormatted (d=${engineAnalysis.currentDepth})",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldSuccess,
                        modifier = Modifier.testTag("camera_evaluation_text")
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            when {
                isInvalidPosition -> {
                    val err = visionResult.validationResult?.errors?.firstOrNull() ?: "Illegal board configuration"
                    Text(
                        text = "Invalid position: $err — No best-move recommendation is generated for an invalid position.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = CrimsonError,
                        modifier = Modifier.testTag("camera_analysis_status_text")
                    )
                }

                !isBoardValidatedAndConfirmed -> {
                    Text(
                        text = "Uncertain / Unconfirmed Camera Scan — Verify pieces on the Synchronized Board below and tap 'Confirm Board' to generate engine recommendations.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AmberWarning,
                        modifier = Modifier.testTag("camera_analysis_status_text")
                    )
                }

                !isUserTurn -> {
                    Text(
                        text = "$turnStatusLabel (${sideToMove.displayName} to move) — Waiting for opponent move on the chessboard.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AmberWarning,
                        modifier = Modifier.testTag("camera_analysis_status_text")
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "When ${sideToMove.displayName} plays a move on the 2nd phone or physical board, tap that move on the Live Move Sync Board below to switch to YOUR TURN.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }

                userRecommendedMoveSan != null && userRecommendedMoveUci != null -> {
                    Text(
                        text = "Recommended Move (${playerSide.displayName}): $userRecommendedMoveSan ($userRecommendedMoveUci)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldSuccess,
                        modifier = Modifier.testTag("camera_recommended_move_text")
                    )

                    if (engineAnalysis.candidateLines.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        engineAnalysis.candidateLines.take(3).forEach { line ->
                            val previewSan = line.principalVariationSan.take(4).joinToString(" ")
                            Text(
                                text = "#${line.multiPvIndex} [${line.formattedEvaluation}] $previewSan",
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onPlayRecommendedMove,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldSuccess,
                            contentColor = Color(0xFF022C22)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("camera_play_recommended_move_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Apply Recommended Move ($userRecommendedMoveSan) on Sync Board",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                else -> {
                    Text(
                        text = "Analyzing validated position for ${playerSide.displayName}...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CyanAccent,
                        modifier = Modifier.testTag("camera_analysis_status_text")
                    )
                }
            }
        }
    }
}

@Composable
private fun DetectionStatusBanner(
    visionResult: VisionFrameAnalysisResult,
    modifier: Modifier = Modifier
) {
    val isAllSet = visionResult.statusBannerMessage == ChessVisionPipeline.MSG_CONFIGURATION_ALL_SET
    val isNotDisplaying = visionResult.statusBannerMessage.startsWith(ChessVisionPipeline.MSG_CHESS_NOT_DISPLAYING)
    val isPieceReq = visionResult.statusBannerMessage == ChessVisionPipeline.MSG_PIECE_RECOGNITION_REQUIRED

    val (borderColor, bgColor, icon) = when {
        isAllSet -> Triple(EmeraldSuccess, Color(0xFF062E22), Icons.Default.Verified)
        isPieceReq -> Triple(AmberWarning, Color(0xFF2E1F05), Icons.Default.Warning)
        isNotDisplaying -> Triple(CrimsonError, Color(0xFF2D0A12), Icons.Default.Error)
        else -> Triple(CyanAccent, NavySurface, Icons.Default.CheckCircle)
    }

    Card(
        modifier = modifier
            .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
            .testTag("camera_status_banner"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = visionResult.statusBannerMessage,
                tint = borderColor,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = visionResult.statusBannerMessage,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.testTag("camera_status_banner_text")
                )
                Text(
                    text = "Stage ${visionResult.detectionState.stepNumber}/7: ${visionResult.detectionState.title} • Confidence ${(visionResult.overallConfidence * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun CameraXLivePreview(
    useBackCamera: Boolean,
    onCameraReady: (Boolean, String?) -> Unit,
    onLuminanceFrame: (ByteArray, Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    DisposableEffect(lifecycleOwner, useBackCamera) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val listener = Runnable {
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    try {
                        val plane = imageProxy.planes.firstOrNull()
                        if (plane != null) {
                            val (bytes, uprightW, uprightH) = ChessVisionPipeline.extractUprightLuminanceFromYPlane(
                                buffer = plane.buffer,
                                width = imageProxy.width,
                                height = imageProxy.height,
                                rowStride = plane.rowStride,
                                pixelStride = plane.pixelStride,
                                rotationDegrees = imageProxy.imageInfo.rotationDegrees
                            )
                            onLuminanceFrame(bytes, uprightW, uprightH)
                        }
                    } catch (_: Exception) {
                    } finally {
                        imageProxy.close()
                    }
                }

                val desiredSelector = if (useBackCamera) {
                    CameraSelector.DEFAULT_BACK_CAMERA
                } else {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                }

                cameraProvider.unbindAll()
                val activeSelector = when {
                    cameraProvider.hasCamera(desiredSelector) -> desiredSelector
                    cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> CameraSelector.DEFAULT_BACK_CAMERA
                    cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> CameraSelector.DEFAULT_FRONT_CAMERA
                    else -> null
                }

                if (activeSelector != null) {
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        activeSelector,
                        preview,
                        imageAnalysis
                    )
                    // Support manual tap-to-focus on PreviewView
                    previewView.setOnTouchListener { view, event ->
                        if (event.action == android.view.MotionEvent.ACTION_UP) {
                            val factory = previewView.meteringPointFactory
                            val point = factory.createPoint(event.x, event.y)
                            val action = FocusMeteringAction.Builder(point).build()
                            camera.cameraControl.startFocusAndMetering(action)
                            view.performClick()
                        }
                        true
                    }
                    onCameraReady(true, null)
                } else {
                    onCameraReady(false, ChessVisionPipeline.MSG_CHESS_NOT_DISPLAYING)
                }
            } catch (e: Exception) {
                onCameraReady(false, "Camera unavailable: ${e.localizedMessage ?: "Hardware error"}")
            }
        }

        cameraProviderFuture.addListener(listener, ContextCompat.getMainExecutor(context))

        onDispose {
            runCatching {
                cameraProviderFuture.get().unbindAll()
            }
            cameraExecutor.shutdown()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    )
}

/**
 * Section 14: CAMERA BOARD OVERLAY
 * Draws perspective-corrected 8x8 grid, draggable 4-corner calibration handles,
 * algebraic coordinates, recognition confidence badges, and Stockfish arrows
 * when the recognized position is validated and it is the user's turn.
 */
@Composable
private fun CameraBoardOverlayCanvas(
    corners: BoardCorners,
    orientation: PieceColor,
    visionResult: VisionFrameAnalysisResult,
    showCoordinates: Boolean,
    recommendedMoveUci: String?,
    onUpdateCorners: (BoardCorners) -> Unit,
    modifier: Modifier = Modifier
) {
    var draggingCornerIndex by remember { mutableStateOf<Int?>(null) }

    Canvas(
        modifier = modifier
            .pointerInput(corners) {
                detectDragGestures(
                    onDragStart = { startOffset ->
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        val pts = listOf(
                            corners.topLeft,
                            corners.topRight,
                            corners.bottomRight,
                            corners.bottomLeft
                        )
                        val closestIdx = pts.indices.minByOrNull { idx ->
                            val px = pts[idx].x * w
                            val py = pts[idx].y * h
                            hypot(startOffset.x - px, startOffset.y - py)
                        }
                        if (closestIdx != null) {
                            val px = pts[closestIdx].x * w
                            val py = pts[closestIdx].y * h
                            if (hypot(startOffset.x - px, startOffset.y - py) < 64f) {
                                draggingCornerIndex = closestIdx
                            }
                        }
                    },
                    onDrag = { change, _ ->
                        val idx = draggingCornerIndex ?: return@detectDragGestures
                        change.consume()
                        val nx = (change.position.x / size.width.toFloat()).coerceIn(0.03f, 0.97f)
                        val ny = (change.position.y / size.height.toFloat()).coerceIn(0.03f, 0.97f)
                        val newPt = NormalizedPoint(nx, ny)
                        val updated = when (idx) {
                            0 -> corners.copy(topLeft = newPt)
                            1 -> corners.copy(topRight = newPt)
                            2 -> corners.copy(bottomRight = newPt)
                            else -> corners.copy(bottomLeft = newPt)
                        }
                        onUpdateCorners(updated)
                    },
                    onDragEnd = { draggingCornerIndex = null },
                    onDragCancel = { draggingCornerIndex = null }
                )
            }
    ) {
        val w = size.width
        val h = size.height

        // 1. Draw 8x8 perspective grid lines
        val gridColor = if (visionResult.isChessboardDetected) {
            CyanAccent.copy(alpha = 0.65f)
        } else {
            AmberWarning.copy(alpha = 0.45f)
        }

        for (i in 0..8) {
            val t = i / 8f
            val hStart = corners.mapBoardToOverlay(0f, t)
            val hEnd = corners.mapBoardToOverlay(1f, t)
            drawLine(
                color = gridColor,
                start = Offset(hStart.x * w, hStart.y * h),
                end = Offset(hEnd.x * w, hEnd.y * h),
                strokeWidth = if (i == 0 || i == 8) 3.5f else 1.5f
            )

            val vStart = corners.mapBoardToOverlay(t, 0f)
            val vEnd = corners.mapBoardToOverlay(t, 1f)
            drawLine(
                color = gridColor,
                start = Offset(vStart.x * w, vStart.y * h),
                end = Offset(vEnd.x * w, vEnd.y * h),
                strokeWidth = if (i == 0 || i == 8) 3.5f else 1.5f
            )
        }

        // 2. Draw coordinates along rank/file edges
        if (showCoordinates) {
            val coordPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = CyanAccent.toArgb()
                textSize = w * 0.028f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            }
            for (c in 0 until 8) {
                val fileChar = if (orientation == PieceColor.WHITE) ('a' + c) else ('h' - c)
                val pt = corners.mapBoardToOverlay((c + 0.5f) / 8f, 0.97f)
                drawContext.canvas.nativeCanvas.drawText(
                    "$fileChar",
                    pt.x * w,
                    pt.y * h,
                    coordPaint
                )
            }
        }

        // 3. Draw directional arrow for recommended move ONLY when position is validated, confident & user's turn
        if (recommendedMoveUci != null && recommendedMoveUci.length >= 4) {
            val fromSq = Square.fromAlgebraic(recommendedMoveUci.substring(0, 2))
            val toSq = Square.fromAlgebraic(recommendedMoveUci.substring(2, 4))
            if (fromSq != null && toSq != null) {
                val p1 = corners.squareCenterInOverlay(fromSq, orientation)
                val p2 = corners.squareCenterInOverlay(toSq, orientation)
                val start = Offset(p1.x * w, p1.y * h)
                val end = Offset(p2.x * h * (w / h), p2.y * h)

                drawCircle(color = EmeraldSuccess.copy(alpha = 0.55f), radius = w * 0.035f, center = start)
                drawLine(
                    color = EmeraldSuccess,
                    start = start,
                    end = end,
                    strokeWidth = w * 0.018f,
                    cap = StrokeCap.Round
                )
                val angle = atan2(end.y - start.y, end.x - start.x)
                val headLen = w * 0.045f
                val arrowPath = Path().apply {
                    moveTo(end.x, end.y)
                    lineTo(end.x - headLen * cos(angle - 0.5f), end.y - headLen * sin(angle - 0.5f))
                    lineTo(end.x - headLen * cos(angle + 0.5f), end.y - headLen * sin(angle + 0.5f))
                    close()
                }
                drawPath(arrowPath, color = EmeraldSuccess)
            }
        }

        // 4. Draw 4 draggable corner calibration handles
        val handlePoints = listOf(
            corners.topLeft,
            corners.topRight,
            corners.bottomRight,
            corners.bottomLeft
        )
        handlePoints.forEach { pt ->
            val center = Offset(pt.x * w, pt.y * h)
            drawCircle(color = Color(0xAA080E1E), radius = 18f, center = center)
            drawCircle(color = CyanAccent, radius = 18f, center = center, style = Stroke(width = 4f))
            drawCircle(color = CyanAccent, radius = 5f, center = center)
        }
    }
}

@Composable
private fun CameraPipelineStepsRow(
    currentState: CameraDetectionState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        CameraDetectionState.entries.forEach { step ->
            val isCompleted = step.stepNumber < currentState.stepNumber
            val isCurrent = step == currentState
            val bg = when {
                isCurrent -> DeepCyanContainer
                isCompleted -> Color(0xFF062E22)
                else -> CharcoalCard
            }
            val border = when {
                isCurrent -> CyanAccent
                isCompleted -> EmeraldSuccess
                else -> CharcoalBorder
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = bg,
                modifier = Modifier.border(1.dp, border, RoundedCornerShape(8.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(border)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${step.stepNumber}. ${step.title}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isCurrent || isCompleted) TextPrimary else TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun PieceBrushSelectorRow(
    selectedBrush: Piece?,
    onSelectBrush: (Piece?) -> Unit
) {
    val whitePieces = PieceType.entries.map { Piece(it, PieceColor.WHITE) }
    val blackPieces = PieceType.entries.map { Piece(it, PieceColor.BLACK) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Empty square eraser
            BrushChip(
                label = "Empty",
                symbol = "✕",
                isSelected = selectedBrush == null,
                onClick = { onSelectBrush(null) }
            )
            whitePieces.forEach { piece ->
                BrushChip(
                    label = "W${piece.type.algebraicSymbol.ifEmpty { "P" }}",
                    symbol = piece.unicodeSymbol,
                    isSelected = selectedBrush == piece,
                    onClick = { onSelectBrush(piece) }
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            blackPieces.forEach { piece ->
                BrushChip(
                    label = "B${piece.type.algebraicSymbol.ifEmpty { "P" }}",
                    symbol = piece.unicodeSymbol,
                    isSelected = selectedBrush == piece,
                    onClick = { onSelectBrush(piece) }
                )
            }
        }
    }
}

@Composable
private fun BrushChip(
    label: String,
    symbol: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) DeepCyanContainer else CharcoalCard,
        modifier = Modifier
            .border(
                if (isSelected) 1.5.dp else 1.dp,
                if (isSelected) CyanAccent else CharcoalBorder,
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = symbol, fontSize = 18.sp, color = CyanAccent)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = TextPrimary
            )
        }
    }
}
