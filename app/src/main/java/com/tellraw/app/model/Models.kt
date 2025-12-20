package com.tellraw.app.model

data class SelectorConversionResult(
    val javaSelector: String,
    val bedrockSelector: String,
    val javaReminders: List<String>,
    val bedrockReminders: List<String>,
    val wasConverted: Boolean
)

data class TellrawCommand(
    val javaCommand: String,
    val bedrockCommand: String,
    val warnings: List<String>
)

data class ColorCodeMapping(
    val javaColor: String,
    val bedrockColor: String,
    val description: String
)

data class SelectorParameter(
    val name: String,
    val value: String,
    val javaOnly: Boolean = false,
    val bedrockOnly: Boolean = false,
    val requiresConversion: Boolean = false
)

enum class SelectorType {
    JAVA, BEDROCK, UNIVERSAL
}

enum class GameMode(val javaName: String, val bedrockName: String, val bedrockCode: String) {
    SURVIVAL("survival", "survival", "0"),
    CREATIVE("creative", "creative", "1"),
    ADVENTURE("adventure", "adventure", "2"),
    SPECTATOR("spectator", "survival", "0"), // 基岩版没有旁观模式，转换为生存模式
    DEFAULT("survival", "survival", "5") // 基岩版默认模式
}

enum class MinecraftVersion {
    JAVA, BEDROCK
}