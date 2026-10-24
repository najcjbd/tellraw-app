package com.tellraw.app.model

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files

class GridSettingsStoreTest {
    @Test fun roundTrip() {
        val dir = Files.createTempDirectory("gridset").toFile()
        val store = GridSettingsStore(File(dir, "grid_settings.json"))
        val s = GridUiSettings(
            selector = "@e[type=zombie]",
            conditions = "if data entity @s {Health:..10}",
            worldDimension = "minecraft:the_nether",
            worldBiome = "minecraft:nether_wastes",
            worldLoaded = false,
            worldBlocks = "0,-60,0=chest",
            worldFunctions = "test:fn",
            worldStopwatches = "sw=3",
            executorUid = "u1",
            useExecutorOrigin = false,
            manualOX = "1", manualOY = "-50", manualOZ = "2",
            camX = 3.5, camZ = -2.0, zoom = 1.5f
        )
        store.save(s)
        val back = store.load()
        assertEquals(s, back)
    }

    @Test fun corruptFileFallsBack() {
        val dir = Files.createTempDirectory("gridset").toFile()
        val f = File(dir, "bad.json"); f.writeText("not json")
        assertEquals(GridUiSettings(), GridSettingsStore(f).load())
    }
}
