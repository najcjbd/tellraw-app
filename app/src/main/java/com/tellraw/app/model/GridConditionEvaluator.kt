package com.tellraw.app.model

import com.tellraw.app.util.ExecCondSupport

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

    /** 方块表里的一格：方块 ID + 可选的方块容器物品（槽位号 -> 物品）。 */
    data class GridBlock(
        val id: String,
        val items: Map<Int, GridItem> = emptyMap()
    )

    /** 网格的世界状态（用于 block/biome/dimension/function/stopwatch/items/slots 等条件）。 */
    data class GridWorld(
        val dimension: String = "minecraft:overworld",
        val biome: String = "",
        val loaded: Boolean = true,
        val blocks: Map<String, GridBlock> = emptyMap(),   // "x,y,z" -> 方块
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
            var neutral = false
            var consumed = 2
            when (name.lowercase()) {
                "entity" -> {
                    val sel = tokens.getOrNull(i + 2)
                    if (sel == null) {
                        notes.add("if/unless entity 缺少选择器，已忽略")
                        ok = true
                        neutral = true
                    } else {
                        val r = SelectorEvaluator.evaluate(
                            sel, all, executor = entity,
                            edition = if (target?.edition == VersionSpec.Edition.BEDROCK) "bedrock" else "java",
                            // 距离/体积应按执行原点算，不能用候选实体自己的坐标
                            origin = origin,
                            target = target
                        )
                        notes.addAll(r.notes)
                        ok = r.matched.any { it.uid == entity.uid }
                        // 选择器里有“未参与筛选”的参数时，unless 也不能反转
                        neutral = r.neutral
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
                        neutral = true
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
                    if (id == null) {
                        notes.add("predicate 条件缺少 ID，已忽略")
                        ok = true
                        neutral = true
                    } else {
                        ok = entity.predicates.any { it == id }
                        consumed = 3
                    }
                }
                "score" -> {
                    val tgt = tokens.getOrNull(i + 2)
                    val obj = tokens.getOrNull(i + 3)
                    val kw2 = tokens.getOrNull(i + 4)
                    if (tgt == null || obj == null) {
                        notes.add("score 条件参数不完整，已忽略")
                        ok = true
                        neutral = true
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
                            neutral = true
                            consumed = 6
                        }
                    } else {
                        notes.add("score 的比较形式需要源目标分数，网格未建模，未参与筛选")
                        ok = true
                        neutral = true
                        consumed = 6
                    }
                }
                "block", "blocks" -> {
                    if (target != null && target.edition == VersionSpec.Edition.BEDROCK) {
                        notes.add("基岩没有 $name 条件；未参与筛选")
                        ok = true
                        neutral = true
                        consumed = conditionSpan(tokens, i + 1)
                    } else if (world.blocks.isEmpty()) {
                        notes.add("未提供方块数据，$name 条件未参与筛选（可在“世界条件”里填写）")
                        ok = true
                        neutral = true
                        consumed = conditionSpan(tokens, i + 1)
                    } else if (name == "block") {
                        val p = readPos(tokens, i + 2, origin, notes)
                        val want = tokens.getOrNull(i + 5)
                        if (p == null || want == null) {
                            notes.add("block 条件参数不完整，已忽略")
                            ok = true
                            neutral = true
                        } else {
                            val actual = world.blocks[posKey(p)]
                            ok = actual != null && normBlock(actual.id) == normBlock(want)
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
                            neutral = true
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
                        neutral = true
                    } else if (world.biome.isBlank()) {
                        notes.add("未提供生物群系，biome 条件未参与筛选")
                        ok = true
                        neutral = true
                    } else {
                        ok = normBlock(world.biome) == normBlock(tokens.getOrNull(i + 5) ?: "")
                    }
                    consumed = conditionSpan(tokens, i + 1)
                }
                "dimension" -> {
                    if (target != null && target.edition == VersionSpec.Edition.BEDROCK) {
                        notes.add("基岩没有 dimension 条件；未参与筛选")
                        ok = true
                        neutral = true
                    } else {
                        ok = normBlock(world.dimension) == normBlock(tokens.getOrNull(i + 2) ?: "")
                    }
                    consumed = 3
                }
                "loaded" -> {
                    if (target != null && target.edition == VersionSpec.Edition.BEDROCK) {
                        notes.add("基岩没有 loaded 条件；未参与筛选")
                        ok = true
                        neutral = true
                    } else {
                        ok = world.loaded
                    }
                    consumed = conditionSpan(tokens, i + 1)
                }
                "function" -> {
                    if (target != null && target.edition == VersionSpec.Edition.BEDROCK) {
                        notes.add("基岩没有 function 条件；未参与筛选")
                        ok = true
                        neutral = true
                    } else {
                        ok = world.functions.contains(tokens.getOrNull(i + 2))
                    }
                    consumed = 3
                }
                "stopwatch" -> {
                    if (target != null && target.edition == VersionSpec.Edition.BEDROCK) {
                        notes.add("基岩没有 stopwatch 条件；未参与筛选")
                        ok = true
                        neutral = true
                    } else {
                        val id = tokens.getOrNull(i + 2)
                        val (lo, hi) = parseRange(tokens.getOrNull(i + 3) ?: "")
                        val v = id?.let { world.stopwatches[it] }
                        if (v == null) {
                            notes.add("stopwatch $id 未在世界条件里设置，未参与筛选")
                            ok = true
                            neutral = true
                        } else {
                            ok = v >= (lo ?: Double.NEGATIVE_INFINITY) && v <= (hi ?: Double.POSITIVE_INFINITY)
                        }
                    }
                    consumed = 4
                }
                "items" -> {
                    val javaOk = target?.let {
                        it.edition == VersionSpec.Edition.JAVA && it.atLeast(1, 20, 5)
                    } ?: true
                    if (!javaOk) {
                        notes.add("items 条件需要 Java ≥1.20.5 且基岩没有该条件；未参与筛选")
                        ok = true
                        neutral = true
                        consumed = conditionSpan(tokens, i + 1)
                    } else {
                        val r = evalItems(tokens, i, entity, world, origin, notes)
                        if (r == null) {
                            notes.add("items 条件参数不完整，已忽略")
                            ok = true
                            neutral = true
                            consumed = conditionSpan(tokens, i + 1)
                        } else {
                            ok = r.ok
                            consumed = r.consumed
                            neutral = r.neutral
                        }
                    }
                }
                "slots" -> {
                    val javaOk = target?.let {
                        it.edition == VersionSpec.Edition.JAVA && it.atLeast(26, 3)
                    } ?: true
                    if (!javaOk) {
                        notes.add("slots 条件需要 Java 26.3 且基岩没有该条件；未参与筛选")
                        ok = true
                        neutral = true
                        consumed = conditionSpan(tokens, i + 1)
                    } else {
                        val r = evalSlots(tokens, i, entity, world, origin, notes)
                        if (r == null) {
                            notes.add("slots 条件参数不完整，已忽略")
                            ok = true
                            neutral = true
                            consumed = conditionSpan(tokens, i + 1)
                        } else {
                            ok = r.ok
                            consumed = r.consumed
                            neutral = r.neutral
                        }
                    }
                }
                else -> {
                    notes.add("条件 \"$name\" 未支持，未参与筛选")
                    ok = true
                    neutral = true
                }
            }
            if (!neutral) {
                if (neg) ok = !ok
                if (!ok) return Result(false, notes.distinct())
            }
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

    /** 条件求值结果；neutral = 数据/版本不足，未参与筛选（unless 也不能反转它）。 */
    private data class CondEval(val ok: Boolean, val consumed: Int, val neutral: Boolean = false)

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
                    val first = tokens.getOrNull(i + 1)
                    when (first) {
                        "as" -> {
                            val sel = tokens.getOrNull(i + 2)
                            if (sel == null) {
                                notes.add("positioned as 缺少选择器，已忽略")
                                i += 1
                            } else {
                                val r = SelectorEvaluator.evaluate(
                                    sel, all, executor = executor,
                                    edition = if (target?.edition == VersionSpec.Edition.BEDROCK) "bedrock" else "java",
                                    origin = origin, target = target
                                )
                                notes.addAll(r.notes)
                                val firstEntity = r.matched.firstOrNull()
                                if (firstEntity != null) {
                                    origin = Triple(firstEntity.x, firstEntity.y, firstEntity.z)
                                } else {
                                    notes.add("positioned as $sel 没有选中实体，原点保持不变")
                                }
                                i += 3
                            }
                        }
                        "over" -> {
                            notes.add("execute positioned over（高度图）未建模，原点保持不变")
                            i += 3
                        }
                        else -> {
                            val xs = first
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

    // ---------- items / slots ----------

    /** 返回 (条件是否成立, 消耗的 token 数)。 */
    private fun evalItems(
        tokens: List<String>,
        i: Int,
        entity: GridEntity,
        world: GridWorld,
        origin: Triple<Double, Double, Double>,
        notes: MutableList<String>
    ): CondEval? {
        val source = tokens.getOrNull(i + 2) ?: return null
        if (source == "entity") {
            val target = tokens.getOrNull(i + 3) ?: return null
            val slots = tokens.getOrNull(i + 4) ?: return null
            val predicate = tokens.getOrNull(i + 5) ?: return null
            val ok = if (target == "@s" || target.startsWith("@s")) {
                matchesItemPredicate(entity.items, slots, predicate, notes)
            } else {
                notes.add("items 条件的目标不是 @s（$target），网格按候选实体近似")
                matchesItemPredicate(entity.items, slots, predicate, notes)
            }
            return CondEval(ok, 6)
        }
        if (source == "block") {
            val pos = readPos(tokens, i + 3, origin, notes) ?: return null
            val slots = tokens.getOrNull(i + 6) ?: return null
            val predicate = tokens.getOrNull(i + 7) ?: return null
            val block = world.blocks[posKey(pos)]
            if (block == null) {
                notes.add("方块表里没有 ${posKey(pos)} 的容器数据，items 条件未参与筛选")
                return CondEval(true, 8, neutral = true)
            }
            return CondEval(matchesItemPredicate(blockItems(block), slots, predicate, notes), 8)
        }
        return null
    }

    private fun evalSlots(
        tokens: List<String>,
        i: Int,
        entity: GridEntity,
        world: GridWorld,
        origin: Triple<Double, Double, Double>,
        notes: MutableList<String>
    ): CondEval? {
        val source = tokens.getOrNull(i + 2) ?: return null
        if (source == "entity") {
            val target = tokens.getOrNull(i + 3) ?: return null
            val slots = tokens.getOrNull(i + 4) ?: return null
            val ok = if (target == "@s" || target.startsWith("@s")) {
                entityHasSlots(entity, slots)
            } else {
                notes.add("slots 条件的目标不是 @s（$target），网格按候选实体近似")
                entityHasSlots(entity, slots)
            }
            return CondEval(ok, 5)
        }
        if (source == "block") {
            val pos = readPos(tokens, i + 3, origin, notes) ?: return null
            val slots = tokens.getOrNull(i + 6) ?: return null
            val block = world.blocks[posKey(pos)]
            if (block == null) {
                notes.add("方块表里没有 ${posKey(pos)} 的容器数据，slots 条件未参与筛选")
                return CondEval(true, 7, neutral = true)
            }
            return CondEval(blockHasSlots(block, slots), 7)
        }
        return null
    }

    private fun blockItems(block: GridBlock): Map<String, GridItem> =
        block.items.mapKeys { "container.${it.key}" }

    private fun slotMatches(source: String, key: String): Boolean {
        val src = source.removePrefix("slot.")
        val k = key.removePrefix("slot.")
        if (src == "*") return true
        if (src.endsWith(".*")) return k.startsWith(src.dropLast(1)) || k == src.dropLast(2)
        // 槽位名不带编号时，视为该类的任意槽
        if (src.count { it == '.' } == 0) return k == src || k.startsWith("$src.")
        return k == src
    }

    private fun matchesItemPredicate(
        items: Map<String, GridItem>,
        slots: String,
        predicate: String,
        notes: MutableList<String>
    ): Boolean {
        val candidates = items.filterKeys { slotMatches(slots, it) }.values
        val p = predicate.trim()
        if (p.isEmpty()) return candidates.any { true }

        val bracketIndex = p.indexOf('[')
        val idPart = (if (bracketIndex >= 0) p.substring(0, bracketIndex) else p).trim()
        val bracket = if (bracketIndex >= 0 && p.endsWith("]")) {
            p.substring(bracketIndex + 1, p.length - 1)
        } else ""

        val idNeg = idPart.startsWith("!")
        val idRaw = if (idNeg) idPart.substring(1) else idPart
        val wantId = if (idRaw == "*" || idRaw.isEmpty()) null else normBlock(idRaw)

        // 测试项：`,` 是 AND（外层），`|` 是 OR（内层，优先级高）
        val groups = if (bracket.isBlank()) emptyList()
        else ExecCondSupport.splitTopLevel(bracket, ',')
        val parsedGroups = groups.map { g ->
            ExecCondSupport.splitTopLevel(g, '|').map { parseItemTerm(it.trim()) }
        }

        fun matchesTerm(item: GridItem, term: ItemTerm): Boolean {
            val hit = when (term.kind) {
                ItemTerm.Kind.COUNT -> {
                    val c = item.count.toDouble()
                    when {
                        term.op == null -> true
                        term.range == null -> false
                        else -> c >= (term.range.first ?: Double.NEGATIVE_INFINITY) &&
                            c <= (term.range.second ?: Double.POSITIVE_INFINITY)
                    }
                }
                ItemTerm.Kind.COMPONENT -> {
                    val key = normalizeComponent(term.key)
                    val actual: SnbtLite.Value? = when (key) {
                        "damage", "minecraft:damage" ->
                            item.damage?.let { SnbtLite.Value.Num(it.toDouble()) }
                                ?: item.components["minecraft:damage"]
                                ?: item.components["damage"]
                        else -> item.components[key] ?: item.components[term.key]
                    }
                    when {
                        term.op == null -> actual != null
                        actual == null -> false
                        term.op == "=" -> term.value != null &&
                            SnbtLite.serialize(term.value) == SnbtLite.serialize(actual)
                        term.op == "~" -> term.value != null && SnbtLite.matches(term.value, actual)
                        else -> false
                    }
                }
            }
            return if (term.neg) !hit else hit
        }

        // 同一件物品必须同时满足所有 AND 组；组内任一 OR 项即可。
        val hit = candidates.any { item ->
            (wantId == null || normBlock(item.id) == wantId) &&
                parsedGroups.all { orGroup ->
                    orGroup.isEmpty() || orGroup.any { term -> matchesTerm(item, term) }
                }
        }
        return if (idNeg) !hit else hit
    }

    private data class ItemTerm(
        val kind: Kind,
        val key: String = "",
        val op: String? = null,
        val range: Pair<Double?, Double?>? = null,
        val value: SnbtLite.Value? = null,
        val neg: Boolean = false
    ) {
        enum class Kind { COUNT, COMPONENT }
    }

    private fun parseItemTerm(raw: String): ItemTerm {
        val neg = raw.startsWith("!")
        val body = if (neg) raw.substring(1) else raw
        if (body == "count" || body.startsWith("count=") || body.startsWith("count~")) {
            val op = when {
                body.startsWith("count=") -> "="
                body.startsWith("count~") -> "~"
                else -> null
            }
            val valText = if (op != null) body.substringAfter(op) else ""
            val range = when {
                valText.isBlank() -> null
                valText.startsWith("{") -> {
                    val map = SnbtLite.parseCompoundBody(valText)
                    if (map == null) null else {
                        (map["min"] as? SnbtLite.Value.Num)?.v to
                            (map["max"] as? SnbtLite.Value.Num)?.v
                    }
                }
                ".." in valText -> valText.substringBefore("..").toDoubleOrNull() to
                    valText.substringAfter("..").toDoubleOrNull()
                else -> {
                    val n = valText.toDoubleOrNull()
                    n to n
                }
            }
            return ItemTerm(ItemTerm.Kind.COUNT, op = op, range = range, neg = neg)
        }
        val eq = ExecCondSupport.indexOfTopLevel(body, '=')
        val tilde = ExecCondSupport.indexOfTopLevel(body, '~')
        val opIndex = when {
            eq >= 0 -> eq
            tilde >= 0 -> tilde
            else -> -1
        }
        val key = if (opIndex >= 0) body.substring(0, opIndex).trim() else body.trim()
        val op = when {
            opIndex < 0 -> null
            body[opIndex] == '=' -> "="
            else -> "~"
        }
        val value = if (opIndex >= 0) SnbtLite.parseValue(body.substring(opIndex + 1).trim()) else null
        return ItemTerm(ItemTerm.Kind.COMPONENT, key = key, op = op, value = value, neg = neg)
    }

    private fun normalizeComponent(key: String): String {
        val k = key.trim()
        return when {
            k == "damage" -> "damage"
            k.startsWith("minecraft:") -> k
            else -> "minecraft:$k"
        }
    }

    private fun entityHasSlots(e: GridEntity, slots: String): Boolean {
        val known = mutableSetOf<String>()
        when (EntityRegistry.categoryOf(e.id)) {
            EntityRegistry.Category.PLAYER -> {
                known += listOf("inventory", "hotbar", "armor", "weapon", "enderchest")
                for (i in 0..8) known += "hotbar.$i"
                for (i in 0..26) known += "inventory.$i"
                known += listOf("armor.head", "armor.chest", "armor.legs", "armor.feet")
                known += listOf("weapon.mainhand", "weapon.offhand")
            }
            EntityRegistry.Category.HOSTILE, EntityRegistry.Category.PASSIVE -> {
                known += listOf("armor", "weapon")
                known += listOf("armor.head", "armor.chest", "armor.legs", "armor.feet")
                known += listOf("weapon.mainhand", "weapon.offhand")
                // 只有能装东西的动物才有 container 槽
                if (e.id.removePrefix("minecraft:") in setOf(
                        "donkey", "mule", "llama", "trader_llama"
                    ) || e.items.keys.any { it.startsWith("container.") }
                ) known += "container"
            }
            else -> Unit
        }
        known += e.items.keys
        return known.any { slotMatches(slots, it) }
    }

    private fun blockHasSlots(block: GridBlock, slots: String): Boolean {
        val containerBlocks = setOf(
            "chest", "trapped_chest", "barrel", "furnace", "blast_furnace", "smoker",
            "dispenser", "dropper", "hopper", "brewing_stand", "shulker_box"
        )
        val base = block.id.removePrefix("minecraft:")
        val hasContainer = base in containerBlocks || base.endsWith("_shulker_box")
        val keys = mutableSetOf<String>()
        if (hasContainer) keys += "container"
        for (i in block.items.keys) keys += "container.$i"
        return keys.any { slotMatches(slots, it) }
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
                t == "~" -> kotlin.math.floor(base).toInt()
                t.startsWith("~") -> kotlin.math.floor(base + (t.substring(1).toDoubleOrNull() ?: 0.0)).toInt()
                else -> t.toDoubleOrNull()?.let { kotlin.math.floor(it).toInt() }
                    ?: kotlin.math.floor(base).toInt()
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
        val masked = mode.equals("masked", true)
        for (x in x1..x2) for (y in y1..y2) for (z in z1..z2) {
            val srcRaw = world.blocks["$x,$y,$z"]?.id
            if (masked && (srcRaw == null || normBlock(srcRaw) == "minecraft:air")) continue
            val src = srcRaw ?: return false
            val dx = dest.first + (x - x1)
            val dy = dest.second + (y - y1)
            val dz = dest.third + (z - z1)
            val dst = world.blocks["$dx,$dy,$dz"]?.id ?: return false
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
