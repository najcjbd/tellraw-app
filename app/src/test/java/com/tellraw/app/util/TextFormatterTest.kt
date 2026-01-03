package com.tellraw.app.util

import com.tellraw.app.model.MinecraftVersion
import org.junit.Test
import org.junit.Assert.*

/**
 * 测试TextFormatter的文本转换功能，特别是§m/§n的处理
 */
class TextFormatterTest {

    @Test
    fun testMNCodeHandlingFontMode() {
        println("\n${"=".repeat(80)}")
        println("测试§m/§n处理 - 字体模式（Java版使用格式化，基岩版使用颜色）")
        println("=".repeat(80))

        val testMessage = "§m删除线文本§n下划线文本"
        val mNHandling = "font"

        println("\n输入消息: $testMessage")
        println("处理模式: $mNHandling")

        // 测试Java版转换
        val javaJson = TextFormatter.convertToJavaJson(testMessage, mNHandling)
        println("\nJava版输出:")
        println(javaJson)

        // 验证Java版输出包含strikethrough和underlined
        assertTrue("Java版应该包含strikethrough", javaJson.contains("strikethrough"))
        assertTrue("Java版应该包含underlined", javaJson.contains("underlined"))

        // 测试基岩版转换
        val bedrockJson = TextFormatter.convertToBedrockJson(testMessage, mNHandling)
        println("\n基岩版输出:")
        println(bedrockJson)

        // 验证基岩版输出保留§m/§n
        assertTrue("基岩版应该保留§m", bedrockJson.contains("§m"))
        assertTrue("基岩版应该保留§n", bedrockJson.contains("§n"))
    }

    @Test
    fun testMNCodeHandlingColorMode() {
        println("\n${"=".repeat(80)}")
        println("测试§m/§n处理 - 颜色模式（Java版和基岩版都使用颜色）")
        println("=".repeat(80))

        val testMessage = "§m红色文本§n金色文本"
        val mNHandling = "color"

        println("\n输入消息: $testMessage")
        println("处理模式: $mNHandling")

        // 测试Java版转换
        val javaJson = TextFormatter.convertToJavaJson(testMessage, mNHandling)
        println("\nJava版输出:")
        println(javaJson)

        // 验证Java版输出包含dark_red和gold
        assertTrue("Java版应该包含dark_red", javaJson.contains("dark_red"))
        assertTrue("Java版应该包含gold", javaJson.contains("gold"))

        // 测试基岩版转换
        val bedrockJson = TextFormatter.convertToBedrockJson(testMessage, mNHandling)
        println("\n基岩版输出:")
        println(bedrockJson)

        // 验证基岩版输出转换为标准颜色代码
        assertTrue("基岩版应该包含§4（深红色）", bedrockJson.contains("§4"))
        assertTrue("基岩版应该包含§6（金色）", bedrockJson.contains("§6"))
    }

    @Test
    fun testMixedMNCodeHandling() {
        println("\n${"=".repeat(80)}")
        println("测试混合使用§m/§n - 字体模式")
        println("=".repeat(80))

        val testMessage = "§a绿色文本§m删除线§b青色文本§n下划线§c红色文本"
        val mNHandling = "font"

        println("\n输入消息: $testMessage")
        println("处理模式: $mNHandling")

        // 测试Java版转换
        val javaJson = TextFormatter.convertToJavaJson(testMessage, mNHandling)
        println("\nJava版输出:")
        println(javaJson)

        // 验证Java版输出
        assertTrue("Java版应该包含green", javaJson.contains("green"))
        assertTrue("Java版应该包含strikethrough", javaJson.contains("strikethrough"))
        assertTrue("Java版应该包含aqua", javaJson.contains("aqua"))
        assertTrue("Java版应该包含underlined", javaJson.contains("underlined"))
        assertTrue("Java版应该包含red", javaJson.contains("red"))

        // 测试基岩版转换
        val bedrockJson = TextFormatter.convertToBedrockJson(testMessage, mNHandling)
        println("\n基岩版输出:")
        println(bedrockJson)

        // 验证基岩版输出保留所有代码
        assertTrue("基岩版应该保留§a", bedrockJson.contains("§a"))
        assertTrue("基岩版应该保留§m", bedrockJson.contains("§m"))
        assertTrue("基岩版应该保留§b", bedrockJson.contains("§b"))
        assertTrue("基岩版应该保留§n", bedrockJson.contains("§n"))
        assertTrue("基岩版应该保留§c", bedrockJson.contains("§c"))
    }

