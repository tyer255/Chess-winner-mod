package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "saved_games")
data class SavedGameEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val whitePlayer: String,
    val blackPlayer: String,
    val event: String,
    val finalFen: String,
    val pgnText: String,
    val resultText: String,
    val moveCount: Int,
    val evaluationSummary: String,
    val sourceMode: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "learning_progress")
data class LearningProgressEntity(
    @PrimaryKey val lessonId: String,
    val category: String,
    val completed: Boolean,
    val completedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "analysis_snapshots")
data class AnalysisSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val fen: String,
    val bestMoveSan: String,
    val evalScoreText: String,
    val depth: Int,
    val notes: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface ChessDao {
    @Query("SELECT * FROM saved_games ORDER BY timestamp DESC")
    fun getAllSavedGames(): Flow<List<SavedGameEntity>>

    @Query("SELECT COUNT(*) FROM saved_games")
    suspend fun getSavedGamesCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedGame(game: SavedGameEntity): Long

    @Query("DELETE FROM saved_games WHERE id = :id")
    suspend fun deleteSavedGameById(id: Long)

    @Query("DELETE FROM saved_games")
    suspend fun deleteAllSavedGames()

    @Query("SELECT * FROM learning_progress")
    fun getAllLearningProgress(): Flow<List<LearningProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLearningProgress(progress: LearningProgressEntity)

    @Query("SELECT * FROM analysis_snapshots ORDER BY timestamp DESC")
    fun getAllAnalysisSnapshots(): Flow<List<AnalysisSnapshotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalysisSnapshot(snapshot: AnalysisSnapshotEntity): Long

    @Query("DELETE FROM analysis_snapshots WHERE id = :id")
    suspend fun deleteAnalysisSnapshotById(id: Long)
}

@Database(
    entities = [
        SavedGameEntity::class,
        LearningProgressEntity::class,
        AnalysisSnapshotEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ChessWinnerDatabase : RoomDatabase() {
    abstract fun chessDao(): ChessDao

    companion object {
        @Volatile
        private var INSTANCE: ChessWinnerDatabase? = null

        fun getInstance(context: Context): ChessWinnerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ChessWinnerDatabase::class.java,
                    "chess_winner_offline.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class ChessRepository(private val dao: ChessDao) {

    val savedGames: Flow<List<SavedGameEntity>> = dao.getAllSavedGames()
    val learningProgress: Flow<List<LearningProgressEntity>> = dao.getAllLearningProgress()
    val analysisSnapshots: Flow<List<AnalysisSnapshotEntity>> = dao.getAllAnalysisSnapshots()

    suspend fun saveGame(game: SavedGameEntity): Long = dao.insertSavedGame(game)

    suspend fun deleteGame(id: Long) = dao.deleteSavedGameById(id)

    suspend fun clearAllSavedGames() = dao.deleteAllSavedGames()

    suspend fun markLessonCompleted(lessonId: String, category: String) {
        dao.upsertLearningProgress(
            LearningProgressEntity(
                lessonId = lessonId,
                category = category,
                completed = true
            )
        )
    }

    suspend fun saveAnalysisSnapshot(snapshot: AnalysisSnapshotEntity): Long =
        dao.insertAnalysisSnapshot(snapshot)

    suspend fun deleteAnalysisSnapshot(id: Long) = dao.deleteAnalysisSnapshotById(id)

    suspend fun verifyAndSeedInitialGamesIfNeeded(): Boolean {
        val count = dao.getSavedGamesCount()
        if (count == 0) {
            dao.insertSavedGame(
                SavedGameEntity(
                    title = "Morphy's Opera House Game (Tactical Masterpiece)",
                    whitePlayer = "Paul Morphy",
                    blackPlayer = "Duke Karl / Count Isouard",
                    event = "Paris Opera House 1858",
                    finalFen = "1n1Rkb1r/p4ppp/4q3/4p1B1/4P3/8/PPP2PPP/2K5 b k - 1 17",
                    pgnText = """
                        [Event "Paris Opera House"]
                        [White "Paul Morphy"]
                        [Black "Duke Karl"]
                        [Result "1-0"]

                        1. e4 e5 2. Nf3 d6 3. d4 Bg4 4. dxe5 Bxf3 5. Qxf3 dxe5 6. Bc4 Nf6 7. Qb3 Qe7 8. Nc3 c6 9. Bg5 b5 10. Nxb5 cxb5 11. Bxb5+ Nbd7 12. O-O-O Rd8 13. Rxd7 Rxd7 14. Rd1 Qe6 15. Bxd7+ Nxd7 16. Qb8+ Nxb8 17. Rd8# 1-0
                    """.trimIndent(),
                    resultText = "1-0 (Checkmate)",
                    moveCount = 17,
                    evaluationSummary = "+M0 (Opera Mate Delivered)",
                    sourceMode = "Study Library"
                )
            )
            dao.insertSavedGame(
                SavedGameEntity(
                    title = "Italian Giuoco Piano — Central Breakthrough",
                    whitePlayer = "Chess Winner Study",
                    blackPlayer = "Club Opponent",
                    event = "Tactical Training",
                    finalFen = "r1bqk2r/pppp1ppp/2n2n2/2b1p3/2B1P3/2PP1N2/PP3PPP/RNBQK2R b KQkq - 0 5",
                    pgnText = """
                        [Event "Tactical Training"]
                        [White "Chess Winner Study"]
                        [Black "Club Opponent"]
                        [Result "*"]

                        1. e4 e5 2. Nf3 Nc6 3. Bc4 Bc5 4. c3 Nf6 5. d3 *
                    """.trimIndent(),
                    resultText = "In Progress",
                    moveCount = 5,
                    evaluationSummary = "+0.28 (Balanced Opening)",
                    sourceMode = "Digital Chessboard"
                )
            )
        }
        return true
    }
}
