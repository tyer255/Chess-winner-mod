package com.example.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.core.ChessBoardState
import com.example.chess.core.ChessMove
import com.example.chess.core.ChessRulesEngine
import com.example.chess.core.Piece
import com.example.chess.core.PieceColor
import com.example.chess.core.PieceType
import com.example.chess.core.Square
import com.example.ui.theme.BoardThemePalette
import com.example.ui.theme.CharcoalBorder
import com.example.ui.theme.CharcoalCard
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.NavySurface
import com.example.ui.theme.PieceVisualStyle
import com.example.ui.theme.TextPrimary
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun InteractiveChessboard(
    boardState: ChessBoardState,
    orientation: PieceColor,
    selectedSquare: Square?,
    legalMoves: List<ChessMove>,
    recommendedMoveUci: String? = null,
    secondaryMoveUci: String? = null,
    boardTheme: BoardThemePalette = BoardThemePalette.TOURNAMENT_NAVY,
    pieceStyle: PieceVisualStyle = PieceVisualStyle.STAUNTON_PRO,
    showBestMoveArrow: Boolean = true,
    onSquareTapped: (Square) -> Unit,
    onPieceDragged: (Square, Square) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var draggedFromSquare by remember { mutableStateOf<Square?>(null) }
    var dragCurrentOffset by remember { mutableStateOf(Offset.Zero) }

    val legalDestinations = remember(legalMoves) {
        legalMoves.map { it.to }.toSet()
    }
    val kingInCheckSquare = remember(boardState) {
        if (ChessRulesEngine.isKingInCheck(boardState, boardState.activeColor)) {
            boardState.findKing(boardState.activeColor)
        } else {
            null
        }
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(boardTheme.borderColor)
            .border(1.5.dp, CharcoalBorder, RoundedCornerShape(12.dp))
            .padding(6.dp)
            .testTag("interactive_chessboard")
            .semantics { contentDescription = "Interactive 8 by 8 Chessboard" }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .pointerInput(orientation, boardState) {
                    detectTapGestures { offset ->
                        val sq = offsetToSquare(offset, size.width.toFloat(), size.height.toFloat(), orientation)
                        if (sq != null) {
                            onSquareTapped(sq)
                        }
                    }
                }
                .pointerInput(orientation, boardState) {
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            val sq = offsetToSquare(
                                startOffset,
                                size.width.toFloat(),
                                size.height.toFloat(),
                                orientation
                            )
                            if (sq != null && boardState.pieceAt(sq)?.color == boardState.activeColor) {
                                draggedFromSquare = sq
                                dragCurrentOffset = startOffset
                                onSquareTapped(sq)
                            }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            dragCurrentOffset = change.position
                        },
                        onDragEnd = {
                            val from = draggedFromSquare
                            val to = offsetToSquare(
                                dragCurrentOffset,
                                size.width.toFloat(),
                                size.height.toFloat(),
                                orientation
                            )
                            draggedFromSquare = null
                            if (from != null && to != null && from != to) {
                                onPieceDragged(from, to)
                            }
                        },
                        onDragCancel = {
                            draggedFromSquare = null
                        }
                    )
                }
        ) {
            val boardWidth = size.width
            val boardHeight = size.height
            val cellW = boardWidth / 8f
            val cellH = boardHeight / 8f

            val coordPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = cellW * 0.21f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            }

            // 1. Draw 64 squares & highlights
            for (row in 0 until 8) {
                for (col in 0 until 8) {
                    val file = if (orientation == PieceColor.WHITE) col else (7 - col)
                    val rank = if (orientation == PieceColor.WHITE) (7 - row) else row
                    val square = Square(file, rank)
                    val topLeft = Offset(col * cellW, row * cellH)
                    val sqSize = Size(cellW, cellH)

                    val baseColor = if (square.isLightSquare) boardTheme.lightSquare else boardTheme.darkSquare
                    drawRect(color = baseColor, topLeft = topLeft, size = sqSize)

                    // Last move highlight
                    val lastMove = boardState.lastMove
                    if (lastMove != null && (square == lastMove.from || square == lastMove.to)) {
                        drawRect(
                            color = ElectricBlue.copy(alpha = 0.36f),
                            topLeft = topLeft,
                            size = sqSize
                        )
                    }

                    // Selected square highlight
                    if (square == selectedSquare) {
                        drawRect(
                            color = CyanAccent.copy(alpha = 0.38f),
                            topLeft = topLeft,
                            size = sqSize
                        )
                        drawRect(
                            color = CyanAccent,
                            topLeft = topLeft,
                            size = sqSize,
                            style = Stroke(width = cellW * 0.06f)
                        )
                    }

                    // King in check radial warning
                    if (square == kingInCheckSquare) {
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(CrimsonError.copy(alpha = 0.85f), CrimsonError.copy(alpha = 0.2f)),
                                center = Offset(topLeft.x + cellW / 2f, topLeft.y + cellH / 2f),
                                radius = cellW * 0.65f
                            ),
                            topLeft = topLeft,
                            size = sqSize
                        )
                    }

                    // Draw algebraic coordinates on outer edges
                    if (col == 0) {
                        coordPaint.color = (if (square.isLightSquare) boardTheme.darkSquare else boardTheme.lightSquare).toArgb()
                        drawContext.canvas.nativeCanvas.drawText(
                            "${rank + 1}",
                            topLeft.x + cellW * 0.07f,
                            topLeft.y + cellH * 0.25f,
                            coordPaint
                        )
                    }
                    if (row == 7) {
                        coordPaint.color = (if (square.isLightSquare) boardTheme.darkSquare else boardTheme.lightSquare).toArgb()
                        drawContext.canvas.nativeCanvas.drawText(
                            "${('a' + file)}",
                            topLeft.x + cellW * 0.78f,
                            topLeft.y + cellH * 0.93f,
                            coordPaint
                        )
                    }
                }
            }

            // 2. Draw engine candidate arrows before or over pieces
            if (showBestMoveArrow) {
                secondaryMoveUci?.let { uci ->
                    drawUciArrow(uci, orientation, cellW, cellH, ElectricBlue.copy(alpha = 0.55f), strokeFactor = 0.11f)
                }
                recommendedMoveUci?.let { uci ->
                    drawUciArrow(uci, orientation, cellW, cellH, CyanAccent.copy(alpha = 0.85f), strokeFactor = 0.15f)
                }
            }

            // 3. Draw pieces and legal move indicators
            for (row in 0 until 8) {
                for (col in 0 until 8) {
                    val file = if (orientation == PieceColor.WHITE) col else (7 - col)
                    val rank = if (orientation == PieceColor.WHITE) (7 - row) else row
                    val square = Square(file, rank)
                    val center = Offset((col + 0.5f) * cellW, (row + 0.5f) * cellH)

                    val piece = boardState.pieceAt(square)
                    if (piece != null && square != draggedFromSquare) {
                        drawChessPiece(
                            piece = piece,
                            center = center,
                            cellSize = cellW,
                            pieceStyle = pieceStyle
                        )
                    }

                    if (square in legalDestinations) {
                        val isCapture = piece != null ||
                            legalMoves.any { it.to == square && it.isEnPassant }
                        if (isCapture) {
                            drawCircle(
                                color = CyanAccent.copy(alpha = 0.88f),
                                radius = cellW * 0.42f,
                                center = center,
                                style = Stroke(width = cellW * 0.09f)
                            )
                        } else {
                            drawCircle(
                                color = CyanAccent.copy(alpha = 0.82f),
                                radius = cellW * 0.16f,
                                center = center
                            )
                        }
                    }
                }
            }

            // 4. Draw dragged piece under finger
            val draggingSq = draggedFromSquare
            if (draggingSq != null) {
                val piece = boardState.pieceAt(draggingSq)
                if (piece != null) {
                    drawChessPiece(
                        piece = piece,
                        center = dragCurrentOffset,
                        cellSize = cellW * 1.18f,
                        pieceStyle = pieceStyle
                    )
                }
            }
        }
    }
}

