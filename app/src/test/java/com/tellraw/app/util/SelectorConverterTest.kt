package com.tellraw.app.util

import com.tellraw.app.TestApplication
import com.tellraw.app.model.SelectorType
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import android.content.Context
import androidx.test.core.app.ApplicationProvider

/**
 * 选择器转换和命令生成测试
 * 测试各种选择器参数的组合和转换
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [28],
    application = TestApplication::class,
    packageName = "com.tellraw.app"
)
class SelectorConverterTest {
    private val context: Context = ApplicationProvider.getApplicationContext<TestApplication>()
    
    /**
     * 测试组1：通用选择器参数（8~9个参数）
     * 测试Java版和基岩版都支持的参数
     */
    @Test
    fun testUniversalParameters_1() {
        val selector = "@e[x=10,y=20,z=30,dx=5,dy=5,dz=5,scores={test=10},tag=player,name=Steve,type=zombie]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }
    
    @Test
    fun testUniversalParameters_2() {
        val selector = "@a[x=0,y=64,z=0,dx=100,dy=50,dz=100,scores={kills=5..10},tag=team_red,name=\"\",type=player]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }
    
    @Test
    fun testUniversalParameters_3() {
        val selector = "@p[x=100,y=70,z=100,dx=20,dy=10,dz=20,scores={health=20},tag=!enemy,name=Alex,type=player]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }
    
    @Test
    fun testUniversalParameters_4() {
        val selector = "@e[x=50,y=50,z=50,dx=30,dy=30,dz=30,scores={score=0..},tag=,name=!Boss,type=!zombie]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }
    
    @Test
    fun testUniversalParameters_5() {
        val selector = "@a[x=~10,y=~10,z=~10,dx=15,dy=15,dz=15,scores={money=100},tag=shop,name=Trader,type=villager]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }
    
    @Test
    fun testUniversalParameters_6() {
        val selector = "@e[x=0,y=0,z=0,dx=10,dy=256,dz=10,scores={level=30},tag=admin,name=Admin,type=player]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }
    
    @Test
    fun testUniversalParameters_7() {
        val selector = "@p[x=100,y=100,z=100,dx=5,dy=5,dz=5,scores={xp=50..100},tag=friend,name=Friend,type=player]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }
    
    @Test
    fun testUniversalParameters_8() {
        val selector = "@a[x=-100,y=-50,z=-100,dx=50,dy=100,dz=50,scores={rank=1},tag=vip,name=VIP,type=player]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }
    
    @Test
    fun testUniversalParameters_9() {
        val selector = "@e[x=10.5,y=20.3,z=30.7,dx=5.2,dy=5.8,dz=5.1,scores={test=10..20},tag=a,tag=b,name=Test,type=!player]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }
    
    /**
     * 测试组2：通用选择器参数（5~6个）+ Java版选择器参数（3~4个）
     */
    @Test
    fun testUniversalWithJavaParameters_1() {
        val selector = "@e[x=10,y=20,z=30,dx=5,dy=5,dz=5,distance=10,team=red,gamemode=survival,sort=nearest]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    @Test
    fun testUniversalWithJavaParameters_2() {
        val selector = "@a[x=0,y=64,z=0,dx=100,dy=50,dz=100,distance=5..20,team=blue,gamemode=creative,sort=random]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    @Test
    fun testUniversalWithJavaParameters_3() {
        val selector = "@p[x=100,y=70,z=100,dx=20,dy=10,dz=20,distance=..15,team=!enemy,gamemode=adventure,sort=furthest]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    @Test
    fun testUniversalWithJavaParameters_4() {
        val selector = "@e[x=50,y=50,z=50,dx=30,dy=30,dz=30,distance=10..,team=,gamemode=spectator,limit=5]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    @Test
    fun testUniversalWithJavaParameters_5() {
        val selector = "@a[x=~10,y=~10,z=~10,dx=15,dy=15,dz=15,distance=..50,team=!red,gamemode=survival,sort=arbitrary]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    @Test
    fun testUniversalWithJavaParameters_6() {
        val selector = "@e[x=0,y=0,z=0,dx=10,dy=256,dz=10,distance=1..100,team=green,gamemode=!survival,limit=10,sort=random]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    /**
     * 测试组3：通用选择器参数（5~6个）+ 基岩版选择器参数（3~4个）
     */
    @Test
    fun testUniversalWithBedrockParameters_1() {
        val selector = "@e[x=10,y=20,z=30,dx=5,dy=5,dz=5,r=10,m=0,c=5]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    @Test
    fun testUniversalWithBedrockParameters_2() {
        val selector = "@a[x=0,y=64,z=0,dx=100,dy=50,dz=100,rm=5,r=20,m=1,c=3]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    @Test
    fun testUniversalWithBedrockParameters_3() {
        val selector = "@p[x=100,y=70,z=100,dx=20,dy=10,dz=20,r=15,m=2,c=1]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    @Test
    fun testUniversalWithBedrockParameters_4() {
        val selector = "@e[x=50,y=50,z=50,dx=30,dy=30,dz=30,rm=10,r=50,m=s,c=10]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    @Test
    fun testUniversalWithBedrockParameters_5() {
        val selector = "@a[x=~10,y=~10,z=~10,dx=15,dy=15,dz=15,r=0,m=!0,c=-5]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    @Test
    fun testUniversalWithBedrockParameters_6() {
        val selector = "@e[x=0,y=0,z=0,dx=10,dy=256,dz=10,rm=1,r=100,m=survival,c=8]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    /**
     * 测试组4：Java版独有参数组合
     */
    @Test
    fun testJavaOnlyParameters_1() {
        val selector = "@a[distance=10,team=red,gamemode=survival,level=5..10,sort=nearest]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    @Test
    fun testJavaOnlyParameters_2() {
        val selector = "@e[x_rotation=0..45,y_rotation=-90..90,nbt={CustomName:\"Steve\"},limit=5]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    @Test
    fun testJavaOnlyParameters_3() {
        val selector = "@a[advancements={story/form_obsidian=true},predicate=test:test,gamemode=adventure]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    @Test
    fun testJavaOnlyParameters_4() {
        val selector = "@e[distance=..50,x_rotation=-30..30,y_rotation=0..180,limit=10,sort=furthest]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    @Test
    fun testJavaOnlyParameters_5() {
        val selector = "@a[team=!red,team=!blue,gamemode=!survival,level=10..,sort=random]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    @Test
    fun testJavaOnlyParameters_6() {
        val selector = "@e[distance=5..15,x_rotation=-45..45,y_rotation=-90..90,nbt={Tags:[\"a\",\"b\"]},limit=3,sort=arbitrary]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    /**
     * 测试组5：基岩版独有参数组合
     */
    @Test
    fun testBedrockOnlyParameters_1() {
        val selector = "@a[r=10,m=0,c=5,l=5..10,lm=5]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    @Test
    fun testBedrockOnlyParameters_2() {
        val selector = "@e[family=zombie,hasitem={item=diamond_sword},rx=-45..45,ry=0..90]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    @Test
    fun testBedrockOnlyParameters_3() {
        val selector = "@a[haspermission={movement=enabled},m=1,c=3,l=10]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    @Test
    fun testBedrockOnlyParameters_4() {
        val selector = "@e[family=!monster,hasitem=[{item=apple,quantity=5..}],rxm=-90,rx=90]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    @Test
    fun testBedrockOnlyParameters_5() {
        val selector = "@a[has_property={minecraft:has_nectar=true},m=2,c=1,lm=20]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    @Test
    fun testBedrockOnlyParameters_6() {
        val selector = "@e[family=undead,hasitem={item=diamond,quantity=10},rxm=-30,rx=30,rym=0,ry=180]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    /**
     * 测试组6：特殊组合和边界情况
     */
    @Test
    fun testSpecialCases_1() {
        // 空选择器
        val selector = ""
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }
    
    @Test
    fun testSpecialCases_2() {
        // 只有变量
        val selector = "@a"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }
    
    @Test
    fun testSpecialCases_3() {
        // 嵌套选择器（基岩版）
        val selector = "@e[type=player,m=0,r=10,c=1]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    @Test
    fun testSpecialCases_4() {
        // 多个否定参数
        val selector = "@e[type=!zombie,type=!skeleton,tag=!enemy,name=!Boss]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }
    
    @Test
    fun testSpecialCases_5() {
        // 范围参数组合
        val selector = "@a[scores={test=1..10,foo=5..,bar=..20},level=5..15,distance=10..50]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    @Test
    fun testSpecialCases_6() {
        // 负值坐标
        val selector = "@e[x=-100,y=-50,z=-100,dx=50,dy=100,dz=50]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }
    
    @Test
    fun testSpecialCases_7() {
        // 小数坐标
        val selector = "@e[x=10.5,y=20.3,z=30.7,dx=5.2,dy=5.8,dz=5.1]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }
    
    @Test
    fun testSpecialCases_8() {
        // 波浪号坐标
        val selector = "@e[x=~10,y=~-5,z=~20,dx=10,dy=10,dz=10]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }
    
    @Test
    fun testSpecialCases_9() {
        // 空标签
        val selector = "@e[tag=,name=]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }
    
    @Test
    fun testSpecialCases_10() {
        // 多个标签
        val selector = "@e[tag=a,tag=b,tag=c]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }
    
    /**
     * 测试组7：选择器变量测试
     */
    @Test
    fun testSelectorVariables() {
        val selectors = listOf("@p", "@r", "@a", "@e", "@s", "@n")
        for (selector in selectors) {
            val type = SelectorConverter.detectSelectorType(selector)
            assertEquals(SelectorType.UNIVERSAL, type)
        }
    }
    
    /**
     * 测试组8：参数值测试
     */
    @Test
    fun testParameterValues_1() {
        // 游戏模式值测试
        val modes = listOf("survival", "creative", "adventure", "spectator")
        for (mode in modes) {
            val selector = "@a[gamemode=$mode]"
            val type = SelectorConverter.detectSelectorType(selector)
            assertEquals(SelectorType.JAVA, type)
        }
    }
    
    @Test
    fun testParameterValues_2() {
        // 基岩版游戏模式值测试
        val modes = listOf("0", "1", "2", "5", "s", "c", "a", "d", "survival", "creative", "adventure", "default")
        for (mode in modes) {
            val selector = "@a[m=$mode]"
            val type = SelectorConverter.detectSelectorType(selector)
            assertEquals(SelectorType.BEDROCK, type)
        }
    }
    
    @Test
    fun testParameterValues_3() {
        // 排序值测试
        val sorts = listOf("nearest", "furthest", "random", "arbitrary")
        for (sort in sorts) {
            val selector = "@e[limit=5,sort=$sort]"
            val type = SelectorConverter.detectSelectorType(selector)
            assertEquals(SelectorType.JAVA, type)
        }
    }
    
    /**
     * 测试组9：sort和limit的所有组合
     */
    @Test
    fun testSortAndLimitCombinations_1() {
        // sort=nearest + limit=正数
        val selector = "@a[limit=5,sort=nearest]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    @Test
    fun testSortAndLimitCombinations_2() {
        // sort=nearest（无limit）
        val selector = "@a[sort=nearest]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    @Test
    fun testSortAndLimitCombinations_3() {
        // sort=furthest + limit=正数
        val selector = "@a[limit=10,sort=furthest]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    @Test
    fun testSortAndLimitCombinations_4() {
        // sort=furthest（无limit）
        val selector = "@a[sort=furthest]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    @Test
    fun testSortAndLimitCombinations_5() {
        // sort=random + limit=正数
        val selector = "@a[limit=3,sort=random]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    @Test
    fun testSortAndLimitCombinations_6() {
        // sort=random（无limit）
        val selector = "@a[sort=random]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    @Test
    fun testSortAndLimitCombinations_7() {
        // sort=arbitrary + limit=正数
        val selector = "@a[limit=7,sort=arbitrary]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    @Test
    fun testSortAndLimitCombinations_8() {
        // sort=arbitrary（无limit）
        val selector = "@a[sort=arbitrary]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    @Test
    fun testSortAndLimitCombinations_9() {
        // limit=正数（无sort）
        val selector = "@a[limit=5]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    @Test
    fun testSortAndLimitCombinations_10() {
        // c=正数（基岩版）
        val selector = "@a[c=5]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    @Test
    fun testSortAndLimitCombinations_11() {
        // c=负数（基岩版）
        val selector = "@a[c=-5]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    /**
     * 测试组10：范围参数的所有可能
     */
    @Test
    fun testRangeParameters_distance_1() {
        // distance=单个值
        val selector = "@a[distance=10]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    @Test
    fun testRangeParameters_distance_2() {
        // distance=5..10
        val selector = "@a[distance=5..10]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    @Test
    fun testRangeParameters_distance_3() {
        // distance=5..
        val selector = "@a[distance=5..]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    @Test
    fun testRangeParameters_distance_4() {
        // distance=..10
        val selector = "@a[distance=..10]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    @Test
    fun testRangeParameters_x_rotation_1() {
        // x_rotation=单个值
        val selector = "@a[x_rotation=45]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    @Test
    fun testRangeParameters_x_rotation_2() {
        // x_rotation=-45..45
        val selector = "@a[x_rotation=-45..45]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    @Test
    fun testRangeParameters_x_rotation_3() {
        // x_rotation=-45..
        val selector = "@a[x_rotation=-45..]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    @Test
    fun testRangeParameters_x_rotation_4() {
        // x_rotation=..45
        val selector = "@a[x_rotation=..45]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    @Test
    fun testRangeParameters_y_rotation_1() {
        // y_rotation=单个值
        val selector = "@a[y_rotation=90]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    @Test
    fun testRangeParameters_y_rotation_2() {
        // y_rotation=-90..90
        val selector = "@a[y_rotation=-90..90]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    @Test
    fun testRangeParameters_y_rotation_3() {
        // y_rotation=-90..
        val selector = "@a[y_rotation=-90..]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    @Test
    fun testRangeParameters_y_rotation_4() {
        // y_rotation=..90
        val selector = "@a[y_rotation=..90]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    @Test
    fun testRangeParameters_level_1() {
        // level=单个值
        val selector = "@a[level=5]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    @Test
    fun testRangeParameters_level_2() {
        // level=5..10
        val selector = "@a[level=5..10]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    @Test
    fun testRangeParameters_level_3() {
        // level=5..
        val selector = "@a[level=5..]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    @Test
    fun testRangeParameters_level_4() {
        // level=..10
        val selector = "@a[level=..10]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    @Test
    fun testRangeParameters_r_rm_1() {
        // r=10
        val selector = "@a[r=10]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    @Test
    fun testRangeParameters_r_rm_2() {
        // rm=5
        val selector = "@a[rm=5]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    @Test
    fun testRangeParameters_r_rm_3() {
        // rm=5,r=10
        val selector = "@a[rm=5,r=10]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    @Test
    fun testRangeParameters_rx_rxm_1() {
        // rx=45
        val selector = "@a[rx=45]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    @Test
    fun testRangeParameters_rx_rxm_2() {
        // rxm=-45
        val selector = "@a[rxm=-45]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    @Test
    fun testRangeParameters_rx_rxm_3() {
        // rxm=-45,rx=45
        val selector = "@a[rxm=-45,rx=45]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    @Test
    fun testRangeParameters_ry_rym_1() {
        // ry=90
        val selector = "@a[ry=90]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    @Test
    fun testRangeParameters_ry_rym_2() {
        // rym=-90
        val selector = "@a[rym=-90]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    @Test
    fun testRangeParameters_ry_rym_3() {
        // rym=-90,ry=90
        val selector = "@a[rym=-90,ry=90]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    @Test
    fun testRangeParameters_l_lm_1() {
        // l=10
        val selector = "@a[l=10]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    @Test
    fun testRangeParameters_l_lm_2() {
        // lm=5
        val selector = "@a[lm=5]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    @Test
    fun testRangeParameters_l_lm_3() {
        // lm=5,l=10
        val selector = "@a[lm=5,l=10]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    @Test
    fun testRangeParameters_scores_1() {
        // scores={test=10}
        val selector = "@a[scores={test=10}]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }
    
    @Test
    fun testRangeParameters_scores_2() {
        // scores={test=5..10}
        val selector = "@a[scores={test=5..10}]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }
    
    @Test
    fun testRangeParameters_scores_3() {
        // scores={test=5..}
        val selector = "@a[scores={test=5..}]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }
    
    @Test
    fun testRangeParameters_scores_4() {
        // scores={test=..10}
        val selector = "@a[scores={test=..10}]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }
    
    @Test
    fun testRangeParameters_scores_5() {
        // 多个scores参数
        val selector = "@a[scores={test=10,foo=5..10,bar=5..,baz=..20}]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }
    
    /**
     * 测试组11：否定参数测试
     */
    @Test
    fun testNegationParameters_1() {
        // type=!zombie
        val selector = "@e[type=!zombie]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }
    
    @Test
    fun testNegationParameters_2() {
        // tag=!enemy
        val selector = "@e[tag=!enemy]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }
    
    @Test
    fun testNegationParameters_3() {
        // name=!Boss
        val selector = "@e[name=!Boss]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.UNIVERSAL, type)
    }
    
    @Test
    fun testNegationParameters_4() {
        // gamemode=!survival
        val selector = "@a[gamemode=!survival]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    @Test
    fun testNegationParameters_5() {
        // m=!0
        val selector = "@a[m=!0]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    @Test
    fun testNegationParameters_6() {
        // team=!red
        val selector = "@a[team=!red]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, type)
    }
    
    @Test
    fun testNegationParameters_7() {
        // family=!monster
        val selector = "@e[family=!monster]"
        val type = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, type)
    }
    
    /**
     * 测试组12：hasitem参数测试
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
    
    /**
     * 测试组13：nbt参数测试
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
    
    /**
     * 测试组14：选择器转换测试
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
     * 测试组15：参数过滤测试
     */
    @Test
    fun testParameterFiltering_1() {
        // 过滤Java版独有参数到基岩版
        val javaSelector = "@a[distance=10,team=red,gamemode=survival]"
        val (filtered, removed) = SelectorConverter.filterSelectorParameters(javaSelector, SelectorType.BEDROCK, context)
        assertTrue("应移除team参数", removed.contains("team"))
        assertFalse("不应包含team参数", filtered.contains("team"))
    }
    
    @Test
    fun testParameterFiltering_2() {
        // 过滤基岩版独有参数到Java版
        val bedrockSelector = "@a[r=10,m=0,haspermission={movement=enabled}]"
        val (filtered, removed) = SelectorConverter.filterSelectorParameters(bedrockSelector, SelectorType.JAVA, context)
        assertTrue("应移除haspermission参数", removed.contains("haspermission"))
        assertFalse("不应包含haspermission参数", filtered.contains("haspermission"))
    }
    
    @Test
    fun testParameterFiltering_3() {
        // 过滤family参数到Java版
        val bedrockSelector = "@e[family=zombie]"
        val (filtered, removed) = SelectorConverter.filterSelectorParameters(bedrockSelector, SelectorType.JAVA, context)
        assertTrue("应移除family参数", removed.contains("family"))
        assertFalse("不应包含family参数", filtered.contains("family"))
    }
    
    @Test
    fun testParameterFiltering_4() {
        // 过滤predicate参数到基岩版
        val javaSelector = "@a[predicate=test:test]"
        val (filtered, removed) = SelectorConverter.filterSelectorParameters(javaSelector, SelectorType.BEDROCK, context)
        assertTrue("应移除predicate参数", removed.contains("predicate"))
        assertFalse("不应包含predicate参数", filtered.contains("predicate"))
    }
    
    @Test
    fun testParameterFiltering_5() {
        // 过滤advancements参数到基岩版
        val javaSelector = "@a[advancements={story/form_obsidian=true}]"
        val (filtered, removed) = SelectorConverter.filterSelectorParameters(javaSelector, SelectorType.BEDROCK, context)
        assertTrue("应移除advancements参数", removed.contains("advancements"))
        assertFalse("不应包含advancements参数", filtered.contains("advancements"))
    }
}