package com.tellraw.app.util

import com.tellraw.app.model.TellrawCommand

object TextFormatter {
    
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
    
    // 从文本中直接提取的颜色代码
    private val TEXT_COLOR_CODES = mapOf(
        // 基岩版特有颜色代码（映射到标准代码）
        "§g" to "§6",  // 基岩版minecoin_gold -> 金色
        "§h" to "§f",  // 基岩版material_quartz -> 白色 
        "§i" to "§7",  // 基岩版material_iron -> 灰色
        "§j" to "§8",  // 基岩版material_netherite -> 深灰色
        "§m" to "§4",  // 基岩版material_redstone -> 深红色 (特殊处理)
        "§n" to "§6",  // 基岩版material_copper -> 金色 (特殊处理)
        "§p" to "§6",  // 基岩版material_gold -> 金色
        "§q" to "§a",  // 基岩版material_emerald -> 绿色
        "§s" to "§b",  // 基岩版material_diamond -> 青色
        "§t" to "§1",  // 基岩版material_lapis -> 深蓝色
        "§u" to "§d",  // 基岩版material_amethyst -> 粉色
        "§v" to "§6",  // 基岩版material_resin -> 金色
        // 标准单字符颜色代码
        "§a" to "§a",  // 绿色
        "§b" to "§b",  // 青色
        "§c" to "§c",  // 红色
        "§d" to "§d",  // 粉色
        "§e" to "§e",  // 黄色
        "§f" to "§f",  // 白色
        "§0" to "§0",  // 黑色
        "§1" to "§1",  // 深蓝
        "§2" to "§2",  // 深绿
        "§3" to "§3",  // 深青
        "§4" to "§4",  // 深红
        "§5" to "§5",  // 深紫
        "§6" to "§6",  // 金色
        "§7" to "§7",  // 灰色
        "§8" to "§8",  // 深灰
        "§9" to "§9",  // 蓝色
    )
    
    // 格式代码映射
    private val FORMAT_CODES = mapOf(
        "§k" to "§k",  // random
        "§l" to "§l",  // bold
        "§m" to "§m",  // strikethrough (仅Java版)
        "§n" to "§n",  // underline (仅Java版) 
        "§o" to "§o",  // italic
        "§r" to "§r",  // reset
    )
    
    /**
     * 检测文本中是否包含§m§n代码
     */
    fun containsMNCodes(text: String): Boolean {
        return "§m" in text || "§n" in text
    }
    
    /**
     * 处理§m§n代码转换，与Python版本的handle_m_n_codes函数逻辑一致
     * @param text 输入文本
     * @param useJavaFontStyle Java版是否使用字体方式，true=字体方式，false=颜色代码方式
     * @return 处理后的文本和警告信息
     */
    fun processMNCodes(text: String, useJavaFontStyle: Boolean): Pair<String, List<String>> {
        val warnings = mutableListOf<String>()
        var processedText = text
        
        if (containsMNCodes(text)) {
            if (useJavaFontStyle) {
                // Java版使用字体方式，基岩版使用颜色代码方式
                warnings.add("Java版使用字体方式，基岩版使用颜色代码方式")
                // 不修改原始文本，保留§m§n代码，在转换时处理
            } else {
                // Java版和基岩版都使用颜色代码方式
                warnings.add("Java版和基岩版都使用颜色代码方式")
                // 将§m§n转换为对应的颜色代码
                processedText = processedText.replace("§m", "§4") // strikethrough -> dark_red
                processedText = processedText.replace("§n", "§6") // underline -> gold
            }
        }
        
        return processedText to warnings
    }
    
    /**
     * 转换文本中的颜色代码
     */
    fun convertColorCodes(text: String, targetVersion: MinecraftVersion): String {
        var result = text
        
        if (targetVersion == MinecraftVersion.JAVA) {
            // 基岩版特有颜色代码转换为Java版标准代码
            TEXT_COLOR_CODES.forEach { (bedrockCode, javaCode) ->
                result = result.replace(bedrockCode, javaCode)
            }
        } else {
            // Java版到基岩版的转换（如果需要）
            // 大部分颜色代码是通用的，除了特殊情况
        }
        
        return result
    }
    
    /**
     * 处理空格转换
     */
    fun convertSpaces(text: String, targetVersion: MinecraftVersion): String {
        // 在某些情况下，不同版本对空格的处理可能不同
        // 目前保持一致，但保留此函数以备将来扩展
        return text
    }
    
    /**
     * 处理§字符转义
     */
    fun escapeSectionSigns(text: String): String {
        return text.replace("§", "\\u00A7")
    }
    
    /**
     * 恢复§字符
     */
    fun unescapeSectionSigns(text: String): String {
        return text.replace("\\u00A7", "§")
    }
    
