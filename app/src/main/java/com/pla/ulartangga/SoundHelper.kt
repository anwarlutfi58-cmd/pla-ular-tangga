package com.pla.ulartangga

import android.media.AudioManager
import android.media.ToneGenerator

class SoundHelper {
    private val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 60)
    var enabled = true

    fun click() { if (enabled) tone.startTone(ToneGenerator.TONE_PROP_BEEP, 60) }
    fun step() { if (enabled) tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 80) }
    fun ladder() { if (enabled) tone.startTone(ToneGenerator.TONE_PROP_ACK, 200) }
    fun snake() { if (enabled) tone.startTone(ToneGenerator.TONE_PROP_NACK, 200) }
    fun win() { if (enabled) tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 400) }
    fun release() { tone.release() }
}