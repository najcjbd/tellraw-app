package com.tellraw.app.util

import org.junit.Assert.*
import org.junit.Test

/**
 * 新旧版本差异统一表（[VersionDiff]）的单测。
 * 见 `ttellraw/need/新旧版本差异规范.txt`：所有生成/改写只查这张表，出 bug 只改一处。
 * 数据依据：数据组件.txt:1238/566-569、nbt1.txt:182、minecraft.wiki /w/Data_component_format
 */
class VersionDiffTest {

    @Test
    fun testSyntaxParsingDefaultsToModern() {
        assertEquals(VersionDiff.NbtSyntax.MODERN, VersionDiff.NbtSyntax.from("modern"))
        assertEquals(VersionDiff.NbtSyntax.LEGACY, VersionDiff.NbtSyntax.from("legacy"))
        assertEquals(VersionDiff.NbtSyntax.FOLLOW_INPUT, VersionDiff.NbtSyntax.from("follow"))
        // 默认新版；非法值也回落成新版
        assertEquals(VersionDiff.NbtSyntax.MODERN, VersionDiff.NbtSyntax.from(null))
        assertEquals(VersionDiff.NbtSyntax.MODERN, VersionDiff.NbtSyntax.from("随便什么"))
    }

    @Test
    fun testKeyRenameTableIsBidirectional() {
        assertEquals("minecraft:damage", VersionDiff.KEY_RENAMES["Damage"])
        assertEquals("count", VersionDiff.KEY_RENAMES["Count"])
        assertEquals("enchantments", VersionDiff.KEY_RENAMES["Enchantments"])
        assertEquals("unbreakable", VersionDiff.KEY_RENAMES["Unbreakable"])
        assertEquals("custom_name", VersionDiff.DISPLAY_RENAMES["Name"])
        assertEquals("lore", VersionDiff.DISPLAY_RENAMES["Lore"])
        assertEquals("dyed_color", VersionDiff.DISPLAY_RENAMES["color"])
        // 反查
        assertEquals("Damage", VersionDiff.MODERN_TO_LEGACY_KEY["minecraft:damage"])
        assertEquals("display.Name", VersionDiff.MODERN_TO_LEGACY_KEY["custom_name"])
    }

    @Test
    fun testCountValueShapeTransforms() {
        // 1.20.5 起 count 是整数（实测 v2=0：count:1b 不匹配；v3=1：count:1 匹配）
        assertEquals("1", VersionDiff.legacyValueToModern("Count", "1b"))
        assertEquals("1", VersionDiff.legacyValueToModern("Count", "1"))
        assertNull(VersionDiff.legacyValueToModern("Count", "abc"))
        assertEquals("1b", VersionDiff.modernValueToLegacy("count", "1"))
        // 还没实现的键：返回 null = "只改键名 + 提醒"，不许瞎猜
        // （Enchantments/Potion 本轮已实现，改用仍是"只改名"的 AttributeModifiers 当例子）
        assertNull(VersionDiff.legacyValueToModern("AttributeModifiers", "[{Amount:1.0}]"))
        assertNull(VersionDiff.modernValueToLegacy("attribute_modifiers", "[{amount:1.0}]"))
        assertNull(VersionDiff.modernValueToLegacy("profile", "{name:\"x\"}"))
    }

    @Test
    fun testTagBodySplitKnownKeysAndCustomData() {
        // 认得的原版键 -> 各自组件；不认得的 -> custom_data（wiki: custom_data = 任意自定义数据）
        val (c1, u1) = VersionDiff.legacyTagBodyToComponentMap("foo:1")!!
        assertEquals(listOf("\"minecraft:custom_data\":{foo:1}"), c1)
        assertTrue(u1.isEmpty())

        val (c2, _) = VersionDiff.legacyTagBodyToComponentMap("Damage:5")!!
        assertEquals(listOf("\"minecraft:damage\":5"), c2)

        val (c3, _) = VersionDiff.legacyTagBodyToComponentMap(
            "Enchantments:[{id:\"minecraft:sharpness\",lvl:3}]"
        )!!
        // z7 实测纠正：**没有** levels 外壳（wiki 原例 [enchantments={sharpness:3}]）
        assertEquals(listOf("\"enchantments\":{\"minecraft:sharpness\":3}"), c3)

        // 混合：附魔 + 自定义 + 损耗值 -> 三个组件
        val (c4, _) = VersionDiff.legacyTagBodyToComponentMap(
            "Enchantments:[{id:\"minecraft:sharpness\",lvl:3}],foo:1,Damage:2"
        )!!
        assertEquals(3, c4.size)
        assertTrue(c4.any { it.startsWith("\"enchantments\"") })
        assertTrue(c4.any { it.startsWith("\"minecraft:damage\"") })
        assertTrue(c4.any { it.startsWith("\"minecraft:custom_data\"") })
    }

