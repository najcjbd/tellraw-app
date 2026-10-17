package com.tellraw.app.model

import com.google.gson.GsonBuilder
import java.io.File
import java.util.UUID

/**
 * 网格里用户放置的一个实体（可编辑、可持久化）。
 * 与 [GridEntity] 的区别：这里存的是"用户配置"（可选字段用字符串编辑），
 * 求值时再转换成 [GridEntity]。
 */
data class EditableGridEntity(
    val uid: String = UUID.randomUUID().toString(),
    val id: String = "minecraft:player",
    val name: String = "",
    val tags: String = "",
    val team: String = "",
    val x: Double = 0.0,
    val y: Double = -60.0,
    val z: Double = 0.0,
    val scores: String = "",
    /** 装备：`head=diamond_helmet,mainhand=diamond_sword,offhand=shield` 等。 */
    val equipment: String = "",
    /** 物品栏：`0=stone x64,hotbar.1=bread` 等（数字键按 inventory.<n> 处理）。 */
    val inventory: String = "",
    /** 游戏模式：survival/creative/adventure/spectator（空 = 未设置）。 */
    val gamemode: String = "",
    /** 经验等级（空 = 未设置）。 */
    val level: String = "",
    /** 垂直视角 -90..90（空 = 未设置）。 */
    val rotationX: String = "",
    /** 水平视角 -180..180（空 = 未设置）。 */
    val rotationY: String = "",
    /** 额外 SNBT（覆盖/补充派生 NBT），例如 `Health:20.0f, NoAI:1b`。 */
    val nbt: String = "",
    /** Bedrock has_property：`is_baby=1, fire` 等（bare = 存在即可）。 */
    val properties: String = "",
    /** Bedrock haspermission：`camera=enabled,movement=disabled`。 */
    val permissions: String = "",
    /** Java predicate：该实体匹配的谓词 ID（逗号分隔）。 */
    val predicates: String = "",
    /** Java advancements：已完成的 ID；`id|criterion` 表示该准则已完成。 */
    val advancements: String = "",
    /** 自定义实体条目的唯一键（CustomEntity.id）；非空时显示名/头像取自它。 */
    val customKey: String? = null
) {
    fun tagSet(): Set<String> =
        tags.split(',', '，').map { it.trim() }.filter { it.isNotEmpty() }.toSet()

    /** "kills=3,deaths=1" -> map。无法解析的项忽略（界面上有提醒）。 */
    fun scoreMap(): Map<String, Int> =
        scores.split(',', '，').mapNotNull { part ->
            val i = part.indexOf('=')
            if (i <= 0) return@mapNotNull null
            val k = part.substring(0, i).trim()
            val v = part.substring(i + 1).trim().toIntOrNull() ?: return@mapNotNull null
            if (k.isEmpty()) null else k to v
        }.toMap()

    /** 装备 + 物品栏 -> 槽位名到物品的映射（供 hasitem 求值）。 */
    fun itemMap(): Map<String, GridItem> {
        val out = linkedMapOf<String, GridItem>()
        fun add(rawKey: String, rawValue: String, numericPrefix: String) {
            val key0 = rawKey.trim().lowercase()
            val key = when (key0) {
                "head", "helmet" -> "armor.head"
                "chest", "chestplate" -> "armor.chest"
                "legs", "leggings" -> "armor.legs"
                "feet", "boots" -> "armor.feet"
                "mainhand" -> "weapon.mainhand"
                "offhand" -> "weapon.offhand"
                else -> {
                    val n = key0.toIntOrNull()
                    if (n != null) "$numericPrefix.$n" else key0
                }
            }
            val m = Regex("^([^\\s*\\[]+)\\s*(?:[x*]\\s*(\\d+))?\\s*(?:d(?:amage)?\\s*=\\s*(\\d+))?$")
                .find(rawValue.trim()) ?: return
            val id0 = m.groupValues[1]
            val id = if (id0.contains(':')) id0 else "minecraft:$id0"
            val count = m.groupValues[2].toIntOrNull() ?: 1
            val damage = m.groupValues[3].toIntOrNull()
            out[key] = GridItem(id, count, damage)
        }
        for (part in equipment.split(',', '，')) {
            val i = part.indexOf('=')
            if (i > 0) add(part.substring(0, i), part.substring(i + 1), "armor")
        }
        for (part in inventory.split(',', '，')) {
            val i = part.indexOf('=')
            if (i > 0) add(part.substring(0, i), part.substring(i + 1), "inventory")
        }
        return out
    }

    fun propertyMap(): Map<String, String> =
        properties.split(',', '，').mapNotNull { part ->
            val t = part.trim()
            if (t.isEmpty()) return@mapNotNull null
            val i = t.indexOf('=')
            if (i < 0) t to "true" else t.substring(0, i).trim() to t.substring(i + 1).trim()
        }.toMap()

    fun permissionMap(): Map<String, String> =
        permissions.split(',', '，').mapNotNull { part ->
            val i = part.indexOf('=')
            if (i <= 0) null
            else part.substring(0, i).trim().lowercase() to part.substring(i + 1).trim().lowercase()
        }.toMap()

    fun predicateSet(): Set<String> =
        predicates.split(',', '，').map { it.trim() }.filter { it.isNotEmpty() }.toSet()

    fun advancementSet(): Set<String> =
        advancements.split(',', '，').map { it.trim() }.filter { it.isNotEmpty() }.toSet()

    fun toGridEntity(): GridEntity = GridEntity(
        uid = uid,
        id = if (id.contains(':')) id else "minecraft:$id",
        name = name.ifBlank { null },
        tags = tagSet(),
        team = team.ifBlank { null },
        x = x,
        y = y,
        z = z,
        scores = scoreMap(),
        families = EntityFamilies.familiesOf(id),
        items = itemMap(),
        gamemode = gamemode.ifBlank { null },
        level = level.trim().toIntOrNull(),
        rotationX = rotationX.trim().toDoubleOrNull(),
        rotationY = rotationY.trim().toDoubleOrNull(),
        hitboxWidth = when (EntityRegistry.categoryOf(id)) {
            EntityRegistry.Category.PROJECTILE -> 0.25
            EntityRegistry.Category.VEHICLE -> 1.375
            else -> 0.6
        },
        hitboxHeight = when (EntityRegistry.categoryOf(id)) {
            EntityRegistry.Category.PROJECTILE -> 0.25
            EntityRegistry.Category.VEHICLE -> 0.5625
            else -> 1.8
        },
        extraNbt = nbt,
        properties = propertyMap(),
        permissions = permissionMap(),
        predicates = predicateSet(),
        advancements = advancementSet()
    )
}

/** 网格实体的 JSON 持久化（默认文件由界面传入 filesDir）。 */
class GridEntityStore(private val file: File) {
    private val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

    fun load(): List<EditableGridEntity> = try {
        if (!file.exists()) emptyList()
        else gson.fromJson(file.readText(), Array<EditableGridEntity>::class.java)?.toList()
            ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }

    fun save(list: List<EditableGridEntity>) {
        file.parentFile?.mkdirs()
        file.writeText(gson.toJson(list))
    }
}
