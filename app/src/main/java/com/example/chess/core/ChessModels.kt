package com.example.chess.core

enum class PieceColor(val displayName: String, val fenToken: String) {
    WHITE("White", "w"),
    BLACK("Black", "b");

    fun opposite(): PieceColor = if (this == WHITE) BLACK else WHITE
}

enum class PieceType(
    val displayName: String,
    val algebraicSymbol: String,
    val whiteFenChar: Char,
    val blackFenChar: Char,
    val centipawnValue: Int
) {
    KING("King", "K", 'K', 'k', 20000),
    QUEEN("Queen", "Q", 'Q', 'q', 900),
    ROOK("Rook", "R", 'R', 'r', 500),
    BISHOP("Bishop", "B", 'B', 'b', 330),
    KNIGHT("Knight", "N", 'N', 'n', 320),
    PAWN("Pawn", "", 'P', 'p', 100);

    companion object {
        fun fromFenChar(c: Char): Pair<PieceType, PieceColor>? {
            return when (c) {
                'K' -> KING to PieceColor.WHITE
                'Q' -> QUEEN to PieceColor.WHITE
                'R' -> ROOK to PieceColor.WHITE
                'B' -> BISHOP to PieceColor.WHITE
                'N' -> KNIGHT to PieceColor.WHITE
                'P' -> PAWN to PieceColor.WHITE
                'k' -> KING to PieceColor.BLACK
                'q' -> QUEEN to PieceColor.BLACK
                'r' -> ROOK to PieceColor.BLACK
                'b' -> BISHOP to PieceColor.BLACK
                'n' -> KNIGHT to PieceColor.BLACK
                'p' -> PAWN to PieceColor.BLACK
                else -> null
            }
        }
    }
}

data class Piece(
    val type: PieceType,
    val color: PieceColor
) {
    val fenChar: Char
        get() = if (color == PieceColor.WHITE) type.whiteFenChar else type.blackFenChar

    val unicodeSymbol: String
        get() = when (color) {
            PieceColor.WHITE -> when (type) {
                PieceType.KING -> "♔"
                PieceType.QUEEN -> "♕"
                PieceType.ROOK -> "♖"
                PieceType.BISHOP -> "♗"
                PieceType.KNIGHT -> "♘"
                PieceType.PAWN -> "♙"
            }
            PieceColor.BLACK -> when (type) {
                PieceType.KING -> "♚"
                PieceType.QUEEN -> "♛"
                PieceType.ROOK -> "♜"
                PieceType.BISHOP -> "♝"
                PieceType.KNIGHT -> "♞"
                PieceType.PAWN -> "♟"
            }
        }
}

/**
 * Represents a square on an 8x8 chessboard.
 * file: 0 ('a') .. 7 ('h')
 * rank: 0 ('1') .. 7 ('8')
 */
data class Square(val file: Int, val rank: Int) {
    init {
        require(file in 0..7 && rank in 0..7) { "Invalid square coordinates: ($file, $rank)" }
    }

    val index: Int get() = rank * 8 + file
    val algebraic: String get() = "${('a' + file)}${rank + 1}"
    val isLightSquare: Boolean get() = (file + rank) % 2 != 0

    fun offset(dFile: Int, dRank: Int): Square? {
        val nf = file + dFile
        val nr = rank + dRank
        return if (nf in 0..7 && nr in 0..7) Square(nf, nr) else null
    }

    override fun toString(): String = algebraic

    companion object {
        fun fromIndex(index: Int): Square = Square(index % 8, index / 8)

        fun fromAlgebraic(coord: String): Square? {
            val clean = coord.trim().lowercase()
            if (clean.length != 2) return null
            val file = clean[0] - 'a'
            val rank = clean[1] - '1'
            return if (file in 0..7 && rank in 0..7) Square(file, rank) else null
        }

        val ALL: List<Square> = (0 until 64).map { fromIndex(it) }
    }
}

