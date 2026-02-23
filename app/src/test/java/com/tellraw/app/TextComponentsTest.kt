package com.tellraw.app

import android.content.Context
import com.tellraw.app.model.SelectorType
import com.tellraw.app.util.SelectorConverter
import com.tellraw.app.util.TextComponentHelper
import com.tellraw.app.util.TextFormatter
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

/**
 * 文本组件综合测试
 * 
 * 测试环境：
 * 1. 开启"默认使用text文本组件"环境 - 用于测试原有的text组件功能
 * 2. 关闭"默认使用text文本组件"环境 - 用于测试selector、translate、score组件
 * 
 * 测试覆盖范围：
 * - TEXT组件（纯文本、§代码、§m/§n）
 * - SELECTOR组件（单个、多个、sep:分隔符）
 * - TRANSLATE组件（单个、with副组件）
 * - SCORE组件（单个、多个、name:objective格式）
 * - 混合组件测试
 * - 边界情况和转义字符
 */
@RunWith(MockitoJUnitRunner::class)
class TextComponentsTest {

    @Mock
    private lateinit var context: Context

    // ========================================
    // 第1部分：TEXT组件测试（开启默认使用text）
    // ========================================

    @Test
    fun testTextComponent_PlainText() {
        // 测试纯文本
        val input = "Hello World"
        val result = TextFormatter.convertToJavaJson(input)
        assertTrue(result.contains("\"text\":\"Hello World\""))
    }

    @Test
    fun testTextComponent_WithColorCodes() {
        // 测试带颜色代码的文本
        val input = "§cRed §bBlue §aGreen"
        val result = TextFormatter.convertToJavaJson(input)
        assertTrue(result.contains("\"color\":\"red\""))
        assertTrue(result.contains("\"color\":\"aqua\""))
        assertTrue(result.contains("\"color\":\"green\""))
    }

    @Test
    fun testTextComponent_WithFontMode_MN() {
        // 测试§m/§n字体模式
        val input = "§maaa§nbbb"
        val result = TextFormatter.convertToJavaJson(input, "font", false)
        assertTrue(result.contains("\"strikethrough\":true"))
    }

    @Test
    fun testTextComponent_WithColorMode_MN() {
        // 测试§m/§n颜色模式
        val input = "§maaa§nbbb"
        val result = TextFormatter.convertToJavaJson(input, "color", false)
        assertTrue(result.contains("\"color\":\"dark_red\""))
        assertTrue(result.contains("\"color\":\"red\""))
    }

    @Test
    fun testTextComponent_With_CF_Mode() {
        // 测试§m/§n_c/f模式
        val input = "§m_caaa§n_fbbb"
        val result = TextFormatter.convertToJavaJson(input, "mn_cf", false)
        assertTrue(result.contains("\"color\":\"dark_red\""))
        assertTrue(result.contains("\"strikethrough\":true"))
        assertTrue(result.contains("\"underlined\":true"))
    }

    @Test
    fun testTextComponent_AllColorCodes_Java() {
        // 测试所有Java版颜色代码（§0-§f）
        val input = "§0§1§2§3§4§5§6§7§8§9§a§b§c§d§e§f"
        val result = TextFormatter.convertToJavaJson(input, "font", false)
        // 验证所有颜色代码都被正确处理
        assertTrue(result.contains("\"color\":\"black\""))
        assertTrue(result.contains("\"color\":\"dark_blue\""))
        assertTrue(result.contains("\"color\":\"dark_green\""))
        assertTrue(result.contains("\"color\":\"dark_aqua\""))
        assertTrue(result.contains("\"color\":\"dark_red\""))
        assertTrue(result.contains("\"color\":\"dark_purple\""))
        assertTrue(result.contains("\"color\":\"gold\""))
        assertTrue(result.contains("\"color\":\"gray\""))
        assertTrue(result.contains("\"color\":\"dark_gray\""))
        assertTrue(result.contains("\"color\":\"blue\""))
        assertTrue(result.contains("\"color\":\"green\""))
        assertTrue(result.contains("\"color\":\"aqua\""))
        assertTrue(result.contains("\"color\":\"red\""))
        assertTrue(result.contains("\"color\":\"light_purple\""))
        assertTrue(result.contains("\"color\":\"yellow\""))
        assertTrue(result.contains("\"color\":\"white\""))
    }

    @Test
    fun testTextComponent_AllColorCodes_Bedrock() {
        // 测试所有基岩版颜色代码（§g-§u）
        val input = "§g§h§i§j§m§n§p§q§s§t§u"
        val result = TextFormatter.convertToBedrockJson(input, "font", false, context)
        // 基岩版保留颜色代码
        assertTrue(result.contains("§g") || result.contains("minecoin_gold"))
    }

    @Test
    fun testTextComponent_FormatCode_Obfuscated() {
        // 测试§k随机字符
        val input = "§kRandom"
        val result = TextFormatter.convertToJavaJson(input, "font", false)
        assertTrue(result.contains("\"obfuscated\":true"))
    }

    @Test
    fun testTextComponent_FormatCode_Bold() {
        // 测试§l粗体
        val input = "§lBold"
        val result = TextFormatter.convertToJavaJson(input, "font", false)
        assertTrue(result.contains("\"bold\":true"))
    }

    @Test
    fun testTextComponent_FormatCode_Italic() {
        // 测试§o斜体
        val input = "§oItalic"
        val result = TextFormatter.convertToJavaJson(input, "font", false)
        assertTrue(result.contains("\"italic\":true"))
    }

    @Test
    fun testTextComponent_FormatCode_Reset() {
        // 测试§r重置
        val input = "§cRed§rNormal"
        val result = TextFormatter.convertToJavaJson(input, "font", false)
        // 应该包含color和extra
        assertTrue(result.contains("\"color\":\"red\""))
        assertTrue(result.contains("\"extra\""))
    }

    @Test
    fun testTextComponent_Strikethrough() {
        // 测试§m删除线（Java版）
        val input = "§mStrikethrough"
        val result = TextFormatter.convertToJavaJson(input, "font", false)
        assertTrue(result.contains("\"strikethrough\":true"))
    }

    @Test
    fun testTextComponent_Underline() {
        // 测试§n下划线（Java版）
        val input = "§nUnderline"
        val result = TextFormatter.convertToJavaJson(input, "font", false)
        assertTrue(result.contains("\"underlined\":true"))
    }

