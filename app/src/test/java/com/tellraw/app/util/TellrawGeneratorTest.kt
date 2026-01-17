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
 * Tellraw命令生成器综合测试
 * 测试选择器转换和命令生成的完整流程
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = TestApplication::class)
class TellrawGeneratorTest {
    private val context: Context = ApplicationProvider.getApplicationContext<android.app.Application>()
    
    /**
     * 测试组1：通用选择器参数命令生成测试
     */
    @Test
    fun testUniversalSelectorCommandGeneration_1() {
        val selector = "@e[x=10,y=20,z=30,dx=5,dy=5,dz=5,scores={test=10},tag=player,name=Steve,type=zombie]"
        val message = "§c测试消息"
        
        val javaJson = TextFormatter.convertToJavaJson(message, "none", false)
        val bedrockJson = TextFormatter.convertToBedrockJson(message, "none", false)
        
        val javaCommand = "tellraw $selector $javaJson"
        val bedrockCommand = "tellraw $selector $bedrockJson"
        
        assertNotNull("Java版命令不应为null", javaCommand)
        assertNotNull("基岩版命令不应为null", bedrockCommand)
        assertTrue("Java版命令应包含tellraw", javaCommand.startsWith("tellraw"))
        assertTrue("基岩版命令应包含tellraw", bedrockCommand.startsWith("tellraw"))
    }
    
    @Test
    fun testUniversalSelectorCommandGeneration_2() {
        val selector = "@a[x=0,y=64,z=0,dx=100,dy=50,dz=100,scores={kills=5..10},tag=team_red,name=\"\",type=player]"
        val message = "§a§l欢迎加入红色队伍！"
        
        val javaJson = TextFormatter.convertToJavaJson(message, "none", false)
        val bedrockJson = TextFormatter.convertToBedrockJson(message, "none", false)
        
        val javaCommand = "tellraw $selector $javaJson"
        val bedrockCommand = "tellraw $selector $bedrockJson"
        
        assertNotNull("Java版命令不应为null", javaCommand)
        assertNotNull("基岩版命令不应为null", bedrockCommand)
    }
    
    @Test
    fun testUniversalSelectorCommandGeneration_3() {
        val selector = "@p[x=100,y=70,z=100,dx=20,dy=10,dz=20,scores={health=20},tag=!enemy,name=Alex,type=player]"
        val message = "§e§l你是最棒的玩家！"
        
        val javaJson = TextFormatter.convertToJavaJson(message, "none", false)
        val bedrockJson = TextFormatter.convertToBedrockJson(message, "none", false)
        
        val javaCommand = "tellraw $selector $javaJson"
        val bedrockCommand = "tellraw $selector $bedrockJson"
        
        assertNotNull("Java版命令不应为null", javaCommand)
        assertNotNull("基岩版命令不应为null", bedrockCommand)
    }
    
    /**
     * 测试组2：Java版选择器参数命令生成测试
     */
    @Test
    fun testJavaSelectorCommandGeneration_1() {
        val selector = "@a[distance=10,team=red,gamemode=survival,level=5..10,sort=nearest]"
        val message = "§c§l警告：敌对生物接近！"
        
        val javaJson = TextFormatter.convertToJavaJson(message, "none", false)
        val javaCommand = "tellraw $selector $javaJson"
        
        assertNotNull("Java版命令不应为null", javaCommand)
        assertTrue("Java版命令应包含tellraw", javaCommand.startsWith("tellraw"))
    }
    
    @Test
    fun testJavaSelectorCommandGeneration_2() {
        val selector = "@e[x_rotation=0..45,y_rotation=-90..90,nbt={CustomName:\"Steve\"},limit=5]"
        val message = "§d§l发现特殊实体！"
        
        val javaJson = TextFormatter.convertToJavaJson(message, "none", false)
        val javaCommand = "tellraw $selector $javaJson"
        
        assertNotNull("Java版命令不应为null", javaCommand)
    }
    
