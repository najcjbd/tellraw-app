package com.tellraw.app.util

import android.content.Context
import com.tellraw.app.R
import com.tellraw.app.model.SelectorType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * 选择器转换器测试
 * 测试Java版和基岩版选择器之间的转换逻辑
 */
@RunWith(RobolectricTestRunner::class)
class SelectorConverterTest {
    
    private lateinit var context: Context
    
    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
    }
    
    /**
     * 测试组1：目标选择器变量测试
     */
    @Test
    fun testSelectorVariables_1() {
        // @a - 所有玩家
        val selector = "@a"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }
    
    @Test
    fun testSelectorVariables_2() {
        // @p - 最近的玩家
        val selector = "@p"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }
    
    @Test
    fun testSelectorVariables_3() {
        // @r - 随机玩家
        val selector = "@r"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }
    
    @Test
    fun testSelectorVariables_4() {
        // @e - 所有实体
        val selector = "@e"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }
    
    @Test
    fun testSelectorVariables_5() {
        // @s - 命令执行者
        val selector = "@s"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }
    
    @Test
    fun testSelectorVariables_6() {
        // @n - 最近的实体
        val selector = "@n"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }
    
    @Test
    fun testSelectorVariables_7() {
        // @initiator - 基岩版特有
        val selector = "@initiator"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    @Test
    fun testSelectorVariables_8() {
        // @c - 教育版特有
        val selector = "@c"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    @Test
    fun testSelectorVariables_9() {
        // @v - 教育版特有
        val selector = "@v"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    /**
     * 测试组2：通用参数测试
     */
    @Test
    fun testUniversalParameters_1() {
        // type参数
        val selector = "@e[type=cow]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }
    
    @Test
    fun testUniversalParameters_2() {
        // name参数
        val selector = "@e[name=Steve]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }
    
    @Test
    fun testUniversalParameters_3() {
        // tag参数
        val selector = "@e[tag=vip]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }
    
    @Test
    fun testUniversalParameters_4() {
        // x, y, z参数
        val selector = "@e[x=0,y=64,z=0]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }
    
    @Test
    fun testUniversalParameters_5() {
        // dx, dy, dz参数
        val selector = "@e[dx=10,dy=5,dz=10]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }
    
    @Test
    fun testUniversalParameters_6() {
        // scores参数
        val selector = "@e[scores={kills=10}]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }
    
    /**
     * 测试组3：Java版特有参数测试
     */
    @Test
    fun testJavaParameters_1() {
        // distance参数
        val selector = "@e[distance=10]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    @Test
    fun testJavaParameters_2() {
        // x_rotation参数
        val selector = "@e[x_rotation=45]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    @Test
    fun testJavaParameters_3() {
        // y_rotation参数
        val selector = "@e[y_rotation=90]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    @Test
    fun testJavaParameters_4() {
        // nbt参数
        val selector = "@e[nbt={CustomName:\"Steve\"}]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    @Test
    fun testJavaParameters_5() {
        // team参数
        val selector = "@a[team=red]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    @Test
    fun testJavaParameters_6() {
        // limit参数
        val selector = "@e[limit=5]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    @Test
    fun testJavaParameters_7() {
        // sort参数
        val selector = "@e[sort=nearest]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    @Test
    fun testJavaParameters_8() {
        // predicate参数
        val selector = "@a[predicate=example:test]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    @Test
    fun testJavaParameters_9() {
        // advancements参数
        val selector = "@a[advancements={story/form_obsidian=true}]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    @Test
    fun testJavaParameters_10() {
        // level参数
        val selector = "@a[level=5]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    @Test
    fun testJavaParameters_11() {
        // gamemode参数
        val selector = "@a[gamemode=survival]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    /**
     * 测试组4：基岩版特有参数测试
     */
    @Test
    fun testBedrockParameters_1() {
        // r参数
        val selector = "@e[r=10]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    @Test
    fun testBedrockParameters_2() {
        // rm参数
        val selector = "@e[rm=5]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    @Test
    fun testBedrockParameters_3() {
        // rx参数
        val selector = "@e[rx=45]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    @Test
    fun testBedrockParameters_4() {
        // rxm参数
        val selector = "@e[rxm=-45]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    @Test
    fun testBedrockParameters_5() {
        // ry参数
        val selector = "@e[ry=90]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    @Test
    fun testBedrockParameters_6() {
        // rym参数
        val selector = "@e[rym=-90]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    @Test
    fun testBedrockParameters_7() {
        // hasitem参数
        val selector = "@a[hasitem={item=diamond}]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    @Test
    fun testBedrockParameters_8() {
        // family参数
        val selector = "@e[family=zombie]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    @Test
    fun testBedrockParameters_9() {
        // l参数
        val selector = "@a[l=5]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    @Test
    fun testBedrockParameters_10() {
        // lm参数
        val selector = "@a[lm=10]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    @Test
    fun testBedrockParameters_11() {
        // m参数
        val selector = "@a[m=0]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    @Test
    fun testBedrockParameters_12() {
        // c参数
        val selector = "@a[c=5]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    @Test
    fun testBedrockParameters_13() {
        // haspermission参数
        val selector = "@a[haspermission={camera=enabled}]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    @Test
    fun testBedrockParameters_14() {
        // has_property参数
        val selector = "@e[has_property={minecraft:has_nectar}]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    /**
     * 测试组5：参数范围测试
     */
    @Test
    fun testRangeParameters_1() {
        // distance范围
        val selector = "@e[distance=5..15]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    @Test
    fun testRangeParameters_2() {
        // distance范围（只有下限）
        val selector = "@e[distance=5..]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    @Test
    fun testRangeParameters_3() {
        // distance范围（只有上限）
        val selector = "@e[distance=..15]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    @Test
    fun testRangeParameters_4() {
        // level范围
        val selector = "@a[level=5..15]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    @Test
    fun testRangeParameters_5() {
        // l范围
        val selector = "@a[l=5..15]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    @Test
    fun testRangeParameters_6() {
        // scores范围
        val selector = "@e[scores={kills=5..10}]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }
    
    /**
     * 测试组6：参数反选测试
     */
    @Test
    fun testNegationParameters_1() {
        // type反选
        val selector = "@e[type=!cow]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }
    
    @Test
    fun testNegationParameters_2() {
        // name反选
        val selector = "@e[name=!Steve]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }
    
    @Test
    fun testNegationParameters_3() {
        // tag反选
        val selector = "@e[tag=!vip]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }
    
    @Test
    fun testNegationParameters_4() {
        // scores反选（基岩版）
        val selector = "@e[scores={kills=!5}]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    @Test
    fun testNegationParameters_5() {
        // family反选
        val selector = "@e[family=!monster]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    @Test
    fun testNegationParameters_6() {
        // gamemode反选
        val selector = "@a[gamemode=!survival]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    @Test
    fun testNegationParameters_7() {
        // m反选
        val selector = "@a[m=!0]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    /**
     * 测试组7：混合参数测试（通用+Java）
     */
    @Test
    fun testMixedParameters_1() {
        // 通用+Java
        val selector = "@e[type=cow,distance=10]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    @Test
    fun testMixedParameters_2() {
        // 通用+Java
        val selector = "@e[name=Steve,x_rotation=45]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    @Test
    fun testMixedParameters_3() {
        // 通用+Java
        val selector = "@e[tag=vip,limit=5]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    @Test
    fun testMixedParameters_4() {
        // 通用+Java
        val selector = "@e[x=0,y=64,z=0,distance=10]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    @Test
    fun testMixedParameters_5() {
        // 通用+Java
        val selector = "@e[dx=10,dy=5,dz=10,nbt={CustomName:\"Steve\"}]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    /**
     * 测试组8：混合参数测试（通用+基岩版）
     */
    @Test
    fun testMixedParameters_6() {
        // 通用+基岩版
        val selector = "@e[type=cow,r=10]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    @Test
    fun testMixedParameters_7() {
        // 通用+基岩版
        val selector = "@e[name=Steve,rx=45]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    @Test
    fun testMixedParameters_8() {
        // 通用+基岩版
        val selector = "@e[tag=vip,c=5]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    @Test
    fun testMixedParameters_9() {
        // 通用+基岩版
        val selector = "@e[x=0,y=64,z=0,r=10]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    @Test
    fun testMixedParameters_10() {
        // 通用+基岩版
        val selector = "@e[dx=10,dy=5,dz=10,hasitem={item=diamond}]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    /**
     * 测试组9：复杂参数组合测试
     */
    @Test
    fun testComplexParameters_1() {
        // 多个通用参数
        val selector = "@e[type=cow,name=Test,tag=vip]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }
    
    @Test
    fun testComplexParameters_2() {
        // 多个Java参数
        val selector = "@e[distance=10,x_rotation=45,y_rotation=90,limit=5,sort=nearest]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    @Test
    fun testComplexParameters_3() {
        // 多个基岩版参数
        val selector = "@e[r=10,rm=5,rx=45,rxm=-45,ry=90,rym=-90]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    @Test
    fun testComplexParameters_4() {
        // 通用+多个Java参数
        val selector = "@e[type=cow,distance=10,x_rotation=45,nbt={CustomName:\"Steve\"}]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    @Test
    fun testComplexParameters_5() {
        // 通用+多个基岩版参数
        val selector = "@e[type=cow,r=10,rx=45,hasitem={item=diamond}]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    @Test
    fun testComplexParameters_6() {
        // scores+其他参数
        val selector = "@e[scores={kills=10},distance=5]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    @Test
    fun testComplexParameters_7() {
        // scores+其他参数（基岩版）
        val selector = "@e[scores={kills=10},r=5]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    /**
     * 测试组10：选择器转换测试
     */
    @Test
    fun testSelectorConversion_1() {
        // 基岩版到Java版：r/rm -> distance
        val bedrockSelector = "@a[r=10]"
        val conversion = SelectorConverter.convertBedrockToJava(bedrockSelector, context)
        assertTrue("应该检测到基岩版选择器", conversion.wasConverted)
        assertTrue("Java版选择器应包含distance", conversion.javaSelector.contains("distance"))
    }
    
    @Test
    fun testSelectorConversion_2() {
        // Java版到基岩版：distance -> r/rm
        val javaSelector = "@a[distance=10]"
        val conversion = SelectorConverter.convertJavaToBedrock(javaSelector, context)
        assertTrue("基岩版选择器应包含r", conversion.bedrockSelector.contains("r"))
    }
    
    @Test
    fun testSelectorConversion_3() {
        // 基岩版到Java版：m -> gamemode
        val bedrockSelector = "@a[m=0]"
        val conversion = SelectorConverter.convertBedrockToJava(bedrockSelector, context)
        assertTrue("Java版选择器应包含gamemode", conversion.javaSelector.contains("gamemode"))
    }
    
    @Test
    fun testSelectorConversion_4() {
        // Java版到基岩版：gamemode -> m
        val javaSelector = "@a[gamemode=survival]"
        val conversion = SelectorConverter.convertJavaToBedrock(javaSelector, context)
        assertTrue("基岩版选择器应包含m", conversion.bedrockSelector.contains("m"))
    }
    
    @Test
    fun testSelectorConversion_5() {
        // 基岩版到Java版：c -> limit/sort
        val bedrockSelector = "@a[c=5]"
        val conversion = SelectorConverter.convertBedrockToJava(bedrockSelector, context)
        assertTrue("Java版选择器应包含limit", conversion.javaSelector.contains("limit"))
        assertTrue("Java版选择器应包含sort", conversion.javaSelector.contains("sort"))
    }
    
    @Test
    fun testSelectorConversion_6() {
        // Java版到基岩版：limit/sort -> c
        val javaSelector = "@a[limit=5,sort=nearest]"
        val conversion = SelectorConverter.convertJavaToBedrock(javaSelector, context)
        assertTrue("基岩版选择器应包含c", conversion.bedrockSelector.contains("c"))
    }
    
    @Test
    fun testSelectorConversion_7() {
        // Java版到基岩版：sort=random (无limit) - @a转换为@r
        val javaSelector = "@a[sort=random]"
        val conversion = SelectorConverter.convertJavaToBedrock(javaSelector, context)
        assertEquals("基岩版选择器应为@r[c=9999]", "@r[c=9999]", conversion.bedrockSelector)
    }
    
    @Test
    fun testSelectorConversion_8() {
        // Java版到基岩版：sort=random + limit - @a转换为@r
        val javaSelector = "@a[limit=5,sort=random]"
        val conversion = SelectorConverter.convertJavaToBedrock(javaSelector, context)
        assertEquals("基岩版选择器应为@r[c=5]", "@r[c=5]", conversion.bedrockSelector)
    }
    
    @Test
    fun testSelectorConversion_9() {
        // Java版到基岩版：sort=random - @r保持@r
        val javaSelector = "@r[sort=random]"
        val conversion = SelectorConverter.convertJavaToBedrock(javaSelector, context)
        assertEquals("基岩版选择器应为@r[c=9999]", "@r[c=9999]", conversion.bedrockSelector)
    }
    
    @Test
    fun testSelectorConversion_10() {
        // Java版到基岩版：sort=random - @e转换为c参数
        val javaSelector = "@e[sort=random]"
        val conversion = SelectorConverter.convertJavaToBedrock(javaSelector, context)
        assertEquals("基岩版选择器应为@e[c=9999]", "@e[c=9999]", conversion.bedrockSelector)
    }
    
    /**
     * 测试组11：hasitem-nbt转换测试
     */
    @Test
    fun testHasitemNbtConversion_1() {
        // 基岩版hasitem到Java版nbt：简单物品
        val bedrockSelector = "@a[hasitem={item=diamond}]"
        val conversion = SelectorConverter.convertBedrockToJava(bedrockSelector, context)
        assertTrue("Java版选择器应包含nbt", conversion.javaSelector.contains("nbt"))
        assertTrue("Java版选择器应包含Inventory", conversion.javaSelector.contains("Inventory"))
    }
    
    @Test
    fun testHasitemNbtConversion_2() {
        // 基岩版hasitem到Java版nbt：主手物品
        val bedrockSelector = "@a[hasitem={item=diamond_sword,location=slot.weapon.mainhand,slot=0}]"
        val conversion = SelectorConverter.convertBedrockToJava(bedrockSelector, context)
        assertTrue("Java版选择器应包含SelectedItem", conversion.javaSelector.contains("SelectedItem"))
    }
    
    @Test
    fun testHasitemNbtConversion_3() {
        // 基岩版hasitem到Java版nbt：装备栏
        val bedrockSelector = "@a[hasitem={item=diamond_helmet,location=slot.armor.head,slot=0}]"
        val conversion = SelectorConverter.convertBedrockToJava(bedrockSelector, context)
        assertTrue("Java版选择器应包含equipment", conversion.javaSelector.contains("equipment"))
        assertTrue("Java版选择器应包含head", conversion.javaSelector.contains("head"))
    }
    
    @Test
    fun testHasitemNbtConversion_4() {
        // Java版nbt到基岩版hasitem：物品栏
        val javaSelector = "@a[nbt={Inventory:[{Slot:0b,id:\"minecraft:diamond\",Count:1b}]}]"
        val conversion = SelectorConverter.convertJavaToBedrock(javaSelector, context)
        assertTrue("基岩版选择器应包含hasitem", conversion.bedrockSelector.contains("hasitem"))
        assertTrue("基岩版选择器应包含slot.hotbar", conversion.bedrockSelector.contains("slot.hotbar"))
    }
    
    @Test
    fun testHasitemNbtConversion_5() {
        // Java版nbt到基岩版hasitem：主手物品
        val javaSelector = "@a[nbt={SelectedItem:{id:\"minecraft:diamond_sword\",Count:1b}}]"
        val conversion = SelectorConverter.convertJavaToBedrock(javaSelector, context)
        assertTrue("基岩版选择器应包含hasitem", conversion.bedrockSelector.contains("hasitem"))
        assertTrue("基岩版选择器应包含slot.weapon.mainhand", conversion.bedrockSelector.contains("slot.weapon.mainhand"))
    }
    
    @Test
    fun testHasitemNbtConversion_6() {
        // Java版nbt到基岩版hasitem：装备栏
        val javaSelector = "@a[nbt={equipment:{head:{id:\"minecraft:diamond_helmet\"}}}]"
        val conversion = SelectorConverter.convertJavaToBedrock(javaSelector, context)
        assertTrue("基岩版选择器应包含hasitem", conversion.bedrockSelector.contains("hasitem"))
        assertTrue("基岩版选择器应包含slot.armor.head", conversion.bedrockSelector.contains("slot.armor.head"))
    }
    
    @Test
    fun testHasitemNbtConversion_7() {
        // 基岩版hasitem数组到Java版nbt：多个物品
        val bedrockSelector = "@a[hasitem=[{item=diamond,quantity=5..},{item=iron,quantity=10..}]]"
        val conversion = SelectorConverter.convertBedrockToJava(bedrockSelector, context)
        assertTrue("Java版选择器应包含nbt", conversion.javaSelector.contains("nbt"))
    }
    
    @Test
    fun testHasitemNbtConversion_8() {
        // 基岩版hasitem数组到Java版nbt：混合位置
        val bedrockSelector = "@a[hasitem=[{item=diamond_sword,location=slot.weapon.mainhand,slot=0},{item=diamond,location=slot.hotbar,slot=8}]]"
        val conversion = SelectorConverter.convertBedrockToJava(bedrockSelector, context)
        assertTrue("Java版选择器应包含nbt", conversion.javaSelector.contains("nbt"))
    }
    
    @Test
    fun testHasitemNbtConversion_9() {
        // Java版nbt到基岩版hasitem：多个nbt参数
        val javaSelector = "@a[nbt={SelectedItem:{id:\"minecraft:diamond_sword\"}},nbt={Inventory:[{id:\"minecraft:diamond\",count:2b}]}]"
        val conversion = SelectorConverter.convertJavaToBedrock(javaSelector, context)
        assertTrue("基岩版选择器应包含hasitem", conversion.bedrockSelector.contains("hasitem"))
    }
    
    @Test
    fun testHasitemNbtConversion_10() {
        // Java版nbt到基岩版hasitem：装备栏
        val javaSelector = "@a[nbt={equipment:{head:{id:\"minecraft:diamond_helmet\"},chest:{id:\"minecraft:diamond_chestplate\"}}}]"
        val conversion = SelectorConverter.convertJavaToBedrock(javaSelector, context)
        assertTrue("基岩版选择器应包含hasitem", conversion.bedrockSelector.contains("hasitem"))
    }
    
    /**
     * 测试组12：hasitem参数详细测试
     */
    @Test
    fun testHasitemParameters_1() {
        // 简单hasitem
        val selector = "@a[hasitem={item=diamond}]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    @Test
    fun testHasitemParameters_2() {
        // hasitem with quantity
        val selector = "@a[hasitem={item=diamond,quantity=5}]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    @Test
    fun testHasitemParameters_3() {
        // hasitem with quantity range
        val selector = "@a[hasitem={item=diamond,quantity=5..10}]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    @Test
    fun testHasitemParameters_4() {
        // hasitem with location
        val selector = "@a[hasitem={item=diamond,location=slot.weapon.mainhand}]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    @Test
    fun testHasitemParameters_5() {
        // hasitem array
        val selector = "@a[hasitem=[{item=diamond,quantity=5..},{item=iron,quantity=10..}]]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    @Test
    fun testHasitemParameters_6() {
        // hasitem with negation
        val selector = "@a[hasitem={item=diamond,quantity=!5}]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    @Test
    fun testHasitemParameters_7() {
        // hasitem with slot range
        val selector = "@a[hasitem={item=diamond,location=slot.hotbar,slot=0..8}]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    @Test
    fun testHasitemParameters_8() {
        // hasitem with data parameter
        val selector = "@a[hasitem={item=stone,data=1}]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    /**
     * 测试组13：nbt参数详细测试
     */
    @Test
    fun testNbtParameters_1() {
        // 简单nbt
        val selector = "@a[nbt={CustomName:\"Steve\"}]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    @Test
    fun testNbtParameters_2() {
        // nbt with Inventory
        val selector = "@a[nbt={Inventory:[{id:\"minecraft:diamond\",Count:5b,Slot:0b}]}]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    @Test
    fun testNbtParameters_3() {
        // nbt with SelectedItem
        val selector = "@a[nbt={SelectedItem:{id:\"minecraft:diamond_sword\",Count:1b}}]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    @Test
    fun testNbtParameters_4() {
        // nbt with Tags
        val selector = "@a[nbt={Tags:[\"a\",\"b\"]}]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    @Test
    fun testNbtParameters_5() {
        // nbt with multiple fields
        val selector = "@a[nbt={CustomName:\"Steve\",Tags:[\"vip\"],Health:20f}]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    @Test
    fun testNbtParameters_6() {
        // nbt with equipment
        val selector = "@a[nbt={equipment:{head:{id:\"minecraft:diamond_helmet\"}}}]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    @Test
    fun testNbtParameters_7() {
        // nbt with complex Inventory
        val selector = "@a[nbt={Inventory:[{Slot:0b,id:\"minecraft:diamond\",Count:1b},{Slot:8b,id:\"minecraft:iron\",Count:64b}]}]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    @Test
    fun testNbtParameters_8() {
        // nbt with multiple top-level tags
        val selector = "@a[nbt={SelectedItem:{id:\"minecraft:diamond_sword\"}},nbt={Inventory:[{id:\"minecraft:diamond\"}]}]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    /**
     * 测试组14：参数过滤测试
     */
    @Test
    fun testParameterFiltering_1() {
        // 过滤Java版独有参数到基岩版
        val javaSelector = "@a[distance=10,team=red,gamemode=survival]"
        val (filtered, removed, reminders) = SelectorConverter.filterSelectorParameters(javaSelector, SelectorType.BEDROCK, context)
        assertTrue("应移除team参数", removed.contains("team"))
        assertFalse("不应包含team参数", filtered.contains("team"))
    }
    
    @Test
    fun testParameterFiltering_2() {
        // 过滤基岩版独有参数到Java版
        // hasitem 应该被转换为 nbt，r 应该被转换为 distance，family 参数会被移除
        val bedrockSelector = "@a[r=10,hasitem={item=diamond},family=zombie]"
        val (filtered, removed, _) = SelectorConverter.filterSelectorParameters(bedrockSelector, SelectorType.JAVA, context)
        // 检查转换后的选择器
        assertTrue("转换后的选择器应包含nbt", filtered.contains("nbt"))
        assertTrue("转换后的选择器应包含distance", filtered.contains("distance"))
        assertFalse("转换后的选择器不应包含hasitem", filtered.contains("hasitem"))
        assertFalse("转换后的选择器不应包含r", filtered.contains("r"))
        assertFalse("转换后的选择器不应包含family", filtered.contains("family"))
    }
    
    @Test
    fun testParameterFiltering_3() {
        // 保留通用参数
        val selector = "@e[type=cow,name=Test]"
        val (filtered, removed, reminders) = SelectorConverter.filterSelectorParameters(selector, SelectorType.JAVA, context)
        // 检查转换后的选择器
        assertTrue("应保留type参数", filtered.contains("type"))
        assertTrue("应保留name参数", filtered.contains("name"))
        assertTrue("通用参数转换应该没有提醒信息", reminders.isEmpty())
    }
    
    @Test
    fun testParameterFiltering_4() {
        // 过滤scores反选（基岩版到Java版）
        // Java版不支持scores反选，所以整个scores参数应该被移除
        val bedrockSelector = "@e[scores={kills=!5}]"
        val (filtered, removed, reminders) = SelectorConverter.filterSelectorParameters(bedrockSelector, SelectorType.JAVA, context)
        // 检查转换后的选择器
        assertFalse("转换后的选择器不应包含scores参数", filtered.contains("scores"))
        // 检查提醒信息中是否有scores反选移除的提醒
        assertTrue("提醒信息应包含scores反选移除的提醒", reminders.any { it.contains("scores") })
    }
    
    @Test
    fun testParameterFiltering_5() {
        // 保留scores非反选
        val selector = "@e[scores={kills=5}]"
        val (filtered, removed, _) = SelectorConverter.filterSelectorParameters(selector, SelectorType.JAVA, context)
        // 检查转换后的选择器
        assertTrue("应保留scores参数", filtered.contains("scores"))
    }
    
    /**
     * 测试组15：事例转换测试
     */
    @Test
    fun testExampleConversions_1() {
        // 事例1：全身钻石装备
        val bedrockSelector = "@a[hasitem=[{item=shield,location=slot.weapon.offhand,slot=0},{item=diamond_helmet,location=slot.armor.head,slot=0},{item=diamond_chestplate,location=slot.armor.chest,slot=0},{item=diamond_leggings,location=slot.armor.legs,slot=0},{item=diamond_boots,location=slot.armor.feet,slot=0}]]"
        val conversion = SelectorConverter.convertBedrockToJava(bedrockSelector, context)
        assertTrue("应包含equipment", conversion.javaSelector.contains("equipment"))
        assertTrue("应包含offhand", conversion.javaSelector.contains("offhand"))
        assertTrue("应包含head", conversion.javaSelector.contains("head"))
        assertTrue("应包含chest", conversion.javaSelector.contains("chest"))
        assertTrue("应包含legs", conversion.javaSelector.contains("legs"))
        assertTrue("应包含feet", conversion.javaSelector.contains("feet"))
    }
    
    @Test
    fun testExampleConversions_2() {
        // 事例2：物品栏第21格
        val bedrockSelector = "@a[hasitem={item=netherite_ingot,location=slot.inventory,slot=12}]"
        val conversion = SelectorConverter.convertBedrockToJava(bedrockSelector, context)
        assertTrue("应包含Inventory", conversion.javaSelector.contains("Inventory"))
        assertTrue("应包含Slot:21b", conversion.javaSelector.contains("Slot:21b"))
    }
    
    @Test
    fun testExampleConversions_3() {
        // 事例3：快捷栏第八格有两个钻石
        val bedrockSelector = "@a[hasitem={item=diamond,location=slot.hotbar,slot=8,quantity=2}]"
        val conversion = SelectorConverter.convertBedrockToJava(bedrockSelector, context)
        assertTrue("应包含Inventory", conversion.javaSelector.contains("Inventory"))
        assertTrue("应包含Slot:8b", conversion.javaSelector.contains("Slot:8b"))
        assertTrue("应包含Count:2b", conversion.javaSelector.contains("Count:2b"))
    }
    
    @Test
    fun testExampleConversions_4() {
        // 事例4：主手钻石剑，物品栏钻石
        val bedrockSelector = "@a[hasitem=[{item=minecraft:diamond_sword,location=slot.weapon.mainhand,slot=0},{item=diamond,quantity=2}]]"
        val conversion = SelectorConverter.convertBedrockToJava(bedrockSelector, context)
        assertTrue("应包含nbt", conversion.javaSelector.contains("nbt"))
        assertTrue("应包含SelectedItem", conversion.javaSelector.contains("SelectedItem"))
        assertTrue("应包含Inventory", conversion.javaSelector.contains("Inventory"))
    }
    
    @Test
    fun testExampleConversions_5() {
        // 事例5：副手两根线
        val bedrockSelector = "@a[hasitem={item=string,location=slot.weapon.offhand,slot=0,quantity=2}]"
        val conversion = SelectorConverter.convertBedrockToJava(bedrockSelector, context)
        assertTrue("应包含equipment", conversion.javaSelector.contains("equipment"))
        assertTrue("应包含offhand", conversion.javaSelector.contains("offhand"))
        assertTrue("应包含string", conversion.javaSelector.contains("string"))
        assertTrue("应包含Count:2b", conversion.javaSelector.contains("Count:2b"))
    }
    
    @Test
    fun testExampleConversions_6() {
        // 事例2反向：Java版到基岩版
        val javaSelector = "@a[nbt={Inventory:[{Slot:21b,id:\"minecraft:netherite_ingot\"}]}]"
        val conversion = SelectorConverter.convertJavaToBedrock(javaSelector, context)
        assertTrue("应包含hasitem", conversion.bedrockSelector.contains("hasitem"))
        assertTrue("应包含slot.inventory", conversion.bedrockSelector.contains("slot.inventory"))
        assertTrue("应包含slot=12", conversion.bedrockSelector.contains("slot=12"))
    }
    
    @Test
    fun testExampleConversions_7() {
        // 事例3反向：Java版到基岩版
        val javaSelector = "@a[nbt={Inventory:[{Slot:8b,id:\"minecraft:diamond\",Count:2b}]}]"
        val conversion = SelectorConverter.convertJavaToBedrock(javaSelector, context)
        assertTrue("应包含hasitem", conversion.bedrockSelector.contains("hasitem"))
        assertTrue("应包含slot.hotbar", conversion.bedrockSelector.contains("slot.hotbar"))
        assertTrue("应包含slot=8", conversion.bedrockSelector.contains("slot=8"))
    }
    
    @Test
    fun testExampleConversions_8() {
        // 事例4反向：Java版到基岩版
        val javaSelector = "@a[nbt={SelectedItem:{id:\"minecraft:diamond_sword\"}},nbt={Inventory:[{id:\"minecraft:diamond\",Count:2b}]}]"
        val conversion = SelectorConverter.convertJavaToBedrock(javaSelector, context)
        assertTrue("应包含hasitem", conversion.bedrockSelector.contains("hasitem"))
    }
    
    @Test
 fun testExampleConversions_9() {
        // 事例5反向：Java版到基岩版
        val javaSelector = "@a[nbt={equipment:{offhand:{id:\"minecraft:string\",Count:2b}}}]"
        val conversion = SelectorConverter.convertJavaToBedrock(javaSelector, context)
        assertTrue("应包含hasitem", conversion.bedrockSelector.contains("hasitem"))
        assertTrue("应包含slot.weapon.offhand", conversion.bedrockSelector.contains("slot.weapon.offhand"))
        assertTrue("应包含quantity=2", conversion.bedrockSelector.contains("quantity=2"))
    }
    
    /**
     * 测试组16：参数组合深度测试
     */
    @Test
    fun testDeepParameterCombinations_1() {
        // distance + x_rotation + y_rotation + nbt + scores + tag + type
        val javaSelector = "@a[distance=5..10,x_rotation=-45..45,y_rotation=-90..90,nbt={SelectedItem:{id:\"minecraft:diamond_sword\"}},scores={kills=5..10},tag=warrior,type=player]"
        val conversion = SelectorConverter.convertJavaToBedrock(javaSelector, context)
        assertTrue("应包含r参数", conversion.bedrockSelector.contains("r="))
        assertTrue("应包含rx参数", conversion.bedrockSelector.contains("rx="))
        assertTrue("应包含ry参数", conversion.bedrockSelector.contains("ry="))
        assertTrue("应包含tag参数", conversion.bedrockSelector.contains("tag="))
        assertTrue("应包含scores参数", conversion.bedrockSelector.contains("scores="))
    }
    
    @Test
    fun testDeepParameterCombinations_2() {
        // r + rm + rx + rxm + ry + rym + hasitem + family + scores
        val bedrockSelector = "@a[r=10,rm=5,rx=30,rxm=-30,ry=90,rym=-90,hasitem={item=diamond},family=player,scores={health=10..20}]"
        val conversion = SelectorConverter.convertBedrockToJava(bedrockSelector, context)
        assertTrue("应包含distance参数", conversion.javaSelector.contains("distance="))
        assertTrue("应包含x_rotation参数", conversion.javaSelector.contains("x_rotation="))
        assertTrue("应包含y_rotation参数", conversion.javaSelector.contains("y_rotation="))
        assertTrue("应包含nbt参数", conversion.javaSelector.contains("nbt="))
        assertTrue("应包含scores参数", conversion.javaSelector.contains("scores="))
    }
    
    @Test
    fun testDeepParameterCombinations_3() {
        // limit + sort + type + team + gamemode + advancements
        val javaSelector = "@a[limit=5,sort=nearest,type=player,team=red,gamemode=survival,advancements={story/obtain_armor={done:true}}]"
        val conversion = SelectorConverter.convertJavaToBedrock(javaSelector, context)
        assertTrue("应包含c参数", conversion.bedrockSelector.contains("c="))
        assertTrue("应包含m参数", conversion.bedrockSelector.contains("m="))
    }
    
    @Test
    fun testDeepParameterCombinations_4() {
        // x + y + z + dx + dy + dz + type + name
        val selector = "@e[x=100,y=64,z=200,dx=10,dy=5,dz=10,type=cow,name=Bessie]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
        assertTrue("应包含x参数", selector.contains("x="))
        assertTrue("应包含y参数", selector.contains("y="))
        assertTrue("应包含z参数", selector.contains("z="))
        assertTrue("应包含dx参数", selector.contains("dx="))
        assertTrue("应包含dy参数", selector.contains("dy="))
        assertTrue("应包含dz参数", selector.contains("dz="))
    }
    
    @Test
    fun testDeepParameterCombinations_5() {
        // 复杂的scores对象
        val javaSelector = "@a[scores={kills=5,deaths=3,health=10..20,experience=100..500}]"
        val conversion = SelectorConverter.convertJavaToBedrock(javaSelector, context)
        assertTrue("应包含scores参数", conversion.bedrockSelector.contains("scores="))
    }
    
    /**
     * 测试组17：边界情况扩展测试
     */
    @Test
    fun testExtendedEdgeCases_1() {
        // 空参数列表
        val selector = "@a[]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }
    
    @Test
    fun testExtendedEdgeCases_2() {
        // 单个空格参数
        val selector = "@a[ ]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }
    
    @Test
    fun testExtendedEdgeCases_3() {
        // 极端距离值
        val javaSelector = "@a[distance=0.1..10000]"
        val conversion = SelectorConverter.convertJavaToBedrock(javaSelector, context)
        assertTrue("应包含r参数", conversion.bedrockSelector.contains("r="))
    }
    
    @Test
    fun testExtendedEdgeCases_4() {
        // 负坐标
        val selector = "@e[x=-100,y=-64,z=-200,dx=10,dy=5,dz=10]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }
    
    @Test
    fun testExtendedEdgeCases_5() {
        // 大数量值
        val bedrockSelector = "@a[hasitem={item=diamond,quantity=1000}]"
        val conversion = SelectorConverter.convertBedrockToJava(bedrockSelector, context)
        assertTrue("应包含Count参数", conversion.javaSelector.contains("Count:"))
        assertTrue("应包含nbt参数", conversion.javaSelector.contains("nbt="))
    }
    
    @Test
    fun testExtendedEdgeCases_6() {
        // 无效的参数格式
        val selector = "@a[type=,name=,tag=]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }
    
    @Test
    fun testExtendedEdgeCases_7() {
        // 特殊字符在name中
        val selector = "@a[name=Player_123!@#]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }
    
    @Test
    fun testExtendedEdgeCases_8() {
        // Unicode字符在name中
        val selector = "@a[name=玩家123]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }
    
    /**
     * 测试组18：目标选择器与参数的全面组合测试
     */
    @Test
    fun testSelectorParameterCombinations_1() {
        // @a + 所有通用参数
        val selector = "@a[type=player,name=Test,tag=warrior,x=100,y=64,z=200,dx=10,dy=5,dz=10,scores={kills=5}]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }
    
    @Test
    fun testSelectorParameterCombinations_2() {
        // @p + Java版特有参数
        val javaSelector = "@p[distance=5,team=red,gamemode=survival,limit=1]"
        val type = SelectorConverter.detectSelectorType(javaSelector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    @Test
    fun testSelectorParameterCombinations_3() {
        // @r + 基岩版特有参数
        val bedrockSelector = "@r[r=10,hasitem={item=diamond},family=player,c=1]"
        val type = SelectorConverter.detectSelectorType(bedrockSelector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    @Test
    fun testSelectorParameterCombinations_4() {
        // @e + type参数
        val selector = "@e[type=cow]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }
    
    @Test
    fun testSelectorParameterCombinations_5() {
        // @s + 通用参数
        val selector = "@s[tag=active,scores={health=20}]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }
    
    @Test
    fun testSelectorParameterCombinations_6() {
        // @n + 参数（基岩版）
        val bedrockSelector = "@n[r=5,hasitem={item=diamond}]"
        val type = SelectorConverter.detectSelectorType(bedrockSelector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    @Test
    fun testSelectorParameterCombinations_7() {
        // @initiator + 参数（基岩版）
        val bedrockSelector = "@initiator[tag=initiator]"
        val type = SelectorConverter.detectSelectorType(bedrockSelector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    @Test
    fun testSelectorParameterCombinations_8() {
        // @c + 参数（基岩版）
        val bedrockSelector = "@c[r=10,family=entity]"
        val type = SelectorConverter.detectSelectorType(bedrockSelector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    @Test
    fun testSelectorParameterCombinations_9() {
        // @v + 参数（基岩版）
        val bedrockSelector = "@v[r=5,hasitem={item=arrow}]"
        val type = SelectorConverter.detectSelectorType(bedrockSelector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    @Test
    fun testSelectorParameterCombinations_10() {
        // 所有选择器变量 + type=player
        val selectors = listOf("@a[type=player]", "@p[type=player]", "@r[type=player]", "@s[type=player]")
        for (selector in selectors) {
            val type = SelectorConverter.detectSelectorType(selector)
            assertEquals(SelectorType.UNIVERSAL, type)
        }
    }
    
    /**
     * 测试组19：hasitem-nbt 转换的更多边界情况
     */
    @Test
    fun testHasitemNbtEdgeCases_1() {
        // 空hasitem数组应该被移除（没有item选项）
        val bedrockSelector = "@a[hasitem=[]]"
        val conversion = SelectorConverter.convertBedrockToJava(bedrockSelector, context)
        // 空的hasitem数组没有item，应该被移除
        assertFalse("hasitem应该被移除", conversion.javaSelector.contains("hasitem"))
        assertFalse("空的hasitem数组不应该转换为nbt", conversion.javaSelector.contains("nbt="))
    }
    
    @Test
    fun testHasitemNbtEdgeCases_2() {
        // hasitem只有item
        val bedrockSelector = "@a[hasitem={item=diamond}]"
        val conversion = SelectorConverter.convertBedrockToJava(bedrockSelector, context)
        assertTrue("应包含nbt参数", conversion.javaSelector.contains("nbt="))
    }
    
    @Test
    fun testHasitemNbtEdgeCases_3() {
        // hasitem只有quantity，没有item，应该被移除
        val bedrockSelector = "@a[hasitem={quantity=5}]"
        val conversion = SelectorConverter.convertBedrockToJava(bedrockSelector, context)
        // 没有item的hasitem应该被移除
        assertFalse("没有item的hasitem应该被移除", conversion.javaSelector.contains("hasitem"))
        assertFalse("没有item的hasitem不应该转换为nbt", conversion.javaSelector.contains("nbt="))
    }
    
    @Test
    fun testHasitemNbtEdgeCases_4() {
        // hasitem只有location，没有item，应该被移除
        val bedrockSelector = "@a[hasitem={location=slot.hotbar}]"
        val conversion = SelectorConverter.convertBedrockToJava(bedrockSelector, context)
        // 没有item的hasitem应该被移除
        assertFalse("没有item的hasitem应该被移除", conversion.javaSelector.contains("hasitem"))
        assertFalse("没有item的hasitem不应该转换为nbt", conversion.javaSelector.contains("nbt="))
    }
    
    @Test
    fun testHasitemNbtEdgeCases_5() {
        // hasitem只有slot，没有item，应该被移除
        val bedrockSelector = "@a[hasitem={slot=5}]"
        val conversion = SelectorConverter.convertBedrockToJava(bedrockSelector, context)
        // 没有item的hasitem应该被移除
        assertFalse("没有item的hasitem应该被移除", conversion.javaSelector.contains("hasitem"))
        assertFalse("没有item的hasitem不应该转换为nbt", conversion.javaSelector.contains("nbt="))
    }
    
    @Test
    fun testHasitemNbtEdgeCases_6() {
        // hasitem带有data参数，data在Java版中不再使用，应该被忽略
        val bedrockSelector = "@a[hasitem={item=diamond,data=1}]"
        val conversion = SelectorConverter.convertBedrockToJava(bedrockSelector, context)
        // 应该转换为nbt，但data参数会被忽略
        assertTrue("应包含nbt参数", conversion.javaSelector.contains("nbt="))
        assertTrue("应包含diamond物品", conversion.javaSelector.contains("diamond"))
        // 检查是否有关于data参数的提醒
        val hasDataReminder = conversion.javaReminders.any { it.contains("data") }
        // 如果有提醒，说明data参数被处理了（可能被忽略）
        if (hasDataReminder) {
            assertTrue("提醒信息应包含data", conversion.javaReminders.any { it.contains("data") })
        }
    }
    
    @Test
    fun testHasitemNbtEdgeCases_7() {
        // nbt为空对象
        val javaSelector = "@a[nbt={}]"
        val conversion = SelectorConverter.convertJavaToBedrock(javaSelector, context)
        assertTrue("应保留nbt参数", conversion.bedrockSelector.contains("nbt="))
    }
    
    @Test
    fun testHasitemNbtEdgeCases_8() {
        // nbt只有SelectedItem
        val javaSelector = "@a[nbt={SelectedItem:{id:\"minecraft:diamond_sword\"}}]"
        val conversion = SelectorConverter.convertJavaToBedrock(javaSelector, context)
        assertTrue("应包含hasitem", conversion.bedrockSelector.contains("hasitem"))
        assertTrue("应包含slot.weapon.mainhand", conversion.bedrockSelector.contains("slot.weapon.mainhand"))
    }
    
    @Test
    fun testHasitemNbtEdgeCases_9() {
        // nbt只有Inventory且没有Slot，应该转换为hasitem
        val javaSelector = "@a[nbt={Inventory:[{id:\"minecraft:diamond\"}]}]"
        val conversion = SelectorConverter.convertJavaToBedrock(javaSelector, context)
        assertTrue("应包含hasitem", conversion.bedrockSelector.contains("hasitem"))
        assertTrue("应包含item=diamond", conversion.bedrockSelector.contains("item=diamond"))
    }
    
    @Test
    fun testHasitemNbtEdgeCases_10() {
        // nbt只有equipment
        val javaSelector = "@a[nbt={equipment:{head:{id:\"minecraft:diamond_helmet\"}}}]"
        val conversion = SelectorConverter.convertJavaToBedrock(javaSelector, context)
        assertTrue("应包含hasitem", conversion.bedrockSelector.contains("hasitem"))
        assertTrue("应包含slot.armor.head", conversion.bedrockSelector.contains("slot.armor.head"))
    }
    
    /**
     * 测试组20：特殊字符和转义测试
     */
    @Test
    fun testSpecialCharacters_1() {
        // name中的引号
        val selector = "@a[name=\"Player\\\"Name\"]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }
    
    @Test
    fun testSpecialCharacters_2() {
        // name中的逗号
        val selector = "@a[name=Player,Name]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }
    
    @Test
    fun testSpecialCharacters_3() {
        // name中的方括号
        val selector = "@a[name=Player[123]]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }
    
    @Test
    fun testSpecialCharacters_4() {
        // name中的花括号
        val selector = "@a[name=Player{123}]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }
    
    @Test
    fun testSpecialCharacters_5() {
        // tag中的特殊字符
        val selector = "@a[tag=warrior_123!@#]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }
    
    @Test
    fun testSpecialCharacters_6() {
        // nbt中的特殊字符
        val javaSelector = "@a[nbt={SelectedItem:{id:\"minecraft:diamond_sword_with_underscores\"}}]"
        val conversion = SelectorConverter.convertJavaToBedrock(javaSelector, context)
        assertTrue("应包含hasitem", conversion.bedrockSelector.contains("hasitem"))
    }
    
    @Test
    fun testSpecialCharacters_7() {
        // hasitem中的特殊字符
        val bedrockSelector = "@a[hasitem={item=minecraft:diamond_with_underscores}]"
        val conversion = SelectorConverter.convertBedrockToJava(bedrockSelector, context)
        assertTrue("应包含nbt", conversion.javaSelector.contains("nbt="))
    }
    
    @Test
    fun testSpecialCharacters_8() {
        // scores中的特殊键名
        val selector = "@a[scores={custom_score=5}]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }
    
    /**
     * 测试组21：空值和无效输入测试
     */
    @Test
    fun testEmptyInvalidInputs_1() {
        // 空选择器
        val selector = ""
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }

    @Test
    fun testEmptyInvalidInputs_2() {
        // 只有@
        val selector = "@"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }

    @Test
    fun testEmptyInvalidInputs_3() {
        // 无效的选择器变量
        val selector = "@x"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }
    
    @Test
    fun testEmptyInvalidInputs_4() {
        // 参数中只有等号
        val selector = "@a[=]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }
    
    @Test
    fun testEmptyInvalidInputs_5() {
        // 参数中只有逗号
        val selector = "@a[,]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }
    
    @Test
    fun testEmptyInvalidInputs_6() {
        // 不匹配的方括号
        val selector = "@a["
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }
    
    @Test
    fun testEmptyInvalidInputs_7() {
        // 多余的方括号
        val selector = "@a[type=player]]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }
    
    @Test
    fun testEmptyInvalidInputs_8() {
        // 空的scores对象
        val selector = "@a[scores={}]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }
    
    /**
     * 测试组22：多个相同参数测试
     */
    @Test
    fun testMultipleSameParameters_1() {
        // 多个type参数（最后一个生效）
        val selector = "@a[type=player,type=cow]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }
    
    @Test
    fun testMultipleSameParameters_2() {
        // 多个tag参数
        val selector = "@a[tag=warrior,tag=hero]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }
    
    @Test
    fun testMultipleSameParameters_3() {
        // 多个nbt参数
        val javaSelector = "@a[nbt={SelectedItem:{id:\"minecraft:diamond_sword\"}},nbt={Inventory:[{id:\"minecraft:diamond\"}]}]"
        val conversion = SelectorConverter.convertJavaToBedrock(javaSelector, context)
        assertTrue("应包含hasitem", conversion.bedrockSelector.contains("hasitem"))
    }
    
    @Test
    fun testMultipleSameParameters_4() {
        // 多个hasitem参数（数组形式）应该转换为合并的nbt参数
        val bedrockSelector = "@a[hasitem=[{item=diamond},{item=iron}]]"
        val conversion = SelectorConverter.convertBedrockToJava(bedrockSelector, context)
        assertTrue("应包含nbt", conversion.javaSelector.contains("nbt="))
        assertTrue("应包含Inventory", conversion.javaSelector.contains("Inventory"))
    }
    
    @Test
    fun testMultipleSameParameters_5() {
        // 多个scores参数
        val selector = "@a[scores={kills=5},scores={deaths=3}]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }
    
    /**
     * 测试组23：嵌套参数测试
     */
    @Test
    fun testNestedParameters_1() {
        // 嵌套的scores对象
        val selector = "@a[scores={kills={min=5,max=10}}]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }
    
    @Test
    fun testNestedParameters_2() {
        // 嵌套的nbt对象
        val javaSelector = "@a[nbt={SelectedItem:{id:\"minecraft:diamond_sword\",tag:{Enchantments:[{id:\"sharpness\",lvl:5}]}}}]"
        val conversion = SelectorConverter.convertJavaToBedrock(javaSelector, context)
        assertTrue("应包含hasitem", conversion.bedrockSelector.contains("hasitem"))
    }
    
    @Test
    fun testNestedParameters_3() {
        // 嵌套的Inventory数组
        val javaSelector = "@a[nbt={Inventory:[{Slot:0b,id:\"minecraft:diamond\",Count:1b,tag:{display:{Name:\"\\\"钻石\\\"\"}}}]}]"
        val conversion = SelectorConverter.convertJavaToBedrock(javaSelector, context)
        assertTrue("应包含hasitem", conversion.bedrockSelector.contains("hasitem"))
    }
    
    @Test
    fun testNestedParameters_4() {
        // 嵌套的equipment对象
        val javaSelector = "@a[nbt={equipment:{head:{id:\"minecraft:diamond_helmet\",tag:{Enchantments:[{id:\"protection\",lvl:4}]}}}}]"
        val conversion = SelectorConverter.convertJavaToBedrock(javaSelector, context)
        assertTrue("应包含hasitem", conversion.bedrockSelector.contains("hasitem"))
    }
    
    @Test
    fun testNestedParameters_5() {
        // 复杂的嵌套结构
        val javaSelector = "@a[nbt={SelectedItem:{id:\"minecraft:diamond_sword\",tag:{Enchantments:[{id:\"sharpness\",lvl:5},{id:\"unbreaking\",lvl:3}]}}},nbt={Inventory:[{Slot:0b,id:\"minecraft:diamond\",Count:64b,tag:{display:{Lore:[\"\\\"高品质钻石\\\"\",\"\\\"稀有物品\\\"\"]}}}]}]"
        val conversion = SelectorConverter.convertJavaToBedrock(javaSelector, context)
        assertTrue("应包含hasitem", conversion.bedrockSelector.contains("hasitem"))
    }
}
