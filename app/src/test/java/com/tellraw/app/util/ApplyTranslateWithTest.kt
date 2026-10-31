package com.tellraw.app.util

import com.tellraw.app.util.TextComponentHelper.ComponentType
import com.tellraw.app.util.TextComponentHelper.SubComponent
import com.tellraw.app.util.TextComponentHelper.SubComponentType
import com.tellraw.app.util.TextComponentHelper.TextComponent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ApplyTranslateWithTest {

    private fun text(s: String) = TextComponent(ComponentType.TEXT, s)

    private fun translate(key: String, vararg subs: SubComponent) =
        TextComponent(ComponentType.TRANSLATE, key, subs.toMutableList())

    private fun markers(vararg components: TextComponent) =
        TextComponentHelper.componentsToText(components.toList())

    private fun parse(markers: String) = TextComponentHelper.parseTextComponents(markers)

    private fun withJsonOf(component: TextComponent): String? =
        component.subComponents.firstOrNull { it.type == SubComponentType.WITH_JSON }?.content

    @Test
    fun replacesPlainWithAndKeepsFallback() {
        val start = markers(
            translate(
                "old",
                SubComponent(SubComponentType.WITH, "a,b"),
                SubComponent(SubComponentType.FALLBACK, "fb")
            )
        )
        val result = TextComponentHelper.applyTranslateWithAt(
            start, 0, "new.key", "fb2", """[{"text":"x"}]"""
        )
        val component = parse(result).single()
        assertEquals(ComponentType.TRANSLATE, component.type)
        assertEquals("new.key", component.content)
        assertEquals("fb2", component.subComponents.first { it.type == SubComponentType.FALLBACK }.content)
        assertEquals("""[{"text":"x"}]""", withJsonOf(component))
        assertTrue(component.subComponents.none { it.type == SubComponentType.WITH })
    }

    @Test
    fun createsNewTranslateBySplittingText() {
        val start = markers(text("abcdef"))
        val result = TextComponentHelper.applyTranslateWithAt(start, 3, "key", "", "[]")
        val components = parse(result)
        assertEquals(3, components.size)
        assertEquals("abc", components[0].content)
        assertEquals(ComponentType.TRANSLATE, components[1].type)
        assertEquals("key", components[1].content)
        assertTrue(components[1].subComponents.isEmpty())
        assertEquals("def", components[2].content)
    }

    @Test
    fun appendsAtEnd() {
        val start = markers(text("abcdef"))
        val result = TextComponentHelper.applyTranslateWithAt(start, 6, "key", "", "[]")
        val components = parse(result)
        assertEquals(2, components.size)
        assertEquals("abcdef", components[0].content)
        assertEquals(ComponentType.TRANSLATE, components[1].type)
    }

    @Test
    fun targetsOnlyTranslateUnderCursor() {
        val start = markers(
            translate("one", SubComponent(SubComponentType.WITH_JSON, """[{"text":"1"}]""")),
            translate("two", SubComponent(SubComponentType.WITH_JSON, """[{"text":"2"}]"""))
        )
        // 可见长度：第一个 = "one"(3) + JSON(14) = 17；光标 18 落在第二个 translate 内
        val result = TextComponentHelper.applyTranslateWithAt(
            start, 18, "TWO", "", """[{"text":"x"}]"""
        )
        val components = parse(result)
        assertEquals("one", components[0].content)
        assertEquals("""[{"text":"1"}]""", withJsonOf(components[0]))
        assertEquals("TWO", components[1].content)
        assertEquals("""[{"text":"x"}]""", withJsonOf(components[1]))
    }

    @Test
    fun cursorAtTranslateEndEditsThatTranslate() {
        val start = markers(text("ab"), translate("k"))
        val result = TextComponentHelper.applyTranslateWithAt(start, 3, "K2", "", "[]")
        val components = parse(result)
        assertEquals(2, components.size)
        assertEquals("K2", components[1].content)
    }

    @Test
    fun emptyArgsRemovesWithButKeepsFallback() {
        val start = markers(
            translate(
                "k",
                SubComponent(SubComponentType.WITH_JSON, """[{"text":"x"}]"""),
                SubComponent(SubComponentType.FALLBACK, "fb")
            )
        )
        val result = TextComponentHelper.applyTranslateWithAt(start, 0, "k", "fb", "[]")
        val component = parse(result).single()
        assertTrue(component.subComponents.none { it.type == SubComponentType.WITH_JSON })
        assertEquals("fb", component.subComponents.first { it.type == SubComponentType.FALLBACK }.content)
    }

    @Test
    fun blankFallbackRemovesFallback() {
        val start = markers(
            translate("k", SubComponent(SubComponentType.FALLBACK, "fb"))
        )
        val result = TextComponentHelper.applyTranslateWithAt(start, 0, "k", "", "[]")
        val component = parse(result).single()
        assertTrue(component.subComponents.none { it.type == SubComponentType.FALLBACK })
    }

    @Test
    fun objectArgsAreCanonicalizedToArray() {
        val start = markers(translate("k"))
        val result = TextComponentHelper.applyTranslateWithAt(start, 0, "k", "", """{"text":"a"}""")
        assertEquals("""[{"text":"a"}]""", withJsonOf(parse(result).single()))
    }

    @Test
    fun jsonIsNotDoubleEscaped() {
        val args = """[{"text":"a_b\\c"}]"""
        val start = markers(translate("k"))
        val result = TextComponentHelper.applyTranslateWithAt(start, 0, "k", "", args)
        assertEquals(
            WithArgCodec.normalizeArgsJson(args),
            withJsonOf(parse(result).single())
        )
    }

    @Test
    fun invalidArgsThrows() {
        val start = markers(translate("k"))
        assertThrows(IllegalArgumentException::class.java) {
            TextComponentHelper.applyTranslateWithAt(start, 0, "k", "", "[")
        }
        assertThrows(IllegalArgumentException::class.java) {
            TextComponentHelper.applyTranslateWithAt(start, 0, " ", "", "[]")
        }
    }

    @Test
    fun emptyMarkersCreatesTranslate() {
        val result = TextComponentHelper.applyTranslateWithAt("", 0, "key", "", "[]")
        val component = parse(result).single()
        assertEquals(ComponentType.TRANSLATE, component.type)
        assertEquals("key", component.content)
    }

    @Test
    fun findTranslateIndexUsesInclusiveBounds() {
        val components = parse(markers(text("ab"), translate("k"), text("cd")))
        assertEquals(1, TextComponentHelper.findTranslateIndexAt(components, 2))
        assertEquals(1, TextComponentHelper.findTranslateIndexAt(components, 3))
        assertEquals(null, TextComponentHelper.findTranslateIndexAt(components, 1))
        assertEquals(null, TextComponentHelper.findTranslateIndexAt(components, -1))
    }

    @Test
    fun appliedNestedArgsFlowIntoJavaAndBedrockJson() {
        val args = """[{"translate":"chat.type.text","with":[{"text":"Alex","color":"gold"}]}]"""
        val start = markers(translate("outer"))
        val updated = TextComponentHelper.applyTranslateWithAt(start, 0, "outer", "", args)
        val components = parse(updated)

        val java = TextComponentHelper.convertToJavaJson(components)
        assertTrue(java.contains("\"translate\":\"chat.type.text\""))
        assertTrue(java.contains("\"color\":\"gold\""))
        assertTrue(java.contains("\"with\""))

        val warnings = mutableListOf<String>()
        val bedrock = TextComponentHelper.convertToBedrockJson(components, warnings = warnings)
        assertTrue(bedrock.contains("chat.type.text"))
        assertTrue("应提示嵌套样式被基岩版忽略：$warnings", warnings.any { "样式" in it })
    }
}
