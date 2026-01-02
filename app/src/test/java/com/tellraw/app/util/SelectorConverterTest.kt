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
}
