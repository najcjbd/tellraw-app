package com.tellraw.app.util

import android.content.Context
import com.tellraw.app.R
import com.tellraw.app.model.*
import java.util.regex.Pattern
import kotlin.math.roundToInt

object SelectorConverter {
    
    // Java版特有参数
    private val JAVA_SPECIFIC_PARAMS = listOf(
        "distance", "x_rotation", "y_rotation", "nbt", "team", "limit", "sort",
        "predicate", "advancements", "level", "gamemode", "attributes"
    )
    
    // 基岩版特有参数
    private val BEDROCK_SPECIFIC_PARAMS = listOf(
        "r", "rm", "rx", "rxm", "ry", "rym", "hasitem", "family", "l", "lm",
        "m", "haspermission", "has_property", "c"
    )
    
    // 基岩版特有选择器变量
    private val BEDROCK_SPECIFIC_SELECTORS = listOf("@initiator", "@c", "@v")
    
    // 颜色代码映射表 - Java版到基岩版的对应关系
    private val JAVA_COLORS = mapOf(
        "black" to "§0",
        "dark_blue" to "§1", 
        "dark_green" to "§2",
        "dark_aqua" to "§3",
        "dark_red" to "§4",
        "dark_purple" to "§5",
        "gold" to "§6",
        "gray" to "§7",
        "dark_gray" to "§8",
        "blue" to "§9",
        "green" to "§a",
        "aqua" to "§b",
        "red" to "§c",
        "light_purple" to "§d",
        "yellow" to "§e",
        "white" to "§f"
    )
    
    // 基岩版特有颜色代码映射
    private val BEDROCK_COLORS = mapOf(
        "§g" to "§6",  // minecoin_gold -> gold
        "§h" to "§f",  // material_quartz -> white
        "§i" to "§7",  // material_iron -> gray
        "§j" to "§8",  // material_netherite -> dark_gray
        "§m" to "§4",  // material_redstone -> dark_red (特殊处理)
        "§n" to "§c",  // material_copper -> red (特殊处理)
        "§p" to "§6",  // material_gold -> gold
        "§q" to "§a",  // material_emerald -> green
        "§s" to "§b",  // material_diamond -> aqua
        "§t" to "§1",  // material_lapis -> dark_blue
        "§u" to "§d",  // material_amethyst -> light_purple
        "§v" to "§6",  // material_resin -> gold,
    )

    // 字符串资源的默认值映射（用于测试环境中资源不可用时）
    private val STRING_DEFAULTS = mapOf(
        R.string.bedrock_selector_converted to "基岩版选择器 %s 在Java版不支持，已转换为 %s",
        R.string.bedrock_scores_negation_removed to "基岩版scores反选参数%s在Java版不支持，已移除",
        R.string.java_sort_nearest_converted to "Java版sort=nearest已转换为基岩版c=%s",
        R.string.java_sort_furthest_converted to "Java版sort=furthest已转换为基岩版c=%s",
        R.string.java_sort_furthest_converted_all to "Java版sort=furthest已转换为基岩版c=-9999。注意：sort=furthest是按顺序从远到近的全部，c=-9999是按顺序从远到近的最近9999个实体",
        R.string.java_sort_arbitrary_not_supported to "Java版sort=arbitrary在基岩版不支持，已移除",
        R.string.java_sort_random_converted to "Java版%1\$s[sort=random]已转换为基岩版@r[c=%2\$s]",
        R.string.java_sort_random_to_c to "Java版sort=random已转换为基岩版c=%s",
        R.string.java_sort_not_supported to "Java版sort=%s在基岩版不支持，已移除",
        R.string.java_limit_converted to "Java版limit=%s已转换为基岩版c=%s",
        R.string.limit_description to "limit限制数量，c由近到远",
        R.string.bedrock_param_no_equivalent to "警告：基岩版%s参数在Java版无对应功能，已移除",
        R.string.bedrock_family_param_not_supported to "警告：基岩版%s参数在Java版无对应功能，已移除。建议用type参数指定实体类型",
        R.string.java_team_param_not_supported to "警告：Java版%s参数在基岩版不支持，已移除。基岩版无队伍系统",
        R.string.java_predicate_param_not_supported to "警告：Java版%s参数在基岩版不支持，已移除。基岩版无谓词系统",
        R.string.java_advancements_param_not_supported to "警告：Java版%s参数在基岩版不支持，已移除。基岩版无进度系统",
        R.string.java_param_not_supported to "警告：Java版%s参数在基岩版不支持，已移除",
        R.string.bedrock_param_not_supported to "警告：基岩版%s参数在Java版不支持，已移除",
        R.string.java_distance_converted to "Java版distance=%s已转换为基岩版rm=%s,r=%s",
        R.string.java_distance_to_rm to "Java版distance=%s已转换为基岩版rm=%s",
        R.string.java_distance_to_r to "Java版distance=%s已转换为基岩版r=%s",
        R.string.java_distance_exact to "Java版distance=%s已转换为基岩版rm=%s,r=%s",
        R.string.java_x_rotation_converted to "Java版x_rotation=%s已转换为基岩版rxm=%s,rx=%s",
        R.string.java_x_rotation_to_rxm to "Java版x_rotation=%s已转换为基岩版rxm=%s",
        R.string.java_x_rotation_to_rx to "Java版x_rotation=%s已转换为基岩版rx=%s",
        R.string.java_x_rotation_exact to "Java版x_rotation=%s已转换为基岩版rxm=%s,rx=%s",
        R.string.java_y_rotation_converted to "Java版y_rotation=%s已转换为基岩版rym=%s,ry=%s",
        R.string.java_y_rotation_to_rym to "Java版y_rotation=%s已转换为基岩版rym=%s",
        R.string.java_y_rotation_to_ry to "Java版y_rotation=%s已转换为基岩版ry=%s",
        R.string.java_y_rotation_exact to "Java版y_rotation=%s已转换为基岩版rym=%s,ry=%s",
        R.string.java_level_converted to "Java版level=%s已转换为基岩版lm=%s,l=%s",
        R.string.java_level_to_lm to "Java版level=%s已转换为基岩版lm=%s",
        R.string.java_level_to_l to "Java版level=%s已转换为基岩版l=%s",
        R.string.java_level_exact to "Java版level=%s已转换为基岩版lm=%s,l=%s",
        R.string.java_negation_spectator_converted to "Java版反选旁观模式(gamemode=!)在基岩版不支持，已转换为反选生存模式",
        R.string.java_spectator_converted to "Java版旁观模式(gamemode=%s)在基岩版不支持，已转换为生存模式",
        R.string.java_gamemode_converted to "Java版gamemode=%1\$s已转换为基岩版m=%2\$s",
        R.string.bedrock_rm_r_converted to "基岩版rm=%1\$s,r=%2\$s已转换为Java版distance=%3\$s",
        R.string.bedrock_rm_converted to "基岩版rm=%1\$s已转换为Java版distance=%2\$s",
        R.string.bedrock_r_converted to "基岩版r=%1\$s已转换为Java版distance=%2\$s",
        R.string.java_distance_to_bedrock to "Java版distance=%s已转换为基岩版%s",
        R.string.bedrock_rotation_converted to "基岩版%s=%s,%s=%s已转换为Java版%s=%s",
        R.string.bedrock_rotation_min_converted to "基岩版%s=%s已转换为Java版%s=%s",
        R.string.bedrock_rotation_max_converted to "基岩版%s=%s已转换为Java版%s=%s",
        R.string.java_rotation_converted to "Java版%s=%s已转换为基岩版%s=%s,%s=%s",
        R.string.java_rotation_to_min to "Java版%s=%s已转换为基岩版%s=%s",
        R.string.java_rotation_to_max to "Java版%s=%s已转换为基岩版%s=%s",
        R.string.bedrock_lm_l_converted to "基岩版lm=%s,l=%s已转换为Java版level=%s",
        R.string.bedrock_lm_converted to "基岩版lm=%s已转换为Java版level=%s",
        R.string.bedrock_l_converted to "基岩版l=%s已转换为Java版level=%s",
        R.string.java_level_to_bedrock to "Java版level=%s已转换为基岩版%s",
        R.string.bedrock_negation_default_converted to "基岩版反选默认模式(m=!)在Java版不支持，已转换为反选生存模式",
        R.string.bedrock_default_converted to "基岩版默认模式(m=%s)在Java版不支持，已转换为生存模式",
        R.string.java_level_in_scores_converted to "Java版level=%s已转换为基岩版lm=%s,l=%s",
        R.string.bedrock_c_negative_converted to "基岩版c=%s已转换为Java版limit=%s,sort=furthest",
        R.string.bedrock_c_converted to "基岩版c=%1\$s已转换为Java版limit=%2\$s,sort=nearest",
        R.string.java_nbt_param_not_supported to "警告：Java版nbt参数在基岩版不支持，已尝试转换为hasitem，失败则移除",
        R.string.hasitem_converted to "hasitem已转换为nbt格式，可能无法完全保留原意",
        R.string.hasitem_conversion_failed to "hasitem转换失败，保留原参数",
        R.string.nbt_converted to "nbt已转换为hasitem格式，可能无法完全保留原意",
        R.string.hasitem_location_not_supported to "警告：基岩版location=%s在Java版无对应功能，已移除",
        R.string.hasitem_quantity_min_only to "注意：quantity=%s.. 已转换为 %s",
        R.string.hasitem_quantity_max_only to "注意：quantity=..%s 已转换为 %s",
        R.string.hasitem_quantity_range to "注意：quantity=%1\$s..%2\$s 已转换为 %3\$s（中间值）",
        R.string.hasitem_quantity_negation_not_supported to "警告：quantity反选（!）在Java版不支持，已移除",
        R.string.hasitem_slot_min_only to "注意：slot=%s.. 已转换为 %s",
        R.string.hasitem_slot_max_only to "注意：slot=..%s 已转换为 %s",
        R.string.hasitem_slot_negation_not_supported to "警告：slot反选（!）在Java版不支持，已移除"
    )

    /**
     * 安全地获取字符串资源
     * 如果资源不可用（例如在测试环境中），返回默认值
     */
    private fun getStringSafely(context: Context, resId: Int, vararg formatArgs: Any): String {
        return try {
            context.getString(resId, *formatArgs)
        } catch (e: Exception) {
            // 资源不可用时，使用默认值
            val defaultTemplate = STRING_DEFAULTS[resId] ?: "警告：资源ID $resId 不可用"
            return try {
                String.format(defaultTemplate, *formatArgs)
            } catch (e: Exception) {
                // 格式化失败时返回原始模板
                defaultTemplate
            }
        }
    }

    /**
     * 检测 scores 参数中是否有反选
     * 例如：[scores={m=!5}] 包含反选，返回 true
     */
    private fun hasScoresNegation(paramsPart: String): Boolean {
        val scoresPattern = "scores=\\{([^}]*)\\}".toRegex()
        val scoresMatch = scoresPattern.find(paramsPart)
        if (scoresMatch != null) {
            val scoresContent = scoresMatch.groupValues[1]
            // 检查是否有反选模式（!= 或 ! 在值部分）
            val negationPattern = "\\w+\\s*=\\s*!".toRegex()
            return negationPattern.containsMatchIn(scoresContent)
        }
        return false
    }
    
    /**
     * 检测目标选择器是Java版还是基岩版
     */
    fun detectSelectorType(selector: String): SelectorType {
        val selectorVar = selector.split('[')[0]
        
        // 检查基岩版特有选择器变量
        if (selectorVar in BEDROCK_SPECIFIC_SELECTORS) {
            return SelectorType.BEDROCK
        }
        
        if ('[' in selector && ']' in selector) {
            val paramsPart = selector.substringAfter('[').substringBefore(']')
            val params = paramsPart.split(',').map { it.trim() }
            
            var javaCount = 0
            var bedrockCount = 0
            
            // 检查是否有 scores 反选
            val hasScoresNegation = hasScoresNegation(paramsPart)
            if (hasScoresNegation) {
                bedrockCount++
            }
            
            for (param in params) {
                if ('=' in param) {
                    val paramName = param.split('=')[0].trim()
                    if (paramName in JAVA_SPECIFIC_PARAMS) {
                        javaCount++
                    } else if (paramName in BEDROCK_SPECIFIC_PARAMS) {
                        bedrockCount++
                    }
                }
            }
            
            // 检查坐标参数格式 - 基岩版允许空格
            if (" = " in paramsPart || ", " in paramsPart) {
                bedrockCount++
            }
            
            return when {
                javaCount > bedrockCount -> SelectorType.JAVA
                bedrockCount > javaCount -> SelectorType.BEDROCK
                else -> {
                    // 当Java参数和基岩参数数量相等时，尝试使用其他启发式方法
                    // 如果参数完全相等或只有通用参数，使用更细致的判断
                    if (javaCount == 0 && bedrockCount == 0 && '[' in selector && ']' in selector) {
                        // 只有通用参数，返回UNIVERSAL
                        SelectorType.UNIVERSAL
                    } else if (hasScoresNegation) {
                        // 如果有 scores 反选，识别为基岩版
                        SelectorType.BEDROCK
                    } else {
                        // 默认返回bedrock
                        SelectorType.BEDROCK
                    }
                }
            }
        }
        
        return SelectorType.UNIVERSAL // 默认返回通用版
    }
    
    /**
     * 将基岩版选择器转换为Java版
     */
    fun convertBedrockToJava(selector: String, context: android.content.Context): SelectorConversionResult {
        val reminders = mutableListOf<String>()
        
        // 基岩版特有选择器变量到Java版的映射
        val selectorMapping = mapOf(
            "@initiator" to "@a",
            "@c" to "@a",
            "@v" to "@a"
        )
        
        val selectorVar = selector.split('[')[0]
        val paramsPart = if ('[' in selector && ']' in selector) {
            selector.substringAfter('[').substringBefore(']')
        } else ""
        
        var newSelector = selector
        var wasConverted = false
        
        // 转换选择器变量
        if (selectorVar in selectorMapping) {
            newSelector = selectorMapping[selectorVar]!! + if (paramsPart.isNotEmpty()) "[$paramsPart]" else ""
            reminders.add(getStringSafely(context, R.string.bedrock_selector_converted, selectorVar, selectorMapping[selectorVar]!!))
            wasConverted = true
        }
        
        // 转换参数
        val (convertedSelector, removedParams, paramReminders) = filterSelectorParameters(newSelector, SelectorType.JAVA, context)
        reminders.addAll(paramReminders)
        
        // 注意：filterSelectorParameters 已经处理了参数转换，所以不需要再次调用 convertBedrockParametersToJava
        // 直接返回转换后的选择器
        val javaSelector = convertedSelector
        
        // 如果参数发生了变化，也认为发生了转换
        if (javaSelector != newSelector) {
            wasConverted = true
        }
        
        return SelectorConversionResult(
            javaSelector = javaSelector,
            bedrockSelector = selector,
            javaReminders = reminders,
            bedrockReminders = emptyList(),
            wasConverted = wasConverted
        )
    }
    
