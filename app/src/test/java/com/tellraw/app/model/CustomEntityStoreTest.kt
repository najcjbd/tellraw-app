package com.tellraw.app.model

import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class CustomEntityStoreTest {
    private val dir = File(System.getProperty("java.io.tmpdir"), "tt_custom_entity_test").apply {
        deleteRecursively(); mkdirs()
    }
    private val file = File(dir, "custom_entities.json")
    private val store = CustomEntityStore(file)

    @After fun cleanup() { dir.deleteRecursively() }

    @Test
    fun persistUpsertRemove() {
        assertTrue(store.load().isEmpty())

        val steve = CustomEntity(id = "p1", officialId = "Steve", displayName = "史蒂夫", avatarPath = "a.png")
        store.upsert(steve)
        assertEquals(1, store.load().size)
        assertEquals("史蒂夫", store.load()[0].displayName)
        assertEquals("Steve", store.load()[0].officialId)

        // 同 id 覆盖
        store.upsert(steve.copy(displayName = "史提夫"))
        assertEquals(1, store.load().size)
        assertEquals("史提夫", store.load()[0].displayName)

        // 删除
        assertTrue(store.remove("p1").isEmpty())
        assertTrue(store.load().isEmpty())
    }

    @Test
    fun displayNamePrefersCustomThenBuiltinThenId() {
        store.upsert(CustomEntity(id = "p2", officialId = "Notch", displayName = "创世神"))
        assertEquals("创世神", store.displayNameOf("Notch"))
        assertEquals("僵尸", store.displayNameOf("minecraft:zombie"))
        assertEquals("unknown_thing", store.displayNameOf("minecraft:unknown_thing"))
    }

    @Test
    fun corruptedFileFallsBackToEmpty() {
        file.writeText("{not valid json")
        assertTrue(store.load().isEmpty())
    }
}
