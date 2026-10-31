package com.tellraw.app.model

import com.tellraw.app.util.ExecCondSupport
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * 网格里的实体（供目标选择器求值）。
 * 字段覆盖选择器最常用的筛选维度；未建模的维度（gamemode/level/rotation…）在求值时会被明确提示，而不是静默忽略。
 */
data class GridEntity(
    val uid: String,
    val id: String,
    val name: String? = null,
    val tags: Set<String> = emptySet(),
    val team: String? = null,
    val x: Double = 0.0,
    val y: Double = -60.0,
    val z: Double = 0.0,
    val scores: Map<String, Int> = emptyMap(),
    /** 由实体类型派生的族（见 [EntityFamilies]）。 */
    val families: Set<String> = emptySet(),
    /** 已建模的物品栏/装备：槽位名（如 armor.head / weapon.mainhand / hotbar.0）-> 物品。 */
    val items: Map<String, GridItem> = emptyMap(),
    /** 游戏模式（survival/creative/adventure/spectator）。 */
    val gamemode: String? = null,
    /** 经验等级（仅玩家）。 */
    val level: Int? = null,
    /** 垂直视角（-90 上 .. 90 下）。 */
    val rotationX: Double? = null,
    /** 水平视角（-180..180，正南 0、顺时针增）。 */
    val rotationY: Double? = null,
    /** 判定箱宽度（XZ 平面），用于 dx/dy/dz 的"相交"判定。 */
    val hitboxWidth: Double = 0.6,
    /** 判定箱高度（从脚部 y 往上）。 */
    val hitboxHeight: Double = 1.8,
    /** 额外 SNBT（用户手填，参与 nbt= 求值；会覆盖同名派生值）。 */
    val extraNbt: String = "",
    /** Bedrock has_property：属性名 -> 值（"true" 表示存在即可）。 */
    val properties: Map<String, String> = emptyMap(),
    /** Bedrock haspermission：camera/movement -> enabled/disabled。 */
    val permissions: Map<String, String> = emptyMap(),
    /** Java predicate：该实体匹配的谓词 ID 集合。 */
    val predicates: Set<String> = emptySet(),
    /** Java advancements：已完成的 `id` 或 `id|criterion`。 */
    val advancements: Set<String> = emptySet()
) {
    val isPlayer: Boolean get() = id == "minecraft:player"
}

/** 网格实体某个槽位里的物品（count 用于 hasitem quantity 判定）。 */
data class GridItem(
    val id: String,
    val count: Int = 1,
    val damage: Int? = null,
    /** 物品数据组件（键可为短名或命名空间 ID），用于 items 谓词。 */
    val components: Map<String, SnbtLite.Value> = emptyMap()
)

/**
 * 在网格实体上求值一个目标选择器：返回"会被选中"的实体 + 无法求值/不支持的提醒。
 *
 * 设计原则（对应需求）：**目标版本不支持某参数时不能静默当成"没选中"**，必须落进 notes。
 */
object SelectorEvaluator {

    data class Result(
        val matched: List<GridEntity>,
        val notes: List<String>
    )

