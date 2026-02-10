package com.devfigas.ninemensmorris.game.ai

import com.devfigas.ninemensmorris.game.engine.NineMensMorrisMove
import com.devfigas.ninemensmorris.game.state.NineMensMorrisGameState

interface NineMensMorrisAI {
    fun selectMove(state: NineMensMorrisGameState): NineMensMorrisMove?
}
