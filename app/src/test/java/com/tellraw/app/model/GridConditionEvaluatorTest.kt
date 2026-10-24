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

    @Test fun positionedAndAtResolveOrigin() {
        val a = GridEntity("a", "minecraft:player", x = 10.0)
        val z = GridEntity("z", "minecraft:zombie", x = 3.0)
        val list = listOf(a, z)
        val base = Triple(0.0, -60.0, 0.0)

        val pos = GridConditionEvaluator.resolveOrigin("positioned 5 -60 0", list, null, base)
        assertEquals(5.0, pos.origin.first, 0.0)

        val at = GridConditionEvaluator.resolveOrigin("at @e[type=zombie]", list, null, base)
        assertEquals(3.0, at.origin.first, 0.0)

        val rel = GridConditionEvaluator.resolveOrigin(
            "positioned ~2 ~ ~", emptyList(), null, Triple(1.0, -60.0, 0.0)
        )
        assertEquals(3.0, rel.origin.first, 0.0)
    }

    @Test fun selectorXyzOverridesPositioned() {
        // positioned 原点在 10，但选择器自己写了 x=0，distance 应按选择器的 x 算
        val e = GridEntity("e", "minecraft:zombie", x = 0.0)
        val r = SelectorEvaluator.evaluate(
            "@e[x=0,y=-60,z=0,distance=..1]", listOf(e),
            origin = Triple(10.0, -60.0, 0.0)
        )
        assertEquals(listOf("e"), r.matched.map { it.uid })
    }

    @Test fun unsupportedConditionsAreNotedNotSilent() {
        val r = eval("if block ~ ~ ~ stone")
        assertTrue(r.ok)
        assertTrue(r.notes.any { it.contains("未建模") })
    }
}
