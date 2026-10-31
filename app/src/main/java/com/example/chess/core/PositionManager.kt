package com.example.chess.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Unified Chess Position Manager used by:
 * 1. Physical Chessboard Camera Mode
 * 2. Second Mobile Phone Camera Mode
 * 3. Same-Device Digital Chessboard Mode
 *
 * Guarantees that only validated chess positions are accepted and analyzed.
 */
class PositionManager {

    private val _boardState = MutableStateFlow(ChessRulesEngine.createInitialState())
    val boardState: StateFlow<ChessBoardState> = _boardState.asStateFlow()

    private val _selectedSquare = MutableStateFlow<Square?>(null)
    val selectedSquare: StateFlow<Square?> = _selectedSquare.asStateFlow()

    private val _legalMovesForSelected = MutableStateFlow<List<ChessMove>>(emptyList())
    val legalMovesForSelected: StateFlow<List<ChessMove>> = _legalMovesForSelected.asStateFlow()

    private val _pendingPromotionMoves = MutableStateFlow<List<ChessMove>>(emptyList())
    val pendingPromotionMoves: StateFlow<List<ChessMove>> = _pendingPromotionMoves.asStateFlow()

    private val _boardOrientation = MutableStateFlow(PieceColor.WHITE)
    val boardOrientation: StateFlow<PieceColor> = _boardOrientation.asStateFlow()

    private val stateStack = mutableListOf<ChessBoardState>()

    fun selectSquare(square: Square): ChessMove? {
        val current = _boardState.value
        val currentlySelected = _selectedSquare.value

        // If tapping a valid destination for the already-selected piece, execute or prompt promotion
        if (currentlySelected != null) {
            val matchingMoves = _legalMovesForSelected.value.filter { it.to == square }
            if (matchingMoves.isNotEmpty()) {
                if (matchingMoves.size > 1 && matchingMoves.any { it.promotion != null }) {
                    _pendingPromotionMoves.value = matchingMoves
                    return null
                }
                val chosenMove = matchingMoves.first()
                makeMove(chosenMove)
                return chosenMove
            }
        }

        // Otherwise select own piece on that square
        val piece = current.pieceAt(square)
        if (piece != null && piece.color == current.activeColor && !current.status.isTerminal) {
            _selectedSquare.value = square
            _legalMovesForSelected.value = ChessRulesEngine.generateLegalMovesFromSquare(current, square)
        } else {
            clearSelection()
        }
        return null
    }

    fun tryMoveBetweenSquares(from: Square, to: Square): ChessMove? {
        val current = _boardState.value
        if (current.status.isTerminal) return null
        val matchingMoves = ChessRulesEngine.generateLegalMovesFromSquare(current, from)
            .filter { it.to == to }

        if (matchingMoves.isEmpty()) return null
        if (matchingMoves.size > 1 && matchingMoves.any { it.promotion != null }) {
            _selectedSquare.value = from
            _legalMovesForSelected.value = matchingMoves
            _pendingPromotionMoves.value = matchingMoves
            return null
        }

        val move = matchingMoves.first()
        makeMove(move)
        return move
    }

    fun completePromotion(pieceType: PieceType): ChessMove? {
        val move = _pendingPromotionMoves.value.firstOrNull { it.promotion == pieceType }
        _pendingPromotionMoves.value = emptyList()
        if (move != null) {
            makeMove(move)
        }
        return move
    }

    fun cancelPromotion() {
        _pendingPromotionMoves.value = emptyList()
    }

    fun makeMove(move: ChessMove): Boolean {
        val current = _boardState.value
        val legal = ChessRulesEngine.generateLegalMoves(current)
        val verifiedMove = legal.firstOrNull {
            it.from == move.from && it.to == move.to && it.promotion == move.promotion
        } ?: return false

        stateStack.add(current)
        _boardState.value = ChessRulesEngine.applyMove(current, verifiedMove)
        clearSelection()
        return true
    }

    fun undoLastMove(): Boolean {
        if (stateStack.isEmpty()) return false
        val previous = stateStack.removeAt(stateStack.lastIndex)
        _boardState.value = previous
        clearSelection()
        return true
    }

    fun restartGame(orientation: PieceColor = _boardOrientation.value) {
        stateStack.clear()
        _boardState.value = ChessRulesEngine.createInitialState()
        _boardOrientation.value = orientation
        clearSelection()
    }

    fun flipOrientation() {
        _boardOrientation.value = _boardOrientation.value.opposite()
    }

    fun setOrientation(color: PieceColor) {
        _boardOrientation.value = color
    }

    /**
     * Validates and loads a FEN position into the central PositionManager.
     * Never accepts an unvalidated or illegal chess position.
     */
    fun loadValidatedFen(fen: String): Result<ChessBoardState> {
        val parsed = ChessRulesEngine.parseFen(fen)
        parsed.onSuccess { validState ->
            stateStack.clear()
            _boardState.value = validState
            clearSelection()
        }
        return parsed
    }

    /**
     * Validates and loads a PGN game into the central PositionManager.
     */
    fun loadValidatedPgn(pgn: String): Result<ChessBoardState> {
        val parsed = ChessRulesEngine.importPgn(pgn)
        parsed.onSuccess { validState ->
            stateStack.clear()
            _boardState.value = validState
            clearSelection()
        }
        return parsed
    }

    /**
     * Validates and loads a board state from Computer Vision or the Manual Board Editor.
     */
    fun loadCustomState(state: ChessBoardState): PositionValidationResult {
        val validation = ChessRulesEngine.validatePosition(state)
        if (validation.isValid) {
            stateStack.clear()
            val status = ChessRulesEngine.evaluateGameStatus(state)
            _boardState.value = state.copy(
                status = status,
                positionHistoryKeys = listOf(state.toPositionKey())
            )
            clearSelection()
        }
        return validation
    }

    fun clearSelection() {
        _selectedSquare.value = null
        _legalMovesForSelected.value = emptyList()
        _pendingPromotionMoves.value = emptyList()
    }
}
