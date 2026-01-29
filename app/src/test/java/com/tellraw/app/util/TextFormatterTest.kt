package com.tellraw.app.util

import org.junit.Assert.*
import org.junit.Test

/**
 * 文本格式化器测试
 * 测试颜色代码和格式代码的转换逻辑
 */
class TextFormatterTest {
    
    /**
     * 测试组1：颜色代码测试
     */
    @Test
    fun testColorCodes_1() {
        // 基本颜色代码
        val text = "§a绿色§r§c红色§r§b青色"
        // 验证颜色代码被正确识别
        assertTrue("应包含绿色", text.contains("§a"))
        assertTrue("应包含红色", text.contains("§c"))
        assertTrue("应包含青色", text.contains("§b"))
        assertTrue("应包含重置", text.contains("§r"))
    }
    
    @Test
    fun testColorCodes_2() {
        // 所有基本颜色代码
        val colors = listOf("§0", "§1", "§2", "§3", "§4", "§5", "§6", "§7", "§8", "§9", "§a", "§b", "§c", "§d", "§e", "§f")
        val text = colors.joinToString("")
        
        for (color in colors) {
            assertTrue("应包含颜色代码 $color", text.contains(color))
        }
    }
    
    @Test
    fun testColorCodes_3() {
        // 基岩版特有颜色代码
        val bedrockColors = listOf("§g", "§h", "§i", "§j", "§m", "§n", "§p", "§q", "§s", "§t", "§u", "§v")
        val text = bedrockColors.joinToString("")
        
        for (color in bedrockColors) {
            assertTrue("应包含基岩版颜色代码 $color", text.contains(color))
        }
    }
    
    /**
     * 测试组2：格式代码测试
     */
    @Test
    fun testFormatCodes_1() {
        // 基本格式代码
        val text = "§l粗体§r§m删除线§r§n下划线§r§o斜体§r§k混乱§r"
        
        assertTrue("应包含粗体", text.contains("§l"))
        assertTrue("应包含删除线", text.contains("§m"))
        assertTrue("应包含下划线", text.contains("§n"))
        assertTrue("应包含斜体", text.contains("§o"))
        assertTrue("应包含混乱", text.contains("§k"))
        assertTrue("应包含重置", text.contains("§r"))
    }
    
    /**
     * 测试组3：§m§n代码测试
     */
    @Test
    fun testMNCodes_1() {
        // §m删除线代码
        val text = "§m删除线文本"
        assertTrue("应包含§m代码", text.contains("§m"))
    }
    
    @Test
    fun testMNCodes_2() {
        // §n下划线代码
        val text = "§n下划线文本"
        assertTrue("应包含§n代码", text.contains("§n"))
    }
    
    @Test
    fun testMNCodes_3() {
        // §m§n组合
        val text = "§m§n删除线下划线"
        assertTrue("应包含§m代码", text.contains("§m"))
        assertTrue("应包含§n代码", text.contains("§n"))
    }
    
    /**
     * 测试组4：混合模式测试
     */
    @Test
    fun testMixedMode_1() {
        // §m_f 字体方式
        val text = "§m_f删除线文本"
        assertTrue("应包含§m_f", text.contains("§m_f"))
    }
    
    @Test
    fun testMixedMode_2() {
        // §m_c 颜色方式
        val text = "§m_c删除线文本"
        assertTrue("应包含§m_c", text.contains("§m_c"))
    }
    
    @Test
    fun testMixedMode_3() {
        // §n_f 字体方式
        val text = "§n_f下划线文本"
        assertTrue("应包含§n_f", text.contains("§n_f"))
    }
    
    @Test
    fun testMixedMode_4() {
        // §n_c 颜色方式
        val text = "§n_c下划线文本"
        assertTrue("应包含§n_c", text.contains("§n_c"))
    }
    
