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
    fun multiCommandTagPlanFoldsSelectorExpressibleConditionsAndCarriesPrefix() {
        val plan = ExecuteConverter.buildMultiCommandTagPlan(
            "execute as @a at @s " +
                "if items entity @s hotbar.* minecraft:diamond " +
                "if score @s kills matches 5.. " +
                "if data entity @s {foo:1b} " +
                "if predicate my:is_special",
            Direction.BEDROCK_TO_JAVA,
            tellrawTarget = "@s",
            tellrawJson = "{\"text\":\"ok\"}",
            requireUnmergeable = false,
            tagSuffix = "test"
        )
        requireNotNull(plan)
        // predicate 属顺序/随机敏感条件：整链 tag，保持原链求值次数与顺序
        assertEquals(1, plan.tags.size)
        assertEquals(3, plan.commands.size)
        assertTrue(
            plan.commands[0],
            plan.commands[0].startsWith(
                "execute as @a at @s if items entity @s hotbar.* minecraft:diamond " +
                    "if score @s kills matches 5.. if data entity @s {foo:1b} " +
                    "if predicate my:is_special run tag @s add tellraw_all_test"
            )
        )
        assertTrue(
            plan.commands[1],
            plan.commands[1].contains("as @a at @s") &&
                plan.commands[1].contains("tag=tellraw_all_test") &&
                plan.commands[1].contains("run tellraw @s")
        )
        assertEquals("tag @e[tag=tellraw_all_test] remove tellraw_all_test", plan.commands[2])
    }

    @Test
    fun multiCommandTagPlanNotUsedWhenEverythingFoldsIntoSelector() {
        val plan = ExecuteConverter.buildMultiCommandTagPlan(
            "execute as @a if score @s kills matches 5.. if data entity @s {foo:1b}",
            Direction.BEDROCK_TO_JAVA,
            tellrawTarget = "@s",
            tellrawJson = "{\"text\":\"ok\"}",
            requireUnmergeable = false
        )
        assertNull(plan)
    }

    @Test
    fun allOnRelationsAndSummonAreParsed() {
        val relations = listOf(
            "attacker", "controller", "leasher", "origin", "owner", "passengers", "target", "vehicle"
        )
        for (relation in relations) {
            val (out, _) = conv("execute on $relation if score @s obj matches 1..2")
            assertEquals(
                "execute on $relation if score @s obj matches 1..2",
                out
            )
        }
        val (summoned, _) = conv("execute summon minecraft:pig if score @s obj matches 1..2")
        assertEquals(
            "execute summon minecraft:pig if score @s obj matches 1..2",
            summoned
        )

        val (_, invalidOn) = conv("execute on bogus if score @s obj matches 1..2")
        assertTrue(invalidOn.any { it.contains("未知的 on 关系") })

        val (_, bedrockOn) = convDir(
            "execute on owner if score @s obj matches 1..2",
            Direction.JAVA_TO_BEDROCK
        )
        assertTrue(bedrockOn.any { it.contains("基岩 execute 没有 \"on\"") })
    }

    @Test
    fun inDimensionIsCarriedIntoTagAndFinal() {
        val plan = ExecuteConverter.buildMultiCommandTagPlan(
            "execute as @a in minecraft:the_nether if block 0 0 0 minecraft:stone " +
                "if block 0 0 0 minecraft:dirt",
            Direction.BEDROCK_TO_JAVA,
            tellrawTarget = "@a",
            tellrawJson = "{\"text\":\"ok\"}",
            requireUnmergeable = false,
            tagSuffix = "dim"
        )
        requireNotNull(plan)
        assertTrue(plan.commands[0].contains("in minecraft:the_nether"))
        assertTrue(plan.commands[2].contains("in minecraft:the_nether"))
        assertTrue(plan.commands.last().contains("in minecraft:the_nether"))
    }

    @Test
    fun modifierRandomSelectorUsesWholeChain() {
        val plan = ExecuteConverter.buildMultiCommandTagPlan(
            "execute as @a at @r if block ~ ~ ~ minecraft:stone if block ~ ~ ~ minecraft:dirt",
            Direction.BEDROCK_TO_JAVA,
            tellrawTarget = "@a",
            tellrawJson = "{\"text\":\"ok\"}",
            requireUnmergeable = false,
            tagSuffix = "atr"
        )
        requireNotNull(plan)
        assertEquals(1, plan.tags.size)
        assertTrue(
            plan.commands[0].startsWith(
                "execute as @a at @r if block ~ ~ ~ minecraft:stone " +
                    "if block ~ ~ ~ minecraft:dirt run tag @s add tellraw_all_atr"
            )
        )
    }

    @Test
    fun noAsPrefixFallsBackSingleChain() {
        val plan = ExecuteConverter.buildMultiCommandTagPlan(
            "execute if block ~ ~ ~ minecraft:stone if block ~ ~ ~ minecraft:dirt",
            Direction.BEDROCK_TO_JAVA,
            tellrawTarget = "@a",
            tellrawJson = "{\"text\":\"ok\"}",
            requireUnmergeable = false,
            tagSuffix = "noas"
        )
        assertNull(plan)
    }

    @Test
    fun nearestBaseUsesWholeChain() {
        val plan = ExecuteConverter.buildMultiCommandTagPlan(
            "execute as @p if block ~ ~ ~ minecraft:stone if block ~ ~ ~ minecraft:dirt",
            Direction.BEDROCK_TO_JAVA,
            tellrawTarget = "@s",
            tellrawJson = "{\"text\":\"ok\"}",
            requireUnmergeable = false,
            tagSuffix = "p"
        )
        requireNotNull(plan)
        assertEquals(1, plan.tags.size)
        assertTrue(plan.commands[0].startsWith("execute as @p "))
        assertTrue(plan.commands[1].startsWith("execute as @e[tag=tellraw_all_p] run tellraw @s"))
    }

    @Test
    fun recipientSelectorGetsItsOwnTagSet() {
        val plan = ExecuteConverter.buildMultiCommandTagPlan(
            "execute as @a if items entity @s hotbar.* minecraft:diamond if data entity @s {foo:1b}",
            Direction.BEDROCK_TO_JAVA,
            tellrawTarget = "@a[scores={n=!5}]",
            tellrawJson = "{\"text\":\"ok\"}",
            requireUnmergeable = false,
            tagSuffix = "recv"
        )
        requireNotNull(plan)
        assertTrue(plan.tags.contains("tellraw_recv_recv"))
        assertTrue(plan.commands.any { it.contains("run tag @s add tellraw_recv_recv") })
        assertTrue(plan.commands.any { it.contains("tellraw @a[tag=tellraw_recv_recv]") })
        assertTrue(plan.commands.any { it.contains("remove tellraw_recv_recv") })
    }

    @Test
    fun messageSelectorGetsItsOwnTagSetAndJsonIsRewritten() {
        val plan = ExecuteConverter.buildMultiCommandTagPlan(
            "execute as @a if items entity @s hotbar.* minecraft:diamond if data entity @s {foo:1b}",
            Direction.BEDROCK_TO_JAVA,
            tellrawTarget = "@a",
            tellrawJson = "{\"text\":\"目标：\",\"extra\":[{\"selector\":\"@a[scores={n=!5}]\"}]}",
            requireUnmergeable = false,
            tagSuffix = "msg"
        )
        requireNotNull(plan)
        assertTrue(plan.tags.contains("tellraw_msg_0_msg"))
        assertTrue(plan.commands.any { it.contains("run tag @s add tellraw_msg_0_msg") })
        assertTrue(plan.commands.any { it.contains("\"selector\":\"@a[tag=tellraw_msg_0_msg]\"") })
        assertTrue(plan.commands.any { it.contains("remove tellraw_msg_0_msg") })
    }

    @Test
    fun executorRecipientAndMessageSelectorsUseSeparateTags() {
        val plan = ExecuteConverter.buildMultiCommandTagPlan(
            "execute as @a if items entity @s hotbar.* minecraft:diamond if data entity @s {foo:1b}",
            Direction.BEDROCK_TO_JAVA,
            tellrawTarget = "@a[scores={n=!5}]",
            tellrawJson = "{\"text\":\"目标：\",\"extra\":[{\"selector\":\"@a[scores={n=!5}]\"}]}",
            requireUnmergeable = false,
            tagSuffix = "multi"
        )
        requireNotNull(plan)
        assertTrue(plan.tags.any { it.startsWith("tellraw_a") })
        assertTrue(plan.tags.contains("tellraw_recv_multi"))
        assertTrue(plan.tags.contains("tellraw_msg_0_multi"))
        assertTrue(plan.commands.any { it.contains("tellraw @a[tag=tellraw_recv_multi]") })
        assertTrue(plan.commands.any { it.contains("\"selector\":\"@a[tag=tellraw_msg_0_multi]\"") })
        assertTrue(plan.commands.any { it.contains("remove tellraw_recv_multi") })
        assertTrue(plan.commands.any { it.contains("remove tellraw_msg_0_multi") })
    }

    @Test
    fun selectorOnlyTagsKeepStandaloneExecutorChain() {
        val plan = ExecuteConverter.buildMultiCommandTagPlan(
            "execute as @a",
            Direction.BEDROCK_TO_JAVA,
            tellrawTarget = "@a[scores={n=!5}]",
            tellrawJson = "{\"text\":\"ok\"}",
            requireUnmergeable = false,
            tagSuffix = "only"
        )
        requireNotNull(plan)
        assertTrue(plan.tags.contains("tellraw_recv_only"))
        assertTrue(plan.commands.any { it.contains("execute as @a run tellraw @a[tag=tellraw_recv_only]") })
    }

    @Test
    fun nonSelfTargetFoldsIntoExecutorAndRunsTellrawPerExecutor() {
        val plan = ExecuteConverter.buildMultiCommandTagPlan(
            "execute as @a if items entity @s hotbar.* minecraft:diamond if data entity @s {foo:1b}",
            Direction.BEDROCK_TO_JAVA,
            tellrawTarget = "@a",
            tellrawJson = "{\"text\":\"ok\"}",
            requireUnmergeable = false,
            tagSuffix = "fold"
        )
        requireNotNull(plan)
        assertEquals(1, plan.tags.size)
        assertTrue(plan.commands[1].startsWith("execute as @a[nbt={foo:1b},tag=tellraw_a1_fold] run tellraw @a"))
        assertTrue(plan.commands[2].startsWith("tag @a[tag=tellraw_a1_fold] remove"))
        assertTrue(plan.warnings.any { it.contains("重复执行") })
    }

    @Test
    fun onExecutorUsesFinalExecutorTag() {
        val plan = ExecuteConverter.buildMultiCommandTagPlan(
            "execute as @a on owner at @s if items entity @s hotbar.* minecraft:diamond " +
                "if predicate my:is_special",
            Direction.BEDROCK_TO_JAVA,
            tellrawTarget = "@s",
            tellrawJson = "{\"text\":\"ok\"}",
            requireUnmergeable = false,
            tagSuffix = "on"
        )
        requireNotNull(plan)
        assertEquals(1, plan.tags.size)
        assertTrue(plan.commands[0].startsWith("execute as @a on owner at @s if items entity @s"))
        assertTrue(plan.commands[1].contains("on owner") && plan.commands[1].contains("at @s"))
        assertTrue(plan.commands[2].startsWith("tag @e[tag=tellraw_all_on] remove"))
    }

    @Test
    fun onExecutorTagsRecipientSelectorWhenTargetIsNotS() {
        val plan = ExecuteConverter.buildMultiCommandTagPlan(
            "execute as @a on owner if items entity @s hotbar.* minecraft:diamond " +
                "if predicate my:is_special",
            Direction.BEDROCK_TO_JAVA,
            tellrawTarget = "@a",
            tellrawJson = "{\"text\":\"ok\"}",
            requireUnmergeable = false,
            tagSuffix = "on"
        )
        requireNotNull(plan)
        assertEquals(1, plan.tags.size)
        assertTrue(plan.commands[0].contains("run tag @s add tellraw_all_on"))
        assertTrue(plan.commands[1].startsWith("execute as @e[tag=tellraw_all_on] run tellraw @a"))
        assertTrue(plan.commands[2].startsWith("tag @e[tag=tellraw_all_on] remove"))
    }

    @Test
    fun selfSelectorWithParamsIsKeptAsFinalTarget() {
        val plan = ExecuteConverter.buildMultiCommandTagPlan(
            "execute as @a if items entity @s hotbar.* minecraft:diamond if predicate my:is_special",
            Direction.BEDROCK_TO_JAVA,
            tellrawTarget = "@s[tag=x]",
            tellrawJson = "{\"text\":\"ok\"}",
            requireUnmergeable = false,
            tagSuffix = "self"
        )
        requireNotNull(plan)
        assertTrue(plan.commands.any { it.contains("run tellraw @s[tag=x]") })
    }

    @Test
    fun randomOrLimitedBaseUsesWholeChainTag() {
        val plan = ExecuteConverter.buildMultiCommandTagPlan(
            "execute as @r if items entity @s hotbar.* minecraft:diamond if predicate my:is_special",
            Direction.BEDROCK_TO_JAVA,
            tellrawTarget = "@s",
            tellrawJson = "{\"text\":\"ok\"}",
            requireUnmergeable = false,
            tagSuffix = "r"
        )
        requireNotNull(plan)
        assertEquals(1, plan.tags.size)
        assertTrue(plan.commands[0].startsWith("execute as @r "))
        assertTrue(plan.commands[1].startsWith("execute as @e[tag=tellraw_all_r] run tellraw @s"))
    }

    @Test
    fun multipleAsUsesFinalExecutorTag() {
        val plan = ExecuteConverter.buildMultiCommandTagPlan(
            "execute as @a as @e[type=zombie] if items entity @s weapon.mainhand minecraft:diamond " +
                "if predicate my:is_special",
            Direction.BEDROCK_TO_JAVA,
            tellrawTarget = "@s",
            tellrawJson = "{\"text\":\"ok\"}",
            requireUnmergeable = false,
            tagSuffix = "as"
        )
        requireNotNull(plan)
        assertEquals(1, plan.tags.size)
        assertTrue(plan.commands[0].startsWith("execute as @a as @e[type=zombie] if items entity @s"))
        assertTrue(plan.commands[1].startsWith("execute as @e[tag="))
        assertTrue(plan.commands[2].startsWith("tag @e[tag="))
    }

    @Test
    fun interleavedModifierUsesWholeChainTag() {
        val plan = ExecuteConverter.buildMultiCommandTagPlan(
            "execute as @a if items entity @s hotbar.* minecraft:diamond at @s " +
                "if predicate my:is_special",
            Direction.BEDROCK_TO_JAVA,
            tellrawTarget = "@s",
            tellrawJson = "{\"text\":\"ok\"}",
            requireUnmergeable = false,
            tagSuffix = "all"
        )
        requireNotNull(plan)
        assertEquals(1, plan.tags.size)
        assertTrue(plan.tags[0].startsWith("tellraw_all_"))
        assertTrue(
            plan.commands[0].startsWith(
                "execute as @a if items entity @s hotbar.* minecraft:diamond at @s " +
                    "if predicate my:is_special run tag @s add "
            )
        )
        assertTrue(plan.commands[1].contains("at @s"))
        assertTrue(plan.commands[1].contains("if entity @s[tag=tellraw_all_"))
        assertTrue(plan.commands[2].startsWith("tag @e[tag=tellraw_all_"))
    }

    @Test
    fun summonChainUsesWholeChainTag() {
        val plan = ExecuteConverter.buildMultiCommandTagPlan(
            "execute as @a summon pig if items entity @s hotbar.* minecraft:diamond " +
                "if predicate my:is_special",
            Direction.BEDROCK_TO_JAVA,
            tellrawTarget = "@s",
            tellrawJson = "{\"text\":\"ok\"}",
            requireUnmergeable = false,
            tagSuffix = "summon"
        )
        requireNotNull(plan)
        assertEquals(1, plan.tags.size)
        assertTrue(plan.commands[0].contains("summon pig"))
        assertTrue(plan.commands.any { it.contains("as @e[tag=tellraw_all_summon]") })
    }

    @Test
    fun entityLimitIsNotFoldedIntoTellrawTarget() {
        val plan = ExecuteConverter.buildMultiCommandTagPlan(
            "execute as @a if entity @s[limit=1] if items entity @s hotbar.* minecraft:diamond",
            Direction.BEDROCK_TO_JAVA,
            tellrawTarget = "@s",
            tellrawJson = "{\"text\":\"ok\"}",
            requireUnmergeable = false,
            tagSuffix = "test"
        )
        requireNotNull(plan)
        // limit 是“选几个”，不能折进最终执行者选择器，否则只会发给一个实体
        assertFalse(plan.commands.any { it.contains("limit=") && it.contains("run tellraw") })
        assertTrue(plan.commands[2].startsWith("execute as @a[tag="))
        assertTrue(plan.commands[2].contains(" run tellraw @s "))
    }

    @Test
    fun multiCommandTagPlanAllowsSelectorParamsAndEntityBase() {
        val plan = ExecuteConverter.buildMultiCommandTagPlan(
            "execute as @e[type=zombie] if items entity @s weapon.mainhand minecraft:diamond " +
                "unless predicate my:is_special",
            Direction.BEDROCK_TO_JAVA,
            tellrawTarget = "@s",
            tellrawJson = "{\"text\":\"ok\"}",
            requireUnmergeable = false,
            tagSuffix = "test"
        )
        requireNotNull(plan)
        assertTrue(plan.commands[0].startsWith("execute as @e[type=zombie] if items entity @s"))
        assertTrue(plan.commands[1].startsWith("execute as @e[tag="))
        assertTrue(plan.commands.last().startsWith("tag @e[tag="))
    }

    @Test
    fun multiCommandTagPlanPreservesNonPlayerRecipientSelector() {
        val plan = ExecuteConverter.buildMultiCommandTagPlan(
            "execute as @a if items entity @s hotbar.* minecraft:diamond if predicate my:is_special",
            Direction.BEDROCK_TO_JAVA,
            tellrawTarget = "@e",
            tellrawJson = "{\"text\":\"ok\"}",
            requireUnmergeable = false,
            tagSuffix = "test"
        )
        requireNotNull(plan)
        assertEquals(1, plan.tags.size)
        assertTrue(plan.commands[1].startsWith("execute as @e[tag="))
        assertTrue(plan.commands[1].contains(" run tellraw @e "))
        assertTrue(plan.warnings.any { it.contains("可能包含非玩家") })
    }

    @Test
    fun multiCommandTagPlanAllowsBaseParamsAndSanitizesTags() {
        val conditions = "if items entity @s hotbar.* minecraft:diamond unless predicate my:is_special " +
            "run tellraw @s {\"text\":\"x\"}"
        val plan = ExecuteConverter.buildMultiCommandTagPlan(
            "execute as @a[tag=base] $conditions", Direction.BEDROCK_TO_JAVA,
            "@s", "{\"text\":\"x\"}", requireUnmergeable = false, tagSuffix = "a b:c"
        )
        requireNotNull(plan)
        assertTrue(plan.commands[0].startsWith("execute as @a[tag=base] "))
        assertTrue(plan.tags.all { it.matches(Regex("[A-Za-z0-9_]+")) })
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

    @Test
    fun recipientSelectorsThatNeedOriginalContextFallBack() {
        val prefix = "execute as @a if items entity @s hotbar.* minecraft:diamond " +
            "if predicate my:is_special"
        for (target in listOf("@p[scores={n=!5}]", "@n[scores={n=!5}]", "@r[scores={n=!5}]")) {
            val plan = ExecuteConverter.buildMultiCommandTagPlan(
                prefix, Direction.BEDROCK_TO_JAVA, target,
                "{\"text\":\"ok\"}", tagSuffix = "ctx"
            )
            requireNotNull(plan)
            assertTrue(plan.commands.any { it.contains("tag @p") || it.contains("tag @n") || it.contains("tag @r") })
        }
        for (target in listOf(
            "@a[distance=..5,scores={n=!5}]",
            "@a[limit=1,scores={n=!5}]",
            "@a[sort=nearest,scores={n=!5}]"
        )) {
            val plan = ExecuteConverter.buildMultiCommandTagPlan(
                prefix, Direction.BEDROCK_TO_JAVA, target,
                "{\"text\":\"ok\"}", tagSuffix = "ctx"
            )
            assertNull("$target 不应拆出会二次抽样的 tag", plan)
        }
    }


    @Test
    fun selectorTagPlanFiltersUnsupportedBaseParams() {
        val plan = ExecuteConverter.buildMultiCommandTagPlan(
            "execute as @a if items entity @s hotbar.* minecraft:diamond if predicate my:is_special",
            Direction.BEDROCK_TO_JAVA,
            tellrawTarget = "@a[family=zombie,scores={n=!5}]",
            tellrawJson = "{\"text\":\"ok\"}",
            requireUnmergeable = false,
            tagSuffix = "filter",
            filterJavaSelector = { "@a" }
        )
        requireNotNull(plan)
        assertTrue(plan.commands.any { it.contains("run tag @s add tellraw_recv_filter") })
        assertFalse(plan.commands.any { it.contains("family=zombie") })
        assertTrue(plan.commands.any { it.contains("tellraw @a[tag=tellraw_recv_filter]") })
    }


    @Test
    fun messageSelectorIsFilteredEvenWhenNoTagIsNeeded() {
        val plan = ExecuteConverter.buildMultiCommandTagPlan(
            "execute as @a if items entity @s hotbar.* minecraft:diamond if predicate my:is_special",
            Direction.BEDROCK_TO_JAVA,
            tellrawTarget = "@a",
            tellrawJson = "{\"text\":\"x\",\"extra\":[{\"selector\":\"@e[family=zombie]\"}]}",
            requireUnmergeable = false,
            tagSuffix = "msgfilter",
            filterJavaSelector = { "@e" }
        )
        requireNotNull(plan)
        assertFalse(plan.commands.any { it.contains("family=zombie") })
        assertTrue(plan.commands.any { it.contains("\"selector\":\"@e\"") })
    }


    @Test
    fun unsafeMessageSelectorAbortsTagPlanInsteadOfLeakingRawSelector() {
        val plan = ExecuteConverter.buildMultiCommandTagPlan(
            "execute as @a if items entity @s hotbar.* minecraft:diamond if predicate my:is_special",
            Direction.BEDROCK_TO_JAVA,
            tellrawTarget = "@a",
            tellrawJson = "{\"selector\":\"@p[scores={n=!5}]\"}",
            requireUnmergeable = false,
            tagSuffix = "unsafe",
            filterJavaSelector = { it.replace("scores={n=!5}", "") }
        )
        assertNull(plan)
    }


    @Test
    fun anchorsCommandBlockChainToFirstBlock() {
        val anchored = ExecuteConverter.anchorCommandChain(
            listOf(
                "execute as @a run tag @s add t1",
                "tellraw @a[tag=t1] {\"text\":\"ok\"}",
                "tag @a[tag=t1] remove t1"
            )
        )
        assertEquals("execute positioned ~ ~ ~ as @a run tag @s add t1", anchored[0])
        assertEquals("execute positioned ~ ~-1 ~ run tellraw @a[tag=t1] {\"text\":\"ok\"}", anchored[1])
        assertEquals("execute positioned ~ ~-2 ~ run tag @a[tag=t1] remove t1", anchored[2])
    }


    @Test
    fun contextDependentMessageSelectorUsesReplayedContext() {
        val plan = ExecuteConverter.buildMultiCommandTagPlan(
            "execute as @a at @s if items entity @s hotbar.* minecraft:diamond if predicate my:is_special",
            Direction.BEDROCK_TO_JAVA,
            tellrawTarget = "@a",
            tellrawJson = "{\"selector\":\"@p\"}",
            requireUnmergeable = false,
            tagSuffix = "ctxmsg"
        )
        requireNotNull(plan)
        val finalLine = plan.commands.first { it.contains("run tellraw") }
        assertTrue(finalLine, finalLine.contains("at @s") && finalLine.contains("if entity @s[tag="))
    }


    @Test
    fun contextDependentRecipientUsesReplayedFinalContext() {
        val plan = ExecuteConverter.buildMultiCommandTagPlan(
            "execute as @a at @s if items entity @s hotbar.* minecraft:diamond if predicate my:is_special",
            Direction.BEDROCK_TO_JAVA,
            tellrawTarget = "@r",
            tellrawJson = "{\"text\":\"ok\"}",
            requireUnmergeable = false,
            tagSuffix = "ctxrecv"
        )
        requireNotNull(plan)
        val finalLine = plan.commands.first { it.contains("run tellraw") }
        assertTrue(finalLine, finalLine.contains("at @s") && finalLine.contains("if entity @s[tag="))
    }


    @Test
    fun conditionalRawtextDoesNotEnterTagPlan() {
        val json = "{\"rawtext\":[{\"translate\":\"%%2\",\"with\":{" +
            "\"rawtext\":[{\"selector\":\"@s[scores={menu=1}]\"},{\"text\":\"x\"}]}}]}"
        val plan = ExecuteConverter.buildMultiCommandTagPlan(
            "execute as @a if items entity @s hotbar.* minecraft:diamond if predicate my:is_special",
            Direction.BEDROCK_TO_JAVA,
            "@a", json, tagSuffix = "condraw"
        )
        assertNull(plan)
    }


    @Test
    fun summonUsesWholeChainTagOnce() {
        val plan = ExecuteConverter.buildMultiCommandTagPlan(
            "execute as @a summon minecraft:pig if items entity @s hotbar.* minecraft:diamond " +
                "if predicate my:is_special",
            Direction.BEDROCK_TO_JAVA,
            tellrawTarget = "@s",
            tellrawJson = "{\"text\":\"ok\"}",
            requireUnmergeable = false,
            tagSuffix = "summon"
        )
        requireNotNull(plan)
        assertEquals(1, plan.tags.size)
        assertTrue(plan.commands[0].contains("summon minecraft:pig"))
        assertTrue(plan.commands.any { it.contains("as @e[tag=tellraw_all_summon]") })
    }


    @Test
    fun summonWithNoPositionContextAllowsRandomRecipient() {
        val plan = ExecuteConverter.buildMultiCommandTagPlan(
            "execute as @a summon minecraft:pig if items entity @s hotbar.* minecraft:diamond " +
                "if predicate my:is_special",
            Direction.BEDROCK_TO_JAVA,
            tellrawTarget = "@r",
            tellrawJson = "{\"text\":\"ok\"}",
            requireUnmergeable = false,
            tagSuffix = "sumr"
        )
        requireNotNull(plan)
        assertTrue(plan.commands.any { it.contains("run tellraw @r") })
    }

}
