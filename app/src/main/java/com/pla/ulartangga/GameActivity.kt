package com.pla.ulartangga

import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class GameActivity : AppCompatActivity() {
    private lateinit var prefs: SharedPreferences
    private lateinit var speech: SpeechHelper
    private lateinit var sound: SoundHelper
    private lateinit var state: GameState
    private lateinit var engine: GameEngine
    private lateinit var ai: AiPlayer

    private lateinit var boardView: BoardView
    private lateinit var btnMain: Button
    private lateinit var btnHome: Button
    private lateinit var txtTurn: TextView
    private lateinit var txtInfo: TextView

    private val handler = Handler(Looper.getMainLooper())
    private var busy = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)
        prefs = getSharedPreferences("ularTangga", MODE_PRIVATE)
        speech = SpeechHelper(this)
        sound = SoundHelper()

        val mode = intent.getStringExtra("mode") ?: "ai"
        val diffStr = intent.getStringExtra("difficulty") ?: "MEDIUM"
        val p1 = intent.getStringExtra("p1") ?: "boy"
        val p2 = intent.getStringExtra("p2") ?: "girl"

        state = GameState()
        state.difficulty = try { Difficulty.valueOf(diffStr) } catch (e: Exception) { Difficulty.MEDIUM }
        engine = GameEngine(state)
        engine.startGame(mode, state.difficulty, p1, p2)

        ai = AiPlayer(state, { state.players[1].position }, { state.players[0].position })

        boardView = findViewById(R.id.boardView)
        btnMain = findViewById(R.id.btnMain)
        btnHome = findViewById(R.id.btnHome)
        txtTurn = findViewById(R.id.txtTurn)
        txtInfo = findViewById(R.id.txtInfo)

        boardView.onStepRequested = {
            if (state.phase == GamePhase.WAIT_STEP && !busy) {
                handleUserStep()
            }
        }
        btnHome.setOnClickListener {
            speech.speak("Kembali ke menu")
            finish()
        }
        btnMain.setOnClickListener { handleMainButton() }

        updateBoard()
        updateTurnText()
        speakTurn()
    }

    private fun updateBoard() {
        boardView.tokens = state.players
        boardView.activePlayer = state.turn
        boardView.allowUserStep = state.phase == GamePhase.WAIT_STEP && !engine.isAiTurn()
    }

    private fun updateTurnText() {
        val p = state.players.first { it.player == state.turn }
        txtTurn.text = when {
            state.mode == "ai" && p.isAi -> getString(R.string.turn_ai)
            state.mode == "ai" -> getString(R.string.turn_you)
            p.player == 1 -> getString(R.string.turn_p1)
            else -> getString(R.string.turn_p2)
        }
    }

    private fun speakTurn() {
        if (state.mode == "ai" && state.players.first { it.player == state.turn }.isAi) {
            speech.speak("Giliran AI")
        } else {
            speech.speak("Giliranmu. Kocok dadu.")
        }
    }

    private fun handleMainButton() {
        if (busy) return
        if (engine.isAiTurn()) {
            speech.speak(getString(R.string.ai_turn_wait))
            return
        }
        when (state.phase) {
            GamePhase.WAIT_DICE -> rollDice()
            GamePhase.WAIT_STEP -> handleUserStep()
            else -> {}
        }
    }

    private fun rollDice() {
        busy = true
        btnMain.isEnabled = false
        sound.click()

        var ticks = 0
        val anim = object : Runnable {
            override fun run() {
                val temp = 1 + (Math.random() * 6).toInt()
                btnMain.text = "🎲 $temp"
                ticks++
                if (ticks < 8) {
                    handler.postDelayed(this, 90)
                } else {
                    val v = engine.rollDice()
                    sound.click()
                    btnMain.text = "🎲 Dadu: $v"
                    speech.speak("Dadu $v. Tekan bidak untuk jalan.")
                    busy = false
                    updateButtonForPhase()
                    updateBoard()
                    if (engine.isAiTurn()) {
                        scheduleAiMove()
                    }
                }
            }
        }
        handler.post(anim)
    }

    private fun updateButtonForPhase() {
        btnMain.isEnabled = true
        when (state.phase) {
            GamePhase.WAIT_DICE -> btnMain.text = getString(R.string.roll_dice)
            GamePhase.WAIT_STEP -> {
                btnMain.text = if (state.stepsLeft == 1) getString(R.string.step_1)
                else getString(R.string.step_n, state.stepsLeft)
            }
            GamePhase.AI_THINKING -> {
                btnMain.text = getString(R.string.ai_thinking)
                btnMain.isEnabled = false
            }
            else -> btnMain.isEnabled = false
        }
    }

    private fun handleUserStep() {
        if (busy) return
        if (state.phase != GamePhase.WAIT_STEP) return
        if (state.stepsLeft <= 0) return
        busy = true
        btnMain.isEnabled = false

        val p = engine.currentPlayer()
        val newPos = engine.stepOnce() ?: run { busy = false; return }
        sound.step()
        speech.speak("Kotak $newPos")
        boardView.animateTokenStep(p, newPos) {
            boardView.tokens = state.players
            boardView.invalidate()
            updateTurnText()
            txtInfo.text = "Kotak $newPos — sisa ${state.stepsLeft}"

            if (state.stepsLeft == 0) {
                handler.postDelayed({ resolveSpecialFor(p) }, 200)
            } else {
                busy = false
                updateButtonForPhase()
                updateBoard()
            }
        }
    }

    private fun resolveSpecialFor(p: PlayerToken) {
        val (kind, from, to) = engine.resolveSpecial()
        if (kind == "none") {
            finishTurn()
            return
        }
        val animMs = 450L
        boardView.animateTokenStep(p, to) {
            boardView.tokens = state.players
            boardView.invalidate()
            when (kind) {
                "ladder" -> {
                    sound.ladder(); speech.speak("Naik tangga ke $to")
                    txtInfo.text = "🪜 Naik tangga $from → $to"
                }
                "snake" -> {
                    sound.snake(); speech.speak("Dimakan ular, turun ke $to")
                    txtInfo.text = "🐍 Dimakan ular $from → $to"
                }
            }
            handler.postDelayed({ finishTurn() }, animMs)
        }
    }

    private fun finishTurn() {
        if (engine.isWinner()) {
            gameOver()
            return
        }
        engine.endTurn()
        busy = false
        updateBoard()
        updateTurnText()
        updateButtonForPhase()
        speakTurn()
        if (engine.isAiTurn()) {
            scheduleAiMove()
        }
    }

    private fun scheduleAiMove() {
        state.phase = GamePhase.AI_THINKING
        updateButtonForPhase()
        txtInfo.text = getString(R.string.ai_thinking) + " (${ai.describeIntent()})"
        speech.speak("AI sedang berpikir")

        handler.postDelayed({
            // AI kocok dadu
            var ticks = 0
            val anim = object : Runnable {
                override fun run() {
                    val temp = 1 + (Math.random() * 6).toInt()
                    btnMain.text = "🎲 $temp"
                    ticks++
                    if (ticks < 8) {
                        handler.postDelayed(this, 90)
                    } else {
                        val v = engine.rollDice()
                        sound.click()
                        btnMain.text = "🎲 AI dapat: $v"
                        speech.speak("AI dapat dadu $v")
                        txtInfo.text = "AI dapat dadu $v"
                        handler.postDelayed({ aiStepLoop() }, 500)
                    }
                }
            }
            handler.post(anim)
        }, ai.thinkDelayMs())
    }

    private fun aiStepLoop() {
        if (state.stepsLeft <= 0) {
            handler.postDelayed({ resolveSpecialFor(engine.currentPlayer()) }, 200)
            return
        }
        val p = engine.currentPlayer()
        val newPos = engine.stepOnce() ?: return
        sound.step()
        speech.speak("AI ke kotak $newPos")
        boardView.animateTokenStep(p, newPos) {
            boardView.tokens = state.players
            boardView.invalidate()
            txtInfo.text = "AI ke kotak $newPos — sisa ${state.stepsLeft}"
            handler.postDelayed({ aiStepLoop() }, 250)
        }
    }

    private fun gameOver() {
        state.phase = GamePhase.GAME_OVER
        state.running = false
        sound.win()
        val winner = state.turn
        val isUserWin = if (state.mode == "ai") winner == 1 else true

        // Update progress
        val editor = prefs.edit()
        if (state.mode == "ai" && isUserWin) {
            editor.putInt("wins", prefs.getInt("wins", 0) + 1)
            editor.putInt("coins", prefs.getInt("coins", 0) + 100)
            val wins = prefs.getInt("wins", 0) + 1
            editor.putInt("level", Math.floor(wins / 3.0).toInt() + 1)
        } else if (state.mode == "ai") {
            editor.putInt("coins", prefs.getInt("coins", 0) + 20)
        } else {
            editor.putInt("coins", prefs.getInt("coins", 0) + 50)
        }
        editor.apply()

        val (title, msg) = when {
            state.mode == "ai" && winner == 1 -> Pair(getString(R.string.win_title), getString(R.string.win_text))
            state.mode == "ai" -> Pair(getString(R.string.lose_title), getString(R.string.lose_text))
            winner == 1 -> Pair(getString(R.string.p1_win_title), getString(R.string.p_win_text, 1))
            else -> Pair(getString(R.string.p2_win_title), getString(R.string.p_win_text, 2))
        }
        speech.speak(title)

        val view = layoutInflater.inflate(R.layout.dialog_result, null)
        view.findViewById<TextView>(R.id.resTitle).text = title
        view.findViewById<TextView>(R.id.resText).text = msg
        val dlg = AlertDialog.Builder(this).setView(view).setCancelable(false).create()
        view.findViewById<Button>(R.id.btnRetry).setOnClickListener {
            dlg.dismiss()
            recreate()
        }
        view.findViewById<Button>(R.id.btnMenu).setOnClickListener {
            dlg.dismiss(); finish()
        }
        dlg.show()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        speech.shutdown()
        sound.release()
        super.onDestroy()
    }
}