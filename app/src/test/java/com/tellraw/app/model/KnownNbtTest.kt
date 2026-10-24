package com.tellraw.app.model

import org.junit.Assert.*
import org.junit.Test

class KnownNbtTest {

    @Test fun typedWriteBack() {
        fun spec(k: String) = KnownNbt.ALL.first { it.key == k }
        assertEquals("20.0f", KnownNbt.nbtTextOf(spec("Health"), "20"))
        assertEquals("1b", KnownNbt.nbtTextOf(spec("NoAI"), "true"))
        assertEquals("0b", KnownNbt.nbtTextOf(spec("NoAI"), "false"))
        assertEquals("100s", KnownNbt.nbtTextOf(spec("Fire"), "100"))
    }

    @Test fun manualNbtFillsQuick() {
        val out = mutableMapOf<String, String>()
        KnownNbt.fillQuick("""Health:18.5f,NoAI:1b,Silent:0b,Tags:["a"]""", out)
        assertEquals("18.5", out["Health"])
        assertEquals("true", out["NoAI"])
        assertEquals("false", out["Silent"])
        // Tags 由上方 tags 字段负责，不重复进快捷表
        assertNull(out["Tags"])
    }

    @Test fun quickChangesMergeWithoutDroppingUnknownKeys() {
        val merged = KnownNbt.merge(
            """Health:20.0f,NoAI:1b,Tags:["a"],SomeModData:{x:1}""",
            mapOf("Health" to "30", "NoAI" to "", "Glowing" to "true")
        )
        requireNotNull(merged)
        assertTrue(merged, merged.contains("Health:30.0f"))
        assertFalse("清空的快捷键应被移除", merged.contains("NoAI"))
        assertTrue(merged, merged.contains("Glowing:1b"))
        assertTrue("未知键不能丢", merged.contains("Tags"))
        assertTrue("未知键不能丢", merged.contains("SomeModData"))
    }

    @Test fun externalFieldsReadAndStrip() {
        val ext = KnownNbt.readExternal(
            """{Tags:["a","b"],CustomName:'{"text":"Bob"}',Team:"red",Health:20.0f}"""
        )
        requireNotNull(ext)
        assertEquals("a,b", ext.tags)
        assertEquals("Bob", ext.name)
        assertEquals("red", ext.team)

        val stripped = KnownNbt.stripExternal(
            """{Health:20.0f,Tags:["a"],CustomName:"Bob",Team:"red"}"""
        )
        requireNotNull(stripped)
        assertFalse(stripped.contains("Tags"))
        assertFalse(stripped.contains("CustomName"))
        assertFalse(stripped.contains("Team"))
        assertTrue(stripped.contains("Health"))
    }

    @Test fun perEntityCatalogMergesWithCommon() {
        val sheep = KnownNbt.specsFor("minecraft:sheep")
        assertTrue(sheep.any { it.key == "Color" && it.kind == KnownNbt.Kind.BYTE })
        assertTrue(sheep.any { it.key == "Sheared" })
        assertTrue("通用键仍保留", sheep.any { it.key == "Health" })
        assertEquals(
            "Color:5b",
            KnownNbt.merge("", mapOf("Color" to "5"), "minecraft:sheep")
        )
        // 别名实体也能拿到专属目录
        assertTrue(KnownNbt.specsFor("minecraft:zombie_pigman").any { it.key == "IsBaby" })
        // 字符串键
        assertEquals(
            "variant:\"pale\"",
            KnownNbt.merge("", mapOf("variant" to "pale"), "minecraft:wolf")
        )
    }

    @Test fun playerAndStructuredKeys() {
        val p = KnownNbt.specsFor("minecraft:player")
        assertTrue(p.any { it.key == "XpLevel" && it.kind == KnownNbt.Kind.INT })
        assertTrue(p.any { it.key == "Inventory" && it.kind == KnownNbt.Kind.LIST })

        val merged = KnownNbt.merge(
            "",
            mapOf("Inventory" to """[{Slot:0b,id:"minecraft:stone"}]"""),
            "minecraft:player"
        )
        requireNotNull(merged)
        assertTrue(merged, merged.contains("Inventory:[{Slot:0b"))

        // 结构化值必须是合法 SNBT；非法值不写回
        assertEquals(
            "",
            KnownNbt.merge("", mapOf("Inventory" to "not a list"), "minecraft:player")
        )
        // 列表/复合键从现有 NBT 回填
        val out = mutableMapOf<String, String>()
        KnownNbt.fillQuick(
            """attributes:[{id:"minecraft:movement_speed",base:0.3d}],Health:20.0f""",
            out, "minecraft:zombie"
        )
        assertTrue(out["attributes"]?.contains("movement_speed") == true)
    }

    @Test fun invalidManualNbtReturnsNull() {
        assertNull(KnownNbt.merge("Health:", mapOf("Health" to "20")))
    }
}