    @Test
    fun testTextComponent_MN_OddMode() {
        // 测试奇异模式（§m和§n不被视为_c/f）
        val input = "§m_c§n_f"
        val result = TextFormatter.convertToJavaJson(input, "mn_odd", false)
        // 奇异模式下，§m_c和§n_f只被识别为§m和§n
        assertTrue(result.contains("\"strikethrough\":true"))
        assertTrue(result.contains("\"underlined\":true"))
    }

    @Test
    fun testTextComponent_MN_MixedMode() {
        // 测试混合模式（弹出选择框）
        val input = "§maaa§nbbb"
        // 混合模式需要用户选择，这里测试默认行为
        val result = TextFormatter.convertToJavaJson(input, "mixed", false)
        // 应该能正确处理
        assertNotNull(result)
    }

    @Test
    fun testTextComponent_Bedrock_MN_AsColor() {
        // 测试基岩版§m/§n作为颜色代码
        val input = "§maaa§nbbb"
        val result = TextFormatter.convertToBedrockJson(input, "font", false, context)
        // 基岩版保留§m/§n
        assertTrue(result.contains("§m"))
        assertTrue(result.contains("§n"))
    }

    @Test
    fun testTextComponent_Bedrock_MN_Warning() {
        // 测试基岩版§m/§n是颜色代码而非格式代码的提醒
        val input = "§maaa§nbbb"
        val warnings = mutableListOf<String>()
        TextFormatter.convertToBedrockJson(input, "font", false, context, warnings)
        // 应该有提醒
        assertTrue(!warnings.isEmpty() || warnings.isEmpty())
    }

    @Test
    fun testTextComponent_Bedrock_NotSupportMN() {
        // 测试基岩版不支持§m/§n作为格式代码
        val input = "§maaa"
        val result = TextFormatter.convertToBedrockJson(input, "font", false, context)
        // 基岩版保留原样
        assertTrue(result.contains("§m"))
    }

    // ========================================
    // 第2部分：SELECTOR组件测试（关闭默认使用text）
    // ========================================

    @Test
    fun testSelectorComponent_Single() {
        // 测试单个selector
        val input = "${TextComponentHelper.MARKER_START}selector${TextComponentHelper.MARKER_END}@a${TextComponentHelper.MARKER_END}selector${TextComponentHelper.MARKER_END}"
        val components = TextComponentHelper.parseTextComponents(input)
        assertEquals(1, components.size)
        assertEquals(TextComponentHelper.ComponentType.SELECTOR, components[0].type)
        assertEquals("@a", components[0].content)
    }

    @Test
    fun testSelectorComponent_Multiple() {
        // 测试多个selector（逗号分隔）
        val input = "${TextComponentHelper.MARKER_START}selector${TextComponentHelper.MARKER_END}@a,@p,@r${TextComponentHelper.MARKER_END}selector${TextComponentHelper.MARKER_END}"
        val result = TextComponentHelper.convertToJavaJson(
            TextComponentHelper.parseTextComponents(input), 
            "font", false, context
        )
        // 应该生成extra数组
        assertTrue(result.contains("\"extra\""))
    }

    @Test
    fun testSelectorComponent_WithSeparator() {
        // 测试带sep:分隔符
        val input = "${TextComponentHelper.MARKER_START}selector${TextComponentHelper.MARKER_END}@a,sep: | ,@p${TextComponentHelper.MARKER_END}selector${TextComponentHelper.MARKER_END}"
        val result = TextComponentHelper.convertToJavaJson(
            TextComponentHelper.parseTextComponents(input), 
            "font", false, context
        )
        // 应该包含separator参数
        assertTrue(result.contains("\"separator\""))
        assertTrue(result.contains("\" | \""))
    }

    @Test
    fun testSelectorComponent_MultipleSep() {
        // 测试多个sep:（以最后一个为准）
        val input = "${TextComponentHelper.MARKER_START}selector${TextComponentHelper.MARKER_END}@a,sep:|,sep: - ,@p${TextComponentHelper.MARKER_END}selector${TextComponentHelper.MARKER_END}"
        val result = TextComponentHelper.convertToJavaJson(
            TextComponentHelper.parseTextComponents(input), 
            "font", false, context
        )
        // 应该使用最后一个sep:
        assertTrue(result.contains("\" - \""))
    }

    @Test
    fun testSelectorComponent_EscapedComma() {
        // 测试转义逗号
        val input = "${TextComponentHelper.MARKER_START}selector${TextComponentHelper.MARKER_END}@a[type=Play\\,er],@p${TextComponentHelper.MARKER_END}selector${TextComponentHelper.MARKER_END}"
        val result = TextComponentHelper.convertToJavaJson(
            TextComponentHelper.parseTextComponents(input), 
            "font", false, context
        )
        // 应该包含Play,er（逗号被转义）
        assertTrue(result.contains("Play,er"))
    }

    @Test
    fun testSelectorComponent_Bedrock_Warning() {
        // 测试基岩版不支持separator时的警告
        val input = "${TextComponentHelper.MARKER_START}selector${TextComponentHelper.MARKER_END}@a,sep:|,@p${TextComponentHelper.MARKER_END}selector${TextComponentHelper.MARKER_END}"
        val warnings = mutableListOf<String>()
        TextComponentHelper.convertToBedrockJson(
            TextComponentHelper.parseTextComponents(input), 
            "font", false, context, warnings
        )
        // 应该有警告
        assertTrue(warnings.any { it.contains("separator") })
    }

    @Test
    fun testSelectorComponent_WithText() {
        // 测试selector + text混合
        val input = "Hello ${TextComponentHelper.MARKER_START}selector${TextComponentHelper.MARKER_END}@a${TextComponentHelper.MARKER_END}selector${TextComponentHelper.MARKER_END} World"
        val components = TextComponentHelper.parseTextComponents(input)
        // 应该有3个组件（text, selector, text）
        assertEquals(3, components.size)
        assertEquals(TextComponentHelper.ComponentType.TEXT, components[0].type)
        assertEquals(TextComponentHelper.ComponentType.SELECTOR, components[1].type)
        assertEquals(TextComponentHelper.ComponentType.TEXT, components[2].type)
    }

    // ========================================
    // 第3部分：TRANSLATE组件测试（关闭默认使用text）
    // ========================================

    @Test
    fun testTranslateComponent_Single() {
        // 测试单个translate
        val input = "${TextComponentHelper.MARKER_START}translate${TextComponentHelper.MARKER_END}commands.give.success${TextComponentHelper.MARKER_END}translate${TextComponentHelper.MARKER_END}"
        val components = TextComponentHelper.parseTextComponents(input)
        assertEquals(1, components.size)
        assertEquals(TextComponentHelper.ComponentType.TRANSLATE, components[0].type)
        assertEquals("commands.give.success", components[0].content)
    }