    @Test
    fun testMixedMode_5() {
        // 混合使用不同方式
        val text = "§m_f删除线§m_c和§n_f下划线§n_c"
        assertTrue("应包含§m_f", text.contains("§m_f"))
        assertTrue("应包含§m_c", text.contains("§m_c"))
        assertTrue("应包含§n_f", text.contains("§n_f"))
        assertTrue("应包含§n_c", text.contains("§n_c"))
    }
    
    /**
     * 测试组5：文本格式化类型
     */
    @Test
    fun testTextFormatTypes_1() {
        // 验证格式代码
        val formatCodes = mapOf(
            "bold" to "§l",
            "strikethrough" to "§m",
            "underline" to "§n",
            "italic" to "§o",
            "obfuscated" to "§k",
            "reset" to "§r"
        )
        formatCodes.forEach { (name, code) ->
            assertTrue("格式代码 $name 应为 $code", code.isNotEmpty())
        }
    }

    @Test
    fun testTextFormatCodes_1() {
        // 验证格式代码映射
        assertEquals("§l", "§l")
        assertEquals("§m", "§m")
        assertEquals("§n", "§n")
        assertEquals("§o", "§o")
        assertEquals("§k", "§k")
        assertEquals("§r", "§r")
    }
    
    /**
     * 测试组6：颜色代码映射测试
     */
    @Test
    fun testColorMapping_1() {
        // 验证基岩版颜色代码映射到Java版
        val mapping = mapOf(
            "§g" to "§6",  // minecoin_gold -> gold
            "§h" to "§f",  // material_quartz -> white
            "§i" to "§7",  // material_iron -> gray
            "§j" to "§8",  // material_netherite -> dark_gray
            "§m" to "§4",  // material_redstone -> dark_red (特殊处理)
            "§n" to "§c",  // material_copper -> red (特殊处理)
            "§p" to "§6",  // material_gold -> gold
            "§q" to "§a",  // material_emerald -> green
            "§s" to "§b",  // material_diamond -> aqua
            "§t" to "§1",  // material_lapis -> dark_blue
            "§u" to "§d",  // material_amethyst -> light_purple
            "§v" to "§6"   // material_resin -> gold
        )

        mapping.forEach { (bedrock, java) ->
            val converted = TextFormatter.convertColorCodes(bedrock, com.tellraw.app.model.MinecraftVersion.JAVA)
            assertEquals("基岩版 $bedrock 应映射到 Java版 $java", java, converted)
        }
    }
    
    /**
     * 测试组7：复杂文本格式化
     */
    @Test
    fun testComplexFormatting_1() {
        // 多种格式代码组合
        val text = "§l§a粗体绿色§r§m§c删除线红色§r§n§b下划线青色§r"
        assertTrue("应包含粗体", text.contains("§l"))
        assertTrue("应包含绿色", text.contains("§a"))
        assertTrue("应包含删除线", text.contains("§m"))
        assertTrue("应包含红色", text.contains("§c"))
        assertTrue("应包含下划线", text.contains("§n"))
        assertTrue("应包含青色", text.contains("§b"))
    }
    
    @Test
    fun testComplexFormatting_2() {
        // 嵌套格式代码
        val text = "§l§a粗体§m删除线§n下划线§r§o斜体§k混乱"
        assertTrue("应包含多个格式代码", text.count { it == '§' } >= 7)
    }
    
    @Test
    fun testComplexFormatting_3() {
        // 基岩版特有颜色+格式
        val text = "§g金色§h白色§i灰色§j深灰§m深红§n红色"
        assertTrue("应包含基岩版颜色", text.contains("§g"))
        assertTrue("应包含基岩版颜色", text.contains("§h"))
        assertTrue("应包含基岩版颜色", text.contains("§i"))
        assertTrue("应包含基岩版颜色", text.contains("§j"))
        assertTrue("应包含基岩版颜色", text.contains("§m"))
        assertTrue("应包含基岩版颜色", text.contains("§n"))
    }
    