    /**
     * @param edition 目标平台："java" / "bedrock"
     * @param version 目标版本数值（如 1.16.100 -> 1_16_100），用于 family 等特性门槛；null=最新
     */
    fun evaluate(
        selector: String,
        entities: List<GridEntity>,
        executor: GridEntity? = null,
        edition: String = "java",
        version: Long? = null,
        origin: Triple<Double, Double, Double> = Triple(0.0, -60.0, 0.0),
        target: VersionSpec? = null
    ): Result {
        val notes = mutableListOf<String>()
        // 同一条选择器里两版实体 ID 同时出现 -> 提醒（不改变求值，只提示可能写重/混用）
        EntityIdAliases.detectMixedEditionIds(selector)?.let { notes.add(it) }
        val s = selector.trim()
        val base = s.substringBefore('[').trim()
        val paramsPart = if ('[' in s && s.endsWith("]")) s.substringAfter('[').dropLast(1) else ""
        val params = if (paramsPart.isBlank()) emptyList()
        else ExecCondSupport.splitTopLevel(paramsPart, ',').map { it.trim() }.filter { it.isNotEmpty() }

        val isBedrock = target?.edition == VersionSpec.Edition.BEDROCK ||
            (target == null && edition == "bedrock")
        val hasTypeParam = params.any { it.substringBefore('=').trim() == "type" }

        // 1) 选择器本体：先确定候选集合
        var candidates: List<GridEntity> = when (base) {
            "@a", "@p" -> entities.filter { it.isPlayer }
            // 基岩 @r 可以配 type= 选非玩家；Java @r 只选玩家
            "@r" -> if (isBedrock && hasTypeParam) entities else entities.filter { it.isPlayer }
            "@e", "@n" -> entities
            "@s" -> listOfNotNull(executor)
            "@c", "@v", "@initiator" -> {
                notes.add("\"$base\" 仅基岩/教育版，且依赖运行环境；网格里按\"全部实体\"近似")
                entities
            }
            else -> {
                notes.add("无法识别的选择器 \"$base\"")
                return Result(emptyList(), notes)
            }
        }
        if (hasTypeParam && (base == "@a" || base == "@p" || (base == "@r" && !isBedrock))) {
            notes.add("\"$base\" 上 type= 无效（该选择器本身就限定了玩家）")
        }
        if (base == "@n" && target != null && target != VersionSpec.LATEST &&
            !FeatureGate.isAvailable(FeatureGate.Feature.AT_N, target)
        ) {
            notes.add("@n 在目标版本不存在（Java 1.21+ / 基岩 1.21.100+ 才有）；网格仍按最近实体求值")
        }

        var ox = origin.first
        var oy = origin.second
        var oz = origin.third
        var dx: Double? = null
        var dy: Double? = null
        var dz: Double? = null
        var maxDist: Double? = null
        var minDist: Double? = null
        var limit: Int? = null
        var sort: String? = null
        var invalidLimit = false

        // 2) 逐参数筛选
        candidates = candidates.filter { e ->
            for (p in params) {
                val i = ExecCondSupport.indexOfTopLevel(p, '=')
                if (i < 0) { notes.add("参数 \"$p\" 无法解析"); return@filter false }
                val key = p.substring(0, i).trim()
                val value = p.substring(i + 1).trim().removeSurrounding("\"")
                when (key) {
                    "type" -> if (!matchType(e, value, notes)) return@filter false
                    "name" -> if (!matchName(e, value)) return@filter false
                    "tag" -> if (!matchTag(e, value)) return@filter false
                    "team" -> if (!matchTeam(e, value)) return@filter false
                    "family" -> {
                        val editionOk = if (target != null) target.edition == VersionSpec.Edition.BEDROCK
                        else edition == "bedrock"
                        val versionOk = if (target != null) {
                            FeatureGate.isAvailable(FeatureGate.Feature.FAMILY_PARAM, target)
                        } else {
                            version == null || version >= 1_16_100L
                        }
                        if (!editionOk || !versionOk) {
                            notes.add("family= 仅基岩版 ≥1.16.100 支持；目标版本不满足，**没有按它筛选**（不能当作已匹配）")
                        } else if (!matchFamily(e, value)) return@filter false
                    }
                    "scores" -> if (!matchScores(e, value, notes)) return@filter false
                    "x" -> ox = coord(value, ox)
                    "y" -> oy = coord(value, oy)
                    "z" -> oz = coord(value, oz)
                    "dx" -> dx = value.toDoubleOrNull()
                    "dy" -> dy = value.toDoubleOrNull()
                    "dz" -> dz = value.toDoubleOrNull()
                    "distance" -> { val (a, b) = parseRange(value); minDist = a; maxDist = b }
                    "r" -> maxDist = value.toDoubleOrNull()
                    "rm" -> minDist = value.toDoubleOrNull()
                    "limit" -> {
                        val n = value.toIntOrNull()
                        if (n == null || n < 0) {
                            notes.add("limit=$value 无效（Java 要求非负整数），该选择器不会匹配任何实体")
                            invalidLimit = true
                        } else {
                            limit = n
                        }
                    }
                    "sort" -> sort = value
                    "c" -> {
                        val n = value.toIntOrNull()
                        if (n != null) {
                            limit = kotlin.math.abs(n)
                            // 基岩 c= 按到执行点距离排序；负值取最远
                            sort = if (n < 0) "furthest" else "nearest"
                        }
                    }
                    "hasitem" -> {
                        val editionOk = if (target != null) target.edition == VersionSpec.Edition.BEDROCK
                        else edition == "bedrock"
                        val versionOk = if (target != null) {
                            FeatureGate.isAvailable(FeatureGate.Feature.HASITEM_PARAM, target)
                        } else {
                            version == null || version >= 1_18_20L
                        }
                        if (!editionOk || !versionOk) {
                            notes.add("hasitem= 仅基岩版 ≥1.18.20 支持；目标版本不满足，**没有按它筛选**")
                        } else if (!matchHasitem(e, value, notes)) return@filter false
                    }
                    "gamemode", "m" ->
                        if (!matchGamemode(e, value, notes)) return@filter false
                    "level" -> {
                        val (lo, hi) = parseRange(value)
                        val lv = e.level
                        if (lv == null) {
                            notes.add("level= 只适用于玩家；非玩家自动排除")
                            return@filter false
                        } else if (lv < (lo ?: 0.0) || lv > (hi ?: Double.MAX_VALUE)) {
                            return@filter false
                        }
                    }
                    "l", "lm" -> {
                        val num = value.toDoubleOrNull()
                        val lv = e.level
                        if (lv == null) {
                            notes.add("$key= 只适用于玩家；非玩家自动排除")
                            return@filter false
                        } else if (num == null) {
                            notes.add("$key=$value 无法解析，已忽略")
                        } else if (key == "l" && lv > num) {
                            return@filter false
                        } else if (key == "lm" && lv < num) {
                            return@filter false
                        }
                    }
                    "x_rotation", "y_rotation" -> {
                        val (lo, hi) = parseRange(value)
                        val actual = if (key == "x_rotation") e.rotationX else e.rotationY
                        if (actual == null) {
                            notes.add("$key= 需要实体设置视角；该实体未设置，未按它筛选")
                        } else if (actual < (lo ?: Double.NEGATIVE_INFINITY) ||
                            actual > (hi ?: Double.MAX_VALUE)
                        ) {
                            return@filter false
                        }
                    }
                    "rx", "ry", "rxm", "rym" -> {
                        // 基岩：rx/ry 是上限、rxm/rym 是下限（单值），不是"必须相等"
                        val actual = if (key.startsWith("rx")) e.rotationX else e.rotationY
                        if (actual == null) {
                            notes.add("$key= 需要实体设置视角；该实体未设置，未按它筛选")
                        } else if (".." in value) {
                            val (lo, hi) = parseRange(value)
                            if (actual < (lo ?: Double.NEGATIVE_INFINITY) ||
                                actual > (hi ?: Double.MAX_VALUE)
                            ) return@filter false
                        } else {
                            val n = value.toDoubleOrNull()
                            if (n == null) {
                                notes.add("$key=$value 无法解析，已忽略")
                            } else {
                                val ok = if (key.endsWith("m")) actual >= n else actual <= n
                                if (!ok) return@filter false
                            }
                        }
                    }
                    "nbt" -> {
                        val javaOk = if (target != null) target.edition == VersionSpec.Edition.JAVA
                        else edition == "java"
                        if (!javaOk) {
                            notes.add("nbt= 仅 Java 版支持；目标版本不是 Java，**没有按它筛选**")
                        } else if (!matchNbt(e, value, notes)) {
                            return@filter false
                        }
                    }
                    "has_property" -> {
                        val ok = if (target != null) {
                            FeatureGate.isAvailable(FeatureGate.Feature.HAS_PROPERTY_PARAM, target)
                        } else {
                            edition == "bedrock" && (version == null || version >= 1_20_70L)
                        }
                        if (!ok) notes.add("has_property 仅基岩版 ≥1.20.70 支持，**没有按它筛选**")
                        else if (!matchHasProperty(e, value, notes)) return@filter false
                    }
                    "haspermission" -> {
                        val ok = if (target != null) {
                            FeatureGate.isAvailable(FeatureGate.Feature.HASPERMISSION_PARAM, target)
                        } else {
                            edition == "bedrock" && (version == null || version >= 1_19_80L)
                        }
                        if (!ok) notes.add("haspermission 仅基岩版 ≥1.19.80 支持，**没有按它筛选**")
                        else if (!matchPermission(e, value, notes)) return@filter false
                    }
                    "predicate" -> {
                        val javaOk = if (target != null) target.edition == VersionSpec.Edition.JAVA
                        else edition == "java"
                        if (!javaOk) notes.add("predicate 仅 Java 版支持，**没有按它筛选**")
                        else if (!matchPredicate(e, value)) return@filter false
                    }
                    "advancements" -> {
                        val javaOk = if (target != null) target.edition == VersionSpec.Edition.JAVA
                        else edition == "java"
                        if (!javaOk) notes.add("advancements 仅 Java 版支持，**没有按它筛选**")
                        else if (!matchAdvancements(e, value, notes)) return@filter false
                    }
                    else -> notes.add("参数 \"$key\" 未支持，已忽略")
                }
            }
            true
        }

        // 3) 位置/体积
        if (dx != null || dy != null || dz != null) {
            candidates = candidates.filter { e ->
                inBox(e, ox, oy, oz, dx ?: 0.0, dy ?: 0.0, dz ?: 0.0)
            }
        }
        if (maxDist != null || minDist != null) {
            val lo = minDist ?: 0.0
            val hi = maxDist ?: Double.MAX_VALUE
            candidates = candidates.filter { e ->
                val d = sqrt((e.x - ox) * (e.x - ox) + (e.y - oy) * (e.y - oy) + (e.z - oz) * (e.z - oz))
                d >= lo && d <= hi
            }
        }

        if (invalidLimit) return Result(emptyList(), notes.distinct())

        // 4) 排序 + 数量
        val sorted = when (sort ?: defaultSort(base)) {
            "nearest" -> candidates.sortedBy { dist2(it, ox, oy, oz) }
            "furthest" -> candidates.sortedByDescending { dist2(it, ox, oy, oz) }
            "random", "arbitrary" -> candidates
            else -> candidates
        }
        // @p/@r/@n 本身就是"一个目标"，没写 limit 时按 1 处理（@a/@e 不设默认上限）。
        val n = limit ?: when (base) {
            "@p", "@r", "@n" -> 1
            else -> null
        }
        val limited = if (n == null) sorted else sorted.take(n)
        return Result(limited, notes.distinct())
    }

