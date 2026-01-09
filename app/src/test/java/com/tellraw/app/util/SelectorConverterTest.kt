package com.tellraw.app.util

import com.tellraw.app.model.SelectorType
import org.junit.Assert.*
import org.junit.Test

/**
 * SelectorConverter 测试类
 * 测试选择器参数的转换功能，包括Java版和基岩版之间的转换
 */
class SelectorConverterTest {

    // ==================== 选择器类型检测测试 ====================

    @Test
    fun `test detectSelectorType - 基岩版特有选择器变量`() {
        // 测试基岩版特有选择器变量
        assertEquals(SelectorType.BEDROCK, SelectorConverter.detectSelectorType("@initiator"))
        assertEquals(SelectorType.BEDROCK, SelectorConverter.detectSelectorType("@c"))
        assertEquals(SelectorType.BEDROCK, SelectorConverter.detectSelectorType("@v"))
    }

    @Test
    fun `test detectSelectorType - Java版特有参数`() {
        // 测试Java版特有参数
        assertEquals(SelectorType.JAVA, SelectorConverter.detectSelectorType("@a[distance=5]"))
        assertEquals(SelectorType.JAVA, SelectorConverter.detectSelectorType("@a[x_rotation=0..90]"))
        assertEquals(SelectorType.JAVA, SelectorConverter.detectSelectorType("@a[level=10]"))
        assertEquals(SelectorType.JAVA, SelectorConverter.detectSelectorType("@a[gamemode=survival]"))
    }

    @Test
    fun `test detectSelectorType - 基岩版特有参数`() {
        // 测试基岩版特有参数
        assertEquals(SelectorType.BEDROCK, SelectorConverter.detectSelectorType("@a[r=5]"))
        assertEquals(SelectorType.BEDROCK, SelectorConverter.detectSelectorType("@a[rx=0..90]"))
        assertEquals(SelectorType.BEDROCK, SelectorConverter.detectSelectorType("@a[l=10]"))
        assertEquals(SelectorType.BEDROCK, SelectorConverter.detectSelectorType("@a[m=0]"))
    }

    @Test
    fun `test detectSelectorType - 通用参数`() {
        // 测试通用参数（两个版本都支持的参数）
        assertEquals(SelectorType.BEDROCK, SelectorConverter.detectSelectorType("@a[x=10,y=20,z=30]"))
        assertEquals(SelectorType.BEDROCK, SelectorConverter.detectSelectorType("@a[name=Steve]"))
        assertEquals(SelectorType.BEDROCK, SelectorConverter.detectSelectorType("@a[type=zombie]"))
    }

    @Test
    fun `test detectSelectorType - 无参数`() {
        // 测试没有参数的选择器
        assertEquals(SelectorType.BEDROCK, SelectorConverter.detectSelectorType("@a"))
        assertEquals(SelectorType.BEDROCK, SelectorConverter.detectSelectorType("@p"))
        assertEquals(SelectorType.BEDROCK, SelectorConverter.detectSelectorType("@r"))
    }

    // ==================== 基岩版到Java版转换测试 ====================

    @Test
    fun `test convertBedrockToJava - 基岩版特有选择器变量转换`() {
        // 测试@initiator转换为@a
        val result1 = SelectorConverter.convertBedrockToJava("@initiator")
        assertEquals("@a", result1.javaSelector)
        assertTrue(result1.wasConverted)
        assertTrue(result1.javaReminders.isNotEmpty())

        // 测试@c转换为@a
        val result2 = SelectorConverter.convertBedrockToJava("@c")
        assertEquals("@a", result2.javaSelector)
        assertTrue(result2.wasConverted)

        // 测试@v转换为@a
        val result3 = SelectorConverter.convertBedrockToJava("@v")
        assertEquals("@a", result3.javaSelector)
        assertTrue(result3.wasConverted)
    }

