package com.tellraw.app.util

import org.junit.Test
import org.junit.Assert.*

/**
 * 测试字符标记
 */
class MarkerTest {

    @Test
    fun testMarkerCharacters() {
        val markerStart = '\u0FC8'
        val markerEnd = '\u0F34'
        val testString = "hello\u0FC8text\u0FC9"

        println("========================================")
        println("测试字符标记:")
        println("MARKER_START: '$markerStart' (\\u0FC8)")
        println("MARKER_END: '$markerEnd' (\\u0F34)")
        println("测试字符串: '$testString'")
        println("测试字符串长度: ${testString.length}")

        // 打印每个字符
        for (i in testString.indices) {
            val char = testString[i]
            val isMarkerStart = char == markerStart
            val isMarkerEnd = char == markerEnd
            println("字符 $i: '$char' (\\u${char.code.toString(16).padStart(4, '0')}) == MARKER_START? $isMarkerStart, == MARKER_END? $isMarkerEnd")
        }

        // 查找MARKER_START
        val firstMarkerStart = testString.indexOf(markerStart)
        println("第一个MARKER_START位置: $firstMarkerStart")

        // 查找MARKER_END
        val firstMarkerEnd = testString.indexOf(markerEnd)
        println("第一个MARKER_END位置: $firstMarkerEnd")

        println("========================================")

        assertTrue("应该包含MARKER_START", testString.contains(markerStart))
        assertTrue("应该包含MARKER_END", testString.contains(markerEnd))
        assertEquals("MARKER_START应该在位置5", 5, firstMarkerStart)
        assertEquals("MARKER_END应该在位置10", 10, firstMarkerEnd)
    }

    @Test
    fun testParseTextComponents() {
        val message = "hello\u0FC8text\u0FC9"

        println("========================================")
        println("测试parseTextComponents:")
        println("输入: '$message'")
        println("长度: ${message.length}")

        val components = TextComponentHelper.parseTextComponents(message)

        println("解析得到的组件数量: ${components.size}")
        for ((index, component) in components.withIndex()) {
            println("组件 $index: type=${component.type}, content='${component.content}', length=${component.content.length}")
            println("  content的每个字符:")
            for (i in component.content.indices) {
                val char = component.content[i]
                println("    字符 $i: '$char' (\\u${char.code.toString(16).padStart(4, '0')})")
            }
        }

        println("========================================")
    }
}
