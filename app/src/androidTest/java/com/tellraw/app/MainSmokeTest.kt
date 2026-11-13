package com.tellraw.app

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class MainSmokeTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun mainInputsAreVisible() {
        compose.onNodeWithTag("selector_input").assertIsDisplayed()
        compose.onNodeWithTag("message_input").assertIsDisplayed()

        // Capture before opening the dialog: a dialog adds a second Compose root.
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val screenshot = File(context.getExternalFilesDir(null), "ui-smoke.png")
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        FileOutputStream(screenshot).use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }

        compose.onNodeWithText("Tellraw preview").performClick()
        compose.onNodeWithText("Import command file").assertIsDisplayed()
    }
}
