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

    @Test fun worldConditions() {
        val world = GridConditionEvaluator.GridWorld(
            dimension = "minecraft:the_nether",
            biome = "minecraft:nether_wastes",
            loaded = true,
            blocks = mapOf(
                "0,-60,0" to GridConditionEvaluator.GridBlock("minecraft:stone"),
                "1,-60,0" to GridConditionEvaluator.GridBlock("minecraft:dirt"),
                "10,-60,0" to GridConditionEvaluator.GridBlock("minecraft:stone"),
                "11,-60,0" to GridConditionEvaluator.GridBlock("minecraft:dirt")
            ),
            functions = setOf("test:fn"),
            stopwatches = mapOf("sw" to 3.0)
        )
        val e = GridEntity("e", "minecraft:zombie", x = 0.0, y = -60.0, z = 0.0,
            scores = mapOf("obj" to 5))
        fun ok(c: String) = GridConditionEvaluator.evaluate(
            c, e, listOf(e), null, world, Triple(0.0, -60.0, 0.0)
        ).ok

        assertTrue(ok("if block 0 -60 0 minecraft:stone"))
        assertFalse(ok("if block 0 -60 0 minecraft:dirt"))
        assertTrue(ok("if blocks 0 -60 0 1 -60 0 10 -60 0 all"))
        assertTrue(ok("if biome 0 -60 0 minecraft:nether_wastes"))
        assertTrue(ok("if dimension minecraft:the_nether"))
        assertTrue(ok("if loaded 0 -60 0"))
        assertTrue(ok("if function test:fn"))
        assertTrue(ok("if stopwatch sw 1..5"))
        assertTrue(ok("if score @s obj matches 4..6"))
        assertFalse(ok("if score @s obj matches 6..9"))
    }

    @Test fun unsupportedConditionsAreNotedNotSilent() {
        // block 已建模：没有世界方块数据时提示去填写，而不是静默
        val block = eval("if block ~ ~ ~ stone")
        assertTrue(block.ok)
        assertTrue(block.notes.any { it.contains("方块") })
    }

    @Test fun itemsAndSlotsEntity() {
        val p = steve.copy(items = mapOf(
            "weapon.mainhand" to GridItem("minecraft:diamond_sword", 1),
            "armor.head" to GridItem("minecraft:diamond_helmet", 1),
            "hotbar.0" to GridItem("minecraft:stone", 64)
        ))
        fun ok(c: String) = GridConditionEvaluator.evaluate(c, p, listOf(p)).ok
        assertTrue(ok("if items entity @s weapon.mainhand minecraft:diamond_sword"))
        assertFalse(ok("if items entity @s weapon.mainhand minecraft:stone"))
        assertTrue(ok("if items entity @s armor.* minecraft:diamond_helmet"))
        assertTrue(ok("if items entity @s hotbar.0 minecraft:stone[count=64..]"))
        assertFalse(ok("if items entity @s hotbar.0 minecraft:stone[count=65..]"))
        assertTrue(ok("if items entity @s * minecraft:diamond_sword"))
        assertTrue(ok("unless items entity @s weapon.mainhand minecraft:stone"))

        assertTrue(ok("if slots entity @s armor.*"))
        assertTrue(ok("if slots entity @s hotbar.0"))
        assertFalse(ok("if slots entity @s hotbar.9"))
        assertFalse(ok("if slots entity @s container.*"))
        assertTrue(ok("if slots entity @s inventory.*"))
    }

    @Test fun itemsPredicateComponents() {
        val comp = SnbtLite.parseCompoundBody("""{id:"test:bar"}""")!!
        val item = GridItem(
            "minecraft:diamond_sword", 1, null,
            mapOf("minecraft:custom_data" to SnbtLite.Value.Compound(comp))
        )
        val e = GridEntity("e", "minecraft:player", items = mapOf("weapon.mainhand" to item))
        fun ok(c: String) = GridConditionEvaluator.evaluate(c, e, listOf(e)).ok

        assertTrue(ok("""if items entity @s weapon.mainhand *[custom_data~{id:"test:bar"}]"""))
        assertFalse(ok("""if items entity @s weapon.mainhand *[custom_data~{id:"other"}]"""))
        assertTrue(ok("""if items entity @s weapon.mainhand diamond_sword[custom_data={id:"test:bar"}]"""))
        assertTrue(ok("if items entity @s weapon.mainhand *[!damage|damage=0]"))
        assertTrue(ok("if items entity @s weapon.mainhand *[count~{min:1,max:1}]"))
        assertFalse(ok("if items entity @s weapon.mainhand *[count~{min:2}]"))
    }

    @Test fun itemPredicateAndGroupsMustMatchSameItem() {
        val a = GridItem("minecraft:diamond", 1, null,
            mapOf("minecraft:custom_data" to SnbtLite.Value.Compound(
                SnbtLite.parseCompoundBody("""{id:"foo"}""")!!
            )))
        val b = GridItem("minecraft:stone", 2)
        val e = GridEntity("e", "minecraft:player", items = mapOf(
            "hotbar.0" to a, "hotbar.1" to b
        ))
        // 组1匹配 a，组2匹配 b，但没有同一件物品同时满足两组 -> 不应该命中
        assertFalse(
            GridConditionEvaluator.evaluate(
                """if items entity @s hotbar.* *[custom_data~{id:"foo"},count=2]""",
                e, listOf(e)
            ).ok
        )
    }

    @Test fun blockCoordinatesFloorAndMaskedMode() {
        val world = GridConditionEvaluator.GridWorld(
            blocks = mapOf(
                "-1,-60,0" to GridConditionEvaluator.GridBlock("minecraft:stone"),
                "0,-60,0" to GridConditionEvaluator.GridBlock("minecraft:air"),
                "1,-60,0" to GridConditionEvaluator.GridBlock("minecraft:stone"),
                "10,-60,0" to GridConditionEvaluator.GridBlock("minecraft:stone"),
                "11,-60,0" to GridConditionEvaluator.GridBlock("minecraft:air"),
                "12,-60,0" to GridConditionEvaluator.GridBlock("minecraft:stone")
            )
        )
        val e = GridEntity("e", "minecraft:zombie")
        fun ok(c: String) = GridConditionEvaluator.evaluate(
            c, e, listOf(e), null, world, Triple(0.0, -60.0, 0.0)
        ).ok

        // 方块坐标向下取整：-0.5 -> -1
        assertTrue(ok("if block -0.5 -60 0 minecraft:stone"))
        // masked：源区域的空气被跳过；destination 只比较非空气
        assertTrue(ok("if blocks -1 -60 0 1 -60 0 10 -60 0 masked"))
        // all：空气也要一致（10/12 是 stone,air,stone；源是 stone,air,stone -> 实际一致）
        assertTrue(ok("if blocks -1 -60 0 1 -60 0 10 -60 0 all"))
    }

    @Test fun itemsAndSlotsBlockContainer() {
        val world = GridConditionEvaluator.GridWorld(
            blocks = mapOf(
                "0,-60,0" to GridConditionEvaluator.GridBlock(
                    "minecraft:chest",
                    mapOf(0 to GridItem("minecraft:diamond", 3))
                )
            )
        )
        val e = GridEntity("e", "minecraft:zombie")
        fun ok(c: String) = GridConditionEvaluator.evaluate(
            c, e, listOf(e), null, world, Triple(0.0, -60.0, 0.0)
        ).ok
        assertTrue(ok("if items block 0 -60 0 container.0 minecraft:diamond"))
        assertTrue(ok("if items block 0 -60 0 container.* minecraft:diamond[count=3]"))
        assertFalse(ok("if items block 0 -60 0 container.1 minecraft:diamond"))
        assertTrue(ok("if slots block 0 -60 0 container.*"))
        assertTrue(ok("if slots block 0 -60 0 container.0"))
        assertFalse(ok("if slots block 0 -60 0 container.9"))
    }
}
