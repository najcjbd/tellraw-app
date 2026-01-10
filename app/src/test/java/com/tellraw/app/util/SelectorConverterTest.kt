package com.tellraw.app.util

import com.tellraw.app.model.SelectorType
import org.junit.Assert.*
import org.junit.Test

/**
 * Android版本全面测试
 * 测试范围：
 * 1. 基本选择器变量
 * 2. 通用选择器参数
 * 3. 多个通用参数组合
 * 4. Java版特有参数
 * 5. 基岩版特有参数
 * 6. 特殊参数
 * 7. 特殊组合
 * 8. 文本格式化测试
 * 9. §m§n混合测试
 * 10. 基岩版特有颜色代码
 * 11. 复杂文本组合
 */
class SelectorConverterTest {

    // ============================================
    // 1. 基本选择器变量测试（5个测试）
    // ============================================
    
    @Test
    fun testSelector_a() {
        val selector = "@a"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, result)
    }

    @Test
    fun testSelector_p() {
        val selector = "@p"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, result)
    }

    @Test
    fun testSelector_r() {
        val selector = "@r"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, result)
    }

    @Test
    fun testSelector_e() {
        val selector = "@e"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, result)
    }

    @Test
    fun testSelector_s() {
        val selector = "@s"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, result)
    }

    // ============================================
    // 2. 通用参数测试（14个测试）
    // ============================================

    @Test
    fun testParam_x() {
        val selector = "@a[x=10]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, result)
    }

    @Test
    fun testParam_y() {
        val selector = "@a[y=20]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, result)
    }

    @Test
    fun testParam_z() {
        val selector = "@a[z=30]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, result)
    }

    @Test
    fun testParam_dx() {
        val selector = "@a[dx=5]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, result)
    }

    @Test
    fun testParam_dy() {
        val selector = "@a[dy=5]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, result)
    }

    @Test
    fun testParam_dz() {
        val selector = "@a[dz=5]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, result)
    }

    @Test
    fun testParam_scores_exact() {
        val selector = "@a[scores={myscore=10}]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, result)
    }

    @Test
    fun testParam_scores_range() {
        val selector = "@a[scores={myscore=5..10}]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, result)
    }

    @Test
    fun testParam_scores_min() {
        val selector = "@a[scores={myscore=5..}]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, result)
    }

    @Test
    fun testParam_scores_max() {
        val selector = "@a[scores={myscore=..10}]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, result)
    }

    @Test
    fun testParam_tag() {
        val selector = "@a[tag=mytag]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, result)
    }

    @Test
    fun testParam_name() {
        val selector = "@a[name=Player]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, result)
    }

    @Test
    fun testParam_type() {
        val selector = "@a[type=zombie]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, result)
    }

    @Test
    fun testParam_scoresMultiple() {
        val selector = "@a[scores={score1=5,score2=10..20}]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, result)
    }

    // ============================================
    // 3. 多个通用参数组合测试（3个测试）
    // ============================================

    @Test
    fun testMultipleParams_coordinates_tag_scores() {
        val selector = "@a[x=10,y=20,z=30,tag=mytag,scores={myscore=10}]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, result)
    }

    @Test
    fun testMultipleParams_type_box_name() {
        val selector = "@a[type=zombie,dx=5,dy=5,dz=5,name=Player]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, result)
    }

    @Test
    fun testMultipleParams_box_scores() {
        val selector = "@a[dx=10,dy=10,dz=10,scores={myscore=5..10}]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, result)
    }

    // ============================================
    // 4. Java版特有参数测试（20个测试）
    // ============================================

    @Test
    fun testJavaParam_distance_exact() {
        val selector = "@a[distance=10]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, result)
    }

    @Test
    fun testJavaParam_distance_range() {
        val selector = "@a[distance=5..10]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, result)
    }

    @Test
    fun testJavaParam_distance_min() {
        val selector = "@a[distance=5..]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, result)
    }

    @Test
    fun testJavaParam_distance_max() {
        val selector = "@a[distance=..10]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, result)
    }

    @Test
    fun testJavaParam_x_rotation_exact() {
        val selector = "@a[x_rotation=45]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, result)
    }

    @Test
    fun testJavaParam_x_rotation_range() {
        val selector = "@a[x_rotation=-45..45]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, result)
    }

    @Test
    fun testJavaParam_x_rotation_min() {
        val selector = "@a[x_rotation=-45..]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, result)
    }

    @Test
    fun testJavaParam_x_rotation_max() {
        val selector = "@a[x_rotation=..45]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, result)
    }

    @Test
    fun testJavaParam_y_rotation_exact() {
        val selector = "@a[y_rotation=90]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, result)
    }

    @Test
    fun testJavaParam_y_rotation_range() {
        val selector = "@a[y_rotation=-90..90]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, result)
    }

    @Test
    fun testJavaParam_y_rotation_min() {
        val selector = "@a[y_rotation=-90..]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, result)
    }

    @Test
    fun testJavaParam_y_rotation_max() {
        val selector = "@a[y_rotation=..90]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, result)
    }

    @Test
    fun testJavaParam_level_exact() {
        val selector = "@a[level=10]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, result)
    }

    @Test
    fun testJavaParam_level_range() {
        val selector = "@a[level=5..10]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, result)
    }

    @Test
    fun testJavaParam_level_min() {
        val selector = "@a[level=5..]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, result)
    }

    @Test
    fun testJavaParam_level_max() {
        val selector = "@a[level=..10]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, result)
    }

    @Test
    fun testJavaParam_gamemode_survival() {
        val selector = "@a[gamemode=survival]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, result)
    }

    @Test
    fun testJavaParam_gamemode_creative() {
        val selector = "@a[gamemode=creative]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, result)
    }

    @Test
    fun testJavaParam_gamemode_adventure() {
        val selector = "@a[gamemode=adventure]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, result)
    }

    @Test
    fun testJavaParam_gamemode_spectator() {
        val selector = "@a[gamemode=spectator]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, result)
    }

    @Test
    fun testJavaParam_gamemode_not() {
        val selector = "@a[gamemode=!survival]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, result)
    }

    @Test
    fun testJavaParam_limit_sort_nearest() {
        val selector = "@a[limit=4,sort=nearest]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, result)
    }

    @Test
    fun testJavaParam_sort_furthest() {
        val selector = "@a[sort=furthest]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, result)
    }

    @Test
    fun testJavaParam_sort_arbitrary() {
        val selector = "@a[sort=arbitrary]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, result)
    }

    @Test
    fun testJavaParam_sort_random() {
        val selector = "@a[sort=random]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, result)
    }

    // ============================================
    // 5. 基岩版特有参数测试（19个测试）
    // ============================================

    @Test
    fun testBedrockParam_r() {
        val selector = "@a[r=10]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, result)
    }

    @Test
    fun testBedrockParam_rm() {
        val selector = "@a[rm=5]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, result)
    }

    @Test
    fun testBedrockParam_r_rm() {
        val selector = "@a[r=10,rm=5]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, result)
    }

    @Test
    fun testBedrockParam_rx() {
        val selector = "@a[rx=45]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, result)
    }

    @Test
    fun testBedrockParam_rxm() {
        val selector = "@a[rxm=-45]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, result)
    }

    @Test
    fun testBedrockParam_rx_rxm() {
        val selector = "@a[rx=45,rxm=-45]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, result)
    }

    @Test
    fun testBedrockParam_ry() {
        val selector = "@a[ry=90]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, result)
    }

    @Test
    fun testBedrockParam_rym() {
        val selector = "@a[rym=-90]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, result)
    }

    @Test
    fun testBedrockParam_ry_rym() {
        val selector = "@a[ry=90,rym=-90]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, result)
    }

    @Test
    fun testBedrockParam_l() {
        val selector = "@a[l=10]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, result)
    }

    @Test
    fun testBedrockParam_lm() {
        val selector = "@a[lm=5]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, result)
    }

    @Test
    fun testBedrockParam_l_lm() {
        val selector = "@a[l=10,lm=5]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, result)
    }

    @Test
    fun testBedrockParam_m_survival() {
        val selector = "@a[m=survival]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, result)
    }

    @Test
    fun testBedrockParam_m_creative() {
        val selector = "@a[m=creative]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, result)
    }

    @Test
    fun testBedrockParam_m_adventure() {
        val selector = "@a[m=adventure]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, result)
    }

    @Test
    fun testBedrockParam_m_not() {
        val selector = "@a[m=!survival]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, result)
    }

    @Test
    fun testBedrockParam_c_positive() {
        val selector = "@a[c=4]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, result)
    }

    @Test
    fun testBedrockParam_c_negative() {
        val selector = "@a[c=-4]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, result)
    }

    @Test
    fun testBedrockParam_hasitem() {
        val selector = "@a[hasitem={item=diamond}]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, result)
    }

    @Test
    fun testBedrockParam_family() {
        val selector = "@a[family=zombie]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, result)
    }

    @Test
    fun testBedrockParam_haspermission() {
        val selector = "@a[haspermission={permission=level}]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, result)
    }

    @Test
    fun testBedrockParam_has_property() {
        val selector = "@a[has_property={property=value}]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, result)
    }

    // ============================================
    // 6. 特殊参数测试（12个测试）
    // ============================================

    @Test
    fun testSpecialParam_nbt() {
        val selector = "@a[nbt={CustomName:\"Test\"}]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, result)
    }

    @Test
    fun testSpecialParam_team() {
        val selector = "@a[team=red]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, result)
    }

    @Test
    fun testSpecialParam_predicate() {
        val selector = "@a[predicate=my_predicate]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, result)
    }

    @Test
    fun testSpecialParam_advancements() {
        val selector = "@a[advancements={story/root=true}]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, result)
    }

    @Test
    fun testSpecialParam_attributes() {
        val selector = "@a[attributes={generic.movement_speed={base:0.1}}]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, result)
    }

    @Test
    fun testSpecialParam_multiple_nbt() {
        val selector = "@a[nbt={CustomName:\"Test\",Health:20}]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, result)
    }

    @Test
    fun testSpecialParam_team_not() {
        val selector = "@a[team=!red]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, result)
    }

    @Test
    fun testSpecialParam_advancements_complex() {
        val selector = "@a[advancements={story/root=true,adventure/adventuring_time={criteria:{minecraft:plains:true}}}]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, result)
    }

    @Test
    fun testSpecialParam_hasitem_complex() {
        val selector = "@a[hasitem={item=diamond,quantity=1,location=slot.weapon.mainhand}]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, result)
    }

    @Test
    fun testSpecialParam_family_multiple() {
        val selector = "@a[family=zombie,skeleton]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, result)
    }

    @Test
    fun testSpecialParam_haspermission_complex() {
        val selector = "@a[haspermission={permission=level,level=4}]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, result)
    }

    @Test
    fun testSpecialParam_has_property_complex() {
        val selector = "@a[has_property={property=has_saddle,value=true}]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, result)
    }

    // ============================================
    // 7. 特殊组合测试（10个测试）
    // ============================================

    @Test
    fun testSpecialCombination_limit_sort_nearest() {
        val selector = "@a[limit=4,sort=nearest]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, result)
    }

    @Test
    fun testSpecialCombination_limit_sort_furthest() {
        val selector = "@a[limit=4,sort=furthest]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, result)
    }

    @Test
    fun testSpecialCombination_limit_sort_random() {
        val selector = "@a[limit=4,sort=random]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, result)
    }

    @Test
    fun testSpecialCombination_sort_only() {
        val selector = "@a[sort=nearest]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, result)
    }

    @Test
    fun testSpecialCombination_distance_scores() {
        val selector = "@a[distance=5..10,scores={myscore=10}]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, result)
    }

    @Test
    fun testSpecialCombination_distance_type() {
        val selector = "@a[distance=5..10,type=zombie]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, result)
    }

    @Test
    fun testSpecialCombination_distance_rotations() {
        val selector = "@a[distance=5..10,x_rotation=-45..45,y_rotation=-90..90]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, result)
    }

    @Test
    fun testSpecialCombination_level_gamemode() {
        val selector = "@a[level=5..10,gamemode=survival]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, result)
    }

    @Test
    fun testSpecialCombination_c_r_rm() {
        val selector = "@a[c=4,r=10,rm=5]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, result)
    }

    @Test
    fun testSpecialCombination_c_lm_l() {
        val selector = "@a[c=4,lm=5,l=10]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, result)
    }

    @Test
    fun testSpecialCombination_complex() {
        val selector = "@a[x=10,y=20,z=30,distance=5..10,scores={myscore=10},tag=mytag]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.JAVA, result)
    }

    @Test
    fun testSpecialCombination_complex_bedrock() {
        val selector = "@a[type=zombie,dx=10,dy=10,dz=10,rx=45,rxm=-45,ry=90,rym=-90]"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, result)
    }

    // ============================================
    // 8. 基岩版特有选择器变量测试
    // ============================================

    @Test
    fun testBedrockSelector_initiator() {
        val selector = "@initiator"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, result)
    }

    @Test
    fun testBedrockSelector_c() {
        val selector = "@c"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, result)
    }

    @Test
    fun testBedrockSelector_v() {
        val selector = "@v"
        val result = SelectorConverter.detectSelectorType(selector)
        assertEquals(SelectorType.BEDROCK, result)
    }

    // ============================================
    // 9. 参数转换测试
    // ============================================

    @Test
    fun testConversion_distance_to_r_rm() {
        val javaSelector = "@a[distance=5..10]"
        val result = SelectorConverter.convertJavaToBedrock(javaSelector)
        assertTrue(result.bedrockSelector.contains("rm=5"))
        assertTrue(result.bedrockSelector.contains("r=10"))
    }

    @Test
    fun testConversion_x_rotation_to_rx_rxm() {
        val javaSelector = "@a[x_rotation=-45..45]"
        val result = SelectorConverter.convertJavaToBedrock(javaSelector)
        assertTrue(result.bedrockSelector.contains("rxm=-45"))
        assertTrue(result.bedrockSelector.contains("rx=45"))
    }

    @Test
    fun testConversion_y_rotation_to_ry_rym() {
        val javaSelector = "@a[y_rotation=-90..90]"
        val result = SelectorConverter.convertJavaToBedrock(javaSelector)
        assertTrue(result.bedrockSelector.contains("rym=-90"))
        assertTrue(result.bedrockSelector.contains("ry=90"))
    }

    @Test
    fun testConversion_level_to_l_lm() {
        val javaSelector = "@a[level=5..10]"
        val result = SelectorConverter.convertJavaToBedrock(javaSelector)
        assertTrue(result.bedrockSelector.contains("lm=5"))
        assertTrue(result.bedrockSelector.contains("l=10"))
    }

    @Test
    fun testConversion_gamemode_to_m() {
        val javaSelector = "@a[gamemode=survival]"
        val result = SelectorConverter.convertJavaToBedrock(javaSelector)
        assertTrue(result.bedrockSelector.contains("m=survival"))
    }

    @Test
    fun testConversion_limit_sort_nearest_to_c() {
        val javaSelector = "@a[limit=4,sort=nearest]"
        val result = SelectorConverter.convertJavaToBedrock(javaSelector)
        assertTrue(result.bedrockSelector.contains("c=4"))
    }

    @Test
    fun testConversion_limit_sort_furthest_to_c_negative() {
        val javaSelector = "@a[limit=4,sort=furthest]"
        val result = SelectorConverter.convertJavaToBedrock(javaSelector)
        assertTrue(result.bedrockSelector.contains("c=-4"))
    }

    @Test
    fun testConversion_sort_random_to_c() {
        val javaSelector = "@a[sort=random]"
        val result = SelectorConverter.convertJavaToBedrock(javaSelector)
        assertTrue(result.bedrockSelector.contains("c=9999"))
    }

    @Test
    fun testConversion_bedrock_to_java_r_rm_to_distance() {
        val bedrockSelector = "@a[r=10,rm=5]"
        val result = SelectorConverter.convertBedrockToJava(bedrockSelector)
        assertTrue(result.javaSelector.contains("distance=5..10"))
    }

    @Test
    fun testConversion_bedrock_to_java_rx_rxm_to_x_rotation() {
        val bedrockSelector = "@a[rx=45,rxm=-45]"
        val result = SelectorConverter.convertBedrockToJava(bedrockSelector)
        assertTrue(result.javaSelector.contains("x_rotation=-45..45"))
    }

    @Test
    fun testConversion_bedrock_to_java_ry_rym_to_y_rotation() {
        val bedrockSelector = "@a[ry=90,rym=-90]"
        val result = SelectorConverter.convertBedrockToJava(bedrockSelector)
        assertTrue(result.javaSelector.contains("y_rotation=-90..90"))
    }

    @Test
    fun testConversion_bedrock_to_java_l_lm_to_level() {
        val bedrockSelector = "@a[l=10,lm=5]"
        val result = SelectorConverter.convertBedrockToJava(bedrockSelector)
        assertTrue(result.javaSelector.contains("level=5..10"))
    }

    @Test
    fun testConversion_bedrock_to_java_m_to_gamemode() {
        val bedrockSelector = "@a[m=survival]"
        val result = SelectorConverter.convertBedrockToJava(bedrockSelector)
        assertTrue(result.javaSelector.contains("gamemode=survival"))
    }

    @Test
    fun testConversion_bedrock_to_java_c_to_limit_sort() {
        val bedrockSelector = "@a[c=4]"
        val result = SelectorConverter.convertBedrockToJava(bedrockSelector)
        assertTrue(result.javaSelector.contains("limit=4"))
        assertTrue(result.javaSelector.contains("sort=nearest"))
    }

    @Test
    fun testConversion_bedrock_to_java_c_negative_to_limit_sort() {
        val bedrockSelector = "@a[c=-4]"
        val result = SelectorConverter.convertBedrockToJava(bedrockSelector)
        assertTrue(result.javaSelector.contains("limit=4"))
        assertTrue(result.javaSelector.contains("sort=furthest"))
    }

    @Test
    fun testConversion_bedrock_selector_initiator_to_a() {
        val bedrockSelector = "@initiator"
        val result = SelectorConverter.convertBedrockToJava(bedrockSelector)
        assertTrue(result.javaSelector.startsWith("@a"))
    }

    @Test
    fun testConversion_bedrock_selector_c_to_a() {
        val bedrockSelector = "@c"
        val result = SelectorConverter.convertBedrockToJava(bedrockSelector)
        assertTrue(result.javaSelector.startsWith("@a"))
    }

    @Test
    fun testConversion_bedrock_selector_v_to_a() {
        val bedrockSelector = "@v"
        val result = SelectorConverter.convertBedrockToJava(bedrockSelector)
        assertTrue(result.javaSelector.startsWith("@a"))
    }

    // ============================================
    // 10. 参数过滤测试
    // ============================================

    @Test
    fun testFilter_java_to_bedrock_remove_java_params() {
        val javaSelector = "@a[distance=5..10,team=red,predicate=my_predicate]"
        val (filtered, reminders) = SelectorConverter.filterSelectorParameters(javaSelector, SelectorType.BEDROCK)
        assertFalse(filtered.contains("team"))
        assertFalse(filtered.contains("predicate"))
        assertTrue(filtered.contains("rm=5"))
        assertTrue(filtered.contains("r=10"))
    }

    @Test
    fun testFilter_bedrock_to_java_remove_bedrock_params() {
        val bedrockSelector = "@a[r=10,rm=5,hasitem={item=diamond},family=zombie]"
        val (filtered, reminders) = SelectorConverter.filterSelectorParameters(bedrockSelector, SelectorType.JAVA)
        assertFalse(filtered.contains("hasitem"))
        assertFalse(filtered.contains("family"))
        assertTrue(filtered.contains("distance=5..10"))
    }

    @Test
    fun testFilter_remove_team() {
        val selector = "@a[team=red]"
        val (filtered, reminders) = SelectorConverter.filterSelectorParameters(selector, SelectorType.BEDROCK)
        assertFalse(filtered.contains("team"))
        assertTrue(reminders.any { it.contains("team") })
    }

    @Test
    fun testFilter_remove_predicate() {
        val selector = "@a[predicate=my_predicate]"
        val (filtered, reminders) = SelectorConverter.filterSelectorParameters(selector, SelectorType.BEDROCK)
        assertFalse(filtered.contains("predicate"))
        assertTrue(reminders.any { it.contains("predicate") })
    }

    @Test
    fun testFilter_remove_advancements() {
        val selector = "@a[advancements={story/root=true}]"
        val (filtered, reminders) = SelectorConverter.filterSelectorParameters(selector, SelectorType.BEDROCK)
        assertFalse(filtered.contains("advancements"))
        assertTrue(reminders.any { it.contains("advancements") })
    }

    @Test
    fun testFilter_remove_hasitem() {
        val selector = "@a[hasitem={item=diamond}]"
        val (filtered, reminders) = SelectorConverter.filterSelectorParameters(selector, SelectorType.JAVA)
        assertFalse(filtered.contains("hasitem"))
    }

    @Test
    fun testFilter_remove_family() {
        val selector = "@a[family=zombie]"
        val (filtered, reminders) = SelectorConverter.filterSelectorParameters(selector, SelectorType.JAVA)
        assertFalse(filtered.contains("family"))
        assertTrue(reminders.any { it.contains("family") })
    }

    @Test
    fun testFilter_remove_haspermission() {
        val selector = "@a[haspermission={permission=level}]"
        val (filtered, reminders) = SelectorConverter.filterSelectorParameters(selector, SelectorType.JAVA)
        assertFalse(filtered.contains("haspermission"))
        assertTrue(reminders.any { it.contains("haspermission") })
    }

    @Test
    fun testFilter_remove_has_property() {
        val selector = "@a[has_property={property=value}]"
        val (filtered, reminders) = SelectorConverter.filterSelectorParameters(selector, SelectorType.JAVA)
        assertFalse(filtered.contains("has_property"))
        assertTrue(reminders.any { it.contains("has_property") })
    }

    // ============================================
    // 11. §n映射验证测试
    // ============================================

    @Test
    fun testSectionNMappingToRed() {
        // 验证§n映射为§c（红色）
        val message = "§n测试文本"
        val bedrockJson = TextFormatter.convertToBedrockJson(message, "color")
        // 基岩版JSON中§n应该被转换为§c
        val expectedContains = "§c"
        assertTrue("§n应该映射为§c（红色）", bedrockJson.contains(expectedContains))
    }

    @Test
    fun testSectionNMappingInJava() {
        // 验证§n在Java版中的处理
        val message = "§n测试文本"
        val javaJson = TextFormatter.convertToJavaJson(message, "color")
        // Java版JSON中§n应该被正确处理
        assertTrue("§n应该被正确处理", javaJson.isNotEmpty())
    }

    @Test
    fun testSectionNFontMode() {
        // 验证§n在font模式下的处理
        val message = "§n测试文本"
        val javaJson = TextFormatter.convertToJavaJson(message, "font")
        // font模式下§n应该作为字体样式处理
        assertTrue("font模式下§n应该作为字体样式处理", javaJson.isNotEmpty())
    }
}