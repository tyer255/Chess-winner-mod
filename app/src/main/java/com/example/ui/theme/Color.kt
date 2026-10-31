package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Primary Deep Navy & Charcoal Palette
val NavyBackground = Color(0xFF080E1E)
val NavySurface = Color(0xFF0F172A)
val NavySurfaceVariant = Color(0xFF16223B)
val CharcoalCard = Color(0xFF1E293B)
val CharcoalBorder = Color(0xFF334155)

// Cyan & Electric Blue Accents
val CyanAccent = Color(0xFF00E5FF)
val CyanMuted = Color(0xFF06B6D4)
val ElectricBlue = Color(0xFF3B82F6)
val DeepCyanContainer = Color(0xFF083344)

// Semantic Status Colors
val EmeraldSuccess = Color(0xFF10B981)
val AmberWarning = Color(0xFFF59E0B)
val CrimsonError = Color(0xFFEF4444)

// Text Hierarchy
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)

// Chessboard Theme Palettes
enum class BoardThemePalette(
    val displayName: String,
    val lightSquare: Color,
    val darkSquare: Color,
    val borderColor: Color,
    val coordColor: Color
) {
    TOURNAMENT_NAVY(
        displayName = "Grandmaster Navy",
        lightSquare = Color(0xFFCBD5E1),
        darkSquare = Color(0xFF334E68),
        borderColor = Color(0xFF0F172A),
        coordColor = Color(0xFF00E5FF)
    ),
    CLASSIC_WOOD(
        displayName = "Staunton Walnut",
        lightSquare = Color(0xFFEDD6B0),
        darkSquare = Color(0xFF8B5A2B),
        borderColor = Color(0xFF2C1810),
        coordColor = Color(0xFFFDE68A)
    ),
    OBSIDIAN_CYBER(
        displayName = "Obsidian Cyan",
        lightSquare = Color(0xFF334155),
        darkSquare = Color(0xFF0F172A),
        borderColor = Color(0xFF080E1E),
        coordColor = Color(0xFF00E5FF)
    ),
    CHAMPIONSHIP_GREEN(
        displayName = "Tournament Club",
        lightSquare = Color(0xFFEEEED2),
        darkSquare = Color(0xFF4B7399),
        borderColor = Color(0xFF1E293B),
        coordColor = Color(0xFF38BDF8)
    )
}

enum class PieceVisualStyle(val displayName: String) {
    STAUNTON_PRO("Tournament Staunton"),
    GEOMETRIC_MODERN("Geometric Precision"),
    NEO_CLASSIC("Neo-Classic")
}
