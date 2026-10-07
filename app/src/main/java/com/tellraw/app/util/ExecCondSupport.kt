package com.tellraw.app.util

/**
 * execute 条件子命令互转用的纯字符串工具（无 Android 依赖）。
 *
 * 只服务 [ExecuteConverter]：把 Java 的 `if/unless items|data`、物品谓词、NBT 主片段
 * 映射成基岩的 hasitem 子选项，以及反向映射。写法沿用 SelectorConverter 的既有约定：
 *  - Java Inventory 槽位 0-8  -> 基岩 location=slot.hotbar,slot=0-8
 *  - Java Inventory 槽位 9-35 -> 基岩 location=slot.inventory,slot=(N-9)
 *  - 玩家主手 SelectedItem    -> location=slot.weapon.mainhand,slot=0
 *  - equipment.{head,chest,legs,feet,offhand} -> slot.armor.* / slot.weapon.offhand
 *  - hasitem 参数顺序：item,location,slot,quantity
 *
 * 本文件不做"选择器整体"的解析（那是 SelectorConverter 的事），只处理单个
 * hasitem 对象 / 单个物品谓词 / 单个 NBT 主片段这类"条件参数"。
 */
internal object ExecCondSupport {

    // ============================ 通用 ============================

    fun stripNs(id: String): String =
        if (id.startsWith("minecraft:")) id.substring("minecraft:".length) else id

    fun addNs(id: String): String =
        if (id.contains(':')) id else "minecraft:$id"

    /**
     * 基岩 hasitem 的"物品" -> Java NBT 主片段，用于**没写 location**（= 目标的所有物品栏）的场合。
     *
     * 依据（2026-09-27 游戏内实测）：Java 的 items **没有裸 `*` 这种槽位源**（J3 失败原因是"* 不存在"），
     * 而第十节【有/无某物品】的口径本来就是"只要有没有这个物品（不限槽位）-> 用 data"。
     * `Inventory` = 物品栏 36 格，**不含副手/装备那 5 格**（B12）。
     */
    fun hasitemToJavaNbt(itemId: String): String = "{Inventory:[{id:\"${addNs(itemId)}\"}]}"

    /**
     * 基岩 hasitem 对象（`item=…,location=…,slot=…,quantity=…`）-> Java NBT 主片段。
     * 用于修饰子命令的选择器（如 `as @a[hasitem=…]`）在"基岩 -> Java"方向上的互转。
     *
     * 覆盖：无 location（Inventory）、`slot.hotbar.N` / `slot.inventory.N`（Inventory + Slot）、
     * `slot.weapon.mainhand`（SelectedItem）、`slot.weapon.offhand` 与 `slot.armor.*`（新版 equipment）。
     * 其余返回 null（调用方保留原样 + 提醒）。
     *
     * [legacyCount] = true（LEGACY 策略）写回旧版堆叠数 `Count:Nb`。
     * [legacySlots] = true（LEGACY 策略，pre-1.20.5）时，副手/装备改用**玩家 `Inventory` 数字槽**：
     *   头盔=103b、胸甲=102b、护腿=101b、靴子=100b、副手=-106b
     *   （2026-10-07 在 1.20.4 真机实测：这些全 =1；`ArmorItems`/`HandItems` =0；`equipment` 字段当时不存在）。
     * 主手两版都用 `SelectedItem`（1.20.4 实测 =1，无需区分）。
     */
    fun hasitemObjectToJavaNbt(obj: String, legacyCount: Boolean = false, legacySlots: Boolean = false): String? {
        val h = parseHasitemObject(obj)
        val item = h.item ?: return null
        val id = addNs(item)
        // 2026-09-27 实测（Java 26.2）：`count:1b`（老式字节）**不再匹配**，`count:1`（整数）才行
        // （1.20.5 起物品堆叠改用数据组件）。所以这里写整数、不加 b。
        // [legacyCount] = true（LEGACY 策略）时才写回旧版的 `Count:Nb`（旧版的键是大写 C、值是字节）。
        val cnt = h.quantity?.toIntOrNull()?.let {
            if (legacyCount) "Count:${VersionDiff.modernValueToLegacy("count", it.toString()) ?: "${it}b"}"
            else "count:$it"
        }
        val idPart = if (cnt == null) "id:\"$id\"" else "id:\"$id\",$cnt"
        if (h.location == null) {
            return if (h.slot == null) "{Inventory:[{$idPart}]}" else null
        }
        val n = h.slot?.toIntOrNull()
        val invSlot: (Int) -> String = { "{Inventory:[{Slot:${it}b,$idPart}]}" }
        return when (h.location) {
            "slot.weapon.mainhand" -> "{SelectedItem:{$idPart}}"
            "slot.weapon.offhand" -> if (legacySlots) invSlot(-106) else "{equipment:{offhand:{$idPart}}}"
            "slot.armor.head" -> if (legacySlots) invSlot(103) else "{equipment:{head:{$idPart}}}"
            "slot.armor.chest" -> if (legacySlots) invSlot(102) else "{equipment:{chest:{$idPart}}}"
            "slot.armor.legs" -> if (legacySlots) invSlot(101) else "{equipment:{legs:{$idPart}}}"
            "slot.armor.feet" -> if (legacySlots) invSlot(100) else "{equipment:{feet:{$idPart}}}"
            "slot.hotbar" -> if (n != null) invSlot(n) else null
            "slot.inventory" -> if (n != null) invSlot(n + 9) else null
            // 末影箱在 Java 的 NBT 里是 EnderItems（Slot 就是箱内编号）
            "slot.enderchest" -> if (n != null) "{EnderItems:[{Slot:${n}b,$idPart}]}" else null
            else -> null
        }
    }

