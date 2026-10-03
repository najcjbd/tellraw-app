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
        "Count" -> legacyValue.trim().removeSuffix("b").removeSuffix("B").takeIf { it.toIntOrNull() != null }
        else -> null
    }

    /**
     * 新值 -> 旧值。返回 null 表示"只改键名、值原样 + 提醒"。
     * 已实现：`count:1` -> `Count:1b`。
     */
    fun modernValueToLegacy(modernKey: String, modernValue: String): String? = when (modernKey) {
        "count" -> modernValue.trim().takeIf { it.toIntOrNull() != null }?.let { "${it}b" }
        else -> null
    }

    /**
     * 旧 `tag:{…}` 的复合体 -> 新组件写法（整段搬运规则）。
     *  - 只有 `Damage:N` 一项时 -> `components:{"minecraft:damage":N}`（同名同义，特判）
     *  - 其余 -> `components:{"minecraft:custom_data":{…}}`（wiki：custom_data = 任意自定义数据）
     */
    fun legacyTagBodyToComponents(body: String): String {
        val dmg = Regex("^Damage\\s*:\\s*(\\d+)[bBsSlL]?$").find(body.trim())?.groupValues?.get(1)
        return if (dmg != null) {
            "components:{\"${KEY_RENAMES["Damage"]}\":$dmg}"
        } else {
            "components:{\"minecraft:$MODERN_CUSTOM_DATA_KEY\":{$body}}"
        }
    }
}
