package com.devfigas.ninemensmorris.game.manager

import android.os.Handler
import android.os.Looper
import com.devfigas.ninemensmorris.game.ai.NineMensMorrisAI
import com.devfigas.ninemensmorris.game.ai.NineMensMorrisAIEngine
import com.devfigas.ninemensmorris.game.engine.NineMensMorrisMove
import com.devfigas.ninemensmorris.game.engine.NineMensMorrisRules
import com.devfigas.ninemensmorris.game.engine.PlayerColor
import com.devfigas.ninemensmorris.game.state.NineMensMorrisGamePhase
import com.devfigas.ninemensmorris.game.state.NineMensMorrisGameResult
import com.devfigas.ninemensmorris.game.state.NineMensMorrisGameState
import com.devfigas.mockpvp.model.GameMode
import com.devfigas.mockpvp.model.User
import kotlin.random.Random

class LocalGameManager(
    onStateChanged: (NineMensMorrisGameState) -> Unit,
    onError: (String) -> Unit,
    private val gameMode: GameMode
) : NineMensMorrisGameManager(onStateChanged, onError) {

    private val usesAI: Boolean = gameMode == GameMode.CPU || gameMode == GameMode.INTERNET
    private val ai: NineMensMorrisAI? = if (usesAI) NineMensMorrisAIEngine(level = AI_LEVEL) else null
    private val handler = Handler(Looper.getMainLooper())
    private var cpuColor: PlayerColor = PlayerColor.BLUE

    companion object {
        private const val AI_LEVEL = 5
        private const val CPU_FIXED_DELAY = 500L
        private const val MIN_REMATCH_RESPONSE_DELAY = 1000L
        private const val MAX_REMATCH_RESPONSE_DELAY = 3000L
        private const val REMATCH_DECLINE_CHANCE = 0.10
    }

    override fun startGame(myColor: PlayerColor, opponent: User?) {
        cpuColor = myColor.opposite()

        val state = NineMensMorrisGameState.createNew(gameMode, myColor, opponent).copy(
            isUnlimitedTime = gameMode != GameMode.INTERNET,
            timerActive = false
        )

        updateState(state)

        if (usesAI && cpuColor == PlayerColor.RED) {
            scheduleCPUMove()
        }
    }

    override fun handleBoardAction(from: Int, to: Int) {
        val state = currentState ?: return
        if (state.phase != NineMensMorrisGamePhase.PLAYING) return
        if (usesAI && state.currentTurn == cpuColor) return
        super.handleBoardAction(from, to)
    }

    override fun onMoveApplied(move: NineMensMorrisMove, newState: NineMensMorrisGameState, mustRemove: Boolean) {
        if (usesAI && newState.phase == NineMensMorrisGamePhase.PLAYING && newState.currentTurn == cpuColor) {
            scheduleCPUMove()
        }
    }

    private fun scheduleCPUMove() {
        val delay = if (gameMode == GameMode.INTERNET) {
            val aiEngine = ai as? NineMensMorrisAIEngine
            val state = currentState
            if (aiEngine != null && state != null) aiEngine.calculateThinkingTimeMs(state) else CPU_FIXED_DELAY
        } else {
            CPU_FIXED_DELAY
        }
        handler.postDelayed({ makeCPUMove() }, delay)
    }

    private fun makeCPUMove() {
        val state = currentState ?: return
        if (state.phase != NineMensMorrisGamePhase.PLAYING) return
        if (state.currentTurn != cpuColor) return

        val move = ai?.selectMove(state)
        if (move != null) {
            applyMove(move)
        }
    }

    override fun resign() {
        val state = currentState ?: return
        val winner = if (usesAI) cpuColor else state.currentTurn.opposite()
        val (redPieces, bluePieces) = NineMensMorrisRules.getScore(state.board)
        updateState(state.copy(
            phase = NineMensMorrisGamePhase.GAME_OVER,
            result = NineMensMorrisGameResult(winner, NineMensMorrisGameResult.Reason.RESIGNATION, redPieces, bluePieces)
        ))
    }

    override fun voteRematch(vote: Boolean) {
        val state = currentState ?: return

        if (gameMode == GameMode.LOCAL) {
            if (vote) restartGame()
        } else if (gameMode == GameMode.INTERNET) {
            updateState(state.copy(
                myRematchVote = vote,
                phase = if (vote) NineMensMorrisGamePhase.WAITING_REMATCH else NineMensMorrisGamePhase.GAME_OVER
            ))
            if (vote) {
                val responseDelay = Random.nextLong(MIN_REMATCH_RESPONSE_DELAY, MAX_REMATCH_RESPONSE_DELAY)
                handler.postDelayed({
                    val currentState = this.currentState ?: return@postDelayed
                    val opponentAccepts = Random.nextDouble() >= REMATCH_DECLINE_CHANCE
                    if (opponentAccepts) {
                        updateState(currentState.copy(opponentRematchVote = true))
                        restartGame()
                    } else {
                        updateState(currentState.copy(
                            opponentRematchVote = false,
                            phase = NineMensMorrisGamePhase.GAME_OVER
                        ))
                    }
                }, responseDelay)
            }
        } else {
            updateState(state.copy(myRematchVote = vote, opponentRematchVote = vote))
            if (vote) restartGame()
        }
    }

    override fun restartGame() {
        val state = currentState ?: return
        val newColor = state.myColor.opposite()
        if (usesAI) {
            cpuColor = newColor.opposite()
        }
        startGame(newColor, state.opponent)
    }

    fun cleanup() {
        handler.removeCallbacksAndMessages(null)
    }

    fun checkAndHandleTimeout() {
        val state = currentState ?: return
        if (state.phase != NineMensMorrisGamePhase.PLAYING) return
        if (state.isUnlimitedTime) return
        if (!state.timerActive) return

        if (state.isTimeExpired()) {
            val winner = state.currentTurn.opposite()
            val (redPieces, bluePieces) = NineMensMorrisRules.getScore(state.board)
            updateState(state.copy(
                phase = NineMensMorrisGamePhase.GAME_OVER,
                result = NineMensMorrisGameResult(winner, NineMensMorrisGameResult.Reason.TIMEOUT, redPieces, bluePieces)
            ))
        }
    }

    fun activateTimer() {
        val state = currentState ?: return
        if (state.timerActive) return
        updateState(state.copy(timerActive = true, turnStartTime = System.currentTimeMillis()))
    }
}