private fun offsetToSquare(
    offset: Offset,
    width: Float,
    height: Float,
    orientation: PieceColor
): Square? {
    if (width <= 0f || height <= 0f) return null
    val col = (offset.x / (width / 8f)).toInt()
    val row = (offset.y / (height / 8f)).toInt()
    if (col !in 0..7 || row !in 0..7) return null
    val file = if (orientation == PieceColor.WHITE) col else (7 - col)
    val rank = if (orientation == PieceColor.WHITE) (7 - row) else row
    return Square(file, rank)
}

fun DrawScope.drawChessPiece(
    piece: Piece,
    center: Offset,
    cellSize: Float,
    pieceStyle: PieceVisualStyle
) {
    val isWhite = piece.color == PieceColor.WHITE
    val badgeRadius = cellSize * 0.39f

    // Base shadow disc
    drawCircle(
        color = Color.Black.copy(alpha = 0.38f),
        radius = badgeRadius,
        center = Offset(center.x, center.y + cellSize * 0.04f)
    )

    // Piece medallion background for clear visual contrast on all board themes
    val fillBrush = if (isWhite) {
        Brush.radialGradient(
            colors = listOf(Color(0xFFFFFFFF), Color(0xFFE2E8F0)),
            center = Offset(center.x - badgeRadius * 0.25f, center.y - badgeRadius * 0.25f),
            radius = badgeRadius * 1.3f
        )
    } else {
        Brush.radialGradient(
            colors = listOf(Color(0xFF1E293B), Color(0xFF090D16)),
            center = Offset(center.x - badgeRadius * 0.25f, center.y - badgeRadius * 0.25f),
            radius = badgeRadius * 1.3f
        )
    }

    drawCircle(
        brush = fillBrush,
        radius = badgeRadius,
        center = center
    )

    val rimColor = if (isWhite) Color(0xFF334155) else CyanAccent.copy(alpha = 0.75f)
    drawCircle(
        color = rimColor,
        radius = badgeRadius,
        center = center,
        style = Stroke(width = cellSize * 0.045f)
    )

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        val glyphSymbol = when (pieceStyle) {
            PieceVisualStyle.GEOMETRIC_MODERN -> piece.type.algebraicSymbol.ifEmpty { "P" }
            else -> when (piece.type) {
                PieceType.KING -> "♚"
                PieceType.QUEEN -> "♛"
                PieceType.ROOK -> "♜"
                PieceType.BISHOP -> "♝"
                PieceType.KNIGHT -> "♞"
                PieceType.PAWN -> "♟"
            }
        }
        textSize = if (pieceStyle == PieceVisualStyle.GEOMETRIC_MODERN) cellSize * 0.44f else cellSize * 0.54f
        typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        color = if (isWhite) Color(0xFF0F172A).toArgb() else Color(0xFFF8FAFC).toArgb()
        val fm = fontMetrics
        val textOffset = (fm.descent + fm.ascent) / 2f
        drawContext.canvas.nativeCanvas.drawText(
            glyphSymbol,
            center.x,
            center.y - textOffset,
            this
        )
    }
}

