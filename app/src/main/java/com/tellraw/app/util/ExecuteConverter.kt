package com.tellraw.app.util

/**
 * execute 前置命令解析 / 条件子命令互转。
 *
 * 本模块做三件事：
 *  1. tokenize、识别每个子命令的参数边界、决定"保留"还是"放弃"；
 *  2. 跟踪执行者（as / on / summon 改执行者，at / positioned / rotated … 只改坐标）；
 *  3. 把 if/unless 条件子命令在 Java 与基岩之间互转（items↔hasitem、score↔scores、
 *     data↔nbt=/hasitem），并原样保留 as/at/... 这些修饰子命令。
 *
 * 条件互转的取舍：**不把条件折进 `as` 的选择器**，只折进它自己写的目标选择器
 * （多数情况就是 `@s`），这样即使前面已有 `as`/`on`，`@s` 仍指"当时"的执行者，
 * 不会出现 `as @e[type=creeper] … as @a[c=5] …` 里把 @a 的条件折进 creeper 的错误。
 * 转不了的子条件一律"保留原样 + 提醒"（不静默丢弃内容）。
 */
object ExecuteConverter {

    /** 转换方向。 */
    enum class Direction { JAVA_TO_BEDROCK, BEDROCK_TO_JAVA }

    // 修饰子命令：只限定执行上下文，本 App 不转换它们，但语法完整就必须原样保留（不能因"没意义"而丢弃）。
    private val MODIFIERS = setOf(
        "align", "anchored", "as", "at", "facing", "in", "on", "positioned", "rotated", "summon"
    )

    // 所有子命令关键字。用于判断"参数位置是否出现了关键字"——参数不可能是关键字，
    // 一旦出现说明该子命令参数缺失，借此把 `execute as if ...` 这类退化输入判为不完整而非吞掉下一个子命令。
    private val KEYWORDS = MODIFIERS + setOf("execute", "if", "unless", "run")

    // 放弃一条子命令后，"可以从中恢复解析"的关键字。注意此处不含 "execute"：
    // 中段再出现 execute 属于非法输入，不能当作可继续解析的锚点。
    private val RESUME_KEYWORDS = MODIFIERS + setOf("if", "unless", "run")

    // 坐标/方块位置在 token 层面是 3 个（x y z，如 `~ ~ ~`、`^ ^ ^`），因此占位符"<pos>"要展开成 3 个 token。
    private val POSITION_TOKENS = 3

    // 记分板整型全域（int 的最小值~最大值）。`if score @s <obj> matches <全域>` 可作"分数存在"判定：
    // 目标没有该记分项时该判定失败（execute.txt:1229-1230 的官方例子印证；execute需求 第十节 9）。
    private const val SCORE_FULL_RANGE = "-2147483648..2147483647"

    // 已知条件类型；不在此集合内即视为未知条件并放弃（链已断开，不做猜测）。
    private val KNOWN_CONDITIONS = setOf(
        "biome", "block", "blocks", "data", "dimension", "entity", "function",
        "items", "loaded", "predicate", "score", "slots", "stopwatch"
    )

    // Java 专有条件：基岩没有对应写法，只能保留 + 提醒（是否该"放弃"是 execute实例表 的规格冲突①，未裁决）。
    private val JAVA_ONLY_CONDITIONS = setOf("data", "items", "predicate", "function", "stopwatch", "loaded", "slots")

    /**
     * 解析 execute 前置命令（只解析、不转换条件）。
     *
     * @param prefix    用户在前置框输入的内容，可能带或不带开头的 "execute"
     * @param reminders 放弃/异常原因（中文）写回此列表
     * @return 保留下来的子命令 token 序列；不是合法 execute 前缀返回 null。
     *         `execute`（只有它）返回空列表，表示"合法但没有可保留的子命令"；
     *         空输入同样返回 null，因为空串并不是一个合法的 execute 前缀。
     */
    fun parseExecutePrefix(prefix: String, reminders: MutableList<String>): List<String>? {
        val segs = parseSegments(prefix, reminders) ?: return null
        val kept = mutableListOf<String>()
        for (seg in segs) {
            when (seg) {
                is Seg.Mod -> kept.addAll(seg.tokens)
                is Seg.Cond -> {
                    kept.add(if (seg.unless) "unless" else "if")
                    kept.add(seg.name)
                    kept.addAll(seg.args)
                }
            }
        }
        return kept
    }

    /**
     * 解析 execute 前置命令，并把其中的条件子命令按 [direction] 互转。
     *
     * @return 转换后的完整前缀（以 `execute` 开头，不含 `run ...`）；非法前缀返回 null。
     *         `execute`（只有它）返回 "execute"。
     */
    fun convertExecutePrefix(
        prefix: String,
        direction: Direction,
        reminders: MutableList<String>
    ): String? {
        val segs = parseSegments(prefix, reminders) ?: return null
        mergeSafetyNotice(segs, reminders)
        val out = mutableListOf("execute")
        var exec = ExecState(isPlayer = true, changed = false)
        for (seg in segs) {
            when (seg) {
                is Seg.Mod -> {
                    out.addAll(seg.tokens)
                    exec = applyModifier(seg.tokens, exec)
                }
                is Seg.Cond -> out.addAll(convertCondition(seg, direction, exec, reminders))
            }
        }
        return out.joinToString(" ")
    }

    /**
     * 组合出最终命令：`<转换后的 execute 前缀> run <tellraw 命令>`（需求七.3"本 App 的产物是
     * 'execute 前缀 + tellraw 命令'这一整条命令"）。
     *
     * [convertedPrefix] 必须已含开头的 `execute`（[convertExecutePrefix] 的返回值满足此条件）。
     */
    fun composeTellrawCommand(convertedPrefix: String, tellrawCommand: String): String =
        "$convertedPrefix run $tellrawCommand"

