package com.tellraw.app.util

import android.content.Context
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.tellraw.app.model.SelectorType
import com.tellraw.app.model.VersionSpec
import com.tellraw.app.util.TextComponentHelper.ComponentType
import com.tellraw.app.util.TextComponentHelper.SubComponent
import com.tellraw.app.util.TextComponentHelper.SubComponentType
import com.tellraw.app.util.TextComponentHelper.TextComponent

/**
 * Simpler translate 的数据模型与生成器。
 *
 * 语义：消息里的 Simpler token 是一个"按收件人替换"的位置。
 *  - 每个收信人行 = 选择器参数 + 替换内容；
 *  - 生成时把 token 原位替换成该行内容，保留 token 前后的文本/组件；
 *  - Java 多条 tellraw 做第一匹配；基岩默认一条 conditional rawtext，
 *    能在选择器里表达排除时也做第一匹配，否则明确提醒重叠。
 */
object SimpleTranslatePlanner {

    const val KEY = TextComponentHelper.SIMPLE_TRANSLATE_KEY

    data class Row(
        val id: String = "",
        val params: String = "",
        val content: List<WithArgDraft> = listOf(WithArgDraft.Text(""))
    )

    data class Alias(
        val from: String = "",
        val to: List<WithArgDraft> = listOf(WithArgDraft.Text(""))
    )

    data class Table(
        val rows: List<Row> = emptyList(),
        val aliases: List<Alias> = emptyList(),
        val bedrockFirstMatch: Boolean = false
    )

    data class Result(
        val java: List<String>,
        val bedrock: List<String>,
        val warnings: List<String>
    )

    // ---------------------------------------------------------------- 编解码

    fun decode(json: String?): Table? {
        if (json.isNullOrBlank()) return null
        return runCatching {
            val root = JsonParser.parseString(json).asJsonObject
            val rows = root.getAsJsonArray("rows")?.mapIndexed { i, el ->
                val o = el.asJsonObject
                Row(
                    id = "r$i",
                    params = o.get("params")?.takeIf { it.isJsonPrimitive }?.asString.orEmpty(),
                    content = WithArgCodec.parseArgs(
                        o.getAsJsonArray("content")?.toString() ?: "[]"
                    )
                )
            }.orEmpty()
            val aliases = root.getAsJsonArray("aliases")?.map { el ->
                val o = el.asJsonObject
                Alias(
                    from = o.get("from")?.takeIf { it.isJsonPrimitive }?.asString.orEmpty(),
                    to = WithArgCodec.parseArgs(
                        o.getAsJsonArray("to")?.toString() ?: "[]"
                    )
                )
            }.orEmpty()
            Table(
                rows = rows,
                aliases = aliases,
                bedrockFirstMatch = root.get("bedrockFirstMatch")?.takeIf { it.isJsonPrimitive }?.asBoolean == true
            )
        }.getOrNull()
    }

    fun encode(table: Table): String {
        val root = JsonObject()
        root.addProperty("bedrockFirstMatch", table.bedrockFirstMatch)
        root.add("rows", JsonArray().apply {
            table.rows.forEach { row ->
                add(JsonObject().apply {
                    addProperty("params", row.params)
                    add("content", JsonParser.parseString(WithArgCodec.serialize(row.content)))
                })
            }
        })
        root.add("aliases", JsonArray().apply {
            table.aliases.forEach { alias ->
                add(JsonObject().apply {
                    addProperty("from", alias.from)
                    add("to", JsonParser.parseString(WithArgCodec.serialize(alias.to)))
                })
            }
        })
        return root.toString()
    }

    fun hasMarker(json: String): Boolean =
        runCatching { containsMarker(JsonParser.parseString(json)) }.getOrDefault(false)

    fun stripJavaMarker(json: String): String =
        replaceJavaMarker(json, null) ?: json

    fun stripBedrockMarker(json: String): String =
        replaceBedrockMarker(json, emptyList()) ?: json