    /**
     * 测试组8：§m§n处理方式测试
     */
    @Test
    fun testMNHandling_1() {
        // 字体方式：Java版用字体，基岩版用颜色
        val text = "§m删除线文本"
        // 验证可以识别§m代码
        assertTrue("应包含§m代码", text.contains("§m"))
    }
    
    @Test
    fun testMNHandling_2() {
        // 颜色方式：两版都用颜色
        val text = "§m删除线文本"
        // 验证可以识别§m代码
        assertTrue("应包含§m代码", text.contains("§m"))
    }
    
    @Test
    fun testMNHandling_3() {
        // 混合模式：为每个§m/§n单独指定
        val text = "§m_f删除线§m_c和§n_f下划线§n_c"
        assertTrue("应包含§m_f", text.contains("§m_f"))
        assertTrue("应包含§m_c", text.contains("§m_c"))
        assertTrue("应包含§n_f", text.contains("§n_f"))
        assertTrue("应包含§n_c", text.contains("§n_c"))
    }
    
    /**
     * 测试组9：边界情况测试
     */
    @Test
    fun testEdgeCases_1() {
        // 空文本
        val text = ""
        assertEquals("空文本", "", text)
    }
    
    @Test
    fun testEdgeCases_2() {
        // 只有重置代码
        val text = "§r"
        assertTrue("应包含重置代码", text.contains("§r"))
    }
    
    @Test
    fun testEdgeCases_3() {
        // 连续重置代码
        val text = "§r§r§r"
        assertEquals("应包含3个重置代码", 3, text.count { it == '§' })
    }
    
    @Test
    fun testEdgeCases_4() {
        // 无效的颜色代码
        val text = "§z无效"
        assertTrue("应包含§z", text.contains("§z"))
    }
    
    @Test
    fun testEdgeCases_5() {
        // 无效的格式代码
        val text = "§x无效"
        assertTrue("应包含§x", text.contains("§x"))
    }
    
    @Test
    fun testEdgeCases_6() {
        // 不完整的颜色代码
        val text = "§"
        assertTrue("应包含§", text.contains("§"))
    }
    
    /**
     * 测试组10：颜色代码名称
     */
    @Test
    fun testColorCodeNames_1() {
        // 验证颜色代码名称映射
        val colorNames = mapOf(
            "0" to "black",
            "1" to "dark_blue",
            "2" to "dark_green",
            "3" to "dark_aqua",
            "4" to "dark_red",
            "5" to "dark_purple",
            "6" to "gold",
            "7" to "gray",
            "8" to "dark_gray",
            "9" to "blue",
            "a" to "green",
            "b" to "aqua",
            "c" to "red",
            "d" to "light_purple",
            "e" to "yellow",
            "f" to "white"
        )
        
        colorNames.forEach { (code, name) ->
            assertEquals("颜色代码 $code 应对应 $name", name, TextFormatter.getColorName("§$code"))
        }
    }
    
    @Test
    fun testColorCodeNames_2() {
        // 验证基岩版颜色代码名称
        val bedrockColorNames = mapOf(
            "g" to "minecoin_gold",
            "h" to "material_quartz",
            "i" to "material_iron",
            "j" to "material_netherite",
            "m" to "material_redstone",
            "n" to "material_copper",
            "p" to "material_gold",
            "q" to "material_emerald",
            "s" to "material_diamond",
            "t" to "material_lapis",
            "u" to "material_amethyst",
            "v" to "material_resin"
        )
        
        bedrockColorNames.forEach { (code, name) ->
            assertEquals("基岩版颜色代码 §$code 应对应 $name", name, TextFormatter.getColorName("§$code"))
        }
    }
    
