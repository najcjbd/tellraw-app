package com.tellraw.app.util

import com.tellraw.app.util.TextComponentHelper.ComponentType
import com.tellraw.app.util.TextComponentHelper.SubComponent
import com.tellraw.app.util.TextComponentHelper.SubComponentType
import com.tellraw.app.util.TextComponentHelper.TextComponent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 复核审计报告中的文本编辑/插入问题。 */
class TextEditAuditTest {

    private fun text(s: String) = TextComponent(ComponentType.TEXT, s)
    private fun translate(key: String, vararg subs: SubComponent) =
        TextComponent(ComponentType.TRANSLATE, key, subs.toMutableList())

    private fun markers(vararg c: TextComponent) =
        TextComponentHelper.componentsToText(c.toList())

    @Test
    fun typingInsideSubComponentDoesNotCrash() {
        val start = markers(translate("k", SubComponent(SubComponentType.WITH, "world")))
        // 可见文本 kworld；位置 4 在 with 的 "wor|ld" 之间
        val updated = TextComponentHelper.insertTextWithComponent(start, 4, "X", ComponentType.TRANSLATE)
        val c = TextComponentHelper.parseTextComponents(updated).single()
        assertEquals("k", c.content)
        assertEquals("worXld", c.subComponents.single().content)
    }

    @Test
    fun insertAtPositionZeroGoesBeforeLeadingNonText() {
        val start = markers(translate("k"))
        val updated = TextComponentHelper.insertComponentAt(start, 0, text(","))
        val components = TextComponentHelper.parseTextComponents(updated)
        assertEquals(ComponentType.TEXT, components[0].type)
        assertEquals(",", components[0].content)
        assertEquals(ComponentType.TRANSLATE, components[1].type)
    }

    @Test
    fun diffTextEditFindsReplacementRange() {
        val e = TextComponentHelper.diffTextEdit("abcdef", "aXYZQQef")
        assertEquals(1, e.start)
        assertEquals(3, e.removedLength)
        assertEquals("XYZQQ", e.insertedText)

        val ins = TextComponentHelper.diffTextEdit("abc", "aXbc")
        assertEquals(1, ins.start)
        assertEquals(0, ins.removedLength)
        assertEquals("X", ins.insertedText)
    }

    @Test
    fun boundaryInsertStaysInPrecedingComponent() {
        val old = listOf(text("ab"), translate("hi", SubComponent(SubComponentType.WITH, "world")))
        val result = TextComponentHelper.remapComponentContentsForEdit(old, "abhiworld", "abXhiworld")
        requireNotNull(result)
        assertEquals(2, result.size)
        assertEquals("abX", result[0].content)
        assertEquals("hi", result[1].content)
        assertEquals("world", result[1].subComponents.single().content)
    }

    @Test
    fun replacementInsideSubComponentStaysThere() {
        val old = listOf(text("ab"), translate("hi", SubComponent(SubComponentType.WITH, "world")))
        val result = TextComponentHelper.remapComponentContentsForEdit(old, "abhiworld", "abhiW")
        requireNotNull(result)
        assertEquals("ab", result[0].content)
        assertEquals("hi", result[1].content)
        assertEquals("W", result[1].subComponents.single().content)
    }

    @Test
    fun crossSegmentEditReturnsNullForFallback() {
        val old = listOf(text("ab"), translate("hi", SubComponent(SubComponentType.WITH, "world")))
        // 删除 "bhi"：跨 TEXT 与 translate 主内容
        val result = TextComponentHelper.remapComponentContentsForEdit(old, "abhiworld", "aworld")
        assertNull(result)
    }

    @Test
    fun appendAtEndGoesToLastSegment() {
        val old = listOf(text("ab"), translate("hi", SubComponent(SubComponentType.WITH, "world")))
        val result = TextComponentHelper.remapComponentContentsForEdit(old, "abhiworld", "abhiworldX")
        requireNotNull(result)
        assertEquals("ab", result[0].content)
        assertEquals("worldX", result[1].subComponents.single().content)
        assertTrue(result.size == 2)
    }
}
