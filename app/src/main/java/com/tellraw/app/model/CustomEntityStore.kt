package com.tellraw.app.model

import com.google.gson.GsonBuilder
import java.io.File

/**
 * 自定义实体（D5）：尤其"玩家" —— 正版ID / 自定义名字 / 头像。
 * 持久化到 JSON；保存后与内置 [EntityRegistry] 的实体并列，供"玩家队列/网格"选择与复用。
 */
data class CustomEntity(
    val id: String,                  // 唯一键（自定义条目的标识）
    val officialId: String? = null,  // 正版ID（玩家名/UUID）
    val displayName: String? = null, // 自定义名字（优先于官方/内置名）
    val avatarPath: String? = null,  // 头像（本地文件或 URL，后续 UI 使用）
    val builtinId: String? = null    // 外观/类型关联的内置实体（如 minecraft:player）
)

/** 自定义实体的读写（JSON 文件）。 */
class CustomEntityStore(private val file: File) {
    private val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

    fun load(): List<CustomEntity> = try {
        if (!file.exists()) emptyList()
        else gson.fromJson(file.readText(), Array<CustomEntity>::class.java)?.toList() ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }

    fun save(list: List<CustomEntity>) {
        file.parentFile?.mkdirs()
        file.writeText(gson.toJson(list))
    }

    /** 有同 id 就替换，否则追加。返回保存后的完整列表。 */
    fun upsert(entity: CustomEntity): List<CustomEntity> {
        val all = load().toMutableList()
        val i = all.indexOfFirst { it.id == entity.id }
        if (i >= 0) all[i] = entity else all.add(entity)
        save(all)
        return all
    }

    fun remove(id: String): List<CustomEntity> {
        val all = load().filterNot { it.id == id }
        save(all)
        return all
    }

    /**
     * 组合查找显示名：先自定义（按 id 或 正版ID），再内置。
     */
    fun displayNameOf(id: String): String {
        val custom = load().firstOrNull { it.id == id || it.officialId == id }
        return custom?.displayName?.takeIf { it.isNotBlank() } ?: EntityRegistry.displayName(id)
    }
}
