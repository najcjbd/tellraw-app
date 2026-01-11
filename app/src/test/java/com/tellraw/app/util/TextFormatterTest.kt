package com.tellraw.app.util

import org.junit.Test
import org.junit.Assert.*

/**
 * 文本格式化测试
 * 测试各种颜色代码和格式代码的转换
 */
class TextFormatterTest {
    
    /**
     * 测试组1：基础颜色代码测试（§0-§f）
     */
    @Test
    fun testBasicColorCodes() {
        val colorCodes = listOf(
            "§0" to "黑色",
            "§1" to "深蓝",
            "§2" to "深绿",
            "§3" to "深青",
            "§4" to "深红",
            "§5" to "深紫",
            "§6" to "金色",
            "§7" to "灰色",
            "§8" to "深灰",
            "§9" to "蓝色",
            "§a" to "绿色",
            "§b" to "青色",
            "§c" to "红色",
            "§d" to "粉色",
            "§e" to "黄色",
            "§f" to "白色"
        )
        
        for ((code, name) in colorCodes) {
            val message = "$code测试文字"
            val javaJson = TextFormatter.convertToJavaJson(message, "none", false)
            val bedrockJson = TextFormatter.convertToBedrockJson(message, "none", false)
            
            assertNotNull("Java版JSON不应为null: $name", javaJson)
            assertNotNull("基岩版JSON不应为null: $name", bedrockJson)
            assertTrue("Java版JSON应包含color: $name", javaJson.contains("color"))
            assertTrue("基岩版JSON应包含color: $name", bedrockJson.contains("color"))
        }
    }
    
    /**
     * 测试组2：基础格式代码测试
     */
    @Test
    fun testBasicFormatCodes() {
        val formatCodes = listOf(
            "§l" to "粗体",
            "§o" to "斜体",
            "§k" to "混乱",
            "§r" to "重置"
        )
        
        for ((code, name) in formatCodes) {
            val message = "$code测试文字"
            val javaJson = TextFormatter.convertToJavaJson(message, "none", false)
            val bedrockJson = TextFormatter.convertToBedrockJson(message, "none", false)
            
            assertNotNull("Java版JSON不应为null: $name", javaJson)
            assertNotNull("基岩版JSON不应为null: $name", bedrockJson)
        }
    }
    
    /**
     * 测试组3：§m删除线代码测试
     */
    @Test
    fun testStrikethroughCode() {
        val message = "§m删除线文字"
        
        // Java版字体方式
        val javaJsonFont = TextFormatter.convertToJavaJson(message, "font", false)
        assertNotNull("Java版字体方式JSON不应为null", javaJsonFont)
        assertTrue("Java版字体方式应包含strikethrough", javaJsonFont.contains("strikethrough"))
        
        // Java版颜色代码方式
        val javaJsonColor = TextFormatter.convertToJavaJson(message, "color", false)
        assertNotNull("Java版颜色代码方式JSON不应为null", javaJsonColor)
        
        // 基岩版JSON
        val bedrockJson = TextFormatter.convertToBedrockJson(message, "none", false)
        assertNotNull("基岩版JSON不应为null", bedrockJson)
    }
    
    /**
     * 测试组4：§n下划线代码测试
     */
    @Test
    fun testUnderlineCode() {
        val message = "§n下划线文字"
        
        // Java版字体方式
        val javaJsonFont = TextFormatter.convertToJavaJson(message, "font", false)
        assertNotNull("Java版字体方式JSON不应为null", javaJsonFont)
        assertTrue("Java版字体方式应包含underlined", javaJsonFont.contains("underlined"))
        
        // Java版颜色代码方式
        val javaJsonColor = TextFormatter.convertToJavaJson(message, "color", false)
        assertNotNull("Java版颜色代码方式JSON不应为null", javaJsonColor)
        
        // 基岩版JSON
        val bedrockJson = TextFormatter.convertToBedrockJson(message, "none", false)
        assertNotNull("基岩版JSON不应为null", bedrockJson)
    }
    
