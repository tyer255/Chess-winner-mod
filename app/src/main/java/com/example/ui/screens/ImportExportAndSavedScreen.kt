package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.ImageSearch
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.core.CastlingRights
import com.example.chess.core.ChessBoardState
import com.example.chess.core.ChessRulesEngine
import com.example.chess.core.Piece
import com.example.chess.core.PieceColor
import com.example.chess.core.PositionValidationResult
import com.example.chess.core.Square
import com.example.data.SavedGameEntity
import com.example.ui.components.InteractiveChessboard
import com.example.ui.theme.CharcoalBorder
import com.example.ui.theme.CharcoalCard
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NavyBackground
import com.example.ui.theme.NavySurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ImportExportScreen(
    currentBoardState: ChessBoardState,
    editorSquares: List<Piece?>,
    editorActiveColor: PieceColor,
    editorSelectedBrush: Piece?,
    onImportFen: (String) -> Result<ChessBoardState>,
    onImportPgn: (String) -> Result<ChessBoardState>,
    onImportScreenshotBitmap: (android.graphics.Bitmap) -> Unit,
    onSelectEditorBrush: (Piece?) -> Unit,
    onTapEditorSquare: (Square) -> Unit,
    onSetEditorActiveColor: (PieceColor) -> Unit,
    onClearEditorBoard: () -> Unit,
    onResetEditorBoard: () -> Unit,
    onValidateEditorBoard: () -> PositionValidationResult,
    onApplyEditorBoardAndAnalyze: () -> PositionValidationResult,
    onShowToast: (String) -> Unit,
    onOpenAnalysis: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var fenInputText by remember { mutableStateOf(currentBoardState.toFen()) }
    var pgnInputText by remember { mutableStateOf(ChessRulesEngine.exportPgn(currentBoardState)) }
    var validationErrorText by remember { mutableStateOf<String?>(null) }
    var validationSuccessText by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bmp = BitmapFactory.decodeStream(stream)
                    if (bmp != null) {
                        onImportScreenshotBitmap(bmp)
                        selectedTabIndex = 2 // Jump to Manual Board Editor to verify imported screenshot
                    } else {
                        validationErrorText = "Could not decode selected screenshot image."
                    }
                }
            }.onFailure {
                validationErrorText = "Failed to read image: ${it.localizedMessage}"
            }
        }
    }

    val editorValidation = remember(editorSquares, editorActiveColor) {
        onValidateEditorBoard()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(NavyBackground)
            .testTag("import_export_screen"),
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
                    modifier = Modifier.testTag("import_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Column {
                    Text(
                        text = "Import, Export & Board Editor",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = "FEN, PGN, Android Photo Picker Screenshot & Custom Position Setup",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }

        item {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = NavySurface,
                contentColor = CyanAccent,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 620.dp)
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = {
                        selectedTabIndex = 0
                        validationErrorText = null
                    },
                    text = { Text("FEN / PGN") },
                    modifier = Modifier.testTag("tab_fen_pgn")
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = {
                        selectedTabIndex = 1
                        validationErrorText = null
                    },
                    text = { Text("Screenshot") },
                    modifier = Modifier.testTag("tab_screenshot")
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = {
                        selectedTabIndex = 2
                        validationErrorText = null
                    },
                    text = { Text("Board Editor") },
                    modifier = Modifier.testTag("tab_board_editor")
                )
            }
        }

        if (validationErrorText != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF2D0A12),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 620.dp)
                        .border(1.dp, CrimsonError, RoundedCornerShape(12.dp))
                        .testTag("import_validation_error_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = CrimsonError)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = validationErrorText!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                    }
                }
            }
        }

        if (validationSuccessText != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF062E22),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 620.dp)
                        .border(1.dp, EmeraldSuccess, RoundedCornerShape(12.dp))
                        .testTag("import_validation_success_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldSuccess)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = validationSuccessText!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                        }
                        TextButton(onClick = onOpenAnalysis) {
                            Text("Analyze", color = CyanAccent, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        when (selectedTabIndex) {
            0 -> {
                // FEN Import & Export Card
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "FORSYTH-EDWARDS NOTATION (FEN)",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = CyanAccent
                                )
                                TextButton(
                                    onClick = {
                                        val exported = currentBoardState.toFen()
                                        fenInputText = exported
                                        clipboard.setText(AnnotatedString(exported))
                                        onShowToast("Exported FEN copied to clipboard")
                                    },
                                    modifier = Modifier.testTag("export_fen_button")
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Export & Copy FEN", color = CyanAccent)
                                }
                            }

                            OutlinedTextField(
                                value = fenInputText,
                                onValueChange = {
                                    fenInputText = it
                                    validationErrorText = null
                                    validationSuccessText = null
                                },
                                label = { Text("FEN Position String") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("fen_input_field")
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val res = onImportFen(fenInputText)
                                        res.onSuccess {
                                            validationErrorText = null
                                            validationSuccessText = "Valid FEN imported: ${it.activeColor.displayName} to move."
                                        }.onFailure { err ->
                                            validationSuccessText = null
                                            validationErrorText = err.localizedMessage ?: "Invalid FEN string."
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CyanAccent,
                                        contentColor = Color.Black
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("validate_import_fen_button")
                                ) {
                                    Text("Validate & Import FEN", fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        fenInputText = "r1bqk2r/pppp1ppp/2n2n2/2b1p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4"
                                    },
                                    modifier = Modifier.testTag("sample_italian_fen_button")
                                ) {
                                    Text("Sample FEN")
                                }
                            }
                        }
                    }
                }

                // PGN Import & Export Card
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "PORTABLE GAME NOTATION (PGN)",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = CyanAccent
                                )
                                TextButton(
                                    onClick = {
                                        val exported = ChessRulesEngine.exportPgn(currentBoardState)
                                        pgnInputText = exported
                                        clipboard.setText(AnnotatedString(exported))
                                        onShowToast("Exported PGN copied to clipboard")
                                    },
                                    modifier = Modifier.testTag("export_pgn_button")
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Export & Copy PGN", color = CyanAccent)
                                }
                            }

                            OutlinedTextField(
                                value = pgnInputText,
                                onValueChange = {
                                    pgnInputText = it
                                    validationErrorText = null
                                    validationSuccessText = null
                                },
                                label = { Text("PGN Game Record") },
                                minLines = 4,
                                maxLines = 8,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("pgn_input_field")
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val res = onImportPgn(pgnInputText)
                                        res.onSuccess {
                                            validationErrorText = null
                                            validationSuccessText = "Valid PGN imported (${it.moveHistory.size} plies verified)."
                                        }.onFailure { err ->
                                            validationSuccessText = null
                                            validationErrorText = err.localizedMessage ?: "Invalid PGN record."
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CyanAccent,
                                        contentColor = Color.Black
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("validate_import_pgn_button")
                                ) {
                                    Text("Validate & Import PGN", fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        pgnInputText = "1. e4 e5 2. Nf3 Nc6 3. Bb5 a6 4. Ba4 Nf6 5. O-O Be7"
                                    },
                                    modifier = Modifier.testTag("sample_pgn_button")
                                ) {
                                    Text("Sample PGN")
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                // Screenshot Import Card (Android Photo Picker)
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 620.dp)
                            .border(1.dp, CharcoalBorder, RoundedCornerShape(14.dp)),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = CharcoalCard)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.ImageSearch,
                                contentDescription = "Screenshot Import",
                                tint = CyanAccent,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Import Chessboard Screenshot",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "For third-party chess apps on the same phone, select a voluntary screenshot using the zero-permission Android Photo Picker. The computer-vision pipeline extracts the 8×8 grid and opens the Manual Board Editor for instant verification.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyanAccent,
                                    contentColor = Color.Black
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("pick_screenshot_button")
                            ) {
                                Icon(Icons.Default.FileOpen, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Select Chess Screenshot from Gallery", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            2 -> {
                // Manual Board Editor Tab
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 620.dp)
                            .border(1.dp, CharcoalBorder, RoundedCornerShape(14.dp)),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = NavySurface)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "MANUAL CHESSBOARD POSITION EDITOR",
                                style = MaterialTheme.typography.labelLarge,
                                color = CyanAccent
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            PieceBrushSelectorRow(
                                selectedBrush = editorSelectedBrush,
                                onSelectBrush = onSelectEditorBrush
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            val previewState = remember(editorSquares, editorActiveColor) {
                                ChessBoardState(
                                    squares = editorSquares,
                                    activeColor = editorActiveColor,
                                    castlingRights = CastlingRights.ALL
                                )
                            }

                            InteractiveChessboard(
                                boardState = previewState,
                                orientation = PieceColor.WHITE,
                                selectedSquare = null,
                                legalMoves = emptyList(),
                                showBestMoveArrow = false,
                                onSquareTapped = onTapEditorSquare,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = 420.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Side to Move:", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                FilterChip(
                                    selected = editorActiveColor == PieceColor.WHITE,
                                    onClick = { onSetEditorActiveColor(PieceColor.WHITE) },
                                    label = { Text("White") }
                                )
                                FilterChip(
                                    selected = editorActiveColor == PieceColor.BLACK,
                                    onClick = { onSetEditorActiveColor(PieceColor.BLACK) },
                                    label = { Text("Black") }
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onResetEditorBoard,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("editor_reset_start_button")
                                ) {
                                    Text("Start Position")
                                }
                                OutlinedButton(
                                    onClick = onClearEditorBoard,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("editor_clear_board_button")
                                ) {
                                    Text("Bare Kings")
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            if (!editorValidation.isValid) {
                                Text(
                                    text = "Position Invalid: ${editorValidation.errors.firstOrNull()}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = CrimsonError
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            }

                            Button(
                                onClick = {
                                    val res = onApplyEditorBoardAndAnalyze()
                                    if (!res.isValid) {
                                        validationErrorText = res.errors.joinToString(" ")
                                    }
                                },
                                enabled = editorValidation.isValid,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyanAccent,
                                    contentColor = Color.Black
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("apply_editor_board_button")
                            ) {
                                Icon(Icons.Default.Analytics, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Validate & Analyze Edited Position", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Section 13E: SAVED GAMES SCREEN (Local Room Database)
 */
@Composable
fun SavedGamesScreen(
    savedGames: List<SavedGameEntity>,
    onOpenGame: (SavedGameEntity) -> Unit,
    onDeleteGame: (Long) -> Unit,
    onSaveCurrentBoard: (String) -> Unit,
    onShowToast: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val clipboard = LocalClipboardManager.current
    var gamePendingDelete by remember { mutableStateOf<SavedGameEntity?>(null) }

    if (gamePendingDelete != null) {
        val target = gamePendingDelete!!
        AlertDialog(
            onDismissRequest = { gamePendingDelete = null },
            containerColor = NavySurface,
            title = { Text("Delete Saved Game?", color = TextPrimary) },
            text = {
                Text(
                    "Are you sure you want to permanently delete '${target.title}' from local Room storage?",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteGame(target.id)
                        gamePendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonError, contentColor = Color.White),
                    modifier = Modifier.testTag("confirm_delete_saved_game_button")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { gamePendingDelete = null }) {
                    Text("Cancel", color = CyanAccent)
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(NavyBackground)
            .testTag("saved_games_screen"),
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
                        modifier = Modifier.testTag("saved_games_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                    Column {
                        Text(
                            text = "Saved Games (Room Database)",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = "${savedGames.size} games stored locally offline",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }

                Button(
                    onClick = { onSaveCurrentBoard("Saved Session #${savedGames.size + 1}") },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black),
                    modifier = Modifier.testTag("quick_save_current_board_button")
                ) {
                    Text("Save Current", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (savedGames.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 620.dp)
                        .border(1.dp, CharcoalBorder, RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CharcoalCard)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No Saved Games Yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Save any game or analyzed position to store its FEN and PGN locally in the Room database.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        } else {
            items(savedGames, key = { it.id }) { game ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 620.dp)
                        .border(1.dp, CharcoalBorder, RoundedCornerShape(14.dp))
                        .testTag("saved_game_card_${game.id}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CharcoalCard)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = game.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${game.whitePlayer} vs ${game.blackPlayer} • ${game.moveCount} moves • ${game.resultText}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = CyanAccent
                                )
                            }
                            IconButton(
                                onClick = { gamePendingDelete = game },
                                modifier = Modifier.testTag("delete_saved_game_${game.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Saved Game",
                                    tint = CrimsonError
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "FEN: ${game.finalFen}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = game.evaluationSummary,
                            style = MaterialTheme.typography.labelMedium,
                            color = EmeraldSuccess
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onOpenGame(game) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyanAccent,
                                    contentColor = Color.Black
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("open_saved_game_${game.id}")
                            ) {
                                Text("Open & Analyze", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(game.pgnText))
                                    onShowToast("PGN copied to clipboard")
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Copy PGN")
                            }
                        }
                    }
                }
            }
        }
    }
}