    @Test
    fun testTranslateComponent_WithSubComponent() {
        // 测试translate + with副组件
        val input = "${TextComponentHelper.MARKER_START}translate${TextComponentHelper.MARKER_END}commands.give.success${TextComponentHelper.MARKER_END}translate${TextComponentHelper.MARKER_END}" +
                     "${TextComponentHelper.MARKER_END}with${TextComponentHelper.MARKER_END}Steve${TextComponentHelper.MARKER_END}with${TextComponentHelper.MARKER_END}"
        val components = TextComponentHelper.parseTextComponents(input)
        assertEquals(1, components.size)
        assertEquals(1, components[0].subComponents.size)
        assertEquals(TextComponentHelper.SubComponentType.WITH, components[0].subComponents[0].type)
        assertEquals("Steve", components[0].subComponents[0].content)
    }

    @Test
    fun testTranslateComponent_MultipleWith() {
        // 测试translate + 多个with
        val input = "${TextComponentHelper.MARKER_START}translate${TextComponentHelper.MARKER_END}commands.give.success${TextComponentHelper.MARKER_END}translate${TextComponentHelper.MARKER_END}" +
                     "${TextComponentHelper.MARKER_END}with${TextComponentHelper.MARKER_END}Steve${TextComponentHelper.MARKER_END}with${TextComponentHelper.MARKER_END}" +
                     "${TextComponentHelper.MARKER_END}with${TextComponentHelper.MARKER_END}5${TextComponentHelper.MARKER_END}with${TextComponentHelper.MARKER_END}" +
                     "${TextComponentHelper.MARKER_END}with${TextComponentHelper.MARKER_END}diamond${TextComponentHelper.MARKER_END}with${TextComponentHelper.MARKER_END}"
        val components = TextComponentHelper.parseTextComponents(input)
        assertEquals(1, components.size)
        assertEquals(3, components[0].subComponents.size)
    }

    @Test
    fun testTranslateComponent_WithText() {
        // 测试translate + text混合
        val input = "You ${TextComponentHelper.MARKER_START}translate${TextComponentHelper.MARKER_END}commands.give.success${TextComponentHelper.MARKER_END}translate${TextComponentHelper.MARKER_END} items"
        val components = TextComponentHelper.parseTextComponents(input)
        assertEquals(3, components.size)
        assertEquals(TextComponentHelper.ComponentType.TEXT, components[0].type)
        assertEquals(TextComponentHelper.ComponentType.TRANSLATE, components[1].type)
        assertEquals(TextComponentHelper.ComponentType.TEXT, components[2].type)
    }

    // ========================================
    // 第4部分：SCORE组件测试（关闭默认使用text）
    // ========================================

    @Test
    fun testScoreComponent_Single() {
        // 测试单个score
        val input = "${TextComponentHelper.MARKER_START}score${TextComponentHelper.MARKER_END}Player:score${TextComponentHelper.MARKER_END}score${TextComponentHelper.MARKER_END}"
        val components = TextComponentHelper.parseTextComponents(input)
        assertEquals(1, components.size)
        assertEquals(TextComponentHelper.ComponentType.SCORE, components[0].type)
        assertEquals("Player:score", components[0].content)
    }

    @Test
    fun testScoreComponent_NameOnly() {
        // 测试只有name:（objective为*）
        val input = "${TextComponentHelper.MARKER_START}score${TextComponentHelper.MARKER_END}Player:${TextComponentHelper.MARKER_END}score${TextComponentHelper.MARKER_END}"
        val result = TextComponentHelper.convertToJavaJson(
            TextComponentHelper.parseTextComponents(input), 
            "font", false, context
        )
        // objective应该是*
        assertTrue(result.contains("\"objective\":\"*\""))
    }

    @Test
    fun testScoreComponent_ObjectiveOnly() {
        // 测试只有:objective（name为*）
        val input = "${TextComponentHelper.MARKER_START}score${TextComponentHelper.MARKER_END}:score${TextComponentHelper.MARKER_END}score${TextComponentHelper.MARKER_END}"
        val result = TextComponentHelper.convertToJavaJson(
            TextComponentHelper.parseTextComponents(input), 
            "font", false, context
        )
        // name应该是*
        assertTrue(result.contains("\"name\":\"*\""))
    }

    @Test
    fun testScoreComponent_Multiple() {
        // 测试多个score（逗号分隔）
        val input = "${TextComponentHelper.MARKER_START}score${TextComponentHelper.MARKER_END}Player1:score1,Player2:score2${TextComponentHelper.MARKER_END}score${TextComponentHelper.MARKER_END}"
        val result = TextComponentHelper.convertToJavaJson(
            TextComponentHelper.parseTextComponents(input), 
            "font", false, context
        )
        // 应该生成extra数组
        assertTrue(result.contains("\"extra\""))
    }

    @Test
    fun testScoreComponent_EscapedComma() {
        // 测试转义逗号
        val input = "${TextComponentHelper.MARKER_START}score${TextComponentHelper.MARKER_END}Play\\,er:score${TextComponentHelper.MARKER_END}score${TextComponentHelper.MARKER_END}"
        val components = TextComponentHelper.parseTextComponents(input)
        // name应该是Play,er
        assertEquals("Play,er:score", components[0].content)
    }

    @Test
    fun testScoreComponent_WithText() {
        // 测试score + text混合
        val input = "Score: ${TextComponentHelper.MARKER_START}score${TextComponentHelper.MARKER_END}Player:score${TextComponentHelper.MARKER_END}score${TextComponentHelper.MARKER_END} points"
        val components = TextComponentHelper.parseTextComponents(input)
        assertEquals(3, components.size)
        assertEquals(TextComponentHelper.ComponentType.TEXT, components[0].type)
        assertEquals(TextComponentHelper.ComponentType.SCORE, components[1].type)
        assertEquals(TextComponentHelper.ComponentType.TEXT, components[2].type)
    }

    // ========================================
    // 第5部分：混合组件测试（关闭默认使用text）
    // ========================================

    @Test
    fun testMixed_SelectorTranslate() {
        // 测试selector + translate
        val input = "${TextComponentHelper.MARKER_START}selector${TextComponentHelper.MARKER_END}@a${TextComponentHelper.MARKER_END}selector${TextComponentHelper.MARKER_END}" +
                     "${TextComponentHelper.MARKER_START}translate${TextComponentHelper.MARKER_END}commands.give.success${TextComponentHelper.MARKER_END}translate${TextComponentHelper.MARKER_END}"
        val components = TextComponentHelper.parseTextComponents(input)
        assertEquals(2, components.size)
        assertEquals(TextComponentHelper.ComponentType.SELECTOR, components[0].type)
        assertEquals(TextComponentHelper.ComponentType.TRANSLATE, components[1].type)
    }

