package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.ArcadeHaptics
import com.example.audio.ArcadeSoundEngine
import com.example.data.AchievementEntity
import com.example.data.AppDatabase
import com.example.data.GameRepository
import com.example.data.GameStatsEntity
import com.example.data.HangarUpgradeEntity
import com.example.data.HighScoreEntity
import com.example.game.ControlScheme
import com.example.game.GameEngine
import com.example.game.GameMode
import com.example.game.GameStateSnapshot
import com.example.game.ShipSkin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class GameUiState(
    val isGameActive: Boolean = false,
    val currentGameMode: GameMode = GameMode.HYPER_NEON,
    val snapshot: GameStateSnapshot = GameStateSnapshot(),
    val playerName: String = "PILOT-AX01",
    val controlScheme: ControlScheme = ControlScheme.DRAG,
    val autoFire: Boolean = true,
    val crtScanlines: Boolean = false,
    val screenShake: Boolean = true,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val soundVolume: Float = 0.85f,
    val selectedSkin: ShipSkin = ShipSkin.CYBER_CYAN,
    val isNewHighScore: Boolean = false
)

class GameViewModel(application: Application) : AndroidViewModel(application) {
    val database = AppDatabase.getInstance(application)
    val repository = GameRepository(database)
    val soundEngine = ArcadeSoundEngine()
    val haptics = ArcadeHaptics(application)

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    val topScores: StateFlow<List<HighScoreEntity>> = repository.topScores
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val upgrades: StateFlow<List<HangarUpgradeEntity>> = repository.upgrades
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val gameStats: StateFlow<GameStatsEntity?> = repository.gameStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val achievements: StateFlow<List<AchievementEntity>> = repository.achievements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var activeEngine: GameEngine? = null
        private set

    private var gameLoopJob: Job? = null
    private var hasRecordedScoreForCurrentGame = false

    init {
        // Collect stats to update skins & settings
        viewModelScope.launch {
            repository.gameStats.collect { stats ->
                if (stats != null) {
                    val skin = try {
                        ShipSkin.valueOf(stats.selectedSkin)
                    } catch (_: Exception) {
                        ShipSkin.CYBER_CYAN
                    }
                    val control = try {
                        ControlScheme.valueOf(stats.controlScheme)
                    } catch (_: Exception) {
                        ControlScheme.DRAG
                    }
                    soundEngine.setSoundEnabled(stats.soundEnabled)
                    haptics.setHapticsEnabled(stats.hapticsEnabled)

                    _uiState.value = _uiState.value.copy(
                        selectedSkin = skin,
                        controlScheme = control,
                        soundEnabled = stats.soundEnabled,
                        hapticsEnabled = stats.hapticsEnabled,
                        crtScanlines = stats.crtScanlinesEnabled,
                        screenShake = stats.screenShakeEnabled,
                        autoFire = stats.autoFire
                    )
                }
            }
        }
    }

    fun startNewGame(mode: GameMode) {
        gameLoopJob?.cancel()
        hasRecordedScoreForCurrentGame = false

        val currentUpgrades = upgrades.value
        val engine = GameEngine(
            gameMode = mode,
            soundEngine = soundEngine,
            haptics = haptics,
            upgrades = currentUpgrades,
            skin = _uiState.value.selectedSkin
        )
        activeEngine = engine

        _uiState.value = _uiState.value.copy(
            isGameActive = true,
            currentGameMode = mode,
            isNewHighScore = false,
            snapshot = engine.getSnapshot()
        )

        // Set initial firing state based on autoFire preference
        engine.isFiringRequested = _uiState.value.autoFire

        startGameLoop()
    }