    /**
     * 将文本转换为Java版tellraw JSON格式，与Python版本的parse_minecraft_formatting函数逻辑一致
     */
    fun convertToJavaJson(text: String, mNHandling: String = "color"): String {
        var jsonText = text
        val components = mutableListOf<Map<String, Any>>()
        var currentText = ""
        var currentFormat = mutableMapOf<String, Any>()
        
        // 使用正则表达式解析颜色代码和文本
        // 匹配§+字符的模式，然后处理后续的文本
        val tokens = mutableListOf<Pair<String, String>>() // (type, value)
        var i = 0
        while (i < jsonText.length) {
            if (jsonText[i] == '§' && i + 1 < jsonText.length) {
                val code = jsonText.substring(i, i + 2)
                tokens.add("format_code" to code)
                i += 2
            } else {
                // 收集非§开头的文本
                val start = i
                while (i < jsonText.length && jsonText[i] != '§') {
                    i++
                }
                if (i > start) {
                    tokens.add("text" to jsonText.substring(start, i))
                }
            }
        }
        
        // 构建结果
        var result = mutableMapOf<String, Any>("text" to "")
        val extraParts = mutableListOf<Map<String, Any>>()
        
        // 按Java版逻辑处理：相同颜色相同字体形式的文本放在一起处理
        for ((tokenType, tokenValue) in tokens) {
            if (tokenType == "format_code") {
                val code = tokenValue
                // 颜色代码
                if (code[1] in "0123456789abcdefg hijpqs tuv") {
                    // 颜色代码
                    when (code) {
                        "§0" -> currentFormat["color"] = "black"
                        "§1" -> currentFormat["color"] = "dark_blue"
                        "§2" -> currentFormat["color"] = "dark_green"
                        "§3" -> currentFormat["color"] = "dark_aqua"
                        "§4" -> currentFormat["color"] = "dark_red"
                        "§5" -> currentFormat["color"] = "dark_purple"
                        "§6" -> currentFormat["color"] = "gold"
                        "§7" -> currentFormat["color"] = "gray"
                        "§8" -> currentFormat["color"] = "dark_gray"
                        "§9" -> currentFormat["color"] = "blue"
                        "§a" -> currentFormat["color"] = "green"
                        "§b" -> currentFormat["color"] = "aqua"
                        "§c" -> currentFormat["color"] = "red"
                        "§d" -> currentFormat["color"] = "light_purple"
                        "§e" -> currentFormat["color"] = "yellow"
                        "§f" -> currentFormat["color"] = "white"
                        // 基岩版颜色代码
                        "§g" -> currentFormat["color"] = "gold"  // minecoin_gold
                        "§h" -> currentFormat["color"] = "white"  // material_quartz
                        "§i" -> currentFormat["color"] = "gray"  // material_iron
                        "§j" -> currentFormat["color"] = "dark_gray"  // material_netherite
                        "§m" -> {
                            if (mNHandling == "color") {
                                currentFormat["color"] = "dark_red"  // material_redstone
                            } else {
                                // 在Java版中，§m是删除线格式
                                currentFormat["strikethrough"] = true
                            }
                        }
                        "§n" -> {
                            if (mNHandling == "color") {
                                currentFormat["color"] = "gold"  // material_copper
                            } else {
                                // 在Java版中，§n是下划线格式
                                currentFormat["underlined"] = true
                            }
                        }
                        "§p" -> currentFormat["color"] = "gold"  // material_gold
                        "§q" -> currentFormat["color"] = "green"  // material_emerald
                        "§s" -> currentFormat["color"] = "aqua"  // material_diamond
                        "§t" -> currentFormat["color"] = "dark_blue"  // material_lapis
                        "§u" -> currentFormat["color"] = "light_purple"  // material_amethyst
                        "§v" -> currentFormat["color"] = "gold"  // material_resin
                    }
                }
                // 格式代码
                else if (code[1] in "klmnor") {
                    when (code) {
                        "§k" -> currentFormat["obfuscated"] = true  // 随机字符（混淆）
                        "§l" -> currentFormat["bold"] = true  // 粗体
                        "§m" -> if (mNHandling != "color") currentFormat["strikethrough"] = true  // 删除线
                        "§n" -> if (mNHandling != "color") currentFormat["underlined"] = true  // 下划线
                        "§o" -> currentFormat["italic"] = true  // 斜体
                        "§r" -> {
                            // 重置所有格式
                            currentFormat.clear()
                        }
                    }
                }
            } else {  // token_type == 'text'
                val textContent = tokenValue
                
                // 按Java版逻辑：将相同颜色和格式的文本组合在一起
                // 检查是否可以与前一部分合并
                if (extraParts.isNotEmpty()) {
                    // 检查最后一个部分的格式是否与当前格式相同
                    val lastPart = extraParts.last()
                    
                    // 检查格式是否完全相同（包括所有格式属性）
                    val allFormatKeys = (currentFormat.keys + lastPart.keys.filter { it != "text" }).toSet()
                    val formatsMatch = allFormatKeys.all { key ->
                        if (key == "text") true else currentFormat[key] == lastPart[key]
                    }
                        
                    if (formatsMatch) {
                        // 格式相同，合并文本
                        extraParts[extraParts.size - 1] = lastPart + ("text" to (lastPart["text"] as String + textContent))
                    } else {
                        // 格式不同，添加新部分
                        val newPart = mutableMapOf<String, Any>("text" to textContent)
                        newPart.putAll(currentFormat)
                        extraParts.add(newPart)
                    }
                } else {
                    // Check if can merge with main text
                    if (result["text"] as String?.isNotEmpty() == true) {
                        // Check if main text format matches current format
                        val allFormatKeys = (currentFormat.keys + result.keys.filter { key: String -> key != "text" }).toSet()
                        val formatsMatch = allFormatKeys.all { key ->
                            if (key == "text") true else currentFormat[key] == result[key]
                        }
                            
                        if (formatsMatch) {
                            // 格式相同，合并到主文本
                            result["text"] = (result["text"] as String) + textContent
                        } else {
                            // 格式不同，添加到extra部分
                            val newPart = mutableMapOf<String, Any>("text" to textContent)
                            newPart.putAll(currentFormat)
                            extraParts.add(newPart)
                        }
                    } else {
                        // 第一部分，设置为主文本
                        result["text"] = textContent
                        result.putAll(currentFormat)
                    }
                }
            }
        }
        
        // 添加extra部分（如果有的话）
        if (extraParts.isNotEmpty()) {
            result["extra"] = extraParts
        }
        
        // 转换为JSON字符串
        return mapToJsonString(result)
    }
    