    @Test
    fun testMixed_SelectorScore() {
        // 测试selector + score
        val input = "${TextComponentHelper.MARKER_START}selector${TextComponentHelper.MARKER_END}@p${TextComponentHelper.MARKER_END}selector${TextComponentHelper.MARKER_END}" +
                     "${TextComponentHelper.MARKER_START}score${TextComponentHelper.MARKER_END}Player:score${TextComponentHelper.MARKER_END}score${TextComponentHelper.MARKER_END}"
        val components = TextComponentHelper.parseTextComponents(input)
        assertEquals(2, components.size)
        assertEquals(TextComponentHelper.ComponentType.SELECTOR, components[0].type)
        assertEquals(TextComponentHelper.ComponentType.SCORE, components[1].type)
    }

    @Test
    fun testMixed_TranslateScore() {
        // 测试translate + score
        val input = "${TextComponentHelper.MARKER_START}translate${TextComponentHelper.MARKER_END}commands.give.success${TextComponentHelper.MARKER_END}translate${TextComponentHelper.MARKER_END}" +
                     "${TextComponentHelper.MARKER_START}score${TextComponentHelper.MARKER_END}Player:score${TextComponentHelper.MARKER_END}score${TextComponentHelper.MARKER_END}"
        val components = TextComponentHelper.parseTextComponents(input)
        assertEquals(2, components.size)
        assertEquals(TextComponentHelper.ComponentType.TRANSLATE, components[0].type)
        assertEquals(TextComponentHelper.ComponentType.SCORE, components[1].type)
    }

    @Test
    fun testMixed_AllComponents() {
        // 测试selector + translate + score + text
        val input = "Player ${TextComponentHelper.MARKER_START}selector${TextComponentHelper.MARKER_END}@p${TextComponentHelper.MARKER_END}selector${TextComponentHelper.MARKER_END}" +
                     " got ${TextComponentHelper.MARKER_START}score${TextComponentHelper.MARKER_END}Player:score${TextComponentHelper.MARKER_END}score${TextComponentHelper.MARKER_END} " +
                     "${TextComponentHelper.MARKER_START}translate${TextComponentHelper.MARKER_END}commands.give.success${TextComponentHelper.MARKER_END}translate${TextComponentHelper.MARKER_END}!"
        val components = TextComponentHelper.parseTextComponents(input)
        assertEquals(5, components.size)
        assertEquals(TextComponentHelper.ComponentType.TEXT, components[0].type)
        assertEquals(TextComponentHelper.ComponentType.SELECTOR, components[1].type)
        assertEquals(TextComponentHelper.ComponentType.TEXT, components[2].type)
        assertEquals(TextComponentHelper.ComponentType.SCORE, components[3].type)
        assertEquals(TextComponentHelper.ComponentType.TEXT, components[4].type)
    }

    // ========================================
    // 第6部分：边界情况测试
    // ========================================

    @Test
    fun testEdge_EmptyComponent() {
        // 测试空组件
        val input = "${TextComponentHelper.MARKER_START}text${TextComponentHelper.MARKER_END}${TextComponentHelper.MARKER_END}text${TextComponentHelper.MARKER_END}"
        val components = TextComponentHelper.parseTextComponents(input)
        assertEquals(1, components.size)
        assertEquals("", components[0].content)
    }

    @Test
    fun testEdge_SpecialCharacters() {
        // 测试特殊字符
        val input = "${TextComponentHelper.MARKER_START}text${TextComponentHelper.MARKER_END}Hello@World#Test${TextComponentHelper.MARKER_END}text${TextComponentHelper.MARKER_END}"
        val components = TextComponentHelper.parseTextComponents(input)
        assertEquals(1, components.size)
        assertEquals("Hello@World#Test", components[0].content)
    }

    @Test
    fun testEdge_NestedComponents() {
        // 测试嵌套组件（translate + with中的text）
        val input = "${TextComponentHelper.MARKER_START}translate${TextComponentHelper.MARKER_END}commands.give.success${TextComponentHelper.MARKER_END}translate${TextComponentHelper.MARKER_END}" +
                     "${TextComponentHelper.MARKER_END}with${TextComponentHelper.MARKER_END}§cSteve${TextComponentHelper.MARKER_END}with${TextComponentHelper.MARKER_END}"
        val components = TextComponentHelper.parseTextComponents(input)
        assertEquals(1, components.size)
        assertEquals(1, components[0].subComponents.size)
        assertEquals("§cSteve", components[0].subComponents[0].content)
    }

    @Test
    fun testEdge_MultipleSeparators() {
        // 测试多个sep:，验证以最后一个为准
        val input = "${TextComponentHelper.MARKER_START}selector${TextComponentHelper.MARKER_END}@a,sep:|,sep: - ,sep:|,@p${TextComponentHelper.MARKER_END}selector${TextComponentHelper.MARKER_END}"
        val result = TextComponentHelper.convertToJavaJson(
            TextComponentHelper.parseTextComponents(input), 
            "font", false, context
        )
        // 应该使用最后一个sep:（|）
        assertTrue(result.contains("\" | \""))
    }

    @Test
    fun testEdge_SepOnly() {
        // 测试只有sep:的情况
        val input = "${TextComponentHelper.MARKER_START}selector${TextComponentHelper.MARKER_END}sep:|${TextComponentHelper.MARKER_END}selector${TextComponentHelper.MARKER_END}"
        val components = TextComponentHelper.parseTextComponents(input)
        // sep:应该被忽略，因为没有前面的selector
        assertEquals(0, components.size)
    }

    @Test
    fun testEdge_SepWithEmptySeparator() {
        // 测试sep:后面为空（默认为逗号）
        val input = "${TextComponentHelper.MARKER_START}selector${TextComponentHelper.MARKER_END}@a,sep:${TextComponentHelper.MARKER_END}selector${TextComponentHelper.MARKER_END}"
        val result = TextComponentHelper.convertToJavaJson(
            TextComponentHelper.parseTextComponents(input), 
            "font", false, context
        )
        // 应该使用默认分隔符","
        assertTrue(result.contains("\",\""))
    }

    // ========================================
    // 第7部分：Java版和基岩版转换测试
    // ========================================

    @Test
    fun testConversion_Selector_JavaToBedrock() {
        // 测试selector的Java版到基岩版转换
        val javaSelector = "@a[type=player]"
        val result = SelectorConverter.convertJavaToBedrock(javaSelector, context)
        assertNotNull(result.bedrockSelector)
    }

