package com.tellraw.app.util

import com.tellraw.app.model.MinecraftVersion
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

/**
 * TextFormatter 测试类
 * 测试文本格式化功能，包括§颜色代码转换、JSON格式生成等
 */
class TextFormatterTest {

    private val gson = Gson()

    // ==================== §颜色代码检测测试 ====================

    @Test
    fun `test containsMNCodes - 检测§m代码`() {
        // 测试检测§m代码
        assertTrue(TextFormatter.containsMNCodes("§m测试文本"))
        assertTrue(TextFormatter.containsMNCodes("普通§m文本§r恢复"))
        assertFalse(TextFormatter.containsMNCodes("普通文本"))
    }

    @Test
    fun `test containsMNCodes - 检测§n代码`() {
        // 测试检测§n代码
        assertTrue(TextFormatter.containsMNCodes("§n测试文本"))
        assertTrue(TextFormatter.containsMNCodes("普通§n文本§r恢复"))
        assertFalse(TextFormatter.containsMNCodes("普通文本"))
    }

    @Test
    fun `test containsMNCodes - 同时检测§m和§n`() {
        // 测试同时检测§m和§n
        assertTrue(TextFormatter.containsMNCodes("§m§n测试文本"))
        assertTrue(TextFormatter.containsMNCodes("§m测试§n文本"))
    }

    // ==================== §m§n代码处理测试 ====================

    @Test
    fun `test processMNCodes - Java字体方式`() {
        // 测试Java字体方式
        val result = TextFormatter.processMNCodes("§m§n测试文本", true)
        assertEquals("§m§n测试文本", result.first)
        assertTrue(result.second.isNotEmpty())
    }

    @Test
    fun `test processMNCodes - 颜色代码方式`() {
        // 测试颜色代码方式
        val result = TextFormatter.processMNCodes("§m§n测试文本", false)
        assertEquals("§m§n测试文本", result.first)
        assertTrue(result.second.isNotEmpty())
    }

    // ==================== 颜色代码转换测试 ====================

    @Test
    fun `test convertColorCodes - 基岩版特有颜色代码转Java版`() {
        // 测试基岩版特有颜色代码转换为Java版
        val result = TextFormatter.convertColorCodes("§g§h§i§j", MinecraftVersion.JAVA)
        
        // §g -> §6 (gold)
        // §h -> §f (white)
        // §i -> §7 (gray)
        // §j -> §8 (dark_gray)
        assertTrue(result.contains("§6"))
        assertTrue(result.contains("§f"))
        assertTrue(result.contains("§7"))
        assertTrue(result.contains("§8"))
    }

    @Test
    fun `test convertColorCodes - 基岩版特有颜色代码转基岩版`() {
        // 测试基岩版特有颜色代码保持不变
        val result = TextFormatter.convertColorCodes("§g§h§i§j", MinecraftVersion.BEDROCK)
        
        // 基岩版颜色代码应该保持不变
        assertTrue(result.contains("§g"))
        assertTrue(result.contains("§h"))
        assertTrue(result.contains("§i"))
        assertTrue(result.contains("§j"))
    }

    // ==================== §字符转义测试 ====================

    @Test
    fun `test escapeSectionSigns - 转义§字符`() {
        // 测试转义§字符
        val result = TextFormatter.escapeSectionSigns("§a§b§c")
        assertEquals("\\u00A7a\\u00A7b\\u00A7c", result)
    }

    @Test
    fun `test unescapeSectionSigns - 恢复§字符`() {
        // 测试恢复§字符
        val result = TextFormatter.unescapeSectionSigns("\\u00A7a\\u00A7b\\u00A7c")
        assertEquals("§a§b§c", result)
    }

    // ==================== Java版JSON转换测试 ====================

    @Test
    fun `test convertToJavaJson - 纯文本`() {
        // 测试纯文本转换
        val result = TextFormatter.convertToJavaJson("普通文本")
        
        val json = gson.fromJson(result, Map::class.java)
        assertEquals("普通文本", json["text"])
    }