    /**
     * 将Map转换为JSON字符串
     */
    private fun mapToJsonString(map: Map<String, Any>): String {
        val jsonBuilder = StringBuilder()
        
        fun appendValue(value: Any) {
            when (value) {
                is String -> jsonBuilder.append("\"${value.replace("\"", "\\\"")}\"")
                is Boolean -> jsonBuilder.append(value)
                is Number -> jsonBuilder.append(value)
                is List<*> -> {
                    jsonBuilder.append("[")
                    value.forEachIndexed { index, item ->
                        if (index > 0) jsonBuilder.append(",")
                        appendValue(item ?: "")
                    }
                    jsonBuilder.append("]")
                }
                is Map<*, *> -> {
                    jsonBuilder.append("{")
                    value.entries.forEachIndexed { index, (key, item) ->
                        if (index > 0) jsonBuilder.append(",")
                        jsonBuilder.append("\"$key\":")
                        appendValue(item ?: "")
                    }
                    jsonBuilder.append("}")
                }
                else -> jsonBuilder.append("\"$value\"")
            }
        }
        
        jsonBuilder.append("{")
        map.entries.forEachIndexed { index, (key, value) ->
            if (index > 0) jsonBuilder.append(",")
            jsonBuilder.append("\"$key\":")
            appendValue(value)
        }
        jsonBuilder.append("}")
        
        return jsonBuilder.toString()
    }
    
    /**
     * 将文本转换为基岩版tellraw原始文本格式
     */
    fun convertToBedrockRaw(text: String): String {
        // 基岩版直接使用带格式代码的原始文本
        return text
    }
    
    /**
     * 将文本转换为基岩版tellraw JSON格式，与Python版本保持一致
     */
    fun convertToBedrockJson(text: String, useJavaFontStyle: Boolean = true): String {
        var processedText = text
        
        // 使用TEXT_COLOR_CODES处理所有颜色代码
        // 按长度降序排列，确保较长的代码先被处理
        val sortedCodes = TEXT_COLOR_CODES.entries.sortedByDescending { it.key.length }
        sortedCodes.forEach { (colorCode, replacement) ->
            processedText = processedText.replace(colorCode, replacement)
        }
        
        // 根据useJavaFontStyle参数处理§m§n代码（如果需要）
        if (useJavaFontStyle) {
            // 基岩版使用默认颜色代码方式
            // 将基岩版特有颜色代码转换为相似的Java版颜色代码
            TEXT_COLOR_CODES.forEach { (bedrockCode, replacement) ->
                if (processedText.contains(bedrockCode)) {
                    processedText = processedText.replace(bedrockCode, replacement)
                }
            }
        } else {
            // 同样使用颜色代码方式
            TEXT_COLOR_CODES.forEach { (bedrockCode, replacement) ->
                if (processedText.contains(bedrockCode)) {
                    processedText = processedText.replace(bedrockCode, replacement)
                }
            }
        }
        
        // 返回rawtext格式，与Python版本保持一致
        return "{\"rawtext\":[{\"text\":\"$processedText\"}]}"
    }
    
    /**
     * 构建单个文本组件
     */
    private fun buildTextComponent(text: String, color: String, formats: List<String>): String {
        val component = mutableMapOf<String, Any>()
        component["text"] = text
        
        if (color.isNotEmpty()) {
            component["color"] = getColorName(color)
        }
        
        formats.forEach { format ->
            when (format) {
                "§l" -> component["bold"] = true
                "§m" -> component["strikethrough"] = true
                "§n" -> component["underlined"] = true
                "§o" -> component["italic"] = true
                "§k" -> component["obfuscated"] = true
            }
        }
        
        // 转换为JSON字符串
        return component.entries.joinToString(",", "{", "}") { (key, value) ->
            "\"$key\":${if (value is String) "\"$value\"" else value}"
        }
    }
    
    /**
     * 获取颜色名称
     */
    private fun getColorName(colorCode: String): String {
        val colorNames = mapOf(
            "§0" to "black",
            "§1" to "dark_blue",
            "§2" to "dark_green",
            "§3" to "dark_aqua",
            "§4" to "dark_red",
            "§5" to "dark_purple",
            "§6" to "gold",
            "§7" to "gray",
            "§8" to "dark_gray",
            "§9" to "blue",
            "§a" to "green",
            "§b" to "aqua",
            "§c" to "red",
            "§d" to "light_purple",
            "§e" to "yellow",
            "§f" to "white"
        )
        return colorNames[colorCode] ?: "white"
    }
    
    /**
     * 生成tellraw命令
     */
    fun generateTellrawCommand(
        selector: String,
        message: String,
        useJavaFontStyle: Boolean = true
    ): TellrawCommand {
        val warnings = mutableListOf<String>()
        
        // 处理§m§n代码
        val (processedMessage, mnWarnings) = processMNCodes(message, useJavaFontStyle)
        warnings.addAll(mnWarnings)
        
        // 确定m_n_handling参数
        val mNHandling = if (useJavaFontStyle) "font" else "color"
        
        // 转换为Java版JSON格式
        val javaMessage = convertToJavaJson(processedMessage, mNHandling)
        val javaCommand = "tellraw $selector $javaMessage"
        
        // 转换为基岩版JSON格式，与Python版本保持一致
        val bedrockMessage = convertToBedrockJson(processedMessage, useJavaFontStyle)
        val bedrockCommand = "tellraw $selector $bedrockMessage"
        
        return TellrawCommand(
            javaCommand = javaCommand,
            bedrockCommand = bedrockCommand,
            warnings = warnings
        )
    }
    
    /**
     * 验证tellraw命令格式
     */
    fun validateTellrawCommand(command: String): List<String> {
        val errors = mutableListOf<String>()
        
        if (!command.startsWith("tellraw ")) {
            errors.add("命令必须以'tellraw '开头")
            return errors
        }
        
        val parts = command.split(" ", limit = 3)
        if (parts.size < 3) {
            errors.add("命令格式不正确，应为: tellraw <选择器> <消息>")
            return errors
        }
        
        val selector = parts[1]
        val message = parts[2]
        
        // 验证选择器
        if (!selector.startsWith("@")) {
            errors.add("选择器必须以@开头")
        }
        
        // 验证消息格式
        if (message.startsWith("{") && message.endsWith("}")) {
            // JSON格式，验证是否为有效的JSON
            try {
                // 简单的JSON验证
                if (!isValidJson(message)) {
                    errors.add("消息JSON格式无效")
                }
            } catch (e: Exception) {
                errors.add("消息JSON格式无效: ${e.message}")
            }
        }
        
        return errors
    }
    
    /**
     * 简单的JSON验证
     */
    private fun isValidJson(json: String): Boolean {
        var braceCount = 0
        var bracketCount = 0
        var inString = false
        var escapeNext = false
        
        for (char in json) {
            when {
                escapeNext -> {
                    escapeNext = false
                }
                char == '\\' -> {
                    escapeNext = true
                }
                char == '"' -> {
                    inString = !inString
                }
                !inString -> {
                    when (char) {
                        '{' -> braceCount++
                        '}' -> braceCount--
                        '[' -> bracketCount++
                        ']' -> bracketCount--
                    }
                }
            }
        }
        
        return braceCount == 0 && bracketCount == 0 && !inString
    }
}