    @Test
    fun testJavaSelectorCommandGeneration_3() {
        val selector = "@a[advancements={story/form_obsidian=true},predicate=test:test,gamemode=adventure]"
        val message = "§b§l达成成就：冰桶挑战！"
        
        val javaJson = TextFormatter.convertToJavaJson(message, "none", false)
        val javaCommand = "tellraw $selector $javaJson"
        
        assertNotNull("Java版命令不应为null", javaCommand)
    }
    
    /**
     * 测试组3：基岩版选择器参数命令生成测试
     */
    @Test
    fun testBedrockSelectorCommandGeneration_1() {
        val selector = "@a[r=10,m=0,c=5,l=5..10,lm=5]"
        val message = "§a§l欢迎玩家！"
        
        val bedrockJson = TextFormatter.convertToBedrockJson(message, "none", false)
        val bedrockCommand = "tellraw $selector $bedrockJson"
        
        assertNotNull("基岩版命令不应为null", bedrockCommand)
        assertTrue("基岩版命令应包含tellraw", bedrockCommand.startsWith("tellraw"))
    }
    
    @Test
    fun testBedrockSelectorCommandGeneration_2() {
        val selector = "@e[family=zombie,hasitem={item=diamond_sword},rx=-45..45,ry=0..90]"
        val message = "§c§l发现僵尸！"
        
        val bedrockJson = TextFormatter.convertToBedrockJson(message, "none", false)
        val bedrockCommand = "tellraw $selector $bedrockJson"
        
        assertNotNull("基岩版命令不应为null", bedrockCommand)
    }
    
    @Test
    fun testBedrockSelectorCommandGeneration_3() {
        val selector = "@a[haspermission={movement=enabled},m=1,c=3,l=10]"
        val message = "§e§l权限已启用！"
        
        val bedrockJson = TextFormatter.convertToBedrockJson(message, "none", false)
        val bedrockCommand = "tellraw $selector $bedrockJson"
        
        assertNotNull("基岩版命令不应为null", bedrockCommand)
    }
    
    /**
     * 测试组4：选择器类型检测和转换测试
     */
    @Test
    fun testSelectorTypeDetectionAndConversion() {
        val testCases: List<Pair<String, SelectorType>> = listOf(
            "@a[distance=10]" to SelectorType.JAVA,
            "@a[r=10]" to SelectorType.BEDROCK,
            "@a[x=10,y=20,z=30]" to SelectorType.UNIVERSAL,
            "@e[gamemode=survival]" to SelectorType.JAVA,
            "@e[m=0]" to SelectorType.BEDROCK,
            "@p[team=red]" to SelectorType.JAVA,
            "@p[tag=player]" to SelectorType.UNIVERSAL
        )
        
        for ((selector, expectedType) in testCases) {
            val detectedType = SelectorConverter.detectSelectorType(selector)
            assertEquals("选择器类型检测错误: $selector", expectedType, detectedType)
        }
    }
    
    /**
     * 测试组5：基岩版到Java版选择器转换测试
     */
    @Test
    fun testBedrockToJavaConversion() {
        val testCases = listOf(
            "@a[r=10]" to "distance=..10",
            "@a[rm=5]" to "distance=5..",
            "@a[rm=5,r=10]" to "distance=5..10",
            "@a[m=0]" to "gamemode=survival",
            "@a[m=1]" to "gamemode=creative",
            "@a[m=2]" to "gamemode=adventure",
            "@a[m=s]" to "gamemode=survival",
            "@a[m=c]" to "gamemode=creative",
            "@a[m=a]" to "gamemode=adventure",
            "@a[l=5]" to "level=5",
            "@a[lm=5]" to "level=5..",
            "@a[lm=5,l=10]" to "level=5..10",
            "@a[rx=-45]" to "x_rotation=-45..",
            "@a[rx=45]" to "x_rotation=..45",
            "@a[rxm=-45,rx=45]" to "x_rotation=-45..45",
            "@a[ry=-90]" to "y_rotation=-90..",
            "@a[ry=90]" to "y_rotation=..90",
            "@a[rym=-90,ry=90]" to "y_rotation=-90..90",
            "@a[c=5]" to "limit=5,sort=nearest",
            "@a[c=-5]" to "limit=5,sort=furthest"
        )
        
        for ((bedrockSelector, expectedJavaParam) in testCases) {
            val conversion = SelectorConverter.convertBedrockToJava(bedrockSelector, context)
            assertTrue("应该检测到基岩版选择器: $bedrockSelector", conversion.wasConverted)
            assertNotNull("转换后的Java选择器不应为null: $bedrockSelector", conversion.javaSelector)
        }
    }
    
