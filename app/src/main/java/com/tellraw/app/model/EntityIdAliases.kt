package com.tellraw.app.model

/**
 * Java / 基岩实体 ID 对照（D5 扩展）。
 *
 * 两个版本对同一实体可能使用不同 ID（例如 Java `zombified_piglin` / 基岩 `zombie_pigman`）。
 * 本表用于：
 *  - 输入里的两种 ID 都能被识别（网格、求值、显示名、族）；
 *  - 按目标版本把 ID 改写成该版本认可的那个；
 *  - 同一条输入里同时出现两版 ID 时提醒用户（多半是写重了或混用了两版语法）。
 *
 * 只收录**确认存在差异**的条目；两版相同的 ID 走普通路径，不在此表。
 */
object EntityIdAliases {

    data class Alias(
        val java: Set<String>,
        val bedrock: Set<String>,
        val note: String? = null
    )

    private fun n(id: String): String {
        val t = id.trim().removeSurrounding("\"").lowercase()
        return if (t.contains(':')) t else "minecraft:$t"
    }

    private val BOAT_JAVA = setOf(
        "minecraft:oak_boat", "minecraft:spruce_boat", "minecraft:birch_boat",
        "minecraft:jungle_boat", "minecraft:acacia_boat", "minecraft:dark_oak_boat",
        "minecraft:mangrove_boat", "minecraft:cherry_boat", "minecraft:bamboo_raft",
        "minecraft:pale_oak_boat", "minecraft:poplar_boat"
    )
    private val CHEST_BOAT_JAVA = setOf(
        "minecraft:oak_chest_boat", "minecraft:spruce_chest_boat", "minecraft:birch_chest_boat",
        "minecraft:jungle_chest_boat", "minecraft:acacia_chest_boat",
        "minecraft:dark_oak_chest_boat", "minecraft:mangrove_chest_boat",
        "minecraft:cherry_chest_boat", "minecraft:bamboo_chest_raft",
        "minecraft:pale_oak_chest_boat", "minecraft:poplar_chest_boat"
    )

    private val GROUPS: List<Alias> = listOf(
        Alias(setOf("minecraft:zombified_piglin"), setOf("minecraft:zombie_pigman"), "基岩旧 ID"),
        Alias(setOf("minecraft:evoker"), setOf("minecraft:evocation_illager"), "基岩旧 ID"),
        Alias(
            setOf("minecraft:vindicator"),
            setOf("minecraft:vindicator", "minecraft:vindication_illager"), "基岩旧 ID"
        ),
        Alias(setOf("minecraft:villager"), setOf("minecraft:villager_v2", "minecraft:villager"), null),
        Alias(
            setOf("minecraft:zombie_villager"),
            setOf("minecraft:zombie_villager_v2", "minecraft:zombie_villager"), null
        ),
        Alias(setOf("minecraft:experience_orb"), setOf("minecraft:xp_orb"), null),
        Alias(setOf("minecraft:experience_bottle"), setOf("minecraft:xp_bottle"), null),
        Alias(setOf("minecraft:potion"), setOf("minecraft:splash_potion"), "投掷药水"),
        Alias(setOf("minecraft:trident"), setOf("minecraft:thrown_trident"), "投掷三叉戟"),
        Alias(setOf("minecraft:fishing_bobber"), setOf("minecraft:fishing_hook"), null),
        Alias(
            setOf("minecraft:eye_of_ender"),
            setOf("minecraft:eye_of_ender_signal"), null
        ),
        Alias(setOf("minecraft:evoker_fangs"), setOf("minecraft:evocation_fang"), null),
        Alias(setOf("minecraft:end_crystal"), setOf("minecraft:ender_crystal"), null),
        Alias(
            setOf("minecraft:wind_charge"),
            setOf("minecraft:wind_charge_projectile"), null
        ),
        Alias(
            setOf("minecraft:breeze_wind_charge"),
            setOf("minecraft:breeze_wind_charge_projectile"), null
        ),
        Alias(BOAT_JAVA, setOf("minecraft:boat"), "基岩用统一的 boat"),
        Alias(CHEST_BOAT_JAVA, setOf("minecraft:chest_boat"), "基岩用统一的 chest_boat")
    )

