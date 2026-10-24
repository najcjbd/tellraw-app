package com.tellraw.app.model

/**
 * 网格里的 execute 条件求值（近似）：把 `if/unless` 条件作用到**候选实体**上，
 * 用于回答"哪些实体会通过这条带条件的命令"。
 *
 * 支持：
 *  - if/unless entity <选择器>      （候选实体是否被该选择器选中；@s = 候选）
 *  - if/unless data entity <目标> <NBT>（目标为 @s 时直接查候选；其他目标按候选近似）
 *  - if/unless predicate <ID>       （Java 谓词）
 *  - if/unless items entity …       （未建模，提示）
 *  - 其他条件（block/biome/dimension/function/score…）提示未建模
 */
object GridConditionEvaluator {

    data class Result(val ok: Boolean, val notes: List<String>)

    /** 网格的世界状态（用于 block/biome/dimension/function/stopwatch 等条件）。 */
    data class GridWorld(
        val dimension: String = "minecraft:overworld",
        val biome: String = "",
        val loaded: Boolean = true,
        val blocks: Map<String, String> = emptyMap(),   // "x,y,z" -> block id
        val functions: Set<String> = emptySet(),
        val stopwatches: Map<String, Double> = emptyMap()
    )

    fun evaluate(
        conditionText: String,
        entity: GridEntity,
        all: List<GridEntity>,
        target: VersionSpec? = null,
        world: GridWorld = GridWorld(),
        origin: Triple<Double, Double, Double> = Triple(0.0, -60.0, 0.0)
    ): Result {
        val notes = mutableListOf<String>()
        val tokens = tokenize(conditionText)
        if (tokens.isEmpty()) return Result(true, notes)
        var i = 0
        while (i < tokens.size) {
            val t = tokens[i]
            if (t != "if" && t != "unless") {
                i++
                continue
            }
            val neg = t == "unless"
            val name = tokens.getOrNull(i + 1)
            if (name == null) {
                notes.add("条件 \"$t\" 后面没有内容，已忽略")
                break
            }
            var ok: Boolean
            var consumed = 2
            when (name.lowercase()) {
                "entity" -> {
                    val sel = tokens.getOrNull(i + 2)
                    if (sel == null) {
                        notes.add("if/unless entity 缺少选择器，已忽略")
                        ok = true
                    } else {
                        val r = SelectorEvaluator.evaluate(
                            sel, all, executor = entity,
                            edition = if (target?.edition == VersionSpec.Edition.BEDROCK) "bedrock" else "java",
                            origin = Triple(entity.x, entity.y, entity.z),
                            target = target
                        )
                        notes.addAll(r.notes)
                        ok = r.matched.any { it.uid == entity.uid }
                        consumed = 3
                    }
                }
                "data" -> {
                    val source = tokens.getOrNull(i + 2)
                    val tgt = tokens.getOrNull(i + 3)
                    val nbt = tokens.getOrNull(i + 4)
                    if (source != "entity" || tgt == null || nbt == null) {
                        notes.add("只建模了 if/unless data entity <@s> <NBT>；该条件未参与筛选")
                        ok = true
                    } else {
                        if (tgt != "@s" && tgt != "@p" && tgt != "@n") {
                            notes.add("if/unless data 的目标是 $tgt，网格按候选实体近似判断")
                        }
                        ok = SelectorEvaluator.matchesNbt(entity, nbt, notes)
                        consumed = 5
                    }
                }
                "predicate" -> {
                    val id = tokens.getOrNull(i + 2)
                    ok = if (id == null) true else entity.predicates.any { it == id }
                    if (id != null) consumed = 3
                }
                "score" -> {
                    val tgt = tokens.getOrNull(i + 2)
                    val obj = tokens.getOrNull(i + 3)
                    val kw2 = tokens.getOrNull(i + 4)
                    if (tgt == null || obj == null) {
                        notes.add("score 条件参数不完整，已忽略")
                        ok = true
                    } else if (kw2 == "matches") {
                        val range = tokens.getOrNull(i + 5)
                        val (lo, hi) = parseRange(range ?: "")
                        val v = entity.scores[obj]
                        if (tgt == "@s" && v != null) {
                            ok = v.toDouble() >= (lo ?: Double.NEGATIVE_INFINITY) &&
                                v.toDouble() <= (hi ?: Double.POSITIVE_INFINITY)
                            consumed = 6
                        } else {
                            notes.add("score 条件的目标不是 @s 或实体没有该分数，按候选近似未筛选")
                            ok = true
                            consumed = 6
                        }
                    } else {
                        notes.add("score 的比较形式需要源目标分数，网格未建模，未参与筛选")
                        ok = true
                        consumed = 6
                    }
                }
                "block", "blocks" -> {
                    if (target != null && target.edition == VersionSpec.Edition.BEDROCK) {
                        notes.add("基岩没有 $name 条件；未参与筛选")
                        ok = true
                        consumed = conditionSpan(tokens, i + 1)
                    } else if (world.blocks.isEmpty()) {
                        notes.add("未提供方块数据，$name 条件未参与筛选（可在“世界条件”里填写）")
                        ok = true
                        consumed = conditionSpan(tokens, i + 1)
                    } else if (name == "block") {
                        val p = readPos(tokens, i + 2, origin, notes)
                        val want = tokens.getOrNull(i + 5)
                        if (p == null || want == null) {
                            notes.add("block 条件参数不完整，已忽略")
                            ok = true
                        } else {
                            val actual = world.blocks[posKey(p)]
                            ok = actual != null && normBlock(actual) == normBlock(want)
                            consumed = 6
                        }
                    } else {
                        val a = readPos(tokens, i + 2, origin, notes)
                        val b = readPos(tokens, i + 5, origin, notes)
                        val dest = readPos(tokens, i + 8, origin, notes)
                        val mode = tokens.getOrNull(i + 11)
                        if (a == null || b == null || dest == null || mode == null) {
                            notes.add("blocks 条件参数不完整，已忽略")
                            ok = true
                        } else {
                            ok = compareRegions(world, a, b, dest, mode)
                            consumed = 12
                        }
                    }
                }
                "biome" -> {
                    if (target != null && target.edition == VersionSpec.Edition.BEDROCK) {
                        notes.add("基岩没有 biome 条件；未参与筛选")
                        ok = true
                    } else if (world.biome.isBlank()) {
                        notes.add("未提供生物群系，biome 条件未参与筛选")
                        ok = true
                    } else {
                        ok = normBlock(world.biome) == normBlock(tokens.getOrNull(i + 5) ?: "")
                    }
                    consumed = conditionSpan(tokens, i + 1)
                }
                "dimension" -> {
                    if (target != null && target.edition == VersionSpec.Edition.BEDROCK) {
                        notes.add("基岩没有 dimension 条件；未参与筛选")
                        ok = true
                    } else {
                        ok = normBlock(world.dimension) == normBlock(tokens.getOrNull(i + 2) ?: "")
                    }
                    consumed = 3
                }
                "loaded" -> {
                    if (target != null && target.edition == VersionSpec.Edition.BEDROCK) {
                        notes.add("基岩没有 loaded 条件；未参与筛选")
                        ok = true
                    } else {
                        ok = world.loaded
                    }
                    consumed = conditionSpan(tokens, i + 1)
                }
                "function" -> {
                    if (target != null && target.edition == VersionSpec.Edition.BEDROCK) {
                        notes.add("基岩没有 function 条件；未参与筛选")
                        ok = true
                    } else {
                        ok = world.functions.contains(tokens.getOrNull(i + 2))
                    }
                    consumed = 3
                }
                "stopwatch" -> {
                    if (target != null && target.edition == VersionSpec.Edition.BEDROCK) {
                        notes.add("基岩没有 stopwatch 条件；未参与筛选")
                        ok = true
                    } else {
                        val id = tokens.getOrNull(i + 2)
                        val (lo, hi) = parseRange(tokens.getOrNull(i + 3) ?: "")
                        val v = id?.let { world.stopwatches[it] }
                        if (v == null) {
                            notes.add("stopwatch $id 未在世界条件里设置，未参与筛选")
                            ok = true
                        } else {
                            ok = v >= (lo ?: Double.NEGATIVE_INFINITY) && v <= (hi ?: Double.POSITIVE_INFINITY)
                        }
                    }
                    consumed = 4
                }
                "items", "slots" -> {
                    notes.add("条件 \"$name\" 在网格里未建模，未参与筛选")
                    ok = true
                    consumed = conditionSpan(tokens, i + 1)
                }
                else -> {
                    notes.add("条件 \"$name\" 未支持，未参与筛选")
                    ok = true
                }
            }
            if (neg) ok = !ok
            if (!ok) return Result(false, notes.distinct())
            i += consumed
        }
        return Result(true, notes.distinct())
    }

