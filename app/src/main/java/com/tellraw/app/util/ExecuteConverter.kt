package com.tellraw.app.util

/**
 * execute 前置命令解析器（独立模块，不依赖选择器/文本/组件转换逻辑）。
 *
 * 本步骤只做三件事：tokenize、识别每个子命令的参数边界、决定"保留"还是"放弃"。
 * 条件子命令（if/unless）本步骤一律原样保留，不做互转——所以必须先在这里把边界切准，
 * 后续步骤才能拿到确定的 token 序列去做条件互转；若在此处切错，后续转换会连带出错。
 */
object ExecuteConverter {

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

    // 已知条件类型；不在此集合内即视为未知条件并放弃（链已断开，不做猜测）。
    private val KNOWN_CONDITIONS = setOf(
        "biome", "block", "blocks", "data", "dimension", "entity", "function",
        "items", "loaded", "predicate", "score", "slots", "stopwatch"
    )

    /**
     * 解析 execute 前置命令。
     *
     * @param prefix    用户在前置框输入的内容，可能带或不带开头的 "execute"
     * @param reminders 放弃/异常原因（中文）写回此列表
     * @return 保留下来的子命令 token 序列；不是合法 execute 前缀返回 null。
     *         `execute`（只有它）返回空列表，表示"合法但没有可保留的子命令"；
     *         空输入同样返回 null，因为空串并不是一个合法的 execute 前缀。
     */
    fun parseExecutePrefix(prefix: String, reminders: MutableList<String>): List<String>? {
        val tokens = tokenize(prefix) ?: return null
        if (tokens.isEmpty()) return null

        var idx: Int
        if (tokens[0] == "execute") {
            // 开头的 execute 只是命令头，不算子命令
            idx = 1
        } else if (tokens[0] in MODIFIERS) {
            // 允许省略开头的 execute：用户可能只写了 `as @a ...` 这样的片段，语法上等价，直接复用
            idx = 0
        } else {
            // 首词既不是 execute 也不是已知修饰子命令（例如 `if score ...` 缺 execute）-> 整条不是合法前缀
            return null
        }

        val kept = mutableListOf<String>()
        while (idx < tokens.size) {
            val name = tokens[idx]

            // run 是结束标记：不保留（调用方会重新拼 `run tellraw ...`），其后的参数属于被替换掉的命令，本次忽略
            if (name == "run") break

            if (name in MODIFIERS) {
                val span = modifierSpan(tokens, idx)
                if (span != null) {
                    for (k in 0 until span) kept.add(tokens[idx + k])
                    idx += span
                    continue
                }
                // 已知但参数不完整：放弃它，再从后面能识别的下一个已知关键字继续
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
                        // 本次不做条件互转，整段（if/unless + 条件名 + 参数）原样保留
                        for (k in 0 until 2 + argCount) kept.add(tokens[idx + k])
                        idx += 2 + argCount
                        continue
                    }
                    val resume = abandonAndResume(
                        tokens, idx, "条件子命令 \"$name $condName\" 参数不完整，已放弃", reminders
                    ) ?: break
                    idx = resume
                    continue
                }
                // 缺少条件类型，或条件类型未知：放弃这条 if/unless，再尝试从后续关键字继续
                val reason = if (condName == null) {
                    "条件子命令 \"$name\" 后缺少条件类型，已放弃"
                } else {
                    "未知的条件类型 \"$condName\"（在 $name 之后），已放弃"
                }
                val resume = abandonAndResume(tokens, idx, reason, reminders) ?: break
                idx = resume
                continue
            }

