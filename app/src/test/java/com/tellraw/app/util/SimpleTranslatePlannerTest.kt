package com.tellraw.app.util

import com.tellraw.app.model.VersionSpec
import com.tellraw.app.util.SimpleTranslatePlanner.Alias
import com.tellraw.app.util.SimpleTranslatePlanner.Row
import com.tellraw.app.util.SimpleTranslatePlanner.Table
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SimpleTranslatePlannerTest {

    private val javaBase =
        """{"text":"Hello ","extra":[{"translate":"__simpler_translate__"},{"text":"!"}]}"""
    private val bedrockBase =
        """{"rawtext":[{"text":"Hello "},{"translate":"__simpler_translate__"},{"text":"!"}]}"""

    private fun plan(
        table: Table,
        includeDefaultFallback: Boolean = true,
        bedrockFirstMatch: Boolean = false
    ): SimpleTranslatePlanner.Result {
        val warnings = mutableListOf<String>()
        return SimpleTranslatePlanner.plan(
            table = table.copy(bedrockFirstMatch = bedrockFirstMatch),
            javaMessageJson = javaBase,
            bedrockMessageJson = bedrockBase,
            javaSelector = "@a",
            bedrockSelector = "@a",
            javaPrefix = null,
            bedrockPrefix = null,
            includeDefaultFallback = includeDefaultFallback,
            context = null,
            nbtSyntax = VersionDiff.NbtSyntax.MODERN,
            target = VersionSpec.LATEST,
            mNHandling = "font",
            mnCFEnabled = false,
            separatorAsTextComponent = false,
            bedrockColorToRgb = true,
            warnings = warnings
        )
    }

    @Test
    fun javaReplacesTokenInPlaceAndKeepsSurroundingText() {
        val result = plan(
            Table(
                rows = listOf(
                    Row(params = "tag=a", content = listOf(WithArgDraft.Text("A"))),
                    Row(params = "scores={k=1}", content = listOf(WithArgDraft.Text("B")))
                )
            )
        )

        assertEquals(3, result.java.size)
        assertTrue(result.java[0], result.java[0].contains("execute as @a[tag=a] run tellraw @s"))
        assertTrue(result.java[0], result.java[0].contains("\"Hello \""))
        assertTrue(result.java[0], result.java[0].contains("\"A\""))
        assertTrue(result.java[0], result.java[0].contains("\"!\""))

        assertTrue(result.java[1], result.java[1].contains("unless entity @s[tag=a]"))
        assertTrue(result.java[1], result.java[1].contains("\"B\""))

        // token 前后有内容 -> 未命中任何行的收件人收到去掉 token 的完整消息
        assertTrue(result.java[2], result.java[2].contains("unless entity @s[tag=a]"))
        assertTrue(result.java[2], result.java[2].contains("unless entity @s[scores={k=1}]"))
        assertFalse(result.java[2], result.java[2].contains(SimpleTranslatePlanner.KEY))
    }

    @Test
    fun tokenOnlyMessageDoesNotSendEmptyFallback() {
        val result = plan(
            Table(rows = listOf(Row(params = "tag=a", content = listOf(WithArgDraft.Text("A"))))),
            includeDefaultFallback = false
        )
        assertEquals(1, result.java.size)
    }

    @Test
    fun aliasIsOneLevelOnly() {
        val result = plan(
            Table(
                rows = listOf(Row(params = "tag=a", content = listOf(WithArgDraft.Text("a")))),
                aliases = listOf(
                    Alias("a", listOf(WithArgDraft.Text("b"))),
                    Alias("b", listOf(WithArgDraft.Text("c")))
                )
            ),
            includeDefaultFallback = false
        )
        assertTrue(result.java[0], result.java[0].contains("\"b\""))
        assertFalse(result.java[0], result.java[0].contains("\"c\""))
    }

    @Test
    fun blankRowIsFallbackAndExcludesEarlierRows() {
        val result = plan(
            Table(
                rows = listOf(
                    Row(params = "tag=a", content = listOf(WithArgDraft.Text("A"))),
                    Row(params = "", content = listOf(WithArgDraft.Text("all")))
                )
            )
        )
        assertEquals(2, result.java.size)
        assertTrue(result.java[0], result.java[0].contains("execute as @a[tag=a]"))
        assertTrue(result.java[1], result.java[1].contains("execute as @a unless entity @s[tag=a]"))
        assertTrue(result.java[1], result.java[1].contains("\"all\""))
    }

    @Test
    fun rowsAfterBlankRowAreSkipped() {
        val result = plan(
            Table(
                rows = listOf(
                    Row(params = "", content = listOf(WithArgDraft.Text("all"))),
                    Row(params = "tag=a", content = listOf(WithArgDraft.Text("A")))
                )
            )
        )
        assertTrue(result.java.size == 1)
        assertTrue(result.warnings.any { it.contains("永远不会命中") })
    }

    @Test
    fun bedrockConditionalUsesInvertedEarlierParams() {
        val result = plan(
            Table(
                rows = listOf(
                    Row(params = "tag=a", content = listOf(WithArgDraft.Text("A"))),
                    Row(params = "scores={k=1}", content = listOf(WithArgDraft.Text("B")))
                )
            ),
            includeDefaultFallback = false
        )
        assertEquals(1, result.bedrock.size)
        val cmd = result.bedrock[0]
        assertTrue(cmd, cmd.contains("execute as @a run tellraw @s"))
        assertTrue(cmd, cmd.contains("\"translate\":\"%%2\""))
        assertTrue(cmd, cmd.contains("@s[tag=a]"))
        assertTrue(cmd, cmd.contains("@s[scores={k=1},tag=!a]"))
    }

    @Test
    fun bedrockConditionalDoesNotCreateDuplicateSelectorKeys() {
        val result = plan(
            Table(
                rows = listOf(
                    Row(params = "tag=a", content = listOf(WithArgDraft.Text("A"))),
                    Row(params = "tag=b", content = listOf(WithArgDraft.Text("B")))
                )
            ),
            includeDefaultFallback = false
        )
        val cmd = result.bedrock[0]
        assertFalse(cmd, cmd.contains("@s[tag=b,tag=!a]"))
        assertTrue(result.warnings.any { it.contains("无法排除") })
    }

    @Test
    fun bedrockConditionalDoesNotFakeScoreComplement() {
        val result = plan(
            Table(
                rows = listOf(
                    Row(params = "tag=a", content = listOf(WithArgDraft.Text("A"))),
                    Row(params = "scores={k=1}", content = listOf(WithArgDraft.Text("B"))),
                    Row(params = "tag=c", content = listOf(WithArgDraft.Text("C")))
                )
            ),
            includeDefaultFallback = false
        )
        val cmd = result.bedrock.single()
        // 第三个条件不能生成 scores={k=!1}：它会把"没有 k 分数"的人排掉。
        assertFalse(cmd, cmd.contains("k=!1"))
        assertTrue(result.warnings.any { it.contains("无法排除") })
    }

    @Test
    fun bedrockFirstMatchProducesMultipleCommands() {
        val result = plan(
            Table(
                rows = listOf(
                    Row(params = "tag=a", content = listOf(WithArgDraft.Text("A"))),
                    Row(params = "scores={k=1}", content = listOf(WithArgDraft.Text("B")))
                )
            ),
            bedrockFirstMatch = true
        )
        assertEquals(3, result.bedrock.size)
        assertTrue(result.bedrock[1], result.bedrock[1].contains("unless entity @s[tag=a]"))
    }

    @Test
    fun emptyRowContentStillLeavesValidComponent() {
        val result = plan(
            Table(rows = listOf(Row(params = "tag=a", content = emptyList()))),
            includeDefaultFallback = false
        )
        assertFalse(result.java[0], result.java[0].contains(":{}"))
        assertTrue(result.java[0], result.java[0].contains("\"text\":\"\""))
    }

    @Test
    fun selectorPrefixInsideParamsIsRemovedWithWarning() {
        val result = plan(
            Table(rows = listOf(Row(params = "@a[tag=a]", content = listOf(WithArgDraft.Text("A")))))
        )
        assertTrue(result.java[0], result.java[0].contains("@a[tag=a]"))
        assertTrue(result.warnings.any { it.contains("不要写 @a") })
    }

    @Test
    fun sectionCodeInRowContentIsConverted() {
        val result = plan(
            Table(rows = listOf(Row(params = "tag=a", content = listOf(WithArgDraft.Text("§aX")))))
        )
        assertTrue(result.java[0], result.java[0].contains("\"color\":\"green\""))
    }

    @Test
    fun simplerMarkerSurvivesComponentConversion() {
        val token = TextComponentHelper.TextComponent(
            TextComponentHelper.ComponentType.TRANSLATE,
            TextComponentHelper.SIMPLE_TRANSLATE_KEY
        )
        val java = TextComponentHelper.convertToJavaJson(listOf(token))
        assertTrue(java, java.contains("\"translate\":\"${TextComponentHelper.SIMPLE_TRANSLATE_KEY}\""))

        val warnings = mutableListOf<String>()
        val bedrock = TextComponentHelper.convertToBedrockJson(listOf(token), warnings = warnings)
        assertTrue(bedrock, bedrock.contains("\"translate\":\"${TextComponentHelper.SIMPLE_TRANSLATE_KEY}\""))
        assertTrue(warnings.isEmpty())
    }
}
