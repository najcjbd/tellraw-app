package com.tellraw.app.model

/**
 * SNBT 的轻量解析/子集匹配（网格 nbt= 求值用）。
 *
 * 支持：compound、list、byte/short/int/long/float/double、带引号字符串、true/false、类型化数组（[I;…] 等）。
 * 匹配按 Minecraft 的"查询是实际值的子集"语义：
 *  - compound：查询里的每个键都必须在实际值里存在且匹配；
 *  - list：查询里的每个元素都必须能在实际列表里找到对应项（顺序无关）；
 *  - 数字按数值比较（0b == 0 == 0.0）；布尔与 0/1 互通。
 */
object SnbtLite {

    sealed class Value {
        data class Num(val v: Double) : Value()
        data class Str(val v: String) : Value()
        data class Bool(val v: Boolean) : Value()
        data class ListV(val items: List<Value>) : Value()
        data class Compound(val map: Map<String, Value>) : Value()
    }

    /** 解析一个 `{...}` compound；失败返回 null。 */
    fun parseCompound(text: String): Map<String, Value>? {
        val p = Parser(text.trim())
        return try {
            val v = p.value() as? Value.Compound ?: return null
            v.map
        } catch (e: Exception) {
            null
        }
    }

    /** 解析任意 SNBT 值（compound/list/标量）；失败返回 null。 */
    fun parseValue(text: String): Value? = try {
        Parser(text.trim()).value()
    } catch (e: Exception) {
        null
    }

    /** 解析 compound 的**内容**（不带外层花括号）。 */
    fun parseCompoundBody(text: String): Map<String, Value>? {
        val t = text.trim()
        return parseCompound(if (t.startsWith("{")) t else "{$t}")
    }

    fun matches(query: Value, actual: Value): Boolean {
        return when (query) {
        is Value.Compound -> {
            val a = actual as? Value.Compound ?: return false
            query.map.all { (k, v) ->
                a.map[k]?.let { actualValue ->
                    if (k == "id") matchesId(v, actualValue) else matches(v, actualValue)
                } == true
            }
        }
        is Value.ListV -> {
            val a = actual as? Value.ListV ?: return false
            val used = BooleanArray(a.items.size)
            query.items.all { q ->
                var found = false
                for (i in a.items.indices) {
                    if (!used[i] && matches(q, a.items[i])) {
                        used[i] = true; found = true; break
                    }
                }
                found
            }
        }
        is Value.Num -> when (actual) {
            is Value.Num -> kotlin.math.abs(query.v - actual.v) < 1e-9
            is Value.Bool -> query.v != 0.0 == actual.v
            else -> false
        }
        is Value.Bool -> when (actual) {
            is Value.Bool -> query.v == actual.v
            is Value.Num -> (query.v == (actual.v != 0.0))
            else -> false
        }
        is Value.Str -> actual is Value.Str && sameText(query.v, actual.v)
    }
    }

    /** 物品/实体 ID：`diamond_sword` 与 `minecraft:diamond_sword` 等价。 */
    private fun matchesId(a: Value, b: Value): Boolean {
        if (a !is Value.Str || b !is Value.Str) return matches(a, b)
        // 同一实体的 Java/基岩 ID 视为相同（如 zombified_piglin / zombie_pigman）
        if (EntityIdAliases.isKnown(a.v) || EntityIdAliases.isKnown(b.v)) {
            return EntityIdAliases.canonical(a.v) == EntityIdAliases.canonical(b.v)
        }
        fun norm(x: String) = if (x.contains(':')) x else "minecraft:$x"
        return norm(a.v) == norm(b.v)
    }

    /** 把 compound 写回 SNBT 文本（多键用逗号分隔）。 */
    fun serializeCompound(map: Map<String, Value>): String =
        map.entries.joinToString(",") { (k, v) -> "${keyText(k)}:${serialize(v)}" }

    fun serialize(v: Value): String = when (v) {
        is Value.Num -> {
            if (v.v == v.v.toLong().toDouble()) v.v.toLong().toString()
            else v.v.toString()
        }
        is Value.Str -> "\"" + v.v.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
        is Value.Bool -> if (v.v) "1b" else "0b"
        is Value.ListV -> "[" + v.items.joinToString(",") { serialize(it) } + "]"
        is Value.Compound -> "{" + serializeCompound(v.map) + "}"
    }

