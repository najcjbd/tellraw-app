package com.tellraw.app.util

import org.junit.Assert.assertEquals
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
        val contents = r!!.components.map { it.content }
        assertEquals("§lA", contents[0])
        assertEquals("§rB", contents[1])
    }

    @Test
    fun unsupportedStyleFieldsAreWarned() {
        val r = parse(
            """{"text":"A","color":"#12AB34","clickEvent":{"action":"run_command","value":"/say hi"},"font":"minecraft:uniform"}"""
        )
        assertNotNull(r)
        val warnings = r!!.warnings.joinToString("\n")
        assertTrue(warnings, warnings.contains("clickEvent"))
        assertTrue(warnings, warnings.contains("font"))
        assertTrue(warnings, warnings.contains("#12AB34"))
    }
}
