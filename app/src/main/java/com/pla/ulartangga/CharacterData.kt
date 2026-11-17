package com.pla.ulartangga

data class Character(val id: String, val emoji: String, val name: String)

object CharacterData {
    val all = listOf(
        Character("boy", "👦", "Anak Laki-laki"),
        Character("girl", "👧", "Anak Perempuan"),
        Character("man", "🧑", "Orang Dewasa"),
        Character("grandpa", "👴", "Kakek"),
        Character("grandma", "👵", "Nenek")
    )
    fun byId(id: String): Character = all.firstOrNull { it.id == id } ?: all[0]
}