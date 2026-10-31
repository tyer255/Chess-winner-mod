package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.core.ChessBoardState
import com.example.chess.core.ChessMove
import com.example.chess.core.GameStatus
import com.example.chess.core.PieceColor
import com.example.chess.core.PieceType
import com.example.chess.core.Square
import com.example.chess.engine.CandidateLine
import com.example.chess.engine.EngineAnalysisResult
import com.example.chess.learning.TacticalExplanation
import com.example.data.UserSettingsState
import com.example.ui.components.EvaluationBar
import com.example.ui.components.InteractiveChessboard
import com.example.ui.components.PromotionSelectionDialog
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CharcoalBorder
import com.example.ui.theme.CharcoalCard
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DeepCyanContainer
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NavyBackground
import com.example.ui.theme.NavySurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun DigitalChessAndAnalysisScreen(
    isAnalysisFocusMode: Boolean,
    boardState: ChessBoardState,
    orientation: PieceColor,
    selectedSquare: Square?,
    legalMoves: List<ChessMove>,
    pendingPromotionMoves: List<ChessMove>,
    engineAnalysis: EngineAnalysisResult,
    tacticalExplanation: TacticalExplanation,
    userSettings: UserSettingsState,
    playAgainstEngine: Boolean,
    aiCoachingText: String?,
    isAiCoachingLoading: Boolean,
    aiCoachingError: String?,
    onSquareTapped: (Square) -> Unit,
    onPieceDragged: (Square, Square) -> Unit,
    onSelectPromotion: (PieceType) -> Unit,
    onCancelPromotion: () -> Unit,
    onUndoMove: () -> Unit,
    onRestartGame: () -> Unit,
    onFlipBoard: () -> Unit,
    onTogglePlayAgainstEngine: (Boolean) -> Unit,
    onPlayRecommendedMove: () -> Unit,
    onTriggerEngineAnalysis: () -> Unit,
    onStopEngineAnalysis: () -> Unit,
    onSaveGame: (String) -> Unit,
    onRequestHighThinkingCoaching: () -> Unit,
    onShowToast: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val clipboard = LocalClipboardManager.current
    var showSaveDialog by remember { mutableStateOf(false) }
    var saveTitleInput by remember { mutableStateOf("") }
    var showBestArrow by remember { mutableStateOf(true) }

    if (pendingPromotionMoves.isNotEmpty()) {
        PromotionSelectionDialog(
            color = boardState.activeColor,
            onSelectPieceType = onSelectPromotion,
            onDismiss = onCancelPromotion
        )
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            containerColor = NavySurface,
            title = { Text("Save Game to Room Database", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = saveTitleInput,
                    onValueChange = { saveTitleInput = it },
                    label = { Text("Game Title") },
                    placeholder = { Text("e.g., Tactical Study #1") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_game_title_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveGame(saveTitleInput)
                        saveTitleInput = ""
                        showSaveDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black),
                    modifier = Modifier.testTag("confirm_save_game_button")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    val topMoveUci = engineAnalysis.candidateLines.getOrNull(0)?.moveUci
    val secondMoveUci = engineAnalysis.candidateLines.getOrNull(1)?.moveUci

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(NavyBackground)
            .testTag(if (isAnalysisFocusMode) "position_analysis_screen" else "digital_board_screen"),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header Bar
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
                        modifier = Modifier.testTag("board_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                    Column {
                        Text(
                            text = if (isAnalysisFocusMode) "Chess Position Analysis" else "Digital Chessboard",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = "${boardState.activeColor.displayName} to move • Move ${boardState.fullMoveNumber} • ${boardState.status.label}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (boardState.status == GameStatus.CHECK || boardState.status == GameStatus.CHECKMATE) {
                                CrimsonError
                            } else {
                                CyanAccent
                            }
                        )
                    }
                }

                Row {
                    IconButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(boardState.toFen()))
                            onShowToast("FEN copied to clipboard")
                        },
                        modifier = Modifier.testTag("copy_fen_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy FEN",
                            tint = CyanAccent
                        )
                    }
                    IconButton(
                        onClick = { showSaveDialog = true },
                        modifier = Modifier.testTag("save_game_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.BookmarkAdd,
                            contentDescription = "Save Game",
                            tint = CyanAccent
                        )
                    }
                }
            }
        }

        // Game Status Alert Banner (Checkmate, Stalemate, Draw, Check)
        if (boardState.status != GameStatus.ONGOING) {
            item {
                val isCheckmate = boardState.status == GameStatus.CHECKMATE
                val bannerColor = when {
                    isCheckmate -> CrimsonError
                    boardState.status == GameStatus.CHECK -> AmberWarning
                    else -> ElectricBlue
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = bannerColor.copy(alpha = 0.18f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 620.dp)
                        .border(1.dp, bannerColor, RoundedCornerShape(12.dp))
                        .testTag("game_status_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isCheckmate) {
                                "${boardState.status.label} — ${boardState.activeColor.opposite().displayName} Wins!"
                            } else {
                                boardState.status.label
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        if (boardState.status.isTerminal) {
                            TextButton(onClick = onRestartGame) {
                                Text("New Game", color = CyanAccent, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Stockfish Evaluation Bar
        item {
            EvaluationBar(
                evaluationCentipawns = engineAnalysis.evaluationCentipawns,
                mateInMoves = engineAnalysis.mateInMoves,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
            )
        }

        // Interactive 8x8 Chessboard
        item {
            InteractiveChessboard(
                boardState = boardState,
                orientation = orientation,
                selectedSquare = selectedSquare,
                legalMoves = legalMoves,
                recommendedMoveUci = topMoveUci,
                secondaryMoveUci = secondMoveUci,
                boardTheme = userSettings.boardTheme,
                pieceStyle = userSettings.pieceStyle,
                showBestMoveArrow = showBestArrow,
                onSquareTapped = onSquareTapped,
                onPieceDragged = onPieceDragged,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
            )
        }

        // Board Action Controls (Undo, Restart, Flip, Arrow Toggle, Play Best Move)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 620.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onUndoMove,
                        enabled = boardState.moveHistory.isNotEmpty(),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("undo_move_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Undo")
                    }
                    OutlinedButton(
                        onClick = onRestartGame,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("restart_game_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Restart", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Restart")
                    }
                    OutlinedButton(
                        onClick = onFlipBoard,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("flip_board_button")
                    ) {
                        Icon(Icons.Default.FlipCameraAndroid, contentDescription = "Flip", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Flip")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onPlayRecommendedMove,
                        enabled = !boardState.status.isTerminal && engineAnalysis.bestMoveUci != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanAccent,
                            contentColor = Color(0xFF04131D)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("play_best_move_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play Best Move", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Play Best (${engineAnalysis.bestMoveSan ?: "..."})",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    FilterChip(
                        selected = showBestArrow,
                        onClick = { showBestArrow = !showBestArrow },
                        label = { Text("PV Arrows") },
                        modifier = Modifier.testTag("toggle_pv_arrows_chip")
                    )
                }

                if (!isAnalysisFocusMode) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CharcoalCard,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, CharcoalBorder, RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.SmartToy,
                                    contentDescription = "Opponent Mode",
                                    tint = CyanAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (playAgainstEngine) "Play vs Stockfish UCI Engine" else "2-Player / Free Study Board",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary
                                )
                            }
                            Switch(
                                checked = playAgainstEngine,
                                onCheckedChange = onTogglePlayAgainstEngine,
                                modifier = Modifier.testTag("toggle_engine_opponent_switch")
                            )
                        }
                    }
                }
            }
        }

        // Move History Strip
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 620.dp)
                    .border(1.dp, CharcoalBorder, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CharcoalCard)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "MOVE HISTORY (${boardState.moveHistory.size} PLIES)",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    if (boardState.moveHistory.isEmpty()) {
                        Text(
                            text = "No moves played yet. Tap or drag a piece on the board.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    } else {
                        val formattedHistory = remember(boardState.moveHistory) {
                            buildString {
                                boardState.moveHistory.forEach { rec ->
                                    if (rec.color == PieceColor.WHITE) {
                                        append("${rec.moveNumber}. ${rec.move.san} ")
                                    } else {
                                        append("${rec.move.san}   ")
                                    }
                                }
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = formattedHistory,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 13.sp,
                                color = CyanAccent,
                                modifier = Modifier.testTag("move_history_text")
                            )
                        }
                    }
                }
            }
        }

        // Stockfish UCI Multi-PV Candidate Lines Panel
        item {
            StockfishAnalysisPanel(
                analysis = engineAnalysis,
                onReanalyze = onTriggerEngineAnalysis,
                onStop = onStopEngineAnalysis,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 620.dp)
            )
        }

        // Chess Learning Assistant Coaching Panel (Deterministic + Optional Gemini 4 Argon High Thinking)
        item {
            CoachingExplanationCard(
                explanation = tacticalExplanation,
                aiCoachingText = aiCoachingText,
                isAiCoachingLoading = isAiCoachingLoading,
                aiCoachingError = aiCoachingError,
                onRequestHighThinkingCoaching = onRequestHighThinkingCoaching,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 620.dp)
            )
        }
    }
}

