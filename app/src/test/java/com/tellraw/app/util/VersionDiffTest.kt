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
        assertNull(VersionDiff.legacyValueToModern("Enchantments", "[{id:\"x\",lvl:1}]"))
        assertNull(VersionDiff.modernValueToLegacy("enchantments", "{levels:{x:1}}"))
    }

    @Test
    fun testLegacyTagBodyRules() {
        assertTrue(VersionDiff.legacyTagBodyToComponents("Damage:5").contains("minecraft:damage"))
        assertTrue(VersionDiff.legacyTagBodyToComponents("Damage:5b").contains("minecraft:damage"))
        assertTrue(VersionDiff.legacyTagBodyToComponents("foo:1").contains("minecraft:custom_data"))
    }
}
