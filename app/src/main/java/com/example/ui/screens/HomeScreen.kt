package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.chess.core.PieceColor
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CharcoalBorder
import com.example.ui.theme.CharcoalCard
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DeepCyanContainer
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.NavyBackground
import com.example.ui.theme.NavySurface
import com.example.ui.theme.NavySurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.AppDestination
import com.example.viewmodel.ModuleCheckStatus
import com.example.viewmodel.ModuleInitItem
import com.example.viewmodel.SessionLaunchTarget
import com.example.viewmodel.SystemInitReport

@Composable
fun HomeScreen(
    engineReady: Boolean,
    savedGamesCount: Int,
    onSelectSessionTarget: (SessionLaunchTarget) -> Unit,
    onNavigate: (AppDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(NavyBackground)
            .testTag("home_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. CHESS WINNER Hero Header Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
                    .border(1.dp, CyanAccent.copy(alpha = 0.35f), RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = NavySurface)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_banner),
                        contentDescription = "Chess Winner Tactical Analysis Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(168.dp)
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                color = DeepCyanContainer.copy(alpha = 0.9f),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.border(1.dp, CyanAccent.copy(alpha = 0.5f), RoundedCornerShape(50))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (engineReady) EmeraldSuccess else AmberWarning)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (engineReady) "UCI TACTICAL ENGINE READY" else "INITIALIZING ENGINE",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = CyanAccent
                                    )
                                }
                            }

                            Text(
                                text = "OFFLINE-FIRST",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(22.dp))

                        Text(
                            text = stringResource(id = R.string.brand_title),
                            style = MaterialTheme.typography.displayLarge,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(id = R.string.brand_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = CyanAccent
                        )
                    }
                }
            }
        }

        // 2. Primary Action: Start Chess Analysis
        item {
            PrimaryHeroActionCard(
                title = stringResource(id = R.string.home_start_analysis),
                subtitle = "Launch Stockfish UCI Multi-PV calculation, mate-in-N detection & tactical coaching",
                icon = Icons.Default.Analytics,
                accentColor = CyanAccent,
                testTag = "home_start_analysis",
                onClick = { onSelectSessionTarget(SessionLaunchTarget.START_ANALYSIS) }
            )
        }

        // 3. Three Core Chess Input Modes: Camera Chess, Physical Practice, Digital Board
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "CHESS INPUT & RECOGNITION MODES",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary,
                    modifier = Modifier.padding(start = 4.dp)
                )

                HomeMenuFeatureRow(
                    title = stringResource(id = R.string.home_camera_chess),
                    subtitle = "Live CameraX detection for physical boards & 2nd mobile phone screens",
                    badgeText = "CAMERAX + CV",
                    icon = Icons.Default.CameraAlt,
                    accentColor = CyanAccent,
                    testTag = "home_camera_chess",
                    onClick = { onSelectSessionTarget(SessionLaunchTarget.CAMERA_CHESS) }
                )

                HomeMenuFeatureRow(
                    title = stringResource(id = R.string.home_physical_practice),
                    subtitle = "Aligned AR board overlay, perspective calibration & tactical arrows",
                    badgeText = "3D OVERLAY",
                    icon = Icons.Default.Visibility,
                    accentColor = EmeraldSuccess,
                    testTag = "home_physical_practice",
                    onClick = { onSelectSessionTarget(SessionLaunchTarget.PHYSICAL_PRACTICE) }
                )

                HomeMenuFeatureRow(
                    title = stringResource(id = R.string.home_digital_board),
                    subtitle = "Same-device playable 8x8 chessboard with full FIDE rules & Stockfish play",
                    badgeText = "PLAYABLE",
                    icon = Icons.Default.GridOn,
                    accentColor = ElectricBlue,
                    testTag = "home_digital_board",
                    onClick = { onSelectSessionTarget(SessionLaunchTarget.DIGITAL_CHESSBOARD) }
                )
            }
        }

        // 4. Secondary Modules Grid: Import Game, Saved Games, Chess Learning, Settings
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "STUDY, STORAGE & SYSTEM",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary,
                    modifier = Modifier.padding(start = 4.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CompactFeatureGridCard(
                        title = stringResource(id = R.string.home_import_game),
                        subtitle = "FEN, PGN, Photo Picker & Board Editor",
                        icon = Icons.Default.FileUpload,
                        accentColor = CyanAccent,
                        testTag = "home_import_game",
                        onClick = { onNavigate(AppDestination.IMPORT_EXPORT) },
                        modifier = Modifier.weight(1f)
                    )
                    CompactFeatureGridCard(
                        title = stringResource(id = R.string.home_saved_games),
                        subtitle = "$savedGamesCount stored in Room DB",
                        icon = Icons.Default.Bookmark,
                        accentColor = ElectricBlue,
                        testTag = "home_saved_games",
                        onClick = { onNavigate(AppDestination.SAVED_GAMES) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CompactFeatureGridCard(
                        title = stringResource(id = R.string.home_chess_learning),
                        subtitle = "Checkmates, Blunders & Openings",
                        icon = Icons.Default.School,
                        accentColor = EmeraldSuccess,
                        testTag = "home_chess_learning",
                        onClick = { onNavigate(AppDestination.CHESS_LEARNING) },
                        modifier = Modifier.weight(1f)
                    )
                    CompactFeatureGridCard(
                        title = stringResource(id = R.string.home_settings),
                        subtitle = "Engine Depth, Themes & GPLv3 License",
                        icon = Icons.Default.Settings,
                        accentColor = AmberWarning,
                        testTag = "home_settings",
                        onClick = { onNavigate(AppDestination.SETTINGS) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PrimaryHeroActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 640.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(DeepCyanContainer, NavySurfaceVariant)
                )
            )
            .border(1.5.dp, accentColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = accentColor.copy(alpha = 0.2f),
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Launch $title",
                tint = accentColor
            )
        }
    }
}