    private fun defaultSort(base: String): String = when (base) {
        "@p", "@n" -> "nearest"
        "@r" -> "random"
        else -> "arbitrary"
    }

    private fun dist2(e: GridEntity, x: Double, y: Double, z: Double): Double =
        val0(e.x - x) + val0(e.y - y) + val0(e.z - z)

    private fun val0(v: Double) = v * v

    /**
     * dx/dy/dz 的区域：从 (x,y,z) 起、边长 |d|+1（wiki：未指定默认 0）。
     * 选取的是"判定箱与区域相交"的实体，而不是只有中心点在区域内的实体。
     * 判定箱按 [GridEntity.hitboxWidth]/[hitboxHeight] 近似（脚部在 y）。
     */
    private fun inBox(e: GridEntity, x: Double, y: Double, z: Double, dx: Double, dy: Double, dz: Double): Boolean {
        val x1 = minOf(x, x + dx); val x2 = maxOf(x, x + dx) + 1.0
        val y1 = minOf(y, y + dy); val y2 = maxOf(y, y + dy) + 1.0
        val z1 = minOf(z, z + dz); val z2 = maxOf(z, z + dz) + 1.0
        val ex1 = e.x - e.hitboxWidth / 2.0; val ex2 = e.x + e.hitboxWidth / 2.0
        val ey1 = e.y; val ey2 = e.y + e.hitboxHeight
        val ez1 = e.z - e.hitboxWidth / 2.0; val ez2 = e.z + e.hitboxWidth / 2.0
        return ex2 >= x1 && ex1 <= x2 && ey2 >= y1 && ey1 <= y2 && ez2 >= z1 && ez1 <= z2
    }

