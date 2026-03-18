package com.devfigas.ninemensmorris.e2e.ui

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice

/**
 * UI Automation actions for the Nine Men's Morris game board.
 * Handles piece placement, movement, and board state verification.
 *
 * Nine Men's Morris board has 24 positions arranged in 3 concentric squares
 * connected by lines. The positions map to a logical 7x7 grid where only
 * specific intersections are valid:
 *
 *  0-----------1-----------2       (row 0: cols 0, 3, 6)
 *  |           |           |
 *  |   8-------9------10   |       (row 1: cols 1, 3, 5)
 *  |   |       |       |   |
 *  |   |  16--17--18   |   |       (row 2: cols 2, 3, 4)
 *  |   |   |       |   |   |
 *  7---15--23      19--11---3       (row 3: cols 0,1,2 and 4,5,6)
 *  |   |   |       |   |   |
 *  |   |  22--21--20   |   |       (row 4: cols 2, 3, 4)
 *  |   |       |       |   |
 *  |  14------13------12   |       (row 5: cols 1, 3, 5)
 *  |           |           |
 *  6----------5-----------4       (row 6: cols 0, 3, 6)
 *
 * Position index to grid coordinate mapping:
 *  0=(0,0)  1=(0,3)  2=(0,6)
 *  3=(3,6)  4=(6,6)  5=(6,3)
 *  6=(6,0)  7=(3,0)
 *  8=(1,1)  9=(1,3) 10=(1,5)
 * 11=(3,5) 12=(5,5) 13=(5,3)
 * 14=(5,1) 15=(3,1)
 * 16=(2,2) 17=(2,3) 18=(2,4)
 * 19=(3,4) 20=(4,4) 21=(4,3)
 * 22=(4,2) 23=(3,2)
 */
object GameActions {
    private const val TAG = "GameActions"
    private const val TIMEOUT_MS = 5000L
    private const val PACKAGE_NAME = "com.devfigas.ninemensmorris"

    private val device: UiDevice by lazy {
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    /**
     * Maps position index (0-23) to grid coordinates (row, col) on a 7x7 logical grid.
     */
    private val positionToGrid: Map<Int, Pair<Int, Int>> = mapOf(
        // Outer square
        0 to Pair(0, 0),   1 to Pair(0, 3),   2 to Pair(0, 6),
        3 to Pair(3, 6),   4 to Pair(6, 6),   5 to Pair(6, 3),
        6 to Pair(6, 0),   7 to Pair(3, 0),
        // Middle square
        8 to Pair(1, 1),   9 to Pair(1, 3),  10 to Pair(1, 5),
        11 to Pair(3, 5), 12 to Pair(5, 5),  13 to Pair(5, 3),
        14 to Pair(5, 1), 15 to Pair(3, 1),
        // Inner square
        16 to Pair(2, 2), 17 to Pair(2, 3),  18 to Pair(2, 4),
        19 to Pair(3, 4), 20 to Pair(4, 4),  21 to Pair(4, 3),
        22 to Pair(4, 2), 23 to Pair(3, 2)
    )

    /**
     * Taps a specific position on the Nine Men's Morris board by position index (0-23).
     *
     * @param posIndex The position index (0-23)
     * @return true if the tap was performed successfully
     */
    fun tapPosition(posIndex: Int): Boolean {
        Log.d(TAG, "tapPosition: tapping position $posIndex")

        if (posIndex !in 0..23) {
            Log.e(TAG, "Invalid position index: $posIndex (must be 0-23)")
            return false
        }

        val gridCoord = positionToGrid[posIndex] ?: return false
        return tapBoardAt(gridCoord.first, gridCoord.second)
    }

    /**
     * Taps the board at the specified grid coordinates (0-6, 0-6) on the logical 7x7 grid.
     *
     * @param row Row coordinate (0-6, top to bottom)
     * @param col Column coordinate (0-6, left to right)
     * @return true if the tap was performed successfully
     */
    fun tapBoardAt(row: Int, col: Int): Boolean {
        Log.d(TAG, "tapBoardAt: row=$row, col=$col")

        try {
            val boardView = device.findObject(By.res("$PACKAGE_NAME:id/nine_mens_morris_board_view"))
            if (boardView == null) {
                Log.e(TAG, "Nine Men's Morris board view not found")
                return false
            }

            val bounds = boardView.visibleBounds
            Log.d(TAG, "Board bounds: left=${bounds.left}, top=${bounds.top}, width=${bounds.width()}, height=${bounds.height()}")

            // Calculate tap coordinates based on 7x7 grid
            val x = bounds.left + (col * bounds.width() / 6)
            val y = bounds.top + (row * bounds.height() / 6)

            Log.d(TAG, "Tapping at ($x, $y) for grid position ($row, $col)")
            device.click(x, y)

            // Wait for tap to be processed
            Thread.sleep(300)

            Log.d(TAG, "tapBoardAt: completed")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error tapping board: ${e.message}", e)
            return false
        }
    }

    /**
     * Verifies that the game screen is visible and ready.
     *
     * @return true if the game screen is ready
     */
    fun isGameScreenReady(): Boolean {
        Log.d(TAG, "isGameScreenReady: checking...")

        try {
            val boardView = device.findObject(By.res("$PACKAGE_NAME:id/nine_mens_morris_board_view"))
            val statusView = device.findObject(By.res("$PACKAGE_NAME:id/tv_game_status"))

            val ready = boardView != null && statusView != null
            Log.d(TAG, "Game screen ready: $ready (board=${boardView != null}, status=${statusView != null})")
            return ready
        } catch (e: Exception) {
            Log.e(TAG, "Error checking game screen: ${e.message}", e)
            return false
        }
    }

    /**
     * Waits for the game screen to be ready.
     *
     * @param timeoutMs Maximum time to wait
     * @return true if the game screen became ready within the timeout
     */
    fun waitForGameScreen(timeoutMs: Long = 10000L): Boolean {
        Log.d(TAG, "waitForGameScreen: waiting...")

        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (isGameScreenReady()) {
                return true
            }
            Thread.sleep(500)
        }

        Log.e(TAG, "Timeout waiting for game screen")
        return false
    }

