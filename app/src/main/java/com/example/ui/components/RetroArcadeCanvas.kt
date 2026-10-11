package com.example.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.example.game.Enemy
import com.example.game.EnemyType
import com.example.game.GameEngine
import com.example.game.GameMode
import com.example.game.Laser
import com.example.game.PlayerShip
import com.example.game.PowerUp
import com.example.ui.theme.CruiserRed
import com.example.ui.theme.DroneViolet
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonYellow
import com.example.ui.theme.ShieldBlue
import com.example.ui.theme.SpaceBlack
import com.example.ui.theme.SpaceDark
import com.example.ui.theme.TerminalDark
import com.example.ui.theme.TerminalGreen
import com.example.ui.theme.TerminalScanline
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun RetroArcadeCanvas(
    gameEngine: GameEngine,
    crtScanlinesEnabled: Boolean = false,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        val scaleX = canvasWidth / gameEngine.worldWidth
        val scaleY = canvasHeight / gameEngine.worldHeight

        // Apply screen shake offset
        val shakeOffset = if (gameEngine.screenShake > 0f) {
            Offset(
                (Random.nextFloat() - 0.5f) * gameEngine.screenShake * 2f,
                (Random.nextFloat() - 0.5f) * gameEngine.screenShake * 2f
            )
        } else {
            Offset.Zero
        }

        if (gameEngine.gameMode == GameMode.CLASSIC_TERMINAL) {
            drawClassicTerminalMode(gameEngine, canvasWidth, canvasHeight)
        } else {
            drawHyperNeonMode(gameEngine, scaleX, scaleY, shakeOffset, canvasWidth, canvasHeight)
        }

        // Draw Scanlines overlay if enabled
        if (crtScanlinesEnabled || gameEngine.gameMode == GameMode.CLASSIC_TERMINAL) {
            drawScanlines(canvasWidth, canvasHeight, if (gameEngine.gameMode == GameMode.CLASSIC_TERMINAL) 0.18f else 0.08f)
        }
    }
}

private fun DrawScope.drawHyperNeonMode(
    engine: GameEngine,
    scaleX: Float,
    scaleY: Float,
    shake: Offset,
    canvasW: Float,
    canvasH: Float
) {
    // 1. Deep Space Gradient Background
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(SpaceBlack, SpaceDark, SpaceBlack)
        ),
        size = Size(canvasW, canvasH)
    )

    // 2. Parallax Starfield
    engine.stars.forEach { star ->
        val sx = star.x * scaleX + shake.x
        val sy = star.y * scaleY + shake.y
        drawCircle(
            color = Color.White.copy(alpha = star.brightness),
            radius = star.size * scaleX,
            center = Offset(sx, sy)
        )
    }

    // 3. Particles (Thrust flame, explosions, sparks)
    engine.particles.forEach { p ->
        val px = p.x * scaleX + shake.x
        val py = p.y * scaleY + shake.y
        drawCircle(
            color = p.color.copy(alpha = (p.life).coerceIn(0f, 1f)),
            radius = p.size * scaleX * p.life,
            center = Offset(px, py)
        )
    }

    // 4. PowerUps
    engine.powerUps.forEach { p ->
        drawPowerUp(p, scaleX, scaleY, shake)
    }

    // 5. Lasers
    engine.lasers.forEach { laser ->
        drawLaser(laser, scaleX, scaleY, shake)
    }

    // 6. Enemies
    engine.enemies.forEach { enemy ->
        drawEnemy(enemy, scaleX, scaleY, shake)
    }

    // 7. Player (AX-01 Voominight Craft)
    if (!engine.isGameOver || (engine.player.health > 0)) {
        drawPlayerShip(engine.player, engine.skin.primaryColor, engine.skin.accentColor, scaleX, scaleY, shake)
    }

    // 8. Floating text popups
    val nativeCanvas = drawContext.canvas.nativeCanvas
    val textPaint = Paint().apply {
        isAntiAlias = true
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        textSize = 28f * scaleX
        textAlign = Paint.Align.CENTER
    }

    engine.floatingTexts.forEach { ft ->
        textPaint.color = ft.color.copy(alpha = ft.life.coerceIn(0f, 1f)).toArgb()
        nativeCanvas.drawText(
            ft.text,
            ft.x * scaleX + shake.x,
            ft.y * scaleY + shake.y,
            textPaint
        )
    }
}