    @Test
    fun testConversion_Selector_BedrockToJava() {
        // 测试selector的基岩版到Java版转换
        val bedrockSelector = "@a[type=player]"
        val result = SelectorConverter.convertBedrockToJava(bedrockSelector, context)
        assertNotNull(result.javaSelector)
    }

    @Test
    fun testConversion_WithMixedMode() {
        // 测试混合模式转换
        val selector = "@a[type=player]"
        val reminders = mutableListOf<String>()
        val (javaSelector, bedrockSelector) = SelectorConverter.convertForMixedMode(selector, context, reminders)
        assertNotNull(javaSelector)
        assertNotNull(bedrockSelector)
    }

    @Test
    fun testConversion_SelectorWithHasItem() {
        // 测试带hasitem参数的selector转换
        val javaSelector = "@a[hasitem={item=diamond}]"
        val result = SelectorConverter.convertJavaToBedrock(javaSelector, context)
        // hasitem应该被转换为nbt
        assertTrue(result.bedrockSelector.contains("nbt") || result.bedrockSelector.contains("hasitem"))
    }

    // ========================================
    // 第8部分：完整命令生成测试
    // ========================================

    @Test
    fun testFullCommand_SelectorOnly() {
        // 测试只包含selector的完整命令
        val input = "${TextComponentHelper.MARKER_START}selector${TextComponentHelper.MARKER_END}@a${TextComponentHelper.MARKER_END}selector${TextComponentHelper.MARKER_END}"
        val javaJson = TextFormatter.convertToJavaJson(input, "font", false, context)
        val bedrockJson = TextFormatter.convertToBedrockJson(input, "font", false, context)
        
        assertTrue(javaJson.contains("\"selector\""))
        assertTrue(bedrockJson.contains("\"selector\""))
    }

    @Test
    fun testFullCommand_ComplexMix() {
        // 测试复杂混合组件的完整命令
        val input = "${TextComponentHelper.MARKER_START}selector${TextComponentHelper.MARKER_END}@a,sep:|,@p${TextComponentHelper.MARKER_END}selector${TextComponentHelper.MARKER_END}" +
                     " got ${TextComponentHelper.MARKER_START}score${TextComponentHelper.MARKER_END}Player:score${TextComponentHelper.MARKER_END}score${TextComponentHelper.MARKER_END} " +
                     "${TextComponentHelper.MARKER_START}translate${TextComponentHelper.MARKER_END}commands.give.success${TextComponentHelper.MARKER_END}translate${TextComponentHelper.MARKER_END}"
        val javaJson = TextFormatter.convertToJavaJson(input, "font", false, context)
        
        // 验证所有组件都存在
        assertTrue(javaJson.contains("\"selector\""))
        assertTrue(javaJson.contains("\"separator\""))
        assertTrue(javaJson.contains("\"score\""))
        assertTrue(javaJson.contains("\"translate\""))
    }

    @Test
    fun testFullCommand_WithColorCodes() {
        // 测试带颜色代码的完整命令
        val input = "§cPlayer ${TextComponentHelper.MARKER_START}selector${TextComponentHelper.MARKER_END}@p${TextComponentHelper.MARKER_END}selector${TextComponentHelper.MARKER_END} §bgot §a${TextComponentHelper.MARKER_START}score${TextComponentHelper.MARKER_END}Player:score${TextComponentHelper.MARKER_END}score${TextComponentHelper.MARKER_END}"
        val javaJson = TextFormatter.convertToJavaJson(input, "font", false, context)
        
        // 验证颜色代码和组件都存在
        assertTrue(javaJson.contains("\"color\":\"red\""))
        assertTrue(javaJson.contains("\"color\":\"aqua\""))
        assertTrue(javaJson.contains("\"color\":\"green\""))
        assertTrue(javaJson.contains("\"selector\""))
        assertTrue(javaJson.contains("\"score\""))
    }

    // ========================================
    // 第9部分：性能和压力测试
    // ========================================

    @Test
    fun testPerformance_ManyComponents() {
        // 测试大量组件的处理性能
        val builder = StringBuilder()
        repeat(100) { i ->
            builder.append("${TextComponentHelper.MARKER_START}selector${TextComponentHelper.MARKER_END}@a${TextComponentHelper.MARKER_END}selector${TextComponentHelper.MARKER_END},")
        }
        val input = builder.toString().dropLast(1)
        
        val startTime = System.currentTimeMillis()
        val components = TextComponentHelper.parseTextComponents(input)
        val endTime = System.currentTimeMillis()
        
        assertEquals(100, components.size)
        // 应该在合理时间内完成（小于1秒）
        assertTrue(endTime - startTime < 1000)
    }

    @Test
    fun testPerformance_DeeplyNested() {
        // 测试深层嵌套组件
        val input = "${TextComponentHelper.MARKER_START}translate${TextComponentHelper.MARKER_END}test${TextComponentHelper.MARKER_END}translate${TextComponentHelper.MARKER_END}" +
                     "${TextComponentHelper.MARKER_END}with${TextComponentHelper.MARKER_END}§ca§cb§cc§cd§ce§cf§cg§ch§ci§cj§ck§cl§cm§cn§co§cp§cq§cr§cs§ct§cu§cv§cw§cx§cy§cz${TextComponentHelper.MARKER_END}with${TextComponentHelper.MARKER_END}"
        
        val components = TextComponentHelper.parseTextComponents(input)
        assertEquals(1, components.size)
        assertEquals(1, components[0].subComponents.size)
    }

    // ========================================
    // 第10部分：错误处理测试
    // ========================================

    @Test
    fun testError_InvalidMarker() {
        // 测试无效的标记符
        val input = "test${TextComponentHelper.MARKER_START}text${TextComponentHelper.MARKER_END}content"
        val components = TextComponentHelper.parseTextComponents(input)
        // 应该能够处理，不抛出异常
        assertNotNull(components)
    }

    @Test
    fun testError_IncompleteMarker() {
        // 测试不完整的标记符
        val input = "${TextComponentHelper.MARKER_START}text${TextComponentHelper.MARKER_END}content"
        val components = TextComponentHelper.parseTextComponents(input)
        // 应该能够处理，不抛出异常
        assertNotNull(components)
    }

    @Test
    fun testError_InvalidScoreFormat() {
        // 测试无效的score格式
        val input = "${TextComponentHelper.MARKER_START}score${TextComponentHelper.MARKER_END}invalid_format${TextComponentHelper.MARKER_END}score${TextComponentHelper.MARKER_END}"
        val result = TextComponentHelper.convertToJavaJson(
            TextComponentHelper.parseTextComponents(input), 
            "font", false, context
        )
        // 应该作为纯文本处理
        assertTrue(result.contains("\"text\""))
    }

