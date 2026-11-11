package com.studyflow.app.feature.pomodoro

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper

/** Som de fim de ciclo. Abstraído para permitir um fake nos testes. */
fun interface PomodoroSound {
    fun play()
}

/** Bipe curto quando o foco/intervalo termina, equivalente ao `playChime` do web. */
object PomodoroChime : PomodoroSound {
    override fun play() {
        runCatching {
            val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
            tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 700)
            Handler(Looper.getMainLooper()).postDelayed({ tone.release() }, 900)
        }
    }
}