data class CastlingRights(
    val whiteKingside: Boolean = true,
    val whiteQueenside: Boolean = true,
    val blackKingside: Boolean = true,
    val blackQueenside: Boolean = true
) {
    fun toFenString(): String {
        val sb = StringBuilder()
        if (whiteKingside) sb.append('K')
        if (whiteQueenside) sb.append('Q')
        if (blackKingside) sb.append('k')
        if (blackQueenside) sb.append('q')
        return if (sb.isEmpty()) "-" else sb.toString()
    }

    companion object {
        val NONE = CastlingRights(false, false, false, false)
        val ALL = CastlingRights(true, true, true, true)

        fun fromFenString(str: String): CastlingRights? {
            if (str == "-") return NONE
            if (str.isEmpty() || str.length > 4) return null
            if (str.any { it !in "KQkq" }) return null
            return CastlingRights(
                whiteKingside = 'K' in str,
                whiteQueenside = 'Q' in str,
                blackKingside = 'k' in str,
                blackQueenside = 'q' in str
            )
        }
    }
}

enum class GameStatus(val label: String, val isTerminal: Boolean) {
    ONGOING("In Progress", false),
    CHECK("Check", false),
    CHECKMATE("Checkmate", true),
    STALEMATE("Stalemate — Draw", true),
    DRAW_FIFTY_MOVE("Draw — 50-Move Rule", true),
    DRAW_THREEFOLD_REPETITION("Draw — Threefold Repetition", true),
    DRAW_INSUFFICIENT_MATERIAL("Draw — Insufficient Material", true)
}

data class ChessMove(
    val from: Square,
    val to: Square,
    val piece: Piece,
    val capturedPiece: Piece? = null,
    val promotion: PieceType? = null,
    val isCastlingKingside: Boolean = false,
    val isCastlingQueenside: Boolean = false,
    val isEnPassant: Boolean = false,
    val san: String = "",
    val uci: String = "${from.algebraic}${to.algebraic}${promotion?.blackFenChar ?: ""}"
)

data class MoveRecord(
    val ply: Int,
    val moveNumber: Int,
    val color: PieceColor,
    val move: ChessMove,
    val fenBefore: String,
    val fenAfter: String,
    val evalCentipawns: Int? = null
)

data class PositionValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
)

data class ChessBoardState(
    val squares: List<Piece?>, // Size 64, indexed by Square.index (rank * 8 + file)
    val activeColor: PieceColor = PieceColor.WHITE,
    val castlingRights: CastlingRights = CastlingRights.ALL,
    val enPassantTarget: Square? = null,
    val halfMoveClock: Int = 0,
    val fullMoveNumber: Int = 1,
    val moveHistory: List<MoveRecord> = emptyList(),
    val positionHistoryKeys: List<String> = emptyList(),
    val status: GameStatus = GameStatus.ONGOING,
    val lastMove: ChessMove? = null
) {
    init {
        require(squares.size == 64) { "Board must have exactly 64 squares" }
    }

    fun pieceAt(square: Square): Piece? = squares[square.index]
    fun pieceAt(file: Int, rank: Int): Piece? =
        if (file in 0..7 && rank in 0..7) squares[rank * 8 + file] else null

    fun findKing(color: PieceColor): Square? {
        for (i in 0 until 64) {
            val p = squares[i]
            if (p != null && p.type == PieceType.KING && p.color == color) {
                return Square.fromIndex(i)
            }
        }
        return null
    }

    fun toPositionKey(): String {
        val boardPart = buildString {
            for (rank in 7 downTo 0) {
                var empty = 0
                for (file in 0..7) {
                    val p = pieceAt(file, rank)
                    if (p == null) {
                        empty++
                    } else {
                        if (empty > 0) {
                            append(empty)
                            empty = 0
                        }
                        append(p.fenChar)
                    }
                }
                if (empty > 0) append(empty)
                if (rank > 0) append('/')
            }
        }
        return "$boardPart ${activeColor.fenToken} ${castlingRights.toFenString()} ${enPassantTarget?.algebraic ?: "-"}"
    }

    fun toFen(): String {
        return "${toPositionKey()} $halfMoveClock $fullMoveNumber"
    }

    companion object {
        const val INITIAL_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    }
}
