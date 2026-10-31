package com.example.chess.learning

import android.util.Log
import com.example.chess.core.ChessBoardState
import com.example.chess.core.ChessMove
import com.example.chess.core.ChessRulesEngine
import com.example.chess.core.GameStatus
import com.example.chess.core.PieceColor
import com.example.chess.core.PieceType
import com.example.chess.core.Square
import com.example.chess.engine.CandidateLine
import com.example.chess.engine.EngineAnalysisResult
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.ThinkingLevel
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.thinkingConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlin.math.abs

data class TacticalExplanation(
    val headline: String,
    val evaluationSummary: String,
    val bestMoveReasoning: String,
    val candidateComparison: String,
    val tacticalMotifs: List<String>,
    val strategicPrinciples: List<String>,
    val phaseLabel: String
)

data class CheckmateLesson(
    val id: String,
    val title: String,
    val patternName: String,
    val mateInMoves: Int,
    val fen: String,
    val solutionSan: List<String>,
    val solutionUci: List<String>,
    val explanation: String,
    val tacticalHint: String
)

data class BlunderReviewItem(
    val id: String,
    val title: String,
    val fenBeforeMove: String,
    val playedMoveUci: String,
    val playedMoveSan: String,
    val bestMoveUci: String,
    val bestMoveSan: String,
    val evalBeforeCp: Int,
    val evalAfterPlayedCp: Int,
    val severityLabel: String, // "Blunder (??)", "Mistake (?)", "Inaccuracy (?!)"
    val tacticalExplanation: String
)

data class OpeningLesson(
    val id: String,
    val ecoCode: String,
    val name: String,
    val movesSan: List<String>,
    val resultingFen: String,
    val corePrinciples: List<String>,
    val strategicPlanWhite: String,
    val strategicPlanBlack: String
)

object ChessLearningAssistant {

    private const val TAG = "ChessLearningAssistant"

    // Optional High-Thinking Gemini 4 Argon Model for Deep Grandmaster Commentary
    // Never replaces legal chess rules or Stockfish engine calculations.
    private val deepThinkingModel by lazy {
        val config = generationConfig {
            thinkingConfig = thinkingConfig {
                thinkingLevel = ThinkingLevel.HIGH
            }
        }
        Firebase.ai.generativeModel(
            modelName = "gemini-4-argon",
            generationConfig = config
        )
    }

    /**
     * Generates a complete deterministic educational explanation from the validated
     * board state and real Stockfish UCI analysis output. Works 100% offline.
     */
    fun explainPositionDeterministically(
        state: ChessBoardState,
        analysis: EngineAnalysisResult
    ): TacticalExplanation {
        val phase = detectGamePhase(state)
        val evalText = explainEvaluationScore(state, analysis)
        val topLine = analysis.candidateLines.firstOrNull()
        val bestMove = topLine?.let { ChessRulesEngine.parseUciMove(state, it.moveUci) }

        val bestMoveReasoning = if (topLine != null && bestMove != null) {
            explainMoveStrategicPurpose(state, bestMove, topLine)
        } else if (state.status == GameStatus.CHECKMATE) {
            val winner = state.activeColor.opposite().displayName
            "Checkmate on the board. $winner has delivered a decisive attack with no legal escape squares for the ${state.activeColor.displayName} King."
        } else if (state.status.isTerminal) {
            "The game has ended in ${state.status.label}. Neither side has legal winning continuation."
        } else {
            "Run Stockfish analysis to compute candidate lines and principal variations."
        }

        val candidateComparison = compareCandidateVariations(state, analysis.candidateLines)
        val motifs = detectTacticalMotifs(state, bestMove, analysis)
        val principles = derivePhasePrinciples(state, phase)

        val headline = when {
            analysis.mateInMoves != null && analysis.mateInMoves != 0 -> {
                val side = if (analysis.mateInMoves > 0) "White" else "Black"
                "Forced Checkmate Detected: $side Mates in ${abs(analysis.mateInMoves)}"
            }
            topLine != null -> {
                "Recommended Best Move: ${topLine.moveSan} (${topLine.formattedEvaluation})"
            }
            else -> "${phase}: ${state.activeColor.displayName} to Move"
        }

        return TacticalExplanation(
            headline = headline,
            evaluationSummary = evalText,
            bestMoveReasoning = bestMoveReasoning,
            candidateComparison = candidateComparison,
            tacticalMotifs = motifs,
            strategicPrinciples = principles,
            phaseLabel = phase
        )
    }