fun DrawScope.drawUciArrow(
    uci: String,
    orientation: PieceColor,
    cellW: Float,
    cellH: Float,
    color: Color,
    strokeFactor: Float = 0.14f
) {
    if (uci.length < 4) return
    val from = Square.fromAlgebraic(uci.substring(0, 2)) ?: return
    val to = Square.fromAlgebraic(uci.substring(2, 4)) ?: return

    val fromCol = if (orientation == PieceColor.WHITE) from.file else (7 - from.file)
    val fromRow = if (orientation == PieceColor.WHITE) (7 - from.rank) else from.rank
    val toCol = if (orientation == PieceColor.WHITE) to.file else (7 - to.file)
    val toRow = if (orientation == PieceColor.WHITE) (7 - to.rank) else to.rank

    val start = Offset((fromCol + 0.5f) * cellW, (fromRow + 0.5f) * cellH)
    val end = Offset((toCol + 0.5f) * cellW, (toRow + 0.5f) * cellH)

    val angle = atan2(end.y - start.y, end.x - start.x)
    val headLen = cellW * 0.36f
    val shortenedEnd = Offset(
        end.x - cos(angle) * (headLen * 0.55f),
        end.y - sin(angle) * (headLen * 0.55f)
    )

    drawLine(
        color = color,
        start = start,
        end = shortenedEnd,
        strokeWidth = cellW * strokeFactor,
        cap = StrokeCap.Round
    )

    val path = Path().apply {
        moveTo(end.x, end.y)
        lineTo(
            end.x - headLen * cos(angle - 0.55f),
            end.y - headLen * sin(angle - 0.55f)
        )
        lineTo(
            end.x - headLen * cos(angle + 0.55f),
            end.y - headLen * sin(angle + 0.55f)
        )
        close()
    }
    drawPath(path = path, color = color)
}

@Composable
fun EvaluationBar(
    evaluationCentipawns: Int,
    mateInMoves: Int?,
    modifier: Modifier = Modifier
) {
    // Compute White fill fraction between 0.05 and 0.95
    val whiteFraction = remember(evaluationCentipawns, mateInMoves) {
        when {
            mateInMoves != null -> if (mateInMoves >= 0) 0.96f else 0.04f
            else -> {
                val clamped = evaluationCentipawns.coerceIn(-1000, 1000)
                (0.5f + (clamped / 2000f)).coerceIn(0.05f, 0.95f)
            }
        }
    }

    val scoreText = remember(evaluationCentipawns, mateInMoves) {
        if (mateInMoves != null) {
            if (mateInMoves == 0) "MATE"
            else "${if (mateInMoves > 0) "+" else "-"}M${kotlin.math.abs(mateInMoves)}"
        } else {
            val pawns = evaluationCentipawns / 100.0
            String.format("%s%.2f", if (pawns > 0) "+" else "", pawns)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(26.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0F172A))
            .border(1.dp, CharcoalBorder, RoundedCornerShape(8.dp))
            .testTag("evaluation_bar")
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(whiteFraction)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFFE2E8F0), CyanAccent)
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0B132B))
            )
        }
        Text(
            text = "STOCKFISH EVAL: $scoreText",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (whiteFraction > 0.52f) Color(0xFF080E1E) else TextPrimary,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 8.dp)
        )
    }
}

@Composable
fun PromotionSelectionDialog(
    color: PieceColor,
    onSelectPieceType: (PieceType) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(
        PieceType.QUEEN,
        PieceType.ROOK,
        PieceType.BISHOP,
        PieceType.KNIGHT
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NavySurface,
        title = {
            Text(
                text = "Promote Pawn",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
        },
        text = {
            Column {
                Text(
                    text = "Select the piece to promote your pawn to:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    options.forEach { type ->
                        val piece = Piece(type, color)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CharcoalCard,
                            modifier = Modifier
                                .size(64.dp)
                                .border(1.dp, CyanAccent.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                .clickable { onSelectPieceType(type) }
                                .testTag("promote_${type.name.lowercase()}")
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = piece.unicodeSymbol,
                                    fontSize = 26.sp,
                                    color = CyanAccent
                                )
                                Text(
                                    text = type.displayName,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontSize = 10.sp,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = CyanAccent)
            }
        }
    )
}
