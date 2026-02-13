package com.tellraw.app.util

import android.content.Context
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * 合并逻辑和JAVA/基岩混合模式测试
 * 测试参数合并逻辑、混合模式转换和混合模式下的合并逻辑
 */
@RunWith(RobolectricTestRunner::class)
class MergeLogicAndMixedModeTest {
    
    private lateinit var context: Context
    
    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        // 重置所有开关
        SelectorConverter.setJavaBedrockMixedModeEnabled(false)
        SelectorConverter.setMergeLogicMode(false)
    }
    
    /**
     * 测试组1：默认合并逻辑测试（选取差的绝对值最大的范围）
     */
    @Test
    fun testDefaultMergeLogic_1() {
        // 单值参数：取最大值
        val selector = "@a[x=8,x=9.5,y=5,y=6]"
        val conversion = SelectorConverter.convertBedrockToJava(selector, context)
        assertTrue("应包含x=9.5", conversion.javaSelector.contains("x=9.5"))
        assertTrue("应包含y=6", conversion.javaSelector.contains("y=6"))
        assertFalse("不应包含x=8", conversion.javaSelector.contains("x=8"))
        assertFalse("不应包含y=5", conversion.javaSelector.contains("y=5"))
    }
    
    @Test
    fun testDefaultMergeLogic_2() {
        // 基岩版特有参数转换为Java版参数后的合并逻辑
        val selector = "@a[rm=1,rm=3.5,rxm=-5.5,rxm=-1]"
        val conversion = SelectorConverter.convertBedrockToJava(selector, context)
        // rm=1,rm=3.5 转换为 distance=1..3.5（取最小值1，最大值3.5）
        // rxm=-5.5,rxm=-1 转换为 x_rotation=-5.5..-1（取最小值-5.5，最大值-1）
        assertTrue("应包含distance=1..3.5", conversion.javaSelector.contains("distance=1..3.5"))
        assertTrue("应包含x_rotation=-5.5..-1", conversion.javaSelector.contains("x_rotation=-5.5..-1"))
    }

    
    @Test
    fun testDefaultMergeLogic_3() {
        // 范围类型：选取差的绝对值最大的范围
        val selector = "@a[distance=5..7,distance=3..9]"
        val conversion = SelectorConverter.convertBedrockToJava(selector, context)
        // 5..7的差是2，3..9的差是6，应该选择3..9
        assertTrue("应包含distance=3..9", conversion.javaSelector.contains("distance=3..9"))
    }
    
    @Test
    fun testDefaultMergeLogic_4() {
        // x_rotation范围：选取差的绝对值最大的范围
        val selector = "@a[x_rotation=-45..45,x_rotation=-32..0]"
        val conversion = SelectorConverter.convertBedrockToJava(selector, context)
        // -45..45的差是90，-32..0的差是32，应该选择-45..45
        assertTrue("应包含x_rotation=-45..45", conversion.javaSelector.contains("x_rotation=-45..45"))
    }
    
    @Test
    fun testDefaultMergeLogic_5() {
        // 格式化数字：去除无意义的小数位
        val selector = "@a[x=9.00,y=9.50,z=9.555]"
        val conversion = SelectorConverter.convertBedrockToJava(selector, context)
        assertTrue("应包含x=9", conversion.javaSelector.contains("x=9"))
        assertTrue("应包含y=9.5", conversion.javaSelector.contains("y=9.5"))
        assertTrue("应包含z=9.555", conversion.javaSelector.contains("z=9.555"))
    }
    
    @Test
    @Test
    fun testDefaultMergeLogic_6() {
        // 基岩版特有参数转换为Java版参数后的合并逻辑：负数处理
        val selector = "@a[rx=30,rxm=-45,ry=90,rym=-90]"
        val conversion = SelectorConverter.convertBedrockToJava(selector, context)
        // rxm=-45, rx=30 转换为 x_rotation=-45..30（取最小值-45，最大值30）
        // rym=-90, ry=90 转换为 y_rotation=-90..90（取最小值-90，最大值90）
        assertTrue("应包含x_rotation=-45..30", conversion.javaSelector.contains("x_rotation=-45..30"))
        assertTrue("应包含y_rotation=-90..90", conversion.javaSelector.contains("y_rotation=-90..90"))
    }

    }
    
    @Test
    fun testDefaultMergeLogic_7() {
        // 负数处理：x, y, z
        val selector = "@a[x=-10,x=-5,y=-20,y=-15,z=-5,z=0]"
        val conversion = SelectorConverter.convertBedrockToJava(selector, context)
        assertTrue("应包含x=-5", conversion.javaSelector.contains("x=-5"))
        assertTrue("应包含y=-15", conversion.javaSelector.contains("y=-15"))
        assertTrue("应包含z=0", conversion.javaSelector.contains("z=0"))
    }
    
    @Test
    @Test
    fun testDefaultMergeLogic_8() {
        // 基岩版特有参数转换为Java版参数后的合并逻辑：c和l
        val selector = "@a[c=-10,l=-5,limit=-3]"
        val conversion = SelectorConverter.convertBedrockToJava(selector, context)
        // c=-10, limit=-3 转换为 limit=-3（取最大值-3）
        // l=-5 转换为 level=-5
        assertTrue("应包含limit=-3", conversion.javaSelector.contains("limit=-3"))
        assertTrue("应包含level=-5", conversion.javaSelector.contains("level=-5"))
    }

    }
    
    @Test
    fun testDefaultMergeLogic_9() {
        // 单边范围：只有上限或下限
        val selector = "@a[distance=5..,distance=..10]"
        val conversion = SelectorConverter.convertBedrockToJava(selector, context)
        // 5..的差是无限大，..10的差是10，应该选择5..
        assertTrue("应包含distance=5..", conversion.javaSelector.contains("distance=5.."))
    }
    
    @Test
    fun testDefaultMergeLogic_10() {
        // level范围：选取差的绝对值最大的范围
        val selector = "@a[level=5..10,level=3..15]"
        val conversion = SelectorConverter.convertBedrockToJava(selector, context)
        // 5..10的差是5，3..15的差是12，应该选择3..15
        assertTrue("应包含level=3..15", conversion.javaSelector.contains("level=3..15"))
    }
    
    @Test
    fun testDefaultMergeLogic_11() {
        // dx参数：取最大值
        val selector = "@a[dx=5,dx=10]"
        val conversion = SelectorConverter.convertBedrockToJava(selector, context)
        assertTrue("应包含dx=10", conversion.javaSelector.contains("dx=10"))
        assertFalse("不应包含dx=5", conversion.javaSelector.contains("dx=5"))
    }
    
    @Test
    fun testDefaultMergeLogic_12() {
        // dy参数：取最大值
        val selector = "@a[dy=3,dy=8]"
        val conversion = SelectorConverter.convertBedrockToJava(selector, context)
        assertTrue("应包含dy=8", conversion.javaSelector.contains("dy=8"))
        assertFalse("不应包含dy=3", conversion.javaSelector.contains("dy=3"))
    }
    
    @Test
    fun testDefaultMergeLogic_13() {
        // dz参数：取最大值
        val selector = "@a[dz=2,dz=15]"
        val conversion = SelectorConverter.convertBedrockToJava(selector, context)
        assertTrue("应包含dz=15", conversion.javaSelector.contains("dz=15"))
        assertFalse("不应包含dz=2", conversion.javaSelector.contains("dz=2"))
    }
    
    @Test
    fun testDefaultMergeLogic_14() {
        // dx, dy, dz负数处理：取最大值（负数中-2比-5大）
        val selector = "@a[dx=-5,dx=-2,dy=-10,dy=-3,dz=-8,dz=-1]"
        val conversion = SelectorConverter.convertBedrockToJava(selector, context)
        assertTrue("应包含dx=-2", conversion.javaSelector.contains("dx=-2"))
        assertTrue("应包含dy=-3", conversion.javaSelector.contains("dy=-3"))
        assertTrue("应包含dz=-1", conversion.javaSelector.contains("dz=-1"))
    }
    
    @Test
    fun testDefaultMergeLogic_15() {
        // dx, dy, dz格式化：去除无意义的小数位
        val selector = "@a[dx=9.00,dy=9.50,dz=9.555]"
        val conversion = SelectorConverter.convertBedrockToJava(selector, context)
        assertTrue("应包含dx=9", conversion.javaSelector.contains("dx=9"))
        assertTrue("应包含dy=9.5", conversion.javaSelector.contains("dy=9.5"))
        assertTrue("应包含dz=9.555", conversion.javaSelector.contains("dz=9.555"))
    }
    
    @Test
    fun testDefaultMergeLogic_16() {
        // dx, dy, dz混合正负数：取最大值
        val selector = "@a[dx=-5,dx=5,dy=-10,dy=10,dz=-3,dz=3]"
        val conversion = SelectorConverter.convertBedrockToJava(selector, context)
        assertTrue("应包含dx=5", conversion.javaSelector.contains("dx=5"))
        assertTrue("应包含dy=10", conversion.javaSelector.contains("dy=10"))
        assertTrue("应包含dz=3", conversion.javaSelector.contains("dz=3"))
    }
    
    /**
     * 测试组2：混合模式合并逻辑测试（取所有最小值的最小值和所有最大值的最大值）
     */
    @Test
    fun testMixedModeMergeLogic_1() {
        // 启用混合模式合并逻辑
        SelectorConverter.setMergeLogicMode(true)
        
        val selector = "@a[distance=1..5,distance=2..81]"
        val conversion = SelectorConverter.convertBedrockToJava(selector, context)
        // 应该取1..81（最小值1，最大值81）
        assertTrue("应包含distance=1..81", conversion.javaSelector.contains("distance=1..81"))
        
        // 重置
        SelectorConverter.setMergeLogicMode(false)
    }
    
    @Test
    fun testMixedModeMergeLogic_2() {
        // 启用混合模式合并逻辑
        SelectorConverter.setMergeLogicMode(true)
        
        val selector = "@a[x_rotation=-45..45,x_rotation=-32..0]"
        val conversion = SelectorConverter.convertBedrockToJava(selector, context)
        // 应该取-45..45（最小值-45，最大值45）
        assertTrue("应包含x_rotation=-45..45", conversion.javaSelector.contains("x_rotation=-45..45"))
        
        // 重置
        SelectorConverter.setMergeLogicMode(false)
    }
    
    @Test
    fun testMixedModeMergeLogic_3() {
        // 启用混合模式合并逻辑
        SelectorConverter.setMergeLogicMode(true)
        
        val selector = "@a[y_rotation=-90..90,y_rotation=0..180]"
        val conversion = SelectorConverter.convertBedrockToJava(selector, context)
        // 应该取-90..180（最小值-90，最大值180）
        assertTrue("应包含y_rotation=-90..180", conversion.javaSelector.contains("y_rotation=-90..180"))
        
        // 重置
        SelectorConverter.setMergeLogicMode(false)
    }
    
    @Test
    fun testMixedModeMergeLogic_4() {
        // 启用混合模式合并逻辑
        SelectorConverter.setMergeLogicMode(true)
        
        val selector = "@a[level=5..10,level=3..15,level=8..20]"
        val conversion = SelectorConverter.convertBedrockToJava(selector, context)
        // 应该取3..20（最小值3，最大值20）
        assertTrue("应包含level=3..20", conversion.javaSelector.contains("level=3..20"))
        
        // 重置
        SelectorConverter.setMergeLogicMode(false)
    }
    
    @Test
    fun testMixedModeMergeLogic_5() {
        // 启用混合模式合并逻辑
        SelectorConverter.setMergeLogicMode(true)
        
        val selector = "@a[distance=5..7,distance=3..9,distance=1..10]"
        val conversion = SelectorConverter.convertBedrockToJava(selector, context)
        // 应该取1..10（最小值1，最大值10）
        assertTrue("应包含distance=1..10", conversion.javaSelector.contains("distance=1..10"))
        
        // 重置
        SelectorConverter.setMergeLogicMode(false)
    }
    
    /**
     * 测试组3：JAVA/基岩混合模式基础测试
     */
    @Test
    fun testJavaBedrockMixedMode_1() {
        // Java版输出：@a[distance=..9,y_rotation=..150]（r=8转换为distance=..8，与distance=..9合并）
        assertTrue("应包含distance=..9", conversion.javaSelector.contains("distance=..9"))
        assertTrue("应包含y_rotation=..150", conversion.javaSelector.contains("y_rotation=..150"))
        // 基岩版输出：@a[r=9,ry=150]
        assertTrue("应包含r=9", conversion.bedrockSelector.contains("r=9"))
        assertTrue("应包含ry=150", conversion.bedrockSelector.contains("ry=150"))
        // Java版输出：@a[distance=..9,y_rotation=..150]（r=8转换为distance=..8，与distance=..9合并）
        assertTrue("应包含distance=..9", conversion.javaSelector.contains("distance=..9"))
        assertTrue("应包含y_rotation=..150", conversion.javaSelector.contains("y_rotation=..150"))
        // 基岩版输出：@a[r=9,ry=150]
        assertTrue("应包含r=9", conversion.bedrockSelector.contains("r=9"))
        assertTrue("应包含ry=150", conversion.bedrockSelector.contains("ry=150"))
        // Java版输出：@a[distance=..9,y_rotation=..150]（r=8转换为distance=..8，与distance=..9合并）
        assertTrue("应包含distance=..9", conversion.javaSelector.contains("distance=..9"))
        assertTrue("应包含y_rotation=..150", conversion.javaSelector.contains("y_rotation=..150"))
        // 基岩版输出：@a[r=9,ry=150]
        assertTrue("应包含r=9", conversion.bedrockSelector.contains("r=9"))
        assertTrue("应包含ry=150", conversion.bedrockSelector.contains("ry=150"))
        // Java版输出：@a[distance=..9,y_rotation=..150]（r=8转换为distance=..8，与distance=..9合并）
        assertTrue("应包含distance=..9", conversion.javaSelector.contains("distance=..9"))
        assertTrue("应包含y_rotation=..150", conversion.javaSelector.contains("y_rotation=..150"))
        // 基岩版输出：@a[r=9,ry=150]
        assertTrue("应包含r=9", conversion.bedrockSelector.contains("r=9"))
        assertTrue("应包含ry=150", conversion.bedrockSelector.contains("ry=150"))
        // Java版输出：@a[distance=..9,y_rotation=..150]（r=8转换为distance=..8，与distance=..9合并）
        assertTrue("应包含distance=..9", conversion.javaSelector.contains("distance=..9"))
        assertTrue("应包含y_rotation=..150", conversion.javaSelector.contains("y_rotation=..150"))
        // 基岩版输出：@a[r=9,ry=150]
        assertTrue("应包含r=9", conversion.bedrockSelector.contains("r=9"))
        assertTrue("应包含ry=150", conversion.bedrockSelector.contains("ry=150"))
        // Java版输出：@a[distance=..9,y_rotation=..150]（r=8转换为distance=..8，与distance=..9合并）
        assertTrue("应包含distance=..9", conversion.javaSelector.contains("distance=..9"))
        assertTrue("应包含y_rotation=..150", conversion.javaSelector.contains("y_rotation=..150"))
        // 基岩版输出：@a[r=9,ry=150]
        assertTrue("应包含r=9", conversion.bedrockSelector.contains("r=9"))
        assertTrue("应包含ry=150", conversion.bedrockSelector.contains("ry=150"))
        // Java版输出：@a[distance=..9,y_rotation=..150]（r=8转换为distance=..8，与distance=..9合并）
        assertTrue("应包含distance=..9", conversion.javaSelector.contains("distance=..9"))
        assertTrue("应包含y_rotation=..150", conversion.javaSelector.contains("y_rotation=..150"))
        // 基岩版输出：@a[r=9,ry=150]
        assertTrue("应包含r=9", conversion.bedrockSelector.contains("r=9"))
        assertTrue("应包含ry=150", conversion.bedrockSelector.contains("ry=150"))
        // Java版输出：@a[distance=..9,y_rotation=..150]（r=8转换为distance=..8，与distance=..9合并）
        assertTrue("应包含distance=..9", conversion.javaSelector.contains("distance=..9"))
        assertTrue("应包含y_rotation=..150", conversion.javaSelector.contains("y_rotation=..150"))
        // 基岩版输出：@a[r=9,ry=150]
        assertTrue("应包含r=9", conversion.bedrockSelector.contains("r=9"))
        assertTrue("应包含ry=150", conversion.bedrockSelector.contains("ry=150"))
        // Java版输出：@a[distance=..9,y_rotation=..150]（r=8转换为distance=..8，与distance=..9合并）
        assertTrue("应包含distance=..9", conversion.javaSelector.contains("distance=..9"))
        assertTrue("应包含y_rotation=..150", conversion.javaSelector.contains("y_rotation=..150"))
        // 基岩版输出：@a[r=9,ry=150]
        assertTrue("应包含r=9", conversion.bedrockSelector.contains("r=9"))
        assertTrue("应包含ry=150", conversion.bedrockSelector.contains("ry=150"))
        
        // 重置
        SelectorConverter.setJavaBedrockMixedModeEnabled(false)
    }
    
    @Test
    fun testJavaBedrockMixedMode_2() {
        // 启用JAVA/基岩混合模式
        SelectorConverter.setJavaBedrockMixedModeEnabled(true)
        
        // 同时包含nbt（Java版特有）和r（基岩版特有）
        val selector = "@a[r=9,nbt={Inventory:[{Slot:21b,id:\"minecraft:netherite_ingot\"}]}]"
        val conversion = SelectorConverter.convertBedrockToJava(selector, context)
        
        // 应该触发混合模式转换
        assertTrue("应包含提醒信息", conversion.javaReminders.any { it.contains("混合模式") })
        
        // 重置
        SelectorConverter.setJavaBedrockMixedModeEnabled(false)
    }
    
    @Test
    fun testJavaBedrockMixedMode_3() {
        // 启用JAVA/基岩混合模式
        SelectorConverter.setJavaBedrockMixedModeEnabled(true)
        
        // 同时包含hasitem（基岩版特有）和distance（Java版特有）
        val selector = "@a[hasitem={item=diamond},distance=10]"
        val conversion = SelectorConverter.convertJavaToBedrock(selector, context)
        
        // 应该触发混合模式转换
        assertTrue("应包含提醒信息", conversion.bedrockReminders.any { it.contains("混合模式") })
        
        // 重置
        SelectorConverter.setJavaBedrockMixedModeEnabled(false)
    }
    
    @Test
    fun testJavaBedrockMixedMode_4() {
        // 启用JAVA/基岩混合模式
        SelectorConverter.setJavaBedrockMixedModeEnabled(true)
        
        // 同时包含x_rotation（Java版特有）和rx/rxm（基岩版特有）
        val selector = "@a[rx=30,rxm=-45,x_rotation=-90..90]"
        val conversion = SelectorConverter.convertBedrockToJava(selector, context)
        
        // 应该触发混合模式转换
        assertTrue("应包含提醒信息", conversion.javaReminders.any { it.contains("混合模式") })
        
        // 重置
        SelectorConverter.setJavaBedrockMixedModeEnabled(false)
    }
    
    @Test
    fun testJavaBedrockMixedMode_5() {
        // 不启用JAVA/基岩混合模式
        SelectorConverter.setJavaBedrockMixedModeEnabled(false)
        
        // 同时包含Java版和基岩版特有参数
        val selector = "@a[r=8,distance=..9]"
        val conversion = SelectorConverter.convertBedrockToJava(selector, context)
        
        // 不应该触发混合模式转换，使用单向转换
        assertFalse("不应该包含混合模式提醒", conversion.javaReminders.any { it.contains("混合模式") })
    }
    
    /**
     * 测试组4：JAVA/基岩混合模式转换测试
     */
    @Test
    fun testJavaBedrockMixedModeConversion_1() {
        // 启用JAVA/基岩混合模式
        SelectorConverter.setJavaBedrockMixedModeEnabled(true)
        
        // 基岩版输入：r=8,distance=..9,y_rotation=..150
        // 预期Java版输出：保留distance和y_rotation，转换r为distance
        val selector = "@a[r=8,distance=..9,y_rotation=..150]"
        val conversion = SelectorConverter.convertBedrockToJava(selector, context)
        
        // 应该保留Java版参数
        assertTrue("应包含distance", conversion.javaSelector.contains("distance"))
        assertTrue("应包含y_rotation", conversion.javaSelector.contains("y_rotation"))
        
        // 重置
        SelectorConverter.setJavaBedrockMixedModeEnabled(false)
    }
    
    @Test
    fun testJavaBedrockMixedModeConversion_2() {
        // 启用JAVA/基岩混合模式
        SelectorConverter.setJavaBedrockMixedModeEnabled(true)
        
        // 基岩版输入：r=9,nbt={Inventory:[{Slot:21b,id:"minecraft:netherite_ingot"}]}
        // 预期基岩版输出：保留r，转换nbt为hasitem
        val selector = "@a[r=9,nbt={Inventory:[{Slot:21b,id:\"minecraft:netherite_ingot\"}]}]"
        val conversion = SelectorConverter.convertJavaToBedrock(selector, context)
        
        // 应该保留基岩版参数
        assertTrue("应包含r", conversion.bedrockSelector.contains("r="))
        // nbt应该被转换为hasitem
        assertTrue("应包含hasitem", conversion.bedrockSelector.contains("hasitem"))
        
        // 重置
        SelectorConverter.setJavaBedrockMixedModeEnabled(false)
    }
    
    @Test
    fun testJavaBedrockMixedModeConversion_3() {
        // 启用JAVA/基岩混合模式
        SelectorConverter.setJavaBedrockMixedModeEnabled(true)
        
        // Java版输入：distance=10,hasitem={item=diamond}
        // 预期基岩版输出：保留hasitem，转换distance为r
        val selector = "@a[distance=10,hasitem={item=diamond}]"
        val conversion = SelectorConverter.convertJavaToBedrock(selector, context)
        
        // 应该保留基岩版参数
        assertTrue("应包含hasitem", conversion.bedrockSelector.contains("hasitem"))
        // distance应该被转换为r
        assertTrue("应包含r", conversion.bedrockSelector.contains("r="))
        
        // 重置
        SelectorConverter.setJavaBedrockMixedModeEnabled(false)
    }
    
    /**
     * 测试组5：JAVA/基岩混合模式下的合并逻辑测试
     */
    @Test
    fun testMixedModeMergeLogicWithJavaBedrock_1() {
        // 启用JAVA/基岩混合模式
        SelectorConverter.setJavaBedrockMixedModeEnabled(true)
        
        // 同时包含distance参数，一个来自原选择器，一个来自转换后的基岩版参数
        val selector = "@a[distance=1..5,distance=2..81,r=8]"
        val conversion = SelectorConverter.convertBedrockToJava(selector, context)
        
        // 在混合模式下，合并应该使用混合模式合并逻辑（取1..81）
        assertTrue("应包含distance=1..81", conversion.javaSelector.contains("distance=1..81"))
        
        // 重置
        SelectorConverter.setJavaBedrockMixedModeEnabled(false)
    }
    
    @Test
    fun testMixedModeMergeLogicWithJavaBedrock_2() {
        // 启用JAVA/基岩混合模式
        SelectorConverter.setJavaBedrockMixedModeEnabled(true)
        
        // 同时包含x_rotation参数
        val selector = "@a[x_rotation=-45..45,x_rotation=-32..0,rx=30]"
        val conversion = SelectorConverter.convertBedrockToJava(selector, context)
        
        // 在混合模式下，合并应该使用混合模式合并逻辑（取-45..45）
        assertTrue("应包含x_rotation=-45..45", conversion.javaSelector.contains("x_rotation=-45..45"))
        
        // 重置
        SelectorConverter.setJavaBedrockMixedModeEnabled(false)
    }
    
    @Test
    fun testMixedModeMergeLogicWithJavaBedrock_3() {
        // 启用JAVA/基岩混合模式
        SelectorConverter.setJavaBedrockMixedModeEnabled(true)
        
        // 同时包含level参数
        val selector = "@a[level=5..10,level=3..15,l=10]"
        val conversion = SelectorConverter.convertBedrockToJava(selector, context)
        
        // 在混合模式下，合并应该使用混合模式合并逻辑（取3..15）
        assertTrue("应包含level=3..15", conversion.javaSelector.contains("level=3..15"))
        
        // 重置
        SelectorConverter.setJavaBedrockMixedModeEnabled(false)
    }
    
    @Test
    fun testMixedModeMergeLogicWithJavaBedrock_4() {
        // 启用JAVA/基岩混合模式
        SelectorConverter.setJavaBedrockMixedModeEnabled(true)
        
        // 同时包含y_rotation参数
        val selector = "@a[y_rotation=-90..90,y_rotation=0..180,ry=45]"
        val conversion = SelectorConverter.convertBedrockToJava(selector, context)
        
        // 在混合模式下，合并应该使用混合模式合并逻辑（取-90..180）
        assertTrue("应包含y_rotation=-90..180", conversion.javaSelector.contains("y_rotation=-90..180"))
        
        // 重置
        SelectorConverter.setJavaBedrockMixedModeEnabled(false)
    }
    
    @Test
    fun testMixedModeMergeLogicWithJavaBedrock_5() {
        // 启用JAVA/基岩混合模式
        SelectorConverter.setJavaBedrockMixedModeEnabled(true)
        
        // 同时包含distance参数，包含转换冲突
        val selector = "@a[distance=5..7,distance=3..9,r=10]"
        val conversion = SelectorConverter.convertBedrockToJava(selector, context)
        
        // 在混合模式下，合并应该使用混合模式合并逻辑（取3..9）
        assertTrue("应包含distance=3..9", conversion.javaSelector.contains("distance=3..9"))
        
        // 重置
        SelectorConverter.setJavaBedrockMixedModeEnabled(false)
    }
    
    /**
     * 测试组6：边界情况测试
     */
    @Test
    fun testEdgeCases_1() {
        // 只包含Java版特有参数，不应该触发混合模式
        SelectorConverter.setJavaBedrockMixedModeEnabled(true)
        
        val selector = "@a[distance=10,limit=5]"
        val conversion = SelectorConverter.convertJavaToBedrock(selector, context)
        
        // 不应该触发混合模式
        assertFalse("不应该包含混合模式提醒", conversion.bedrockReminders.any { it.contains("混合模式") })
        
        // 重置
        SelectorConverter.setJavaBedrockMixedModeEnabled(false)
    }
    
    @Test
    fun testEdgeCases_2() {
        // 只包含基岩版特有参数，不应该触发混合模式
        SelectorConverter.setJavaBedrockMixedModeEnabled(true)
        
        val selector = "@a[r=10,c=5]"
        val conversion = SelectorConverter.convertBedrockToJava(selector, context)
        
        // 不应该触发混合模式
        assertFalse("不应该包含混合模式提醒", conversion.javaReminders.any { it.contains("混合模式") })
        
        // 重置
        SelectorConverter.setJavaBedrockMixedModeEnabled(false)
    }
    
    @Test
    fun testEdgeCases_3() {
        // 只包含通用参数，不应该触发混合模式
        SelectorConverter.setJavaBedrockMixedModeEnabled(true)
        
        val selector = "@a[x=10,y=20,z=30,type=player]"
        val conversion = SelectorConverter.convertBedrockToJava(selector, context)
        
        // 不应该触发混合模式
        assertFalse("不应该包含混合模式提醒", conversion.javaReminders.any { it.contains("混合模式") })
        
        // 重置
        SelectorConverter.setJavaBedrockMixedModeEnabled(false)
    }
    
    @Test
    fun testEdgeCases_4() {
        // 空参数
        val selector = "@a"
        val conversion = SelectorConverter.convertBedrockToJava(selector, context)
        
        assertEquals("应该保持不变", "@a", conversion.javaSelector)
    }
    
    @Test
    fun testEdgeCases_5() {
        // 开关切换测试
        SelectorConverter.setMergeLogicMode(true)
        SelectorConverter.setMergeLogicMode(false)
        SelectorConverter.setMergeLogicMode(true)
        
        val selector = "@a[distance=1..5,distance=2..10]"
        val conversion = SelectorConverter.convertBedrockToJava(selector, context)
        
        // 应该使用混合模式合并逻辑
        assertTrue("应包含distance=1..10", conversion.javaSelector.contains("distance=1..10"))
        
        // 重置
        SelectorConverter.setMergeLogicMode(false)
    }
    
    /**
     * 测试组7：最大值参数合并逻辑补充测试（Java版输出）
     */
    @Test
    fun testMaxValueParamsMerge_1() {
        // distance参数：取最大值（单值）
        val selector = "@a[distance=5,distance=10]"
        val conversion = SelectorConverter.convertJavaToBedrock(selector, context)
        // distance转换为r，取最大值
        assertTrue("应包含r=10", conversion.bedrockSelector.contains("r=10"))
        assertFalse("不应包含r=5", conversion.bedrockSelector.contains("r=5"))
    }
    
    @Test
    fun testMaxValueParamsMerge_2() {
        // x_rotation参数：取最大值（单值）
        val selector = "@a[x_rotation=10,x_rotation=20]"
        val conversion = SelectorConverter.convertJavaToBedrock(selector, context)
        // x_rotation转换为rx，取最大值
        assertTrue("应包含rx=20", conversion.bedrockSelector.contains("rx=20"))
        assertFalse("不应包含rx=10", conversion.bedrockSelector.contains("rx=10"))
    }
    
    @Test
    fun testMaxValueParamsMerge_3() {
        // y_rotation参数：取最大值（单值）
        val selector = "@a[y_rotation=15,y_rotation=25]"
        val conversion = SelectorConverter.convertJavaToBedrock(selector, context)
        // y_rotation转换为ry，取最大值
        assertTrue("应包含ry=25", conversion.bedrockSelector.contains("ry=25"))
        assertFalse("不应包含ry=15", conversion.bedrockSelector.contains("ry=15"))
    }
    
    @Test
    fun testMaxValueParamsMerge_4() {
        // level参数：取最大值（单值）
        val selector = "@a[level=3,level=8]"
        val conversion = SelectorConverter.convertJavaToBedrock(selector, context)
        // level转换为l，取最大值
        assertTrue("应包含l=8", conversion.bedrockSelector.contains("l=8"))
        assertFalse("不应包含l=3", conversion.bedrockSelector.contains("l=3"))
    }
    
    @Test
    fun testMaxValueParamsMerge_5() {
        // limit参数：取最大值
        val selector = "@a[limit=3,limit=5]"
        val conversion = SelectorConverter.convertJavaToBedrock(selector, context)
        // limit转换为c，取最大值
        assertTrue("应包含c=5", conversion.bedrockSelector.contains("c=5"))
        assertFalse("不应包含c=3", conversion.bedrockSelector.contains("c=3"))
    }
    
    @Test
    fun testMaxValueParamsMerge_6() {
        // Java版通用参数的负数处理：x, y, z
        val selector = "@a[x=-10,x=-5,y=-20,y=-15,z=-5,z=0]"
        val conversion = SelectorConverter.convertBedrockToJava(selector, context)
        assertTrue("应包含x=-5", conversion.javaSelector.contains("x=-5"))
        assertTrue("应包含y=-15", conversion.javaSelector.contains("y=-15"))
        assertTrue("应包含z=0", conversion.javaSelector.contains("z=0"))
    }
    
    @Test
    fun testMaxValueParamsMerge_7() {
        // Java版通用参数的负数处理：dx, dy, dz
        val selector = "@a[dx=-5,dx=-2,dy=-10,dy=-3,dz=-8,dz=-1]"
        val conversion = SelectorConverter.convertBedrockToJava(selector, context)
        assertTrue("应包含dx=-2", conversion.javaSelector.contains("dx=-2"))
        assertTrue("应包含dy=-3", conversion.javaSelector.contains("dy=-3"))
        assertTrue("应包含dz=-1", conversion.javaSelector.contains("dz=-1"))
    }
    
    /**
     * 测试组8：Java版范围参数合并逻辑补充测试
     */
    @Test
    fun testMinValueParamsMerge_1() {
        // distance参数（范围）：使用默认合并逻辑，取所有最小值的最小值
        val selector = "@a[distance=5..10,distance=2..8]"
        val conversion = SelectorConverter.convertJavaToBedrock(selector, context)
        // distance转换为r和rm，取最小值2..10
        assertTrue("应包含rm=2", conversion.bedrockSelector.contains("rm=2"))
        assertTrue("应包含r=10", conversion.bedrockSelector.contains("r=10"))
    }
    
    @Test
    fun testMinValueParamsMerge_2() {
        // distance参数（范围）的负数处理
        val selector = "@a[distance=-10..-5,distance=-8..-2]"
        val conversion = SelectorConverter.convertJavaToBedrock(selector, context)
        // distance转换为r和rm，取最小值-10..-2
        assertTrue("应包含rm=-10", conversion.bedrockSelector.contains("rm=-10"))
        assertTrue("应包含r=-2", conversion.bedrockSelector.contains("r=-2"))
    }
    
    /**
     * 测试组9：多个参数同时重复的情况
     */
    @Test
    fun testMultipleParamsMerge_1() {
        // 多个参数同时重复：最大值类型（Java版通用参数）
        val selector = "@a[x=8,y=5,dx=10,dx=5]"
        val conversion = SelectorConverter.convertBedrockToJava(selector, context)
        assertTrue("应包含x=8", conversion.javaSelector.contains("x=8"))
        assertTrue("应包含y=5", conversion.javaSelector.contains("y=5"))
        assertTrue("应包含dx=10", conversion.javaSelector.contains("dx=10"))
        assertFalse("不应包含dx=5", conversion.javaSelector.contains("dx=5"))
    }
    
    @Test
    fun testMultipleParamsMerge_2() {
        // 多个参数同时重复：范围类型（Java版参数）
        val selector = "@a[distance=5..7,distance=3..9,x_rotation=-45..45,x_rotation=-30..30,y_rotation=-90..90,y_rotation=0..180,level=5..10,level=3..15]"
        val conversion = SelectorConverter.convertBedrockToJava(selector, context)
        assertTrue("应包含distance=3..9", conversion.javaSelector.contains("distance=3..9"))
        assertTrue("应包含x_rotation=-45..45", conversion.javaSelector.contains("x_rotation=-45..45"))
        assertTrue("应包含y_rotation=0..180", conversion.javaSelector.contains("y_rotation=-90..180"))
        assertTrue("应包含level=3..15", conversion.javaSelector.contains("level=3..15"))
    }
    
    @Test
    fun testMultipleParamsMerge_3() {
        // 多个参数同时重复：混合类型（最大值、最小值、范围类型）
        val selector = "@a[x=5,x=10,dx=3,dx=8,distance=3..5,distance=1..8]"
        val conversion = SelectorConverter.convertBedrockToJava(selector, context)
        assertTrue("应包含x=10", conversion.javaSelector.contains("x=10"))
        assertTrue("应包含dx=8", conversion.javaSelector.contains("dx=8"))
        assertTrue("应包含distance=1..8", conversion.javaSelector.contains("distance=1..8"))
    }
    
    @Test
    fun testMultipleParamsMerge_4() {
        // 混合类型参数：所有类型同时重复
        val selector = "@a[x=5,x=10,y=3,y=8,dx=2,dx=7,dy=1,dy=6,dz=4,dz=9,distance=5..7,distance=2..9,x_rotation=-45..45,x_rotation=-30..30,y_rotation=-90..90,y_rotation=0..180,level=5..10,level=3..15]"
        val conversion = SelectorConverter.convertBedrockToJava(selector, context)
        assertTrue("应包含x=10", conversion.javaSelector.contains("x=10"))
        assertTrue("应包含y=8", conversion.javaSelector.contains("y=8"))
        assertTrue("应包含dx=7", conversion.javaSelector.contains("dx=7"))
        assertTrue("应包含dy=6", conversion.javaSelector.contains("dy=6"))
        assertTrue("应包含dz=9", conversion.javaSelector.contains("dz=9"))
        assertTrue("应包含distance=2..9", conversion.javaSelector.contains("distance=2..9"))
        assertTrue("应包含x_rotation=-45..45", conversion.javaSelector.contains("x_rotation=-45..45"))
        assertTrue("应包含y_rotation=0..180", conversion.javaSelector.contains("y_rotation=-90..180"))
        assertTrue("应包含level=3..15", conversion.javaSelector.contains("level=3..15"))
    }
    
    /**
     * 测试组10：JAVA基岩混合模式下的各种合并情况
     */
    @Test
    fun testJavaBedrockMixedModeMerge_1() {
        // 启用JAVA/基岩混合模式
        SelectorConverter.setJavaBedrockMixedModeEnabled(true)
        
        // 同时包含distance参数（Java版）和r参数（基岩版）
        val selector = "@a[distance=5,r=10]"
        val conversion = SelectorConverter.convertBedrockToJava(selector, context)
        
        // 应该触发混合模式转换
        assertTrue("应包含提醒信息", conversion.javaReminders.any { it.contains("混合模式") })
        // r应该被转换为distance，并与原来的distance合并
        assertTrue("应包含distance参数", conversion.javaSelector.contains("distance"))
        
        // 重置
        SelectorConverter.setJavaBedrockMixedModeEnabled(false)
    }
    
    @Test
    fun testJavaBedrockMixedModeMerge_2() {
        // 启用JAVA/基岩混合模式
        SelectorConverter.setJavaBedrockMixedModeEnabled(true)
        
        // 同时包含x_rotation参数（Java版）和rx参数（基岩版）
        val selector = "@a[x_rotation=-45..45,rx=10]"
        val conversion = SelectorConverter.convertBedrockToJava(selector, context)
        
        // 应该触发混合模式转换
        assertTrue("应包含提醒信息", conversion.javaReminders.any { it.contains("混合模式") })
        // rx应该被转换为x_rotation，并与原来的x_rotation合并
        assertTrue("应包含x_rotation参数", conversion.javaSelector.contains("x_rotation"))
        
        // 重置
        SelectorConverter.setJavaBedrockMixedModeEnabled(false)
    }
    
    @Test
    fun testJavaBedrockMixedModeMerge_3() {
        // 启用JAVA/基岩混合模式
        SelectorConverter.setJavaBedrockMixedModeEnabled(true)
        
        // 同时包含level参数（Java版）和l参数（基岩版）
        val selector = "@a[level=5..15,l=10]"
        val conversion = SelectorConverter.convertBedrockToJava(selector, context)
        
        // 应该触发混合模式转换
        assertTrue("应包含提醒信息", conversion.javaReminders.any { it.contains("混合模式") })
        // l应该被转换为level，并与原来的level合并
        assertTrue("应包含level参数", conversion.javaSelector.contains("level"))
        
        // 重置
        SelectorConverter.setJavaBedrockMixedModeEnabled(false)
    }
    
    @Test
    fun testJavaBedrockMixedModeMerge_4() {
        // 启用JAVA/基岩混合模式
        SelectorConverter.setJavaBedrockMixedModeEnabled(true)
        
        // 同时包含limit参数（Java版）和c参数（基岩版）
        val selector = "@a[limit=3,c=5]"
        val conversion = SelectorConverter.convertBedrockToJava(selector, context)
        
        // 应该触发混合模式转换
        assertTrue("应包含提醒信息", conversion.javaReminders.any { it.contains("混合模式") })
        // c应该被转换为limit，并与原来的limit合并
        assertTrue("应包含limit参数", conversion.javaSelector.contains("limit"))
        
        // 重置
        SelectorConverter.setJavaBedrockMixedModeEnabled(false)
    }
    
    @Test
    fun testJavaBedrockMixedModeMerge_5() {
        // 启用JAVA/基岩混合模式
        SelectorConverter.setJavaBedrockMixedModeEnabled(true)
        
        // 同时包含y_rotation参数（Java版）和ry参数（基岩版）
        val selector = "@a[y_rotation=-90..90,ry=45]"
        val conversion = SelectorConverter.convertBedrockToJava(selector, context)
        
        // 应该触发混合模式转换
        assertTrue("应包含提醒信息", conversion.javaReminders.any { it.contains("混合模式") })
        // ry应该被转换为y_rotation，并与原来的y_rotation合并
        assertTrue("应包含y_rotation参数", conversion.javaSelector.contains("y_rotation"))
        
        // 重置
        SelectorConverter.setJavaBedrockMixedModeEnabled(false)
    }
    
    /**
     * 测试组11：所有参数的组合测试
     */
    @Test
    fun testAllParamsCombination_1() {
        // 所有最大值参数的组合（Java版通用参数）
        val selector = "@a[x=10,x=15,y=20,y=25,z=30,z=35,dx=5,dx=10,dy=8,dy=12,dz=3,dz=7,limit=7,limit=12]"
        val conversion = SelectorConverter.convertBedrockToJava(selector, context)
        assertTrue("应包含x=15", conversion.javaSelector.contains("x=15"))
        assertTrue("应包含y=25", conversion.javaSelector.contains("y=25"))
        assertTrue("应包含z=35", conversion.javaSelector.contains("z=35"))
        assertTrue("应包含dx=10", conversion.javaSelector.contains("dx=10"))
        assertTrue("应包含dy=12", conversion.javaSelector.contains("dy=12"))
        assertTrue("应包含dz=7", conversion.javaSelector.contains("dz=7"))
        assertTrue("应包含limit=12", conversion.javaSelector.contains("limit=12"))
    }
    
    @Test
    fun testAllParamsCombination_2() {
        // 所有范围参数的组合（Java版参数）
        val selector = "@a[distance=5..10,distance=3..12,x_rotation=-45..45,x_rotation=-30..60,y_rotation=-90..90,y_rotation=0..180,level=10..20,level=5..25]"
        val conversion = SelectorConverter.convertBedrockToJava(selector, context)
        assertTrue("应包含distance=3..12", conversion.javaSelector.contains("distance=3..12"))
        assertTrue("应包含x_rotation=-45..60", conversion.javaSelector.contains("x_rotation=-45..60"))
        assertTrue("应包含y_rotation=-90..180", conversion.javaSelector.contains("y_rotation=-90..180"))
        assertTrue("应包含level=5..25", conversion.javaSelector.contains("level=5..25"))
    }
    
    @Test
    fun testAllParamsCombination_3() {
        // 混合模式合并逻辑下的所有范围参数
        SelectorConverter.setMergeLogicMode(true)
        
        val selector = "@a[distance=5..10,distance=2..15,x_rotation=-45..45,x_rotation=-30..60,y_rotation=-90..90,y_rotation=0..180,level=10..20,level=5..25]"
        val conversion = SelectorConverter.convertBedrockToJava(selector, context)
        assertTrue("应包含distance=2..15", conversion.javaSelector.contains("distance=2..15"))
        assertTrue("应包含x_rotation=-45..60", conversion.javaSelector.contains("x_rotation=-45..60"))
        assertTrue("应包含y_rotation=-90..180", conversion.javaSelector.contains("y_rotation=-90..180"))
        assertTrue("应包含level=5..25", conversion.javaSelector.contains("level=5..25"))
        
        // 重置
        SelectorConverter.setMergeLogicMode(false)
    }
    
    @Test
    fun testAllParamsCombination_4() {
        // JAVA基岩混合模式下的所有参数组合
        SelectorConverter.setJavaBedrockMixedModeEnabled(true)
        
        val selector = "@a[distance=5..10,x_rotation=-45..45,limit=5,team=red]"
        val conversion = SelectorConverter.convertJavaToBedrock(selector, context)
        
        // 应该触发混合模式转换
        assertTrue("应包含提醒信息", conversion.bedrockReminders.any { it.contains("混合模式") })
        
        // 重置
        SelectorConverter.setJavaBedrockMixedModeEnabled(false)
    }
    
    @Test
    fun testAllParamsCombination_5() {
        // 完整组合：最大值、最小值、范围类型
        val selector = "@a[x=5,x=10,y=3,y=8,z=1,z=6,dx=2,dx=7,dy=4,dy=9,dz=3,dz=8,distance=5..10,distance=2..15,x_rotation=-45..45,x_rotation=-30..60,y_rotation=-90..90,y_rotation=0..180,level=10..20,level=5..25,limit=3,limit=7]"
        val conversion = SelectorConverter.convertBedrockToJava(selector, context)
        assertTrue("应包含x=10", conversion.javaSelector.contains("x=10"))
        assertTrue("应包含y=8", conversion.javaSelector.contains("y=8"))
        assertTrue("应包含z=6", conversion.javaSelector.contains("z=6"))
        assertTrue("应包含dx=7", conversion.javaSelector.contains("dx=7"))
        assertTrue("应包含dy=9", conversion.javaSelector.contains("dy=9"))
        assertTrue("应包含dz=8", conversion.javaSelector.contains("dz=8"))
        assertTrue("应包含distance=2..15", conversion.javaSelector.contains("distance=2..15"))
        assertTrue("应包含x_rotation=-45..60", conversion.javaSelector.contains("x_rotation=-45..60"))
        assertTrue("应包含y_rotation=-90..180", conversion.javaSelector.contains("y_rotation=-90..180"))
        assertTrue("应包含level=5..25", conversion.javaSelector.contains("level=5..25"))
        assertTrue("应包含limit=7", conversion.javaSelector.contains("limit=7"))
    }
}