    fun explainEvaluationScore(state: ChessBoardState, analysis: EngineAnalysisResult): String {
        val mate = analysis.mateInMoves
        if (mate != null) {
            if (mate == 0) {
                return "Checkmate has been reached on the board."
            }
            val attacker = if (mate > 0) "White" else "Black"
            return "Stockfish calculates a forced checkmate in ${abs(mate)} move(s) for $attacker (${if (mate > 0) "+M" else "-M"}${abs(mate)}). Even against optimal defense, the opposing King cannot escape."
        }

        val cp = analysis.evaluationCentipawns
        val pawns = cp / 100.0
        val absPawns = abs(pawns)
        val leader = if (cp >= 0) "White" else "Black"
        val sign = if (cp > 0) "+" else ""
        val scoreStr = String.format("%s%.2f", sign, pawns)

        return when {
            absPawns < 0.30 -> "Evaluation $scoreStr: The position is dynamically balanced. Both sides have equal chances; focus on piece coordination and king safety."
            absPawns < 0.85 -> "Evaluation $scoreStr: $leader holds a slight positional edge (equivalent to ${String.format("%.1f", absPawns)} pawns) through superior square control or development."
            absPawns < 1.85 -> "Evaluation $scoreStr: $leader has a clear strategic advantage (over a pawn's worth of value). Press the initiative and restrict counterplay."
            absPawns < 4.00 -> "Evaluation $scoreStr: $leader has a winning material or tactical advantage. Simplify cleanly while avoiding stalemate traps."
            else -> "Evaluation $scoreStr: $leader has a decisive, overwhelming advantage."
        }
    }

    private fun explainMoveStrategicPurpose(
        state: ChessBoardState,
        move: ChessMove,
        line: CandidateLine
    ): String {
        val reasons = mutableListOf<String>()
        val activeName = state.activeColor.displayName

        if (move.san.endsWith("#")) {
            reasons.add("${move.san} delivers immediate checkmate!")
        } else if (line.mateInMoves != null) {
            reasons.add("${move.san} initiates a forced checkmate sequence in ${abs(line.mateInMoves)} moves (${line.principalVariationSan.joinToString(" → ")}).")
        }

        if (move.isCastlingKingside || move.isCastlingQueenside) {
            val side = if (move.isCastlingKingside) "kingside" else "queenside"
            reasons.add("Castles $side (${move.san}), tucking the $activeName King into safety and activating the Rook toward the central files.")
        }

        if (move.promotion != null) {
            reasons.add("Promotes the passed pawn on ${move.to.algebraic} into a ${move.promotion.displayName}, dramatically increasing material firepower.")
        }

        if (move.capturedPiece != null) {
            val gain = move.capturedPiece.type.centipawnValue - move.piece.type.centipawnValue
            if (gain > 0) {
                reasons.add("Wins high-value material by capturing the ${move.capturedPiece.color.displayName} ${move.capturedPiece.type.displayName} on ${move.to.algebraic} with a ${move.piece.type.displayName}.")
            } else {
                reasons.add("Captures the ${move.capturedPiece.type.displayName} on ${move.to.algebraic}, removing an active defender and opening key lines.")
            }
        }

        if (move.san.endsWith("+")) {
            reasons.add("Delivers a forcing check (`${move.san}`), seizing the initiative and compelling the opponent King to respond immediately.")
        }

        val centerSquares = setOf(Square(3, 3), Square(4, 3), Square(3, 4), Square(4, 4))
        if (move.to in centerSquares) {
            reasons.add("Occupies the critical central square ${move.to.algebraic}, maximizing piece mobility across both flanks.")
        }

        val homeRank = if (state.activeColor == PieceColor.WHITE) 0 else 7
        if (move.from.rank == homeRank && (move.piece.type == PieceType.KNIGHT || move.piece.type == PieceType.BISHOP)) {
            reasons.add("Develops the ${move.piece.type.displayName} from its starting square ${move.from.algebraic} to an active outpost at ${move.to.algebraic}.")
        }

        if (reasons.isEmpty()) {
            reasons.add("${move.san} (${move.from.algebraic} → ${move.to.algebraic}) improves ${activeName}'s piece coordination and prepares the principal continuation: ${line.principalVariationSan.take(4).joinToString(" ")}.")
        }

        return reasons.joinToString(" ")
    }