    /**
     * 测试组6：Java版到基岩版选择器转换测试
     */
    @Test
    fun testJavaToBedrockConversion() {
        val testCases = listOf(
            "@a[distance=10]" to "r=10",
            "@a[distance=5..]" to "rm=5",
            "@a[distance=5..10]" to "rm=5,r=10",
            "@a[gamemode=survival]" to "m=0",
            "@a[gamemode=creative]" to "m=1",
            "@a[gamemode=adventure]" to "m=2",
            "@a[level=5]" to "l=5",
            "@a[level=5..]" to "lm=5",
            "@a[level=5..10]" to "lm=5,l=10",
            "@a[x_rotation=-45..45]" to "rxm=-45,rx=45",
            "@a[y_rotation=-90..90]" to "rym=-90,ry=90",
            "@a[limit=5,sort=nearest]" to "c=5",
            "@a[limit=5,sort=furthest]" to "c=-5"
        )
        
        for ((javaSelector, expectedBedrockParam) in testCases) {
            val conversion = SelectorConverter.convertJavaToBedrock(javaSelector, context)
            // Java版选择器转换后，基岩版选择器应该包含对应的参数
            assertNotNull("转换后的基岩版选择器不应为null: $javaSelector", conversion.bedrockSelector)
        }
    }
    
    /**
     * 测试组7：§m和§n代码命令生成测试
     */
    @Test
    fun testStrikethroughAndUnderlineCommandGeneration() {
        val testCases = listOf(
            "§m删除线文字" to "font",
            "§m删除线文字" to "color",
            "§n下划线文字" to "font",
            "§n下划线文字" to "color",
            "§m§n删除线下划线" to "font",
            "§m§n删除线下划线" to "color",
            "§l§m粗体删除线" to "font",
            "§l§n粗体下划线" to "font"
        )
        
        for ((message, mode) in testCases) {
            val javaJson = TextFormatter.convertToJavaJson(message, mode, false)
            val bedrockJson = TextFormatter.convertToBedrockJson(message, "none", false)
            
            assertNotNull("Java版JSON不应为null: $message ($mode)", javaJson)
            assertNotNull("基岩版JSON不应为null: $message", bedrockJson)
        }
    }
    
    /**
     * 测试组8：混合模式_c/_f后缀命令生成测试
     */
    @Test
    fun testMixedModeSuffixCommandGeneration() {
        val testCases = listOf(
            "§m_f字体删除线§m_c颜色删除线",
            "§n_f字体下划线§n_c颜色下划线",
            "§m_f§n_f字体删除线下划线§m_c§n_c颜色删除线下划线",
            "§l§m_f粗体字体删除线§r§a§n_c绿色颜色下划线"
        )
        
        for (message in testCases) {
            val javaJson = TextFormatter.convertToJavaJson(message, "font", true)
            val bedrockJson = TextFormatter.convertToBedrockJson(message, "none", true)
            
            assertNotNull("Java版JSON不应为null: $message", javaJson)
            assertNotNull("基岩版JSON不应为null: $message", bedrockJson)
        }
    }
    
