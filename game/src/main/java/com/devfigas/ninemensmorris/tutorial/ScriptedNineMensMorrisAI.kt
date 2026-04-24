package com.devfigas.ninemensmorris.tutorial

import com.devfigas.ninemensmorris.game.ai.NineMensMorrisAI
import com.devfigas.ninemensmorris.game.engine.NineMensMorrisMove
import com.devfigas.ninemensmorris.game.state.NineMensMorrisGameState

class ScriptedNineMensMorrisAI(private val placements: List<Int>) : NineMensMorrisAI {
    override fun selectMove(state: NineMensMorrisGameState): NineMensMorrisMove? {
        if (!state.board.isPlacingPhase()) return null
        if (state.mustRemove) return null
        val aiMoveIndex = state.moveHistory.count {
            it.player == state.currentTurn && it.type == NineMensMorrisMove.MoveType.PLACE
        }
        val target = placements.getOrNull(aiMoveIndex) ?: return null
        if (!state.board.isEmpty(target)) return null
        return NineMensMorrisMove(
            player = state.currentTurn,
            type = NineMensMorrisMove.MoveType.PLACE,
            to = target
        )
    }
}
