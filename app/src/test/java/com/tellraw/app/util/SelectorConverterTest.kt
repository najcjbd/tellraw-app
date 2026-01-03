package com.tellraw.app.util

import com.tellraw.app.model.SelectorType
import org.junit.Test
import org.junit.Assert.*

/**
 * 测试SelectorConverter的选择器转换功能
 */
class SelectorConverterTest {

    private fun testSelector(selector: String) {
        println("\n${"=".repeat(80)}")
        println("测试选择器: $selector")
        println("=".repeat(80))

        // 检测选择器类型
        val selectorType = SelectorConverter.detectSelectorType(selector)
        println("检测到的选择器类型: $selectorType")

        // 转换参数
        if (selectorType == SelectorType.BEDROCK) {
            // 基岩版转Java版
            val result = SelectorConverter.convertBedrockToJava(selector)
            println("转换后的Java版选择器: ${result.javaSelector}")
            println("是否转换: ${result.wasConverted}")
            if (result.javaReminders.isNotEmpty()) {
                println("转换提醒:")
                result.javaReminders.forEach { println("  - $it") }
            }
        } else {
            // Java版转基岩版
            val result = SelectorConverter.convertJavaToBedrock(selector)
            println("转换后的基岩版选择器: ${result.bedrockSelector}")
            println("是否转换: ${result.wasConverted}")
            if (result.bedrockReminders.isNotEmpty()) {
                println("转换提醒:")
                result.bedrockReminders.forEach { println("  - $it") }
            }
        }

        // 测试参数过滤
        println("\n--- 参数过滤测试 ---")
        val (filteredJava, javaReminders) = SelectorConverter.filterSelectorParameters(selector, SelectorType.JAVA)
        println("过滤后的Java版选择器: $filteredJava")
        if (javaReminders.isNotEmpty()) {
            println("Java版过滤提醒:")
            javaReminders.forEach { println("  - $it") }
        }

        val (filteredBedrock, bedrockReminders) = SelectorConverter.filterSelectorParameters(selector, SelectorType.BEDROCK)
        println("过滤后的基岩版选择器: $filteredBedrock")
        if (bedrockReminders.isNotEmpty()) {
            println("基岩版过滤提醒:")
            bedrockReminders.forEach { println("  - $it") }
        }
    }

    @Test
    fun testAllSelectors() {
        println("=".repeat(80))
        println("Android版本选择器转换测试")
        println("=".repeat(80))

        // 测试用例列表
        val testCases = listOf(
            // 通用选择器参数
            "@p[x=1,y=2,z=3]",
            "@e[x=1,y=2,z=3,dx=4,dy=5,dz=6]",
            "@a[name=Steve]",
            "@e[type=zombie]",
            "@r[tag=test]",
            "@a[scores={test=10}]",

            // Java版特有参数
            "@a[distance=10]",
            "@e[distance=5..10]",
            "@e[distance=5..]",
            "@e[distance=..15]",
            "@e[x_rotation=-45..45]",
            "@e[x_rotation=45]",
            "@e[y_rotation=-90..90]",
            "@e[level=10]",
            "@e[level=5..10]",
            "@a[gamemode=survival]",
            "@a[gamemode=spectator]",
            "@e[limit=4,sort=nearest]",
            "@e[limit=4,sort=furthest]",
            "@e[limit=4,sort=random]",
            "@e[limit=4,sort=arbitrary]",
            "@e[nbt={Inventory:[{id:\"minecraft:diamond\",Count:3b}]}]",
            "@a[team=red]",

            // 基岩版特有参数
            "@a[r=10]",
            "@e[rm=5]",
            "@e[rm=10,r=12]",
            "@e[rx=45]",
            "@e[rxm=-45,rx=45]",
            "@e[ry=90]",
            "@e[rym=-90,ry=90]",
            "@e[l=10]",
            "@e[lm=5,l=10]",
            "@a[m=survival]",
            "@a[m=default]",
            "@e[c=4]",
            "@e[c=-4]",
            "@e[hasitem={item=diamond}]",
            "@e[hasitem={item=diamond,quantity=5..10}]",
            "@e[family=monster]",

            // 特殊组合
            "@e[limit=4,sort=furthest]",
            "@e[limit=4,sort=nearest,distance=5..10]",
            "@e[distance=5..10,x_rotation=-45..45,y_rotation=-90..90]",
            "@a[gamemode=survival,level=1..10]",
            "@e[tag=test,scores={test=10..20}]",
            "@e[scores={test=!10}]",
            "@e[nbt={Inventory:[{id:\"minecraft:diamond\",Count:3..}]}]",
            "@e[hasitem={item=diamond,quantity=5..10,location=slot.weapon.mainhand}]",
            "@e[hasitem=[{item=diamond},{item=iron}]]",

            // 基岩版特有选择器变量
            "@initiator",
            "@c",
            "@v",

            // 复杂组合
            "@e[distance=5..10,x_rotation=-45..45,y_rotation=-90..90,limit=4,sort=nearest]",
            "@a[gamemode=survival,level=1..10,scores={test=10..20}]",
            "@e[type=zombie,tag=test,nbt={Inventory:[{id:\"minecraft:diamond\",Count:3b}]}]",
            "@e[rm=5,r=12,rxm=-45,rx=45,rym=-90,ry=90,l=1..10,m=survival,c=4]",
        )

        // 执行测试
        testCases.forEach { testSelector(it) }

        println("\n${"=".repeat(80)}")
        println("测试完成")
        println("=".repeat(80))
    }