    @Test
    fun `test convertToJavaJson - 单个颜色代码`() {
        // 测试单个颜色代码
        val result = TextFormatter.convertToJavaJson("§a绿色文本")
        
        val json = gson.fromJson(result, Map::class.java)
        assertEquals("绿色文本", json["text"])
        assertEquals("green", json["color"])
    }

    @Test
    fun `test convertToJavaJson - 多个颜色代码`() {
        // 测试多个颜色代码
        val result = TextFormatter.convertToJavaJson("§a绿色§b青色§c红色")
        
        val json = gson.fromJson(result, Map::class.java)
        assertEquals("绿色", json["text"])
        assertEquals("green", json["color"])
        
        val extra = json["extra"] as List<*>
        assertEquals(2, extra.size)
        
        val part1 = extra[0] as Map<*, *>
        assertEquals("青色", part1["text"])
        assertEquals("aqua", part1["color"])
        
        val part2 = extra[1] as Map<*, *>
        assertEquals("红色", part2["text"])
        assertEquals("red", part2["color"])
    }

    @Test
    fun `test convertToJavaJson - 格式代码`() {
        // 测试格式代码
        val result = TextFormatter.convertToJavaJson("§l粗体§o斜体§k混淆")
        
        val json = gson.fromJson(result, Map::class.java)
        val extra = json["extra"] as List<*>
        assertEquals(3, extra.size)
        
        val part1 = extra[0] as Map<*, *>
        assertEquals("粗体", part1["text"])
        assertEquals(true, part1["bold"])
        
        val part2 = extra[1] as Map<*, *>
        assertEquals("斜体", part2["text"])
        assertEquals(true, part2["italic"])
        
        val part3 = extra[2] as Map<*, *>
        assertEquals("混淆", part3["text"])
        assertEquals(true, part3["obfuscated"])
    }

    @Test
    fun `test convertToJavaJson - §r重置代码`() {
        // 测试§r重置代码
        val result = TextFormatter.convertToJavaJson("§a绿色§r普通文本")
        
        val json = gson.fromJson(result, Map::class.java)
        val extra = json["extra"] as List<*>
        assertEquals(2, extra.size)
        
        val part1 = extra[0] as Map<*, *>
        assertEquals("绿色", part1["text"])
        assertEquals("green", part1["color"])
        
        val part2 = extra[1] as Map<*, *>
        assertEquals("普通文本", part2["text"])
        assertNull(part2["color"])
    }

    @Test
    fun `test convertToJavaJson - §m作为字体方式`() {
        // 测试§m作为字体方式（删除线）
        val result = TextFormatter.convertToJavaJson("§m删除线文本", "font")
        
        val json = gson.fromJson(result, Map::class.java)
        assertEquals("删除线文本", json["text"])
        assertEquals(true, json["strikethrough"])
    }

    @Test
    fun `test convertToJavaJson - §m作为颜色代码方式`() {
        // 测试§m作为颜色代码方式（深红色）
        val result = TextFormatter.convertToJavaJson("§m深红色文本", "color")
        
        val json = gson.fromJson(result, Map::class.java)
        assertEquals("深红色文本", json["text"])
        assertEquals("dark_red", json["color"])
    }

    @Test
    fun `test convertToJavaJson - §n作为字体方式`() {
        // 测试§n作为字体方式（下划线）
        val result = TextFormatter.convertToJavaJson("§n下划线文本", "font")
        
        val json = gson.fromJson(result, Map::class.java)
        assertEquals("下划线文本", json["text"])
        assertEquals(true, json["underlined"])
    }

    @Test
    fun `test convertToJavaJson - §n作为颜色代码方式`() {
        // 测试§n作为颜色代码方式（红色）
        val result = TextFormatter.convertToJavaJson("§n红色文本", "color")
        
        val json = gson.fromJson(result, Map::class.java)
        assertEquals("红色文本", json["text"])
        assertEquals("red", json["color"])
    }

    @Test
    fun `test convertToJavaJson - 基岩版特有颜色代码`() {
        // 测试基岩版特有颜色代码
        val result = TextFormatter.convertToJavaJson("§g§h§i§j")
        
        val json = gson.fromJson(result, Map::class.java)
        // §g -> gold
        // §h -> white
        // §i -> gray
        // §j -> dark_gray
        val extra = json["extra"] as List<*>
        assertEquals(4, extra.size)
    }