@Composable
fun StockfishAnalysisPanel(
    analysis: EngineAnalysisResult,
    onReanalyze: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .border(1.dp, CharcoalBorder, RoundedCornerShape(14.dp))
            .testTag("stockfish_analysis_panel"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = NavySurface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = analysis.engineName.uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        color = CyanAccent
                    )
                    Text(
                        text = "Depth ${analysis.currentDepth}/${analysis.targetDepth} • ${analysis.nodesSearched} nodes • ${analysis.nodesPerSecond} nps",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                }

                if (analysis.isAnalyzing) {
                    IconButton(onClick = onStop) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop Analysis", tint = CrimsonError)
                    }
                } else {
                    IconButton(onClick = onReanalyze) {
                        Icon(Icons.Default.Refresh, contentDescription = "Re-run Analysis", tint = CyanAccent)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (analysis.candidateLines.isEmpty()) {
                Text(
                    text = analysis.error ?: "No legal moves available in this terminal position.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            } else {
                analysis.candidateLines.forEach { line ->
                    CandidateLineRow(line = line)
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun CandidateLineRow(line: CandidateLine) {
    val isTop = line.multiPvIndex == 1
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isTop) DeepCyanContainer else CharcoalCard,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isTop) CyanAccent.copy(alpha = 0.6f) else CharcoalBorder,
                RoundedCornerShape(8.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (line.scoreCentipawns >= 0) CyanAccent.copy(alpha = 0.2f) else CharcoalBorder
            ) {
                Text(
                    text = line.formattedEvaluation,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isTop) CyanAccent else TextPrimary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "#${line.multiPvIndex} ${line.moveSan} (${line.moveUci})",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "PV: ${line.principalVariationSan.joinToString(" ")}",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun CoachingExplanationCard(
    explanation: TacticalExplanation,
    aiCoachingText: String?,
    isAiCoachingLoading: Boolean,
    aiCoachingError: String?,
    onRequestHighThinkingCoaching: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .border(1.dp, CharcoalBorder, RoundedCornerShape(14.dp))
            .testTag("coaching_explanation_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CharcoalCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "Chess Coach",
                        tint = EmeraldSuccess,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CHESS LEARNING ASSISTANT",
                        style = MaterialTheme.typography.labelLarge,
                        color = EmeraldSuccess
                    )
                }
                Text(
                    text = explanation.phaseLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = CyanAccent
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = explanation.headline,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = explanation.evaluationSummary,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Why Best Move Works: ${explanation.bestMoveReasoning}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Candidate Comparison: ${explanation.candidateComparison}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(10.dp))
            explanation.tacticalMotifs.forEach { motif ->
                Text(
                    text = "• $motif",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CyanAccent,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Optional High-Thinking Gemini 4 Argon Deep Reasoning
            OutlinedButton(
                onClick = onRequestHighThinkingCoaching,
                enabled = !isAiCoachingLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("high_thinking_ai_button")
            ) {
                if (isAiCoachingLoading) {
                    CircularProgressIndicator(
                        color = CyanAccent,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Thinking Deeply (Gemini 4 Argon • High Thinking)...", color = CyanAccent)
                } else {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "High Thinking Coaching",
                        tint = CyanAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Deep Tactical Breakdown (High Thinking Mode)", color = CyanAccent)
                }
            }

            AnimatedVisibility(visible = aiCoachingText != null || aiCoachingError != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = NavySurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .border(1.dp, CyanAccent.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = if (aiCoachingText != null) {
                                "DEEP TACTICAL REASONING (GEMINI 4 ARGON)"
                            } else {
                                "OFFLINE DETERMINISTIC COACH ACTIVE"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = CyanAccent
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = aiCoachingText ?: aiCoachingError.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
    }
}
