package com.example.game

import androidx.compose.ui.graphics.Color
import com.example.audio.ArcadeHaptics
import com.example.audio.ArcadeSoundEngine
import com.example.data.HangarUpgradeEntity
import com.example.ui.theme.CruiserRed
import com.example.ui.theme.DroneViolet
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonYellow
import com.example.ui.theme.ShieldBlue
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class GameEngine(
    val gameMode: GameMode,
    private val soundEngine: ArcadeSoundEngine,
    private val haptics: ArcadeHaptics,
    upgrades: List<HangarUpgradeEntity>,
    val skin: ShipSkin = ShipSkin.CYBER_CYAN
) {
    val worldWidth = 1000f
    val worldHeight = 1600f

    // Upgrades modifiers
    private val laserLevel = upgrades.find { it.upgradeKey == "laser_power" }?.currentLevel ?: 1
    private val shieldLevel = upgrades.find { it.upgradeKey == "shield_matrix" }?.currentLevel ?: 1
    private val hullLevel = upgrades.find { it.upgradeKey == "hull_plating" }?.currentLevel ?: 1
    private val speedLevel = upgrades.find { it.upgradeKey == "thruster_speed" }?.currentLevel ?: 1
    private val magnetLevel = upgrades.find { it.upgradeKey == "tractor_magnet" }?.currentLevel ?: 1

    // Game Entities
    val player = PlayerShip(
        x = worldWidth / 2f,
        y = worldHeight - 180f,
        maxHealth = 100 + (hullLevel - 1) * 25,
        health = 100 + (hullLevel - 1) * 25,
        maxShield = 50 + (shieldLevel - 1) * 20,
        shield = 50 + (shieldLevel - 1) * 20,
        speed = 650f + (speedLevel - 1) * 80f
    )

    val enemies = mutableListOf<Enemy>()
    val lasers = mutableListOf<Laser>()
    val powerUps = mutableListOf<PowerUp>()
    val particles = mutableListOf<Particle>()
    val floatingTexts = mutableListOf<FloatingText>()
    val stars = mutableListOf<Star>()

    // Game Progression & Metrics
    var score: Int = 0
        private set
    var level: Int = 1
        private set
    var isGameOver: Boolean = false
        private set
    var isPaused: Boolean = false
    var timeRemainingSec: Float = 180f // For time attack mode
    var kills: Int = 0
        private set
    var asteroidsBlasted: Int = 0
        private set
    var shotsFired: Int = 0
        private set
    var shotsHit: Int = 0
        private set
    var creditsEarned: Int = 0
        private set
    var comboCount: Int = 0
        private set
    var comboTimer: Float = 0f
        private set

    // Spawning & timers
    private var enemySpawnTimer: Float = 0f
    private var fireCooldown: Float = 0f
    private var nextEntityId: Long = 1L
    var screenShake: Float = 0f

    // Input state
    var inputDirectionX: Float = 0f // -1f..1f
    var targetTouchX: Float? = null
    var isFiringRequested: Boolean = false

    init {
        // Initialize background starfield
        for (i in 0 until 90) {
            stars.add(
                Star(
                    x = Random.nextFloat() * worldWidth,
                    y = Random.nextFloat() * worldHeight,
                    speed = Random.nextFloat() * 180f + 60f,
                    size = Random.nextFloat() * 2.5f + 1.2f,
                    brightness = Random.nextFloat() * 0.6f + 0.4f
                )
            )
        }
    }

    fun update(deltaTime: Float) {
        if (isGameOver || isPaused) return

        val dt = deltaTime.coerceIn(0.001f, 0.05f)

        // Screen shake decay
        if (screenShake > 0f) {
            screenShake = (screenShake - dt * 25f).coerceAtLeast(0f)
        }

        // Time attack countdown
        if (gameMode == GameMode.TIME_ATTACK) {
            timeRemainingSec -= dt
            if (timeRemainingSec <= 0f) {
                timeRemainingSec = 0f
                isGameOver = true
                soundEngine.play(ArcadeSoundEngine.SoundEffect.GAME_OVER)
            }
        }

        // Combo timer decay
        if (comboTimer > 0f) {
            comboTimer -= dt
            if (comboTimer <= 0f) {
                comboCount = 0
            }
        }

        updateStars(dt)
        updatePlayer(dt)
        updateLasers(dt)
        updateEnemies(dt)
        updatePowerUps(dt)
        updateParticles(dt)
        updateFloatingTexts(dt)
        handleSpawning(dt)
        checkCollisions()
    }

    private fun updateStars(dt: Float) {
        val speedMult = if (player.isOverdriveActive) 3.5f else 1.0f
        stars.forEach { star ->
            star.y += star.speed * speedMult * dt
            if (star.y > worldHeight) {
                star.y = 0f
                star.x = Random.nextFloat() * worldWidth
            }
        }
    }

    private fun updatePlayer(dt: Float) {
        // Lateral movement
        val targetX = targetTouchX
        if (targetX != null) {
            val diff = targetX - player.x
            val step = player.speed * 1.5f * dt
            if (kotlin.math.abs(diff) <= step) {
                player.x = targetX
            } else {
                player.x += kotlin.math.sign(diff) * step
            }
            player.tiltAngle = (diff / 80f).coerceIn(-15f, 15f)
        } else if (inputDirectionX != 0f) {
            player.x += inputDirectionX * player.speed * dt
            player.tiltAngle = (inputDirectionX * 18f)
        } else {
            player.tiltAngle *= 0.85f
        }

        // Clamp inside boundary
        val halfW = player.width / 2f
        player.x = player.x.coerceIn(halfW + 15f, worldWidth - halfW - 15f)

        // Timers
        if (player.invulnerableTimer > 0f) player.invulnerableTimer -= dt
        if (player.spreadLaserTimer > 0f) player.spreadLaserTimer -= dt
        if (player.rapidFireTimer > 0f) player.rapidFireTimer -= dt

        // Overdrive state
        if (player.isOverdriveActive) {
            player.overdriveTimer -= dt
            if (player.overdriveTimer <= 0f) {
                player.isOverdriveActive = false
            }
        }

        // Shield regeneration logic: after 3.5s of no damage taken, restore 12 shield/sec
        player.shieldRegenTimer += dt
        val regenCooldown = (4.0f - (shieldLevel - 1) * 0.4f).coerceAtLeast(1.8f)
        if (player.shieldRegenTimer >= regenCooldown && player.shield < player.maxShield) {
            player.shield = (player.shield + (15f * dt).toInt()).coerceAtMost(player.maxShield)
        }

        // Thruster flame particles
        if (Random.nextFloat() < 0.7f) {
            val exhaustColor = if (player.isOverdriveActive) NeonMagenta else skin.primaryColor
            particles.add(
                Particle(
                    x = player.x + Random.nextFloat() * 12f - 6f,
                    y = player.y + player.height / 2f + 4f,
                    vx = Random.nextFloat() * 30f - 15f,
                    vy = Random.nextFloat() * 200f + 300f,
                    color = exhaustColor,
                    size = Random.nextFloat() * 4f + 3f,
                    life = 1f,
                    maxLife = Random.nextFloat() * 0.25f + 0.15f
                )
            )
        }

        // Fire cooldown and shooting
        fireCooldown -= dt
        val baseFireRate = if (player.rapidFireTimer > 0f || player.isOverdriveActive) {
            0.08f
        } else {
            (0.24f - (laserLevel - 1) * 0.025f).coerceAtLeast(0.11f)
        }

        if (isFiringRequested && fireCooldown <= 0f) {
            shootPlayerLaser()
            fireCooldown = baseFireRate
        }
    }

    fun shootPlayerLaser() {
        shotsFired++
        val isSpread = player.spreadLaserTimer > 0f || laserLevel >= 3 || player.isOverdriveActive
        val isQuad = laserLevel >= 5 || (player.spreadLaserTimer > 0f && laserLevel >= 3)
        val laserDmg = if (player.isOverdriveActive) 3 else if (laserLevel >= 4) 2 else 1
        val laserColor = if (player.isOverdriveActive) NeonMagenta else skin.primaryColor

        if (isQuad) {
            // 4x spread
            lasers.add(Laser(nextEntityId++, player.x - 22f, player.y - 20f, -80f, -1250f, true, laserDmg, laserColor))
            lasers.add(Laser(nextEntityId++, player.x - 8f, player.y - 28f, 0f, -1350f, true, laserDmg, laserColor))
            lasers.add(Laser(nextEntityId++, player.x + 8f, player.y - 28f, 0f, -1350f, true, laserDmg, laserColor))
            lasers.add(Laser(nextEntityId++, player.x + 22f, player.y - 20f, 80f, -1250f, true, laserDmg, laserColor))
        } else if (isSpread) {
            // 3x spread
            lasers.add(Laser(nextEntityId++, player.x - 18f, player.y - 20f, -140f, -1200f, true, laserDmg, laserColor))
            lasers.add(Laser(nextEntityId++, player.x, player.y - 26f, 0f, -1350f, true, laserDmg, laserColor))
            lasers.add(Laser(nextEntityId++, player.x + 18f, player.y - 20f, 140f, -1200f, true, laserDmg, laserColor))
        } else if (laserLevel >= 2) {
            // Dual laser
            lasers.add(Laser(nextEntityId++, player.x - 14f, player.y - 22f, 0f, -1300f, true, laserDmg, laserColor))
            lasers.add(Laser(nextEntityId++, player.x + 14f, player.y - 22f, 0f, -1300f, true, laserDmg, laserColor))
        } else {
            // Single central laser (Classic C-style)
            lasers.add(Laser(nextEntityId++, player.x, player.y - 26f, 0f, -1300f, true, laserDmg, laserColor))
        }

        soundEngine.play(ArcadeSoundEngine.SoundEffect.LASER)
        haptics.shootLaser()
    }

    fun triggerOverdriveEMP() {
        if (player.overdriveGauge < 100f && !player.isOverdriveActive) return

        player.isOverdriveActive = true
        player.overdriveGauge = 0f
        player.overdriveTimer = 7.0f // 7 seconds of invincible overdrive
        player.shield = player.maxShield
        screenShake = 18f

        soundEngine.play(ArcadeSoundEngine.SoundEffect.OVERDRIVE)
        haptics.impactHeavy()

        addFloatingText(player.x, player.y - 80f, "OVERDRIVE EMP NOVA!", NeonMagenta)

        // Blast all enemies on screen
        enemies.forEach { enemy ->
            enemy.health -= 8
            createExplosion(enemy.x, enemy.y, enemy.type.color(), 20)
        }

        // Clear all hostile lasers
        lasers.removeAll { !it.isFromPlayer }

        // Blast wave visual particles
        for (i in 0 until 50) {
            val angle = (i / 50f) * PI * 2f
            val spd = Random.nextFloat() * 350f + 250f
            particles.add(
                Particle(
                    x = player.x,
                    y = player.y,
                    vx = (cos(angle) * spd).toFloat(),
                    vy = (sin(angle) * spd).toFloat(),
                    color = NeonMagenta,
                    size = Random.nextFloat() * 6f + 3f,
                    life = 1f,
                    maxLife = 0.5f
                )
            )
        }
    }

    private fun updateLasers(dt: Float) {
        val iterator = lasers.iterator()
        while (iterator.hasNext()) {
            val laser = iterator.next()
            laser.x += laser.vx * dt
            laser.y += laser.vy * dt

            // Remove if offscreen
            if (laser.y < -40f || laser.y > worldHeight + 40f || laser.x < -30f || laser.x > worldWidth + 30f) {
                iterator.remove()
            }
        }
    }

    private fun updateEnemies(dt: Float) {
        val iterator = enemies.iterator()
        while (iterator.hasNext()) {
            val enemy = iterator.next()
            enemy.animTimer += dt

            // Movement patterns
            when (enemy.type) {
                EnemyType.DRONE -> {
                    // Fast dive with sinusoidal sway
                    enemy.x += sin(enemy.animTimer * 5f) * 110f * dt
                    enemy.y += enemy.vy * dt
                    enemy.rotation = sin(enemy.animTimer * 5f) * 15f
                }
                EnemyType.CRUISER -> {
                    // Heavy slow advance
                    enemy.y += enemy.vy * dt
                    // Cruiser retaliatory fire
                    enemy.shootCooldown -= dt
                    if (enemy.shootCooldown <= 0f && enemy.y < worldHeight - 300f) {
                        enemy.shootCooldown = Random.nextFloat() * 1.5f + 1.8f
                        lasers.add(
                            Laser(
                                id = nextEntityId++,
                                x = enemy.x,
                                y = enemy.y + enemy.type.height / 2f + 5f,
                                vx = 0f,
                                vy = 420f,
                                isFromPlayer = false,
                                damage = 1,
                                color = CruiserRed,
                                radius = 7f
                            )
                        )
                        soundEngine.play(ArcadeSoundEngine.SoundEffect.ENEMY_LASER)
                    }
                }
                EnemyType.ASTEROID, EnemyType.ASTEROID_MINI -> {
                    // Tumbling rock
                    enemy.x += enemy.vx * dt
                    enemy.y += enemy.vy * dt
                    enemy.rotation += 45f * dt
                }
                EnemyType.BOSS_TITAN -> {
                    // Side-to-side sweeping mothership
                    enemy.x += enemy.vx * dt
                    if (enemy.x <= 120f || enemy.x >= worldWidth - 120f) {
                        enemy.vx = -enemy.vx
                    }
                    if (enemy.y < 220f) {
                        enemy.y += enemy.vy * dt
                    }
                    // Boss attack barrage
                    enemy.shootCooldown -= dt
                    if (enemy.shootCooldown <= 0f) {
                        enemy.shootCooldown = 1.1f
                        // 3-way spread attack
                        lasers.add(Laser(nextEntityId++, enemy.x - 40f, enemy.y + 40f, -120f, 450f, false, 1, NeonMagenta, 8f))
                        lasers.add(Laser(nextEntityId++, enemy.x, enemy.y + 50f, 0f, 500f, false, 1, NeonMagenta, 8f))
                        lasers.add(Laser(nextEntityId++, enemy.x + 40f, enemy.y + 40f, 120f, 450f, false, 1, NeonMagenta, 8f))
                        soundEngine.play(ArcadeSoundEngine.SoundEffect.ENEMY_LASER)
                    }
                }
            }

            // Enemy reaches bottom barrier (Per original C code logic: damage player hull)
            if (enemy.y >= worldHeight - 40f && enemy.type != EnemyType.BOSS_TITAN) {
                iterator.remove()
                damagePlayer(12)
                createExplosion(enemy.x, worldHeight - 40f, CruiserRed, 12)
                soundEngine.play(ArcadeSoundEngine.SoundEffect.EXPLOSION_SMALL)
                haptics.impactSmall()
                addFloatingText(enemy.x, worldHeight - 60f, "BREACH! -12 HP", CruiserRed)
            }
        }
    }

    private fun updatePowerUps(dt: Float) {
        val magnetRange = 100f + (magnetLevel - 1) * 70f
        val iterator = powerUps.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.animTimer += dt

            // Gravitational pull toward player
            val dx = player.x - p.x
            val dy = player.y - p.y
            val dist = kotlin.math.sqrt(dx * dx + dy * dy)
            if (dist < magnetRange) {
                val pullSpeed = 450f * dt
                p.x += (dx / dist) * pullSpeed
                p.y += (dy / dist) * pullSpeed
            } else {
                p.y += p.vy * dt
            }

            // Check collect by player
            if (dist < player.width / 2f + 25f) {
                iterator.remove()
                applyPowerUp(p.type)
            } else if (p.y > worldHeight + 40f) {
                iterator.remove()
            }
        }
    }

    private fun applyPowerUp(type: PowerUpType) {
        soundEngine.play(ArcadeSoundEngine.SoundEffect.POWERUP)
        haptics.powerupCollected()
        when (type) {
            PowerUpType.SHIELD_BOOST -> {
                player.shield = player.maxShield
                addFloatingText(player.x, player.y - 40f, "SHIELD FULL!", ShieldBlue)
            }
            PowerUpType.SPREAD_LASER -> {
                player.spreadLaserTimer = 12f
                addFloatingText(player.x, player.y - 40f, "TRIPLE BLASTER!", NeonCyan)
            }
            PowerUpType.RAPID_FIRE -> {
                player.rapidFireTimer = 10f
                addFloatingText(player.x, player.y - 40f, "RAPID FIRE!", NeonYellow)
            }
            PowerUpType.OVERDRIVE_NUKE -> {
                player.overdriveGauge = 100f
                triggerOverdriveEMP()
            }
            PowerUpType.SCRAP_CREDITS -> {
                creditsEarned += 50
                score += 150
                addFloatingText(player.x, player.y - 40f, "+50 CREDITS!", NeonAmber)
            }
            PowerUpType.REPAIR_NANITE -> {
                player.health = (player.health + 35).coerceAtMost(player.maxHealth)
                addFloatingText(player.x, player.y - 40f, "HULL REPAIRED +35", NeonGreen)
            }
        }
    }

    private fun updateParticles(dt: Float) {
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.life -= dt / p.maxLife
            if (p.life <= 0f) {
                iterator.remove()
            }
        }
    }

    private fun updateFloatingTexts(dt: Float) {
        val iterator = floatingTexts.iterator()
        while (iterator.hasNext()) {
            val text = iterator.next()
            text.y -= 70f * dt
            text.life -= dt * 1.2f
            if (text.life <= 0f) {
                iterator.remove()
            }
        }
    }

    private fun handleSpawning(dt: Float) {
        enemySpawnTimer -= dt
        val baseSpawnInterval = (1.8f - (level - 1) * 0.12f).coerceAtLeast(0.45f)

        if (enemySpawnTimer <= 0f) {
            enemySpawnTimer = baseSpawnInterval

            // Boss spawn check every 5 levels if none present
            val bossPresent = enemies.any { it.type == EnemyType.BOSS_TITAN }
            if (level % 5 == 0 && !bossPresent && enemies.size <= 3) {
                spawnBoss()
            } else if (enemies.size < 8) {
                spawnEnemy()
            }
        }
    }

    private fun spawnEnemy() {
        val roll = Random.nextFloat()
        val type = when {
            roll < 0.45f -> EnemyType.DRONE
            roll < 0.75f -> EnemyType.ASTEROID
            else -> EnemyType.CRUISER
        }

        val enemyX = Random.nextFloat() * (worldWidth - 160f) + 80f
        val baseVy = when (type) {
            EnemyType.DRONE -> 230f + level * 15f
            EnemyType.CRUISER -> 100f + level * 10f
            EnemyType.ASTEROID -> 160f + level * 12f
            else -> 120f
        }

        enemies.add(
            Enemy(
                id = nextEntityId++,
                x = enemyX,
                y = -60f,
                vx = if (type == EnemyType.ASTEROID) Random.nextFloat() * 60f - 30f else 0f,
                vy = baseVy,
                type = type,
                health = type.baseHealth + (if (type == EnemyType.CRUISER) (level / 2) else 0),
                maxHealth = type.baseHealth + (if (type == EnemyType.CRUISER) (level / 2) else 0)
            )
        )
    }

    private fun spawnBoss() {
        enemies.add(
            Enemy(
                id = nextEntityId++,
                x = worldWidth / 2f,
                y = -100f,
                vx = 140f,
                vy = 80f,
                type = EnemyType.BOSS_TITAN,
                health = EnemyType.BOSS_TITAN.baseHealth + (level * 8),
                maxHealth = EnemyType.BOSS_TITAN.baseHealth + (level * 8)
            )
        )
        soundEngine.play(ArcadeSoundEngine.SoundEffect.WARNING_SIREN)
        addFloatingText(worldWidth / 2f, 280f, "WARNING: TITAN DREADNOUGHT!", CruiserRed)
        screenShake = 10f
    }

    private fun checkCollisions() {
        // 1. Lasers vs Enemies
        val laserIterator = lasers.iterator()
        while (laserIterator.hasNext()) {
            val laser = laserIterator.next()
            if (!laser.isFromPlayer) {
                // Hostile laser hitting player
                val dx = laser.x - player.x
                val dy = laser.y - player.y
                if (dx * dx + dy * dy < (player.width / 2f) * (player.width / 2f)) {
                    laserIterator.remove()
                    damagePlayer(15 * laser.damage)
                    createExplosion(laser.x, laser.y, CruiserRed, 8)
                    soundEngine.play(ArcadeSoundEngine.SoundEffect.SHIELD_HIT)
                    haptics.impactSmall()
                }
                continue
            }

            // Player laser hitting enemies
            for (enemy in enemies) {
                val halfW = enemy.type.width / 2f
                val halfH = enemy.type.height / 2f
                if (laser.x in (enemy.x - halfW)..(enemy.x + halfW) &&
                    laser.y in (enemy.y - halfH)..(enemy.y + halfH)
                ) {
                    laserIterator.remove()
                    shotsHit++
                    enemy.health -= laser.damage

                    // Sparks
                    for (k in 0 until 5) {
                        particles.add(
                            Particle(
                                x = laser.x,
                                y = laser.y,
                                vx = Random.nextFloat() * 140f - 70f,
                                vy = Random.nextFloat() * 140f - 70f,
                                color = laser.color,
                                size = Random.nextFloat() * 3f + 2f,
                                life = 1f,
                                maxLife = 0.2f
                            )
                        )
                    }

                    if (enemy.health <= 0) {
                        destroyEnemy(enemy)
                    } else {
                        soundEngine.play(ArcadeSoundEngine.SoundEffect.SHIELD_HIT)
                    }
                    break
                }
            }
        }

        // 2. Enemies vs Player (Physical Collision)
        val enemyIterator = enemies.iterator()
        while (enemyIterator.hasNext()) {
            val enemy = enemyIterator.next()
            val dx = enemy.x - player.x
            val dy = enemy.y - player.y
            val collisionDist = (player.width + enemy.type.width) / 2.3f

            if (dx * dx + dy * dy < collisionDist * collisionDist) {
                // Take shield first, then health (faithful to C source code)
                val damageAmount = when (enemy.type) {
                    EnemyType.DRONE -> 25
                    EnemyType.ASTEROID, EnemyType.ASTEROID_MINI -> 30
                    EnemyType.CRUISER -> 45
                    EnemyType.BOSS_TITAN -> 60
                }
                damagePlayer(damageAmount)
                createExplosion(enemy.x, enemy.y, CruiserRed, 18)
                screenShake = 12f

                if (enemy.type != EnemyType.BOSS_TITAN) {
                    enemyIterator.remove()
                }
                soundEngine.play(ArcadeSoundEngine.SoundEffect.EXPLOSION_LARGE)
                haptics.impactHeavy()
            }
        }
    }

    private fun destroyEnemy(enemy: Enemy) {
        enemies.remove(enemy)
        kills++

        // Score with combo multiplier
        comboCount++
        comboTimer = 2.5f
        val multiplier = (1 + (comboCount / 4)).coerceAtMost(5)
        val pointsEarned = enemy.type.scoreValue * level * multiplier
        score += pointsEarned
        creditsEarned += (enemy.type.scoreValue / 3).coerceAtLeast(1)

        // Charge Overdrive Gauge
        player.overdriveGauge = (player.overdriveGauge + (enemy.type.scoreValue * 0.4f)).coerceAtMost(100f)

        // Floating score popup
        addFloatingText(enemy.x, enemy.y, "+$pointsEarned", if (multiplier > 1) NeonYellow else NeonCyan)

        // Asteroid Fragmentation: splits into 2 mini asteroids!
        if (enemy.type == EnemyType.ASTEROID) {
            asteroidsBlasted++
            enemies.add(
                Enemy(
                    id = nextEntityId++,
                    x = enemy.x - 18f,
                    y = enemy.y,
                    vx = -90f,
                    vy = enemy.vy * 1.2f,
                    type = EnemyType.ASTEROID_MINI,
                    health = 1,
                    maxHealth = 1
                )
            )
            enemies.add(
                Enemy(
                    id = nextEntityId++,
                    x = enemy.x + 18f,
                    y = enemy.y,
                    vx = 90f,
                    vy = enemy.vy * 1.2f,
                    type = EnemyType.ASTEROID_MINI,
                    health = 1,
                    maxHealth = 1
                )
            )
        }

        // PowerUp Drop Chance
        val dropRoll = Random.nextFloat()
        if (dropRoll < 0.22f) {
            val pType = when {
                dropRoll < 0.05f -> PowerUpType.SHIELD_BOOST
                dropRoll < 0.10f -> PowerUpType.SPREAD_LASER
                dropRoll < 0.14f -> PowerUpType.RAPID_FIRE
                dropRoll < 0.18f -> PowerUpType.REPAIR_NANITE
                dropRoll < 0.20f -> PowerUpType.OVERDRIVE_NUKE
                else -> PowerUpType.SCRAP_CREDITS
            }
            powerUps.add(PowerUp(nextEntityId++, enemy.x, enemy.y, pType))
        }

        // Explosion effect
        createExplosion(enemy.x, enemy.y, enemy.type.color(), if (enemy.type == EnemyType.BOSS_TITAN) 45 else 16)
        soundEngine.play(if (enemy.type == EnemyType.CRUISER || enemy.type == EnemyType.BOSS_TITAN) ArcadeSoundEngine.SoundEffect.EXPLOSION_LARGE else ArcadeSoundEngine.SoundEffect.EXPLOSION_SMALL)
        haptics.impactSmall()

        // Level Progression Check (From C source code: player.score >= level * 120)
        val scoreForNextLevel = level * 140
        if (score >= scoreForNextLevel) {
            levelUp()
        }
    }

    private fun levelUp() {
        level++
        soundEngine.play(ArcadeSoundEngine.SoundEffect.LEVEL_UP)
        addFloatingText(worldWidth / 2f, worldHeight / 2f - 100f, "SECTOR $level CLEARED!", NeonGreen)
        // Bonus shield recharge on level up
        player.shield = player.maxShield
    }

    private fun damagePlayer(amount: Int) {
        if (player.invulnerableTimer > 0f || player.isOverdriveActive) return

        player.shieldRegenTimer = 0f // Reset recharge counter
        screenShake = 10f

        if (player.shield > 0) {
            val shieldDmg = amount.coerceAtMost(player.shield)
            player.shield -= shieldDmg
            val remainingDmg = amount - shieldDmg
            if (remainingDmg > 0) {
                player.health -= remainingDmg
            }
        } else {
            player.health -= amount
        }

        player.invulnerableTimer = 0.25f // Brief damage invulnerability flicker

        if (player.health <= 0) {
            player.health = 0
            isGameOver = true
            createExplosion(player.x, player.y, NeonCyan, 35)
            soundEngine.play(ArcadeSoundEngine.SoundEffect.GAME_OVER)
            haptics.impactHeavy()
        }
    }

    private fun createExplosion(x: Float, y: Float, color: Color, count: Int) {
        for (i in 0 until count) {
            val angle = Random.nextFloat() * PI * 2f
            val speed = Random.nextFloat() * 220f + 60f
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = (cos(angle) * speed).toFloat(),
                    vy = (sin(angle) * speed).toFloat(),
                    color = color,
                    size = Random.nextFloat() * 5f + 2f,
                    life = 1f,
                    maxLife = Random.nextFloat() * 0.35f + 0.2f
                )
            )
        }
    }

    private fun addFloatingText(x: Float, y: Float, text: String, color: Color) {
        floatingTexts.add(FloatingText(nextEntityId++, x, y, text, color))
    }

    /**
     * Converts the current game field state into the exact 60x15 ASCII character grid
     * from the C source code for the Retro Terminal C-Engine view mode.
     */
    fun generateTerminalAsciiGrid(): List<String> {
        val gridWidth = 60
        val gridHeight = 15
        val buffer = Array(gridHeight) { CharArray(gridWidth) { ' ' } }

        // 1. Draw Player: 'A' and '<' and '>'
        val normPlayerX = ((player.x / worldWidth) * gridWidth).toInt().coerceIn(1, gridWidth - 2)
        val normPlayerY = ((player.y / worldHeight) * gridHeight).toInt().coerceIn(0, gridHeight - 1)
        buffer[normPlayerY][normPlayerX] = 'A'
        if (normPlayerX > 0) buffer[normPlayerY][normPlayerX - 1] = '<'
        if (normPlayerX < gridWidth - 1) buffer[normPlayerY][normPlayerX + 1] = '>'

        // 2. Draw Lasers: '|'
        lasers.forEach { laser ->
            val lx = ((laser.x / worldWidth) * gridWidth).toInt().coerceIn(0, gridWidth - 1)
            val ly = ((laser.y / worldHeight) * gridHeight).toInt().coerceIn(0, gridHeight - 1)
            buffer[ly][lx] = if (laser.isFromPlayer) '|' else '!'
        }

        // 3. Draw Enemies: 'v' (Drone), 'W' (Cruiser), 'o' (Asteroid), 'M' (Boss)
        enemies.forEach { enemy ->
            val ex = ((enemy.x / worldWidth) * gridWidth).toInt().coerceIn(0, gridWidth - 1)
            val ey = ((enemy.y / worldHeight) * gridHeight).toInt().coerceIn(0, gridHeight - 1)
            buffer[ey][ex] = enemy.type.charSymbol
        }

        return buffer.map { String(it) }
    }

    fun getSnapshot(): GameStateSnapshot {
        val activePowerupList = mutableListOf<String>()
        if (player.spreadLaserTimer > 0f) activePowerupList.add("TRIPLE LASER (${player.spreadLaserTimer.toInt()}s)")
        if (player.rapidFireTimer > 0f) activePowerupList.add("RAPID FIRE (${player.rapidFireTimer.toInt()}s)")
        if (player.isOverdriveActive) activePowerupList.add("OVERDRIVE EMP (${player.overdriveTimer.toInt()}s)")

        val boss = enemies.find { it.type == EnemyType.BOSS_TITAN }

        return GameStateSnapshot(
            score = score,
            level = level,
            health = player.health,
            maxHealth = player.maxHealth,
            shield = player.shield,
            maxShield = player.maxShield,
            overdriveGauge = player.overdriveGauge,
            isGameOver = isGameOver,
            isPaused = isPaused,
            comboMultiplier = (1 + (comboCount / 4)).coerceAtMost(5),
            timeRemainingSec = timeRemainingSec.toInt(),
            kills = kills,
            asteroidsBlasted = asteroidsBlasted,
            shotsFired = shotsFired,
            shotsHit = shotsHit,
            creditsEarned = creditsEarned,
            activePowerups = activePowerupList,
            bossActive = boss != null,
            bossHealthPercent = if (boss != null) (boss.health.toFloat() / boss.maxHealth) else 0f,
            screenShakeIntensity = screenShake,
            terminalGrid = if (gameMode == GameMode.CLASSIC_TERMINAL) generateTerminalAsciiGrid() else emptyList()
        )
    }

    private fun EnemyType.color(): Color = when (this) {
        EnemyType.DRONE -> DroneViolet
        EnemyType.CRUISER -> CruiserRed
        EnemyType.ASTEROID, EnemyType.ASTEROID_MINI -> NeonAmber
        EnemyType.BOSS_TITAN -> NeonMagenta
    }
}