private fun DrawScope.drawPlayerShip(
    player: PlayerShip,
    primaryColor: Color,
    accentColor: Color,
    scaleX: Float,
    scaleY: Float,
    shake: Offset
) {
    // Invulnerability flicker
    if (player.invulnerableTimer > 0f && (player.invulnerableTimer * 20).toInt() % 2 == 0) {
        return
    }

    val px = player.x * scaleX + shake.x
    val py = player.y * scaleY + shake.y
    val pw = player.width * scaleX
    val ph = player.height * scaleY

    rotate(degrees = player.tiltAngle, pivot = Offset(px, py)) {
        // Shield Bubble
        if (player.shield > 0) {
            val shieldAlpha = (0.25f + (player.shield.toFloat() / player.maxShield) * 0.4f).coerceIn(0.2f, 0.7f)
            drawCircle(
                color = ShieldBlue.copy(alpha = shieldAlpha * 0.35f),
                radius = pw * 0.85f,
                center = Offset(px, py),
                style = Fill
            )
            drawCircle(
                color = ShieldBlue.copy(alpha = shieldAlpha),
                radius = pw * 0.85f,
                center = Offset(px, py),
                style = Stroke(width = 3.5f * scaleX)
            )
        }

        // Overdrive Golden Aura
        if (player.isOverdriveActive) {
            drawCircle(
                color = NeonMagenta.copy(alpha = 0.5f),
                radius = pw * 0.95f,
                center = Offset(px, py),
                style = Stroke(width = 4f * scaleX)
            )
        }

        // AX-01 Starfighter Geometry
        val shipPath = Path().apply {
            moveTo(px, py - ph * 0.55f) // Nose tip
            lineTo(px + pw * 0.45f, py + ph * 0.45f) // Right main wing
            lineTo(px + pw * 0.2f, py + ph * 0.35f) // Right thruster notch
            lineTo(px, py + ph * 0.25f) // Center engine keel
            lineTo(px - pw * 0.2f, py + ph * 0.35f) // Left thruster notch
            lineTo(px - pw * 0.45f, py + ph * 0.45f) // Left main wing
            close()
        }

        // Fill ship body
        drawPath(
            path = shipPath,
            brush = Brush.verticalGradient(
                colors = listOf(primaryColor, primaryColor.copy(alpha = 0.8f), SpaceDark)
            )
        )
        // Outer glow stroke
        drawPath(
            path = shipPath,
            color = primaryColor,
            style = Stroke(width = 2.5f * scaleX, cap = StrokeCap.Round)
        )

        // Wing Canards & Cannons
        drawLine(
            color = accentColor,
            start = Offset(px - pw * 0.35f, py - ph * 0.05f),
            end = Offset(px - pw * 0.35f, py + ph * 0.35f),
            strokeWidth = 3f * scaleX
        )
        drawLine(
            color = accentColor,
            start = Offset(px + pw * 0.35f, py - ph * 0.05f),
            end = Offset(px + pw * 0.35f, py + ph * 0.35f),
            strokeWidth = 3f * scaleX
        )

        // Cockpit canopy glow
        drawOval(
            color = Color.White,
            topLeft = Offset(px - pw * 0.08f, py - ph * 0.28f),
            size = Size(pw * 0.16f, ph * 0.28f)
        )
    }
}

