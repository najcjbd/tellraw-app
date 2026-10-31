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
}
