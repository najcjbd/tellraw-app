package com.tellraw.app.util

/**
 * 新旧版本差异的**统一表**（见 `ttellraw/need/新旧版本差异规范.txt`）。
 *
 * 为什么要单独一个文件：这个应用里"跟游戏版本有关"的差异**只有一族** ——
 * 物品 NBT 与数据组件（分界线 1.20.5 / 24w09a）。以前这些知识散落在
 * [SelectorConverter] / [ExecCondSupport] 多处，于是出现"改了一处、漏了另一处"
 * （`count:Nb`、`tag:`、`equipment.mainhand` 三次事故都是这样来的）。
 *
 * 规范：**所有生成 / 改写只查这张表**，别处不许硬编码键名；出 bug 只改这一处。
 *
 * 依据：
 *  - `ttellraw/数据组件.txt:1238`（1.20.5 加入数据组件）、`:566-569`（damage = 物品的损坏值）
 *  - `ttellraw/nbt1.txt:182`（旧物品 NBT 章节在 1.20.5 及以后不适用）
 *  - minecraft.wiki /w/Data_component_format（1.20.5 "Replaced tag and all underlying
 *    item-specific tags with data components"；`custom_data` = 任意自定义数据）
 */
object VersionDiff {

    /** 版本策略（用户可选，默认 [NbtSyntax.MODERN]）。 */
    enum class NbtSyntax {
        /** 一律按 1.20.5+ 的数据组件写法输出。 */
        MODERN,
        /** 一律按 1.20.4- 的旧 NBT 写法输出。 */
        LEGACY,
        /** 全是新版就新版、全是旧版就旧版、新旧混用问一嘴。 */
        FOLLOW_INPUT;

        companion object {
            /** 配置里的字符串 -> 枚举；非法值一律回落成 MODERN（默认）。 */
            fun from(value: String?): NbtSyntax = when (value) {
                "legacy" -> LEGACY
                "follow" -> FOLLOW_INPUT
                else -> MODERN
            }
        }
    }

    /** 旧写法里"自定义数据"的整体搬运规则：`tag:{…}` -> `components:{"minecraft:custom_data":{…}}`。 */
    const val LEGACY_CUSTOM_DATA_TAG = "tag:"
    const val MODERN_CUSTOM_DATA_KEY = "custom_data"

    /**
     * 键名对照：旧键名 -> 新组件 ID。
     * 注意 `Damage`/`Count` 不只改名、**值的形状也变了**（见 [legacyValueToModern]）。
     */
    val KEY_RENAMES: Map<String, String> = linkedMapOf(
        "Damage" to "minecraft:damage",
        "Count" to "count",
        "Unbreakable" to "unbreakable",
        "Enchantments" to "enchantments",
        "AttributeModifiers" to "attribute_modifiers",
        "CanDestroy" to "can_break",
        "CanPlaceOn" to "can_place_on",
        "HideFlags" to "tooltip_display",
        "RepairCost" to "repair_cost",
        "SkullOwner" to "profile",
        "Potion" to "potion_contents",
        "Charged" to "charged_projectiles",
    )

    /**
     * 只出现在**物品堆栈层级**的键（`id` / `Count` / `tag` 的兄弟），不在 tag 体内。
     */
    val STACK_LEVEL_RENAMES: Map<String, String> = linkedMapOf(
        "Count" to "count",
    )

    /**
     * 出现在 **tag 体内**的键 -> 新组件 ID（1.20.5 起它们都变成平级组件）。
     * `Count` 不在此表（它是堆栈层级）。
     */
    val TAG_BODY_RENAMES: Map<String, String> = linkedMapOf(
        "Damage" to "minecraft:damage",
        "Unbreakable" to "unbreakable",
        "Enchantments" to "enchantments",
        "AttributeModifiers" to "attribute_modifiers",
        "CanDestroy" to "can_break",
        "CanPlaceOn" to "can_place_on",
        "HideFlags" to "tooltip_display",
        "RepairCost" to "repair_cost",
        "SkullOwner" to "profile",
        "Potion" to "potion_contents",
        "Charged" to "charged_projectiles",
    )

    /** 旧写法里 `display:<复合>` 的子键 -> 新组件 ID（这三个在新版是**平级**组件，display 壳被拆掉）。 */
    val DISPLAY_CONTAINER = "display"

    /** `display.<子键>` -> 新组件 ID（旧写法把这三个塞在 display 复合里）。 */
    val DISPLAY_RENAMES: Map<String, String> = linkedMapOf(
        "Name" to "custom_name",
        "Lore" to "lore",
        "color" to "dyed_color",
    )

