package com.tellraw.app.util

import org.junit.Test
import org.junit.Assert.*

/**
 * 混合模式测试
 * 测试§m_f/§m_c/§n_f/§n_c在混合模式下的转换
 */
class MixedModeTest {

    /**
     * 测试混合模式下Java版转换为strikethrough（字体方式）
     */
    @Test
    fun testMixedModeJavaFontStrikethrough() {
        // 混合模式：mnCFEnabled=false, mNHandling="font"
        val message = "§m_fqq§m_cpp"
        val javaJson = TextFormatter.convertToJavaJson(message, "font", false)

        // 验证Java版输出包含strikethrough
        assertTrue("Java版应包含strikethrough", javaJson.contains("strikethrough"))
        assertTrue("Java版应包含dark_red颜色", javaJson.contains("dark_red"))
    }

    /**
     * 测试混合模式下Java版转换为underlined（字体方式）
     */
    @Test
    fun testMixedModeJavaFontUnderline() {
        // 混合模式：mnCFEnabled=false, mNHandling="font"
        val message = "§n_fzz§n_cll"
        val javaJson = TextFormatter.convertToJavaJson(message, "font", false)

        // 验证Java版输出包含underlined
        assertTrue("Java版应包含underlined", javaJson.contains("underlined"))
        assertTrue("Java版应包含red颜色", javaJson.contains("red"))
    }

    /**
     * 测试混合模式下基岩版转换为颜色代码
     */
    @Test
    fun testMixedModeBedrockColor() {
        // 混合模式：mnCFEnabled=false, mNHandling="font"
        val message = "§m_fqq§m_cpp§n_fzz§n_cll"
        val bedrockJson = TextFormatter.convertToBedrockJson(message, "font", false)

        // 验证基岩版输出包含rawtext
        assertTrue("基岩版应包含rawtext", bedrockJson.contains("rawtext"))
        // 验证转换为§m/§n（颜色代码）
        val rawtextContent = bedrockJson.substringAfter("text\":").substringBefore("}")
        assertTrue("基岩版应包含§m颜色代码", rawtextContent.contains("§m"))
        assertTrue("基岩版应包含§n颜色代码", rawtextContent.contains("§n"))
    }

    /**
     * 测试混合模式下完整的转换逻辑
     * 示例：§m(方式一)qq§m(方式二)pp§n(方式一)zz§n(方式二)ll
     * 后台存储：§m_fqq§m_cpp§n_fzz§n_cll
     * Java输出：{"text":"_fqq_cpp","strikethrough":true,"extra":[{"text":"_fzz","strikethrough":true,"underlined":true}]}
     * 基岩版输出：{"rawtext":[{"text":"§m_fqq§m_cpp§n_fzz§n_cll"}]}
     */
    @Test
    fun testMixedModeCompleteExample() {
        // 后台存储的格式
        val backendMessage = "§m_fqq§m_cpp§n_fzz§n_cll"

        // Java版输出（混合模式）
        val javaJson = TextFormatter.convertToJavaJson(backendMessage, "font", false)

        // 验证Java版输出
        assertNotNull("Java版JSON不应为null", javaJson)
        assertTrue("Java版应包含strikethrough", javaJson.contains("strikethrough"))
        assertTrue("Java版应包含underlined", javaJson.contains("underlined"))

        // 基岩版输出（混合模式）
        val bedrockJson = TextFormatter.convertToBedrockJson(backendMessage, "font", false)

        // 验证基岩版输出
        assertNotNull("基岩版JSON不应为null", bedrockJson)
        assertTrue("基岩版应包含rawtext", bedrockJson.contains("rawtext"))
    }

    /**
     * 测试混合模式下§m_f转换为strikethrough
     */
    @Test
    fun testMixedModeMFontToStrikethrough() {
        val message = "§m_f测试文字"
        val javaJson = TextFormatter.convertToJavaJson(message, "font", false)

        assertTrue("§m_f应转换为strikethrough", javaJson.contains("strikethrough"))
    }