    private fun containsMarker(el: JsonElement): Boolean = when {
        el.isJsonObject -> {
            val o = el.asJsonObject
            if (o.get("translate")?.takeIf { it.isJsonPrimitive }?.asString == KEY) {
                true
            } else {
                o.entrySet().any { containsMarker(it.value) }
            }
        }
        el.isJsonArray -> el.asJsonArray.any { containsMarker(it) }
        else -> false
    }

    // ---------------------------------------------------------------- 生成

    /**
     * @param javaMessageJson 已经转换好的完整 Java 消息 JSON（其中含 Simpler marker）
     * @param bedrockMessageJson 已经转换好的完整基岩 rawtext JSON（其中含 Simpler marker）
     * @param includeDefaultFallback token 前后是否有真实内容；有则未命中任何行的收件人收到去掉 token 的消息
     */
    fun plan(
        table: Table,
        javaMessageJson: String,
        bedrockMessageJson: String,
        javaSelector: String,
        bedrockSelector: String,
        javaPrefix: String?,
        bedrockPrefix: String?,
        includeDefaultFallback: Boolean,
        context: Context?,
        nbtSyntax: VersionDiff.NbtSyntax,
        target: VersionSpec?,
        mNHandling: String,
        mnCFEnabled: Boolean,
        separatorAsTextComponent: Boolean,
        bedrockColorToRgb: Boolean,
        warnings: MutableList<String>
    ): Result {
        if (table.rows.isEmpty()) {
            warnings.add("Simpler translate 没有任何收信人行；token 已忽略")
            return Result(emptyList(), emptyList(), warnings.distinct())
        }
        val javaHasMarker = hasMarker(javaMessageJson)
        val bedrockHasMarker = hasMarker(bedrockMessageJson)
        if (!javaHasMarker) warnings.add("Simpler token 没有进入 Java 消息 JSON；请重新插入 token")
        if (!bedrockHasMarker) warnings.add("Simpler token 没有进入基岩消息 JSON；请重新插入 token")
        if (!javaHasMarker && !bedrockHasMarker) {
            return Result(emptyList(), emptyList(), warnings.distinct())
        }

        val aliasMap = linkedMapOf<String, List<WithArgDraft>>()
        table.aliases.forEachIndexed { index, alias ->
            val from = alias.from
            if (from.isBlank()) return@forEachIndexed
            if (aliasMap.containsKey(from)) {
                warnings.add("别名「$from」重复，第 ${index + 1} 条会覆盖前面的")
            }
            aliasMap[from] = alias.to
        }

        fun applyAliases(args: List<WithArgDraft>): List<WithArgDraft> = args.flatMap { draft ->
            when (draft) {
                is WithArgDraft.Text -> aliasMap[draft.text] ?: listOf(draft)
                is WithArgDraft.Translate -> listOf(draft.copy(args = applyAliases(draft.args)))
                else -> listOf(draft)
            }
        }

        val prepared = mutableListOf<PreparedRow>()
        var fallbackSeen = false
        table.rows.forEachIndexed { index, row ->
            val label = "第 ${index + 1} 行"
            val params = normalizeParams(row.params, label, warnings)
            if (fallbackSeen) {
                warnings.add("$label 在无参数回退行之后，永远不会命中，已跳过")
                return@forEachIndexed
            }
            if (params.isBlank()) fallbackSeen = true

            val contents = applyAliases(row.content)
            val components = draftsToComponents(contents, label, warnings)
            val javaContent = TextComponentHelper.convertToJavaJson(
                components, mNHandling, mnCFEnabled, context, warnings,
                separatorAsTextComponent, nbtSyntax, bedrockColorToRgb, target
            )
            val bedrockContent = TextComponentHelper.convertToBedrockJson(
                components, mNHandling, mnCFEnabled, context, warnings, nbtSyntax, target
            )
            val rowJava = replaceJavaMarker(
                javaMessageJson,
                if (components.isEmpty()) {
                    JsonObject().apply { addProperty("text", "") }
                } else {
                    runCatching { JsonParser.parseString(javaContent) }.getOrNull()
                }
            )
            val bedrockItems = rawtextItems(bedrockContent)
            val rowBedrock = replaceBedrockMarker(bedrockMessageJson, bedrockItems)
            if (rowJava == null || rowBedrock == null) {
                warnings.add("$label 无法把内容替换进消息 JSON，已跳过")
                return@forEachIndexed
            }
            prepared.add(PreparedRow(params, rowJava, rawtextItems(rowBedrock), params.isBlank()))
        }
        if (prepared.isEmpty()) {
            return Result(emptyList(), emptyList(), warnings.distinct())
        }

        val baseJavaSelector = javaSelector.ifBlank { "@a" }
        val baseBedrockSelector = bedrockSelector.ifBlank { "@a" }
        val defaultJava = replaceJavaMarker(javaMessageJson, null) ?: javaMessageJson
        val defaultBedrockItems = rawtextItems(
            replaceBedrockMarker(bedrockMessageJson, emptyList()) ?: bedrockMessageJson
        )

        val java = mutableListOf<String>()
        val earlierJava = mutableListOf<String>()
        var javaFallback: PreparedRow? = null
        prepared.forEach { row ->
            if (row.params.isBlank()) {
                javaFallback = row
                java.add(
                    javaAsCommand(
                        baseJavaSelector, "", earlierJava, row.javaJson, javaPrefix,
                        context, nbtSyntax, target, warnings
                    )
                )
            } else {
                java.add(
                    javaAsCommand(
                        baseJavaSelector, row.params, earlierJava, row.javaJson, javaPrefix,
                        context, nbtSyntax, target, warnings
                    )
                )
                earlierJava.add(row.params)
            }
        }
        if (javaFallback == null && includeDefaultFallback) {
            java.add(
                javaAsCommand(
                    baseJavaSelector, "", earlierJava, defaultJava, javaPrefix,
                    context, nbtSyntax, target, warnings
                )
            )
        }

        val bedrock = if (table.bedrockFirstMatch) {
            buildBedrockFirstMatch(
                prepared, baseBedrockSelector, bedrockPrefix, includeDefaultFallback,
                defaultBedrockItems, context, nbtSyntax, target, warnings
            )
        } else {
            buildBedrockConditional(
                prepared, defaultBedrockItems, includeDefaultFallback,
                baseBedrockSelector, bedrockPrefix, context, nbtSyntax, target, warnings
            )
        }

        return Result(java, bedrock, warnings.distinct())
    }

