package com.tellraw.app.model

import com.tellraw.app.util.ExecuteConverter
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 解析“多命令 tag 方案”，并在网格实体上模拟：
 *
 *   execute as @a <条件> run tag @s add t1
 *   execute as @a <条件> run tag @s add t2
 *   tellraw @a[tag=t1,tag=t2] {...}
 *   tag @a[tag=t1] remove t1
 *   tag @a[tag=t2] remove t2
 *
 * 模拟时：
 *  - 每条 tag add 行按候选实体逐个求值（[GridConditionEvaluator] 会把 @s 当候选实体），
 *    通过的实体获得虚拟 tag；
 *  - 最终 tellraw 行用 [SelectorEvaluator] 求值，tag= 参数就能选中对应实体；
 *  - remove 行在 tellraw 之后执行，不参与“谁收到 tellraw”的选择。
 *
 * 这是离线近似：未建模条件会保留提醒，并按“未参与筛选”处理（与网格既有口径一致）。
 */
object GridCommandPlanParser {

    data class TaggedCondition(val tag: String, val prefix: String, val tagTarget: String)

    data class TagPlan(
        val tellrawSelector: String,
        val conditions: List<TaggedCondition>,
        val notes: List<String> = emptyList(),
        /** 最终行是 `execute <prefix> run tellraw …` 时的前缀；普通 `tellraw …` 为 null。 */
        val finalPrefix: String? = null
    )

    data class Result(val matched: List<GridEntity>, val notes: List<String>)

    fun parse(input: String): TagPlan? {
        val lines = input.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.size < 2) return null

        val notes = mutableListOf<String>()
        var tellrawSelector: String? = null
        var finalPrefix: String? = null
        val conditions = mutableListOf<TaggedCondition>()

        fun parseTellrawSelector(text: String): String? {
            val rest = text.removePrefix("tellraw").trimStart()
            val sp = firstTopLevelSpace(rest)
            if (sp <= 0) {
                notes.add("tellraw 行缺少选择器，已忽略该行")
                return null
            }
            return rest.substring(0, sp).trim()
        }

        for (line in lines) {
            when {
                line == "tellraw" || line.startsWith("tellraw ") -> {
                    val sel = parseTellrawSelector(line) ?: continue
                    if (tellrawSelector == null) tellrawSelector = sel
                    else if (tellrawSelector != sel) notes.add("检测到多条不同的 tellraw 行，按第一条模拟")
                }
                line == "execute" || line.startsWith("execute ") -> {
                    val runIdx = findTopLevelRun(line) ?: continue
                    val prefix = line.substring(0, runIdx).trim()
                    val rest = line.substring(runIdx + 3).trimStart()
                    if (rest.startsWith("tag ")) {
                        if (" add " !in rest) continue
                        val body = rest.removePrefix("tag ").trim()
                        val addIdx = body.indexOf(" add ")
                        if (addIdx <= 0) continue
                        val tagTarget = body.substring(0, addIdx).trim()
                        val tag = body.substring(addIdx + 5).trim().substringBefore(' ')
                        if (tagTarget.isBlank() || tag.isBlank()) continue
                        conditions.add(TaggedCondition(tag, prefix, tagTarget))
                    } else if (rest == "tellraw" || rest.startsWith("tellraw ")) {
                        val sel = parseTellrawSelector(rest) ?: continue
                        if (tellrawSelector == null) {
                            tellrawSelector = sel
                            finalPrefix = prefix
                        } else if (tellrawSelector != sel) {
                            notes.add("检测到多条不同的 tellraw 行，按第一条模拟")
                        }
                    }
                }
                line.startsWith("tag ") -> {
                    notes.add("tag remove 行在 tellraw 之后执行，不参与收信人选择模拟")
                }
            }
        }

