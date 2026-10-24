package com.tellraw.app.util

import com.google.gson.JsonParser
import com.tellraw.app.util.TextComponentHelper.ComponentType
import com.tellraw.app.util.TextComponentHelper.SubComponent
import com.tellraw.app.util.TextComponentHelper.SubComponentType
import com.tellraw.app.util.TextComponentHelper.TextComponent
import org.junit.Assert.*
import org.junit.Test

class SpecialSymbolsTest {

    @Test
    fun actualNewlineIsEscapedAndRestored() {
        val json = TextFormatter.convertToJavaJson("a\nb", "font", false, null)
        val text = JsonParser.parseString(json).asJsonObject.get("text").asString
        assertEquals("a\nb", text)
        assertTrue("JSON 里应是 \\n 转义：$json", json.contains("\\n"))
    }

    @Test
    fun literalBackslashNStaysLiteral() {
        val json = TextFormatter.convertToJavaJson("a\\nb", "font", false, null)
        val text = JsonParser.parseString(json).asJsonObject.get("text").asString
        assertEquals("a\\nb", text)
    }

    @Test
    fun translateWithKeepsNewlineAndEscapedComma() {
        val comp = TextComponent(
            ComponentType.TRANSLATE, "chat.type.text",
            mutableListOf(SubComponent(SubComponentType.WITH, "a\\,b,c\nd"))
        )
        val args = TextComponentHelper.parseTranslateArgs("a\\,b,c\nd")
        assertEquals(listOf("a,b", "c\nd"), args)

        val json = TextComponentHelper.convertToJavaJson(listOf(comp))
        val arr = JsonParser.parseString(json).asJsonObject.getAsJsonArray("with")
        assertEquals("a,b", arr[0].asString)
        assertEquals("c\nd", arr[1].asString)
    }

    @Test
    fun percentInTranslateKeyIsNotProcessed() {
        val comp = TextComponent(ComponentType.TRANSLATE, "key.%s.%%")
        val json = TextComponentHelper.convertToJavaJson(listOf(comp))
        assertEquals("key.%s.%%", JsonParser.parseString(json).asJsonObject.get("translate").asString)
    }
}
