package com.tellraw.app.util

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import java.io.StringReader

/**
 * `translate.with` 参数的可视化编辑草稿。
 * 只负责组件 JSON 的结构化编辑；不涉及 Android / Compose。
 */
sealed interface WithArgDraft {
    fun toJson(): JsonElement

    data class Text(
        val text: String,
        val color: String? = null,
        val bold: Boolean? = null,
        val italic: Boolean? = null,
        val underlined: Boolean? = null,
        val strikethrough: Boolean? = null,
        val obfuscated: Boolean? = null,
        val retained: JsonObject = JsonObject()
    ) : WithArgDraft {
        override fun toJson(): JsonElement {
            val o = retained.deepCopy()
            o.addProperty("text", text)
            addOptional(o, "color", color)
            addOptionalBool(o, "bold", bold)
            addOptionalBool(o, "italic", italic)
            addOptionalBool(o, "underlined", underlined)
            addOptionalBool(o, "strikethrough", strikethrough)
            addOptionalBool(o, "obfuscated", obfuscated)
            return o
        }
    }

    data class Translate(
        val key: String,
        val fallback: String? = null,
        val args: List<WithArgDraft> = emptyList(),
        val retained: JsonObject = JsonObject()
    ) : WithArgDraft {
        override fun toJson(): JsonElement {
            val o = retained.deepCopy()
            o.addProperty("translate", key)
            // 空字符串 fallback 与"没有 fallback"语义不同，保留原样
            fallback?.let { o.addProperty("fallback", it) }
            if (args.isNotEmpty()) {
                o.add("with", JsonArray().apply { args.forEach { add(it.toJson()) } })
            }
            return o
        }
    }

    data class Selector(
        val selector: String,
        val separator: String? = null,
        val retained: JsonObject = JsonObject()
    ) : WithArgDraft {
        override fun toJson(): JsonElement {
            val o = retained.deepCopy()
            o.addProperty("selector", selector)
            // 空 separator 与缺省 separator 语义不同，保留原样
            separator?.let { o.addProperty("separator", it) }
            return o
        }
    }

    data class Score(
        val name: String,
        val objective: String,
        val retained: JsonObject = JsonObject()
    ) : WithArgDraft {
        override fun toJson(): JsonElement {
            val o = retained.deepCopy()
            o.add("score", JsonObject().apply {
                addProperty("name", name)
                addProperty("objective", objective)
            })
            return o
        }
    }

    data class Keybind(
        val keybind: String,
        val retained: JsonObject = JsonObject()
    ) : WithArgDraft {
        override fun toJson(): JsonElement {
            val o = retained.deepCopy()
            o.addProperty("keybind", keybind)
            return o
        }
    }

    data class Nbt(
        val path: String,
        val source: String? = null,
        val interpret: Boolean? = null,
        val separator: String? = null,
        val retained: JsonObject = JsonObject()
    ) : WithArgDraft {
        override fun toJson(): JsonElement {
            val o = retained.deepCopy()
            o.addProperty("nbt", path)
            addOptional(o, "source", source)
            addOptionalBool(o, "interpret", interpret)
            // 空 separator 与缺省 separator 语义不同，保留原样
            separator?.let { o.addProperty("separator", it) }
            return o
        }
    }

    /** 无法结构化识别的值（字符串、数字、数组、未知对象）原样保留。 */
    data class Raw(val element: JsonElement) : WithArgDraft {
        override fun toJson(): JsonElement = element.deepCopy()
    }
}

object WithArgCodec {

    enum class FormatError { BLANK, SYNTAX, NOT_ARRAY_OR_OBJECT, NULL_ITEM }

    class WithArgsFormatException(
        val kind: FormatError,
        override val message: String
    ) : IllegalArgumentException(message)

    enum class BedrockIssue { STYLES, SELECTOR_SEPARATOR, KEYBIND, NBT, FALLBACK }

    private val prettyGson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

    /** fromJson 时删除这些键，避免序列化后出现重复/互斥字段。 */
    private val DISCRIMINATORS = listOf(
        "text", "translate", "with", "fallback", "selector", "separator",
        "score", "keybind", "nbt", "source", "interpret"
    )
    private val STYLE_KEYS = listOf(
        "color", "bold", "italic", "underlined", "strikethrough", "obfuscated",
        "font", "clickEvent", "hoverEvent", "insertion"
    )
    private val STYLE_FLAGS = listOf(
        "bold", "italic", "underlined", "strikethrough", "obfuscated"
    )

    // ---------- 解析 ----------

    fun parseArgs(text: String): List<WithArgDraft> =
        argsOf(parseStrict(text))

    /** 原始 JSON 项编辑器：允许任何非 null 的 JSON 值。 */
    fun parseRaw(text: String): JsonElement {
        val el = parseStrict(text)
        if (el.isJsonNull) throw WithArgsFormatException(FormatError.NULL_ITEM, "null item")
        return el
    }

