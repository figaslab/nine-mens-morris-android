package com.devfigas.ninemensmorris.e2e

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import com.devfigas.ninemensmorris.e2e.ui.GameActions
import com.devfigas.ninemensmorris.e2e.ui.LoginActions
import com.devfigas.ninemensmorris.e2e.ui.TournamentActions
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * E2E Instrumented tests for Nine Men's Morris rematch functionality.
 * Tests:
 * 1. Rematch button visibility in GameResultDialog
 * 2. Game restarts correctly after rematch
 * 3. Multiple rematches work correctly
 *
 * These tests use CPU games for reliability (no network dependencies).
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class RematchE2ETests {

    companion object {
        private const val TAG = "RematchE2ETests"
        private const val PACKAGE_NAME = "com.devfigas.ninemensmorris"
        private const val GAME_TIMEOUT_MS = 120000L // Nine Men's Morris games can be longer
    }

    private val device: UiDevice by lazy {
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    @Before
    fun setUp() {
        Log.d(TAG, "setUp: preparing test environment")
        // Launch app and login
        assertTrue("Launch app failed", LoginActions.launchApp())
        if (!LoginActions.isOnGameModeScreen()) {
            assertTrue("Login failed", LoginActions.login("RematchTester"))
        }
        assertTrue("Not on game mode screen", LoginActions.isOnGameModeScreen())

        // Reset progress and set balance via debug menu
        assertTrue("Failed to open debug menu", TournamentActions.openDebugMenu())
        assertTrue("Failed to reset progress", TournamentActions.resetDebugProgress())
        assertTrue("Failed to set balance", TournamentActions.setDebugBalance(500))
        TournamentActions.pressBack()
        Thread.sleep(1000)
    }

    /**
     * Test: Verify buttons are visible and clickable in GameResultDialog.
     *
     * Steps:
     * 1. Start a tournament game vs CPU/bot
     * 2. Wait for game over
     * 3. Verify "Back" and "Rematch" buttons are visible
     * 4. Verify buttons are clickable
     */
    @Test
    fun test_gameResultDialogButtonsVisible() {
        Log.d(TAG, "test_gameResultDialogButtonsVisible: starting")

        // Navigate to tournament
        assertTrue("Failed to navigate to tournaments", TournamentActions.navigateToTournaments())
        assertTrue("Failed to select Bronze", TournamentActions.selectTournament("Bronze"))
        Thread.sleep(2000)

        // Challenge bot
        assertTrue("Failed to challenge bot", TournamentActions.challengeBotPlayer())
        TournamentActions.waitAndAcceptBotChallenge()

        // Wait for game over
        assertTrue("Game over not detected", waitForGameResultDialog(GAME_TIMEOUT_MS))

        // Verify buttons are visible
        val backButton = device.findObject(By.res("$PACKAGE_NAME:id/btn_exit"))
        val rematchButton = device.findObject(By.res("$PACKAGE_NAME:id/btn_rematch"))

        assertNotNull("Back button should be visible", backButton)
        assertNotNull("Rematch button should be visible", rematchButton)

        // Verify buttons are clickable
        assertTrue("Back button should be clickable", backButton?.isClickable ?: false)
        assertTrue("Rematch button should be clickable", rematchButton?.isClickable ?: false)

        Log.d(TAG, "test_gameResultDialogButtonsVisible: PASSED")
    }

    /**
     * Test: Verify rematch starts a new game correctly.
     *
     * Steps:
     * 1. Play a tournament game
     * 2. Wait for game over
     * 3. Click Rematch
     * 4. Verify new game starts (board is visible, placement phase)
     */
    @Test
    fun test_rematchStartsNewGame() {
        Log.d(TAG, "test_rematchStartsNewGame: starting")

        // Navigate to tournament and start game
        assertTrue("Failed to navigate to tournaments", TournamentActions.navigateToTournaments())
        assertTrue("Failed to select Bronze", TournamentActions.selectTournament("Bronze"))
        Thread.sleep(2000)

        // Challenge bot
        assertTrue("Failed to challenge bot", TournamentActions.challengeBotPlayer())
        TournamentActions.waitAndAcceptBotChallenge()

        // Wait for game over
        assertTrue("Game over not detected", waitForGameResultDialog(GAME_TIMEOUT_MS))

        // Click rematch
        val rematchButton = device.findObject(By.res("$PACKAGE_NAME:id/btn_rematch"))
            ?: device.findObject(By.text("Rematch"))
            ?: device.findObject(By.textContains("REMATCH"))

        assertNotNull("Rematch button should be visible", rematchButton)
        rematchButton?.click()
        Log.d(TAG, "Clicked Rematch button")

        // Wait for new game to start
        Thread.sleep(3000)

        // Verify board is visible (new game started)
        val boardView = device.findObject(By.res("$PACKAGE_NAME:id/nine_mens_morris_board_view"))
        assertNotNull("Board should be visible after rematch", boardView)

        Log.d(TAG, "test_rematchStartsNewGame: PASSED")
    }

    /**
     * Test: Multiple rematches work correctly.
     * Plays multiple games in succession using rematch.
     */
    @Test
    fun test_multipleRematches() {
        Log.d(TAG, "test_multipleRematches: starting")

        // Navigate to tournament and start game
        assertTrue("Failed to navigate to tournaments", TournamentActions.navigateToTournaments())

        // Add more coins for multiple games
        TournamentActions.pressBack()
        assertTrue("Failed to open debug menu", TournamentActions.openDebugMenu())
        assertTrue("Failed to set balance", TournamentActions.setDebugBalance(500))
        TournamentActions.pressBack()
        Thread.sleep(500)

        assertTrue("Failed to navigate to tournaments", TournamentActions.navigateToTournaments())
        assertTrue("Failed to select Bronze", TournamentActions.selectTournament("Bronze"))
        Thread.sleep(2000)

        // Challenge bot
        assertTrue("Failed to challenge bot", TournamentActions.challengeBotPlayer())
        TournamentActions.waitAndAcceptBotChallenge()

        // Play 2 games with rematches
        for (gameNum in 1..2) {
            Log.d(TAG, "=== Game $gameNum ===")

            // Wait for game over
            assertTrue("Game $gameNum over not detected", waitForGameResultDialog(GAME_TIMEOUT_MS))

            // Click rematch
            val clicked = clickRematchButton()
            assertTrue("Failed to click rematch for game $gameNum", clicked)

            // Wait for new game to start
            Thread.sleep(3000)

            // Verify board is visible
            val boardView = device.findObject(By.res("$PACKAGE_NAME:id/nine_mens_morris_board_view"))
            assertNotNull("Board should be visible after rematch $gameNum", boardView)

            Log.d(TAG, "Game $gameNum completed and rematched successfully")
        }

        Log.d(TAG, "test_multipleRematches: PASSED")
    }

    /**
     * Test: Back button in game result dialog returns to lobby/menu.
     */
    @Test
    fun test_backButtonReturnsToMenu() {
        Log.d(TAG, "test_backButtonReturnsToMenu: starting")

        // Navigate to tournament and start game
        assertTrue("Failed to navigate to tournaments", TournamentActions.navigateToTournaments())
        assertTrue("Failed to select Bronze", TournamentActions.selectTournament("Bronze"))
        Thread.sleep(2000)

        // Challenge bot
        assertTrue("Failed to challenge bot", TournamentActions.challengeBotPlayer())
        TournamentActions.waitAndAcceptBotChallenge()

        // Wait for game over
        assertTrue("Game over not detected", waitForGameResultDialog(GAME_TIMEOUT_MS))

        // Click Back button
        val backButton = device.findObject(By.res("$PACKAGE_NAME:id/btn_exit"))
            ?: device.findObject(By.text("Back"))
            ?: device.findObject(By.textContains("Back"))

        assertNotNull("Back button should be visible", backButton)
        backButton?.click()
        Log.d(TAG, "Clicked Back button")

        Thread.sleep(2000)

        // Verify we returned to some menu screen (lobby or game mode)
        val onGameMode = LoginActions.isOnGameModeScreen()
        val onTournaments = TournamentActions.isOnTournamentSelectionScreen()
        val onLobby = TournamentActions.isOnInternetLobby()

        assertTrue(
            "Should be on game mode, tournament, or lobby screen after clicking Back",
            onGameMode || onTournaments || onLobby
        )

        Log.d(TAG, "test_backButtonReturnsToMenu: PASSED")
    }

    private fun waitForGameResultDialog(timeoutMs: Long): Boolean {
        Log.d(TAG, "waitForGameResultDialog: waiting...")
        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            val resultTitle = device.findObject(By.res("$PACKAGE_NAME:id/tv_result_title"))
            val backButton = device.findObject(By.res("$PACKAGE_NAME:id/btn_exit"))
            val rematchButton = device.findObject(By.res("$PACKAGE_NAME:id/btn_rematch"))
            val youWin = device.findObject(By.textContains("You Win"))
            val youLose = device.findObject(By.textContains("You Lose"))

            if (resultTitle != null || backButton != null || rematchButton != null ||
                youWin != null || youLose != null) {
                Log.d(TAG, "Game result dialog detected")
                return true
            }

            Thread.sleep(500)
        }

        Log.e(TAG, "Timeout waiting for game result dialog")
        return false
    }

    private fun clickRematchButton(): Boolean {
        val rematchButton = device.findObject(By.res("$PACKAGE_NAME:id/btn_rematch"))
            ?: device.findObject(By.text("Rematch"))
            ?: device.findObject(By.textContains("REMATCH"))

        if (rematchButton != null) {
            rematchButton.click()
            Log.d(TAG, "Rematch button clicked")
            return true
        }

        Log.e(TAG, "Rematch button not found")
        return false
    }
}