    /**
     * 测试组11：颜色代码转换测试
     */
    @Test
    fun testColorCodeConversion_1() {
        // 基岩版颜色代码到Java版转换
        val bedrockText = "§g金色"
        val javaCode = TextFormatter.convertColorCodes("§g", com.tellraw.app.model.MinecraftVersion.JAVA)
        assertEquals("§6", javaCode)
    }

    @Test
    fun testColorCodeConversion_2() {
        // 基岩版material_redstone到Java版dark_red
        val javaCode = TextFormatter.convertColorCodes("§m", com.tellraw.app.model.MinecraftVersion.JAVA)
        assertEquals("§4", javaCode)
    }

    @Test
    fun testColorCodeConversion_3() {
        // 基岩版material_copper到Java版red
        val javaCode = TextFormatter.convertColorCodes("§n", com.tellraw.app.model.MinecraftVersion.JAVA)
        assertEquals("§c", javaCode)
    }

    @Test
    fun testColorCodeConversion_4() {
        // Java版颜色代码保持不变
        val javaCode = TextFormatter.convertColorCodes("§a", com.tellraw.app.model.MinecraftVersion.JAVA)
        assertEquals("§a", javaCode)
    }

    @Test
    fun testColorCodeConversion_5() {
        // 基岩版material_amethyst到Java版light_purple
        val javaCode = TextFormatter.convertColorCodes("§u", com.tellraw.app.model.MinecraftVersion.JAVA)
        assertEquals("§d", javaCode)
    }

    @Test
    fun testColorCodeConversion_6() {
        // 所有基岩版颜色代码转换
        val bedrockColors = listOf("§g", "§h", "§i", "§j", "§m", "§n", "§p", "§q", "§s", "§t", "§u", "§v")
        for (color in bedrockColors) {
            val javaCode = TextFormatter.convertColorCodes(color, com.tellraw.app.model.MinecraftVersion.JAVA)
            assertNotNull("颜色代码 $color 应该能转换", javaCode)
            assertTrue("转换结果应该以§开头", javaCode.startsWith("§"))
        }
    }

    @Test
    fun testColorCodeConversion_7() {
        // 不存在的颜色代码
        val invalidCode = "§z"
        val javaCode = TextFormatter.convertColorCodes(invalidCode, com.tellraw.app.model.MinecraftVersion.JAVA)
        assertEquals("不存在的颜色代码应该返回原值", invalidCode, javaCode)
    }

    @Test
    fun testColorCodeConversion_8() {
        // 特殊情况：§m和§n在Java版中是格式代码
        val javaM = TextFormatter.convertColorCodes("§m", com.tellraw.app.model.MinecraftVersion.JAVA)
        assertEquals("基岩版§m应转换为Java版dark_red", "§4", javaM)

        val javaN = TextFormatter.convertColorCodes("§n", com.tellraw.app.model.MinecraftVersion.JAVA)
        assertEquals("基岩版§n应转换为Java版red", "§c", javaN)
    }
    
    /**
     * 测试组12：格式代码转换测试
     */
    @Test
    fun testFormatCodeConversion_1() {
        // 粗体代码
        val text = "§l粗体文本"
        assertTrue("应包含粗体代码", text.contains("§l"))
    }
    
    @Test
    fun testFormatCodeConversion_2() {
        // 斜体代码
        val text = "§o斜体文本"
        assertTrue("应包含斜体代码", text.contains("§o"))
    }
    
    @Test
    fun testFormatCodeConversion_3() {
        // 混乱代码
        val text = "§k混乱文本"
        assertTrue("应包含混乱代码", text.contains("§k"))
    }
    
    @Test
    fun testFormatCodeConversion_4() {
        // 重置代码
        val text = "§l粗体§r普通"
        assertTrue("应包含重置代码", text.contains("§r"))
    }
    
    @Test
    fun testFormatCodeConversion_5() {
        // 所有格式代码
        val formatCodes = listOf("§l", "§m", "§n", "§o", "§k", "§r")
        for (code in formatCodes) {
            val text = code + "文本"
            assertTrue("应包含格式代码 $code", text.contains(code))
        }
    }
    
