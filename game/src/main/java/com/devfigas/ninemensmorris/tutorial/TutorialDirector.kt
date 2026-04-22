package com.devfigas.ninemensmorris.tutorial

import android.app.Activity
import android.graphics.RectF
import com.devfigas.ninemensmorris.R
import com.devfigas.ninemensmorris.game.state.NineMensMorrisGamePhase
import com.devfigas.ninemensmorris.game.state.NineMensMorrisGameState
import com.devfigas.ninemensmorris.ui.NineMensMorrisBoardView
import ui.devfigas.uikit.tutorial.TutorialOverlayView

/**
 * Nine Men's Morris tutorial — placement phase only. Player is RED.
 *
 * Script:
 *   P1 place at 0
 *   AI  place at 5
 *   P2 place at 1          (threatens top-row mill 0-1-2)
 *   AI  place at 2         (BLOCK the top-row mill)
 *   P3 place at 4          (pivot: cross-mill 1-4-7)
 *   AI  place at 13        (random, misses the block)
 *   P4 place at 7          (mill formed 1-4-7, would trigger capture)
 *
 * Tutorial ends at the congrats step once the mill is formed. The engine
 * will enter the "must remove" sub-phase, but the tutorial exits the
 * activity before the player has to act on it.
 */
class TutorialDirector(
    private val activity: Activity,
    private val overlay: TutorialOverlayView,
    private val boardView: NineMensMorrisBoardView,
    private val placeAt: (Int) -> Unit,
    private val onFinished: () -> Unit
) {
    companion object {
        val AI_PLACEMENTS: List<Int> = listOf(5, 2, 13)
        val PLAYER_PLACEMENTS: List<Int> = listOf(0, 1, 4, 7)
    }

    private enum class Advance { BUTTON, MOVE }

    private data class Step(
        val messageRes: Int,
        val buttonRes: Int,
        val advance: Advance,
        val targetPos: Int? = null,
        val balloonSide: TutorialOverlayView.BalloonSide = TutorialOverlayView.BalloonSide.BOTTOM
    )

    private val steps: List<Step> = listOf(
        Step(R.string.tutorial_welcome, R.string.tutorial_next, Advance.BUTTON),
        Step(R.string.tutorial_objective, R.string.tutorial_next, Advance.BUTTON),
        Step(R.string.tutorial_first_move, R.string.tutorial_next, Advance.MOVE,
            targetPos = PLAYER_PLACEMENTS[0], balloonSide = TutorialOverlayView.BalloonSide.BOTTOM),
        Step(R.string.tutorial_second_move, R.string.tutorial_next, Advance.MOVE,
            targetPos = PLAYER_PLACEMENTS[1], balloonSide = TutorialOverlayView.BalloonSide.BOTTOM),
        Step(R.string.tutorial_third_move, R.string.tutorial_next, Advance.MOVE,
            targetPos = PLAYER_PLACEMENTS[2], balloonSide = TutorialOverlayView.BalloonSide.BOTTOM),
        Step(R.string.tutorial_winning_move, R.string.tutorial_next, Advance.MOVE,
            targetPos = PLAYER_PLACEMENTS[3], balloonSide = TutorialOverlayView.BalloonSide.TOP),
        Step(R.string.tutorial_congrats, R.string.tutorial_play, Advance.BUTTON)
    )

    private var currentStepIndex = 0
    private var lastObservedMoveCount = -1
    private var finished = false

    fun start() {
        currentStepIndex = 0
        showCurrentStep()
    }

    fun onGameStateChanged(state: NineMensMorrisGameState) {
        if (finished) return
        val moveCount = state.moveHistory.size

        if (state.phase == NineMensMorrisGamePhase.GAME_OVER) {
            currentStepIndex = steps.size - 1
            showCurrentStep()
            return
        }

        if (moveCount != lastObservedMoveCount) {
            lastObservedMoveCount = moveCount
            val step = currentStep()
            if (step?.advance == Advance.MOVE && moveCount > 0 && state.currentTurn == state.myColor) {
                advance()
                return
            }
        }

        if (state.currentTurn == state.myColor && currentStep()?.advance == Advance.MOVE) {
            showCurrentStep()
        } else if (state.currentTurn != state.myColor && state.phase == NineMensMorrisGamePhase.PLAYING) {
            overlay.hide()
        }
    }

    fun allowedPositions(): Set<Int>? {
        val step = currentStep() ?: return emptySet()
        if (step.advance != Advance.MOVE) return emptySet()
        val pos = step.targetPos ?: return null
        return setOf(pos)
    }

    fun onSkipRequested() {
        finish()
    }

    private fun currentStep(): Step? = steps.getOrNull(currentStepIndex)

    private fun advance() {
        currentStepIndex++
        if (currentStepIndex >= steps.size) {
            finish()
            return
        }
        showCurrentStep()
    }

    private fun showCurrentStep() {
        val step = currentStep() ?: return
        val message = activity.getString(step.messageRes)
        val buttonLabel = if (step.advance == Advance.BUTTON) activity.getString(step.buttonRes) else null
        val isLast = currentStepIndex == steps.size - 1
        val skipLabel = if (isLast) null else activity.getString(R.string.tutorial_skip_tutorial)

        val targetRect = step.targetPos?.let { boardView.getPositionRectInWindow(it) }
            ?.let { translateToOverlay(it) }

        boardView.post {
            overlay.showStep(
                target = targetRect,
                message = message,
                buttonText = buttonLabel,
                skipButtonText = skipLabel,
                showArrow = false,
                balloonSide = step.balloonSide,
                blockOutsideTouches = step.advance == Advance.BUTTON,
                onAdvance = {
                    if (step.advance == Advance.BUTTON) {
                        if (isLast) finish() else advance()
                    }
                },
                onSkip = if (isLast) null else ({ finish() })
            )
        }
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