    /**
     * 折进 / 合并守卫（本次修正 C；依据 execute需求 十.8、A5）。
     *
     * 把条件折进（合并进）选择器必须**同时**满足：
     *  ① 该区间内没有执行者变化（as / on / summon）；
     *  ② 没有执行点（坐标）变化（at / positioned / rotated / facing / anchored / align / in）；
     *  ③ 折后选中的是**同一个单一实体**。
     *
     * 本实现取最保守口径：只要同一 execute 链里出现**目标不一致的条件**（如 `if entity @s` 与
     * `if entity @a`），就判定"不能保证同一人"，**一律不合并**，只写一条提醒。
     * 反例：`execute as @a if entity @s if entity @a` —— @s 是执行者本人，@a 只要一人满足即可，
     * 二者不保证同一实体，禁止合并。（"宁可长不可错"）
     */
    private fun mergeSafetyNotice(segs: List<Seg>, reminders: MutableList<String>) {
        var execChanged = false
        var posChanged = false
        val targets = mutableListOf<String>()
        for (seg in segs) {
            when (seg) {
                is Seg.Mod -> when (seg.tokens.firstOrNull()) {
                    "as", "on", "summon" -> execChanged = true
                    "at", "positioned", "rotated", "facing", "anchored", "align", "in" -> posChanged = true
                }
                is Seg.Cond -> {
                    // 只关心"带目标的选择器类"条件：目标不一致就不可能合并成同一个单一实体。
                    // 归一化到选择器变量（去掉 [] 里的参数），这样 @s[tag=x] 与 @s[tag=y] 视为同一目标。
                    if (seg.name in setOf("entity", "items", "data", "score")) {
                        seg.args.firstOrNull { it.startsWith("@") || it == "*" }
                            ?.substringBefore('[')?.trim()
                            ?.let { targets.add(it) }
                    }
                }
            }
        }
        val distinct = targets.distinct()
        if (distinct.size <= 1) return
        val why = buildList {
            if (execChanged) add("链中有执行者变化")
            if (posChanged) add("链中有坐标变化")
        }
        reminders.add(
            "检测到多个条件的目标不一致（${distinct.joinToString("、")}）" +
                (if (why.isEmpty()) "" else "，" + why.joinToString("、")) +
                "；按修正 C 的三条件校验（无执行者变化 / 无坐标变化 / 同一单一实体）未通过，" +
                "**不做任何合并**（宁可长不可错）"
        )
    }

    /**
     * 基岩选择器里的"选择器级否定"（scores 的 `=!`、hasitem 的 quantity=0 等）转成 Java
     * 需要的命令级前置。
     *
     * @return Pair(Java 的 execute 前缀, 处理后的 tellraw 选择器)；无需转换返回 null。
     *         例：`@a[scores={n=!5}]` -> ("execute as @a unless entity @s[scores={n=5}]", "@s")
     */
    fun convertBedrockSelectorToJavaPrefix(
        selector: String,
        reminders: MutableList<String>
    ): Pair<String, String>? {
        val neg = bedrockSelectorNegation(selector, reminders) ?: return null
        val (asSelector, conditionTokens) = neg
        return "execute as $asSelector " + conditionTokens.joinToString(" ") to "@s"
    }

    /**
     * 拆出基岩选择器里"Java 无法用选择器表达、必须靠 execute"的部分。
     *
     * @return Pair(选择器本体（已去掉被拆出的参数，可能仍是 `@a[tag=x]` 这种）, 条件 token 列表)；
     *         没有需要拆出的内容时返回 null。
     *         例：`@a[tag=x,scores={n=!5}]` -> (`@a[tag=x]`, ["unless","entity","@s[scores={n=5}]"])
     *
     * 调用方负责：给"选择器本体"再过一遍目标版本的参数过滤，并把条件 token 折进 execute 前缀；
     * 若前缀里还要接用户自己写的前置命令，**用户写的在前，本结果接在其后**（execute需求 第十一节 9）。
     */
    fun bedrockSelectorNegation(
        selector: String,
        reminders: MutableList<String>
    ): Pair<String, List<String>>? {
        if (!selector.contains('[') || !selector.endsWith("]")) return null
        val varName = selector.substringBefore('[')
        val paramsPart = selector.substringAfter('[').dropLast(1)
        val params = ExecCondSupport.splitTopLevel(paramsPart, ',')

        val kept = mutableListOf<String>()
        val conditionTokens = mutableListOf<String>()
        var changed = false

        for (p in params) {
            val name = p.substringBefore('=').trim()
            when (name) {
                "scores" -> {
                    val content = p.substringAfter("scores=").trim().removeSurrounding("{", "}")
                    val pos = mutableListOf<String>()
                    val neg = mutableListOf<String>()
                    for (e in ExecCondSupport.splitTopLevel(content, ',')) {
                        val i = ExecCondSupport.indexOfTopLevel(e, '=')
                        if (i < 0) continue
                        val k = e.substring(0, i).trim()
                        val v = e.substring(i + 1).trim()
                        if (v.startsWith("!")) neg.add("$k=${v.substring(1)}") else pos.add("$k=$v")
                    }
                    if (pos.isNotEmpty()) kept.add("scores={${pos.joinToString(",")}}")
                    if (neg.isNotEmpty()) {
                        // 本次修正 D：`=!` 的口径 = "存在 ∧ ≠x"（execute需求 第十节 9）
                        conditionTokens.addAll(negatedScoreConditionTokens(neg, reminders))
                        changed = true
                    }
                }
                "hasitem" -> {
                    val value = p.substringAfter("hasitem=").trim()
                    val objects = parseHasitemValue(value)
                    if (objects == null) {
                        reminders.add("基岩 hasitem 结构无法解析，已原样保留：$p")
                        kept.add(p)
                        continue
                    }
                    // 只有"Java 的选择器根本写不出来"的 hasitem 才拆进 execute：
                    // item=air（"该槽为空"；按玩家决定保留该写法）、quantity=0（"没有该物品"）、
                    // quantity=0..（"不做过滤"，条件形同虚设 -> 去掉）。
                    // 正向"有某物品"交给原有的 nbt= 路径（避免无谓地改动既有输出）。
                    if (objects.none { isNegationLikeHasitem(it) }) {
                        kept.add(p)
                        continue
                    }
                    changed = true
                    for (obj in objects) {
                        // 这里由调用方补 `as <varName>`，@s 指代正确
                        val conv = hasitemToJavaConditionTokens(
                            obj, enclosingUnless = false, target = "@s", reminders
                        )
                        if (conv == null) {
                            reminders.add("hasitem（$obj）无法回译成 Java 条件，已忽略该条件项目")
                        } else {
                            conditionTokens.addAll(conv)
                        }
                    }
                }
                else -> kept.add(p)
            }
        }

        if (!changed) return null
        val asSelector = if (kept.isEmpty()) varName else "$varName[${kept.joinToString(",")}]"
        return asSelector to conditionTokens
    }

    // ==================================================================
    //  解析为段落（修饰 / 条件）
    // ==================================================================

    private sealed class Seg {
        class Mod(val tokens: List<String>) : Seg()
        class Cond(val unless: Boolean, val name: String, val args: List<String>) : Seg()
    }