    private fun matchType(e: GridEntity, value: String, notes: MutableList<String>): Boolean {
        if (value.startsWith("#")) {
            notes.add("type=#标签 仅 Java 支持；网格未建模实体标签，已忽略该条件")
            return true
        }
        val neg = value.startsWith("!")
        val raw = if (neg) value.substring(1) else value
        val want = EntityIdAliases.canonical(raw)
        val hit = EntityIdAliases.canonical(e.id) == want
        return if (neg) !hit else hit
    }

    private fun matchName(e: GridEntity, value: String): Boolean {
        if (value == "!") return !e.name.isNullOrBlank()
        if (value.startsWith("!")) return e.name != value.substring(1)
        return e.name == value
    }

    private fun matchTag(e: GridEntity, value: String): Boolean = when {
        value == "!" -> e.tags.isNotEmpty()      // tag=! = 有任意标签
        value.isEmpty() -> e.tags.isEmpty()      // tag=  = 无标签
        value.startsWith("!") -> value.substring(1) !in e.tags
        else -> value in e.tags
    }

    private fun matchTeam(e: GridEntity, value: String): Boolean = when {
        value == "!" -> !e.team.isNullOrBlank()
        value.isEmpty() -> e.team.isNullOrBlank()
        else -> e.team == value
    }