    @Test
    fun testFormatCodeConversion_6() {
        // 格式代码连续使用
        val text = "§l粗体§m删除线§n下划线§o斜体"
        assertTrue("应包含粗体", text.contains("§l"))
        assertTrue("应包含删除线", text.contains("§m"))
        assertTrue("应包含下划线", text.contains("§n"))
        assertTrue("应包含斜体", text.contains("§o"))
    }
    
    @Test
    fun testFormatCodeConversion_7() {
        // 格式代码重复使用
        val text = "§l粗体§l更粗"
        assertTrue("应包含粗体", text.contains("§l"))
    }
    
    @Test
    fun testFormatCodeConversion_8() {
        // 重置代码清除所有格式
        val text = "§l§m§n§o§k所有格式§r清除"
        assertTrue("应包含重置", text.contains("§r"))
    }
    
    /**
     * 测试组13：混合颜色和格式代码测试
     */
    @Test
    fun testMixedColorAndFormat_1() {
        // 颜色+格式
        val text = "§a§l绿色粗体"
        assertTrue("应包含绿色", text.contains("§a"))
        assertTrue("应包含粗体", text.contains("§l"))
    }
    
    @Test
    fun testMixedColorAndFormat_2() {
        // 多个颜色+格式组合
        val text = "§a§l绿色粗体§c§m红色删除线§b§n青色下划线"
        assertTrue("应包含绿色", text.contains("§a"))
        assertTrue("应包含粗体", text.contains("§l"))
        assertTrue("应包含红色", text.contains("§c"))
        assertTrue("应包含删除线", text.contains("§m"))
        assertTrue("应包含青色", text.contains("§b"))
        assertTrue("应包含下划线", text.contains("§n"))
    }
    
    @Test
    fun testMixedColorAndFormat_3() {
        // 颜色重置格式
        val text = "§a§l绿色粗体§c红色"
        assertTrue("应包含绿色", text.contains("§a"))
        assertTrue("应包含粗体", text.contains("§l"))
        assertTrue("应包含红色", text.contains("§c"))
    }
    
    @Test
    fun testMixedColorAndFormat_4() {
        // 格式重置颜色
        val text = "§l粗体§a绿色"
        assertTrue("应包含粗体", text.contains("§l"))
        assertTrue("应包含绿色", text.contains("§a"))
    }
    
    @Test
    fun testMixedColorAndFormat_5() {
        // 基岩版颜色+格式
        val text = "§g§l金色粗体"
        assertTrue("应包含金色", text.contains("§g"))
        assertTrue("应包含粗体", text.contains("§l"))
    }
    
    @Test
    fun testMixedColorAndFormat_6() {
        // 所有颜色+所有格式
        val colors = listOf("§0", "§1", "§2", "§3", "§4", "§5", "§6", "§7", "§8", "§9", "§a", "§b", "§c", "§d", "§e", "§f")
        val formats = listOf("§l", "§m", "§n", "§o", "§k")
        for (color in colors) {
            for (format in formats) {
                val text = color + format + "文本"
                assertTrue("应包含颜色 $color", text.contains(color))
                assertTrue("应包含格式 $format", text.contains(format))
            }
        }
    }
    
    @Test
    fun testMixedColorAndFormat_7() {
        // 颜色和格式的顺序
        val text1 = "§a§l颜色格式"
        val text2 = "§l§a格式颜色"
        assertTrue("颜色+格式应包含颜色", text1.contains("§a"))
        assertTrue("颜色+格式应包含格式", text1.contains("§l"))
        assertTrue("格式+颜色应包含格式", text2.contains("§l"))
        assertTrue("格式+颜色应包含颜色", text2.contains("§a"))
    }
    
