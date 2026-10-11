package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.game.ControlScheme
import com.example.game.GameMode
import com.example.ui.components.RetroArcadeCanvas
import com.example.ui.theme.CruiserRed
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonYellow
import com.example.ui.theme.ShieldBlue
import com.example.ui.theme.SpaceBlack
import com.example.ui.theme.SpaceCard
import com.example.ui.theme.SpaceCardBorder
import com.example.ui.theme.SpaceDark
import com.example.ui.theme.TerminalDark
import com.example.ui.theme.TerminalGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.GameViewModel

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snap = uiState.snapshot
    val engine = viewModel.activeEngine

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (uiState.currentGameMode == GameMode.CLASSIC_TERMINAL) TerminalDark else SpaceBlack)
    ) {
        // Game Canvas & Touch Interceptor
        if (engine != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(uiState.controlScheme) {
                        if (uiState.controlScheme == ControlScheme.DRAG) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val targetWorldX = (offset.x / size.width) * engine.worldWidth
                                    viewModel.setTouchTargetX(targetWorldX)
                                    viewModel.setFiring(true)
                                },
                                onDrag = { change, _ ->
                                    val targetWorldX = (change.position.x / size.width) * engine.worldWidth
                                    viewModel.setTouchTargetX(targetWorldX)
                                    change.consume()
                                },
                                onDragEnd = {
                                    viewModel.setTouchTargetX(null)
                                },
                                onDragCancel = {
                                    viewModel.setTouchTargetX(null)
                                }
                            )
                        }
                    }
                    .pointerInput(uiState.controlScheme) {
                        if (uiState.controlScheme == ControlScheme.DRAG) {
                            detectTapGestures(
                                onPress = { offset ->
                                    val targetWorldX = (offset.x / size.width) * engine.worldWidth
                                    viewModel.setTouchTargetX(targetWorldX)
                                    viewModel.triggerSingleShot()
                                    tryAwaitRelease()
                                    viewModel.setTouchTargetX(null)
                                }
                            )
                        }
                    }
            ) {
                RetroArcadeCanvas(
                    gameEngine = engine,
                    crtScanlinesEnabled = uiState.crtScanlines,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Top HUD Overlay
        GameTopHud(
            uiState = uiState,
            onPauseClick = { viewModel.pauseGame() },
            onOverdriveClick = { viewModel.triggerOverdrive() },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Bottom Controls Overlay (When BUTTONS mode or manual Fire needed)
        if (!snap.isGameOver && !snap.isPaused) {
            GameBottomControls(
                controlScheme = uiState.controlScheme,
                overdriveReady = snap.overdriveGauge >= 100f,
                autoFire = uiState.autoFire,
                onLeftPress = { viewModel.setInputDirectionX(-1f) },
                onLeftRelease = { viewModel.setInputDirectionX(0f) },
                onRightPress = { viewModel.setInputDirectionX(1f) },
                onRightRelease = { viewModel.setInputDirectionX(0f) },
                onFirePress = { viewModel.setFiring(true) },
                onFireRelease = { viewModel.setFiring(false) },
                onOverdriveClick = { viewModel.triggerOverdrive() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
            )
        }

        // Pause Menu Dialog
        if (snap.isPaused && !snap.isGameOver) {
            PauseOverlay(
                uiState = uiState,
                onResume = { viewModel.resumeGame() },
                onRestart = { viewModel.restartGame() },
                onQuit = {
                    viewModel.quitGameToMenu()
                    onNavigateBack()
                },
                onToggleSound = { viewModel.toggleSound() },
                onToggleHaptics = { viewModel.toggleHaptics() }
            )
        }

        // Game Over Screen Overlay
        AnimatedVisibility(
            visible = snap.isGameOver,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            GameOverOverlay(
                uiState = uiState,
                onRestart = { viewModel.restartGame() },
                onHangar = {
                    viewModel.quitGameToMenu()
                    onNavigateBack()
                }
            )
        }
    }
}

@Composable
private fun GameTopHud(
    uiState: com.example.viewmodel.GameUiState,
    onPauseClick: () -> Unit,
    onOverdriveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snap = uiState.snapshot
    val isClassic = uiState.currentGameMode == GameMode.CLASSIC_TERMINAL

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Score & Multiplier
            Column {
                Text(
                    text = "SCORE",
                    color = if (isClassic) TerminalGreen else TextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${snap.score}",
                        color = if (isClassic) TerminalGreen else TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                    if (snap.comboMultiplier > 1) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = NeonYellow.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, NeonYellow)
                        ) {
                            Text(
                                text = "x${snap.comboMultiplier}",
                                color = NeonYellow,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Level / Sector Badge & Mode
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isClassic) TerminalDark else SpaceCard,
                border = BorderStroke(1.dp, if (isClassic) TerminalGreen else SpaceCardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "SECTOR ${snap.level}",
                        color = if (isClassic) TerminalGreen else NeonCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    if (uiState.currentGameMode == GameMode.TIME_ATTACK) {
                        Text(
                            text = "${snap.timeRemainingSec}s",
                            color = NeonYellow,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Pause Button
            IconButton(
                onClick = onPauseClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isClassic) TerminalDark else SpaceCard)
                    .border(1.dp, if (isClassic) TerminalGreen else SpaceCardBorder, CircleShape)
                    .testTag("pause_game_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Pause,
                    contentDescription = "Pause Game",
                    tint = if (isClassic) TerminalGreen else TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Shield & Health Bars
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Shield Bar
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "SHIELD",
                        color = if (isClassic) TerminalGreen else ShieldBlue,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${snap.shield}/${snap.maxShield}",
                        color = if (isClassic) TerminalGreen else ShieldBlue,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                LinearProgressIndicator(
                    progress = { if (snap.maxShield > 0) (snap.shield.toFloat() / snap.maxShield).coerceIn(0f, 1f) else 0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (isClassic) TerminalGreen else ShieldBlue,
                    trackColor = if (isClassic) TerminalDark else SpaceDark
                )
            }

            // Health Bar
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "HULL HP",
                        color = if (isClassic) TerminalGreen else if (snap.health < 30) CruiserRed else NeonGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${snap.health}/${snap.maxHealth}",
                        color = if (isClassic) TerminalGreen else if (snap.health < 30) CruiserRed else NeonGreen,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                LinearProgressIndicator(
                    progress = { if (snap.maxHealth > 0) (snap.health.toFloat() / snap.maxHealth).coerceIn(0f, 1f) else 0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (isClassic) TerminalGreen else if (snap.health < 30) CruiserRed else NeonGreen,
                    trackColor = if (isClassic) TerminalDark else SpaceDark
                )
            }
        }

        // Active Powerups Row
        if (snap.activePowerups.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                snap.activePowerups.forEach { p ->
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = NeonCyan.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = p,
                            color = NeonCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GameBottomControls(
    controlScheme: ControlScheme,
    overdriveReady: Boolean,
    autoFire: Boolean,
    onLeftPress: () -> Unit,
    onLeftRelease: () -> Unit,
    onRightPress: () -> Unit,
    onRightRelease: () -> Unit,
    onFirePress: () -> Unit,
    onFireRelease: () -> Unit,
    onOverdriveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        // D-Pad for Buttons Scheme
        if (controlScheme == ControlScheme.BUTTONS) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ArcadeDirectionButton(
                    icon = Icons.Default.ChevronLeft,
                    contentDesc = "Left",
                    onPress = onLeftPress,
                    onRelease = onLeftRelease,
                    testTag = "control_btn_left"
                )
                ArcadeDirectionButton(
                    icon = Icons.Default.ChevronRight,
                    contentDesc = "Right",
                    onPress = onRightPress,
                    onRelease = onRightRelease,
                    testTag = "control_btn_right"
                )
            }
        } else {
            Spacer(modifier = Modifier.width(1.dp))
        }

        // Action Buttons: EMP Bomb + Fire
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Overdrive Bomb Button
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .clip(CircleShape)
                    .background(
                        if (overdriveReady) Brush.radialGradient(
                            listOf(NeonMagenta, CruiserRed, SpaceDark)
                        ) else Brush.radialGradient(listOf(SpaceCard, SpaceDark))
                    )
                    .border(
                        2.dp,
                        if (overdriveReady) NeonMagenta else SpaceCardBorder,
                        CircleShape
                    )
                    .pointerInput(overdriveReady) {
                        detectTapGestures(
                            onTap = {
                                if (overdriveReady) onOverdriveClick()
                            }
                        )
                    }
                    .testTag("emp_nuke_button"),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Overdrive EMP Bomb",
                        tint = if (overdriveReady) Color.White else TextMuted,
                        modifier = Modifier.size(26.dp)
                    )
                    Text(
                        text = if (overdriveReady) "EMP" else "CHRG",
                        color = if (overdriveReady) Color.White else TextMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Primary Fire Button (If not in auto-fire mode or in buttons scheme)
            if (controlScheme == ControlScheme.BUTTONS || !autoFire) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(NeonCyan, Color(0xFF0088CC), SpaceDark)
                            )
                        )
                        .border(2.dp, NeonCyan, CircleShape)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    onFirePress()
                                    tryAwaitRelease()
                                    onFireRelease()
                                }
                            )
                        }
                        .testTag("arcade_fire_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = "Fire Laser",
                            tint = SpaceBlack,
                            modifier = Modifier.size(30.dp)
                        )
                        Text(
                            text = "FIRE",
                            color = SpaceBlack,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArcadeDirectionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDesc: String,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SpaceCard)
            .border(1.5.dp, NeonCyan.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onPress()
                        tryAwaitRelease()
                        onRelease()
                    }
                )
            }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDesc,
            tint = NeonCyan,
            modifier = Modifier.size(36.dp)
        )
    }
}