    private fun parseSegments(prefix: String, reminders: MutableList<String>): List<Seg>? {
        val tokens = tokenize(prefix) ?: return null
        if (tokens.isEmpty()) return null

        var idx: Int
        if (tokens[0] == "execute") {
            idx = 1
        } else if (tokens[0] in MODIFIERS) {
            idx = 0
        } else {
            return null
        }

        val segs = mutableListOf<Seg>()
        while (idx < tokens.size) {
            val name = tokens[idx]

            if (name == "run") {
                // 需求七.3 / 八【run】：输入里已有 run 时，只处理 run 之前的部分，
                // 之后的内容由本 App 依据"选择器 / 消息"两个输入框重新生成（原样丢弃并提醒）。
                val trailing = tokens.size - idx - 1
                if (trailing > 0) {
                    reminders.add(
                        if (tokens[idx + 1] == "tellraw") {
                            "输入里已包含 \"run tellraw …\"：已丢弃 run 之后的 $trailing 个 token，" +
                                "并按需求七.3 用选择器/消息框重新生成 tellraw（原内容不参与转换）"
                        } else {
                            "输入里已有 \"run\"：已丢弃 run 之后的 $trailing 个 token" +
                                "（需求八【run】：run 之后非空且不是 tellraw -> 直接丢弃）"
                        }
                    )
                }
                break
            }

            if (name in MODIFIERS) {
                val span = modifierSpan(tokens, idx)
                if (span != null) {
                    segs.add(Seg.Mod(tokens.subList(idx, idx + span).toList()))
                    idx += span
                    continue
                }
                val resume = abandonAndResume(
                    tokens, idx, "修饰子命令 \"$name\" 参数不完整，已放弃", reminders
                ) ?: break
                idx = resume
                continue
            }

            if (name == "if" || name == "unless") {
                val condName = if (idx + 1 < tokens.size) tokens[idx + 1] else null
                if (condName != null && condName in KNOWN_CONDITIONS) {
                    val argCount = conditionArgCount(tokens, idx, condName)
                    if (argCount != null && idx + 2 + argCount <= tokens.size) {
                        segs.add(
                            Seg.Cond(
                                unless = name == "unless",
                                name = condName,
                                args = tokens.subList(idx + 2, idx + 2 + argCount).toList()
                            )
                        )
                        idx += 2 + argCount
                        continue
                    }
                    val resume = abandonAndResume(
                        tokens, idx, "条件子命令 \"$name $condName\" 参数不完整，已放弃", reminders
                    ) ?: break
                    idx = resume
                    continue
                }
                val reason = if (condName == null) {
                    "条件子命令 \"$name\" 后缺少条件类型，已放弃"
                } else {
                    "未知的条件类型 \"$condName\"（在 $name 之后），已放弃"
                }
                val resume = abandonAndResume(tokens, idx, reason, reminders) ?: break
                idx = resume
                continue
            }

            val resume = abandonAndResume(
                tokens, idx, "未知的子命令 \"$name\"，已放弃", reminders
            ) ?: break
            idx = resume
        }

        return segs
    }

    /**
     * 放弃 idx 处（含）的子命令后，寻找其后第一个可继续解析的已知关键字。
     * 返回 null 表示后缀里再没有已知关键字，调用方应结束解析。
     */
    private fun abandonAndResume(
        tokens: List<String>,
        idx: Int,
        message: String,
        reminders: MutableList<String>
    ): Int? {
        val resume = nextResumeIndex(tokens, idx + 1)
        val skipped = if (resume == null) tokens.size - idx - 1 else resume - idx - 1
        reminders.add("$message（跳过其后 $skipped 个 token），尝试从后续子命令继续解析")
        return resume
    }

    /** 从 from 起找第一个 RESUME_KEYWORDS 中的 token；找不到返回 null。 */
    private fun nextResumeIndex(tokens: List<String>, from: Int): Int? {
        var i = from
        while (i < tokens.size) {
            if (tokens[i] in RESUME_KEYWORDS) return i
            i++
        }
        return null
    }

    /**
     * 计算修饰子命令占用的 token 数（含关键字本身）。返回 null 表示参数不完整。
     */
    private fun modifierSpan(tokens: List<String>, idx: Int): Int? {
        val rest = tokens.size - idx - 1
        fun arg(i: Int): String? = if (idx + 1 + i < tokens.size) tokens[idx + 1 + i] else null

        return when (tokens[idx]) {
            "align", "anchored", "as", "at", "in", "on", "summon" ->
                if (rest >= 1 && arg(0) !in KEYWORDS) 2 else null

            "rotated" -> when {
                rest < 1 -> null
                arg(0) == "as" -> if (rest >= 2) 3 else null
                arg(0) in KEYWORDS -> null
                else -> if (rest >= 2) 3 else null
            }

            "facing" -> when {
                rest < 1 -> null
                arg(0) == "entity" -> if (rest >= 3) 4 else null
                arg(0) in KEYWORDS -> null
                else -> if (rest >= 3) 4 else null
            }

            "positioned" -> when {
                rest < 1 -> null
                arg(0) == "as" || arg(0) == "over" -> if (rest >= 2) 3 else null
                arg(0) in KEYWORDS -> null
                else -> if (rest >= 3) 4 else null
            }

            else -> null
        }
    }

    /**
     * 计算条件子命令除 "if/unless" 和条件名之外还需的参数个数。null 表示不完整。
     */
    private fun conditionArgCount(tokens: List<String>, idx: Int, condName: String): Int? {
        val source = if (idx + 2 < tokens.size) tokens[idx + 2] else null

        return when (condName) {
            "biome" -> POSITION_TOKENS + 1
            "block" -> POSITION_TOKENS + 1
            "blocks" -> POSITION_TOKENS * 3 + 1
            "loaded" -> POSITION_TOKENS
            "dimension", "entity", "function", "predicate", "stopwatch" -> 1
            // score：`<target> <obj> matches <range>` 只占 4 个；比较形式（= < <= > >=）占 5 个
            "score" -> {
                val op = if (idx + 4 < tokens.size) tokens[idx + 4] else null
                if (op == "matches") 4 else 5
            }
            "data" -> when (source) {
                null -> null
                "block" -> 1 + POSITION_TOKENS + 1
                else -> 3
            }
            "items" -> when (source) {
                null -> null
                "block" -> 1 + POSITION_TOKENS + 1 + 1
                else -> 4
            }
            "slots" -> when (source) {
                null -> null
                "block" -> 1 + POSITION_TOKENS + 1 + 1
                else -> 4
            }
            else -> null
        }
    }

    // ==================================================================
    //  执行者跟踪
    // ==================================================================

    private class ExecState(
        /** true=玩家, false=实体, null=无法确定（如 `on owner`）。 */
        val isPlayer: Boolean?,
        /** 整条链是否有过 as/on/summon。false 时执行者一定是玩家。 */
        val changed: Boolean
    )

    private fun applyModifier(tokens: List<String>, prev: ExecState): ExecState {
        return when (tokens[0]) {
            "as" -> ExecState(classifySelector(tokens[1], prev.isPlayer == true), true)
            "on" -> ExecState(null, true)          // owner/controller 可能是玩家，无法确定
            "summon" -> ExecState(false, true)      // summon 只能生成实体
            else -> prev                            // at/positioned/rotated/facing/anchored/in/align 只改坐标
        }
    }