    /**
     * 将Java版选择器转换为基岩版
     */
    fun convertJavaToBedrock(selector: String, context: Context): SelectorConversionResult {
        val reminders = mutableListOf<String>()
        
        // 转换参数
        val (convertedSelector, removedParams, paramReminders) = filterSelectorParameters(selector, SelectorType.BEDROCK, context)
        reminders.addAll(paramReminders)

        // 注意：filterSelectorParameters 已经处理了参数转换，所以不需要再次调用 convertJavaParametersToBedrock
        // 直接返回转换后的选择器
        var bedrockSelector = convertedSelector

        return SelectorConversionResult(
            javaSelector = selector,
            bedrockSelector = bedrockSelector,
            javaReminders = emptyList(),
            bedrockReminders = reminders,
            wasConverted = false
        )
    }
    
    /**
     * 转换选择器参数格式
     */
    private fun convertSelectorParameters(selector: String): Pair<String, List<String>> {
        val reminders = mutableListOf<String>()

        if ('[' !in selector || ']' !in selector) {
            return selector to reminders
        }

        val selectorVar = selector.split('[')[0]
        var paramsPart = selector.substringAfter('[').substringBefore(']')

        // 去除不必要的空格
        paramsPart = removeUnnecessarySpaces(paramsPart)

        return selectorVar + "[" + paramsPart + "]" to reminders
    }
    
    /**
     * 根据目标版本过滤选择器参数，对于可转换的参数进行转换，
     * 只有完全不支持的参数才被剔除
     * 与Python版本的filter_selector_parameters函数逻辑一致
     */
    fun filterSelectorParameters(selector: String, targetVersion: SelectorType, context: android.content.Context): Triple<String, List<String>, List<String>> {
        val conversionReminders = mutableListOf<String>()
        
        // Java版特有参数（完全不支持，无法转换）
        val javaSpecificParams = listOf(
            "predicate", "advancements", "team"
        )
        
        // 基岩版特有参数（完全不支持，无法转换）
        val bedrockSpecificParams = listOf(
            "haspermission", "has_property", "family"
        )
        
        // 如果没有参数部分，直接返回
        if ('[' !in selector || ']' !in selector) {
            return Triple(selector, emptyList(), conversionReminders)
        }
        
        // 提取参数部分
        var selectorVar = selector.split('[')[0]
        var paramsPart = selector.substringAfter('[').substringBefore(']')

        // 注意：scores 参数内部的记分项不应该被当作选择器参数处理
        // 例如：[scores={level=6}] 中的 level 是记分项名字，不是经验等级参数
        // 只有独立的 level 参数（不在 scores 内部）才需要转换
        // 因此这里不需要处理 scores 参数内部的 level
        
        // 处理 scores 参数的反选（基岩版特有功能）
        if (targetVersion == SelectorType.JAVA && "scores=" in paramsPart) {
            // 查找所有scores参数并检查是否包含反选
            val scoresPattern = "scores=\\{".toRegex()
            val scoresToProcess = mutableListOf<Pair<Int, String>>()  // (startIndex, "scores={...}")
            
            var match = scoresPattern.find(paramsPart)
            while (match != null) {
                val startIndex = match.range.first
                // 提取完整的scores内容
                val scoresContent = extractBraceContent(paramsPart.substring(startIndex + 7))
                if (scoresContent != null) {
                    val fullMatch = "scores={$scoresContent}"
                    scoresToProcess.add(Pair(startIndex, fullMatch))
                }
                match = scoresPattern.find(paramsPart, startIndex + 1)
            }
            
            // 处理每个scores参数
            for ((_, fullMatch) in scoresToProcess) {
                val scoresContent = fullMatch.substring(8, fullMatch.length - 1)  // 去掉 "scores={" 和 "}"
                
                // 检查是否有反选模式（!= 或 ! 在值部分）
                val negationPattern = "\\w+\\s*=\\s*!".toRegex()
                if (negationPattern.containsMatchIn(scoresContent)) {
                    // Java版不支持scores反选，直接移除整个scores参数
                    conversionReminders.add(getStringSafely(context, R.string.bedrock_scores_negation_removed, fullMatch))
                    paramsPart = paramsPart.replace(fullMatch, "")
                }
                // 如果没有反选，scores参数应该被保留（不做什么）
            }
            
            // 清理可能的双逗号
            paramsPart = paramsPart.replace(",,", ",")
            paramsPart = paramsPart.replace(",\\]".toRegex(), "]")
        }

        // 处理hasitem到nbt的转换（基岩版到Java版）
        if (targetVersion == SelectorType.JAVA && "hasitem=" in paramsPart) {
            val (converted, hasitemToNbtReminders) = convertHasitemToNbt(paramsPart, context)
            paramsPart = converted
            conversionReminders.addAll(hasitemToNbtReminders)
        }

        // 处理nbt到hasitem的转换（Java版到基岩版）
        if (targetVersion == SelectorType.BEDROCK && "nbt=" in paramsPart) {
            val (converted, nbtToHasitemReminders) = convertNbtToHasitem(paramsPart, context)
            paramsPart = converted
            conversionReminders.addAll(nbtToHasitemReminders)
        }
        
        // 根据目标版本进行参数转换
        if (targetVersion == SelectorType.JAVA) {
            // 基岩版到Java版的参数转换
            paramsPart = convertR_RmToDistance(paramsPart, conversionReminders, context)
            paramsPart = convertRx_RxmToXRotation(paramsPart, conversionReminders, context)
            paramsPart = convertRy_RymToYRotation(paramsPart, conversionReminders, context)
            paramsPart = convertL_LmToLevel(paramsPart, conversionReminders, context)
            paramsPart = convertMToGamemode(paramsPart, conversionReminders, context)
            paramsPart = convertCToLimitSort(paramsPart, conversionReminders, context)
        } else if (targetVersion == SelectorType.BEDROCK) {
            // Java版到基岩版的参数转换
            paramsPart = convertDistanceParameters(paramsPart, conversionReminders, context)
            paramsPart = convertRotationParameters(paramsPart, conversionReminders, context)
            paramsPart = convertLevelParameters(paramsPart, conversionReminders, context)

            // 处理gamemode到m的转换（Java版到基岩版）
            paramsPart = convertGamemodeToM(paramsPart, conversionReminders, context)

            // 处理sort参数和limit参数的联合转换（Java版到基岩版）
            // 根据sort.txt的要求:
            // 1. sort=arbitrary直接删除(当大选择器为@a或@e时),否则提醒用户
            // 2. sort=furthest没有limit时转c=-9999并提醒,有limit时转c=-limit值
            // 3. c=-x转换成limit=x,sort=furthest(c=-9999也转为limit=9999,sort=furthest)
            // 4. limit=数字没有sort时转c=数字并提醒
            // 5. limit=数字,sort=random时,@a或@r转@r[c=数字],否则只保留c=数字并提醒
            // 6. 只有sort=random没有limit时,@a或@r转@r[c=9999],否则删除sort=random并提醒
            
            val sortPattern = "(^|,)sort=([^,\\]]+)".toRegex()
            val limitPattern = "(^|,)limit=([+-]?\\d+)".toRegex()

            // 先查找sort和limit参数
            val sortMatch = sortPattern.find(paramsPart)
            val limitMatch = limitPattern.find(paramsPart)
            val sortValue: String? = sortMatch?.groupValues?.get(2)
            val limitValue: String? = limitMatch?.groupValues?.get(2)

            // 处理sort和limit参数
            if (sortValue != null) {
                when (sortValue) {
                    "nearest" -> {
                        // 当limit=数字,sort=nearest时，基岩版转换为c=数字
                        // 当只有sort=nearest，没有limit时，基岩版转换为c=9999
                        val cValue = limitValue ?: "9999"
                        paramsPart = paramsPart.replace(sortPattern, "")
                        if (limitValue != null) {
                            paramsPart = paramsPart.replace(limitPattern, "")
                        }
                        // 添加c参数
                        if (Regex("c=[+-]?\\d+").containsMatchIn(paramsPart)) {
                            paramsPart = paramsPart.replace(Regex("c=[+-]?\\d+"), "c=$cValue")
                        } else {
                            paramsPart = if (paramsPart.endsWith("[")) {
                                paramsPart.dropLast(1) + "c=$cValue]"
                            } else if (paramsPart.endsWith("]")) {
                                paramsPart.dropLast(1) + ",c=$cValue]"
                            } else {
                                paramsPart + "c=$cValue"
                            }
                        }
                        conversionReminders.add(getStringSafely(context, R.string.java_sort_nearest_converted, cValue))
                    }
                    "furthest" -> {
                        // 当limit=数字,sort=furthest时，基岩版转换为c=-数字
                        // 当只有sort=furthest，没有limit时，基岩版转换为c=-9999
                        val cValue = if (limitValue != null) "-$limitValue" else "-9999"
                        paramsPart = paramsPart.replace(sortPattern, "")
                        if (limitValue != null) {
                            paramsPart = paramsPart.replace(limitPattern, "")
                        }
                        // 添加c参数
                        if (Regex("c=[+-]?\\d+").containsMatchIn(paramsPart)) {
                            paramsPart = paramsPart.replace(Regex("c=[+-]?\\d+"), "c=$cValue")
                        } else {
                            paramsPart = if (paramsPart.endsWith("[")) {
                                paramsPart.dropLast(1) + "c=$cValue]"
                            } else if (paramsPart.endsWith("]")) {
                                paramsPart.dropLast(1) + ",c=$cValue]"
                            } else {
                                paramsPart + "c=$cValue"
                            }
                        }
                        // 只有在转换为 c=-9999 时才显示说明
                        if (cValue == "-9999") {
                            conversionReminders.add(getStringSafely(context, R.string.java_sort_furthest_converted_all))
                        } else {
                            conversionReminders.add(getStringSafely(context, R.string.java_sort_furthest_converted, cValue))
                        }
                    }
                    "arbitrary" -> {
                        paramsPart = paramsPart.replace(sortPattern, "")
                        // 当大选择器为 @a 或 @e 时，直接删除（不提醒）
                        // 当为其他大选择器时，删除并提醒用户
                        if (selectorVar != "@a" && selectorVar != "@e") {
                            conversionReminders.add(getStringSafely(context, R.string.java_sort_arbitrary_not_supported))
                        }
                    }
                    "random" -> {
                        // 当@a[limit=数字,sort=random]或@r[limit=数字,sort=random]时，转换为@r[c=数字]
                        // 当只有@a[sort=random]或@r[sort=random]时，转换为基岩版的@r[c=9999]
                        val cValue = limitValue ?: "9999"
                        if (selectorVar == "@a" || selectorVar == "@r") {
                            paramsPart = paramsPart.replace(sortPattern, "")
                            if (limitValue != null) {
                                paramsPart = paramsPart.replace(limitPattern, "")
                            }
                            // 添加c参数
                            if (Regex("c=[+-]?\\d+").containsMatchIn(paramsPart)) {
                                paramsPart = paramsPart.replace(Regex("c=[+-]?\\d+"), "c=$cValue")
                            } else {
                                paramsPart = if (paramsPart.endsWith("[")) {
                                    paramsPart.dropLast(1) + "c=$cValue]"
                                } else if (paramsPart.endsWith("]")) {
                                    paramsPart.dropLast(1) + ",c=$cValue]"
                                } else {
                                    paramsPart + "c=$cValue"
                                }
                            }
                            conversionReminders.add(getStringSafely(context, R.string.java_sort_random_converted, selectorVar, cValue))
                            // 当选择器是 @a 时，更新为 @r
                            if (selectorVar == "@a") {
                                selectorVar = "@r"
                            }
                        } else {
                            paramsPart = paramsPart.replace(sortPattern, "")
                            if (limitValue != null) {
                                paramsPart = paramsPart.replace(limitPattern, "")
                            }
                            // 添加c参数
                            if (Regex("c=[+-]?\\d+").containsMatchIn(paramsPart)) {
                                paramsPart = paramsPart.replace(Regex("c=[+-]?\\d+"), "c=$cValue")
                            } else {
                                paramsPart = if (paramsPart.endsWith("[")) {
                                    paramsPart.dropLast(1) + "c=$cValue]"
                                } else if (paramsPart.endsWith("]")) {
                                    paramsPart.dropLast(1) + ",c=$cValue]"
                                } else {
                                    paramsPart + "c=$cValue"
                                }
                            }
                            conversionReminders.add(getStringSafely(context, R.string.java_sort_random_to_c, cValue))
                        }
                    }
                    else -> {
                        paramsPart = paramsPart.replace(sortPattern, "")
                        conversionReminders.add(getStringSafely(context, R.string.java_sort_not_supported, sortValue))
                    }
                }
            } else {
                // 没有sort参数，只转换limit
                if (limitValue != null) {
                    conversionReminders.add(getStringSafely(context, R.string.java_limit_converted, limitValue, limitValue))
                    conversionReminders.add(getStringSafely(context, R.string.limit_description))
                    paramsPart = paramsPart.replace(limitPattern, "c=$limitValue")
                }
            }
        } else if (targetVersion == SelectorType.JAVA) {
            // 基岩版到Java版的参数转换
            paramsPart = convertR_RmToDistance(paramsPart, conversionReminders, context)
            paramsPart = convertRx_RxmToXRotation(paramsPart, conversionReminders, context)
            paramsPart = convertRy_RymToYRotation(paramsPart, conversionReminders, context)
            paramsPart = convertL_LmToLevel(paramsPart, conversionReminders, context)
            paramsPart = convertMToGamemode(paramsPart, conversionReminders, context)
            paramsPart = convertCToLimitSort(paramsPart, conversionReminders, context)
        }
        
        // 分割参数，但要处理包含大括号的参数
        // 使用更智能的方法来分割参数，避免在{}内部分割（与Python版本一致）
        val params = mutableListOf<String>()
        var currentParam = ""
        var braceCount = 0
        var inStringValue = false
        var stringChar = '"'

        for (char in paramsPart) {
            when {
                !inStringValue && (char == '"' || char == '\'') -> {
                    inStringValue = true
                    stringChar = char
                    currentParam += char
                }
                inStringValue && char == stringChar -> {
                    inStringValue = false
                    currentParam += char
                }
                !inStringValue && char == '{' -> {
                    braceCount++
                    currentParam += char
                }
                !inStringValue && char == '}' -> {
                    braceCount--
                    currentParam += char
                }
                !inStringValue && char == ',' && braceCount == 0 -> {
                    // 在最外层遇到逗号，分割参数
                    if (currentParam.trim().isNotEmpty()) {
                        params.add(currentParam.trim())
                    }
                    currentParam = ""
                }
                else -> {
                    currentParam += char
                }
            }
        }

        // 添加最后一个参数
        if (currentParam.trim().isNotEmpty()) {
            params.add(currentParam.trim())
        }
        
        // 过滤参数并收集提醒信息
        val filteredParams = mutableListOf<String>()
        val removedParams = mutableListOf<String>()
        
        for (param in params) {
            if ('=' in param) {
                val paramName = param.split('=')[0].trim()
                
                // 特殊处理haspermission参数，它包含大括号
                if (paramName == "haspermission" && targetVersion == SelectorType.JAVA) {
                    removedParams.add(paramName)
                    conversionReminders.add(getStringSafely(context, R.string.bedrock_param_no_equivalent, paramName))
                    continue  // 跳过此参数，不添加到filteredParams中
                }

                // 根据目标版本过滤参数
                if (targetVersion == SelectorType.JAVA && paramName in bedrockSpecificParams) {
                    removedParams.add(paramName)
                    when (paramName) {
                        "family" -> conversionReminders.add(getStringSafely(context, R.string.bedrock_family_param_not_supported, paramName))
                        "haspermission" -> conversionReminders.add(getStringSafely(context, R.string.bedrock_param_no_equivalent, paramName))
                        "has_property" -> conversionReminders.add(getStringSafely(context, R.string.bedrock_param_no_equivalent, paramName))
                        else -> conversionReminders.add(getStringSafely(context, R.string.bedrock_param_not_supported, paramName))
                    }
                    // 不将此参数添加到filteredParams中，即跳过此参数
                    continue  // 跳过此参数，不添加到filteredParams中
                }
                else if (targetVersion == SelectorType.BEDROCK && paramName in javaSpecificParams) {
                    removedParams.add(paramName)
                    when (paramName) {
                        "team" -> conversionReminders.add(getStringSafely(context, R.string.java_team_param_not_supported, paramName))
                        "predicate" -> conversionReminders.add(getStringSafely(context, R.string.java_predicate_param_not_supported, paramName))
                        "advancements" -> conversionReminders.add(getStringSafely(context, R.string.java_advancements_param_not_supported, paramName))
                        else -> conversionReminders.add(getStringSafely(context, R.string.java_param_not_supported, paramName))
                    }
                    // 不将此参数添加到filteredParams中，即跳过此参数
                    continue  // 跳过此参数，不添加到filteredParams中
                }
                else {
                    filteredParams.add(param)
                }
            } else {
                // 没有等号的参数（可能是一些特殊参数）
                filteredParams.add(param)
            }
        }
        
        // 重构选择器
        val newSelector = if (filteredParams.isNotEmpty()) {
            val newParamsPart = "[" + filteredParams.joinToString(",") + "]"
            selectorVar + newParamsPart
        } else {
            // 如果过滤后没有参数，只返回选择器变量
            selectorVar
        }
        
        // 清理多余的逗号和空括号
        val finalSelector = newSelector
            .replace(",,", ",")
            .replace(",\\]".toRegex(), "]")
            .replace("\\[".toRegex(), "[")
        
        return Triple(finalSelector, removedParams, conversionReminders)
    }
    