    @Test
    fun `test convertBedrockToJava - 基岩版特有选择器变量带参数`() {
        // 测试@initiator带参数
        val result = SelectorConverter.convertBedrockToJava("@initiator[r=5]")
        assertEquals("@a[r=5]", result.javaSelector)
        assertTrue(result.wasConverted)
    }

    // ==================== 参数过滤测试 ====================

    @Test
    fun `test filterSelectorParameters - Java版到基岩版过滤Java特有参数`() {
        // 测试过滤Java版特有参数
        val result = SelectorConverter.filterSelectorParameters(
            "@a[distance=5,predicate=test,team=red]",
            SelectorType.BEDROCK
        )
        
        // distance应该被转换为r=5,r=5
        assertTrue(result.first.contains("r=5"))
        // predicate应该被移除
        assertFalse(result.first.contains("predicate"))
        // team应该被移除
        assertFalse(result.first.contains("team"))
        // 应该有提醒信息
        assertTrue(result.second.isNotEmpty())
    }

    @Test
    fun `test filterSelectorParameters - 基岩版到Java版过滤基岩特有参数`() {
        // 测试过滤基岩版特有参数
        val result = SelectorConverter.filterSelectorParameters(
            "@a[r=5,haspermission=op,family=monster]",
            SelectorType.JAVA
        )
        
        // haspermission应该被移除
        assertFalse(result.first.contains("haspermission"))
        // family应该被移除
        assertFalse(result.first.contains("family"))
        // 应该有提醒信息
        assertTrue(result.second.isNotEmpty())
    }

    @Test
    fun `test filterSelectorParameters - 保留通用参数`() {
        // 测试保留通用参数
        val result = SelectorConverter.filterSelectorParameters(
            "@a[x=10,y=20,z=30,name=Steve,type=zombie]",
            SelectorType.JAVA
        )
        
        // 所有通用参数都应该保留
        assertTrue(result.first.contains("x=10"))
        assertTrue(result.first.contains("y=20"))
        assertTrue(result.first.contains("z=30"))
        assertTrue(result.first.contains("name=Steve"))
        assertTrue(result.first.contains("type=zombie"))
    }

    // ==================== distance参数转换测试 ====================

    @Test
    fun `test convertDistanceParameters - 范围值转换`() {
        // 测试范围值转换
        val result = SelectorConverter.filterSelectorParameters(
            "@a[distance=5..10]",
            SelectorType.BEDROCK
        )
        
        // distance=5..10应该转换为rm=5,r=10
        assertTrue(result.first.contains("rm=5"))
        assertTrue(result.first.contains("r=10"))
        assertFalse(result.first.contains("distance"))
    }

    @Test
    fun `test convertDistanceParameters - 只有下限`() {
        // 测试只有下限
        val result = SelectorConverter.filterSelectorParameters(
            "@a[distance=5..]",
            SelectorType.BEDROCK
        )
        
        // distance=5..应该转换为rm=5
        assertTrue(result.first.contains("rm=5"))
        assertFalse(result.first.contains("distance"))
    }

    @Test
    fun `test convertDistanceParameters - 只有上限`() {
        // 测试只有上限
        val result = SelectorConverter.filterSelectorParameters(
            "@a[distance=..10]",
            SelectorType.BEDROCK
        )
        
        // distance=..10应该转换为r=10
        assertTrue(result.first.contains("r=10"))
        assertFalse(result.first.contains("distance"))
    }

    @Test
    fun `test convertDistanceParameters - 单个值`() {
        // 测试单个值
        val result = SelectorConverter.filterSelectorParameters(
            "@a[distance=10]",
            SelectorType.BEDROCK
        )
        
        // distance=10应该转换为rm=10,r=10
        assertTrue(result.first.contains("rm=10"))
        assertTrue(result.first.contains("r=10"))
        assertFalse(result.first.contains("distance"))
    }

    // ==================== x_rotation参数转换测试 ====================

