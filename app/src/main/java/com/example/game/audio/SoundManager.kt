package com.example.game.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * High-performance procedural sound engine:
 * - Pre-computed static PCM audio clips for instant zero-lag playback
 * - Non-blocking AudioTrack writes (WRITE_NON_BLOCKING)
 * - Radio music synthesizer & siren loops
 */
class SoundManager(private val context: Context) {
  private val scope = CoroutineScope(Dispatchers.Default)

  private val sampleRate = 22050
  private var audioTrack: AudioTrack? = null
  private var isInitialized = false

  private val vibrator: Vibrator? by lazy {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
      vm?.defaultVibrator
    } else {
      @Suppress("DEPRECATION")
      context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
  }

  // Pre-computed audio clips
  private lateinit var gunshotPcm: ShortArray
  private lateinit var heavyGunshotPcm: ShortArray
  private lateinit var explosionPcm: ShortArray
  private lateinit var crashPcm: ShortArray
  private lateinit var tireScreechPcm: ShortArray
  private lateinit var hornPcm: ShortArray
  private lateinit var cashChimePcm: ShortArray
  private lateinit var sirenHighPcm: ShortArray
  private lateinit var sirenLowPcm: ShortArray

  // Radio state
  var isRadioEnabled: Boolean = true
  var currentRadioIndex: Int = 0
  private var radioJob: Job? = null
  val radioStations = listOf(
    "Radio Los Santos" to "West Coast Hip-Hop",
    "West Coast Classics" to "90s G-Funk",
    "Non-Stop Pop FM" to "Electro Pop",
    "Los Santos Rock" to "Classic Rock",
    "Radio OFF" to "Muted"
  )

  // Siren state
  private var sirenJob: Job? = null
  private var isSirenActive = false

  init {
    initAudioTrack()
    precalculateClips()
  }