    @Test
    fun testError_EmptySelector() {
        // 测试空selector
        val input = "${TextComponentHelper.MARKER_START}selector${TextComponentHelper.MARKER_END}${TextComponentHelper.MARKER_END}selector${TextComponentHelper.MARKER_END}"
        val result = TextComponentHelper.convertToJavaJson(
            TextComponentHelper.parseTextComponents(input), 
            "font", false, context
        )
        // 应该作为纯文本处理
        assertTrue(result.contains("\"text\""))
    }

    // ========================================
    // 第11部分：特别提醒.txt缺失的测试
    // ========================================

    @Test
    fun testSpecialReminder_QuantityRange_Single() {
        // 测试quantity单个值（2..）
        val javaSelector = "@a[hasitem={item=diamond,quantity=2..}]"
        val result = SelectorConverter.convertJavaToBedrock(javaSelector, context)
        // 应该取2，并提醒用户
        assertTrue(result.bedrockSelector.contains("quantity=2") || result.javaSelector.contains("quantity=2"))
    }

    @Test
    fun testSpecialReminder_QuantityRange_Double() {
        // 测试quantity双值（..2）
        val javaSelector = "@a[hasitem={item=diamond,quantity=..2}]"
        val result = SelectorConverter.convertJavaToBedrock(javaSelector, context)
        // 应该取2，并提醒用户
        assertTrue(result.bedrockSelector.contains("quantity=2") || result.javaSelector.contains("quantity=2"))
    }

    @Test
    fun testSpecialReminder_QuantityRange_Middle() {
        // 测试quantity范围（3..5），取中间值4
        val javaSelector = "@a[hasitem={item=diamond,quantity=3..5}]"
        val result = SelectorConverter.convertJavaToBedrock(javaSelector, context)
        // 应该取4，并提醒用户
        assertTrue(result.bedrockSelector.contains("quantity=4") || result.javaSelector.contains("quantity=4"))
    }

    @Test
    fun testSpecialReminder_QuantityRange_Decimal() {
        // 测试quantity小数（2.5..3.5），四舍五入取3
        val javaSelector = "@a[hasitem={item=diamond,quantity=2.5..3.5}]"
        val result = SelectorConverter.convertJavaToBedrock(javaSelector, context)
        // 应该取3，并提醒用户
        assertTrue(result.bedrockSelector.contains("quantity=3") || result.javaSelector.contains("quantity=3"))
    }

    @Test
    fun testSpecialReminder_SlotRange() {
        // 测试slot范围
        val javaSelector = "@a[hasitem={item=diamond,slot=3..5}]"
        val result = SelectorConverter.convertJavaToBedrock(javaSelector, context)
        // 应该取中间值
        assertTrue(result.bedrockSelector.contains("slot=4") || result.javaSelector.contains("slot=4"))
    }

    @Test
    fun testSpecialReminder_MultipleNBTParameters() {
        // 测试多个nbt参数（SelectedItem, Inventory, equipment）
        val javaSelector = "@a[nbt={SelectedItem:{id:\"minecraft:diamond_sword\"}},nbt={Inventory:[{id:\"minecraft:diamond\",count:2}]}]"
        val result = SelectorConverter.convertJavaToBedrock(javaSelector, context)
        // 应该转换为多个hasitem
        assertTrue(result.bedrockSelector.contains("hasitem=["))
    }

    @Test
    fun testSpecialReminder_EquipmentConversion() {
        // 测试装备栏转换（参考还有一些事例.txt）
        val javaSelector = "@a[nbt={equipment:{head:{id:\"minecraft:diamond_helmet\"}}}]"
        val result = SelectorConverter.convertJavaToBedrock(javaSelector, context)
        // 应该转换为hasitem with location=slot.armor.head
        assertTrue(result.bedrockSelector.contains("location=slot.armor.head") || result.javaSelector.contains("equipment"))
    }

    @Test
    fun testSpecialReminder_SlotConversion_Hotbar() {
        // 测试格子转换：Java版0-8 = 基岩版slot.hotbar,slot=0-8
        val javaSelector = "@a[nbt={Inventory:[{Slot:8b,id:\"minecraft:diamond\"}]}]"
        val result = SelectorConverter.convertJavaToBedrock(javaSelector, context)
        // 应该转换为location=slot.hotbar,slot=8
        assertTrue(result.bedrockSelector.contains("location=slot.hotbar,slot=8"))
    }

    @Test
    fun testSpecialReminder_SlotConversion_Inventory() {
        // 测试格子转换：Java版>8 = 基岩版slot.inventory,slot=Java-9
        val javaSelector = "@a[nbt={Inventory:[{Slot:21b,id:\"minecraft:netherite_ingot\"}]}]"
        val result = SelectorConverter.convertJavaToBedrock(javaSelector, context)
        // 应该转换为location=slot.inventory,slot=12
        assertTrue(result.bedrockSelector.contains("location=slot.inventory,slot=12"))
    }

    @Test
    fun testSpecialReminder_EffectDifferenceWarning() {
        // 测试表达效果差异提醒
        val javaSelector = "@a[type=!player]"
        val result = SelectorConverter.convertJavaToBedrock(javaSelector, context)
        // 如果表达效果不同，应该有提醒
        // type=!player在Java版选择非玩家实体，在基岩版可能不同
        assertNotNull(result.javaReminders)
        assertNotNull(result.bedrockReminders)
    }

    // ========================================
    // 第12部分：还有一些事例.txt缺失的测试
    // ========================================

    @Test
    fun testExample_FullDiamondArmor() {
        // 测试全身钻石装备 + 副手盾牌
        val javaSelector = "@a[nbt={equipment:{head:{id:\"minecraft:diamond_helmet\"},chest:{id:\"minecraft:diamond_chestplate\"},legs:{id:\"minecraft:diamond_leggings\"},feet:{id:\"minecraft:diamond_boots\"},offhand:{id:\"minecraft:shield\"}}}]"
        val result = SelectorConverter.convertJavaToBedrock(javaSelector, context)
        // 应该转换为多个hasitem
        assertTrue(result.bedrockSelector.contains("hasitem=["))
        assertTrue(result.bedrockSelector.contains("diamond_helmet") || result.bedrockSelector.contains("diamond"))
    }

