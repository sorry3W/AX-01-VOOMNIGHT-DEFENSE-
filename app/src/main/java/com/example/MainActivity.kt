/*
 * Copyright 2026 Sumon Mahmud (rzk) - AX-01 RZK Software Company
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.game.ControlScheme
import com.example.game.GameMode
import com.example.ui.screens.GameScreen
import com.example.ui.screens.HangarScreen
import com.example.ui.screens.LeaderboardScreen
import com.example.ui.screens.LicensesScreen
import com.example.ui.screens.LoreScreen
import com.example.ui.screens.MainMenuScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.GameViewModel

enum class AppScreen {
    MAIN_MENU,
    GAME,
    HANGAR,
    LEADERBOARD,
    LORE,
    SETTINGS,
    LICENSES
}

class MainActivity : ComponentActivity(), SensorEventListener {
    private val viewModel: GameViewModel by viewModels()
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        sensorManager = getSystemService(SENSOR_SERVICE) as? SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        setContent {
            MyApplicationTheme {
                AppNavigator(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
        // Pause active game if running
        if (viewModel.uiState.value.isGameActive) {
            viewModel.pauseGame()
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null && event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            if (viewModel.uiState.value.controlScheme == ControlScheme.TILT && viewModel.uiState.value.isGameActive) {
                // X axis tilt: positive means tilted left, negative tilted right
                val rawTilt = -event.values[0]
                val normalizedTilt = (rawTilt / 5.0f).coerceIn(-1.0f, 1.0f)
                viewModel.setInputDirectionX(normalizedTilt)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (viewModel.uiState.value.isGameActive) {
            when (keyCode) {
                KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_DPAD_LEFT -> {
                    viewModel.setInputDirectionX(-1f)
                    return true
                }
                KeyEvent.KEYCODE_D, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    viewModel.setInputDirectionX(1f)
                    return true
                }
                KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_DPAD_UP -> {
                    viewModel.setFiring(true)
                    viewModel.triggerSingleShot()
                    return true
                }
                KeyEvent.KEYCODE_E, KeyEvent.KEYCODE_B -> {
                    viewModel.triggerOverdrive()
                    return true
                }
                KeyEvent.KEYCODE_Q, KeyEvent.KEYCODE_ESCAPE -> {
                    viewModel.pauseGame()
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (viewModel.uiState.value.isGameActive) {
            when (keyCode) {
                KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_D,
                KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    viewModel.setInputDirectionX(0f)
                    return true
                }
                KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_DPAD_UP -> {
                    viewModel.setFiring(false)
                    return true
                }
            }
        }
        return super.onKeyUp(keyCode, event)
    }
}

@Composable
fun AppNavigator(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    var currentScreen by remember { mutableStateOf(AppScreen.MAIN_MENU) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(enabled = currentScreen != AppScreen.MAIN_MENU) {
        if (currentScreen == AppScreen.GAME) {
            viewModel.quitGameToMenu()
        }
        currentScreen = AppScreen.MAIN_MENU
    }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "AppScreenTransition",
        modifier = modifier.fillMaxSize()
    ) { screen ->
        when (screen) {
            AppScreen.MAIN_MENU -> {
                MainMenuScreen(
                    viewModel = viewModel,
                    onStartGame = { mode ->
                        viewModel.startNewGame(mode)
                        currentScreen = AppScreen.GAME
                    },
                    onOpenHangar = { currentScreen = AppScreen.HANGAR },
                    onOpenLeaderboard = { currentScreen = AppScreen.LEADERBOARD },
                    onOpenLore = { currentScreen = AppScreen.LORE },
                    onOpenSettings = { currentScreen = AppScreen.SETTINGS },
                    onOpenLicenses = { currentScreen = AppScreen.LICENSES }
                )
            }
            AppScreen.GAME -> {
                GameScreen(
                    viewModel = viewModel,
                    onNavigateBack = { currentScreen = AppScreen.MAIN_MENU }
                )
            }
            AppScreen.HANGAR -> {
                HangarScreen(
                    viewModel = viewModel,
                    onNavigateBack = { currentScreen = AppScreen.MAIN_MENU },
                    onLaunchSortie = {
                        viewModel.startNewGame(GameMode.HYPER_NEON)
                        currentScreen = AppScreen.GAME
                    }
                )
            }
            AppScreen.LEADERBOARD -> {
                LeaderboardScreen(
                    viewModel = viewModel,
                    onNavigateBack = { currentScreen = AppScreen.MAIN_MENU }
                )
            }
            AppScreen.LORE -> {
                LoreScreen(
                    onNavigateBack = { currentScreen = AppScreen.MAIN_MENU }
                )
            }
            AppScreen.SETTINGS -> {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { currentScreen = AppScreen.MAIN_MENU },
                    onOpenLicenses = { currentScreen = AppScreen.LICENSES }
                )
            }
            AppScreen.LICENSES -> {
                LicensesScreen(
                    onNavigateBack = { currentScreen = AppScreen.MAIN_MENU }
                )
            }
        }
    }
}
