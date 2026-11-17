package com.pla.ulartangga

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class SpeechHelper(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech = TextToSpeech(context.applicationContext, this)
    private var ready = false
    var enabled = true

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale("id", "ID")
            ready = true
        }
    }

    fun speak(text: String) {
        if (!enabled || !ready) return
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    fun shutdown() { tts.stop(); tts.shutdown() }
}