    /**
     * Verifies it's the player's turn by checking the game status text.
     *
     * @return true if it's the player's turn
     */
    fun verifyMyTurn(): Boolean {
        Log.d(TAG, "verifyMyTurn: checking...")

        try {
            val statusText = device.findObject(By.res("$PACKAGE_NAME:id/tv_game_status"))
            if (statusText == null) {
                Log.e(TAG, "Game status text not found")
                return false
            }

            val text = statusText.text?.lowercase() ?: ""
            Log.d(TAG, "Current status text: $text")

            val isMyTurn = text.contains("your") && text.contains("turn")
            Log.d(TAG, "Is my turn: $isMyTurn")
            return isMyTurn
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying turn: ${e.message}", e)
            return false
        }
    }

    /**
     * Waits until it's the player's turn.
     *
     * @param timeoutMs Maximum time to wait
     * @return true if it became the player's turn within the timeout
     */
    fun waitForMyTurn(timeoutMs: Long = 30000L): Boolean {
        Log.d(TAG, "waitForMyTurn: waiting...")

        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (verifyMyTurn()) {
                Log.d(TAG, "It's now my turn")
                return true
            }
            Thread.sleep(500)
        }

        Log.e(TAG, "Timeout waiting for my turn")
        return false
    }

    /**
     * Waits for game over dialog to appear.
     *
     * @param expectedResult "win" or "lose" or null for any result
     * @param timeoutMs Maximum time to wait
     * @return true if game over dialog appeared with expected result
     */
    fun waitForGameOver(expectedResult: String? = null, timeoutMs: Long = 60000L): Boolean {
        Log.d(TAG, "waitForGameOver: expecting $expectedResult, timeout=${timeoutMs}ms")

        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            try {
                val youWin = device.findObject(By.text("You Win!"))
                val youLose = device.findObject(By.text("You Lose!"))

                if (youWin != null) {
                    Log.d(TAG, "Found game over dialog: You Win!")
                    if (expectedResult == null || expectedResult.lowercase() == "win") {
                        return true
                    } else {
                        Log.e(TAG, "Expected $expectedResult but got win")
                        return false
                    }
                }

                if (youLose != null) {
                    Log.d(TAG, "Found game over dialog: You Lose!")
                    if (expectedResult == null || expectedResult.lowercase() == "lose") {
                        return true
                    } else {
                        Log.e(TAG, "Expected $expectedResult but got lose")
                        return false
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking game over: ${e.message}")
            }

            Thread.sleep(500)
        }

        Log.e(TAG, "Timeout waiting for game over dialog")
        return false
    }

    /**
     * Clicks the rematch button in the game result dialog.
     *
     * @param timeoutMs Maximum time to wait for button to appear
     * @return true if rematch button was clicked successfully
     */
    fun clickRematchButton(timeoutMs: Long = 15000L): Boolean {
        Log.d(TAG, "clickRematchButton: looking for rematch button")

        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            try {
                val rematchBtn = device.findObject(By.res("$PACKAGE_NAME:id/btn_rematch"))
                    ?: device.findObject(By.text("Rematch"))
                    ?: device.findObject(By.textContains("REMATCH"))

                if (rematchBtn != null) {
                    Log.d(TAG, "Rematch button found, clicking...")
                    rematchBtn.click()
                    Log.d(TAG, "Rematch button clicked")
                    Thread.sleep(500)
                    return true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error looking for rematch button: ${e.message}")
            }
            Thread.sleep(500)
        }

        Log.e(TAG, "Rematch button not found within timeout")
        return false
    }

    /**
     * Waits for the game to restart after rematch (new game starts).
     *
     * @param timeoutMs Maximum time to wait
     * @return true if new game started successfully
     */
    fun waitForGameRestart(timeoutMs: Long = 35000L): Boolean {
        Log.d(TAG, "waitForGameRestart: waiting for new game to start")

        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            try {
                // Check that game over dialog is gone
                val youWin = device.findObject(By.text("You Win!"))
                val youLose = device.findObject(By.text("You Lose!"))
                val rematchBtn = device.findObject(By.res("$PACKAGE_NAME:id/btn_rematch"))

                // Check that game board is visible
                val boardView = device.findObject(By.res("$PACKAGE_NAME:id/nine_mens_morris_board_view"))
                val statusView = device.findObject(By.res("$PACKAGE_NAME:id/tv_game_status"))

                val dialogsGone = youWin == null && youLose == null && rematchBtn == null
                val gameReady = boardView != null && statusView != null

                if (dialogsGone && gameReady) {
                    val statusText = statusView.text?.lowercase() ?: ""
                    if (statusText.contains("turn") || statusText.contains("place")) {
                        Log.d(TAG, "New game started successfully! Status: ${statusView.text}")
                        return true
                    }
                }

                Log.d(TAG, "Still waiting... dialogsGone=$dialogsGone, gameReady=$gameReady")
            } catch (e: Exception) {
                Log.e(TAG, "Error checking for game restart: ${e.message}")
            }
            Thread.sleep(500)
        }

        Log.e(TAG, "Timeout waiting for game to restart")
        return false
    }

    /**
     * Clicks the Leave Game button to exit the current game.
     * Presses back to open the leave game confirmation dialog,
     * then clicks the "Leave" button to confirm.
     *
     * @param timeoutMs Maximum time to wait for button to appear
     * @return true if leave game was clicked successfully
     */
    fun clickLeaveGame(timeoutMs: Long = 5000L): Boolean {
        Log.d(TAG, "clickLeaveGame: pressing back to open leave game dialog")

        try {
            device.pressBack()
            Thread.sleep(1000)

            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < timeoutMs) {
                var leaveBtn = device.findObject(By.res("$PACKAGE_NAME:id/btn_leave"))
                if (leaveBtn == null) {
                    leaveBtn = device.findObject(By.text("Leave"))
                }

                if (leaveBtn != null) {
                    Log.d(TAG, "Leave button found in dialog, clicking...")
                    leaveBtn.click()
                    Thread.sleep(1000)
                    Log.d(TAG, "Leave button clicked successfully")
                    return true
                }

                Thread.sleep(300)
            }

            Log.e(TAG, "Leave button not found in dialog within timeout")
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Error during leave game: ${e.message}", e)
            return false
        }
    }

    /**
     * Presses the device back button to exit game screen.
     *
     * @return true if back was pressed
     */
    fun pressBack(): Boolean {
        Log.d(TAG, "pressBack: pressing back button from game")
        try {
            device.pressBack()
            Thread.sleep(500)
            Log.d(TAG, "pressBack: SUCCESS")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error pressing back: ${e.message}", e)
            return false
        }
    }

    /**
     * Logs the current board state for debugging.
     */
    fun logBoardState() {
        Log.d(TAG, "=== Board State ===")
        try {
            val statusText = device.findObject(By.res("$PACKAGE_NAME:id/tv_game_status"))
            Log.d(TAG, "Status: ${statusText?.text ?: "not found"}")

            val opponentName = device.findObject(By.res("$PACKAGE_NAME:id/tv_opponent_name"))
            Log.d(TAG, "Opponent: ${opponentName?.text ?: "not found"}")
        } catch (e: Exception) {
            Log.e(TAG, "Error logging board state: ${e.message}")
        }
        Log.d(TAG, "===================")
    }
}
