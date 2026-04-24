package com.devfigas.ninemensmorris.tutorial

import android.app.Activity
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import com.devfigas.ninemensmorris.R
import com.devfigas.ninemensmorris.game.engine.NineMensMorrisBoard
import com.devfigas.ninemensmorris.game.engine.NineMensMorrisMove
import com.devfigas.ninemensmorris.game.engine.PlayerColor
import com.devfigas.ninemensmorris.game.state.NineMensMorrisGamePhase
import com.devfigas.ninemensmorris.game.state.NineMensMorrisGameState
import com.devfigas.ninemensmorris.ui.NineMensMorrisBoardView
import com.devfigas.mockpvp.model.GameMode
import com.devfigas.mockpvp.model.User
import ui.devfigas.uikit.tutorial.TutorialOverlayView
import java.util.UUID

/**
 * Scripted tutorial that walks the player through the three core Nine Men's
 * Morris use cases: place + form a mill, capture, and move a piece to
 * re-form a mill + capture again. The move-phase scene is served by a
 * pre-built board state so the player doesn't have to play through 18
 * placements first.
 *
 * Scene A — placement phase (player = RED):
 *   Player at 0 (top-left outer)
 *   AI    at 17 (non-blocking inner)
 *   Player at 1 (top-middle outer)
 *   AI    at 13 (non-blocking)
 *   Player at 2 (top-right outer) → mill 0-1-2
 *   Player removes AI piece at 17
 *
 * Scene B — movement phase (state injected):
 *   RED at 3, 5, 7, 18  (4 on board; can't fly)
 *   BLUE at 0, 2, 8, 11 (4 on board)
 *   Player moves 7 → 4 → mill 3-4-5
 *   Player removes BLUE piece at 0
 *
 * Congrats and exit.
 */