    private fun matchFamily(e: GridEntity, value: String): Boolean {
        val neg = value.startsWith("!")
        val v = if (neg) value.substring(1) else value
        val hit = v in e.families
        return if (neg) !hit else hit
    }

    /** scores={a=1..5,b=!2}：支持区间；`=!` 的口径 = "存在且不在范围内"。 */
    private fun matchScores(e: GridEntity, value: String, notes: MutableList<String>): Boolean {
        val body = value.removeSurrounding("{", "}")
        for (entry in ExecCondSupport.splitTopLevel(body, ',')) {
            val i = ExecCondSupport.indexOfTopLevel(entry, '=')
            if (i < 0) continue
            val k = entry.substring(0, i).trim()
            val raw = entry.substring(i + 1).trim()
            val neg = raw.startsWith("!")
            val v = if (neg) raw.substring(1) else raw
            val score = e.scores[k]
            val hit = score != null && scoreInRange(score, v)
            if (neg) {
                if (score == null || hit) return false   // 必须存在且不在范围内
            } else {
                if (!hit) return false
            }
        }
        return true
    }

    /** 分数值：单值或 `a..b` 区间。 */
    private fun scoreInRange(score: Int, raw: String): Boolean {
        val v = raw.trim()
        if (".." in v) {
            val (lo, hi) = parseRange(v)
            return score.toDouble() >= (lo ?: Double.NEGATIVE_INFINITY) &&
                score.toDouble() <= (hi ?: Double.POSITIVE_INFINITY)
        }
        return score == v.toIntOrNull()
    }

    /** 游戏模式：支持 Java 全称、基岩缩写 s/c/a/d 与数字 0/1/2/5、以及 `!` 反选。 */
    private fun matchGamemode(e: GridEntity, raw: String, notes: MutableList<String>): Boolean {
        val neg = raw.startsWith("!")
        val value = (if (neg) raw.substring(1) else raw).lowercase()
        val want = when (value) {
            "s", "0", "survival" -> "survival"
            "c", "1", "creative" -> "creative"
            "a", "2", "adventure" -> "adventure"
            "d", "5", "spectator" -> "spectator"
            else -> {
                notes.add("gamemode=$raw 无法识别，已忽略")
                return true
            }
        }
        val actual = e.gamemode
        if (actual == null) {
            notes.add("gamemode= 只适用于玩家；非玩家自动排除")
            return false
        }
        val hit = actual.lowercase() == want
        return if (neg) !hit else hit
    }

    /**
     * nbt= 求值：把网格字段派生为 NBT（Tags/CustomName/Inventory/SelectedItem/equipment/playerGameType/XpLevel），
     * 再叠加用户手填的额外 SNBT；按"查询是实际值子集"匹配。
     * 缺失的键会明确提示（通常是实体没填对应字段）。
     */
    /** 供条件求值等外部调用：按网格实体的派生 NBT + 额外 SNBT 判断 nbt=。 */
    fun matchesNbt(e: GridEntity, raw: String, notes: MutableList<String> = mutableListOf()): Boolean =
        matchNbt(e, raw, notes)

    private fun matchNbt(e: GridEntity, raw: String, notes: MutableList<String>): Boolean {
        val query = SnbtLite.parseCompound(raw)
        if (query == null) {
            notes.add("nbt 结构无法解析，已忽略该条件：$raw")
            return true
        }
        val actual = effectiveNbt(e)
        val ok = SnbtLite.matches(SnbtLite.Value.Compound(query), SnbtLite.Value.Compound(actual))
        if (!ok) {
            val missing = query.keys.filter { it !in actual }
            if (missing.isNotEmpty()) {
                notes.add("nbt 条件里的 ${missing.joinToString("、")} 在该实体上未设置（可在实体的 NBT/相关字段里补充），按不匹配处理")
            }
        }
        return ok
    }

