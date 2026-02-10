package com.devfigas.ninemensmorris.game.engine

data class NineMensMorrisMove(
    val player: PlayerColor,
    val type: MoveType,
    val from: Int = -1,
    val to: Int
) {
    enum class MoveType {
        PLACE,
        MOVE,
        FLY,
        REMOVE
    }

    fun toNotation(): String {
        return when (type) {
            MoveType.PLACE -> "P:$to"
            MoveType.MOVE -> "M:$from:$to"
            MoveType.FLY -> "F:$from:$to"
            MoveType.REMOVE -> "R:$to"
        }
    }

    companion object {
        fun fromNotation(notation: String, player: PlayerColor): NineMensMorrisMove? {
            val parts = notation.split(":")
            if (parts.size < 2) return null
            return when (parts[0]) {
                "P" -> {
                    val to = parts[1].toIntOrNull() ?: return null
                    NineMensMorrisMove(player, MoveType.PLACE, to = to)
                }
                "M" -> {
                    if (parts.size != 3) return null
                    val from = parts[1].toIntOrNull() ?: return null
                    val to = parts[2].toIntOrNull() ?: return null
                    NineMensMorrisMove(player, MoveType.MOVE, from, to)
                }
                "F" -> {
                    if (parts.size != 3) return null
                    val from = parts[1].toIntOrNull() ?: return null
                    val to = parts[2].toIntOrNull() ?: return null
                    NineMensMorrisMove(player, MoveType.FLY, from, to)
                }
                "R" -> {
                    val to = parts[1].toIntOrNull() ?: return null
                    NineMensMorrisMove(player, MoveType.REMOVE, to = to)
                }
                else -> null
            }
        }
    }
}
