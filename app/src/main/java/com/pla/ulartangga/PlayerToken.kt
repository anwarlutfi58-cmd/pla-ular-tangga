package com.pla.ulartangga

data class PlayerToken(
    val player: Int,
    val characterId: String,
    var position: Int = 1,
    val isAi: Boolean = false
) {
    val emoji: String get() = if (isAi) "🤖" else CharacterData.byId(characterId).emoji
    val name: String get() = if (isAi) "AI" else CharacterData.byId(characterId).name
}