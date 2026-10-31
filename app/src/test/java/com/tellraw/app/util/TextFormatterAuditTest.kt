package com.tellraw.app.util

import com.google.gson.JsonParser
import com.tellraw.app.util.TextComponentHelper.ComponentType
import com.tellraw.app.util.TextComponentHelper.TextComponent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 复核审计报告中的 § 渲染/继承问题（纯 JVM）。 */
class TextFormatterAuditTest {

    private fun java(text: String, warnings: MutableList<String>? = null): String =
        TextFormatter.convertToJavaJson(text, context = null, warnings = warnings)

    @Test
    fun resetDoesNotInheritRootColor() {
        val root = JsonParser.parseString(java("§cA§rB")).asJsonObject
        assertEquals("A", root.get("text").asString)
        assertEquals("red", root.get("color").asString)
        val b = root.getAsJsonArray("extra")[0].asJsonObject
        assertEquals("B", b.get("text").asString)
        assertEquals("reset", b.get("color").asString)
    }

    @Test
    fun colorCodeClearsInheritedBold() {
        val root = JsonParser.parseString(java("A§lB§cC")).asJsonObject
        val extra = root.getAsJsonArray("extra")
        val b = extra[0].asJsonObject
        assertEquals(true, b.get("bold").asBoolean)
        val c = extra[1].asJsonObject
        assertEquals("red", c.get("color").asString)
        assertFalse("§c 之后不应继续继承粗体", c.has("bold"))
    }

    @Test
    fun uppercaseSectionCodesAreRecognized() {
        val root = JsonParser.parseString(java("§AHello")).asJsonObject
        assertEquals("Hello", root.get("text").asString)
        assertEquals("green", root.get("color").asString)
    }

    @Test
    fun legacyHexColorIsWarned() {
        val warnings = mutableListOf<String>()
        java("§x§F§F§0§0§0§0Hi", warnings)
        assertTrue(warnings.joinToString(), warnings.any { it.contains("§x") })
    }

    @Test
    fun textComponentSectionCodesAreSplitInMixedComponentOutput() {
        val components = listOf(
            TextComponent(ComponentType.TEXT, "§cA§aB"),
            TextComponent(ComponentType.SCORE, "Steve:kills")
        )
        val json = TextComponentHelper.convertToJavaJson(components, context = null)
        assertFalse("TEXT 里的 § 代码不应原样出现在 JSON：$json", json.contains("§"))
        assertTrue(json, json.contains("\"color\":\"red\""))
        assertTrue(json, json.contains("\"color\":\"green\""))
        assertTrue(json, json.contains("\"score\""))
    }
}
