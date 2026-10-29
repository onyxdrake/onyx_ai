package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.sin

class AudioSynthesizer(context: Context) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var ambientJob: Job? = null
    private var ambientTrack: AudioTrack? = null
    private var isAmbientPlaying = false

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                tts?.setPitch(1.15f) // slightly brighter anime AI pitch
                tts?.setSpeechRate(1.05f)
                isTtsReady = true
            }
        }
    }

    /**
     * Plays a pleasant 5-second futuristic chime melody using synthesized PCM audio.
     */
    fun play5SecondChime() {
        scope.launch(Dispatchers.IO) {
            val sampleRate = 44100
            val durationSeconds = 5.0
            val totalSamples = (sampleRate * durationSeconds).toInt()
            val buffer = ShortArray(totalSamples)

            // Frequencies for a shimmering futuristic 5-note arpeggio chord (C5, E5, G5, B5, C6)
            val notes = listOf(523.25, 659.25, 783.99, 987.77, 1046.50)
            val noteDuration = totalSamples / notes.size

            for (i in 0 until totalSamples) {
                val noteIndex = (i / noteDuration).coerceIn(0, notes.size - 1)
                val freq = notes[noteIndex]
                val t = i.toDouble() / sampleRate
                val noteProgress = (i % noteDuration).toDouble() / noteDuration

                // Decay envelope per note with lingering echo
                val envelope = (1.0 - noteProgress * 0.7) * (1.0 - (i.toDouble() / totalSamples) * 0.5)

                // Fundamental + harmonics for crystalline chime sound
                val wave1 = sin(2.0 * Math.PI * freq * t)
                val wave2 = 0.5 * sin(2.0 * Math.PI * freq * 2.0 * t)
                val wave3 = 0.25 * sin(2.0 * Math.PI * freq * 3.0 * t)
                val combined = (wave1 + wave2 + wave3) * envelope * 0.3

                buffer[i] = (combined * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }

            try {
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
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
                    .setBufferSizeInBytes(totalSamples * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(buffer, 0, totalSamples)
                track.play()
                delay(5200)
                track.stop()
                track.release()
            } catch (_: Exception) {
                // Ignore audio failure
            }
        }
    }

    /**
     * Starts or stops futuristic ambient background audio drone.
     */
    fun setAmbientAudio(enabled: Boolean) {
        if (enabled && !isAmbientPlaying) {
            startAmbientAudio()
        } else if (!enabled && isAmbientPlaying) {
            stopAmbientAudio()
        }
    }

    private fun startAmbientAudio() {
        stopAmbientAudio()
        isAmbientPlaying = true

        ambientJob = scope.launch(Dispatchers.IO) {
            val sampleRate = 22050
            val bufferSize = sampleRate * 2 // 2 second loop buffer
            val buffer = ShortArray(bufferSize)

            // Warm low-frequency binaural hum: 110Hz + 114Hz with soft modulation
            for (i in 0 until bufferSize) {
                val t = i.toDouble() / sampleRate
                val hum1 = sin(2.0 * Math.PI * 110.0 * t)
                val hum2 = sin(2.0 * Math.PI * 114.0 * t)
                val lfo = 0.5 + 0.5 * sin(2.0 * Math.PI * 0.2 * t)
                val sample = ((hum1 + hum2) * 0.12 * lfo)
                buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
            }

            try {
                ambientTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                ambientTrack?.write(buffer, 0, bufferSize)
                ambientTrack?.setLoopPoints(0, bufferSize, -1)
                ambientTrack?.play()

                while (isActive && isAmbientPlaying) {
                    delay(1000)
                }
            } catch (_: Exception) {
                // Ignore
            }
        }
    }

    private fun stopAmbientAudio() {
        isAmbientPlaying = false
        ambientJob?.cancel()
        ambientJob = null
        try {
            ambientTrack?.stop()
            ambientTrack?.release()
        } catch (_: Exception) {}
        ambientTrack = null
    }

    fun speak(text: String) {
        if (isTtsReady && tts != null) {
            tts?.speak(text.take(300), TextToSpeech.QUEUE_FLUSH, null, "zenith_tts")
        }
    }

    fun stopSpeaking() {
        tts?.stop()
    }

    fun release() {
        stopAmbientAudio()
        tts?.stop()
        tts?.shutdown()
    }
}
