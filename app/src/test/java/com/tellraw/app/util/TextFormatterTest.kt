package com.tellraw.app.util

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
}