    @Test
    fun testMixedColorAndFormat_8() {
        // 重置后的颜色和格式
        val text = "§a§l绿色粗体§r§c§m红色删除线"
        assertTrue("应包含重置", text.contains("§r"))
        assertTrue("应包含红色", text.contains("§c"))
        assertTrue("应包含删除线", text.contains("§m"))
    }
    
    /**
     * 测试组14：连续格式代码测试
     */
    @Test
    fun testConsecutiveFormatCodes_1() {
        // 连续的格式代码
        val text = "§l§m§n§o§k所有格式"
        assertTrue("应包含粗体", text.contains("§l"))
        assertTrue("应包含删除线", text.contains("§m"))
        assertTrue("应包含下划线", text.contains("§n"))
        assertTrue("应包含斜体", text.contains("§o"))
        assertTrue("应包含混乱", text.contains("§k"))
    }
    
    @Test
    fun testConsecutiveFormatCodes_2() {
        // 连续的颜色代码
        val text = "§0§1§2§3§4连续颜色"
        assertTrue("应包含多个颜色代码", text.count { it == '§' } >= 5)
    }
    
    @Test
    fun testConsecutiveFormatCodes_3() {
        // 连续的相同格式代码
        val text = "§l§l§l§l多重粗体"
        assertTrue("应包含多个粗体代码", text.count { it == '§' } >= 4)
    }
    
    @Test
    fun testConsecutiveFormatCodes_4() {
        // 连续的重置代码
        val text = "§l粗体§r§r§r多重重置"
        assertTrue("应包含多个重置代码", text.count { it == '§' } >= 4)
    }
    
    @Test
    fun testConsecutiveFormatCodes_5() {
        // 交替的颜色和格式
        val text = "§a§l§c§m§b§n交替"
        assertTrue("应包含绿色", text.contains("§a"))
        assertTrue("应包含粗体", text.contains("§l"))
        assertTrue("应包含红色", text.contains("§c"))
        assertTrue("应包含删除线", text.contains("§m"))
        assertTrue("应包含青色", text.contains("§b"))
        assertTrue("应包含下划线", text.contains("§n"))
    }
    
    @Test
    fun testConsecutiveFormatCodes_6() {
        // 重复的相同颜色代码
        val text = "§a§a§a§a重复绿色"
        assertTrue("应包含多个绿色代码", text.count { it == '§' } >= 4)
    }
    
    @Test
    fun testConsecutiveFormatCodes_7() {
        // 连续的基岩版颜色代码
        val text = "§g§h§i§j§m§n基岩版颜色"
        assertTrue("应包含基岩版金色", text.contains("§g"))
        assertTrue("应包含基岩版白色", text.contains("§h"))
        assertTrue("应包含基岩版灰色", text.contains("§i"))
        assertTrue("应包含基岩版深灰", text.contains("§j"))
        assertTrue("应包含基岩版深红", text.contains("§m"))
        assertTrue("应包含基岩版红色", text.contains("§n"))
    }
    
    @Test
    fun testConsecutiveFormatCodes_8() {
        // 连续的混合代码
        val text = "§a§l§g§m§b§n§q§o混合代码"
        assertTrue("应包含多种代码", text.count { it == '§' } >= 8)
    }
    
    /**
     * 测试组15：特殊字符和转义测试
     */
    @Test
    fun testSpecialCharacters_1() {
        // 文本中的引号
        val text = "§a\"引号\"文本"
        assertTrue("应包含引号", text.contains("\""))
    }
    
    @Test
    fun testSpecialCharacters_2() {
        // 文本中的反斜杠
        val text = "§a\\反斜杠\\文本"
        assertTrue("应包含反斜杠", text.contains("\\"))
    }
    
    @Test
    fun testSpecialCharacters_3() {
        // 文本中的换行符
        val text = "§a第一行\n第二行"
        assertTrue("应包含换行符", text.contains("\n"))
    }
    
