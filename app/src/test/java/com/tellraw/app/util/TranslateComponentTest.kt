package com.tellraw.app.util

import com.tellraw.app.util.TextComponentHelper.ComponentType
import com.tellraw.app.util.TextComponentHelper.SubComponent
import com.tellraw.app.util.TextComponentHelper.SubComponentType
import com.tellraw.app.util.TextComponentHelper.TextComponent
import org.junit.Assert.*
import org.junit.Test

class TranslateComponentTest {

    @Test
    fun fallbackAndEmptyWithArguments() {
        val comp = TextComponent(
            ComponentType.TRANSLATE, "chat.type.text",
            mutableListOf(
                SubComponent(SubComponentType.WITH, "a,,b"),
                SubComponent(SubComponentType.FALLBACK, "fb")
            )
        )
        val java = TextComponentHelper.convertToJavaJson(listOf(comp))
        assertTrue(java, java.contains("\"with\":[\"a\",\"\",\"b\"]"))
        assertTrue(java, java.contains("\"fallback\":\"fb\""))

        val warnings = mutableListOf<String>()
        TextComponentHelper.convertToBedrockJson(listOf(comp), warnings = warnings)
        assertTrue(warnings.any { it.contains("fallback") })
    }

    @Test
    fun emptyWithContentDoesNotEmitWith() {
        val comp = TextComponent(
            ComponentType.TRANSLATE, "item.minecraft.diamond",
            mutableListOf(SubComponent(SubComponentType.WITH, ""))
        )
        val java = TextComponentHelper.convertToJavaJson(listOf(comp))
        assertFalse(java, java.contains("\"with\""))
    }

    @Test
    fun nestedWithJsonPreservedForJava() {
        val comp = TextComponent(
            ComponentType.TRANSLATE, "chat.type.text",
            mutableListOf(
                SubComponent(
                    SubComponentType.WITH_JSON,
                    """[{"text":"A","color":"red"},{"text":"B","extra":[{"text":"!"}]}]"""
                )
            )
        )
        val java = TextComponentHelper.convertToJavaJson(listOf(comp))
        assertTrue(java, java.contains("\"color\":\"red\""))
        assertTrue(java, java.contains("\"extra\""))

        val warnings = mutableListOf<String>()
        TextComponentHelper.convertToBedrockJson(listOf(comp), warnings = warnings)
        assertTrue(warnings.any { it.contains("嵌套") })
    }

    @Test
    fun rawNestedWithIsPreserved() {
        val r = CommandInputParser.parse(
            """{"translate":"x","with":[{"text":"A","color":"red"}]}"""
        )!!
        val with = r.components[0].subComponents.first { it.type == SubComponentType.WITH_JSON }.content
        assertTrue(with, with.trim().startsWith("["))
        assertTrue(r.javaJson, r.javaJson.contains("\"color\":\"red\""))
        assertFalse(r.warnings.any { it.contains("不是纯文本") })
    }

    @Test
    fun jsonLookingWithStaysPlainInTextMode() {
        // 明确选 with（纯文本）时，即使内容看起来像 JSON 也按纯文本处理
        val comp = TextComponent(
            ComponentType.TRANSLATE, "x",
            mutableListOf(SubComponent(SubComponentType.WITH, """{"text":"A"}"""))
        )
        val json = TextComponentHelper.convertToJavaJson(listOf(comp))
        val arr = com.google.gson.JsonParser.parseString(json).asJsonObject.getAsJsonArray("with")
        assertTrue(arr[0].isJsonPrimitive)
    }

    @Test
    fun withEscapedCommaStaysInsideArgument() {
        val args = TextComponentHelper.parseTranslateArgs("a\\,b,c")
        assertEquals(listOf("a,b", "c"), args)
    }

    @Test
    fun translatePreviewSubstitutesPlaceholders() {
        assertEquals("Steve：你好", TranslatePreview.render("chat.type.text", listOf("Steve", "你好")))
        assertEquals("%s 加入了游戏", TranslatePreview.render("multiplayer.player.joined", emptyList()))
        assertEquals("custom.key(a, b)", TranslatePreview.render("custom.key", listOf("a", "b")))
    }

    @Test
    fun insertComponentAtSplitsText() {
        val out = TextComponentHelper.insertComponentAt(
            "hello", 2, TextComponent(ComponentType.TRANSLATE, "chat.type.text")
        )
        val comps = TextComponentHelper.parseTextComponents(out)
        assertEquals(3, comps.size)
        assertEquals("he", comps[0].content)
        assertEquals(ComponentType.TRANSLATE, comps[1].type)
        assertEquals("llo", comps[2].content)
    }
}