    /**
     * 将Java版的distance参数转换为基岩版的r/rm参数
     */
    private fun convertDistanceParameters(paramsPart: String, conversionReminders: MutableList<String>, context: Context): String {
        var result = paramsPart
        
        // 先提取并保存scores参数，避免scores内部的参数名被误处理
        val scoresMatches = mutableListOf<Pair<String, String>>()  // (placeholder, original)
        val scoresPattern = "scores=\\{[^}]*\\}".toRegex()
        
        result = result.replace(scoresPattern) { match ->
            val placeholder = "__SCORES_${scoresMatches.size}__"
            scoresMatches.add(Pair(placeholder, match.value))
            placeholder
        }
        
        // 处理distance参数（此时scores参数已被替换为占位符，不会误匹配）
        val distancePattern = "(?<!__SCORES_)\\bdistance=([^,\\]]+)".toRegex()
        
        result = result.replace(distancePattern) { match ->
            val distanceValue = match.groupValues[1]
            
            // 检查是否为范围格式
            if (".." in distanceValue) {
                val parts = distanceValue.split("..")
                if (parts[0].isNotEmpty() && parts[1].isNotEmpty()) {
                    // 有上下限：5..10 -> rm=5,r=10
                    val rmVal = parts[0]
                    val rVal = parts[1]
                    conversionReminders.add(getStringSafely(context, R.string.java_distance_converted, distanceValue, rmVal, rVal))
                    "rm=$rmVal,r=$rVal"
                } else if (parts[0].isNotEmpty()) {
                    // 只有下限：5.. -> rm=5
                    val rmVal = parts[0]
                    conversionReminders.add(getStringSafely(context, R.string.java_distance_to_rm, distanceValue, rmVal))
                    "rm=$rmVal"
                } else if (parts[1].isNotEmpty()) {
                    // 只有上限：..10 -> r=10
                    val rVal = parts[1]
                    conversionReminders.add(getStringSafely(context, R.string.java_distance_to_r, distanceValue, rVal))
                    "r=$rVal"
                } else {
                    // 无效格式
                    match.value
                }
            } else {
                // 单个值：10 -> rm=10,r=10（精确匹配）
                conversionReminders.add(getStringSafely(context, R.string.java_distance_exact, distanceValue, distanceValue, distanceValue))
                "rm=$distanceValue,r=$distanceValue"
            }
        }
        
        // 恢复scores参数
        for ((placeholder, original) in scoresMatches) {
            result = result.replace(placeholder, original)
        }
        
        // 清理多余的逗号和空括号
        result = result.replace(",,", ",")
        result = result.replace(",\\]".toRegex(), "]")
        
        return result
    }
    
    /**
     * 将Java版的x_rotation/y_rotation参数转换为基岩版的rx/rxm和ry/rym参数
     */
    private fun convertRotationParameters(paramsPart: String, conversionReminders: MutableList<String>, context: Context): String {
        var result = paramsPart
        
        // 先提取并保存scores参数，避免scores内部的参数名被误处理
        val scoresMatches = mutableListOf<Pair<String, String>>()  // (placeholder, original)
        val scoresPattern = "scores=\\{[^}]*\\}".toRegex()
        
        result = result.replace(scoresPattern) { match ->
            val placeholder = "__SCORES_${scoresMatches.size}__"
            scoresMatches.add(Pair(placeholder, match.value))
            placeholder
        }
        
        // 处理x_rotation参数（此时scores参数已被替换为占位符，不会误匹配）
        val xRotationPattern = "(?<!__SCORES_)\\bx_rotation=([^,\\]]+)".toRegex()
        
        result = result.replace(xRotationPattern) { match ->
            val rotationValue = match.groupValues[1]
            
            // 检查是否为范围格式
            if (".." in rotationValue) {
                val parts = rotationValue.split("..")
                if (parts[0].isNotEmpty() && parts[1].isNotEmpty()) {
                    // 有上下限：-45..45 -> rxm=-45,rx=45
                    val rxmVal = parts[0]
                    val rxVal = parts[1]
                    conversionReminders.add(getStringSafely(context, R.string.java_x_rotation_converted, rotationValue, rxmVal, rxVal))
                    "rxm=$rxmVal,rx=$rxVal"
                } else if (parts[0].isNotEmpty()) {
                    // 只有下限：-45.. -> rxm=-45
                    val rxmVal = parts[0]
                    conversionReminders.add(getStringSafely(context, R.string.java_x_rotation_to_rxm, rotationValue, rxmVal))
                    "rxm=$rxmVal"
                } else if (parts[1].isNotEmpty()) {
                    // 只有上限：..45 -> rx=45
                    val rxVal = parts[1]
                    conversionReminders.add(getStringSafely(context, R.string.java_x_rotation_to_rx, rotationValue, rxVal))
                    "rx=$rxVal"
                } else {
                    // 无效格式
                    match.value
                }
            } else {
                // 单个值：45 -> rxm=45,rx=45（精确匹配）
                conversionReminders.add(getStringSafely(context, R.string.java_x_rotation_exact, rotationValue, rotationValue, rotationValue))
                "rxm=$rotationValue,rx=$rotationValue"
            }
        }
        
        // 处理y_rotation参数（此时scores参数已被替换为占位符，不会误匹配）
        val yRotationPattern = "(?<!__SCORES_)\\by_rotation=([^,\\]]+)".toRegex()
        
        result = result.replace(yRotationPattern) { match ->
            val rotationValue = match.groupValues[1]
            
            // 检查是否为范围格式
            if (".." in rotationValue) {
                val parts = rotationValue.split("..")
                if (parts[0].isNotEmpty() && parts[1].isNotEmpty()) {
                    // 有上下限：-45..45 -> rym=-45,ry=45
                    val rymVal = parts[0]
                    val ryVal = parts[1]
                    conversionReminders.add(getStringSafely(context, R.string.java_y_rotation_converted, rotationValue, rymVal, ryVal))
                    "rym=$rymVal,ry=$ryVal"
                } else if (parts[0].isNotEmpty()) {
                    // 只有下限：-45.. -> rym=-45
                    val rymVal = parts[0]
                    conversionReminders.add(getStringSafely(context, R.string.java_y_rotation_to_rym, rotationValue, rymVal))
                    "rym=$rymVal"
                } else if (parts[1].isNotEmpty()) {
                    // 只有上限：..45 -> ry=45
                    val ryVal = parts[1]
                    conversionReminders.add(getStringSafely(context, R.string.java_y_rotation_to_ry, rotationValue, ryVal))
                    "ry=$ryVal"
                } else {
                    // 无效格式
                    match.value
                }
            } else {
                // 单个值：90 -> rym=90,ry=90（精确匹配）
                conversionReminders.add(getStringSafely(context, R.string.java_y_rotation_exact, rotationValue, rotationValue, rotationValue))
                "rym=$rotationValue,ry=$rotationValue"
            }
        }
        
        // 恢复scores参数
        for ((placeholder, original) in scoresMatches) {
            result = result.replace(placeholder, original)
        }
        
        // 清理多余的逗号和空括号
        result = result.replace(",,", ",")
        result = result.replace(",\\]".toRegex(), "]")
        
        return result
    }
    
    /**
     * 将Java版的level参数转换为基岩版的l/lm参数
     */
    private fun convertLevelParameters(paramsPart: String, conversionReminders: MutableList<String>, context: Context): String {
        var result = paramsPart
        
        // 先提取并保存scores参数，避免scores内部的参数名被误处理
        val scoresMatches = mutableListOf<Pair<String, String>>()  // (placeholder, original)
        val scoresPattern = "scores=\\{[^}]*\\}".toRegex()
        
        result = result.replace(scoresPattern) { match ->
            val placeholder = "__SCORES_${scoresMatches.size}__"
            scoresMatches.add(Pair(placeholder, match.value))
            placeholder
        }
        
        // 处理level参数（此时scores参数已被替换为占位符，不会误匹配）
        val levelPattern = "(?<!__SCORES_)\\blevel=([^,\\]]+)".toRegex()
        
        result = result.replace(levelPattern) { match ->
            val levelValue = match.groupValues[1]
            
            // 检查是否为范围格式
            if (".." in levelValue) {
                val parts = levelValue.split("..")
                if (parts[0].isNotEmpty() && parts[1].isNotEmpty()) {
                    // 有上下限：5..10 -> lm=5,l=10
                    val lmVal = parts[0]
                    val lVal = parts[1]
                    conversionReminders.add(getStringSafely(context, R.string.java_level_converted, levelValue, lmVal, lVal))
                    "lm=$lmVal,l=$lVal"
                } else if (parts[0].isNotEmpty()) {
                    // 只有下限：5.. -> lm=5
                    val lmVal = parts[0]
                    conversionReminders.add(getStringSafely(context, R.string.java_level_to_lm, levelValue, lmVal))
                    "lm=$lmVal"
                } else if (parts[1].isNotEmpty()) {
                    // 只有上限：..10 -> l=10
                    val lVal = parts[1]
                    conversionReminders.add(getStringSafely(context, R.string.java_level_to_l, levelValue, lVal))
                    "l=$lVal"
                } else {
                    // 无效格式
                    match.value
                }
            } else {
                // 单个值：10 -> lm=10,l=10
                conversionReminders.add(getStringSafely(context, R.string.java_level_exact, levelValue, levelValue, levelValue))
                "lm=$levelValue,l=$levelValue"
            }
        }
        
        // 恢复scores参数
        for ((placeholder, original) in scoresMatches) {
            result = result.replace(placeholder, original)
        }
        
        // 清理多余的逗号和空括号
        result = result.replace(",,", ",")
        result = result.replace(",\\]".toRegex(), "]")
        
        return result
    }
    