  private fun initAudioTrack() {
    try {
      val minBufferSize = AudioTrack.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_OUT_MONO,
        AudioFormat.ENCODING_PCM_16BIT
      )
      val bufferSize = minBufferSize.coerceAtLeast(sampleRate / 4)

      audioTrack = AudioTrack.Builder()
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
        .setTransferMode(AudioTrack.MODE_STREAM)
        .build()

      audioTrack?.play()
      isInitialized = true
    } catch (_: Exception) {
      isInitialized = false
    }
  }

  private fun precalculateClips() {
    val random = Random.Default

    // 1. Light Gunshot (70ms)
    val gsSamples = (sampleRate * 0.07).toInt()
    gunshotPcm = ShortArray(gsSamples)
    for (i in 0 until gsSamples) {
      val t = i.toDouble() / sampleRate
      val decay = (1.0 - (i.toDouble() / gsSamples)).coerceAtLeast(0.0)
      val noise = (random.nextDouble() * 2.0 - 1.0)
      val punch = sin(2.0 * PI * 160.0 * t)
      val sample = (noise * 0.7 + punch * 0.8) * decay * Short.MAX_VALUE * 0.7
      gunshotPcm[i] = sample.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
    }

    // 2. Heavy Gunshot / Sniper (110ms)
    val hgsSamples = (sampleRate * 0.11).toInt()
    heavyGunshotPcm = ShortArray(hgsSamples)
    for (i in 0 until hgsSamples) {
      val t = i.toDouble() / sampleRate
      val decay = (1.0 - (i.toDouble() / hgsSamples)).coerceAtLeast(0.0)
      val noise = (random.nextDouble() * 2.0 - 1.0)
      val punch = sin(2.0 * PI * 95.0 * t)
      val sample = (noise * 0.75 + punch * 0.85) * decay * Short.MAX_VALUE * 0.85
      heavyGunshotPcm[i] = sample.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
    }

    // 3. Explosion (300ms)
    val expSamples = (sampleRate * 0.30).toInt()
    explosionPcm = ShortArray(expSamples)
    for (i in 0 until expSamples) {
      val t = i.toDouble() / sampleRate
      val progress = i.toDouble() / expSamples
      val decay = (1.0 - progress) * (1.0 - progress)
      val rumble = sin(2.0 * PI * 50.0 * t)
      val noise = (random.nextDouble() * 2.0 - 1.0) * (1.0 - progress * 0.6)
      val sample = (rumble * 0.65 + noise * 0.7) * decay * Short.MAX_VALUE * 0.85
      explosionPcm[i] = sample.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
    }

    // 4. Car Crash (120ms)
    val crashSamples = (sampleRate * 0.12).toInt()
    crashPcm = ShortArray(crashSamples)
    for (i in 0 until crashSamples) {
      val progress = i.toDouble() / crashSamples
      val decay = 1.0 - progress
      val crunch = sin(2.0 * PI * (300.0 - progress * 140.0) * (i.toDouble() / sampleRate))
      val noise = (random.nextDouble() * 2.0 - 1.0)
      val sample = (crunch * 0.5 + noise * 0.6) * decay * Short.MAX_VALUE * 0.75
      crashPcm[i] = sample.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
    }

    // 5. Tire Screech (100ms)
    val tireSamples = (sampleRate * 0.10).toInt()
    tireScreechPcm = ShortArray(tireSamples)
    for (i in 0 until tireSamples) {
      val t = i.toDouble() / sampleRate
      val squeal = sin(2.0 * PI * 1300.0 * t)
      val noise = (random.nextDouble() * 2.0 - 1.0)
      val sample = (squeal * 0.6 + noise * 0.4) * Short.MAX_VALUE * 0.35
      tireScreechPcm[i] = sample.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
    }

    // 6. Car Horn (220ms)
    val hornSamples = (sampleRate * 0.22).toInt()
    hornPcm = ShortArray(hornSamples)
    for (i in 0 until hornSamples) {
      val t = i.toDouble() / sampleRate
      val tone = (sin(2.0 * PI * 370.0 * t) + sin(2.0 * PI * 440.0 * t)) * 0.5
      hornPcm[i] = (tone * Short.MAX_VALUE * 0.55).toInt().toShort()
    }

    // 7. Cash Chime (100ms)
    val chimeSamples = (sampleRate * 0.10).toInt()
    cashChimePcm = ShortArray(chimeSamples)
    for (i in 0 until chimeSamples) {
      val t = i.toDouble() / sampleRate
      val tone = sin(2.0 * PI * 1046.5 * t) * (1.0 - (i.toDouble() / chimeSamples))
      cashChimePcm[i] = (tone * Short.MAX_VALUE * 0.45).toInt().toShort()
    }

    // 8. Siren High & Low (180ms each)
    val sirenSamples = (sampleRate * 0.18).toInt()
    sirenHighPcm = ShortArray(sirenSamples)
    sirenLowPcm = ShortArray(sirenSamples)
    for (i in 0 until sirenSamples) {
      val t = i.toDouble() / sampleRate
      sirenHighPcm[i] = (sin(2.0 * PI * 880.0 * t) * Short.MAX_VALUE * 0.26).toInt().toShort()
      sirenLowPcm[i] = (sin(2.0 * PI * 660.0 * t) * Short.MAX_VALUE * 0.26).toInt().toShort()
    }
  }

  fun vibrate(durationMs: Long, amplitude: Int = 180) {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val clampedAmp = amplitude.coerceIn(1, 255)
        vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, clampedAmp))
      } else {
        @Suppress("DEPRECATION")
        vibrator?.vibrate(durationMs)
      }
    } catch (_: Exception) {}
  }

  fun playGunshot(isHeavy: Boolean = false) {
    vibrate(35, if (isHeavy) 210 else 130)
    playPcmBuffer(if (isHeavy) heavyGunshotPcm else gunshotPcm)
  }

  fun playExplosion() {
    vibrate(220, 255)
    playPcmBuffer(explosionPcm)
  }

  fun playCarCrash() {
    vibrate(70, 180)
    playPcmBuffer(crashPcm)
  }

  fun playTireScreech() {
    playPcmBuffer(tireScreechPcm)
  }

  fun playHorn() {
    playPcmBuffer(hornPcm)
  }

  fun playCashChime() {
    vibrate(25, 90)
    playPcmBuffer(cashChimePcm)
  }

  fun playMissionFanfare() {
    vibrate(120, 180)
    playPcmBuffer(cashChimePcm)
  }

  fun setPoliceSirenActive(active: Boolean) {
    if (active == isSirenActive) return
    isSirenActive = active
    if (active) {
      sirenJob = scope.launch {
        var highPitch = false
        while (isActive && isSirenActive) {
          playPcmBuffer(if (highPitch) sirenHighPcm else sirenLowPcm)
          highPitch = !highPitch
          delay(220)
        }
      }
    } else {
      sirenJob?.cancel()
      sirenJob = null
    }
  }

  fun startRadio() {
    if (!isRadioEnabled || currentRadioIndex == radioStations.size - 1) {
      stopRadio()
      return
    }
    stopRadio()
    radioJob = scope.launch {
      var step = 0
      val pentatonicA = doubleArrayOf(220.0, 261.63, 293.66, 329.63, 392.0, 440.0)
      val funkBass = doubleArrayOf(110.0, 110.0, 130.81, 146.83, 164.81, 146.83)
      val popArp = doubleArrayOf(330.0, 392.0, 493.88, 587.33, 493.88, 392.0)

      while (isActive && isRadioEnabled && currentRadioIndex < radioStations.size - 1) {
        val freq = when (currentRadioIndex) {
          0 -> funkBass[step % funkBass.size]
          1 -> pentatonicA[step % pentatonicA.size] * 0.5
          2 -> popArp[step % popArp.size]
          else -> pentatonicA[(step * 2) % pentatonicA.size]
        }
        step++

        val noteDuration = 0.22
        val noteSamples = (sampleRate * noteDuration).toInt()
        val buffer = ShortArray(noteSamples)
        for (i in 0 until noteSamples) {
          val t = i.toDouble() / sampleRate
          val decay = (1.0 - (i.toDouble() / noteSamples)).coerceAtLeast(0.0)
          val tone = (sin(2.0 * PI * freq * t) + 0.3 * sin(2.0 * PI * freq * 2.0 * t)) * decay
          buffer[i] = (tone * Short.MAX_VALUE * 0.16).toInt().toShort()
        }
        playPcmBuffer(buffer)
        delay((noteDuration * 1000).toLong())
      }
    }
  }

  fun stopRadio() {
    radioJob?.cancel()
    radioJob = null
  }

  fun cycleRadio(): String {
    currentRadioIndex = (currentRadioIndex + 1) % radioStations.size
    val (name, genre) = radioStations[currentRadioIndex]
    if (name == "Radio OFF") {
      stopRadio()
    } else {
      startRadio()
    }
    return "$name ($genre)"
  }

  private fun playPcmBuffer(buffer: ShortArray) {
    if (!isInitialized) return
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        audioTrack?.write(buffer, 0, buffer.size, AudioTrack.WRITE_NON_BLOCKING)
      } else {
        audioTrack?.write(buffer, 0, buffer.size)
      }
    } catch (_: Exception) {}
  }

  fun release() {
    stopRadio()
    setPoliceSirenActive(false)
    try {
      audioTrack?.stop()
      audioTrack?.release()
    } catch (_: Exception) {}
    audioTrack = null
    isInitialized = false
  }
}
