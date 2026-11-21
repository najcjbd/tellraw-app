package com.tellraw.app.util

import android.content.Context
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.tellraw.app.model.FeatureGate
import com.tellraw.app.model.SelectorType
import com.tellraw.app.model.VersionSpec

/**
 * 将“基岩 conditional rawtext”转换成 Java 的多条 tellraw（第一匹配）。
 * 只识别：rawtext -> translate("...%%n...") + with.rawtext[selector, content...]。
 */
object ConditionalRawtextConverter {

    data class Row(
        val selector: String,
        val params: String,
        val contentJson: String,
        val key: String = "",
        val args: List<JsonElement> = emptyList()
    )

    fun parse(json: String): List<Row>? {
        val root = runCatching { JsonParser.parseString(json) }.getOrNull() ?: return null
        val rawtext = root.takeIf { it.isJsonObject }
            ?.asJsonObject?.getAsJsonArray("rawtext") ?: return null
        val rows = mutableListOf<Row>()
        for (el in rawtext) {
            val row = el.takeIf { it.isJsonObject }?.asJsonObject ?: return null
            val key = row.get("translate")?.takeIf { it.isJsonPrimitive }?.asString ?: return null
            if ("%%" !in key) return null
            val with = row.getAsJsonObject("with") ?: return null
            val args = with.getAsJsonArray("rawtext") ?: return null
            if (args.size() < 2) return null
            val selector = args[0].takeIf { it.isJsonObject }
                ?.asJsonObject?.get("selector")?.takeIf { it.isJsonPrimitive }?.asString
                ?: return null
            val params = if (selector.contains('[') && selector.endsWith("]")) {
                selector.substringAfter('[').dropLast(1)
            } else ""
            val content = JsonArray()
            for (i in 1 until args.size()) content.add(args[i])
            rows.add(Row(selector, params, content.toString(), key, args.toList()))
        }
        return rows.takeIf { it.isNotEmpty() }
    }

    fun toJava(
        rows: List<Row>,
        prefix: String = "",
        baseSelector: String = "@a",
        context: Context? = null,
        nbtSyntax: VersionDiff.NbtSyntax = VersionDiff.NbtSyntax.MODERN,
        target: VersionSpec? = null,
        warnings: MutableList<String>? = null
    ): List<String> {
        val earlier = mutableListOf<String>()
        val out = mutableListOf<String>()
        val base = prefix.trim().takeIf { it.isNotEmpty() } ?: "execute"
        val executeAvailable = FeatureGate.isAvailable(FeatureGate.Feature.EXECUTE_MODERN, target)
        rows.forEachIndexed { index, row ->
            if (!row.selector.startsWith("@s")) {
                warnings?.add(
                    "conditional rawtext 的条件选择器是 ${row.selector.substringBefore('[')}，不是 @s；已把方括号参数按收件人条件近似处理"
                )
            }
            val raw = appendSelectorParams(baseSelector.ifBlank { "@a" }, row.params)
            val jContent = javaContent(row, context, nbtSyntax, target, warnings) ?: row.contentJson
            if (!executeAvailable) {
                if (earlier.isNotEmpty() || row.params.isBlank() && index < rows.lastIndex) {
                    warnings?.add("目标版本不支持 execute；conditional rawtext 的第一匹配/排除无法表达")
                }
                out.add("tellraw ${filterJava(raw, context, nbtSyntax, target, warnings)} $jContent")
                if (row.params.isBlank() && index < rows.lastIndex) {
                    warnings?.add("无条件条件行之后的 ${rows.size - index - 1} 行永远不会命中，已跳过")
                    return@toJava out
                }
                return@forEachIndexed
            }
            val negation = ExecuteConverter.bedrockSelectorNegation(
                raw, warnings ?: mutableListOf(), preferExecute = true, nbtSyntax = nbtSyntax
            )
            val inner: List<String>
            if (negation != null) {
                val (asSelector, conditions) = negation
                val filtered = filterJava(asSelector, context, nbtSyntax, target, warnings)
                inner = if (conditions.isEmpty()) listOf("as", filtered)
                else listOf("as", filtered) + conditions
            } else {
                inner = listOf("as", filterJava(raw, context, nbtSyntax, target, warnings))
            }
            val exclusions = exclusionsForJava(earlier, context, nbtSyntax, target, warnings)
            out.add("$base ${inner.joinToString(" ")}$exclusions run tellraw @s $jContent")
            if (row.params.isBlank()) {
                if (index < rows.lastIndex) {
                    warnings?.add("无条件条件行之后的 ${rows.size - index - 1} 行永远不会命中，已跳过")
                    return@toJava out
                }
            } else {
                earlier.add(row.params)
            }
        }
        return out
    }

