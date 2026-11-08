package com.tellraw.app.model

import com.google.gson.GsonBuilder
import java.io.File
import java.util.UUID

/**
 * 网格里用户放置的一个实体（可编辑、可持久化）。
 * 与 [GridEntity] 的区别：这里存的是"用户配置"（可选字段用字符串编辑），
 * 求值时再转换成 [GridEntity]。
 */
/**
 * execute on 关系：键 -> 实体 uid 列表（passengers 可多个，其余一对一）。
 * 持久化用 `key=uid1|uid2;key2=uid`，uid 本身不含这些分隔符。
 */
object GridRelations {
    val KEYS = listOf(
        "attacker", "controller", "leasher", "origin", "owner", "passengers", "target", "vehicle"
    )

    fun label(key: String): String = when (key) {
        "attacker" -> "attacker（攻击者）"
        "controller" -> "controller（控制者）"
        "leasher" -> "leasher（拴绳者）"
        "origin" -> "origin（来源）"
        "owner" -> "owner（主人）"
        "passengers" -> "passengers（乘客，可多选）"
        "target" -> "target（目标）"
        "vehicle" -> "vehicle（载具）"
        else -> key
    }

    fun decode(raw: String?): Map<String, List<String>> {
        if (raw.isNullOrBlank()) return emptyMap()
        val out = linkedMapOf<String, List<String>>()
        for (part in raw.split(';')) {
            val i = part.indexOf('=')
            if (i <= 0) continue
            val key = part.substring(0, i).trim()
            if (key !in KEYS) continue
            val uids = part.substring(i + 1)
                .split('|')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
            if (uids.isNotEmpty()) out[key] = uids
        }
        return out
    }

    fun encode(map: Map<String, List<String>>): String =
        KEYS.mapNotNull { key ->
            val uids = map[key].orEmpty().map { it.trim() }.filter { it.isNotEmpty() }.distinct()
            if (uids.isEmpty()) null else "$key=${uids.joinToString("|")}"
        }.joinToString(";")

    fun withSelection(raw: String?, key: String, uids: List<String>): String {
        val map = decode(raw).toMutableMap()
        val clean = uids.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (clean.isEmpty()) map.remove(key) else map[key] = clean
        return encode(map)
    }

    /** 删除实体时清掉其它实体对它的关系引用。 */
    fun removeUid(raw: String?, uid: String): String {
        val map = decode(raw)
            .mapValues { (_, uids) -> uids.filterNot { it == uid } }
            .filterValues { it.isNotEmpty() }
        return encode(map)
    }

    /**
     * 保存一个实体时维护 vehicle ↔ passengers 的对等引用。
     * [old] 为空表示新实体；只处理这两个关系，其余关系不伪造反向 NBT/引用。
     */
    fun applyReciprocal(
        entities: List<EditableGridEntity>,
        old: EditableGridEntity?,
        updated: EditableGridEntity
    ): List<EditableGridEntity> {
        val byUid = entities.associateBy { it.uid }.toMutableMap()
        byUid[updated.uid] = updated

        fun relationSet(raw: String?, key: String): Set<String> =
            decode(raw)[key].orEmpty().toSet()

        fun edit(uid: String, transform: (EditableGridEntity) -> EditableGridEntity) {
            val e = byUid[uid] ?: return
            byUid[uid] = transform(e)
        }

        fun addToRelation(e: EditableGridEntity, key: String, uid: String): EditableGridEntity {
            val map = decode(e.relations).toMutableMap()
            val list = map[key].orEmpty().toMutableList()
            if (uid !in list) {
                list.add(uid)
                map[key] = list
            }
            return e.copy(relations = encode(map))
        }

        fun removeFromRelation(e: EditableGridEntity, key: String, uid: String): EditableGridEntity {
            val map = decode(e.relations).toMutableMap()
            val list = map[key].orEmpty().filterNot { it == uid }
            if (list.isEmpty()) map.remove(key) else map[key] = list
            return e.copy(relations = encode(map))
        }

        // vehicle 是一对一：给乘客换载具时，先从旧载具的 passengers 里摘掉它。
        fun setSingleVehicle(e: EditableGridEntity, vehicleUid: String): EditableGridEntity {
            val map = decode(e.relations).toMutableMap()
            val oldVehicles = map["vehicle"].orEmpty().filterNot { it == vehicleUid }
            for (oldVehicle in oldVehicles) {
                edit(oldVehicle) { removeFromRelation(it, "passengers", e.uid) }
            }
            map["vehicle"] = listOf(vehicleUid)
            return e.copy(relations = encode(map))
        }

        val oldVehicles = old?.let { relationSet(it.relations, "vehicle") }.orEmpty()
        val newVehicles = relationSet(updated.relations, "vehicle")
        for (uid in oldVehicles - newVehicles) {
            edit(uid) { removeFromRelation(it, "passengers", updated.uid) }
        }
        for (uid in newVehicles - oldVehicles) {
            edit(uid) { addToRelation(it, "passengers", updated.uid) }
        }

        val oldPassengers = old?.let { relationSet(it.relations, "passengers") }.orEmpty()
        val newPassengers = relationSet(updated.relations, "passengers")
        for (uid in oldPassengers - newPassengers) {
            edit(uid) { removeFromRelation(it, "vehicle", updated.uid) }
        }
        for (uid in newPassengers - oldPassengers) {
            edit(uid) { setSingleVehicle(it, updated.uid) }
        }

        // 补全旧存档/手动单向关系：vehicle 的反向 passengers、passengers 的反向 vehicle。
        var changed = true
        var guard = 0
        while (changed && guard++ < maxOf(12, entities.size * 4)) {
            changed = false
            for (e in byUid.values.toList()) {
                val vehicles = decode(e.relations)["vehicle"].orEmpty()
                if (vehicles.size > 1) {
                    byUid[e.uid] = setSingleVehicle(e, vehicles.first())
                    changed = true
                    continue
                }
                for (p in decode(e.relations)["passengers"].orEmpty()) {
                    val before = byUid[p] ?: continue
                    val after = addToRelation(before, "vehicle", e.uid)
                    if (after != before) {
                        byUid[p] = after
                        changed = true
                    }
                }
                for (v in decode(e.relations)["vehicle"].orEmpty()) {
                    val before = byUid[v] ?: continue
                    val after = addToRelation(before, "passengers", e.uid)
                    if (after != before) {
                        byUid[v] = after
                        changed = true
                    }
                }
            }
        }

        val out = entities.map { byUid[it.uid] ?: it }.toMutableList()
        if (out.none { it.uid == updated.uid }) out.add(byUid[updated.uid] ?: updated)
        return out
    }