    /**
     * 测试混合模式下§m_c转换为dark_red颜色
     */
    @Test
    fun testMixedModeMColorToDarkRed() {
        val message = "§m_c测试文字"
        val javaJson = TextFormatter.convertToJavaJson(message, "font", false)

        assertTrue("§m_c应转换为dark_red颜色", javaJson.contains("dark_red"))
        assertFalse("§m_c不应包含strikethrough", javaJson.contains("strikethrough"))
    }

    /**
     * 测试混合模式下§n_f转换为underlined
     */
    @Test
    fun testMixedModeNFontToUnderlined() {
        val message = "§n_f测试文字"
        val javaJson = TextFormatter.convertToJavaJson(message, "font", false)

        assertTrue("§n_f应转换为underlined", javaJson.contains("underlined"))
    }

    /**
     * 测试混合模式下§n_c转换为red颜色
     */
    @Test
    fun testMixedModeNColorToRed() {
        val message = "§n_c测试文字"
        val javaJson = TextFormatter.convertToJavaJson(message, "font", false)

        assertTrue("§n_c应转换为red颜色", javaJson.contains("red"))
        assertFalse("§n_c不应包含underlined", javaJson.contains("underlined"))
    }

    /**
     * 测试混合模式下基岩版将§m_f/§m_c统一转换为§m
     */
    @Test
    fun testMixedModeBedrockMConversion() {
        val message = "§m_f字体删除线§m_c颜色删除线"
        val bedrockJson = TextFormatter.convertToBedrockJson(message, "font", false)

        val rawtextContent = bedrockJson.substringAfter("text\":").substringBefore("}")
        // §m_f和§m_c都应该转换为§m
        assertFalse("基岩版不应包含§m_f", rawtextContent.contains("§m_f"))
        assertFalse("基岩版不应包含§m_c", rawtextContent.contains("§m_c"))
        assertTrue("基岩版应包含§m", rawtextContent.contains("§m"))
    }

    /**
     * 测试混合模式下基岩版将§n_f/§n_c统一转换为§n
     */
    @Test
    fun testMixedModeBedrockNConversion() {
        val message = "§n_f字体下划线§n_c颜色下划线"
        val bedrockJson = TextFormatter.convertToBedrockJson(message, "font", false)

        val rawtextContent = bedrockJson.substringAfter("text\":").substringBefore("}")
        // §n_f和§n_c都应该转换为§n
        assertFalse("基岩版不应包含§n_f", rawtextContent.contains("§n_f"))
        assertFalse("基岩版不应包含§n_c", rawtextContent.contains("§n_c"))
        assertTrue("基岩版应包含§n", rawtextContent.contains("§n"))
    }

    /**
     * 测试混合模式下保留普通的§m/§n（不带_f/_c后缀）
     * 注意：在混合模式下（mnCFEnabled=false），普通的§m/§n应该被保留，因为它们是有效的基岩版颜色代码
     */
    @Test
    fun testMixedModeKeepPlainMN() {
        val message = "§m普通删除线§n普通下划线"
        val bedrockJson = TextFormatter.convertToBedrockJson(message, "font", false)

        val rawtextContent = bedrockJson.substringAfter("text\":").substringBefore("}")
        // 混合模式下，普通的§m/§n应该被保留（它们是有效的基岩版颜色代码）
        assertTrue("混合模式下基岩版应保留§m", rawtextContent.contains("§m"))
        assertTrue("混合模式下基岩版应保留§n", rawtextContent.contains("§n"))
    }

    /**
     * 测试§m/§n_c/f模式下移除普通的§m/§n（不带_f/_c后缀）
     */
    @Test
    fun testMNCFModeRemovePlainMN() {
        val message = "§m普通删除线§n普通下划线§m_f字体删除线§m_c颜色删除线"
        val bedrockJson = TextFormatter.convertToBedrockJson(message, "font", true)

        val rawtextContent = bedrockJson.substringAfter("text\":").substringBefore("}")
        // §m/§n_c/f模式下，普通的§m/§n应该被移除
        assertFalse("§m/§n_c/f模式下基岩版不应包含独立的§m", rawtextContent.matches(Regex("(?<!§m)[_]m(?![_cn])")))
        assertFalse("§m/§n_c/f模式下基岩版不应包含独立的§n", rawtextContent.matches(Regex("(?<!§n)[_]n(?![_cn])")))
        // 但应该保留§m_f和§m_c（转换为§m）
        assertTrue("应包含转换后的§m", rawtextContent.contains("§m"))
    }

