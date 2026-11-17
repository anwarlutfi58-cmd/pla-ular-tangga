package com.pla.ulartangga

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class SelectCharacterActivity : AppCompatActivity() {
    private var mode = "ai"
    private var p1Char = "boy"
    private var p2Char = "girl"
    private lateinit var speech: SpeechHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_character)
        speech = SpeechHelper(this)
        mode = intent.getStringExtra("mode") ?: "ai"

        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }

        val container = findViewById<LinearLayout>(R.id.charContainer)
        for (c in CharacterData.all) {
            val v = LayoutInflater.from(this).inflate(R.layout.item_character, container, false)
            v.findViewById<TextView>(R.id.charEmoji).text = c.emoji
            v.findViewById<TextView>(R.id.charName).text = c.name
            v.setOnClickListener {
                speech.speak(c.name)
                showPickPlayerDialog(c)
            }
            container.addView(v)
        }

        findViewById<Button>(R.id.btnStart).setOnClickListener {
            if (mode == "ai") {
                showDifficultyDialog()
            } else {
                startGame(Difficulty.MEDIUM)
            }
        }
    }

    private fun showPickPlayerDialog(c: Character) {
        val items = if (mode == "ai") arrayOf("Pemain 1 (Kamu)") else arrayOf("Pemain 1", "Pemain 2")
        AlertDialog.Builder(this)
            .setTitle("Pilih untuk siapa?")
            .setItems(items) { _, which ->
                if (which == 0) p1Char = c.id else p2Char = c.id
                speech.speak("${c.name} dipilih")
            }
            .show()
    }

    private fun showDifficultyDialog() {
        val items = arrayOf(
            getString(R.string.easy),
            getString(R.string.medium),
            getString(R.string.hard)
        )
        AlertDialog.Builder(this)
            .setTitle(R.string.select_difficulty)
            .setItems(items) { _, which ->
                val diff = when (which) {
                    0 -> Difficulty.EASY
                    1 -> Difficulty.MEDIUM
                    else -> Difficulty.HARD
                }
                startGame(diff)
            }
            .show()
    }

    private fun startGame(diff: Difficulty) {
        val i = Intent(this, GameActivity::class.java)
        i.putExtra("mode", mode)
        i.putExtra("difficulty", diff.name)
        i.putExtra("p1", p1Char)
        i.putExtra("p2", p2Char)
        startActivity(i)
        finish()
    }

    override fun onDestroy() {
        speech.shutdown()
        super.onDestroy()
    }
}