package com.tellraw.app.util

import android.content.Context
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * 意见.txt问题简单测试
 * 测试意见.txt中提到的核心问题
 */
@RunWith(RobolectricTestRunner::class)
class YiJianSimpleTest {
    
    private lateinit var context: Context
    
    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
    }
    
    /**
     * 测试1：空消息不应该有任何组件
     */
    @Test
    fun testEmptyMessage() {
        val emptyMessage = ""
        val components = TextComponentHelper.parseTextComponents(emptyMessage)
        assertTrue("空消息不应该有任何组件", components.isEmpty())
    }
    
    /**
     * 测试2：text组件应该正确解析
     */
    @Test
    fun testTextComponent() {
        val message = "hello\u0FC8text\u0FC9"
        val components = TextComponentHelper.parseTextComponents(message)
        
        // 调试输出
        println("========================================")
        println("testTextComponent 调试输出:")
        println("输入消息: '$message'")
        println("消息长度: ${message.length}")
        println("解析得到的组件数量: ${components.size}")
        for ((index, component) in components.withIndex()) {
            println("组件 $index: type=${component.type}, content='${component.content}', content.length=${component.content.length}")
        }
        println("========================================")
        
        assertEquals("应该有1个text组件", 1, components.size)
        assertEquals("text内容应该是'hello'", "hello", components[0].content)
    }
    
    /**
     * 测试3：连续输入text组件不应该被拆分
     */
    @Test
    fun testConsecutiveTextInput() {
        var message = ""
        message = TextComponentHelper.insertTextWithComponent(message, 0, "a", TextComponentHelper.ComponentType.TEXT)
        message = TextComponentHelper.insertTextWithComponent(message, 1, "b", TextComponentHelper.ComponentType.TEXT)
        message = TextComponentHelper.insertTextWithComponent(message, 2, "c", TextComponentHelper.ComponentType.TEXT)
        
        val components = TextComponentHelper.parseTextComponents(message)
        assertEquals("应该只有1个text组件", 1, components.size)
        assertEquals("text内容应该是'abc'", "abc", components[0].content)
    }
    
    /**
     * 测试4：translate组件应该正确解析
     */
    @Test
    fun testTranslateComponent() {
        val message = "test\u0FC8translate\u0FC9"
        val components = TextComponentHelper.parseTextComponents(message)
        assertEquals("应该有1个translate组件", 1, components.size)
        assertEquals("translate内容应该是'test'", "test", components[0].content)
    }
    
    /**
     * 测试5：score组件应该正确解析
     */
    @Test
    fun testScoreComponent() {
        val message = "test:123\u0FC8score\u0FC9"
        val components = TextComponentHelper.parseTextComponents(message)
        assertEquals("应该有1个score组件", 1, components.size)
        assertEquals("score内容应该是'test:123'", "test:123", components[0].content)
    }
    
    /**
     * 测试6：selector组件应该正确解析
     */
    @Test
    fun testSelectorComponent() {
        val message = "@a\u0FC8selector\u0FC9"
        val components = TextComponentHelper.parseTextComponents(message)
        assertEquals("应该有1个selector组件", 1, components.size)
        assertEquals("selector内容应该是'@a'", "@a", components[0].content)
    }
    
    /**
     * 测试7：selector组件包含多个选择器
     */
    @Test
    fun testSelectorWithMultipleEntries() {
        val message = "@a@p@e\u0FC8selector\u0FC9"
        val components = TextComponentHelper.parseTextComponents(message)
        assertEquals("应该有1个selector组件", 1, components.size)
        assertEquals("selector内容应该是'@a@p@e'", "@a@p@e", components[0].content)
    }
    
    /**
     * 测试8：selector组件包含第一个@前面的文本
     */
    @Test
    fun testSelectorWithTextBeforeAt() {
        val message = "text@a@p\u0FC8selector\u0FC9"
        val components = TextComponentHelper.parseTextComponents(message)
        assertEquals("应该有1个selector组件", 1, components.size)
        assertEquals("selector内容应该是'text@a@p'", "text@a@p", components[0].content)
    }
    
    /**
     * 测试9：selector组件包含sep:定义
     */
    @Test
    fun testSelectorWithSepDefinition() {
        val message = "@a@p,'sep':kk,@e@r,'sep':66666\u0FC8selector\u0FC9"
        val components = TextComponentHelper.parseTextComponents(message)
        assertEquals("应该有1个selector组件", 1, components.size)
    }
    
    /**
     * 测试10：translate和text组件的JSON输出
     */
    @Test
    fun testTranslateWithTextJsonOutput() {
        val message = "lll\u0FC8translate\u0FC9bbb\u0FC8text\u0FC9"
        val components = TextComponentHelper.parseTextComponents(message)
        assertEquals("应该有2个组件", 2, components.size)
        
        val javaJson = TextComponentHelper.convertToJavaJson(components, "font", false, context)
        
        // 验证text应该是一个整体，不应该被拆分
        assertTrue("text应该是一个整体", javaJson.contains("\"text\":\"bbb\""))
        assertFalse("text不应该被拆分", javaJson.contains("{\"text\":\"b\"},{\"text\":\"b\"},{\"text\":\"b\"}"))
    }
    
    /**
     * 测试11：score和text组件的JSON输出
     */
    @Test
    fun testScoreWithTextJsonOutput() {
        val message = "lll:999\u0FC8score\u0FC9lll\u0FC8text\u0FC9"
        val components = TextComponentHelper.parseTextComponents(message)
        assertEquals("应该有2个组件", 2, components.size)
        
        val javaJson = TextComponentHelper.convertToJavaJson(components, "font", false, context)
        
        // 验证text应该是一个整体，不应该被拆分
        assertTrue("text应该是一个整体", javaJson.contains("\"text\":\"lll\""))
        assertFalse("text不应该被拆分", javaJson.contains("{\"text\":\"l\"},{\"text\":\"l\"},{\"text\":\"l\"}"))
    }
    
    /**
     * 测试12：基岩版应该忽略separator
     */
    @Test
    fun testBedrockShouldIgnoreSeparator() {
        val message = "@a@p,'sep':kk,@e@r,'sep':66666\u0FC8selector\u0FC9"
        val components = TextComponentHelper.parseTextComponents(message)
        assertEquals("应该有1个selector组件", 1, components.size)
        
        val bedrockJson = TextComponentHelper.convertToBedrockJson(components, "font", false, context, null)
        
        // 验证：基岩版不应该包含separator
        assertFalse("基岩版不应该包含separator", bedrockJson.contains("\"separator\""))
        
        // 验证：基岩版应该包含所有selector
        assertTrue("基岩版应该包含\"selector\":\"@a\"", bedrockJson.contains("\"selector\":\"@a\""))
        assertTrue("基岩版应该包含\"selector\":\"@p\"", bedrockJson.contains("\"selector\":\"@p\""))
        assertTrue("基岩版应该包含\"selector\":\"@e\"", bedrockJson.contains("\"selector\":\"@e\""))
        assertTrue("基岩版应该包含\"selector\":\"@r\"", bedrockJson.contains("\"selector\":\"@r\""))
    }
    
    /**
     * 测试13：separator修饰@选择器
     */
    @Test
    fun testSeparatorModifiesAtSelectors() {
        val message = "@a@p,'sep':kk,@e@r\u0FC8selector\u0FC9"
        val components = TextComponentHelper.parseTextComponents(message)
        assertEquals("应该有1个selector组件", 1, components.size)
        
        val javaJson = TextComponentHelper.convertToJavaJson(components, "font", false, context)
        
        // 验证：应该包含separator
        assertTrue("应该包含separator \"kk\"", javaJson.contains("\"separator\":\"kk\""))
        
        // 验证：应该包含所有selector
        assertTrue("Java版应该包含\"selector\":\"@a\"", javaJson.contains("\"selector\":\"@a\""))
        assertTrue("Java版应该包含\"selector\":\"@p\"", javaJson.contains("\"selector\":\"@p\""))
        assertTrue("Java版应该包含\"selector\":\"@e\"", javaJson.contains("\"selector\":\"@e\""))
        assertTrue("Java版应该包含\"selector\":\"@r\"", javaJson.contains("\"selector\":\"@r\""))
    }
    
    /**
     * 测试14：'sep':'air'的特殊情况
     */
    @Test
    fun testSepAirSpecialCase() {
        val message = "@a,'sep':'air',@p,'sep':666\u0FC8selector\u0FC9"
        val components = TextComponentHelper.parseTextComponents(message)
        assertEquals("应该有1个selector组件", 1, components.size)
        
        val javaJson = TextComponentHelper.convertToJavaJson(components, "font", false, context)
        
        // 验证：应该包含selector
        assertTrue("应该包含@a", javaJson.contains("\"selector\":\"@a\""))
        assertTrue("应该包含@p", javaJson.contains("\"selector\":\"@p\""))
        assertTrue("应该包含separator \"666\"", javaJson.contains("\"separator\":\"666\""))
    }
    
    /**
     * 测试15：第一个@前面的sep:定义应该被忽略
     */
    @Test
    fun testSepBeforeFirstAtShouldBeIgnored() {
        val message = "text,'sep':ignore,@a@p\u0FC8selector\u0FC9"
        val components = TextComponentHelper.parseTextComponents(message)
        assertEquals("应该有1个selector组件", 1, components.size)
        
        val javaJson = TextComponentHelper.convertToJavaJson(components, "font", false, context)
        
        // 验证：应该包含selector
        assertTrue("应该包含text", javaJson.contains("\"selector\":\"text\""))
        assertTrue("应该包含@a", javaJson.contains("\"selector\":\"@a\""))
        assertTrue("应该包含@p", javaJson.contains("\"selector\":\"@p\""))
    }
    
    /**
     * 测试16：没有@的selector文本组件
     */
    @Test
    fun testSelectorWithoutAtSymbol() {
        val message = "mknbt\u0FC8selector\u0FC9"
        val components = TextComponentHelper.parseTextComponents(message)
        assertEquals("应该有1个selector组件", 1, components.size)
        assertEquals("selector内容应该是'mknbt'", "mknbt", components[0].content)
        
        val javaJson = TextComponentHelper.convertToJavaJson(components, "font", false, context)
        assertTrue("Java版应该包含\"selector\":\"mknbt\"", javaJson.contains("\"selector\":\"mknbt\""))
    }
    
    /**
     * 测试17：多个不同类型的组件
     */
    @Test
    fun testMultipleDifferentComponents() {
        val message = "a\u0FC8text\u0FC9b\u0FC8translate\u0FC9c\u0FC8score\u0FC9d\u0FC8selector\u0FC9"
        val components = TextComponentHelper.parseTextComponents(message)
        assertEquals("应该有4个组件", 4, components.size)
        
        assertEquals("第一个组件应该是text", TextComponentHelper.ComponentType.TEXT, components[0].type)
        assertEquals("第二个组件应该是translate", TextComponentHelper.ComponentType.TRANSLATE, components[1].type)
        assertEquals("第三个组件应该是score", TextComponentHelper.ComponentType.SCORE, components[2].type)
        assertEquals("第四个组件应该是selector", TextComponentHelper.ComponentType.SELECTOR, components[3].type)
    }
    
    /**
     * 测试19：insertTextWithComponent函数
     */
    @Test
    fun testInsertTextWithComponent() {
        val message = "test\u0FC8text\u0FC9"
        val newMessage = TextComponentHelper.insertTextWithComponent(message, 0, "prefix", TextComponentHelper.ComponentType.TEXT)
        assertTrue("新消息应该以'prefix'开头", newMessage.startsWith("prefix"))
    }
    
    }