    /**
     * 判断一个目标选择器（或玩家名）代表玩家还是实体。
     * 规则（execute需求 八）：玩家选择器且非 @r -> 玩家；type=player、或 @r[type=…] 只含 player -> 玩家；
     * @s -> 沿用当前执行者；其它 -> 实体；玩家名 -> 玩家。
     */
    private fun classifySelector(selector: String, currentIsPlayer: Boolean): Boolean? {
        val sel = selector.trim()
        if (!sel.startsWith("@")) return true // 玩家名
        val varName = sel.substringBefore('[')
        val typeVals = Regex("(?:^|[,{])\\s*type\\s*=\\s*([^,}\\]]+)")
            .findAll(sel).map { it.groupValues[1].trim() }.toList()
        val hasNegPlayer = typeVals.any { it == "!player" || it == "!minecraft:player" }
        val nonPlayer = typeVals.filter { it != "player" && it != "minecraft:player" && !it.startsWith("!") }
        return when (varName) {
            "@a", "@p" -> true
            "@s" -> currentIsPlayer
            "@r" -> typeVals.isNotEmpty() && nonPlayer.isEmpty() && !hasNegPlayer
            "@e", "@n" -> typeVals.isNotEmpty() && nonPlayer.isEmpty() && !hasNegPlayer
            else -> null
        }
    }

    // ==================================================================
    //  条件转换
    // ==================================================================

    /** 条件转换结果。 */
    private sealed class CondOut {
        /** 已转换、或本来两版通用无需改动的 token。 */
        class Out(val tokens: List<String>) : CondOut()
        /** 无法转换：按需求十.5"实在不能转的才舍弃该子条件"丢弃这一条（其余继续）。 */
        object Drop : CondOut()
    }

    private fun originalTokens(cond: Seg.Cond): List<String> = buildList {
        add(if (cond.unless) "unless" else "if")
        add(cond.name)
        addAll(cond.args)
    }

    private fun dropAnd(reminders: MutableList<String>, reason: String): CondOut {
        reminders.add("$reason → 按需求十.5 舍弃该子条件（其余照常转换）")
        return CondOut.Drop
    }

    private fun convertCondition(
        cond: Seg.Cond,
        direction: Direction,
        exec: ExecState,
        reminders: MutableList<String>
    ): List<String> {
        val out: CondOut = when (direction) {
            Direction.JAVA_TO_BEDROCK -> when (cond.name) {
                "items" -> convertItemsJavaToBedrock(cond, reminders)
                "score" -> convertScoreJavaToBedrock(cond, reminders)
                "data" -> convertDataJavaToBedrock(cond, exec, reminders)
                else -> if (cond.name in JAVA_ONLY_CONDITIONS) {
                    reminders.add("子条件 \"${cond.name}\" 基岩无对应写法 → 按需求十.5 舍弃该条")
                    CondOut.Drop
                } else {
                    CondOut.Out(originalTokens(cond)) // block/biome/dimension/entity 等两版都有，原样保留
                }
            }
            Direction.BEDROCK_TO_JAVA -> when (cond.name) {
                "entity" -> convertEntityBedrockToJava(cond, reminders)
                // items/data/slots 本就是 Java 语法，目标版本即 Java，原样保留即正确
                else -> CondOut.Out(originalTokens(cond))
            }
        }
        return when (out) {
            is CondOut.Out -> out.tokens
            CondOut.Drop -> emptyList()
        }
    }

    // ---------------------- Java -> 基岩 ----------------------

    /** `if|unless items entity <target> <slots> <item_predicate>` -> `if|unless entity <target>[hasitem=…]` */
    private fun convertItemsJavaToBedrock(cond: Seg.Cond, reminders: MutableList<String>): CondOut {
        val args = cond.args
        if (args.size < 4) return dropAnd(reminders, "items 参数不完整")
        if (args[0] != "entity") {
            return dropAnd(reminders, "if/unless items 的 block 形式基岩 hasitem 无法承载")
        }
        val target = args[1]
        val slots = args[2]
        val predicateRaw = args[3]
        if (target == "*") return dropAnd(reminders, "items 的目标 * (所有被追踪实体) 基岩无对应")
        val pred = ExecCondSupport.parseJavaPredicate(predicateRaw)
        if (pred.reason != null) return dropAnd(reminders, "物品谓词无法映射到 hasitem：${pred.reason}")
        val (loc, slot, err) = ExecCondSupport.javaSlotsToHasitem(slots, reminders)
        if (err != null) return dropAnd(reminders, "Java 槽位无法映射到 hasitem：$err")

        // 多槽位（通配/未定具体槽位）+ 数量约束：Java 是"存在某槽满足"（或关系），
        // 基岩 hasitem 是"且"、无法逐槽展开表达"或"（execute需求 第十节 13）。
        val multiSlot = slot == null
        var dropQuantity = false
        if (multiSlot && pred.count != null) {
            if (pred.count.startsWith("{")) {
                val hasZeroMin = Regex("min\\s*:\\s*0\\b").containsMatchIn(pred.count)
                if (hasZeroMin) {
                    // 含 0 的区间：保留"含 0"语义（0..N -> quantity=0..N）
                    reminders.add(
                        "多槽位近似：Java \"items … $slots $predicateRaw\" 是\"存在某槽满足\"（或关系），" +
                            "基岩 hasitem 是\"且\"、其 quantity 是\"该栏所有槽位的总量\"，二者不等价" +
                            "（execute需求 第十节 7/11）；已按 quantity=${ExecCondSupport.quantityForPredicate(pred)} 输出" +
                            "（区间含 0，保留\"含 0\"语义），请自行核对"
                    )
                } else {
                    // 非 0 起始区间：基岩的 quantity 是"该栏总量"，与 Java 的"堆叠数"不同义，
                    // 按裁决一并放开为"该栏里有这个物品"（不写 quantity）
                    dropQuantity = true
                    reminders.add(
                        "多槽位 + 非 0 起始区间，无法逐槽表达：原语义 Java \"$predicateRaw\" 中的 " +
                            "[${if (pred.countNegated) "!" else ""}count~${pred.count}] ＝ \"某一个槽的堆叠数落在该区间\"" +
                            "（多槽位之间是或关系）；基岩 hasitem 的 quantity 是\"该栏所有槽位的总量\"、语义不同，" +
                            "已退化为\"该栏里有这个物品\"（不写 slot、不写 quantity），请自行核对"
                    )
                }
            } else {
                dropQuantity = true
                val orig = if (pred.countNegated) "!count=${pred.count}" else "count=${pred.count}"
                reminders.add(
                    "多槽位 + 具体数量，无法逐槽表达：原语义 Java \"$predicateRaw\" 中的 [$orig] ＝ " +
                        "\"某一个槽正好 ${pred.count} 个\"（多槽位之间是或关系）；基岩 hasitem 为且关系、" +
                        "无法展开\"或\"，已退化为\"该栏里有这个物品\"（不写 slot、不写 quantity）"
                )
            }
        }
        val obj = ExecCondSupport.hasitemFromJavaPredicate(pred, loc, slot, dropQuantity = dropQuantity)
            ?: return dropAnd(reminders, "物品谓词无法映射到 hasitem")

        // 极性：unless + 无数量约束 = "没有该物品" -> 基岩 quantity=0（execute需求 A5/C18）
        if (cond.unless && pred.count == null) {
            reminders.add("\"没有该物品\"已用基岩 quantity=0 表示（execute需求 A5/C18）")
            return foldEntity("if", target, "hasitem={$obj,quantity=0}")
        }
        val kw = if (cond.unless) "unless" else "if"
        return foldEntity(kw, target, "hasitem={$obj}")
    }