    @Test
    fun testDisplayContainerIsUnwrapped() {
        // display:{Name/Lore/color} 在新版是平级组件，display 壳要拆掉；display 里其它子键仍算自定义数据
        val (c, u) = VersionDiff.legacyTagBodyToComponentMap(
            "display:{Name:\"\\\"n\\\"\",Lore:[\"a\"],foo:1}"
        )!!
        assertTrue(c.any { it.startsWith("\"custom_name\"") })
        assertTrue(c.any { it.startsWith("\"lore\"") })
        assertTrue(c.any { it.contains("custom_data") && it.contains("display:{foo:1}") })
        // 这两个的值形状我没把握 -> 要提醒
        assertTrue(u.contains("display.Name"))
        assertTrue(u.contains("display.Lore"))
    }

    @Test
    fun testUnbreakableTruthyAndFalsy() {
        val (on, _) = VersionDiff.legacyTagBodyToComponentMap("Unbreakable:1b")!!
        assertEquals(listOf("\"unbreakable\":{}"), on)
        // 旧版"不毁=假" = 新版"没有这个组件" -> 一个组件都不产出
        val (off, _) = VersionDiff.legacyTagBodyToComponentMap("Unbreakable:0b")!!
        assertTrue(off.isEmpty())
        assertEquals("", VersionDiff.legacyTagBodyToComponents("Unbreakable:0b"))
    }

    @Test
    fun testEnchantmentsAndPotionBothWays() {
        assertEquals(
            "{\"minecraft:sharpness\":3}",
            VersionDiff.legacyValueToModern("Enchantments", "[{id:\"minecraft:sharpness\",lvl:3}]")
        )
        assertEquals(
            "[{id:\"minecraft:sharpness\",lvl:3}]",
            VersionDiff.modernValueToLegacy("enchantments", "{\"minecraft:sharpness\":3}")
        )
        assertEquals(
            "{potion:\"minecraft:water\"}",
            VersionDiff.legacyValueToModern("Potion", "\"minecraft:water\"")
        )
        assertEquals(
            "minecraft:water",
            VersionDiff.modernValueToLegacy("potion_contents", "{potion:\"minecraft:water\"}")
        )
        // 形状没把握的键 -> null（只改键名 + 提醒）
        assertNull(VersionDiff.legacyValueToModern("AttributeModifiers", "[{Amount:1.0}]"))
    }

    @Test
    fun testModernComponentsToLegacyTag() {
        // custom_data 摊回 tag
        assertEquals(
            "foo:1,bar:2",
            VersionDiff.modernComponentsToLegacyTag("minecraft:custom_data:{foo:1,bar:2}")!!.first
        )
        // 损耗值 / 不毁
        assertEquals("Damage:5", VersionDiff.modernComponentsToLegacyTag("minecraft:damage:5")!!.first)
        assertEquals("Unbreakable:1b", VersionDiff.modernComponentsToLegacyTag("minecraft:unbreakable:{}")!!.first)
        // 附魔反向
        assertEquals(
            "Enchantments:[{id:\"minecraft:sharpness\",lvl:3}]",
            VersionDiff.modernComponentsToLegacyTag(
                "minecraft:enchantments:{\"minecraft:sharpness\":3}"
            )!!.first
        )
        // display 三件套 -> 合并成一个 display 复合（旧格式只有一个 display）
        assertEquals(
            "display:{Name:\"n\",Lore:[\"a\"],color:16711680}",
            VersionDiff.modernComponentsToLegacyTag("custom_name:\"n\",lore:[\"a\"],dyed_color:16711680")!!.first
        )
        // 堆栈层级的 count 不该塞进 tag；新版专有组件无处安放（两者都要提醒）
        val c = VersionDiff.modernComponentsToLegacyTag("count:5")!!
        assertEquals("", c.first)
        assertTrue(c.third.contains("count"))
        val e = VersionDiff.modernComponentsToLegacyTag("minecraft:enchantable:{value:15}")!!
        assertTrue(e.third.contains("enchantable"))
    }

