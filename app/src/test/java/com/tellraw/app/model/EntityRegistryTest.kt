package com.tellraw.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test
    fun categoriesAndInventoryFlag() {
        assertEquals(EntityRegistry.Category.PLAYER, EntityRegistry.categoryOf("minecraft:player"))
        assertEquals(EntityRegistry.Category.HOSTILE, EntityRegistry.categoryOf("zombie"))
        assertEquals(EntityRegistry.Category.HOSTILE, EntityRegistry.categoryOf("minecraft:creeper"))
        assertEquals(EntityRegistry.Category.PASSIVE, EntityRegistry.categoryOf("cow"))
        assertEquals(EntityRegistry.Category.PROJECTILE, EntityRegistry.categoryOf("arrow"))
        assertEquals(EntityRegistry.Category.VEHICLE, EntityRegistry.categoryOf("oak_boat"))
        assertEquals(EntityRegistry.Category.OTHER, EntityRegistry.categoryOf("armor_stand"))
        assertTrue(EntityRegistry.hasInventory("player"))
        assertTrue(EntityRegistry.hasInventory("zombie"))
        assertTrue(EntityRegistry.hasInventory("cow"))
        assertFalse(EntityRegistry.hasInventory("arrow"))
        assertFalse(EntityRegistry.hasInventory("oak_boat"))
    }
}