    /** `if|unless score <target> <obj> matches <range>` -> `if|unless entity <target>[scores={<obj>=<range>}]` */
    private fun convertScoreJavaToBedrock(cond: Seg.Cond, reminders: MutableList<String>): CondOut {
        val args = cond.args
        // 比较形式（= < <= > >= 目标 记分项）：Java/基岩语法一致，原样保留
        if (args.size != 4) return CondOut.Out(originalTokens(cond))
        val target = args[0]
        // 非选择器（玩家名/UUID/*）：基岩也支持 matches 形式，原样保留
        if (!target.startsWith("@")) return CondOut.Out(originalTokens(cond))
        val objective = args[1]
        val range = args[3]
        val kw = if (cond.unless) "unless" else "if"
        return CondOut.Out(listOf(kw, "entity", foldParam(target, "scores={$objective=$range}")))
    }

    /** `if|unless data entity <target> <nbt>` -> `if|unless entity <target>[hasitem=…]` */
    private fun convertDataJavaToBedrock(
        cond: Seg.Cond,
        exec: ExecState,
        reminders: MutableList<String>
    ): CondOut {
        val args = cond.args
        if (args.size < 3) return dropAnd(reminders, "data 参数不完整")
        if (args[0] != "entity") {
            return dropAnd(reminders, "if/unless data 的 block/storage 形式基岩 hasitem 无法承载")
        }
        val target = args[1]
        val nbt = args[2]
        if (target == "*") return dropAnd(reminders, "data 的目标 * (所有被追踪实体) 基岩无对应")
        val mapping = ExecCondSupport.nbtToHasitemItems(nbt, reminders)
        if (mapping.items.isEmpty()) return dropAnd(reminders, "NBT 主片段无法映射到基岩 hasitem")
        if (nbt.contains("SelectedItem") && exec.isPlayer != true) {
            if (exec.isPlayer == false) {
                reminders.add("执行者被判定为实体，SelectedItem（玩家主手）对实体无效；基岩侧仍写 location=slot.weapon.mainhand")
            } else {
                reminders.add("执行者身份无法确定（on / 未知选择器），SelectedItem 是否适用待确认")
            }
        }
        val hasitem = if (mapping.items.size == 1) {
            "{${mapping.items[0]}}"
        } else {
            "[${mapping.items.joinToString(",") { "{$it}" }}]"
        }
        // 空气法极性（execute需求 第十节 15）：基岩 hasitem={item=air} 本身表示"该槽是空的"，
        // 因此 Java 的 unless（该槽为空）要换成基岩 if；Java 的 if（该槽有物品）要换成基岩 unless。
        var kw = if (cond.unless) "unless" else "if"
        if (mapping.airExistence) {
            kw = if (cond.unless) "if" else "unless"
            reminders.add(
                "空气法极性：Java \"${if (cond.unless) "unless" else "if"} data …\" 表示该槽位" +
                    (if (cond.unless) "没有物品（空）" else "有物品") +
                    "，已转成基岩 \"$kw entity …[hasitem={item=air}]\"（基岩 item=air 表示该槽为空）"
            )
        }
        return foldEntity(kw, target, "hasitem=$hasitem")
    }

    /**
     * 把基岩 scores 的 `=!` 项转成 Java 条件 token（本次修正 D）。
     *
     * 口径（execute需求 第十节 9 / 目标选择器.txt:470）：`k=!v` = "分数存在 ∧ 分数 ≠ v"。
     *  - 单项：`if score @s k matches <全域> unless score @s k matches v`
     *    （未设置时 `<全域>` 判定失败 → 排除"分数不存在"的目标；`v` 可为区间如 `1..5`）
     *  - 多项：`unless entity @s[scores={…}]`，即 **NOT(全部条件同时成立)**（execute需求 第八节②）；
     *    这是线性的 if/unless 链唯一能表达"非合取"的写法。
     *
     * 前提：@s 指代正确（调用方需保证已有 `as <选择器>` 或当前执行者即目标）。
     */
    private fun negatedScoreConditionTokens(
        neg: List<String>,
        reminders: MutableList<String>
    ): List<String> {
        if (neg.size == 1) {
            val i = ExecCondSupport.indexOfTopLevel(neg[0], '=')
            val k = neg[0].substring(0, i).trim()
            val v = neg[0].substring(i + 1).trim()
            reminders.add(
                "scores 的 \"$k=!$v\" 按\"分数存在 ∧ ≠$v\"转换（execute需求 第十节 9）：" +
                    "先 `if score @s $k matches $SCORE_FULL_RANGE` 判存在，再 `unless score @s $k matches $v`"
            )
            return listOf(
                "if", "score", "@s", k, "matches", SCORE_FULL_RANGE,
                "unless", "score", "@s", k, "matches", v
            )
        }
        reminders.add(
            "scores 多项 \"!\"（${neg.joinToString(",")}）按 NOT(全部条件同时成立) 处理，" +
                "用 `unless entity @s[scores={…}]` 表达（execute需求 第八节②）"
        )
        return listOf("unless", "entity", "@s[scores={${neg.joinToString(",")}}]")
    }

    /**
     * 把条件折进 `if|unless entity <target>`。
     * target 是选择器 -> `if entity @…[...]`；target 是玩家名/UUID -> 补 `as <target>` 包装，
     * 让 `@s` 有指代（需求 3 的"必须加 as/@s 包装"）。
     */
    private fun foldEntity(kw: String, target: String, param: String): CondOut {
        return if (target.startsWith("@")) {
            CondOut.Out(listOf(kw, "entity", foldParam(target, param)))
        } else {
            CondOut.Out(listOf("as", target, kw, "entity", "@s[$param]"))
        }
    }

    // ---------------------- 基岩 -> Java ----------------------

