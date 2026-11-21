package com.tellraw.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.tellraw.app.ui.components.SimpleTranslateGroupDialog
import com.tellraw.app.ui.components.WithBuilderDialog
import com.tellraw.app.ui.viewmodel.WithBuilderMode
import com.tellraw.app.ui.viewmodel.WithBuilderState
import com.tellraw.app.util.SimpleTranslatePlanner
import com.tellraw.app.util.SimpleTranslatePlanner.Row
import com.tellraw.app.util.SimpleTranslatePlanner.Table
import com.tellraw.app.util.WithArgDraft
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.io.FileOutputStream

class SimpleTranslateUiTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun withBuilderShowsSimplerEntryAtTop() {
        var opened = false
        compose.setContent {
            MaterialTheme {
                WithBuilderDialog(
                    state = WithBuilderState(
                        targetPosition = 0,
                        isNew = true,
                        key = "chat.type.text",
                        fallback = "",
                        args = emptyList(),
                        mode = WithBuilderMode.VISUAL,
                        rawJson = "[]"
                    ),
                    simpleMode = true,
                    onOpenSimpleTranslate = { opened = true },
                    onDismiss = {},
                    onKeyChange = {},
                    onFallbackChange = {},
                    onArgsChange = {},
                    onModeChange = {},
                    onRawJsonChange = {},
                    onApply = {}
                )
            }
        }
        compose.onNodeWithText("Simpler translate：按收信人替换消息内容")
            .assertIsDisplayed()
            .performClick()
        assertTrue(opened)
        capture("with-builder-simpler.png")
    }

    @Test
    fun tableOpensWithExistingRowsAndCanAddAnother() {
        val block = SimpleTranslatePlanner.encode(
            Table(rows = listOf(Row(params = "tag=a", content = listOf(WithArgDraft.Text("A")))))
        )
        var completed: String? = null
        compose.setContent {
            MaterialTheme {
                SimpleTranslateGroupDialog(
                    initialBlockJson = block,
                    onComplete = { json, _ -> completed = json },
                    onDismiss = {}
                )
            }
        }
        compose.onNodeWithText("收信人参数 → 替换内容").assertIsDisplayed()
        compose.onAllNodesWithText("#1").assertCountEquals(1)
        capture("simple-translate-table.png")
        compose.onNodeWithText("添加收信人参数").performClick()
        compose.onAllNodesWithText("#2").assertCountEquals(1)
        compose.onNodeWithText("完成").performClick()
        assertTrue(completed.orEmpty().contains("tag=a"))
    }

    private fun capture(name: String) {
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        val dir = InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null)
        FileOutputStream(File(dir, name)).use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
