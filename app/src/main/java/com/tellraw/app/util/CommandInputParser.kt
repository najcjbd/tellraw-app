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
        val components: List<TextComponent>,
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
        // 只有"完全无内容且无警告"才返回 null；keybind/nbt/object 等不支持的类型
        // 仍返回结果，让调用方展示 warnings，而不是静默失败。
        if (components.isEmpty() && warnings.isEmpty()) return null

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
            components = components,
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
                val rawtext = root.asJsonObject.get("rawtext")
                if (rawtext == null || !rawtext.isJsonArray) {
                    warnings.add("rawtext 必须是数组；无法解析该组件")
                    null
                } else {
                    decodeComponentArray(rawtext.asJsonArray, warnings) to SourceEdition.BEDROCK
                }
            }
            root.isJsonArray -> decodeComponentArray(root.asJsonArray, warnings) to SourceEdition.JAVA
            root.isJsonObject -> decodeComponent(root.asJsonObject, warnings) to SourceEdition.JAVA
            else -> null
        }

    private fun decodeComponentArray(arr: JsonArray, warnings: MutableList<String>): List<TextComponent> {
        val out = mutableListOf<TextComponent>()
        var previousStyled = false
        for (e in arr) {
            val decoded = when {
                e.isJsonObject -> decodeComponent(e.asJsonObject, warnings)
                e.isJsonPrimitive -> listOf(TextComponent(ComponentType.TEXT, e.asString))
                else -> emptyList()
            }
            // JSON 数组的兄弟组件互不继承；平铺成 TEXT 模型时用 §r 隔开
            if (previousStyled && decoded.isNotEmpty()) {
                out.add(TextComponent(ComponentType.TEXT, "§r"))
            }
            out.addAll(decoded)
            previousStyled = decoded.any {
                it.type == ComponentType.TEXT && '§' in it.content
            }
        }
        return out
    }

    private fun decodeComponent(
        obj: JsonObject,
        warnings: MutableList<String>,
        inheritedStyle: TextComponentHelper.ComponentStyle = TextComponentHelper.ComponentStyle()
    ): List<TextComponent> {
        val out = mutableListOf<TextComponent>()
        val effectiveStyle = inheritedStyle.overlay(
            TextComponentHelper.ComponentStyle.fromJson(obj)
        )
        when {
            obj.has("translate") -> {
                val comp = TextComponent(
                    ComponentType.TRANSLATE, str(obj, "translate"),
                    style = effectiveStyle
                )
                obj.get("fallback")?.let {
                    warnStyleLoss(it, "fallback", warnings)
                    comp.subComponents.add(
                        SubComponent(SubComponentType.FALLBACK, plainTextOf(it, warnings))
                    )
                }
                obj.get("with")?.let { w ->
                    val arr = when {
                        w.isJsonArray -> w.asJsonArray
                        w.isJsonObject && w.asJsonObject.get("rawtext")?.isJsonArray == true -> {
                            warnings.add("基岩 translate 的 with.rawtext 已展开为参数；条件显示语义在 Java 下可能不同")
                            w.asJsonObject.getAsJsonArray("rawtext")
                        }
                        else -> {
                            warnings.add("translate 的 with 必须是数组或带 rawtext 的对象，已忽略")
                            null
                        }
                    } ?: return@let
                    if (arr.size() == 0) return@let
                    val cleaned = JsonArray()
                    for (el in arr) {
                        if (el.isJsonNull) {
                            warnings.add("translate 的 with 含 null，已忽略该项")
                        } else {
                            cleaned.add(el)
                        }
                    }
                    if (cleaned.size() == 0) return@let
                    // 只有"全是不含逗号/反斜杠的非空字符串"才能安全地塞进纯文本 with；
                    // 否则保留 JSON 数组，避免逗号被再次当成分隔符、空串被当成没有参数。
                    val safePlain = cleaned.all {
                        it.isJsonPrimitive && it.asJsonPrimitive.isString &&
                            it.asString.isNotEmpty() && ',' !in it.asString && '\\' !in it.asString
                    }
                    if (safePlain) {
                        val params = cleaned.map { it.asString }
                        comp.subComponents.add(SubComponent(SubComponentType.WITH, params.joinToString(",")))
                    } else {
                        comp.subComponents.add(SubComponent(SubComponentType.WITH_JSON, cleaned.toString()))
                    }
                    Unit
                }
                out.add(comp)
            }
            obj.has("score") && obj.get("score").isJsonObject -> {
                val sc = obj.getAsJsonObject("score")
                out.add(
                    TextComponent(
                        ComponentType.SCORE,
                        TextComponentHelper.encodeScorePart(str(sc, "name")) + ":" +
                            TextComponentHelper.encodeScorePart(str(sc, "objective")),
                        style = effectiveStyle
                    )
                )
            }
            obj.has("selector") -> {
                val comp = TextComponent(
                    ComponentType.SELECTOR, str(obj, "selector"),
                    style = effectiveStyle
                )
                obj.get("separator")?.let { sep ->
                    warnStyleLoss(sep, "separator", warnings)
                    comp.subComponents.add(SubComponent(SubComponentType.SEPARATOR, plainTextOf(sep, warnings)))
                }
                out.add(comp)
            }
            obj.has("text") -> out.add(
                TextComponent(
                    ComponentType.TEXT,
                    str(obj, "text"),
                    style = effectiveStyle
                )
            )
            obj.has("keybind") -> warnings.add("keybind 文本组件暂不支持，已忽略：${str(obj, "keybind")}")
            obj.has("nbt") -> warnings.add("nbt 文本组件暂不支持，已忽略")
            obj.has("object") -> warnings.add("object/精灵图文本组件暂不支持，已忽略")
            else -> if (!obj.has("extra")) {
                warnings.add("无法识别的组件（既不是 text/translate/score/selector/keybind/nbt/object）：$obj")
            }
        }
        // extra 子组件继承父组件样式（父样式 + 子组件自己的样式）
        obj.get("extra")?.takeIf { it.isJsonArray }?.let { ex ->
            for (e in ex.asJsonArray) if (e.isJsonObject) {
                out.addAll(decodeComponent(e.asJsonObject, warnings, effectiveStyle))
            }
        }
        return out
    }

    /** 子组件目前只能存纯文本；有样式时明确提醒，不再静默丢掉。 */
    private fun warnStyleLoss(e: JsonElement, label: String, warnings: MutableList<String>) {
        if (!e.isJsonObject) return
        val keys = listOf("color", "bold", "italic", "underlined", "strikethrough", "obfuscated", "font", "clickEvent", "hoverEvent")
            .filter { e.asJsonObject.has(it) }
        if (keys.isNotEmpty()) {
            warnings.add("$label 的样式字段 ${keys.joinToString("、")} 暂未保留（只能存纯文本）")
        }
    }

    /** `with` 里的一项 -> 纯文本（非文本组件给出去壳文字，并提醒）。 */
    private fun plainTextOf(e: JsonElement, warnings: MutableList<String>): String = when {
        e.isJsonPrimitive -> e.asString
        e.isJsonObject && e.asJsonObject.has("text") -> str(e.asJsonObject, "text")
        else -> { warnings.add("translate 的 with 里有一项不是纯文本，已尽力取其文字：$e"); e.toString() }
    }

    private fun str(o: JsonObject, key: String): String {
        val e = o.get(key) ?: return ""
        return if (e.isJsonPrimitive) e.asString else e.toString()
    }
}