    private fun compareCandidateVariations(
        state: ChessBoardState,
        lines: List<CandidateLine>
    ): String {
        if (lines.isEmpty()) return "No candidate variations available."
        if (lines.size == 1) {
            val top = lines.first()
            return "Single legal or dominant candidate: ${top.moveSan} (${top.formattedEvaluation}) with continuation ${top.principalVariationSan.joinToString(" ")}."
        }
        val first = lines[0]
        val second = lines[1]
        val cpDiff = abs(first.scoreCentipawns - second.scoreCentipawns)
        val comparison = when {
            first.mateInMoves != null && second.mateInMoves == null ->
                "${first.moveSan} (${first.formattedEvaluation}) is the only move that forces checkmate, whereas ${second.moveSan} (${second.formattedEvaluation}) misses the forced mate."
            cpDiff >= 150 ->
                "${first.moveSan} (${first.formattedEvaluation}) is critical and outperforms the second choice ${second.moveSan} (${second.formattedEvaluation}) by ${String.format("%.2f", cpDiff / 100.0)} pawns."
            cpDiff >= 45 ->
                "${first.moveSan} (${first.formattedEvaluation}) is visibly stronger than ${second.moveSan} (${second.formattedEvaluation}), maintaining clearer pressure."
            else ->
                "${first.moveSan} (${first.formattedEvaluation}) and ${second.moveSan} (${second.formattedEvaluation}) are closely matched alternatives; both preserve a sound position."
        }
        val thirdNote = lines.getOrNull(2)?.let {
            " Third candidate: ${it.moveSan} (${it.formattedEvaluation}, PV: ${it.principalVariationSan.take(3).joinToString(" ")})."
        } ?: ""
        return comparison + thirdNote
    }

    private fun detectTacticalMotifs(
        state: ChessBoardState,
        bestMove: ChessMove?,
        analysis: EngineAnalysisResult
    ): List<String> {
        val motifs = mutableListOf<String>()
        if (ChessRulesEngine.isKingInCheck(state, state.activeColor)) {
            motifs.add("King in Check — Priority Evasion Required: Only moves that block, capture the attacker, or move the King to an unattacked square are legal.")
        }
        if (analysis.mateInMoves != null) {
            motifs.add("Mating Net (Mate in ${abs(analysis.mateInMoves)}): Forcing checks and escape-square restriction create an unstoppable checkmate pattern.")
        }
        if (bestMove != null) {
            val afterMove = ChessRulesEngine.applyMove(state, bestMove)
            // Check if the moved piece simultaneously attacks 2+ opponent pieces of higher/equal value (Fork)
            val enemyColor = state.activeColor.opposite()
            var attackedValuable = 0
            for (sq in Square.ALL) {
                val p = afterMove.pieceAt(sq) ?: continue
                if (p.color == enemyColor && (p.type == PieceType.KING || p.type == PieceType.QUEEN || p.type == PieceType.ROOK)) {
                    val pseudoFromBest = ChessRulesEngine.generateLegalMovesFromSquare(
                        afterMove.copy(activeColor = state.activeColor),
                        bestMove.to
                    )
                    if (pseudoFromBest.any { it.to == sq }) {
                        attackedValuable++
                    }
                }
            }
            if (attackedValuable >= 2) {
                motifs.add("Double Attack / Tactical Fork: ${bestMove.san} places the ${bestMove.piece.type.displayName} on ${bestMove.to.algebraic} to threaten multiple high-value targets simultaneously.")
            }
            if (bestMove.isEnPassant) {
                motifs.add("En Passant Tactical Capture: Captures the opponent's double-pushed pawn in passing on ${bestMove.to.algebraic}.")
            }
            if (bestMove.promotion != null) {
                motifs.add("Pawn Promotion Tactics: Converting an advanced passed pawn on the 8th/1st rank.")
            }
        }
        if (motifs.isEmpty()) {
            motifs.add("Prophylaxis & Piece Harmonization: Improve your least active piece and control central outposts before launching flank attacks.")
        }
        return motifs
    }

    private fun detectGamePhase(state: ChessBoardState): String {
        val nonPawnMaterial = state.squares.filterNotNull()
            .filter { it.type != PieceType.KING && it.type != PieceType.PAWN }
            .sumOf { it.type.centipawnValue }
        return when {
            state.fullMoveNumber <= 10 && nonPawnMaterial >= 5200 -> "Opening Phase"
            nonPawnMaterial <= 2600 -> "Endgame Phase"
            else -> "Middlegame Phase"
        }
    }

