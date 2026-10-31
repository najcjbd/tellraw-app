package com.tellraw.app.model

/**
 * 目标版本。支持 Java/基岩两种命名（按 zh.minecraft.wiki）：
 *  - `26.1` / `26.2` / `26.3`      -> Java 26.x（新式命名，无前导 1）
 *  - `1.21.2` / `1.21.11`          -> Java 1.21.x（补丁号一般不大）
 *  - `1.26.1`                      -> Bedrock 26.1（基岩永远带前导 1；Java 26.x 写作 26.x）
 *  - `1.21.131`                    -> Bedrock 1.21.131（补丁号很大）
 *  - `1.21.130.1`（四段）          -> Bedrock
 *  - 可用 `java:` / `bedrock:` 前缀**显式**指定，避免歧义。
 * 配置默认 `-1` 表示"最新版"（[LATEST]）。
 *
 * 注意：`1.21.2` 与 Bedrock `1.21.2` 字面上撞车，按你的口径默认判 Java；要用基岩请写前缀。
 */
data class VersionSpec(val edition: Edition, val parts: List<Int>) : Comparable<VersionSpec> {

    enum class Edition { JAVA, BEDROCK }

    fun atLeast(major: Int, vararg rest: Int): Boolean = compareTo(VersionSpec(edition, listOf(major) + rest.toList())) >= 0

    override fun compareTo(other: VersionSpec): Int {
        val a = parts
        val b = other.parts
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x.compareTo(y)
        }
        return 0
    }

    companion object {
        /** 最新版哨兵（配置 -1）。 */
        val LATEST = VersionSpec(Edition.JAVA, listOf(Int.MAX_VALUE))

        fun parse(raw: String?): VersionSpec? {
            val s = raw?.trim().orEmpty()
            if (s.isEmpty()) return null
            if (s == "-1") return LATEST

            var t = s
            var explicit: Edition? = null
            when {
                t.startsWith("java:", true) -> { explicit = Edition.JAVA; t = t.substring(5) }
                t.startsWith("bedrock:", true) -> { explicit = Edition.BEDROCK; t = t.substring(8) }
            }
            val rawParts = t.split('.')
            // 每段都必须是纯数字；像 1.20.5-pre1 这种预发布号不能截断成 1.20
            if (rawParts.any { it.trim().toIntOrNull() == null }) return null
            val nums = rawParts.map { it.trim().toInt() }
            if (nums.size < 2) return null

            if (explicit != null) return VersionSpec(explicit, nums)

            // 无前缀：按命名规则推断
            val edition = when {
                nums[0] != 1 -> Edition.JAVA                       // 26.1 等
                nums.size >= 4 -> Edition.BEDROCK                  // 1.21.130.1
                nums[1] >= 26 -> Edition.BEDROCK                   // 1.26.1 = Bedrock 26.1
                nums.size >= 3 && nums[2] >= 100 -> Edition.BEDROCK // 1.21.131
                else -> Edition.JAVA                               // 1.21.2 / 1.21.11
            }
            return VersionSpec(edition, nums)
        }
    }
}