    /**
     * 将Java版的gamemode参数转换为基岩版的m参数
     */
    private fun convertGamemodeToM(paramsPart: String, conversionReminders: MutableList<String>, context: Context): String {
        var result = paramsPart
        
        // Java版游戏模式到基岩版的映射
        val javaToBedrockGamemode = mapOf(
            "survival" to "survival",
            "creative" to "creative",
            "adventure" to "adventure",
            "spectator" to "survival"  // 基岩版没有旁观模式，转换为生存模式
        )
        
        // 先提取并保存scores参数，避免scores内部的参数名被误处理
        val scoresMatches = mutableListOf<Pair<String, String>>()  // (placeholder, original)
        val scoresPattern = "scores=\\{[^}]*\\}".toRegex()
        
        result = result.replace(scoresPattern) { match ->
            val placeholder = "__SCORES_${scoresMatches.size}__"
            scoresMatches.add(Pair(placeholder, match.value))
            placeholder
        }
        
        // 从gamemode到m的转换（Java版到基岩版）
        // 此时scores参数已被替换为占位符，不会误匹配
        val gamemodePattern = "(?<!__SCORES_)\\bgamemode=(!?)([^,\\]]+)".toRegex()
        
        result = result.replace(gamemodePattern) { match ->
            val negation = match.groupValues[1]  // ! 或空
            val gamemodeValue = match.groupValues[2].trim()

            if (gamemodeValue == "spectator") {
                if (negation.isNotEmpty()) {
                    conversionReminders.add(getStringSafely(context, R.string.java_negation_spectator_converted))
                } else {
                    conversionReminders.add(getStringSafely(context, R.string.java_spectator_converted, gamemodeValue))
                }
                "m=${negation}survival"
            } else if (gamemodeValue in javaToBedrockGamemode) {
                conversionReminders.add(getStringSafely(context, R.string.java_gamemode_converted, gamemodeValue, javaToBedrockGamemode[gamemodeValue]!!))
                "m=${negation}${javaToBedrockGamemode[gamemodeValue]!!}"
            } else {
                // 保持原值
                match.value
            }
        }
        
        // 恢复scores参数
        for ((placeholder, original) in scoresMatches) {
            result = result.replace(placeholder, original)
        }
        
        return result
    }
    
    /**
     * 去除参数中不必要的空格
     */
    private fun removeUnnecessarySpaces(paramsPart: String): String {
        var result = paramsPart
        
        // 保护字符串内容
        val strings = mutableListOf<String>()
        val stringPattern = Pattern.compile("\"([^\"]*)\"")
        val matcher = stringPattern.matcher(result)
        
        val protectedResult = StringBuffer()
        while (matcher.find()) {
            strings.add(matcher.group(0)!!)
            matcher.appendReplacement(protectedResult, "__STRING_${strings.size - 1}__")
        }
        matcher.appendTail(protectedResult)
        result = protectedResult.toString()
        
        // 去除等号周围的空格
        result = result.replace("\\s*=\\s*".toRegex(), "=")
        // 去除逗号后的空格
        result = result.replace(",\\s*".toRegex(), ",")
        // 去除方括号内的前后空格
        result = result.replace("\\[\\s+".toRegex(), "[")
        result = result.replace("\\s+]".toRegex(), "]")
        
        // 恢复字符串内容
        for ((index, string) in strings.withIndex()) {
            result = result.replace("__STRING_" + index + "__", string)
        }
        
        return result
    }
    
    /**
     * 将基岩版参数转换为Java版参数
     */
    private fun convertBedrockParametersToJava(selector: String, reminders: MutableList<String>, context: Context): String {
        var result = selector
        
        if ('[' !in result || ']' !in result) {
            return result
        }
        
        val selectorVar = result.split('[')[0]
        var paramsPart = result.substringAfter('[').substringBefore(']')
        
        // 转换r/rm参数到distance参数
        paramsPart = convertR_RmToDistance(paramsPart, reminders, context)
        
        // 转换rx/rxm参数到x_rotation参数
        paramsPart = convertRx_RxmToXRotation(paramsPart, reminders, context)
        
        // 转换ry/rym参数到y_rotation参数
        paramsPart = convertRy_RymToYRotation(paramsPart, reminders, context)
        
        // 转换l/lm参数到level参数
        paramsPart = convertL_LmToLevel(paramsPart, reminders, context)
        
        // 转换m参数到gamemode参数
        paramsPart = convertMToGamemode(paramsPart, reminders, context)
        
        // 转换c参数到limit/sort参数
        paramsPart = convertCToLimitSort(paramsPart, reminders, context)
        
        return selectorVar + "[" + paramsPart + "]"
    }
    
    /**
     * 将Java版参数转换为基岩版参数
     */
    private fun convertJavaParametersToBedrock(selector: String, reminders: MutableList<String>, context: Context): String {
        var result = selector
        
        if ('[' !in result || ']' !in result) {
            return result
        }
        
        val selectorVar = result.split('[')[0]
        var paramsPart = result.substringAfter('[').substringBefore(']')
        
        // 转换distance参数到r/rm参数
        paramsPart = convertDistanceToR_Rm(paramsPart, reminders, context)
        
        // 转换x_rotation参数到rx/rxm参数
        paramsPart = convertXRotationToRx_Rxm(paramsPart, reminders, context)
        
        // 转换y_rotation参数到ry/rym参数
        paramsPart = convertYRotationToRy_Rym(paramsPart, reminders, context)
        
        // 转换level参数到l/lm参数
        paramsPart = convertLevelToL_Lm(paramsPart, reminders, context)
        
        // 转换gamemode参数到m参数
        paramsPart = convertGamemodeToM(paramsPart, reminders, context)
        
        // 转换limit/sort参数到c参数
        paramsPart = convertLimitSortToC(paramsPart, selectorVar, reminders, context)
        
        // 检查是否需要修改选择器变量（用于 sort=random 的 @a/@r 转换）
        var finalSelectorVar = selectorVar
        if (paramsPart.contains("__SELECTOR_VAR_CHANGE_TO__")) {
            val matchResult = Regex("__SELECTOR_VAR_CHANGE_TO__([@\\w]+)__").find(paramsPart)
            if (matchResult != null) {
                finalSelectorVar = matchResult.groupValues[1]
                paramsPart = paramsPart.replace(matchResult.value, "")
            }
        }
        
        return finalSelectorVar + "[" + paramsPart + "]"
    }
    
    // 以下是各种参数转换的具体实现
    
    private fun convertR_RmToDistance(paramsPart: String, reminders: MutableList<String>, context: Context): String {
        var result = paramsPart
        
        // 先提取并保存scores参数，避免scores内部的参数名被误处理
        val scoresMatches = mutableListOf<Pair<String, String>>()  // (placeholder, original)
        val scoresPattern = "scores=\\{[^}]*\\}".toRegex()
        
        result = result.replace(scoresPattern) { match ->
            val placeholder = "__SCORES_${scoresMatches.size}__"
            scoresMatches.add(Pair(placeholder, match.value))
            placeholder
        }
        
        // 此时scores参数已被替换为占位符，不会误匹配
        val rPattern = "(?<!__SCORES_)\\br=([^,\\]]+)".toRegex()
        val rmPattern = "(?<!__SCORES_)\\brm=([^,\\]]+)".toRegex()
        
        val rMatch = rPattern.find(result)
        val rmMatch = rmPattern.find(result)
        
        if (rMatch != null || rmMatch != null) {
            val rValue = rMatch?.groupValues?.get(1)
            val rmValue = rmMatch?.groupValues?.get(1)
            
            val distanceValue = when {
                rmValue != null && rValue != null -> rmValue + ".." + rValue
                rmValue != null -> rmValue + ".."
                rValue != null -> "..$rValue"
                else -> ""
            }
            
            if (distanceValue.isNotEmpty()) {
                when {
                    rmValue != null && rValue != null ->
                        reminders.add(getStringSafely(context, R.string.bedrock_rm_r_converted, rmValue, rValue, distanceValue))
                    rmValue != null ->
                        reminders.add(getStringSafely(context, R.string.bedrock_rm_converted, rmValue, distanceValue))
                    rValue != null ->
                        reminders.add(getStringSafely(context, R.string.bedrock_r_converted, rValue, distanceValue))
                }
                
                // 移除原有的r和rm参数
                result = result.replace(rPattern, "")
                result = result.replace(rmPattern, "")
                
                // 恢复scores参数
                for ((placeholder, original) in scoresMatches) {
                    result = result.replace(placeholder, original)
                }
                
                // 添加distance参数
                result = addParameterToResult(result, "distance=$distanceValue")
            } else {
                // 恢复scores参数（如果没有匹配到r或rm）
                for ((placeholder, original) in scoresMatches) {
                    result = result.replace(placeholder, original)
                }
            }
        } else {
            // 恢复scores参数（如果没有匹配到r或rm）
            for ((placeholder, original) in scoresMatches) {
                result = result.replace(placeholder, original)
            }
        }
        
        return result
    }
    
    private fun convertDistanceToR_Rm(paramsPart: String, reminders: MutableList<String>, context: Context): String {
        var result = paramsPart
        
        // 先提取并保存scores参数，避免scores内部的参数名被误处理
        val scoresMatches = mutableListOf<Pair<String, String>>()  // (placeholder, original)
        val scoresPattern = "scores=\\{[^}]*\\}".toRegex()
        
        result = result.replace(scoresPattern) { match ->
            val placeholder = "__SCORES_${scoresMatches.size}__"
            scoresMatches.add(Pair(placeholder, match.value))
            placeholder
        }
        
        // 此时scores参数已被替换为占位符，不会误匹配
        val distancePattern = "(?<!__SCORES_)\\bdistance=([^,\\]]+)".toRegex()
        
        val match = distancePattern.find(result)
        if (match != null) {
            val distanceValue = match.groupValues[1]
            
            val (rValue, rmValue) = when {
                ".." in distanceValue -> {
                    val parts = distanceValue.split("..")
                    val rm = parts.getOrNull(0)?.takeIf { it.isNotEmpty() }
                    val r = parts.getOrNull(1)?.takeIf { it.isNotEmpty() }
                    r to rm
                }
                distanceValue.startsWith("..") -> {
                    null to distanceValue.substring(2)
                }
                distanceValue.endsWith("..") -> {
                    distanceValue.substring(0, distanceValue.length - 2) to null
                }
                else -> {
                    distanceValue to distanceValue
                }
            }
            
            // 构建提醒信息
            val reminderParts = mutableListOf<String>()
            rmValue?.let { reminderParts.add("rm=$it") }
            rValue?.let { reminderParts.add("r=$it") }
            if (reminderParts.isNotEmpty()) {
                reminders.add(getStringSafely(context, R.string.java_distance_to_bedrock, distanceValue, reminderParts.joinToString(",")))
            }
            
            // 移除distance参数
            result = result.replace(distancePattern, "")
            
            // 恢复scores参数
            for ((placeholder, original) in scoresMatches) {
                result = result.replace(placeholder, original)
            }
            
            // 添加r和rm参数
            rmValue?.let { result = addParameterToResult(result, "rm=$it") }
            rValue?.let { result = addParameterToResult(result, "r=$it") }
        } else {
            // 恢复scores参数（如果没有匹配到distance）
            for ((placeholder, original) in scoresMatches) {
                result = result.replace(placeholder, original)
            }
        }
        
        return result
    }
    
    private fun convertRx_RxmToXRotation(paramsPart: String, reminders: MutableList<String>, context: Context): String {
        return convertRotationParameter(
            paramsPart = paramsPart,
            paramName = "x_rotation",
            minParam = "rxm",
            maxParam = "rx",
            reminders = reminders,
            context = context
        )
    }
    
    private fun convertXRotationToRx_Rxm(paramsPart: String, reminders: MutableList<String>, context: Context): String {
        return convertRotationParameter(
            paramsPart = paramsPart,
            paramName = "x_rotation",
            minParam = "rxm",
            maxParam = "rx",
            reminders = reminders,
            context = context,
            toJava = false
        )
    }
    
    private fun convertRy_RymToYRotation(paramsPart: String, reminders: MutableList<String>, context: Context): String {
        return convertRotationParameter(
            paramsPart = paramsPart,
            paramName = "y_rotation",
            minParam = "rym",
            maxParam = "ry",
            reminders = reminders,
            context = context
        )
    }
    
    private fun convertYRotationToRy_Rym(paramsPart: String, reminders: MutableList<String>, context: Context): String {
        return convertRotationParameter(
            paramsPart = paramsPart,
            paramName = "y_rotation",
            minParam = "rym",
            maxParam = "ry",
            reminders = reminders,
            context = context,
            toJava = false
        )
    }
    
