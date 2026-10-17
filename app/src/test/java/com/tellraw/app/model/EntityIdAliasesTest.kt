package com.tellraw.app.model

import org.junit.Assert.*
import org.junit.Test

class EntityIdAliasesTest {

    @Test
    fun bothEditionIdsMapToSameCanonical() {
        assertEquals("minecraft:zombified_piglin", EntityIdAliases.canonical("minecraft:zombie_pigman"))
        assertEquals("minecraft:zombified_piglin", EntityIdAliases.canonical("zombified_piglin"))
        assertEquals("minecraft:evoker", EntityIdAliases.canonical("minecraft:evocation_illager"))
        assertEquals("minecraft:villager", EntityIdAliases.canonical("minecraft:villager_v2"))
        assertEquals("minecraft:zombie", EntityIdAliases.canonical("minecraft:zombie"))
    }

    @Test
    fun conversionForTargetEdition() {
        assertEquals("minecraft:zombified_piglin", EntityIdAliases.toJava("minecraft:zombie_pigman"))
        assertEquals("minecraft:zombie_pigman", EntityIdAliases.toBedrock("minecraft:zombified_piglin"))
        assertEquals("minecraft:evoker", EntityIdAliases.toJava("minecraft:evocation_illager"))
        assertEquals("minecraft:evocation_illager", EntityIdAliases.toBedrock("minecraft:evoker"))
        assertEquals("minecraft:villager", EntityIdAliases.toJava("minecraft:villager_v2"))
        // 基岩当前写法优先；旧 ID 也能识别并升级
        assertEquals("minecraft:vindicator", EntityIdAliases.toBedrock("minecraft:vindication_illager"))
        assertEquals(
            "minecraft:eye_of_ender_signal",
            EntityIdAliases.toBedrock("minecraft:eye_of_ender")
        )
        // 基岩统一的 boat 无法用单一 Java ID 表达
        assertNull(EntityIdAliases.toJava("minecraft:boat"))
        assertEquals("minecraft:boat", EntityIdAliases.toBedrock("minecraft:spruce_boat"))
        // 未知 ID / 无需改写时保留用户写法
        assertEquals("minecraft:unknown_mob", EntityIdAliases.toJava("minecraft:unknown_mob"))
        assertEquals("player", EntityIdAliases.toJava("player"))
        assertEquals("player", EntityIdAliases.toBedrock("player"))
        assertEquals("zombie", EntityIdAliases.toJava("zombie"))
    }

    @Test
    fun mixedEditionIdsWarn() {
        assertNull(EntityIdAliases.detectMixedEditionIds("@e[type=minecraft:zombified_piglin]"))
        assertNull(EntityIdAliases.detectMixedEditionIds("@e[type=minecraft:zombie]"))
        val warning = EntityIdAliases.detectMixedEditionIds(
            "@e[type=minecraft:zombified_piglin,type=minecraft:zombie_pigman]"
        )
        assertNotNull(warning)
        assertTrue(warning!!.contains("zombified_piglin"))
        assertTrue(warning.contains("zombie_pigman"))
        // 反选也算
        assertNotNull(
            EntityIdAliases.detectMixedEditionIds("@e[type=!minecraft:evoker,type=minecraft:evocation_illager]")
        )
    }

    @Test
    fun counterpartForPicker() {
        assertEquals("minecraft:zombie_pigman", EntityIdAliases.counterpart("minecraft:zombified_piglin"))
        assertEquals("minecraft:zombified_piglin", EntityIdAliases.counterpart("minecraft:zombie_pigman"))
        assertNull(EntityIdAliases.counterpart("minecraft:zombie"))
    }

    @Test
    fun registryAndFamiliesRecognizeAliases() {
        assertEquals("僵尸猪灵", EntityRegistry.displayName("minecraft:zombie_pigman"))
        assertEquals(EntityRegistry.Category.HOSTILE, EntityRegistry.categoryOf("minecraft:zombie_pigman"))
        assertTrue(EntityFamilies.familiesOf("minecraft:zombie_pigman").isNotEmpty())
    }
}