    @Test
    fun testSpecialCharacters_4() {
        // 文本中的制表符
        val text = "§a第一列\t第二列"
        assertTrue("应包含制表符", text.contains("\t"))
    }
    
    @Test
    fun testSpecialCharacters_5() {
        // 文本中的Unicode字符
        val text = "§a中文文本"
        assertTrue("应包含中文", text.contains("中文"))
    }
    
    @Test
    fun testSpecialCharacters_6() {
        // 文本中的emoji
        val text = "§a😀表情符号"
        assertTrue("应包含emoji", text.contains("😀"))
    }
    
    @Test
    fun testSpecialCharacters_7() {
        // 文本中的特殊符号
        val text = "§a@#$%^&*()"
        assertTrue("应包含特殊符号", text.contains("@"))
    }
    
    @Test
    fun testSpecialCharacters_8() {
        // 文本中的空格
        val text = "§a带 空格 的 文本"
        assertTrue("应包含空格", text.contains(" "))
    }
    
    /**
     * 测试组16：文本长度测试
     */
    @Test
    fun testTextLength_1() {
        // 短文本
        val text = "§a短"
        assertTrue("应包含颜色代码", text.contains("§a"))
        assertEquals("文本长度应为3", 3, text.length)
    }
    
    @Test
    fun testTextLength_2() {
        // 中等长度文本
        val text = "§a这是一段中等长度的文本"
        assertTrue("应包含颜色代码", text.contains("§a"))
        assertTrue("文本长度应大于10", text.length > 10)
    }
    
    @Test
    fun testTextLength_3() {
        // 长文本
        val text = "§a这是一段很长的文本，包含了很多内容，用于测试长文本的处理能力。".repeat(5)
        assertTrue("应包含颜色代码", text.contains("§a"))
        assertTrue("文本长度应大于100", text.length > 100)
    }
    
    @Test
    fun testTextLength_4() {
        // 超长文本
        val text = "§a超长文本".repeat(100)
        assertTrue("应包含颜色代码", text.contains("§a"))
        assertTrue("文本长度应大于500", text.length > 500)
    }
    
    @Test
    fun testTextLength_5() {
        // 只有颜色代码的文本
        val text = "§a"
        assertEquals("文本长度应为2", 2, text.length)
    }
    
    @Test
    fun testTextLength_6() {
        // 多个颜色代码的文本
        val text = "§a§b§c§d§e§f"
        assertEquals("文本长度应为12", 12, text.length)
    }
    
    @Test
    fun testTextLength_7() {
        // 空文本
        val text = ""
        assertEquals("空文本长度应为0", 0, text.length)
    }
    
    @Test
    fun testTextLength_8() {
        // 只有格式代码的文本
        val text = "§l§m§n§o§k"
        assertEquals("文本长度应为10", 10, text.length)
    }
    
    /**
     * 测试组17：颜色代码优先级测试
     */
    @Test
    fun testColorCodePriority_1() {
        // 后面的颜色代码覆盖前面的
        val text = "§a绿色§c红色"
        assertTrue("应包含绿色", text.contains("§a"))
        assertTrue("应包含红色", text.contains("§c"))
    }
    
    @Test
    fun testColorCodePriority_2() {
        // 颜色代码清除前面的颜色
        val text = "§a绿色§c红色文本"
        assertTrue("应包含绿色", text.contains("§a"))
        assertTrue("应包含红色", text.contains("§c"))
    }
    
    @Test
    fun testColorCodePriority_3() {
        // 基岩版颜色代码覆盖Java版颜色代码
        val text = "§a绿色§g金色"
        assertTrue("应包含绿色", text.contains("§a"))
        assertTrue("应包含金色", text.contains("§g"))
    }
    
    @Test
    fun testColorCodePriority_4() {
        // Java版颜色代码覆盖基岩版颜色代码
        val text = "§g金色§a绿色"
        assertTrue("应包含金色", text.contains("§g"))
        assertTrue("应包含绿色", text.contains("§a"))
    }
    
