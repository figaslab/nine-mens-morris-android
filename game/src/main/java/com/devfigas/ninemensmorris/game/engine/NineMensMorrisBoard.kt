package com.devfigas.ninemensmorris.game.engine

class NineMensMorrisBoard private constructor(
    private val positions: Array<PlayerColor?>,
    val redPiecesInHand: Int,
    val bluePiecesInHand: Int,
    val redPiecesOnBoard: Int,
    val bluePiecesOnBoard: Int
) {

    companion object {
        const val POSITION_COUNT = 24
        const val INITIAL_PIECES = 9

        val ADJACENCIES: Array<IntArray> = arrayOf(
            intArrayOf(1, 9),           // 0
            intArrayOf(0, 2, 4),        // 1
            intArrayOf(1, 14),          // 2
            intArrayOf(4, 10),          // 3
            intArrayOf(1, 3, 5, 7),     // 4
            intArrayOf(4, 13),          // 5
            intArrayOf(7, 11),          // 6
            intArrayOf(4, 6, 8),        // 7
            intArrayOf(7, 12),          // 8
            intArrayOf(0, 10, 21),      // 9
            intArrayOf(3, 9, 11, 18),   // 10
            intArrayOf(6, 10, 15),      // 11
            intArrayOf(8, 13, 17),      // 12
            intArrayOf(5, 12, 14, 20),  // 13
            intArrayOf(2, 13, 23),      // 14
            intArrayOf(11, 16),         // 15
            intArrayOf(15, 17, 19),     // 16
            intArrayOf(12, 16),         // 17
            intArrayOf(10, 19),         // 18
            intArrayOf(16, 18, 20, 22), // 19
            intArrayOf(13, 19),         // 20
            intArrayOf(9, 22),          // 21
            intArrayOf(19, 21, 23),     // 22
            intArrayOf(14, 22)          // 23
        )

        val MILLS: Array<IntArray> = arrayOf(
            intArrayOf(0, 1, 2),    intArrayOf(3, 4, 5),
            intArrayOf(6, 7, 8),    intArrayOf(9, 10, 11),
            intArrayOf(12, 13, 14), intArrayOf(15, 16, 17),
            intArrayOf(18, 19, 20), intArrayOf(21, 22, 23),
            intArrayOf(0, 9, 21),   intArrayOf(3, 10, 18),
            intArrayOf(6, 11, 15),  intArrayOf(1, 4, 7),
            intArrayOf(16, 19, 22), intArrayOf(8, 12, 17),
            intArrayOf(5, 13, 20),  intArrayOf(2, 14, 23)
        )

        // Precomputed: which mills contain each position
        val MILLS_FOR_POSITION: Array<List<IntArray>> = Array(POSITION_COUNT) { pos ->
            MILLS.filter { mill -> pos in mill }
        }

        fun createEmpty(): NineMensMorrisBoard {
            return NineMensMorrisBoard(
                positions = arrayOfNulls(POSITION_COUNT),
                redPiecesInHand = INITIAL_PIECES,
                bluePiecesInHand = INITIAL_PIECES,
                redPiecesOnBoard = 0,
                bluePiecesOnBoard = 0
            )
        }

        fun decode(data: String): NineMensMorrisBoard? {
            val parts = data.split("|")
            if (parts.size != 3) return null
            val posStr = parts[0]
            if (posStr.length != POSITION_COUNT) return null

            val positions = Array<PlayerColor?>(POSITION_COUNT) { i ->
                when (posStr[i]) {
                    'R' -> PlayerColor.RED
                    'B' -> PlayerColor.BLUE
                    else -> null
                }
            }

            val redInHand = parts[1].toIntOrNull() ?: return null
            val blueInHand = parts[2].toIntOrNull() ?: return null

            val redOnBoard = positions.count { it == PlayerColor.RED }
            val blueOnBoard = positions.count { it == PlayerColor.BLUE }

            return NineMensMorrisBoard(positions, redInHand, blueInHand, redOnBoard, blueOnBoard)
        }
    }

    fun getPosition(pos: Int): PlayerColor? {
        if (pos !in 0 until POSITION_COUNT) return null
        return positions[pos]
    }

    fun isEmpty(pos: Int): Boolean = getPosition(pos) == null

    fun isAdjacent(from: Int, to: Int): Boolean {
        if (from !in 0 until POSITION_COUNT || to !in 0 until POSITION_COUNT) return false
        return to in ADJACENCIES[from]
    }

    fun getAdjacentPositions(pos: Int): IntArray {
        if (pos !in 0 until POSITION_COUNT) return intArrayOf()
        return ADJACENCIES[pos]
    }

    fun formsMill(pos: Int, player: PlayerColor): Boolean {
        for (mill in MILLS_FOR_POSITION[pos]) {
            if (mill.all { p -> if (p == pos) true else positions[p] == player }) {
                return true
            }
        }
        return false
    }

    fun isInMill(pos: Int): Boolean {
        val player = positions[pos] ?: return false
        for (mill in MILLS_FOR_POSITION[pos]) {
            if (mill.all { positions[it] == player }) return true
        }
        return false
    }

    fun allPiecesInMills(player: PlayerColor): Boolean {
        for (i in 0 until POSITION_COUNT) {
            if (positions[i] == player && !isInMill(i)) return false
        }
        return true
    }

    fun piecesInHand(player: PlayerColor): Int =
        if (player == PlayerColor.RED) redPiecesInHand else bluePiecesInHand

    fun piecesOnBoard(player: PlayerColor): Int =
        if (player == PlayerColor.RED) redPiecesOnBoard else bluePiecesOnBoard

    fun totalPieces(player: PlayerColor): Int = piecesInHand(player) + piecesOnBoard(player)

    fun isPlacingPhase(): Boolean = redPiecesInHand > 0 || bluePiecesInHand > 0

    fun canFly(player: PlayerColor): Boolean =
        piecesOnBoard(player) == 3 && piecesInHand(player) == 0

    fun placePiece(pos: Int, player: PlayerColor): NineMensMorrisBoard {
        val newPositions = positions.copyOf()
        newPositions[pos] = player
        return NineMensMorrisBoard(
            positions = newPositions,
            redPiecesInHand = if (player == PlayerColor.RED) redPiecesInHand - 1 else redPiecesInHand,
            bluePiecesInHand = if (player == PlayerColor.BLUE) bluePiecesInHand - 1 else bluePiecesInHand,
            redPiecesOnBoard = if (player == PlayerColor.RED) redPiecesOnBoard + 1 else redPiecesOnBoard,
            bluePiecesOnBoard = if (player == PlayerColor.BLUE) bluePiecesOnBoard + 1 else bluePiecesOnBoard
        )
    }

    fun movePiece(from: Int, to: Int, player: PlayerColor): NineMensMorrisBoard {
        val newPositions = positions.copyOf()
        newPositions[from] = null
        newPositions[to] = player
        return NineMensMorrisBoard(
            positions = newPositions,
            redPiecesInHand = redPiecesInHand,
            bluePiecesInHand = bluePiecesInHand,
            redPiecesOnBoard = redPiecesOnBoard,
            bluePiecesOnBoard = bluePiecesOnBoard
        )
    }

    fun removePiece(pos: Int): NineMensMorrisBoard {
        val removed = positions[pos] ?: return this
        val newPositions = positions.copyOf()
        newPositions[pos] = null
        return NineMensMorrisBoard(
            positions = newPositions,
            redPiecesInHand = redPiecesInHand,
            bluePiecesInHand = bluePiecesInHand,
            redPiecesOnBoard = if (removed == PlayerColor.RED) redPiecesOnBoard - 1 else redPiecesOnBoard,
            bluePiecesOnBoard = if (removed == PlayerColor.BLUE) bluePiecesOnBoard - 1 else bluePiecesOnBoard
        )
    }

    fun encode(): String {
        val sb = StringBuilder(POSITION_COUNT + 6)
        for (i in 0 until POSITION_COUNT) {
            sb.append(
                when (positions[i]) {
                    PlayerColor.RED -> 'R'
                    PlayerColor.BLUE -> 'B'
                    null -> '.'
                }
            )
        }
        sb.append('|').append(redPiecesInHand)
        sb.append('|').append(bluePiecesInHand)
        return sb.toString()
    }

    fun copy(): NineMensMorrisBoard {
        return NineMensMorrisBoard(
            positions.copyOf(), redPiecesInHand, bluePiecesInHand,
            redPiecesOnBoard, bluePiecesOnBoard
        )
    }

    fun getOccupiedPositions(player: PlayerColor): List<Int> {
        val result = mutableListOf<Int>()
        for (i in 0 until POSITION_COUNT) {
            if (positions[i] == player) result.add(i)
        }
        return result
    }

    fun getEmptyPositions(): List<Int> {
        val result = mutableListOf<Int>()
        for (i in 0 until POSITION_COUNT) {
            if (positions[i] == null) result.add(i)
        }
        return result
    }

    fun countMills(player: PlayerColor): Int {
        var count = 0
        for (mill in MILLS) {
            if (mill.all { positions[it] == player }) count++
        }
        return count
    }

    fun countPotentialMills(player: PlayerColor): Int {
        var count = 0
        for (mill in MILLS) {
            val playerCount = mill.count { positions[it] == player }
            val emptyCount = mill.count { positions[it] == null }
            if (playerCount == 2 && emptyCount == 1) count++
        }
        return count
    }
}
