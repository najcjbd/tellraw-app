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
    val families: Set<String> = emptySet()
) {
    val isPlayer: Boolean get() = id == "minecraft:player"
}

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
        origin: Triple<Double, Double, Double> = Triple(0.0, -60.0, 0.0)
    ): Result {
        val notes = mutableListOf<String>()
        val s = selector.trim()
        val base = s.substringBefore('[').trim()
        val paramsPart = if ('[' in s && s.endsWith("]")) s.substringAfter('[').dropLast(1) else ""
        val params = if (paramsPart.isBlank()) emptyList()
        else ExecCondSupport.splitTopLevel(paramsPart, ',').map { it.trim() }.filter { it.isNotEmpty() }

        // 1) 选择器本体：先确定候选集合
        var candidates: List<GridEntity> = when (base) {
            "@a", "@p", "@r" -> entities.filter { it.isPlayer }
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
        if (base in setOf("@a", "@p", "@r") && params.any { it.startsWith("type=") }) {
            notes.add("\"$base\" 上 type= 无效（该选择器本身就限定了玩家）")
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
        var cNegative = false

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
                        val need = if (edition == "bedrock" && (version == null || version >= 1_16_100L)) true else false
                        if (!need) {
                            notes.add("family= 仅基岩版 ≥1.16.100 支持；目标版本不满足，**没有按它筛选**（不能当作已匹配）")
                        } else if (!matchFamily(e, value)) return@filter false
                    }
                    "scores" -> if (!matchScores(e, value, notes)) return@filter false
                    "x" -> ox = value.toDoubleOrNull() ?: ox
                    "y" -> oy = value.toDoubleOrNull() ?: oy
                    "z" -> oz = value.toDoubleOrNull() ?: oz
                    "dx" -> dx = value.toDoubleOrNull()
                    "dy" -> dy = value.toDoubleOrNull()
                    "dz" -> dz = value.toDoubleOrNull()
                    "distance" -> { val (a, b) = parseRange(value); minDist = a; maxDist = b }
                    "r" -> maxDist = value.toDoubleOrNull()
                    "rm" -> minDist = value.toDoubleOrNull()
                    "limit" -> limit = value.toIntOrNull()
                    "sort" -> sort = value
                    "c" -> {
                        val n = value.toIntOrNull()
                        if (n != null && n < 0) { cNegative = true; limit = -n } else limit = n
                    }
                    // 网格未建模的维度：明确提示
                    "gamemode", "m", "level", "l", "lm",
                    "x_rotation", "rx", "rxm", "y_rotation", "ry", "rym",
                    "nbt", "hasitem", "has_property", "haspermission",
                    "predicate", "advancements" ->
                        notes.add("参数 \"$key\" 在网格里尚未建模，已忽略（请自行核对）")
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

        // 4) 排序 + 数量
        val sorted = when (sort ?: defaultSort(base)) {
            "nearest" -> candidates.sortedBy { dist2(it, ox, oy, oz) }
            "furthest" -> candidates.sortedByDescending { dist2(it, ox, oy, oz) }
            "random", "arbitrary" -> candidates
            else -> candidates
        }
        val n = limit
        val limited = when {
            n == null -> sorted
            cNegative -> sorted.takeLast(n)   // c=-N：取最远 N 个
            else -> sorted.take(n)
        }
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

    private fun inBox(e: GridEntity, x: Double, y: Double, z: Double, dx: Double, dy: Double, dz: Double): Boolean {
        val x1 = minOf(x, x + dx); val x2 = maxOf(x, x + dx)
        val y1 = minOf(y, y + dy); val y2 = maxOf(y, y + dy)
        val z1 = minOf(z, z + dz); val z2 = maxOf(z, z + dz)
        return e.x in x1..x2 && e.y in y1..y2 && e.z in z1..z2
    }

    private fun matchType(e: GridEntity, value: String, notes: MutableList<String>): Boolean {
        if (value.startsWith("#")) {
            notes.add("type=#标签 仅 Java 支持；网格未建模实体标签，已忽略该条件")
            return true
        }
        val neg = value.startsWith("!")
        val want = (if (neg) value.substring(1) else value).let { if (it.contains(':')) it else "minecraft:$it" }
        val hit = e.id == want
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

    /** scores={a=1,b=!2}：`=!` 的口径 = "存在且不等于"。 */
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
            val num = v.toIntOrNull()
            val hit = if (num != null) score == num else score?.toString() == v
            if (neg) {
                if (score == null || hit) return false   // 必须存在且不等
            } else {
                if (!hit) return false
            }
        }
        return true
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