private fun DrawScope.drawEnemy(
    enemy: Enemy,
    scaleX: Float,
    scaleY: Float,
    shake: Offset
) {
    val ex = enemy.x * scaleX + shake.x
    val ey = enemy.y * scaleY + shake.y
    val ew = enemy.type.width * scaleX
    val eh = enemy.type.height * scaleY

    when (enemy.type) {
        EnemyType.DRONE -> {
            // Swift 'v' shaped aggressive hunter
            rotate(degrees = enemy.rotation, pivot = Offset(ex, ey)) {
                val dronePath = Path().apply {
                    moveTo(ex, ey + eh * 0.5f) // Bottom point
                    lineTo(ex + ew * 0.5f, ey - eh * 0.4f) // Top right wing
                    lineTo(ex, ey - eh * 0.15f) // Top center inlet
                    lineTo(ex - ew * 0.5f, ey - eh * 0.4f) // Top left wing
                    close()
                }
                drawPath(dronePath, DroneViolet)
                drawPath(dronePath, NeonMagenta, style = Stroke(width = 2f * scaleX))
                // Sensor eye
                drawCircle(color = NeonCyan, radius = 3.5f * scaleX, center = Offset(ex, ey))
            }
        }
        EnemyType.CRUISER -> {
            // Heavy 'W' shaped armored dreadnought
            val cruiserPath = Path().apply {
                moveTo(ex, ey + eh * 0.45f) // Front prow
                lineTo(ex + ew * 0.25f, ey + eh * 0.2f)
                lineTo(ex + ew * 0.5f, ey + eh * 0.35f) // Right flank
                lineTo(ex + ew * 0.4f, ey - eh * 0.45f) // Right engine
                lineTo(ex, ey - eh * 0.25f) // Core bay
                lineTo(ex - ew * 0.4f, ey - eh * 0.45f) // Left engine
                lineTo(ex - ew * 0.5f, ey + eh * 0.35f) // Left flank
                lineTo(ex - ew * 0.25f, ey + eh * 0.2f)
                close()
            }
            drawPath(cruiserPath, CruiserRed)
            drawPath(cruiserPath, NeonAmber, style = Stroke(width = 3f * scaleX))
            // Core reactor pulse
            val pulse = (sin(enemy.animTimer * 6.0) * 0.3f + 0.7f).toFloat()
            drawCircle(color = NeonYellow.copy(alpha = pulse), radius = 6f * scaleX, center = Offset(ex, ey))
        }
        EnemyType.ASTEROID, EnemyType.ASTEROID_MINI -> {
            // Faceted tumbling asteroid
            rotate(degrees = enemy.rotation, pivot = Offset(ex, ey)) {
                val radius = ew * 0.5f
                val rockPath = Path()
                val points = 7
                for (i in 0 until points) {
                    val angle = (i.toFloat() / points) * PI * 2f
                    val rMod = if (i % 2 == 0) radius * 0.85f else radius * 1.05f
                    val rx = ex + (cos(angle) * rMod).toFloat()
                    val ry = ey + (sin(angle) * rMod).toFloat()
                    if (i == 0) rockPath.moveTo(rx, ry) else rockPath.lineTo(rx, ry)
                }
                rockPath.close()
                drawPath(rockPath, Color(0xFF6C584C))
                drawPath(rockPath, NeonAmber.copy(alpha = 0.8f), style = Stroke(width = 2.5f * scaleX))
                // Crater
                drawCircle(color = Color(0xFF4A3B32), radius = radius * 0.25f, center = Offset(ex - radius * 0.2f, ey - radius * 0.2f))
            }
        }
        EnemyType.BOSS_TITAN -> {
            // Massive Titan Mothership
            val bossPath = Path().apply {
                moveTo(ex, ey + eh * 0.55f) // Heavy command ram
                lineTo(ex + ew * 0.35f, ey + eh * 0.35f)
                lineTo(ex + ew * 0.5f, ey - eh * 0.2f)
                lineTo(ex + ew * 0.35f, ey - eh * 0.5f)
                lineTo(ex, ey - eh * 0.3f)
                lineTo(ex - ew * 0.35f, ey - eh * 0.5f)
                lineTo(ex - ew * 0.5f, ey - eh * 0.2f)
                lineTo(ex - ew * 0.35f, ey + eh * 0.35f)
                close()
            }
            drawPath(bossPath, Brush.verticalGradient(listOf(CruiserRed, SpaceDark, Color(0xFF4A0E17))))
            drawPath(bossPath, NeonMagenta, style = Stroke(width = 4.5f * scaleX))

            // Boss Health Bar directly above
            val barW = ew * 0.8f
            val barH = 8f * scaleY
            val barX = ex - barW / 2f
            val barY = ey - eh * 0.65f
            drawRoundRect(color = Color.Black, topLeft = Offset(barX, barY), size = Size(barW, barH), cornerRadius = CornerRadius(4f))
            drawRoundRect(
                color = CruiserRed,
                topLeft = Offset(barX, barY),
                size = Size(barW * (enemy.health.toFloat() / enemy.maxHealth).coerceIn(0f, 1f), barH),
                cornerRadius = CornerRadius(4f)
            )
        }
    }
}

private fun DrawScope.drawLaser(
    laser: Laser,
    scaleX: Float,
    scaleY: Float,
    shake: Offset
) {
    val lx = laser.x * scaleX + shake.x
    val ly = laser.y * scaleY + shake.y
    val r = laser.radius * scaleX

    if (laser.isFromPlayer) {
        // Glowing vertical beam
        drawLine(
            color = laser.color.copy(alpha = 0.4f),
            start = Offset(lx, ly + 22f * scaleY),
            end = Offset(lx, ly - 10f * scaleY),
            strokeWidth = r * 2.6f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color.White,
            start = Offset(lx, ly + 18f * scaleY),
            end = Offset(lx, ly - 8f * scaleY),
            strokeWidth = r * 1.1f,
            cap = StrokeCap.Round
        )
    } else {
        // Hostile plasma orb
        drawCircle(
            color = laser.color.copy(alpha = 0.4f),
            radius = r * 1.8f,
            center = Offset(lx, ly)
        )
        drawCircle(
            color = Color.White,
            radius = r * 0.8f,
            center = Offset(lx, ly)
        )
    }
}

