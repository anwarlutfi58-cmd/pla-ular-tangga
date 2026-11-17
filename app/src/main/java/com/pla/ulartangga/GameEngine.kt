package com.pla.ulartangga

import kotlin.random.Random

class GameEngine(private val state: GameState) {

    fun startGame(mode: String, difficulty: Difficulty, p1Char: String, p2Char: String) {
        state.mode = mode
        state.difficulty = difficulty
        state.players.clear()
        state.players.add(PlayerToken(player = 1, characterId = p1Char, position = 1, isAi = false))
        if (mode == "ai") {
            state.players.add(PlayerToken(player = 2, characterId = "ai", position = 1, isAi = true))
        } else {
            state.players.add(PlayerToken(player = 2, characterId = p2Char, position = 1, isAi = false))
        }
        state.turn = 1
        state.phase = GamePhase.WAIT_DICE
        state.diceValue = 0
        state.stepsLeft = 0
        state.running = true
        state.winner = 0
    }

    fun currentPlayer(): PlayerToken = state.players.first { it.player == state.turn }

    fun isAiTurn(): Boolean = state.mode == "ai" && state.turn == 2

    fun rollDice(): Int {
        val v = 1 + Random.nextInt(6)
        state.diceValue = v
        state.stepsLeft = v
        state.phase = if (isAiTurn()) GamePhase.AI_THINKING else GamePhase.WAIT_STEP
        return v
    }

    /**
     * Langkah manual 1 kotak. Return posisi baru, atau null kalau tidak boleh.
     */
    fun stepOnce(): Int? {
        if (state.phase != GamePhase.WAIT_STEP && state.phase != GamePhase.AI_THINKING) return null
        if (state.stepsLeft <= 0) return null
        val p = currentPlayer()
        if (p.position >= 100) return null
        p.position = (p.position + 1).coerceAtMost(100)
        state.stepsLeft--
        return p.position
    }

    /**
     * Setelah semua langkah habis, cek tangga/ular.
     * Return Pair(jenis, tujuan) — jenis: "ladder", "snake", atau "none"
     */
    fun resolveSpecial(): Triple<String, Int, Int> {
        val p = currentPlayer()
        val pos = p.position
        SnakeLadderData.ladders[pos]?.let { dest ->
            p.position = dest
            return Triple("ladder", pos, dest)
        }
        SnakeLadderData.snakes[pos]?.let { dest ->
            p.position = dest
            return Triple("snake", pos, dest)
        }
        return Triple("none", pos, pos)
    }

    fun isWinner(): Boolean = currentPlayer().position >= 100

    fun endTurn() {
        if (isWinner()) {
            state.winner = state.turn
            state.phase = GamePhase.GAME_OVER
            state.running = false
            return
        }
        state.turn = if (state.turn == 1) 2 else 1
        state.phase = GamePhase.WAIT_DICE
        state.diceValue = 0
        state.stepsLeft = 0
    }
}