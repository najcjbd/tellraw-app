package com.tellraw.app.data.repository

import com.tellraw.app.TestApplication
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * 历史记录格式回归：记录里出现的零宽符号不能把记录切断，`标记文本` 要能原样往返。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = TestApplication::class, packageName = "com.tellraw.app")
class HistoryRepositoryRecordsTest {

    private lateinit var repo: HistoryRepository

    @Before
    fun setup() {
        repo = HistoryRepository(RuntimeEnvironment.getApplication())
    }

    @Test
    fun recordsRoundTripWithSpecialSymbolsAndMarkers() {
        val first = HistoryItem(
            selector = "@a",
            message = "前\u200B中\u200C后",
            javaCommand = """tellraw @a {"text":"x"}""",
            bedrockCommand = """tellraw @a {"rawtext":[{"text":"x"}]}""",
            timestamp = 1_700_000_000_000L,
            markers = "a\u200Bb\u200Cc"
        )
        val second = HistoryItem(
            selector = "@p",
            message = "second",
            javaCommand = "tellraw @p y",
            bedrockCommand = "tellraw @p y",
            timestamp = 1_700_000_005_000L
        )

        val parsed = repo.parseHistoryContent(
            repo.buildSingleHistoryItem(first) + repo.buildSingleHistoryItem(second)
        )

        assertEquals(2, parsed.size)
        assertEquals("second", parsed[0].message)
        assertEquals("前\u200B中\u200C后", parsed[1].message)
        assertEquals("a\u200Bb\u200Cc", parsed[1].markers)
        assertEquals("", parsed[0].markers)
        // 时间戳必须真正解析出来（否则会回落到“当前时间”，同秒记录的顺序不稳定）
        assertEquals(5000L, parsed[0].timestamp - parsed[1].timestamp)
    }

    @Test
    fun oldFormatWithoutMarkersStillParses() {
        val old = "\u200B时间:2023-11-14 22:13:20\n" +
            " 输入文本:hello\n" +
            " JAVA版输出:tellraw @a x\n" +
            " 基岩版输出:tellraw @a y\u200C\n\n"

        val parsed = repo.parseHistoryContent(old)

        assertEquals(1, parsed.size)
        assertEquals("hello", parsed[0].message)
        assertEquals("tellraw @a x", parsed[0].javaCommand)
        assertEquals("", parsed[0].markers)
    }
}
