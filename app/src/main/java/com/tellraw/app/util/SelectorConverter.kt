package com.tellraw.app.util

import com.tellraw.app.model.*
import java.util.regex.Pattern

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
        "§n" to "§6",  // material_copper -> gold (特殊处理)
        "§p" to "§6",  // material_gold -> gold
        "§q" to "§a",  // material_emerald -> green
        "§s" to "§b",  // material_diamond -> aqua
        "§t" to "§1",  // material_lapis -> dark_blue
        "§u" to "§d",  // material_amethyst -> light_purple
        "§v" to "§6",  // material_resin -> gold,
    )
    
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
                else -> SelectorType.BEDROCK // 默认返回基岩版
            }
        }
        
        return SelectorType.BEDROCK // 默认返回基岩版
    }
    
    /**
     * 将基岩版选择器转换为Java版
     */
    fun convertBedrockToJava(selector: String): SelectorConversionResult {
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
            newSelector = selectorMapping[selectorVar] + if (paramsPart.isNotEmpty()) "[$paramsPart]" else ""
            reminders.add("基岩版选择器 " + selectorVar + " 在Java版中不支持，已转换为 " + selectorMapping[selectorVar])
            wasConverted = true
        }
        
        // 转换参数
        val (convertedSelector, paramReminders) = convertSelectorParameters(newSelector, SelectorType.JAVA)
        reminders.addAll(paramReminders)
        
        // 进一步转换参数格式
        val javaSelector = convertBedrockParametersToJava(convertedSelector, reminders)
        
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
    fun convertJavaToBedrock(selector: String): SelectorConversionResult {
        val reminders = mutableListOf<String>()
        
        // 转换参数
        val (convertedSelector, paramReminders) = convertSelectorParameters(selector, SelectorType.BEDROCK)
        reminders.addAll(paramReminders)
        
        // 进一步转换参数格式
        var bedrockSelector = convertJavaParametersToBedrock(convertedSelector, reminders)
        
        // 处理NBT参数中的范围值
        bedrockSelector = processRangeValues(bedrockSelector)
        
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
    private fun convertSelectorParameters(selector: String, targetType: SelectorType): Pair<String, List<String>> {
        val reminders = mutableListOf<String>()
        
        if ('[' !in selector || ']' !in selector) {
            return selector to reminders
        }
        
        val selectorVar = selector.split('[')[0]
        var paramsPart = selector.substringAfter('[').substringBefore(']')
        
        // 处理hasitem参数转换为nbt参数
        if (targetType == SelectorType.JAVA && "hasitem=" in paramsPart) {
            val (converted, hasitemReminders) = convertHasitemToNbt(paramsPart)
            paramsPart = converted
            reminders.addAll(hasitemReminders)
        }
        
        // 处理nbt参数转换为hasitem参数
        if (targetType == SelectorType.BEDROCK && "nbt=" in paramsPart) {
            val (converted, nbtReminders) = convertNbtToHasitem(paramsPart)
            paramsPart = converted
            reminders.addAll(nbtReminders)
        }
        
        // 去除不必要的空格
        paramsPart = removeUnnecessarySpaces(paramsPart)
        
        return selectorVar + "[" + paramsPart + "]" to reminders
    }
    
    /**
     * 根据目标版本过滤选择器参数，对于可转换的参数进行转换，
     * 只有完全不支持的参数才被剔除
     * 与Python版本的filter_selector_parameters函数逻辑一致
     */
    fun filterSelectorParameters(selector: String, targetVersion: SelectorType): Pair<String, List<String>> {
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
            return selector to conversionReminders
        }
        
        // 提取参数部分
        val selectorVar = selector.split('[')[0]
        var paramsPart = selector.substringAfter('[').substringBefore(']')
        
        // 处理hasitem到nbt的转换（基岩版到Java版）
        if (targetVersion == SelectorType.JAVA && "hasitem=" in paramsPart) {
            val (converted, hasitemToNbtReminders) = convertHasitemToNbt(paramsPart)
            paramsPart = converted
            conversionReminders.addAll(hasitemToNbtReminders)
        }
        
        // 处理nbt到hasitem的转换（Java版到基岩版）
        if (targetVersion == SelectorType.BEDROCK && "nbt=" in paramsPart) {
            val (converted, nbtToHasitemReminders) = convertNbtToHasitem(paramsPart)
            paramsPart = converted
            conversionReminders.addAll(nbtToHasitemReminders)
        }
        
        // 根据目标版本进行参数转换
        if (targetVersion == SelectorType.BEDROCK) {
            // Java版到基岩版的参数转换
            paramsPart = convertDistanceParameters(paramsPart, conversionReminders)
            paramsPart = convertRotationParameters(paramsPart, conversionReminders)
            paramsPart = convertLevelParameters(paramsPart, conversionReminders)
            
            // 处理gamemode到m的转换（Java版到基岩版）
            paramsPart = convertGamemodeToM(paramsPart, conversionReminders)
            
            // 处理sort参数（基岩版不支持）
            val sortPattern = ",?sort=([^,\\]]+)".toRegex()
            paramsPart = paramsPart.replace(sortPattern) { match ->
                val sortValue = match.groupValues[1]
                conversionReminders.add("Java版sort=" + sortValue + "参数在基岩版中不支持，已移除")
                ""
            }
            
            // 处理limit到c的转换
            val limitPattern = "limit=([+-]?\\d+)".toRegex()
            paramsPart = paramsPart.replace(limitPattern) { match ->
                val limitValue = match.groupValues[1]
                conversionReminders.add("Java版limit=" + limitValue + "参数已转换为基岩版c=" + limitValue)
                "c=" + limitValue
            }
        }
        
        // 分割参数，但要处理包含大括号的参数
        val paramPattern = "([^=,\\{\\}]+=\\{[^{}]*\\}|[^=,\\{\\}]+=[^,\\{\\}]+)".toRegex()
        val params = paramPattern.findAll(paramsPart).map { it.value }.toList()
        
        // 过滤参数并收集提醒信息
        val filteredParams = mutableListOf<String>()
        val removedParams = mutableListOf<String>()
        
        for (param in params) {
            if ('=' in param) {
                val paramName = param.split('=')[0].trim()
                
                // 特殊处理haspermission参数，它包含大括号
                if (paramName == "haspermission" && targetVersion == SelectorType.JAVA) {
                    removedParams.add(paramName)
                    conversionReminders.add("警告：基岩版" + paramName + "参数在Java版中没有对应的功能，已移除")
                    continue  // 跳过此参数，不添加到filteredParams中
                }
                
                // 特殊处理nbt参数
                if (paramName == "nbt") {
                    if (targetVersion == SelectorType.BEDROCK) {
                        // Java版的nbt参数在基岩版中不支持
                        removedParams.add("nbt")
                        conversionReminders.add("警告：Java版nbt参数在基岩版中不支持，已尝试转换为hasitem参数，如果转换失败则已移除")
                    } else {
                        // 保留Java版的nbt参数
                        filteredParams.add(param)
                    }
                }
                // 根据目标版本过滤参数
                else if (targetVersion == SelectorType.JAVA && paramName in bedrockSpecificParams) {
                    removedParams.add(paramName)
                    when (paramName) {
                        "family" -> conversionReminders.add("警告：基岩版" + paramName + "参数在Java版中没有直接对应的功能，已移除。建议使用type参数指定实体类型作为替代")
                                        "haspermission" -> conversionReminders.add("警告：基岩版" + paramName + "参数在Java版中没有对应的功能，已移除")
                                        "has_property" -> conversionReminders.add("警告：基岩版" + paramName + "参数在Java版中没有对应的功能，已移除")
                                        else -> conversionReminders.add("警告：基岩版" + paramName + "参数在Java版中不支持，已移除")                    }
                    // 不将此参数添加到filteredParams中，即跳过此参数
                    continue  // 跳过此参数，不添加到filteredParams中
                }
                else if (targetVersion == SelectorType.BEDROCK && paramName in javaSpecificParams) {
                    removedParams.add(paramName)
                    when (paramName) {
                        "team" -> conversionReminders.add("警告：Java版" + paramName + "参数在基岩版中不支持，已移除。基岩版中没有队伍系统的直接对应功能")
                                        "predicate" -> conversionReminders.add("警告：Java版" + paramName + "参数在基岩版中不支持，已移除。基岩版中没有谓词系统")
                                        "advancements" -> conversionReminders.add("警告：Java版" + paramName + "参数在基岩版中不支持，已移除。基岩版中没有进度系统")
                                        else -> conversionReminders.add("警告：Java版" + paramName + "参数在基岩版中不支持，已移除")                    }
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
        
        return finalSelector to conversionReminders
    }
    
    /**
     * 将Java版的distance参数转换为基岩版的r/rm参数
     */
    private fun convertDistanceParameters(paramsPart: String, conversionReminders: MutableList<String>): String {
        var result = paramsPart
        
        // 处理distance参数
        val distancePattern = "distance=([^,\\]]+)".toRegex()
        
        result = result.replace(distancePattern) { match ->
            val distanceValue = match.groupValues[1]
            
            // 检查是否为范围格式
            if (".." in distanceValue) {
                val parts = distanceValue.split("..")
                if (parts[0].isNotEmpty() && parts[1].isNotEmpty()) {
                    // 有上下限：5..10 -> rm=5,r=10
                    val rmVal = parts[0]
                    val rVal = parts[1]
                    conversionReminders.add("Java版distance=" + distanceValue + "参数已转换为基岩版rm=" + rmVal + ",r=" + rVal)
                    "rm=$rmVal,r=$rVal"
                } else if (parts[0].isNotEmpty()) {
                    // 只有下限：5.. -> rm=5
                    val rmVal = parts[0]
                    conversionReminders.add("Java版distance=" + distanceValue + "参数已转换为基岩版rm=" + rmVal)
                    "rm=$rmVal"
                } else if (parts[1].isNotEmpty()) {
                    // 只有上限：..10 -> r=10
                    val rVal = parts[1]
                    conversionReminders.add("Java版distance=" + distanceValue + "参数已转换为基岩版r=" + rVal)
                    "r=$rVal"
                } else {
                    // 无效格式
                    match.value
                }
            } else {
                // 单个值：10 -> rm=10,r=10（精确匹配）
                conversionReminders.add("Java版distance=" + distanceValue + "参数已转换为基岩版rm=" + distanceValue + ",r=" + distanceValue)
                "rm=$distanceValue,r=$distanceValue"
            }
        }
        
        // 清理多余的逗号和空括号
        result = result.replace(",,", ",")
        result = result.replace(",\\]".toRegex(), "]")
        
        return result
    }
    
    /**
     * 将Java版的x_rotation/y_rotation参数转换为基岩版的rx/rxm和ry/rym参数
     */
    private fun convertRotationParameters(paramsPart: String, conversionReminders: MutableList<String>): String {
        var result = paramsPart
        
        // 处理x_rotation参数
        val xRotationPattern = "x_rotation=([^,\\]]+)".toRegex()
        
        result = result.replace(xRotationPattern) { match ->
            val rotationValue = match.groupValues[1]
            
            // 检查是否为范围格式
            if (".." in rotationValue) {
                val parts = rotationValue.split("..")
                if (parts[0].isNotEmpty() && parts[1].isNotEmpty()) {
                    // 有上下限：-45..45 -> rxm=-45,rx=45
                    val rxmVal = parts[0]
                    val rxVal = parts[1]
                    conversionReminders.add("Java版x_rotation=" + rotationValue + "参数已转换为基岩版rxm=" + rxmVal + ",rx=" + rxVal)
                    "rxm=$rxmVal,rx=$rxVal"
                } else if (parts[0].isNotEmpty()) {
                    // 只有下限：-45.. -> rxm=-45
                    val rxmVal = parts[0]
                    conversionReminders.add("Java版x_rotation=" + rotationValue + "参数已转换为基岩版rxm=" + rxmVal)
                    "rxm=$rxmVal"
                } else if (parts[1].isNotEmpty()) {
                    // 只有上限：..45 -> rx=45
                    val rxVal = parts[1]
                    conversionReminders.add("Java版x_rotation=" + rotationValue + "参数已转换为基岩版rx=" + rxVal)
                    "rx=$rxVal"
                } else {
                    // 无效格式
                    match.value
                }
            } else {
                // 单个值：45 -> rxm=45,rx=45（精确匹配）
                conversionReminders.add("Java版x_rotation=" + rotationValue + "参数已转换为基岩版rxm=" + rotationValue + ",rx=" + rotationValue)
                "rxm=$rotationValue,rx=$rotationValue"
            }
        }
        
        // 处理y_rotation参数
        val yRotationPattern = "y_rotation=([^,\\]]+)".toRegex()
        
        result = result.replace(yRotationPattern) { match ->
            val rotationValue = match.groupValues[1]
            
            // 检查是否为范围格式
            if (".." in rotationValue) {
                val parts = rotationValue.split("..")
                if (parts[0].isNotEmpty() && parts[1].isNotEmpty()) {
                    // 有上下限：-45..45 -> rym=-45,ry=45
                    val rymVal = parts[0]
                    val ryVal = parts[1]
                    conversionReminders.add("Java版y_rotation=" + rotationValue + "参数已转换为基岩版rym=" + rymVal + ",ry=" + ryVal)
                    "rym=$rymVal,ry=$ryVal"
                } else if (parts[0].isNotEmpty()) {
                    // 只有下限：-45.. -> rym=-45
                    val rymVal = parts[0]
                    conversionReminders.add("Java版y_rotation=" + rotationValue + "参数已转换为基岩版rym=" + rymVal)
                    "rym=$rymVal"
                } else if (parts[1].isNotEmpty()) {
                    // 只有上限：..45 -> ry=45
                    val ryVal = parts[1]
                    conversionReminders.add("Java版y_rotation=" + rotationValue + "参数已转换为基岩版ry=" + ryVal)
                    "ry=$ryVal"
                } else {
                    // 无效格式
                    match.value
                }
            } else {
                // 单个值：90 -> rym=90,ry=90（精确匹配）
                conversionReminders.add("Java版y_rotation=" + rotationValue + "参数已转换为基岩版rym=" + rotationValue + ",ry=" + rotationValue)
                "rym=$rotationValue,ry=$rotationValue"
            }
        }
        
        // 清理多余的逗号和空括号
        result = result.replace(",,", ",")
        result = result.replace(",\\]".toRegex(), "]")
        
        return result
    }
    
    /**
     * 将Java版的level参数转换为基岩版的l/lm参数
     */
    private fun convertLevelParameters(paramsPart: String, conversionReminders: MutableList<String>): String {
        var result = paramsPart
        
        // 处理level参数
        val levelPattern = "level=([^,\\]]+)".toRegex()
        
        result = result.replace(levelPattern) { match ->
            val levelValue = match.groupValues[1]
            
            // 检查是否为范围格式
            if (".." in levelValue) {
                val parts = levelValue.split("..")
                if (parts[0].isNotEmpty() && parts[1].isNotEmpty()) {
                    // 有上下限：5..10 -> lm=5,l=10
                    val lmVal = parts[0]
                    val lVal = parts[1]
                    conversionReminders.add("Java版level=" + levelValue + "参数已转换为基岩版lm=" + lmVal + ",l=" + lVal)
                    "lm=$lmVal,l=$lVal"
                } else if (parts[0].isNotEmpty()) {
                    // 只有下限：5.. -> lm=5
                    val lmVal = parts[0]
                    conversionReminders.add("Java版level=" + levelValue + "参数已转换为基岩版lm=" + lmVal)
                    "lm=$lmVal"
                } else if (parts[1].isNotEmpty()) {
                    // 只有上限：..10 -> l=10
                    val lVal = parts[1]
                    conversionReminders.add("Java版level=" + levelValue + "参数已转换为基岩版l=" + lVal)
                    "l=$lVal"
                } else {
                    // 无效格式
                    match.value
                }
            } else {
                // 单个值：10 -> lm=10,l=10
                conversionReminders.add("Java版level=" + levelValue + "参数已转换为基岩版lm=" + levelValue + ",l=" + levelValue)
                "lm=$levelValue,l=$levelValue"
            }
        }
        
        // 清理多余的逗号和空括号
        result = result.replace(",,", ",")
        result = result.replace(",\\]".toRegex(), "]")
        
        return result
    }
    
    /**
     * 将Java版的gamemode参数转换为基岩版的m参数
     */
    private fun convertGamemodeToM(paramsPart: String, conversionReminders: MutableList<String>): String {
        var result = paramsPart
        
        // Java版游戏模式到基岩版的映射
        val javaToBedrockGamemode = mapOf(
            "survival" to "survival",
            "creative" to "creative",
            "adventure" to "adventure",
            "spectator" to "survival"  // 基岩版没有旁观模式，转换为生存模式
        )
        
        // 从gamemode到m的转换（Java版到基岩版）
        val gamemodePattern = "gamemode=(!?)([^,\\]]+)".toRegex()
        
        result = result.replace(gamemodePattern) { match ->
            val negation = match.groupValues[1]  // ! 或空
            val gamemodeValue = match.groupValues[2].trim()
            
            if (gamemodeValue == "spectator") {
                if (negation.isNotEmpty()) {
                    conversionReminders.add("Java版反选旁观模式(gamemode=!" + gamemodeValue + ")在基岩版中不支持，已转换为反选生存模式")
                } else {
                    conversionReminders.add("Java版旁观模式(gamemode=" + gamemodeValue + ")在基岩版中不支持，已转换为生存模式")
                }
                "m=${negation}survival"
            } else if (gamemodeValue in javaToBedrockGamemode) {
                conversionReminders.add("Java版gamemode=" + gamemodeValue + "参数已转换为基岩版m=" + javaToBedrockGamemode[gamemodeValue])
                "m=${negation}${javaToBedrockGamemode[gamemodeValue]}"
            } else {
                // 保持原值
                match.value
            }
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
            strings.add(matcher.group(0))
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
    private fun convertBedrockParametersToJava(selector: String, reminders: MutableList<String>): String {
        var result = selector
        
        if ('[' !in result || ']' !in result) {
            return result
        }
        
        val selectorVar = result.split('[')[0]
        var paramsPart = result.substringAfter('[').substringBefore(']')
        
        // 转换r/rm参数到distance参数
        paramsPart = convertR_RmToDistance(paramsPart, reminders)
        
        // 转换rx/rxm参数到x_rotation参数
        paramsPart = convertRx_RxmToXRotation(paramsPart, reminders)
        
        // 转换ry/rym参数到y_rotation参数
        paramsPart = convertRy_RymToYRotation(paramsPart, reminders)
        
        // 转换l/lm参数到level参数
        paramsPart = convertL_LmToLevel(paramsPart, reminders)
        
        // 转换m参数到gamemode参数
        paramsPart = convertMToGamemode(paramsPart, reminders)
        
        // 转换c参数到limit/sort参数
        paramsPart = convertCToLimitSort(paramsPart, reminders)
        
        return selectorVar + "[" + paramsPart + "]"
    }
    
    /**
     * 将Java版参数转换为基岩版参数
     */
    private fun convertJavaParametersToBedrock(selector: String, reminders: MutableList<String>): String {
        var result = selector
        
        if ('[' !in result || ']' !in result) {
            return result
        }
        
        val selectorVar = result.split('[')[0]
        var paramsPart = result.substringAfter('[').substringBefore(']')
        
        // 转换distance参数到r/rm参数
        paramsPart = convertDistanceToR_Rm(paramsPart, reminders)
        
        // 转换x_rotation参数到rx/rxm参数
        paramsPart = convertXRotationToRx_Rxm(paramsPart, reminders)
        
        // 转换y_rotation参数到ry/rym参数
        paramsPart = convertYRotationToRy_Rym(paramsPart, reminders)
        
        // 转换level参数到l/lm参数
        paramsPart = convertLevelToL_Lm(paramsPart, reminders)
        
        // 转换gamemode参数到m参数
        paramsPart = convertGamemodeToM(paramsPart, reminders)
        
        // 转换limit/sort参数到c参数
        paramsPart = convertLimitSortToC(paramsPart, selectorVar, reminders)
        
        return selectorVar + "[" + paramsPart + "]"
    }
    
    // 以下是各种参数转换的具体实现
    
    private fun convertR_RmToDistance(paramsPart: String, reminders: MutableList<String>): String {
        var result = paramsPart
        val rPattern = "\\br=([^,\\]]+)".toRegex()
        val rmPattern = "\\brm=([^,\\]]+)".toRegex()
        
        val rMatch = rPattern.find(result)
        val rmMatch = rmPattern.find(result)
        
        if (rMatch != null || rmMatch != null) {
            val rValue = rMatch?.groupValues?.get(1)
            val rmValue = rmMatch?.groupValues?.get(1)
            
            val distanceValue = when {
                rmValue != null && rValue != null -> rmValue + ".." + rValue
                            rmValue != null -> rmValue + ".."                rValue != null -> "..$rValue"
                else -> ""
            }
            
            if (distanceValue.isNotEmpty()) {
                when {
                    rmValue != null && rValue != null -> 
                        reminders.add("基岩版rm=" + rmValue + ",r=" + rValue + "参数已转换为Java版distance=" + distanceValue)
                    rmValue != null -> 
                        reminders.add("基岩版rm=" + rmValue + "参数已转换为Java版distance=" + distanceValue)
                    rValue != null -> 
                        reminders.add("基岩版r=" + rValue + "参数已转换为Java版distance=" + distanceValue)
                }
                
                // 移除原有的r和rm参数
                result = result.replace(rPattern, "")
                result = result.replace(rmPattern, "")
                
                // 添加distance参数
                result = addParameterToResult(result, "distance=$distanceValue")
            }
        }
        
        return result
    }
    
    private fun convertDistanceToR_Rm(paramsPart: String, reminders: MutableList<String>): String {
        var result = paramsPart
        val distancePattern = "\\bdistance=([^,\\]]+)".toRegex()
        
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
                    distanceValue.substring(2) to null
                }
                distanceValue.endsWith("..") -> {
                    null to distanceValue.substring(0, distanceValue.length - 2)
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
                reminders.add("Java版distance=" + distanceValue + "参数已转换为基岩版" + reminderParts.joinToString(","))
            }
            
            // 移除distance参数
            result = result.replace(distancePattern, "")
            
            // 添加r和rm参数
            rmValue?.let { result = addParameterToResult(result, "rm=$it") }
            rValue?.let { result = addParameterToResult(result, "r=$it") }
        }
        
        return result
    }
    
    private fun convertRx_RxmToXRotation(paramsPart: String, reminders: MutableList<String>): String {
        return convertRotationParameter(
            paramsPart = paramsPart,
            paramName = "x_rotation",
            minParam = "rxm",
            maxParam = "rx",
            reminders = reminders
        )
    }
    
    private fun convertXRotationToRx_Rxm(paramsPart: String, reminders: MutableList<String>): String {
        return convertRotationParameter(
            paramsPart = paramsPart,
            paramName = "rx",
            minParam = "x_rotation",
            maxParam = "x_rotation",
            reminders = reminders,
            toJava = false
        )
    }
    
    private fun convertRy_RymToYRotation(paramsPart: String, reminders: MutableList<String>): String {
        return convertRotationParameter(
            paramsPart = paramsPart,
            paramName = "y_rotation",
            minParam = "rym",
            maxParam = "ry",
            reminders = reminders
        )
    }
    
    private fun convertYRotationToRy_Rym(paramsPart: String, reminders: MutableList<String>): String {
        return convertRotationParameter(
            paramsPart = paramsPart,
            paramName = "ry",
            minParam = "y_rotation",
            maxParam = "y_rotation",
            reminders = reminders,
            toJava = false
        )
    }
    
    private fun convertRotationParameter(
        paramsPart: String,
        paramName: String,
        minParam: String,
        maxParam: String,
        reminders: MutableList<String>,
        toJava: Boolean = true
    ): String {
        var result = paramsPart
        
        val minPattern = "\\b$minParam=([^,\\]]+)".toRegex()
        val maxPattern = "\\b$maxParam=([^,\\]]+)".toRegex()
        
        val minMatch = minPattern.find(result)
        val maxMatch = maxPattern.find(result)
        
        if (minMatch != null || maxMatch != null) {
            val minValue = minMatch?.groupValues?.get(1)
            val maxValue = maxMatch?.groupValues?.get(1)
            
            val rotationValue = when {
                minValue != null && maxValue != null -> minValue + ".." + maxValue
                            minValue != null -> minValue + ".."                maxValue != null -> "..$maxValue"
                else -> ""
            }
            
            if (rotationValue.isNotEmpty()) {
                val fromParam = if (toJava) minParam + "/" + maxParam else paramName
                        val toParam = if (toJava) paramName else minParam + "/" + maxParam                
                when {
                    minValue != null && maxValue != null -> 
                        reminders.add("基岩版" + minParam + "=" + minValue + "," + maxParam + "=" + maxValue + "参数已转换为Java版" + paramName + "=" + rotationValue)
                    minValue != null -> 
                        reminders.add("基岩版" + minParam + "=" + minValue + "参数已转换为Java版" + paramName + "=" + rotationValue)
                    maxValue != null -> 
                        reminders.add("基岩版" + maxParam + "=" + maxValue + "参数已转换为Java版" + paramName + "=" + rotationValue)
                }
                
                // 移除原有参数
                result = result.replace(minPattern, "")
                result = result.replace(maxPattern, "")
                
                // 添加新参数
                result = addParameterToResult(result, paramName + "=" + rotationValue)
            }
        }
        
        return result
    }
    
    private fun convertL_LmToLevel(paramsPart: String, reminders: MutableList<String>): String {
        var result = paramsPart
        val lPattern = "\\bl=([^,\\]]+)".toRegex()
        val lmPattern = "\\blm=([^,\\]]+)".toRegex()
        
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
                        reminders.add("基岩版lm=" + lmValue + ",l=" + lValue + "参数已转换为Java版level=" + levelValue)
                    lmValue != null -> 
                        reminders.add("基岩版lm=" + lmValue + "参数已转换为Java版level=" + levelValue)
                    lValue != null -> 
                        reminders.add("基岩版l=" + lValue + "参数已转换为Java版level=" + levelValue)
                }
                
                // 移除原有的l和lm参数
                result = result.replace(lPattern, "")
                result = result.replace(lmPattern, "")
                
                // 添加level参数
                result = addParameterToResult(result, "level=$levelValue")
            }
        }
        
        return result
    }
    
    private fun convertLevelToL_Lm(paramsPart: String, reminders: MutableList<String>): String {
        var result = paramsPart
        val levelPattern = "\\blevel=([^,\\]]+)".toRegex()
        
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
                    levelValue.substring(2) to null
                }
                levelValue.endsWith("..") -> {
                    null to levelValue.substring(0, levelValue.length - 2)
                }
                else -> {
                    levelValue to levelValue
                }
            }
            
            reminders.add("Java版level=" + levelValue + "参数已转换为基岩版" + listOfNotNull(lmValue?.let { "lm=" + it }, lValue?.let { "l=" + it }).joinToString(","))
            
            // 移除level参数
            result = result.replace(levelPattern, "")
            
            // 添加l和lm参数
            lmValue?.let { result = addParameterToResult(result, "lm=$it") }
            lValue?.let { result = addParameterToResult(result, "l=$it") }
        }
        
        return result
    }
    
    private fun convertMToGamemode(paramsPart: String, reminders: MutableList<String>): String {
        var result = paramsPart
        
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
        val mPattern = "\\bm=(!?)([^,\\]]+)".toRegex()
        
        result = result.replace(mPattern) { match ->
            val negation = match.groupValues[1]  // ! 或空
            val mValue = match.groupValues[2].trim()
            
            // 如果是默认模式，需要提醒用户并转换为生存模式
            if (mValue == "default" || mValue == "d" || mValue == "5") {
                if (negation.isNotEmpty()) {
                    reminders.add("基岩版反选默认模式(m=!" + mValue + ")在Java版中不支持，已转换为反选生存模式")
                } else {
                    reminders.add("基岩版默认模式(m=" + mValue + ")在Java版中不支持，已转换为生存模式")
                }
                "gamemode=${negation}survival"
            } else if (mValue in bedrockToJavaGamemode) {
                "gamemode=${negation}${bedrockToJavaGamemode[mValue]}"
            } else {
                // 保持原值
                match.value
            }
        }
        
        return result
    }
    
    
    
    private fun convertCToLimitSort(paramsPart: String, reminders: MutableList<String>): String {
        var result = paramsPart
        val cPattern = "\\bc=([+-]?\\d+)".toRegex()
        
        val match = cPattern.find(result)
        if (match != null) {
            val cValue = match.groupValues[1]
            
            if (cValue.startsWith("-")) {
                val absCVal = cValue.substring(1)
                reminders.add("基岩版c=" + cValue + "参数已转换为Java版limit=" + absCVal + ",sort=furthest")
                
                // 移除c参数
                result = result.replace(cPattern, "")
                
                // 添加limit和sort参数
                result = addParameterToResult(result, "limit=$absCVal")
                if (!result.contains("sort=")) {
                    result = addParameterToResult(result, "sort=furthest")
                }
            } else {
                reminders.add("基岩版c=" + cValue + "参数已转换为Java版limit=" + cValue)
                result = result.replace(cPattern, "limit=$cValue")
            }
        }
        
        return result
    }
    
    private fun convertLimitSortToC(paramsPart: String, selectorVar: String, reminders: MutableList<String>): String {
        var result = paramsPart
        
        // 处理sort参数
        val sortPattern = "\\bsort=([^,\\]]+)".toRegex()
        val sortMatch = sortPattern.find(result)
        
        if (sortMatch != null) {
            val sortValue = sortMatch.groupValues[1]
            
            // 查找limit参数
            val limitPattern = "\\blimit=([+-]?\\d+)".toRegex()
            val limitMatch = limitPattern.find(result)
            val limitValue = limitMatch?.groupValues?.get(1)
            
            when (sortValue) {
                "nearest" -> {
                    val cValue = limitValue ?: "9999"
                    result = result.replace(sortPattern, "")
                    result = result.replace(limitPattern, "")
                    result = addParameterToResult(result, "c=$cValue")
                    reminders.add("Java版sort=nearest已转换为基岩版c=" + cValue)
                }
                "furthest" -> {
                    val cValue = if (limitValue != null) "-$limitValue" else "-9999"
                    result = result.replace(sortPattern, "")
                    result = result.replace(limitPattern, "")
                    result = addParameterToResult(result, "c=$cValue")
                    reminders.add("Java版sort=furthest已转换为基岩版c=" + cValue)
                }
                "arbitrary" -> {
                    result = result.replace(sortPattern, "")
                    reminders.add("Java版sort=arbitrary在基岩版中不支持，已移除")
                }
                "random" -> {
                    val cValue = limitValue ?: "9999"
                    if (selectorVar == "@a") {
                        reminders.add("Java版@a[sort=random]已转换为基岩版@r")
                        result = result.replace("@a", "@r")
                    } else {
                        reminders.add("Java版sort=random已转换为基岩版c=" + cValue)
                    }
                    result = result.replace(sortPattern, "")
                    result = result.replace(limitPattern, "")
                    result = addParameterToResult(result, "c=$cValue")
                }
                else -> {
                    result = result.replace(sortPattern, "")
                    reminders.add("Java版sort=" + sortValue + "在基岩版中不支持，已移除")
                }
            }
        } else {
            // 没有sort参数，只转换limit
            val limitPattern = "\\blimit=([+-]?\\d+)".toRegex()
            val limitMatch = limitPattern.find(result)
            if (limitMatch != null) {
                val limitValue = limitMatch.groupValues[1]
                reminders.add("Java版limit=" + limitValue + "参数已转换为基岩版c=" + limitValue)
                result = result.replace(limitPattern, "c=$limitValue")
            }
        }
        
        return result
    }
    
    private fun addParameterToResult(paramsPart: String, newParam: String): String {
        val result = StringBuilder()
        
        if (paramsPart.isEmpty()) {
            return newParam
        }
        
        // 清理参数
        var cleanParams = paramsPart
        cleanParams = cleanParams.replace(",,", ",")
        cleanParams = cleanParams.replace("\\[,", "[".toRegex())
        cleanParams = cleanParams.replace(",\\]".toRegex(), "]")
        cleanParams = cleanParams.replace("\\[\\]".toRegex(), "")
        
        if (cleanParams.isEmpty()) {
            return newParam
        }
        
        if (cleanParams.endsWith("[")) {
            result.append(cleanParams.dropLast(1))
            result.append(newParam)
            result.append("]")
        } else if (cleanParams.endsWith("]")) {
            result.append(cleanParams.dropLast(1))
            result.append(",$newParam]")
        } else {
            result.append(cleanParams)
            result.append(",$newParam]")
        }
        
        return result.toString()
    }
    
    // hasitem和nbt转换的完善实现，与Python版本一致
    private fun convertHasitemToNbt(paramsPart: String): Pair<String, List<String>> {
        val reminders = mutableListOf<String>()
        var result = paramsPart
        
        // 处理hasitem的复杂格式：hasitem=[{...},{...}] 或 hasitem={...}
        // 先处理复杂格式 [{}]
        val complexPattern = "hasitem=\\[([^\\[\\]]*)\\]".toRegex()
        result = result.replace(complexPattern) { match ->
            val fullMatch = match.value  // 完整的匹配，如 hasitem=[{...}]
            val content = match.groupValues[1]
            val nbtResult = parseHasitemComplex(content)
            
            if (nbtResult.isNotEmpty()) {
                reminders.add("hasitem参数已转换为nbt格式，可能无法完全保留原意")
                reminders.add("注意：Java版NBT不需要Count值，hasitem的quantity参数未转换为NBT的Count字段")
                nbtResult
            } else {
                // 转换失败，移除参数并添加提醒
                reminders.add("hasitem参数转换失败，已移除")
                ""
            }
        }
        
        // 再处理简单格式 {...}
        val simplePattern = "hasitem=\\{([^}]*)\\}".toRegex()
        result = result.replace(simplePattern) { match ->
            val fullMatch = match.value  // 完整的匹配，如 hasitem={...}
            val content = match.groupValues[1]
            val nbtResult = parseHasitemSimple(content)
            
            if (nbtResult.isNotEmpty()) {
                reminders.add("hasitem参数已转换为nbt格式，可能无法完全保留原意")
                reminders.add("注意：Java版NBT不需要Count值，hasitem的quantity参数未转换为NBT的Count字段")
                nbtResult
            } else {
                // 转换失败，移除参数并添加提醒
                reminders.add("hasitem参数转换失败，已移除")
                ""
            }
        }
        
        // 清理多余的逗号和空括号
        result = result.replace(",,", ",")
        result = result.replace(",\\]".toRegex(), "]")
        result = result.replace("\\[".toRegex(), "[")
        
        return result to reminders
    }
    
    /**
     * 解析简单的hasitem参数并转换为nbt参数
     */
    private fun parseHasitemSimple(hasitemContent: String): String {
        // 解析hasitem参数
        val params = mutableMapOf<String, String>()
        val parts = hasitemContent.split(",(?![^{}]*\\})".toRegex())
        
        for (part in parts) {
            if ("=" in part) {
                val (key, value) = part.split("=", limit = 2)
                params[key.trim()] = value.trim()
            }
        }
        
        // 提取物品信息
        val itemName = params["item"]?.trim('"') ?: return ""
        val quantity = params["quantity"] ?: "1.."
        val location = params["location"]
        val slot = params["slot"]
        
        // 解析数量范围
        var countValue = 1
        val hasQuantity = "quantity" in params
        
        if (quantity.isNotEmpty()) {
            if (".." in quantity) {
                // 根据要求：hasitem如果有数量范围则取中间值(整数)
                val rangeParts = quantity.split("..")
                try {
                    if (rangeParts[0].isNotEmpty() && rangeParts[1].isNotEmpty()) {
                        // 两个数字都存在：取中间值
                        val start = rangeParts[0].toInt()
                        val end = rangeParts[1].toInt()
                        countValue = kotlin.math.round((start + end).toDouble() / 2.0).toInt()
                    } else if (rangeParts[0].isNotEmpty()) {
                        // 只有下限：使用下限值
                        countValue = rangeParts[0].toInt()
                    } else if (rangeParts[1].isNotEmpty()) {
                        // 只有上限：使用上限值
                        countValue = rangeParts[1].toInt()
                    }
                } catch (e: NumberFormatException) {
                    countValue = 1
                }
            } else {
                try {
                    countValue = quantity.toInt()
                } catch (e: NumberFormatException) {
                    countValue = 1
                }
            }
        }
        
        // 构建nbt参数
        if (itemName.isNotEmpty()) {
            // Java版NBT不需要Count值，有了反而会让检测失效
            // 所以不转换quantity参数到Count字段，保留原始的hasitem格式
            // 添加minecraft:前缀（如果需要）
            val fullItemName = if (!itemName.startsWith("minecraft:")) {
                "minecraft:$itemName"
            } else {
                itemName
            }
            
            // 构建NBT项，不包含Count字段
            val nbtItem = if (location != null && slot != null) {
                // 有具体位置信息，尝试映射到NBT的Slot字段
                val slotNum = when (location) {
                    "slot.weapon.mainhand" -> 0
                    "slot.weapon.offhand" -> 1
                    "slot.hotbar" -> {
                        // 解析slot范围，如"0..2"，取第一个值
                        if (".." in slot) slot.split("..")[0].toInt() else slot.toInt()
                    }
                    "slot.inventory" -> {
                        // 解析slot范围，如"9..35"，取第一个值
                        if (".." in slot) slot.split("..")[0].toInt() else slot.toInt()
                    }
                    else -> null
                }
                
                if (slotNum != null) {
                    "{id:\"" + fullItemName + "\",Slot:" + slotNum + "b}"
                } else {
                    // 无法确定槽位，使用通用格式
                    "{id:\"" + fullItemName + "\"}"
                }
            } else {
                // 没有具体位置信息，使用通用格式
                "{id:\"" + fullItemName + "\"}"
            }
            
            // 构建完整的NBT内容
            return "nbt={Inventory:[$nbtItem]}"
        }
        
        return ""
    }
    
    /**
     * 解析复杂的hasitem参数并转换为nbt参数
     */
    private fun parseHasitemComplex(hasitemContent: String): String {
        // 解析数组中的每个对象
        val objects = mutableListOf<String>()
        var braceCount = 0
        var currentObj = ""
        
        for (char in hasitemContent + ",") {
            when (char) {
                '{' -> {
                    braceCount++
                    if (braceCount == 1) {
                        currentObj = "{"
                        continue
                    }
                }
                '}' -> {
                    braceCount--
                    currentObj += char
                    if (braceCount == 0 && currentObj.isNotEmpty()) {
                        objects.add(currentObj.substring(1, currentObj.length - 1)) // 移除首尾的{}
                        continue
                    }
                }
            }
            if (braceCount > 0) {
                currentObj += char
            }
        }
        
        // 构建nbt内容
        val nbtItems = mutableListOf<String>()
        
        for (obj in objects) {
            val params = mutableMapOf<String, String>()
            val parts = obj.split(",(?![^{}]*\\})".toRegex())
            
            for (part in parts) {
                if ("=" in part) {
                    val (key, value) = part.split("=", limit = 2)
                    params[key.trim()] = value.trim()
                }
            }
            
            // 提取物品信息
            val itemName = params["item"]?.trim('"') ?: continue
            val quantity = params["quantity"] ?: "1.."
            val location = params["location"]
            val slot = params["slot"]
            
            // 解析数量范围 - 根据需求取中间值（整数）
            var countValue = 1
            val hasQuantity = "quantity" in params
            
            if (quantity.isNotEmpty()) {
                if (".." in quantity) {
                    val rangeParts = quantity.split("..")
                    try {
                        if (rangeParts[0].isNotEmpty() && rangeParts[1].isNotEmpty()) {
                            // 两个数字都存在：取中间值
                            val start = rangeParts[0].toInt()
                            val end = rangeParts[1].toInt()
                            countValue = kotlin.math.round((start + end).toDouble() / 2.0).toInt()
                        } else if (rangeParts[0].isNotEmpty()) {
                            // 只有下限：使用下限值
                            countValue = rangeParts[0].toInt()
                        } else if (rangeParts[1].isNotEmpty()) {
                            // 只有上限：使用上限值
                            countValue = rangeParts[1].toInt()
                        }
                    } catch (e: NumberFormatException) {
                        countValue = 1
                    }
                } else if (quantity.startsWith("!")) {
                    // 处理反选情况
                    countValue = 1
                } else {
                    try {
                        countValue = quantity.toInt()
                    } catch (e: NumberFormatException) {
                        countValue = 1
                    }
                }
            }
            
            // 添加minecraft:前缀（如果需要）
            val fullItemName = if (!itemName.startsWith("minecraft:")) {
                "minecraft:$itemName"
            } else {
                itemName
            }
            
            // 处理位置信息
            val slotNum = if (location != null && slot != null) {
                when (location) {
                    "slot.weapon.mainhand" -> 0
                    "slot.weapon.offhand" -> 1
                    "slot.hotbar" -> {
                        // 解析slot范围，如"0..2"，取第一个值
                        if (".." in slot) slot.split("..")[0].toInt() else slot.toInt()
                    }
                    "slot.inventory" -> {
                        // 解析slot范围，如"9..35"，取第一个值
                        if (".." in slot) slot.split("..")[0].toInt() else slot.toInt()
                    }
                    else -> null
                }
            } else {
                null
            }
            
            // 构建带Slot信息的NBT项，不包含Count字段
            val nbtItem = if (slotNum != null) {
                "{id:\"" + fullItemName + "\",Slot:" + slotNum + "b}"
            } else {
                // 没有具体位置信息，使用通用格式
                "{id:\"" + fullItemName + "\"}"
            }
            
            nbtItems.add(nbtItem)
        }
        
        if (nbtItems.isNotEmpty()) {
            val nbtContent = "{Inventory:[${nbtItems.joinToString(",")}]}"
            return nbtContent
        }
        
        return ""
    }
    
    private fun convertNbtToHasitem(paramsPart: String): Pair<String, List<String>> {
        val reminders = mutableListOf<String>()
        var result = paramsPart
        
        // 使用非递归正则表达式匹配nbt参数，处理嵌套的大括号结构
        val nbtPattern = "nbt=(\\{[^{}]*(?:\\{[^{}]*\\}[^{}]*)*\\})".toRegex()
        
        result = result.replace(nbtPattern) { match ->
            val fullMatch = match.value  // 完整匹配，如 nbt={...}
            val nbtContent = match.groupValues[1]  // 大括号内的内容
            
            // 先检查是否包含物品相关信息（SelectedItem、Item、Inventory）
            if (!setOf("SelectedItem", "Item", "Inventory").any { it in nbtContent }) {
                // 如果不包含物品信息，直接返回原匹配内容
                fullMatch
            } else {
                // 尝试解析nbt内容，看是否可以转换为hasitem
                val hasitemResult = tryConvertNbtContentToHasitem(nbtContent)
                
                if (hasitemResult.isNotEmpty()) {
                    // 如果可以转换，返回hasitem参数
                    reminders.add("nbt参数已转换为hasitem格式，可能无法完全保留原意")
                    "hasitem=$hasitemResult"
                } else {
                    // 如果不能转换，返回原始nbt参数（保持完整格式）
                    fullMatch
                }
            }
        }
        
        return result to reminders
    }
    
    /**
     * 尝试将nbt内容转换为hasitem内容
     */
    private fun tryConvertNbtContentToHasitem(nbtContent: String): String {
        // 模式1: SelectedItem:{...id:"xxx"..., ...,Slot:0b} - 用于指定槽位
        val selectedItemPattern = "SelectedItem\\s*:\\s*\\{([^}]*(?:\\{[^}]*\\}[^}]*)*)\\}".toRegex()
        selectedItemPattern.find(nbtContent)?.let { match ->
            val itemData = match.groupValues[1]
            // 提取id
            val idPattern = "id\\s*:\\s*[\"']([^\"']+)[\"']".toRegex()
            idPattern.find(itemData)?.let { idMatch ->
                val itemId = idMatch.groupValues[1]
                // 移除minecraft:前缀（如果存在）
                val cleanItemId = if (itemId.startsWith("minecraft:")) {
                    itemId.substring(10)  // 移除 'minecraft:' 前缀
                } else {
                    itemId
                }
                
                // 提取Count（数量）
                val countPattern = "Count\\s*[\"']?\\s*:\\s*(\\d+)[bBfFdD]?".toRegex()
                val countMatch = countPattern.find(itemData)
                
                // 提取Slot信息
                val slotPattern = "Slot\\s*[\"']?\\s*:\\s*(\\d+)".toRegex()
                val slotMatch = slotPattern.find(itemData)
                
                if (slotMatch != null) {
                    val slotValue = slotMatch.groupValues[1]
                    val slotStr = when (slotValue) {
                        "0" -> ",location=slot.weapon.mainhand"  // 主手
                        "1" -> ",location=slot.weapon.offhand"   // 副手
                        else -> ",location=slot.inventory,slot=$slotValue..$slotValue"
                    }
                    
                    val countStr = if (countMatch != null) {
                        ",quantity=${countMatch.groupValues[1]}.."
                    } else ""
                    
                    return "{item=$cleanItemId$countStr$slotStr}"
                } else {
                    // 如果没有显式槽位信息，SelectedItem默认是主手物品
                    val slotStr = ",location=slot.weapon.mainhand"
                    val countStr = if (countMatch != null) {
                        ",quantity=${countMatch.groupValues[1]}.."
                    } else ""
                    
                    return "{item=$cleanItemId$countStr$slotStr}"
                }
            }
        }
        
        // 模式2: Inventory:[{...id:"xxx"..., ...,Slot:0b, ...}, ...] - 指定具体槽位的物品
        val inventoryPattern = "Inventory\\s*:\\s*\\[([^\\]]*)\\]".toRegex()
        inventoryPattern.find(nbtContent)?.let { match ->
            val inventoryContent = match.groupValues[1]
            // 解析所有物品
            val items = mutableListOf<String>()
            
            // 使用正则表达式匹配所有物品对象
            val itemObjectPattern = "\\{([^{}]*(?:\\{[^{}]*\\}[^{}]*)*)\\}".toRegex()
            val itemObjects = itemObjectPattern.findAll(inventoryContent).map { it.groupValues[1] }.toList()
            
            for (itemObj in itemObjects) {
                // 提取id
                val idPattern = "id\\s*:\\s*[\"']([^\"']+)[\"']".toRegex()
                val idMatch = idPattern.find(itemObj)
                if (idMatch == null) continue  // 跳过没有id的物品
                
                val itemId = idMatch.groupValues[1]
                // 移除minecraft:前缀（如果存在）
                val cleanItemId = if (itemId.startsWith("minecraft:")) {
                    itemId.substring(10)  // 移除 'minecraft:' 前缀
                } else {
                    itemId
                }
                
                // 提取Count（数量）
                val countPattern = "Count\\s*[\"']?\\s*:\\s*(\\d+)[bBfFdD]?".toRegex()
                val countMatch = countPattern.find(itemObj)
                
                // 提取Slot信息
                val slotPattern = "Slot\\s*[\"']?\\s*:\\s*(\\d+)".toRegex()
                val slotMatch = slotPattern.find(itemObj)
                
                // 构建hasitem参数
                var itemStr = "item=$cleanItemId"
                if (countMatch != null) {
                    itemStr += ",quantity=${countMatch.groupValues[1]}.."
                }
                
                if (slotMatch != null) {
                    val slotValue = slotMatch.groupValues[1]
                    val locationStr = when (slotValue) {
                        "0" -> ",location=slot.weapon.mainhand"
                        "1" -> ",location=slot.weapon.offhand"
                        in setOf("2", "3", "4", "5", "6", "7", "8") -> ",location=slot.hotbar,slot=$slotValue..$slotValue"
                        else -> ",location=slot.inventory,slot=$slotValue..$slotValue"
                    }
                    itemStr += locationStr
                }
                
                items.add(itemStr)
            }
            
            if (items.isNotEmpty()) {
                // 如果有多个物品，构建hasitem数组格式
                return if (items.size == 1) {
                    "{${items[0]}}"
                } else {
                    // 多个物品使用数组格式
                    val itemsStr = items.joinToString(",") { "{$it}" }
                    "[$itemsStr]"
                }
            }
        }
        
        // 模式3: Item:{...id:"xxx"..., ...} (对于物品实体)
        val itemPattern = "Item\\s*:\\s*\\{([^}]*(?:\\{[^}]*\\}[^}]*)*)\\}".toRegex()
        itemPattern.find(nbtContent)?.let { match ->
            val itemData = match.groupValues[1]
            // 提取id
            val idPattern = "id\\s*:\\s*[\"']([^\"']+)[\"']".toRegex()
            idPattern.find(itemData)?.let { idMatch ->
                val itemId = idMatch.groupValues[1]
                // 移除minecraft:前缀（如果存在）
                val cleanItemId = if (itemId.startsWith("minecraft:")) {
                    itemId.substring(10)  // 移除 'minecraft:' 前缀
                } else {
                    itemId
                }
                
                // 提取Count（数量）
                val countPattern = "Count\\s*[\"']?\\s*:\\s*(\\d+)[bBfFdD]?".toRegex()
                val countMatch = countPattern.find(itemData)
                
                return if (countMatch != null) {
                    "{item=$cleanItemId,quantity=${countMatch.groupValues[1]}..}"
                } else {
                    "{item=$cleanItemId}"
                }
            }
        }
        
        // 模式4: 直接的物品ID匹配，如 {id:"minecraft:diamond"}
        val directIdPattern = "id\\s*:\\s*[\"']([^\"']+)[\"']".toRegex()
        directIdPattern.find(nbtContent)?.let { match ->
            val itemId = match.groupValues[1]
            // 移除minecraft:前缀（如果存在）
            val cleanItemId = if (itemId.startsWith("minecraft:")) {
                itemId.substring(10)  // 移除 'minecraft:' 前缀
            } else {
                itemId
            }
            return "hasitem={item=$cleanItemId}"
        }
        
        // 模式5: Tags匹配，如 {Tags:["a","b"]}
        val tagsPattern = "Tags\\s*:\\s*\\[([^\\]]*)\\]".toRegex()
        tagsPattern.find(nbtContent)?.let { match ->
            val tagsContent = match.groupValues[1]
            // 提取所有标签
            val tagPattern = "[\"']([^\"']+)[\"']".toRegex()
            val tags = tagPattern.findAll(tagsContent).map { it.groupValues[1] }.toList()
            
            if (tags.isNotEmpty()) {
                // 将每个标签转换为单独的tag参数
                // 返回第一个标签作为tag参数，因为hasitem不支持多个标签
                return "tag=${tags[0]}"
            }
        }
        
        // 模式6: 实体类型匹配，如 {Type:"minecraft:zombie"}
        val entityTypePattern = "Type\\s*:\\s*[\"']([^\"']+)[\"']".toRegex()
        entityTypePattern.find(nbtContent)?.let { match ->
            val entityType = match.groupValues[1]
            // 移除minecraft:前缀（如果存在）
            val cleanEntityType = if (entityType.startsWith("minecraft:")) {
                entityType.substring(10)  // 移除 'minecraft:' 前缀
            } else {
                entityType
            }
            return "type=$cleanEntityType"
        }
        
        // 如果没有找到可转换的模式，返回空字符串
        return ""
    }
    
    /**
     * 处理参数中的范围数值，提取第一个数字用于不支持范围的参数
     * 与Python版本的process_range_values函数逻辑一致
     */
    private fun processRangeValues(selector: String): String {
        var result = selector
        
        // Java版的nbt参数不支持范围选择，需要提取第一个数字
        // 例如：nbt={Inventory:[{id:"minecraft:diamond",Count:3..}]}
        // 需要转换为：nbt={Inventory:[{id:"minecraft:diamond",Count:3b}]}
        
        val nbtPattern = "nbt=(\\{[^{}]*(?:\\{[^{}]*\\}[^{}]*)*\\})".toRegex()
        result = result.replace(nbtPattern) { match ->
            val nbtContent = match.groupValues[1]
            // 处理nbt中的范围数值
            processNbtContentRanges(nbtContent)
        }
        
        return result
    }
    
    /**
     * 处理NBT内容中的范围数值
     */
    private fun processNbtContentRanges(nbtContent: String): String {
        // 处理Count范围
        val countRangePattern = "Count:(\\d*\\.\\.\\d*)".toRegex()
        var result = nbtContent.replace(countRangePattern) { match ->
            val countValue = match.groupValues[1]
            // 如果是范围，提取第一个数字
            if (".." in countValue) {
                val rangeParts = countValue.split("..")
                try {
                    val firstNum = if (rangeParts[0].isNotEmpty()) rangeParts[0].toInt() else 0
                    "Count:${firstNum}b"
                } catch (e: NumberFormatException) {
                    match.value
                }
            } else {
                match.value
            }
        }
        
        // 处理其他可能的数值范围字段
        val generalRangePattern = "(\\w+):(\\d*\\.\\.\\d*)".toRegex()
        result = result.replace(generalRangePattern) { match ->
            val fieldName = match.groupValues[1]
            val fieldValue = match.groupValues[2]
            // 如果是范围，提取第一个数字
            if (".." in fieldValue) {
                val rangeParts = fieldValue.split("..")
                try {
                    val firstNum = if (rangeParts[0].isNotEmpty()) rangeParts[0].toInt() else 0
                    // 根据字段名决定后缀
                    val suffix = if (fieldName in setOf("Count", "Damage")) "b" else ""
                    fieldName + ":" + firstNum + suffix
                } catch (e: NumberFormatException) {
                    match.value
                }
            } else {
                match.value
            }
        }
        
        return result
    }
}