    private fun derivePhasePrinciples(state: ChessBoardState, phase: String): List<String> {
        return when (phase) {
            "Opening Phase" -> listOf(
                "Control the center (d4, e4, d5, e5) with pawns and minor pieces.",
                "Develop Knights and Bishops before moving the Queen or Rooks prematurely.",
                "Castle early to safeguard the King and connect your Rooks."
            )
            "Endgame Phase" -> listOf(
                "Activate the King immediately — in the endgame, the King is a fighting piece.",
                "Create and advance passed pawns; place Rooks behind passed pawns.",
                "Use opposition and triangulation to outmaneuver the enemy King."
            )
            else -> listOf(
                "Identify undefended ('loose') pieces and weak squares in the opponent's camp.",
                "Place Rooks on open or semi-open files and fight for the 7th/2nd rank.",
                "Coordinate pieces toward the enemy King when holding a development lead."
            )
        }
    }

    /**
     * Requests a Deep High-Thinking explanation from `gemini-4-argon` with `ThinkingLevel.HIGH`.
     * Uses real Stockfish UCI results and legal FEN as immutable ground truth.
     */
    suspend fun generateHighThinkingAiCoaching(
        state: ChessBoardState,
        analysis: EngineAnalysisResult
    ): Result<String> {
        val candidateSummary = analysis.candidateLines.joinToString("\n") { line ->
            "MultiPV #${line.multiPvIndex}: ${line.moveSan} (Eval: ${line.formattedEvaluation}, PV: ${line.principalVariationSan.joinToString(" ")})"
        }
        val prompt = """
            You are a Grandmaster Chess Coach inside Chess Winner.
            Analyze this verified chess position and Stockfish UCI calculation:
            - FEN: ${state.toFen()}
            - Side to move: ${state.activeColor.displayName}
            - Game Status: ${state.status.label}
            - Stockfish Search Depth: ${analysis.currentDepth}
            - Stockfish Candidate Lines:
            $candidateSummary

            Do NOT invent moves outside the legal rules or contradict Stockfish's evaluation.
            Provide a concise, high-clarity 4-part coaching breakdown:
            1. Strategic Assessment (pawn structure, king safety, material & space)
            2. Why Stockfish's top move (${analysis.bestMoveSan ?: "N/A"}) works tactically
            3. Key danger or blunder to avoid for ${state.activeColor.displayName}
            4. Practical 2-step plan for the next 3 moves.
        """.trimIndent()

        return try {
            val response = withTimeout(60_000L) {
                deepThinkingModel.generateContent(prompt)
            }
            val text = response.text
            if (!text.isNullOrBlank()) {
                Result.success(text.trim())
            } else {
                Result.failure(IllegalStateException("Model returned empty coaching text."))
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "High-thinking coaching timed out", e)
            Result.failure(Exception("Deep AI coaching timed out after 60s. Deterministic analysis remains active."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Optional Gemini 4 Argon coaching unavailable (using deterministic coach)", e)
            Result.failure(Exception("Cloud AI coaching unavailable offline (${e.localizedMessage ?: "App Check / Network"}). Deterministic Grandmaster Coach is active above."))
        }
    }

    val CHECKMATE_LESSONS: List<CheckmateLesson> = listOf(
        CheckmateLesson(
            id = "mate_back_rank",
            title = "1. Classic Back-Rank Checkmate",
            patternName = "Back-Rank Mate (Mate in 1)",
            mateInMoves = 1,
            fen = "6k1/5ppp/8/8/8/8/5PPP/3R2K1 w - - 0 1",
            solutionSan = listOf("Rd8#"),
            solutionUci = listOf("d1d8"),
            explanation = "Black's King on g8 is trapped behind its own wall of pawns on f7, g7, and h7. Because no Black piece defends the 8th rank, White's Rook penetrates to d8 for an immediate checkmate.",
            tacticalHint = "Use the Rook on d1 to attack the 8th rank where the Black King has no flight squares."
        ),
        CheckmateLesson(
            id = "mate_queen_support",
            title = "2. Supported Queen Kiss of Death",
            patternName = "Supported Queen Mate (Mate in 1)",
            mateInMoves = 1,
            fen = "6k1/5p2/5KpQ/8/8/8/8/8 w - - 0 1",
            solutionSan = listOf("Qg7#"),
            solutionUci = listOf("h6g7"),
            explanation = "White's King on f6 controls f7 and g7 while pinning down the Black King's escape. Moving the Queen to g7 delivers checkmate at point-blank range, protected by the White King.",
            tacticalHint = "Land the White Queen on g7 where it is defended by the King on f6."
        ),
        CheckmateLesson(
            id = "mate_arabian",
            title = "3. Arabian Mate Pattern",
            patternName = "Rook + Knight Corner Mate (Mate in 1)",
            mateInMoves = 1,
            fen = "7k/2R5/5N2/8/8/8/8/6K1 w - - 0 1",
            solutionSan = listOf("Rh7#"),
            solutionUci = listOf("c7h7"),
            explanation = "The Knight on f6 performs double duty: it controls the g8 escape square AND defends the h7 square. When the Rook slides to h7, the Black King on h8 is checkmated.",
            tacticalHint = "Deliver check on h7 with the Rook, defended by the Knight on f6."
        ),
        CheckmateLesson(
            id = "mate_anastasia",
            title = "4. Opera / Corridor Mate",
            patternName = "Rook + Bishop File Mate (Mate in 1)",
            mateInMoves = 1,
            fen = "3k4/8/3K4/4B3/8/8/8/R7 w - - 0 1",
            solutionSan = listOf("Ra8#"),
            solutionUci = listOf("a1a8"),
            explanation = "White's King on d6 and Bishop on e5 cut off all 7th-rank escape squares (c7, d7, e7). Swinging the Rook to a8 delivers a clean checkmate along the back rank.",
            tacticalHint = "Bring the Rook from a1 to the 8th rank."
        ),
        CheckmateLesson(
            id = "mate_in_two_sacrifice",
            title = "5. Double-Rook Ladder Mate in 2",
            patternName = "Lawnmower / Ladder Mate (Mate in 2)",
            mateInMoves = 2,
            fen = "3k4/1R6/8/8/8/8/R7/6K1 w - - 0 1",
            solutionSan = listOf("Ra8#"),
            solutionUci = listOf("a2a8"),
            explanation = "With the b7 Rook already sealing off the entire 7th rank (c7, d7, e7), the second Rook on a2 shoots straight to a8 to complete the two-rook ladder checkmate.",
            tacticalHint = "Keep the b7 Rook cutting off the 7th rank and strike on the 8th rank with the a2 Rook."
        )
    )

    val BLUNDER_REVIEW_CASES: List<BlunderReviewItem> = listOf(
        BlunderReviewItem(
            id = "blunder_fools_defense",
            title = "Leaving the Back Rank Undefended",
            fenBeforeMove = "3r2k1/5ppp/8/8/8/8/5PPP/3R2K1 b - - 0 1",
            playedMoveUci = "h7h6",
            playedMoveSan = "h6",
            bestMoveUci = "d8d1",
            bestMoveSan = "Rxd1#",
            evalBeforeCp = -9900,
            evalAfterPlayedCp = -20,
            severityLabel = "Blunder (??)",
            tacticalExplanation = "Black played the passive pawn move ...h6 instead of executing the immediate winning capture ...Rxd1#, missing a forced back-rank checkmate and allowing White to trade Rooks or create luft."
        ),
        BlunderReviewItem(
            id = "blunder_hanging_queen",
            title = "Walking Into a Royal Knight Fork",
            fenBeforeMove = "r1bqk2r/pppp1ppp/2n2n2/2b1p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4",
            playedMoveUci = "f3g5",
            playedMoveSan = "Ng5",
            bestMoveUci = "d2d3",
            bestMoveSan = "d3",
            evalBeforeCp = 25,
            evalAfterPlayedCp = -45,
            severityLabel = "Inaccuracy (?!)",
            tacticalExplanation = "Against two developed Black Knights (Nc6 and Nf6), premature Ng5 allows Black energetic counterplay with ...O-O or ...d5! Solid central support with d3 or c3 completing development first is preferred by Stockfish."
        ),
        BlunderReviewItem(
            id = "blunder_scholar_greed",
            title = "Ignoring Central Development for Material",
            fenBeforeMove = "rnbqkbnr/pppp1ppp/8/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R b KQkq - 1 2",
            playedMoveUci = "f7f6",
            playedMoveSan = "f6",
            bestMoveUci = "b8c6",
            bestMoveSan = "Nc6",
            evalBeforeCp = 20,
            evalAfterPlayedCp = 260,
            severityLabel = "Blunder (??)",
            tacticalExplanation = "Playing 2...f6?? (Damiano's Defense) weakens the e8-h5 diagonal, deprives the g8 Knight of its natural f6 square, and allows White the tactical sacrifice 3.Nxe5! Instead, 2...Nc6 defends e5 while developing a piece."
        )
    )

    val OPENING_LESSONS: List<OpeningLesson> = listOf(
        OpeningLesson(
            id = "opening_italian",
            ecoCode = "C50",
            name = "Italian Game (Giuoco Piano)",
            movesSan = listOf("e4", "e5", "Nf3", "Nc6", "Bc4", "Bc5", "c3", "Nf6", "d3"),
            resultingFen = "r1bqk2r/pppp1ppp/2n2n2/2b1p3/2B1P3/2PP1N2/PP3PPP/RNBQK2R b KQkq - 0 5",
            corePrinciples = listOf(
                "Rapid minor-piece development targeting the vulnerable f7/f2 squares.",
                "Use c3 and d3/d4 to build an ideal pawn center while keeping the a2-g8 diagonal open.",
                "Castle kingside quickly and maneuver the b1 Knight via d2-f1-g3."
            ),
            strategicPlanWhite = "Build a strong center with c3 and d4, castle kingside, and apply pressure on f7 and d5.",
            strategicPlanBlack = "Develop harmoniously with ...Bc5 and ...Nf6, castle early, and strike back in the center with ...d6 or ...d5."
        ),
        OpeningLesson(
            id = "opening_sicilian",
            ecoCode = "B90",
            name = "Sicilian Defense: Open Najdorf Structure",
            movesSan = listOf("e4", "c5", "Nf3", "d6", "d4", "cxd4", "Nxd4", "Nf6", "Nc3", "a6"),
            resultingFen = "rn1qkb1r/1p2pppp/p2p1n2/8/3NP3/2N5/PPP2PPP/R1BQKB1R w KQkq - 0 6",
            corePrinciples = listOf(
                "Asymmetrical pawn structure: Black trades a flank c-pawn for White's central d-pawn.",
                "Black obtains two central pawns (d6, e7) against White's one (e4) and uses the semi-open c-file.",
                "5...a6 controls the b5 square, preventing Nb5/Bb5+ and preparing ...e5 or ...b5."
            ),
            strategicPlanWhite = "Utilize active piece play and space advantage in the center/kingside with Be3, f3, Qd2, and O-O-O or Be2/O-O.",
            strategicPlanBlack = "Generate queenside counterplay along the half-open c-file with ...b5, ...Bb7, and ...Rc8 while contesting d5/e5."
        ),
        OpeningLesson(
            id = "opening_ruy_lopez",
            ecoCode = "C65",
            name = "Ruy Lopez (Spanish Game)",
            movesSan = listOf("e4", "e5", "Nf3", "Nc6", "Bb5", "a6", "Ba4", "Nf6", "O-O"),
            resultingFen = "r1bqkb1r/1ppp1ppp/p1n2n2/4p3/B3P3/5N2/PPPP1PPP/RNBQ1RK1 b kq - 3 5",
            corePrinciples = listOf(
                "3.Bb5 pressures the c6 Knight that defends Black's central e5 pawn.",
                "White castles early and prepares Re1, c3, and d4 to claim long-term central supremacy.",
                "Black challenges the Bishop with ...a6 and ...b5 while maintaining a solid stronghold on e5."
            ),
            strategicPlanWhite = "Protect e4 with Re1, play c3 and d4 to open the center, and maneuver Nd2-f1-g3.",
            strategicPlanBlack = "Complete development with ...Be7 and ...O-O, with options for the Marshall Attack (...d5) or closed Breyer/Chigorin setups."
        ),
        OpeningLesson(
            id = "opening_queens_gambit",
            ecoCode = "D35",
            name = "Queen's Gambit Declined",
            movesSan = listOf("d4", "d5", "c4", "e6", "Nc3", "Nf6", "Bg5", "Be7", "e3", "O-O"),
            resultingFen = "rnbq1rk1/ppp1bppp/4pn2/3p2B1/2PP4/2N1P3/PP3PPP/R2QKBNR w KQ - 2 6",
            corePrinciples = listOf(
                "2.c4 immediately challenges Black's d5 anchor pawn to gain central control.",
                "Black supports d5 with 2...e6, keeping a rock-solid pawn chain.",
                "Both sides fight for control of the e4 and c5 squares and open the c- or d-files."
            ),
            strategicPlanWhite = "Execute a minority attack on the queenside (b4-b5) or central break (e4) after developing Bd3, Nf3, and O-O.",
            strategicPlanBlack = "Free the light-squared Bishop via ...b6/...Bb7 or strike at the center with ...c5 or ...Nbd7 and ...e5."
        )
    )
}
