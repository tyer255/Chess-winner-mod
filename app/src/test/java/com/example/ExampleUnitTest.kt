package com.example

import com.example.chess.core.CastlingRights
import com.example.chess.core.ChessBoardState
import com.example.chess.core.ChessRulesEngine
import com.example.chess.core.GameStatus
import com.example.chess.core.Piece
import com.example.chess.core.PieceColor
import com.example.chess.core.PieceType
import com.example.chess.core.PositionManager
import com.example.chess.core.Square
import com.example.chess.engine.StockfishUciEngine
import com.example.chess.learning.ChessLearningAssistant
import com.example.chess.vision.ChessVisionPipeline
import com.example.chess.vision.VisionScanMode
import java.nio.ByteBuffer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `initial chess position has 20 legal moves and valid FEN`() {
        val state = ChessRulesEngine.createInitialState()
        assertEquals(ChessBoardState.INITIAL_FEN, state.toFen())
        val legalMoves = ChessRulesEngine.generateLegalMoves(state)
        assertEquals(20, legalMoves.size)
        assertEquals(GameStatus.ONGOING, state.status)
    }

    @Test
    fun `castling kingside and queenside works and updates rook and rights`() {
        val fen = "r3k2r/pppppppp/8/8/8/8/PPPPPPPP/R3K2R w KQkq - 0 1"
        val state = ChessRulesEngine.parseFen(fen).getOrThrow()
        val legalMoves = ChessRulesEngine.generateLegalMoves(state)

        val kingside = legalMoves.firstOrNull { it.isCastlingKingside }
        val queenside = legalMoves.firstOrNull { it.isCastlingQueenside }
        assertNotNull("White kingside castling must be legal", kingside)
        assertNotNull("White queenside castling must be legal", queenside)
        assertEquals("O-O", kingside!!.san)
        assertEquals("O-O-O", queenside!!.san)

        val afterKingside = ChessRulesEngine.applyMove(state, kingside)
        assertEquals(Piece(PieceType.KING, PieceColor.WHITE), afterKingside.pieceAt(Square.fromAlgebraic("g1")!!))
        assertEquals(Piece(PieceType.ROOK, PieceColor.WHITE), afterKingside.pieceAt(Square.fromAlgebraic("f1")!!))
        assertFalse(afterKingside.castlingRights.whiteKingside)
        assertFalse(afterKingside.castlingRights.whiteQueenside)
    }

    @Test
    fun `en passant capture works and removes captured pawn`() {
        var state = ChessRulesEngine.createInitialState()
        // 1. e4 a6 2. e5 d5 3. exd6 (en passant)
        val moves = listOf("e4", "a6", "e5", "d5")
        for (san in moves) {
            val m = ChessRulesEngine.parseSanMove(state, san)!!
            state = ChessRulesEngine.applyMove(state, m)
        }
        assertEquals(Square.fromAlgebraic("d6"), state.enPassantTarget)

        val epMove = ChessRulesEngine.generateLegalMoves(state).firstOrNull { it.isEnPassant }
        assertNotNull("En passant move exd6 must be generated", epMove)
        val afterEp = ChessRulesEngine.applyMove(state, epMove!!)
        assertEquals(Piece(PieceType.PAWN, PieceColor.WHITE), afterEp.pieceAt(Square.fromAlgebraic("d6")!!))
        assertEquals(null, afterEp.pieceAt(Square.fromAlgebraic("d5")!!))
    }

    @Test
    fun `pawn promotion generates 4 promotion choices and promotes correctly`() {
        val fen = "7k/P7/8/8/8/8/8/4K3 w - - 0 1"
        val state = ChessRulesEngine.parseFen(fen).getOrThrow()
        val a7Moves = ChessRulesEngine.generateLegalMovesFromSquare(state, Square.fromAlgebraic("a7")!!)
        assertEquals(4, a7Moves.size)
        val queenPromo = a7Moves.first { it.promotion == PieceType.QUEEN }
        val afterPromo = ChessRulesEngine.applyMove(state, queenPromo)
        assertEquals(Piece(PieceType.QUEEN, PieceColor.WHITE), afterPromo.pieceAt(Square.fromAlgebraic("a8")!!))
        assertEquals(GameStatus.CHECK, afterPromo.status)
    }

    @Test
    fun `fools mate checkmate and stalemate are accurately detected`() {
        // Fool's Mate: 1. f3 e5 2. g4 Qh4#
        val pgn = "1. f3 e5 2. g4 Qh4#"
        val finalState = ChessRulesEngine.importPgn(pgn).getOrThrow()
        assertEquals(GameStatus.CHECKMATE, finalState.status)
        assertTrue(ChessRulesEngine.generateLegalMoves(finalState).isEmpty())

        // Stalemate FEN
        val stalemateFen = "7k/5Q2/6K1/8/8/8/8/8 b - - 0 1"
        val staleState = ChessRulesEngine.parseFen(stalemateFen).getOrThrow()
        assertEquals(GameStatus.STALEMATE, staleState.status)
    }

    @Test
    fun `insufficient material and 50-move draw conditions are detected`() {
        val kvsk = ChessRulesEngine.parseFen("4k3/8/8/8/8/8/8/4K3 w - - 0 1").getOrThrow()
        assertEquals(GameStatus.DRAW_INSUFFICIENT_MATERIAL, kvsk.status)

        val fiftyMove = ChessRulesEngine.parseFen("4k3/8/8/8/8/8/R7/4K3 w - - 100 60").getOrThrow()
        assertEquals(GameStatus.DRAW_FIFTY_MOVE, fiftyMove.status)
    }

    @Test
    fun `undo and restart in PositionManager restore previous board states`() {
        val manager = PositionManager()
        val e2 = Square.fromAlgebraic("e2")!!
        val e4 = Square.fromAlgebraic("e4")!!
        manager.tryMoveBetweenSquares(e2, e4)
        assertEquals(1, manager.boardState.value.moveHistory.size)
        assertTrue(manager.undoLastMove())
        assertEquals(0, manager.boardState.value.moveHistory.size)
        assertEquals(ChessBoardState.INITIAL_FEN, manager.boardState.value.toFen())
    }

    @Test
    fun `StockfishUciEngine initializes, reports exact engine identity, and verifies Multi-PV, evaluation and mate-in-1`() = runBlocking {
        val engine = StockfishUciEngine()
        val initRes = engine.initializeEngine()
        assertTrue(initRes.isSuccess)
        assertTrue(engine.isReady.value)

        val identity = engine.verifyEngineIdentity()
        assertTrue(identity.uciHandshakeVerified)
        assertTrue(identity.executedBinaryPath.isNotBlank())
        if (!identity.isGenuineStockfishBinary) {
            assertEquals(StockfishUciEngine.FALLBACK_ENGINE_NAME, identity.uciIdName)
            assertTrue(identity.statusSummary.contains("Fallback", ignoreCase = true))
        } else {
            assertTrue(identity.uciIdName.contains("Stockfish", ignoreCase = true))
        }

        // Test Multi-PV & centipawn evaluation on Italian Game opening
        val italianState = ChessRulesEngine.parseFen(
            "r1bqkbnr/pppp1ppp/2n5/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R b KQkq - 3 3"
        ).getOrThrow()
        val openingAnalysis = engine.analyzePositionBlocking(italianState, depth = 2, maxTimeMs = 1000L)
        assertEquals(3, openingAnalysis.candidateLines.size)
        assertNotNull(openingAnalysis.bestMoveUci)
        assertNotNull(openingAnalysis.bestMoveSan)

        // Back-rank Mate in 1: 6k1/5ppp/8/8/8/8/5PPP/3R2K1 w - - 0 1 -> Rd8#
        val mateState = ChessRulesEngine.parseFen("6k1/5ppp/8/8/8/8/5PPP/3R2K1 w - - 0 1").getOrThrow()
        val analysis = engine.analyzePositionBlocking(mateState, depth = 2, maxTimeMs = 1000L)
        assertEquals("d1d8", analysis.bestMoveUci)
        assertEquals("Rd8#", analysis.bestMoveSan)
        assertNotNull(analysis.mateInMoves)
        assertEquals(1, analysis.mateInMoves)

        val explanation = ChessLearningAssistant.explainPositionDeterministically(mateState, analysis)
        assertTrue(explanation.headline.contains("Mate", ignoreCase = true))
    }

    @Test
    fun `StockfishUciEngine rejects invalid positions and clears stale recommendations`() = runBlocking {
        val engine = StockfishUciEngine()
        engine.initializeEngine()

        // First analyze a valid position so engine has a recommendation
        val validState = ChessRulesEngine.createInitialState()
        val validRes = engine.analyzePositionBlocking(validState, depth = 2, maxTimeMs = 500L)
        assertNotNull(validRes.bestMoveUci)

        // Now pass an invalid position (0 kings) to analyzePositionBlocking
        val invalidState = ChessBoardState(
            squares = List(64) { null },
            activeColor = PieceColor.WHITE,
            castlingRights = CastlingRights.NONE
        )
        val invalidRes = engine.analyzePositionBlocking(invalidState, depth = 2, maxTimeMs = 500L)
        assertNull("Invalid position must never produce bestMoveUci", invalidRes.bestMoveUci)
        assertNull("Invalid position must never produce bestMoveSan", invalidRes.bestMoveSan)
        assertTrue("Invalid position must have empty candidateLines", invalidRes.candidateLines.isEmpty())
        assertNotNull("Invalid position must report validation error", invalidRes.error)

        // Also verify analyzePosition(invalidState) clears stale state on StateFlow
        engine.analyzePosition(invalidState)
        assertNull(engine.analysisState.value.bestMoveUci)
        assertNull(engine.analysisState.value.bestMoveSan)
        assertTrue(engine.analysisState.value.candidateLines.isEmpty())
    }

    @Test
    fun `TFLite model audit verifies dimensions, labels, and reports missing model without fake weights`() {
        val pipeline = ChessVisionPipeline()
        val audit = pipeline.auditTfliteModel()
        assertFalse("chess_piece_classifier.tflite should not be fabricated", audit.assetExists)
        assertFalse("Trained model readiness must be false when .tflite is missing", audit.isTrainedModelReady)
        assertTrue("Board geometry detection must be ready", audit.isBoardGeometryDetectionReady)
        assertTrue("Manual calibration must be available", audit.isManualCalibrationAvailable)
        assertEquals(listOf(1, 64, 64, 3), audit.expectedInputDimensions)
        assertEquals(listOf(1, 13), audit.expectedOutputDimensions)
        assertEquals(13, audit.classLabels.size)
        assertTrue(pipeline.getModelStatusDescription().contains("Not Trained AI"))
        assertTrue(pipeline.verifyGeometryDetectorSelfTest())
    }

    @Test
    fun `extractUprightLuminanceFromYPlane strips rowStride padding and rotates sensor frames accurately`() {
        // Create a 4x2 image with rowStride = 8 (4 padding bytes per row)
        val width = 4
        val height = 2
        val rowStride = 8
        val rawBytes = ByteArray(rowStride * height) { 99.toByte() }
        // Row 0: 10, 20, 30, 40
        rawBytes[0] = 10; rawBytes[1] = 20; rawBytes[2] = 30; rawBytes[3] = 40
        // Row 1: 50, 60, 70, 80
        rawBytes[8] = 50; rawBytes[9] = 60; rawBytes[10] = 70; rawBytes[11] = 80

        val unrotated = ChessVisionPipeline.extractUprightLuminanceFromYPlane(
            buffer = ByteBuffer.wrap(rawBytes),
            width = width,
            height = height,
            rowStride = rowStride,
            pixelStride = 1,
            rotationDegrees = 0
        )
        assertEquals(4, unrotated.second)
        assertEquals(2, unrotated.third)
        assertEquals(10, unrotated.first[0].toInt())
        assertEquals(40, unrotated.first[3].toInt())
        assertEquals(50, unrotated.first[4].toInt())
        assertEquals(80, unrotated.first[7].toInt())

        // Rotate 90 degrees clockwise -> output is 2x4
        val rotated90 = ChessVisionPipeline.extractUprightLuminanceFromYPlane(
            buffer = ByteBuffer.wrap(rawBytes),
            width = width,
            height = height,
            rowStride = rowStride,
            pixelStride = 1,
            rotationDegrees = 90
        )
        assertEquals(2, rotated90.second)
        assertEquals(4, rotated90.third)
        // Top-left of 90-CW rotated image comes from (x=0, y=1) = 50; top-right from (x=0, y=0) = 10
        assertEquals(50, rotated90.first[0].toInt())
        assertEquals(10, rotated90.first[1].toInt())
    }

    @Test
    fun `ComputerVision pipeline tests physical board, second-phone display, angles, lighting, rotation, manual correction, and invalid-position rejection`() {
        val pipeline = ChessVisionPipeline()

        // 1. Uniform blank frame -> "Chess is not displaying"
        val blankFrame = ByteArray(128 * 128) { 100.toByte() }
        val resBlank = pipeline.analyzeLuminanceFrame(
            luminanceData = blankFrame,
            width = 128,
            height = 128,
            scanMode = VisionScanMode.PHYSICAL_BOARD
        )
        assertFalse(resBlank.isChessboardDetected)
        assertTrue(resBlank.statusBannerMessage.startsWith(ChessVisionPipeline.MSG_CHESS_NOT_DISPLAYING))

        // 2. Poor lighting detection (mean luminance < 32)
        val darkFrame = ByteArray(128 * 128) { 12.toByte() }
        val resDark = pipeline.analyzeLuminanceFrame(
            luminanceData = darkFrame,
            width = 128,
            height = 128,
            scanMode = VisionScanMode.PHYSICAL_BOARD
        )
        assertTrue(resDark.poorLightingDetected)
        assertFalse(resDark.isChessboardDetected)

        // 3. Synthetic 8x8 high-contrast checkerboard frame (Physical Board & Second Phone Display)
        val checkerFrame = ByteArray(128 * 128)
        for (y in 0 until 128) {
            for (x in 0 until 128) {
                val cellX = x / 16
                val cellY = y / 16
                val isLight = (cellX + cellY) % 2 == 0
                checkerFrame[y * 128 + x] = (if (isLight) 220 else 40).toByte()
            }
        }
        val resPhysical = pipeline.analyzeLuminanceFrame(
            luminanceData = checkerFrame,
            width = 128,
            height = 128,
            scanMode = VisionScanMode.PHYSICAL_BOARD,
            customCorners = com.example.chess.vision.BoardCorners(
                topLeft = com.example.chess.vision.NormalizedPoint(0f, 0f),
                topRight = com.example.chess.vision.NormalizedPoint(1f, 0f),
                bottomRight = com.example.chess.vision.NormalizedPoint(1f, 1f),
                bottomLeft = com.example.chess.vision.NormalizedPoint(0f, 1f)
            )
        )
        assertTrue(resPhysical.isChessboardDetected)
        assertFalse("Unconfirmed heuristic recognition must not claim full confidence", resPhysical.isPieceRecognitionConfident)
        assertEquals(ChessVisionPipeline.MSG_PIECE_RECOGNITION_REQUIRED, resPhysical.statusBannerMessage)

        // 4. Second-phone screen glare detection (bright specular pixels > 245)
        val glareFrame = checkerFrame.clone()
        for (i in 0 until (128 * 32)) {
            glareFrame[i] = 252.toByte()
        }
        val resSecondPhoneGlare = pipeline.analyzeLuminanceFrame(
            luminanceData = glareFrame,
            width = 128,
            height = 128,
            scanMode = VisionScanMode.SECOND_PHONE_DISPLAY
        )
        assertTrue(resSecondPhoneGlare.glareDetected)

        // 5. Board rotation / orientation coordinate mapping (White vs Black perspective)
        val corners = com.example.chess.vision.BoardCorners(
            topLeft = com.example.chess.vision.NormalizedPoint(0f, 0f),
            topRight = com.example.chess.vision.NormalizedPoint(1f, 0f),
            bottomRight = com.example.chess.vision.NormalizedPoint(1f, 1f),
            bottomLeft = com.example.chess.vision.NormalizedPoint(0f, 1f)
        )
        val a1White = corners.squareCenterInOverlay(Square.fromAlgebraic("a1")!!, PieceColor.WHITE)
        val a1Black = corners.squareCenterInOverlay(Square.fromAlgebraic("a1")!!, PieceColor.BLACK)
        assertTrue(a1White.x < 0.2f && a1White.y > 0.8f)
        assertTrue(a1Black.x > 0.8f && a1Black.y < 0.2f)

        // 6. Incorrect-position rejection: empty board (0 kings) confirmed by user -> rejected as invalid!
        val resEmptyConfirmed = pipeline.analyzeLuminanceFrame(
            luminanceData = checkerFrame,
            width = 128,
            height = 128,
            scanMode = VisionScanMode.SCREENSHOT_IMPORT,
            userConfirmedCalibration = true
        )
        assertNotNull(resEmptyConfirmed.validationResult)
        assertFalse("Empty board without kings must be rejected", resEmptyConfirmed.validationResult!!.isValid)
        assertTrue(resEmptyConfirmed.statusBannerMessage.startsWith("Invalid position:"))

        // 7. Incorrect-position rejection: two White Kings via manual override -> rejected!
        val invalidTwoKingsOverrides = mapOf(
            Square.fromAlgebraic("e1")!! to Piece(PieceType.KING, PieceColor.WHITE),
            Square.fromAlgebraic("d1")!! to Piece(PieceType.KING, PieceColor.WHITE),
            Square.fromAlgebraic("e8")!! to Piece(PieceType.KING, PieceColor.BLACK)
        )
        val resTwoKings = pipeline.analyzeLuminanceFrame(
            luminanceData = checkerFrame,
            width = 128,
            height = 128,
            scanMode = VisionScanMode.SCREENSHOT_IMPORT,
            manualOverrides = invalidTwoKingsOverrides,
            userConfirmedCalibration = true
        )
        assertFalse("Position with 2 White Kings must be rejected", resTwoKings.validationResult!!.isValid)

        // 8. Manual square correction with valid kings + rook and independent orientation vs activeColor
        val validOverrides = mapOf(
            Square.fromAlgebraic("e1")!! to Piece(PieceType.KING, PieceColor.WHITE),
            Square.fromAlgebraic("a1")!! to Piece(PieceType.ROOK, PieceColor.WHITE),
            Square.fromAlgebraic("e8")!! to Piece(PieceType.KING, PieceColor.BLACK)
        )
        val resValidCalibrated = pipeline.analyzeLuminanceFrame(
            luminanceData = checkerFrame,
            width = 128,
            height = 128,
            scanMode = VisionScanMode.SCREENSHOT_IMPORT,
            orientation = PieceColor.BLACK,
            manualOverrides = validOverrides,
            userConfirmedCalibration = true,
            activeColor = PieceColor.WHITE
        )
        assertTrue(resValidCalibrated.validationResult!!.isValid)
        assertEquals(PieceColor.WHITE, resValidCalibrated.recognizedState!!.activeColor)
        assertEquals(ChessVisionPipeline.MSG_CONFIGURATION_ALL_SET, resValidCalibrated.statusBannerMessage)
    }
}
