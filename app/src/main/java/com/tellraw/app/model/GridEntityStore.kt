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
            val m = Regex("^([^\\s*]+)\\s*(?:[x*]\\s*(\\d+))?$").find(rawValue.trim())
                ?: return
            val id0 = m.groupValues[1]
            val id = if (id0.contains(':')) id0 else "minecraft:$id0"
            val count = m.groupValues[2].toIntOrNull() ?: 1
            out[key] = GridItem(id, count)
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
        items = itemMap()
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