    @Test
    fun testEdgeCases() {
        println("\n${"=".repeat(80)}")
        println("测试边界情况")
        println("=".repeat(80))

        // 测试空参数
        val emptyParams = "@a[]"
        testSelector(emptyParams)

        // 测试无参数
        val noParams = "@a"
        testSelector(noParams)

        // 测试单个参数
        val singleParam = "@a[gamemode=survival]"
        testSelector(singleParam)

        // 测试极值
        val extremeValues = "@e[distance=0..1000,x_rotation=-180..180,y_rotation=-180..180]"
        testSelector(extremeValues)

        // 测试负值
        val negativeValues = "@e[x=-1000,y=-100,z=-100]"
        testSelector(negativeValues)

        // 测试大数值
        val largeValues = "@e[x=1000000,y=1000000,z=1000000]"
        testSelector(largeValues)

        // 测试特殊字符
        val specialChars = "@a[name=Test_Player-123]"
        testSelector(specialChars)

        // 测试中文字符
        val chineseChars = "@a[name=玩家]"
        testSelector(chineseChars)
    }

    @Test
    fun testInvalidSelectors() {
        println("\n${"=".repeat(80)}")
        println("测试无效选择器")
        println("=".repeat(80))

        // 测试不匹配的括号
        val mismatchedBrackets = "@a[gamemode=survival"
        println("\n测试不匹配的括号: $mismatchedBrackets")
        val type1 = SelectorConverter.detectSelectorType(mismatchedBrackets)
        println("检测到的类型: $type1")

        // 测试无效的参数格式
        val invalidParamFormat = "@a[gamemode:survival]"
        println("\n测试无效的参数格式: $invalidParamFormat")
        val type2 = SelectorConverter.detectSelectorType(invalidParamFormat)
        println("检测到的类型: $type2")

        // 测试未知的选择器变量
        val unknownSelector = "@x[gamemode=survival]"
        println("\n测试未知的选择器变量: $unknownSelector")
        val type3 = SelectorConverter.detectSelectorType(unknownSelector)
        println("检测到的类型: $type3")

        // 测试空字符串
        val emptyString = ""
        println("\n测试空字符串: '$emptyString'")
        val type4 = SelectorConverter.detectSelectorType(emptyString)
        println("检测到的类型: $type4")
    }

    @Test
    fun testComplexScores() {
        println("\n${"=".repeat(80)}")
        println("测试复杂的scores参数")
        println("=".repeat(80))

        val complexScoresCases = listOf(
            "@e[scores={test=10}]",
            "@e[scores={test=10..20}]",
            "@e[scores={test=!10}]",
            "@e[scores={test=10..20,other=5..15}]",
            "@e[scores={test=10,other=20,third=30}]",
            "@e[scores={test=!10,other=20..30}]",
            "@e[scores={level=10}]",  // 测试level在scores中
            "@e[scores={level=10..20}]"
        )

        complexScoresCases.forEach { selector ->
            testSelector(selector)
        }
    }