    @Test
    fun `test convertRotationParameters - x_rotation范围值转换`() {
        // 测试x_rotation范围值转换
        val result = SelectorConverter.filterSelectorParameters(
            "@a[x_rotation=-45..45]",
            SelectorType.BEDROCK
        )
        
        // x_rotation=-45..45应该转换为rxm=-45,rx=45
        assertTrue(result.first.contains("rxm=-45"))
        assertTrue(result.first.contains("rx=45"))
        assertFalse(result.first.contains("x_rotation"))
    }

    @Test
    fun `test convertRotationParameters - y_rotation范围值转换`() {
        // 测试y_rotation范围值转换
        val result = SelectorConverter.filterSelectorParameters(
            "@a[y_rotation=-90..90]",
            SelectorType.BEDROCK
        )
        
        // y_rotation=-90..90应该转换为rym=-90,ry=90
        assertTrue(result.first.contains("rym=-90"))
        assertTrue(result.first.contains("ry=90"))
        assertFalse(result.first.contains("y_rotation"))
    }

    // ==================== level参数转换测试 ====================

    @Test
    fun `test convertLevelParameters - level范围值转换`() {
        // 测试level范围值转换
        val result = SelectorConverter.filterSelectorParameters(
            "@a[level=5..10]",
            SelectorType.BEDROCK
        )
        
        // level=5..10应该转换为lm=5,l=10
        assertTrue(result.first.contains("lm=5"))
        assertTrue(result.first.contains("l=10"))
        assertFalse(result.first.contains("level"))
    }

    @Test
    fun `test convertLevelParameters - level单个值`() {
        // 测试level单个值
        val result = SelectorConverter.filterSelectorParameters(
            "@a[level=10]",
            SelectorType.BEDROCK
        )
        
        // level=10应该转换为lm=10,l=10
        assertTrue(result.first.contains("lm=10"))
        assertTrue(result.first.contains("l=10"))
        assertFalse(result.first.contains("level"))
    }

    // ==================== gamemode参数转换测试 ====================

    @Test
    fun `test convertGamemodeToM - survival转换`() {
        // 测试survival转换
        val result = SelectorConverter.filterSelectorParameters(
            "@a[gamemode=survival]",
            SelectorType.BEDROCK
        )
        
        // gamemode=survival应该转换为m=0
        assertTrue(result.first.contains("m=0"))
        assertFalse(result.first.contains("gamemode"))
    }

    @Test
    fun `test convertGamemodeToM - creative转换`() {
        // 测试creative转换
        val result = SelectorConverter.filterSelectorParameters(
            "@a[gamemode=creative]",
            SelectorType.BEDROCK
        )
        
        // gamemode=creative应该转换为m=1
        assertTrue(result.first.contains("m=1"))
        assertFalse(result.first.contains("gamemode"))
    }

    @Test
    fun `test convertGamemodeToM - adventure转换`() {
        // 测试adventure转换
        val result = SelectorConverter.filterSelectorParameters(
            "@a[gamemode=adventure]",
            SelectorType.BEDROCK
        )
        
        // gamemode=adventure应该转换为m=2
        assertTrue(result.first.contains("m=2"))
        assertFalse(result.first.contains("gamemode"))
    }

    @Test
    fun `test convertGamemodeToM - spectator转换`() {
        // 测试spectator转换（基岩版没有旁观模式，转换为生存模式）
        val result = SelectorConverter.filterSelectorParameters(
            "@a[gamemode=spectator]",
            SelectorType.BEDROCK
        )
        
        // gamemode=spectator应该转换为m=0（生存模式）
        assertTrue(result.first.contains("m=0"))
        assertFalse(result.first.contains("gamemode"))
    }

    // ==================== sort和limit参数转换测试 ====================

    @Test
    fun `test sortAndLimitConversion - sort=nearest with limit`() {
        // 测试sort=nearest with limit
        val result = SelectorConverter.filterSelectorParameters(
            "@a[limit=5,sort=nearest]",
            SelectorType.BEDROCK
        )
        
        // sort=nearest,limit=5应该转换为c=5
        assertTrue(result.first.contains("c=5"))
        assertFalse(result.first.contains("sort"))
        assertFalse(result.first.contains("limit"))
    }

