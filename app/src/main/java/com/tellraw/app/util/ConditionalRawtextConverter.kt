package com.tellraw.app.util

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser

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

    fun toJava(rows: List<Row>, prefix: String = ""): List<String> {
        val earlier = mutableListOf<String>()
        val out = mutableListOf<String>()
        for (row in rows) {
            val target = if (row.params.isBlank()) "@a" else "@a[${row.params}]"
            val exclusions = earlier.joinToString("") { " unless entity @s[$it]" }
            val base = if (prefix.isBlank()) "execute" else prefix
            out.add("$base as $target$exclusions run tellraw @s ${row.contentJson}")
            if (row.params.isNotBlank()) earlier.add(row.params)
        }
        return out
    }
}