private fun DrawScope.drawPowerUp(
    p: PowerUp,
    scaleX: Float,
    scaleY: Float,
    shake: Offset
) {
    val px = p.x * scaleX + shake.x
    val py = p.y * scaleY + shake.y
    val r = 20f * scaleX

    val ringScale = (sin(p.animTimer * 5.0) * 0.2f + 1.0f).toFloat()

    // Outer pulsing ring
    drawCircle(
        color = p.type.color.copy(alpha = 0.6f),
        radius = r * ringScale,
        center = Offset(px, py),
        style = Stroke(width = 2.5f * scaleX)
    )

    // Inner glowing core
    drawCircle(
        color = p.type.color.copy(alpha = 0.85f),
        radius = r * 0.7f,
        center = Offset(px, py),
        style = Fill
    )

    // Symbol
    val nativeCanvas = drawContext.canvas.nativeCanvas
    val paint = Paint().apply {
        textSize = 18f * scaleX
        textAlign = Paint.Align.CENTER
    }
    nativeCanvas.drawText(p.type.symbol, px, py + 6f * scaleY, paint)
}

/**
 * Renders the authentic Green Phosphor CRT Terminal Mode from original C code.
 */
private fun DrawScope.drawClassicTerminalMode(
    engine: GameEngine,
    canvasW: Float,
    canvasH: Float
) {
    // 1. Dark CRT phosphor background
    drawRect(color = TerminalDark, size = Size(canvasW, canvasH))

    val nativeCanvas = drawContext.canvas.nativeCanvas

    val titlePaint = Paint().apply {
        isAntiAlias = true
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        textSize = (canvasW / 28f).coerceIn(18f, 32f)
        color = TerminalGreen.toArgb()
        textAlign = Paint.Align.CENTER
    }

    val statsPaint = Paint().apply {
        isAntiAlias = true
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        textSize = (canvasW / 36f).coerceIn(14f, 22f)
        color = TerminalGreen.toArgb()
        textAlign = Paint.Align.CENTER
    }

    val gridPaint = Paint().apply {
        isAntiAlias = true
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        textSize = (canvasW / 32f).coerceIn(14f, 26f)
        color = TerminalGreen.toArgb()
        textAlign = Paint.Align.LEFT
    }

    val headerY = canvasH * 0.08f
    nativeCanvas.drawText("============================================================", canvasW / 2f, headerY, statsPaint)
    nativeCanvas.drawText("[ AX-01 VOOMINIGHT DEFENSE SYSTEM v1.0 ] - C ENGINE", canvasW / 2f, headerY + 32f, titlePaint)

    val statsLine = String.format(
        "SCORE: %-5d | HP: %-3d%% | SHIELD: %-3d%% | LVL: %-2d",
        engine.score,
        engine.player.health,
        engine.player.shield,
        engine.level
    )
    nativeCanvas.drawText(statsLine, canvasW / 2f, headerY + 68f, statsPaint)
    nativeCanvas.drawText("============================================================", canvasW / 2f, headerY + 96f, statsPaint)

    // Draw ASCII character grid lines
    val grid = engine.generateTerminalAsciiGrid()
    val startGridY = headerY + 130f
    val lineSpacing = (canvasH * 0.58f) / 15f
    val startX = (canvasW - (canvasW * 0.94f)) / 2f

    grid.forEachIndexed { index, line ->
        val y = startGridY + index * lineSpacing
        nativeCanvas.drawText("|$line|", startX, y, gridPaint)
    }

    val bottomY = startGridY + 15 * lineSpacing + 20f
    nativeCanvas.drawText("============================================================", canvasW / 2f, bottomY, statsPaint)
    nativeCanvas.drawText("CONTROLS: [TOUCH/DRAG] Steer | [W/TAP] Fire | [Q] Abort", canvasW / 2f, bottomY + 36f, statsPaint)
}

private fun DrawScope.drawScanlines(canvasW: Float, canvasH: Float, alpha: Float) {
    var y = 0f
    val spacing = 5f
    while (y < canvasH) {
        drawLine(
            color = Color.Black.copy(alpha = alpha),
            start = Offset(0f, y),
            end = Offset(canvasW, y),
            strokeWidth = 1.5f
        )
        y += spacing
    }
}
