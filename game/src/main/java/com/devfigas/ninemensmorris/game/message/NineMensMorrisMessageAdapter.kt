package com.devfigas.ninemensmorris.game.message

import com.devfigas.ninemensmorris.game.engine.NineMensMorrisMove
import com.devfigas.ninemensmorris.game.engine.PlayerColor

object NineMensMorrisMessageAdapter {

    fun encodeMoveData(move: NineMensMorrisMove): String = move.toNotation()

    fun decodeMoveData(moveData: String, player: PlayerColor): NineMensMorrisMove? =
        NineMensMorrisMove.fromNotation(moveData, player)

    fun PlayerColor.toSideName(): String = name

    fun String.toPlayerColor(): PlayerColor = PlayerColor.valueOf(this)
}