    private fun convertRotationParameter(
        paramsPart: String,
        paramName: String,
        minParam: String,
        maxParam: String,
        reminders: MutableList<String>,
        context: Context,
        toJava: Boolean = true
    ): String {
        var result = paramsPart
        
        // 先提取并保存scores参数，避免scores内部的参数名被误处理
        val scoresMatches = mutableListOf<Pair<String, String>>()  // (placeholder, original)
        val scoresPattern = "scores=\\{[^}]*\\}".toRegex()
        
        result = result.replace(scoresPattern) { match ->
            val placeholder = "__SCORES_${scoresMatches.size}__"
            scoresMatches.add(Pair(placeholder, match.value))
            placeholder
        }
        
        if (toJava) {
            // 从基岩版转Java版：将 minParam 和 maxParam 合并为 paramName
            // 此时scores参数已被替换为占位符，不会误匹配
            val minPattern = "(?<!__SCORES_)\\b$minParam=([^,\\]]+)".toRegex()
            val maxPattern = "(?<!__SCORES_)\\b$maxParam=([^,\\]]+)".toRegex()
            
            val minMatch = minPattern.find(result)
            val maxMatch = maxPattern.find(result)
            
            if (minMatch != null || maxMatch != null) {
                val minValue = minMatch?.groupValues?.get(1)
                val maxValue = maxMatch?.groupValues?.get(1)
                
                val rotationValue = when {
                    minValue != null && maxValue != null -> minValue + ".." + maxValue
                    minValue != null -> minValue + ".."
                    maxValue != null -> "..$maxValue"
                    else -> ""
                }
                
                if (rotationValue.isNotEmpty()) {
                    when {
                        minValue != null && maxValue != null ->
                            reminders.add(getStringSafely(context, R.string.bedrock_rotation_converted, minParam, minValue, maxParam, maxValue, paramName, rotationValue))
                        minValue != null ->
                            reminders.add(getStringSafely(context, R.string.bedrock_rotation_min_converted, minParam, minValue, paramName, rotationValue))
                        maxValue != null ->
                            reminders.add(getStringSafely(context, R.string.bedrock_rotation_max_converted, maxParam, maxValue, paramName, rotationValue))
                    }
                    
                    // 移除原有参数
                    result = result.replace(minPattern, "")
                    result = result.replace(maxPattern, "")
                    
                    // 恢复scores参数
                    for ((placeholder, original) in scoresMatches) {
                        result = result.replace(placeholder, original)
                    }
                    
                    // 添加新参数
                    result = addParameterToResult(result, paramName + "=" + rotationValue)
                } else {
                    // 恢复scores参数（如果没有匹配到参数）
                    for ((placeholder, original) in scoresMatches) {
                        result = result.replace(placeholder, original)
                    }
                }
            } else {
                // 恢复scores参数（如果没有匹配到参数）
                for ((placeholder, original) in scoresMatches) {
                    result = result.replace(placeholder, original)
                }
            }
        } else {
            // 从Java版转基岩版：将 paramName 拆分为 minParam 和 maxParam
            // 此时scores参数已被替换为占位符，不会误匹配
            val paramPattern = "(?<!__SCORES_)\\b$paramName=([^,\\]]+)".toRegex()
            val paramMatch = paramPattern.find(result)
            
            if (paramMatch != null) {
                val paramValue = paramMatch.groupValues[1]
                
                // 解析范围值
                val rotationParts = if (".." in paramValue) {
                    paramValue.split("..")
                } else {
                    listOf(paramValue, paramValue)
                }
                
                val minValue = rotationParts.getOrNull(0)?.takeIf { it.isNotEmpty() }
                val maxValue = rotationParts.getOrNull(1)?.takeIf { it.isNotEmpty() }
                
                // 添加提醒信息
                when {
                    minValue != null && maxValue != null ->
                        reminders.add(getStringSafely(context, R.string.java_rotation_converted, paramName, paramValue, minParam, minValue, maxParam, maxValue))
                    minValue != null ->
                        reminders.add(getStringSafely(context, R.string.java_rotation_to_min, paramName, paramValue, minParam, minValue))
                    maxValue != null ->
                        reminders.add(getStringSafely(context, R.string.java_rotation_to_max, paramName, paramValue, maxParam, maxValue))
                }
                
                // 移除原有参数
                result = result.replace(paramPattern, "")
                
                // 恢复scores参数
                for ((placeholder, original) in scoresMatches) {
                    result = result.replace(placeholder, original)
                }
                
                // 添加新参数
                if (minValue != null && maxValue != null) {
                    result = addParameterToResult(result, minParam + "=" + minValue)
                    result = addParameterToResult(result, maxParam + "=" + maxValue)
                } else if (minValue != null) {
                    result = addParameterToResult(result, minParam + "=" + minValue)
                } else if (maxValue != null) {
                    result = addParameterToResult(result, maxParam + "=" + maxValue)
                }
            } else {
                // 恢复scores参数（如果没有匹配到参数）
                for ((placeholder, original) in scoresMatches) {
                    result = result.replace(placeholder, original)
                }
            }
        }
        
        return result
    }
    
    private fun convertL_LmToLevel(paramsPart: String, reminders: MutableList<String>, context: Context): String {
        var result = paramsPart
        
        // 先提取并保存scores参数，避免scores内部的参数名被误处理
        val scoresMatches = mutableListOf<Pair<String, String>>()  // (placeholder, original)
        val scoresPattern = "scores=\\{[^}]*\\}".toRegex()
        
        result = result.replace(scoresPattern) { match ->
            val placeholder = "__SCORES_${scoresMatches.size}__"
            scoresMatches.add(Pair(placeholder, match.value))
            placeholder
        }
        
        // 此时scores参数已被替换为占位符，不会误匹配
        val lPattern = "(?<!__SCORES_)\\bl=([^,\\]]+)".toRegex()
        val lmPattern = "(?<!__SCORES_)\\blm=([^,\\]]+)".toRegex()
        
        val lMatch = lPattern.find(result)
        val lmMatch = lmPattern.find(result)
        
        if (lMatch != null || lmMatch != null) {
            val lValue = lMatch?.groupValues?.get(1)
            val lmValue = lmMatch?.groupValues?.get(1)
            
            val levelValue = when {
                lmValue != null && lValue != null -> lmValue + ".." + lValue
                lmValue != null -> lmValue + ".."
                lValue != null -> "..$lValue"
                else -> ""
            }
            
            if (levelValue.isNotEmpty()) {
                when {
                    lmValue != null && lValue != null ->
                        reminders.add(getStringSafely(context, R.string.bedrock_lm_l_converted, lmValue, lValue, levelValue))
                    lmValue != null ->
                        reminders.add(getStringSafely(context, R.string.bedrock_lm_converted, lmValue, levelValue))
                    lValue != null ->
                        reminders.add(getStringSafely(context, R.string.bedrock_l_converted, lValue, levelValue))
                }
                
                // 移除原有的l和lm参数
                result = result.replace(lPattern, "")
                result = result.replace(lmPattern, "")
                
                // 恢复scores参数
                for ((placeholder, original) in scoresMatches) {
                    result = result.replace(placeholder, original)
                }
                
                // 添加level参数
                result = addParameterToResult(result, "level=$levelValue")
            } else {
                // 恢复scores参数（如果没有匹配到l或lm）
                for ((placeholder, original) in scoresMatches) {
                    result = result.replace(placeholder, original)
                }
            }
        } else {
            // 恢复scores参数（如果没有匹配到l或lm）
            for ((placeholder, original) in scoresMatches) {
                result = result.replace(placeholder, original)
            }
        }
        
        return result
    }
    
    private fun convertLevelToL_Lm(paramsPart: String, reminders: MutableList<String>, context: Context): String {
        var result = paramsPart
        
        // 先提取并保存scores参数，避免scores内部的参数名被误处理
        val scoresMatches = mutableListOf<Pair<String, String>>()  // (placeholder, original)
        val scoresPattern = "scores=\\{[^}]*\\}".toRegex()
        
        result = result.replace(scoresPattern) { match ->
            val placeholder = "__SCORES_${scoresMatches.size}__"
            scoresMatches.add(Pair(placeholder, match.value))
            placeholder
        }
        
        // 此时scores参数已被替换为占位符，不会误匹配
        val levelPattern = "(?<!__SCORES_)\\blevel=([^,\\]]+)".toRegex()
        
        val match = levelPattern.find(result)
        if (match != null) {
            val levelValue = match.groupValues[1]
            
            val (lValue, lmValue) = when {
                ".." in levelValue -> {
                    val parts = levelValue.split("..")
                    val lm = parts.getOrNull(0)?.takeIf { it.isNotEmpty() }
                    val l = parts.getOrNull(1)?.takeIf { it.isNotEmpty() }
                    l to lm
                }
                levelValue.startsWith("..") -> {
                    null to levelValue.substring(2)
                }
                levelValue.endsWith("..") -> {
                    levelValue.substring(0, levelValue.length - 2) to null
                }
                else -> {
                    levelValue to levelValue
                }
            }

            reminders.add(getStringSafely(context, R.string.java_level_to_bedrock, levelValue, listOfNotNull(lmValue?.let { "lm=" + it }, lValue?.let { "l=" + it }).joinToString(",")))

            // 移除level参数
            result = result.replace(levelPattern, "")
            
            // 恢复scores参数
            for ((placeholder, original) in scoresMatches) {
                result = result.replace(placeholder, original)
            }
            
            // 添加l和lm参数
            lmValue?.let { result = addParameterToResult(result, "lm=$it") }
            lValue?.let { result = addParameterToResult(result, "l=$it") }
        } else {
            // 恢复scores参数（如果没有匹配到level）
            for ((placeholder, original) in scoresMatches) {
                result = result.replace(placeholder, original)
            }
        }
        
        return result
    }
    
    private fun convertMToGamemode(paramsPart: String, reminders: MutableList<String>, context: Context): String {
        var result = paramsPart
        
        // 先提取并保存scores参数，避免scores内部的参数名被误处理
        val scoresMatches = mutableListOf<Pair<String, String>>()  // (placeholder, original)
        val scoresPattern = "scores=\\{[^}]*\\}".toRegex()
        
        result = result.replace(scoresPattern) { match ->
            val placeholder = "__SCORES_${scoresMatches.size}__"
            scoresMatches.add(Pair(placeholder, match.value))
            placeholder
        }
        
        // 基岩版游戏模式到Java版的映射
        val bedrockToJavaGamemode = mapOf(
            "survival" to "survival",
            "creative" to "creative",
            "adventure" to "adventure",
            "default" to "survival",
            "s" to "survival",
            "c" to "creative",
            "a" to "adventure",
            "d" to "survival",
            "0" to "survival",
            "1" to "creative",
            "2" to "adventure",
            "5" to "survival"
        )
        
        // 从m到gamemode的转换（基岩版到Java版）
        // 此时scores参数已被替换为占位符，不会误匹配
        val mPattern = "(?<!__SCORES_)\\bm=(!?)([^,\\]]+)".toRegex()
        
        result = result.replace(mPattern) { match ->
            val negation = match.groupValues[1]  // ! 或空
            val mValue = match.groupValues[2].trim()
            
            // 如果是默认模式，需要提醒用户并转换为生存模式
            if (mValue == "default" || mValue == "d" || mValue == "5") {
                if (negation.isNotEmpty()) {
                    reminders.add(getStringSafely(context, R.string.bedrock_negation_default_converted))
                } else {
                    reminders.add(getStringSafely(context, R.string.bedrock_default_converted, mValue))
                }
                "gamemode=${negation}survival"
            } else if (mValue in bedrockToJavaGamemode) {
                "gamemode=${negation}${bedrockToJavaGamemode[mValue]!!}"
            } else {
                // 保持原值
                match.value
            }
        }
        
        // 恢复scores参数
        for ((placeholder, original) in scoresMatches) {
            result = result.replace(placeholder, original)
        }
        
        return result
    }
    
    // 特殊处理scores参数中的!=反选：在Java版输出中移除整个scores参数
    fun processScoresNegation(selector: String, targetVersion: SelectorType, context: Context): Pair<String, List<String>> {
        val conversionReminders = mutableListOf<String>()
        
        if ('[' !in selector || ']' !in selector) {
            return selector to conversionReminders
        }
        
        val selectorVar = selector.split('[')[0]
        var paramsPart = selector.substringAfter('[').substringBefore(']')
        
        // 特殊处理scores参数中的!=反选：在Java版输出中移除整个scores参数
        if (targetVersion == SelectorType.JAVA && "scores=" in paramsPart) {
            val scoresPattern = "scores=\\{([^}]*)\\}".toRegex()
            paramsPart = paramsPart.replace(scoresPattern) { match ->
                val fullMatch = match.value
                val scoresContent = match.groupValues[1]
                
                // 检查是否有反选模式
                if ("![" in scoresContent || "!" in scoresContent) {
                    // Java版不支持scores反选，直接移除整个scores参数
                    conversionReminders.add(getStringSafely(context, R.string.bedrock_scores_negation_removed, fullMatch))
                    ""  // 返回空字符串，表示移除整个参数
                } else {
                    fullMatch
                }
            }
        }
        // 处理scores参数中的level参数（Java版到基岩版转换）
        else if (targetVersion == SelectorType.BEDROCK && "scores=" in paramsPart) {
            val scoresPattern = "scores=\\{([^}]*)\\}".toRegex()
            paramsPart = paramsPart.replace(scoresPattern) { match ->
                val fullMatch = match.value
                val scoresContent = match.groupValues[1]

                // 处理level参数
                val levelPattern = "level=([^,\\}]+)".toRegex()
                val newScoresContent = scoresContent.replace(levelPattern) { levelMatch ->
                    val levelValue = levelMatch.groupValues[1]
                    conversionReminders.add(getStringSafely(context, R.string.java_level_in_scores_converted, levelValue, levelValue, levelValue))
                    "lm=$levelValue,l=$levelValue"
                }
                
                // 如果scores内容发生了变化，返回新的scores参数
                if (newScoresContent != scoresContent) {
                    "scores={$newScoresContent}"
                } else {
                    fullMatch
                }
            }
        }
        
        return selectorVar + "[" + paramsPart + "]" to conversionReminders
    }
    
    
    
    private fun convertCToLimitSort(paramsPart: String, reminders: MutableList<String>, context: Context): String {
        var result = paramsPart
        
        // 先提取并保存scores参数，避免scores内部的参数名被误处理
        val scoresMatches = mutableListOf<Pair<String, String>>()  // (placeholder, original)
        val scoresPattern = "scores=\\{[^}]*\\}".toRegex()
        
        result = result.replace(scoresPattern) { match ->
            val placeholder = "__SCORES_${scoresMatches.size}__"
            scoresMatches.add(Pair(placeholder, match.value))
            placeholder
        }
        
        // 此时scores参数已被替换为占位符，不会误匹配
        val cPattern = "(?<!__SCORES_)\\bc=([+-]?\\d+)".toRegex()
        
        val match = cPattern.find(result)
        if (match != null) {
            val cValue = match.groupValues[1]
            
            if (cValue.startsWith("-")) {
                val absCVal = cValue.substring(1)
                reminders.add(getStringSafely(context, R.string.bedrock_c_negative_converted, cValue, absCVal))

                // 移除c参数
                result = result.replace(cPattern, "")

                // 恢复scores参数
                for ((placeholder, original) in scoresMatches) {
                    result = result.replace(placeholder, original)
                }

                // 添加limit和sort参数
                result = addParameterToResult(result, "limit=$absCVal")
                if (!result.contains("sort=")) {
                    result = addParameterToResult(result, "sort=furthest")
                }
            } else {
                reminders.add(getStringSafely(context, R.string.bedrock_c_converted, cValue, cValue))
                result = result.replace(cPattern, "limit=$cValue")
                
                // 恢复scores参数
                for ((placeholder, original) in scoresMatches) {
                    result = result.replace(placeholder, original)
                }
                
                if (!result.contains("sort=")) {
                    result = addParameterToResult(result, "sort=nearest")
                }
            }
        } else {
            // 恢复scores参数（如果没有匹配到c）
            for ((placeholder, original) in scoresMatches) {
                result = result.replace(placeholder, original)
            }
        }
        
        return result
    }
    