    /** 新组件 ID -> 旧键名（反向用；同名冲突时取先注册的那个）。 */
    val MODERN_TO_LEGACY_KEY: Map<String, String> = buildMap {
        for ((legacy, modern) in KEY_RENAMES) putIfAbsent(modern, legacy)
        for ((sub, modern) in DISPLAY_RENAMES) putIfAbsent(modern, "display.$sub")
    }

    /**
     * 旧值 -> 新值（形状也变了的逐键实现）。
     * 返回 null 表示"只改键名、值原样 + 提醒"（不为难自己猜）。
     * 已实现：`Count:1b` -> `count:1`（1.20.5 起 count 是整数，实测 v2=0 / v3=1）。
     */
    fun legacyValueToModern(legacyKey: String, legacyValue: String): String? = when (legacyKey) {
        "Count", "Damage", "RepairCost" ->
            stripByteSuffix(legacyValue)?.takeIf { it.toIntOrNull() != null }
        // 附魔：旧的 [{id:"x",lvl:N}] -> 新的 {levels:{"x":N}}
        "Enchantments" -> legacyEnchantmentsToModern(legacyValue)
        // 药水：旧的字符串 id -> 新的 {potion:"…"}
        "Potion" -> legacyValue.trim().removeSurrounding("\"")
            .takeIf { it.isNotBlank() }?.let { "{potion:\"$it\"}" }
        else -> null
    }

    private fun stripByteSuffix(v: String): String? =
        v.trim().removeSuffix("b").removeSuffix("B").trim().takeIf { it.isNotBlank() }

    /** `[{id:"minecraft:x",lvl:2},{…}]` -> `{levels:{"minecraft:x":2,…}}`。解析不了返回 null。 */
    private fun legacyEnchantmentsToModern(value: String): String? {
        val body = value.trim().removeSurrounding("[", "]")
        if (value.trim() == "[]") return "{levels:{}}"
        val entries = ExecCondSupport.splitTopLevel(body, ',')
        if (entries.isEmpty()) return null
        val pairs = mutableListOf<String>()
        for (e in entries) {
            val id = Regex("id\\s*:\\s*\"([^\"]+)\"").find(e)?.groupValues?.get(1) ?: return null
            val lvl = Regex("lvl\\s*:\\s*(-?\\d+)").find(e)?.groupValues?.get(1) ?: return null
            pairs.add("\"$id\":$lvl")
        }
        return "{levels:{${pairs.joinToString(",")}}}"
    }

    /**
     * 新值 -> 旧值。返回 null 表示"只改键名、值原样 + 提醒"。
     * 已实现：`count:1` -> `Count:1b`。
     */
    fun modernValueToLegacy(modernKey: String, modernValue: String): String? = when (modernKey.removePrefix("minecraft:")) {
        "count" -> modernValue.trim().takeIf { it.toIntOrNull() != null }?.let { "${it}b" }
        "damage", "repair_cost" -> modernValue.trim().takeIf { it.toIntOrNull() != null }
        // 附魔：新的 {levels:{"x":N}} -> 旧的 [{id:"x",lvl:N}]
        "enchantments" -> modernEnchantmentsToLegacy(modernValue)
        // 药水：新的 {potion:"…"} -> 旧的字符串 id
        "potion_contents" -> Regex("potion\\s*:\\s*\"([^\"]+)\"").find(modernValue)?.groupValues?.get(1)
        else -> null
    }

    /** `{levels:{"minecraft:x":2}}` -> `[{id:"minecraft:x",lvl:2}]`。解析不了返回 null。 */
    private fun modernEnchantmentsToLegacy(value: String): String? {
        val m = Regex("levels\\s*:\\s*\\{").find(value) ?: return null
        val open = m.range.last
        val end = matchBrace(value, open) ?: return null
        val body = value.substring(open + 1, end)
        if (body.isBlank()) return "[]"
        val out = mutableListOf<String>()
        for (e in ExecCondSupport.splitTopLevel(body, ',')) {
            val i = e.lastIndexOf(':')
            if (i < 0) return null
            val id = e.substring(0, i).trim().removeSurrounding("\"")
            val lvl = e.substring(i + 1).trim().toIntOrNull() ?: return null
            out.add("{id:\"$id\",lvl:$lvl}")
        }
        return "[${out.joinToString(",")}]"
    }

