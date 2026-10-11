package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

class ArcadeSoundEngine {
    private val scope = CoroutineScope(Dispatchers.Default)
    private val sampleRate = 22050
    private var isEnabled = true
    private var volume = 0.85f

    // Cached PCM buffers for zero-latency playback
    private val soundBuffers = ConcurrentHashMap<SoundEffect, ShortArray>()

    enum class SoundEffect {
        LASER,
        ENEMY_LASER,
        EXPLOSION_SMALL,
        EXPLOSION_LARGE,
        SHIELD_HIT,
        POWERUP,
        LEVEL_UP,
        OVERDRIVE,
        GAME_OVER,
        BUTTON_CLICK,
        WARNING_SIREN
    }

    init {
        scope.launch {
            preloadSounds()
        }
    }

    fun setSoundEnabled(enabled: Boolean) {
        isEnabled = enabled
    }

    fun isSoundEnabled(): Boolean = isEnabled

    fun setVolume(vol: Float) {
        volume = vol.coerceIn(0f, 1f)
    }

    fun getVolume(): Float = volume

    private fun preloadSounds() {
        soundBuffers[SoundEffect.LASER] = generateLaserPcm(sampleRate, 0.08f, 900f, 220f)
        soundBuffers[SoundEffect.ENEMY_LASER] = generateLaserPcm(sampleRate, 0.09f, 400f, 150f)
        soundBuffers[SoundEffect.EXPLOSION_SMALL] = generateExplosionPcm(sampleRate, 0.18f, lowPass = false)
        soundBuffers[SoundEffect.EXPLOSION_LARGE] = generateExplosionPcm(sampleRate, 0.35f, lowPass = true)
        soundBuffers[SoundEffect.SHIELD_HIT] = generateShieldHitPcm(sampleRate, 0.12f)
        soundBuffers[SoundEffect.POWERUP] = generateArpeggioPcm(sampleRate, floatArrayOf(523.25f, 659.25f, 783.99f, 1046.50f), 0.05f)
        soundBuffers[SoundEffect.LEVEL_UP] = generateArpeggioPcm(sampleRate, floatArrayOf(440f, 554.37f, 659.25f, 880f, 1108.73f), 0.06f)
        soundBuffers[SoundEffect.OVERDRIVE] = generateOverdrivePcm(sampleRate, 0.45f)
        soundBuffers[SoundEffect.GAME_OVER] = generateArpeggioPcm(sampleRate, floatArrayOf(440f, 415.30f, 392f, 349.23f, 293.66f), 0.09f)
        soundBuffers[SoundEffect.BUTTON_CLICK] = generateLaserPcm(sampleRate, 0.025f, 1200f, 800f)
        soundBuffers[SoundEffect.WARNING_SIREN] = generateSirenPcm(sampleRate, 0.25f)
    }

    fun play(effect: SoundEffect) {
        if (!isEnabled || volume <= 0.01f) return
        scope.launch {
            val buffer = soundBuffers[effect] ?: return@launch
            playPcmBuffer(buffer, sampleRate, volume)
        }
    }

    private suspend fun playPcmBuffer(pcmData: ShortArray, sampleRate: Int, gain: Float) {
        try {
            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = maxOf(pcmData.size * 2, minBufferSize)

            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            // Apply gain
            val scaledData = if (gain < 0.99f) {
                ShortArray(pcmData.size) { i -> (pcmData[i] * gain).toInt().toShort() }
            } else {
                pcmData
            }

            audioTrack.write(scaledData, 0, scaledData.size)
            audioTrack.play()

            // Clean up after playback
            val durationMs = ((scaledData.size.toFloat() / sampleRate) * 1000).toLong() + 50
            kotlinx.coroutines.delay(durationMs)
            audioTrack.stop()
            audioTrack.release()
        } catch (_: Exception) {
            // Ignored on audio resource allocation edge cases
        }
    }

