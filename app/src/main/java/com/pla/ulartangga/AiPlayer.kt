package com.pla.ulartangga

import kotlin.random.Random

/**
 * AI cerdas tapi TIDAK curang. Dadu tetap acak 1-6.
 * AI hanya "berpikir" berdasarkan situasi papan untuk animasi & kecepatan respons.
 */
class AiPlayer(
    private val state: GameState,
    private val myPos: () -> Int,
    private val oppPos: () -> Int
) {
    fun thinkDelayMs(): Long {
        // Berpikir lebih lama kalau situasi kritis
        val my = myPos()
        val opp = oppPos()
        val base = when (state.difficulty) {
            Difficulty.EASY -> 1800L
            Difficulty.MEDIUM -> 1300L
            Difficulty.HARD -> 900L
        }
        val critical = my >= 85 || opp >= 85
        val jitter = Random.nextLong(200, 700)
        return base + jitter + if (critical) 400L else 0L
    }

    /** Hanya untuk log/animasi — angka dadu tetap acak saat roll */
    fun describeIntent(): String {
        val my = myPos()
        val opp = oppPos()
        return when {
            my in SnakeLadderData.ladders.keys -> "AI melihat tangga di dekatnya"
            my in SnakeLadderData.snakes.keys -> "AI waspada ular"
            opp - my > 15 -> "AI tertinggal, main agresif"
            my - opp > 15 -> "AI memimpin, main aman"
            else -> "AI main normal"
        }
    }
}