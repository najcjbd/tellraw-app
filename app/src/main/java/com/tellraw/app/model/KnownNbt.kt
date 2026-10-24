package com.tellraw.app.model

/**
 * 网格“快捷 NBT”目录：常见、可安全快捷编辑的实体 NBT 键。
 * 快捷字段与手动 NBT 文本双向同步（手动文本里出现这些键时，快捷字段回填；
 * 快捷字段修改时，写回手动文本）。
 */
object KnownNbt {

    enum class Kind { BOOL, BYTE, FLOAT, INT, STRING }

    data class Spec(val key: String, val label: String, val kind: Kind, val hint: String = "")

    val ALL: List<Spec> = listOf(
        Spec("Health", "血量 Health", Kind.FLOAT, "例如 20.0"),
        Spec("Absorption", "伤害吸收 Absorption", Kind.FLOAT, "黄心"),
        Spec("Fire", "着火时间 Fire", Kind.INT, "刻；负值=防火"),
        Spec("Air", "氧气 Air", Kind.INT, "刻"),
        Spec("FallDistance", "下落距离 FallDistance", Kind.FLOAT, ""),
        Spec("NoAI", "无 AI", Kind.BOOL, ""),
        Spec("Silent", "无声", Kind.BOOL, ""),
        Spec("Invulnerable", "无敌", Kind.BOOL, ""),
        Spec("Glowing", "发光", Kind.BOOL, ""),
        Spec("Invisible", "隐身", Kind.BOOL, ""),
        Spec("PersistenceRequired", "防止自然消失", Kind.BOOL, ""),
        Spec("NoGravity", "无重力", Kind.BOOL, ""),
        Spec("CustomNameVisible", "名字始终可见", Kind.BOOL, "")
    )

    fun specsFor(entityId: String? = null): List<Spec> {
        val extra = entityId?.let { EntityNbtCatalog.extrasFor(it) } ?: emptyList()
        if (extra.isEmpty()) return ALL
        val known = ALL.map { it.key }.toSet()
        return ALL + extra.filterNot { it.key in known }
    }

    fun byKey(key: String, entityId: String? = null): Spec? =
        specsFor(entityId).firstOrNull { it.key.equals(key, true) }

    /** 快捷字段文本 -> 写回手动 NBT 的写法（带合适的类型后缀）。 */
    fun nbtTextOf(spec: Spec, text: String): String? {
        val t = text.trim()
        if (t.isEmpty()) return null
        return when (spec.kind) {
            Kind.BOOL -> when {
                t.equals("true", true) || t == "1" -> "1b"
                t.equals("false", true) || t == "0" -> "0b"
                else -> null
            }
            Kind.BYTE -> t.toLongOrNull()?.let { "${it}b" } ?: "\"$t\""
            Kind.INT -> t.toLongOrNull()?.let { "${it}s" } ?: "\"$t\""
            Kind.FLOAT -> t.toDoubleOrNull()?.let { "${it}f" } ?: "\"$t\""
            Kind.STRING -> "\"" + t.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
        }
    }

    /** 与上方字段同源的 NBT 键（Tags / CustomName / Team）。 */
    data class External(val tags: String?, val name: String?, val team: String?)

    fun readExternal(nbtText: String): External? {
        val parsed = SnbtLite.parseCompoundBody(nbtText) ?: return null
        val tags = (parsed["Tags"] as? SnbtLite.Value.ListV)?.items
            ?.mapNotNull { (it as? SnbtLite.Value.Str)?.v }
            ?.joinToString(",")
        val name = (parsed["CustomName"] as? SnbtLite.Value.Str)?.v?.let { SnbtLite.plainText(it) }
        val team = (parsed["Team"] as? SnbtLite.Value.Str)?.v
        return External(tags, name, team)
    }

    /**
     * 去掉与上方字段同源的键（避免手写 NBT 覆盖字段值）。
     * 只做"按顶层逗号删条目"，不重排/重写其他键，保住用户写的 `20.0f` 等类型后缀。
     */
    fun stripExternal(nbtText: String): String? {
        if (nbtText.isBlank()) return ""
        if (SnbtLite.parseCompoundBody(nbtText) == null) return null
        val body = nbtText.trim().removeSurrounding("{", "}")
        val keys = setOf("Tags", "CustomName", "Team")
        val kept = com.tellraw.app.util.ExecCondSupport.splitTopLevel(body, ',').filterNot { part ->
            val key = part.substringBefore(':').substringBefore('=').trim().removeSurrounding("\"")
            key in keys
        }
        return kept.joinToString(",").trim()
    }

    /** 从 NBT 文本回填已知快捷键（未知键忽略，但文本本身不会被丢）。 */
    fun fillQuick(text: String, out: MutableMap<String, String>, entityId: String? = null) {
        val parsed = SnbtLite.parseCompoundBody(text) ?: return
        for ((k, v) in parsed) {
            val spec = byKey(k, entityId) ?: continue
            val t = if (spec.kind == Kind.BOOL) {
                when (v) {
                    is SnbtLite.Value.Bool -> if (v.v) "true" else "false"
                    is SnbtLite.Value.Num -> if (v.v != 0.0) "true" else "false"
                    else -> textOf(v)
                }
            } else textOf(v)
            if (t.isNotBlank()) out[spec.key] = t
        }
    }

    /**
     * 把快捷字段合并回手动 NBT：已知键按快捷值重写，未知键原样保留。
     * 无法解析时返回 null（调用方应提示用户先修正文本）。
     */
    fun merge(nbtText: String, quick: Map<String, String>, entityId: String? = null): String? {
        val base = SnbtLite.parseCompoundBody(nbtText)?.toMutableMap() ?: return null
        val knownParts = mutableListOf<String>()
        for (spec in specsFor(entityId)) {
            base.remove(spec.key)
            val v = quick[spec.key].orEmpty()
            if (v.isNotBlank()) {
                nbtTextOf(spec, v)?.let { knownParts.add("${spec.key}:$it") }
            }
        }
        val unknown = SnbtLite.serializeCompound(base)
        return listOf(unknown, knownParts.joinToString(","))
            .filter { it.isNotBlank() }
            .joinToString(",")
    }

    /** NBT 值 -> 快捷字段文本（无法表示时返回空，让用户手动处理）。 */
    fun textOf(v: SnbtLite.Value): String = when (v) {
        is SnbtLite.Value.Bool -> if (v.v) "true" else "false"
        is SnbtLite.Value.Num ->
            if (v.v == v.v.toLong().toDouble()) v.v.toLong().toString() else v.v.toString()
        is SnbtLite.Value.Str -> v.v
        else -> ""
    }
}