    // Audio generation algorithms
    private fun generateLaserPcm(rate: Int, durationSec: Float, startFreq: Float, endFreq: Float): ShortArray {
        val totalSamples = (rate * durationSec).toInt()
        val pcm = ShortArray(totalSamples)
        var phase = 0.0

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / totalSamples
            val currentFreq = startFreq + (endFreq - startFreq) * t
            phase += 2.0 * PI * currentFreq / rate
            val sample = sin(phase)
            val envelope = (1.0 - t) // linear fade out
            pcm[i] = (sample * envelope * 24000).toInt().toShort()
        }
        return pcm
    }

    private fun generateExplosionPcm(rate: Int, durationSec: Float, lowPass: Boolean): ShortArray {
        val totalSamples = (rate * durationSec).toInt()
        val pcm = ShortArray(totalSamples)
        var lastVal = 0.0

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / totalSamples
            val rawNoise = Random.nextDouble(-1.0, 1.0)
            val noise = if (lowPass) {
                lastVal = lastVal * 0.85 + rawNoise * 0.15
                lastVal
            } else {
                rawNoise
            }
            val envelope = (1.0 - t) * (1.0 - t) // Quadratic decay
            pcm[i] = (noise * envelope * 28000).toInt().toShort()
        }
        return pcm
    }

    private fun generateShieldHitPcm(rate: Int, durationSec: Float): ShortArray {
        val totalSamples = (rate * durationSec).toInt()
        val pcm = ShortArray(totalSamples)
        var phase = 0.0

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / totalSamples
            val freq = 160.0 - 60.0 * t
            phase += 2.0 * PI * freq / rate
            val square = if (sin(phase) >= 0) 1.0 else -1.0
            val noise = Random.nextDouble(-0.3, 0.3)
            val envelope = (1.0 - t)
            pcm[i] = ((square * 0.7 + noise) * envelope * 22000).toInt().toShort()
        }
        return pcm
    }

    private fun generateArpeggioPcm(rate: Int, frequencies: FloatArray, noteDurationSec: Float): ShortArray {
        val samplesPerNote = (rate * noteDurationSec).toInt()
        val totalSamples = samplesPerNote * frequencies.size
        val pcm = ShortArray(totalSamples)

        for (n in frequencies.indices) {
            val freq = frequencies[n]
            var phase = 0.0
            for (i in 0 until samplesPerNote) {
                val t = i.toDouble() / samplesPerNote
                phase += 2.0 * PI * freq / rate
                // Triangle wave + sine for rich arcade tone
                val sine = sin(phase)
                val square = if (sin(phase) >= 0) 0.5 else -0.5
                val sample = sine * 0.7 + square * 0.3
                val envelope = if (t < 0.1) t / 0.1 else (1.0 - t) / 0.9
                val sampleIdx = n * samplesPerNote + i
                pcm[sampleIdx] = (sample * envelope * 25000).toInt().toShort()
            }
        }
        return pcm
    }

    private fun generateOverdrivePcm(rate: Int, durationSec: Float): ShortArray {
        val totalSamples = (rate * durationSec).toInt()
        val pcm = ShortArray(totalSamples)
        var phase = 0.0

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / totalSamples
            val freq = if (t < 0.3) {
                200.0 + (t / 0.3) * 800.0 // Pitch ramp up
            } else {
                1000.0 - ((t - 0.3) / 0.7) * 850.0 // Super sweep down
            }
            phase += 2.0 * PI * freq / rate
            val sample = sin(phase) + sin(phase * 2.0) * 0.4
            val envelope = (1.0 - t)
            pcm[i] = (sample * envelope * 26000).toInt().toShort()
        }
        return pcm
    }

    private fun generateSirenPcm(rate: Int, durationSec: Float): ShortArray {
        val totalSamples = (rate * durationSec).toInt()
        val pcm = ShortArray(totalSamples)
        var phase = 0.0

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / totalSamples
            val freq = 450.0 + sin(t * PI * 4.0) * 150.0
            phase += 2.0 * PI * freq / rate
            val sample = sin(phase)
            val envelope = (1.0 - t * 0.5)
            pcm[i] = (sample * envelope * 20000).toInt().toShort()
        }
        return pcm
    }
}
