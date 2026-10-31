package com.example.chess.core

import kotlin.math.abs

object ChessRulesEngine {

    private val KNIGHT_DELTAS = listOf(
        -2 to -1, -2 to 1, -1 to -2, -1 to 2,
        1 to -2, 1 to 2, 2 to -1, 2 to 1
    )

    private val KING_DELTAS = listOf(
        -1 to -1, 0 to -1, 1 to -1,
        -1 to 0,           1 to 0,
        -1 to 1,  0 to 1,  1 to 1
    )

    private val BISHOP_DIRECTIONS = listOf(-1 to -1, 1 to -1, -1 to 1, 1 to 1)
    private val ROOK_DIRECTIONS = listOf(0 to -1, 0 to 1, -1 to 0, 1 to 0)
    private val QUEEN_DIRECTIONS = BISHOP_DIRECTIONS + ROOK_DIRECTIONS

    fun createInitialState(): ChessBoardState {
        return parseFen(ChessBoardState.INITIAL_FEN).getOrThrow()
    }

    /**
     * Validates and parses a FEN string into a ChessBoardState.
     */
    fun parseFen(fen: String): Result<ChessBoardState> {
        val trimmed = fen.trim().replace(Regex("\\s+"), " ")
        val parts = trimmed.split(" ")
        if (parts.size < 2 || parts.size > 6) {
            return Result.failure(IllegalArgumentException("Malformed FEN: Expected 4 to 6 space-separated fields, found ${parts.size}."))
        }

        val boardPlacement = parts[0]
        val activeColorToken = parts[1]
        val castlingToken = parts.getOrNull(2) ?: "-"
        val epToken = parts.getOrNull(3) ?: "-"
        val halfMoveToken = parts.getOrNull(4) ?: "0"
        val fullMoveToken = parts.getOrNull(5) ?: "1"

        val ranks = boardPlacement.split("/")
        if (ranks.size != 8) {
            return Result.failure(IllegalArgumentException("Malformed FEN board: Expected 8 ranks separated by '/', found ${ranks.size}."))
        }

        val squares = MutableList<Piece?>(64) { null }
        for ((idx, rankStr) in ranks.withIndex()) {
            val rank = 7 - idx
            var file = 0
            for (ch in rankStr) {
                if (ch in '1'..'8') {
                    file += (ch - '0')
                } else {
                    val piecePair = PieceType.fromFenChar(ch)
                        ?: return Result.failure(IllegalArgumentException("Invalid FEN piece character '$ch' on rank ${rank + 1}."))
                    if (file > 7) {
                        return Result.failure(IllegalArgumentException("Too many squares on rank ${rank + 1} in FEN."))
                    }
                    squares[rank * 8 + file] = Piece(piecePair.first, piecePair.second)
                    file++
                }
            }
            if (file != 8) {
                return Result.failure(IllegalArgumentException("Rank ${rank + 1} in FEN has $file files instead of 8."))
            }
        }

        val activeColor = when (activeColorToken) {
            "w" -> PieceColor.WHITE
            "b" -> PieceColor.BLACK
            else -> return Result.failure(IllegalArgumentException("Invalid active color '$activeColorToken' in FEN (must be 'w' or 'b')."))
        }

        val castlingRights = CastlingRights.fromFenString(castlingToken)
            ?: return Result.failure(IllegalArgumentException("Invalid castling rights '$castlingToken' in FEN."))

        val enPassantSquare = if (epToken == "-") {
            null
        } else {
            val sq = Square.fromAlgebraic(epToken)
                ?: return Result.failure(IllegalArgumentException("Invalid en passant square '$epToken' in FEN."))
            if (sq.rank != 2 && sq.rank != 5) {
                return Result.failure(IllegalArgumentException("En passant target square '$epToken' must be on rank 3 or rank 6."))
            }
            sq
        }

        val halfMove = halfMoveToken.toIntOrNull()
            ?: return Result.failure(IllegalArgumentException("Invalid halfmove clock '$halfMoveToken' in FEN."))
        val fullMove = fullMoveToken.toIntOrNull()
            ?: return Result.failure(IllegalArgumentException("Invalid fullmove number '$fullMoveToken' in FEN."))

        if (halfMove < 0 || fullMove < 1) {
            return Result.failure(IllegalArgumentException("Move counters in FEN must be non-negative (halfmove >= 0, fullmove >= 1)."))
        }

        val rawState = ChessBoardState(
            squares = squares,
            activeColor = activeColor,
            castlingRights = sanitizeCastlingRights(squares, castlingRights),
            enPassantTarget = enPassantSquare,
            halfMoveClock = halfMove,
            fullMoveNumber = fullMove
        )

        val validation = validatePosition(rawState)
        if (!validation.isValid) {
            return Result.failure(IllegalArgumentException(validation.errors.joinToString(" ")))
        }

        val key = rawState.toPositionKey()
        val withHistory = rawState.copy(positionHistoryKeys = listOf(key))
        val status = evaluateGameStatus(withHistory)
        return Result.success(withHistory.copy(status = status))
    }