    /** 校验并规范成紧凑 JSON 数组；对象会包成单元素数组。 */
    fun normalizeArgsJson(text: String): String {
        val el = parseStrict(text)
        val arr = argsArrayOf(el)
        if (arr.any { it.isJsonNull }) {
            throw WithArgsFormatException(FormatError.NULL_ITEM, "null item")
        }
        return arr.toString()
    }

    private fun argsOf(el: JsonElement): List<WithArgDraft> =
        argsArrayOf(el).map { fromJson(it) }

    private fun argsArrayOf(el: JsonElement): JsonArray = when {
        el.isJsonArray -> el.asJsonArray
        el.isJsonObject -> JsonArray().also { it.add(el) }
        else -> throw WithArgsFormatException(
            FormatError.NOT_ARRAY_OR_OBJECT, "with must be an array or object"
        )
    }

    private fun parseStrict(text: String): JsonElement {
        val t = text.trim()
        if (t.isEmpty()) throw WithArgsFormatException(FormatError.BLANK, "empty json")
        try {
            val reader = JsonReader(StringReader(t))
            reader.isLenient = false
            val el = JsonParser.parseReader(reader)
            if (reader.peek() != JsonToken.END_DOCUMENT) {
                throw WithArgsFormatException(FormatError.SYNTAX, "trailing data")
            }
            return el
        } catch (e: WithArgsFormatException) {
            throw e
        } catch (e: Exception) {
            throw WithArgsFormatException(FormatError.SYNTAX, e.message ?: "invalid json")
        }
    }

    fun fromJson(el: JsonElement): WithArgDraft {
        if (!el.isJsonObject) return WithArgDraft.Raw(el.deepCopy())
        val o = el.asJsonObject

        if (o.has("text")) return textOf(o) ?: WithArgDraft.Raw(el.deepCopy())
        if (o.has("translate")) return translateOf(o) ?: WithArgDraft.Raw(el.deepCopy())
        if (o.has("selector")) return selectorOf(o) ?: WithArgDraft.Raw(el.deepCopy())
        if (o.has("score")) return scoreOf(o) ?: WithArgDraft.Raw(el.deepCopy())
        if (o.has("keybind")) return keybindOf(o) ?: WithArgDraft.Raw(el.deepCopy())
        if (o.has("nbt")) return nbtOf(o) ?: WithArgDraft.Raw(el.deepCopy())
        return WithArgDraft.Raw(el.deepCopy())
    }

    private fun textOf(o: JsonObject): WithArgDraft.Text? {
        val text = stringOrNull(o, "text") ?: return null
        if (o.has("color") && !isStringField(o, "color")) return null
        STYLE_FLAGS.forEach { if (o.has(it) && !isBoolField(o, it)) return null }
        val color = if (o.has("color")) o.get("color").asString else null
        val bold = boolValue(o, "bold")
        val italic = boolValue(o, "italic")
        val underlined = boolValue(o, "underlined")
        val strikethrough = boolValue(o, "strikethrough")
        val obfuscated = boolValue(o, "obfuscated")
        val retained = retainedOf(o, extraRemoved = listOf("color") + STYLE_FLAGS)
        return WithArgDraft.Text(
            text, color, bold, italic, underlined, strikethrough, obfuscated, retained
        )
    }

    private fun translateOf(o: JsonObject): WithArgDraft.Translate? {
        val key = stringOrNull(o, "translate") ?: return null
        if (o.has("fallback") && !isStringField(o, "fallback")) return null
        val fallback = if (o.has("fallback")) o.get("fallback").asString else null
        val withEl = o.get("with")
        val args: List<WithArgDraft> = when {
            withEl == null -> emptyList()
            !withEl.isJsonArray -> return null
            else -> withEl.asJsonArray.map { fromJson(it) }
        }
        return WithArgDraft.Translate(key, fallback, args, retainedOf(o))
    }

    private fun selectorOf(o: JsonObject): WithArgDraft.Selector? {
        val selector = stringOrNull(o, "selector") ?: return null
        if (o.has("separator") && !isStringField(o, "separator")) return null
        val separator = if (o.has("separator")) o.get("separator").asString else null
        return WithArgDraft.Selector(selector, separator, retainedOf(o))
    }

    private fun scoreOf(o: JsonObject): WithArgDraft.Score? {
        val score = o.get("score") ?: return null
        if (!score.isJsonObject) return null
        val so = score.asJsonObject
        val name = stringOrNull(so, "name") ?: return null
        val objective = stringOrNull(so, "objective") ?: return null
        return WithArgDraft.Score(name, objective, retainedOf(o))
    }

    private fun keybindOf(o: JsonObject): WithArgDraft.Keybind? {
        val keybind = stringOrNull(o, "keybind") ?: return null
        return WithArgDraft.Keybind(keybind, retainedOf(o))
    }

