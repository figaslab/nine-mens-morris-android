package com.devfigas.ninemensmorris.game.engine

data class NineMensMorrisMoveResult(
    val board: NineMensMorrisBoard,
    val move: NineMensMorrisMove,
    val millFormed: Boolean,
    val requiresRemoval: Boolean
)
