package com.example.taskmanager.ui.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import android.view.View
import com.example.taskmanager.settings.model.SoundEffectType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

object SoundHelper {
    private val scope = CoroutineScope(Dispatchers.Default)
    private const val SAMPLE_RATE = 44100

    /**
     * Plays the exact selected completion sound with soothing harmonic synthesis and gentle haptic vibration.
     */
    fun playTaskCompletionSound(
        context: Context? = null,
        view: View? = null,
        soundType: SoundEffectType = SoundEffectType.ZEN_BELL
    ) {
        // 1. Gentle haptic feedback
        triggerHapticVibration(context, view)

        // 2. Play the selected audio tone
        if (soundType == SoundEffectType.NONE) return

        scope.launch {
            try {
                val pcmBuffer = generateSoundBuffer(soundType)
                if (pcmBuffer.isNotEmpty()) {
                    playPcmAudio(pcmBuffer)
                } else {
                    playFallbackTone(soundType)
                }
            } catch (e: Exception) {
                // Fallback tone generator in case AudioTrack is constrained
                playFallbackTone(soundType)
            }
        }
    }

    private fun triggerHapticVibration(context: Context?, view: View?) {
        try {
            view?.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            if (context != null) {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vibratorManager?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }

                if (vibrator != null && vibrator.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(40)
                    }
                }
            }
        } catch (_: Exception) {}
    }

    private fun generateSoundBuffer(soundType: SoundEffectType): ShortArray {
        return when (soundType) {
            SoundEffectType.ZEN_BELL -> generateZenBell()
            SoundEffectType.MARIMBA -> generateMarimba()
            SoundEffectType.BUBBLE_POP -> generateBubblePop()
            SoundEffectType.NONE -> ShortArray(0)
        }
    }

    /**
     * 528 Hz Meditative Solfeggio Zen Bell with rich harmonics and a peaceful exponential decay.
     */
    private fun generateZenBell(): ShortArray {
        val durationSeconds = 0.85
        val numSamples = (durationSeconds * SAMPLE_RATE).toInt()
        val buffer = ShortArray(numSamples)
        val f0 = 528.0 // Solfeggio frequency

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            // Multi-harmonic bell shimmer
            val wave = 0.60 * sin(2.0 * PI * f0 * t) +
                    0.28 * sin(2.0 * PI * (f0 * 2.0) * t) +
                    0.18 * sin(2.0 * PI * (f0 * 3.0) * t) +
                    0.08 * sin(2.0 * PI * (f0 * 4.2) * t)

            // Smooth attack and long exponential decay
            val attack = (t / 0.012).coerceAtMost(1.0)
            val decay = exp(-3.8 * t)
            val sample = (wave * attack * decay * 32000.0).toInt().coerceIn(-32768, 32767)
            buffer[i] = sample.toShort()
        }
        return buffer
    }

    /**
     * Warm wooden acoustic Marimba strike (A4 440 Hz + harmonic doublets).
     */
    private fun generateMarimba(): ShortArray {
        val durationSeconds = 0.35
        val numSamples = (durationSeconds * SAMPLE_RATE).toInt()
        val buffer = ShortArray(numSamples)
        val f0 = 440.0 // A4 note

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val wave = 0.75 * sin(2.0 * PI * f0 * t) +
                    0.30 * sin(2.0 * PI * (f0 * 2.0) * t) +
                    0.15 * sin(2.0 * PI * (f0 * 3.0) * t)

            val attack = (t / 0.005).coerceAtMost(1.0)
            val decay = exp(-10.0 * t)
            val sample = (wave * attack * decay * 32000.0).toInt().coerceIn(-32768, 32767)
            buffer[i] = sample.toShort()
        }
        return buffer
    }

    /**
     * Pitch-glide playful bubble pop (380 Hz to 1100 Hz).
     */
    private fun generateBubblePop(): ShortArray {
        val durationSeconds = 0.16
        val numSamples = (durationSeconds * SAMPLE_RATE).toInt()
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val currentFreq = 380.0 + (720.0 * (t / durationSeconds))
            val wave = sin(2.0 * PI * currentFreq * t)

            val attack = (t / 0.008).coerceAtMost(1.0)
            val decay = exp(-16.0 * t)
            val sample = (wave * attack * decay * 32000.0).toInt().coerceIn(-32768, 32767)
            buffer[i] = sample.toShort()
        }
        return buffer
    }

    private fun playPcmAudio(pcmData: ShortArray) {
        val minBufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = maxOf(minBufferSize, pcmData.size * 2)

        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        audioTrack.setVolume(1.0f)
        audioTrack.write(pcmData, 0, pcmData.size)
        audioTrack.play()

        // Safely release AudioTrack after playback finishes
        scope.launch {
            delay(1200)
            try {
                audioTrack.stop()
                audioTrack.release()
            } catch (_: Exception) {}
        }
    }

    private fun playFallbackTone(soundType: SoundEffectType) {
        try {
            val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 90)
            val toneType = when (soundType) {
                SoundEffectType.ZEN_BELL -> ToneGenerator.TONE_PROP_BEEP2
                SoundEffectType.MARIMBA -> ToneGenerator.TONE_PROP_PROMPT
                SoundEffectType.BUBBLE_POP -> ToneGenerator.TONE_PROP_ACK
                SoundEffectType.NONE -> -1
            }
            if (toneType != -1) {
                toneGenerator.startTone(toneType, 180)
                scope.launch {
                    delay(300)
                    toneGenerator.release()
                }
            }
        } catch (_: Exception) {}
    }
}
