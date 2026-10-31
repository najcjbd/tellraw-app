package com.tellraw.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 复核审计报告中的选择器求值问题。 */
class SelectorEvaluatorAuditTest {

    private val stone5 = GridItem("minecraft:stone", 5)
    private val diamond = GridItem("minecraft:diamond", 1)

    private fun eval(
        selector: String,
        entities: List<GridEntity>,
        edition: String = "java",
        origin: Triple<Double, Double, Double> = Triple(0.0, -60.0, 0.0)
    ) = SelectorEvaluator.evaluate(selector, entities, origin = origin, edition = edition)

    @Test
    fun hasitemArrayRequiresAllObjects() {
        val onlyStone = GridEntity(
            "a", "minecraft:player",
            items = mapOf("hotbar.0" to stone5)
        )
        val both = onlyStone.copy(uid = "b", items = mapOf("hotbar.0" to stone5, "hotbar.1" to diamond))

        val r = eval("@a[hasitem=[{item=stone},{item=diamond}]]", listOf(onlyStone, both), edition = "bedrock")
        assertEquals(listOf("b"), r.matched.map { it.uid })
    }

    @Test
    fun scoreRangeMatches() {
        val z = GridEntity("z", "minecraft:zombie", scores = mapOf("hp" to 5))
        assertTrue(eval("@e[scores={hp=1..5}]", listOf(z)).matched.isNotEmpty())
        assertTrue(eval("@e[scores={hp=6..}]", listOf(z)).matched.isEmpty())
        assertTrue(eval("@e[scores={hp=..4}]", listOf(z)).matched.isEmpty())
        assertTrue(eval("@e[scores={hp=!1..5}]", listOf(z)).matched.isEmpty())
        assertTrue(eval("@e[scores={hp=!6..}]", listOf(z)).matched.isNotEmpty())
    }

    @Test
    fun gamemodeAndLevelExcludeNonPlayers() {
        val zombie = GridEntity("z", "minecraft:zombie")
        val p = GridEntity("p", "minecraft:player", gamemode = "creative", level = 30)
        assertEquals(listOf("p"), eval("@e[gamemode=creative]", listOf(zombie, p)).matched.map { it.uid })
        assertTrue(eval("@e[gamemode=creative]", listOf(zombie)).matched.isEmpty())
        assertEquals(listOf("p"), eval("@e[level=20..40]", listOf(zombie, p)).matched.map { it.uid })
        assertTrue(eval("@e[level=20..40]", listOf(zombie)).matched.isEmpty())
        // 反选也不能把非玩家选进来
        assertTrue(eval("@e[gamemode=!creative]", listOf(zombie)).matched.isEmpty())
        assertTrue(eval("@e[level=!20..40]", listOf(zombie)).matched.isEmpty())
        // 玩家只是没填该字段：按“未设置、未参与筛选”处理，不应误杀
        val unknownPlayer = GridEntity("u", "minecraft:player")
        assertTrue(eval("@e[gamemode=creative]", listOf(unknownPlayer)).matched.isNotEmpty())
        assertTrue(eval("@e[level=20..40]", listOf(unknownPlayer)).matched.isNotEmpty())
    }

    @Test
    fun bedrockCOrdersByDistance() {
        val near = GridEntity("n", "minecraft:zombie", x = 1.0)
        val mid = GridEntity("m", "minecraft:zombie", x = 5.0)
        val far = GridEntity("f", "minecraft:zombie", x = 10.0)
        val list = listOf(far, mid, near)   // 故意乱序

        assertEquals(
            listOf("n", "m"),
            eval("@e[c=2]", list, edition = "bedrock").matched.map { it.uid }
        )
        assertEquals(
            listOf("f", "m"),
            eval("@e[c=-2]", list, edition = "bedrock").matched.map { it.uid }
        )
    }

    @Test
    fun bedrockRWithTypeCanSelectNonPlayers() {
        val p = GridEntity("p", "minecraft:player")
        val z = GridEntity("z", "minecraft:zombie")
        val r = eval("@r[type=zombie]", listOf(p, z), edition = "bedrock")
        assertEquals(listOf("z"), r.matched.map { it.uid })
    }

    @Test
    fun hasitemSlotRangeAndNegatedQuantity() {
        val e = GridEntity(
            "a", "minecraft:player",
            items = mapOf(
                "hotbar.0" to GridItem("minecraft:stone", 5),
                "hotbar.5" to GridItem("minecraft:diamond", 1)
            )
        )
        // slot=0..2 只覆盖 hotbar.0..2，diamond 在 5
        assertTrue(eval("@a[hasitem={item=diamond,location=slot.hotbar,slot=0..2}]", listOf(e), edition = "bedrock").matched.isEmpty())
        assertTrue(eval("@a[hasitem={item=diamond,location=slot.hotbar,slot=5}]", listOf(e), edition = "bedrock").matched.isNotEmpty())
        // quantity=!5：stone 正好 5，不应匹配；!6 应匹配
        assertTrue(eval("@a[hasitem={item=stone,location=slot.hotbar,slot=0,quantity=!5}]", listOf(e), edition = "bedrock").matched.isEmpty())
        assertTrue(eval("@a[hasitem={item=stone,location=slot.hotbar,slot=0,quantity=!6}]", listOf(e), edition = "bedrock").matched.isNotEmpty())
    }

    @Test
    fun hasitemQuantityZeroMeansNone() {
        val e = GridEntity(
            "a", "minecraft:player",
            items = mapOf("hotbar.0" to stone5)
        )
        assertTrue(
            eval("@a[hasitem={item=diamond,quantity=0}]", listOf(e), edition = "bedrock")
                .matched.isNotEmpty()
        )
        assertTrue(
            eval("@a[hasitem={item=stone,quantity=0}]", listOf(e), edition = "bedrock")
                .matched.isEmpty()
        )
    }

    @Test
    fun negativeLimitDoesNotCrash() {
        val z = GridEntity("z", "minecraft:zombie")
        val r = eval("@e[limit=-1]", listOf(z))
        assertTrue(r.matched.isEmpty())
        assertTrue(r.notes.isNotEmpty())
    }
}
