package com.tellraw.app.util

import com.tellraw.app.util.ExecuteConverter.Direction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 复核审计报告中的 execute 转换问题。 */
class ExecuteConverterAuditTest {

    private fun conv(prefix: String): Pair<String?, List<String>> =
        convDir(prefix, Direction.BEDROCK_TO_JAVA)

    private fun convDir(prefix: String, direction: Direction): Pair<String?, List<String>> {
        val reminders = mutableListOf<String>()
        return ExecuteConverter.convertExecutePrefix(prefix, direction, reminders) to reminders
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
    fun unlessEntityWithMultipleNegatedScoresIsDropped() {
        val (out, reminders) = conv("execute unless entity @s[scores={a=!1,b=!2}]")
        val text = out.orEmpty()
        assertFalse(text, text.contains("scores={a=1,b=2}"))
        assertTrue(reminders.any { it.contains("多个 scores=!") })
    }

    @Test
    fun multiCommandTagPlanBuildsAddTellrawRemove() {
        val plan = ExecuteConverter.buildMultiCommandTagPlan(
            "execute as @a if entity @s[tag=x] unless entity @s[tag=y] run tellraw @s {\"text\":\"x\"}",
            Direction.JAVA_TO_BEDROCK,
            tellrawTarget = "@s",
            tellrawJson = "{\"text\":\"x\"}",
            requireUnmergeable = false,
            tagSuffix = "test"
        )
        requireNotNull(plan)
        assertEquals(5, plan.commands.size)
        assertTrue(plan.commands[0], plan.commands[0].contains("run tag @s add tellraw_a1_test"))
        assertTrue(plan.commands[1], plan.commands[1].contains("run tag @s add tellraw_a2_test"))
        assertTrue(plan.commands[2], plan.commands[2].startsWith("tellraw @a[tag=tellraw_a1_test,tag=tellraw_a2_test]"))
        assertTrue(plan.commands[3], plan.commands[3].contains("remove tellraw_a1_test"))
        assertTrue(plan.commands[4], plan.commands[4].contains("remove tellraw_a2_test"))
    }

    @Test
    fun multiCommandTagPlanRejectsUnsafeBasesAndSanitizesTags() {
        val conditions = "if entity @s[tag=x] unless entity @s[tag=y] run tellraw @s {\"text\":\"x\"}"
        assertNull(
            ExecuteConverter.buildMultiCommandTagPlan(
                "execute as @a[tag=base] $conditions", Direction.JAVA_TO_BEDROCK,
                "@s", "{\"text\":\"x\"}", requireUnmergeable = false
            )
        )
        assertNull(
            ExecuteConverter.buildMultiCommandTagPlan(
                "execute as @e $conditions", Direction.JAVA_TO_BEDROCK,
                "@s", "{\"text\":\"x\"}", requireUnmergeable = false
            )
        )
        val plan = ExecuteConverter.buildMultiCommandTagPlan(
            "execute as @a $conditions", Direction.JAVA_TO_BEDROCK,
            "@s", "{\"text\":\"x\"}", requireUnmergeable = false, tagSuffix = "a b:c"
        )
        requireNotNull(plan)
        assertTrue(plan.tags.all { it.matches(Regex("[A-Za-z0-9_]+")) })
    }

    @Test
    fun multiCommandTagPlanOnlyForUnmergeableByDefault() {
        val plan = ExecuteConverter.buildMultiCommandTagPlan(
            "execute as @a if entity @s[tag=x] unless entity @s[tag=y] run tellraw @s {\"text\":\"x\"}",
            Direction.JAVA_TO_BEDROCK,
            tellrawTarget = "@s",
            tellrawJson = "{\"text\":\"x\"}"
        )
        assertNull(plan)
    }

    @Test
    fun unlessItemsOnMultiTargetIsDropped() {
        val (out, reminders) = convDir(
            "execute unless items entity @a hotbar.* minecraft:diamond",
            Direction.JAVA_TO_BEDROCK
        )
        assertFalse(out.orEmpty(), out.orEmpty().contains("quantity=0"))
        assertTrue(reminders.any { it.contains("量词不同") })
    }

    @Test
    fun bedrockHasitemArrayOnMultiTargetIsDropped() {
        val (out, reminders) = conv(
            "execute if entity @a[hasitem=[{item=minecraft:diamond},{item=minecraft:emerald}]]"
        )
        val text = out.orEmpty()
        assertFalse(text, text.contains("if items entity @a"))
        assertTrue(reminders.any { it.contains("同一个目标") })
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