    /**
     * `if|unless entity <sel>[hasitem=… / scores={x=!…}]` -> Java 条件。
     * hasitem 折成 `if/unless data|items`；scores 的 `=!` 用反极性的 entity 条件表达；
     * 无法转换的部分按需求十.5 舍弃（只丢那一个参数）并提醒。
     */
    private fun convertEntityBedrockToJava(cond: Seg.Cond, reminders: MutableList<String>): CondOut {
        if (cond.args.size < 1) return dropAnd(reminders, "entity 条件缺少目标")
        val selector = cond.args[0]
        if (!selector.startsWith("@") || !selector.contains('[')) return CondOut.Out(originalTokens(cond))
        val varName = selector.substringBefore('[')
        val paramsPart = selector.substringAfter('[').dropLast(1)
        val params = ExecCondSupport.splitTopLevel(paramsPart, ',')

        val kept = mutableListOf<String>()
        val extra = mutableListOf<List<String>>()
        for (p in params) {
            val name = p.substringBefore('=').trim()
            when (name) {
                "hasitem" -> {
                    val objs = parseHasitemValue(p.substringAfter("hasitem=").trim())
                    if (objs == null) {
                        reminders.add("hasitem 结构无法解析，已舍弃该参数")
                        continue
                    }
                    if (objs.size > 1 && cond.unless) {
                        // unless entity @s[hasitem=[{A},{B}]] = NOT(A∧B)，Java 无法用顺序条件表达
                        return dropAnd(reminders, "\"unless + hasitem 数组\"语义为 NOT(A∧B)，Java 无可对应写法")
                    }
                    for (obj in objs) {
                        // 条件目标用选择器本身（可能是 @a 等），不能一律折成 @s（修正 C：同一单一实体）
                        val conv = hasitemToJavaConditionTokens(obj, cond.unless, varName, reminders)
                        if (conv == null) reminders.add("hasitem（$obj）无法回译成 Java 条件，已舍弃该参数")
                        else extra.add(conv)
                    }
                }
                "scores" -> {
                    val content = p.substringAfter("scores=").trim().removeSurrounding("{", "}")
                    val pos = mutableListOf<String>()
                    val neg = mutableListOf<String>()
                    for (e in ExecCondSupport.splitTopLevel(content, ',')) {
                        val i = ExecCondSupport.indexOfTopLevel(e, '=')
                        if (i < 0) continue
                        val k = e.substring(0, i).trim()
                        val v = e.substring(i + 1).trim()
                        if (v.startsWith("!")) neg.add("$k=${v.substring(1)}") else pos.add("$k=$v")
                    }
                    if (pos.isNotEmpty()) kept.add("scores={${pos.joinToString(",")}}")
                    if (neg.isNotEmpty()) {
                        // 本次修正 D：单项 + 目标就是 @s + 正向条件 → 用"存在 ∧ ≠v"的完整 Java 形式；
                        // 其余（多项 / 目标是别的选择器 / unless 叠加）无法用顺序条件精确表达，退回反极性 entity 形式并提醒。
                        if (!cond.unless && neg.size == 1 && varName == "@s") {
                            extra.add(negatedScoreConditionTokens(neg, reminders))
                        } else {
                            if (neg.size >= 2) reminders.add(
                                "scores 多项 \"!\" 按 NOT(全部条件同时成立) 处理（execute需求 第八节②）"
                            )
                            if (varName != "@s") reminders.add(
                                "scores 条件的目标是 $varName（非单一执行者 @s），无法加\"分数存在\"判定，" +
                                    "改用反极性 entity 条件（语义可能包含\"分数不存在\"）"
                            )
                            if (cond.unless) reminders.add(
                                "unless 与 scores=! 叠加无法用顺序条件精确表达，保留反极性 entity 形式（语义近似）"
                            )
                            val kw = if (cond.unless) "if" else "unless"
                            extra.add(listOf(kw, "entity", "$varName[scores={${neg.joinToString(",")}}]"))
                        }
                    }
                }
                else -> kept.add(p)
            }
        }

        val result = mutableListOf<List<String>>()
        if (kept.isNotEmpty()) {
            result.add(listOf(if (cond.unless) "unless" else "if", "entity", "$varName[${kept.joinToString(",")}]"))
        }
        result.addAll(extra)
        return if (result.isEmpty()) dropAnd(reminders, "entity 条件的内容已全部舍弃") else CondOut.Out(result.flatten())
    }