    private fun startGameLoop() {
        gameLoopJob = viewModelScope.launch(Dispatchers.Default) {
            var lastTime = System.nanoTime()
            while (isActive) {
                val now = System.nanoTime()
                val dt = ((now - lastTime) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                lastTime = now

                val engine = activeEngine
                if (engine != null) {
                    engine.update(dt)
                    val snap = engine.getSnapshot()

                    _uiState.value = _uiState.value.copy(snapshot = snap)

                    if (snap.isGameOver && !hasRecordedScoreForCurrentGame) {
                        hasRecordedScoreForCurrentGame = true
                        handleGameOver(engine, snap)
                    }
                }

                delay(16) // Target ~60 FPS
            }
        }
    }

    private fun handleGameOver(engine: GameEngine, snap: GameStateSnapshot) {
        viewModelScope.launch {
            val highest = topScores.value.firstOrNull()?.score ?: 0
            val isNewHigh = snap.score > highest && snap.score > 0
            _uiState.value = _uiState.value.copy(isNewHighScore = isNewHigh)

            repository.recordGameSession(
                playerName = _uiState.value.playerName,
                score = snap.score,
                level = snap.level,
                kills = snap.kills,
                asteroids = snap.asteroidsBlasted,
                shotsFired = snap.shotsFired,
                shotsHit = snap.shotsHit,
                gameMode = engine.gameMode.name,
                creditsEarned = snap.creditsEarned
            )
        }
    }

    // In-game control handlers
    fun setTouchTargetX(targetX: Float?) {
        activeEngine?.targetTouchX = targetX
    }

    fun setInputDirectionX(dirX: Float) {
        activeEngine?.inputDirectionX = dirX
    }

    fun setFiring(isFiring: Boolean) {
        val engine = activeEngine ?: return
        if (_uiState.value.autoFire) {
            engine.isFiringRequested = true
        } else {
            engine.isFiringRequested = isFiring
        }
    }

    fun triggerSingleShot() {
        activeEngine?.shootPlayerLaser()
    }

    fun triggerOverdrive() {
        activeEngine?.triggerOverdriveEMP()
    }

    fun pauseGame() {
        activeEngine?.let {
            it.isPaused = true
            _uiState.value = _uiState.value.copy(snapshot = it.getSnapshot())
        }
    }

    fun resumeGame() {
        activeEngine?.let {
            it.isPaused = false
            _uiState.value = _uiState.value.copy(snapshot = it.getSnapshot())
        }
    }

    fun quitGameToMenu() {
        gameLoopJob?.cancel()
        activeEngine = null
        _uiState.value = _uiState.value.copy(isGameActive = false)
    }

    fun restartGame() {
        startNewGame(_uiState.value.currentGameMode)
    }

    // Upgrades & Customization
    fun purchaseUpgrade(upgrade: HangarUpgradeEntity) {
        val cost = upgrade.baseCost * upgrade.currentLevel
        val availableCredits = gameStats.value?.credits ?: 0
        if (availableCredits >= cost && upgrade.currentLevel < upgrade.maxLevel) {
            viewModelScope.launch {
                repository.purchaseUpgrade(upgrade.upgradeKey, upgrade.currentLevel, cost)
                soundEngine.play(ArcadeSoundEngine.SoundEffect.POWERUP)
            }
        }
    }

    fun selectSkin(skin: ShipSkin) {
        _uiState.value = _uiState.value.copy(selectedSkin = skin)
        viewModelScope.launch {
            repository.setSelectedSkin(skin.name)
        }
    }

    fun setPlayerName(name: String) {
        _uiState.value = _uiState.value.copy(playerName = name)
    }

    fun setControlScheme(scheme: ControlScheme) {
        _uiState.value = _uiState.value.copy(controlScheme = scheme)
    }

    fun toggleAutoFire() {
        val next = !_uiState.value.autoFire
        _uiState.value = _uiState.value.copy(autoFire = next)
        activeEngine?.isFiringRequested = next
    }

    fun toggleSound() {
        val next = !_uiState.value.soundEnabled
        soundEngine.setSoundEnabled(next)
        _uiState.value = _uiState.value.copy(soundEnabled = next)
    }

    fun toggleHaptics() {
        val next = !_uiState.value.hapticsEnabled
        haptics.setHapticsEnabled(next)
        _uiState.value = _uiState.value.copy(hapticsEnabled = next)
    }

    fun toggleCrtScanlines() {
        val next = !_uiState.value.crtScanlines
        _uiState.value = _uiState.value.copy(crtScanlines = next)
    }

    fun toggleScreenShake() {
        val next = !_uiState.value.screenShake
        _uiState.value = _uiState.value.copy(screenShake = next)
    }
}