@Composable
private fun PauseOverlay(
    uiState: com.example.viewmodel.GameUiState,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onQuit: () -> Unit,
    onToggleSound: () -> Unit,
    onToggleHaptics: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceBlack.copy(alpha = 0.88f))
            .pointerInput(Unit) {},
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SpaceCard),
            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "MISSION PAUSED",
                    color = NeonCyan,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Audio & Haptic Toggles in pause
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(
                        onClick = onToggleSound,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(SpaceDark)
                    ) {
                        Icon(
                            imageVector = if (uiState.soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                            contentDescription = "Sound",
                            tint = if (uiState.soundEnabled) NeonCyan else TextMuted
                        )
                    }

                    IconButton(
                        onClick = onToggleHaptics,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(SpaceDark)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Vibration,
                            contentDescription = "Haptics",
                            tint = if (uiState.hapticsEnabled) NeonCyan else TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onResume,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("resume_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = SpaceBlack)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "RESUME SORTIE", color = SpaceBlack, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onRestart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pause_restart_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SpaceCardBorder)
                ) {
                    Icon(imageVector = Icons.Default.Replay, contentDescription = null, tint = TextPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "RESTART MISSION", color = TextPrimary)
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onQuit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pause_quit_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CruiserRed.copy(alpha = 0.5f))
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = CruiserRed)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "ABORT TO MENU", color = CruiserRed)
                }
            }
        }
    }
}