    private fun nbtOf(o: JsonObject): WithArgDraft.Nbt? {
        val path = stringOrNull(o, "nbt") ?: return null
        if (o.has("source") && !isStringField(o, "source")) return null
        if (o.has("separator") && !isStringField(o, "separator")) return null
        if (o.has("interpret") && !isBoolField(o, "interpret")) return null
        val source = if (o.has("source")) o.get("source").asString else null
        val interpret = boolValue(o, "interpret")
        val separator = if (o.has("separator")) o.get("separator").asString else null
        return WithArgDraft.Nbt(path, source, interpret, separator, retainedOf(o))
    }

    private fun isStringField(o: JsonObject, key: String): Boolean {
        val v = o.get(key) ?: return false
        return v.isJsonPrimitive && v.asJsonPrimitive.isString
    }

    private fun isBoolField(o: JsonObject, key: String): Boolean {
        val v = o.get(key) ?: return false
        return v.isJsonPrimitive && v.asJsonPrimitive.isBoolean
    }

    private fun boolValue(o: JsonObject, key: String): Boolean? =
        if (o.has(key)) o.get(key).asBoolean else null

    private fun stringOrNull(o: JsonObject, key: String): String? {
        val v = o.get(key) ?: return null
        return if (v.isJsonPrimitive && v.asJsonPrimitive.isString) v.asString else null
    }

    private fun retainedOf(o: JsonObject, extraRemoved: List<String> = emptyList()): JsonObject {
        val r = o.deepCopy()
        DISCRIMINATORS.forEach { r.remove(it) }
        extraRemoved.forEach { r.remove(it) }
        return r
    }

    // ---------- 序列化 ----------

    fun serialize(args: List<WithArgDraft>): String =
        JsonArray().apply { args.forEach { add(it.toJson()) } }.toString()

    fun serializePretty(args: List<WithArgDraft>): String =
        prettyGson.toJson(JsonArray().apply { args.forEach { add(it.toJson()) } })

    fun prettyJson(el: JsonElement): String = prettyGson.toJson(el)

    // ---------- 基岩兼容提示 ----------

    fun bedrockIssues(args: List<WithArgDraft>): Set<BedrockIssue> {
        val out = linkedSetOf<BedrockIssue>()
        fun scan(draft: WithArgDraft) {
            when (draft) {
                is WithArgDraft.Text -> {
                    if (hasAnyStyle(draft.retained) || listOf(
                            draft.color, draft.bold, draft.italic, draft.underlined,
                            draft.strikethrough, draft.obfuscated
                        ).any { it != null }
                    ) out.add(BedrockIssue.STYLES)
                }
                is WithArgDraft.Translate -> {
                    if (!draft.fallback.isNullOrEmpty()) out.add(BedrockIssue.FALLBACK)
                    if (hasAnyStyle(draft.retained)) out.add(BedrockIssue.STYLES)
                    draft.args.forEach { scan(it) }
                }
                is WithArgDraft.Selector -> {
                    if (!draft.separator.isNullOrEmpty()) out.add(BedrockIssue.SELECTOR_SEPARATOR)
                    if (hasAnyStyle(draft.retained)) out.add(BedrockIssue.STYLES)
                }
                is WithArgDraft.Score -> {
                    if (hasAnyStyle(draft.retained)) out.add(BedrockIssue.STYLES)
                }
                is WithArgDraft.Keybind -> {
                    out.add(BedrockIssue.KEYBIND)
                    if (hasAnyStyle(draft.retained)) out.add(BedrockIssue.STYLES)
                }
                is WithArgDraft.Nbt -> {
                    out.add(BedrockIssue.NBT)
                    if (hasAnyStyle(draft.retained)) out.add(BedrockIssue.STYLES)
                }
                is WithArgDraft.Raw -> scanRaw(draft.element, out, ::scan)
            }
        }
        args.forEach(::scan)
        return out
    }

    private fun scanRaw(
        el: JsonElement,
        out: MutableSet<BedrockIssue>,
        scanDraft: (WithArgDraft) -> Unit
    ) {
        when {
            el.isJsonArray -> el.asJsonArray.forEach { scanRaw(it, out, scanDraft) }
            el.isJsonObject -> {
                val o = el.asJsonObject
                if (hasAnyStyle(o)) out.add(BedrockIssue.STYLES)
                if (o.has("keybind")) out.add(BedrockIssue.KEYBIND)
                if (o.has("nbt")) out.add(BedrockIssue.NBT)
                if (o.has("separator")) out.add(BedrockIssue.SELECTOR_SEPARATOR)
                if (o.has("fallback")) out.add(BedrockIssue.FALLBACK)
                // 结构化对象交给常规扫描；纯 Raw 的未知对象只需上面的键检查
                val draft = fromJson(o)
                if (draft !is WithArgDraft.Raw) scanDraft(draft)
            }
        }
    }

    private fun hasAnyStyle(o: JsonObject): Boolean = STYLE_KEYS.any { o.has(it) }
}

private fun addOptional(o: JsonObject, key: String, value: String?) {
    if (value == null) o.remove(key) else o.addProperty(key, value)
}

private fun addOptionalBool(o: JsonObject, key: String, value: Boolean?) {
    if (value == null) o.remove(key) else o.addProperty(key, value)
}
