package com.tellraw.app.util

/**
 * 网格/预览用的 translate 近似渲染：
 *  - 内置少量常见语言键的中文模板；
 *  - 支持 %s、%1$s、%%；
 *  - 未知键显示 key(args)；
 *  - 不做语言文件查找（本软件没有客户端语言包）。
 */
object TranslatePreview {

    private val TEMPLATES: Map<String, String> = linkedMapOf(
        "chat.type.text" to "%1\$s：%2\$s",
        "chat.type.announcement" to "[%1\$s] %2\$s",
        "chat.type.emote" to "* %1\$s %2\$s",
        "chat.type.admin" to "[%1\$s: %2\$s]",
        "multiplayer.player.joined" to "%s 加入了游戏",
        "multiplayer.player.left" to "%s 离开了游戏",
        "multiplayer.player.joined.renamed" to "%s（原 %s）加入了游戏",
        "item.minecraft.diamond" to "钻石",
        "item.minecraft.diamond_sword" to "钻石剑",
        "item.minecraft.stone" to "石头",
        "block.minecraft.stone" to "石头",
        "entity.minecraft.zombie" to "僵尸",
        "entity.minecraft.player" to "玩家"
    )

    fun render(key: String, args: List<String>): String {
        val template = TEMPLATES[key]
        if (template == null) {
            return if (args.isEmpty()) key else "$key(" + args.joinToString(", ") + ")"
        }
        return format(template, args)
    }

    private fun format(template: String, args: List<String>): String {
        val sb = StringBuilder()
        var i = 0
        var seq = 0
        while (i < template.length) {
            val c = template[i]
            if (c != '%') {
                sb.append(c); i++; continue
            }
            if (i + 1 < template.length && template[i + 1] == '%') {
                sb.append('%'); i += 2; continue
            }
            var j = i + 1
            val num = StringBuilder()
            while (j < template.length && template[j].isDigit()) { num.append(template[j]); j++ }
            if (j < template.length && template[j] == '$') j++
            if (j < template.length && template[j] == 's') {
                val idx = if (num.isNotEmpty()) (num.toString().toIntOrNull() ?: 1) - 1 else seq++
                if (idx in args.indices) sb.append(args[idx])
                else sb.append(template.substring(i, j + 1)) // 缺参数时保留占位符，提示用户
                i = j + 1
                continue
            }
            sb.append(c); i++
        }
        return sb.toString()
    }
}
