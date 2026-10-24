package com.tellraw.app.util

import com.tellraw.app.util.TextComponentHelper.ComponentType
import com.tellraw.app.util.TextComponentHelper.TextComponent
import org.junit.Assert.*
import org.junit.Test

class ContentAsRewriterTest {

    private fun message(vararg parts: Pair<ComponentType, String>): String =
        TextComponentHelper.componentsToText(parts.map { (t, c) -> TextComponent(t, c) })

    @Test
    fun rewritesContentSelectorsAndTarget() {
        val msg = message(
            ComponentType.SELECTOR to "@a[scores={l=!5},tag=6]",
            ComponentType.TEXT to "666",
            ComponentType.SELECTOR to "@a[scores={l=!5},tag=7]",
            ComponentType.TEXT to "91"
        )
        val r = ContentAsRewriter.rewrite("@s", msg, "")
        assertTrue(r.applied)
        assertEquals("@p", r.targetSelector)
        assertEquals(
            listOf(
                "at", "@s", "as", "@a",
                "if", "score", "@s", "l", "matches", "-2147483648..2147483647",
                "unless", "score", "@s", "l", "matches", "5"
            ),
            r.extraTokens
        )
        val rewritten = TextComponentHelper.parseTextComponents(r.message)
        assertEquals("@s[tag=6]", rewritten[0].content)
        assertEquals("@s[tag=7]", rewritten[2].content)
    }

    @Test
    fun keepsTargetWhenItIsNotAtS() {
        val msg = message(ComponentType.SELECTOR to "@e[scores={hp=!5},type=zombie]")
        val r = ContentAsRewriter.rewrite("@a", msg, "")
        assertTrue(r.applied)
        assertEquals("@a", r.targetSelector)
        assertFalse(r.extraTokens.contains("at"))
        assertEquals("@s[type=zombie]", TextComponentHelper.parseTextComponents(r.message)[0].content)
    }

    @Test
    fun existingAsIsNoted() {
        val msg = message(ComponentType.SELECTOR to "@a[scores={l=!5},tag=6]")
        val r = ContentAsRewriter.rewrite("@s", msg, "execute as @e[tag=x]")
        assertTrue(r.applied)
        assertTrue(r.notes.any { it.contains("已有 as") })
    }

    @Test
    fun rewritesHasitemNegationInContent() {
        val msg = message(
            ComponentType.SELECTOR to "@a[hasitem={item=diamond,quantity=0},tag=6]",
            ComponentType.TEXT to "x"
        )
        val r = ContentAsRewriter.rewrite("@s", msg, "")
        assertTrue(r.applied)
        assertEquals("@p", r.targetSelector)
        assertTrue(r.extraTokens.joinToString(" "), r.extraTokens.joinToString(" ").contains("unless"))
        val rewritten = TextComponentHelper.parseTextComponents(r.message)[0].content
        assertEquals("@s[tag=6]", rewritten)
    }

    @Test
    fun noNegativeScoresDoesNotApply() {
        val msg = message(ComponentType.SELECTOR to "@a[scores={l=5},tag=6]")
        val r = ContentAsRewriter.rewrite("@s", msg, "")
        assertFalse(r.applied)
        assertEquals("@s", r.targetSelector)
    }

    @Test
    fun keepsPositiveScoresInContentSelector() {
        val msg = message(ComponentType.SELECTOR to "@a[scores={l=!5,hp=10},tag=6]")
        val r = ContentAsRewriter.rewrite("@a", msg, "")
        val rewritten = TextComponentHelper.parseTextComponents(r.message)[0].content
        assertTrue(rewritten, rewritten.contains("scores={hp=10}"))
        assertFalse(rewritten, rewritten.contains("l=!5"))
    }
}