@Composable
private fun GameOverOverlay(
    uiState: com.example.viewmodel.GameUiState,
    onRestart: () -> Unit,
    onHangar: () -> Unit
) {
    val snap = uiState.snapshot
    val accuracy = if (snap.shotsFired > 0) ((snap.shotsHit.toFloat() / snap.shotsFired) * 100f).toInt() else 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceBlack.copy(alpha = 0.92f))
            .pointerInput(Unit) {},
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SpaceCard),
            border = BorderStroke(1.5.dp, if (uiState.isNewHighScore) NeonYellow else CruiserRed)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (uiState.isNewHighScore) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = NeonYellow.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, NeonYellow),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = NeonYellow, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "NEW ALL-TIME RECORD!",
                                color = NeonYellow,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Text(
                    text = "AX-01 COMPROMISED",
                    color = CruiserRed,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "${snap.score}",
                    color = TextPrimary,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "FINAL SCORE",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Stats Matrix
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = SpaceDark,
                    border = BorderStroke(1.dp, SpaceCardBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        StatRow("SECTOR REACHED", "${snap.level}")
                        StatRow("HOSTILES ELIMINATED", "${snap.kills}")
                        StatRow("ASTEROIDS CRUSHED", "${snap.asteroidsBlasted}")
                        StatRow("FIRING ACCURACY", "$accuracy%")
                        StatRow("SCRAP CREDITS", "+${snap.creditsEarned} CR", NeonAmber)
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                Button(
                    onClick = onRestart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("game_over_retry_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Icon(imageVector = Icons.Default.Replay, contentDescription = null, tint = SpaceBlack)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "DEPLOY NEW CRAFT",
                        color = SpaceBlack,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onHangar,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("game_over_hangar_button"),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, SpaceCardBorder)
                ) {
                    Text(
                        text = "RETURN TO BASE HANGAR",
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String, valueColor: Color = TextPrimary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        Text(text = value, color = valueColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}