    /** 跳过一条未建模条件，避免把它的参数当成新的 if/unless。 */
    private fun conditionSpan(tokens: List<String>, start: Int): Int {
        var j = start + 1
        while (j < tokens.size && tokens[j] != "if" && tokens[j] != "unless") j++
        return j - start
    }

    data class OriginResult(val origin: Triple<Double, Double, Double>, val notes: List<String>)

    /**
     * 解析 execute 前缀里的 positioned / at，得到"选择器外部"的默认原点。
     * 选择器自己写的 x/y/z 优先级更高（在 SelectorEvaluator 内部覆盖）。
     */
    fun resolveOrigin(
        prefix: String,
        all: List<GridEntity>,
        executor: GridEntity?,
        base: Triple<Double, Double, Double>,
        target: VersionSpec? = null
    ): OriginResult {
        val notes = mutableListOf<String>()
        var origin = base
        val tokens = tokenize(prefix)
        var i = 0
        while (i < tokens.size) {
            when (tokens[i].lowercase()) {
                "at" -> {
                    val sel = tokens.getOrNull(i + 1)
                    if (sel == null) {
                        notes.add("at 缺少选择器，已忽略")
                    } else {
                        val r = SelectorEvaluator.evaluate(
                            sel, all, executor = executor,
                            edition = if (target?.edition == VersionSpec.Edition.BEDROCK) "bedrock" else "java",
                            origin = origin, target = target
                        )
                        notes.addAll(r.notes)
                        val first = r.matched.firstOrNull()
                        if (first != null) {
                            origin = Triple(first.x, first.y, first.z)
                        } else {
                            notes.add("at $sel 没有选中实体，原点保持不变")
                        }
                    }
                    i += 2
                }
                "positioned" -> {
                    val xs = tokens.getOrNull(i + 1)
                    val ys = tokens.getOrNull(i + 2)
                    val zs = tokens.getOrNull(i + 3)
                    if (xs == null || ys == null || zs == null) {
                        notes.add("positioned 参数不完整，已忽略")
                        i += 1
                    } else {
                        origin = Triple(
                            coord(xs, origin.first, notes),
                            coord(ys, origin.second, notes),
                            coord(zs, origin.third, notes)
                        )
                        i += 4
                    }
                }
                "in" -> {
                    notes.add("execute in（维度）未建模，原点按当前世界处理")
                    i += 2
                }
                "anchored" -> {
                    val a = tokens.getOrNull(i + 1)
                    if (a == "eyes") {
                        notes.add("anchored eyes：按脚部 +1.62 近似眼睛高度")
                        origin = origin.copy(second = origin.second + 1.62)
                    }
                    i += 2
                }
                "rotated", "facing", "align", "on" -> {
                    notes.add("execute ${tokens[i]} 未建模（不改变选择器筛选结果）")
                    i += 2
                }
                else -> i++
            }
        }
        return OriginResult(origin, notes.distinct())
    }