    /**
     * 旧 `tag:{…}` 的复合体 -> 新写法的组件列表（每个元素形如 `"minecraft:damage":3`）。
     *
     * 规则（wiki："Replaced tag and all underlying item-specific tags with data components"）：
     *  - **认得的原版键** -> 各自变成平级组件（值形状按表换算；换算不了的**只改键名**，键名交给调用方提醒）
     *  - `display:{…}` 里的 Name/Lore/color -> 平级的 custom_name / lore / dyed_color（display 壳拆掉）；
     *    display 里其它子键仍算自定义数据
     *  - `Unbreakable:1b` -> `unbreakable:{}`（存在即"不毁"）；`0b` -> 新版就是"没有该组件"，不输出
     *  - **不认得的键** -> 收集进 `custom_data`（wiki：custom_data = 任意自定义数据）
     *
     * 返回 (组件列表, 值形状没能换算的键名列表)；花括号不配对返回 null（不猜）。
     */
    fun legacyTagBodyToComponentMap(body: String): Pair<List<String>, List<String>>? {
        val components = mutableListOf<String>()
        val custom = mutableListOf<String>()
        val unshaped = mutableListOf<String>()

        fun putComponent(modernId: String, key: String, rawValue: String) {
            val v = legacyValueToModern(key, rawValue) ?: run { unshaped.add(key); rawValue }
            components.add("\"$modernId\":$v")
        }

        for (entry in ExecCondSupport.splitTopLevel(body, ',')) {
            val kv = splitKeyValue(entry)
            if (kv == null) { custom.add(entry); continue }
            val key = kv.first
            val value = kv.second

            if (key == DISPLAY_CONTAINER && value.startsWith("{")) {
                val end = matchBrace(value, 0) ?: return null
                val rest = mutableListOf<String>()
                for (sub in ExecCondSupport.splitTopLevel(value.substring(1, end), ',')) {
                    val j = ExecCondSupport.indexOfTopLevel(sub, ':')
                    if (j < 0) { rest.add(sub); continue }
                    val subKey = sub.substring(0, j).trim().removeSurrounding("\"")
                    val subVal = sub.substring(j + 1).trim()
                    val modern = DISPLAY_RENAMES[subKey]
                    if (modern == null) rest.add(sub) else putComponent(modern, "$DISPLAY_CONTAINER.$subKey", subVal)
                }
                if (rest.isNotEmpty()) custom.add("$DISPLAY_CONTAINER:{${rest.joinToString(",")}}")
                continue
            }

            if (key == "Unbreakable") {
                when (value.trim().removeSuffix("b").removeSuffix("B").trim()) {
                    "1", "true" -> components.add("\"unbreakable\":{}")
                    "0", "false" -> { /* 旧版"不毁=假" -> 新版 = 没有这个组件 */ }
                    else -> { unshaped.add(key); components.add("\"unbreakable\":$value") }
                }
                continue
            }

            val modern = TAG_BODY_RENAMES[key]
            if (modern != null) putComponent(modern, key, value) else custom.add(entry)
        }

        if (custom.isNotEmpty()) {
            components.add("\"minecraft:$MODERN_CUSTOM_DATA_KEY\":{${custom.joinToString(",")}}")
        }
        return components to unshaped
    }

    /**
     * 兼容旧调用：把 tag 体直接转成 `components:{…}` 文本。
     * （不认识的键进 custom_data；解析失败时整体按自定义数据处理，与旧行为一致。）
     */
    fun legacyTagBodyToComponents(body: String): String {
        val parsed = legacyTagBodyToComponentMap(body)
            ?: return "components:{\"minecraft:$MODERN_CUSTOM_DATA_KEY\":{$body}}"
        // 一个组件都没有（例如旧版写着 Unbreakable:0b = "不毁=假" = 新版默认状态）
        // -> 返回空串，由调用方把整个 tag:{} 去掉（并清掉多余的逗号）
        if (parsed.first.isEmpty()) return ""
        return "components:{${parsed.first.joinToString(",")}}"
    }

