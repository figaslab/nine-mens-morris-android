package com.devfigas.ninemensmorris.game.manager

import com.devfigas.ninemensmorris.game.engine.NineMensMorrisMove
import com.devfigas.ninemensmorris.game.engine.NineMensMorrisRules
import com.devfigas.ninemensmorris.game.engine.PlayerColor
import com.devfigas.ninemensmorris.game.state.NineMensMorrisGamePhase
import com.devfigas.ninemensmorris.game.state.NineMensMorrisGameResult
import com.devfigas.ninemensmorris.game.state.NineMensMorrisGameState
import com.devfigas.mockpvp.model.User

abstract class NineMensMorrisGameManager(
    onStateChanged: (NineMensMorrisGameState) -> Unit,
    onError: (String) -> Unit
) {
    protected var currentState: NineMensMorrisGameState? = null

    protected var stateCallback: (NineMensMorrisGameState) -> Unit = onStateChanged
    protected var errorCallback: (String) -> Unit = onError

    open fun updateCallbacks(
        onStateChanged: (NineMensMorrisGameState) -> Unit,
        onError: (String) -> Unit
    ) {
        this.stateCallback = onStateChanged
        this.errorCallback = onError
        currentState?.let { stateCallback(it) }
    }

    protected abstract fun startGame(myColor: PlayerColor, opponent: User?)

    open fun handleBoardAction(from: Int, to: Int) {
        val state = currentState ?: return
        if (state.phase != NineMensMorrisGamePhase.PLAYING) return

        if (state.mustRemove) {
            val move = NineMensMorrisMove(state.currentTurn, NineMensMorrisMove.MoveType.REMOVE, to = to)
            val validRemovals = NineMensMorrisRules.getValidRemovals(state.board, state.currentTurn)
            if (validRemovals.any { it.to == to }) {
                applyMove(move)
            }
        } else if (state.board.isPlacingPhase() && state.board.piecesInHand(state.currentTurn) > 0) {
            if (state.board.isEmpty(to)) {
                val move = NineMensMorrisMove(state.currentTurn, NineMensMorrisMove.MoveType.PLACE, to = to)
                applyMove(move)
            }
        } else {
            if (from >= 0 && state.board.getPosition(from) == state.currentTurn && state.board.isEmpty(to)) {
                val canFly = state.board.canFly(state.currentTurn)
                if (canFly) {
                    val move = NineMensMorrisMove(state.currentTurn, NineMensMorrisMove.MoveType.FLY, from, to)
                    applyMove(move)
                } else if (state.board.isAdjacent(from, to)) {
                    val move = NineMensMorrisMove(state.currentTurn, NineMensMorrisMove.MoveType.MOVE, from, to)
                    applyMove(move)
                }
            }
        }
    }

    protected open fun applyMove(move: NineMensMorrisMove) {
        val state = currentState ?: return

        val result = NineMensMorrisRules.applyMove(state.board, move)

        // Determine next turn and mustRemove state
        val nextTurn: PlayerColor
        val mustRemove: Boolean

        if (result.requiresRemoval) {
            nextTurn = state.currentTurn
            mustRemove = true
        } else if (move.type == NineMensMorrisMove.MoveType.REMOVE) {
            nextTurn = state.currentTurn.opposite()
            mustRemove = false
        } else {
            nextTurn = state.currentTurn.opposite()
            mustRemove = false
        }

        // Timer logic
        val now = System.currentTimeMillis()
        val elapsed = if (state.timerActive) now - state.turnStartTime else 0L

        val newRedTime: Long
        val newBlueTime: Long
        if (!mustRemove && move.type != NineMensMorrisMove.MoveType.REMOVE) {
            // Only deduct time on turn switch (not during removal sub-phase)
            if (state.currentTurn == PlayerColor.RED) {
                newRedTime = maxOf(0, state.redTimeRemainingMs - elapsed + state.incrementMs)
                newBlueTime = state.blueTimeRemainingMs
            } else {
                newRedTime = state.redTimeRemainingMs
                newBlueTime = maxOf(0, state.blueTimeRemainingMs - elapsed + state.incrementMs)
            }
        } else {
            newRedTime = state.redTimeRemainingMs
            newBlueTime = state.blueTimeRemainingMs
        }

        // Position history for threefold repetition (only track after placement phase, on turn switch)
        val newPositionHistory = if (!mustRemove && !result.board.isPlacingPhase()) {
            state.positionHistory + (result.board.encode() + "|" + nextTurn.name)
        } else {
            state.positionHistory
        }

        var newState = state.copy(
            board = result.board,
            currentTurn = nextTurn,
            mustRemove = mustRemove,
            moveHistory = state.moveHistory + move,
            positionHistory = newPositionHistory,
            turnStartTime = now,
            redTimeRemainingMs = newRedTime,
            blueTimeRemainingMs = newBlueTime
        )

        // Check game over (only after removal is done, not during mustRemove)
        if (!mustRemove) {
            val gameOver = NineMensMorrisRules.isGameOver(result.board, nextTurn)
            val isRepetition = newState.isThreefoldRepetition()

            if (gameOver) {
                val winner = NineMensMorrisRules.getWinner(result.board, nextTurn)
                val (redPieces, bluePieces) = NineMensMorrisRules.getScore(result.board)
                newState = newState.copy(
                    phase = NineMensMorrisGamePhase.GAME_OVER,
                    result = NineMensMorrisGameResult(
                        winner = winner,
                        reason = NineMensMorrisGameResult.Reason.GAME_COMPLETE,
                        redPieces = redPieces,
                        bluePieces = bluePieces
                    )
                )
            } else if (isRepetition) {
                val (redPieces, bluePieces) = NineMensMorrisRules.getScore(result.board)
                newState = newState.copy(
                    phase = NineMensMorrisGamePhase.GAME_OVER,
                    result = NineMensMorrisGameResult(
                        winner = null,
                        reason = NineMensMorrisGameResult.Reason.REPETITION,
                        redPieces = redPieces,
                        bluePieces = bluePieces
                    )
                )
            }
        }

        updateState(newState)
        onMoveApplied(move, newState, mustRemove)
    }

    protected open fun onMoveApplied(move: NineMensMorrisMove, newState: NineMensMorrisGameState, mustRemove: Boolean) {
        // Override in subclasses
    }

    abstract fun resign()

    open fun leave() {
        resign()
    }

    open fun voteRematch(vote: Boolean) {
        val state = currentState ?: return
        updateState(state.copy(myRematchVote = vote))
        checkRematchVotes()
    }

    protected open fun checkRematchVotes() {
        val state = currentState ?: return
        if (state.myRematchVote == true && state.opponentRematchVote == true) {
            restartGame()
        }
    }

    protected open fun restartGame() {
        val state = currentState ?: return
        val newColor = state.myColor.opposite()
        startGame(newColor, state.opponent)
    }

    open fun sendChatMessage(message: String) {
        // No-op in base class
    }

    open fun resetGame() {
        currentState = null
    }

    protected fun updateState(newState: NineMensMorrisGameState) {
        currentState = newState
        stateCallback(newState)
    }

    protected fun notifyError(error: String) {
        errorCallback(error)
    }

    fun getState(): NineMensMorrisGameState? = currentState
}