    // ---------------------------------------------------------------- 命令拼装

    private fun javaAsCommand(
        baseSelector: String,
        params: String,
        earlier: List<String>,
        json: String,
        prefix: String?,
        context: Context?,
        nbtSyntax: VersionDiff.NbtSyntax,
        target: VersionSpec?,
        warnings: MutableList<String>
    ): String {
        val merged = appendSelectorParams(baseSelector, params)
        val negation = ExecuteConverter.bedrockSelectorNegation(
            merged, warnings, preferExecute = true, nbtSyntax = nbtSyntax
        )
        val inner = if (negation != null) {
            val (asSelector, conditions) = negation
            val filtered = filterJava(asSelector, context, nbtSyntax, target, warnings)
            if (conditions.isEmpty()) listOf("as", filtered)
            else listOf("as", filtered) + conditions
        } else {
            listOf("as", filterJava(merged, context, nbtSyntax, target, warnings))
        }
        val exclusions = exclusionsForJava(earlier, context, nbtSyntax, target, warnings)
        val base = prefix?.trim()?.takeIf { it.isNotEmpty() } ?: "execute"
        return "$base ${inner.joinToString(" ")}$exclusions run tellraw @s $json"
    }

    private fun exclusionsForJava(
        earlier: List<String>,
        context: Context?,
        nbtSyntax: VersionDiff.NbtSyntax,
        target: VersionSpec?,
        warnings: MutableList<String>
    ): String = earlier.mapNotNull { params ->
        val filtered = filterJava("@s[$params]", context, nbtSyntax, target, warnings)
        val inner = when {
            filtered.startsWith("@s[") && filtered.endsWith("]") ->
                filtered.substring(3, filtered.length - 1)
            filtered == "@s" -> ""
            else -> filtered
        }
        if (inner.isBlank()) {
            warnings.add("Java 选择器无法表达收信人参数「$params」，该排除条件已省略（后面行可能重复显示）")
            null
        } else {
            " unless entity @s[$inner]"
        }
    }.joinToString("")