    @Test
    fun testComplexNbt() {
        println("\n${"=".repeat(80)}")
        println("测试复杂的NBT参数")
        println("=".repeat(80))

        val complexNbtCases = listOf(
            "@e[nbt={test:\"value\"}]",
            "@e[nbt={test:123}]",
            "@e[nbt={test:true}]",
            "@e[nbt={test:[1,2,3]}]",
            "@e[nbt={test:{nested:\"value\"}}]",
            "@e[nbt={Inventory:[{id:\"minecraft:diamond\",Count:3b}]}]",
            "@e[nbt={Inventory:[{id:\"minecraft:diamond\",Count:3b},{id:\"minecraft:iron\",Count:5b}]}]",
            "@e[nbt={test:\"value with spaces\"}]"
        )

        complexNbtCases.forEach { selector ->
            testSelector(selector)
        }
    }

    @Test
    fun testComplexHasitem() {
        println("\n${"=".repeat(80)}")
        println("测试复杂的hasitem参数")
        println("=".repeat(80))

        val complexHasitemCases = listOf(
            "@e[hasitem={item=diamond}]",
            "@e[hasitem={item=diamond,quantity=5}]",
            "@e[hasitem={item=diamond,quantity=5..10}]",
            "@e[hasitem={item=diamond,location=slot.weapon.mainhand}]",
            "@e[hasitem={item=diamond,quantity=5..10,location=slot.weapon.mainhand}]",
            "@e[hasitem=[{item=diamond},{item=iron}]]",
            "@e[hasitem=[{item=diamond,quantity=5..10},{item=iron,quantity=3..7}]]"
        )

        complexHasitemCases.forEach { selector ->
            testSelector(selector)
        }
    }

    @Test
    fun testNegativeAndInvertedParameters() {
        println("\n${"=".repeat(80)}")
        println("测试否定和反选参数")
        println("=".repeat(80))

        val negativeCases = listOf(
            "@a[gamemode=!survival]",
            "@a[gamemode=!creative]",
            "@a[gamemode=!adventure]",
            "@a[gamemode=!spectator]",
            "@a[name=!Steve]",
            "@a[tag=!test]",
            "@e[type=!zombie]"
        )

        negativeCases.forEach { selector ->
            testSelector(selector)
        }
    }

    @Test
    fun testRangeParameters() {
        println("\n${"=".repeat(80)}")
        println("测试范围参数")
        println("=".repeat(80))

        val rangeCases = listOf(
            // distance参数的各种范围格式
            "@a[distance=5]",
            "@a[distance=5..10]",
            "@a[distance=5..]",
            "@a[distance=..10]",
            "@a[distance=0..]",
            "@a[distance=..0]",

            // x_rotation参数的各种范围格式
            "@e[x_rotation=-45]",
            "@e[x_rotation=-45..45]",
            "@e[x_rotation=-45..]",
            "@e[x_rotation=..45]",
            "@e[x_rotation=-180..180]",

            // y_rotation参数的各种范围格式
            "@e[y_rotation=-90]",
            "@e[y_rotation=-90..90]",
            "@e[y_rotation=-90..]",
            "@e[y_rotation=..90]",
            "@e[y_rotation=-180..180]",

            // level参数的各种范围格式
            "@e[level=5]",
            "@e[level=5..10]",
            "@e[level=5..]",
            "@e[level=..10]",
            "@e[level=0..100]"
        )

        rangeCases.forEach { selector ->
            testSelector(selector)
        }
    }

    @Test
    fun testParameterSpaces() {
        println("\n${"=".repeat(80)}")
        println("测试参数中的空格处理")
        println("=".repeat(80))

        val spaceCases = listOf(
            // 标准格式（无空格）
            "@a[gamemode=survival,name=Steve]",
            // 等号后有空格
            "@a[gamemode= survival, name= Steve]",
            // 逗号后有空格
            "@a[gamemode=survival ,name=Steve ]",
            // 多种空格组合
            "@a[ gamemode = survival , name = Steve ]"
        )

        spaceCases.forEach { selector ->
            testSelector(selector)
        }
    }