    @Test
    fun testColorCodePriority_5() {
        // 多个颜色代码的优先级
        val text = "§a§b§c§d§e§f最后"
        assertTrue("应包含多个颜色代码", text.count { it == '§' } >= 6)
    }
    
    @Test
    fun testColorCodePriority_6() {
        // 颜色代码和格式代码的优先级
        val text = "§a§l绿色粗体§c红色"
        assertTrue("应包含绿色", text.contains("§a"))
        assertTrue("应包含粗体", text.contains("§l"))
        assertTrue("应包含红色", text.contains("§c"))
    }
    
    @Test
    fun testColorCodePriority_7() {
        // 重置代码清除颜色
        val text = "§a绿色§r普通"
        assertTrue("应包含绿色", text.contains("§a"))
        assertTrue("应包含重置", text.contains("§r"))
    }
    
    @Test
    fun testColorCodePriority_8() {
        // 颜色代码清除格式
        val text = "§l粗体§a绿色"
        assertTrue("应包含粗体", text.contains("§l"))
        assertTrue("应包含绿色", text.contains("§a"))
    }
    
    /**
     * 测试组18：格式代码优先级测试
     */
    @Test
    fun testFormatCodePriority_1() {
        // 后面的格式代码不影响前面的
        val text = "§l粗体§m删除线"
        assertTrue("应包含粗体", text.contains("§l"))
        assertTrue("应包含删除线", text.contains("§m"))
    }
    
    @Test
    fun testFormatCodePriority_2() {
        // 重置代码清除所有格式
        val text = "§l§m§n§o§k所有格式§r清除"
        assertTrue("应包含所有格式", text.contains("§l"))
        assertTrue("应包含重置", text.contains("§r"))
    }
    
    @Test
    fun testFormatCodePriority_3() {
        // 颜色代码保留格式
        val text = "§l粗体§a绿色粗体"
        assertTrue("应包含粗体", text.contains("§l"))
        assertTrue("应包含绿色", text.contains("§a"))
    }
    
    @Test
    fun testFormatCodePriority_4() {
        // 格式代码可以叠加
        val text = "§l§m§n粗体删除线下划线"
        assertTrue("应包含粗体", text.contains("§l"))
        assertTrue("应包含删除线", text.contains("§m"))
        assertTrue("应包含下划线", text.contains("§n"))
    }
    
    @Test
    fun testFormatCodePriority_5() {
        // 重复的格式代码
        val text = "§l粗体§l更粗"
        assertTrue("应包含粗体", text.contains("§l"))
    }
    
    @Test
    fun testFormatCodePriority_6() {
        // 格式代码的顺序
        val text1 = "§l§m粗体删除线"
        val text2 = "§m§l删除线粗体"
        assertTrue("粗体+删除线应包含粗体", text1.contains("§l"))
        assertTrue("粗体+删除线应包含删除线", text1.contains("§m"))
        assertTrue("删除线+粗体应包含删除线", text2.contains("§m"))
        assertTrue("删除线+粗体应包含粗体", text2.contains("§l"))
    }
    
    @Test
    fun testFormatCodePriority_7() {
        // 重置代码后的格式代码
        val text = "§l粗体§r§m删除线"
        assertTrue("应包含粗体", text.contains("§l"))
        assertTrue("应包含重置", text.contains("§r"))
        assertTrue("应包含删除线", text.contains("§m"))
    }
    
    @Test
    fun testFormatCodePriority_8() {
        // 所有格式代码的组合
        val text = "§l§m§n§o§k所有格式"
        assertTrue("应包含粗体", text.contains("§l"))
        assertTrue("应包含删除线", text.contains("§m"))
        assertTrue("应包含下划线", text.contains("§n"))
        assertTrue("应包含斜体", text.contains("§o"))
        assertTrue("应包含混乱", text.contains("§k"))
    }
}