    /**
     * 把 `%%n` 按 with 参数展开，再交给 Java 互转。
     * 例：`%%2『传送主城』` + [selector, {"text":"A"}] -> A + 『传送主城』。
     */
    private fun javaContent(
        row: Row,
        context: Context?,
        nbtSyntax: VersionDiff.NbtSyntax,
        target: VersionSpec?,
        warnings: MutableList<String>?
    ): String? {
        if (row.key.isBlank() || row.args.isEmpty()) return null
        if (Regex("%%(?![0-9])").containsMatchIn(row.key)) {
            warnings?.add("conditional rawtext 的 translate 含非数字 %% 参数，已保留原参数内容，可能有字面文本丢失")
            return row.contentJson
        }
        val token = Regex("%%(\\d+)")
        if (!token.containsMatchIn(row.key)) return row.contentJson

        val expanded = JsonArray()
        fun addComponent(el: JsonElement) {
            val nested = el.takeIf { it.isJsonObject }
                ?.asJsonObject?.getAsJsonArray("rawtext")
            if (nested != null) nested.forEach { expanded.add(it.deepCopy()) }
            else expanded.add(el.deepCopy())
        }
        var cursor = 0
        token.findAll(row.key).forEach { m ->
            val literal = row.key.substring(cursor, m.range.first)
            if (literal.isNotEmpty()) {
                expanded.add(JsonObject().apply { addProperty("text", literal) })
            }
            val index = m.groupValues[1].toIntOrNull()
            val arg = index?.let { row.args.getOrNull(it - 1) }
            if (arg == null) {
                warnings?.add("conditional rawtext 的 %%$index 没有对应参数，已保留原参数内容")
                return row.contentJson
            }
            addComponent(arg)
            cursor = m.range.last + 1
        }
        if (cursor < row.key.length) {
            val tail = row.key.substring(cursor)
            if (tail.isNotEmpty()) expanded.add(JsonObject().apply { addProperty("text", tail) })
        }
        if (expanded.size() == 0) return row.contentJson

        val raw = JsonObject().apply { add("rawtext", expanded) }.toString()
        val parsed = runCatching {
            CommandInputParser.parse(raw, context, nbtSyntax, target)
        }.getOrNull()
        if (parsed == null) {
            warnings?.add("conditional rawtext 内容无法转成 Java，已保留原参数内容")
            return row.contentJson
        }
        warnings?.addAll(parsed.warnings)
        return parsed.javaJson
    }

    private fun exclusionsForJava(
        earlier: List<String>,
        context: Context?,
        nbtSyntax: VersionDiff.NbtSyntax,
        target: VersionSpec?,
        warnings: MutableList<String>?
    ): String = earlier.mapNotNull { params ->
        if (params.contains("=!") || params.contains("quantity=0")) {
            warnings?.add("Java 选择器无法精确表达反选条件「$params」，该排除已省略")
            return@mapNotNull null
        }
        val filtered = filterJava("@s[$params]", context, nbtSyntax, target, warnings)
        val inner = when {
            filtered.startsWith("@s[") && filtered.endsWith("]") ->
                filtered.substring(3, filtered.length - 1)
            filtered == "@s" -> ""
            else -> filtered
        }
        if (inner.isBlank()) {
            warnings?.add("Java 选择器无法表达条件「$params」，该排除已省略")
            null
        } else {
            " unless entity @s[$inner]"
        }
    }.joinToString("")

    private fun filterJava(
        selector: String,
        context: Context?,
        nbtSyntax: VersionDiff.NbtSyntax,
        target: VersionSpec?,
        warnings: MutableList<String>?
    ): String {
        if (context == null) return selector
        val (filtered, _, reminders) = SelectorConverter.filterSelectorParameters(
            selector, SelectorType.JAVA, context, nbtSyntax, target
        )
        warnings?.addAll(reminders)
        return filtered
    }

    private fun appendSelectorParams(base: String, params: String): String {
        val p = params.trim().trimStart(',').trimEnd(',')
        if (p.isBlank()) return base
        val trimmed = base.trim()
        if (trimmed.isEmpty()) return "@a[$p]"
        val open = trimmed.indexOf('[')
        if (open < 0 || !trimmed.endsWith("]")) return "$trimmed[$p]"
        val inner = trimmed.substring(open + 1, trimmed.lastIndexOf(']'))
        val joiner = if (inner.isBlank()) "" else ","
        return trimmed.substring(0, open + 1) + inner + joiner + p + "]"
    }
}
