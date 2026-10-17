package com.tellraw.app.model

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files

class GridEntityStoreTest {

    @Test
    fun tagsAndScoresParsing() {
        val e = EditableGridEntity(tags = "a, b，c", scores = "kills=3,deaths=x,empty=")
        assertEquals(setOf("a", "b", "c"), e.tagSet())
        assertEquals(mapOf("kills" to 3), e.scoreMap())
    }

    @Test
    fun toGridEntityFillsFamiliesAndDefaults() {
        val e = EditableGridEntity(id = "zombie", y = -60.0)
        val g = e.toGridEntity()
        assertEquals("minecraft:zombie", g.id)
        assertEquals(-60.0, g.y, 0.0)
        assertTrue(g.families.isNotEmpty())
    }

    @Test
    fun equipmentAndInventoryParsing() {
        val e = EditableGridEntity(
            equipment = "head=diamond_helmet,mainhand=diamond_sword,offhand=shield",
            inventory = "0=stone x64,hotbar.1=bread*3"
        )
        val items = e.itemMap()
        assertEquals(GridItem("minecraft:diamond_helmet"), items["armor.head"])
        assertEquals(GridItem("minecraft:diamond_sword"), items["weapon.mainhand"])
        assertEquals(GridItem("minecraft:shield"), items["weapon.offhand"])
        assertEquals(GridItem("minecraft:stone", 64), items["inventory.0"])
        assertEquals(GridItem("minecraft:bread", 3), items["hotbar.1"])
    }

    @Test
    fun gamemodeLevelRotationParsing() {
        val g = EditableGridEntity(
            gamemode = "creative", level = "30", rotationX = "-45.5", rotationY = "90"
        ).toGridEntity()
        assertEquals("creative", g.gamemode)
        assertEquals(30, g.level)
        assertEquals(-45.5, g.rotationX!!, 0.0)
        assertEquals(90.0, g.rotationY!!, 0.0)
        // 空值 = 未设置
        val empty = EditableGridEntity().toGridEntity()
        assertNull(empty.gamemode)
        assertNull(empty.level)
        assertNull(empty.rotationX)
    }

    @Test
    fun storeRoundTrip() {
        val dir = Files.createTempDirectory("grid").toFile()
        val file = File(dir, "grid_entities.json")
        val store = GridEntityStore(file)
        val list = listOf(
            EditableGridEntity(uid = "u1", id = "minecraft:player", name = "Steve", x = 1.0, z = -2.0),
            EditableGridEntity(uid = "u2", id = "minecraft:zombie", tags = "monster", x = 3.0)
        )
        store.save(list)
        val loaded = store.load()
        assertEquals(2, loaded.size)
        assertEquals("Steve", loaded[0].name)
        assertEquals("monster", loaded[1].tags)
        assertEquals(-60.0, loaded[1].y, 0.0)
    }

    @Test
    fun corruptFileFallsBackToEmpty() {
        val dir = Files.createTempDirectory("grid").toFile()
        val file = File(dir, "grid.json")
        file.writeText("not json")
        assertTrue(GridEntityStore(file).load().isEmpty())
    }
}
