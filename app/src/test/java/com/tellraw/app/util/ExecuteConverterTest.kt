package com.tellraw.app.util

import com.tellraw.app.util.ExecuteConverter.Direction
import org.junit.Assert.*
import org.junit.Test

/**
 * execute 前置命令的解析 / 条件互转 / 输出组合测试。
 * 期望值均由 JVM 试验台（/tmp/vharness）实测得到，不写"想当然"的字符串。
 */
class ExecuteConverterTest {

    private fun conv(prefix: String, direction: Direction): Pair<String?, List<String>> {
        val reminders = mutableListOf<String>()
        return ExecuteConverter.convertExecutePrefix(prefix, direction, reminders) to reminders
    }

    // ---------------------------------------------------------------
    //  ③ 输出组合 + run 处理（需求七.3、八【run】）
    // ---------------------------------------------------------------

    @Test
    fun testComposeTellrawCommand() {
        assertEquals(
            "execute as @a run tellraw @a {\"text\":\"hi\"}",
            ExecuteConverter.composeTellrawCommand("execute as @a", "tellraw @a {\"text\":\"hi\"}")
        )
    }

    @Test
    fun testRunWithoutTrailingTokens() {
        val (out, _) = conv("execute as @a run", Direction.BEDROCK_TO_JAVA)
        assertEquals("execute as @a", out)
    }

    @Test
    fun testRunWithTrailingTellrawDroppedAndReported() {
        val (out, reminders) = conv("execute as @a run tellraw @p {\"text\":\"x\"}", Direction.BEDROCK_TO_JAVA)
        assertEquals("execute as @a", out)
        assertTrue("应提示 run 之后的内容被丢弃、tellraw 由本 App 重新生成",
            reminders.any { it.contains("run tellraw") && it.contains("丢弃") })
    }

    @Test
    fun testRunWithTrailingNonTellrawDroppedAndReported() {
        val (out, reminders) = conv("execute as @a run say hello", Direction.BEDROCK_TO_JAVA)
        assertEquals("execute as @a", out)
        assertTrue(reminders.any { it.contains("丢弃") && it.contains("2 个 token") })
    }

    @Test
    fun testBareExecuteKeepsExecuteKeyword() {
        val (out, _) = conv("execute", Direction.JAVA_TO_BEDROCK)
        assertEquals("execute", out)
    }

    @Test
    fun testPrefixWithoutLeadingExecuteIsAccepted() {
        val (out, _) = conv("as @a if entity @s[tag=x]", Direction.BEDROCK_TO_JAVA)
        assertEquals("execute as @a if entity @s[tag=x]", out)
    }

    // ---------------------------------------------------------------
    //  解析失败 / 放弃并继续（需求七.2、C19）
    // ---------------------------------------------------------------

    @Test
    fun testUnbalancedBracketsReturnsNull() {
        val (out, _) = conv("execute as @a if entity @s[tag=x", Direction.BEDROCK_TO_JAVA)
        assertNull(out)
    }

    @Test
    fun testLeadingIfWithoutExecuteReturnsNull() {
        val (out, _) = conv("if entity @s[tag=x]", Direction.BEDROCK_TO_JAVA)
        assertNull(out)
    }

    @Test
    fun testIncompleteConditionAbandonedButRestKept() {
        val (out, reminders) = conv("execute as @a if entity", Direction.BEDROCK_TO_JAVA)
        assertEquals("execute as @a", out)
        assertTrue(reminders.any { it.contains("参数不完整") })
    }

    @Test
    fun testUnknownSubcommandAbandoned() {
        val (out, reminders) = conv("execute as @a frobnicate @s", Direction.BEDROCK_TO_JAVA)
        assertEquals("execute as @a", out)
        assertTrue(reminders.any { it.contains("未知的子命令") })
    }

    @Test
    fun testUnknownConditionTypeAbandoned() {
        val (out, reminders) = conv("execute as @a if weird @s", Direction.BEDROCK_TO_JAVA)
        assertEquals("execute as @a", out)
        assertTrue(reminders.any { it.contains("未知的条件类型") })
    }

    // ---------------------------------------------------------------
    //  Java -> 基岩
    // ---------------------------------------------------------------

    @Test
    fun testJavaItemsToBedrockKeepsSpecificSlot() {
        val (out, _) = conv(
            "execute as @a if items entity @s armor.head minecraft:diamond", Direction.JAVA_TO_BEDROCK
        )
        assertEquals(
            "execute as @a if entity @s[hasitem={item=diamond,location=slot.armor.head,slot=0}]", out
        )
    }

    @Test
    fun testJavaUnlessItemsWithoutCountBecomesQuantityZero() {
        val (out, reminders) = conv(
            "execute as @a unless items entity @s armor.head minecraft:diamond", Direction.JAVA_TO_BEDROCK
        )
        assertEquals(
            "execute as @a if entity @s[hasitem={item=diamond,location=slot.armor.head,slot=0,quantity=0}]", out
        )
        assertTrue(reminders.any { it.contains("quantity=0") })
    }