    private fun keyText(k: String): String =
        if (k.matches(Regex("[A-Za-z0-9_\\-]+"))) k else "\"" + k.replace("\"", "\\\"") + "\""

    /** 文本比较：兼容 CustomName 的 JSON 形式（{"text":"Bob"}）与普通字符串。 */
    private fun sameText(a: String, b: String): Boolean {
        if (a == b) return true
        return plainText(a) == plainText(b)
    }

    fun plainText(s: String): String {
        var t = s.trim()
        if ((t.startsWith("\"") && t.endsWith("\"")) || (t.startsWith("'") && t.endsWith("'"))) {
            t = t.substring(1, t.length - 1)
        }
        if (t.startsWith("{") && t.endsWith("}")) {
            Regex("\"text\"\\s*:\\s*\"([^\"]*)\"").find(t)?.let { return it.groupValues[1] }
        }
        return t
    }

    private class Parser(private val s: String) {
        private var i = 0

        fun value(): Value {
            ws()
            return when {
                i >= s.length -> throw IllegalArgumentException("eof")
                s[i] == '{' -> compound()
                s[i] == '[' -> list()
                s[i] == '"' || s[i] == '\'' -> Value.Str(quoted())
                else -> bare()
            }
        }

        private fun compound(): Value.Compound {
            expect('{')
            val map = linkedMapOf<String, Value>()
            ws()
            if (peek() == '}') { i++; return Value.Compound(map) }
            while (true) {
                ws()
                val key = if (peek() == '"' || peek() == '\'') quoted() else rawUntil(':', '=', ',', '}')
                ws()
                if (peek() == ':' || peek() == '=') i++ else throw IllegalArgumentException("key")
                val v = value()
                map[key] = v
                ws()
                when (peek()) {
                    ',' -> { i++; continue }
                    '}' -> { i++; break }
                    else -> throw IllegalArgumentException("compound")
                }
            }
            return Value.Compound(map)
        }

        private fun list(): Value.ListV {
            expect('[')
            ws()
            // 类型化数组：[I;1,2] / [B;…] / [L;…]
            if (i + 1 < s.length && s[i].uppercaseChar() in setOf('I', 'B', 'L') && s[i + 1] == ';') {
                i += 2
            }
            val items = mutableListOf<Value>()
            ws()
            if (peek() == ']') { i++; return Value.ListV(items) }
            while (true) {
                items.add(value())
                ws()
                when (peek()) {
                    ',' -> { i++; continue }
                    ']' -> { i++; break }
                    else -> throw IllegalArgumentException("list")
                }
            }
            return Value.ListV(items)
        }

        private fun bare(): Value {
            val start = i
            while (i < s.length && s[i] !in charArrayOf(',', '}', ']', ':')) i++
            val token = s.substring(start, i).trim()
            if (token.isEmpty()) throw IllegalArgumentException("empty")
            return when {
                token == "true" -> Value.Bool(true)
                token == "false" -> Value.Bool(false)
                else -> {
                    val m = Regex("^([-+]?(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][-+]?\\d+)?)([bBsSlLfFdD])?$")
                        .find(token)
                    if (m != null) Value.Num(m.groupValues[1].toDouble())
                    else Value.Str(unquote(token))
                }
            }
        }

        private fun quoted(): String {
            val q = s[i]; i++
            val sb = StringBuilder()
            while (i < s.length) {
                val c = s[i]
                if (c == '\\' && i + 1 < s.length) {
                    sb.append(s[i + 1]); i += 2
                } else if (c == q) {
                    i++; return sb.toString()
                } else {
                    sb.append(c); i++
                }
            }
            return sb.toString()
        }

        private fun unquote(t: String): String =
            t.removeSurrounding("\"").removeSurrounding("'")

        private fun rawUntil(vararg stop: Char): String {
            val start = i
            while (i < s.length && s[i] !in stop) i++
            return s.substring(start, i).trim()
        }

        private fun expect(c: Char) {
            ws()
            if (i >= s.length || s[i] != c) throw IllegalArgumentException("expected $c")
            i++
        }

        private fun peek(): Char = if (i < s.length) s[i] else '\u0000'

        private fun ws() {
            while (i < s.length && s[i].isWhitespace()) i++
        }
    }
}
