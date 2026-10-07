package com.tellraw.app.util

import android.content.Context
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.tellraw.app.model.SelectorType
import com.tellraw.app.util.TextComponentHelper.ComponentType
import com.tellraw.app.util.TextComponentHelper.SubComponent
import com.tellraw.app.util.TextComponentHelper.SubComponentType
import com.tellraw.app.util.TextComponentHelper.TextComponent

/**
 * 识别"整条命令 / 裸 JSON"输入并转成双版本的 tellraw（或 execute + tellraw）。
 *
 * 支持的输入形态（可带 `execute … run` 前缀）：
 *  - 基岩 rawtext：`{"rawtext":[{"text":"a"}]}`
 *  - Java 文本组件：`{"text":"a"}` / `[{"text":"a"}]`
 *  - 单个 tellraw：`tellraw @a {"text":"a"}`
 *  - execute + tellraw：`execute as @a run tellraw @s {"text":"a"}`
 *
 * 处理：拆出 execute 前缀 / tellraw 目标 / JSON；把 JSON 解码成内部文本组件模型；
 * 再用既有转换器产出两版 JSON，并对目标选择器做双版本过滤（混合参数走 convertForMixedMode）。
 *
 * 纯结构转换，不依赖 Android（[context] 为 null 时跳过选择器过滤，便于单测）。
 */
object CommandInputParser {

    enum class SourceEdition { JAVA, BEDROCK, UNKNOWN }

    data class Result(
        val executePrefix: String?,      // "execute …"（不含 run）；没有则 null
        val selector: String?,           // tellraw 的目标选择器；裸 JSON 时为 null
        val javaSelector: String?,       // 过滤后的 Java 选择器（selector 为 null 时也是 null）
        val bedrockSelector: String?,
        val javaJson: String,
        val bedrockJson: String,
        val sourceEdition: SourceEdition,
        val warnings: List<String>
    )

    fun parse(
        input: String,
        context: Context? = null,
        nbtSyntax: VersionDiff.NbtSyntax = VersionDiff.NbtSyntax.MODERN
    ): Result? {
        val warnings = mutableListOf<String>()
        var s = input.trim()
        if (s.isEmpty()) return null

        // 1) execute 前缀（保留；只认到最后一个顶层 " run "）
        var prefix: String? = null
        if (s == "execute" || s.startsWith("execute ") || s.startsWith("execute\t")) {
            val runIdx = findTopLevelRunIndex(s) ?: return null
            prefix = s.substring(0, runIdx).trim()
            s = s.substring(runIdx + 3).trim()   // 跳过 "run"
            if (prefix.isEmpty()) return null
        }

        // 2) tellraw <selector> <json>  或  裸 JSON
        var selector: String? = null
        if (s == "tellraw" || s.startsWith("tellraw ") || s.startsWith("tellraw\t")) {
            val rest = s.removePrefix("tellraw").trimStart()
            val sp = firstTopLevelSpace(rest)
            if (sp <= 0) return null
            selector = rest.substring(0, sp).trim()
            s = rest.substring(sp + 1).trim()
        }
        if (s.isEmpty()) return null

        // 3) 解析 JSON
        val root: JsonElement = try {
            JsonParser.parseString(s)
        } catch (e: Exception) {
            return null
        }
        if (root.isJsonNull || (!root.isJsonObject && !root.isJsonArray)) return null

        val (components, edition) = decodeRoot(root, warnings) ?: return null
        if (components.isEmpty()) return null

        // 4) 生成两版 JSON（复用既有转换器）
        val javaJson = TextComponentHelper.convertToJavaJson(components, context = context, warnings = warnings)
        val bedrockJson = TextComponentHelper.convertToBedrockJson(components, context = context, warnings = warnings)

        // 5) 目标选择器：双版本过滤（混合参数时用混合模式）
        var javaSel: String? = null
        var bedrockSel: String? = null
        if (selector != null) {
            if (context == null) {
                javaSel = selector
                bedrockSel = selector
            } else {
                val mixed = SelectorConverter.convertForMixedMode(selector, context, warnings, nbtSyntax)
                val mixedWorked = mixed.first != selector || mixed.second != selector
                javaSel = if (mixedWorked) mixed.first
                    else SelectorConverter.filterSelectorParameters(selector, SelectorType.JAVA, context, nbtSyntax).first
                bedrockSel = if (mixedWorked) mixed.second
                    else SelectorConverter.filterSelectorParameters(selector, SelectorType.BEDROCK, context, nbtSyntax).first
            }
        }

        return Result(
            executePrefix = prefix,
            selector = selector,
            javaSelector = javaSel,
            bedrockSelector = bedrockSel,
            javaJson = javaJson,
            bedrockJson = bedrockJson,
            sourceEdition = edition,
            warnings = warnings
        )
    }

