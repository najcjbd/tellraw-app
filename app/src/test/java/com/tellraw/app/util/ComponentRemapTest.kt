package com.tellraw.app.util

import com.tellraw.app.util.TextComponentHelper.ComponentType
import com.tellraw.app.util.TextComponentHelper.SubComponent
import com.tellraw.app.util.TextComponentHelper.SubComponentType
import com.tellraw.app.util.TextComponentHelper.TextComponent
import org.junit.Assert.assertEquals
import org.junit.Test

/** 组件模式下按纯文本增删时，结构保留、内容按绝对位置重新切分。 */
class ComponentRemapTest {

    private fun text(s: String) = TextComponent(ComponentType.TEXT, s)
    private fun translate(main: String, vararg with: String) = TextComponent(
        ComponentType.TRANSLATE, main,
        with.map { SubComponent(SubComponentType.WITH, it) }.toMutableList()
    )

    @Test
    fun truncatedNonFirstComponentDoesNotStealPreviousContent() {
        val old = listOf(text("ab"), translate("hello", "world"))
        val result = TextComponentHelper.remapComponentContents(old, "abcdef")

        assertEquals(2, result.size)
        assertEquals("ab", result[0].content)
        assertEquals("cdef", result[1].content)
        assertEquals(0, result[1].subComponents.size)
    }

    @Test
    fun partialSubComponentUsesAbsoluteOffset() {
        val old = listOf(text("ab"), translate("hi", "world"))
        val result = TextComponentHelper.remapComponentContents(old, "abcde")

        assertEquals(2, result.size)
        assertEquals("ab", result[0].content)
        assertEquals("cd", result[1].content)
        assertEquals("e", result[1].subComponents.single().content)
    }

    @Test
    fun normalRemapKeepsStructure() {
        val old = listOf(text("ab"), translate("hi", "world"))
        val result = TextComponentHelper.remapComponentContents(old, "aXhiworld")

        assertEquals(2, result.size)
        assertEquals("aX", result[0].content)
        assertEquals("hi", result[1].content)
        assertEquals("world", result[1].subComponents.single().content)
    }

    @Test
    fun appendedTextBecomesTrailingTextComponent() {
        val old = listOf(translate("hi", "world"))
        val result = TextComponentHelper.remapComponentContents(old, "hiworldXY")

        assertEquals(2, result.size)
        assertEquals("hi", result[0].content)
        assertEquals("world", result[0].subComponents.single().content)
        assertEquals("XY", result[1].content)
        assertEquals(ComponentType.TEXT, result[1].type)
    }

    @Test
    fun truncatedMainContentWithoutSubs() {
        val old = listOf(text("ab"), translate("hello"))
        val result = TextComponentHelper.remapComponentContents(old, "abcd")

        assertEquals(2, result.size)
        assertEquals("cd", result[1].content)
        assertEquals(0, result[1].subComponents.size)
    }
}