    @Test
    fun testJavaScoreMatchesToBedrockScores() {
        val (out, _) = conv("execute if score @s points matches 1..5", Direction.JAVA_TO_BEDROCK)
        assertEquals("execute if entity @s[scores={points=1..5}]", out)
    }

    @Test
    fun testJavaScoreComparisonKeptAsIs() {
        val (out, _) = conv("execute if score @s points > @s other", Direction.JAVA_TO_BEDROCK)
        assertEquals("execute if score @s points > @s other", out)
    }

    @Test
    fun testJavaOnlyPredicateDroppedWithReminder() {
        val (out, reminders) = conv("execute as @a if predicate minecraft:foo", Direction.JAVA_TO_BEDROCK)
        assertEquals("execute as @a", out)
        assertTrue(reminders.any { it.contains("predicate") && it.contains("舍弃") })
    }

    // ---------------------------------------------------------------
    //  基岩 -> Java：hasitem / scores
    // ---------------------------------------------------------------

    @Test
    fun testBedrockHasitemUniqueSlotWithoutSlotConverts() {
        // 第十节 16：缺 slot 不等于"无法确定槽位"，单槽位 location 照常精确转换
        val (out, reminders) = conv(
            "execute as @a if entity @s[hasitem={item=diamond,location=slot.armor.head}]",
            Direction.BEDROCK_TO_JAVA
        )
        assertEquals("execute as @a if items entity @s armor.head minecraft:diamond", out)
        assertTrue(reminders.any { it.contains("唯一确定槽位") })
    }

    @Test
    fun testBedrockAirUniqueSlotWithoutSlotConverts() {
        val (out, _) = conv(
            "execute as @a if entity @s[hasitem={item=air,location=slot.armor.head}]",
            Direction.BEDROCK_TO_JAVA
        )
        assertEquals("execute as @a unless items entity @s armor.head *", out)
    }

    @Test
    fun testBedrockAirMultiSlotDroppedNotDiscardedSilently() {
        val (out, reminders) = conv(
            "execute as @a if entity @s[hasitem={item=air,location=slot.hotbar}]",
            Direction.BEDROCK_TO_JAVA
        )
        assertEquals("execute as @a", out)
        assertTrue("必须提醒", reminders.any { it.contains("多槽位物品栏") })
    }

    @Test
    fun testBedrockQuantityZeroBecomesUnlessItems() {
        val (out, reminders) = conv(
            "execute as @a if entity @s[hasitem={item=diamond,quantity=0}]",
            Direction.BEDROCK_TO_JAVA
        )
        assertEquals("execute as @a unless items entity @s * minecraft:diamond", out)
        assertTrue(reminders.any { it.contains("没有该物品") })
    }

    @Test
    fun testBedrockScoresSingleNegationUsesExistenceCheck() {
        val (out, _) = conv("execute as @a if entity @s[scores={n=!5}]", Direction.BEDROCK_TO_JAVA)
        assertEquals(
            "execute as @a if score @s n matches -2147483648..2147483647 " +
                "unless score @s n matches 5",
            out
        )
    }

    @Test
    fun testBedrockScoresMultipleNegationUsesNotConjunction() {
        val (out, _) = conv("execute as @a if entity @s[scores={n=!5,m=!6}]", Direction.BEDROCK_TO_JAVA)
        assertEquals("execute as @a unless entity @s[scores={n=5,m=6}]", out)
    }

    @Test
    fun testBedrockScoresMixedNegationAndPositive() {
        val (out, _) = conv("execute as @a if entity @s[scores={n=!5,m=6}]", Direction.BEDROCK_TO_JAVA)
        assertEquals(
            "execute as @a if entity @s[scores={m=6}] " +
                "if score @s n matches -2147483648..2147483647 unless score @s n matches 5",
            out
        )
    }

    @Test
    fun testBedrockPlainEntityConditionKeptInBothDirections() {
        val (java, _) = conv("execute as @a if entity @s[tag=x,type=player]", Direction.BEDROCK_TO_JAVA)
        val (bedrock, _) = conv("execute as @a if entity @s[tag=x,type=player]", Direction.JAVA_TO_BEDROCK)
        assertEquals("execute as @a if entity @s[tag=x,type=player]", java)
        assertEquals("execute as @a if entity @s[tag=x,type=player]", bedrock)
    }

    // ---------------------------------------------------------------
    //  slot 取反（=!N）/ 范围：Java 无槽位取反与槽位集合写法（目标选择器.txt:609）
    // ---------------------------------------------------------------

    @Test
    fun testSlotNegationUsesUnlessSpecificSlot() {
        val (out, reminders) = conv(
            "execute as @a if entity @s[hasitem={item=diamond,location=slot.hotbar,slot=!0}]",
            Direction.BEDROCK_TO_JAVA
        )
        assertEquals("execute as @a unless items entity @s hotbar.0 minecraft:diamond", out)
        assertTrue(reminders.any { it.contains("槽位取反") })
    }

    @Test
    fun testSlotNegationFlipsWithEnclosingUnless() {
        val (out, _) = conv(
            "execute as @a unless entity @s[hasitem={item=diamond,location=slot.hotbar,slot=!0}]",
            Direction.BEDROCK_TO_JAVA
        )
        assertEquals("execute as @a if items entity @s hotbar.0 minecraft:diamond", out)
    }