    private fun buildBedrockFirstMatch(
        prepared: List<PreparedRow>,
        baseSelector: String,
        prefix: String?,
        includeDefaultFallback: Boolean,
        defaultItems: List<JsonElement>,
        context: Context?,
        nbtSyntax: VersionDiff.NbtSyntax,
        target: VersionSpec?,
        warnings: MutableList<String>
    ): List<String> {
        val out = mutableListOf<String>()
        val earlier = mutableListOf<String>()
        var fallback: PreparedRow? = null
        prepared.forEach { row ->
            if (row.fallback) {
                fallback = row
                out.add(
                    bedrockAsCommand(
                        baseSelector, "", earlier, row.bedrockItems, prefix,
                        context, nbtSyntax, target, warnings
                    )
                )
            } else {
                out.add(
                    bedrockAsCommand(
                        baseSelector, row.params, earlier, row.bedrockItems, prefix,
                        context, nbtSyntax, target, warnings
                    )
                )
                earlier.add(row.params)
            }
        }
        if (fallback == null && includeDefaultFallback) {
            out.add(
                bedrockAsCommand(
                    baseSelector, "", earlier, defaultItems, prefix,
                    context, nbtSyntax, target, warnings
                )
            )
        }
        return out
    }

    private fun bedrockAsCommand(
        baseSelector: String,
        params: String,
        earlier: List<String>,
        items: List<JsonElement>,
        prefix: String?,
        context: Context?,
        nbtSyntax: VersionDiff.NbtSyntax,
        target: VersionSpec?,
        warnings: MutableList<String>
    ): String {
        val merged = appendSelectorParams(baseSelector, params)
        val selector = filterBedrock(merged, context, nbtSyntax, target, warnings)
        val exclusions = earlier.joinToString("") { " unless entity @s[$it]" }
        val base = prefix?.trim()?.takeIf { it.isNotEmpty() } ?: "execute"
        val raw = rawtextJson(items)
        return "$base as $selector$exclusions run tellraw @s $raw"
    }

    private fun buildBedrockConditional(
        prepared: List<PreparedRow>,
        defaultItems: List<JsonElement>,
        includeDefaultFallback: Boolean,
        baseSelector: String,
        prefix: String?,
        context: Context?,
        nbtSyntax: VersionDiff.NbtSyntax,
        target: VersionSpec?,
        warnings: MutableList<String>
    ): List<String> {
        val rows = JsonArray()
        val earlierInverted = mutableListOf<Pair<String, String>>()
        var canExcludeEarlier = true
        var fallback: PreparedRow? = null
        var overlapWarned = false

        fun addConditional(params: String, items: List<JsonElement>) {
            val selectorText = if (params.isBlank()) "@s" else "@s[$params]"
            val selector = filterBedrock(selectorText, context, nbtSyntax, target, warnings)
            val args = JsonArray()
            args.add(JsonObject().apply { addProperty("selector", selector) })
            if (items.size == 1) {
                args.add(items[0].deepCopy())
            } else {
                args.add(JsonObject().apply {
                    add("rawtext", JsonArray().apply { items.forEach { add(it.deepCopy()) } })
                })
            }
            rows.add(JsonObject().apply {
                addProperty("translate", "%%2")
                add("with", JsonObject().apply { add("rawtext", args) })
            })
        }

        // 单条条件 rawtext 只有一个选择器；同一参数键（例如多个 tag=）无法可靠 AND 排除。
        fun usableEarlier(currentKeys: Set<String>): List<String> {
            val grouped = earlierInverted.groupBy { it.first }
            return grouped
                .filterKeys { key -> key !in currentKeys && grouped[key]?.size == 1 }
                .values
                .map { it[0].second }
        }

        fun warnOverlap() {
            if (overlapWarned) return
            warnings.add("基岩 conditional rawtext 无法排除部分更早的收信人；重叠的收件人会收到多条消息（需要严格第一匹配请勾选“基岩也用多条 tellraw”）")
            overlapWarned = true
        }

        prepared.forEach { row ->
            if (row.fallback) {
                fallback = row
                return@forEach
            }
            val exclusions = usableEarlier(paramKeys(row.params))
            if (!canExcludeEarlier || exclusions.size < earlierInverted.size) warnOverlap()
            addConditional(
                (listOf(row.params) + exclusions).joinToString(","),
                row.bedrockItems
            )
            val inverted = invertParams(row.params)
            if (inverted == null) {
                canExcludeEarlier = false
            } else {
                splitTopLevel(inverted, ',').forEach { part ->
                    val eq = indexOfTopLevel(part, '=')
                    if (eq > 0) earlierInverted.add(part.substring(0, eq).trim() to part)
                }
            }
        }

        val wantsFallback = fallback != null || includeDefaultFallback
        if (wantsFallback) {
            val items = fallback?.bedrockItems ?: defaultItems
            val exclusions = usableEarlier(emptySet())
            if (exclusions.size < earlierInverted.size || !canExcludeEarlier) {
                warnOverlap()
            }
            addConditional(exclusions.joinToString(","), items)
        } else {
            // 未命中任何行的收件人本来就不该收到任何内容（token 是整条消息时）。
            // token 前后有文字时，includeDefaultFallback 会为其余人补一条回退消息。
        }
        if (rows.size() == 0) return emptyList()

        val selector = filterBedrock(baseSelector, context, nbtSyntax, target, warnings)
        val base = prefix?.trim()?.takeIf { it.isNotEmpty() } ?: "execute"
        val raw = JsonObject().apply { add("rawtext", rows) }
        return listOf("$base as $selector run tellraw @s $raw")
    }

