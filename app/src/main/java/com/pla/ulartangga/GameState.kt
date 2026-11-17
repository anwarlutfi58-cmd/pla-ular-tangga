package com.pla.ulartangga

enum class GamePhase {
    WAIT_DICE,      // menunggu kocok dadu
    WAIT_STEP,      // menunggu pemain melangkah
    AI_THINKING,    // AI sedang berpikir
    RESOLVING,      // cek tangga/ular
    GAME_OVER
}

enum class Difficulty { EASY, MEDIUM, HARD }

class GameState {
    var mode = "ai"                // "ai" | "local"
    var difficulty = Difficulty.MEDIUM
    var turn = 1                   // 1 atau 2
    var phase = GamePhase.WAIT_DICE
    var diceValue = 0
    var stepsLeft = 0
    var running = false
    var winner = 0
    val players = mutableListOf<PlayerToken>()
}