@Composable
private fun HomeMenuFeatureRow(
    title: String,
    subtitle: String,
    badgeText: String,
    icon: ImageVector,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, CharcoalBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CharcoalCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = accentColor.copy(alpha = 0.16f),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = accentColor.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelMedium,
                            fontSize = 10.sp,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Open $title",
                tint = TextSecondary
            )
        }
    }
}

@Composable
private fun CompactFeatureGridCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, CharcoalBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CharcoalCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = accentColor.copy(alpha = 0.16f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
    }
}

/**
 * Section 5: PLAYER COLOR CONFIGURATION & 7-MODULE INITIALIZATION VERIFICATION SCREEN
 */
@Composable
fun ColorConfigurationScreen(
    report: SystemInitReport,
    onSelectColor: (PieceColor) -> Unit,
    onStartInitialization: (Boolean) -> Unit,
    onContinueToSession: () -> Unit,
    onSwitchToDigitalFallback: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current

    var hasCameraPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPerm = granted
        onStartInitialization(granted)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NavyBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("color_config_screen"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("color_config_back")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Home",
                    tint = TextPrimary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(id = R.string.select_your_color),
                    style = MaterialTheme.typography.titleLarge,
                    color = CyanAccent
                )
                Text(
                    text = "${report.launchTarget.title} • Choose side & initialize modules",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }

        // "Configuration All Set" Success Banner (ONLY shown after all required modules pass)
        if (report.isAllSet) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .border(1.5.dp, EmeraldSuccess, RoundedCornerShape(14.dp))
                    .testTag("configuration_all_set_banner"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF062E22))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Configuration All Set",
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(id = R.string.configuration_all_set),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldSuccess
                            )
                            Text(
                                text = report.readinessBannerSubtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                fontSize = 12.sp,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = report.nextActionGuidance,
                                style = MaterialTheme.typography.bodyMedium,
                                fontSize = 11.sp,
                                color = CyanAccent
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onContinueToSession,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldSuccess,
                            contentColor = Color(0xFF022C22)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("enter_session_after_init_button")
                    ) {
                        Text(
                            text = "Launch ${report.launchTarget.title} (${report.selectedColor.displayName})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ColorChoiceCard(
                title = stringResource(id = R.string.white_pieces),
                kingGlyph = "♔",
                subtitle = "Rank 1 bottom",
                isSelected = report.selectedColor == PieceColor.WHITE,
                testTag = "select_white_pieces",
                onClick = { onSelectColor(PieceColor.WHITE) },
                modifier = Modifier.weight(1f)
            )
            ColorChoiceCard(
                title = stringResource(id = R.string.black_pieces),
                kingGlyph = "♚",
                subtitle = "Rank 8 bottom",
                isSelected = report.selectedColor == PieceColor.BLACK,
                testTag = "select_black_pieces",
                onClick = { onSelectColor(PieceColor.BLACK) },
                modifier = Modifier.weight(1f)
            )
        }

        Button(
            onClick = {
                val currentPerm = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
                hasCameraPerm = currentPerm
                if (report.launchTarget.requiresCameraAndVision && !currentPerm) {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                } else {
                    onStartInitialization(currentPerm)
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = CyanAccent,
                contentColor = Color(0xFF04131D)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .height(48.dp)
                .testTag("start_session_button")
        ) {
            Text(
                text = stringResource(id = R.string.start_session),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Failure & Recovery Card (when any module fails initialization)
        if (report.failedModuleName != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, CrimsonError, RoundedCornerShape(14.dp))
                    .testTag("module_init_failure_banner"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2D0A12))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Module Failure",
                            tint = CrimsonError,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Initialization Halted: ${report.failedModuleName}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CrimsonError
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = report.failureRecoveryMessage ?: "Check module permissions and retry.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (report.launchTarget.requiresCameraAndVision) {
                            Button(
                                onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("grant_camera_recovery_button")
                            ) {
                                Text("Grant Camera Permission")
                            }
                            OutlinedButton(
                                onClick = onSwitchToDigitalFallback,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("fallback_digital_board_button")
                            ) {
                                Text("Use Digital Board", color = TextPrimary)
                            }
                        } else {
                            Button(
                                onClick = { onStartInitialization(hasCameraPerm) },
                                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Retry Initialization")
                            }
                        }
                    }
                }
            }
        }

        // 7-Module Diagnostic Checklist
        Text(
            text = "SYSTEM MODULE VERIFICATION CHECKLIST (7 MODULES)",
            style = MaterialTheme.typography.labelLarge,
            color = TextSecondary,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
        )

        report.modules.forEach { module ->
            ModuleChecklistRow(
                item = module,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
            )
        }
    }
}