        val selector = tellrawSelector ?: return null
        if (conditions.isEmpty()) {
            notes.add("没有识别到 tag add 行；按普通 tellraw 选择器求值")
        }
        return TagPlan(selector, conditions, notes.distinct(), finalPrefix)
    }

    /**
     * 单条 execute 链的逐步求值（供网格普通命令使用），与 tag 方案共用同一 context 引擎。
     * 返回最终收信人集合；无法解析返回 null。
     */
    fun evaluateChain(
        prefix: String,
        targetSelector: String,
        entities: List<GridEntity>,
        executor: GridEntity? = null,
        target: VersionSpec? = null,
        world: GridConditionEvaluator.GridWorld = GridConditionEvaluator.GridWorld(),
        baseOrigin: Triple<Double, Double, Double> = Triple(0.0, -60.0, 0.0),
        edition: String = "java"
    ): Result? {
        if (prefix.isBlank()) return null
        val notes = mutableListOf<String>()
        val contexts = evaluateParts(
            prefix, entities, executor, edition, baseOrigin, target, world, notes
        )
        val matched = contexts.flatMap { ctx ->
            SelectorEvaluator.evaluate(
                selector = targetSelector,
                entities = entities,
                executor = ctx.entity,
                edition = edition,
                origin = ctx.origin,
                target = target
            ).also { notes.addAll(it.notes) }.matched
        }.distinctBy { it.uid }
        return Result(matched, notes.distinct())
    }

    fun evaluate(
        plan: TagPlan,
        entities: List<GridEntity>,
        baseOrigin: Triple<Double, Double, Double>,
        executor: GridEntity? = null,
        target: VersionSpec? = null,
        world: GridConditionEvaluator.GridWorld = GridConditionEvaluator.GridWorld()
    ): Result {
        val notes = plan.notes.toMutableList()
        val virtualTags = mutableMapOf<String, MutableSet<String>>()
        val edition = if (target?.edition == VersionSpec.Edition.BEDROCK) "bedrock" else "java"

        for (condition in plan.conditions) {
            val contexts = evaluateParts(
                prefix = condition.prefix,
                entities = entities,
                executor = executor,
                edition = edition,
                baseOrigin = baseOrigin,
                target = target,
                world = world,
                notes = notes
            )
            if (contexts.isEmpty()) {
                notes.add("该 tag 条件的执行者为空（as/on 没有选中实体，或关系未设置）")
                continue
            }
            for (ctx in contexts) {
                if (condition.tagTarget == "@s") {
                    virtualTags.getOrPut(ctx.entity.uid) { mutableSetOf() }.add(condition.tag)
                } else {
                    val tr = SelectorEvaluator.evaluate(
                        selector = condition.tagTarget,
                        entities = entities,
                        executor = ctx.entity,
                        edition = edition,
                        origin = ctx.origin,
                        target = target
                    )
                    notes.addAll(tr.notes)
                    for (m in tr.matched) {
                        virtualTags.getOrPut(m.uid) { mutableSetOf() }.add(condition.tag)
                    }
                }
            }
        }

        val augmented = entities.map { e ->
            val added = virtualTags[e.uid]
            if (added.isNullOrEmpty()) e else e.copy(tags = e.tags + added)
        }
        val matched = if (plan.finalPrefix != null) {
            val contexts = evaluateParts(
                prefix = plan.finalPrefix,
                entities = augmented,
                executor = executor,
                edition = edition,
                baseOrigin = baseOrigin,
                target = target,
                world = world,
                notes = notes
            )
            contexts.flatMap { ctx ->
                SelectorEvaluator.evaluate(
                    selector = plan.tellrawSelector,
                    entities = augmented,
                    executor = ctx.entity,
                    edition = edition,
                    origin = ctx.origin,
                    target = target
                ).also { notes.addAll(it.notes) }.matched
            }.distinctBy { it.uid }
        } else {
            val final = SelectorEvaluator.evaluate(
                selector = plan.tellrawSelector,
                entities = augmented,
                executor = executor,
                edition = edition,
                origin = baseOrigin,
                target = target
            )
            notes.addAll(final.notes)
            final.matched
        }
        return Result(matched, notes.distinct())
    }

    /**
     * 按 tag 命令里的 as/on 修饰，把执行者解析到最后一批实体。
     * at/positioned 交给 [GridConditionEvaluator.resolveOrigin]；其余修饰不影响筛选。
     * summon 生成物未知，返回空（并提示）。
     */
    private data class ExecContext(
        val entity: GridEntity,
        val origin: Triple<Double, Double, Double>,
        val yaw: Double = 0.0,
        val pitch: Double = 0.0,
        val dimension: String = "minecraft:overworld",
        val anchorEyes: Boolean = false
    )

    private fun ExecContext.contextKey(): String =
        "${entity.uid}|${origin.first}|${origin.second}|${origin.third}|$yaw|$pitch|$dimension|$anchorEyes"

    /**
     * 逐步执行 execute 前缀：as/on 换执行者，at/positioned/rotated/facing/align/anchored 更新
     * 执行位置、旋转、维度和锚点，if/unless 立即对当时上下文求值。
     */
    private fun evaluateParts(
        prefix: String,
        entities: List<GridEntity>,
        executor: GridEntity?,
        edition: String,
        baseOrigin: Triple<Double, Double, Double>,
        target: VersionSpec?,
        world: GridConditionEvaluator.GridWorld,
        notes: MutableList<String>
    ): List<ExecContext> {
        val parts = ExecuteConverter.parseExecuteParts(prefix, notes) ?: return emptyList()
        var contexts = if (executor != null) {
            listOf(
                ExecContext(
                    entity = executor,
                    origin = baseOrigin,
                    yaw = executor.rotationY ?: 0.0,
                    pitch = executor.rotationX ?: 0.0,
                    dimension = world.dimension
                )
            )
        } else {
            entities.map {
                ExecContext(
                    entity = it,
                    origin = baseOrigin,
                    yaw = it.rotationY ?: 0.0,
                    pitch = it.rotationX ?: 0.0,
                    dimension = world.dimension
                )
            }
        }

        fun selectEntities(selector: String, ctx: ExecContext): List<GridEntity> =
            SelectorEvaluator.evaluate(
                selector = selector,
                entities = entities,
                executor = ctx.entity,
                edition = edition,
                origin = ctx.origin,
                target = target
            ).also { notes.addAll(it.notes) }.matched

        fun angleValue(text: String, base: Double): Double {
            val t = text.trim()
            return when {
                t == "~" -> base
                t.startsWith("~") -> base + (t.substring(1).toDoubleOrNull() ?: 0.0)
                else -> t.toDoubleOrNull() ?: base
            }
        }

        for (part in parts) {
            val conditionText = part.conditionText
            if (conditionText != null) {
                contexts = contexts.mapNotNull { ctx ->
                    val worldForCtx = world.copy(dimension = ctx.dimension)
                    val r = GridConditionEvaluator.evaluate(
                        conditionText, ctx.entity, entities, target, worldForCtx, ctx.origin,
                        ctx.yaw, ctx.pitch, ctx.anchorEyes
                    )
                    notes.addAll(r.notes)
                    if (r.ok) ctx else null
                }
                continue
            }

            val tokens = part.tokens
            when (tokens.firstOrNull()) {
                "as" -> {
                    val selector = tokens.getOrNull(1)
                    if (selector == null) {
                        notes.add("as 缺少选择器，已忽略")
                    } else {
                        contexts = contexts.flatMap { ctx ->
                            selectEntities(selector, ctx).map {
                                ctx.copy(entity = it)
                            }
                        }.distinctBy { it.contextKey() }
                        if (contexts.isEmpty()) notes.add("as $selector 没有选中网格实体")
                    }
                }
                "on" -> {
                    val relation = tokens.getOrNull(1)
                    if (relation == null) {
                        notes.add("on 缺少关系，已忽略")
                    } else {
                        val next = mutableListOf<ExecContext>()
                        var missing = 0
                        for (ctx in contexts) {
                            val uids = ctx.entity.relations[relation].orEmpty()
                            if (uids.isEmpty()) missing++
                            for (uid in uids) {
                                val related = entities.firstOrNull { it.uid == uid }
                                if (related == null) {
                                    notes.add("on $relation 指向的实体 $uid 不在场上")
                                } else {
                                    // on 只换执行者，不改变执行位置/旋转/维度。
                                    next.add(ctx.copy(entity = related))
                                }
                            }
                        }
                        if (missing > 0) notes.add("$missing 个执行者没有设置 on $relation 关系")
                        contexts = next.distinctBy { it.contextKey() }
                    }
                }
                "at" -> {
                    val selector = tokens.getOrNull(1)
                    if (selector == null) {
                        notes.add("at 缺少选择器，已忽略")
                    } else {
                        contexts = contexts.map { ctx ->
                            val first = selectEntities(selector, ctx).firstOrNull()
                            if (first == null) {
                                notes.add("at $selector 没有选中实体，上下文保持不变")
                                ctx
                            } else {
                                ctx.copy(
                                    origin = Triple(first.x, first.y, first.z),
                                    yaw = first.rotationY ?: ctx.yaw,
                                    pitch = first.rotationX ?: ctx.pitch
                                )
                            }
                        }
                    }
                }
                "positioned" -> {
                    val first = tokens.getOrNull(1)
                    when (first) {
                        "as" -> {
                            val selector = tokens.getOrNull(2)
                            if (selector == null) {
                                notes.add("positioned as 缺少选择器，已忽略")
                            } else {
                                contexts = contexts.map { ctx ->
                                    val targetEntity = selectEntities(selector, ctx).firstOrNull()
                                    if (targetEntity == null) {
                                        notes.add("positioned as $selector 没有选中实体，原点保持不变")
                                        ctx
                                    } else {
                                        ctx.copy(
                                            origin = Triple(
                                                targetEntity.x, targetEntity.y, targetEntity.z
                                            )
                                        )
                                    }
                                }
                            }
                        }
                        "over" -> notes.add("execute positioned over（高度图）未建模，原点保持不变")
                        else -> {
                            val ys = tokens.getOrNull(2)
                            val zs = tokens.getOrNull(3)
                            if (first == null || ys == null || zs == null) {
                                notes.add("positioned 参数不完整，已忽略")
                            } else if (
                                first.startsWith("^") || ys.startsWith("^") || zs.startsWith("^")
                            ) {
                                contexts = contexts.map { ctx ->
                                    val p = localToWorld(first, ys, zs, ctx, notes)
                                    ctx.copy(origin = p)
                                }
                            } else {
                                contexts = contexts.map { ctx ->
                                    ctx.copy(
                                        origin = Triple(
                                            coord(first, ctx.origin.first, notes),
                                            coord(ys, ctx.origin.second, notes),
                                            coord(zs, ctx.origin.third, notes)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
                "rotated" -> {
                    val first = tokens.getOrNull(1)
                    if (first == "as") {
                        val selector = tokens.getOrNull(2)
                        if (selector == null) {
                            notes.add("rotated as 缺少选择器，已忽略")
                        } else {
                            contexts = contexts.map { ctx ->
                                val targetEntity = selectEntities(selector, ctx).firstOrNull()
                                if (targetEntity == null) {
                                    notes.add("rotated as $selector 没有选中实体，旋转保持不变")
                                    ctx
                                } else if (targetEntity.rotationY == null && targetEntity.rotationX == null) {
                                    notes.add("rotated as 的目标没有设置视角，旋转保持不变")
                                    ctx
                                } else {
                                    ctx.copy(
                                        yaw = targetEntity.rotationY ?: ctx.yaw,
                                        pitch = targetEntity.rotationX ?: ctx.pitch
                                    )
                                }
                            }
                        }
                    } else {
                        val yawText = tokens.getOrNull(1)
                        val pitchText = tokens.getOrNull(2)
                        if (yawText == null || pitchText == null) {
                            notes.add("rotated 参数不完整，已忽略")
                        } else {
                            contexts = contexts.map { ctx ->
                                ctx.copy(
                                    yaw = angleValue(yawText, ctx.yaw),
                                    pitch = angleValue(pitchText, ctx.pitch)
                                )
                            }
                        }
                    }
                }
                "facing" -> {
                    if (tokens.getOrNull(1) == "entity") {
                        val selector = tokens.getOrNull(2)
                        val anchor = tokens.getOrNull(3)
                        if (selector == null || anchor == null) {
                            notes.add("facing entity 参数不完整，已忽略")
                        } else {
                            contexts = contexts.map { ctx ->
                                val targetEntity = selectEntities(selector, ctx).firstOrNull()
                                if (targetEntity == null) {
                                    notes.add("facing entity $selector 没有选中实体，旋转保持不变")
                                    ctx
                                } else {
                                    val targetY = targetEntity.y +
                                        if (anchor == "eyes") 1.62 else 0.0
                                    ctx.copyFromFacing(targetEntity.x, targetY, targetEntity.z)
                                }
                            }
                        }
                    } else {
                        val xs = tokens.getOrNull(1)
                        val ys = tokens.getOrNull(2)
                        val zs = tokens.getOrNull(3)
                        if (xs == null || ys == null || zs == null) {
                            notes.add("facing 参数不完整，已忽略")
                        } else {
                            contexts = contexts.map { ctx ->
                                val tx = coord(xs, ctx.origin.first, notes)
                                val ty = coord(ys, ctx.origin.second, notes)
                                val tz = coord(zs, ctx.origin.third, notes)
                                ctx.copyFromFacing(tx, ty, tz)
                            }
                        }
                    }
                }
                "align" -> {
                    val axes = tokens.getOrNull(1).orEmpty()
                    if (axes.isEmpty()) {
                        notes.add("align 缺少轴，已忽略")
                    } else {
                        contexts = contexts.map { ctx ->
                            ctx.copy(
                                origin = Triple(
                                    if ('x' in axes) floor(ctx.origin.first) else ctx.origin.first,
                                    if ('y' in axes) floor(ctx.origin.second) else ctx.origin.second,
                                    if ('z' in axes) floor(ctx.origin.third) else ctx.origin.third
                                )
                            )
                        }
                    }
                }
                "anchored" -> {
                    when (tokens.getOrNull(1)) {
                        "eyes" -> contexts = contexts.map { it.copy(anchorEyes = true) }
                        "feet" -> contexts = contexts.map { it.copy(anchorEyes = false) }
                        else -> notes.add("anchored 参数不是 eyes/feet，已忽略")
                    }
                }
                "in" -> {
                    val dimension = tokens.getOrNull(1)
                    if (dimension == null) {
                        notes.add("in 缺少维度，已忽略")
                    } else {
                        contexts = contexts.map { it.copy(dimension = dimension) }
                        notes.add(
                            "in $dimension 已切换执行维度；方块/群系等世界数据仍按网格当前维度表，未按维度拆分"
                        )
                    }
                }
                "summon" -> {
                    notes.add("summon 生成的新实体未建模，执行者解析停止")
                    return emptyList()
                }
                else -> Unit
            }
        }
        return contexts
    }

    private fun ExecContext.copyFromFacing(tx: Double, ty: Double, tz: Double): ExecContext {
        val dx = tx - origin.first
        val dy = ty - origin.second
        val dz = tz - origin.third
        val horizontal = sqrt(dx * dx + dz * dz)
        if (horizontal < 1e-9 && kotlin.math.abs(dy) < 1e-9) return this
        val yaw = Math.toDegrees(atan2(-dx, dz))
        val pitch = Math.toDegrees(atan2(-dy, horizontal))
        return copy(yaw = yaw, pitch = pitch)
    }

    private fun localToWorld(
        xText: String,
        yText: String,
        zText: String,
        ctx: ExecContext,
        notes: MutableList<String>
    ): Triple<Double, Double, Double> {
        val left = xText.trim().removePrefix("^").toDoubleOrNull()
            ?: return Triple(
                coord(xText, ctx.origin.first, notes),
                coord(yText, ctx.origin.second, notes),
                coord(zText, ctx.origin.third, notes)
            )
        val up = yText.trim().removePrefix("^").toDoubleOrNull()
            ?: return ctx.origin
        val forward = zText.trim().removePrefix("^").toDoubleOrNull()
            ?: return ctx.origin
        val yaw = Math.toRadians(ctx.yaw)
        val pitch = Math.toRadians(ctx.pitch)
        val baseY = ctx.origin.second + if (ctx.anchorEyes) 1.62 else 0.0
        val fx = -sin(yaw) * cos(pitch)
        val fy = -sin(pitch)
        val fz = cos(yaw) * cos(pitch)
        val lx = cos(yaw)
        val lz = sin(yaw)
        val ux = sin(yaw) * sin(pitch)
        val uy = cos(pitch)
        val uz = -cos(yaw) * sin(pitch)
        return Triple(
            ctx.origin.first + left * lx + up * ux + forward * fx,
            baseY + up * uy + forward * fy,
            ctx.origin.third + left * lz + up * uz + forward * fz
        )
    }

    private fun coord(text: String, base: Double, notes: MutableList<String>): Double {
        val t = text.trim()
        return when {
            t == "~" -> base
            t.startsWith("~") -> base + (t.substring(1).toDoubleOrNull() ?: 0.0)
            t.startsWith("^") -> {
                notes.add("局部坐标 ^ 只应成组出现，已按相对坐标 ~ 处理")
                base
            }
            else -> t.toDoubleOrNull() ?: base
        }
    }

    /** 按空格切分，但保留 {} / [] / "" / '' 内部的空格。 */
    private fun splitTopLevelTokens(input: String): List<String> {
        val out = mutableListOf<String>()
        val cur = StringBuilder()
        var depth = 0
        var quote: Char? = null
        var i = 0
        while (i < input.length) {
            val c = input[i]
            if (quote != null) {
                cur.append(c)
                if (c == '\\' && i + 1 < input.length) {
                    cur.append(input[i + 1])
                    i += 2
                    continue
                }
                if (c == quote) quote = null
                i++
                continue
            }
            when (c) {
                '"', '\'' -> {
                    quote = c
                    cur.append(c)
                }
                '{', '[' -> {
                    depth++
                    cur.append(c)
                }
                '}', ']' -> {
                    if (depth > 0) depth--
                    cur.append(c)
                }
                ' ', '\t' -> {
                    if (depth == 0) {
                        if (cur.isNotEmpty()) {
                            out.add(cur.toString())
                            cur.clear()
                        }
                    } else {
                        cur.append(c)
                    }
                }
                else -> cur.append(c)
            }
            i++
        }
        if (cur.isNotEmpty()) out.add(cur.toString())
        return out
    }

    // ---------- 顶层空格 / run 查找 ----------

    /** 第一个括号/引号外、深度为 0 的空格下标；找不到返回 -1。 */
    private fun firstTopLevelSpace(s: String): Int {
        var depth = 0
        var quote: Char? = null
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (quote != null) {
                if (c == '\\') {
                    i += 2
                    continue
                }
                if (c == quote) quote = null
                i++
                continue
            }
            when (c) {
                '"', '\'' -> quote = c
                '{', '[' -> depth++
                '}', ']' -> if (depth > 0) depth--
                ' ', '\t' -> if (depth == 0) return i
            }
            i++
        }
        return -1
    }

    /** 顶层 `run` 的起始下标；找不到返回 null。 */
    private fun findTopLevelRun(s: String): Int? {
        var depth = 0
        var quote: Char? = null
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (quote != null) {
                if (c == '\\') {
                    i += 2
                    continue
                }
                if (c == quote) quote = null
                i++
                continue
            }
            when (c) {
                '"', '\'' -> quote = c
                '{', '[' -> depth++
                '}', ']' -> if (depth > 0) depth--
                ' ', '\t' -> {
                    if (depth == 0 && s.startsWith("run", i + 1)) {
                        val end = i + 4
                        if (end >= s.length || s[end].isWhitespace()) return i + 1
                    }
                }
            }
            i++
        }
        return null
    }
}
