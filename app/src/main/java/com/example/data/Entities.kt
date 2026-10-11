package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "high_scores")
data class HighScoreEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val playerName: String,
    val score: Int,
    val level: Int,
    val enemiesDestroyed: Int,
    val accuracy: Float,
    val gameMode: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "hangar_upgrades")
data class HangarUpgradeEntity(
    @PrimaryKey
    val upgradeKey: String,
    val name: String,
    val description: String,
    val currentLevel: Int = 1,
    val maxLevel: Int = 5,
    val baseCost: Int = 100
)

@Entity(tableName = "game_stats")
data class GameStatsEntity(
    @PrimaryKey
    val id: Int = 1,
    val gamesPlayed: Int = 0,
    val totalScore: Long = 0L,
    val totalKills: Int = 0,
    val totalAsteroids: Int = 0,
    val totalShotsFired: Int = 0,
    val totalShotsHit: Int = 0,
    val credits: Int = 200,
    val selectedSkin: String = "CYBER_CYAN",
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val screenShakeEnabled: Boolean = true,
    val crtScanlinesEnabled: Boolean = false,
    val controlScheme: String = "DRAG",
    val autoFire: Boolean = true,
    val tiltSensitivity: Float = 1.0f
)

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val iconKey: String,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null
)