    /**
     * 测试组5：颜色代码组合测试
     */
    @Test
    fun testColorCodeCombinations() {
        val testCases = listOf(
            "§c红色§a绿色§b蓝色",
            "§e黄色§d粉色§f白色",
            "§0黑色§1深蓝§2深绿§3深青",
            "§4深红§5深紫§6金色§7灰色",
            "§8深灰§9蓝色§a绿色§b青色",
            "§c红色§d粉色§e黄色§f白色"
        )
        
        for (message in testCases) {
            val javaJson = TextFormatter.convertToJavaJson(message, "none", false)
            val bedrockJson = TextFormatter.convertToBedrockJson(message, "none", false)
            
            assertNotNull("Java版JSON不应为null", javaJson)
            assertNotNull("基岩版JSON不应为null", bedrockJson)
        }
    }
    
    /**
     * 测试组6：格式代码组合测试
     */
    @Test
    fun testFormatCodeCombinations() {
        val testCases = listOf(
            "§l粗体§o斜体",
            "§k混乱§r重置",
            "§l粗体§o斜体§n下划线",
            "§k混乱§l粗体§o斜体",
            "§l粗体文字§r普通文字",
            "§o斜体§n下划线§r重置"
        )
        
        for (message in testCases) {
            val javaJson = TextFormatter.convertToJavaJson(message, "none", false)
            val bedrockJson = TextFormatter.convertToBedrockJson(message, "none", false)
            
            assertNotNull("Java版JSON不应为null", javaJson)
            assertNotNull("基岩版JSON不应为null", bedrockJson)
        }
    }
    
    /**
     * 测试组7：颜色和格式代码混合测试
     */
    @Test
    fun testColorAndFormatMix() {
        val testCases = listOf(
            "§c§l红色粗体",
            "§a§o绿色斜体",
            "§b§n青色下划线",
            "§e§k黄色混乱",
            "§f§l白色粗体§r§c红色",
            "§d§o粉色斜体§n下划线",
            "§0§l黑色粗体§a绿色§b青色"
        )
        
        for (message in testCases) {
            val javaJson = TextFormatter.convertToJavaJson(message, "none", false)
            val bedrockJson = TextFormatter.convertToBedrockJson(message, "none", false)
            
            assertNotNull("Java版JSON不应为null", javaJson)
            assertNotNull("基岩版JSON不应为null", bedrockJson)
        }
    }
    
    /**
     * 测试组8：§m代码在不同模式下的测试
     */
    @Test
    fun testStrikethroughModes() {
        val message = "§m删除线文字"
        
        // 字体模式
        val javaJsonFont = TextFormatter.convertToJavaJson(message, "font", false)
        assertNotNull("字体模式Java版JSON不应为null", javaJsonFont)
        
        // 颜色代码模式
        val javaJsonColor = TextFormatter.convertToJavaJson(message, "color", false)
        assertNotNull("颜色代码模式Java版JSON不应为null", javaJsonColor)
        
        // 混合模式（启用_c/_f后缀）
        val messageMixed = "§m_f删除线文字§m_c删除线文字"
        val javaJsonMixed = TextFormatter.convertToJavaJson(messageMixed, "font", true)
        assertNotNull("混合模式Java版JSON不应为null", javaJsonMixed)
    }
    
    /**
     * 测试组9：§n代码在不同模式下的测试
     */
    @Test
    fun testUnderlineModes() {
        val message = "§n下划线文字"
        
        // 字体模式
        val javaJsonFont = TextFormatter.convertToJavaJson(message, "font", false)
        assertNotNull("字体模式Java版JSON不应为null", javaJsonFont)
        
        // 颜色代码模式
        val javaJsonColor = TextFormatter.convertToJavaJson(message, "color", false)
        assertNotNull("颜色代码模式Java版JSON不应为null", javaJsonColor)
        
        // 混合模式（启用_c/_f后缀）
        val messageMixed = "§n_f下划线文字§n_c下划线文字"
        val javaJsonMixed = TextFormatter.convertToJavaJson(messageMixed, "font", true)
        assertNotNull("混合模式Java版JSON不应为null", javaJsonMixed)
    }
    
    /**
     * 测试组10：§m和§n组合测试
     */
    @Test
    fun testStrikethroughAndUnderline() {
        val testCases = listOf(
            "§m删除线§n下划线",
            "§n下划线§m删除线",
            "§l§m粗体删除线",
            "§l§n粗体下划线",
            "§m§n删除线下划线",
            "§n§m下划线删除线",
            "§c§m红色删除线§a§n绿色下划线"
        )
        
        for (message in testCases) {
            val javaJsonFont = TextFormatter.convertToJavaJson(message, "font", false)
            val javaJsonColor = TextFormatter.convertToJavaJson(message, "color", false)
            val bedrockJson = TextFormatter.convertToBedrockJson(message, "none", false)
            
            assertNotNull("Java版字体方式JSON不应为null", javaJsonFont)
            assertNotNull("Java版颜色代码方式JSON不应为null", javaJsonColor)
            assertNotNull("基岩版JSON不应为null", bedrockJson)
        }
    }
    