    private val ID_TO_GROUP: Map<String, Alias> = buildMap {
        for (g in GROUPS) {
            for (id in g.java + g.bedrock) put(n(id), g)
        }
    }

    /** 该 ID 属于哪个对照组（没有则 null）。 */
    fun groupOf(id: String): Alias? = ID_TO_GROUP[n(id)]

    /** 统一到 Java 侧的代表 ID（用于显示名/族/求值匹配）。 */
    fun canonical(id: String): String {
        val t = n(id)
        val g = ID_TO_GROUP[t] ?: return t
        return g.java.first()
    }

    fun isKnown(id: String): Boolean = ID_TO_GROUP.containsKey(n(id))

    /**
     * 转成 Java 版可用的 ID。无法用单一 Java ID 表达（如基岩统一的 boat）时返回 null，
     * 由调用方丢弃并提醒。
     */
    fun toJava(id: String): String? {
        val raw = id.trim().removeSurrounding("\"").lowercase()
        val t = n(raw)
        val g = ID_TO_GROUP[t] ?: return raw
        if (t in g.java) return raw
        return g.java.singleOrNull()
    }

    /** 转成基岩版可用的 ID（优先第一个基岩 ID）。 */
    fun toBedrock(id: String): String? {
        val raw = id.trim().removeSurrounding("\"").lowercase()
        val t = n(raw)
        val g = ID_TO_GROUP[t] ?: return raw
        // 有多个基岩写法时优先用当前版本的那个（如 villager_v2 / vindicator）
        return g.bedrock.firstOrNull()
    }

    /** 另一版本的对应 ID（没有对照关系时 null）。 */
    fun counterpart(id: String): String? {
        val t = n(id)
        val g = ID_TO_GROUP[t] ?: return null
        return if (t in g.java) g.bedrock.firstOrNull() else g.java.firstOrNull()
    }

    /** 该 ID 是否是"只在某一个版本里才这么写"的别名。 */
    fun isAliasOfOtherEdition(id: String): Boolean {
        val t = n(id)
        val g = ID_TO_GROUP[t] ?: return false
        return (t in g.bedrock && t !in g.java) || (t in g.java && t !in g.bedrock)
    }

    /** 只提取顶层 type= 的实体 ID（忽略 scores={...,type=...}、nbt={...} 内部）。 */
    private fun typeIds(text: String): List<String> {
        val ids = mutableListOf<String>()
        var brace = 0
        var quote: Char? = null
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (quote != null) {
                if (c == quote) quote = null
                i++; continue
            }
            when (c) {
                '"', '\'' -> { quote = c; i++; continue }
                '{' -> { brace++; i++; continue }
                '}' -> { brace--; i++; continue }
            }
            if (brace == 0 && text.startsWith("type", i) &&
                (i == 0 || !(text[i - 1].isLetterOrDigit() || text[i - 1] == '_'))
            ) {
                var j = i + 4
                while (j < text.length && text[j].isWhitespace()) j++
                if (j < text.length && text[j] == '=') {
                    j++
                    while (j < text.length && text[j].isWhitespace()) j++
                    val q = if (j < text.length && text[j] == '"') { j++; '"' } else null
                    val startVal = j
                    while (j < text.length) {
                        val ch = text[j]
                        if (q != null && ch == q) break
                        if (q == null && (ch == ',' || ch == ']' || ch.isWhitespace())) break
                        j++
                    }
                    val raw = text.substring(startVal, j).trim().removePrefix("!")
                    if (raw.isNotBlank() && !raw.startsWith("#")) ids.add(n(raw))
                    i = j + 1
                    continue
                }
            }
            i++
        }
        return ids
    }

    /**
     * 同一条输入里同时出现"同一实体的两版 ID"时返回提醒文案；否则 null。
     * 例：type=zombified_piglin 与 type=zombie_pigman 同时出现。
     */
    fun detectMixedEditionIds(text: String): String? {
        val ids = typeIds(text).distinct()
        if (ids.size < 2) return null
        for ((_, list) in ids.groupBy { canonical(it) }) {
            if (list.size >= 2) {
                return "同一条输入里同时出现了 Java 版和基岩版的实体 ID：" +
                    list.joinToString(" / ") +
                    "（指向同一实体）。已按目标版本统一改写，请确认不是重复或混用。"
            }
        }
        return null
    }
}