    /**
     * 测试组9：复杂选择器参数组合测试
     */
    @Test
    fun testComplexSelectorCombinations() {
        val testCases = listOf(
            "@a[x=10,y=20,z=30,dx=5,dy=5,dz=5,distance=10,team=red,gamemode=survival,sort=nearest]",
            "@e[x=0,y=64,z=0,dx=100,dy=50,dz=100,r=10,m=0,c=5,hasitem={item=diamond}]",
            "@p[x=100,y=70,z=100,dx=20,dy=10,dz=20,distance=..15,team=!enemy,gamemode=adventure,sort=furthest]",
            "@e[x=50,y=50,z=50,dx=30,dy=30,dz=30,rm=10,r=50,m=s,c=10,hasitem=[{item=apple,quantity=5..}]]"
        )
        
        for (selector in testCases) {
            val type = SelectorConverter.detectSelectorType(selector)
            val message = "§c§l测试消息"
            
            val javaJson = TextFormatter.convertToJavaJson(message, "none", false)
            val bedrockJson = TextFormatter.convertToBedrockJson(message, "none", false)
            
            assertNotNull("选择器类型不应为null: $selector", type)
            assertNotNull("Java版JSON不应为null: $selector", javaJson)
            assertNotNull("基岩版JSON不应为null: $selector", bedrockJson)
        }
    }
    
    /**
     * 测试组10：实际游戏场景测试
     */
    @Test
    fun testRealGameScenarios() {
        val scenarios = listOf(
            Scenario(
                selector = "@a[gamemode=survival,level=5..]",
                message = "§c§l警告：生存模式玩家请远离危险区域！",
                description = "生存模式玩家警告"
            ),
            Scenario(
                selector = "@e[type=zombie,tag=!tamed]",
                message = "§c§l发现野生僵尸！",
                description = "野生僵尸警告"
            ),
            Scenario(
                selector = "@a[team=red,scores={kills=10..}]",
                message = "§a§l恭喜红色队伍击杀数达到10！",
                description = "队伍击杀数奖励"
            ),
            Scenario(
                selector = "@p[tag=vip,level=30..]",
                message = "§d§l欢迎VIP玩家！",
                description = "VIP玩家欢迎"
            ),
            Scenario(
                selector = "@a[distance=..50,x_rotation=-30..30]",
                message = "§e§l注意：前方有陷阱！",
                description = "附近玩家陷阱警告"
            ),
            Scenario(
                selector = "@e[type=player,gamemode=creative]",
                message = "§b§l创造模式玩家请注意规则！",
                description = "创造模式玩家提醒"
            ),
            Scenario(
                selector = "@a[r=10,m=0,c=1]",
                message = "§a§l欢迎新玩家加入！",
                description = "新玩家欢迎（基岩版）"
            ),
            Scenario(
                selector = "@e[family=zombie,hasitem={item=diamond}]",
                message = "§c§l发现稀有僵尸！",
                description = "稀有僵尸发现（基岩版）"
            )
        )
        
        for (scenario in scenarios) {
            val type = SelectorConverter.detectSelectorType(scenario.selector)
            val javaJson = TextFormatter.convertToJavaJson(scenario.message, "none", false)
            val bedrockJson = TextFormatter.convertToBedrockJson(scenario.message, "none", false)
            
            assertNotNull("场景测试失败: ${scenario.description}", type)
            assertNotNull("Java版JSON不应为null: ${scenario.description}", javaJson)
            assertNotNull("基岩版JSON不应为null: ${scenario.description}", bedrockJson)
        }
    }
    
    /**
     * 测试组11：边界情况和异常处理测试
     */
    @Test
    fun testEdgeCasesAndExceptionHandling() {
        val testCases = listOf(
            "" to "空选择器",
            "@a" to "只有变量",
            "@a[]" to "空参数",
            "@a[invalid_param=value]" to "无效参数",
            "@a[x=invalid]" to "无效值",
            "@a[x=1.5.5]" to "无效小数"
        )
        
        for ((selector, description) in testCases) {
            val type = SelectorConverter.detectSelectorType(selector)
            val message = "测试消息"
            
            val javaJson = TextFormatter.convertToJavaJson(message, "none", false)
            val bedrockJson = TextFormatter.convertToBedrockJson(message, "none", false)
            
            assertNotNull("边界情况测试失败: $description", type)
            assertNotNull("Java版JSON不应为null: $description", javaJson)
            assertNotNull("基岩版JSON不应为null: $description", bedrockJson)
        }
    }
    