    private fun convertLimitSortToC(paramsPart: String, selectorVar: String, reminders: MutableList<String>, context: Context): String {
        var result = paramsPart

        // 先提取并保存scores参数，避免scores内部的参数名被误处理
        val scoresMatches = mutableListOf<Pair<String, String>>()  // (placeholder, original)
        val scoresPattern = "scores=\\{[^}]*\\}".toRegex()

        result = result.replace(scoresPattern) { match ->
            val placeholder = "__SCORES_${scoresMatches.size}__"
            scoresMatches.add(Pair(placeholder, match.value))
            placeholder
        }

        // 处理sort参数（此时scores参数已被替换为占位符，不会误匹配）
        val sortPattern = "(?<!__SCORES_)(^|,)sort=([^,\\]]+)".toRegex()
        val sortMatch = sortPattern.find(result)

        if (sortMatch != null) {
            val sortValue: String = sortMatch.groupValues[2]

            // 查找limit参数（此时scores参数已被替换为占位符，不会误匹配）
            val limitPattern = "(?<!__SCORES_)(^|,)limit=([+-]?\\d+)".toRegex()
            val limitMatch = limitPattern.find(result)
            val limitValue: String? = limitMatch?.groupValues?.get(2)

            when (sortValue) {
                "nearest" -> {
                    // 当limit=数字,sort=nearest时，基岩版转换为c=数字
                    // 当只有sort=nearest，没有limit时，基岩版转换为c=9999
                    val cValue = limitValue ?: "9999"
                    result = result.replace(sortPattern, "")
                    if (limitValue != null) {
                        result = result.replace(limitPattern, "")
                    }

                    // 恢复scores参数
                    for ((placeholder, original) in scoresMatches) {
                        result = result.replace(placeholder, original)
                    }

                    result = addParameterToResult(result, "c=$cValue")
                    reminders.add(getStringSafely(context, R.string.java_sort_nearest_converted, cValue))
                }
                "furthest" -> {
                    // 当limit=数字,sort=furthest时，基岩版转换为c=-数字
                    // 当只有sort=furthest，没有limit时，基岩版转换为c=-9999
                    val cValue = if (limitValue != null) "-$limitValue" else "-9999"
                    result = result.replace(sortPattern, "")
                    if (limitValue != null) {
                        result = result.replace(limitPattern, "")
                    }

                    // 恢复scores参数
                    for ((placeholder, original) in scoresMatches) {
                        result = result.replace(placeholder, original)
                    }

                    result = addParameterToResult(result, "c=$cValue")
                    // 只有在转换为 c=-9999 时才显示说明
                    if (cValue == "-9999") {
                        reminders.add(getStringSafely(context, R.string.java_sort_furthest_converted_all))
                    } else {
                        reminders.add(getStringSafely(context, R.string.java_sort_furthest_converted, cValue))
                    }
                }
                "arbitrary" -> {
                    result = result.replace(sortPattern, "")

                    // 恢复scores参数
                    for ((placeholder, original) in scoresMatches) {
                        result = result.replace(placeholder, original)
                    }

                    // 当大选择器为 @a 或 @e 时，直接删除（不提醒）
                    // 当为其他大选择器时，删除并提醒用户
                    if (selectorVar != "@a" && selectorVar != "@e") {
                        reminders.add(getStringSafely(context, R.string.java_sort_arbitrary_not_supported))
                    }
                }
                "random" -> {
                    // 当@a[limit=数字,sort=random]或@r[limit=数字,sort=random]时，转换为@r[c=数字]
                    // 当只有@a[sort=random]或@r[sort=random]时，转换为基岩版的@r[c=9999]
                    val cValue = limitValue ?: "9999"
                    result = result.replace(sortPattern, "")
                    if (limitValue != null) {
                        result = result.replace(limitPattern, "")
                    }

                    // 恢复scores参数
                    for ((placeholder, original) in scoresMatches) {
                        result = result.replace(placeholder, original)
                    }

                    result = addParameterToResult(result, "c=$cValue")
                    if (selectorVar == "@a" || selectorVar == "@r") {
                        reminders.add(getStringSafely(context, R.string.java_sort_random_converted, selectorVar, cValue))
                        // 在 result 开头添加特殊标记，表示需要修改选择器变量
                        result = "__SELECTOR_VAR_CHANGE_TO__@r__" + result
                    } else {
                        reminders.add(getStringSafely(context, R.string.java_sort_random_to_c, cValue))
                    }
                }
                else -> {
                    result = result.replace(sortPattern, "")

                    // 恢复scores参数
                    for ((placeholder, original) in scoresMatches) {
                        result = result.replace(placeholder, original)
                    }

                    val sortValueForMessage = sortValue
                    reminders.add(getStringSafely(context, R.string.java_sort_not_supported, sortValueForMessage))
                }
            }
        } else {
            // 恢复scores参数（如果没有匹配到sort）
            for ((placeholder, original) in scoresMatches) {
                result = result.replace(placeholder, original)
            }

            // 没有sort参数，只转换limit
            // 再次提取并保存scores参数，避免scores内部的参数名被误处理
            val scoresMatches2 = mutableListOf<Pair<String, String>>()  // (placeholder, original)
            val scoresPattern2 = "scores=\\{[^}]*\\}".toRegex()

            result = result.replace(scoresPattern2) { match ->
                val placeholder = "__SCORES_${scoresMatches2.size}__"
                scoresMatches2.add(Pair(placeholder, match.value))
                placeholder
            }

            // 此时scores参数已被替换为占位符，不会误匹配
            val limitPattern = "(?<!__SCORES_)(^|,)limit=([+-]?\\d+)".toRegex()
            val limitMatch = limitPattern.find(result)
            if (limitMatch != null) {
                val limitValue = limitMatch.groupValues[2]
                reminders.add(getStringSafely(context, R.string.java_limit_converted, limitValue, limitValue))
                reminders.add(getStringSafely(context, R.string.limit_description))
                result = result.replace(limitPattern, "c=$limitValue")

                // 恢复scores参数
                for ((placeholder, original) in scoresMatches2) {
                    result = result.replace(placeholder, original)
                }
            } else {
                // 恢复scores参数（如果没有匹配到limit）
                for ((placeholder, original) in scoresMatches2) {
                    result = result.replace(placeholder, original)
                }
            }
        }

        return result
    }
    
    private fun addParameterToResult(paramsPart: String, newParam: String): String {
        if (paramsPart.isEmpty()) {
            return newParam
        }
        
        // 清理参数
        var cleanParams = paramsPart
        cleanParams = cleanParams.replace(",,", ",")
        cleanParams = cleanParams.replace("^,".toRegex(), "")
        cleanParams = cleanParams.replace(",$".toRegex(), "")
        
        if (cleanParams.isEmpty()) {
            return newParam
        }
        
        return cleanParams + "," + newParam
    }

    /**
     * 处理 hasitem 参数转换为 nbt 参数（基岩版到Java版）
     * 支持完整的转换逻辑，包括装备槽位和物品栏槽位
     */
    private fun convertHasitemToNbt(paramsPart: String, context: Context): Pair<String, List<String>> {
        val reminders = mutableListOf<String>()
        var result = paramsPart

        // 处理空数组格式：hasitem=[]
        val emptyArrayPattern = "hasitem=\\[\\]".toRegex()
        val emptyArrayMatch = emptyArrayPattern.find(paramsPart)

        if (emptyArrayMatch != null) {
            val fullMatch = emptyArrayMatch.value
            // 移除空数组
            val index = result.indexOf(fullMatch)
            if (index >= 0) {
                val replacement = when {
                    index > 0 && result[index - 1] == ',' -> result.substring(0, index - 1) + result.substring(index + fullMatch.length)
                    index + fullMatch.length < result.length && result[index + fullMatch.length] == ',' -> result.substring(0, index) + result.substring(index + fullMatch.length + 1)
                    else -> result.substring(0, index) + result.substring(index + fullMatch.length)
                }
                result = replacement
            }
        }

        // 处理数组格式：hasitem=[{...},{...}]
        val arrayPattern = "hasitem=\\[\\{".toRegex()
        val arrayMatch = arrayPattern.find(result)

        if (arrayMatch != null) {
            val startIndex = arrayMatch.range.first
            // 找到匹配的 hasitem 数组的完整内容
            val arrayContent = extractArrayContent(result.substring(startIndex))

            if (arrayContent != null) {
                val fullMatch = "hasitem=[$arrayContent]"
                val nbtResult = parseHasitemArray(arrayContent, reminders, context)

                if (nbtResult.isNotEmpty()) {
                    // 精确替换
                    val index = result.indexOf(fullMatch)
                    if (index >= 0) {
                        result = result.substring(0, index) + nbtResult + result.substring(index + fullMatch.length)
                    }
                } else {
                    // 空数组或转换失败，移除整个 hasitem 参数
                    val index = result.indexOf(fullMatch)
                    if (index >= 0) {
                        val replacement = when {
                            index > 0 && result[index - 1] == ',' -> result.substring(0, index - 1) + result.substring(index + fullMatch.length)
                            index + fullMatch.length < result.length && result[index + fullMatch.length] == ',' -> result.substring(0, index) + result.substring(index + fullMatch.length + 1)
                            else -> result.substring(0, index) + result.substring(index + fullMatch.length)
                        }
                        result = replacement
                    }
                }
            }
        }


        // 处理简单格式：hasitem={...}
        val simplePattern = "hasitem=\\{".toRegex()
        val simpleMatch = simplePattern.find(result)

        if (simpleMatch != null) {
            val startIndex = simpleMatch.range.first
            // 找到匹配的 hasitem 对象的完整内容
            val objectContent = extractObjectContent(result.substring(startIndex + 9))

            if (objectContent != null) {
                val fullMatch = "hasitem={$objectContent}"
                val nbtResult = parseHasitemSingle(objectContent, reminders, context)

                if (nbtResult.isNotEmpty()) {
                    // 精确替换
                    val index = result.indexOf(fullMatch)
                    if (index >= 0) {
                        result = result.substring(0, index) + nbtResult + result.substring(index + fullMatch.length)
                    }
                } else {
                    // 无效参数或转换失败，移除整个 hasitem 参数
                    val index = result.indexOf(fullMatch)
                    if (index >= 0) {
                        val replacement = when {
                            index > 0 && result[index - 1] == ',' -> result.substring(0, index - 1) + result.substring(index + fullMatch.length)
                            index + fullMatch.length < result.length && result[index + fullMatch.length] == ',' -> result.substring(0, index) + result.substring(index + fullMatch.length + 1)
                            else -> result.substring(0, index) + result.substring(index + fullMatch.length)
                        }
                        result = replacement
                    }
                }
            }
        }

        // 清理多余的逗号和空括号
        result = result.replace(",,", ",")
        result = result.replace(",\\]".toRegex(), "]")
        result = result.replace("\\[".toRegex(), "[")

        return result to reminders
    }

    /**
     * 提取完整的数组内容
     */
    private fun extractArrayContent(str: String): String? {
        var bracketCount = 0
        var braceCount = 0
        var result = StringBuilder()
        var started = false

        for (char in str) {
            when (char) {
                '[' -> {
                    bracketCount++
                    if (!started) {
                        started = true  // 跳过第一个 '['
                    } else {
                        result.append(char)  // 保留内层的 '['
                    }
                }
                ']' -> {
                    bracketCount--
                    if (bracketCount == 0 && started) {
                        // 遇到外层的 ']'，结束
                        return result.toString()
                    } else {
                        result.append(char)  // 保留内层的 ']'
                    }
                }
                '{' -> {
                    braceCount++
                    if (started) {
                        result.append(char)
                    }
                }
                '}' -> {
                    braceCount--
                    if (started) {
                        result.append(char)
                    }
                }
                else -> {
                    if (started) {
                        result.append(char)
                    }
                }
            }
        }

        return null
    }

    /**
     * 提取完整的对象内容
     */
    private fun extractObjectContent(str: String): String? {
        var braceCount = 0
        var result = StringBuilder()
        var started = false

        for (char in str) {
            when (char) {
                '{' -> {
                    braceCount++
                    if (!started) {
                        started = true  // 跳过第一个 '{'
                    } else {
                        result.append(char)  // 保留内层的 '{'
                    }
                }
                '}' -> {
                    braceCount--
                    if (braceCount == 0 && started) {
                        // 遇到外层的 '}'，结束
                        return result.toString()
                    } else {
                        result.append(char)  // 保留内层的 '}'
                    }
                }
                else -> {
                    if (started) {
                        result.append(char)
                    }
                }
            }
        }

        return null
    }

