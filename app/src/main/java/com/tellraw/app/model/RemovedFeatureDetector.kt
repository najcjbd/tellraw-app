package com.tellraw.app.model

/**
 * 检测用户输入里"最新版已不支持"的写法（V2 弹窗触发依据）。
 * 命中时给出一个"同版本"建议（该写法仍可用的最后一个版本）。
 */
object RemovedFeatureDetector {

    data class Hit(
        val feature: FeatureGate.RemovedFeature,
        val marker: String,
        val suggested: VersionSpec
    )

    private fun spec(vararg p: Int) = VersionSpec(VersionSpec.Edition.JAVA, p.toList())

    /** 按出现顺序匹配；命中第一个即可。 */
    fun detect(text: String): Hit? {
        val t = text
        if ("map_color" in t) return Hit(FeatureGate.RemovedFeature.MAP_COLOR, "map_color", spec(26, 2))
        if ("HurtByTimestamp" in t) return Hit(FeatureGate.RemovedFeature.HURT_BY_TIMESTAMP, "HurtByTimestamp", spec(1, 21, 11))
        if ("swing_animation" in t) return Hit(FeatureGate.RemovedFeature.SWING_ANIMATION, "swing_animation", spec(26, 2))
        return null
    }
}