    @Test
    fun testVersionSpecificParametersRemoved() {
        println("\n${"=".repeat(80)}")
        println("测试版本特定参数的移除")
        println("=".repeat(80))

        // Java版特有参数在基岩版中应该被移除
        val javaSpecificToBedrock = listOf(
            "@a[team=red]" to "team",
            "@e[predicate=test]" to "predicate",
            "@a[advancements={test=true}]" to "advancements"
        )

        javaSpecificToBedrock.forEach { (selector, param) ->
            println("\n测试Java版参数'$param'在基岩版中的移除")
            val (filteredBedrock, bedrockReminders) = SelectorConverter.filterSelectorParameters(selector, SelectorType.BEDROCK)
            println("过滤后的基岩版选择器: $filteredBedrock")
            println("提醒信息: $bedrockReminders")
            assertFalse("过滤后的选择器不应该包含'$param'参数", filteredBedrock.contains(param))
            assertTrue("应该有移除提醒", bedrockReminders.isNotEmpty())
        }

        // 基岩版特有参数在Java版中应该被移除
        val bedrockSpecificToJava = listOf(
            "@e[family=monster]" to "family",
            "@a[haspermission=false]" to "haspermission",
            "@e[has_property=false]" to "has_property"
        )

        bedrockSpecificToJava.forEach { (selector, param) ->
            println("\n测试基岩版参数'$param'在Java版中的移除")
            val (filteredJava, javaReminders) = SelectorConverter.filterSelectorParameters(selector, SelectorType.JAVA)
            println("过滤后的Java版选择器: $filteredJava")
            println("提醒信息: $javaReminders")
            assertFalse("过滤后的选择器不应该包含'$param'参数", filteredJava.contains(param))
            assertTrue("应该有移除提醒", javaReminders.isNotEmpty())
        }
    }

    @Test
    fun testSpectatorGamemodeConversion() {
        println("\n${"=".repeat(80)}")
        println("测试旁观模式转换")
        println("=".repeat(80))

        val spectatorCases = listOf(
            "@a[gamemode=spectator]",
            "@a[gamemode=!spectator]"
        )

        spectatorCases.forEach { selector ->
            println("\n测试: $selector")
            val (filteredBedrock, bedrockReminders) = SelectorConverter.filterSelectorParameters(selector, SelectorType.BEDROCK)
            println("过滤后的基岩版选择器: $filteredBedrock")
            println("提醒信息: $bedrockReminders")
            
            // 验证spectator被转换为survival
            assertFalse("不应该包含spectator", filteredBedrock.contains("spectator"))
            assertTrue("应该包含survival", filteredBedrock.contains("survival"))
            assertTrue("应该有转换提醒", bedrockReminders.any { it.contains("spectator") })
        }
    }

    @Test
    fun testSortParameterRemoval() {
        println("\n${"=".repeat(80)}")
        println("测试sort参数的移除")
        println("=".repeat(80))

        val sortCases = listOf(
            "@e[limit=4,sort=nearest]",
            "@e[limit=4,sort=furthest]",
            "@e[limit=4,sort=random]",
            "@e[limit=4,sort=arbitrary]"
        )

        sortCases.forEach { selector ->
            println("\n测试: $selector")
            val (filteredBedrock, bedrockReminders) = SelectorConverter.filterSelectorParameters(selector, SelectorType.BEDROCK)
            println("过滤后的基岩版选择器: $filteredBedrock")
            println("提醒信息: $bedrockReminders")
            
            // 验证sort被移除
            assertFalse("不应该包含sort参数", filteredBedrock.contains("sort"))
            assertTrue("应该有移除提醒", bedrockReminders.any { it.contains("sort") })
        }
    }

    @Test
    fun testMultipleConversions() {
        println("\n${"=".repeat(80)}")
        println("测试多次转换链")
        println("=".repeat(80))

        // Java版 -> 基岩版 -> Java版
        val originalJava = "@a[distance=5..10,gamemode=survival,level=1..10]"
        println("原始Java版: $originalJava")
        
        val result1 = SelectorConverter.convertJavaToBedrock(originalJava)
        println("转换为基岩版: ${result1.bedrockSelector}")
        println("提醒: ${result1.bedrockReminders}")
        
        val result2 = SelectorConverter.convertBedrockToJava(result1.bedrockSelector)
        println("转换回Java版: ${result2.javaSelector}")
        println("提醒: ${result2.javaReminders}")

        // 基岩版 -> Java版 -> 基岩版
        val originalBedrock = "@a[rm=5,r=10,m=survival,lm=1,l=10]"
        println("\n原始基岩版: $originalBedrock")
        
        val result3 = SelectorConverter.convertBedrockToJava(originalBedrock)
        println("转换为Java版: ${result3.javaSelector}")
        println("提醒: ${result3.javaReminders}")
        
        val result4 = SelectorConverter.convertJavaToBedrock(result3.javaSelector)
        println("转换回基岩版: ${result4.bedrockSelector}")
        println("提醒: ${result4.bedrockReminders}")
    }
}