package com.tellraw.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 复核审计报告中的 JSON 解析/样式问题。 */
class CommandInputParserAuditTest {

    private fun parse(input: String) = CommandInputParser.parse(input)

    @Test
    fun nonArrayRawtextReturnsNullInsteadOfCrash() {
        assertNull(parse("""{"rawtext":{"text":"a"}}"""))
        assertNull(parse("""{"rawtext":null}"""))
    }

    @Test
    fun nullInWithIsIgnored() {
        val r = parse("""{"translate":"k","with":[null]}""")
        assertNotNull(r)
        assertTrue(r!!.warnings.any { "null" in it })
        assertTrue(r.components.single().subComponents.isEmpty())
    }

    @Test
    fun primitiveWithKeepsCommasAndEmptyArgs() {
        val r = parse("""{"translate":"k","with":["a,b","c"]}""")
        assertNotNull(r)
        val w = r!!.components.single().subComponents.single()
        assertEquals(TextComponentHelper.SubComponentType.WITH_JSON, w.type)
        val java = TextComponentHelper.convertToJavaJson(r.components)
        assertTrue(java, java.contains("\"a,b\"") && java.contains("\"c\""))

        val empty = parse("""{"translate":"k","with":[""]}""")
        assertNotNull(empty)
        val w2 = empty!!.components.single().subComponents.single()
        assertEquals(TextComponentHelper.SubComponentType.WITH_JSON, w2.type)
    }

    @Test
    fun explicitFalseOverridesInheritedBold() {
        val r = parse("""{"text":"A","bold":true,"extra":[{"text":"B","bold":false}]}""")
        assertNotNull(r)
        val components = r!!.components
        assertEquals("A", components[0].content)
        assertEquals("B", components[1].content)
        assertEquals(true, components[0].style.bold)
        assertEquals(false, components[1].style.bold)
    }

    @Test
    fun rootArraySiblingsDoNotInheritStyles() {
        val r = parse("""[{"text":"A","color":"red"},{"text":"B"}]""")
        assertNotNull(r)
        val json = TextComponentHelper.convertToJavaJson(r!!.components, context = null)
        val root = com.google.gson.JsonParser.parseString(json).asJsonObject
        val b = root.getAsJsonArray("extra").first {
            it.asJsonObject.get("text")?.asString == "B"
        }.asJsonObject
        assertFalse("数组兄弟不应生成 color:white 复位：$b", b.has("color"))
    }

    @Test
    fun unsupportedStyleFieldsArePreserved() {
        val r = parse(
            """{"text":"A","color":"#12AB34","clickEvent":{"action":"run_command","value":"/say hi"},"font":"minecraft:uniform"}"""
        )
        assertNotNull(r)
        val style = r!!.components.single().style
        assertEquals("#12AB34", style.color)
        assertEquals("minecraft:uniform", style.font)
        assertNotNull(style.clickEvent)
        val java = TextComponentHelper.convertToJavaJson(r.components)
        assertTrue(java, java.contains("#12AB34"))
        assertTrue(java, java.contains("clickEvent"))
        assertTrue(java, java.contains("minecraft:uniform"))
    }
}