    /**
     * 解析单个 hasitem 条目
     */
    private fun parseHasitemSingle(content: String, reminders: MutableList<String>, context: Context): String {
        val params = mutableMapOf<String, String>()
        val parts = parseHasitemObjectParams(content)

        for (part in parts) {
            if ("=" in part) {
                val equalIndex = part.indexOf('=')
                val key = part.substring(0, equalIndex).trim()
                val value = part.substring(equalIndex + 1).trim()
                params[key] = value
            }
        }

        val item = params["item"]

        // 如果没有item参数，或者item参数为空，返回空字符串（表示移除该参数）
        if (item.isNullOrBlank()) {
            return ""
        }

        val quantity = params["quantity"]
        val location = params["location"]
        val slot = params["slot"]

        // 处理物品ID
        val itemId = if (item.startsWith("minecraft:")) item else "minecraft:$item"

        // 处理 quantity 范围
        val processedQuantity = processQuantityRange(quantity, reminders, context)

        // 根据位置类型转换
        return when (location) {
            "slot.weapon.mainhand" -> {
                // 主手 → SelectedItem
                val countPart = if (processedQuantity != null) ",Count:${processedQuantity}b" else ""
                "nbt={SelectedItem:{id:\"$itemId\"$countPart}}"
            }
            "slot.weapon.offhand" -> {
                // 副手 → equipment.offhand
                val countPart = if (processedQuantity != null) ",Count:${processedQuantity}b" else ""
                "nbt={equipment:{offhand:{id:\"$itemId\"$countPart}}}"
            }
            "slot.armor.head" -> {
                // 头盔 → equipment.head
                val countPart = if (processedQuantity != null) ",Count:${processedQuantity}b" else ""
                "nbt={equipment:{head:{id:\"$itemId\"$countPart}}}"
            }
            "slot.armor.chest" -> {
                // 胸甲 → equipment.chest
                val countPart = if (processedQuantity != null) ",Count:${processedQuantity}b" else ""
                "nbt={equipment:{chest:{id:\"$itemId\"$countPart}}}"
            }
            "slot.armor.legs" -> {
                // 护腿 → equipment.legs
                val countPart = if (processedQuantity != null) ",Count:${processedQuantity}b" else ""
                "nbt={equipment:{legs:{id:\"$itemId\"$countPart}}}"
            }
            "slot.armor.feet" -> {
                // 靴子 → equipment.feet
                val countPart = if (processedQuantity != null) ",Count:${processedQuantity}b" else ""
                "nbt={equipment:{feet:{id:\"$itemId\"$countPart}}}"
            }
            "slot.hotbar", "slot.inventory" -> {
                // 物品栏 → Inventory
                // 转换规则：
                // - 基岩版 slot.hotbar,slot=0-8 → Java版 Inventory Slot 0-8（直接对应）
                // - 基岩版 slot.inventory,slot=0-... → Java版 Inventory Slot 9-...（需要+9）
                val slotNumbers = parseSlotRange(slot, location, reminders, context)
                if (slotNumbers.isEmpty()) {
                    // 没有指定槽位，使用通用格式
                    "nbt={Inventory:[{id:\"$itemId\"}]}"
                } else {
                    // 构建多个槽位的 NBT
                    // parseSlotRange 已经返回了转换后的 Java 版槽位编号
                    val nbtItems = slotNumbers.map { slotNum ->
                        val countPart = if (processedQuantity != null) ",Count:${processedQuantity}b" else ""
                        "{Slot:${slotNum}b,id:\"$itemId\"$countPart}"
                    }
                    "nbt={Inventory:[${nbtItems.joinToString(",")}]}"
                }
            }
            null -> {
                // 没有指定位置，使用通用格式（不指定槽位）
                "nbt={Inventory:[{id:\"$itemId\"}]}"
            }
            else -> {
                // 不支持的位置
                reminders.add(getStringSafely(context, R.string.hasitem_location_not_supported, location))
                ""
            }
        }
    }

    /**
     * 解析 hasitem 数组
     * 根据特别提醒.txt的要求:SelectedItem, Inventory, equipment这三大nbt参数不能放在同一个nbt大括里面
     * 需要返回多个独立的nbt参数
     */
    private fun parseHasitemArray(content: String, reminders: MutableList<String>, context: Context): String {
        // 如果内容为空，返回空字符串（表示移除该参数）
        if (content.trim().isEmpty()) {
            return ""
        }

        val objects = mutableListOf<String>()
        var braceCount = 0
        var currentObj = ""

        for (char in content + ",") {
            when (char) {
                '{' -> {
                    braceCount++
                    if (braceCount == 1) {
                        currentObj = "{"
                    } else {
                        currentObj += char
                    }
                }
                '}' -> {
                    braceCount--
                    currentObj += char
                    if (braceCount == 0 && currentObj.isNotEmpty()) {
                        // 去掉外层花括号
                        objects.add(currentObj.substring(1, currentObj.length - 1))
                        currentObj = ""
                    }
                }
                else -> {
                    if (braceCount > 0) {
                        currentObj += char
                    }
                }
            }
        }

        // 解析每个对象，分类处理
        val equipmentItems = mutableMapOf<String, String>()  // head, chest, legs, feet, offhand
        val inventoryItems = mutableListOf<String>()  // 物品栏物品
        val selectedItem = mutableListOf<String>()  // 主手物品

        for (obj in objects) {
            val params = mutableMapOf<String, String>()
            // 使用智能解析来分割参数，正确处理嵌套的NBT结构
            val parts = parseHasitemObjectParams(obj)

            for (part in parts) {
                if ("=" in part) {
                    val equalIndex = part.indexOf('=')
                    val key = part.substring(0, equalIndex).trim()
                    val value = part.substring(equalIndex + 1).trim()
                    params[key] = value
                }
            }

            val item = params["item"]
            // 如果item为空或只有quantity/location/slot参数，跳过此对象
            if (item.isNullOrBlank()) {
                continue
            }

            val quantity = params["quantity"]
            val location = params["location"]
            val slot = params["slot"]

            // 处理物品ID
            val itemId = if (item.startsWith("minecraft:")) item else "minecraft:$item"

            // 处理 quantity 范围
            val processedQuantity = processQuantityRange(quantity, reminders, context)
            val countPart = if (processedQuantity != null) ",Count:${processedQuantity}b" else ""

            // 根据位置类型分类
            when (location) {
                "slot.weapon.mainhand" -> {
                    // 主手 → SelectedItem
                    selectedItem.add("{id:\"$itemId\"$countPart}")
                }
                "slot.weapon.offhand" -> {
                    // 副手 → equipment.offhand
                    equipmentItems["offhand"] = "{id:\"$itemId\"$countPart}"
                }
                "slot.armor.head" -> {
                    // 头盔 → equipment.head
                    equipmentItems["head"] = "{id:\"$itemId\"$countPart}"
                }
                "slot.armor.chest" -> {
                    // 胸甲 → equipment.chest
                    equipmentItems["chest"] = "{id:\"$itemId\"$countPart}"
                }
                "slot.armor.legs" -> {
                    // 护腿 → equipment.legs
                    equipmentItems["legs"] = "{id:\"$itemId\"$countPart}"
                }
                "slot.armor.feet" -> {
                    // 靴子 → equipment.feet
                    equipmentItems["feet"] = "{id:\"$itemId\"$countPart}"
                }
                "slot.hotbar", "slot.inventory" -> {
                    // 物品栏 → Inventory
                    // 转换规则：
                    // - 基岩版 slot.hotbar,slot=0-8 → Java版 Inventory Slot 0-8（直接对应）
                    // - 基岩版 slot.inventory,slot=0-... → Java版 Inventory Slot 9-...（需要+9）
                    val slotNumbers = parseSlotRange(slot, location, reminders, context)
                    if (slotNumbers.isEmpty()) {
                        // 没有指定槽位，使用通用格式
                        inventoryItems.add("{id:\"$itemId\"$countPart}")
                    } else {
                        // 构建多个槽位的 NBT
                        // parseSlotRange 已经返回了转换后的 Java 版槽位编号
                        for (slotNum in slotNumbers) {
                            inventoryItems.add("{Slot:${slotNum}b,id:\"$itemId\"$countPart}")
                        }
                    }
                }
                null -> {
                    // 没有指定位置，使用通用格式（不指定槽位）
                    inventoryItems.add("{id:\"$itemId\"$countPart}")
                }
            }
        }

        // 构建多个独立的nbt参数
        // 根据特别提醒.txt:SelectedItem, Inventory, equipment这三大nbt参数不能放在同一个nbt大括里面
        val nbtParams = mutableListOf<String>()

        // 添加 SelectedItem (独立的nbt参数)
        if (selectedItem.isNotEmpty()) {
            nbtParams.add("nbt={SelectedItem:${selectedItem[0]}}")
        }

        // 添加 equipment (独立的nbt参数)
        if (equipmentItems.isNotEmpty()) {
            val equipmentParts = equipmentItems.map { (key, value) -> "$key:$value" }
            nbtParams.add("nbt={equipment:{${equipmentParts.joinToString(",")}}}")
        }

        // 添加 Inventory (独立的nbt参数)
        if (inventoryItems.isNotEmpty()) {
            nbtParams.add("nbt={Inventory:[${inventoryItems.joinToString(",")}]}")
        }

        // 返回多个独立的nbt参数,用逗号分隔
        return if (nbtParams.isNotEmpty()) {
            nbtParams.joinToString(",")
        } else {
            ""
        }
    }

    /**
     * 解析 hasitem 对象的参数
     * 使用智能解析来正确处理嵌套的NBT结构
     */
    private fun parseHasitemObjectParams(obj: String): List<String> {
        val params = mutableListOf<String>()
        var braceCount = 0
        var currentParam = ""
        var inStringValue = false
        var stringChar = '"'

        for (char in obj + ",") {
            when {
                !inStringValue && (char == '"' || char == '\'') -> {
                    inStringValue = true
                    stringChar = char
                    currentParam += char
                }
                inStringValue && char == stringChar -> {
                    inStringValue = false
                    currentParam += char
                }
                !inStringValue && char == '{' -> {
                    braceCount++
                    currentParam += char
                }
                !inStringValue && char == '}' -> {
                    braceCount--
                    currentParam += char
                }
                !inStringValue && char == ',' && braceCount == 0 -> {
                    if (currentParam.trim().isNotEmpty()) {
                        params.add(currentParam.trim())
                    }
                    currentParam = ""
                }
                else -> {
                    currentParam += char
                }
            }
        }

        return params
    }

    /**
     * 处理 nbt 参数转换为 hasitem 参数（Java版到基岩版）
     * 支持处理多个nbt参数,例如: @a[nbt={SelectedItem:{...}},nbt={Inventory:[...]}]
     */
    private fun convertNbtToHasitem(paramsPart: String, context: Context): Pair<String, List<String>> {
        val reminders = mutableListOf<String>()
        var result = paramsPart

        // 收集所有nbt参数的内容
        val allHasitemItems = mutableListOf<String>()
        var nbtMatches = mutableListOf<Pair<Int, String>>()  // (startIndex, fullMatch)

        // 查找所有nbt参数 - 使用更好的方法
        var searchStart = 0
        while (searchStart < result.length) {
            // 查找下一个nbt={
            val nbtIndex = result.indexOf("nbt={", searchStart)
            if (nbtIndex == -1) break

            // 提取完整的nbt内容
            val nbtContent = extractNbtContent(result.substring(nbtIndex + 5))
            
            if (nbtContent != null) {
                val fullMatch = "nbt={$nbtContent}"
                nbtMatches.add(Pair(nbtIndex, fullMatch))

                // 解析这个nbt参数
                val hasitemItems = parseNbtToHasitemItems(nbtContent, reminders, context)
                allHasitemItems.addAll(hasitemItems)
                
                // 从当前nbt参数结束位置开始搜索下一个
                searchStart = nbtIndex + fullMatch.length
            } else {
                // 提取失败，跳过这个参数
                searchStart = nbtIndex + 1
            }
        }

        // 如果有nbt参数被转换,替换所有的nbt参数为一个hasitem参数
        if (nbtMatches.isNotEmpty()) {
            // 移除所有nbt参数
            for ((_, fullMatch) in nbtMatches) {
                result = result.replace(fullMatch, "")
            }

            // 清理多余的逗号
            result = result.replace(",,", ",")
            result = result.replace(",\\]".toRegex(), "]")

            // 如果成功解析出了hasitem物品，添加hasitem参数
            if (allHasitemItems.isNotEmpty()) {
                // 构建hasitem参数
                val hasitemResult = if (allHasitemItems.size == 1) {
                    "hasitem={${allHasitemItems[0]}}"
                } else {
                    "hasitem=[${allHasitemItems.joinToString(",") { "{$it}" }}]"
                }

                // 添加hasitem参数
                if (result.endsWith("]")) {
                    result = result.dropLast(1) + ",$hasitemResult]"
                } else if (result.endsWith("[")) {
                    result = result.dropLast(1) + "$hasitemResult]"
                } else {
                    result = "$result,$hasitemResult"
                }
            } else {
                // 没有成功解析出hasitem物品，提醒用户
                reminders.add(getStringSafely(context, R.string.java_nbt_param_not_supported))
            }
        }

        return result to reminders
    }

    /**
     * 提取完整的 nbt 内容
     * 确保只提取第一个完整的nbt对象（从第一个{开始，到匹配的}结束）
     */
    private fun extractNbtContent(str: String): String? {
        var braceCount = 0
        var result = StringBuilder()
        var started = false

        for (char in str) {
            when (char) {
                '{' -> {
                    braceCount++
                    if (!started) {
                        started = true  // 跳过第一个 '{'
                    } else {
                        result.append(char)  // 保留内层的 '{'
                    }
                }
                '}' -> {
                    braceCount--
                    if (braceCount == 0 && started) {
                        // 遇到外层的 '}'，结束
                        return result.toString()
                    } else {
                        result.append(char)  // 保留内层的 '}'
                    }
                }
                else -> {
                    if (started) {
                        result.append(char)
                    }
                }
            }
        }

        return null
    }

    /**
     * 解析 nbt 内容转换为 hasitem 物品列表
     * 返回hasitem物品的字符串列表,不进行格式化拼接
     */
    private fun parseNbtToHasitemItems(nbtContent: String, reminders: MutableList<String>, context: Context): List<String> {
        val hasitemItems = mutableListOf<String>()

        // 解析 SelectedItem - 使用智能提取方法
        val selectedItemMatch = extractKeyValue(nbtContent, "SelectedItem")
        if (selectedItemMatch != null) {
            val hasitemItem = parseNbtItemToHasitem(selectedItemMatch, "slot.weapon.mainhand", "0")
            if (hasitemItem.isNotEmpty()) {
                hasitemItems.add(hasitemItem)
            }
        }

        // 解析 equipment - 使用智能提取方法
        val equipmentMatch = extractKeyValue(nbtContent, "equipment")
        if (equipmentMatch != null) {
            // 移除外层花括号
            val innerContent = if (equipmentMatch.startsWith("{") && equipmentMatch.endsWith("}")) {
                equipmentMatch.substring(1, equipmentMatch.length - 1)
            } else {
                equipmentMatch
            }

            // 解析equipment的各个槽位
            val equipmentItems = parseEquipmentContent(innerContent, reminders, context)
            hasitemItems.addAll(equipmentItems)
        }

        // 解析 Inventory - 使用智能提取方法
        val inventoryMatch = extractKeyValue(nbtContent, "Inventory")
        if (inventoryMatch != null) {
            // 移除外层方括号
            val innerContent = if (inventoryMatch.startsWith("[") && inventoryMatch.endsWith("]")) {
                inventoryMatch.substring(1, inventoryMatch.length - 1)
            } else {
                inventoryMatch
            }

            // 解析Inventory中的每个物品
            val inventoryItems = parseInventoryContent(innerContent, reminders, context)
            hasitemItems.addAll(inventoryItems)
        }

        return hasitemItems
    }

