package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class GameRepository(private val database: AppDatabase) {

    val topScores: Flow<List<HighScoreEntity>> = database.highScoreDao().getTopScores()
    val highestScore: Flow<Int?> = database.highScoreDao().getHighestScore()
    val upgrades: Flow<List<HangarUpgradeEntity>> = database.hangarDao().getAllUpgrades()
    val gameStats: Flow<GameStatsEntity?> = database.gameStatsDao().getGameStats()
    val achievements: Flow<List<AchievementEntity>> = database.achievementDao().getAllAchievements()

    fun getTopScoresByMode(mode: String): Flow<List<HighScoreEntity>> {
        return database.highScoreDao().getTopScoresByMode(mode)
    }

    suspend fun recordGameSession(
        playerName: String,
        score: Int,
        level: Int,
        kills: Int,
        asteroids: Int,
        shotsFired: Int,
        shotsHit: Int,
        gameMode: String,
        creditsEarned: Int
    ) = withContext(Dispatchers.IO) {
        val accuracy = if (shotsFired > 0) (shotsHit.toFloat() / shotsFired) * 100f else 0f
        val record = HighScoreEntity(
            playerName = playerName.ifBlank { "PILOT-AX01" },
            score = score,
            level = level,
            enemiesDestroyed = kills,
            accuracy = accuracy,
            gameMode = gameMode
        )
        database.highScoreDao().insertScore(record)

        // Update lifetime statistics
        val currentStats = database.gameStatsDao().getGameStats()
        // Read current stats or create default
        database.gameStatsDao().addCredits(creditsEarned)

        // Check Achievements
        database.achievementDao().unlockAchievement("first_flight")
        if (score >= 1000) database.achievementDao().unlockAchievement("score_1000")
        if (level >= 5) database.achievementDao().unlockAchievement("level_5")
        if (gameMode == "CLASSIC_TERMINAL") database.achievementDao().unlockAchievement("terminal_purist")
        if (asteroids >= 5) database.achievementDao().unlockAchievement("asteroid_miner")
    }

    suspend fun purchaseUpgrade(upgradeKey: String, currentLevel: Int, cost: Int): Boolean = withContext(Dispatchers.IO) {
        // Upgrade level
        database.hangarDao().updateUpgradeLevel(upgradeKey, currentLevel + 1)
        database.gameStatsDao().addCredits(-cost)
        true
    }

    suspend fun setSelectedSkin(skin: String) = withContext(Dispatchers.IO) {
        database.gameStatsDao().setSelectedSkin(skin)
    }

    suspend fun saveSettings(
        sound: Boolean,
        haptics: Boolean,
        screenShake: Boolean,
        crtScanlines: Boolean,
        controlScheme: String,
        autoFire: Boolean,
        sensitivity: Float
    ) = withContext(Dispatchers.IO) {
        // Update stats entity settings fields
    }

    suspend fun unlockAchievement(id: String) = withContext(Dispatchers.IO) {
        database.achievementDao().unlockAchievement(id)
    }
}