    private fun coord(text: String, base: Double, notes: MutableList<String>): Double {
        val t = text.trim()
        if (t.startsWith("^")) {
            notes.add("局部坐标 ^ 未建模，按波浪号相对坐标处理")
            return base
        }
        if (t == "~") return base
        if (t.startsWith("~")) return base + (t.substring(1).toDoubleOrNull() ?: 0.0)
        return t.toDoubleOrNull() ?: base
    }

    private fun parseRange(v: String): Pair<Double?, Double?> {
        val t = v.trim()
        return if (".." in t) {
            t.substringBefore("..").toDoubleOrNull() to t.substringAfter("..").toDoubleOrNull()
        } else {
            val n = t.toDoubleOrNull()
            n to n
        }
    }

    private fun normBlock(s: String): String =
        if (s.contains(':')) s.lowercase() else "minecraft:${s.lowercase()}"

    private fun posKey(p: Triple<Int, Int, Int>): String = "${p.first},${p.second},${p.third}"

    private fun readPos(
        tokens: List<String>,
        start: Int,
        origin: Triple<Double, Double, Double>,
        notes: MutableList<String>
    ): Triple<Int, Int, Int>? {
        val x = tokens.getOrNull(start) ?: return null
        val y = tokens.getOrNull(start + 1) ?: return null
        val z = tokens.getOrNull(start + 2) ?: return null
        fun c(v: String, base: Double): Int {
            val t = v.trim()
            return when {
                t == "~" -> base.toInt()
                t.startsWith("~") -> (base + (t.substring(1).toDoubleOrNull() ?: 0.0)).toInt()
                else -> t.toDoubleOrNull()?.toInt() ?: base.toInt()
            }
        }
        return Triple(c(x, origin.first), c(y, origin.second), c(z, origin.third))
    }

    private fun compareRegions(
        world: GridWorld,
        a: Triple<Int, Int, Int>,
        b: Triple<Int, Int, Int>,
        dest: Triple<Int, Int, Int>,
        mode: String
    ): Boolean {
        val x1 = minOf(a.first, b.first); val x2 = maxOf(a.first, b.first)
        val y1 = minOf(a.second, b.second); val y2 = maxOf(a.second, b.second)
        val z1 = minOf(a.third, b.third); val z2 = maxOf(a.third, b.third)
        for (x in x1..x2) for (y in y1..y2) for (z in z1..z2) {
            val src = world.blocks["$x,$y,$z"] ?: return false
            val dx = dest.first + (x - x1)
            val dy = dest.second + (y - y1)
            val dz = dest.third + (z - z1)
            val dst = world.blocks["$dx,$dy,$dz"] ?: return false
            if (normBlock(src) != normBlock(dst)) return false
        }
        return true
    }

    fun tokenize(s: String): List<String> {
        val out = mutableListOf<String>()
        val sb = StringBuilder()
        var brace = 0
        var quote: Char? = null
        for (c in s) {
            when {
                quote != null -> {
                    sb.append(c)
                    if (c == quote) quote = null
                }
                c == '"' || c == '\'' -> {
                    quote = c
                    sb.append(c)
                }
                c == '{' || c == '[' -> {
                    brace++
                    sb.append(c)
                }
                c == '}' || c == ']' -> {
                    brace--
                    sb.append(c)
                }
                c.isWhitespace() && brace == 0 -> {
                    if (sb.isNotEmpty()) {
                        out.add(sb.toString())
                        sb.clear()
                    }
                }
                else -> sb.append(c)
            }
        }
        if (sb.isNotEmpty()) out.add(sb.toString())
        return out
    }
}