@Composable
private fun ColorChoiceCard(
    title: String,
    kingGlyph: String,
    subtitle: String,
    isSelected: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) CyanAccent else CharcoalBorder
    val bgColor = if (isSelected) DeepCyanContainer else CharcoalCard

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = kingGlyph,
                fontSize = 26.sp,
                color = if (isSelected) CyanAccent else TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 11.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun ModuleChecklistRow(
    item: ModuleInitItem,
    modifier: Modifier = Modifier
) {
    val (icon, tint) = when (item.status) {
        ModuleCheckStatus.PASSED -> Icons.Default.CheckCircle to EmeraldSuccess
        ModuleCheckStatus.WARNING -> Icons.Default.Warning to AmberWarning
        ModuleCheckStatus.FAILED -> Icons.Default.Error to CrimsonError
        ModuleCheckStatus.CHECKING -> Icons.Default.HourglassTop to CyanAccent
        ModuleCheckStatus.PENDING -> Icons.Default.AutoAwesome to TextSecondary
    }

    Card(
        modifier = modifier
            .border(1.dp, CharcoalBorder, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CharcoalCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = item.status.name,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${item.id}. ${item.moduleName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = tint.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = item.readinessCategory.badgeLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontSize = 10.sp,
                            color = tint,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = item.detailMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }
    }
}
