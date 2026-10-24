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

    fun evaluate(
        conditionText: String,
        entity: GridEntity,
        all: List<GridEntity>,
        target: VersionSpec? = null
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
                "items", "block", "blocks", "biome", "dimension", "loaded",
                "function", "score", "stopwatch", "slots" -> {
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