    /**
     * 基岩 hasitem 对象 -> Java 条件 token。
     * 特定槽位 -> `if|unless items entity <target> <slots> <pred>`；
     * 不限槽位 -> `if|unless data entity @s {Inventory:[…]}`（data 只接受单一实体）。
     * quantity=0（或 0..）：本项目语义为"没有"，极性翻转用 unless。
     *
     * [target] 是条件原本的目标选择器：`@s`（执行者）或 `@a` 等。
     * 不能一律写成 `@s`，否则会把"@a 里的某人"错误折成"执行者本人"（修正 C：同一单一实体）。
     */
    private fun hasitemToJavaConditionTokens(
        obj: String,
        enclosingUnless: Boolean,
        target: String,
        reminders: MutableList<String>
    ): List<String>? {
        val h = ExecCondSupport.parseHasitemObject(obj)
        if (h.reason != null) {
            reminders.add("hasitem（$obj）无法解析：${h.reason}，已舍弃该条件项目（其余照常转换）")
            return null
        }
        val itemId = h.item!!

        // ---- 空气法：基岩 item=air 表示"该槽位是空的"（execute需求 第十节 15）----
        if (ExecCondSupport.stripNs(itemId) == "air") {
            return airHasitemToJavaTokens(obj, h, enclosingUnless, target, reminders)
        }

        // quantity=0.. = "当前条件项目不做过滤"（目标选择器.txt:607；2026-09-27 游戏内实测确认：
        // 有 5 颗钻石时同样匹配）-> 条件形同虚设，**去掉它**才是等价转换（不是"没有该物品"）。
        val quantity = h.quantity
        if (quantity == "0..") {
            reminders.add(
                "基岩 \"$obj\" 的 quantity=0.. 表示\"当前条件项目不做过滤\"（目标选择器.txt:607，" +
                    "2026-09-27 实测：有 5 颗钻石时同样匹配）——条件形同虚设，" +
                    "已按等价转换去掉该条件（去掉后不再限制目标，与基岩一致），请自行核对"
            )
            return emptyList()
        }
        // "没有该物品"判定：quantity=0
        val meansNone = quantity == "0"
        val negate = if (meansNone) !enclosingUnless else enclosingUnless
        val kw = if (negate) "unless" else "if"

        // slot 取反（`=!N`，目标选择器.txt:609 允许）：Java 的 items 没有槽位取反写法，
        // 按裁决用"该槽位没有它"表达（execute + unless/if 极性反转），并提醒这是近似写法。
        val negSlot = ExecCondSupport.negatedSingleSlot(h.slot)
        if (negSlot != null) {
            val negSlots = ExecCondSupport.hasitemToJavaSlots(h.location, negSlot.toString())
            if (negSlots != null) {
                val negKw = if (kw == "if") "unless" else "if"
                reminders.add(
                    "基岩 \"$obj\" 的 slot=!$negSlot 是\"槽位取反\"（目标选择器.txt:609）；" +
                        "Java 的 items 没有槽位取反写法，已按\"$negSlots 这一格没有该物品\"表达为 " +
                        "\"$negKw items entity … $negSlots …\"（近似写法，请自行核对）" +
                        // 2026-09-27 实测：单槽位 location 的槽位编号是 0，
                        // 所以 !0 会排除这唯一槽位（基岩里永不成立），而这里写的是"该槽没有它"。
                        if (ExecCondSupport.isSingleSlotItemLocation(h.location)) {
                            "；注意：location=${h.location} 是单槽位（编号 0），基岩的 !0 会把它排除（原条件永不成立），" +
                                "本工具写成\"该槽没有该物品\"，两者不同，请自行核对"
                        } else ""
                )
                return listOf(
                    negKw, "items", "entity", target, negSlots,
                    ExecCondSupport.javaPredicateForHasitem(h)
                )
            }
        }

        // 槽位源（execute需求 第十节 13）：Java items 的 <slots> 必填且支持星号通配，
        // 通配写 `hotbar.*` / `*`（不是"不填 slot"）。location 缺省（任意槽位）时用 `*`。
        val slotsExpr = ExecCondSupport.hasitemToJavaSlots(h.location, h.slot) ?: "*"

        // 具体槽位：quantity 即该槽堆叠数，可精确映射，保持原行为
        if (h.slot != null) {
            // slot 是范围/取反且没能走上面的精确分支时，槽位已被放开成通配，必须提醒（不许静默改义）
            if (h.slot.contains("..") || h.slot.contains("!")) {
                reminders.add(
                    "基岩 \"$obj\" 的 slot=${h.slot} 覆盖多个槽位；Java 的 items 没有槽位集合写法，" +
                        "已放开为 $slotsExpr 通配（近似，请自行核对）"
                )
            }
            return listOf(
                kw, "items", "entity", target, slotsExpr,
                ExecCondSupport.javaPredicateForHasitem(h)
            )
        }

        // 缺 slot 不等于"无法确定槽位"（execute需求 第十节 16）：slot 有默认值 slot=0..（目标选择器.txt:609），
        // location 是单槽位 / 带编号时唯一确定槽位 -> 照常精确转换，只给提醒，不许舍弃。
        val uniqueSlots = ExecCondSupport.resolveUniqueSlotJavaSlots(h.location, h.slot, reminders)
        if (uniqueSlots != null) {
            reminders.add(
                "基岩 \"$obj\" 没有写 slot，按默认 slot=0.. 处理（目标选择器.txt:609）；" +
                    "location=${h.location} 唯一确定槽位 -> Java $uniqueSlots，已照常精确转换"
            )
            return listOf(
                kw, "items", "entity", target, uniqueSlots,
                ExecCondSupport.javaPredicateForHasitem(h)
            )
        }

        // ---- 多槽位总量（未指定具体 slot）：Java items 无法表达"该栏所有槽位的总量" ----
        val id = ExecCondSupport.addNs(itemId)
        val pred: String
        when {
            meansNone -> {
                pred = id
                reminders.add(
                    "基岩 hasitem quantity=$quantity 表示\"没有该物品\"（execute需求 A5/C18），" +
                        "已转为 Java $kw items entity … $slotsExpr（* = 任意物品）"
                )
            }
            // 含 0 的区间（0..N）：保留上限，Java 侧写成 count~{min:0,max:N}
            quantity != null && quantity.startsWith("0..") -> {
                val max = quantity.substring(3)
                pred = "$id[count~{min:0,max:$max}]"
                reminders.add(
                    "含 0 区间近似：基岩 quantity=$quantity 是\"该栏所有槽位的总量在 0..$max\"（含 0，" +
                        "会选中没有该物品的目标；目标选择器.txt:603/607）；Java 的 items 只能测" +
                        "\"某一个槽的堆叠数\"，已写成 [count~{min:0,max:$max}]。" +
                        "注意（2026-09-27 实测，第六组）：Java 的 count~{min:0,…} **匹配不到空槽/完全没有该物品**，" +
                        "所以\"完全没有\"的目标会被漏掉，二者不等价、请自行核对"
                )
            }
            else -> {
                // 具体数量 / 其它区间 / =! ：Java 无法表达"该栏总量"，放开成"有该物品"
                pred = id
                reminders.add(
                    "多槽位总量近似：基岩 \"$obj\" 的 quantity=${quantity ?: "（默认 1..）"}" +
                        " 是\"该栏所有槽位的总量\"；Java 的 items 只能测单个槽的堆叠数、" +
                        "无法表达\"总量\"，已放开为\"有该物品\"（不写 count，槽位源用 $slotsExpr；" +
                        "execute需求 第十节 13），待确认，请自行核对"
                )
            }
        }
        if (quantity == null && h.location != null) {
            reminders.add(
                "基岩 \"$obj\" 不写 slot 表示\"location=${h.location} 的任意槽位只要有该物品即可\"" +
                    "（目标选择器.txt:609 默认 slot=0..）；Java 用 items + $slotsExpr 通配表达"
            )
        }
        if (h.location == null) {
            reminders.add(
                "基岩 \"$obj\" 没有写 location（= 目标的所有物品栏）；Java 的 items 用 `*` 当槽位源，" +
                    "而 `*` 只覆盖物品栏 36 格、**不含副手/装备那 5 格**（2026-09-27 实测第三组 J3 + B12），" +
                    "所以只会漏掉\"只在副手/装备里有该物品\"的目标，请自行核对"
            )
        }
        return listOf(kw, "items", "entity", target, slotsExpr, pred)
    }

