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

    @Test fun invalidManualNbtReturnsNull() {
        assertNull(KnownNbt.merge("Health:", mapOf("Health" to "20")))
    }
}