class TutorialDirector(
    private val activity: Activity,
    private val overlay: TutorialOverlayView,
    private val boardView: NineMensMorrisBoardView,
    private val myColor: PlayerColor,
    private val opponent: User?,
    private val onForceState: (NineMensMorrisGameState) -> Unit,
    private val onFinished: () -> Unit
) {
    companion object {
        val AI_PLACEMENTS: List<Int> = listOf(17, 13)

        private const val SCENE_B_BOARD_ENCODING = "B.BR.R.RB..B......R.....|0|0"

        // Scripted Scene A placement targets.
        private const val PLACE_FIRST_POS = 0
        private const val PLACE_SECOND_POS = 1
        private const val PLACE_MILL_POS = 2
        private const val CAPTURE_FIRST_POS = 17

        // Scripted Scene B move + capture targets.
        private const val MOVE_SOURCE_POS = 7
        private const val MOVE_TARGET_POS = 4
        private const val CAPTURE_SECOND_POS = 0

        // Transition pacing: let the player see their last move before
        // the overlay comes back with the next hint.
        private const val TRANSITION_DELAY_MS = 650L
        private const val SCENE_B_INJECT_DELAY_MS = 900L
    }

    private enum class Step {
        WELCOME,
        RULES_INTRO,
        PLACE_FIRST,
        OPP_PLACES_1,
        PLACE_SECOND,
        OPP_PLACES_2,
        PLACE_MILL,
        CAPTURE_FIRST,
        MOVE_PHASE_INTRO,
        MOVE_FORMATION,
        CAPTURE_SECOND,
        CONGRATS
    }

    private var current: Step = Step.WELCOME
    private var finished = false
    private var lastMoveCount: Int = 0
    private val handler = Handler(Looper.getMainLooper())

    fun start() {
        current = Step.WELCOME
        lastMoveCount = 0
        showCurrent()
    }

    /**
     * Target positions the player is allowed to tap as the destination of a
     * move (placement `to`, movement `to`, or capture target). `null` means
     * any position is allowed; empty set means no tap should advance.
     */
    fun allowedPositions(): Set<Int>? {
        if (finished) return null
        return when (current) {
            Step.PLACE_FIRST -> setOf(PLACE_FIRST_POS)
            Step.PLACE_SECOND -> setOf(PLACE_SECOND_POS)
            Step.PLACE_MILL -> setOf(PLACE_MILL_POS)
            Step.CAPTURE_FIRST -> setOf(CAPTURE_FIRST_POS)
            Step.MOVE_FORMATION -> setOf(MOVE_TARGET_POS)
            Step.CAPTURE_SECOND -> setOf(CAPTURE_SECOND_POS)
            else -> emptySet()
        }
    }

    /**
     * Source positions the player is allowed to drag from during movement.
     * `null` = no restriction (used outside move steps where source is -1).
     */
    fun allowedSources(): Set<Int>? {
        if (finished) return null
        return when (current) {
            Step.MOVE_FORMATION -> setOf(MOVE_SOURCE_POS)
            else -> null
        }
    }

    fun onSkipRequested() { finish() }

    fun onGameStateChanged(state: NineMensMorrisGameState) {
        if (finished) return
        if (state.phase == NineMensMorrisGamePhase.GAME_OVER) {
            advanceTo(Step.CONGRATS)
            return
        }
        val moveCount = state.moveHistory.size
        val lastMove = state.moveHistory.lastOrNull()
        val moveAdvanced = moveCount > lastMoveCount
        lastMoveCount = moveCount

        when (current) {
            Step.PLACE_FIRST -> {
                if (moveAdvanced && lastMove?.type == NineMensMorrisMove.MoveType.PLACE &&
                    lastMove.player == myColor && lastMove.to == PLACE_FIRST_POS) {
                    advanceTo(Step.OPP_PLACES_1)
                }
            }
            Step.OPP_PLACES_1 -> {
                if (moveAdvanced && lastMove?.player == myColor.opposite()) {
                    advanceTo(Step.PLACE_SECOND)
                }
            }
            Step.PLACE_SECOND -> {
                if (moveAdvanced && lastMove?.type == NineMensMorrisMove.MoveType.PLACE &&
                    lastMove.player == myColor && lastMove.to == PLACE_SECOND_POS) {
                    advanceTo(Step.OPP_PLACES_2)
                }
            }
            Step.OPP_PLACES_2 -> {
                if (moveAdvanced && lastMove?.player == myColor.opposite()) {
                    advanceTo(Step.PLACE_MILL)
                }
            }
            Step.PLACE_MILL -> {
                if (moveAdvanced && lastMove?.type == NineMensMorrisMove.MoveType.PLACE &&
                    lastMove.player == myColor && lastMove.to == PLACE_MILL_POS) {
                    advanceTo(Step.CAPTURE_FIRST)
                }
            }
            Step.CAPTURE_FIRST -> {
                if (moveAdvanced && lastMove?.type == NineMensMorrisMove.MoveType.REMOVE &&
                    lastMove.player == myColor) {
                    advanceTo(Step.MOVE_PHASE_INTRO)
                }
            }
            Step.MOVE_FORMATION -> {
                if (moveAdvanced && (lastMove?.type == NineMensMorrisMove.MoveType.MOVE ||
                        lastMove?.type == NineMensMorrisMove.MoveType.FLY) &&
                    lastMove.player == myColor && lastMove.to == MOVE_TARGET_POS) {
                    advanceTo(Step.CAPTURE_SECOND)
                }
            }
            Step.CAPTURE_SECOND -> {
                if (moveAdvanced && lastMove?.type == NineMensMorrisMove.MoveType.REMOVE &&
                    lastMove.player == myColor) {
                    advanceTo(Step.CONGRATS)
                }
            }
            else -> {}
        }
    }

    private fun advanceTo(step: Step) {
        current = step
        overlay.hide()
        if (step == Step.MOVE_PHASE_INTRO) {
            handler.postDelayed({
                if (finished) return@postDelayed
                onForceState(buildMoveScene())
                lastMoveCount = 0
                showCurrent()
            }, SCENE_B_INJECT_DELAY_MS)
            return
        }
        handler.postDelayed({
            if (finished) return@postDelayed
            showCurrent()
        }, TRANSITION_DELAY_MS)
    }

    private fun showCurrent() {
        if (finished) return
        boardView.post { emitForStep(current) }
    }

    private fun emitForStep(step: Step) {
        when (step) {
            Step.WELCOME -> emitCentered(
                messageRes = R.string.tutorial_welcome,
                buttonRes = R.string.tutorial_next,
                onAdvance = { advanceTo(Step.RULES_INTRO) }
            )
            Step.RULES_INTRO -> emitCentered(
                messageRes = R.string.tutorial_rules_intro,
                buttonRes = R.string.tutorial_next,
                onAdvance = { advanceTo(Step.PLACE_FIRST) }
            )
            Step.PLACE_FIRST -> emitSpotlight(
                messageRes = R.string.tutorial_place_first,
                pos = PLACE_FIRST_POS,
                balloonSide = TutorialOverlayView.BalloonSide.BOTTOM,
                advanceOnTap = true
            )
            Step.OPP_PLACES_1, Step.OPP_PLACES_2 -> emitCentered(
                messageRes = R.string.tutorial_opp_places,
                buttonRes = null,
                onAdvance = null
            )
            Step.PLACE_SECOND -> emitSpotlight(
                messageRes = R.string.tutorial_place_second,
                pos = PLACE_SECOND_POS,
                balloonSide = TutorialOverlayView.BalloonSide.BOTTOM,
                advanceOnTap = true
            )
            Step.PLACE_MILL -> emitSpotlight(
                messageRes = R.string.tutorial_place_mill,
                pos = PLACE_MILL_POS,
                balloonSide = TutorialOverlayView.BalloonSide.BOTTOM,
                advanceOnTap = true
            )
            Step.CAPTURE_FIRST -> emitSpotlight(
                messageRes = R.string.tutorial_capture_first,
                pos = CAPTURE_FIRST_POS,
                balloonSide = TutorialOverlayView.BalloonSide.TOP,
                advanceOnTap = true
            )
            Step.MOVE_PHASE_INTRO -> emitCentered(
                messageRes = R.string.tutorial_move_phase_intro,
                buttonRes = R.string.tutorial_next,
                onAdvance = { advanceTo(Step.MOVE_FORMATION) }
            )
            Step.MOVE_FORMATION -> emitSpotlightUnion(
                messageRes = R.string.tutorial_move_formation,
                positions = listOf(MOVE_SOURCE_POS, MOVE_TARGET_POS),
                balloonSide = TutorialOverlayView.BalloonSide.BOTTOM
            )
            Step.CAPTURE_SECOND -> emitSpotlight(
                messageRes = R.string.tutorial_capture_second,
                pos = CAPTURE_SECOND_POS,
                balloonSide = TutorialOverlayView.BalloonSide.BOTTOM,
                advanceOnTap = true
            )
            Step.CONGRATS -> emitCentered(
                messageRes = R.string.tutorial_congrats,
                buttonRes = R.string.tutorial_play,
                showSkip = false,
                onAdvance = { finish() }
            )
        }
    }

    private fun emitCentered(
        messageRes: Int,
        buttonRes: Int?,
        showSkip: Boolean = true,
        onAdvance: (() -> Unit)?
    ) {
        overlay.showStep(
            target = null,
            message = activity.getString(messageRes),
            buttonText = buttonRes?.let { activity.getString(it) },
            skipButtonText = if (showSkip) activity.getString(R.string.tutorial_skip_tutorial) else null,
            showArrow = false,
            balloonSide = TutorialOverlayView.BalloonSide.BOTTOM,
            blockOutsideTouches = true,
            onAdvance = { onAdvance?.invoke() },
            onSkip = if (showSkip) ({ finish() }) else null
        )
    }

    private fun emitSpotlight(
        messageRes: Int,
        pos: Int,
        balloonSide: TutorialOverlayView.BalloonSide,
        advanceOnTap: Boolean,
        buttonRes: Int? = null,
        onAdvance: (() -> Unit)? = null,
        showSkip: Boolean = true
    ) {
        val rect = boardView.getPositionRectInWindow(pos)?.let { translateToOverlay(it) }
        overlay.showStep(
            target = rect,
            message = activity.getString(messageRes),
            buttonText = buttonRes?.let { activity.getString(it) },
            skipButtonText = if (showSkip) activity.getString(R.string.tutorial_skip_tutorial) else null,
            showArrow = false,
            balloonSide = balloonSide,
            blockOutsideTouches = !advanceOnTap,
            onAdvance = { onAdvance?.invoke() },
            onSkip = if (showSkip) ({ finish() }) else null
        )
    }

    private fun emitSpotlightUnion(
        messageRes: Int,
        positions: List<Int>,
        balloonSide: TutorialOverlayView.BalloonSide,
        showSkip: Boolean = true
    ) {
        var union: RectF? = null
        for (pos in positions) {
            val r = boardView.getPositionRectInWindow(pos) ?: continue
            union = union?.apply { union(r) } ?: RectF(r)
        }
        val rect = union?.let { translateToOverlay(it) }
        overlay.showStep(
            target = rect,
            message = activity.getString(messageRes),
            buttonText = null,
            skipButtonText = if (showSkip) activity.getString(R.string.tutorial_skip_tutorial) else null,
            showArrow = false,
            balloonSide = balloonSide,
            blockOutsideTouches = true,
            onAdvance = {},
            onSkip = if (showSkip) ({ finish() }) else null
        )
    }

    private fun buildMoveScene(): NineMensMorrisGameState {
        val board = NineMensMorrisBoard.decode(SCENE_B_BOARD_ENCODING)
            ?: error("Tutorial scene B encoding is invalid")
        return NineMensMorrisGameState(
            gameId = UUID.randomUUID().toString(),
            board = board,
            currentTurn = myColor,
            phase = NineMensMorrisGamePhase.PLAYING,
            gameMode = GameMode.CPU,
            myColor = myColor,
            opponent = opponent
        )
    }

    private fun translateToOverlay(windowRect: RectF): RectF {
        val overlayLoc = IntArray(2)
        overlay.getLocationInWindow(overlayLoc)
        return RectF(
            windowRect.left - overlayLoc[0],
            windowRect.top - overlayLoc[1],
            windowRect.right - overlayLoc[0],
            windowRect.bottom - overlayLoc[1]
        )
    }

    private fun finish() {
        if (finished) return
        finished = true
        overlay.hide()
        TutorialPreferences.markCompleted(activity)
        onFinished()
    }
}