    @Test
    fun testProcessMNCodes() {
        println("\n${"=".repeat(80)}")
        println("测试processMNCodes函数")
        println("=".repeat(80))

        val testMessage = "§m测试文本§n更多文本"

        // 测试字体模式
        val (processedFont, warningsFont) = TextFormatter.processMNCodes(testMessage, true)
        println("\n字体模式:")
        println("原始文本: $testMessage")
        println("处理后文本: $processedFont")
        println("警告信息: $warningsFont")
        assertEquals("字体模式不应该修改文本", testMessage, processedFont)
        assertTrue("字体模式应该有警告信息", warningsFont.isNotEmpty())

        // 测试颜色模式
        val (processedColor, warningsColor) = TextFormatter.processMNCodes(testMessage, false)
        println("\n颜色模式:")
        println("原始文本: $testMessage")
        println("处理后文本: $processedColor")
        println("警告信息: $warningsColor")
        assertEquals("颜色模式不应该修改文本", testMessage, processedColor)
        assertTrue("颜色模式应该有警告信息", warningsColor.isNotEmpty())
    }

    @Test
    fun testContainsMNCodes() {
        println("\n${"=".repeat(80)}")
        println("测试containsMNCodes函数")
        println("=".repeat(80))

        val testCases = listOf(
            "普通文本" to false,
            "§m包含§m" to true,
            "§n包含§n" to true,
            "§m§n都包含" to true,
            "§a§b§c其他代码" to false
        )

        testCases.forEach { (text, expected) ->
            val result = TextFormatter.containsMNCodes(text)
            println("文本: $text -> 包含§m/§n: $result (期望: $expected)")
            assertEquals("检测结果不正确", expected, result)
        }
    }

    @Test
    fun testConvertColorCodes() {
        println("\n${"=".repeat(80)}")
        println("测试convertColorCodes函数")
        println("=".repeat(80))

        // 测试基岩版特有颜色代码转换为Java版
        val bedrockText = "§g§h§i§j§m§n§p§q§s§t§u§v"
        val javaConverted = TextFormatter.convertColorCodes(bedrockText, MinecraftVersion.JAVA)
        println("基岩版文本: $bedrockText")
        println("Java版转换后: $javaConverted")
        
        // 验证转换
        assertFalse("不应该包含§g", javaConverted.contains("§g"))
        assertFalse("不应该包含§h", javaConverted.contains("§h"))
        assertFalse("不应该包含§i", javaConverted.contains("§i"))
        assertFalse("不应该包含§j", javaConverted.contains("§j"))
        assertFalse("不应该包含§p", javaConverted.contains("§p"))
        assertFalse("不应该包含§q", javaConverted.contains("§q"))
        assertFalse("不应该包含§s", javaConverted.contains("§s"))
        assertFalse("不应该包含§t", javaConverted.contains("§t"))
        assertFalse("不应该包含§u", javaConverted.contains("§u"))
        assertFalse("不应该包含§v", javaConverted.contains("§v"))

        // 测试Java版到基岩版（大部分颜色代码是通用的）
        val javaText = "§a§b§c§d§e§f"
        val bedrockConverted = TextFormatter.convertColorCodes(javaText, MinecraftVersion.BEDROCK)
        println("\nJava版文本: $javaText")
        println("基岩版转换后: $bedrockConverted")
        assertEquals("Java版到基岩版应该保持不变", javaText, bedrockConverted)
    }

    @Test
    fun testEscapeSectionSigns() {
        println("\n${"=".repeat(80)}")
        println("测试escapeSectionSigns函数")
        println("=".repeat(80))

        val testCases = listOf(
            "§a文本" to "\\u00A7a文本",
            "§m§n测试" to "\\u00A7m\\u00A7n测试",
            "普通文本" to "普通文本",
            "§" to "\\u00A7"
        )

        testCases.forEach { (input, expected) ->
            val result = TextFormatter.escapeSectionSigns(input)
            println("输入: $input -> 输出: $result (期望: $expected)")
            assertEquals("转义结果不正确", expected, result)
        }
    }

    @Test
    fun testUnescapeSectionSigns() {
        println("\n${"=".repeat(80)}")
        println("测试unescapeSectionSigns函数")
        println("=".repeat(80))

        val testCases = listOf(
            "\\u00A7a文本" to "§a文本",
            "\\u00A7m\\u00A7n测试" to "§m§n测试",
            "普通文本" to "普通文本",
            "\\u00A7" to "§"
        )

        testCases.forEach { (input, expected) ->
            val result = TextFormatter.unescapeSectionSigns(input)
            println("输入: $input -> 输出: $result (期望: $expected)")
            assertEquals("反转义结果不正确", expected, result)
        }
    }

    @Test
    fun testEscapeUnescapeRoundTrip() {
        println("\n${"=".repeat(80)}")
        println("测试转义和反转义的往返")
        println("=".repeat(80))

        val originalText = "§a§m§n§b测试文本§c"
        val escaped = TextFormatter.escapeSectionSigns(originalText)
        val unescaped = TextFormatter.unescapeSectionSigns(escaped)
        
        println("原始文本: $originalText")
        println("转义后: $escaped")
        println("反转义后: $unescaped")
        assertEquals("往返转换应该保持一致", originalText, unescaped)
    }

