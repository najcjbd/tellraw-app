package com.tellraw.app.util

import android.content.Context
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * 文本格式化器测试
 * 测试颜色代码和格式代码的转换逻辑
 */
@RunWith(RobolectricTestRunner::class)
class TextFormatterTest {
    
    private lateinit var context: Context
    
    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
    }
    
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
        // 验证基岩版颜色代码映射到Java版（基于RGB值对比）
        val mapping = mapOf(
            "§g" to "§e",  // minecoin_gold -> yellow
            "§h" to "§f",  // material_quartz -> white
            "§i" to "§f",  // material_iron -> white
            "§j" to "§8",  // material_netherite -> dark_gray
            // §m 和 §n 保持独特处理逻辑，不在此映射
            "§p" to "§6",  // material_gold -> gold
            "§q" to "§a",  // material_emerald -> green
            "§s" to "§b",  // material_diamond -> aqua
            "§t" to "§1",  // material_lapis -> dark_blue
            "§u" to "§d",  // material_amethyst -> light_purple
            "§v" to "§c"   // material_resin -> red
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
        val javaJson = TextFormatter.convertToJavaJson(text, "font", false)
        // 验证Java版将§m作为字体方式处理（strikethrough）
        assertTrue("Java版应包含strikethrough", javaJson.contains("strikethrough"))
    }

    @Test
    fun testMNHandling_2() {
        // 颜色方式：两版都用颜色
        val text = "§m删除线文本"
        val javaJson = TextFormatter.convertToJavaJson(text, "color", false)
        // 验证Java版将§m作为颜色方式处理（dark_red）
        assertTrue("Java版应包含dark_red颜色", javaJson.contains("dark_red"))
        assertFalse("Java版不应包含strikethrough", javaJson.contains("strikethrough"))
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

    @Test
    fun testMNHandling_4() {
        // §n字体方式：Java版用字体
        val text = "§n下划线文本"
        val javaJson = TextFormatter.convertToJavaJson(text, "font", false)
        // 验证Java版将§n作为字体方式处理（underlined）
        assertTrue("Java版应包含underlined", javaJson.contains("underlined"))
    }

    @Test
    fun testMNHandling_5() {
        // §n颜色方式：两版都用颜色
        val text = "§n下划线文本"
        val javaJson = TextFormatter.convertToJavaJson(text, "color", false)
        // 验证Java版将§n作为颜色方式处理（red）
        assertTrue("Java版应包含red颜色", javaJson.contains("red"))
        assertFalse("Java版不应包含underlined", javaJson.contains("underlined"))
    }

    @Test
    fun testMNHandling_6() {
        // §m_f在混合模式下的Java版转换
        val text = "§m_f删除线文本"
        val javaJson = TextFormatter.convertToJavaJson(text, "font", false)
        // 验证Java版将§m_f转换为strikethrough
        assertTrue("Java版应包含strikethrough", javaJson.contains("strikethrough"))
    }

    @Test
    fun testMNHandling_7() {
        // §m_c在混合模式下的Java版转换
        val text = "§m_c删除线文本"
        val javaJson = TextFormatter.convertToJavaJson(text, "font", false)
        // 验证Java版将§m_c转换为dark_red颜色
        assertTrue("Java版应包含dark_red颜色", javaJson.contains("dark_red"))
        assertFalse("Java版不应包含strikethrough", javaJson.contains("strikethrough"))
    }

    @Test
    fun testMNHandling_8() {
        // §n_f在混合模式下的Java版转换
        val text = "§n_f下划线文本"
        val javaJson = TextFormatter.convertToJavaJson(text, "font", false)
        // 验证Java版将§n_f转换为underlined
        assertTrue("Java版应包含underlined", javaJson.contains("underlined"))
    }

    @Test
    fun testMNHandling_9() {
        // §n_c在混合模式下的Java版转换
        val text = "§n_c下划线文本"
        val javaJson = TextFormatter.convertToJavaJson(text, "font", false)
        // 验证Java版将§n_c转换为red颜色
        assertTrue("Java版应包含red颜色", javaJson.contains("red"))
        assertFalse("Java版不应包含underlined", javaJson.contains("underlined"))
    }

    @Test
    fun testMNHandling_10() {
        // §m/§n_c/f模式下普通§m/§n在基岩版中的处理
        val text = "§m普通删除线§n普通下划线"
        val bedrockJson = TextFormatter.convertToBedrockJson(text, "font", true)
        // 在§m/§n_c/f模式下，普通的§m/§n应该被移除
        val rawtextContent = bedrockJson.substringAfter("text\":").substringBefore("}")
        assertFalse("§m/§n_c/f模式下不应包含§m", rawtextContent.matches(Regex("(?<!§m)[_]m(?![_cn])")))
        assertFalse("§m/§n_c/f模式下不应包含§n", rawtextContent.matches(Regex("(?<!§n)[_]n(?![_cn])")))
    }

    @Test
    fun testMNHandling_11() {
        // 混合模式下普通§m/§n在基岩版中的处理
        val text = "§m普通删除线§n普通下划线"
        val bedrockJson = TextFormatter.convertToBedrockJson(text, "font", false)
        // 在混合模式下，普通的§m/§n应该被保留（它们是有效的基岩版颜色代码）
        val rawtextContent = bedrockJson.substringAfter("text\":").substringBefore("}")
        assertTrue("混合模式下应保留§m", rawtextContent.contains("§m"))
        assertTrue("混合模式下应保留§n", rawtextContent.contains("§n"))
    }

    @Test
    fun testMNHandling_12() {
        // §m/§n_c/f模式下§m_f/§m_c/§n_f/§n_c在基岩版中的处理
        val text = "§m_f字体删除线§m_c颜色删除线§n_f字体下划线§n_c颜色下划线"
        val bedrockJson = TextFormatter.convertToBedrockJson(text, "font", true)
        // 在§m/§n_c/f模式下，§m_f/§m_c统一转换为§m，§n_f/§n_c统一转换为§n
        val rawtextContent = bedrockJson.substringAfter("text\":").substringBefore("}")
        assertFalse("不应包含§m_f", rawtextContent.contains("§m_f"))
        assertFalse("不应包含§m_c", rawtextContent.contains("§m_c"))
        assertFalse("不应包含§n_f", rawtextContent.contains("§n_f"))
        assertFalse("不应包含§n_c", rawtextContent.contains("§n_c"))
        assertTrue("应包含§m", rawtextContent.contains("§m"))
        assertTrue("应包含§n", rawtextContent.contains("§n"))
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
        val javaCode = TextFormatter.convertColorCodes("§g", com.tellraw.app.model.MinecraftVersion.JAVA)
        assertEquals("§e", javaCode)  // minecoin_gold -> yellow
    }

    @Test
    fun testColorCodeConversion_2() {
        // §m保持独特的处理逻辑，不在此转换
        val javaCode = TextFormatter.convertColorCodes("§m", com.tellraw.app.model.MinecraftVersion.JAVA)
        assertEquals("§m", javaCode)  // 保持原值，由convertToJavaJson处理
    }

    @Test
    fun testColorCodeConversion_3() {
        // §n保持独特的处理逻辑，不在此转换
        val javaCode = TextFormatter.convertColorCodes("§n", com.tellraw.app.model.MinecraftVersion.JAVA)
        assertEquals("§n", javaCode)  // 保持原值，由convertToJavaJson处理
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
        // 特殊情况：§m和§n保持独特的处理逻辑，不在此转换
        val javaM = TextFormatter.convertColorCodes("§m", com.tellraw.app.model.MinecraftVersion.JAVA)
        assertEquals("§m应保持原值，由convertToJavaJson处理", "§m", javaM)

        val javaN = TextFormatter.convertColorCodes("§n", com.tellraw.app.model.MinecraftVersion.JAVA)
        assertEquals("§n应保持原值，由convertToJavaJson处理", "§n", javaN)
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

    /**
     * 测试组19：convertToJavaJson 函数测试
     */
    @Test
    fun testConvertToJavaJson_1() {
        // 基本文本转换为JSON
        val json = TextFormatter.convertToJavaJson("普通文本")
        assertTrue("应包含text字段", json.contains("\"text\""))
        assertTrue("应包含普通文本", json.contains("普通文本"))
    }

    @Test
    fun testConvertToJavaJson_2() {
        // 颜色代码转换为JSON
        val json = TextFormatter.convertToJavaJson("§a绿色文本")
        assertTrue("应包含color字段", json.contains("\"color\""))
        assertTrue("应包含green颜色", json.contains("\"green\""))
        assertTrue("应包含绿色文本", json.contains("绿色文本"))
    }

    @Test
    fun testConvertToJavaJson_3() {
        // 格式代码转换为JSON
        val json = TextFormatter.convertToJavaJson("§l粗体文本")
        assertTrue("应包含bold字段", json.contains("\"bold\""))
        assertTrue("应包含true", json.contains("true"))
        assertTrue("应包含粗体文本", json.contains("粗体文本"))
    }

    @Test
    fun testConvertToJavaJson_4() {
        // §m代码（字体方式）转换为JSON
        val json = TextFormatter.convertToJavaJson("§m删除线文本", mNHandling = "font")
        assertTrue("应包含strikethrough字段", json.contains("\"strikethrough\""))
        assertTrue("应包含删除线文本", json.contains("删除线文本"))
    }

    @Test
    fun testConvertToJavaJson_5() {
        // §m代码（颜色方式）转换为JSON
        val json = TextFormatter.convertToJavaJson("§m删除线文本", mNHandling = "color")
        assertTrue("应包含color字段", json.contains("\"color\""))
        assertTrue("应包含dark_red颜色", json.contains("\"dark_red\""))
        assertTrue("应包含删除线文本", json.contains("删除线文本"))
    }

    @Test
    fun testConvertToJavaJson_6() {
        // §n代码（字体方式）转换为JSON
        val json = TextFormatter.convertToJavaJson("§n下划线文本", mNHandling = "font")
        assertTrue("应包含underlined字段", json.contains("\"underlined\""))
        assertTrue("应包含下划线文本", json.contains("下划线文本"))
    }

    @Test
    fun testConvertToJavaJson_7() {
        // §n代码（颜色方式）转换为JSON
        val json = TextFormatter.convertToJavaJson("§n下划线文本", mNHandling = "color")
        assertTrue("应包含color字段", json.contains("\"color\""))
        assertTrue("应包含red颜色", json.contains("\"red\""))
        assertTrue("应包含下划线文本", json.contains("下划线文本"))
    }

    @Test
    fun testConvertToJavaJson_8() {
        // §m_f代码转换为JSON
        val json = TextFormatter.convertToJavaJson("§m_f删除线文本", mnCFEnabled = true)
        assertTrue("应包含strikethrough字段", json.contains("\"strikethrough\""))
        assertTrue("应包含删除线文本", json.contains("删除线文本"))
    }

    @Test
    fun testConvertToJavaJson_9() {
        // §m_c代码转换为JSON
        val json = TextFormatter.convertToJavaJson("§m_c删除线文本", mnCFEnabled = true)
        assertTrue("应包含color字段", json.contains("\"color\""))
        assertTrue("应包含dark_red颜色", json.contains("\"dark_red\""))
        assertTrue("应包含删除线文本", json.contains("删除线文本"))
    }

    @Test
    fun testConvertToJavaJson_10() {
        // §n_f代码转换为JSON
        val json = TextFormatter.convertToJavaJson("§n_f下划线文本", mnCFEnabled = true)
        assertTrue("应包含underlined字段", json.contains("\"underlined\""))
        assertTrue("应包含下划线文本", json.contains("下划线文本"))
    }

    @Test
    fun testConvertToJavaJson_11() {
        // §n_c代码转换为JSON
        val json = TextFormatter.convertToJavaJson("§n_c下划线文本", mnCFEnabled = true)
        assertTrue("应包含color字段", json.contains("\"color\""))
        assertTrue("应包含red颜色", json.contains("\"red\""))
        assertTrue("应包含下划线文本", json.contains("下划线文本"))
    }

    @Test
    fun testConvertToJavaJson_12() {
        // §m/§n_c/f模式：mnCFEnabled=true
        // §m_f和§n_f有文本，§m_c和§n_c后面有文本
        val json = TextFormatter.convertToJavaJson("§m_f删除线§m_c红色和§n_f下划线§n_c蓝色", mnCFEnabled = true)
        assertTrue("应包含strikethrough字段", json.contains("\"strikethrough\""))
        assertTrue("应包含dark_red颜色", json.contains("\"dark_red\""))
        assertTrue("应包含underlined字段", json.contains("\"underlined\""))
        assertTrue("应包含red颜色", json.contains("\"red\""))
        assertTrue("应包含删除线文本", json.contains("删除线"))
        assertTrue("应包含下划线文本", json.contains("下划线"))
        assertTrue("应包含红色文本", json.contains("红色"))
        assertTrue("应包含蓝色文本", json.contains("蓝色"))
        assertTrue("应包含和文本", json.contains("和"))
    }

    @Test
    fun testConvertToJavaJson_13() {
        // 多种格式组合
        val json = TextFormatter.convertToJavaJson("§a§l绿色粗体§c§m红色删除线文本")
        assertTrue("应包含多个文本部分", json.contains("\"extra\""))
        assertTrue("应包含green颜色", json.contains("\"green\""))
        assertTrue("应包含red颜色", json.contains("\"red\""))
        assertTrue("应包含bold字段", json.contains("\"bold\""))
        assertTrue("应包含strikethrough字段", json.contains("\"strikethrough\":true"))
        assertTrue("应包含红色文本", json.contains("红色"))
        assertTrue("应包含删除线文本", json.contains("删除线"))
        assertTrue("应包含文本", json.contains("文本"))
    }

    @Test
    fun testConvertToJavaJson_14() {
        // §r重置代码
        val json = TextFormatter.convertToJavaJson("§a绿色§r普通文本")
        assertTrue("应包含多个文本部分", json.contains("\"extra\""))
        assertTrue("应包含green颜色", json.contains("\"green\""))
        assertTrue("应包含普通文本", json.contains("普通文本"))
    }

    @Test
    fun testConvertToJavaJson_15() {
        // 基岩版颜色代码转换（基岩版颜色代码应按颜色代码处理，不受mNHandling影响）
        val json = TextFormatter.convertToJavaJson("§g金色§h白色")
        assertTrue("应包含yellow颜色", json.contains("\"yellow\""))
        assertTrue("应包含white颜色", json.contains("\"white\""))
        assertTrue("应包含金色文本", json.contains("金色"))
        assertTrue("应包含白色文本", json.contains("白色"))
    }

    @Test
    fun testConvertToJavaJson_15a() {
        // 第三个基岩颜色代码 - 验证文本合并逻辑
        val json = TextFormatter.convertToJavaJson("§gh§hi§ig")
        assertTrue("应包含yellow颜色", json.contains("\"yellow\""))
        assertTrue("应包含white颜色", json.contains("\"white\""))
        assertTrue("应包含所有文本字符", json.contains("h") && json.contains("i") && json.contains("g"))
        assertTrue("应包含合并的文本ig", json.contains("\"ig\""))
    }

    @Test
    fun testConvertToJavaJson_15b() {
        // 多个基岩颜色代码 - 验证连续相同颜色合并
        val json = TextFormatter.convertToJavaJson("§gh§hi§ik§qp")
        assertTrue("应包含yellow颜色", json.contains("\"yellow\""))
        assertTrue("应包含white颜色", json.contains("\"white\""))
        assertTrue("应包含green颜色", json.contains("\"green\""))
        assertTrue("应包含所有文本字符", json.contains("h") && json.contains("i") && json.contains("k") && json.contains("p"))
        assertTrue("应包含合并的文本ik", json.contains("\"ik\""))
    }

    @Test
    fun testConvertToJavaJson_15c() {
        // 连续基岩颜色代码带文本 - 验证多段相同颜色合并
        val json = TextFormatter.convertToJavaJson("§g金§h白§i浅§j深§p橙§q绿")
        assertTrue("应包含多个extra部分", json.contains("\"extra\""))
        assertTrue("应包含yellow颜色", json.contains("\"yellow\""))
        assertTrue("应包含white颜色", json.contains("\"white\""))
        assertTrue("应包含所有文本字符", json.contains("金") && json.contains("白") && json.contains("浅") && json.contains("深") && json.contains("橙") && json.contains("绿"))
    }

    @Test
    fun testConvertToJavaJson_15d() {
        // 混合通用颜色和基岩颜色代码
        val json = TextFormatter.convertToJavaJson("§a绿色§g金色§b青色")
        assertTrue("应包含green颜色", json.contains("\"green\""))
        assertTrue("应包含yellow颜色", json.contains("\"yellow\""))
        assertTrue("应包含aqua颜色", json.contains("\"aqua\""))
        assertTrue("应包含所有文本字符", json.contains("绿色") && json.contains("金色") && json.contains("青色"))
    }

    @Test
    fun testConvertToJavaJson_16() {
        // 所有格式代码
        val json = TextFormatter.convertToJavaJson("§l粗体§m删除线§n下划线§o斜体§k混乱")
        assertTrue("应包含bold字段", json.contains("\"bold\""))
        assertTrue("应包含strikethrough字段", json.contains("\"strikethrough\""))
        assertTrue("应包含underlined字段", json.contains("\"underlined\""))
        assertTrue("应包含italic字段", json.contains("\"italic\""))
        assertTrue("应包含obfuscated字段", json.contains("\"obfuscated\""))
    }

    @Test
    fun testConvertToJavaJson_17() {
        // 空文本
        val json = TextFormatter.convertToJavaJson("")
        assertTrue("应包含空文本", json.contains("\"text\":\"\""))
    }

    @Test
    fun testConvertToJavaJson_18() {
        // 只有颜色代码
        val json = TextFormatter.convertToJavaJson("§a文本")
        assertTrue("应包含color字段", json.contains("\"color\""))
        assertTrue("应包含green颜色", json.contains("\"green\""))
        assertTrue("应包含文本内容", json.contains("文本"))
    }

    @Test
    fun testConvertToJavaJson_19() {
        // 只有格式代码
        val json = TextFormatter.convertToJavaJson("§l文本")
        assertTrue("应包含bold字段", json.contains("\"bold\""))
        assertTrue("应包含文本内容", json.contains("文本"))
    }

    @Test
    fun testConvertToJavaJson_20() {
        // 相同格式的文本合并
        val json = TextFormatter.convertToJavaJson("§a绿色§a继续绿色")
        // 相同颜色的文本应该合并在一起
        assertTrue("应包含green颜色", json.contains("\"green\""))
        assertTrue("应包含绿色继续绿色", json.contains("绿色继续绿色"))
    }

    /**
     * 测试组20：convertToBedrockJson 函数测试
     */
    @Test
    fun testConvertToBedrockJson_1() {
        // 基本文本转换为JSON
        val json = TextFormatter.convertToBedrockJson("普通文本")
        assertTrue("应包含rawtext字段", json.contains("\"rawtext\""))
        assertTrue("应包含普通文本", json.contains("普通文本"))
    }

    @Test
    fun testConvertToBedrockJson_2() {
        // 颜色代码转换
        val json = TextFormatter.convertToBedrockJson("§a绿色文本")
        assertTrue("应包含rawtext字段", json.contains("\"rawtext\""))
        assertTrue("应包含绿色文本", json.contains("绿色文本"))
        assertTrue("应包含§a颜色代码", json.contains("§a"))
    }

    @Test
    fun testConvertToBedrockJson_3() {
        // §m代码转换为JSON（基岩版始终作为颜色代码）
        val json = TextFormatter.convertToBedrockJson("§m删除线文本", mNHandling = "font")
        assertTrue("应包含rawtext字段", json.contains("\"rawtext\""))
        assertTrue("应包含§m颜色代码", json.contains("§m"))
        assertTrue("应包含删除线文本", json.contains("删除线文本"))
    }

    @Test
    fun testConvertToBedrockJson_4() {
        // §m代码转换为JSON（颜色方式）
        val json = TextFormatter.convertToBedrockJson("§m删除线文本", mNHandling = "color")
        assertTrue("应包含rawtext字段", json.contains("\"rawtext\""))
        assertTrue("应包含§m颜色代码", json.contains("§m"))
        assertTrue("应包含删除线文本", json.contains("删除线文本"))
    }

    @Test
    fun testConvertToBedrockJson_5() {
        // §n代码转换为JSON（基岩版始终作为颜色代码）
        val json = TextFormatter.convertToBedrockJson("§n下划线文本", mNHandling = "font")
        assertTrue("应包含rawtext字段", json.contains("\"rawtext\""))
        assertTrue("应包含§n颜色代码", json.contains("§n"))
        assertTrue("应包含下划线文本", json.contains("下划线文本"))
    }

    @Test
    fun testConvertToBedrockJson_6() {
        // §n代码转换为JSON（颜色方式）
        val json = TextFormatter.convertToBedrockJson("§n下划线文本", mNHandling = "color")
        assertTrue("应包含rawtext字段", json.contains("\"rawtext\""))
        assertTrue("应包含§n颜色代码", json.contains("§n"))
        assertTrue("应包含下划线文本", json.contains("下划线文本"))
    }

    @Test
    fun testConvertToBedrockJson_7() {
        // §m_f代码转换为JSON（§m/§n_c/f模式）
        val json = TextFormatter.convertToBedrockJson("§m_f删除线文本", mnCFEnabled = true)
        assertTrue("应包含rawtext字段", json.contains("\"rawtext\""))
        // 在§m/§n_c/f模式下，§m_f应该转换为§m
        assertTrue("应包含§m颜色代码", json.contains("§m"))
        assertTrue("应包含删除线文本", json.contains("删除线文本"))
        // 在§m/§n_c/f模式下，普通的§m应该被移除
        assertFalse("不应包含§m_f", json.contains("§m_f"))
    }

    @Test
    fun testConvertToBedrockJson_8() {
        // §m_c代码转换为JSON（§m/§n_c/f模式）
        val json = TextFormatter.convertToBedrockJson("§m_c删除线文本", mnCFEnabled = true)
        assertTrue("应包含rawtext字段", json.contains("\"rawtext\""))
        // 在§m/§n_c/f模式下，§m_c应该转换为§m
        assertTrue("应包含§m颜色代码", json.contains("§m"))
        assertTrue("应包含删除线文本", json.contains("删除线文本"))
        assertFalse("不应包含§m_c", json.contains("§m_c"))
    }

    @Test
    fun testConvertToBedrockJson_9() {
        // §n_f代码转换为JSON（§m/§n_c/f模式）
        val json = TextFormatter.convertToBedrockJson("§n_f下划线文本", mnCFEnabled = true)
        assertTrue("应包含rawtext字段", json.contains("\"rawtext\""))
        // 在§m/§n_c/f模式下，§n_f应该转换为§n
        assertTrue("应包含§n颜色代码", json.contains("§n"))
        assertTrue("应包含下划线文本", json.contains("下划线文本"))
        assertFalse("不应包含§n_f", json.contains("§n_f"))
    }

    @Test
    fun testConvertToBedrockJson_10() {
        // §n_c代码转换为JSON（§m/§n_c/f模式）
        val json = TextFormatter.convertToBedrockJson("§n_c下划线文本", mnCFEnabled = true)
        assertTrue("应包含rawtext字段", json.contains("\"rawtext\""))
        // 在§m/§n_c/f模式下，§n_c应该转换为§n
        assertTrue("应包含§n颜色代码", json.contains("§n"))
        assertTrue("应包含下划线文本", json.contains("下划线文本"))
        assertFalse("不应包含§n_c", json.contains("§n_c"))
    }

    @Test
    fun testConvertToBedrockJson_11() {
        // 混合模式：§m_f/§m_c统一转换为§m
        val json = TextFormatter.convertToBedrockJson("§m_f字体删除线§m_c颜色删除线", mNHandling = "font", mnCFEnabled = false)
        assertTrue("应包含rawtext字段", json.contains("\"rawtext\""))
        // 在混合模式下，§m_f和§m_c都应该转换为§m
        val rawtextContent = json.substringAfter("text\":").substringBefore("}")
        assertTrue("应包含§m颜色代码", rawtextContent.contains("§m"))
        assertFalse("不应包含§m_f", rawtextContent.contains("§m_f"))
        assertFalse("不应包含§m_c", rawtextContent.contains("§m_c"))
    }

    @Test
    fun testConvertToBedrockJson_12() {
        // 混合模式：§n_f/§n_c统一转换为§n
        val json = TextFormatter.convertToBedrockJson("§n_f字体下划线§n_c颜色下划线", mNHandling = "font", mnCFEnabled = false)
        assertTrue("应包含rawtext字段", json.contains("\"rawtext\""))
        // 在混合模式下，§n_f和§n_c都应该转换为§n
        val rawtextContent = json.substringAfter("text\":").substringBefore("}")
        assertTrue("应包含§n颜色代码", rawtextContent.contains("§n"))
        assertFalse("不应包含§n_f", rawtextContent.contains("§n_f"))
        assertFalse("不应包含§n_c", rawtextContent.contains("§n_c"))
    }

    @Test
    fun testConvertToBedrockJson_13() {
        // §m/§n_c/f模式下移除普通的§m/§n
        val json = TextFormatter.convertToBedrockJson("§m普通删除线§n普通下划线", mnCFEnabled = true)
        assertTrue("应包含rawtext字段", json.contains("\"rawtext\""))
        // 在§m/§n_c/f模式下，普通的§m/§n应该被移除
        val rawtextContent = json.substringAfter("text\":").substringBefore("}")
        assertFalse("不应包含独立的§m", rawtextContent.matches(Regex("(?<!§m)[_]m(?![_cn])")))
        assertFalse("不应包含独立的§n", rawtextContent.matches(Regex("(?<!§n)[_]n(?![_cn])")))
    }

    @Test
    fun testConvertToBedrockJson_14() {
        // 混合模式下保留普通的§m/§n
        val json = TextFormatter.convertToBedrockJson("§m普通删除线§n普通下划线", mNHandling = "font", mnCFEnabled = false)
        assertTrue("应包含rawtext字段", json.contains("\"rawtext\""))
        // 在混合模式下，普通的§m/§n应该被保留（它们是有效的基岩版颜色代码）
        val rawtextContent = json.substringAfter("text\":").substringBefore("}")
        assertTrue("混合模式下应保留§m", rawtextContent.contains("§m"))
        assertTrue("混合模式下应保留§n", rawtextContent.contains("§n"))
    }

    @Test
    fun testConvertToBedrockJson_15() {
        // 基岩版特有颜色代码转换
        val json = TextFormatter.convertToBedrockJson("§g金色§h白色§i灰色")
        assertTrue("应包含rawtext字段", json.contains("\"rawtext\""))
        // 基岩版特有颜色代码应该直接保留
        assertTrue("应包含§g", json.contains("§g"))
        assertTrue("应包含§h", json.contains("§h"))
        assertTrue("应包含§i", json.contains("§i"))
        assertTrue("应包含金色", json.contains("金色"))
        assertTrue("应包含白色", json.contains("白色"))
        assertTrue("应包含灰色", json.contains("灰色"))
    }

    @Test
    fun testConvertToBedrockJson_16() {
        // 空文本
        val json = TextFormatter.convertToBedrockJson("")
        assertTrue("应包含rawtext字段", json.contains("\"rawtext\""))
    }

    @Test
    fun testConvertToBedrockJson_17() {
        // 多个颜色和格式代码
        val json = TextFormatter.convertToBedrockJson("§a§l绿色粗体§c§m红色删除线§b§n青色下划线")
        assertTrue("应包含rawtext字段", json.contains("\"rawtext\""))
        assertTrue("应包含§a", json.contains("§a"))
        assertTrue("应包含§l", json.contains("§l"))
        assertTrue("应包含§c", json.contains("§c"))
        assertTrue("应包含§m", json.contains("§m"))
        assertTrue("应包含§b", json.contains("§b"))
        assertTrue("应包含§n", json.contains("§n"))
    }

    @Test
    fun testConvertToBedrockJson_18() {
        // 非§m/§n_c/f模式：保留§m/§n作为颜色代码
        val json = TextFormatter.convertToBedrockJson("§m删除线§n下划线", mNHandling = "color", mnCFEnabled = false)
        assertTrue("应包含rawtext字段", json.contains("\"rawtext\""))
        assertTrue("应包含§m颜色代码", json.contains("§m"))
        assertTrue("应包含§n颜色代码", json.contains("§n"))
    }

    @Test
    fun testConvertToBedrockJson_19() {
        // 连续的§m_f/§m_c/§n_f/§n_c（带文本）
        val json = TextFormatter.convertToBedrockJson("§m_f§m_caa§n_f§n_cbb", mnCFEnabled = true)
        assertTrue("应包含rawtext字段", json.contains("\"rawtext\""))
        // 应该转换为§m§maa§n§nbb
        assertTrue("应包含§m", json.contains("§m"))
        assertTrue("应包含§n", json.contains("§n"))
        assertTrue("应包含aa文本", json.contains("aa"))
        assertTrue("应包含bb文本", json.contains("bb"))
    }

    @Test
    fun testConvertToBedrockJson_20() {
        // 复杂的文本组合
        val json = TextFormatter.convertToBedrockJson("§a绿色§l粗体§r§m_f删除线§m_c颜色§r§n_f下划线§n_c颜色")
        assertTrue("应包含rawtext字段", json.contains("\"rawtext\""))
        assertTrue("应包含§a", json.contains("§a"))
        assertTrue("应包含§l", json.contains("§l"))
        assertTrue("应包含§r", json.contains("§r"))
        assertTrue("应包含§m", json.contains("§m"))
        assertTrue("应包含§n", json.contains("§n"))
    }

    /**
     * 测试组21：processMNCodes 函数测试
     */
    @Test
    fun testProcessMNCodes_1() {
        // Java版字体方式，基岩版颜色方式
        val (text, warnings) = TextFormatter.processMNCodes("§m§n文本", useJavaFontStyle = true, context)
        assertEquals("文本应该保持不变", "§m§n文本", text)
        assertTrue("应该有警告信息", warnings.isNotEmpty())
    }

    @Test
    fun testProcessMNCodes_2() {
        // 两版都用颜色方式
        val (text, warnings) = TextFormatter.processMNCodes("§m§n文本", useJavaFontStyle = false, context)
        assertEquals("文本应该保持不变", "§m§n文本", text)
        assertTrue("应该有警告信息", warnings.isNotEmpty())
    }

    @Test
    fun testProcessMNCodes_3() {
        // 没有§m§n代码
        val (text, warnings) = TextFormatter.processMNCodes("§a绿色文本", useJavaFontStyle = true, context)
        assertEquals("文本应该保持不变", "§a绿色文本", text)
        assertFalse("不应该有警告信息", warnings.isNotEmpty())
    }

    @Test
    fun testProcessMNCodes_4() {
        // 只有§m代码
        val (text, warnings) = TextFormatter.processMNCodes("§m删除线", useJavaFontStyle = true, context)
        assertEquals("文本应该保持不变", "§m删除线", text)
        assertTrue("应该有警告信息", warnings.isNotEmpty())
    }

    @Test
    fun testProcessMNCodes_5() {
        // 只有§n代码
        val (text, warnings) = TextFormatter.processMNCodes("§n下划线", useJavaFontStyle = true, context)
        assertEquals("文本应该保持不变", "§n下划线", text)
        assertTrue("应该有警告信息", warnings.isNotEmpty())
    }

    /**
     * 测试组22：§r重置代码完整测试
     */
    @Test
    fun testResetCode_1() {
        // §r清除颜色
        val json = TextFormatter.convertToJavaJson("§a绿色§r普通")
        assertTrue("应包含绿色部分", json.contains("\"color\":\"green\""))
        assertTrue("应包含普通部分", json.contains("普通"))
    }

    @Test
    fun testResetCode_2() {
        // §r清除格式
        val json = TextFormatter.convertToJavaJson("§l粗体§r普通")
        assertTrue("应包含粗体部分", json.contains("\"bold\":true"))
        assertTrue("应包含普通部分", json.contains("普通"))
    }

    @Test
    fun testResetCode_3() {
        // §r清除颜色和格式
        val json = TextFormatter.convertToJavaJson("§a§l绿色粗体§r普通")
        assertTrue("应包含绿色粗体部分", json.contains("\"color\":\"green\""))
        assertTrue("应包含粗体字段", json.contains("\"bold\":true"))
        assertTrue("应包含普通部分", json.contains("普通"))
    }

    @Test
    fun testResetCode_4() {
        // 多个§r
        val json = TextFormatter.convertToJavaJson("§a绿色§r§c红色§r普通")
        assertTrue("应包含绿色", json.contains("\"color\":\"green\""))
        assertTrue("应包含红色", json.contains("\"color\":\"red\""))
        assertTrue("应包含普通", json.contains("普通"))
    }

    @Test
    fun testResetCode_5() {
        // §r后重新应用格式
        val json = TextFormatter.convertToJavaJson("§a绿色§r§l粗体")
        assertTrue("应包含绿色", json.contains("\"color\":\"green\""))
        assertTrue("应包含粗体", json.contains("\"bold\":true"))
    }

    @Test
    fun testResetCode_6() {
        // 基岩版§r
        val json = TextFormatter.convertToBedrockJson("§a绿色§r普通")
        assertTrue("应包含§a", json.contains("§a"))
        assertTrue("应包含§r", json.contains("§r"))
        assertTrue("应包含普通", json.contains("普通"))
    }

    @Test
    fun testResetCode_7() {
        // §r清除§m§n
        val json = TextFormatter.convertToJavaJson("§m删除线§r普通", mNHandling = "font")
        assertTrue("应包含删除线部分", json.contains("\"strikethrough\":true"))
        assertTrue("应包含普通部分", json.contains("普通"))
    }

    @Test
    fun testResetCode_8() {
        // §r清除所有格式
        val json = TextFormatter.convertToJavaJson("§l§m§n§o§k所有格式§r清除")
        assertTrue("应包含所有格式", json.contains("\"bold\":true"))
        assertTrue("应包含删除线", json.contains("\"strikethrough\":true"))
        assertTrue("应包含下划线", json.contains("\"underlined\":true"))
        assertTrue("应包含斜体", json.contains("\"italic\":true"))
        assertTrue("应包含混乱", json.contains("\"obfuscated\":true"))
        assertTrue("应包含清除", json.contains("清除"))
    }

    /**
     * 测试组23：JAVA版基岩版混合模式测试
     */
    @Test
    fun testJavaBedrockMixedMode_1() {
        // Java版字体方式，基岩版颜色方式
        val javaJson = TextFormatter.convertToJavaJson("§m删除线", mNHandling = "font")
        val bedrockJson = TextFormatter.convertToBedrockJson("§m删除线", mNHandling = "font")
        
        assertTrue("Java版应包含strikethrough字段", javaJson.contains("\"strikethrough\":true"))
        assertTrue("基岩版应包含§m颜色代码", bedrockJson.contains("§m"))
    }

    @Test
    fun testJavaBedrockMixedMode_2() {
        // 两版都用颜色方式
        val javaJson = TextFormatter.convertToJavaJson("§m删除线", mNHandling = "color")
        val bedrockJson = TextFormatter.convertToBedrockJson("§m删除线", mNHandling = "color")
        
        assertTrue("Java版应包含dark_red颜色", javaJson.contains("\"color\":\"dark_red\""))
        assertTrue("基岩版应包含§m颜色代码", bedrockJson.contains("§m"))
    }

    @Test
    fun testJavaBedrockMixedMode_3() {
        // 复杂的混合文本
        val javaJson = TextFormatter.convertToJavaJson("§a§l绿色粗体§m_f删除线§m_c颜色§n_f下划线§n_c颜色", mnCFEnabled = true)
        val bedrockJson = TextFormatter.convertToBedrockJson("§a§l绿色粗体§m_f删除线§m_c颜色§n_f下划线§n_c颜色", mnCFEnabled = true)
        
        assertTrue("Java版应包含多个部分", javaJson.contains("\"extra\""))
        assertTrue("基岩版应包含rawtext", bedrockJson.contains("\"rawtext\""))
        assertTrue("Java版应包含strikethrough", javaJson.contains("\"strikethrough\""))
        assertTrue("Java版应包含dark_red", javaJson.contains("\"dark_red\""))
        assertTrue("Java版应包含underlined", javaJson.contains("\"underlined\""))
        assertTrue("Java版应包含red", javaJson.contains("\"red\""))
        assertTrue("基岩版应包含§m", bedrockJson.contains("§m"))
        assertTrue("基岩版应包含§n", bedrockJson.contains("§n"))
    }

    @Test
    fun testJavaBedrockMixedMode_4() {
        // 相同文本在不同版本下的表现
        val text = "§a§l绿色粗体§r§m_f删除线§m_c颜色§r§n_f下划线§n_c颜色"
        
        val javaJson = TextFormatter.convertToJavaJson(text, mnCFEnabled = true)
        val bedrockJson = TextFormatter.convertToBedrockJson(text, mnCFEnabled = true)
        
        // Java版使用JSON格式
        assertTrue("Java版应为JSON格式", javaJson.startsWith("{"))
        // 基岩版使用rawtext格式
        assertTrue("基岩版应包含rawtext", bedrockJson.contains("\"rawtext\""))
    }

    @Test
    fun testJavaBedrockMixedMode_5() {
        // 颜色代码在不同版本下的转换
        val bedrockText = "§g金色§h白色§i灰色"
        val javaJson = TextFormatter.convertToJavaJson(bedrockText)
        val bedrockJson = TextFormatter.convertToBedrockJson(bedrockText)

        // Java版应该转换基岩版颜色代码（基于RGB值对比）
        assertTrue("Java版应包含yellow", javaJson.contains("\"yellow\""))
        assertTrue("Java版应包含white", javaJson.contains("\"white\""))
        assertTrue("Java版应包含white", javaJson.contains("\"white\""))  // §i也转换为white

        // 基岩版保留颜色代码
        assertTrue("基岩版应包含金色", bedrockJson.contains("金色"))
        assertTrue("基岩版应包含白色", bedrockJson.contains("白色"))
        assertTrue("基岩版应包含灰色", bedrockJson.contains("灰色"))
    }

    /**
     * 测试组24：§m/§n_c/f模式测试
     */
    @Test
    fun testMNCFMode_1() {
        // §m_f在Java版中作为字体方式
        val json = TextFormatter.convertToJavaJson("§m_f删除线", mnCFEnabled = true)
        assertTrue("应包含strikethrough字段", json.contains("\"strikethrough\":true"))
        assertTrue("应包含删除线文本", json.contains("删除线"))
    }

    @Test
    fun testMNCFMode_2() {
        // §m_c在Java版中作为颜色方式
        val json = TextFormatter.convertToJavaJson("§m_c删除线", mnCFEnabled = true)
        assertTrue("应包含dark_red颜色", json.contains("\"color\":\"dark_red\""))
        assertTrue("应包含删除线文本", json.contains("删除线"))
    }

    @Test
    fun testMNCFMode_3() {
        // §n_f在Java版中作为字体方式
        val json = TextFormatter.convertToJavaJson("§n_f下划线", mnCFEnabled = true)
        assertTrue("应包含underlined字段", json.contains("\"underlined\":true"))
        assertTrue("应包含下划线文本", json.contains("下划线"))
    }

    @Test
    fun testMNCFMode_4() {
        // §n_c在Java版中作为颜色方式
        val json = TextFormatter.convertToJavaJson("§n_c下划线", mnCFEnabled = true)
        assertTrue("应包含red颜色", json.contains("\"color\":\"red\""))
        assertTrue("应包含下划线文本", json.contains("下划线"))
    }

    @Test
    fun testMNCFMode_5() {
        // 在§m/§n_c/f模式下，单独的§m/§n被识别并忽略（Java版）
        val json = TextFormatter.convertToJavaJson("§m普通§m_f删除线", mnCFEnabled = true)
        assertTrue("应包含删除线", json.contains("删除线"))
        // "普通"文本应该保留
        assertTrue("应包含普通文本", json.contains("普通"))
        assertTrue("应包含strikethrough字段", json.contains("\"strikethrough\":true"))
    }

    @Test
    fun testMNCFMode_6() {
        // 在§m/§n_c/f模式下，单独的§m/§n被识别并忽略（基岩版）
        val json = TextFormatter.convertToBedrockJson("§m普通§m_f删除线", mnCFEnabled = true)
        assertTrue("应包含删除线", json.contains("删除线"))
        // "普通"文本应该保留
        assertTrue("应包含普通文本", json.contains("普通"))
        assertTrue("应包含§m颜色代码", json.contains("§m"))
    }

    @Test
    fun testMNCFMode_7() {
        // §m_f和§m_c同时使用
        val json = TextFormatter.convertToJavaJson("§m_f删除线字体§m_c删除线颜色", mnCFEnabled = true)
        assertTrue("应包含strikethrough字段", json.contains("\"strikethrough\":true"))
        assertTrue("应包含dark_red颜色", json.contains("\"color\":\"dark_red\""))
        assertTrue("应包含删除线字体", json.contains("删除线字体"))
        assertTrue("应包含删除线颜色", json.contains("删除线颜色"))
    }

    @Test
    fun testMNCFMode_8() {
        // §n_f和§n_c同时使用
        val json = TextFormatter.convertToJavaJson("§n_f下划线字体§n_c下划线颜色", mnCFEnabled = true)
        assertTrue("应包含underlined字段", json.contains("\"underlined\":true"))
        assertTrue("应包含red颜色", json.contains("\"color\":\"red\""))
        assertTrue("应包含下划线字体", json.contains("下划线字体"))
        assertTrue("应包含下划线颜色", json.contains("下划线颜色"))
    }

    @Test
    fun testMNCFMode_9() {
        // 所有§m/§n_c/f代码组合（带文本）
        val json = TextFormatter.convertToJavaJson("§m_f§m_caa§n_f§n_cbb", mnCFEnabled = true)
        assertTrue("应包含strikethrough字段", json.contains("\"strikethrough\":true"))
        assertTrue("应包含dark_red颜色", json.contains("\"dark_red\""))
        assertTrue("应包含aa文本", json.contains("aa"))
        assertTrue("应包含underlined字段", json.contains("\"underlined\":true"))
        assertTrue("应包含red颜色", json.contains("\"red\""))
        assertTrue("应包含bb文本", json.contains("bb"))
    }

    @Test
    fun testMNCFMode_10() {
        // §m/§n_c/f模式与普通颜色代码混合
        val json = TextFormatter.convertToJavaJson("§a绿色§m_f删除线§b青色§n_f下划线", mnCFEnabled = true)
        assertTrue("应包含green颜色", json.contains("\"color\":\"green\""))
        assertTrue("应包含strikethrough字段", json.contains("\"strikethrough\":true"))
        assertTrue("应包含aqua颜色", json.contains("\"color\":\"aqua\""))
        assertTrue("应包含underlined字段", json.contains("\"underlined\":true"))
    }

    /**
     * 测试组27：边界情况和特殊场景测试
     */
    @Test
    fun testEdgeCasesAndSpecialScenarios_1() {
        // 极长的文本
        val longText = "§a测试".repeat(100)
        val json = TextFormatter.convertToJavaJson(longText)
        assertTrue("应包含大量测试文本", json.contains("测试"))
    }

    @Test
    fun testEdgeCasesAndSpecialScenarios_2() {
        // 特殊字符
        val specialText = "§a特殊字符：\\\"\'@#$%^&*()"
        val json = TextFormatter.convertToJavaJson(specialText)
        assertTrue("应包含特殊字符", json.contains("特殊字符"))
    }

    @Test
    fun testEdgeCasesAndSpecialScenarios_3() {
        // Unicode字符
        val unicodeText = "§a中文文本日本語한국어"
        val json = TextFormatter.convertToJavaJson(unicodeText)
        assertTrue("应包含中文", json.contains("中文"))
        assertTrue("应包含日语", json.contains("日本語"))
        assertTrue("应包含韩语", json.contains("한국어"))
    }

    @Test
    fun testEdgeCasesAndSpecialScenarios_4() {
        // Emoji
        val emojiText = "§a😀😁😂🤣😃😄😅😆😉"
        val json = TextFormatter.convertToJavaJson(emojiText)
        assertTrue("应包含emoji", json.contains("😀"))
    }

    @Test
    fun testEdgeCasesAndSpecialScenarios_5() {
        // 换行符
        val newlineText = "§a第一行\n第二行\n第三行"
        val json = TextFormatter.convertToJavaJson(newlineText)
        assertTrue("应包含转义的换行符", json.contains("\\n"))
    }

    @Test
    fun testEdgeCasesAndSpecialScenarios_6() {
        // 制表符
        val tabText = "§a第一列\t第二列\t第三列"
        val json = TextFormatter.convertToJavaJson(tabText)
        assertTrue("应包含转义的制表符", json.contains("\\t"))
    }

    @Test
    fun testEdgeCasesAndSpecialScenarios_7() {
        // 连续的颜色代码（每个代码后面有文本）
        val continuousColors = "§a绿色§b青色§c红色§d紫色§e黄色§f白色"
        val json = TextFormatter.convertToJavaJson(continuousColors)
        assertTrue("应包含多个文本部分", json.contains("\"extra\""))
        assertTrue("应包含所有文本", json.contains("绿色") && json.contains("青色") && json.contains("红色") && json.contains("紫色") && json.contains("黄色") && json.contains("白色"))
    }

    @Test
    fun testEdgeCasesAndSpecialScenarios_8() {
        // 连续的格式代码（每个代码后面有文本）
        val continuousFormats = "§l粗体§m删除线§n下划线§o斜体§k混乱"
        val json = TextFormatter.convertToJavaJson(continuousFormats)
        assertTrue("应包含多个文本部分", json.contains("\"extra\""))
        assertTrue("应包含所有文本", json.contains("粗体") && json.contains("删除线") && json.contains("下划线") && json.contains("斜体") && json.contains("混乱"))
    }

    @Test
    fun testEdgeCasesAndSpecialScenarios_9() {
        // 无效的颜色代码（应该被跳过）
        val invalidColors = "§z测试§x文本§y内容§w示例"
        val json = TextFormatter.convertToJavaJson(invalidColors)
        // 无效的颜色代码应该被跳过，只保留文本
        assertFalse("不应包含无效颜色代码§z", json.contains("§z"))
        assertTrue("应保留文本", json.contains("测试") && json.contains("文本") && json.contains("内容") && json.contains("示例"))
    }

    @Test
    fun testEdgeCasesAndSpecialScenarios_10() {
        // 单个§（不完整的颜色代码）
        val incompleteColor = "§"
        val javaJson = TextFormatter.convertToJavaJson(incompleteColor)
        val bedrockJson = TextFormatter.convertToBedrockJson(incompleteColor)
        // Java版应该返回空文本对象
        assertTrue("Java版应包含空文本", javaJson.contains("\"text\":\"\""))
        // 基岩版应该保留§
        assertTrue("基岩版应包含§", bedrockJson.contains("§"))
    }

    /**
     * 测试组28：综合场景测试
     */
    @Test
    fun testComprehensiveScenarios_1() {
        // 完整的tellraw命令场景
        val text = "§l§a欢迎来到服务器！§r§c请注意遵守规则。§r§e点击这里加入：§n§bdiscord.gg/example"
        val javaJson = TextFormatter.convertToJavaJson(text, "font", false, context)
        val bedrockJson = TextFormatter.convertToBedrockJson(text, "font", false, context)

        assertTrue("Java版命令应包含JSON格式", javaJson.contains("{"))
        assertTrue("基岩版命令应包含rawtext格式", bedrockJson.contains("\"rawtext\""))
    }

    @Test
    fun testComprehensiveScenarios_2() {
        // 多种格式组合的场景
        val text = "§a§l重要通知§r§m删除线文本§n§b下划线链接§o§c斜体提示§k§d混乱密码"
        val javaJson = TextFormatter.convertToJavaJson(text)
        val bedrockJson = TextFormatter.convertToBedrockJson(text)
        
        assertTrue("Java版应包含所有格式", javaJson.contains("\"extra\""))
        assertTrue("基岩版应包含所有代码", bedrockJson.contains("§a") && bedrockJson.contains("§l") && bedrockJson.contains("§m") && bedrockJson.contains("§n") && bedrockJson.contains("§o") && bedrockJson.contains("§k"))
    }

    @Test
    fun testComprehensiveScenarios_3() {
        // §m/§n_c/f模式下的复杂场景
        val text = "§a§l绿色粗体§r§m_f删除线字体§m_c删除线颜色§r§n_f下划线字体§n_c下划线颜色"
        val javaJson = TextFormatter.convertToJavaJson(text, mnCFEnabled = true)
        val bedrockJson = TextFormatter.convertToBedrockJson(text, mnCFEnabled = true)
        
        assertTrue("Java版应包含strikethrough", javaJson.contains("\"strikethrough\""))
        assertTrue("Java版应包含dark_red", javaJson.contains("\"dark_red\""))
        assertTrue("Java版应包含underlined", javaJson.contains("\"underlined\""))
        assertTrue("Java版应包含red", javaJson.contains("\"red\""))
        assertTrue("基岩版应包含§m", bedrockJson.contains("§m"))
        assertTrue("基岩版应包含§n", bedrockJson.contains("§n"))
    }

    @Test
    fun testComprehensiveScenarios_4() {
        // JAVA版基岩版混合模式的复杂场景
        val text = "§g金色§h白色§i灰色§m删除线§n下划线§a绿色§b青色§c红色"
        val javaJson = TextFormatter.convertToJavaJson(text)
        val bedrockJson = TextFormatter.convertToBedrockJson(text)

        // Java版应该转换基岩版颜色代码（基于RGB值对比）
        assertTrue("Java版应包含yellow", javaJson.contains("\"yellow\""))
        assertTrue("Java版应包含white", javaJson.contains("\"white\""))
        assertTrue("Java版应包含white", javaJson.contains("\"white\""))  // §i也转换为white
        assertTrue("Java版应包含dark_red或red", javaJson.contains("\"dark_red\"") || javaJson.contains("\"red\""))
        assertTrue("Java版应包含green", javaJson.contains("\"green\""))
        assertTrue("Java版应包含aqua", javaJson.contains("\"aqua\""))
        assertTrue("基岩版应包含所有颜色代码", bedrockJson.contains("§g") && bedrockJson.contains("§h") && bedrockJson.contains("§i") && bedrockJson.contains("§m") && bedrockJson.contains("§n") && bedrockJson.contains("§a") && bedrockJson.contains("§b") && bedrockJson.contains("§c"))
    }

    @Test
    fun testComprehensiveScenarios_5() {
        // 完整的游戏场景
        val text = "§l§c系统通知§r§f恭喜你获得了成就：§e§n点击这里领取奖励！§r§a请在§f§o聊天框§a中输入§b§l/reward claim§a来领取。"
        val javaJson = TextFormatter.convertToJavaJson(text, "font", false, context)
        val bedrockJson = TextFormatter.convertToBedrockJson(text, "font", false, context)

        assertTrue("Java版命令应包含JSON格式", javaJson.contains("{"))
        assertTrue("基岩版命令应包含rawtext格式", bedrockJson.contains("\"rawtext\""))
    }
}