    data class PreparedRow(
        val params: String,
        val javaJson: String,
        val bedrockItems: List<JsonElement>,
        val fallback: Boolean
    )

    // ---------------------------------------------------------------- 内容模型

    private fun draftsToComponents(
        drafts: List<WithArgDraft>,
        label: String,
        warnings: MutableList<String>
    ): List<TextComponent> {
        val out = mutableListOf<TextComponent>()
        drafts.forEach { draft ->
            when (draft) {
                is WithArgDraft.Text -> {
                    out.add(
                        TextComponent(
                            ComponentType.TEXT,
                            draft.text,
                            style = componentStyle(draft.retained, draft)
                        )
                    )
                }
                is WithArgDraft.Translate -> {
                    if (draft.key.isBlank()) {
                        warnings.add("$label 的 translate key 为空，已跳过该组件")
                    } else {
                        val comp = TextComponent(
                            ComponentType.TRANSLATE,
                            draft.key,
                            style = componentStyle(draft.retained)
                        )
                        if (draft.args.isNotEmpty()) {
                            comp.subComponents.add(
                                SubComponent(
                                    SubComponentType.WITH_JSON,
                                    WithArgCodec.serialize(draft.args)
                                )
                            )
                        }
                        draft.fallback?.let {
                            comp.subComponents.add(SubComponent(SubComponentType.FALLBACK, it))
                        }
                        out.add(comp)
                    }
                }
                is WithArgDraft.Selector -> {
                    val comp = TextComponent(
                        ComponentType.SELECTOR,
                        draft.selector,
                        style = componentStyle(draft.retained)
                    )
                    draft.separator?.let {
                        comp.subComponents.add(SubComponent(SubComponentType.SEPARATOR, it))
                    }
                    out.add(comp)
                }
                is WithArgDraft.Score -> {
                    out.add(
                        TextComponent(
                            ComponentType.SCORE,
                            TextComponentHelper.encodeScorePart(draft.name) + ":" +
                                TextComponentHelper.encodeScorePart(draft.objective),
                            style = componentStyle(draft.retained)
                        )
                    )
                }
                is WithArgDraft.Keybind -> {
                    warnings.add("$label 的 keybind 组件暂不支持放进 Simpler translate，已跳过")
                }
                is WithArgDraft.Nbt -> {
                    warnings.add("$label 的 nbt 组件暂不支持放进 Simpler translate，已跳过")
                }
                is WithArgDraft.Raw -> {
                    val el = draft.element
                    when {
                        el.isJsonPrimitive && el.asJsonPrimitive.isString ->
                            out.add(TextComponent(ComponentType.TEXT, el.asString))
                        el.isJsonPrimitive ->
                            out.add(TextComponent(ComponentType.TEXT, el.asString))
                        else -> warnings.add("$label 的原始 JSON 项不是文本组件，已跳过：$el")
                    }
                }
            }
        }
        return out
    }