    private fun sanitizeCastlingRights(squares: List<Piece?>, rights: CastlingRights): CastlingRights {
        val wKingOnE1 = squares[Square(4, 0).index] == Piece(PieceType.KING, PieceColor.WHITE)
        val wRookOnH1 = squares[Square(7, 0).index] == Piece(PieceType.ROOK, PieceColor.WHITE)
        val wRookOnA1 = squares[Square(0, 0).index] == Piece(PieceType.ROOK, PieceColor.WHITE)
        val bKingOnE8 = squares[Square(4, 7).index] == Piece(PieceType.KING, PieceColor.BLACK)
        val bRookOnH8 = squares[Square(7, 7).index] == Piece(PieceType.ROOK, PieceColor.BLACK)
        val bRookOnA8 = squares[Square(0, 7).index] == Piece(PieceType.ROOK, PieceColor.BLACK)

        return CastlingRights(
            whiteKingside = rights.whiteKingside && wKingOnE1 && wRookOnH1,
            whiteQueenside = rights.whiteQueenside && wKingOnE1 && wRookOnA1,
            blackKingside = rights.blackKingside && bKingOnE8 && bRookOnH8,
            blackQueenside = rights.blackQueenside && bKingOnE8 && bRookOnA8
        )
    }

    /**
     * Performs strict legality validation on a ChessBoardState.
     */
    fun validatePosition(state: ChessBoardState): PositionValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        val whiteKings = state.squares.count { it?.type == PieceType.KING && it.color == PieceColor.WHITE }
        val blackKings = state.squares.count { it?.type == PieceType.KING && it.color == PieceColor.BLACK }

        if (whiteKings != 1) {
            errors.add("Position must contain exactly 1 White King (found $whiteKings).")
        }
        if (blackKings != 1) {
            errors.add("Position must contain exactly 1 Black King (found $blackKings).")
        }

        // Check pawns on rank 1 or rank 8
        for (file in 0..7) {
            val rank1Piece = state.pieceAt(file, 0)
            val rank8Piece = state.pieceAt(file, 7)
            if (rank1Piece?.type == PieceType.PAWN || rank8Piece?.type == PieceType.PAWN) {
                errors.add("Pawns cannot exist on the 1st or 8th rank.")
                break
            }
        }

        val whitePawns = state.squares.count { it?.type == PieceType.PAWN && it.color == PieceColor.WHITE }
        val blackPawns = state.squares.count { it?.type == PieceType.PAWN && it.color == PieceColor.BLACK }
        if (whitePawns > 8) errors.add("White cannot have more than 8 pawns (found $whitePawns).")
        if (blackPawns > 8) errors.add("Black cannot have more than 8 pawns (found $blackPawns).")