    @Test
    fun testConvertToBedrockRaw() {
        println("\n${"=".repeat(80)}")
        println("测试convertToBedrockRaw函数")
        println("=".repeat(80))

        val testCases = listOf(
            "§a绿色文本§m删除线§n下划线",
            "普通文本",
            "§r§l§o粗体斜体",
            ""
        )

        testCases.forEach { input ->
            val result = TextFormatter.convertToBedrockRaw(input)
            println("输入: $input -> 输出: $result")
            assertEquals("基岩版原始文本应该保持不变", input, result)
        }
    }

    @Test
    fun testGetColorName() {
        println("\n${"=".repeat(80)}")
        println("测试getColorName函数")
        println("=".repeat(80))

        val testCases = listOf(
            "§0" to "black",
            "§1" to "dark_blue",
            "§2" to "dark_green",
            "§3" to "dark_aqua",
            "§4" to "dark_red",
            "§5" to "dark_purple",
            "§6" to "gold",
            "§7" to "gray",
            "§8" to "dark_gray",
            "§9" to "blue",
            "§a" to "green",
            "§b" to "aqua",
            "§c" to "red",
            "§d" to "light_purple",
            "§e" to "yellow",
            "§f" to "white"
        )

        testCases.forEach { (code, expectedName) ->
            val result = TextFormatter.getColorName(code)
            println("颜色代码: $code -> 颜色名称: $result (期望: $expectedName)")
            assertEquals("颜色名称不正确", expectedName, result)
        }
    }

    @Test
    fun testGenerateTellrawCommand() {
        println("\n${"=".repeat(80)}")
        println("测试generateTellrawCommand函数")
        println("=".repeat(80))

        // 测试普通消息
        val selector1 = "@a"
        val message1 = "§a绿色文本"
        val command1 = TextFormatter.generateTellrawCommand(selector1, message1, true)
        println("\n测试1: 普通消息")
        println("选择器: $selector1")
        println("消息: $message1")
        println("Java版命令: ${command1.javaCommand}")
        println("基岩版命令: ${command1.bedrockCommand}")
        assertTrue("Java版命令应该以tellraw开头", command1.javaCommand.startsWith("tellraw"))
        assertTrue("基岩版命令应该以tellraw开头", command1.bedrockCommand.startsWith("tellraw"))
        assertTrue("Java版命令应该包含选择器", command1.javaCommand.contains(selector1))
        assertTrue("基岩版命令应该包含选择器", command1.bedrockCommand.contains(selector1))

        // 测试包含§m/§n的消息（字体模式）
        val selector2 = "@p"
        val message2 = "§m删除线§n下划线"
        val command2 = TextFormatter.generateTellrawCommand(selector2, message2, true)
        println("\n测试2: 包含§m/§n的消息（字体模式）")
        println("选择器: $selector2")
        println("消息: $message2")
        println("Java版命令: ${command2.javaCommand}")
        println("基岩版命令: ${command2.bedrockCommand}")
        assertTrue("Java版命令应该包含strikethrough", command2.javaCommand.contains("strikethrough"))
        assertTrue("Java版命令应该包含underlined", command2.javaCommand.contains("underlined"))
        assertTrue("基岩版命令应该保留§m", command2.bedrockCommand.contains("§m"))
        assertTrue("基岩版命令应该保留§n", command2.bedrockCommand.contains("§n"))

        // 测试包含§m/§n的消息（颜色模式）
        val command3 = TextFormatter.generateTellrawCommand(selector2, message2, false)
        println("\n测试3: 包含§m/§n的消息（颜色模式）")
        println("选择器: $selector2")
        println("消息: $message2")
        println("Java版命令: ${command3.javaCommand}")
        println("基岩版命令: ${command3.bedrockCommand}")
        assertTrue("Java版命令应该包含dark_red", command3.javaCommand.contains("dark_red"))
        assertTrue("Java版命令应该包含red", command3.javaCommand.contains("red"))
        assertTrue("基岩版命令应该包含§4", command3.bedrockCommand.contains("§4"))
        assertTrue("基岩版命令应该包含§c", command3.bedrockCommand.contains("§c"))
    }