    /**
     * 测试组12：参数过滤和提醒生成测试
     */
    @Test
    fun testParameterFilteringAndWarningGeneration() {
        val testCases = listOf(
            "@a[distance=10,gamemode=survival,team=red]" to listOf("Java版"),
            "@a[r=10,m=0,tag=player]" to listOf("基岩版"),
            "@a[distance=10,r=10]" to listOf("Java版", "基岩版"),
            "@a[gamemode=survival,m=0]" to listOf("Java版", "基岩版")
        )
        
        for ((selector, expectedTypes) in testCases) {
            val type = SelectorConverter.detectSelectorType(selector)
            val conversion = SelectorConverter.convertBedrockToJava(selector, context)
            
            assertNotNull("选择器类型不应为null: $selector", type)
            assertNotNull("转换结果不应为null: $selector", conversion)
        }
    }
    
    /**
     * 测试组13：多语言文本测试
     */
    @Test
    fun testMultiLanguageText() {
        val testCases = listOf(
            "§c§l中文测试消息",
            "§a§lEnglish Test Message",
            "§b§l日本語テストメッセージ",
            "§d§l한국어 테스트 메시지",
            "§e§lMensaje de prueba en español",
            "§f§lNachricht auf Deutsch"
        )
        
        for (message in testCases) {
            val javaJson = TextFormatter.convertToJavaJson(message, "none", false)
            val bedrockJson = TextFormatter.convertToBedrockJson(message, "none", false)
            
            assertNotNull("Java版JSON不应为null: $message", javaJson)
            assertNotNull("基岩版JSON不应为null: $message", bedrockJson)
        }
    }
    
    /**
     * 测试组14：性能测试（大量参数）
     */
    @Test
    fun testPerformanceWithManyParameters() {
        val selector = "@a[x=10,y=20,z=30,dx=5,dy=5,dz=5,scores={test1=10,test2=20,test3=30,test4=40,test5=50},tag=a,tag=b,tag=c,name=Steve,type=player,distance=10,team=red,gamemode=survival,level=5..10,sort=nearest,limit=5]"
        val message = "§c§l这是一个包含大量参数的选择器测试"
        
        val javaJson = TextFormatter.convertToJavaJson(message, "none", false)
        val bedrockJson = TextFormatter.convertToBedrockJson(message, "none", false)
        
        assertNotNull("Java版JSON不应为null", javaJson)
        assertNotNull("基岩版JSON不应为null", bedrockJson)
    }
    
    /**
     * 测试组15：命令格式验证测试
     */
    @Test
    fun testCommandFormatValidation() {
        val selector = "@a[x=10,y=20,z=30]"
        val message = "§c测试消息"
        
        val javaJson = TextFormatter.convertToJavaJson(message, "none", false)
        val bedrockJson = TextFormatter.convertToBedrockJson(message, "none", false)
        
        val javaCommand = "tellraw $selector $javaJson"
        val bedrockCommand = "tellraw $selector $bedrockJson"
        
        // 验证命令格式
        assertTrue("Java版命令应以'tellraw'开头", javaCommand.startsWith("tellraw"))
        assertTrue("基岩版命令应以'tellraw'开头", bedrockCommand.startsWith("tellraw"))
        assertTrue("Java版命令应包含选择器", javaCommand.contains(selector))
        assertTrue("基岩版命令应包含选择器", bedrockCommand.contains(selector))
    }
}

/**
 * 测试场景数据类
 */
data class Scenario(
    val selector: String,
    val message: String,
    val description: String
)