    private fun componentStyle(
        retained: JsonObject,
        text: WithArgDraft.Text? = null
    ): TextComponentHelper.ComponentStyle {
        val o = retained.deepCopy()
        if (text != null) {
            text.color?.let { o.addProperty("color", it) }
            text.bold?.let { o.addProperty("bold", it) }
            text.italic?.let { o.addProperty("italic", it) }
            text.underlined?.let { o.addProperty("underlined", it) }
            text.strikethrough?.let { o.addProperty("strikethrough", it) }
            text.obfuscated?.let { o.addProperty("obfuscated", it) }
        }
        return TextComponentHelper.ComponentStyle.fromJson(o)
    }

    // ---------------------------------------------------------------- JSON 替换

    private fun replaceJavaMarker(json: String, replacement: JsonElement?): String? {
        val root = runCatching { JsonParser.parseString(json) }.getOrNull() ?: return null
        val replaced = replaceElement(root, replacement)
        return replaced.toString()
    }

    private fun replaceElement(el: JsonElement, replacement: JsonElement?): JsonElement {
        if (el.isJsonObject) {
            val o = el.asJsonObject
            if (o.get("translate")?.takeIf { it.isJsonPrimitive }?.asString == KEY) {
                return replacement?.deepCopy()
                    ?: JsonObject().apply { addProperty("text", "") }
            }
            val copy = JsonObject()
            o.entrySet().forEach { (k, v) -> copy.add(k, replaceElement(v, replacement)) }
            return copy
        }
        if (el.isJsonArray) {
            val out = JsonArray()
            el.asJsonArray.forEach { item ->
                if (item.isJsonObject &&
                    item.asJsonObject.get("translate")?.takeIf { it.isJsonPrimitive }?.asString == KEY
                ) {
                    if (replacement == null) {
                        // 删掉 token
                    } else if (replacement.isJsonArray) {
                        replacement.asJsonArray.forEach { out.add(it.deepCopy()) }
                    } else {
                        out.add(replacement.deepCopy())
                    }
                } else {
                    out.add(replaceElement(item, replacement))
                }
            }
            return out
        }
        return el.deepCopy()
    }

    /** 基岩 rawtext：把 marker 元素替换成内容 rawtext 的条目（拍平，不留下空对象）。 */
    private fun replaceBedrockMarker(json: String, replacementItems: List<JsonElement>): String? {
        val root = runCatching { JsonParser.parseString(json).asJsonObject }.getOrNull() ?: return null
        val rawtext = root.getAsJsonArray("rawtext") ?: return null
        val out = JsonArray()
        rawtext.forEach { item ->
            if (item.isJsonObject &&
                item.asJsonObject.get("translate")?.takeIf { it.isJsonPrimitive }?.asString == KEY
            ) {
                replacementItems.forEach { out.add(it.deepCopy()) }
            } else {
                out.add(replaceElement(item, null))
            }
        }
        return JsonObject().apply { add("rawtext", out) }.toString()
    }

    private fun rawtextItems(json: String): List<JsonElement> {
        val root = runCatching { JsonParser.parseString(json) }.getOrNull() ?: return emptyList()
        if (root.isJsonObject) {
            val arr = root.asJsonObject.getAsJsonArray("rawtext")
            if (arr != null) return arr.toList()
        }
        return listOf(root)
    }

    private fun rawtextJson(items: List<JsonElement>): String =
        JsonObject().apply {
            add("rawtext", JsonArray().apply { items.forEach { add(it.deepCopy()) } })
        }.toString()

    // ---------------------------------------------------------------- 选择器

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