    /**
     * 把老式 NBT 里的 `tag:{…}` 改写成 1.20.5+ 的数据组件写法（用户选的"提醒 + 尝试改写"）。
     *  - `tag:{Damage:N}`（旧式损耗值）-> `components:{"minecraft:damage":N}`
     *  - 其它 `tag:{X:V,…}`（自定义数据）-> `components:{"minecraft:custom_data":{X:V,…}}`
     *
     * 依据：minecraft.wiki（1.20.5 "Replaced tag and all underlying item-specific tags with data
     * components"；`minecraft:custom_data` = 任意自定义数据，原例 `iron_sword[custom_data={foo:1}]`；
     * `minecraft:damage` = 已消耗的耐久值）、数据组件.txt:538-542 / 566-569。
     * 注意：原版标签（display / Enchantments / Unbreakable 等）的组件名与原键名**不同**，
     * 只有 Damage 与 damage 同名同义才特判；其余一律按"自定义数据"改写并由调用方提醒。
     *
     * 返回 (结果, 是否发生过改写)。花括号不配对时原样返回（不猜）。
     */
    fun rewriteLegacyTag(text: String): Pair<String, Boolean> {
        var out = text
        var changed = false
        while (true) {
            val start = out.indexOf("tag:{")
            if (start < 0) break
            val open = start + 4
            val end = matchBrace(out, open) ?: break
            val body = out.substring(open + 1, end)
            val replacement = VersionDiff.legacyTagBodyToComponents(body)
            out = if (replacement.isEmpty()) {
                // 整个 tag 被去掉了（例如 Unbreakable:0b）-> 顺手清掉它前面/后面多余的逗号
                val before = out.substring(0, start).trimEnd().removeSuffix(",")
                val after = out.substring(end + 1).trimStart().removePrefix(",")
                before + after
            } else {
                out.substring(0, start) + replacement + out.substring(end + 1)
            }
            changed = true
        }
        return out to changed
    }

    /** [rewriteModernComponents] 的结果。 */
    class ModernRewrite(
        val text: String,
        val changed: Boolean,
        /** 值形状没能换算、只改了键名的旧键名。 */
        val unshaped: List<String>,
        /** 旧版没有对应写法、被丢掉的组件（要提醒）。 */
        val unmappable: List<String>
    )

    /**
     * 反向：把新式 `components:{…}` 改写成旧 `tag:{…}`（LEGACY 模式用）。
     * 与 [rewriteLegacyTag] 对称；用 [VersionDiff.modernComponentsToLegacyTag] 做转换。
     */
    fun rewriteModernComponents(text: String): ModernRewrite {
        var out = text
        var changed = false
        val unshaped = mutableListOf<String>()
        val unmappable = mutableListOf<String>()
        while (true) {
            val start = out.indexOf("components:{")
            if (start < 0) break
            val open = start + "components:".length
            val end = matchBrace(out, open) ?: break
            val body = out.substring(open + 1, end)
            val r = VersionDiff.modernComponentsToLegacyTag(body) ?: break
            unshaped.addAll(r.second)
            unmappable.addAll(r.third)
            val replacement = if (r.first.isEmpty()) "" else "tag:{${r.first}}"
            out = if (replacement.isEmpty()) {
                val before = out.substring(0, start).trimEnd().removeSuffix(",")
                val after = out.substring(end + 1).trimStart().removePrefix(",")
                before + after
            } else {
                out.substring(0, start) + replacement + out.substring(end + 1)
            }
            changed = true
        }
        return ModernRewrite(out, changed, unshaped.distinct(), unmappable.distinct())
    }

    /** 返回 s[open]（'{'）配对 '}' 的下标；找不到返回 null。跳过引号内的字符。 */
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

