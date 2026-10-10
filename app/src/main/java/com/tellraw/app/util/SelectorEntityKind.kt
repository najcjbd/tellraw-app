package com.tellraw.app.util

/**
 * 判断一个目标选择器**只会选到玩家**、**只会选到非玩家**、还是**无法确定**。
 *
 * 用途：Java 里"玩家主手"只能用 `{SelectedItem:{…}}`，"非玩家主手"要用
 * `{equipment:{mainhand:{…}}}`（2026-10-07 实测口径）。所以转换主手 NBT 时得先知道目标是谁。
 *
 * 只做**能确定才下结论**的保守判断；不确定一律 [Kind.AMBIGUOUS]（交给上层提醒/询问）。
 * （`@n`、`@e[sort=…]`、`@s`、带 `x/y/z/pos` 之类无法从选择器本身看出实体种类的，都是 AMBIGUOUS。）
 */
object SelectorEntityKind {

    enum class Kind { PLAYER_ONLY, NON_PLAYER_ONLY, AMBIGUOUS }

    fun analyze(selector: String): Kind {
        val s = selector.trim()
        val base = s.substringBefore('[').trim()
        val paramsPart = if ('[' in s && s.endsWith("]")) s.substringAfter('[').dropLast(1) else ""

        // 基选择器本身就限定了玩家
        when (base) {
            "@a", "@p", "@r" -> return Kind.PLAYER_ONLY
            "@e", "@s", "@n" -> Unit   // 看 type=
            else -> return Kind.AMBIGUOUS
        }

        if (paramsPart.isBlank()) return Kind.AMBIGUOUS

        val types = ExecCondSupport.splitTopLevel(paramsPart, ',')
            .map { it.trim() }
            .filter { it.startsWith("type=") }
            .map { it.substringAfter("type=").trim() }

        if (types.isEmpty()) return Kind.AMBIGUOUS
        if (types.size > 1) return Kind.AMBIGUOUS   // 多值 type（或/与）无法简单断定

        val t = types[0].removeSurrounding("\"").removeSurrounding("'")
        return when {
            t == "player" || t == "minecraft:player" -> Kind.PLAYER_ONLY
            t.startsWith("!") -> {
                val neg = t.substring(1)
                if (neg == "player" || neg == "minecraft:player") Kind.NON_PLAYER_ONLY else Kind.AMBIGUOUS
            }
            t.startsWith("#") -> Kind.AMBIGUOUS        // 实体标签：成员未知
            t.isBlank() -> Kind.AMBIGUOUS
            else -> Kind.NON_PLAYER_ONLY               // type=<某个非玩家实体>
        }
    }

    /** 该选择器是否**一定**选到玩家（用于主手 NBT 选 SelectedItem）。 */
    fun isCertainlyPlayer(selector: String): Boolean = analyze(selector) == Kind.PLAYER_ONLY

    /** 该选择器是否**一定**选到非玩家（用于主手 NBT 选 equipment.mainhand）。 */
    fun isCertainlyNonPlayer(selector: String): Boolean = analyze(selector) == Kind.NON_PLAYER_ONLY
}
