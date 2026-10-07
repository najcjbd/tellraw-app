package com.tellraw.app.util

import org.junit.Assert.*
import org.junit.Test

/**
 * D4 的命令/JSON 识别器（纯 Kotlin 部分，无 Android Context）。
 * 覆盖：裸 JSON（rawtext / Java 组件）、tellraw、execute+tellraw 的拆分与双版本产出。
 */
class CommandInputParserTest {

    @Test
    fun bedrockRawtextToJava() {
        val r = CommandInputParser.parse("{\"rawtext\":[{\"text\":\"a\"}]}")
        assertNotNull(r)
        assertEquals(CommandInputParser.SourceEdition.BEDROCK, r!!.sourceEdition)
        assertNull("裸 JSON 没有选择器", r.selector)
        assertTrue("Java 侧应含文本 a：${r.javaJson}", r.javaJson.contains("\"a\""))
        assertTrue("基岩侧应含 rawtext：${r.bedrockJson}", r.bedrockJson.contains("rawtext"))
    }

    @Test
    fun javaComponentToBedrock() {
        val r = CommandInputParser.parse("{\"text\":\"a\"}")!!
        assertEquals(CommandInputParser.SourceEdition.JAVA, r.sourceEdition)
        assertTrue("基岩侧应含 rawtext：${r.bedrockJson}", r.bedrockJson.contains("rawtext"))
        assertTrue(r.bedrockJson.contains("\"a\""))
    }

    @Test
    fun tellrawCommandSplitsSelectorAndJson() {
        val r = CommandInputParser.parse("tellraw @a {\"text\":\"hi\"}")!!
        assertEquals("@a", r.selector)
        assertTrue(r.javaJson.contains("hi"))
    }

    @Test
    fun selectorWithBracketsDoesNotBreakSplitting() {
        val r = CommandInputParser.parse("tellraw @a[tag=x] {\"text\":\"hi\"}")!!
        assertEquals("@a[tag=x]", r.selector)
        assertTrue(r.javaJson.contains("hi"))
    }

    @Test
    fun executeTellrawKeepsPrefix() {
        val r = CommandInputParser.parse("execute as @a run tellraw @s {\"text\":\"hi\"}")!!
        assertEquals("execute as @a", r.executePrefix)
        assertEquals("@s", r.selector)
        assertTrue(r.javaJson.contains("hi"))
    }

    @Test
    fun runInsideJsonIsNotTreatedAsPrefixSeparator() {
        // JSON 文本里含 "run" 不应被当成 execute 的分隔
        val r = CommandInputParser.parse("tellraw @a {\"text\":\"run away\"}")!!
        assertNull(r.executePrefix)
        assertEquals("@a", r.selector)
        assertTrue(r.javaJson.contains("run away"))
    }

    @Test
    fun colorFieldMapsToSectionCode() {
        val r = CommandInputParser.parse("{\"text\":\"a\",\"color\":\"red\"}")!!
        assertTrue("应还原出 red：${r.javaJson}", r.javaJson.contains("red"))
    }

    @Test
    fun scoreComponent() {
        val r = CommandInputParser.parse("{\"score\":{\"name\":\"Steve\",\"objective\":\"kills\"}}")!!
        assertTrue("Java 侧应有 score：${r.javaJson}", r.javaJson.contains("Steve") && r.javaJson.contains("kills"))
    }

    @Test
    fun translateWithComponents() {
        val r = CommandInputParser.parse("{\"translate\":\"chat.type.text\",\"with\":[\"Steve\",\"hi\"]}")!!
        assertTrue("应含 translate：${r.javaJson}", r.javaJson.contains("chat.type.text"))
        assertTrue("应含 with 项：${r.javaJson}", r.javaJson.contains("Steve"))
    }

    @Test
    fun garbageReturnsNull() {
        assertNull(CommandInputParser.parse("hello world"))
        assertNull(CommandInputParser.parse(""))
        assertNull(CommandInputParser.parse("execute as @a"))   // 没有 run
    }
}