    /**
     * 测试组11：§r重置代码测试
     */
    @Test
    fun testResetCode() {
        val testCases = listOf(
            "§c红色§r普通",
            "§l粗体§r普通",
            "§c§l红色粗体§r普通",
            "§k混乱§r普通",
            "§n下划线§r普通",
            "§m删除线§r普通",
            "§c§l§n红色粗体下划线§r普通"
        )
        
        for (message in testCases) {
            val javaJson = TextFormatter.convertToJavaJson(message, "none", false)
            val bedrockJson = TextFormatter.convertToBedrockJson(message, "none", false)
            
            assertNotNull("Java版JSON不应为null", javaJson)
            assertNotNull("基岩版JSON不应为null", bedrockJson)
        }
    }
    
    /**
     * 测试组12：复杂文本测试
     */
    @Test
    fun testComplexText() {
        val testCases = listOf(
            "欢迎来到服务器！§c§l请遵守规则",
            "§a玩家§f: §b§lHello World!",
            "§c警告§r: §e请勿使用非法外挂",
            "§m§n删除线下划线§r普通文字",
            "§k§l§o混乱粗体斜体§r普通",
            "§0黑色§1深蓝§2深绿§3深青§4深红§5深紫§6金色§7灰色",
            "§8深灰§9蓝色§a绿色§b青色§c红色§d粉色§e黄色§f白色",
            "§l粗体§o斜体§n下划线§m删除线§k混乱§r重置"
        )
        
        for (message in testCases) {
            val javaJsonFont = TextFormatter.convertToJavaJson(message, "font", false)
            val javaJsonColor = TextFormatter.convertToJavaJson(message, "color", false)
            val bedrockJson = TextFormatter.convertToBedrockJson(message, "none", false)
            
            assertNotNull("Java版字体方式JSON不应为null", javaJsonFont)
            assertNotNull("Java版颜色代码方式JSON不应为null", javaJsonColor)
            assertNotNull("基岩版JSON不应为null", bedrockJson)
        }
    }
    
    /**
     * 测试组13：空文本和纯文本测试
     */
    @Test
    fun testEmptyAndPlainText() {
        val testCases = listOf(
            "",
            "普通文本",
            "Hello World",
            "测试中文",
            "Test 123",
            "!@#$%^&*()"
        )
        
        for (message in testCases) {
            val javaJson = TextFormatter.convertToJavaJson(message, "none", false)
            val bedrockJson = TextFormatter.convertToBedrockJson(message, "none", false)
            
            assertNotNull("Java版JSON不应为null", javaJson)
            assertNotNull("基岩版JSON不应为null", bedrockJson)
        }
    }
    
    /**
     * 测试组14：连续相同的格式代码测试
     */
    @Test
    fun testRepeatedFormatCodes() {
        val testCases = listOf(
            "§l§l粗体",
            "§o§o斜体",
            "§n§n下划线",
            "§m§m删除线",
            "§k§k混乱",
            "§r§r重置",
            "§c§c红色"
        )
        
        for (message in testCases) {
            val javaJson = TextFormatter.convertToJavaJson(message, "none", false)
            val bedrockJson = TextFormatter.convertToBedrockJson(message, "none", false)
            
            assertNotNull("Java版JSON不应为null", javaJson)
            assertNotNull("基岩版JSON不应为null", bedrockJson)
        }
    }
    
    /**
     * 测试组15：基岩版独有颜色代码测试
     */
    @Test
    fun testBedrockExclusiveColorCodes() {
        val bedrockColors = listOf(
            "§g", "§h", "§i", "§j", "§m", "§n", "§p", "§q", "§s", "§t", "§u", "§v"
        )
        
        for (code in bedrockColors) {
            val message = "$code基岩版颜色"
            val javaJson = TextFormatter.convertToJavaJson(message, "none", false)
            val bedrockJson = TextFormatter.convertToBedrockJson(message, "none", false)
            
            assertNotNull("Java版JSON不应为null", javaJson)
            assertNotNull("基岩版JSON不应为null", bedrockJson)
        }
    }
    
