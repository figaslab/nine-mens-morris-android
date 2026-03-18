package com.devfigas.ninemensmorris.e2e

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.devfigas.ninemensmorris.e2e.ui.GameActions
import com.devfigas.ninemensmorris.e2e.ui.LoginActions
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * E2E Instrumented tests for Nine Men's Morris app.
 * These tests focus on CPU-only scenarios (no Bluetooth/WiFi).
 *
 * Nine Men's Morris is played in phases:
 * 1. Placement phase: Each player places 9 pieces on empty intersections
 * 2. Movement phase: Players move pieces to adjacent intersections
 * 3. Flying phase: When a player has only 3 pieces left, they can move to any empty spot
 *
 * Forming a "mill" (3 pieces in a row along a line) allows removing an opponent's piece.
 *
 * Each test method can be called individually via:
 * adb shell am instrument -w -e class com.devfigas.ninemensmorris.e2e.NineMensMorrisE2ETests#methodName \
 *     -e userName "TestUser" ... \
 *     com.devfigas.ninemensmorris.test/androidx.test.runner.AndroidJUnitRunner
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class NineMensMorrisE2ETests {

    companion object {
        private const val TAG = "NineMensMorrisE2E"

        // Argument keys
        const val ARG_USER_NAME = "userName"
        const val ARG_TIMEOUT_MS = "timeoutMs"
        const val ARG_POSITION = "position"
    }

    private val arguments by lazy {
        InstrumentationRegistry.getArguments()
    }

    private fun getArg(key: String, default: String = ""): String {
        return arguments.getString(key, default) ?: default
    }

    private fun getLongArg(key: String, default: Long = 5000L): Long {
        return arguments.getString(key, default.toString())?.toLongOrNull() ?: default
    }

    private fun getIntArg(key: String, default: Int = 0): Int {
        return arguments.getString(key, default.toString())?.toIntOrNull() ?: default
    }

    // ========================================
    // Individual Action Tests
    // ========================================

    /**
     * Launches app and performs login.
     * Args: userName
     */
    @Test
    fun action_login() {
        val userName = getArg(ARG_USER_NAME, "TestUser")
        Log.d(TAG, "action_login: userName=$userName")

        val result = LoginActions.launchAndLogin(userName)
        assertTrue("Login failed for user $userName", result)

        Log.d(TAG, "action_login: SUCCESS")
    }

    /**
     * Starts a CPU game from the game mode screen.
     * Assumes already logged in and on game mode screen.
     */
    @Test
    fun action_start_cpu_game() {
        Log.d(TAG, "action_start_cpu_game: starting CPU game")

        // Ensure we're on game mode screen
        assertTrue("Not on game mode screen", LoginActions.isOnGameModeScreen())

        // Click VS CPU button
        val device = androidx.test.uiautomator.UiDevice.getInstance(
            InstrumentationRegistry.getInstrumentation()
        )
        val cpuButton = device.findObject(
            androidx.test.uiautomator.By.res("com.devfigas.ninemensmorris:id/btn_vs_cpu")
        )
        assertNotNull("VS CPU button not found", cpuButton)
        cpuButton!!.click()
        Log.d(TAG, "VS CPU button clicked")

        // Wait for game screen to appear
        Thread.sleep(2000)
        val gameReady = GameActions.waitForGameScreen(10000L)
        assertTrue("Game screen not ready after clicking VS CPU", gameReady)

        Log.d(TAG, "action_start_cpu_game: SUCCESS")
    }

    /**
     * Taps a position on the Nine Men's Morris board.
     * Args: position (0-23)
     */
    @Test
    fun action_tap_board() {
        val posIndex = getIntArg(ARG_POSITION, 0)
        Log.d(TAG, "action_tap_board: tapping position $posIndex")

        val result = GameActions.tapPosition(posIndex)
        assertTrue("Failed to tap position $posIndex", result)

        Log.d(TAG, "action_tap_board: SUCCESS")
    }

    // ========================================
    // Combined Scenario Tests (CPU-only)
    // ========================================

    /**
     * Full CPU game start scenario:
     * Login -> Select VS CPU -> Verify game screen -> Place first piece.
     */
    @Test
    fun scenario_cpu_game_start() {
        val userName = getArg(ARG_USER_NAME, "TestPlayer")

        Log.d(TAG, "scenario_cpu_game_start: starting with userName=$userName")

        // Step 1: Login
        Log.d(TAG, "Step 1: Login")
        assertTrue("Launch and login failed", LoginActions.launchAndLogin(userName))
        assertTrue("Not on game mode screen", LoginActions.isOnGameModeScreen())

        // Step 2: Start CPU game
        Log.d(TAG, "Step 2: Start CPU game")
        val device = androidx.test.uiautomator.UiDevice.getInstance(
            InstrumentationRegistry.getInstrumentation()
        )
        val cpuButton = device.findObject(
            androidx.test.uiautomator.By.res("com.devfigas.ninemensmorris:id/btn_vs_cpu")
        )
        assertNotNull("VS CPU button not found", cpuButton)
        cpuButton!!.click()

        // Step 3: Wait for game screen
        Log.d(TAG, "Step 3: Wait for game screen")
        assertTrue("Game screen not ready", GameActions.waitForGameScreen(10000L))

        // Step 4: Log board state
        Log.d(TAG, "Step 4: Log board state")
        GameActions.logBoardState()

        // Step 5: Place first piece at position 0 (top-left corner of outer square)
        Log.d(TAG, "Step 5: Place piece at position 0")
        assertTrue("Failed to place piece at position 0", GameActions.tapPosition(0))

        // Wait for CPU response
        Thread.sleep(2000)

        // Step 6: Place second piece at position 1 (top-center of outer square)
        Log.d(TAG, "Step 6: Place piece at position 1")
        assertTrue("Failed to place piece at position 1", GameActions.tapPosition(1))

        Log.d(TAG, "scenario_cpu_game_start: COMPLETED")
    }

    /**
     * Resign game scenario:
     * Login -> Start CPU game -> Leave game (resign).
     */
    @Test
    fun scenario_resign_game() {
        val userName = getArg(ARG_USER_NAME, "TestPlayer")

        Log.d(TAG, "scenario_resign_game: starting with userName=$userName")

        // Step 1: Login
        Log.d(TAG, "Step 1: Login")
        assertTrue("Launch and login failed", LoginActions.launchAndLogin(userName))
        assertTrue("Not on game mode screen", LoginActions.isOnGameModeScreen())

        // Step 2: Start CPU game
        Log.d(TAG, "Step 2: Start CPU game")
        val device = androidx.test.uiautomator.UiDevice.getInstance(
            InstrumentationRegistry.getInstrumentation()
        )
        val cpuButton = device.findObject(
            androidx.test.uiautomator.By.res("com.devfigas.ninemensmorris:id/btn_vs_cpu")
        )
        assertNotNull("VS CPU button not found", cpuButton)
        cpuButton!!.click()

        // Step 3: Wait for game screen
        Log.d(TAG, "Step 3: Wait for game screen")
        assertTrue("Game screen not ready", GameActions.waitForGameScreen(10000L))

        // Step 4: Place a piece to make a move
        Log.d(TAG, "Step 4: Place piece at position 0")
        assertTrue("Failed to place piece", GameActions.tapPosition(0))
        Thread.sleep(1000)

        // Step 5: Leave game (resign)
        Log.d(TAG, "Step 5: Leave game")
        assertTrue("Failed to leave game", GameActions.clickLeaveGame())

        // Step 6: Verify we returned to game mode screen
        Log.d(TAG, "Step 6: Verify return to game mode screen")
        Thread.sleep(1000)
        val onGameMode = LoginActions.waitForGameModeScreen(5000L)
        assertTrue("Should be back on game mode screen after leaving", onGameMode)

        Log.d(TAG, "scenario_resign_game: COMPLETED")
    }

    /**
     * Full game test: Login -> CPU game -> Play multiple placement moves -> Wait for game over.
     * This test plays through multiple moves in the placement phase.
     */
    @Test
    fun scenario_placement_phase() {
        val userName = getArg(ARG_USER_NAME, "TestPlayer")

        Log.d(TAG, "scenario_placement_phase: starting with userName=$userName")

        // Step 1: Login and start CPU game
        Log.d(TAG, "Step 1: Login")
        assertTrue("Launch and login failed", LoginActions.launchAndLogin(userName))

        Log.d(TAG, "Step 2: Start CPU game")
        val device = androidx.test.uiautomator.UiDevice.getInstance(
            InstrumentationRegistry.getInstrumentation()
        )
        val cpuButton = device.findObject(
            androidx.test.uiautomator.By.res("com.devfigas.ninemensmorris:id/btn_vs_cpu")
        )
        assertNotNull("VS CPU button not found", cpuButton)
        cpuButton!!.click()
        assertTrue("Game screen not ready", GameActions.waitForGameScreen(10000L))

        // Step 3: Place pieces in the placement phase
        // Place on outer square corners and midpoints
        val positions = listOf(0, 2, 4, 6, 1, 3, 5, 7, 9)
        for ((index, pos) in positions.withIndex()) {
            Log.d(TAG, "Step 3.${index + 1}: Place piece at position $pos")

            // Wait for my turn
            Thread.sleep(1500)

            val placed = GameActions.tapPosition(pos)
            if (!placed) {
                Log.w(TAG, "Could not place at position $pos (might be occupied), continuing...")
            }

            // Wait for CPU to respond
            Thread.sleep(1500)
        }

        Log.d(TAG, "scenario_placement_phase: COMPLETED - Placed pieces in placement phase")
    }
}
