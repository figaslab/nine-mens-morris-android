package com.devfigas.ninemensmorris.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.devfigas.ninemensmorris.game.engine.NineMensMorrisBoard
import com.devfigas.ninemensmorris.game.engine.NineMensMorrisMove
import com.devfigas.ninemensmorris.game.engine.NineMensMorrisRules
import com.devfigas.ninemensmorris.game.engine.PlayerColor
import com.devfigas.ninemensmorris.game.state.NineMensMorrisGameState
import kotlin.math.sqrt

class NineMensMorrisBoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val PIECE_RADIUS_RATIO = 0.055f
        private const val DOT_RADIUS_RATIO = 0.012f
        private const val SELECTED_RADIUS_RATIO = 0.065f
        private const val LINE_WIDTH_RATIO = 0.005f
        private const val TOUCH_THRESHOLD_RATIO = 0.07f

        private const val ANIM_DURATION_PLACE = 300L
        private const val ANIM_DURATION_MOVE = 350L

        // Checkers-style piece colors
        private val COLOR_RED_FILL = Color.parseColor("#8B0000")      // dark red
        private val COLOR_RED_STROKE = Color.parseColor("#5C0000")    // darker red
        private val COLOR_BLUE_FILL = Color.parseColor("#F5E6D3")     // cream/off-white
        private val COLOR_BLUE_STROKE = Color.parseColor("#C4B5A3")   // tan/brown
        private val COLOR_HIGHLIGHT = Color.parseColor("#40FFFFFF")    // semi-transparent white
        private val COLOR_SHADOW = Color.parseColor("#40000000")      // semi-transparent black

        private val COLOR_BOARD_LINE = Color.rgb(180, 180, 180)
        private val COLOR_DOT = Color.rgb(160, 160, 160)
        private val COLOR_SELECTED = Color.rgb(255, 235, 59)
        private val COLOR_VALID_TARGET = Color.rgb(76, 175, 80)
        private val COLOR_VALID_TARGET_RING = Color.argb(100, 76, 175, 80)
        private val COLOR_REMOVABLE = Color.rgb(255, 87, 34)
        private val COLOR_REMOVABLE_RING = Color.argb(100, 255, 87, 34)
        private val COLOR_BACKGROUND = Color.rgb(30, 30, 50)
        private val COLOR_LAST_MOVE = Color.argb(120, 171, 71, 188)
        private val COLOR_MILL = Color.argb(80, 255, 215, 0)
        private val COLOR_DRAG_PIECE = Color.argb(80, 200, 200, 200)

        // Position coords on a 0-6 normalized grid
        private val POS_COORDS: Array<FloatArray> = arrayOf(
            floatArrayOf(0f, 0f),   // 0
            floatArrayOf(3f, 0f),   // 1
            floatArrayOf(6f, 0f),   // 2
            floatArrayOf(1f, 1f),   // 3
            floatArrayOf(3f, 1f),   // 4
            floatArrayOf(5f, 1f),   // 5
            floatArrayOf(2f, 2f),   // 6
            floatArrayOf(3f, 2f),   // 7
            floatArrayOf(4f, 2f),   // 8
            floatArrayOf(0f, 3f),   // 9
            floatArrayOf(1f, 3f),   // 10
            floatArrayOf(2f, 3f),   // 11
            floatArrayOf(4f, 3f),   // 12
            floatArrayOf(5f, 3f),   // 13
            floatArrayOf(6f, 3f),   // 14
            floatArrayOf(2f, 4f),   // 15
            floatArrayOf(3f, 4f),   // 16
            floatArrayOf(4f, 4f),   // 17
            floatArrayOf(1f, 5f),   // 18
            floatArrayOf(3f, 5f),   // 19
            floatArrayOf(5f, 5f),   // 20
            floatArrayOf(0f, 6f),   // 21
            floatArrayOf(3f, 6f),   // 22
            floatArrayOf(6f, 6f)    // 23
        )

        // Board lines (pairs of position indices to draw)
        private val BOARD_LINES: Array<IntArray> = arrayOf(
            // Outer square
            intArrayOf(0, 1), intArrayOf(1, 2), intArrayOf(2, 14), intArrayOf(14, 23),
            intArrayOf(23, 22), intArrayOf(22, 21), intArrayOf(21, 9), intArrayOf(9, 0),
            // Middle square
            intArrayOf(3, 4), intArrayOf(4, 5), intArrayOf(5, 13), intArrayOf(13, 20),
            intArrayOf(20, 19), intArrayOf(19, 18), intArrayOf(18, 10), intArrayOf(10, 3),
            // Inner square
            intArrayOf(6, 7), intArrayOf(7, 8), intArrayOf(8, 12), intArrayOf(12, 17),
            intArrayOf(17, 16), intArrayOf(16, 15), intArrayOf(15, 11), intArrayOf(11, 6),
            // Cross connections
            intArrayOf(1, 4), intArrayOf(4, 7),
            intArrayOf(9, 10), intArrayOf(10, 11),
            intArrayOf(12, 13), intArrayOf(13, 14),
            intArrayOf(16, 19), intArrayOf(19, 22)
        )
    }

    private var board: NineMensMorrisBoard? = null
    private var lastMove: NineMensMorrisMove? = null
    private var isMyTurn: Boolean = false
    private var mustRemove: Boolean = false
    private var currentTurnColor: PlayerColor? = null
    private var myColor: PlayerColor = PlayerColor.RED
    private var onBoardAction: ((from: Int, to: Int) -> Unit)? = null

    // Selection state
    private var selectedPosition: Int = -1
    private var isDragging: Boolean = false
    private var dragCurrentX: Float = 0f
    private var dragCurrentY: Float = 0f

    // Calculated dimensions
    private var cellSize: Float = 0f
    private var boardOffsetX: Float = 0f
    private var boardOffsetY: Float = 0f

    // Screen coordinates for each position
    private val screenCoords = Array(NineMensMorrisBoard.POSITION_COUNT) { floatArrayOf(0f, 0f) }

    // --- Animation state ---
    // Fade-in: position -> alpha (0f..1f)
    private val fadeAnimations = mutableMapOf<Int, Float>()
    // Move: animated piece sliding from one position to another
    private var moveAnimFromPos: Int = -1
    private var moveAnimToPos: Int = -1
    private var moveAnimProgress: Float = 1f  // 0f = at from, 1f = at to (done)
    private var moveAnimColor: PlayerColor? = null
    private var currentAnimator: ValueAnimator? = null

    // Paints
    private val backgroundPaint = Paint().apply { color = COLOR_BACKGROUND }
    private val boardLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_BOARD_LINE
        strokeCap = Paint.Cap.ROUND
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_DOT }

    // Checkers-style piece paints
    private val redFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_RED_FILL; style = Paint.Style.FILL }
    private val redStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_RED_STROKE; style = Paint.Style.STROKE }
    private val blueFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_BLUE_FILL; style = Paint.Style.FILL }
    private val blueStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_BLUE_STROKE; style = Paint.Style.STROKE }
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_HIGHLIGHT; style = Paint.Style.FILL }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_SHADOW; style = Paint.Style.FILL }

    private val selectedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_SELECTED
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val validTargetPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_VALID_TARGET }
    private val validTargetRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_VALID_TARGET_RING
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val removablePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_REMOVABLE
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val removableRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_REMOVABLE_RING
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val lastMovePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_LAST_MOVE }
    private val millPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_MILL
        strokeCap = Paint.Cap.ROUND
    }
    private val dragGhostPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_DRAG_PIECE; style = Paint.Style.FILL }

    /**
     * Returns a rect in window coordinates for the given board position, or
     * null if the layout is not ready. Used by the tutorial overlay to
     * highlight a specific node.
     */
    fun getPositionRectInWindow(position: Int): android.graphics.RectF? {
        if (cellSize == 0f || position !in 0 until NineMensMorrisBoard.POSITION_COUNT) return null
        val radius = width * PIECE_RADIUS_RATIO * 1.6f
        val cx = screenCoords[position][0]
        val cy = screenCoords[position][1]
        val loc = IntArray(2)
        getLocationInWindow(loc)
        return android.graphics.RectF(
            loc[0] + cx - radius,
            loc[1] + cy - radius,
            loc[0] + cx + radius,
            loc[1] + cy + radius
        )
    }

    fun setOnBoardActionListener(listener: (from: Int, to: Int) -> Unit) {
        onBoardAction = listener
    }

    fun updateFromState(state: NineMensMorrisGameState) {
        val oldBoard = board
        val newBoard = state.board
        val newMove = state.moveHistory.lastOrNull()

        board = newBoard
        isMyTurn = state.isMyTurn()
        mustRemove = state.mustRemove
        currentTurnColor = state.currentTurn
        myColor = state.myColor
        lastMove = newMove
        selectedPosition = -1
        isDragging = false

        // Determine animation from the latest move
        currentAnimator?.cancel()
        currentAnimator = null

        if (newMove != null && oldBoard != null) {
            when (newMove.type) {
                NineMensMorrisMove.MoveType.PLACE -> {
                    startFadeInAnimation(newMove.to)
                    return
                }
                NineMensMorrisMove.MoveType.MOVE, NineMensMorrisMove.MoveType.FLY -> {
                    startMoveAnimation(newMove.from, newMove.to, newMove.player)
                    return
                }
                NineMensMorrisMove.MoveType.REMOVE -> {
                    // No special animation for removal — just redraw
                }
            }
        }

        invalidate()
    }

    private fun startFadeInAnimation(position: Int) {
        fadeAnimations[position] = 0f
        moveAnimFromPos = -1

        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = ANIM_DURATION_PLACE
            interpolator = DecelerateInterpolator()
            addUpdateListener { anim ->
                fadeAnimations[position] = anim.animatedValue as Float
                invalidate()
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    fadeAnimations.remove(position)
                    invalidate()
                }
            })
        }
        currentAnimator = animator
        animator.start()
    }

    private fun startMoveAnimation(from: Int, to: Int, player: PlayerColor) {
        moveAnimFromPos = from
        moveAnimToPos = to
        moveAnimProgress = 0f
        moveAnimColor = player
        fadeAnimations.clear()

        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = ANIM_DURATION_MOVE
            interpolator = DecelerateInterpolator()
            addUpdateListener { anim ->
                moveAnimProgress = anim.animatedValue as Float
                invalidate()
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    moveAnimFromPos = -1
                    moveAnimToPos = -1
                    moveAnimProgress = 1f
                    moveAnimColor = null
                    invalidate()
                }
            })
        }
        currentAnimator = animator
        animator.start()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(width, width) // Square board
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val b = board ?: return

        calculateDimensions()

        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        drawBoardLines(canvas)
        drawMills(canvas, b)
        drawLastMove(canvas, b)
        drawValidTargets(canvas, b)
        drawPositionDots(canvas, b)
        drawPieces(canvas, b)
        drawMoveAnimation(canvas)
        drawSelection(canvas, b)
        drawDragPreview(canvas, b)
    }

    private fun calculateDimensions() {
        val padding = width * 0.08f
        val availableSize = width - 2 * padding
        cellSize = availableSize / 6f
        boardOffsetX = padding
        boardOffsetY = padding

        for (i in 0 until NineMensMorrisBoard.POSITION_COUNT) {
            screenCoords[i][0] = boardOffsetX + POS_COORDS[i][0] * cellSize
            screenCoords[i][1] = boardOffsetY + POS_COORDS[i][1] * cellSize
        }
    }

    private fun drawBoardLines(canvas: Canvas) {
        boardLinePaint.strokeWidth = width * LINE_WIDTH_RATIO * 2f

        for (line in BOARD_LINES) {
            val from = line[0]
            val to = line[1]
            canvas.drawLine(
                screenCoords[from][0], screenCoords[from][1],
                screenCoords[to][0], screenCoords[to][1],
                boardLinePaint
            )
        }
    }

    private fun drawMills(canvas: Canvas, board: NineMensMorrisBoard) {
        millPaint.strokeWidth = width * LINE_WIDTH_RATIO * 6f

        for (mill in NineMensMorrisBoard.MILLS) {
            val owner = board.getPosition(mill[0])
            if (owner != null && mill.all { board.getPosition(it) == owner }) {
                canvas.drawLine(
                    screenCoords[mill[0]][0], screenCoords[mill[0]][1],
                    screenCoords[mill[2]][0], screenCoords[mill[2]][1],
                    millPaint
                )
            }
        }
    }

    private fun drawLastMove(canvas: Canvas, board: NineMensMorrisBoard) {
        val move = lastMove ?: return
        if (move.type == NineMensMorrisMove.MoveType.REMOVE) return

        val targetPos = move.to
        val pieceRadius = width * PIECE_RADIUS_RATIO
        lastMovePaint.style = Paint.Style.FILL
        canvas.drawCircle(screenCoords[targetPos][0], screenCoords[targetPos][1], pieceRadius * 1.5f, lastMovePaint)
    }

    private fun drawValidTargets(canvas: Canvas, board: NineMensMorrisBoard) {
        val turnColor = currentTurnColor ?: return
        val pieceRadius = width * PIECE_RADIUS_RATIO

        if (mustRemove && isMyTurn) {
            val removals = NineMensMorrisRules.getValidRemovals(board, turnColor)
            removablePaint.strokeWidth = pieceRadius * 0.25f
            removableRingPaint.strokeWidth = pieceRadius * 0.15f
            for (move in removals) {
                val pos = move.to
                canvas.drawCircle(screenCoords[pos][0], screenCoords[pos][1], pieceRadius * 1.4f, removableRingPaint)
                canvas.drawCircle(screenCoords[pos][0], screenCoords[pos][1], pieceRadius * 1.2f, removablePaint)
            }
        } else if (selectedPosition >= 0 && isMyTurn) {
            val validTargets = getValidTargetsForSelected(board, turnColor)
            validTargetRingPaint.strokeWidth = pieceRadius * 0.15f
            for (pos in validTargets) {
                canvas.drawCircle(screenCoords[pos][0], screenCoords[pos][1], pieceRadius * 1.3f, validTargetRingPaint)
                canvas.drawCircle(screenCoords[pos][0], screenCoords[pos][1], pieceRadius * 0.5f, validTargetPaint)
            }
        } else if (!mustRemove && board.isPlacingPhase() && board.piecesInHand(turnColor) > 0 && isMyTurn) {
            val dotRadius = width * DOT_RADIUS_RATIO * 2f
            for (pos in board.getEmptyPositions()) {
                canvas.drawCircle(screenCoords[pos][0], screenCoords[pos][1], dotRadius, validTargetPaint)
            }
        }
    }

    private fun drawPositionDots(canvas: Canvas, board: NineMensMorrisBoard) {
        val dotRadius = width * DOT_RADIUS_RATIO

        for (i in 0 until NineMensMorrisBoard.POSITION_COUNT) {
            if (board.isEmpty(i)) {
                canvas.drawCircle(screenCoords[i][0], screenCoords[i][1], dotRadius, dotPaint)
            }
        }
    }

    private fun drawCheckersPiece(canvas: Canvas, cx: Float, cy: Float, owner: PlayerColor, alpha: Int = 255) {
        val pieceRadius = width * PIECE_RADIUS_RATIO
        val innerRadius = pieceRadius * 0.82f
        val shadowOffsetX = pieceRadius * 0.08f
        val shadowOffsetY = pieceRadius * 0.12f

        val fillPaint = if (owner == PlayerColor.RED) redFillPaint else blueFillPaint
        val strokePaint = if (owner == PlayerColor.RED) redStrokePaint else blueStrokePaint
        strokePaint.strokeWidth = (pieceRadius * 0.12f).coerceAtLeast(1.5f)

        // Apply alpha to all paints
        val prevFillAlpha = fillPaint.alpha
        val prevStrokeAlpha = strokePaint.alpha
        val prevHighlightAlpha = highlightPaint.alpha
        val prevShadowAlpha = shadowPaint.alpha

        fillPaint.alpha = alpha
        strokePaint.alpha = alpha
        highlightPaint.alpha = (0x40 * alpha / 255)
        shadowPaint.alpha = (0x40 * alpha / 255)

        // 1. Shadow
        canvas.drawCircle(cx + shadowOffsetX, cy + shadowOffsetY, pieceRadius, shadowPaint)

        // 2. Main piece circle
        canvas.drawCircle(cx, cy, pieceRadius, fillPaint)

        // 3. Inner highlight (slightly offset up for 3D sphere effect)
        canvas.drawCircle(cx, cy - pieceRadius * 0.04f, innerRadius, highlightPaint)

        // 4. Outer border
        canvas.drawCircle(cx, cy, pieceRadius, strokePaint)

        // 5. Inner border
        canvas.drawCircle(cx, cy, innerRadius, strokePaint)

        // Restore original alpha values
        fillPaint.alpha = prevFillAlpha
        strokePaint.alpha = prevStrokeAlpha
        highlightPaint.alpha = prevHighlightAlpha
        shadowPaint.alpha = prevShadowAlpha
    }

    private fun drawPieces(canvas: Canvas, board: NineMensMorrisBoard) {
        for (i in 0 until NineMensMorrisBoard.POSITION_COUNT) {
            val owner = board.getPosition(i) ?: continue

            // Skip piece being dragged
            if (isDragging && i == selectedPosition) continue

            // Skip piece at move-animation destination (will be drawn in drawMoveAnimation)
            if (moveAnimFromPos >= 0 && i == moveAnimToPos && moveAnimProgress < 1f) continue

            val cx = screenCoords[i][0]
            val cy = screenCoords[i][1]

            // Fade-in animation
            val fadeAlpha = fadeAnimations[i]
            val alpha = if (fadeAlpha != null) (fadeAlpha * 255).toInt().coerceIn(0, 255) else 255

            drawCheckersPiece(canvas, cx, cy, owner, alpha)
        }
    }

    private fun drawMoveAnimation(canvas: Canvas) {
        if (moveAnimFromPos < 0 || moveAnimProgress >= 1f) return
        val color = moveAnimColor ?: return

        val fromX = screenCoords[moveAnimFromPos][0]
        val fromY = screenCoords[moveAnimFromPos][1]
        val toX = screenCoords[moveAnimToPos][0]
        val toY = screenCoords[moveAnimToPos][1]

        val currentX = fromX + (toX - fromX) * moveAnimProgress
        val currentY = fromY + (toY - fromY) * moveAnimProgress

        drawCheckersPiece(canvas, currentX, currentY, color)
    }

    private fun drawSelection(canvas: Canvas, board: NineMensMorrisBoard) {
        if (selectedPosition < 0 || isDragging) return
        val pieceRadius = width * PIECE_RADIUS_RATIO
        selectedPaint.strokeWidth = pieceRadius * 0.25f
        canvas.drawCircle(
            screenCoords[selectedPosition][0],
            screenCoords[selectedPosition][1],
            pieceRadius * 1.3f,
            selectedPaint
        )
    }

    private fun drawDragPreview(canvas: Canvas, board: NineMensMorrisBoard) {
        if (!isDragging || selectedPosition < 0) return
        val owner = board.getPosition(selectedPosition) ?: return

        // Draw ghost at original position
        val pieceRadius = width * PIECE_RADIUS_RATIO
        dragGhostPaint.alpha = 80
        canvas.drawCircle(screenCoords[selectedPosition][0], screenCoords[selectedPosition][1], pieceRadius, dragGhostPaint)

        // Draw piece at drag position
        drawCheckersPiece(canvas, dragCurrentX, dragCurrentY, owner)
    }

    private fun getValidTargetsForSelected(board: NineMensMorrisBoard, player: PlayerColor): Set<Int> {
        if (selectedPosition < 0) return emptySet()
        val canFly = board.canFly(player)
        val targets = mutableSetOf<Int>()

        if (canFly) {
            targets.addAll(board.getEmptyPositions())
        } else {
            for (adj in board.getAdjacentPositions(selectedPosition)) {
                if (board.isEmpty(adj)) targets.add(adj)
            }
        }
        return targets
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val b = board ?: return true
        if (cellSize <= 0) return true

        val touchX = event.x
        val touchY = event.y
        val threshold = width * TOUCH_THRESHOLD_RATIO

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val pos = findPositionAt(touchX, touchY, threshold)
                if (pos < 0) return true

                val turnColor = currentTurnColor ?: return true

                if (mustRemove && isMyTurn) {
                    val removals = NineMensMorrisRules.getValidRemovals(b, turnColor)
                    if (removals.any { it.to == pos }) {
                        onBoardAction?.invoke(-1, pos)
                    }
                    return true
                }

                if (b.isPlacingPhase() && b.piecesInHand(turnColor) > 0 && isMyTurn) {
                    if (b.isEmpty(pos)) {
                        onBoardAction?.invoke(-1, pos)
                    }
                    return true
                }

                // Movement phase
                if (isMyTurn) {
                    if (selectedPosition >= 0) {
                        val validTargets = getValidTargetsForSelected(b, turnColor)
                        if (pos in validTargets) {
                            onBoardAction?.invoke(selectedPosition, pos)
                            selectedPosition = -1
                            isDragging = false
                            invalidate()
                            return true
                        }
                    }

                    if (b.getPosition(pos) == turnColor) {
                        selectedPosition = pos
                        isDragging = true
                        dragCurrentX = touchX
                        dragCurrentY = touchY
                        invalidate()
                    } else {
                        selectedPosition = -1
                        isDragging = false
                        invalidate()
                    }
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (isDragging && selectedPosition >= 0) {
                    dragCurrentX = touchX
                    dragCurrentY = touchY
                    invalidate()
                }
            }

            MotionEvent.ACTION_UP -> {
                if (isDragging && selectedPosition >= 0) {
                    val targetPos = findPositionAt(touchX, touchY, threshold)
                    if (targetPos >= 0 && targetPos != selectedPosition) {
                        val turnColor = currentTurnColor ?: return true
                        val validTargets = getValidTargetsForSelected(b, turnColor)
                        if (targetPos in validTargets) {
                            onBoardAction?.invoke(selectedPosition, targetPos)
                            selectedPosition = -1
                            isDragging = false
                            invalidate()
                            return true
                        }
                    }
                    isDragging = false
                    invalidate()
                }
            }

            MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                invalidate()
            }
        }

        return true
    }

    private fun findPositionAt(x: Float, y: Float, threshold: Float): Int {
        var bestPos = -1
        var bestDist = Float.MAX_VALUE

        for (i in 0 until NineMensMorrisBoard.POSITION_COUNT) {
            val dist = distance(x, y, screenCoords[i][0], screenCoords[i][1])
            if (dist < threshold && dist < bestDist) {
                bestDist = dist
                bestPos = i
            }
        }
        return bestPos
    }

    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x1 - x2
        val dy = y1 - y2
        return sqrt(dx * dx + dy * dy)
    }
}