    @Test
    fun testAirSlotNegationInvertsPolarity() {
        val (out, _) = conv(
            "execute as @a if entity @s[hasitem={item=air,location=slot.armor.head,slot=!0}]",
            Direction.BEDROCK_TO_JAVA
        )
        assertEquals("execute as @a if items entity @s armor.head *", out)
    }

    @Test
    fun testSlotRangeWidensToWildcardNotSilentStart() {
        val (out, reminders) = conv(
            "execute as @a if entity @s[hasitem={item=diamond,location=slot.hotbar,slot=0..2}]",
            Direction.BEDROCK_TO_JAVA
        )
        assertEquals("execute as @a if items entity @s hotbar.* minecraft:diamond", out)
        assertTrue("范围必须提醒，不许静默取起点", reminders.any { it.contains("覆盖多个槽位") })
    }

    @Test
    fun testAirMultiSlotWithoutSlotStaysDroppedWithAccurateWording() {
        val (out, reminders) = conv(
            "execute as @a if entity @s[hasitem={item=air,location=slot.hotbar}]",
            Direction.BEDROCK_TO_JAVA
        )
        assertEquals("execute as @a", out)
        assertTrue(reminders.any { it.contains("至少有一个空格") })
    }

    @Test
    fun testJavaNonZeroStartRangeDegradesWithoutQuantity() {
        val (out, reminders) = conv(
            "execute as @a if items entity @s hotbar.* minecraft:diamond[count~{min:2,max:5}]",
            Direction.JAVA_TO_BEDROCK
        )
        assertEquals("execute as @a if entity @s[hasitem={item=diamond,location=slot.hotbar}]", out)
        assertTrue(reminders.any { it.contains("非 0 起始区间") })
    }

    @Test
    fun testJavaZeroStartRangeKeepsQuantity() {
        val (out, _) = conv(
            "execute as @a if items entity @s hotbar.* minecraft:diamond[count~{min:0,max:4}]",
            Direction.JAVA_TO_BEDROCK
        )
        assertEquals("execute as @a if entity @s[hasitem={item=diamond,location=slot.hotbar,quantity=0..4}]", out)
    }

    // ---------------------------------------------------------------
    //  选择器级否定（需求二.3；Java 的选择器写不出来，要靠 execute 前缀）
    // ---------------------------------------------------------------

    @Test
    fun testSelectorNegationSplitsScoresAndKeepsOtherParams() {
        val neg = ExecuteConverter.bedrockSelectorNegation("@a[tag=x,scores={n=!5}]", mutableListOf())
        assertNotNull(neg)
        assertEquals("@a[tag=x]", neg!!.first)
        assertEquals(
            listOf("if", "score", "@s", "n", "matches", "-2147483648..2147483647",
                "unless", "score", "@s", "n", "matches", "5"),
            neg.second
        )
    }

    @Test
    fun testSelectorNegationForQuantityZero() {
        val neg = ExecuteConverter.bedrockSelectorNegation("@a[hasitem={item=diamond,quantity=0}]", mutableListOf())
        assertNotNull(neg)
        assertEquals("@a", neg!!.first)
        assertEquals(listOf("unless", "items", "entity", "@s", "*", "minecraft:diamond"), neg.second)
    }

    @Test
    fun testSelectorNegationForAir() {
        val neg = ExecuteConverter.bedrockSelectorNegation(
            "@a[hasitem={item=air,location=slot.armor.head}]", mutableListOf()
        )
        assertNotNull(neg)
        assertEquals(listOf("unless", "items", "entity", "@s", "armor.head", "*"), neg!!.second)
    }

    @Test
    fun testPositiveHasitemStaysOnExistingPath() {
        // 正向"有某物品"不拆进 execute，仍交给原有的 nbt= 路径（避免无谓改动既有输出）
        assertNull(
            ExecuteConverter.bedrockSelectorNegation(
                "@a[hasitem={item=diamond,location=slot.armor.head}]", mutableListOf()
            )
        )
    }

    @Test
    fun testSelectorWithoutNegationReturnsNull() {
        assertNull(ExecuteConverter.bedrockSelectorNegation("@a[tag=x,scores={n=5}]", mutableListOf()))
        assertNull(ExecuteConverter.bedrockSelectorNegation("@a", mutableListOf()))
    }

    @Test
    fun testUserPrefixComesFirstThenOurTokens() {
        // execute需求 第十一节 9：用户写的前置在前、必须遵从；缺的（as 等）补在其后
        val neg = ExecuteConverter.bedrockSelectorNegation("@a[scores={n=!5}]", mutableListOf())!!
        val merged = "execute as @p at @s as ${neg.first} ${neg.second.joinToString(" ")}"
        assertEquals(
            "execute as @p at @s as @a if score @s n matches -2147483648..2147483647 " +
                "unless score @s n matches 5 run tellraw @s {\"text\":\"hi\"}",
            ExecuteConverter.composeTellrawCommand(merged, "tellraw @s {\"text\":\"hi\"}")
        )
    }
}