    @Test
    fun testValidateTellrawCommand() {
        println("\n${"=".repeat(80)}")
        println("测试validateTellrawCommand函数")
        println("=".repeat(80))

        // 测试有效命令
        val validCommands = listOf(
            "tellraw @a {\"text\":\"Hello\"}",
            "tellraw @p {\"text\":\"Test\",\"color\":\"red\"}",
            "tellraw @e {\"text\":\"Extra\",\"extra\":[{\"text\":\"test\"}]}"
        )

        validCommands.forEach { command ->
            val errors = TextFormatter.validateTellrawCommand(command)
            println("\n有效命令: $command")
            println("错误列表: $errors")
            assertTrue("有效命令不应该有错误", errors.isEmpty())
        }

        // 测试无效命令
        val invalidCommands = listOf(
            "say @a {\"text\":\"Hello\"}" to listOf("命令必须以'tellraw '开头"),
            "tellraw invalid {\"text\":\"Hello\"}" to listOf("选择器必须以@开头"),
            "tellraw @a" to listOf("命令格式不正确，应为: tellraw <选择器> <消息>"),
            "tellraw @a {\"text\":\"Hello\"" to listOf("消息JSON格式无效")
        )

        invalidCommands.forEach { (command, expectedErrors) ->
            val errors = TextFormatter.validateTellrawCommand(command)
            println("\n无效命令: $command")
            println("错误列表: $errors")
            println("期望错误: $expectedErrors")
            assertTrue("无效命令应该有错误", errors.isNotEmpty())
            expectedErrors.forEach { expectedError ->
                assertTrue("应该包含错误: $expectedError", errors.any { it.contains(expectedError) })
            }
        }
    }

    @Test
    fun testComplexJsonConversion() {
        println("\n${"=".repeat(80)}")
        println("测试复杂JSON转换")
        println("=".repeat(80))

        val testCases = listOf(
            "§a§l粗体绿色" to "bold,green",
            "§c§n§m红色下划线删除线" to "strikethrough,underlined,red",
            "§0§k§o混淆斜体黑色" to "obfuscated,italic,black",
            "§r§e重置后金色" to "yellow",
            "§b§l§n青色粗体下划线§c红色" to "bold,underlined,aqua,red"
        )

        testCases.forEach { (input, expectedFeatures) ->
            val json = TextFormatter.convertToJavaJson(input, "font")
            println("\n输入: $input")
            println("输出: $json")
            
            // 验证期望的特性
            expectedFeatures.split(",").forEach { feature ->
                assertTrue("JSON应该包含$feature", json.contains(feature))
            }
        }
    }

    @Test
    fun testBedrockSpecificColorCodes() {
        println("\n${"=".repeat(80)}")
        println("测试基岩版特有颜色代码")
        println("=".repeat(80))

        val testCases = listOf(
            "§g" to "gold",  // minecoin_gold
            "§h" to "white",  // material_quartz
            "§i" to "gray",  // material_iron
            "§j" to "dark_gray",  // material_netherite
            "§p" to "gold",  // material_gold
            "§q" to "green",  // material_emerald
            "§s" to "aqua",  // material_diamond
            "§t" to "dark_blue",  // material_lapis
            "§u" to "light_purple",  // material_amethyst
            "§v" to "gold"  // material_resin
        )

        testCases.forEach { (code, expectedColor) ->
            val json = TextFormatter.convertToJavaJson(code + "测试文本", "color")
            println("代码: $code -> 颜色: $expectedColor")
            assertTrue("JSON应该包含$expectedColor", json.contains(expectedColor))
        }
    }

    @Test
    fun testEmptyAndSimpleText() {
        println("\n${"=".repeat(80)}")
        println("测试空文本和简单文本")
        println("=".repeat(80))

        // 测试空文本
        val emptyJson = TextFormatter.convertToJavaJson("", "color")
        println("空文本: -> $emptyJson")
        assertTrue("空文本应该返回包含空text的JSON", emptyJson.contains("\"text\":\"\""))

        // 测试纯文本（无格式代码）
        val simpleText = "纯文本消息"
        val simpleJson = TextFormatter.convertToJavaJson(simpleText, "color")
        println("纯文本: $simpleText -> $simpleJson")
        assertTrue("纯文本应该保持不变", simpleJson.contains(simpleText))
        assertTrue("纯文本JSON应该只包含text字段", simpleJson.contains("\"text\":\"$simpleText\""))
    }

    @Test
    fun testFormatCodes() {
        println("\n${"=".repeat(80)}")
        println("测试格式代码")
        println("=".repeat(80))

        val formatTests = listOf(
            "§k混淆文本" to "obfuscated",
            "§l粗体文本" to "bold",
            "§o斜体文本" to "italic",
            "§r重置文本" to "text"
        )

        formatTests.forEach { (input, expectedFormat) ->
            val json = TextFormatter.convertToJavaJson(input, "color")
            println("输入: $input -> 期望包含: $expectedFormat")
            if (expectedFormat != "text") {
                assertTrue("JSON应该包含$expectedFormat", json.contains(expectedFormat))
            }
        }
    }
}