    /** 网格物品 -> NBT 物品堆栈（id/count + 可选的 damage 组件）。 */
    private fun itemNbt(item: GridItem): Map<String, SnbtLite.Value> {
        val m = linkedMapOf<String, SnbtLite.Value>(
            "id" to SnbtLite.Value.Str(item.id),
            "count" to SnbtLite.Value.Num(item.count.toDouble())
        )
        val comps = linkedMapOf<String, SnbtLite.Value>()
        if (item.damage != null && item.damage > 0) {
            comps["minecraft:damage"] = SnbtLite.Value.Num(item.damage.toDouble())
        }
        comps.putAll(item.components)
        if (comps.isNotEmpty()) m["components"] = SnbtLite.Value.Compound(comps)
        return m
    }

    private fun effectiveNbt(e: GridEntity): Map<String, SnbtLite.Value> {
        val m = linkedMapOf<String, SnbtLite.Value>()
        m["id"] = SnbtLite.Value.Str(e.id)
        if (e.tags.isNotEmpty()) {
            m["Tags"] = SnbtLite.Value.ListV(e.tags.map { SnbtLite.Value.Str(it) })
        }
        e.name?.takeIf { it.isNotBlank() }?.let { m["CustomName"] = SnbtLite.Value.Str(it) }
        e.team?.takeIf { it.isNotBlank() }?.let { m["Team"] = SnbtLite.Value.Str(it) }
        e.gamemode?.lowercase()?.let { g ->
            m["playerGameType"] = SnbtLite.Value.Num(
                when (g) {
                    "survival" -> 0.0
                    "creative" -> 1.0
                    "adventure" -> 2.0
                    "spectator" -> 3.0
                    else -> 0.0
                }
            )
        }
        e.level?.let { m["XpLevel"] = SnbtLite.Value.Num(it.toDouble()) }
        // 坐标 / 视角与 NBT 的 Pos / Rotation（Rotation 顺序是 [yaw, pitch]）统一
        m["Pos"] = SnbtLite.Value.ListV(
            listOf(
                SnbtLite.Value.Num(e.x),
                SnbtLite.Value.Num(e.y),
                SnbtLite.Value.Num(e.z)
            )
        )
        if (e.rotationX != null && e.rotationY != null) {
            m["Rotation"] = SnbtLite.Value.ListV(
                listOf(
                    SnbtLite.Value.Num(e.rotationY),
                    SnbtLite.Value.Num(e.rotationX)
                )
            )
        }

        val inv = mutableListOf<SnbtLite.Value>()
        for ((slot, item) in e.items) {
            val n = inventorySlotNumber(slot) ?: continue
            inv.add(
                SnbtLite.Value.Compound(
                    itemNbt(item) + mapOf("Slot" to SnbtLite.Value.Num(n.toDouble()))
                )
            )
        }
        if (inv.isNotEmpty()) m["Inventory"] = SnbtLite.Value.ListV(inv)

        e.items["weapon.mainhand"]?.let { item ->
            m["SelectedItem"] = SnbtLite.Value.Compound(itemNbt(item))
        }
        val eq = linkedMapOf<String, SnbtLite.Value>()
        for ((slot, item) in e.items) {
            val key = when (slot) {
                "weapon.mainhand" -> "mainhand"
                "weapon.offhand" -> "offhand"
                "armor.head" -> "head"
                "armor.chest" -> "chest"
                "armor.legs" -> "legs"
                "armor.feet" -> "feet"
                else -> null
            } ?: continue
            eq[key] = SnbtLite.Value.Compound(itemNbt(item))
        }
        if (eq.isNotEmpty()) m["equipment"] = SnbtLite.Value.Compound(eq)

        SnbtLite.parseCompoundBody(e.extraNbt)?.let { m.putAll(it) }
        return m
    }

    /** 槽位名 -> NBT 数字槽（装备沿用旧版 100..103 / -106；hotbar/inventory 用数字）。 */
    private fun inventorySlotNumber(slot: String): Int? = when (slot) {
        "armor.head" -> 103
        "armor.chest" -> 102
        "armor.legs" -> 101
        "armor.feet" -> 100
        "weapon.offhand" -> -106
        else -> when {
            slot.startsWith("hotbar.") || slot.startsWith("inventory.") ->
                slot.substringAfterLast('.').toIntOrNull()
            else -> null
        }
    }

