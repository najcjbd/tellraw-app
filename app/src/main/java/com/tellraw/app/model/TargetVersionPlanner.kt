package com.tellraw.app.model

/**
 * 按目标版本规划输出（V2）：
 *  - tellraw 不可用 -> 改 `say`；
 *  - 用到的特性越界 -> 逐条提醒（调用方据此转换或舍弃）。
 */
object TargetVersionPlanner {

    enum class OutputKind { TELLRAW, SAY }

    data class Plan(val kind: OutputKind, val notes: List<String>)

    fun plan(
        target: VersionSpec?,
        uses: Set<FeatureGate.Feature> = emptySet()
    ): Plan {
        val notes = mutableListOf<String>()
        if (!FeatureGate.isAvailable(FeatureGate.Feature.TELLRAW, target)) {
            val where = target?.let { "${it.edition} ${it.parts.joinToString(".")}" } ?: "目标版本"
            notes.add("$where 不支持 tellraw（Java 1.7.2+ / 基岩 1.9.0+ 才有）-> 已改用 say")
            return Plan(OutputKind.SAY, notes)
        }
        for (f in uses) {
            if (!FeatureGate.isAvailable(f, target)) {
                notes.add("目标版本不支持特性 $f -> 需按版本转换，或舍弃并提醒")
            }
        }
        return Plan(OutputKind.TELLRAW, notes)
    }

    /**
     * 前置 execute 前缀里用了目标版本没有的子命令 -> 返回该写法的名字（如 `items`）；
     * 都可用 / 目标是最新版 -> null。无法等效改写的整段丢弃并提醒。
     */
    fun unsupportedExecuteToken(prefix: String, target: VersionSpec?): String? {
        if (target == null || target == VersionSpec.LATEST) return null
        if (!FeatureGate.isAvailable(FeatureGate.Feature.EXECUTE_MODERN, target) &&
            prefix.trimStart().startsWith("execute")
        ) {
            return "execute"
        }
        val checks = listOf(
            "items" to FeatureGate.Feature.EXECUTE_ITEMS,
            "stopwatch" to FeatureGate.Feature.EXECUTE_STOPWATCH,
            "slots" to FeatureGate.Feature.EXECUTE_SLOTS,
            "function" to FeatureGate.Feature.EXECUTE_IF_FUNCTION
        )
        for ((token, feature) in checks) {
            if (Regex("\\b$token\\b").containsMatchIn(prefix) &&
                !FeatureGate.isAvailable(feature, target)
            ) {
                return token
            }
        }
        return null
    }
}
