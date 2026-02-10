package com.devfigas.ninemensmorris.game.state

import com.devfigas.ninemensmorris.game.engine.PlayerColor

data class NineMensMorrisGameResult(
    val winner: PlayerColor?,
    val reason: Reason,
    val redPieces: Int,
    val bluePieces: Int
) {
    enum class Reason {
        GAME_COMPLETE,
        RESIGNATION,
        TIMEOUT,
        OPPONENT_LEFT,
        AGREEMENT,
        REPETITION
    }
}