    // ------------------------------------------------------------------
    //  拆分辅助
    // ------------------------------------------------------------------

    /** 找到最外层最后一个 " run "（尊重 {} [] "" ''）；找不到返回 null。返回 "run" 的起始下标。 */
    private fun findTopLevelRunIndex(s: String): Int? {
        var depthBrace = 0; var depthBracket = 0; var quote: Char? = null
        var i = 0
        var last: Int? = null
        while (i < s.length) {
            val c = s[i]
            if (quote != null) {
                if (c == '\\') i++ else if (c == quote) quote = null
            } else when (c) {
                '"', '\'' -> quote = c
                '{' -> depthBrace++
                '}' -> depthBrace--
                '[' -> depthBracket++
                ']' -> depthBracket--
                else -> if (depthBrace == 0 && depthBracket == 0 &&
                    (c == 'r') && s.startsWith("run", i) &&
                    isBoundary(s, i - 1) && isBoundary(s, i + 3)
                ) {
                    last = i
                }
            }
            i++
        }
        return last
    }

    private fun isBoundary(s: String, idx: Int): Boolean =
        idx < 0 || idx >= s.length || s[idx] == ' ' || s[idx] == '\t'

    /** 第一个顶层空白（尊重 {} [] "" ''）；找不到返回 -1。 */
    private fun firstTopLevelSpace(s: String): Int {
        var depthBrace = 0; var depthBracket = 0; var quote: Char? = null
        for (i in s.indices) {
            val c = s[i]
            if (quote != null) {
                if (c == '\\') continue
                if (c == quote) quote = null
                continue
            }
            when (c) {
                '"', '\'' -> quote = c
                '{' -> depthBrace++
                '}' -> depthBrace--
                '[' -> depthBracket++
                ']' -> depthBracket--
                ' ', '\t' -> if (depthBrace == 0 && depthBracket == 0) return i
            }
        }
        return -1
    }

    // ------------------------------------------------------------------
    //  JSON -> 内部组件模型
    // ------------------------------------------------------------------

    private fun decodeRoot(root: JsonElement, warnings: MutableList<String>): Pair<List<TextComponent>, SourceEdition>? =
        when {
            root.isJsonObject && root.asJsonObject.has("rawtext") -> {
                val arr = root.asJsonObject.getAsJsonArray("rawtext")
                decodeComponentArray(arr, warnings) to SourceEdition.BEDROCK
            }
            root.isJsonArray -> decodeComponentArray(root.asJsonArray, warnings) to SourceEdition.JAVA
            root.isJsonObject -> decodeComponent(root.asJsonObject, warnings) to SourceEdition.JAVA
            else -> null
        }

    private fun decodeComponentArray(arr: JsonArray, warnings: MutableList<String>): List<TextComponent> {
        val out = mutableListOf<TextComponent>()
        for (e in arr) {
            if (e.isJsonObject) out.addAll(decodeComponent(e.asJsonObject, warnings))
            else if (e.isJsonPrimitive) out.add(TextComponent(ComponentType.TEXT, e.asString))
        }
        return out
    }

