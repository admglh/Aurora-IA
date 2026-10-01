package com.aurora.core.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class AuroraVoice(context: Context) : TextToSpeech.OnInitListener {
    private val tts = TextToSpeech(context.applicationContext, this)
    @Volatile private var ready = false

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            tts.language = Locale("pt", "BR")
            configure(0.72f, 0.88f)
        }
    }

    fun configure(pitch: Float, rate: Float) {
        if (!ready) return
        tts.setPitch(pitch.coerceIn(0.55f, 1.0f))
        tts.setSpeechRate(rate.coerceIn(0.70f, 1.05f))
    }

    fun speak(text: String) {
        if (!ready || text.isBlank()) return
        tts.stop()
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "aurora-${System.currentTimeMillis()}")
    }

    fun stop() = tts.stop()
    fun shutdown() = tts.shutdown()
}