    /**
     * 测试组16：特殊字符测试
     */
    @Test
    fun testSpecialCharacters() {
        val testCases = listOf(
            "§c测试@#$%^&*()",
            "§a测试<>{}[]",
            "§b测试\\|/?:;'\"",
            "§e测试~`_-+=",
            "§f测试，。！？",
            "§d测试【】《》"
        )
        
        for (message in testCases) {
            val javaJson = TextFormatter.convertToJavaJson(message, "none", false)
            val bedrockJson = TextFormatter.convertToBedrockJson(message, "none", false)
            
            assertNotNull("Java版JSON不应为null", javaJson)
            assertNotNull("基岩版JSON不应为null", bedrockJson)
        }
    }
    
    /**
     * 测试组17：长文本测试
     */
    @Test
    fun testLongText() {
        val message = "§c§l这是一个很长的文本测试，用于测试格式化器在处理长文本时的表现。§r§a普通文本继续§b§l然后又是粗体蓝色文字§r§f最后回到普通文字。"
        
        val javaJson = TextFormatter.convertToJavaJson(message, "none", false)
        val bedrockJson = TextFormatter.convertToBedrockJson(message, "none", false)
        
        assertNotNull("Java版JSON不应为null", javaJson)
        assertNotNull("基岩版JSON不应为null", bedrockJson)
    }
    
    /**
     * 测试组18：混合模式_c/_f后缀测试
     */
    @Test
    fun testMixedModeSuffix() {
        val testCases = listOf(
            "§m_f字体删除线§m_c颜色删除线",
            "§n_f字体下划线§n_c颜色下划线",
            "§m_f§n_f字体删除线下划线§m_c§n_c颜色删除线下划线",
            "§l§m_f粗体字体删除线§r§a§n_c绿色颜色下划线",
            "§c§m_f红色字体删除线§b§n_c蓝色颜色下划线"
        )
        
        for (message in testCases) {
            val javaJson = TextFormatter.convertToJavaJson(message, "font", true)
            val bedrockJson = TextFormatter.convertToBedrockJson(message, "none", true)
            
            assertNotNull("Java版JSON不应为null", javaJson)
            assertNotNull("基岩版JSON不应为null", bedrockJson)
        }
    }
    
    /**
     * 测试组19：边界情况测试
     */
    @Test
    fun testEdgeCases() {
        val testCases = listOf(
            "§", // 单独的§符号
            "§x", // 无效的代码
            "§§", // 两个§符号
            "§c§", // 颜色代码后跟§
            "§l§m§n§k§o", // 连续多个格式代码
            "§r§r§r", // 连续多个重置代码
            "§c§l§n§m§k§o§r", // 所有代码组合
            "§0§1§2§3§4§5§6§7§8§9§a§b§c§d§e§f" // 所有颜色代码
        )
        
        for (message in testCases) {
            val javaJson = TextFormatter.convertToJavaJson(message, "none", false)
            val bedrockJson = TextFormatter.convertToBedrockJson(message, "none", false)
            
            assertNotNull("Java版JSON不应为null", javaJson)
            assertNotNull("基岩版JSON不应为null", bedrockJson)
        }
    }
    
    /**
     * 测试组20：实际使用场景测试
     */
    @Test
    fun testRealWorldScenarios() {
        val testCases = listOf(
            "§c§l[警告] §f检测到非法物品",
            "§a§l[系统] §f欢迎§e§l玩家§f加入服务器",
            "§b[公告] §f服务器将在§c§l5分钟§f后重启",
            "§d§l[VIP] §f玩家§a§lSteve§f发送了消息",
            "§e§l[奖励] §f恭喜获得§d§l传奇物品",
            "§c§l[错误] §f命令执行失败，请检查语法",
            "§a§l[成功] §f操作完成",
            "§b§l[信息] §f查看帮助请输入§e/help"
        )
        
        for (message in testCases) {
            val javaJson = TextFormatter.convertToJavaJson(message, "none", false)
            val bedrockJson = TextFormatter.convertToBedrockJson(message, "none", false)
            
            assertNotNull("Java版JSON不应为null", javaJson)
            assertNotNull("基岩版JSON不应为null", bedrockJson)
        }
    }
    