    /**
     * 空气法（execute需求 第十节 15）：基岩 `hasitem={item=air,location=…,slot=…}` 表示"该槽是空的"，
     * 回译为 Java `unless items entity <目标> <槽位> *`（`*` = 任意物品，谓词.txt:5）。
     *
     * 缺 slot **不等于**"无法确定槽位"（execute需求 第十节 16）：slot 有默认值 slot=0..（目标选择器.txt:609），
     * 单槽位 location 缺 slot 即该唯一槽、带编号 location 缺 slot 即内嵌编号，照常转换；
     * 只有多槽位 location + item=air、或 slot 为区间 + item=air 时才舍弃并提醒
     * （"至少有一个空格"Java 的 items 问不出来）。
     * 极性：基岩 `if`（该槽为空）-> Java `unless items … *`；基岩 `unless`（该槽有物品）-> Java `if items … *`。
     */
    private fun airHasitemToJavaTokens(
        obj: String,
        h: ExecCondSupport.Hasitem,
        enclosingUnless: Boolean,
        target: String,
        reminders: MutableList<String>
    ): List<String>? {
        // slot 取反（`=!N`）：与"slot=N 为空"的极性相反（Java 没有槽位取反写法，只能这样近似）
        val negSlot = ExecCondSupport.negatedSingleSlot(h.slot)
        if (negSlot != null) {
            val negSlots = ExecCondSupport.hasitemToJavaSlots(h.location, negSlot.toString())
            if (negSlots != null) {
                val kw = if (enclosingUnless) "unless" else "if"
                reminders.add(
                    "基岩 \"$obj\" 的 slot=!$negSlot 是\"槽位取反\"（目标选择器.txt:609）；" +
                        "空气法按\"$negSlots 这一格不是空的\"近似表达为 " +
                        "\"$kw items entity … $negSlots *\"（* = 任意物品，近似写法，请自行核对）" +
                        // 2026-09-27 实测（第五组）：单槽位 location 的槽位编号是 0，!0 会排除这唯一槽位
                        if (ExecCondSupport.isSingleSlotItemLocation(h.location)) {
                            "；注意：location=${h.location} 是单槽位（编号 0），基岩的 !0 会把它排除（原条件永不成立），" +
                                "本工具写成\"该槽不是空的\"，两者不同，请自行核对"
                        } else ""
                )
                return listOf(kw, "items", "entity", target, negSlots, "*")
            }
        }
        val slotsExpr = ExecCondSupport.resolveUniqueSlotJavaSlots(h.location, h.slot, reminders)
        if (slotsExpr == null) {
            val why = when {
                h.location == null ->
                    "location 未指定（没有写 location 参数），item=air 找不到对应的单一槽位"
                ExecCondSupport.isMultiSlotItemLocation(h.location) ->
                    "location=${h.location} 是多槽位物品栏（slot 默认 slot=0.." +
                        (if (h.slot != null) "，此处为 slot=${h.slot}" else "") +
                        "）——item=air 表示\"该栏里至少有一个空格\"，" +
                        "但 Java 的 items 只能问\"某一格有没有物品\"、问不出\"存在空格\""
                h.slot != null && h.slot.contains("..") ->
                    "slot=${h.slot} 是区间、覆盖多个槽位，问不出\"存在空格\""
                else ->
                    "location=${h.location} 没有 Java 槽位对应写法"
            }
            reminders.add(
                "hasitem（$obj）用 item=air 表示\"该槽为空\"，但 $why，已舍弃该条件项目"
            )
            return null
        }
        if (h.slot == null) {
            reminders.add(
                "基岩 \"$obj\" 用 item=air 表示\"该槽为空\"，缺 slot 按默认 slot=0.. 处理" +
                    "（目标选择器.txt:609）；location=${h.location} 唯一确定槽位 -> Java $slotsExpr"
            )
        }
        val kw = if (enclosingUnless) "if" else "unless"
        reminders.add(
            "空气法：基岩 \"$obj\"（item=air）表示\"该槽位是空的\"，已回译为 Java " +
                "\"$kw items entity $target $slotsExpr *\"（* = 任意物品）。" +
                "可用性提醒（2026-09-27 实测第二/五组 + 目标选择器.txt:604\"物品栏中空气的数量始终是0个\"）：" +
                "一般物品栏/装备槽里 item=air **实测不匹配**（转出来的基岩命令会永不成立）；" +
                "只有 slot.saddle / 马的 slot.armor / slot.equippable 是文档明确允许 air 的（目标选择器.txt:744-748）。" +
                "按玩家决定保留该写法，请务必自行核实"
        )
        return listOf(kw, "items", "entity", target, slotsExpr, "*")
    }

    // ---------------------- 小工具 ----------------------

    /** 往选择器里追加参数；选择器必须已带 `[...]` 或形如 `@a`。 */
    private fun foldParam(selector: String, param: String): String {
        return if (selector.endsWith("]")) {
            selector.dropLast(1) + ",$param]"
        } else {
            "$selector[$param]"
        }
    }

    /** 解析 `hasitem=` 的值：`{…}` -> 1 个对象；`[{…},{…}]` -> 多个；失败返回 null。 */
    private fun parseHasitemValue(value: String): List<String>? {
        val v = value.trim()
        return when {
            v.startsWith("[") && v.endsWith("]") -> {
                ExecCondSupport.splitTopLevel(v.substring(1, v.length - 1), ',')
                    .map { it.trim().removeSurrounding("{", "}") }
                    .filter { it.isNotBlank() }
            }
            v.startsWith("{") && v.endsWith("}") -> listOf(v.substring(1, v.length - 1))
            else -> null
        }
    }

    /**
     * 这个 hasitem 对象是不是"Java 选择器表达不了、必须从选择器里拆出来单独处理"的情况：
     * - `item=air`：表示该槽为空（按玩家 2026-09-27 的决定保留这种写法，并提醒其可用性风险）；
     * - `quantity=0`：本项目语义 = "没有该物品" -> `unless items …`；
     * - `quantity=0..`：表示"不做过滤"（目标选择器.txt:607 + 游戏内实测），条件形同虚设 -> 去掉。
     */
    private fun isNegationLikeHasitem(obj: String): Boolean {
        val h = ExecCondSupport.parseHasitemObject(obj)
        val item = h.item ?: return false
        if (ExecCondSupport.stripNs(item) == "air") return true
        return h.quantity == "0" || h.quantity == "0.."
    }

    /**
     * 按空格切分，但尊重 {} / [] / "" / '' 内部的空格（NBT、选择器、JSON/文本参数要算作一个 token）。
     * 引号内的 `\x` 视为转义序列。括号或引号不闭合、以及 `[{]}` 这类交叉不匹配 -> 返回 null。
     */
    private fun tokenize(input: String): List<String>? {
        val tokens = mutableListOf<String>()
        val cur = StringBuilder()
        val closers = ArrayDeque<Char>()
        var quote: Char? = null
        var i = 0

        while (i < input.length) {
            val c = input[i]

            if (quote != null) {
                cur.append(c)
                when {
                    c == '\\' && i + 1 < input.length -> {
                        cur.append(input[i + 1])
                        i += 2
                        continue
                    }
                    c == quote -> quote = null
                }
                i++
                continue
            }

            when (c) {
                '"', '\'' -> {
                    quote = c
                    cur.append(c)
                }
                '{' -> {
                    closers.addLast('}')
                    cur.append(c)
                }
                '[' -> {
                    closers.addLast(']')
                    cur.append(c)
                }
                '}', ']' -> {
                    if (closers.isEmpty() || closers.removeLast() != c) return null
                    cur.append(c)
                }
                ' ', '\t' -> {
                    if (cur.isNotEmpty()) {
                        tokens.add(cur.toString())
                        cur.clear()
                    }
                }
                else -> cur.append(c)
            }
            i++
        }

        if (quote != null || closers.isNotEmpty()) return null
        if (cur.isNotEmpty()) tokens.add(cur.toString())
        return tokens
    }
}
