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

    @Test fun gamemodeLevelAndRotation() {
        val creative = steve.copy(gamemode = "creative", level = 30)

        fun sel(s: String, list: List<GridEntity> = listOf(creative, zombie)) =
            SelectorEvaluator.evaluate(s, list)

        assertEquals(listOf("p1"), sel("@a[gamemode=creative]").matched.map { it.uid })
        assertTrue(sel("@a[gamemode=survival]").matched.isEmpty())
        assertEquals(listOf("p1"), sel("@a[m=!survival]").matched.map { it.uid })
        assertEquals(listOf("p1"), sel("@a[m=1]").matched.map { it.uid })
        assertEquals(listOf("p1"), sel("@a[level=20..40]").matched.map { it.uid })
        assertTrue(sel("@a[level=..20]").matched.isEmpty())
        assertEquals(listOf("p1"), sel("@a[lm=10,l=50]").matched.map { it.uid })

        val z = zombie.copy(rotationX = 45.0, rotationY = -90.0)
        val list = listOf(z)
        assertEquals(listOf("z1"), sel("@e[rx=0..90]", list).matched.map { it.uid })
        assertTrue(sel("@e[rx=50..90]", list).matched.isEmpty())
        assertEquals(listOf("z1"), sel("@e[y_rotation=-180..-45]", list).matched.map { it.uid })
        // 未设置视角的实体不按它筛选，但要提醒
        val noRot = sel("@e[rx=0..90]", listOf(zombie))
        assertEquals(listOf("z1"), noRot.matched.map { it.uid })
        assertTrue(noRot.notes.any { it.contains("rx=") })
    }

    @Test fun nbtItemSubset() {
        val p = steve.copy(
            items = mapOf(
                "weapon.mainhand" to GridItem("minecraft:diamond_sword"),
                "armor.head" to GridItem("minecraft:diamond_helmet"),
                "inventory.0" to GridItem("minecraft:stone", 64)
            )
        )
        val list = listOf(p)
        fun sel(s: String) = SelectorEvaluator.evaluate(s, list)

        assertEquals(
            listOf("p1"),
            sel("@a[nbt={SelectedItem:{id:\"minecraft:diamond_sword\"}}]").matched.map { it.uid }
        )
        assertTrue(
            sel("@a[nbt={SelectedItem:{id:\"minecraft:wooden_sword\"}}]").matched.isEmpty()
        )
        assertEquals(
            listOf("p1"),
            sel("@a[nbt={equipment:{mainhand:{id:\"diamond_sword\"},head:{id:\"diamond_helmet\"}}}]")
                .matched.map { it.uid }
        )
        assertEquals(
            listOf("p1"),
            sel("@a[nbt={Inventory:[{Slot:0b,id:\"minecraft:stone\"}]}]").matched.map { it.uid }
        )
        // 未设置的键：按真实 NBT 语义不匹配，并提示缺哪个键
        val unknown = sel("@a[nbt={Health:20.0f}]")
        assertTrue(unknown.matched.isEmpty())
        assertTrue(unknown.notes.any { it.contains("Health") })
        // 在实体上补了 NBT 就能匹配
        val withHealth = steve.copy(extraNbt = "Health:20.0f")
        assertEquals(
            listOf("p1"),
            SelectorEvaluator.evaluate("@a[nbt={Health:20.0f}]", listOf(withHealth))
                .matched.map { it.uid }
        )
    }

    @Test fun derivedNbtFromGridFields() {
        val p = steve.copy(tags = setOf("vip", "staff"))
        val list = listOf(p)
        fun sel(s: String) = SelectorEvaluator.evaluate(s, list).matched.map { it.uid }

        // tag 字段与 nbt Tags 是同一份数据
        assertEquals(listOf("p1"), sel("@a[nbt={Tags:[\"vip\"]}]"))
        assertEquals(listOf("p1"), sel("@a[nbt={Tags:[\"vip\",\"staff\"]}]"))
        assertTrue(sel("@a[nbt={Tags:[\"vip\",\"other\"]}]").isEmpty())
        // name 字段与 CustomName 是同一份数据（普通字符串/JSON 文本都认）
        assertEquals(listOf("p1"), sel("@a[nbt={CustomName:\"Steve\"}]"))
        assertEquals(listOf("p1"), sel("""@a[nbt={CustomName:'{"text":"Steve"}'}]"""))
        // team 字段与 NBT Team 是同一份数据
        assertEquals(
            listOf("p1"),
            SelectorEvaluator.evaluate("@a[nbt={Team:\"red\"}]", listOf(steve.copy(team = "red")))
                .matched.map { it.uid }
        )
        // gamemode/level 派生
        val c = steve.copy(gamemode = "creative", level = 30)
        assertEquals(listOf("p1"), SelectorEvaluator.evaluate(
            "@a[nbt={playerGameType:1}]", listOf(c)
        ).matched.map { it.uid })
        assertEquals(listOf("p1"), SelectorEvaluator.evaluate(
            "@a[nbt={XpLevel:30}]", listOf(c)
        ).matched.map { it.uid })
    }

    @Test fun externalConditions() {
        val p = steve.copy(
            properties = mapOf("is_baby" to "true", "fire" to "true", "scale" to "1.5"),
            permissions = mapOf("camera" to "enabled", "movement" to "disabled"),
            predicates = setOf("minecraft:is_sneaking"),
            advancements = setOf(
                "minecraft:story/mine_diamond",
                "minecraft:story/smelt_iron|crit"
            )
        )
        val list = listOf(p)
        val bTarget = VersionSpec(VersionSpec.Edition.BEDROCK, listOf(1, 20, 80))
        fun bedrock(s: String) = SelectorEvaluator.evaluate(
            s, list, edition = "bedrock", target = bTarget
        ).matched.map { it.uid }
        fun java(s: String) = SelectorEvaluator.evaluate(s, list).matched.map { it.uid }

        assertEquals(listOf("p1"), bedrock("@a[has_property={property=is_baby}]"))
        assertTrue(bedrock("@a[has_property={property=!fire}]").isEmpty())
        assertEquals(listOf("p1"), bedrock("@a[has_property={is_baby=true}]"))
        assertEquals(listOf("p1"), bedrock("@a[has_property={scale=1..2}]"))
        assertEquals(listOf("p1"), bedrock("@a[haspermission={camera=enabled}]"))
        assertTrue(bedrock("@a[haspermission={movement=enabled}]").isEmpty())

        assertEquals(listOf("p1"), java("@a[predicate=minecraft:is_sneaking]"))
        assertTrue(java("@a[predicate=!minecraft:is_sneaking]").isEmpty())
        assertEquals(listOf("p1"), java("""@a[advancements={"minecraft:story/mine_diamond"=true}]"""))
        assertTrue(java("""@a[advancements={"minecraft:story/mine_diamond"=false}]""").isEmpty())
        assertEquals(
            listOf("p1"),
            java("""@a[advancements={"minecraft:story/smelt_iron"={"crit"=true}}]""")
        )
    }

    @Test fun originAndDistanceInterplay() {
        val a = steve.copy(uid = "a", x = 0.0, y = -60.0, z = 0.0)
        val b = steve.copy(uid = "b", x = 10.0, y = -60.0, z = 0.0)
        val list = listOf(a, b)
        fun sel(s: String, origin: Triple<Double, Double, Double> = Triple(0.0, -60.0, 0.0)) =
            SelectorEvaluator.evaluate(s, list, origin = origin)

        // 选择器自带的 x/y/z 定义 distance 的原点
        assertEquals(listOf("b"), sel("@e[x=10,y=-60,z=0,distance=..1]").matched.map { it.uid })
        // 没有 x/y/z 时用传入原点（网格里的 at 执行者 / positioned）
        assertEquals(listOf("b"), sel("@e[distance=..1]", Triple(10.0, -60.0, 0.0)).matched.map { it.uid })
        // sort/limit 用同一原点
        assertEquals(
            listOf("b"),
            sel("@e[sort=nearest,limit=1]", Triple(9.0, -60.0, 0.0)).matched.map { it.uid }
        )
        // dx 负方向：区域 [0,2]
        assertEquals(listOf("a"), sel("@e[x=1,dx=-1]").matched.map { it.uid })
    }

    @Test fun mixedEditionIdsAreNoted() {
        val r = SelectorEvaluator.evaluate(
            "@e[type=minecraft:zombified_piglin,type=minecraft:zombie_pigman]",
            listOf(zombie)
        )
        assertTrue(r.notes.any { it.contains("同时") })
    }

    @Test fun implicitLimitForPAndNAndR() {
        val two = listOf(
            steve.copy(uid = "p1", x = 0.0),
            steve.copy(uid = "p2", x = 5.0)
        )
        assertEquals(1, SelectorEvaluator.evaluate("@p", two).matched.size)
        assertEquals(1, SelectorEvaluator.evaluate("@n", two).matched.size)
        assertEquals(1, SelectorEvaluator.evaluate("@r", two).matched.size)
        assertEquals(2, SelectorEvaluator.evaluate("@a", two).matched.size)
        // 显式 limit 覆盖默认值
        assertEquals(2, SelectorEvaluator.evaluate("@p[limit=2]", two).matched.size)
    }

    @Test fun dxBoxUsesHitboxIntersectionAndPlusOneSize() {
        // 区域 x=9,dx=1 -> [9,11]；实体中心 11.2 在区域外，但 0.6 宽判定箱左缘 10.9 与区域相交
        val e = GridEntity("z", "minecraft:zombie", x = 11.2)
        assertEquals(listOf("z"), SelectorEvaluator.evaluate("@e[x=9,dx=1]", listOf(e)).matched.map { it.uid })
        // dx=0 也是 1 格宽（边长 |0|+1）；中心 10.2 的判定箱左缘 9.9 与 [9,10] 相交
        val near = GridEntity("z2", "minecraft:zombie", x = 10.2)
        assertEquals(listOf("z2"), SelectorEvaluator.evaluate("@e[x=9,dx=0]", listOf(near)).matched.map { it.uid })
        // 远离区域 -> 不匹配
        assertTrue(
            SelectorEvaluator.evaluate("@e[x=0,dx=1]", listOf(e)).matched.isEmpty()
        )
    }

    @Test fun hasitemModeledSlots() {
        val steveWith = steve.copy(
            items = mapOf(
                "armor.head" to GridItem("minecraft:diamond_helmet"),
                "weapon.mainhand" to GridItem("minecraft:diamond_sword"),
                "hotbar.0" to GridItem("minecraft:stone", 64)
            )
        )
        val list = listOf(steveWith, zombie)
        val target = VersionSpec(VersionSpec.Edition.BEDROCK, listOf(1, 20, 80))
        fun sel(s: String) = SelectorEvaluator.evaluate(
            s, list, edition = "bedrock", target = target
        ).matched.map { it.uid }

        assertEquals(listOf("p1"), sel("@a[hasitem={item=diamond_helmet,location=slot.armor.head}]"))
        assertEquals(listOf("p1"), sel("@a[hasitem={item=stone,location=slot.hotbar,slot=0,quantity=64..}]"))
        assertTrue(sel("@a[hasitem={item=stone,location=slot.hotbar,slot=0,quantity=65..}]").isEmpty())
        // item=air = 该槽为空（胸甲槽确实为空）
        assertEquals(listOf("p1"), sel("@a[hasitem={item=air,location=slot.armor.chest}]"))

        // 目标版本不够 -> 不按它筛选 + 明确提醒
        val old = SelectorEvaluator.evaluate(
            "@a[hasitem={item=diamond_helmet}]", list,
            edition = "bedrock", target = VersionSpec(VersionSpec.Edition.BEDROCK, listOf(1, 17, 0))
        )
        assertEquals(1, old.matched.size)
        assertTrue(old.notes.any { it.contains("1.18.20") })
    }

    @Test fun positionAndLimit() {
        assertEquals(listOf("c1"), ids("@e[x=9,y=-60,z=-1,dx=2,dy=1,dz=2]").matched.map { it.uid })
        assertEquals(listOf("p1"), ids("@p").matched.map { it.uid })  // 最近玩家
        assertTrue(ids("@e[limit=1]").matched.size == 1)
    }

    @Test fun unmodelledParamsAreNotSilent() {
        // 无法求值的 nbt 键也要明确提示（这里是"实体未设置该键"）
        val r = ids("@e[nbt={Health:20.0f}]")
        assertTrue(r.matched.isEmpty())
        assertTrue(r.notes.any { it.contains("Health") })
    }
}
