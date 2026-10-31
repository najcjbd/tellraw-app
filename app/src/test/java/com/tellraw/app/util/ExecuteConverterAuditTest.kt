package com.tellraw.app.util

import com.tellraw.app.util.ExecuteConverter.Direction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 复核审计报告中的 execute 转换问题。 */
class ExecuteConverterAuditTest {

    private fun conv(prefix: String): Pair<String?, List<String>> {
        val reminders = mutableListOf<String>()
        return ExecuteConverter.convertExecutePrefix(prefix, Direction.BEDROCK_TO_JAVA, reminders) to reminders
    }

    @Test
    fun unlessEntityWithMultipleConditionsIsDropped() {
        val (out, reminders) = conv("execute unless entity @s[tag=x,hasitem={item=diamond}]")
        val text = out.orEmpty()
        // 不允许输出 ¬tag ∧ ¬item（De Morgan 错误）
        assertFalse(text, text.contains("unless items"))
        assertTrue(reminders.any { it.contains("无法表达 ¬(A∧B)") })
    }

    @Test
    fun positionModifierSelectorNegationIsNotAttachedToWrongExecutor() {
        val (out, reminders) = conv("execute at @a[scores={n=!5}]")
        assertEquals("execute at @a", out)
        assertTrue(reminders.any { it.contains("位置修饰子命令") })
        assertFalse(out.orEmpty().contains("if score"))
    }

    @Test
    fun asModifierSelectorNegationStillWorks() {
        val (out, reminders) = conv("execute as @a[scores={n=!5}]")
        val text = out.orEmpty()
        assertTrue(text, text.contains("as @a"))
        assertTrue(text, text.contains("if score @s n"))
        assertTrue(reminders.isNotEmpty())
    }
}