        val whiteTotal = state.squares.count { it?.color == PieceColor.WHITE }
        val blackTotal = state.squares.count { it?.color == PieceColor.BLACK }
        if (whiteTotal > 16) errors.add("White cannot have more than 16 total pieces (found $whiteTotal).")
        if (blackTotal > 16) errors.add("Black cannot have more than 16 total pieces (found $blackTotal).")

        if (errors.isEmpty()) {
            val wKingSq = state.findKing(PieceColor.WHITE)!!
            val bKingSq = state.findKing(PieceColor.BLACK)!!
            if (abs(wKingSq.file - bKingSq.file) <= 1 && abs(wKingSq.rank - bKingSq.rank) <= 1) {
                errors.add("Illegal position: White and Black Kings cannot be on adjacent squares.")
            }

            // Non-active player's king cannot already be in check
            val inactiveColor = state.activeColor.opposite()
            if (isKingInCheck(state, inactiveColor)) {
                errors.add("Illegal position: ${inactiveColor.displayName} King is already under attack while it is ${state.activeColor.displayName}'s turn.")
            }
        }

        return PositionValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }

    /**
     * Checks if a given square is attacked by any piece of [byColor].
     */
    fun isSquareAttackedBy(state: ChessBoardState, target: Square, byColor: PieceColor): Boolean {
        // 1. Pawn attacks
        val pawnAttackRank = if (byColor == PieceColor.WHITE) target.rank - 1 else target.rank + 1
        if (pawnAttackRank in 0..7) {
            for (df in listOf(-1, 1)) {
                val f = target.file + df
                if (f in 0..7) {
                    val p = state.pieceAt(f, pawnAttackRank)
                    if (p?.color == byColor && p.type == PieceType.PAWN) return true
                }
            }
        }

        // 2. Knight attacks
        for ((df, dr) in KNIGHT_DELTAS) {
            val sq = target.offset(df, dr) ?: continue
            val p = state.pieceAt(sq)
            if (p?.color == byColor && p.type == PieceType.KNIGHT) return true
        }

        // 3. King attacks
        for ((df, dr) in KING_DELTAS) {
            val sq = target.offset(df, dr) ?: continue
            val p = state.pieceAt(sq)
            if (p?.color == byColor && p.type == PieceType.KING) return true
        }

        // 4. Bishop / Queen diagonal attacks
        for ((df, dr) in BISHOP_DIRECTIONS) {
            var cur = target.offset(df, dr)
            while (cur != null) {
                val p = state.pieceAt(cur)
                if (p != null) {
                    if (p.color == byColor && (p.type == PieceType.BISHOP || p.type == PieceType.QUEEN)) {
                        return true
                    }
                    break
                }
                cur = cur.offset(df, dr)
            }
        }

        // 5. Rook / Queen orthogonal attacks
        for ((df, dr) in ROOK_DIRECTIONS) {
            var cur = target.offset(df, dr)
            while (cur != null) {
                val p = state.pieceAt(cur)
                if (p != null) {
                    if (p.color == byColor && (p.type == PieceType.ROOK || p.type == PieceType.QUEEN)) {
                        return true
                    }
                    break
                }
                cur = cur.offset(df, dr)
            }
        }

        return false
    }

    fun isKingInCheck(state: ChessBoardState, kingColor: PieceColor): Boolean {
        val kingSq = state.findKing(kingColor) ?: return false
        return isSquareAttackedBy(state, kingSq, kingColor.opposite())
    }

    /**
     * Generates all strictly legal moves for the current active player.
     */
    fun generateLegalMoves(state: ChessBoardState): List<ChessMove> {
        val pseudoMoves = generatePseudoLegalMoves(state, state.activeColor)
        val legalMoves = mutableListOf<ChessMove>()

        for (move in pseudoMoves) {
            val nextSquares = applyMoveToSquares(state, move)
            val tempState = state.copy(squares = nextSquares)
            if (!isKingInCheck(tempState, state.activeColor)) {
                legalMoves.add(move)
            }
        }

        // Attach SAN notation to each legal move
        return legalMoves.map { move ->
            val san = computeSan(state, move, legalMoves)
            move.copy(san = san)
        }
    }

    fun generateLegalMovesFromSquare(state: ChessBoardState, from: Square): List<ChessMove> {
        return generateLegalMoves(state).filter { it.from == from }
    }

    private fun generatePseudoLegalMoves(state: ChessBoardState, color: PieceColor): List<ChessMove> {
        val moves = mutableListOf<ChessMove>()
        for (i in 0 until 64) {
            val piece = state.squares[i] ?: continue
            if (piece.color != color) continue
            val from = Square.fromIndex(i)

            when (piece.type) {
                PieceType.PAWN -> generatePawnMoves(state, from, piece, moves)
                PieceType.KNIGHT -> {
                    for ((df, dr) in KNIGHT_DELTAS) {
                        val to = from.offset(df, dr) ?: continue
                        val target = state.pieceAt(to)
                        if (target == null || target.color != color) {
                            moves.add(ChessMove(from = from, to = to, piece = piece, capturedPiece = target))
                        }
                    }
                }
                PieceType.BISHOP -> generateSlidingMoves(state, from, piece, BISHOP_DIRECTIONS, moves)
                PieceType.ROOK -> generateSlidingMoves(state, from, piece, ROOK_DIRECTIONS, moves)
                PieceType.QUEEN -> generateSlidingMoves(state, from, piece, QUEEN_DIRECTIONS, moves)
                PieceType.KING -> generateKingMoves(state, from, piece, moves)
            }
        }
        return moves
    }

    private fun generatePawnMoves(
        state: ChessBoardState,
        from: Square,
        piece: Piece,
        out: MutableList<ChessMove>
    ) {
        val color = piece.color
        val dir = if (color == PieceColor.WHITE) 1 else -1
        val startRank = if (color == PieceColor.WHITE) 1 else 6
        val promotionRank = if (color == PieceColor.WHITE) 7 else 0
        val promotionTypes = listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT)

        // 1. Single step forward
        val oneForward = from.offset(0, dir)
        if (oneForward != null && state.pieceAt(oneForward) == null) {
            if (oneForward.rank == promotionRank) {
                for (promo in promotionTypes) {
                    out.add(ChessMove(from = from, to = oneForward, piece = piece, promotion = promo))
                }
            } else {
                out.add(ChessMove(from = from, to = oneForward, piece = piece))
                // 2. Double step from starting rank
                if (from.rank == startRank) {
                    val twoForward = from.offset(0, dir * 2)
                    if (twoForward != null && state.pieceAt(twoForward) == null) {
                        out.add(ChessMove(from = from, to = twoForward, piece = piece))
                    }
                }
            }
        }

        // 3. Captures (diagonal & en passant)
        for (df in listOf(-1, 1)) {
            val diag = from.offset(df, dir) ?: continue
            val targetPiece = state.pieceAt(diag)
            if (targetPiece != null && targetPiece.color != color) {
                if (diag.rank == promotionRank) {
                    for (promo in promotionTypes) {
                        out.add(
                            ChessMove(
                                from = from,
                                to = diag,
                                piece = piece,
                                capturedPiece = targetPiece,
                                promotion = promo
                            )
                        )
                    }
                } else {
                    out.add(ChessMove(from = from, to = diag, piece = piece, capturedPiece = targetPiece))
                }
            } else if (diag == state.enPassantTarget) {
                val epCapturedSq = Square(diag.file, from.rank)
                val epPawn = state.pieceAt(epCapturedSq)
                if (epPawn != null && epPawn.color != color && epPawn.type == PieceType.PAWN) {
                    out.add(
                        ChessMove(
                            from = from,
                            to = diag,
                            piece = piece,
                            capturedPiece = epPawn,
                            isEnPassant = true
                        )
                    )
                }
            }
        }
    }

    private fun generateSlidingMoves(
        state: ChessBoardState,
        from: Square,
        piece: Piece,
        directions: List<Pair<Int, Int>>,
        out: MutableList<ChessMove>
    ) {
        for ((df, dr) in directions) {
            var cur = from.offset(df, dr)
            while (cur != null) {
                val target = state.pieceAt(cur)
                if (target == null) {
                    out.add(ChessMove(from = from, to = cur, piece = piece))
                } else {
                    if (target.color != piece.color) {
                        out.add(ChessMove(from = from, to = cur, piece = piece, capturedPiece = target))
                    }
                    break
                }
                cur = cur.offset(df, dr)
            }
        }
    }

    private fun generateKingMoves(
        state: ChessBoardState,
        from: Square,
        piece: Piece,
        out: MutableList<ChessMove>
    ) {
        val color = piece.color
        for ((df, dr) in KING_DELTAS) {
            val to = from.offset(df, dr) ?: continue
            val target = state.pieceAt(to)
            if (target == null || target.color != color) {
                out.add(ChessMove(from = from, to = to, piece = piece, capturedPiece = target))
            }
        }

        // Castling
        val opponent = color.opposite()
        val homeRank = if (color == PieceColor.WHITE) 0 else 7
        if (from == Square(4, homeRank) && !isSquareAttackedBy(state, from, opponent)) {
            val canKingside = if (color == PieceColor.WHITE) state.castlingRights.whiteKingside else state.castlingRights.blackKingside
            val canQueenside = if (color == PieceColor.WHITE) state.castlingRights.whiteQueenside else state.castlingRights.blackQueenside

            if (canKingside) {
                val fSq = Square(5, homeRank)
                val gSq = Square(6, homeRank)
                val hSq = Square(7, homeRank)
                val rook = state.pieceAt(hSq)
                if (rook == Piece(PieceType.ROOK, color) &&
                    state.pieceAt(fSq) == null &&
                    state.pieceAt(gSq) == null &&
                    !isSquareAttackedBy(state, fSq, opponent) &&
                    !isSquareAttackedBy(state, gSq, opponent)
                ) {
                    out.add(
                        ChessMove(
                            from = from,
                            to = gSq,
                            piece = piece,
                            isCastlingKingside = true
                        )
                    )
                }
            }

            if (canQueenside) {
                val dSq = Square(3, homeRank)
                val cSq = Square(2, homeRank)
                val bSq = Square(1, homeRank)
                val aSq = Square(0, homeRank)
                val rook = state.pieceAt(aSq)
                if (rook == Piece(PieceType.ROOK, color) &&
                    state.pieceAt(dSq) == null &&
                    state.pieceAt(cSq) == null &&
                    state.pieceAt(bSq) == null &&
                    !isSquareAttackedBy(state, dSq, opponent) &&
                    !isSquareAttackedBy(state, cSq, opponent)
                ) {
                    out.add(
                        ChessMove(
                            from = from,
                            to = cSq,
                            piece = piece,
                            isCastlingQueenside = true
                        )
                    )
                }
            }
        }
    }

    private fun applyMoveToSquares(state: ChessBoardState, move: ChessMove): List<Piece?> {
        val next = state.squares.toMutableList()
        next[move.from.index] = null

        if (move.isEnPassant) {
            val capturedPawnSq = Square(move.to.file, move.from.rank)
            next[capturedPawnSq.index] = null
        }

        if (move.isCastlingKingside) {
            val rank = move.from.rank
            val rook = next[Square(7, rank).index]
            next[Square(7, rank).index] = null
            next[Square(5, rank).index] = rook
        } else if (move.isCastlingQueenside) {
            val rank = move.from.rank
            val rook = next[Square(0, rank).index]
            next[Square(0, rank).index] = null
            next[Square(3, rank).index] = rook
        }

        val placedPiece = if (move.promotion != null) {
            Piece(move.promotion, move.piece.color)
        } else {
            move.piece
        }
        next[move.to.index] = placedPiece
        return next
    }

    /**
     * Applies a validated legal move to [state] and returns the updated ChessBoardState.
     */
    fun applyMove(state: ChessBoardState, move: ChessMove): ChessBoardState {
        val fenBefore = state.toFen()
        val nextSquares = applyMoveToSquares(state, move)
        val color = state.activeColor
        val nextColor = color.opposite()

        // Update castling rights
        var rights = state.castlingRights
        if (move.piece.type == PieceType.KING) {
            rights = if (color == PieceColor.WHITE) {
                rights.copy(whiteKingside = false, whiteQueenside = false)
            } else {
                rights.copy(blackKingside = false, blackQueenside = false)
            }
        }
        if (move.from == Square(0, 0) || move.to == Square(0, 0)) rights = rights.copy(whiteQueenside = false)
        if (move.from == Square(7, 0) || move.to == Square(7, 0)) rights = rights.copy(whiteKingside = false)
        if (move.from == Square(0, 7) || move.to == Square(0, 7)) rights = rights.copy(blackQueenside = false)
        if (move.from == Square(7, 7) || move.to == Square(7, 7)) rights = rights.copy(blackKingside = false)

        // En passant target
        val epTarget = if (move.piece.type == PieceType.PAWN && abs(move.to.rank - move.from.rank) == 2) {
            Square(move.from.file, (move.from.rank + move.to.rank) / 2)
        } else {
            null
        }

        // Halfmove clock
        val nextHalfMove = if (move.piece.type == PieceType.PAWN || move.capturedPiece != null) {
            0
        } else {
            state.halfMoveClock + 1
        }

        val nextFullMove = if (color == PieceColor.BLACK) state.fullMoveNumber + 1 else state.fullMoveNumber

        val provisionalState = ChessBoardState(
            squares = nextSquares,
            activeColor = nextColor,
            castlingRights = rights,
            enPassantTarget = epTarget,
            halfMoveClock = nextHalfMove,
            fullMoveNumber = nextFullMove,
            lastMove = move
        )

        val posKey = provisionalState.toPositionKey()
        val nextHistoryKeys = state.positionHistoryKeys + posKey
        val stateWithHistory = provisionalState.copy(positionHistoryKeys = nextHistoryKeys)
        val nextStatus = evaluateGameStatus(stateWithHistory)

        val record = MoveRecord(
            ply = state.moveHistory.size + 1,
            moveNumber = state.fullMoveNumber,
            color = color,
            move = move,
            fenBefore = fenBefore,
            fenAfter = stateWithHistory.toFen()
        )

        return stateWithHistory.copy(
            moveHistory = state.moveHistory + record,
            status = nextStatus
        )
    }

    fun evaluateGameStatus(state: ChessBoardState): GameStatus {
        val legalMoves = generatePseudoLegalMoves(state, state.activeColor).filter { move ->
            val nextSquares = applyMoveToSquares(state, move)
            !isKingInCheck(state.copy(squares = nextSquares), state.activeColor)
        }
        val inCheck = isKingInCheck(state, state.activeColor)

        if (legalMoves.isEmpty()) {
            return if (inCheck) GameStatus.CHECKMATE else GameStatus.STALEMATE
        }

        if (state.halfMoveClock >= 100) {
            return GameStatus.DRAW_FIFTY_MOVE
        }

        val currentKey = state.toPositionKey()
        if (state.positionHistoryKeys.count { it == currentKey } >= 3) {
            return GameStatus.DRAW_THREEFOLD_REPETITION
        }

        if (isInsufficientMaterial(state)) {
            return GameStatus.DRAW_INSUFFICIENT_MATERIAL
        }

        return if (inCheck) GameStatus.CHECK else GameStatus.ONGOING
    }

    fun isInsufficientMaterial(state: ChessBoardState): Boolean {
        val pieces = state.squares.mapIndexedNotNull { idx, p ->
            if (p != null && p.type != PieceType.KING) Pair(Square.fromIndex(idx), p) else null
        }
        if (pieces.isEmpty()) return true // K vs K
        if (pieces.size == 1) {
            val type = pieces[0].second.type
            if (type == PieceType.BISHOP || type == PieceType.KNIGHT) return true // K+B vs K or K+N vs K
        }
        if (pieces.size == 2) {
            val p1 = pieces[0]
            val p2 = pieces[1]
            if (p1.second.type == PieceType.BISHOP &&
                p2.second.type == PieceType.BISHOP &&
                p1.second.color != p2.second.color &&
                p1.first.isLightSquare == p2.first.isLightSquare
            ) {
                return true // K+B vs K+B on same color squares
            }
        }
        return false
    }

    private fun computeSan(state: ChessBoardState, move: ChessMove, allLegalMoves: List<ChessMove>): String {
        val baseSan = when {
            move.isCastlingKingside -> "O-O"
            move.isCastlingQueenside -> "O-O-O"
            move.piece.type == PieceType.PAWN -> {
                val sb = StringBuilder()
                if (move.capturedPiece != null) {
                    sb.append('a' + move.from.file)
                    sb.append('x')
                }
                sb.append(move.to.algebraic)
                if (move.promotion != null) {
                    sb.append('=').append(move.promotion.algebraicSymbol)
                }
                sb.toString()
            }
            else -> {
                val sb = StringBuilder()
                sb.append(move.piece.type.algebraicSymbol)
                // Disambiguate if multiple pieces of same type can move to same destination
                val ambiguous = allLegalMoves.filter {
                    it.from != move.from &&
                        it.to == move.to &&
                        it.piece.type == move.piece.type
                }
                if (ambiguous.isNotEmpty()) {
                    val sameFile = ambiguous.any { it.from.file == move.from.file }
                    val sameRank = ambiguous.any { it.from.rank == move.from.rank }
                    when {
                        !sameFile -> sb.append('a' + move.from.file)
                        !sameRank -> sb.append(move.from.rank + 1)
                        else -> sb.append(move.from.algebraic)
                    }
                }
                if (move.capturedPiece != null) sb.append('x')
                sb.append(move.to.algebraic)
                sb.toString()
            }
        }

        // Check / Checkmate suffix
        val nextSquares = applyMoveToSquares(state, move)
        val opponent = state.activeColor.opposite()
        val tempState = state.copy(squares = nextSquares, activeColor = opponent)
        if (isKingInCheck(tempState, opponent)) {
            val oppHasMoves = generatePseudoLegalMoves(tempState, opponent).any { oppMove ->
                val afterOpp = applyMoveToSquares(tempState, oppMove)
                !isKingInCheck(tempState.copy(squares = afterOpp), opponent)
            }
            return if (oppHasMoves) "$baseSan+" else "$baseSan#"
        }
        return baseSan
    }

    fun parseUciMove(state: ChessBoardState, uci: String): ChessMove? {
        val clean = uci.trim().lowercase()
        if (clean.length !in 4..5) return null
        val from = Square.fromAlgebraic(clean.substring(0, 2)) ?: return null
        val to = Square.fromAlgebraic(clean.substring(2, 4)) ?: return null
        val promoChar = clean.getOrNull(4)
        val promoType = when (promoChar) {
            'q' -> PieceType.QUEEN
            'r' -> PieceType.ROOK
            'b' -> PieceType.BISHOP
            'n' -> PieceType.KNIGHT
            else -> null
        }

        return generateLegalMoves(state).firstOrNull { move ->
            move.from == from && move.to == to && (move.promotion == promoType || (promoType == null && move.promotion == PieceType.QUEEN))
        }
    }

    fun parseSanMove(state: ChessBoardState, sanToken: String): ChessMove? {
        val clean = sanToken.trim()
            .replace("0-0-0", "O-O-O")
            .replace("0-0", "O-O")
            .trimEnd('+', '#', '!', '?')
        val legalMoves = generateLegalMoves(state)
        legalMoves.firstOrNull { it.san.trimEnd('+', '#') == clean || it.san == sanToken.trim() }?.let {
            return it
        }
        // Fallback match by UCI if user entered e2e4
        return parseUciMove(state, clean)
    }

    /**
     * Exports a ChessBoardState's move history to standard PGN format.
     */
    fun exportPgn(
        state: ChessBoardState,
        whitePlayer: String = "Player (White)",
        blackPlayer: String = "Chess Winner Engine",
        event: String = "Chess Winner Analysis"
    ): String {
        val resultTag = when (state.status) {
            GameStatus.CHECKMATE -> if (state.activeColor == PieceColor.WHITE) "0-1" else "1-0"
            GameStatus.STALEMATE,
            GameStatus.DRAW_FIFTY_MOVE,
            GameStatus.DRAW_THREEFOLD_REPETITION,
            GameStatus.DRAW_INSUFFICIENT_MATERIAL -> "1/2-1/2"
            else -> "*"
        }
        val sb = StringBuilder()
        sb.appendLine("[Event \"$event\"]")
        sb.appendLine("[Site \"Chess Winner Android\"]")
        sb.appendLine("[White \"$whitePlayer\"]")
        sb.appendLine("[Black \"$blackPlayer\"]")
        sb.appendLine("[Result \"$resultTag\"]")
        val initialFen = state.moveHistory.firstOrNull()?.fenBefore ?: state.toFen()
        if (initialFen != ChessBoardState.INITIAL_FEN) {
            sb.appendLine("[SetUp \"1\"]")
            sb.appendLine("[FEN \"$initialFen\"]")
        }
        sb.appendLine()

        val movesText = StringBuilder()
        for ((index, record) in state.moveHistory.withIndex()) {
            if (record.color == PieceColor.WHITE) {
                movesText.append("${record.moveNumber}. ${record.move.san} ")
            } else {
                if (index == 0) {
                    movesText.append("${record.moveNumber}... ${record.move.san} ")
                } else {
                    movesText.append("${record.move.san} ")
                }
            }
        }
        movesText.append(resultTag)
        sb.append(movesText.toString().trim())
        return sb.toString()
    }

    /**
     * Imports and validates a PGN game string, replaying all moves with strict rule checking.
     */
    fun importPgn(pgn: String): Result<ChessBoardState> {
        val trimmed = pgn.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("PGN string cannot be empty."))
        }

        // Extract optional [FEN "..."] header
        val fenRegex = Regex("""\[FEN\s+"([^"]+)"\]""")
        val customFen = fenRegex.find(trimmed)?.groupValues?.getOrNull(1)

        var currentState = if (customFen != null) {
            parseFen(customFen).getOrElse { return Result.failure(it) }
        } else {
            createInitialState()
        }

        // Strip headers [ ... ], comments { ... }, and variations ( ... )
        val body = trimmed
            .lineSequence()
            .filterNot { it.trim().startsWith("[") }
            .joinToString(" ")
            .replace(Regex("""\{[^}]*\}"""), " ")
            .replace(Regex("""\([^)]*\)"""), " ")
            .replace(Regex("""\$\d+"""), " ")

        val rawTokens = body.split(Regex("\\s+")).filter { it.isNotBlank() }
        val resultMarkers = setOf("1-0", "0-1", "1/2-1/2", "*")

        var movesApplied = 0
        for (raw in rawTokens) {
            if (raw in resultMarkers) break
            // Remove move numbers like "1." or "12..."
            val moveStr = raw.replace(Regex("""^\d+\.+"""), "").trim()
            if (moveStr.isEmpty() || moveStr in resultMarkers) continue

            val legalMove = parseSanMove(currentState, moveStr)
                ?: return Result.failure(
                    IllegalArgumentException(
                        "Invalid or illegal PGN move '$moveStr' at move ${currentState.fullMoveNumber} (${currentState.activeColor.displayName})."
                    )
                )
            currentState = applyMove(currentState, legalMove)
            movesApplied++
        }

        if (movesApplied == 0 && customFen == null) {
            return Result.failure(IllegalArgumentException("No valid chess moves found in PGN input."))
        }

        return Result.success(currentState)
    }
}
