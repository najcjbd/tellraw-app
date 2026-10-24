package com.tellraw.app.model

import com.google.gson.GsonBuilder
import java.io.File

/** 网格编辑器的界面/世界设置（持久化，避免每次重开都要重填）。 */
data class GridUiSettings(
    val selector: String = "",
    val conditions: String = "",
    val worldDimension: String = "minecraft:overworld",
    val worldBiome: String = "",
    val worldLoaded: Boolean = true,
    val worldBlocks: String = "",
    val worldFunctions: String = "",
    val worldStopwatches: String = "",
    val executorUid: String? = null,
    val useExecutorOrigin: Boolean = true,
    val manualOX: String = "0",
    val manualOY: String = "-60",
    val manualOZ: String = "0",
    val camX: Double = 0.0,
    val camZ: Double = 0.0,
    val zoom: Float = 1f
)

class GridSettingsStore(private val file: File) {
    private val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

    fun load(): GridUiSettings = try {
        if (!file.exists()) GridUiSettings()
        else gson.fromJson(file.readText(), GridUiSettings::class.java) ?: GridUiSettings()
    } catch (e: Exception) {
        GridUiSettings()
    }

    fun save(settings: GridUiSettings) {
        try {
            file.parentFile?.mkdirs()
            file.writeText(gson.toJson(settings))
        } catch (e: Exception) {
            // 持久化失败不影响编辑
        }
    }
}