    /**
     * 新 `components:{…}` 的复合体 -> 旧 `tag:{…}` 的复合体（LEGACY 模式用，正向的镜像）。
     *  - `minecraft:custom_data:{…}` -> 它的键**摊回** tag
     *  - 认得的组件 -> 旧键名（值形状按 [modernValueToLegacy] 反向换算；换算不了**只改键名**）
     *  - `custom_name` / `lore` / `dyed_color` -> 包进 `display:{…}`
     *  - `unbreakable:{}` -> `Unbreakable:1b`
     *  - 堆栈层级的组件（如 `count`）不该出现在 tag 里 -> 归到"无处安放"，由调用方提醒
     *
     * 返回 (tag 体, 值形状未换算的键, 无处安放的组件)；花括号不配对返回 null（不猜）。
     */
    fun modernComponentsToLegacyTag(componentsBody: String): Triple<String, List<String>, List<String>>? {
        val tagEntries = mutableListOf<String>()
        val unshaped = mutableListOf<String>()
        val unmappable = mutableListOf<String>()

        fun unwrap(body: String, openAt: Int): String? =
            matchBrace(body, openAt)?.let { body.substring(openAt + 1, it) }

        fun putLegacy(legacyKey: String, modernKey: String, rawValue: String, wrap: String? = null) {
            val v = modernValueToLegacy(modernKey, rawValue) ?: run { unshaped.add(legacyKey); rawValue }
            val entry = "$legacyKey:$v"
            if (wrap == null) tagEntries.add(entry) else tagEntries.add("$wrap:{$entry}")
        }

        val displayParts = mutableListOf<String>()
        for (entry in ExecCondSupport.splitTopLevel(componentsBody, ',')) {
            val kv = splitKeyValue(entry)
            if (kv == null) { unmappable.add(entry); continue }
            // 新组件 ID 可能带命名空间（minecraft:enchantments）——表里统一按不带命名空间查
            val key = kv.first.removePrefix("minecraft:")
            val value = kv.second

            when {
                // 自定义数据摊回 tag
                key == MODERN_CUSTOM_DATA_KEY -> {
                    if (!value.startsWith("{")) { unmappable.add(key); continue }
                    val inner = unwrap(value, 0) ?: return null
                    tagEntries.addAll(ExecCondSupport.splitTopLevel(inner, ','))
                }
                key == "unbreakable" -> {
                    tagEntries.add("Unbreakable:1b")
                }
                key in DISPLAY_RENAMES.values -> {
                    val legacySub = DISPLAY_RENAMES.entries.firstOrNull { it.value == key }?.key
                    if (legacySub == null) {
                        unmappable.add(key)
                    } else {
                        // 旧格式只有一个 display 复合 -> 先攒着，最后合并
                        val v = modernValueToLegacy(key, value) ?: run { unshaped.add(key); value }
                        displayParts.add("$legacySub:$v")
                    }
                }
                key in STACK_LEVEL_RENAMES.values -> {
                    // count 是物品堆栈层级，不该塞进 tag
                    unmappable.add(key)
                }
                else -> {
                    val legacy = MODERN_TO_LEGACY_KEY[key] ?: MODERN_TO_LEGACY_KEY["minecraft:$key"]
                    if (legacy == null || legacy.startsWith("$DISPLAY_CONTAINER.")) unmappable.add(key)
                    else putLegacy(legacy, key, value)
                }
            }
        }
        if (displayParts.isNotEmpty()) {
            tagEntries.add("$DISPLAY_CONTAINER:{${displayParts.joinToString(",")}}")
        }
        return Triple(tagEntries.joinToString(","), unshaped, unmappable)
    }

    /** 正向：旧 tag 体 -> 新 components 体（含"值形状未换算"的键名）。 */
    fun legacyTagBodyToComponentsBody(body: String): Pair<String, List<String>>? {
        val parsed = legacyTagBodyToComponentMap(body) ?: return null
        return parsed.first.joinToString(",") to parsed.second
    }

    /**
     * 把一条 `键:值` 拆开。**键可能带命名空间**（`minecraft:custom_data`），
     * 所以不能简单取第一个冒号——要按"键 = 标识符[:标识符]"的形状匹配。
     * 解析不了返回 null。
     */
    private fun splitKeyValue(entry: String): Pair<String, String>? {
        val t = entry.trim()
        val m = Regex("^\"?([A-Za-z0-9_]+(?::[A-Za-z0-9_./-]+)?)\"?\\s*:\\s*(.*)$", RegexOption.DOT_MATCHES_ALL)
            .find(t) ?: return null
        val key = m.groupValues[1]
        val value = m.groupValues[2].trim()
        return if (value.isEmpty()) null else key to value
    }

    /** 返回 s[open]（'{'）配对 '}' 的下标；找不到返回 null。跳过引号内的字符。 */
    private fun matchBrace(s: String, open: Int): Int? {
        var depth = 0
        var quote: Char? = null
        var i = open
        while (i < s.length) {
            val c = s[i]
            if (quote != null) {
                if (c == '\\' && i + 1 < s.length) { i += 2; continue }
                if (c == quote) quote = null
            } else when (c) {
                '"', '\'' -> quote = c
                '{' -> depth++
                '}' -> { depth--; if (depth == 0) return i }
            }
            i++
        }
        return null
    }
}
