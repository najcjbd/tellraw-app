package com.tellraw.app.model

import com.google.gson.GsonBuilder
import org.junit.Assert.assertEquals
import org.junit.Test

/** 旧版本写出的 grid_entities.json 缺少后来新增的字段时，应回落到 Kotlin 默认值而不是 null。 */
class GridEntityStoreCompatTest {

    @Test
    fun partialJsonUsesKotlinDefaults() {
        val gson = GsonBuilder().create()
        val e = gson.fromJson(
            """{"uid":"u1","id":"minecraft:zombie"}""",
            EditableGridEntity::class.java
        )
        assertEquals("", e.name)
        assertEquals("", e.gamemode)
        assertEquals("", e.level)
        assertEquals("", e.nbt)
        assertEquals("", e.properties)
        assertEquals("", e.permissions)
        // 不应因 null 字段崩溃
        val ge = e.toGridEntity()
        assertEquals("u1", ge.uid)
    }
}