    private fun matchHasProperty(e: GridEntity, raw: String, notes: MutableList<String>): Boolean {
        val q = SnbtLite.parseCompound(raw)
        if (q == null) {
            notes.add("has_property 结构无法解析，已忽略：$raw")
            return true
        }
        for ((k, v) in q) {
            if (k == "property") {
                val name = SnbtLite.plainText(
                    (v as? SnbtLite.Value.Str)?.v ?: v.toString()
                )
                val neg = name.startsWith("!")
                val actualName = if (neg) name.substring(1) else name
                val exists = e.properties.containsKey(actualName)
                if (neg == exists) return false
            } else {
                val actual = e.properties[k] ?: return false
                if (!propertyValueMatches(v, actual)) return false
            }
        }
        return true
    }

    private fun propertyValueMatches(query: SnbtLite.Value, actual: String): Boolean = when (query) {
        is SnbtLite.Value.Num -> {
            val a = actual.toDoubleOrNull()
            a != null && kotlin.math.abs(a - query.v) < 1e-9
        }
        is SnbtLite.Value.Bool -> actual.equals(query.v.toString(), true) ||
            (actual.toDoubleOrNull()?.let { (it != 0.0) == query.v } == true)
        is SnbtLite.Value.Str -> {
            val t = query.v
            if (".." in t) {
                val lo = t.substringBefore("..").toDoubleOrNull() ?: Double.NEGATIVE_INFINITY
                val hi = t.substringAfter("..").toDoubleOrNull() ?: Double.POSITIVE_INFINITY
                actual.toDoubleOrNull()?.let { it in lo..hi } == true
            } else {
                actual == t
            }
        }
        else -> false
    }

    private fun matchPermission(e: GridEntity, raw: String, notes: MutableList<String>): Boolean {
        val q = SnbtLite.parseCompound(raw)
        if (q == null) {
            notes.add("haspermission 结构无法解析，已忽略：$raw")
            return true
        }
        for ((k, v) in q) {
            val want = when (v) {
                is SnbtLite.Value.Str -> v.v
                is SnbtLite.Value.Bool -> if (v.v) "enabled" else "disabled"
                else -> notes.add("haspermission.$k 的值无法识别，已忽略").let { return true }
            }
            if (!e.permissions[k].equals(want, true)) return false
        }
        return true
    }

    private fun matchPredicate(e: GridEntity, raw: String): Boolean {
        val neg = raw.startsWith("!")
        val id = if (neg) raw.substring(1) else raw
        val hit = e.predicates.any { it == id || it == normId(id) }
        return if (neg) !hit else hit
    }

    private fun matchAdvancements(e: GridEntity, raw: String, notes: MutableList<String>): Boolean {
        val q = SnbtLite.parseCompound(raw)
        if (q == null) {
            notes.add("advancements 结构无法解析，已忽略：$raw")
            return true
        }
        for ((id, v) in q) {
            when (v) {
                is SnbtLite.Value.Bool -> {
                    val hit = e.advancements.contains(id)
                    if (hit != v.v) return false
                }
                is SnbtLite.Value.Compound -> {
                    for ((crit, cv) in v.map) {
                        val want = (cv as? SnbtLite.Value.Bool)?.v
                            ?: notes.add("advancements 准则 $id/$crit 的值不是布尔，已忽略").let { null }
                            ?: continue
                        val hit = e.advancements.contains("$id|$crit")
                        if (hit != want) return false
                    }
                }
                else -> notes.add("advancements.$id 的值无法识别，已忽略")
            }
        }
        return true
    }

    private fun normId(id: String): String =
        if (id.contains(':')) id else "minecraft:$id"

    /**
     * hasitem 求值（网格已建模的部分）：
     *  - 单个对象或对象数组（数组 = 同时满足/AND，与基岩一致）；
     *  - item / quantity（区间、!反选、0=没有）/ location + slot（数字、!数字、a..b）；
     *  - item=air 表示"该槽为空"；
     *  - data 按物品 damage 判定。
     */
    private fun matchHasitem(e: GridEntity, raw: String, notes: MutableList<String>): Boolean {
        val v = raw.trim()
        val objects = if (v.startsWith("[")) {
            ExecCondSupport.splitTopLevel(v.removeSurrounding("[", "]"), ',')
                .filter { it.trim().startsWith("{") }
        } else listOf(v)
        if (objects.isEmpty()) {
            notes.add("hasitem 结构无法解析：$raw")
            return true
        }
        return objects.all { obj ->
            matchHasitemObject(e, obj.trim().removeSurrounding("{", "}"), notes)
        }
    }

