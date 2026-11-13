package com.tellraw.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 复核审计报告中的网格条件求值问题。 */
class GridConditionAuditTest {

    private val steve = GridEntity("p1", "minecraft:player", name = "Steve")

    private fun eval(
        cond: String,
        entity: GridEntity = steve,
        all: List<GridEntity> = listOf(entity),
        target: VersionSpec? = null,
        world: GridConditionEvaluator.GridWorld = GridConditionEvaluator.GridWorld(),
        origin: Triple<Double, Double, Double> = Triple(0.0, -60.0, 0.0)
    ) = GridConditionEvaluator.evaluate(cond, entity, all, target, world, origin)

    @Test
    fun unlessUnknownConditionDoesNotFilter() {
        val r = eval("unless block 0 -60 0 stone")   // 没有方块数据
        assertTrue(r.ok)
        assertTrue(r.notes.isNotEmpty())

        val unknown = eval("unless totally_unknown arg")
        assertTrue(unknown.ok)

        val bedrockItems = eval(
            "unless items entity @s weapon.mainhand minecraft:stone",
            target = VersionSpec.parse("1.21.100")
        )
        assertTrue(bedrockItems.ok)
    }

    @Test
    fun bedrockBlockConditionUsesWorldData() {
        val world = GridConditionEvaluator.GridWorld(
            blocks = mapOf("0,-60,0" to GridConditionEvaluator.GridBlock("minecraft:stone"))
        )
        val bedrock = VersionSpec.parse("1.21.100")
        assertTrue(eval("if block 0 -60 0 stone", world = world, target = bedrock).ok)
        assertFalse(eval("if block 0 -60 0 dirt", world = world, target = bedrock).ok)
    }

    @Test
    fun unlessEntityWithUnsupportedSelectorParamIsNeutral() {
        // Bedrock 目标下 nbt= 不参与筛选；unless 不能把它反转为“全部排除”
        val r = eval(
            "unless entity @e[nbt={Health:20f}]",
            target = VersionSpec.parse("1.21.100")
        )
        assertTrue(r.ok)
        assertTrue(r.notes.isNotEmpty())
    }

    @Test
    fun ifEntityDistanceUsesResolvedOrigin() {
        val farZombie = GridEntity("z", "minecraft:zombie", x = 100.0)
        val r = eval(
            "if entity @e[type=zombie,distance=..5]",
            entity = farZombie,
            all = listOf(farZombie),
            origin = Triple(0.0, -60.0, 0.0)
        )
        assertFalse(r.ok)
    }

    @Test
    fun positionedAsUsesSelectedEntityPosition() {
        val a = GridEntity("a", "minecraft:player", x = 10.0)
        val z = GridEntity("z", "minecraft:zombie", x = 100.0, y = -60.0, z = 3.0)
        val base = Triple(0.0, -60.0, 0.0)
        val r = GridConditionEvaluator.resolveOrigin(
            "positioned as @e[type=zombie]", listOf(a, z), a, base
        )
        assertEquals(100.0, r.origin.first, 0.0)
        assertEquals(-60.0, r.origin.second, 0.0)
        assertEquals(3.0, r.origin.third, 0.0)
    }

    @Test
    fun positionedOverKeepsOriginAndNotes() {
        val base = Triple(5.0, -60.0, 7.0)
        val r = GridConditionEvaluator.resolveOrigin(
            "positioned over world_surface", emptyList(), null, base
        )
        assertEquals(base, r.origin)
        assertTrue(r.notes.isNotEmpty())
    }

    @Test
    fun missingDimensionDoesNotInheritCurrentWorldState() {
        val world = GridConditionEvaluator.GridWorld(
            dimension = "minecraft:overworld",
            biome = "minecraft:plains",
            loaded = true,
            blocks = mapOf("0,-60,0" to GridConditionEvaluator.GridBlock("minecraft:stone")),
            functions = setOf("test:fn"),
            stopwatches = mapOf("sw" to 1.0),
            dimensions = mapOf(
                "minecraft:the_nether" to GridConditionEvaluator.DimensionWorld(
                    biome = "minecraft:nether_wastes", loaded = false,
                    blocks = mapOf("1,2,3" to GridConditionEvaluator.GridBlock("minecraft:netherrack"))
                )
            )
        )
        val nether = world.forDimension("nether")
        assertEquals("minecraft:the_nether", nether.dimension)
        assertEquals("minecraft:nether_wastes", nether.biome)
        assertFalse(nether.loaded)
        assertTrue(nether.blocks.containsKey("1,2,3"))

        val end = world.forDimension("the_end")
        assertEquals("minecraft:the_end", end.dimension)
        assertEquals("", end.biome)
        assertTrue(end.blocks.isEmpty())
        assertTrue(end.functions.isEmpty())
        assertTrue(end.stopwatches.isEmpty())
    }


    @Test
    fun itemsAndSlotsUseExternalToInternalInventoryMapping() {
        val p = EditableGridEntity(
            uid = "p", id = "minecraft:player",
            inventory = "27=minecraft:diamond,9=minecraft:dirt"
        ).toGridEntity()
        val items = eval(
            "if items entity @s inventory.18 minecraft:diamond",
            entity = p, all = listOf(p)
        )
        assertTrue(items.notes.joinToString(), items.ok)
        val slots = eval(
            "if slots entity @s inventory.18",
            entity = p, all = listOf(p)
        )
        assertTrue(slots.notes.joinToString(), slots.ok)
        val inv0 = eval(
            "if items entity @s inventory.0 minecraft:dirt",
            entity = p, all = listOf(p)
        )
        assertTrue(inv0.notes.joinToString(), inv0.ok)
    }

    @Test
    fun itemsIdNegationIsRejectedAsUnsupportedJavaSyntax() {
        val p = EditableGridEntity(uid = "p", id = "minecraft:player").toGridEntity()
        val r = eval(
            "if items entity @s hotbar.0 !minecraft:diamond",
            entity = p, all = listOf(p)
        )
        assertTrue(r.notes.any { it.contains("物品 ID 前取反") })
    }


    @Test
    fun bareInventorySlotAndInvalidIdNegationAreNeutral() {
        val p = EditableGridEntity(
            uid = "p", id = "minecraft:player", inventory = "9=minecraft:diamond"
        ).toGridEntity()
        val bare = eval(
            "if items entity @s inventory minecraft:diamond",
            entity = p, all = listOf(p)
        )
        assertTrue(bare.notes.joinToString(), bare.ok)

        val unlessNeg = eval(
            "unless items entity @s hotbar.0 !minecraft:diamond",
            entity = p, all = listOf(p)
        )
        assertTrue("unless 遇到不支持的语法不能反向排除所有实体", unlessNeg.ok)
        assertTrue(unlessNeg.notes.any { it.contains("物品 ID 前取反") })
    }


    @Test
    fun slotRangesAndNegativeSlotsFollowJavaSemantics() {
        val p = EditableGridEntity(
            uid = "p", id = "minecraft:player", inventory = "9=minecraft:dirt"
        ).toGridEntity()
        val range = eval("if items entity @s inventory.0..8 minecraft:dirt", entity = p, all = listOf(p))
        assertTrue(range.notes.joinToString(), range.ok)
        val negative = eval("if slots entity @s inventory.-1", entity = p, all = listOf(p))
        assertFalse("负数槽位不应该平移成有效槽位", negative.ok)
    }

}