            // 未知子命令：无法确定它到底带几个参数，只能把它后面"看起来像它参数"的 token
            // 一直丢到下一个已知关键字为止（下一个已知关键字归属下一条子命令）。
            val resume = abandonAndResume(
                tokens, idx, "未知的子命令 \"$name\"，已放弃", reminders
            ) ?: break
            idx = resume
        }

        return kept
    }

    /**
     * 放弃 idx 处（含）的子命令后，寻找其后第一个可继续解析的已知关键字。
     *
     * 跳过规则：把 idx+1 起、直到该关键字之前的所有 token 都当作被放弃子命令的参数丢弃
     * （找不到关键字则丢到末尾）。返回 null 表示后缀里再没有已知关键字，调用方应结束解析；
     * 若找到的是 "run"，调用方把它交回主循环即可由 `run` 分支停止解析。
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
     * 计算修饰子命令占用的 token 数（含关键字本身）。
     * 返回 null 表示参数不完整，调用方据此放弃。
     */
    private fun modifierSpan(tokens: List<String>, idx: Int): Int? {
        val rest = tokens.size - idx - 1
        // 取第 i 个参数（0 起），越界返回 null
        fun arg(i: Int): String? = if (idx + 1 + i < tokens.size) tokens[idx + 1 + i] else null

        return when (tokens[idx]) {
            // 单参数修饰：参数位置出现关键字即视为参数缺失
            "align", "anchored", "as", "at", "in", "on", "summon" ->
                if (rest >= 1 && arg(0) !in KEYWORDS) 2 else null

            // rotated <yaw> <pitch> 与 rotated as <targets> 都是 3 个 token
            "rotated" -> when {
                rest < 1 -> null
                arg(0) == "as" -> if (rest >= 2) 3 else null
                arg(0) in KEYWORDS -> null
                else -> if (rest >= 2) 3 else null
            }

            // facing <pos>(x y z) 与 facing entity <targets> <anchor> 都是 4 个 token
            "facing" -> when {
                rest < 1 -> null
                arg(0) == "entity" -> if (rest >= 3) 4 else null
                arg(0) in KEYWORDS -> null
                else -> if (rest >= 3) 4 else null
            }

            // positioned <pos>(x y z)=4；positioned as <targets> / over <heightmap> = 3
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
     * 计算条件子命令除 "if/unless" 和条件名之外还需的参数个数。
     * 返回 null 表示参数不完整（含 source token 缺失的情况）。
     */
    private fun conditionArgCount(tokens: List<String>, idx: Int, condName: String): Int? {
        // source token 位于 idx+2；只有 data/items/slots 需要看它来决定 target 是 1 个 token 还是位置（3 个）
        val source = if (idx + 2 < tokens.size) tokens[idx + 2] else null

        return when (condName) {
            // 坐标类：<pos> 展开成 3 个 token
            "biome" -> POSITION_TOKENS + 1
            "block" -> POSITION_TOKENS + 1
            "blocks" -> POSITION_TOKENS * 3 + 1
            "loaded" -> POSITION_TOKENS
            "dimension", "entity", "function", "predicate", "stopwatch" -> 1
            "score" -> 5
            // data <source> <target> <path>：source=block 时 target 是位置(3)，否则 1
            "data" -> when (source) {
                null -> null
                "block" -> 1 + POSITION_TOKENS + 1
                else -> 3
            }
            // items/slots：source=block 时 target 是位置(3)，否则 1
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

    /**
     * 按空格切分，但尊重 {} / [] / "" / '' 内部的空格（NBT、选择器、JSON/文本参数要算作一个 token）。
     * 引号内的 `\x` 视为转义序列：转义字符本身不闭合字符串（`\"`、`\'`、`\\` 都按此处理），
     * 返回的 token 保留原文（反斜杠与引号一律照抄）。
     * 括号或引号不闭合、以及 `[{]}` 这类交叉不匹配 -> 整条前缀非法，返回 null（无法可靠切分就不要猜）。
     */
    private fun tokenize(input: String): List<String>? {
        val tokens = mutableListOf<String>()
        val cur = StringBuilder()
        // 用栈记录期望的闭合符，能顺带查出 `[{]}` 这类交叉不匹配
        val closers = ArrayDeque<Char>()
        // 当前所处的引号类型（null 表示不在引号内）；" 与 ' 分别独立成对
        var quote: Char? = null
        var i = 0

        while (i < input.length) {
            val c = input[i]

            if (quote != null) {
                cur.append(c)
                when {
                    // 反斜杠转义：连同下一个字符一起吞掉，避免把 \" / \' / \\ 当成字符串结束
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
