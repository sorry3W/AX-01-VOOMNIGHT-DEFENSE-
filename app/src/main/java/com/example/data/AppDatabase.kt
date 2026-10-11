package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Dao
interface HighScoreDao {
    @Query("SELECT * FROM high_scores ORDER BY score DESC LIMIT 50")
    fun getTopScores(): Flow<List<HighScoreEntity>>

    @Query("SELECT * FROM high_scores WHERE gameMode = :mode ORDER BY score DESC LIMIT 30")
    fun getTopScoresByMode(mode: String): Flow<List<HighScoreEntity>>

    @Query("SELECT MAX(score) FROM high_scores")
    fun getHighestScore(): Flow<Int?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScore(score: HighScoreEntity): Long

    @Query("DELETE FROM high_scores")
    suspend fun clearLeaderboard()
}

@Dao
interface HangarDao {
    @Query("SELECT * FROM hangar_upgrades")
    fun getAllUpgrades(): Flow<List<HangarUpgradeEntity>>

    @Query("SELECT * FROM hangar_upgrades WHERE upgradeKey = :key")
    suspend fun getUpgrade(key: String): HangarUpgradeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUpgrades(upgrades: List<HangarUpgradeEntity>)

    @Query("UPDATE hangar_upgrades SET currentLevel = :level WHERE upgradeKey = :key")
    suspend fun updateUpgradeLevel(key: String, level: Int)
}

@Dao
interface GameStatsDao {
    @Query("SELECT * FROM game_stats WHERE id = 1")
    fun getGameStats(): Flow<GameStatsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveGameStats(stats: GameStatsEntity)

    @Query("UPDATE game_stats SET credits = credits + :amount WHERE id = 1")
    suspend fun addCredits(amount: Int)

    @Query("UPDATE game_stats SET selectedSkin = :skin WHERE id = 1")
    suspend fun setSelectedSkin(skin: String)
}

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements")
    fun getAllAchievements(): Flow<List<AchievementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievements(achievements: List<AchievementEntity>)

    @Query("UPDATE achievements SET isUnlocked = 1, unlockedAt = :timestamp WHERE id = :id AND isUnlocked = 0")
    suspend fun unlockAchievement(id: String, timestamp: Long = System.currentTimeMillis())
}

@Database(
    entities = [
        HighScoreEntity::class,
        HangarUpgradeEntity::class,
        GameStatsEntity::class,
        AchievementEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun highScoreDao(): HighScoreDao
    abstract fun hangarDao(): HangarDao
    abstract fun gameStatsDao(): GameStatsDao
    abstract fun achievementDao(): AchievementDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ax01_voominight_database.db"
                ).addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed default upgrades and achievements
                        CoroutineScope(Dispatchers.IO).launch {
                            val database = getInstance(context)
                            seedInitialData(database)
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun seedInitialData(database: AppDatabase) {
            val initialUpgrades = listOf(
                HangarUpgradeEntity(
                    upgradeKey = "laser_power",
                    name = "Photonic Blaster",
                    description = "Increases primary laser fire rate and unlocks dual/triple plasma beams.",
                    currentLevel = 1,
                    maxLevel = 5,
                    baseCost = 120
                ),
                HangarUpgradeEntity(
                    upgradeKey = "shield_matrix",
                    name = "Ion Deflector Matrix",
                    description = "Expands shield capacity (+20 per rank) and accelerates energy regeneration.",
                    currentLevel = 1,
                    maxLevel = 5,
                    baseCost = 150
                ),
                HangarUpgradeEntity(
                    upgradeKey = "hull_plating",
                    name = "Nanotube Hull Plating",
                    description = "Bolsters AX-01 maximum health and reduces incoming kinetic damage.",
                    currentLevel = 1,
                    maxLevel = 5,
                    baseCost = 100
                ),
                HangarUpgradeEntity(
                    upgradeKey = "thruster_speed",
                    name = "Sub-Warp Thrusters",
                    description = "Enhances lateral dodge agility and ship maneuverability.",
                    currentLevel = 1,
                    maxLevel = 5,
                    baseCost = 80
                ),
                HangarUpgradeEntity(
                    upgradeKey = "tractor_magnet",
                    name = "Magnetic Scavenger",
                    description = "Extends the gravitational pull distance for scrap credits and power-ups.",
                    currentLevel = 1,
                    maxLevel = 5,
                    baseCost = 110
                )
            )
            database.hangarDao().insertUpgrades(initialUpgrades)

            val initialStats = GameStatsEntity(
                id = 1,
                gamesPlayed = 0,
                totalScore = 0L,
                totalKills = 0,
                totalAsteroids = 0,
                totalShotsFired = 0,
                totalShotsHit = 0,
                credits = 300,
                selectedSkin = "CYBER_CYAN"
            )
            database.gameStatsDao().saveGameStats(initialStats)

            val initialAchievements = listOf(
                AchievementEntity("first_flight", "Maiden Sortie", "Complete your first mission on the AX-01 Voominight.", "rocket"),
                AchievementEntity("drone_hunter", "Drone Sweeper", "Destroy 50 enemy reconnaissance drones.", "target"),
                AchievementEntity("asteroid_miner", "Kinetic Fracture", "Blast 25 asteroids into fragments.", "meteor"),
                AchievementEntity("cruiser_bane", "Dreadnought Buster", "Annihilate a Heavy Cruiser dreadnought.", "shield"),
                AchievementEntity("level_5", "Deep Space Veteran", "Reach Sector Level 5 in a single sortie.", "star"),
                AchievementEntity("score_1000", "Cadet Ace", "Score over 1,000 points in one run.", "trophy"),
                AchievementEntity("terminal_purist", "Phosphor Soul", "Play a full session in Classic C-Engine Terminal Mode.", "terminal"),
                AchievementEntity("overdrive_master", "Supercharged", "Unleash an Overdrive EMP Nova blast.", "zap")
            )
            database.achievementDao().insertAchievements(initialAchievements)
        }
    }
}