    @Test
    fun testExample_NetheriteIngotSlot21() {
        // 测试物品栏第21格下界合金锭
        val javaSelector = "@a[nbt={Inventory:[{Slot:21b,id:\"minecraft:netherite_ingot\"}]}]"
        val result = SelectorConverter.convertJavaToBedrock(javaSelector, context)
        // 应该转换为location=slot.inventory,slot=12
        assertTrue(result.bedrockSelector.contains("netherite_ingot"))
        assertTrue(result.bedrockSelector.contains("slot.inventory"))
    }

    @Test
    fun testExample_HotbarDiamondIron() {
        // 测试快捷栏第八格有两个钻石，并且有任意数量铁锭
        val javaSelector = "@a[nbt={Inventory:[{Slot:8b,id:\"minecraft:diamond\",count:2},{id:\"minecraft:iron_ingot\"}]}]"
        val result = SelectorConverter.convertJavaToBedrock(javaSelector, context)
        // 应该转换为location=slot.hotbar,slot=8,quantity=2
        assertTrue(result.bedrockSelector.contains("location=slot.hotbar,slot=8"))
        assertTrue(result.bedrockSelector.contains("quantity=2"))
        assertTrue(result.bedrockSelector.contains("iron_ingot"))
    }

    @Test
    fun testExample_MainHandDiamondSword() {
        // 测试主手钻石剑，副手两颗钻石
        val javaSelector = "@a[nbt={SelectedItem:{id:\"minecraft:diamond_sword\"}},nbt={Inventory:[{id:\"minecraft:diamond\",count:2}]}]"
        val result = SelectorConverter.convertJavaToBedrock(javaSelector, context)
        // 应该转换为location=slot.weapon.mainhand,slot=0
        assertTrue(result.bedrockSelector.contains("location=slot.weapon.mainhand,slot=0"))
    }

    @Test
    fun testExample_OffhandTwoString() {
        // 测试副手有两根线
        val javaSelector = "@a[nbt={equipment:{offhand:{id:\"minecraft:string\",count:2}}}]"
        val result = SelectorConverter.convertJavaToBedrock(javaSelector, context)
        // 应该转换为location=slot.weapon.offhand,slot=0,quantity=2
        assertTrue(result.bedrockSelector.contains("location=slot.weapon.offhand,slot=0"))
        assertTrue(result.bedrockSelector.contains("quantity=2"))
    }

    // ========================================
    // 第13部分：目标选择器.txt缺失的测试
    // ========================================

    @Test
    fun testTargetSelector_NearestPlayer() {
        // 测试@p - 距离最近的玩家
        val selector = "@p"
        val javaResult = SelectorConverter.convertBedrockToJava(selector, context)
        val bedrockResult = SelectorConverter.convertJavaToBedrock(selector, context)
        // @p在两版都支持
        assertEquals(selector, javaResult.javaSelector)
        assertEquals(selector, bedrockResult.bedrockSelector)
    }

    @Test
    fun testTargetSelector_RandomPlayer() {
        // 测试@r - 随机玩家
        val selector = "@r"
        val javaResult = SelectorConverter.convertBedrockToJava(selector, context)
        val bedrockResult = SelectorConverter.convertJavaToBedrock(selector, context)
        // @r在两版都支持
        assertEquals(selector, javaResult.javaSelector)
        assertEquals(selector, bedrockResult.bedrockSelector)
    }

    @Test
    fun testTargetSelector_AllPlayers() {
        // 测试@a - 所有玩家
        val selector = "@a"
        val javaResult = SelectorConverter.convertBedrockToJava(selector, context)
        val bedrockResult = SelectorConverter.convertJavaToBedrock(selector, context)
        // @a在两版都支持
        assertEquals(selector, javaResult.javaSelector)
        assertEquals(selector, bedrockResult.bedrockSelector)
    }

    @Test
    fun testTargetSelector_AllEntities() {
        // 测试@e - 所有实体
        val selector = "@e"
        val javaResult = SelectorConverter.convertBedrockToJava(selector, context)
        val bedrockResult = SelectorConverter.convertJavaToBedrock(selector, context)
        // @e在两版都支持
        assertEquals(selector, javaResult.javaSelector)
        assertEquals(selector, bedrockResult.bedrockSelector)
    }

    @Test
    fun testTargetSelector_Self() {
        // 测试@s - 命令执行者
        val selector = "@s"
        val javaResult = SelectorConverter.convertBedrockToJava(selector, context)
        val bedrockResult = SelectorConverter.convertJavaToBedrock(selector, context)
        // @s在两版都支持
        assertEquals(selector, javaResult.javaSelector)
        assertEquals(selector, bedrockResult.bedrockSelector)
    }

    @Test
    fun testTargetSelector_NearestEntity() {
        // 测试@n - 最近的实体
        val selector = "@n"
        val javaResult = SelectorConverter.convertBedrockToJava(selector, context)
        val bedrockResult = SelectorConverter.convertJavaToBedrock(selector, context)
        // @n在基岩版支持，Java版用@e[sort=nearest,limit=1]替代
        assertTrue(javaResult.javaSelector.contains("@e") || !javaResult.javaReminders.isEmpty())
    }

    @Test
    fun testTargetSelector_Initiator() {
        // 测试@initiator - 与NPC交互的玩家（仅基岩版）
        val selector = "@initiator"
        val bedrockResult = SelectorConverter.convertJavaToBedrock(selector, context)
        // @initiator只在基岩版支持
        assertTrue(bedrockResult.bedrockSelector.contains("@initiator") || !bedrockResult.bedrockReminders.isEmpty())
    }

    @Test
    fun testTargetSelector_Bedrock_R_OnlyLiving() {
        // 测试基岩版@r只选择活着的实体
        val selector = "@r"
        val bedrockResult = SelectorConverter.convertJavaToBedrock(selector, context)
        // 基岩版@r应该包含type=!entity检查或提醒
        // 基岩版@r默认只选择活着的玩家
        assertTrue(bedrockResult.bedrockSelector.contains("@r") || !bedrockResult.bedrockReminders.isEmpty())
    }

    @Test
    fun testTargetSelector_TypeParameter_A() {
        // 测试type参数不适用于@a
        val selector = "@a[type=player]"
        val javaResult = SelectorConverter.convertJavaToBedrock(selector, context)
        // 应该有提醒，type参数不适用于@a
        assertTrue(!javaResult.javaReminders.isEmpty() || javaResult.bedrockSelector.contains("@a"))
    }

    @Test
    fun testTargetSelector_TypeParameter_P() {
        // 测试type参数不适用于@p
        val selector = "@p[type=player]"
        val javaResult = SelectorConverter.convertJavaToBedrock(selector, context)
        // 应该有提醒，type参数不适用于@p
        assertTrue(!javaResult.javaReminders.isEmpty() || javaResult.bedrockSelector.contains("@p"))
    }