    private fun normalizeParams(raw: String, label: String, warnings: MutableList<String>): String {
        var p = raw.trim().trimStart(',').trimEnd(',')
        if (p.startsWith("@")) {
            val open = p.indexOf('[')
            p = if (open >= 0 && p.endsWith("]")) {
                p.substring(open + 1, p.length - 1).trim().trimStart(',').trimEnd(',')
            } else ""
            warnings.add("$label 只填选择器参数（tag/scores 等），不要写 @a；已自动去掉选择器前缀")
        }
        if (p.startsWith("[") && p.endsWith("]")) {
            p = p.substring(1, p.length - 1).trim()
            warnings.add("$label 参数外层的 [] 已去掉")
        }
        if (p.contains(']') || p.contains('[')) {
            warnings.add("$label 参数里含有未配对的 []，生成结果可能无效")
        }
        return p
    }

    private fun filterJava(
        selector: String,
        context: Context?,
        nbtSyntax: VersionDiff.NbtSyntax,
        target: VersionSpec?,
        warnings: MutableList<String>
    ): String {
        if (context == null) return selector
        val (filtered, _, reminders) = SelectorConverter.filterSelectorParameters(
            selector, SelectorType.JAVA, context, nbtSyntax, target
        )
        warnings.addAll(reminders)
        return filtered
    }

    private fun filterBedrock(
        selector: String,
        context: Context?,
        nbtSyntax: VersionDiff.NbtSyntax,
        target: VersionSpec?,
        warnings: MutableList<String>
    ): String {
        if (context == null) return selector
        val (filtered, _, reminders) = SelectorConverter.filterSelectorParameters(
            selector, SelectorType.BEDROCK, context, nbtSyntax, target
        )
        warnings.addAll(reminders)
        return filtered
    }

    private fun paramKeys(params: String): Set<String> =
        splitTopLevel(params, ',').mapNotNull { part ->
            val eq = indexOfTopLevel(part, '=')
            if (eq > 0) part.substring(0, eq).trim() else null
        }.toSet()

    private fun invertParams(params: String): String? {
        val parts = splitTopLevel(params, ',')
        if (parts.isEmpty()) return ""
        val out = mutableListOf<String>()
        for (part in parts) {
            val eq = indexOfTopLevel(part, '=')
            if (eq <= 0) return null
            val key = part.substring(0, eq).trim()
            val value = part.substring(eq + 1).trim()
            when (key) {
                "tag", "team" -> {
                    out.add(if (value.startsWith("!")) "$key=${value.drop(1)}" else "$key=!$value")
                }
                // scores 取反的语义是"存在且不等于"，不是补集：没有该分数的人也会漏掉。
                // 单条条件 rawtext 无法表达"分数不存在", 因此不生成排除，交给重叠提醒。
                else -> return null
            }
        }
        return out.joinToString(",")
    }

    private fun splitTopLevel(text: String, separator: Char): List<String> {
        val out = mutableListOf<String>()
        var start = 0
        var brace = 0
        var bracket = 0
        var quote: Char? = null
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (quote != null) {
                if (c == '\\') i++ else if (c == quote) quote = null
            } else when (c) {
                '"', '\'' -> quote = c
                '{' -> brace++
                '}' -> brace--
                '[' -> bracket++
                ']' -> bracket--
                separator -> if (brace == 0 && bracket == 0) {
                    out.add(text.substring(start, i).trim())
                    start = i + 1
                }
            }
            i++
        }
        if (start <= text.length) out.add(text.substring(start).trim())
        return out.filter { it.isNotEmpty() }
    }

    private fun indexOfTopLevel(text: String, needle: Char): Int {
        var brace = 0
        var bracket = 0
        var quote: Char? = null
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (quote != null) {
                if (c == '\\') i++ else if (c == quote) quote = null
            } else when (c) {
                '"', '\'' -> quote = c
                '{' -> brace++
                '}' -> brace--
                '[' -> bracket++
                ']' -> bracket--
                needle -> if (brace == 0 && bracket == 0) return i
            }
            i++
        }
        return -1
    }
}
