package com.tellraw.app.model

import org.junit.Assert.*
import org.junit.Test

class SelectorEvaluatorTest {
    private val zombie = GridEntity("z1", "minecraft:zombie", "Z", tags = setOf("undead_tag"),
        scores = mapOf("hp" to 5), families = EntityFamilies.familiesOf("minecraft:zombie"))
    private val skeleton = GridEntity("s1", "minecraft:skeleton", "S",
        scores = mapOf("hp" to 5), families = EntityFamilies.familiesOf("minecraft:skeleton"))
    private val cow = GridEntity("c1", "minecraft:cow", "C", x = 10.0, families = EntityFamilies.familiesOf("minecraft:cow"))
    private val steve = GridEntity("p1", "minecraft:player", "Steve", tags = setOf("vip"))
    private val all = listOf(zombie, skeleton, cow, steve)

    private fun ids(s: String, edition: String = "java", version: Long? = null) =
        SelectorEvaluator.evaluate(s, all, executor = steve, edition = edition, version = version)

    @Test fun baseSelectors() {
        assertEquals(listOf("p1"), ids("@a").matched.map { it.uid })
        assertEquals(listOf("p1"), ids("@p").matched.map { it.uid })
        assertEquals(4, ids("@e").matched.size)
    }

    @Test fun typeAndTag() {
        assertEquals(listOf("z1"), ids("@e[type=zombie]").matched.map { it.uid })
        assertEquals(listOf("p1", "z1"), ids("@e[tag=!]").matched.map { it.uid }.sorted())   // tag=! 有标签
        assertEquals(listOf("c1", "s1"), ids("@e[tag=]").matched.map { it.uid }.sorted())    // tag= 无标签
        assertEquals(listOf("p1"), ids("@e[tag=vip]").matched.map { it.uid })
    }

    @Test fun familyGatedByEditionAndVersion() {
        // Bedrock >= 1.16.100：生效
        val b = ids("@e[family=undead]", "bedrock", 1_16_100L)
        assertEquals(listOf("s1", "z1"), b.matched.map { it.uid }.sorted())
        assertTrue(b.notes.isEmpty())
        // Java：不支持 -> 不筛选 + 明确提醒
        val j = ids("@e[family=undead]", "java", null)
        assertEquals(4, j.matched.size)
        assertTrue(j.notes.any { it.contains("1.16.100") })
    }

    @Test fun scoresEqualityAndNegation() {
        assertEquals(listOf("s1", "z1"), ids("@e[scores={hp=5}]").matched.map { it.uid }.sorted())
        // =!：存在且不等于 -> 只有 hp=5 的 z/s 被排除；cow/steve 根本没有 hp -> 也不算命中
        assertTrue(ids("@e[scores={hp=!5}]").matched.isEmpty())
        assertTrue(ids("@e[scores={hp=!4}]").matched.size == 2)
    }

    @Test fun positionAndLimit() {
        assertEquals(listOf("c1"), ids("@e[x=9,y=-60,z=-1,dx=2,dy=1,dz=2]").matched.map { it.uid })
        assertEquals(listOf("p1"), ids("@p").matched.map { it.uid })  // 最近玩家
        assertTrue(ids("@e[limit=1]").matched.size == 1)
    }

    @Test fun unmodelledParamsAreNotSilent() {
        val r = ids("@e[nbt={Inventory:[{id:\"minecraft:diamond\"}]}]")
        assertEquals(4, r.matched.size)
        assertTrue(r.notes.any { it.contains("nbt") })
    }
}
