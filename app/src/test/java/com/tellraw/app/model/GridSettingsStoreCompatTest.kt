package com.tellraw.app.model

import com.google.gson.GsonBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 旧/部分字段的网格设置 JSON 应回落到 Kotlin 默认值。 */
class GridSettingsStoreCompatTest {
    @Test
    fun partialSettingsJsonUsesDefaults() {
        val s = GsonBuilder().create()
            .fromJson("""{"selector":"@a"}""", GridUiSettings::class.java)
        assertEquals("@a", s.selector)
        assertEquals("", s.conditions)
        assertEquals("minecraft:overworld", s.worldDimension)
        assertTrue(s.worldLoaded)
        assertEquals("0", s.manualOX)
        assertEquals("-60", s.manualOY)
    }
}