    @Test
    fun `test sortAndLimitConversion - sort=nearest without limit`() {
        // 测试sort=nearest without limit
        val result = SelectorConverter.filterSelectorParameters(
            "@a[sort=nearest]",
            SelectorType.BEDROCK
        )
        
        // sort=nearest应该转换为c=9999
        assertTrue(result.first.contains("c=9999"))
        assertFalse(result.first.contains("sort"))
    }

    @Test
    fun `test sortAndLimitConversion - sort=furthest with limit`() {
        // 测试sort=furthest with limit
        val result = SelectorConverter.filterSelectorParameters(
            "@a[limit=5,sort=furthest]",
            SelectorType.BEDROCK
        )
        
        // sort=furthest,limit=5应该转换为c=-5
        assertTrue(result.first.contains("c=-5"))
        assertFalse(result.first.contains("sort"))
        assertFalse(result.first.contains("limit"))
    }

    @Test
    fun `test sortAndLimitConversion - sort=furthest without limit`() {
        // 测试sort=furthest without limit
        val result = SelectorConverter.filterSelectorParameters(
            "@a[sort=furthest]",
            SelectorType.BEDROCK
        )
        
        // sort=furthest应该转换为c=-9999
        assertTrue(result.first.contains("c=-9999"))
        assertFalse(result.first.contains("sort"))
    }

    @Test
    fun `test sortAndLimitConversion - sort=random with limit`() {
        // 测试sort=random with limit
        val result = SelectorConverter.filterSelectorParameters(
            "@a[limit=5,sort=random]",
            SelectorType.BEDROCK
        )
        
        // sort=random,limit=5应该转换为c=5
        assertTrue(result.first.contains("c=5"))
        assertFalse(result.first.contains("sort"))
        assertFalse(result.first.contains("limit"))
    }

    @Test
    fun `test sortAndLimitConversion - sort=random without limit`() {
        // 测试sort=random without limit
        val result = SelectorConverter.filterSelectorParameters(
            "@a[sort=random]",
            SelectorType.BEDROCK
        )
        
        // sort=random应该转换为c=9999
        assertTrue(result.first.contains("c=9999"))
        assertFalse(result.first.contains("sort"))
    }

    @Test
    fun `test sortAndLimitConversion - sort=arbitrary`() {
        // 测试sort=arbitrary
        val result = SelectorConverter.filterSelectorParameters(
            "@a[sort=arbitrary]",
            SelectorType.BEDROCK
        )
        
        // sort=arbitrary应该被移除
        assertFalse(result.first.contains("sort"))
    }

    @Test
    fun `test sortAndLimitConversion - limit without sort`() {
        // 测试limit without sort
        val result = SelectorConverter.filterSelectorParameters(
            "@a[limit=5]",
            SelectorType.BEDROCK
        )
        
        // limit=5应该转换为c=5
        assertTrue(result.first.contains("c=5"))
        assertFalse(result.first.contains("limit"))
    }

    // ==================== scores参数转换测试 ====================

    @Test
    fun `test scoresParameter - level参数转换`() {
        // 测试scores中的level参数转换
        val result = SelectorConverter.filterSelectorParameters(
            "@a[scores={level=10}]",
            SelectorType.BEDROCK
        )
        
        // scores={level=10}应该转换为scores={lm=10,l=10}
        assertTrue(result.first.contains("lm=10"))
        assertTrue(result.first.contains("l=10"))
    }