    /**
     * 从 NBT 内容中提取键值对
     * 例如从 "SelectedItem:{id:\"...\"}" 提取 "{id:\"...\"}"
     */
    private fun extractKeyValue(nbtContent: String, key: String): String? {
        // 查找键的位置，使用更严格的边界匹配
        // 确保不会匹配其他键名中包含此键名作为子串的情况（例如不会从 "SelectedItem" 中匹配 "Inventory"）
        val keyPattern = Regex("(^|,|\\{)\\s*$key\\s*:\\s*")
        val match = keyPattern.find(nbtContent) ?: return null

        val startIndex = match.range.last
        // 从键的后面开始解析
        val remaining = nbtContent.substring(startIndex).trim()

        // 如果remaining为空，返回null
        if (remaining.isEmpty()) return null

        // 判断值的类型
        val firstChar = remaining.firstOrNull()
        return when (firstChar) {
            '{' -> extractBraceContent(remaining)
            '[' -> extractBracketContent(remaining)
            '"' -> {
                // 字符串值
                val endQuote = remaining.indexOf('"', 1)
                if (endQuote > 0) {
                    remaining.substring(0, endQuote + 1)
                } else {
                    remaining
                }
            }
            else -> {
                // 其他值（数字、布尔值等）
                // 提取直到遇到逗号、花括号或字符串结束
                var braceCount = 0
                var result = StringBuilder()
                var inStringValue = false
                var stringChar = '"'
                
                for (char in remaining) {
                    when {
                        !inStringValue && (char == '"' || char == '\'') -> {
                            inStringValue = true
                            stringChar = char
                            result.append(char)
                        }
                        inStringValue && char == stringChar -> {
                            inStringValue = false
                            result.append(char)
                        }
                        !inStringValue && char == '{' -> {
                            braceCount++
                            result.append(char)
                        }
                        !inStringValue && char == '}' -> {
                            if (braceCount > 0) {
                                braceCount--
                                result.append(char)
                            } else {
                                // 遇到外层的 '}'，结束
                                break
                            }
                        }
                        !inStringValue && char == ',' && braceCount == 0 -> {
                            // 遇到分隔符，结束
                            break
                        }
                        else -> {
                            result.append(char)
                        }
                    }
                }
                
                val resultStr = result.toString().trim()
                if (resultStr.isEmpty()) null else resultStr
            }
        }
    }

    /**
     * 提取花括号内容（包括外层花括号）
     */
    private fun extractBraceContent(str: String): String? {
        var braceCount = 0
        var result = StringBuilder()

        for (char in str) {
            when (char) {
                '{' -> {
                    braceCount++
                    result.append(char)
                }
                '}' -> {
                    braceCount--
                    result.append(char)
                    if (braceCount == 0) {
                        return result.toString()
                    }
                }
                else -> {
                    if (braceCount > 0) {
                        result.append(char)
                    }
                }
            }
        }

        return null
    }

    /**
     * 提取方括号内容（包括外层方括号）
     */
    private fun extractBracketContent(str: String): String? {
        var bracketCount = 0
        var result = StringBuilder()

        for (char in str) {
            when (char) {
                '[' -> {
                    bracketCount++
                    result.append(char)
                }
                ']' -> {
                    bracketCount--
                    result.append(char)
                    if (bracketCount == 0) {
                        return result.toString()
                    }
                }
                else -> {
                    if (bracketCount > 0) {
                        result.append(char)
                    }
                }
            }
        }

        return null
    }

    /**
     * 解析 nbt 内容转换为 hasitem
     * 格式化输出为hasitem参数格式
     */
    private fun parseNbtToHasitem(nbtContent: String, reminders: MutableList<String>, context: Context): String {
        val hasitemItems = parseNbtToHasitemItems(nbtContent, reminders, context)

        return if (hasitemItems.size == 1) {
            "{${hasitemItems[0]}}"
        } else if (hasitemItems.size > 1) {
            "[${hasitemItems.joinToString(",") { "{$it}" }}]"
        } else {
            ""
        }
    }

    /**
     * 解析 equipment 内容
     */
    private fun parseEquipmentContent(equipmentData: String, reminders: MutableList<String>, context: Context): List<String> {
        val items = mutableListOf<String>()

        // 解析equipment中的每个槽位
        // 使用智能解析，正确处理嵌套的NBT结构
        var braceCount = 0
        var currentKey = ""
        var currentValue = ""
        var inKey = true

        for (char in equipmentData.trim()) {
            when {
                char == '{' -> {
                    braceCount++
                    if (braceCount == 1 && inKey) {
                        // 开始值部分
                        inKey = false
                        currentValue = "{"
                    } else {
                        currentValue += char
                    }
                }
                char == '}' -> {
                    braceCount--
                    if (braceCount == 0 && !inKey) {
                        currentValue += char
                        // 完成一个槽位的解析
                        if (currentKey.isNotEmpty() && currentValue.isNotEmpty()) {
                            val location = when (currentKey.trim()) {
                                "head" -> "slot.armor.head"
                                "chest" -> "slot.armor.chest"
                                "legs" -> "slot.armor.legs"
                                "feet" -> "slot.armor.feet"
                                "offhand" -> "slot.weapon.offhand"
                                else -> null
                            }
                            if (location != null) {
                                val hasitemItem = parseNbtItemToHasitem(currentValue, location, "0")
                                if (hasitemItem.isNotEmpty()) {
                                    items.add(hasitemItem)
                                }
                            }
                        }
                        // 重置状态
                        currentKey = ""
                        currentValue = ""
                        inKey = true
                    } else {
                        currentValue += char
                    }
                }
                char == ':' && braceCount == 0 -> {
                    // 键值分隔符
                    inKey = false
                }
                char == ',' && braceCount == 0 -> {
                    // 分隔符，跳过
                }
                else -> {
                    if (inKey) {
                        currentKey += char
                    } else {
                        currentValue += char
                    }
                }
            }
        }

        return items
    }

    /**
     * 解析 Inventory 内容
     */
    private fun parseInventoryContent(inventoryData: String, reminders: MutableList<String>, context: Context): List<String> {
        val items = mutableListOf<String>()

        // 解析Inventory中的每个物品
        // 使用智能解析，正确处理嵌套的NBT结构
        var braceCount = 0
        var currentItem = ""

        for (char in inventoryData.trim()) {
            when {
                char == '{' -> {
                    braceCount++
                    if (braceCount == 1) {
                        currentItem = "{"
                    } else {
                        currentItem += char
                    }
                }
                char == '}' -> {
                    braceCount--
                    currentItem += char
                    if (braceCount == 0 && currentItem.isNotEmpty()) {
                        // 完成一个物品的解析
                        val hasitemItem = parseInventoryItemToHasitem(currentItem, reminders, context)
                        if (hasitemItem.isNotEmpty()) {
                            items.add(hasitemItem)
                        }
                        currentItem = ""
                    }
                }
                else -> {
                    if (braceCount > 0) {
                        currentItem += char
                    }
                }
            }
        }

        return items
    }

    /**
     * 解析单个 Inventory 物品转换为 hasitem 格式
     */
    private fun parseInventoryItemToHasitem(itemData: String, reminders: MutableList<String>, context: Context): String {
        // 提取 Slot（支持带b后缀和不带b后缀）
        val slotPattern = "Slot\\s*:\\s*(-?\\d+)[bB]?".toRegex()
        val slotMatch = slotPattern.find(itemData)
        val slotNum = slotMatch?.groupValues?.get(1)?.toIntOrNull()

        // 跳过 SelectedItem (Slot:-106)
        if (slotNum != null && slotNum == -106) return ""

        // 如果没有Slot，不添加location和slot参数
        if (slotNum == null) {
            // 提取 id
            val idPattern = "id\\s*:\\s*[\"']([^\"']+)[\"']".toRegex()
            val idMatch = idPattern.find(itemData)
            val itemId = idMatch?.groupValues?.get(1) ?: return ""

            // 移除 minecraft: 前缀
            val cleanItemId = if (itemId.startsWith("minecraft:")) {
                itemId.substring(10)
            } else {
                itemId
            }

            // 提取 Count（nbt中使用大写Count，也支持小写count，也支持不带b后缀）
            val countPattern = "[Cc]ount\\s*:\\s*(\\d+)[bB]?".toRegex()
            val countMatch = countPattern.find(itemData)

            val itemStr = mutableListOf("item=$cleanItemId")

            if (countMatch != null) {
                // 转换为quantity（hasitem中使用quantity）
                itemStr.add("quantity=${countMatch.groupValues[1]}")
            }

            return itemStr.joinToString(",")
        }

        // 如果有Slot，确定位置类型
        // 转换规则：
        // - Java版 Slot 0-8 → 基岩版 slot.hotbar 0-8（直接对应）
        // - Java版 Slot 9-35 → 基岩版 slot.inventory (Slot-9)
        val (location, hasitemSlot) = if (slotNum >= 0 && slotNum <= 8) {
            "slot.hotbar" to slotNum.toString()
        } else {
            "slot.inventory" to (slotNum - 9).toString()
        }

        return parseNbtItemToHasitem(itemData, location, hasitemSlot)
    }

    /**
     * 解析单个 NBT 物品转换为 hasitem 格式
     */
    private fun parseNbtItemToHasitem(
        itemData: String,
        location: String,
        slot: String
    ): String {
        // 提取 id
        val idPattern = "id\\s*:\\s*[\"']([^\"']+)[\"']".toRegex()
        val idMatch = idPattern.find(itemData)
        val itemId = idMatch?.groupValues?.get(1) ?: return ""

        // 移除 minecraft: 前缀
        val cleanItemId = if (itemId.startsWith("minecraft:")) {
            itemId.substring(10)
        } else {
            itemId
        }

        // 提取 Count（nbt中使用大写Count，也支持小写count，也支持不带b后缀）
        val countPattern = "[Cc]ount\\s*:\\s*(\\d+)[bB]?".toRegex()
        val countMatch = countPattern.find(itemData)

        val itemStr = mutableListOf("item=$cleanItemId")
        itemStr.add("location=$location")
        itemStr.add("slot=$slot")

        if (countMatch != null) {
            // 转换为quantity（hasitem中使用quantity）
            itemStr.add("quantity=${countMatch.groupValues[1]}")
        }

        return itemStr.joinToString(",")
    }

    /**
     * 处理 quantity 范围
     * 返回处理后的数量（整数），并添加提醒信息
     */
    private fun processQuantityRange(
        quantity: String?,
        reminders: MutableList<String>,
        context: Context
    ): String? {
        if (quantity == null) return null

        return when {
            quantity == "0.." -> {
                // 0.. 表示不做过滤，返回 null
                null
            }
            quantity.endsWith("..") -> {
                // 2.. 取 2
                val value = quantity.substringBefore("..").toIntOrNull()
                if (value != null) {
                    reminders.add(getStringSafely(context, R.string.hasitem_quantity_min_only, value))
                    "${value}b"
                } else null
            }
            quantity.startsWith("..") -> {
                // ..5 取 5
                val value = quantity.substringAfter("..").toIntOrNull()
                if (value != null) {
                    reminders.add(getStringSafely(context, R.string.hasitem_quantity_max_only, value))
                    "${value}b"
                } else null
            }
            ".." in quantity -> {
                // 3..5 取中间值
                val parts = quantity.split("..")
                if (parts.size == 2) {
                    val min = parts[0].toIntOrNull()
                    val max = parts[1].toIntOrNull()
                    if (min != null && max != null) {
                        val midValue = ((min + max) / 2.0).roundToInt()
                        reminders.add(getStringSafely(context, R.string.hasitem_quantity_range, min, max, midValue))
                        "${midValue}b"
                    } else null
                } else null
            }
            quantity.startsWith("!") -> {
                // !5 不支持，返回 null 并提醒
                reminders.add(getStringSafely(context, R.string.hasitem_quantity_negation_not_supported))
                null
            }
            else -> {
                // 单个数字，直接使用
                val value = quantity.toIntOrNull()
                if (value != null) {
                    "${value}b"
                } else null
            }
        }
    }

    /**
     * 解析 slot 范围（基岩版到Java版）
     * 返回Java版的槽位编号列表
     * 
     * 转换规则：
     * - 基岩版 slot.hotbar,slot=0-8 → Java版 Inventory Slot 0-8（直接对应）
     * - 基岩版 slot.inventory,slot=0 → Java版 Inventory Slot 9（需要 +9）
     */
    private fun parseSlotRange(
        slot: String?,
        location: String,
        reminders: MutableList<String>,
        context: Context
    ): List<Int> {
        if (slot == null) return emptyList()

        val slotNumbers = mutableListOf<Int>()

        when {
            slot.endsWith("..") -> {
                // 0.. 取 0
                val value = slot.substringBefore("..").toIntOrNull()
                if (value != null) {
                    slotNumbers.add(value)
                    reminders.add(getStringSafely(context, R.string.hasitem_slot_min_only, value))
                }
            }
            slot.startsWith("..") -> {
                // ..8 取 8
                val value = slot.substringAfter("..").toIntOrNull()
                if (value != null) {
                    slotNumbers.add(value)
                    reminders.add(getStringSafely(context, R.string.hasitem_slot_max_only, value))
                }
            }
            ".." in slot -> {
                // 0..8 取所有值
                val parts = slot.split("..")
                if (parts.size == 2) {
                    val min = parts[0].toIntOrNull()
                    val max = parts[1].toIntOrNull()
                    if (min != null && max != null) {
                        for (i in min..max) {
                            slotNumbers.add(i)
                        }
                    }
                }
            }
            slot.startsWith("!") -> {
                // !0 不支持
                reminders.add(getStringSafely(context, R.string.hasitem_slot_negation_not_supported))
            }
            else -> {
                // 单个槽位
                val value = slot.toIntOrNull()
                if (value != null) {
                    slotNumbers.add(value)
                }
            }
        }

        // 转换槽位编号：基岩版到Java版
        return slotNumbers.map { slotNum ->
            when (location) {
                "slot.hotbar" -> slotNum  // slot.hotbar 0-8 → Inventory 0-8
                "slot.inventory" -> slotNum + 9  // slot.inventory 0 → Inventory 9
                else -> slotNum
            }
        }
    }
}