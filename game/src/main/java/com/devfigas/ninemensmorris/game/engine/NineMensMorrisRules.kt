package com.devfigas.ninemensmorris.game.engine

object NineMensMorrisRules {

    fun getValidPlacements(board: NineMensMorrisBoard, player: PlayerColor): List<NineMensMorrisMove> {
        if (board.piecesInHand(player) <= 0) return emptyList()
        return board.getEmptyPositions().map { pos ->
            NineMensMorrisMove(player, NineMensMorrisMove.MoveType.PLACE, to = pos)
        }
    }

    fun getValidMovements(board: NineMensMorrisBoard, player: PlayerColor): List<NineMensMorrisMove> {
        val moves = mutableListOf<NineMensMorrisMove>()
        val canFly = board.canFly(player)

        for (from in board.getOccupiedPositions(player)) {
            if (canFly) {
                for (to in board.getEmptyPositions()) {
                    moves.add(NineMensMorrisMove(player, NineMensMorrisMove.MoveType.FLY, from, to))
                }
            } else {
                for (to in board.getAdjacentPositions(from)) {
                    if (board.isEmpty(to)) {
                        moves.add(NineMensMorrisMove(player, NineMensMorrisMove.MoveType.MOVE, from, to))
                    }
                }
            }
        }
        return moves
    }

    fun getValidRemovals(board: NineMensMorrisBoard, player: PlayerColor): List<NineMensMorrisMove> {
        val opponent = player.opposite()
        val nonMillPieces = mutableListOf<Int>()
        val allPieces = mutableListOf<Int>()

        for (pos in board.getOccupiedPositions(opponent)) {
            allPieces.add(pos)
            if (!board.isInMill(pos)) {
                nonMillPieces.add(pos)
            }
        }

        val removable = if (nonMillPieces.isNotEmpty()) nonMillPieces else allPieces
        return removable.map { pos ->
            NineMensMorrisMove(player, NineMensMorrisMove.MoveType.REMOVE, to = pos)
        }
    }

    fun getValidMoves(
        board: NineMensMorrisBoard,
        player: PlayerColor,
        mustRemove: Boolean = false
    ): List<NineMensMorrisMove> {
        if (mustRemove) return getValidRemovals(board, player)
        return if (board.isPlacingPhase() && board.piecesInHand(player) > 0) {
            getValidPlacements(board, player)
        } else {
            getValidMovements(board, player)
        }
    }

    fun applyMove(board: NineMensMorrisBoard, move: NineMensMorrisMove): NineMensMorrisMoveResult {
        val newBoard: NineMensMorrisBoard
        val millFormed: Boolean

        when (move.type) {
            NineMensMorrisMove.MoveType.PLACE -> {
                newBoard = board.placePiece(move.to, move.player)
                millFormed = newBoard.formsMill(move.to, move.player)
            }
            NineMensMorrisMove.MoveType.MOVE, NineMensMorrisMove.MoveType.FLY -> {
                newBoard = board.movePiece(move.from, move.to, move.player)
                millFormed = newBoard.formsMill(move.to, move.player)
            }
            NineMensMorrisMove.MoveType.REMOVE -> {
                newBoard = board.removePiece(move.to)
                millFormed = false
            }
        }

        val requiresRemoval = millFormed && getValidRemovals(newBoard, move.player).isNotEmpty()

        return NineMensMorrisMoveResult(
            board = newBoard,
            move = move,
            millFormed = millFormed,
            requiresRemoval = requiresRemoval
        )
    }

    fun isGameOver(board: NineMensMorrisBoard, currentPlayer: PlayerColor): Boolean {
        if (board.isPlacingPhase()) return false
        if (board.piecesOnBoard(currentPlayer) < 3) return true
        if (getValidMovements(board, currentPlayer).isEmpty()) return true
        return false
    }

    fun getWinner(board: NineMensMorrisBoard, currentPlayer: PlayerColor): PlayerColor? {
        if (board.isPlacingPhase()) return null
        if (board.piecesOnBoard(currentPlayer) < 3) return currentPlayer.opposite()
        if (getValidMovements(board, currentPlayer).isEmpty()) return currentPlayer.opposite()
        return null
    }

    fun getScore(board: NineMensMorrisBoard): Pair<Int, Int> {
        return board.piecesOnBoard(PlayerColor.RED) to board.piecesOnBoard(PlayerColor.BLUE)
    }
}
