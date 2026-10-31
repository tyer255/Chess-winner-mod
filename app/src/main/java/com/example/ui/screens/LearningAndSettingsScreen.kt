package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.core.ChessBoardState
import com.example.chess.core.ChessMove
import com.example.chess.core.PieceColor
import com.example.chess.core.Square
import com.example.chess.engine.EngineAnalysisResult
import com.example.chess.learning.BlunderReviewItem
import com.example.chess.learning.CheckmateLesson
import com.example.chess.learning.ChessLearningAssistant
import com.example.chess.learning.OpeningLesson
import com.example.chess.learning.TacticalExplanation
import com.example.data.LearningProgressEntity
import com.example.data.UserSettingsState
import com.example.ui.components.InteractiveChessboard
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.BoardThemePalette
import com.example.ui.theme.CharcoalBorder
import com.example.ui.theme.CharcoalCard
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DeepCyanContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NavyBackground
import com.example.ui.theme.NavySurface
import com.example.ui.theme.PieceVisualStyle
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.LearningSubTab

@Composable
fun ChessLearningScreen(
    activeTab: LearningSubTab,
    boardState: ChessBoardState,
    orientation: PieceColor,
    selectedSquare: Square?,
    legalMoves: List<ChessMove>,
    engineAnalysis: EngineAnalysisResult,
    tacticalExplanation: TacticalExplanation,
    selectedCheckmateLesson: CheckmateLesson,
    checkmateFeedback: String?,
    selectedBlunderItem: BlunderReviewItem,
    selectedOpeningLesson: OpeningLesson,
    openingStepIndex: Int,
    learningProgress: List<LearningProgressEntity>,
    userSettings: UserSettingsState,
    aiCoachingText: String?,
    isAiCoachingLoading: Boolean,
    aiCoachingError: String?,
    onSelectTab: (LearningSubTab) -> Unit,
    onSquareTapped: (Square) -> Unit,
    onPieceDragged: (Square, Square) -> Unit,
    onSelectCheckmateLesson: (CheckmateLesson) -> Unit,
    onPlayRecommendedMove: () -> Unit,
    onSelectBlunderCase: (BlunderReviewItem) -> Unit,
    onPlayBlunderMove: (BlunderReviewItem) -> Unit,
    onPlayBestAlternativeMove: (BlunderReviewItem) -> Unit,
    onSelectOpeningLesson: (OpeningLesson) -> Unit,
    onStepOpeningMove: (Int) -> Unit,
    onRequestHighThinkingCoaching: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val completedIds = remember(learningProgress) {
        learningProgress.filter { it.completed }.map { it.lessonId }.toSet()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(NavyBackground)
            .testTag("chess_learning_screen"),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
                        modifier = Modifier.testTag("learning_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                    Column {
                        Text(
                            text = "Chess Learning & Tactical Academy",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = "${completedIds.size} lessons mastered • Interactive Stockfish Drills",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CyanAccent
                        )
                    }
                }
            }
        }

        // Sub-Tab Selector: Position Analysis, Checkmate Patterns, Blunder Review, Opening Mastery
        item {
            ScrollableTabRow(
                selectedTabIndex = LearningSubTab.entries.indexOf(activeTab),
                containerColor = NavySurface,
                contentColor = CyanAccent,
                edgePadding = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 620.dp)
            ) {
                LearningSubTab.entries.forEach { tab ->
                    Tab(
                        selected = activeTab == tab,
                        onClick = { onSelectTab(tab) },
                        text = { Text(tab.title) },
                        modifier = Modifier.testTag("learning_tab_${tab.name.lowercase()}")
                    )
                }
            }
        }

        // Interactive Board shared by all Learning Modules
        item {
            InteractiveChessboard(
                boardState = boardState,
                orientation = orientation,
                selectedSquare = selectedSquare,
                legalMoves = legalMoves,
                recommendedMoveUci = engineAnalysis.bestMoveUci,
                boardTheme = userSettings.boardTheme,
                pieceStyle = userSettings.pieceStyle,
                showBestMoveArrow = activeTab != LearningSubTab.CHECKMATE_PATTERNS,
                onSquareTapped = onSquareTapped,
                onPieceDragged = onPieceDragged,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp)
            )
        }

        when (activeTab) {
            LearningSubTab.POSITION_COACH -> {
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

            LearningSubTab.CHECKMATE_PATTERNS -> {
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
                                text = "SELECT CHECKMATE PATTERN EXERCISE",
                                style = MaterialTheme.typography.labelLarge,
                                color = CyanAccent
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ChessLearningAssistant.CHECKMATE_LESSONS.forEach { lesson ->
                                    val done = lesson.id in completedIds
                                    FilterChip(
                                        selected = selectedCheckmateLesson.id == lesson.id,
                                        onClick = { onSelectCheckmateLesson(lesson) },
                                        label = {
                                            Text(if (done) "✓ ${lesson.title}" else lesson.title)
                                        },
                                        modifier = Modifier.testTag("checkmate_lesson_${lesson.id}")
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "${selectedCheckmateLesson.title} (${selectedCheckmateLesson.patternName})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = selectedCheckmateLesson.explanation,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )

                            if (checkmateFeedback != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (checkmateFeedback.startsWith("Solved")) Color(0xFF062E22) else DeepCyanContainer,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, CyanAccent, RoundedCornerShape(10.dp))
                                ) {
                                    Text(
                                        text = checkmateFeedback,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onPlayRecommendedMove,
                                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("demonstrate_checkmate_move_button")
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Demonstrate Solution (${selectedCheckmateLesson.solutionSan.first()})", fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { onSelectCheckmateLesson(selectedCheckmateLesson) }
                                ) {
                                    Text("Reset Drill")
                                }
                            }
                        }
                    }
                }
            }

            LearningSubTab.BLUNDER_REVIEW -> {
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
                                text = "TACTICAL BLUNDER vs STOCKFISH REFUTATION",
                                style = MaterialTheme.typography.labelLarge,
                                color = AmberWarning
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ChessLearningAssistant.BLUNDER_REVIEW_CASES.forEach { item ->
                                    FilterChip(
                                        selected = selectedBlunderItem.id == item.id,
                                        onClick = { onSelectBlunderCase(item) },
                                        label = { Text(item.title) },
                                        modifier = Modifier.testTag("blunder_case_${item.id}")
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedBlunderItem.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = CrimsonError.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = selectedBlunderItem.severityLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = CrimsonError,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = selectedBlunderItem.tacticalExplanation,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { onPlayBlunderMove(selectedBlunderItem) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("play_blunder_move_button")
                                ) {
                                    Text("Played: ${selectedBlunderItem.playedMoveSan}", color = CrimsonError)
                                }
                                Button(
                                    onClick = { onPlayBestAlternativeMove(selectedBlunderItem) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = EmeraldSuccess,
                                        contentColor = Color(0xFF022C22)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("play_best_alternative_button")
                                ) {
                                    Text("Best: ${selectedBlunderItem.bestMoveSan}", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            LearningSubTab.OPENING_MASTERY -> {
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
                                text = "OPENING REPERTOIRE & PRINCIPLES",
                                style = MaterialTheme.typography.labelLarge,
                                color = CyanAccent
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ChessLearningAssistant.OPENING_LESSONS.forEach { lesson ->
                                    FilterChip(
                                        selected = selectedOpeningLesson.id == lesson.id,
                                        onClick = { onSelectOpeningLesson(lesson) },
                                        label = { Text("${lesson.ecoCode} ${lesson.name}") },
                                        modifier = Modifier.testTag("opening_lesson_${lesson.id}")
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "[${selectedOpeningLesson.ecoCode}] ${selectedOpeningLesson.name}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Mainline: ${selectedOpeningLesson.movesSan.joinToString(" ")} (Step $openingStepIndex/${selectedOpeningLesson.movesSan.size})",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 12.sp,
                                color = CyanAccent
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { onStepOpeningMove(0) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Start")
                                }
                                OutlinedButton(
                                    onClick = { onStepOpeningMove(openingStepIndex - 1) },
                                    enabled = openingStepIndex > 0,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous Move")
                                }
                                Button(
                                    onClick = { onStepOpeningMove(openingStepIndex + 1) },
                                    enabled = openingStepIndex < selectedOpeningLesson.movesSan.size,
                                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("opening_next_move_button")
                                ) {
                                    Icon(Icons.Default.SkipNext, contentDescription = "Next Move")
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            selectedOpeningLesson.corePrinciples.forEach { principle ->
                                Text(
                                    text = "• $principle",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "White Plan: ${selectedOpeningLesson.strategicPlanWhite}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = EmeraldSuccess
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Black Plan: ${selectedOpeningLesson.strategicPlanBlack}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = CyanAccent
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Section 17: SETTINGS SCREEN
 * Includes Camera preferences, Default player color, Chess engine settings,
 * Analysis depth/time, Board theme, Piece appearance, Sound settings,
 * Local storage management, Privacy info, Open-source licenses (Stockfish GPLv3), and About.
 */
@Composable
fun SettingsScreen(
    settings: UserSettingsState,
    savedGamesCount: Int,
    onUpdateDefaultColor: (PieceColor) -> Unit,
    onUpdateEngineDepth: (Int) -> Unit,
    onUpdateEngineTimeMs: (Long) -> Unit,
    onUpdateMultiPv: (Int) -> Unit,
    onUpdateBoardTheme: (BoardThemePalette) -> Unit,
    onUpdatePieceStyle: (PieceVisualStyle) -> Unit,
    onUpdateSoundEnabled: (Boolean) -> Unit,
    onUpdateCameraThrottleMs: (Long) -> Unit,
    onUpdateUseBackCamera: (Boolean) -> Unit,
    onUpdateShowOverlayArrows: (Boolean) -> Unit,
    onUpdateShowOverlayCoords: (Boolean) -> Unit,
    onClearAllSavedGames: () -> Unit,
    onClearEngineHash: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    var showGplDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showClearStorageDialog by remember { mutableStateOf(false) }

    if (showGplDialog) {
        AlertDialog(
            onDismissRequest = { showGplDialog = false },
            containerColor = NavySurface,
            title = { Text("Open-Source Licenses & Stockfish GPLv3", color = TextPrimary) },
            text = {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(
                        text = "Stockfish Chess Engine — GNU General Public License v3.0 (GPLv3)\n\n" +
                            "Copyright (C) 2004-2026 The Stockfish Developers.\n" +
                            "Stockfish is free software distributed under the terms of the GNU General Public License v3.0.\n\n" +
                            "Corresponding Source Code Availability:\n" +
                            "• Official Stockfish Repository: https://github.com/official-stockfish/Stockfish\n" +
                            "• Full license text is bundled in APK assets at assets/STOCKFISH_LICENSE_GPLv3.txt\n\n" +
                            "Additional Open-Source Libraries:\n" +
                            "• Android Jetpack (Compose, CameraX, Room, DataStore) — Apache License 2.0\n" +
                            "• Kotlin Coroutines — Apache License 2.0",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showGplDialog = false }) {
                    Text("Close", color = CyanAccent)
                }
            }
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            containerColor = NavySurface,
            title = { Text("Privacy & Local-First Processing", color = TextPrimary) },
            text = {
                Text(
                    text = "Chess Winner is designed offline-first:\n\n" +
                        "1. CameraX video frames are analyzed strictly on-device in volatile memory and are never uploaded to external servers.\n" +
                        "2. All saved games, FEN positions, PGN records, and learning progress are stored locally on your device in an encrypted-capable Room SQLite database.\n" +
                        "3. Chess rules validation and Stockfish UCI engine calculations run locally on your phone's processor.\n" +
                        "4. Fair Play Notice: Camera and Second-Phone detection modes are intended exclusively for transparent study, training, and post-game analysis.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("Understood", color = CyanAccent)
                }
            }
        )
    }

    if (showClearStorageDialog) {
        AlertDialog(
            onDismissRequest = { showClearStorageDialog = false },
            containerColor = NavySurface,
            title = { Text("Clear Local Database?", color = TextPrimary) },
            text = {
                Text(
                    text = "This will delete all $savedGamesCount saved games from local Room storage. Preferences and chess engine modules will remain intact.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllSavedGames()
                        showClearStorageDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonError, contentColor = Color.White)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearStorageDialog = false }) {
                    Text("Cancel", color = CyanAccent)
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(NavyBackground)
            .testTag("settings_screen"),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 620.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("settings_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Column {
                    Text(
                        text = "Settings & System Configuration",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = "Engine UCI Parameters, Camera Vision, Board Appearance & Licenses",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }

        // 1. Default Player Color & Sound
        item {
            SettingsSectionCard(title = "PLAYER & AUDIO PREFERENCES") {
                Text("Default Player Color", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = settings.defaultPlayerColor == PieceColor.WHITE,
                        onClick = { onUpdateDefaultColor(PieceColor.WHITE) },
                        label = { Text("White Pieces") },
                        modifier = Modifier.testTag("settings_default_white")
                    )
                    FilterChip(
                        selected = settings.defaultPlayerColor == PieceColor.BLACK,
                        onClick = { onUpdateDefaultColor(PieceColor.BLACK) },
                        label = { Text("Black Pieces") },
                        modifier = Modifier.testTag("settings_default_black")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Move Sound & Tactical Audio Cues", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                    Switch(
                        checked = settings.soundEnabled,
                        onCheckedChange = onUpdateSoundEnabled,
                        modifier = Modifier.testTag("settings_sound_switch")
                    )
                }
            }
        }

        // 2. Stockfish UCI Chess Engine Settings
        item {
            SettingsSectionCard(title = "STOCKFISH UCI ENGINE & ANALYSIS DEPTH") {
                Text(
                    text = "Search Depth: ${settings.engineDepth} plies",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary
                )
                Slider(
                    value = settings.engineDepth.toFloat(),
                    onValueChange = { onUpdateEngineDepth(it.toInt()) },
                    valueRange = 2f..8f,
                    steps = 5
                )

                Text(
                    text = "Max Analysis Time per Move: ${settings.engineTimeMs} ms",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary
                )
                Slider(
                    value = settings.engineTimeMs.toFloat(),
                    onValueChange = { onUpdateEngineTimeMs(it.toLong()) },
                    valueRange = 500f..5000f,
                    steps = 8
                )

                Text(
                    text = "Candidate Lines (MultiPV): ${settings.multiPvLines}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 2, 3, 4).forEach { count ->
                        FilterChip(
                            selected = settings.multiPvLines == count,
                            onClick = { onUpdateMultiPv(count) },
                            label = { Text("$count Lines") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                OutlinedButton(
                    onClick = onClearEngineHash,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Clear Engine Transposition Hash Table")
                }
            }
        }

        // 3. Board Theme & Piece Appearance
        item {
            SettingsSectionCard(title = "BOARD THEME & PIECE APPEARANCE") {
                Text("Chessboard Color Theme", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BoardThemePalette.entries.forEach { theme ->
                        FilterChip(
                            selected = settings.boardTheme == theme,
                            onClick = { onUpdateBoardTheme(theme) },
                            label = { Text(theme.displayName) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("Piece Set Style", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PieceVisualStyle.entries.forEach { style ->
                        FilterChip(
                            selected = settings.pieceStyle == style,
                            onClick = { onUpdatePieceStyle(style) },
                            label = { Text(style.displayName) }
                        )
                    }
                }
            }
        }

        // 4. Camera & Computer Vision Preferences
        item {
            SettingsSectionCard(title = "CAMERA & BOARD OVERLAY PREFERENCES") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Use Rear Camera by Default", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                    Switch(
                        checked = settings.useBackCameraByDefault,
                        onCheckedChange = onUpdateUseBackCamera
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Show Recommended Move Arrows on Overlay", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                    Switch(
                        checked = settings.showOverlayArrows,
                        onCheckedChange = onUpdateShowOverlayArrows
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Show Algebraic Coordinates on Overlay", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                    Switch(
                        checked = settings.showOverlayCoordinates,
                        onCheckedChange = onUpdateShowOverlayCoords
                    )
                }

                Text(
                    text = "Frame Analysis Interval: ${settings.cameraFrameThrottleMs} ms",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Slider(
                    value = settings.cameraFrameThrottleMs.toFloat(),
                    onValueChange = { onUpdateCameraThrottleMs(it.toLong()) },
                    valueRange = 150f..800f
                )
            }
        }

        // 5. Local Storage, Privacy, Open-Source Licenses & About
        item {
            SettingsSectionCard(title = "STORAGE, PRIVACY & OPEN-SOURCE LICENSES") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Local Room Database", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                        Text("$savedGamesCount saved games stored on device", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                    OutlinedButton(
                        onClick = { showClearStorageDialog = true },
                        modifier = Modifier.testTag("settings_clear_storage_button")
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, tint = CrimsonError)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear", color = CrimsonError)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showGplDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("open_source_licenses_button")
                    ) {
                        Icon(Icons.Default.Gavel, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("GPLv3 & Licenses")
                    }

                    OutlinedButton(
                        onClick = { showPrivacyDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("privacy_info_button")
                    ) {
                        Icon(Icons.Default.PrivacyTip, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Privacy Info")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = NavySurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = CyanAccent)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "About CHESS WINNER v1.0",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Offline-first Android Chess Analysis, CameraX Computer Vision, Stockfish 16.1 UCI Engine, and Tactical Learning Platform.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 620.dp)
            .border(1.dp, CharcoalBorder, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CharcoalCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = CyanAccent
            )
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}
