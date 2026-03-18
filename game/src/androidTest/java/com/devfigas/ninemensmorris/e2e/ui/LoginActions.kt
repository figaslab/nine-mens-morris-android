package com.devfigas.ninemensmorris.e2e.ui

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until

/**
 * UI Automation actions for the login/main screen.
 * Handles user name entry and avatar selection.
 */
object LoginActions {
    private const val TAG = "LoginActions"
    private const val TIMEOUT_MS = 5000L
    private const val PACKAGE_NAME = "com.devfigas.ninemensmorris"

    private val device: UiDevice by lazy {
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    /**
     * Performs login with the given user name.
     * Uses the default avatar if none is specified.
     *
     * @param userName The name to enter
     * @return true if login was successful
     */
    fun login(userName: String): Boolean {
        Log.d(TAG, "login: attempting with userName=$userName")

        try {
            // Wait for the main activity to be ready
            if (!waitForLoginScreen()) {
                Log.e(TAG, "Login screen not found")
                return false
            }

            // Find and fill the name field
            val nameField = device.findObject(By.res("$PACKAGE_NAME:id/landing_page_et_name"))
            if (nameField == null) {
                Log.e(TAG, "Name field not found")
                return false
            }

            // Clear and enter the name
            nameField.click()
            Thread.sleep(200)
            nameField.clear()
            nameField.text = userName
            Log.d(TAG, "Name entered: $userName")

            // Hide keyboard
            device.pressBack()
            Thread.sleep(300)

            // Click the enter button
            val enterButton = device.findObject(By.res("$PACKAGE_NAME:id/landing_page_bt_enter"))
            if (enterButton == null) {
                Log.e(TAG, "Enter button not found")
                return false
            }

            enterButton.click()
            Log.d(TAG, "Enter button clicked")

            // Wait for transition to GameModeActivity
            Thread.sleep(1000)

            // Verify we're on the game mode screen
            val success = isOnGameModeScreen()
            Log.d(TAG, "Login result: $success")
            return success
        } catch (e: Exception) {
            Log.e(TAG, "Error during login: ${e.message}", e)
            return false
        }
    }

    /**
     * Waits for the login screen to be visible.
     *
     * @param timeoutMs Maximum time to wait
     * @return true if the login screen is visible
     */
    fun waitForLoginScreen(timeoutMs: Long = TIMEOUT_MS): Boolean {
        Log.d(TAG, "waitForLoginScreen: waiting...")

        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            val nameField = device.findObject(By.res("$PACKAGE_NAME:id/landing_page_et_name"))
            val enterButton = device.findObject(By.res("$PACKAGE_NAME:id/landing_page_bt_enter"))

            if (nameField != null && enterButton != null) {
                Log.d(TAG, "Login screen found")
                return true
            }
            Thread.sleep(300)
        }

        Log.e(TAG, "Timeout waiting for login screen")
        return false
    }

    /**
     * Checks if we're currently on the game mode selection screen.
     *
     * @return true if on game mode screen
     */
    fun isOnGameModeScreen(): Boolean {
        // CPU-only: look for the VS CPU button
        val btCpu = device.findObject(By.res("$PACKAGE_NAME:id/btn_vs_cpu"))
        val btBluetooth = device.findObject(By.res("$PACKAGE_NAME:id/btn_vs_bluetooth"))
        val btWifi = device.findObject(By.res("$PACKAGE_NAME:id/btn_vs_wifi"))
        return btCpu != null || (btBluetooth != null && btWifi != null)
    }

    /**
     * Waits for the game mode screen to be visible.
     *
     * @param timeoutMs Maximum time to wait
     * @return true if the game mode screen is visible
     */
    fun waitForGameModeScreen(timeoutMs: Long = TIMEOUT_MS): Boolean {
        Log.d(TAG, "waitForGameModeScreen: waiting...")

        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (isOnGameModeScreen()) {
                Log.d(TAG, "Game mode screen found")
                return true
            }
            Thread.sleep(300)
        }

        Log.e(TAG, "Timeout waiting for game mode screen")
        return false
    }

    /**
     * Launches the app from the home screen.
     *
     * @return true if the app was launched
     */
    fun launchApp(): Boolean {
        Log.d(TAG, "launchApp: launching $PACKAGE_NAME")

        try {
            // Go to home screen
            device.pressHome()
            Thread.sleep(500)

            // Launch via intent
            val context = InstrumentationRegistry.getInstrumentation().context
            val intent = context.packageManager.getLaunchIntentForPackage(PACKAGE_NAME)
            if (intent == null) {
                Log.e(TAG, "Could not get launch intent for $PACKAGE_NAME")
                return false
            }

            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)

            // Wait for app to launch
            device.wait(Until.hasObject(By.pkg(PACKAGE_NAME).depth(0)), TIMEOUT_MS)

            Log.d(TAG, "App launched")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error launching app: ${e.message}", e)
            return false
        }
    }

    /**
     * Performs full login flow: launch app and enter name.
     *
     * @param userName The name to use
     * @return true if successful
     */
    fun launchAndLogin(userName: String): Boolean {
        // First check if already on game mode screen (already logged in)
        if (isOnGameModeScreen()) {
            Log.d(TAG, "Already on game mode screen, skipping login")
            return true
        }

        if (!launchApp()) {
            return false
        }

        // Check again after launch
        if (isOnGameModeScreen()) {
            Log.d(TAG, "Already on game mode screen after launch, skipping login")
            return true
        }

        return login(userName)
    }
}