    private fun decodeComponent(obj: JsonObject, warnings: MutableList<String>): List<TextComponent> {
        val out = mutableListOf<TextComponent>()
        val stylePrefix = styleToSectionCodes(obj)
        when {
            obj.has("translate") -> {
                val comp = TextComponent(ComponentType.TRANSLATE, str(obj, "translate"))
                obj.get("with")?.takeIf { it.isJsonArray }?.let { w ->
                    val params = (w as JsonArray).map { plainTextOf(it, warnings) }
                    if (params.isNotEmpty()) {
                        comp.subComponents.add(SubComponent(SubComponentType.WITH, params.joinToString(",")))
                    }
                }
                out.add(comp)
            }
            obj.has("score") && obj.get("score").isJsonObject -> {
                val sc = obj.getAsJsonObject("score")
                out.add(TextComponent(ComponentType.SCORE, "${str(sc, "name")}:${str(sc, "objective")}"))
            }
            obj.has("selector") -> out.add(TextComponent(ComponentType.SELECTOR, str(obj, "selector")))
            obj.has("text") -> out.add(TextComponent(ComponentType.TEXT, stylePrefix + str(obj, "text")))
            else -> if (!obj.has("extra")) warnings.add("无法识别的组件（既不是 text/translate/score/selector）：$obj")
        }
        // extra（递归；样式只在最内层 text 生效，这里不向 extra 传递父样式）
        obj.get("extra")?.takeIf { it.isJsonArray }?.let { ex ->
            for (e in ex.asJsonArray) if (e.isJsonObject) out.addAll(decodeComponent(e.asJsonObject, warnings))
        }
        return out
    }

    /** `with` 里的一项 -> 纯文本（非文本组件给出去壳文字，并提醒）。 */
    private fun plainTextOf(e: JsonElement, warnings: MutableList<String>): String = when {
        e.isJsonPrimitive -> e.asString
        e.isJsonObject && e.asJsonObject.has("text") -> str(e.asJsonObject, "text")
        else -> { warnings.add("translate 的 with 里有一项不是纯文本，已尽力取其文字：$e"); e.toString() }
    }

    /** 把常见的样式字段转成 § 前缀（color/format），未知的忽略并提醒。 */
    private fun styleToSectionCodes(obj: JsonObject, warnings: MutableList<String> = mutableListOf()): String {
        val sb = StringBuilder()
        obj.get("color")?.takeIf { it.isJsonPrimitive }?.let { c ->
            val v = c.asString
            val code = NAMED_COLOR_TO_SECTION[v.lowercase()]
                ?: RGB_TO_SECTION[v.uppercase()]     // #RRGGBB
                ?: BEDROCK_ONLY_RGB_TO_SECTION[v.uppercase()]
            if (code != null) sb.append('§').append(code)
        }
        if (bool(obj, "bold")) sb.append("§l")
        if (bool(obj, "italic")) sb.append("§o")
        if (bool(obj, "underlined")) sb.append("§n")
        if (bool(obj, "strikethrough")) sb.append("§m")
        if (bool(obj, "obfuscated")) sb.append("§k")
        return sb.toString()
    }

    private fun str(o: JsonObject, key: String): String {
        val e = o.get(key) ?: return ""
        return if (e.isJsonPrimitive) e.asString else e.toString()
    }

    private fun bool(o: JsonObject, key: String): Boolean =
        o.get(key)?.takeIf { it.isJsonPrimitive }?.asBoolean == true

    // Java 具名色 -> §
    private val NAMED_COLOR_TO_SECTION = mapOf(
        "black" to '0', "dark_blue" to '1', "dark_green" to '2', "dark_aqua" to '3',
        "dark_red" to '4', "dark_purple" to '5', "gold" to '6', "gray" to '7',
        "dark_gray" to '8', "blue" to '9', "green" to 'a', "aqua" to 'b',
        "red" to 'c', "light_purple" to 'd', "yellow" to 'e', "white" to 'f'
    )

    // 16 具名色对应的 RGB -> §
    private val RGB_TO_SECTION: Map<String, Char> = mapOf(
        "#000000" to '0', "#0000AA" to '1', "#00AA00" to '2', "#00AAAA" to '3',
        "#AA0000" to '4', "#AA00AA" to '5', "#FFAA00" to '6', "#AAAAAA" to '7',
        "#555555" to '8', "#5555FF" to '9', "#55FF55" to 'a', "#55FFFF" to 'b',
        "#FF5555" to 'c', "#FF55FF" to 'd', "#FFFF55" to 'e', "#FFFFFF" to 'f'
    )

    // 基岩独有色（含 §m/§n 对应）-> §
    private val BEDROCK_ONLY_RGB_TO_SECTION: Map<String, Char> = buildMap {
        VersionDiff.BEDROCK_ONLY_COLOR_RGB.forEach { (c, hex) -> put(hex.uppercase(), c) }
        VersionDiff.BEDROCK_MN_COLOR_RGB.forEach { (c, hex) -> put(hex.uppercase(), c) }
    }
}