    /**
     * 测试混合模式下普通§m/§n在Java版中的处理
     */
    @Test
    fun testMixedModeJavaPlainMN() {
        val message = "§m普通删除线§n普通下划线"
        val javaJson = TextFormatter.convertToJavaJson(message, "font", false)

        // 在混合模式下，Java版应该将普通§m处理为strikethrough
        assertTrue("Java版应包含strikethrough", javaJson.contains("strikethrough"))
        // 在混合模式下，Java版应该将普通§n处理为underlined
        assertTrue("Java版应包含underlined", javaJson.contains("underlined"))
    }

    /**
     * 测试混合模式下颜色代码组合
     */
    @Test
    fun testMixedModeColorCombination() {
        val message = "§a§m_f绿色字体删除线§r§c§n_c红色颜色下划线"
        val javaJson = TextFormatter.convertToJavaJson(message, "font", true)

        // 验证颜色代码和格式代码正确组合
        assertTrue("应包含green颜色", javaJson.contains("green"))
        assertTrue("应包含red颜色", javaJson.contains("red"))
        assertTrue("应包含strikethrough", javaJson.contains("strikethrough"))
        assertTrue("应包含underlined", javaJson.contains("underlined"))
    }

    /**
     * 测试混合模式下空文本处理
     */
    @Test
    fun testMixedModeEmptyText() {
        val message = ""
        val javaJson = TextFormatter.convertToJavaJson(message, "font", false)
        val bedrockJson = TextFormatter.convertToBedrockJson(message, "font", false)

        // 空文本应该仍然生成有效的JSON
        assertNotNull("Java版JSON不应为null", javaJson)
        assertNotNull("基岩版JSON不应为null", bedrockJson)
        assertTrue("Java版JSON应包含text字段", javaJson.contains("\"text\""))
        assertTrue("基岩版JSON应包含rawtext", bedrockJson.contains("rawtext"))
    }

    /**
     * 测试混合模式下只有§m_f/§m_c/§n_f/§n_c代码
     */
    @Test
    fun testMixedModeOnlyMNCodes() {
        val message = "§m_f§m_c§n_f§n_c"
        val javaJson = TextFormatter.convertToJavaJson(message, "font", true)
        val bedrockJson = TextFormatter.convertToBedrockJson(message, "font", true)

        // Java版应该包含格式代码
        assertTrue("Java版应包含strikethrough", javaJson.contains("strikethrough"))
        assertTrue("Java版应包含underlined", javaJson.contains("underlined"))

        // 基岩版应该转换为颜色代码
        val rawtextContent = bedrockJson.substringAfter("text\":").substringBefore("}")
        assertTrue("基岩版应包含§m", rawtextContent.contains("§m"))
        assertTrue("基岩版应包含§n", rawtextContent.contains("§n"))
    }

    /**
     * 测试混合模式下混合使用普通代码和_c/_f后缀代码
     */
    @Test
    fun testMixedModeMixedCodes() {
        val message = "§m普通删除线§m_f字体删除线§m_c颜色删除线§n普通下划线§n_f字体下划线§n_c颜色下划线"
        val javaJson = TextFormatter.convertToJavaJson(message, "font", false)

        // Java版应该包含所有相关的格式代码和颜色
        assertTrue("Java版应包含strikethrough", javaJson.contains("strikethrough"))
        assertTrue("Java版应包含underlined", javaJson.contains("underlined"))
        assertTrue("Java版应包含dark_red颜色", javaJson.contains("dark_red"))
        assertTrue("Java版应包含red颜色", javaJson.contains("red"))
    }
}