    /** 按 [sep] 切分，尊重 [] {} "" '' 内部的嵌套；空片段丢弃。 */
    fun splitTopLevel(s: String, sep: Char): List<String> {
        val out = mutableListOf<String>()
        val cur = StringBuilder()
        var depth = 0
        var quote: Char? = null
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (quote != null) {
                cur.append(c)
                if (c == '\\' && i + 1 < s.length) {
                    cur.append(s[i + 1]); i += 2; continue
                }
                if (c == quote) quote = null
                i++; continue
            }
            when (c) {
                '"', '\'' -> { quote = c; cur.append(c) }
                '[', '{' -> { depth++; cur.append(c) }
                ']', '}' -> { depth--; cur.append(c) }
                sep -> if (depth == 0) { out.add(cur.toString().trim()); cur.clear() } else cur.append(c)
                else -> cur.append(c)
            }
            i++
        }
        if (cur.isNotBlank()) out.add(cur.toString().trim())
        return out.filter { it.isNotEmpty() }
    }

    /** 返回第一个顶层 [ch] 的下标；没有返回 -1。 */
    fun indexOfTopLevel(s: String, ch: Char): Int {
        var depth = 0
        var quote: Char? = null
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (quote != null) {
                if (c == '\\' && i + 1 < s.length) { i += 2; continue }
                if (c == quote) quote = null
                i++; continue
            }
            if (c == ch && depth == 0) return i
            when (c) {
                '"', '\'' -> quote = c
                '[', '{' -> depth++
                ']', '}' -> depth--
            }
            i++
        }
        return -1
    }

    /** 顶层是否出现 [chars] 中的任一字符。 */
    private fun hasTopLevel(s: String, chars: Set<Char>): Boolean {
        var depth = 0
        var quote: Char? = null
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (quote != null) {
                if (c == '\\' && i + 1 < s.length) { i += 2; continue }
                if (c == quote) quote = null
                i++; continue
            }
            when (c) {
                '"', '\'' -> quote = c
                '[', '{' -> depth++
                ']', '}' -> depth--
                in chars -> if (depth == 0) return true
            }
            i++
        }
        return false
    }

    // ======================= Java 物品谓词 =======================

    /**
     * 简化后的 Java item_predicate。
     * [itemId] 为 null 表示不能映射到 hasitem（通配 `*`、标签 `#...`）。
     * [count] 是数量测试的原文（如 "1"、"{min:0,max:4}"）；null 表示没有数量约束。
     * [countNegated] 对应 `!count=...`。
     * [reason] 非 null 表示不可转换的原因。
     */
    class JavaPredicate(
        val itemId: String?,
        val count: String?,
        val countNegated: Boolean,
        val reason: String?,
        /** `[damage=N]`：有耐久物品的**已损耗耐久** -> 基岩的 `data=N`（数据组件.txt:569 / wiki）。 */
        val damage: String? = null
    )

    fun parseJavaPredicate(raw: String): JavaPredicate {
        val bracket = indexOfTopLevel(raw, '[')
        val idPart = (if (bracket >= 0) raw.substring(0, bracket) else raw).trim()
        if (idPart.isEmpty()) return JavaPredicate(null, null, false, "物品谓词缺少物品ID")
        if (idPart.startsWith("#")) return JavaPredicate(null, null, false, "物品标签 $idPart 无法用 hasitem 的 item 表示")
        if (idPart == "*") return JavaPredicate(null, null, false, "通配物品 * 无法用 hasitem 的 item 表示（hasitem 的 item 为必填且须为具体物品）")

        if (bracket < 0) return JavaPredicate(idPart, null, false, null)

        val tests = raw.substring(bracket).trim()
        if (!tests.endsWith("]")) return JavaPredicate(null, null, false, "物品谓词括号不完整")
        val inner = tests.substring(1, tests.length - 1).trim()
        if (inner.isEmpty()) return JavaPredicate(idPart, null, false, null)

        // hasitem 只有 quantity / data 两个子选项 -> 只允许"一个 count 测试 + 一个 damage 测试"这一种组合；
        // 出现 | （或）或其它组件测试项 -> 无法映射
        if (hasTopLevel(inner, setOf('|'))) {
            return JavaPredicate(null, null, false, "物品谓词含 | （或）组合，hasitem 表达不了")
        }
        val parts = splitTopLevel(inner, ',')
        if (parts.size > 2) {
            return JavaPredicate(null, null, false, "物品谓词测试项太多（hasitem 只有 quantity / data 两个子选项）")
        }
        var count: String? = null
        var countNeg = false
        var damage: String? = null
        for (part in parts) {
            val t = part.trim()
            if (t == "damage" || t.startsWith("damage=") || t.startsWith("damage~") || t.startsWith("!damage")) {
                damage = parseDamageTest(t) ?: return JavaPredicate(
                    null, null, false, "damage 测试 \"$t\" 无法映射到基岩的 data（data 只支持 0..32767 的单个值）"
                )
                continue
            }
            val cp = parseCountTest(idPart, t)
            if (cp.reason != null) return cp
            count = cp.count
            countNeg = cp.countNegated
        }
        return JavaPredicate(idPart, count, countNeg, null, damage)
    }

    /**
     * Java 物品谓词里的 damage 测试 -> 基岩 `data` 值。
     * `damage=N` -> N；`!damage`（没有 damage 组件）与 `damage=0` 都是"未受损" -> "0"；
     * `damage~{…}` 是区间，而基岩 data 只接受 0..32767 的**单个值** -> null（调用方提醒）。
     */
    private fun parseDamageTest(t: String): String? {
        if (t.startsWith("!")) return if (t.substring(1).trim() == "damage") "0" else null
        if (t == "damage") return null
        if (t.startsWith("damage=")) return t.substringAfter("damage=").trim().toIntOrNull()?.takeIf { it in 0..32767 }?.toString()
        return null
    }

    private fun parseCountTest(idPart: String, test: String): JavaPredicate {
        var t = test.trim()
        var negated = false
        if (t.startsWith("!")) { negated = true; t = t.substring(1).trim() }
        if (t == "count") return JavaPredicate(idPart, null, false, null) // count 恒真
        if (t.startsWith("count=")) {
            val v = t.substring("count=".length).trim()
            if (v.toIntOrNull() == null) return JavaPredicate(null, null, false, "count=$v 不是整数")
            return JavaPredicate(idPart, v, negated, null)
        }
        if (t.startsWith("count~")) {
            val body = t.substring("count~".length).trim()
            if (!body.startsWith("{") || !body.endsWith("}")) {
                return JavaPredicate(null, null, false, "count~ 只支持 {min:..,max:..} 形式")
            }
            if (negated) return JavaPredicate(null, null, false, "!count~{..} 取反区间无法用 hasitem 表达")
            return JavaPredicate(idPart, body, false, null)
        }
        return JavaPredicate(null, null, false, "物品谓词测试项 \"$test\" 不是 count 测试，无法映射到 hasitem")
    }

    /** 数量测试原文 -> hasitem 的 quantity 值；null 表示无数量约束。 */
    fun quantityForPredicate(pred: JavaPredicate): String? {
        val c = pred.count ?: return null
        if (c.startsWith("{")) {
            val min = Regex("min\\s*:\\s*(-?\\d+)").find(c)?.groupValues?.get(1)
            val max = Regex("max\\s*:\\s*(-?\\d+)").find(c)?.groupValues?.get(1)
            return when {
                min != null && max != null -> "$min..$max"
                min != null -> "$min.."
                max != null -> "..$max"
                else -> null
            }
        }
        return if (pred.countNegated) "!$c" else c
    }

    /**
     * 组装针对 Java `items` 的 hasitem 对象内容；pred 不可映射时返回 null。
     *
     * [dropQuantity] = true 时不写 quantity（execute需求 第十节 13：多槽位 + 具体数量时
     * 基岩无法逐槽表达"或"，退化为"该栏里有这个物品"）。
     */
    fun hasitemFromJavaPredicate(
        pred: JavaPredicate,
        location: String?,
        slot: String?,
        dropQuantity: Boolean = false
    ): String? {
        val item = pred.itemId ?: return null
        val parts = mutableListOf("item=${stripNs(item)}")
        if (location != null) parts.add("location=$location")
        if (slot != null) parts.add("slot=$slot")
        if (!dropQuantity) quantityForPredicate(pred)?.let { parts.add("quantity=$it") }
        // damage 组件 -> 基岩 data（有耐久物品的损耗值）
        pred.damage?.let { parts.add("data=$it") }
        return parts.joinToString(",")
    }

    // ========================== hasitem ==========================

    class Hasitem(
        val item: String?,
        val quantity: String?,
        val location: String?,
        val slot: String?,
        val reason: String?,
        /** 基岩 `data=`：有耐久物品的**损耗值**（数据组件.txt:569 "物品的损坏值"）；其它物品是旧式变体值。 */
        val data: String? = null
    )

    fun parseHasitemObject(s: String): Hasitem {
        var item: String? = null
        var quantity: String? = null
        var location: String? = null
        var slot: String? = null
        var data: String? = null
        for (part in splitTopLevel(s, ',')) {
            val i = part.indexOf('=')
            if (i < 0) continue
            when (part.substring(0, i).trim()) {
                "item" -> item = part.substring(i + 1).trim().trim('"')
                "quantity" -> quantity = part.substring(i + 1).trim().trim('"')
                "location" -> location = part.substring(i + 1).trim().trim('"')
                "slot" -> slot = part.substring(i + 1).trim().trim('"')
                // 2026-09-27：data 以前被静默丢掉（实测它仍然有效，见自检包 x7=1），现在读进来
                "data" -> data = part.substring(i + 1).trim().trim('"')
            }
        }
        if (item.isNullOrBlank()) return Hasitem(null, quantity, location, slot, "hasitem 缺少必填的 item", data)
        return Hasitem(item, quantity, location, slot, null, data)
    }

    /** quantity -> Java 物品谓词的 count 测试；null 表示无。 */
    fun countTestForQuantity(quantity: String?): String? {
        val q = quantity ?: return null
        return when {
            q.startsWith("!") -> "[!count=${q.substring(1)}]"
            ".." in q -> {
                val parts = q.split("..")
                val min = parts.getOrNull(0)?.takeIf { it.isNotBlank() }
                val max = parts.getOrNull(1)?.takeIf { it.isNotBlank() }
                when {
                    min != null && max != null -> "[count~{min:$min,max:$max}]"
                    min != null -> "[count~{min:$min}]"
                    max != null -> "[count~{max:$max}]"
                    else -> null
                }
            }
            else -> "[count=$q]"
        }
    }

    /**
     * 基岩 `data=` -> Java 物品谓词里的组件测试（**有耐久物品的损耗值**）。
     *
     * 依据：数据组件.txt:566-569（`minecraft:damage` = "物品的损坏值"，值≥0，默认 0）、
     *      谓词.txt:10-12（`<数据组件ID>=<值>`，如 `*[damage=0]`）、
     *      minecraft.wiki（1.20.5 起 `tag` 被数据组件取代；`damage` 是"已消耗的耐久值"）。
     * `data=0`（未受损）在 Java 里是**默认状态**（没有 damage 组件）-> 不加测试，只提醒。
     * 返回 null 表示"没有可加的测试"；非整数也返回 null（由调用方提醒）。
     */
    fun damageTestForData(data: String?): String? {
        val n = data?.trim()?.toIntOrNull() ?: return null
        // 谓词里的测试名来自统一表：KEY_RENAMES["Damage"] = "minecraft:damage" -> "damage"
        val testName = stripNs(VersionDiff.KEY_RENAMES["Damage"] ?: "minecraft:damage")
        return if (n > 0) "$testName=$n" else null
    }

    /** hasitem 对象 -> Java 物品谓词（含命名空间）。count 与 damage 合并成同一个 [] 列表。 */
    fun javaPredicateForHasitem(h: Hasitem): String {
        val id = addNs(h.item!!)
        val tests = mutableListOf<String>()
        countTestForQuantity(h.quantity)?.let { tests.add(it.removeSurrounding("[", "]")) }
        damageTestForData(h.data)?.let { tests.add(it) }
        return if (tests.isEmpty()) id else "$id[${tests.joinToString(",")}]"
    }

    // ========================== 槽位映射 ==========================

    /**
     * Java `items` 的 <slots> -> 基岩 (location, slot)。
     * location/slot 都为 null 表示"任意槽位"（不写 location/slot）。
     * 第三个值非 null 表示不支持的原因。
     *
     * `container.N` 按 execute需求 第十节 6 一律当物品栏 36 格处理：
     *   0-8 -> slot.hotbar（槽位 N）、9-35 -> slot.inventory（槽位 N-9），并提醒玩家。
     */
    fun javaSlotsToHasitem(
        slots: String,
        reminders: MutableList<String>? = null
    ): Triple<String?, String?, String?> {
        val s = slots.trim()
        if (s == "*") {
            return Triple(null, null, null)
        }
        if (s == "container.*") {
            reminders?.add(
                "Java \"container.*\" 覆盖快捷栏 0-8 与物品栏 9-35 共 36 格，跨两个基岩 location，" +
                    "无法用一个 location 表达；已按\"任意槽位\"（不写 location/slot）处理"
            )
            return Triple(null, null, null)
        }
        // Java 的 `inventory.*` / `hotbar.*` 是"整栏通配"：基岩写 location=栏、不写 slot
        //（目标选择器.txt:609 不带 slot 即 slot=0..，语义为"该栏任意槽位有该物品"）。
        if (s == "inventory.*") return Triple("slot.inventory", null, null)
        if (s == "hotbar.*") return Triple("slot.hotbar", null, null)
        if (s == "enderchest.*") return Triple("slot.enderchest", null, null)
        when (s) {
            "weapon.mainhand" -> return Triple("slot.weapon.mainhand", "0", null)
            "weapon.offhand" -> return Triple("slot.weapon.offhand", "0", null)
            "armor.head" -> return Triple("slot.armor.head", "0", null)
            "armor.chest" -> return Triple("slot.armor.chest", "0", null)
            "armor.legs" -> return Triple("slot.armor.legs", "0", null)
            "armor.feet" -> return Triple("slot.armor.feet", "0", null)
        }
        if (s.startsWith("container.")) {
            val n = s.substringAfter("container.").toIntOrNull()
                ?: return Triple(null, null, "容器槽位 \"$s\" 编号不是整数")
            return containerSlotForJavaIndex(n, reminders)
        }
        // Java 的 hotbar.N / inventory.N 本身就是"物品栏"语义，直接对应
        if (s.startsWith("hotbar.")) {
            val n = s.substringAfter("hotbar.").toIntOrNull()
                ?: return Triple(null, null, "槽位 \"$s\" 编号不是整数")
            return Triple("slot.hotbar", "$n", null)
        }
        if (s.startsWith("inventory.")) {
            val n = s.substringAfter("inventory.").toIntOrNull()
                ?: return Triple(null, null, "槽位 \"$s\" 编号不是整数")
            return Triple("slot.inventory", "$n", null)
        }
        if (s.startsWith("enderchest.")) {
            val n = s.substringAfter("enderchest.").toIntOrNull()
                ?: return Triple(null, null, "槽位 \"$s\" 编号不是整数")
            return Triple("slot.enderchest", "$n", null)
        }
        return Triple(null, null, "Java 槽位 \"$s\" 暂无基岩 hasitem 对应写法")
    }

    /**
     * Java 物品栏下标（= `container.N` 的 N，玩家 0-35）-> 基岩 (location, slot)。
     * 0-8 -> slot.hotbar,N；9-35 -> slot.inventory,(N-9)；其余报错。
     * 成功时写一条提醒：`container.N` 语义上是"容器槽位"，这里按物品栏换算，请核对材料。
     */
    fun containerSlotForJavaIndex(
        n: Int,
        reminders: MutableList<String>?
    ): Triple<String?, String?, String?> {
        val loc: String
        val slot: String
        when (n) {
            in 0..8 -> { loc = "slot.hotbar"; slot = "$n" }
            in 9..35 -> { loc = "slot.inventory"; slot = "${n - 9}" }
            else -> return Triple(
                null, null,
                "container.$n 超出物品栏 36 格（0-35），无法映射到 hasitem"
            )
        }
        reminders?.add(
            "container.$n 在 Java 里是\"容器槽位\"，本工具按物品栏 36 格换算为 " +
                "location=$loc,slot=$slot（0-8→slot.hotbar，9-35→slot.inventory(N-9)；" +
                "execute需求 第十节 6）；若原意是别的容器（箱子/实体容器等），请自行核对材料与槽位，避免查错"
        )
        return Triple(loc, slot, null)
    }

    /**
     * 基岩 (location, slot) -> Java `items` 的 <slots>；location 为 null 返回 null（任意槽位）。
     *
     * 只有**纯整数** slot 才算"具体槽位"；slot 是范围（`0..2`）或取反（`!0`）时覆盖多个槽位，
     * Java 的 items 没有槽位集合写法，这里放开为整栏通配（`<栏>.*`），由调用方负责提醒。
     */
    fun hasitemToJavaSlots(location: String?, slot: String?): String? {
        val loc = location ?: return null
        val n = slot?.trim()?.toIntOrNull()
        return when (loc) {
            "slot.weapon.mainhand" -> "weapon.mainhand"
            "slot.weapon.offhand" -> "weapon.offhand"
            "slot.armor.head" -> "armor.head"
            "slot.armor.chest" -> "armor.chest"
            "slot.armor.legs" -> "armor.legs"
            "slot.armor.feet" -> "armor.feet"
            "slot.hotbar" -> if (n != null) "hotbar.$n" else "hotbar.*"
            // slot.inventory.N 对应 Java 物品栏第 N+9 格，即 inventory.N（N 0-26）
            "slot.inventory" -> if (n != null) "inventory.$n" else "inventory.*"
            // 2026-09-27 实测：基岩 location=slot.enderchest 可用 -> Java 对应 enderchest.N / enderchest.*
            "slot.enderchest" -> if (n != null) "enderchest.$n" else "enderchest.*"
            else -> null
        }
    }

    /**
     * slot 形如 `!N`（只取反一个槽位，N 为整数）时返回 N；其余（`!0..2`、`!a`）返回 null。
     * 依据目标选择器.txt:609 —— slot 支持范围与不等式（=!），但 Java 的 items 没有槽位取反写法。
     */
    fun negatedSingleSlot(slot: String?): Int? {
        val s = slot?.trim() ?: return null
        if (!s.startsWith("!")) return null
        return s.substring(1).toIntOrNull()
    }

    /** 基岩 hasitem 里"本身只含一个槽位"的 location（缺 slot 时也唯一，目标选择器.txt:735-740）。 */
    private val SINGLE_SLOT_ITEM_LOCATIONS = setOf(
        "slot.weapon.mainhand", "slot.weapon.offhand",
        "slot.armor.head", "slot.armor.chest", "slot.armor.legs", "slot.armor.feet"
    )

    /** 基岩 hasitem 里"一个 location 含多个槽位"的物品栏（目标选择器.txt:741-748）。 */
    private val MULTI_SLOT_ITEM_LOCATIONS = setOf(
        "slot.hotbar", "slot.inventory", "slot.enderchest",
        "slot.chest", "slot.armor", "slot.equippable"
    )

    /** location 是否"多槽位物品栏"（一个 location 含多个槽位）。 */
    fun isMultiSlotItemLocation(location: String?): Boolean =
        location != null && location.trim() in MULTI_SLOT_ITEM_LOCATIONS

    /** location 是否"本身只含一个槽位"（缺 slot 时也唯一，目标选择器.txt:735-740）。 */
    fun isSingleSlotItemLocation(location: String?): Boolean =
        location != null && location.trim() in SINGLE_SLOT_ITEM_LOCATIONS

    /**
     * 这个 hasitem 对象的落点是否属于"Java 侧只有 1.21.5+ 的 `{equipment:…}` 谓词"那一类
     * （armor.* 与副手）。1.20.5–1.21.4 上该谓词永不匹配 -> 必须改走 `execute if items`。
     * （主手走 SelectedItem、物品栏/末影箱走 Inventory/EnderItems，都不在此列。）
     */
    fun isEquipmentLikeHasitem(obj: String): Boolean {
        val loc = parseHasitemObject(obj).location?.trim() ?: return false
        return loc.startsWith("slot.armor.") || loc == "slot.weapon.offhand"
    }

    /**
     * location 形如 `container.N` / `hotbar.N` / `inventory.N`（编号内嵌在 location 里）时，
     * 返回 (前缀, N)；否则返回 null。
     */
    fun numberedLocation(location: String?): Pair<String, Int>? {
        val loc = location?.trim() ?: return null
        val prefix = when {
            loc.startsWith("container.") -> "container"
            loc.startsWith("hotbar.") -> "hotbar"
            loc.startsWith("inventory.") -> "inventory"
            else -> return null
        }
        val n = loc.substringAfter('.').toIntOrNull() ?: return null
        return prefix to n
    }

    /**
     * 基岩 hasitem 的 (location, slot) 能否**唯一确定一个 Java `items` 槽位**；能则返回 Java
     * `<slots>` 表达式，不能返回 null。
     *
     * 依据 execute需求 第十节 16：`slot` 有默认值 `slot=0..`（目标选择器.txt:609），
     * 所以"缺 slot"**不等于**"无法确定槽位"：
     *  - 单槽位 location（slot.armor.* / slot.weapon.*）：缺 slot 即该唯一槽；
     *  - 带编号 location（container.N / hotbar.N / inventory.N）：缺 slot 即内嵌编号 N；
     *  - 多槽位 location（slot.hotbar / slot.inventory / …）：只有 slot 为单一整数才能唯一确定。
     * 多槽位 location 缺 slot（默认 0..）或 slot 为区间/取反时返回 null。
     */
    fun resolveUniqueSlotJavaSlots(
        location: String?,
        slot: String?,
        reminders: MutableList<String>? = null
    ): String? {
        val loc = location?.trim()
        if (loc.isNullOrEmpty()) return null
        val s = slot?.trim()

        numberedLocation(loc)?.let { (prefix, n) ->
            if (s != null && s != "$n" && !s.contains("..") && !s.contains("!")) {
                reminders?.add(
                    "location=$loc 内嵌的槽位编号为 $n，与 slot=$s 冲突；已以内嵌编号 $n 为准，请自行核对"
                )
            }
            if (prefix == "container") {
                reminders?.add(
                    "location=container.$n 不是基岩 hasitem 的标准物品栏名，已按 Java 容器槽位 container.$n 处理" +
                        "（execute需求 第十节 6：容器按物品栏 36 格，0-8 等价 slot.hotbar、9-35 等价 slot.inventory(N-9)），" +
                        "请自行核对材料与槽位"
                )
            }
            return "$prefix.$n"
        }

        if (loc in SINGLE_SLOT_ITEM_LOCATIONS) {
            if (s != null && (s.contains("..") || s.contains("!"))) {
                reminders?.add("location=$loc 是单槽位，slot=$s 仍只指向该唯一槽位，已按该槽处理")
            }
            return hasitemToJavaSlots(loc, "0")
        }

        if (loc in MULTI_SLOT_ITEM_LOCATIONS) {
            // 缺 slot（默认 0..）或 slot 为区间/取反 -> 覆盖多个槽位，无法唯一确定
            if (s == null || s.contains("..") || s.contains("!")) return null
            val n = s.toIntOrNull() ?: return null
            return when (loc) {
                "slot.hotbar" -> "hotbar.$n"
                // slot.inventory.N 对应 Java 物品栏第 N+9 格，即 inventory.N
                "slot.inventory" -> "inventory.$n"
                "slot.enderchest" -> "enderchest.$n"
                // slot.chest / slot.armor / slot.equippable 只能用在马/驴等实体上，无（玩家）Java items 对应
                else -> null
            }
        }

        if (s == null || s.contains("..") || s.contains("!")) return null
        return hasitemToJavaSlots(loc, s)
    }

    // ======================== NBT 主片段 ========================

    /**
     * NBT 主片段 -> hasitem 的映射结果。
     *
     * [airExistence] = true 表示本次映射用的是"空气法"：得到的 hasitem 对象形如
     * `item=air,location=…,slot=…`，它表示"该槽位是空的"（execute需求 第十节 15）。
     * 调用方必须据此处理 if/unless 极性（基岩 item=air 本身就带"空/否定"含义）。
     */
    class NbtMap(val items: List<String>, val airExistence: Boolean)

    /**
     * Java NBT 主片段 -> 若干 hasitem 对象内容（每个主片段可能展开成多条）。
     * 只识别 SelectedItem / equipment / Inventory（三大主片段）；
     * 不认识的键写提醒并忽略。
     */
    fun nbtToHasitemItems(nbt: String, reminders: MutableList<String>): NbtMap {
        val t = nbt.trim()
        // 老式 tag:{…}：基岩 hasitem 表达不了自定义数据/组件 -> 会被丢掉，必须提醒（不能静默放宽条件）
        if ("tag:" in t) {
            reminders.add(
                "NBT 里含老式 tag:{…}：基岩 hasitem **表达不了**自定义数据/组件，该部分已被丢掉（条件被放宽）。" +
                    "另外 1.20.5 起 Java 也用数据组件取代了 tag（实测：`tag:{}` 在 26.2 上已不匹配）：" +
                    "自定义数据写成 components:{\"minecraft:custom_data\":{…}}，损耗值写成 components:{\"minecraft:damage\":N}"
            )
        }
        // NBT 路径不一定带花括号（execute.txt:1223,1235）：`equipment.head.id`、
        // `Inventory[{Slot:21b,id:"…"}]`、`SelectedItem.id` 等点号/方括号形式单独处理。
        if (!t.startsWith("{")) return nbtPathToHasitemItems(t, reminders)

        val items = mutableListOf<String>()
        val body = t.removeSurrounding("{", "}")
        for (entry in splitTopLevel(body, ',')) {
            val idx = indexOfTopLevel(entry, ':')
            if (idx < 0) continue
            val key = entry.substring(0, idx).trim().trim('"')
            val value = entry.substring(idx + 1).trim()
            when (key) {
                "SelectedItem" -> {
                    val parsed = parseItemObject(value)
                    if (parsed == null) reminders.add("SelectedItem 片段解析失败，已忽略")
                    else items.add(buildItemHasitem(parsed.first, parsed.second, "slot.weapon.mainhand", "0"))
                }
                "equipment" -> items.addAll(parseEquipment(value, reminders))
                "Inventory" -> items.addAll(parseInventory(value, reminders))
                else -> reminders.add("NBT 主片段 \"$key\" 无法映射到基岩 hasitem，已忽略该片段")
            }
        }
        return NbtMap(items, false)
    }

    /**
     * 点号 / 方括号形式的 NBT 路径 -> hasitem 对象（execute.txt:1223,1235）。
     * 只识别 SelectedItem / equipment.<槽位> / Inventory[{Slot:Nb,…}] 三类主片段；
     *  - 路径里能取到**具体物品 id**：正常映射成 `item=<id>…`；
     *  - 纯存在性路径（如 `equipment.head.id` 只表示"该槽有物品"）：改用**空气法**
     *    `item=air,location=…,slot=…`（表示"该槽是空的"，由调用方翻转 if/unless 极性），
     *    并写一条"文档没保证"的提醒（execute需求 第十节 15）。
     */
    private fun nbtPathToHasitemItems(path: String, reminders: MutableList<String>): NbtMap {
        val segs = splitTopLevel(path, '.')
        val head = segs.firstOrNull()?.trim() ?: return NbtMap(emptyList(), false)
        val headKey = head.substringBefore('[').substringBefore('{').trim()

        var location: String? = null
        var slot: String? = null

        when (headKey) {
            "SelectedItem" -> { location = "slot.weapon.mainhand"; slot = "0" }
            "equipment" -> {
                val second = segs.getOrNull(1)?.substringBefore('{')?.substringBefore('[')?.trim()
                val loc = when (second) {
                    "head" -> "slot.armor.head"
                    "chest" -> "slot.armor.chest"
                    "legs" -> "slot.armor.legs"
                    "feet" -> "slot.armor.feet"
                    "offhand" -> "slot.weapon.offhand"
                    "mainhand" -> "slot.weapon.mainhand"
                    else -> null
                }
                if (loc == null) {
                    reminders.add("点号路径 \"$path\" 的装备槽位 \"$second\" 无 hasitem 对应，已忽略该片段")
                    return NbtMap(emptyList(), false)
                }
                location = loc; slot = "0"
            }
            "Inventory" -> {
                val slotNum = Regex("Slot\\s*:\\s*(-?\\d+)").find(head)?.groupValues?.get(1)?.toIntOrNull()
                if (slotNum != null) {
                    val (loc, sl, err) = containerSlotForJavaIndex(slotNum, reminders)
                    if (err != null) { reminders.add(err); return NbtMap(emptyList(), false) }
                    location = loc; slot = sl
                }
                // 无 Slot 过滤 -> 不限槽位（此时无法用空气法确定槽位，下面会走"无法转换"）
            }
            else -> {
                reminders.add("NBT 主片段 \"$headKey\" 无法映射到基岩 hasitem，已忽略该片段（路径：$path）")
                return NbtMap(emptyList(), false)
            }
        }

        val id = Regex("id\\s*:\\s*[\"']([^\"']+)[\"']").find(path)?.groupValues?.get(1)
        if (id == null) {
            // 纯存在性路径："该槽有没有物品"。用空气法：item=air = 该槽是空的。
            if (location == null || slot == null) {
                reminders.add(
                    "点号路径 \"$path\" 只测试\"该槽位是否有物品\"（存在性），但该路径没有限定到单一槽位" +
                        "（如 Inventory 未写 Slot，覆盖多个槽位）；\"空气法\"需要单一槽位，多槽位下" +
                        "\"哪一槽为空\"没有文档定义，该条无法转换为 hasitem"
                )
                return NbtMap(emptyList(), false)
            }
            reminders.add(
                "点号路径 \"$path\" 是\"该槽位是否有物品\"的存在性判定（路径里没有具体物品ID），" +
                    "已用\"空气法\"转换：基岩 hasitem={item=air,location=$location,slot=$slot} 表示\"该槽位是空的\"。" +
                    "风险提醒：文档只在 slot.saddle / 马的 slot.armor / slot.equippable 写明物品名可为空气" +
                    "（目标选择器.txt:744-748），一般物品栏/装备槽写\"物品栏中空气的数量始终是0个\"（:604）；" +
                    "此写法属\"游戏里应该可用但文档没保证\"，请自行核实是否适用"
            )
            return NbtMap(listOf("item=air,location=$location,slot=$slot"), true)
        }
        val count = Regex("[Cc]ount\\s*:\\s*(\\d+)[bB]?").find(path)?.groupValues?.get(1)
        return NbtMap(listOf(buildItemHasitem(id, count, location, slot)), false)
    }

    private fun buildItemHasitem(id: String, count: String?, location: String?, slot: String?): String {
        val parts = mutableListOf("item=${stripNs(id)}")
        if (location != null) parts.add("location=$location")
        if (slot != null) parts.add("slot=$slot")
        if (count != null) parts.add("quantity=$count")
        return parts.joinToString(",")
    }

    private fun parseItemObject(obj: String): Pair<String, String?>? {
        val body = obj.trim().removeSurrounding("{", "}")
        val id = Regex("(?:^|[,{\\s])id\\s*:\\s*[\"']([^\"']+)[\"']").find(body)?.groupValues?.get(1)
            ?: return null
        val count = Regex("(?:^|[,{\\s])[Cc]ount\\s*:\\s*(\\d+)[bB]?").find(body)?.groupValues?.get(1)
        return id to count
    }

    private fun parseEquipment(value: String, reminders: MutableList<String>): List<String> {
        val out = mutableListOf<String>()
        val body = value.trim().removeSurrounding("{", "}")
        for (entry in splitTopLevel(body, ',')) {
            val idx = indexOfTopLevel(entry, ':')
            if (idx < 0) continue
            val key = entry.substring(0, idx).trim().trim('"')
            val location = when (key) {
                "head" -> "slot.armor.head"
                "chest" -> "slot.armor.chest"
                "legs" -> "slot.armor.legs"
                "feet" -> "slot.armor.feet"
                "offhand" -> "slot.weapon.offhand"
                "mainhand" -> "slot.weapon.mainhand"
                else -> null
            }
            if (location == null) {
                reminders.add("equipment 槽位 \"$key\" 无 hasitem 对应，已忽略")
                continue
            }
            val parsed = parseItemObject(entry.substring(idx + 1))
            if (parsed == null) { reminders.add("equipment.$key 解析失败，已忽略"); continue }
            out.add(buildItemHasitem(parsed.first, parsed.second, location, "0"))
        }
        return out
    }

    private fun parseInventory(value: String, reminders: MutableList<String>): List<String> {
        val out = mutableListOf<String>()
        val body = value.trim().removeSurrounding("[", "]")
        for (entry in splitTopLevel(body, ',')) {
            if (entry.isBlank()) continue
            val slotNum = Regex("(?:^|[,{\\s])Slot\\s*:\\s*(-?\\d+)[bB]?").find(entry)?.groupValues?.get(1)?.toIntOrNull()
            val parsed = parseItemObject(entry)
            if (parsed == null) { reminders.add("Inventory 条目解析失败，已忽略：$entry"); continue }
            when {
                slotNum == null ->
                    // 未指定槽位：只测"有该物品"（不限槽位）
                    out.add(buildItemHasitem(parsed.first, parsed.second, null, null))
                slotNum < 0 -> reminders.add("Inventory 负数槽位 $slotNum 已忽略")
                slotNum in 0..8 -> out.add(buildItemHasitem(parsed.first, parsed.second, "slot.hotbar", "$slotNum"))
                slotNum in 9..35 -> out.add(buildItemHasitem(parsed.first, parsed.second, "slot.inventory", "${slotNum - 9}"))
                else -> reminders.add("Inventory 槽位 $slotNum 超出 0-35（物品栏 36 格，不含副手/装备），已忽略")
            }
        }
        return out
    }
}