    /**
     * 载具/乘客同坐标：有 vehicle 关系的实体（骑手）跟随载具；
     * passengers 关系里的实体（乘客）跟随当前载具。返回新列表，不改原对象。
     */
    fun syncPositions(list: List<EditableGridEntity>): List<EditableGridEntity> {
        if (list.isEmpty()) return list
        val byUid = list.associateBy { it.uid }.toMutableMap()
        var changedAny = false
        var guard = 0
        var changed = true
        while (changed && guard++ < maxOf(12, list.size * 4)) {
            changed = false
            for (e in list) {
                val cur = byUid[e.uid] ?: continue
                val rel = decode(cur.relations)

                val vehicleUid = rel["vehicle"]?.firstOrNull()
                val vehicle = vehicleUid?.let { byUid[it] }
                if (vehicle != null &&
                    (cur.x != vehicle.x || cur.y != vehicle.y || cur.z != vehicle.z)
                ) {
                    byUid[cur.uid] = cur.copy(x = vehicle.x, y = vehicle.y, z = vehicle.z)
                    changed = true
                    changedAny = true
                    continue
                }

                val anchor = byUid[e.uid] ?: cur
                for (passengerUid in rel["passengers"].orEmpty()) {
                    val passenger = byUid[passengerUid] ?: continue
                    if (passenger.x != anchor.x || passenger.y != anchor.y || passenger.z != anchor.z) {
                        byUid[passengerUid] = passenger.copy(x = anchor.x, y = anchor.y, z = anchor.z)
                        changed = true
                        changedAny = true
                    }
                }
            }
        }
        return if (changedAny) list.map { byUid[it.uid] ?: it } else list
    }
}

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
    val customKey: String? = null,
    /** execute on 关系：见 [GridRelations]；空串/旧存档 = 未设置。 */
    val relations: String? = ""
) {
    fun relationMap(): Map<String, List<String>> = GridRelations.decode(relations)

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
                    when {
                        n == null -> key0
                        // 数字槽按玩家物品栏习惯：0-8 是快捷栏，9+ 是主物品栏
                        numericPrefix == "inventory" && n in 0..8 -> "hotbar.$n"
                        else -> "$numericPrefix.$n"
                    }
                }
            }
            val m = Regex("^([^\\s*\\[{]+)\\s*(?:[x*]\\s*(\\d+))?\\s*(?:d(?:amage)?\\s*=\\s*(\\d+))?\\s*(\\{.*\\})?$")
                .find(rawValue.trim()) ?: return
            val id0 = m.groupValues[1]
            val id = if (id0.contains(':')) id0 else "minecraft:$id0"
            val count = m.groupValues[2].toIntOrNull() ?: 1
            val damage = m.groupValues[3].toIntOrNull()
            val comps = m.groupValues[4].takeIf { it.isNotBlank() }
                ?.let { SnbtLite.parseCompoundLenient(it) } ?: emptyMap()
            out[key] = GridItem(id, count, damage, comps)
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
        advancements = advancementSet(),
        relations = relationMap().mapValues { it.value.toSet() }
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
        // 读取失败时先保留一份备份，避免下一次保存把原文件直接覆盖掉。
        runCatching {
            if (file.exists()) file.copyTo(File(file.parentFile, file.name + ".corrupt"), overwrite = true)
        }
        emptyList()
    }

    fun save(list: List<EditableGridEntity>) {
        file.parentFile?.mkdirs()
        val json = gson.toJson(list)
        val tmp = File(file.parentFile, file.name + ".tmp")
        runCatching {
            tmp.writeText(json)
            if (!tmp.renameTo(file)) {
                file.writeText(json)
            }
        }.also { tmp.delete() }
    }
}