    @Test
    fun testLegacyModernRoundTrip() {
        // 旧 -> 新 -> 旧 应当回到原样（已实现值形状换算的键）
        for (body in listOf("Damage:5", "RepairCost:2", "Unbreakable:1b", "foo:1,bar:2")) {
            val fwd = VersionDiff.legacyTagBodyToComponentsBody(body)!!.first
            assertEquals(body, VersionDiff.modernComponentsToLegacyTag(fwd)!!.first)
        }
    }

    @Test
    fun testPolicyDecisionMatrix() {
        val onlyLegacy = "{id:\"minecraft:diamond\",tag:{foo:1}}"
        val onlyModern = "{id:\"minecraft:diamond\",components:{\"minecraft:custom_data\":{foo:1}}}"
        val mixed = "{id:\"minecraft:diamond\",tag:{foo:1},components:{\"minecraft:damage\":2}}"
        val neither = "{id:\"minecraft:diamond\"}"

        // MODERN（默认）：出现旧写法（含混用）就改写成新版
        assertEquals(VersionDiff.Decision.TO_MODERN, VersionDiff.decide(VersionDiff.NbtSyntax.MODERN, onlyLegacy))
        assertEquals(VersionDiff.Decision.KEEP, VersionDiff.decide(VersionDiff.NbtSyntax.MODERN, onlyModern))
        assertEquals(VersionDiff.Decision.TO_MODERN, VersionDiff.decide(VersionDiff.NbtSyntax.MODERN, mixed))
        assertEquals(VersionDiff.Decision.KEEP, VersionDiff.decide(VersionDiff.NbtSyntax.MODERN, neither))

        // LEGACY：出现新写法（含混用）就降级回旧版
        assertEquals(VersionDiff.Decision.KEEP, VersionDiff.decide(VersionDiff.NbtSyntax.LEGACY, onlyLegacy))
        assertEquals(VersionDiff.Decision.TO_LEGACY, VersionDiff.decide(VersionDiff.NbtSyntax.LEGACY, onlyModern))
        assertEquals(VersionDiff.Decision.TO_LEGACY, VersionDiff.decide(VersionDiff.NbtSyntax.LEGACY, mixed))

        // FOLLOW_INPUT：只有旧/只有新都原样（你的裁决 B：尊重玩家写法）；混用才问一嘴
        assertEquals(VersionDiff.Decision.KEEP, VersionDiff.decide(VersionDiff.NbtSyntax.FOLLOW_INPUT, onlyLegacy))
        assertEquals(VersionDiff.Decision.KEEP, VersionDiff.decide(VersionDiff.NbtSyntax.FOLLOW_INPUT, onlyModern))
        assertEquals(VersionDiff.Decision.ASK, VersionDiff.decide(VersionDiff.NbtSyntax.FOLLOW_INPUT, mixed))
    }

    @Test
    fun testCanBreakUsesBlockPredicateObject() {
        // z12 实测：直接给列表 `can_break=["minecraft:stone"]` 是**语法错误**（整个文件不加载）；
        // z13 实测：`can_break={blocks:["minecraft:stone"]}` 才对 -> 必须包一层 blocks
        assertEquals(
            "{blocks:[\"minecraft:stone\",\"minecraft:dirt\"]}",
            VersionDiff.legacyValueToModern("CanDestroy", "[\"minecraft:stone\",\"minecraft:dirt\"]")
        )
        assertEquals(
            "[\"minecraft:stone\",\"minecraft:dirt\"]",
            VersionDiff.modernValueToLegacy("can_break", "{blocks:[\"minecraft:stone\",\"minecraft:dirt\"]}")
        )
    }

    @Test
    fun testLegacyTagBodyRules() {
        assertTrue(VersionDiff.legacyTagBodyToComponents("Damage:5").contains("minecraft:damage"))
        assertTrue(VersionDiff.legacyTagBodyToComponents("Damage:5b").contains("minecraft:damage"))
        assertTrue(VersionDiff.legacyTagBodyToComponents("foo:1").contains("minecraft:custom_data"))
    }
}