    @Test
    fun `test convertToJavaJson - 相同颜色文本合并`() {
        // 测试相同颜色的文本合并
        val result = TextFormatter.convertToJavaJson("§a绿色文本1§a绿色文本2")
        
        val json = gson.fromJson(result, Map::class.java)
        assertEquals("绿色文本1绿色文本2", json["text"])
        assertEquals("green", json["color"])
    }

    @Test
    fun `test convertToJavaJson - 未知§代码抛出异常`() {
        // 测试未知§代码抛出异常
        try {
            TextFormatter.convertToJavaJson("§z未知代码")
            fail("应该抛出IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("未知的§格式代码"))
        }
    }

    // ==================== 基岩版JSON转换测试 ====================

    @Test
    fun `test convertToBedrockJson - 纯文本`() {
        // 测试纯文本转换
        val result = TextFormatter.convertToBedrockJson("普通文本")
        
        val json = gson.fromJson(result, Map::class.java)
        val rawtext = json["rawtext"] as List<*>
        assertEquals(1, rawtext.size)
        
        val textPart = rawtext[0] as Map<*, *>
        assertEquals("普通文本", textPart["text"])
    }

    @Test
    fun `test convertToBedrockJson - 带颜色代码的文本`() {
        // 测试带颜色代码的文本
        val result = TextFormatter.convertToBedrockJson("§a绿色§b青色§c红色")
        
        val json = gson.fromJson(result, Map::class.java)
        val rawtext = json["rawtext"] as List<*>
        assertEquals(1, rawtext.size)
        
        val textPart = rawtext[0] as Map<*, *>
        val text = textPart["text"] as String
        assertTrue(text.contains("§a"))
        assertTrue(text.contains("§b"))
        assertTrue(text.contains("§c"))
    }

    @Test
    fun `test convertToBedrockJson - §m作为颜色代码方式`() {
        // 测试§m作为颜色代码方式
        val result = TextFormatter.convertToBedrockJson("§m文本", "color")
        
        val json = gson.fromJson(result, Map::class.java)
        val rawtext = json["rawtext"] as List<*>
        val textPart = rawtext[0] as Map<*, *>
        val text = textPart["text"] as String
        
        // §m应该转换为§4
        assertTrue(text.contains("§4"))
        assertFalse(text.contains("§m"))
    }

    @Test
    fun `test convertToBedrockJson - §n作为颜色代码方式`() {
        // 测试§n作为颜色代码方式
        val result = TextFormatter.convertToBedrockJson("§n文本", "color")
        
        val json = gson.fromJson(result, Map::class.java)
        val rawtext = json["rawtext"] as List<*>
        val textPart = rawtext[0] as Map<*, *>
        val text = textPart["text"] as String
        
        // §n应该转换为§c
        assertTrue(text.contains("§c"))
        assertFalse(text.contains("§n"))
    }

    @Test
    fun `test convertToBedrockJson - 基岩版特有颜色代码保留`() {
        // 测试基岩版特有颜色代码保留
        val result = TextFormatter.convertToBedrockJson("§g§h§i§j", "font")
        
        val json = gson.fromJson(result, Map::class.java)
        val rawtext = json["rawtext"] as List<*>
        val textPart = rawtext[0] as Map<*, *>
        val text = textPart["text"] as String
        
        // 基岩版特有颜色代码应该保留
        assertTrue(text.contains("§g"))
        assertTrue(text.contains("§h"))
        assertTrue(text.contains("§i"))
        assertTrue(text.contains("§j"))
    }

    @Test
    fun `test convertToBedrockJson - 基岩版特有颜色代码转换`() {
        // 测试基岩版特有颜色代码转换
        val result = TextFormatter.convertToBedrockJson("§g§h§i§j", "color")
        
        val json = gson.fromJson(result, Map::class.java)
        val rawtext = json["rawtext"] as List<*>
        val textPart = rawtext[0] as Map<*, *>
        val text = textPart["text"] as String
        
        // 基岩版特有颜色代码应该转换为标准代码
        // §g -> §6, §h -> §f, §i -> §7, §j -> §8
        assertTrue(text.contains("§6"))
        assertTrue(text.contains("§f"))
        assertTrue(text.contains("§7"))
        assertTrue(text.contains("§8"))
    }

    // ==================== tellraw命令生成测试 ====================

    @Test
    fun `test generateTellrawCommand - 简单文本`() {
        // 测试简单文本的tellraw命令生成
        val result = TextFormatter.generateTellrawCommand("@a", "普通文本")
        
        // Java版命令
        assertTrue(result.javaCommand.startsWith("tellraw @a"))
        assertTrue(result.javaCommand.contains("普通文本"))
        
        // 基岩版命令
        assertTrue(result.bedrockCommand.startsWith("tellraw @a"))
        assertTrue(result.bedrockCommand.contains("普通文本"))
    }

    @Test
    fun `test generateTellrawCommand - 带颜色代码的文本`() {
        // 测试带颜色代码的tellraw命令生成
        val result = TextFormatter.generateTellrawCommand("@a", "§a绿色文本")
        
        // Java版命令应该包含JSON格式
        assertTrue(result.javaCommand.contains("\"color\":\"green\""))
        
        // 基岩版命令应该包含rawtext格式
        assertTrue(result.bedrockCommand.contains("rawtext"))
        assertTrue(result.bedrockCommand.contains("§a"))
    }

    @Test
    fun `test generateTellrawCommand - 带§m§n的文本`() {
        // 测试带§m§n的tellraw命令生成
        val result = TextFormatter.generateTellrawCommand("@a", "§m§n测试文本")
        
        // 应该有警告信息
        assertTrue(result.warnings.isNotEmpty())
    }

    @Test
    fun `test generateTellrawCommand - Java字体方式`() {
        // 测试Java字体方式
        val result = TextFormatter.generateTellrawCommand("@a", "§m§n测试文本", true)
        
        // Java版命令应该包含strikethrough和underlined
        assertTrue(result.javaCommand.contains("strikethrough") || result.javaCommand.contains("dark_red"))
        assertTrue(result.javaCommand.contains("underlined") || result.javaCommand.contains("red"))
    }

    @Test
    fun `test generateTellrawCommand - 颜色代码方式`() {
        // 测试颜色代码方式
        val result = TextFormatter.generateTellrawCommand("@a", "§m§n测试文本", false)
        
        // Java版和基岩版都应该使用颜色代码
        assertTrue(result.javaCommand.contains("dark_red") || result.javaCommand.contains("red"))
        assertTrue(result.bedrockCommand.contains("§4") || result.bedrockCommand.contains("§c"))
    }

    // ==================== tellraw命令验证测试 ====================

    @Test
    fun `test validateTellrawCommand - 有效命令`() {
        // 测试有效命令
        val errors = TextFormatter.validateTellrawCommand("tellraw @a {\"text\":\"测试\"}")
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `test validateTellrawCommand - 无效命令前缀`() {
        // 测试无效命令前缀
        val errors = TextFormatter.validateTellrawCommand("say @a 测试")
        assertTrue(errors.isNotEmpty())
        assertTrue(errors.any { it.contains("必须以'tellraw '开头") })
    }

    @Test
    fun `test validateTellrawCommand - 命令格式不正确`() {
        // 测试命令格式不正确
        val errors = TextFormatter.validateTellrawCommand("tellraw @a")
        assertTrue(errors.isNotEmpty())
        assertTrue(errors.any { it.contains("命令格式不正确") })
    }

    @Test
    fun `test validateTellrawCommand - 无效选择器`() {
        // 测试无效选择器
        val errors = TextFormatter.validateTellrawCommand("tellraw player {\"text\":\"测试\"}")
        assertTrue(errors.isNotEmpty())
        assertTrue(errors.any { it.contains("选择器必须以@开头") })
    }

    @Test
    fun `test validateTellrawCommand - 无效选择器变量`() {
        // 测试无效选择器变量
        val errors = TextFormatter.validateTellrawCommand("tellraw @x {\"text\":\"测试\"}")
        assertTrue(errors.isNotEmpty())
        assertTrue(errors.any { it.contains("选择器无效") })
    }

    @Test
    fun `test validateTellrawCommand - 无效JSON格式`() {
        // 测试无效JSON格式
        val errors = TextFormatter.validateTellrawCommand("tellraw @a {\"text\":\"测试")
        assertTrue(errors.isNotEmpty())
        assertTrue(errors.any { it.contains("JSON格式无效") })
    }

    // ==================== 颜色名称映射测试 ====================

    @Test
    fun `test getColorName - 标准颜色代码`() {
        // 测试标准颜色代码映射
        assertEquals("black", TextFormatter.getColorName("§0"))
        assertEquals("dark_blue", TextFormatter.getColorName("§1"))
        assertEquals("dark_green", TextFormatter.getColorName("§2"))
        assertEquals("dark_aqua", TextFormatter.getColorName("§3"))
        assertEquals("dark_red", TextFormatter.getColorName("§4"))
        assertEquals("dark_purple", TextFormatter.getColorName("§5"))
        assertEquals("gold", TextFormatter.getColorName("§6"))
        assertEquals("gray", TextFormatter.getColorName("§7"))
        assertEquals("dark_gray", TextFormatter.getColorName("§8"))
        assertEquals("blue", TextFormatter.getColorName("§9"))
        assertEquals("green", TextFormatter.getColorName("§a"))
        assertEquals("aqua", TextFormatter.getColorName("§b"))
        assertEquals("red", TextFormatter.getColorName("§c"))
        assertEquals("light_purple", TextFormatter.getColorName("§d"))
        assertEquals("yellow", TextFormatter.getColorName("§e"))
        assertEquals("white", TextFormatter.getColorName("§f"))
    }

    @Test
    fun `test getColorName - 未知颜色代码`() {
        // 测试未知颜色代码
        assertEquals("white", TextFormatter.getColorName("§z"))
    }

    // ==================== 复杂场景测试 ====================

    @Test
    fun `test complexScenario - 多种颜色和格式混合`() {
        // 测试多种颜色和格式混合
        val result = TextFormatter.convertToJavaJson("§a§l粗体绿色§r§b§o斜体青色§r§c§n下划线红色")
        
        val json = gson.fromJson(result, Map::class.java)
        val extra = json["extra"] as List<*>
        assertEquals(3, extra.size)
        
        // 检查第一部分
        val part1 = extra[0] as Map<*, *>
        assertEquals("粗体绿色", part1["text"])
        assertEquals("green", part1["color"])
        assertEquals(true, part1["bold"])
        
        // 检查第二部分
        val part2 = extra[1] as Map<*, *>
        assertEquals("斜体青色", part2["text"])
        assertEquals("aqua", part2["color"])
        assertEquals(true, part2["italic"])
        
        // 检查第三部分
        val part3 = extra[2] as Map<*, *>
        assertEquals("下划线红色", part3["text"])
        assertEquals("red", part3["color"])
        assertEquals(true, part3["underlined"])
    }

    @Test
    fun `test complexScenario - 基岩版特有颜色和标准颜色混合`() {
        // 测试基岩版特有颜色和标准颜色混合
        val result = TextFormatter.convertToJavaJson("§g金色§a绿色§h白色§b青色")
        
        val json = gson.fromJson(result, Map::class.java)
        val extra = json["extra"] as List<*>
        assertEquals(4, extra.size)
        
        // 检查所有部分都有颜色
        extra.forEach { part ->
            val map = part as Map<*, *>
            assertNotNull(map["color"])
        }
    }

    @Test
    fun `test complexScenario - 空文本`() {
        // 测试空文本
        val result = TextFormatter.convertToJavaJson("")
        
        val json = gson.fromJson(result, Map::class.java)
        assertEquals("", json["text"])
    }

    @Test
    fun `test complexScenario - 只有格式代码`() {
        // 测试只有格式代码
        val result = TextFormatter.convertToJavaJson("§l§o§n")
        
        val json = gson.fromJson(result, Map::class.java)
        assertEquals("", json["text"])
        assertEquals(true, json["bold"])
        assertEquals(true, json["italic"])
        assertEquals(true, json["underlined"])
    }
}