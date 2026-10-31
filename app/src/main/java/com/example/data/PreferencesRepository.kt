package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.chess.core.PieceColor
import com.example.ui.theme.BoardThemePalette
import com.example.ui.theme.PieceVisualStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.chessWinnerDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "chess_winner_preferences"
)

data class UserSettingsState(
    val defaultPlayerColor: PieceColor = PieceColor.WHITE,
    val engineDepth: Int = 4,
    val engineTimeMs: Long = 2000L,
    val multiPvLines: Int = 3,
    val boardTheme: BoardThemePalette = BoardThemePalette.TOURNAMENT_NAVY,
    val pieceStyle: PieceVisualStyle = PieceVisualStyle.STAUNTON_PRO,
    val soundEnabled: Boolean = true,
    val cameraFrameThrottleMs: Long = 350L,
    val useBackCameraByDefault: Boolean = true,
    val showOverlayArrows: Boolean = true,
    val showOverlayCoordinates: Boolean = true
)

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val DEFAULT_COLOR = stringPreferencesKey("default_player_color")
        val ENGINE_DEPTH = intPreferencesKey("engine_depth")
        val ENGINE_TIME_MS = longPreferencesKey("engine_time_ms")
        val MULTI_PV_LINES = intPreferencesKey("multi_pv_lines")
        val BOARD_THEME = stringPreferencesKey("board_theme")
        val PIECE_STYLE = stringPreferencesKey("piece_style")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val CAMERA_THROTTLE_MS = longPreferencesKey("camera_throttle_ms")
        val USE_BACK_CAMERA = booleanPreferencesKey("use_back_camera")
        val SHOW_OVERLAY_ARROWS = booleanPreferencesKey("show_overlay_arrows")
        val SHOW_OVERLAY_COORDS = booleanPreferencesKey("show_overlay_coords")
    }

    val settingsFlow: Flow<UserSettingsState> = context.chessWinnerDataStore.data.map { prefs ->
        val colorStr = prefs[Keys.DEFAULT_COLOR] ?: PieceColor.WHITE.name
        val color = runCatching { PieceColor.valueOf(colorStr) }.getOrDefault(PieceColor.WHITE)

        val themeStr = prefs[Keys.BOARD_THEME] ?: BoardThemePalette.TOURNAMENT_NAVY.name
        val theme = runCatching { BoardThemePalette.valueOf(themeStr) }
            .getOrDefault(BoardThemePalette.TOURNAMENT_NAVY)

        val styleStr = prefs[Keys.PIECE_STYLE] ?: PieceVisualStyle.STAUNTON_PRO.name
        val style = runCatching { PieceVisualStyle.valueOf(styleStr) }
            .getOrDefault(PieceVisualStyle.STAUNTON_PRO)

        UserSettingsState(
            defaultPlayerColor = color,
            engineDepth = (prefs[Keys.ENGINE_DEPTH] ?: 4).coerceIn(2, 10),
            engineTimeMs = (prefs[Keys.ENGINE_TIME_MS] ?: 2000L).coerceIn(500L, 8000L),
            multiPvLines = (prefs[Keys.MULTI_PV_LINES] ?: 3).coerceIn(1, 5),
            boardTheme = theme,
            pieceStyle = style,
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
            cameraFrameThrottleMs = (prefs[Keys.CAMERA_THROTTLE_MS] ?: 350L).coerceIn(150L, 1000L),
            useBackCameraByDefault = prefs[Keys.USE_BACK_CAMERA] ?: true,
            showOverlayArrows = prefs[Keys.SHOW_OVERLAY_ARROWS] ?: true,
            showOverlayCoordinates = prefs[Keys.SHOW_OVERLAY_COORDS] ?: true
        )
    }

    suspend fun setDefaultPlayerColor(color: PieceColor) {
        context.chessWinnerDataStore.edit { it[Keys.DEFAULT_COLOR] = color.name }
    }

    suspend fun setEngineDepth(depth: Int) {
        context.chessWinnerDataStore.edit { it[Keys.ENGINE_DEPTH] = depth.coerceIn(2, 10) }
    }

    suspend fun setEngineTimeMs(timeMs: Long) {
        context.chessWinnerDataStore.edit { it[Keys.ENGINE_TIME_MS] = timeMs.coerceIn(500L, 8000L) }
    }

    suspend fun setMultiPvLines(lines: Int) {
        context.chessWinnerDataStore.edit { it[Keys.MULTI_PV_LINES] = lines.coerceIn(1, 5) }
    }

    suspend fun setBoardTheme(theme: BoardThemePalette) {
        context.chessWinnerDataStore.edit { it[Keys.BOARD_THEME] = theme.name }
    }

    suspend fun setPieceStyle(style: PieceVisualStyle) {
        context.chessWinnerDataStore.edit { it[Keys.PIECE_STYLE] = style.name }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.chessWinnerDataStore.edit { it[Keys.SOUND_ENABLED] = enabled }
    }

    suspend fun setCameraThrottleMs(throttleMs: Long) {
        context.chessWinnerDataStore.edit { it[Keys.CAMERA_THROTTLE_MS] = throttleMs.coerceIn(150L, 1000L) }
    }

    suspend fun setUseBackCamera(useBack: Boolean) {
        context.chessWinnerDataStore.edit { it[Keys.USE_BACK_CAMERA] = useBack }
    }

    suspend fun setShowOverlayArrows(show: Boolean) {
        context.chessWinnerDataStore.edit { it[Keys.SHOW_OVERLAY_ARROWS] = show }
    }

    suspend fun setShowOverlayCoordinates(show: Boolean) {
        context.chessWinnerDataStore.edit { it[Keys.SHOW_OVERLAY_COORDS] = show }
    }
}
