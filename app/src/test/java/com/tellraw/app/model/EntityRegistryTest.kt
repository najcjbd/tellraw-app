package com.tellraw.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EntityRegistryTest {
    @Test
    fun idsAreUniqueAndLookupWorks() {
        assertTrue(EntityRegistry.ALL.size > 100)
        assertEquals(EntityRegistry.ALL.size, EntityRegistry.ALL.map { it.id }.toSet().size)
        assertEquals("僵尸", EntityRegistry.byId("minecraft:zombie")?.zhName)
        assertEquals("僵尸", EntityRegistry.byId("zombie")?.zhName)
        assertEquals("玩家", EntityRegistry.byId("minecraft:player")?.zhName)
        assertEquals("盔甲架", EntityRegistry.byId("armor_stand")?.zhName)
    }

    @Test
    fun displayNameFallsBackToId() {
        assertEquals("僵尸", EntityRegistry.displayName("minecraft:zombie"))
        assertEquals("not_a_real_entity", EntityRegistry.displayName("minecraft:not_a_real_entity"))
    }
}
