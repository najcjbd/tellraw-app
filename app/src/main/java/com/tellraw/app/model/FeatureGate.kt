package com.tellraw.app.model

/**
 * 特性 -> 版本门槛（来源：zh.minecraft.wiki 各页历史 + 实测，见 need/版本感知实施清单.txt）。
 * 用于：按目标版本决定"能不能用该语法/命令"，越界时转换或舍弃并提醒。
 */
object FeatureGate {

    /** 某特性在某平台上的最低版本；null = 该平台不支持。 */
    data class Min(val java: VersionSpec?, val bedrock: VersionSpec?)

    enum class Feature {
        TELLRAW,               // Java 1.7.2 / Bedrock 1.9.0
        SAY,                   // 两版极早（此处按 0.0 处理，实际无需门槛）
        EXECUTE_MODERN,        // Java 1.13(17w45a) / Bedrock 1.19.50.23（非实验）
        EXECUTE_ITEMS,         // Java 1.20.5(24w10a)；Bedrock 无
        EXECUTE_ITEMS_SLOT_SOURCE, // Java 26.3
        EXECUTE_SLOTS,         // Java 26.3
        EXECUTE_STOPWATCH,     // Java 1.21.11(25w41a)
        EXECUTE_IF_FUNCTION,   // Java 1.19.4(23w31a)
        ITEM_DATA_COMPONENTS,  // Java 1.20.5
        EQUIPMENT_PREDICATE,   // Java 1.21.5
        NBT_PREDICATE,         // Java 1.13
        PREDICATE_PARAM,       // Java 1.15
        ADVANCEMENTS_PARAM,    // Java 1.13
        TEAM_PARAM,            // Java 1.13
        FAMILY_PARAM,          // Bedrock 1.16.100
        HASITEM_PARAM,         // Bedrock 1.18.20
        HASPERMISSION_PARAM,   // Bedrock 1.19.80
        HAS_PROPERTY_PARAM,    // Bedrock 1.20.70
        AT_N,                  // Java 1.21 / Bedrock 1.21.100
    }

    private fun spec(edition: VersionSpec.Edition, vararg parts: Int) = VersionSpec(edition, parts.toList())
    private val J = VersionSpec.Edition.JAVA
    private val B = VersionSpec.Edition.BEDROCK

    private val TABLE: Map<Feature, Min> = mapOf(
        Feature.TELLRAW to Min(spec(J, 1, 7, 2), spec(B, 1, 9, 0)),
        Feature.SAY to Min(spec(J, 0), spec(B, 0)),
        Feature.EXECUTE_MODERN to Min(spec(J, 1, 13), spec(B, 1, 19, 50, 23)),
        Feature.EXECUTE_ITEMS to Min(spec(J, 1, 20, 5), null),
        Feature.EXECUTE_ITEMS_SLOT_SOURCE to Min(spec(J, 26, 3), null),
        Feature.EXECUTE_SLOTS to Min(spec(J, 26, 3), null),
        Feature.EXECUTE_STOPWATCH to Min(spec(J, 1, 21, 11), null),
        Feature.EXECUTE_IF_FUNCTION to Min(spec(J, 1, 19, 4), null),
        Feature.ITEM_DATA_COMPONENTS to Min(spec(J, 1, 20, 5), null),
        Feature.EQUIPMENT_PREDICATE to Min(spec(J, 1, 21, 5), null),
        Feature.NBT_PREDICATE to Min(spec(J, 1, 13), null),
        Feature.PREDICATE_PARAM to Min(spec(J, 1, 15), null),
        Feature.ADVANCEMENTS_PARAM to Min(spec(J, 1, 13), null),
        Feature.TEAM_PARAM to Min(spec(J, 1, 13), null),
        Feature.FAMILY_PARAM to Min(null, spec(B, 1, 16, 100)),
        Feature.HASITEM_PARAM to Min(null, spec(B, 1, 18, 20)),
        Feature.HASPERMISSION_PARAM to Min(null, spec(B, 1, 19, 80)),
        Feature.HAS_PROPERTY_PARAM to Min(null, spec(B, 1, 20, 70)),
        Feature.AT_N to Min(spec(J, 1, 21), spec(B, 1, 21, 100)),
    )

    /** 目标版本是否支持该特性。version=null（最新）按支持处理。 */
    fun isAvailable(feature: Feature, version: VersionSpec?): Boolean {
        val min = TABLE[feature] ?: return true
        if (version == null || version == VersionSpec.LATEST) return true
        val need = when (version.edition) {
            VersionSpec.Edition.JAVA -> min.java
            VersionSpec.Edition.BEDROCK -> min.bedrock
        } ?: return false
        return version >= need
    }
    // ---------------- 被移除/改名的特性（"输入不被最新版支持"的判定） ----------------

    /** 在某个版本被移除（>= 该版本即不可用）。 */
    enum class RemovedFeature { MAP_COLOR, HURT_BY_TIMESTAMP, SWING_ANIMATION, PREDICATE_CONDITION_KEY }

    private val REMOVED_AT: Map<RemovedFeature, VersionSpec> = mapOf(
        RemovedFeature.MAP_COLOR to spec(J, 26, 3),               // 26.3 移除 map_color
        RemovedFeature.HURT_BY_TIMESTAMP to spec(J, 26, 2),        // 26.2 起改用 ticks_since_last_hurt_by_mob
        RemovedFeature.SWING_ANIMATION to spec(J, 26, 3),          // 26.3 改名 attack_animation
        RemovedFeature.PREDICATE_CONDITION_KEY to spec(J, 26, 3),  // 26.3 谓词 condition -> type
    )

    /** 该特性在目标版本上是否已被移除（version=null 视为最新 -> 已移除）。 */
    fun isRemovedIn(feature: RemovedFeature, version: VersionSpec?): Boolean {
        val at = REMOVED_AT[feature] ?: return false
        if (version == null) return true
        if (version == VersionSpec.LATEST) return true
        return version.edition == VersionSpec.Edition.JAVA && version >= at
    }

}