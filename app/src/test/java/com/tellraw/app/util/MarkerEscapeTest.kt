package com.tellraw.app.util

import com.tellraw.app.util.TextComponentHelper.ComponentType
import com.tellraw.app.util.TextComponentHelper.SubComponent
import com.tellraw.app.util.TextComponentHelper.SubComponentType
import com.tellraw.app.util.TextComponentHelper.TextComponent
import org.junit.Assert.*
import org.junit.Test

class MarkerEscapeTest {

    @Test
    fun userSensitiveSymbolsRoundTrip() {
        val content = "a_b__c\\\u0FC8d\u0F34e\n"
        val markers = TextComponentHelper.componentsToText(
            listOf(TextComponent(ComponentType.TEXT, content))
        )
        val parsed = TextComponentHelper.parseTextComponents(markers)
        assertEquals(1, parsed.size)
        assertEquals(content, parsed[0].content)
        assertEquals(content, TextComponentHelper.stripComponentMarkers(markers))
    }

    @Test
    fun subcomponentContentWithUnderscoresAndMarkersRoundTrips() {
        val withContent = """{"text":"a__b_\u0FC8c\u0F34","color":"red"}"""
        val comp = TextComponent(
            ComponentType.TRANSLATE, "k",
            mutableListOf(SubComponent(SubComponentType.WITH_JSON, withContent))
        )
        val markers = TextComponentHelper.componentsToText(listOf(comp))
        val parsed = TextComponentHelper.parseTextComponents(markers)
        assertEquals(1, parsed.size)
        assertEquals("k", parsed[0].content)
        assertEquals(withContent, parsed[0].subComponents[0].content)
    }

    @Test
    fun fuzzSensitiveCombinationsRoundTrip() {
        val parts = listOf("a", "_", "\\", "\u0FC8", "\u0F34", "__", ",", "{", "}", "[", "]", ":", "\"", "'", "\n")
        var count = 0
        for (a in parts) for (b in parts) for (c in parts) {
            val content = a + b + c
            val comp = TextComponent(ComponentType.TEXT, content)
            val markers = TextComponentHelper.componentsToText(listOf(comp))
            val parsed = TextComponentHelper.parseTextComponents(markers)
            if (parsed.size != 1 || parsed[0].content != content) {
                fail("TEXT round-trip failed for <$content>: $parsed")
            }
            count++
        }
        assertTrue(count > 1000)
    }

    @Test
    fun withModeMigrationCarriesContent() {
        val plain = TextComponent(
            ComponentType.TRANSLATE, "k",
            mutableListOf(SubComponent(SubComponentType.WITH, "a,b"))
        )
        val json = TextComponentHelper.migrateWithMode(plain, toJson = true)
        val jsonSub = json.subComponents.first { it.type == SubComponentType.WITH_JSON }
        assertTrue(jsonSub.content, jsonSub.content.contains("\"a\"") && jsonSub.content.contains("\"b\""))

        val back = TextComponentHelper.migrateWithMode(json, toJson = false)
        assertEquals("a,b", back.subComponents.first { it.type == SubComponentType.WITH }.content)

        // 多次交换：纯文本内容稳定
        val again = TextComponentHelper.migrateWithMode(back, toJson = true)
        val back2 = TextComponentHelper.migrateWithMode(again, toJson = false)
        assertEquals("a,b", back2.subComponents.first { it.type == SubComponentType.WITH }.content)

        // JSON 里的逗号降级为纯文本时要写成 \,
        val json2 = TextComponent(
            ComponentType.TRANSLATE, "k",
            mutableListOf(SubComponent(SubComponentType.WITH_JSON, """[{"text":"a,b"}]"""))
        )
        val back2Plain = TextComponentHelper.migrateWithMode(json2, toJson = false)
        assertEquals("a\\,b", back2Plain.subComponents.first { it.type == SubComponentType.WITH }.content)
    }
}
