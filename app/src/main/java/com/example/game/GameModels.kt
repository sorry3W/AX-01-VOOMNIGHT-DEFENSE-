package com.example.game

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.CruiserRed
import com.example.ui.theme.DroneViolet
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonYellow
import com.example.ui.theme.ShieldBlue

enum class GameMode(val displayName: String, val description: String) {
    HYPER_NEON("Hyper Neon", "60 FPS 2D particle engine with glowing laser trails & neon graphics"),
    CLASSIC_TERMINAL("Classic C-Engine", "Authentic ASCII phosphor CRT monitor mode reproducing the C source code"),
    SURVIVAL("Endless Survival", "Defend against relentless intensifying waves of hostile fleets"),
    TIME_ATTACK("3-Min Blitz", "Rack up maximum score against the clock with high enemy density")
}

enum class ControlScheme(val displayName: String) {
    DRAG("Touch & Drag"),
    BUTTONS("Arcade D-Pad & Fire"),
    TILT("Gyro / Tilt Steering")
}

enum class ShipSkin(val displayName: String, val primaryColor: Color, val accentColor: Color) {
    CYBER_CYAN("Cyber Cyan", NeonCyan, NeonMagenta),
    VOID_CRIMSON("Void Crimson", CruiserRed, NeonAmber),
    MATRIX_EMERALD("Matrix Emerald", NeonGreen, Color(0xFF00B4D8)),
    SOLAR_AMBER("Solar Blaze", NeonAmber, NeonYellow)
}

enum class EnemyType(
    val charSymbol: Char,
    val baseHealth: Int,
    val scoreValue: Int,
    val width: Float,
    val height: Float
) {
    DRONE('v', 1, 15, 48f, 48f),
    CRUISER('W', 4, 45, 76f, 68f),
    ASTEROID('o', 2, 20, 60f, 60f),
    ASTEROID_MINI('o', 1, 10, 32f, 32f),
    BOSS_TITAN('M', 35, 300, 160f, 110f)
}

enum class PowerUpType(val displayName: String, val color: Color, val symbol: String) {
    SHIELD_BOOST("Shield Restore", ShieldBlue, "🛡️"),
    SPREAD_LASER("Triple Laser", NeonCyan, "⚡"),
    OVERDRIVE_NUKE("EMP Bomb", NeonMagenta, "💣"),
    RAPID_FIRE("Rapid Fire", NeonYellow, "🔥"),
    SCRAP_CREDITS("Credits +50", NeonAmber, "💎"),
    REPAIR_NANITE("Hull Repair", NeonGreen, "🔧")
}

data class PlayerShip(
    var x: Float = 500f,
    var y: Float = 1400f,
    val width: Float = 64f,
    val height: Float = 64f,
    var health: Int = 100,
    var maxHealth: Int = 100,
    var shield: Int = 50,
    var maxShield: Int = 50,
    var speed: Float = 650f,
    var overdriveGauge: Float = 0f, // 0..100
    var isOverdriveActive: Boolean = false,
    var overdriveTimer: Float = 0f,
    var spreadLaserTimer: Float = 0f,
    var rapidFireTimer: Float = 0f,
    var invulnerableTimer: Float = 0f,
    var shieldRegenTimer: Float = 0f,
    var tiltAngle: Float = 0f
)

data class Enemy(
    val id: Long,
    var x: Float,
    var y: Float,
    var vx: Float = 0f,
    var vy: Float = 180f,
    val type: EnemyType,
    var health: Int,
    val maxHealth: Int,
    var shootCooldown: Float = 2.0f,
    var animTimer: Float = 0f,
    var rotation: Float = 0f
)

data class Laser(
    val id: Long,
    var x: Float,
    var y: Float,
    var vx: Float = 0f,
    var vy: Float = -1100f,
    val isFromPlayer: Boolean = true,
    val damage: Int = 1,
    val color: Color = NeonCyan,
    val radius: Float = 6f
)

data class PowerUp(
    val id: Long,
    var x: Float,
    var y: Float,
    val type: PowerUpType,
    var vy: Float = 160f,
    var animTimer: Float = 0f
)

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var color: Color,
    var size: Float,
    var life: Float, // 0..1
    var maxLife: Float
)

data class FloatingText(
    val id: Long,
    var x: Float,
    var y: Float,
    val text: String,
    val color: Color,
    var life: Float = 1.0f
)

data class Star(
    var x: Float,
    var y: Float,
    val speed: Float,
    val size: Float,
    val brightness: Float
)

data class GameStateSnapshot(
    val score: Int = 0,
    val level: Int = 1,
    val health: Int = 100,
    val maxHealth: Int = 100,
    val shield: Int = 50,
    val maxShield: Int = 50,
    val overdriveGauge: Float = 0f,
    val isGameOver: Boolean = false,
    val isPaused: Boolean = false,
    val comboMultiplier: Int = 1,
    val timeRemainingSec: Int = 180,
    val kills: Int = 0,
    val asteroidsBlasted: Int = 0,
    val shotsFired: Int = 0,
    val shotsHit: Int = 0,
    val creditsEarned: Int = 0,
    val activePowerups: List<String> = emptyList(),
    val bossActive: Boolean = false,
    val bossHealthPercent: Float = 0f,
    val screenShakeIntensity: Float = 0f,
    val terminalGrid: List<String> = emptyList() // For C-Engine mode
)
