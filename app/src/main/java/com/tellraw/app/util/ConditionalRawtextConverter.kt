package com.tellraw.app.util

import android.content.Context
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.tellraw.app.model.SelectorType
import com.tellraw.app.model.VersionSpec

/**
 * 将“基岩 conditional rawtext”转换成 Java 的多条 tellraw（第一匹配）。
 * 只识别：rawtext -> translate("...%%n...") + with.rawtext[selector, content...]。
 */
object ConditionalRawtextConverter {

    data class Row(val selector: String, val params: String, val contentJson: String)

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
            val params = if (selector.startsWith("@s[") && selector.endsWith("]")) {
                selector.substring(3, selector.length - 1)
            } else ""
            val content = JsonArray()
            for (i in 1 until args.size()) content.add(args[i])
            rows.add(Row(selector, params, content.toString()))
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
        for (row in rows) {
            val raw = appendSelectorParams(baseSelector.ifBlank { "@a" }, row.params)
            val negation = ExecuteConverter.bedrockSelectorNegation(
                raw, warnings ?: mutableListOf(), preferExecute = true, nbtSyntax = nbtSyntax
            )
            val inner: List<String>
            val tellrawTarget: String
            if (negation != null) {
                val (asSelector, conditions) = negation
                val filtered = filterJava(asSelector, context, nbtSyntax, target, warnings)
                if (conditions.isEmpty()) {
                    inner = listOf("as", filtered)
                    tellrawTarget = "@s"
                } else {
                    inner = listOf("as", filtered) + conditions
                    tellrawTarget = "@s"
                }
            } else {
                val filtered = filterJava(raw, context, nbtSyntax, target, warnings)
                inner = listOf("as", filtered)
                tellrawTarget = "@s"
            }
            val exclusions = exclusionsForJava(earlier, context, nbtSyntax, target, warnings)
            out.add("$base ${inner.joinToString(" ")}$exclusions run tellraw $tellrawTarget ${row.contentJson}")
            if (row.params.isNotBlank()) earlier.add(row.params)
        }
        return out
    }

    private fun exclusionsForJava(
        earlier: List<String>,
        context: Context?,
        nbtSyntax: VersionDiff.NbtSyntax,
        target: VersionSpec?,
        warnings: MutableList<String>?
    ): String = earlier.mapNotNull { params ->
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
