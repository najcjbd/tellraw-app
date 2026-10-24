package com.tellraw.app.util

/**
 * “目标选择器参数对 as 敏感 = 否”时的内容选择器改写：
 *
 * 当 tellraw 自己的目标选择器不需要 as 时，从左到右找内容里的 SELECTOR 组件；
 * 用 [ExecuteConverter.bedrockSelectorNegation] 判断哪些内容选择器需要 as
 * （scores=!、hasitem air/quantity=0 等），第一个作为 as 的驱动，后续条件相同的选择器一起改写：
 *  - 内容选择器：`@a[scores={k=!v},tag=x]` -> `@s[tag=x]`
 *  - 生成条件：`if score @s k matches <全域> unless score @s k matches v`
 *  - 若 tellraw 目标原本是 `@s` -> 改成 `@p`，并在 as 前加 `at @s`
 *
 * 只处理单选择器、无 sep 定义的内容组件；其余保持原样并提醒。
 */
object ContentAsRewriter {

    data class Result(
        val applied: Boolean,
        val targetSelector: String,
        val message: String,
        val extraTokens: List<String>,
        val notes: List<String>
    )

    private data class Candidate(
        val index: Int,
        val asVar: String,
        val conditions: List<String>,
        val newContent: String
    )

    fun rewrite(
        targetSelector: String,
        message: String,
        existingPrefix: String,
        nbtSyntax: VersionDiff.NbtSyntax = VersionDiff.NbtSyntax.MODERN
    ): Result {
        val notes = mutableListOf<String>()
        val no = { Result(false, targetSelector, message, emptyList(), notes) }
        if (!message.contains(TextComponentHelper.MARKER_START)) return no()

        val components = TextComponentHelper.parseTextComponents(message)
        val candidates = mutableListOf<Candidate>()

        components.forEachIndexed { index, component ->
            if (component.type != TextComponentHelper.ComponentType.SELECTOR) return@forEachIndexed
            val content = component.content.trim()
            if (!content.startsWith("@") || !content.contains('[') || ",'sep':" in content) return@forEachIndexed
            val reminders = mutableListOf<String>()
            val neg = ExecuteConverter.bedrockSelectorNegation(
                content, reminders, preferExecute = true, nbtSyntax = nbtSyntax
            ) ?: return@forEachIndexed
            val (cleaned, conditions) = neg
            notes.addAll(reminders)
            if (conditions.isEmpty()) return@forEachIndexed
            val asVar = cleaned.substringBefore('[')
            if (asVar == "@s") {
                notes.add("内容选择器 @s 上的否定条件无法用 as 改写（会把执行者换掉）")
                return@forEachIndexed
            }
            val newContent = "@s" + cleaned.removePrefix(asVar)
            candidates.add(Candidate(index, asVar, conditions, newContent))
        }

        if (candidates.isEmpty()) return no()
        val driver = candidates.first()
        val applied = candidates.filter { it.asVar == driver.asVar && it.conditions == driver.conditions }
        if (applied.size < candidates.size) {
            notes.add("只有部分内容选择器使用同一个 as/条件，其余保持原样（请自行核对）")
        }

        val newComponents = components.toMutableList()
        for (c in applied) {
            newComponents[c.index] = components[c.index].copy(content = c.newContent)
        }
        val newMessage = TextComponentHelper.componentsToText(newComponents)

        val trimmedTarget = targetSelector.trim()
        val targetUsesS = trimmedTarget == "@s" || trimmedTarget.startsWith("@s[")
        val newTarget = if (targetUsesS) "@p" + trimmedTarget.removePrefix("@s") else trimmedTarget

        val tokens = mutableListOf<String>()
        if (targetUsesS) {
            tokens.addAll(listOf("at", "@s"))
        }
        tokens.addAll(listOf("as", driver.asVar))
        tokens.addAll(driver.conditions)

        if (Regex("\\bas\\b").containsMatchIn(existingPrefix)) {
            notes.add("输入里原本已有 as；新的 as 会追加在其后，请确认执行者")
        }
        return Result(true, newTarget, newMessage, tokens, notes)
    }
}
