package com.devfigas.ninemensmorris.game.ai

import android.util.Log
import com.devfigas.ninemensmorris.game.engine.NineMensMorrisBoard
import com.devfigas.ninemensmorris.game.engine.NineMensMorrisMove
import com.devfigas.ninemensmorris.game.engine.NineMensMorrisRules
import com.devfigas.ninemensmorris.game.engine.PlayerColor
import com.devfigas.ninemensmorris.game.state.NineMensMorrisGameState
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class NineMensMorrisAIEngine(private val level: Int = 5) : NineMensMorrisAI {

    companion object {
        private const val TAG = "NineMensMorrisAI"
    }

    override fun selectMove(state: NineMensMorrisGameState): NineMensMorrisMove? {
        val validMoves = NineMensMorrisRules.getValidMoves(state.board, state.currentTurn, state.mustRemove)
        if (validMoves.isEmpty()) return null
        if (validMoves.size == 1) return validMoves.first()

        return when {
            level <= 2 -> selectMoveEasy(state.board, validMoves, state.currentTurn, state.mustRemove)
            level <= 5 -> selectMoveMedium(state.board, validMoves, state.currentTurn, state.mustRemove)
            else -> selectMoveHard(state.board, validMoves, state.currentTurn, state.mustRemove)
        }
    }

    private fun selectMoveEasy(
        board: NineMensMorrisBoard,
        moves: List<NineMensMorrisMove>,
        player: PlayerColor,
        mustRemove: Boolean
    ): NineMensMorrisMove {
        if (mustRemove) return moves.random()

        // Prioritize mill-forming moves
        val millMoves = moves.filter { move ->
            val result = NineMensMorrisRules.applyMove(board, move)
            result.millFormed
        }
        if (millMoves.isNotEmpty()) return millMoves.random()

        return moves.random()
    }

    private fun selectMoveMedium(
        board: NineMensMorrisBoard,
        moves: List<NineMensMorrisMove>,
        player: PlayerColor,
        mustRemove: Boolean
    ): NineMensMorrisMove {
        if (mustRemove) {
            // Remove piece from opponent's potential mill
            val opponent = player.opposite()
            val bestRemoval = moves.maxByOrNull { move ->
                val posToRemove = move.to
                var score = 0
                // Prefer removing pieces that are part of potential mills
                for (mill in NineMensMorrisBoard.MILLS_FOR_POSITION[posToRemove]) {
                    val oppCount = mill.count { board.getPosition(it) == opponent && it != posToRemove }
                    if (oppCount == 1) score += 10
                    if (oppCount == 2) score += 5 // was a mill, less priority since we're already breaking it
                }
                // Prefer pieces in strategic positions (junctions with 4 adjacencies)
                score += NineMensMorrisBoard.ADJACENCIES[posToRemove].size
                score
            }
            return bestRemoval ?: moves.random()
        }

        // Priority 1: Form a mill
        val millMoves = moves.filter { move ->
            val result = NineMensMorrisRules.applyMove(board, move)
            result.millFormed
        }
        if (millMoves.isNotEmpty()) return millMoves.first()

        // Priority 2: Block opponent's mill
        val opponent = player.opposite()
        val blockMoves = moves.filter { move ->
            val targetPos = move.to
            for (mill in NineMensMorrisBoard.MILLS_FOR_POSITION[targetPos]) {
                val oppCount = mill.count { it != targetPos && board.getPosition(it) == opponent }
                val emptyCount = mill.count { it != targetPos && board.isEmpty(it) }
                if (oppCount == 2 && emptyCount == 0) return@filter true
            }
            false
        }
        if (blockMoves.isNotEmpty()) return blockMoves.first()

        // Priority 3: Build toward a mill (place/move to create 2-in-a-row)
        val buildMoves = moves.filter { move ->
            val targetPos = move.to
            for (mill in NineMensMorrisBoard.MILLS_FOR_POSITION[targetPos]) {
                val myCount = mill.count { it != targetPos && board.getPosition(it) == player }
                val emptyCount = mill.count { it != targetPos && board.isEmpty(it) }
                if (myCount == 1 && emptyCount == 1) return@filter true
            }
            false
        }
        if (buildMoves.isNotEmpty()) return buildMoves.random()

        // Priority 4: Prefer strategic positions (junctions)
        val strategicMoves = moves.sortedByDescending { NineMensMorrisBoard.ADJACENCIES[it.to].size }
        return strategicMoves.first()
    }

    private fun selectMoveHard(
        board: NineMensMorrisBoard,
        moves: List<NineMensMorrisMove>,
        player: PlayerColor,
        mustRemove: Boolean
    ): NineMensMorrisMove {
        val depth = when {
            level >= 9 -> 6
            level >= 7 -> 4
            else -> 3
        }

        var bestMove = moves.first()
        var bestScore = Double.NEGATIVE_INFINITY

        for (move in moves.shuffled()) {
            val result = NineMensMorrisRules.applyMove(board, move)
            val score = if (result.requiresRemoval) {
                // Same player continues (must remove)
                minimax(result.board, depth - 1, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, true, player, player, true)
            } else if (move.type == NineMensMorrisMove.MoveType.REMOVE) {
                // After removal, opponent's turn
                minimax(result.board, depth - 1, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, false, player.opposite(), player, false)
            } else {
                // Normal move, opponent's turn
                minimax(result.board, depth - 1, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, false, player.opposite(), player, false)
            } + if (result.millFormed) 50.0 else 0.0

            if (score > bestScore) {
                bestScore = score
                bestMove = move
            }
        }

        Log.d(TAG, "AI L$level selected ${bestMove.toNotation()} score=${"%.1f".format(bestScore)}")
        return bestMove
    }

    private fun minimax(
        board: NineMensMorrisBoard,
        depth: Int,
        alpha: Double,
        beta: Double,
        isMaximizing: Boolean,
        currentPlayer: PlayerColor,
        myColor: PlayerColor,
        mustRemove: Boolean
    ): Double {
        if (depth == 0) return evaluate(board, myColor)

        if (!mustRemove && !board.isPlacingPhase() && NineMensMorrisRules.isGameOver(board, currentPlayer)) {
            val winner = NineMensMorrisRules.getWinner(board, currentPlayer)
            return when (winner) {
                myColor -> 10000.0
                null -> 0.0
                else -> -10000.0
            }
        }

        val moves = NineMensMorrisRules.getValidMoves(board, currentPlayer, mustRemove)
        if (moves.isEmpty()) return evaluate(board, myColor)

        var currentAlpha = alpha
        var currentBeta = beta

        if (isMaximizing) {
            var maxEval = Double.NEGATIVE_INFINITY
            for (move in moves) {
                val result = NineMensMorrisRules.applyMove(board, move)
                val eval = if (result.requiresRemoval) {
                    minimax(result.board, depth - 1, currentAlpha, currentBeta, true, currentPlayer, myColor, true)
                } else if (move.type == NineMensMorrisMove.MoveType.REMOVE) {
                    minimax(result.board, depth - 1, currentAlpha, currentBeta, false, currentPlayer.opposite(), myColor, false)
                } else {
                    minimax(result.board, depth - 1, currentAlpha, currentBeta, false, currentPlayer.opposite(), myColor, false)
                }

                maxEval = max(maxEval, eval)
                currentAlpha = max(currentAlpha, eval)
                if (currentBeta <= currentAlpha) break
            }
            return maxEval
        } else {
            var minEval = Double.POSITIVE_INFINITY
            for (move in moves) {
                val result = NineMensMorrisRules.applyMove(board, move)
                val eval = if (result.requiresRemoval) {
                    minimax(result.board, depth - 1, currentAlpha, currentBeta, false, currentPlayer, myColor, true)
                } else if (move.type == NineMensMorrisMove.MoveType.REMOVE) {
                    minimax(result.board, depth - 1, currentAlpha, currentBeta, true, currentPlayer.opposite(), myColor, false)
                } else {
                    minimax(result.board, depth - 1, currentAlpha, currentBeta, true, currentPlayer.opposite(), myColor, false)
                }

                minEval = min(minEval, eval)
                currentBeta = min(currentBeta, eval)
                if (currentBeta <= currentAlpha) break
            }
            return minEval
        }
    }

    private fun evaluate(board: NineMensMorrisBoard, myColor: PlayerColor): Double {
        val opp = myColor.opposite()
        var score = 0.0

        val myPieces = board.totalPieces(myColor)
        val oppPieces = board.totalPieces(opp)
        score += (myPieces - oppPieces) * 100.0

        val myMills = board.countMills(myColor)
        val oppMills = board.countMills(opp)
        score += (myMills - oppMills) * 50.0

        val myPotential = board.countPotentialMills(myColor)
        val oppPotential = board.countPotentialMills(opp)
        score += (myPotential - oppPotential) * 25.0

        if (!board.isPlacingPhase()) {
            val myMobility = NineMensMorrisRules.getValidMovements(board, myColor).size
            val oppMobility = NineMensMorrisRules.getValidMovements(board, opp).size
            score += (myMobility - oppMobility) * 10.0

            // Blocked opponent pieces
            for (pos in board.getOccupiedPositions(opp)) {
                val hasMove = board.getAdjacentPositions(pos).any { board.isEmpty(it) }
                if (!hasMove && !board.canFly(opp)) score += 15.0
            }
        }

        // Strategic position bonus (junction positions: 4, 10, 13, 19 have 4 connections; 1, 7, 16, 22 have 3)
        val strategicPositions = intArrayOf(1, 4, 7, 10, 13, 16, 19, 22)
        for (pos in strategicPositions) {
            when (board.getPosition(pos)) {
                myColor -> score += 5.0
                opp -> score -= 5.0
                else -> {}
            }
        }

        return score
    }

    fun calculateThinkingTimeMs(state: NineMensMorrisGameState): Long {
        val totalMoves = state.moveHistory.size
        val baseTime = when {
            level <= 2 -> 400L
            level <= 5 -> 800L
            else -> 1200L
        }
        val perMoveTime = when {
            level <= 2 -> 10L
            level <= 5 -> 20L
            else -> 30L
        }
        val moveFactor = maxOf(0, 36 - totalMoves)
        val time = baseTime + perMoveTime * moveFactor + Random.nextLong(0, 500)
        return time.coerceIn(300, 5000)
    }
}
