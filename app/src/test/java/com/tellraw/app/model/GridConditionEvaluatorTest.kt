package com.tellraw.app.model

import org.junit.Assert.*
import org.junit.Test

class GridConditionEvaluatorTest {

    private val steve = GridEntity(
        uid = "p1", id = "minecraft:player", name = "Steve",
        tags = setOf("vip"), extraNbt = "Health:20.0f"
    )

    private fun eval(cond: String, e: GridEntity = steve) =
        GridConditionEvaluator.evaluate(cond, e, listOf(e))

    @Test fun dataConditionUsesDerivedAndExtraNbt() {
        assertTrue(eval("if data entity @s {Health:20.0f}").ok)
        assertFalse(eval("if data entity @s {Health:10.0f}").ok)
        assertTrue(eval("unless data entity @s {Health:10.0f}").ok)
        // tag 字段与 NBT Tags 是同一份数据
        assertTrue(eval("""if data entity @s {Tags:["vip"]}""").ok)
    }

    @Test fun entityAndPredicateConditions() {
        assertTrue(eval("if entity @s[tag=vip]").ok)
        assertFalse(eval("if entity @s[tag=admin]").ok)
        assertTrue(eval("unless entity @s[tag=admin]").ok)

        assertFalse(eval("if predicate minecraft:is_sneaking").ok)
        val sneaking = steve.copy(predicates = setOf("minecraft:is_sneaking"))
        assertTrue(eval("if predicate minecraft:is_sneaking", sneaking).ok)
    }

    @Test fun multipleConditionsAreAnded() {
        assertTrue(eval("if data entity @s {Health:20.0f} if entity @s[tag=vip]").ok)
        assertFalse(eval("if data entity @s {Health:20.0f} if entity @s[tag=admin]").ok)
    }

    @Test fun unsupportedConditionsAreNotedNotSilent() {
        val r = eval("if block ~ ~ ~ stone")
        assertTrue(r.ok)
        assertTrue(r.notes.any { it.contains("未建模") })
    }
}
