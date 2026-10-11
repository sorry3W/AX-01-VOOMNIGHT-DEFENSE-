package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.audio.ArcadeHaptics
import com.example.audio.ArcadeSoundEngine
import com.example.game.EnemyType
import com.example.game.GameEngine
import com.example.game.GameMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GameEngineUnitTest {

    @Test
    fun `test game engine initialization and firing`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val soundEngine = ArcadeSoundEngine()
        val haptics = ArcadeHaptics(context)

        val engine = GameEngine(
            gameMode = GameMode.HYPER_NEON,
            soundEngine = soundEngine,
            haptics = haptics,
            upgrades = emptyList()
        )

        assertEquals(100, engine.player.health)
        assertEquals(50, engine.player.shield)
        assertEquals(0, engine.score)
        assertEquals(1, engine.level)
        assertFalse(engine.isGameOver)

        // Test laser firing
        engine.shootPlayerLaser()
        assertTrue(engine.lasers.isNotEmpty())
        assertEquals(1, engine.shotsFired)

        // Test update loop
        engine.update(0.016f)
        assertNotNull(engine.getSnapshot())
    }

    @Test
    fun `test terminal ascii grid generation for classic c mode`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val soundEngine = ArcadeSoundEngine()
        val haptics = ArcadeHaptics(context)

        val engine = GameEngine(
            gameMode = GameMode.CLASSIC_TERMINAL,
            soundEngine = soundEngine,
            haptics = haptics,
            upgrades = emptyList()
        )

        val grid = engine.generateTerminalAsciiGrid()
        assertEquals(15, grid.size)
        // Check that player craft 'A' is drawn in the grid
        val hasPlayer = grid.any { line -> line.contains("A") }
        assertTrue(hasPlayer)
    }
}