    private fun matchHasitemObject(e: GridEntity, body: String, notes: MutableList<String>): Boolean {
        var item: String? = null
        var rawQuantity: String? = null
        var location: String? = null
        var slot: String? = null
        var data: Int? = null
        for (part in ExecCondSupport.splitTopLevel(body, ',')) {
            val i = ExecCondSupport.indexOfTopLevel(part, '=')
            if (i < 0) continue
            val k = part.substring(0, i).trim()
            val value = part.substring(i + 1).trim().removeSurrounding("\"")
            when (k) {
                "item" -> item = value
                "quantity" -> rawQuantity = value
                "location" -> location = value
                "slot" -> slot = value
                "data" -> data = value.toIntOrNull()
            }
        }
        if (rawQuantity == null && data != null) {
            notes.add("hasitem 的 data= 按物品损耗值（damage）处理")
        }
        val want = item ?: run {
            notes.add("hasitem 缺少 item=（必填），已忽略该条件")
            return true
        }
        val loc = location?.removePrefix("slot.")
        fun slotMatches(key: String): Boolean {
            if (loc != null) {
                val locOk = key == loc || key.startsWith("$loc.") ||
                    (loc == "hotbar" && key == "hotbar")
                if (!locOk) return false
            }
            if (slot != null) {
                val idx = key.substringAfterLast('.', "")
                val idxNum = idx.toIntOrNull()
                if (slot.startsWith("!")) {
                    val bad = slot.substring(1)
                    val badNum = bad.toIntOrNull()
                    if (idx == bad || (badNum != null && idxNum == badNum)) return false
                } else if (".." in slot) {
                    if (idxNum == null) return false
                    val (lo, hi) = parseRange(slot)
                    if (lo != null && idxNum < lo) return false
                    if (hi != null && idxNum > hi) return false
                } else {
                    val wanted = slot.toIntOrNull()
                    if (wanted != null) {
                        if (idxNum != wanted) return false
                    } else if (idx != slot) {
                        return false
                    }
                }
            }
            return true
        }

        val isAir = ExecCondSupport.stripNs(want) == "air"
        if (isAir) {
            return e.items.none { (key, _) -> slotMatches(key) }
        }
        val wantId = if (want.contains(':')) want else "minecraft:$want"
        val qNeg = rawQuantity?.startsWith("!") == true
        val qText = if (qNeg) rawQuantity!!.substring(1) else rawQuantity
        // quantity=0 / 0.. / 0..0 的含义是"没有该物品"（与 execute 需求 A5/C18 一致）
        val meansNone = !qNeg && qText != null && qText in setOf("0", "0..", "0..0")
        if (meansNone) {
            return e.items.none { (key, stack) ->
                slotMatches(key) && stack.id == wantId &&
                    (data == null || (stack.damage ?: 0) == data)
            }
        }
        val (qLo, qHi) = parseRange(qText ?: "1..")
        val lo = qLo ?: 0.0
        val hi = qHi ?: Double.MAX_VALUE
        return e.items.any { (key, stack) ->
            if (!slotMatches(key) || stack.id != wantId) return@any false
            val qtyOk = if (qNeg) {
                // quantity=!N：存在且数量不等于 N
                qText?.toIntOrNull()?.let { stack.count != it } ?: true
            } else {
                stack.count.toDouble() >= lo && stack.count.toDouble() <= hi
            }
            qtyOk && (data == null || (stack.damage ?: 0) == data)
        }
    }

    /** 选择器坐标：支持 `~` / `~1.5`（相对执行/已设置的原点）。 */
    private fun coord(value: String, base: Double): Double {
        val v = value.trim()
        if (v == "~") return base
        if (v.startsWith("~")) return base + (v.substring(1).toDoubleOrNull() ?: 0.0)
        return v.toDoubleOrNull() ?: base
    }

    private fun parseRange(v: String): Pair<Double?, Double?> {
        val t = v.trim()
        return when {
            ".." in t -> {
                val a = t.substringBefore("..").toDoubleOrNull()
                val b = t.substringAfter("..").toDoubleOrNull()
                a to b
            }
            else -> {
                val n = t.toDoubleOrNull()
                n to n
            }
        }
    }
}
