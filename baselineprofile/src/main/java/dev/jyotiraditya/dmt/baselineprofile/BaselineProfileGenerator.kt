package dev.jyotiraditya.dmt.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test

private const val PACKAGE_NAME = "dev.jyotiraditya.dmt"
private const val DIALOG_TIMEOUT_MS = 3_000L
private const val CONTENT_TIMEOUT_MS = 5_000L

class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generate() = baselineProfileRule.collect(
        packageName = PACKAGE_NAME,
        includeInStartupProfile = true,
    ) {
        pressHome()
        startActivityAndWait()

        repeat(2) {
            val allow = device.wait(Until.findObject(By.textContains("Allow")), DIALOG_TIMEOUT_MS)
            allow?.click()
            device.waitForIdle()
        }

        device.wait(Until.hasObject(By.scrollable(true)), CONTENT_TIMEOUT_MS)
        device.findObject(By.scrollable(true))?.fling(Direction.DOWN)
        device.waitForIdle()
    }
}