    /**
     * 测试组21：所有颜色代码单独测试
     */
    @Test
    fun testAllColorCodesIndividually() {
        val colorCodes = listOf(
            "§0", "§1", "§2", "§3", "§4", "§5", "§6", "§7",
            "§8", "§9", "§a", "§b", "§c", "§d", "§e", "§f"
        )
        
        for (code in colorCodes) {
            val message = "$code测试"
            val javaJson = TextFormatter.convertToJavaJson(message, "none", false)
            val bedrockJson = TextFormatter.convertToBedrockJson(message, "none", false)
            
            assertNotNull("Java版JSON不应为null: $code", javaJson)
            assertNotNull("基岩版JSON不应为null: $code", bedrockJson)
            assertTrue("Java版JSON应包含text: $code", javaJson.contains("text"))
            assertTrue("基岩版JSON应包含text: $code", bedrockJson.contains("text"))
        }
    }
    
    /**
     * 测试组22：所有格式代码单独测试
     */
    @Test
    fun testAllFormatCodesIndividually() {
        val formatCodes = listOf("§k", "§l", "§m", "§n", "§o", "§r")
        
        for (code in formatCodes) {
            val message = "$code测试"
            val javaJson = TextFormatter.convertToJavaJson(message, "none", false)
            val bedrockJson = TextFormatter.convertToBedrockJson(message, "none", false)
            
            assertNotNull("Java版JSON不应为null: $code", javaJson)
            assertNotNull("基岩版JSON不应为null: $code", bedrockJson)
        }
    }
    
    /**
     * 测试组23：§m代码的所有组合
     */
    @Test
    fun testStrikethroughAllCombinations() {
        val testCases = listOf(
            "§m文字",
            "§c§m红色删除线",
            "§l§m粗体删除线",
            "§o§m斜体删除线",
            "§k§m混乱删除线",
            "§n§m下划线删除线",
            "§c§l§m红色粗体删除线",
            "§m§r删除线重置"
        )
        
        for (message in testCases) {
            val javaJsonFont = TextFormatter.convertToJavaJson(message, "font", false)
            val javaJsonColor = TextFormatter.convertToJavaJson(message, "color", false)
            
            assertNotNull("Java版字体方式JSON不应为null: $message", javaJsonFont)
            assertNotNull("Java版颜色代码方式JSON不应为null: $message", javaJsonColor)
        }
    }
    
    /**
     * 测试组24：§n代码的所有组合
     */
    @Test
    fun testUnderlineAllCombinations() {
        val testCases = listOf(
            "§n文字",
            "§c§n红色下划线",
            "§l§n粗体下划线",
            "§o§n斜体下划线",
            "§k§n混乱下划线",
            "§m§n删除线下划线",
            "§c§l§n红色粗体下划线",
            "§n§r下划线重置"
        )
        
        for (message in testCases) {
            val javaJsonFont = TextFormatter.convertToJavaJson(message, "font", false)
            val javaJsonColor = TextFormatter.convertToJavaJson(message, "color", false)
            
            assertNotNull("Java版字体方式JSON不应为null: $message", javaJsonFont)
            assertNotNull("Java版颜色代码方式JSON不应为null: $message", javaJsonColor)
        }
    }
    
    /**
     * 测试组25：混合模式_c/_f后缀的所有组合
     */
    @Test
    fun testMixedModeSuffixAllCombinations() {
        val testCases = listOf(
            "§m_f字体删除线",
            "§m_c颜色删除线",
            "§n_f字体下划线",
            "§n_c颜色下划线",
            "§m_f§n_f字体删除线下划线",
            "§m_c§n_c颜色删除线下划线",
            "§l§m_f粗体字体删除线",
            "§o§n_c斜体颜色下划线",
            "§c§m_f红色字体删除线§b§n_c蓝色颜色下划线",
            "§m_f文字§m_c文字§n_f文字§n_c文字"
        )
        
        for (message in testCases) {
            val javaJson = TextFormatter.convertToJavaJson(message, "font", true)
            val bedrockJson = TextFormatter.convertToBedrockJson(message, "none", true)
            
            assertNotNull("Java版JSON不应为null: $message", javaJson)
            assertNotNull("基岩版JSON不应为null: $message", bedrockJson)
        }
    }
}