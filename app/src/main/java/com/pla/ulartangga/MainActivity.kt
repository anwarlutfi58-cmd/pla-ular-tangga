package com.pla.ulartangga

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var prefs: SharedPreferences
    private lateinit var speech: SpeechHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = getSharedPreferences("ularTangga", MODE_PRIVATE)
        speech = SpeechHelper(this)

        findViewById<TextView>(R.id.txtLevel).text = getString(R.string.level, prefs.getInt("level", 1))
        findViewById<TextView>(R.id.txtCoins).text = getString(R.string.coins, prefs.getInt("coins", 0))
        findViewById<TextView>(R.id.txtWins).text = getString(R.string.wins, prefs.getInt("wins", 0))

        findViewById<Button>(R.id.btnPlayAi).setOnClickListener {
            speech.speak("Lawan AI. Pilih karakter dulu.")
            startActivity(Intent(this, SelectCharacterActivity::class.java).apply {
                putExtra("mode", "ai")
            })
        }
        findViewById<Button>(R.id.btnPlayLocal).setOnClickListener {
            speech.speak("Dua pemain lokal. Pilih karakter dulu.")
            startActivity(Intent(this, SelectCharacterActivity::class.java).apply {
                putExtra("mode", "local")
            })
        }
        findViewById<Button>(R.id.btnHelp).setOnClickListener { showHelp() }
    }

    override fun onResume() {
        super.onResume()
        findViewById<TextView>(R.id.txtLevel).text = getString(R.string.level, prefs.getInt("level", 1))
        findViewById<TextView>(R.id.txtCoins).text = getString(R.string.coins, prefs.getInt("coins", 0))
        findViewById<TextView>(R.id.txtWins).text = getString(R.string.wins, prefs.getInt("wins", 0))
    }

    private fun showHelp() {
        AlertDialog.Builder(this)
            .setTitle(R.string.help)
            .setMessage(R.string.help_text)
            .setPositiveButton(R.string.understand) { d, _ -> d.dismiss() }
            .show()
    }

    override fun onDestroy() {
        speech.shutdown()
        super.onDestroy()
    }
}