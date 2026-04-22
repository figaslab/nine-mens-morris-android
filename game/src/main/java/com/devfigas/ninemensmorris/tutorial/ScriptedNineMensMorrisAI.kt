package com.devfigas.ninemensmorris.tutorial

import com.devfigas.ninemensmorris.game.ai.NineMensMorrisAI
import com.devfigas.ninemensmorris.game.engine.NineMensMorrisMove
import com.devfigas.ninemensmorris.game.state.NineMensMorrisGameState

class ScriptedNineMensMorrisAI(private val placements: List<Int>) : NineMensMorrisAI {
    override fun selectMove(state: NineMensMorrisGameState): NineMensMorrisMove? {
        // Count AI placements already made to pick the next one.
        val aiMoveIndex = state.moveHistory.count { it.player == state.currentTurn }
        val target = placements.getOrNull(aiMoveIndex) ?: return null
        return NineMensMorrisMove(
            player = state.currentTurn,
            type = NineMensMorrisMove.MoveType.PLACE,
            to = target
        )
    }
}