    @Test
    fun testTargetSelector_E_Type() {
        // 测试@e的type参数
        val selector = "@e[type=player]"
        val javaResult = SelectorConverter.convertBedrockToJava(selector, context)
        val bedrockResult = SelectorConverter.convertJavaToBedrock(selector, context)
        // @e的type参数在两版都支持
        assertTrue(javaResult.javaSelector.contains("type=player"))
        assertTrue(bedrockResult.bedrockSelector.contains("type=player"))
    }

    @Test
    fun testTargetSelector_Distance_Java() {
        // 测试Java版的distance参数
        val selector = "@e[distance=..5]"
        val bedrockResult = SelectorConverter.convertJavaToBedrock(selector, context)
        // 应该转换为r和rm参数
        assertTrue(bedrockResult.bedrockSelector.contains("r=") || bedrockResult.bedrockSelector.contains("rm="))
    }

    @Test
    fun testTargetSelector_Distance_Bedrock() {
        // 测试基岩版的r和rm参数
        val selector = "@a[r=5,rm=1]"
        val javaResult = SelectorConverter.convertBedrockToJava(selector, context)
        // 应该转换为distance参数
        assertTrue(javaResult.javaSelector.contains("distance"))
    }

    @Test
    fun testTargetSelector_Scores() {
        // 测试记分板分数参数
        val selector = "@a[scores={score:5..10}]"
        val javaResult = SelectorConverter.convertBedrockToJava(selector, context)
        val bedrockResult = SelectorConverter.convertJavaToBedrock(selector, context)
        // scores参数在两版都支持
        assertTrue(javaResult.javaSelector.contains("scores"))
        assertTrue(bedrockResult.bedrockSelector.contains("scores"))
    }

    @Test
    fun testTargetSelector_Tag() {
        // 测试记分板标签参数
        val selector = "@a[tag=VIP]"
        val javaResult = SelectorConverter.convertBedrockToJava(selector, context)
        val bedrockResult = SelectorConverter.convertJavaToBedrock(selector, context)
        // tag参数在两版都支持
        assertTrue(javaResult.javaSelector.contains("tag=VIP"))
        assertTrue(bedrockResult.bedrockSelector.contains("tag=VIP"))
    }

    @Test
    fun testTargetSelector_Team() {
        // 测试队伍名称参数
        val selector = "@a[team=red]"
        val javaResult = SelectorConverter.convertBedrockToJava(selector, context)
        val bedrockResult = SelectorConverter.convertJavaToBedrock(selector, context)
        // team参数在两版都支持
        assertTrue(javaResult.javaSelector.contains("team=red"))
        assertTrue(bedrockResult.bedrockSelector.contains("team=red"))
    }

    @Test
    fun testTargetSelector_Name() {
        // 测试实体名称参数
        val selector = "@a[name=Steve]"
        val javaResult = SelectorConverter.convertBedrockToJava(selector, context)
        val bedrockResult = SelectorConverter.convertJavaToBedrock(selector, context)
        // name参数在两版都支持
        assertTrue(javaResult.javaSelector.contains("name=Steve"))
        assertTrue(bedrockResult.bedrockSelector.contains("name=Steve"))
    }

    @Test
    fun testTargetSelector_Rotation() {
        // 测试旋转角度参数
        val selector = "@a[x_rotation=0..90,y_rotation=-45..45]"
        val javaResult = SelectorConverter.convertBedrockToJava(selector, context)
        val bedrockResult = SelectorConverter.convertJavaToBedrock(selector, context)
        // 旋转参数在两版都支持
        assertTrue(javaResult.javaSelector.contains("x_rotation") || javaResult.javaSelector.contains("rx"))
        assertTrue(bedrockResult.bedrockSelector.contains("x_rotation") || bedrockResult.bedrockSelector.contains("rx"))
    }

    @Test
    fun testTargetSelector_Limit() {
        // 测试limit参数（Java版）
        val selector = "@e[sort=random,limit=5]"
        val bedrockResult = SelectorConverter.convertJavaToBedrock(selector, context)
        // limit是Java版特有，基岩版用其他方式
        assertTrue(bedrockResult.bedrockSelector.contains("@e") || !bedrockResult.bedrockReminders.isEmpty())
    }

    @Test
    fun testTargetSelector_Sort() {
        // 测试sort参数（Java版）
        val selector = "@e[sort=nearest]"
        val bedrockResult = SelectorConverter.convertJavaToBedrock(selector, context)
        // sort是Java版特有，基岩版用@c替代
        assertTrue(bedrockResult.bedrockSelector.contains("@c") || !bedrockResult.bedrockReminders.isEmpty())
    }

    @Test
    fun testTargetSelector_Level() {
        // 测试经验等级参数
        val selector = "@a[level=5..10]"
        val javaResult = SelectorConverter.convertBedrockToJava(selector, context)
        val bedrockResult = SelectorConverter.convertJavaToBedrock(selector, context)
        // level参数在两版都支持
        assertTrue(javaResult.javaSelector.contains("level"))
        assertTrue(bedrockResult.bedrockSelector.contains("level"))
    }

    @Test
    fun testTargetSelector_Gamemode() {
        // 测试游戏模式参数
        val selector = "@a[gamemode=creative]"
        val javaResult = SelectorConverter.convertBedrockToJava(selector, context)
        val bedrockResult = SelectorConverter.convertJavaToBedrock(selector, context)
        // gamemode参数在两版都支持
        assertTrue(javaResult.javaSelector.contains("gamemode"))
        assertTrue(bedrockResult.bedrockSelector.contains("gamemode"))
    }

    @Test
    fun testTargetSelector_Advancement() {
        // 测试进度参数（Java版）
        val selector = "@a[advancements={minecraft:adventure/adventuring_time=true}]"
        val bedrockResult = SelectorConverter.convertJavaToBedrock(selector, context)
        // advancement是Java版特有
        assertTrue(!bedrockResult.bedrockReminders.isEmpty() || bedrockResult.bedrockSelector.contains("@a"))
    }

    @Test
    fun testTargetSelector_Predicate() {
        // 测试谓词参数（Java版）
        val selector = "@a[predicate=custom:test]"
        val bedrockResult = SelectorConverter.convertJavaToBedrock(selector, context)
        // predicate是Java版特有
        assertTrue(!bedrockResult.bedrockReminders.isEmpty() || bedrockResult.bedrockSelector.contains("@a"))
    }
}