    @Test
    fun `test scoresParameter - 复杂scores`() {
        // 测试复杂的scores参数
        val result = SelectorConverter.filterSelectorParameters(
            "@a[scores={kills=5,level=10,deaths=3}]",
            SelectorType.BEDROCK
        )
        
        // level应该被转换，其他参数应该保留
        assertTrue(result.first.contains("kills=5"))
        assertTrue(result.first.contains("lm=10"))
        assertTrue(result.first.contains("l=10"))
        assertTrue(result.first.contains("deaths=3"))
    }

    // ==================== 复合参数转换测试 ====================

    @Test
    fun `test complexConversion - 多个参数同时转换`() {
        // 测试多个参数同时转换
        val result = SelectorConverter.filterSelectorParameters(
            "@a[distance=5..10,x_rotation=-45..45,level=10,gamemode=survival]",
            SelectorType.BEDROCK
        )
        
        // 所有参数都应该被正确转换
        assertTrue(result.first.contains("rm=5"))
        assertTrue(result.first.contains("r=10"))
        assertTrue(result.first.contains("rxm=-45"))
        assertTrue(result.first.contains("rx=45"))
        assertTrue(result.first.contains("lm=10"))
        assertTrue(result.first.contains("l=10"))
        assertTrue(result.first.contains("m=0"))
        
        // 原始参数应该被移除
        assertFalse(result.first.contains("distance"))
        assertFalse(result.first.contains("x_rotation"))
        assertFalse(result.first.contains("level"))
        assertFalse(result.first.contains("gamemode"))
    }

    @Test
    fun `test complexConversion - 通用参数和转换参数混合`() {
        // 测试通用参数和转换参数混合
        val result = SelectorConverter.filterSelectorParameters(
            "@a[x=10,y=20,z=30,distance=5,name=Steve,gamemode=creative]",
            SelectorType.BEDROCK
        )
        
        // 通用参数应该保留
        assertTrue(result.first.contains("x=10"))
        assertTrue(result.first.contains("y=20"))
        assertTrue(result.first.contains("z=30"))
        assertTrue(result.first.contains("name=Steve"))
        
        // 转换参数应该被转换
        assertTrue(result.first.contains("rm=5"))
        assertTrue(result.first.contains("r=5"))
        assertTrue(result.first.contains("m=1"))
    }

    // ==================== 边界情况测试 ====================

    @Test
    fun `test edgeCases - 空参数`() {
        // 测试空参数
        val result = SelectorConverter.filterSelectorParameters(
            "@a[]",
            SelectorType.BEDROCK
        )
        
        assertEquals("@a", result.first)
    }

    @Test
    fun `test edgeCases - 只有无效参数`() {
        // 测试只有无效参数
        val result = SelectorConverter.filterSelectorParameters(
            "@a[predicate=test,team=red]",
            SelectorType.BEDROCK
        )
        
        // 所有参数都应该被移除
        assertEquals("@a", result.first)
        assertTrue(result.second.isNotEmpty())
    }

    @Test
    fun `test edgeCases - 大括号内的参数`() {
        // 测试包含大括号的参数（如scores）
        val result = SelectorConverter.filterSelectorParameters(
            "@a[scores={kills=5,deaths=3}]",
            SelectorType.JAVA
        )
        
        // scores参数应该被保留
        assertTrue(result.first.contains("scores={kills=5,deaths=3}"))
    }

    @Test
    fun `test edgeCases - 包含空格的参数`() {
        // 测试包含空格的参数
        val result = SelectorConverter.filterSelectorParameters(
            "@a[name=Steve Jobs]",
            SelectorType.JAVA
        )
        
        // 参数应该被保留
        assertTrue(result.first.contains("name=Steve Jobs"))
    }

    @Test
    fun `test edgeCases - 负数参数`() {
        // 测试负数参数
        val result = SelectorConverter.filterSelectorParameters(
            "@a[x=-10,y=-20,z=-30]",
            SelectorType.JAVA
        )
        
        // 负数参数应该被保留
        assertTrue(result.first.contains("x=-10"))
        assertTrue(result.first.contains("y=-20"))
        assertTrue(result.first.contains("z=-30"))
    }
}