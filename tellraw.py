#!/usr/bin/env python3
"""
Minecraft Tellraw指令生成器
支持Java版和基岩版的目标选择器和文本消息格式转换
"""

import json
import sys
import os
import re

# 加载提示池
PROMPT_FILE = os.path.join(os.path.dirname(__file__), 'tellraw_prompts.json')

def load_prompts():
    """加载提示池"""
    try:
        with open(PROMPT_FILE, 'r', encoding='utf-8') as f:
            return json.load(f)
    except FileNotFoundError:
        # 如果提示文件不存在，返回默认提示
        return {
            "prompts": {
                "m_n_choice": "检测到§m§n代码，选择处理方式：\n1. Java版使用字体方式，基岩版使用颜色代码方式\n2. Java版和基岩版都使用颜色代码方式",
                "selector_input": "请输入目标选择器:",
                "message_input": "请输入文本消息:",
                "selector_type": "检测到目标选择器类型: {}",
                "java_command": "Java版: {}",
                "bedrock_command": "基岩版: {}",
                "usage": "用法:\n  python3 tellraw.py \'目标选择器' \'文本消息\'  # 命令行模式\n  python3 tellraw.py  # 交互式模式",
                "selector_conversion_note": "基岩版选择器 {} 已转换为Java版 {}"
            },
            "m_n_options": {
                "1": "Java版使用字体方式，基岩版使用颜色代码方式",
                "2": "Java版和基岩版都使用颜色代码方式"
            }
        }

PROMPTS = load_prompts()

# 颜色代码映射表 - Java版到基岩版的对应关系
JAVA_COLORS = {
    'black': '§0',
    'dark_blue': '§1', 
    'dark_green': '§2',
    'dark_aqua': '§3',
    'dark_red': '§4',
    'dark_purple': '§5',
    'gold': '§6',
    'gray': '§7',
    'dark_gray': '§8',
    'blue': '§9',
    'green': '§a',
    'aqua': '§b',
    'red': '§c',
    'light_purple': '§d',
    'yellow': '§e',
    'white': '§f'
}

# 从文本中直接提取的颜色代码
TEXT_COLOR_CODES = {
    # 基岩版特有颜色代码（映射到标准代码）
    '§g': '§6',  # 基岩版minecoin_gold -> 金色
    '§h': '§f',  # 基岩版material_quartz -> 白色 
    '§i': '§7',  # 基岩版material_iron -> 灰色
    '§j': '§8',  # 基岩版material_netherite -> 深灰色
    '§m': '§4',  # 基岩版material_redstone -> 深红色 (特殊处理)
    '§n': '§6',  # 基岩版material_copper -> 金色 (特殊处理)
    '§p': '§6',  # 基岩版material_gold -> 金色
    '§q': '§a',  # 基岩版material_emerald -> 绿色
    '§s': '§b',  # 基岩版material_diamond -> 青色
    '§t': '§1',  # 基岩版material_lapis -> 深蓝色
    '§u': '§d',  # 基岩版material_amethyst -> 粉色
    '§v': '§6',  # 基岩版material_resin -> 金色
    # 标准单字符颜色代码
    '§a': '§a',  # 绿色
    '§b': '§b',  # 青色
    '§c': '§c',  # 红色
    '§d': '§d',  # 粉色
    '§e': '§e',  # 黄色
    '§f': '§f',  # 白色
    '§0': '§0',  # 黑色
    '§1': '§1',  # 深蓝
    '§2': '§2',  # 深绿
    '§3': '§3',  # 深青
    '§4': '§4',  # 深红
    '§5': '§5',  # 深紫
    '§6': '§6',  # 金色
    '§7': '§7',  # 灰色
    '§8': '§8',  # 深灰
    '§9': '§9',  # 蓝色
}

# 基岩版特有颜色代码 (为了保持向后兼容性)
BEDROCK_COLORS = {
    '§g': '§6',  # minecoin_gold -> gold
    '§h': '§f',  # material_quartz -> white 
    '§i': '§7',  # material_iron -> gray
    '§j': '§8',  # material_netherite -> dark_gray
    '§m': '§4',  # material_redstone -> dark_red (特殊处理)
    '§n': '§6',  # material_copper -> gold (特殊处理)
    '§p': '§6',  # material_gold -> gold
    '§q': '§a',  # material_emerald -> green
    '§s': '§b',  # material_diamond -> aqua
    '§t': '§1',  # material_lapis -> dark_blue
    '§u': '§d',  # material_amethyst -> light_purple
    '§v': '§6',  # material_resin -> gold
}

# 格式代码映射
FORMAT_CODES = {
    '§k': '§k',  # random
    '§l': '§l',  # bold
    '§m': '§m',  # strikethrough (仅Java版)
    '§n': '§n',  # underline (仅Java版) 
    '§o': '§o',  # italic
    '§r': '§r',  # reset
}

def detect_selector_type(selector):
    """
    检测目标选择器是Java版还是基岩版
    如果是通用的，返回'bedrock'作为默认值
    """
    # 检查Java版特有参数
    java_specific_params = ['distance', 'x_rotation', 'y_rotation', 'nbt', 'team', 'limit', 'sort', 'predicate', 'advancements', 'level', 'gamemode', 'attributes']
    # 检查基岩版特有参数
    bedrock_specific_params = ['r', 'rm', 'rx', 'rxm', 'ry', 'rym', 'hasitem', 'family', 'l', 'lm', 'm', 'haspermission', 'has_property', 'c']
    
    # 检查选择器变量
    selector_var = selector.split('[')[0] if '[' in selector else selector
    
    # 基岩版特有的选择器变量
    bedrock_specific_selectors = ['@initiator', '@c', '@v']
    # Java版特有的选择器参数
    java_specific_selectors = []
    
    # 检查选择器变量
    if selector_var in bedrock_specific_selectors:
        return 'bedrock'
    elif selector_var in java_specific_selectors:
        return 'java'
    
    # 提取参数部分
    java_count = 0
    bedrock_count = 0
    
    if '[' in selector and ']' in selector:
        params_part = selector[selector.find('[')+1:selector.find(']')]
        params = [p.strip() for p in params_part.split(',')]
        
        for param in params:
            if '=' in param:
                param_name = param.split('=')[0].strip()
                if param_name in java_specific_params:
                    java_count += 1
                elif param_name in bedrock_specific_params:
                    bedrock_count += 1
                
    
    # 检查坐标参数格式
    # Java版: x=1,y=2,z=3 (无空格)
    # 基岩版: x=1, y=2, z=3 (可以有空格)
    if '[' in selector and ']' in selector:
        params_part = selector[selector.find('[')+1:selector.find(']')]
        # 如果有空格在等号周围，更可能是基岩版
        if ' = ' in params_part or ', ' in params_part:
            bedrock_count += 1
    
    if java_count > bedrock_count:
        return 'java'
    elif bedrock_count > java_count:
        return 'bedrock'
    else:
        # 当Java参数和基岩参数数量相等时，尝试使用其他启发式方法
        # 如果参数完全相等或只有通用参数，使用更细致的判断
        if java_count == 0 and bedrock_count == 0 and '[' in selector and ']' in selector:
            # 只有通用参数，根据社区使用习惯，Java版更常使用scores等参数
            # 所以如果只有通用参数，更可能认为是Java版
            params_part = selector[selector.find('[')+1:selector.find(']')]
            params = [p.strip() for p in params_part.split(',')]
            for param in params:
                if '=' in param:
                    param_name = param.split('=')[0].strip()
                    
        # 默认返回bedrock
        return 'bedrock'

def convert_bedrock_selector_to_java(selector):
    """
    将基岩版特有的选择器转换为Java版兼容的选择器
    """
    import re
    
    # 基岩版特有选择器变量到Java版的映射
    bedrock_to_java_mapping = {
        '@initiator': '@a',  # @initiator 在Java版中最接近 @a (所有玩家)
        '@c': '@a',  # @c (自己的智能体) 在Java版中没有对应，使用 @a
        '@v': '@a'   # @v (所有智能体) 在Java版中没有对应，使用 @a
    }
    
    # 检查选择器变量
    selector_var = selector.split('[')[0] if '[' in selector else selector
    params_part = ''
    
    if '[' in selector and ']' in selector:
        params_part = selector[selector.find('['):]  # 保留参数部分
    
    # 检查是否为基岩版特有选择器
    if selector_var in bedrock_to_java_mapping:
        # 保留参数部分，替换选择器变量
        new_selector = bedrock_to_java_mapping[selector_var] + params_part
        # 转换参数格式
        converted_selector, param_reminders = convert_selector_parameters(new_selector)
        
        # 添加选择器变量转换的提醒信息
        conversion_note = f"基岩版选择器 {selector_var} 在Java版中不支持，已转换为 {bedrock_to_java_mapping[selector_var]}"
        param_reminders.append(conversion_note)
        
        return converted_selector, True, param_reminders  # 返回转换后的选择器、转换状态和提醒
    else:
        # 即使不是基岩版特有选择器，也要转换参数格式
        converted_selector, param_reminders = convert_selector_parameters(selector)
        # 只有当选择器变量发生了实际的基岩版到Java版的转换时才返回True
        return converted_selector, False, param_reminders  # 返回转换后的选择器、转换状态和提醒

def convert_selector_parameters(selector, selector_var=None):
    """
    转换选择器参数格式，去除Java版参数中的空格并处理参数转换
    """
    import re
    
    # 如果没有参数部分，直接返回
    if '[' not in selector or ']' not in selector:
        return selector, []
    
    # 提取选择器变量和参数部分
    current_selector_var = selector.split('[')[0] if selector_var is None else selector_var
    params_part = selector[selector.find('['):]
    
    # 处理hasitem参数转换为nbt参数并收集提醒
    converted_params, hasitem_reminders = convert_hasitem_to_nbt_with_reminders(params_part)
    
    # 只在检测到NBT参数时才调用范围值处理函数
    if 'nbt=' in converted_params:
        converted_params = process_range_values(converted_params)
    
    # 使用更精确的正则表达式去除所有不必要的空格，确保不会影响字符串值中的空格
    # 去除等号周围的空格（但不在字符串内部）
    # 首先保护字符串内容（引号内的内容）
    string_pattern = r'"([^"]*)"'
    strings = []
    def protect_strings(match):
        strings.append(match.group(0))
        return f"__STRING_{len(strings)-1}__"
    
    # 保护所有字符串
    converted_params = re.sub(string_pattern, protect_strings, converted_params)
    
    # 去除等号周围的空格
    converted_params = re.sub(r'\s*=\s*', '=', converted_params)
    # 去除逗号后的空格
    converted_params = re.sub(r',\s*', ',', converted_params)
    # 去除方括号内的前后空格
    converted_params = re.sub(r'\[\s+', '[', converted_params)
    converted_params = re.sub(r'\s+\]', ']', converted_params)
    
    # 恢复字符串内容
    for i, string in enumerate(strings):
        converted_params = converted_params.replace(f"__STRING_{i}__", string)
    
    all_reminders = hasitem_reminders
    return current_selector_var + converted_params, all_reminders

def convert_limit_c_parameters(params_part):
    """
    将limit和c参数互相转换
    """
    import re
    
    reminders = []
    
    # 处理limit参数转换为c参数（Java版到基岩版）
    # 注意：只有在没有sort参数的情况下才直接转换limit为c
    # 如果有sort参数，转换逻辑在convert_sort_parameters中处理
    limit_pattern = r'limit=([+-]?\d+)'
    def replace_limit_to_c(match):
        limit_value = match.group(1)
        reminders.append(f"Java版limit={limit_value}参数已转换为基岩版c={limit_value}")
        reminders.append("limit只是限制数量，c当由近到远")
        return f'c={limit_value}'
    
    params_part = re.sub(limit_pattern, replace_limit_to_c, params_part)
    
    # 处理c参数转换为limit参数（基岩版到Java版）
    c_pattern = r'c=([+-]?\d+)'
    def replace_c_to_limit(match):
        c_value = match.group(1)
        # 当c=数字时，转换为limit=数字,sort=nearest
        if not c_value.startswith('-'):
            reminders.append(f"基岩版c={c_value}参数已转换为Java版limit={c_value},sort=nearest")
            return f'limit={c_value},sort=nearest'
        # 当c=-数字时，转换为limit=数字,sort=furthest
        else:
            abs_c_val = c_value[1:]  # 移除负号
            reminders.append(f"基岩版c={c_value}参数已转换为Java版limit={abs_c_val},sort=furthest")
            return f'limit={abs_c_val},sort=furthest'
    
    params_part = re.sub(c_pattern, replace_c_to_limit, params_part)
    
    return params_part, reminders

def convert_sort_parameters(params_part, selector_var):
    """
    将Java版sort参数转换为基岩版c参数
    """
    import re
    
    reminders = []
    
    # 查找sort参数
    sort_pattern = r'sort=([^,\]]+)'
    match = re.search(sort_pattern, params_part)
    
    if match:
        sort_value = match.group(1)
        
        # 查找limit参数（如果存在）
        limit_pattern = r'limit=([+-]?\d+)'
        limit_match = re.search(limit_pattern, params_part)
        limit_value = limit_match.group(1) if limit_match else None
        
        # 根据sort值进行转换
        if sort_value == 'nearest':
            # 当limit=数字,sort=nearest时，基岩版转换为c=数字
            # 当只有sort=nearest，没有limit时，基岩版转换为c=9999
            c_value = limit_value if limit_value else '9999'
            # 移除sort参数和limit参数
            params_part = re.sub(sort_pattern, '', params_part)
            if limit_value:
                params_part = re.sub(limit_pattern, '', params_part)
            # 添加c参数
            if re.search(r'c=[+-]?\d+', params_part):
                params_part = re.sub(r'c=[+-]?\d+', f'c={c_value}', params_part)
            else:
                if params_part.endswith('['):
                    params_part = params_part[:-1] + f'c={c_value}]'
                elif params_part.endswith(']'):
                    params_part = params_part[:-1] + f',c={c_value}]'
                else:
                    params_part = params_part + f'c={c_value}'
            reminders.append(f"Java版sort=nearest已转换为基岩版c={c_value}")
        elif sort_value == 'furthest':
            # 当limit=数字,sort=furthest时，基岩版转换为c=-数字
            # 当只有sort=furthest，没有limit时，基岩版转换为c=-9999
            c_value = f"-{limit_value}" if limit_value else '-9999'
            # 移除sort参数和limit参数
            params_part = re.sub(sort_pattern, '', params_part)
            if limit_value:
                params_part = re.sub(limit_pattern, '', params_part)
            # 添加c参数
            if re.search(r'c=[+-]?\d+', params_part):
                params_part = re.sub(r'c=[+-]?\d+', f'c={c_value}', params_part)
            else:
                if params_part.endswith('['):
                    params_part = params_part[:-1] + f'c={c_value}]'
                elif params_part.endswith(']'):
                    params_part = params_part[:-1] + f',c={c_value}]'
                else:
                    params_part = params_part + f'c={c_value}'
            reminders.append(f"Java版sort=furthest已转换为基岩版c={c_value}")
        elif sort_value == 'arbitrary':
            # 基岩版不支持sort=arbitrary，直接移除
            params_part = re.sub(sort_pattern, '', params_part)
            reminders.append("Java版sort=arbitrary在基岩版中不支持，已移除")
        elif sort_value == 'random':
            # 当@a[limit=数字,sort=random]或@r[limit=数字,sort=random]时，转换为@r[c=数字]
            # 当只有@a[sort=random]或@r[sort=random]时，转换为基岩版的@r[c=9999]
            c_value = limit_value if limit_value else '9999'
            if selector_var in ['@a', '@r']:
                # 对于@a[sort=random]或@r[sort=random]，转换为@r[c=9999]
                # 移除sort参数和limit参数
                params_part = re.sub(sort_pattern, '', params_part)
                if limit_value:
                    params_part = re.sub(limit_pattern, '', params_part)
                # 添加c参数
                if re.search(r'c=[+-]?\d+', params_part):
                    params_part = re.sub(r'c=[+-]?\d+', f'c={c_value}', params_part)
                else:
                    if params_part.endswith('['):
                        params_part = params_part[:-1] + f'c={c_value}]'
                    elif params_part.endswith(']'):
                        params_part = params_part[:-1] + f',c={c_value}]'
                    else:
                        params_part = params_part + f'c={c_value}'
                reminders.append(f"Java版{selector_var}[sort=random]已转换为基岩版@r[c={c_value}]")
            else:
                # 对于其他选择器，如@e[sort=random,limit=N]，转换为@e[c=N]
                # 移除sort参数和limit参数
                params_part = re.sub(sort_pattern, '', params_part)
                if limit_value:
                    params_part = re.sub(limit_pattern, '', params_part)
                # 添加c参数
                if re.search(r'c=[+-]?\d+', params_part):
                    params_part = re.sub(r'c=[+-]?\d+', f'c={c_value}', params_part)
                else:
                    if params_part.endswith('['):
                        params_part = params_part[:-1] + f'c={c_value}]'
                    elif params_part.endswith(']'):
                        params_part = params_part[:-1] + f',c={c_value}]'
                    else:
                        params_part = params_part + f'c={c_value}'
                reminders.append(f"Java版sort=random已转换为基岩版c={c_value}")
        else:
            # 其他情况砍掉并提示
            params_part = re.sub(sort_pattern, '', params_part)
            reminders.append(f"Java版sort={sort_value}在基岩版中不支持，已移除")
    
    # 清理多余的逗号和空括号
    params_part = re.sub(r',,', ',', params_part)
    params_part = re.sub(r'\[,', '[', params_part)
    params_part = re.sub(r',\]', ']', params_part)
    params_part = re.sub(r'\[\]', '', params_part)
    
    return params_part, reminders

def convert_hasitem_to_nbt_with_reminders(params_part):
    """
    将基岩版的hasitem参数转换为Java版的nbt参数，并返回提醒信息
    """
    import re
    
    reminders = []
    
    # 处理hasitem的复杂格式：hasitem=[{...},{...}] 或 hasitem={...}
    # 先处理复杂格式 [{}]
    complex_pattern = r'hasitem=\[([^\[\]]*)\]'
    def replace_complex_hasitem(match):
        full_match = match.group(0)  # 完整的匹配，如 hasitem=[{...}]
        content = match.group(1)
        nbt_result, item_reminders = parse_hasitem_complex(content)
        reminders.extend(item_reminders)
        if nbt_result:
            return nbt_result
        else:
            # 转换失败，保留原始hasitem参数并添加提醒
            reminders.append(f"hasitem参数转换失败，保留原始hasitem参数")
            return full_match
    
    params_part = re.sub(complex_pattern, replace_complex_hasitem, params_part)
    
    # 再处理简单格式 {...}
    simple_pattern = r'hasitem=\{([^}]*)\}'
    def replace_simple_hasitem(match):
        full_match = match.group(0)  # 完整的匹配，如 hasitem={...}
        content = match.group(1)
        nbt_result, item_reminders = parse_hasitem_simple(content)
        reminders.extend(item_reminders)
        if nbt_result:
            return nbt_result
        else:
            # 转换失败，保留原始hasitem参数并添加提醒
            reminders.append(f"hasitem参数转换失败，保留原始hasitem参数")
            return full_match
    
    params_part = re.sub(simple_pattern, replace_simple_hasitem, params_part)
    
    # 清理多余的逗号和空括号
    params_part = re.sub(r',,', ',', params_part)
    params_part = re.sub(r',\]', ']', params_part)
    params_part = re.sub(r'\[,', '[', params_part)
    
    return params_part, reminders

def convert_gamemode_parameters(java_selector, bedrock_selector):
    """
    转换gamemode和m参数，处理模式映射和提醒
    同时处理其他基岩版到Java版的参数转换
    """
    import re
    
    # Java版游戏模式到基岩版的映射
    java_to_bedrock_gamemode = {
        'survival': 'survival',  # 生存模式 -> 生存模式
        'creative': 'creative',  # 创造模式 -> 创造模式
        'adventure': 'adventure',  # 冒险模式 -> 冒险模式
        'spectator': 'survival'  # 旁观模式 -> 生存模式（基岩版没有旁观模式）
    }
    
    # 基岩版游戏模式到Java版的映射
    bedrock_to_java_gamemode = {
        'survival': 'survival',  # 生存模式 -> 生存模式
        'creative': 'creative',  # 创造模式 -> 创造模式
        'adventure': 'adventure',  # 冒险模式 -> 冒险模式
        'default': 'survival',  # 默认模式 -> 生存模式
        's': 'survival',  # 缩写 -> 全称
        'c': 'creative',  # 缩写 -> 全称
        'a': 'adventure',  # 缩写 -> 全称
        'd': 'survival',  # 缩写 -> 生存模式
        '0': 'survival',  # 数字 -> 全称
        '1': 'creative',  # 数字 -> 全称
        '2': 'adventure',  # 数字 -> 全称
        '5': 'survival'  # 数字 -> 生存模式
    }
    
    # 处理Java版输出的gamemode参数转换（Java版到基岩版）
    java_converted = java_selector
    java_reminders = []
    # 调试：打印输入
    
    # 从gamemode到m的转换（Java版到基岩版）
    gamemode_pattern = r'\bgamemode=(!?)([^,\]]+)'
    def replace_gamemode_to_bedrock(match):
        negation = match.group(1)  # ! 或空
        gamemode_value = match.group(2).strip()
        
        # 如果是旁观模式，需要提醒用户并转换为生存模式
        if gamemode_value == 'spectator':
            if negation:
                java_reminders.append(f"Java版反选旁观模式(gamemode=!{gamemode_value})在基岩版中不支持，已转换为反选生存模式")
            else:
                java_reminders.append(f"Java版旁观模式(gamemode={gamemode_value})在基岩版中不支持，已转换为生存模式")
            return f'm={negation}survival'
        elif gamemode_value in java_to_bedrock_gamemode:
            return f'm={negation}{java_to_bedrock_gamemode[gamemode_value]}'
        else:
            # 保持原值
            return match.group(0)
    
    java_converted = re.sub(gamemode_pattern, replace_gamemode_to_bedrock, java_converted)
    
    # 处理基岩版输出的m参数转换（基岩版到Java版）
    bedrock_converted = bedrock_selector
    bedrock_reminders = []
    
    # 从m到gamemode的转换（基岩版到Java版）
    m_pattern = r'\bm=(!?)([^,\]]+)'
    def replace_m_to_java(match):
        negation = match.group(1)  # ! 或空
        m_value = match.group(2).strip()
        
        # 如果是默认模式，需要提醒用户并转换为生存模式
        if m_value == 'default' or m_value == 'd' or m_value == '5':
            if negation:
                bedrock_reminders.append(f"基岩版反选默认模式(m=!{m_value})在Java版中不支持，已转换为反选生存模式")
            else:
                bedrock_reminders.append(f"基岩版默认模式(m={m_value})在Java版中不支持，已转换为生存模式")
            return f'gamemode={negation}survival'
        elif m_value in bedrock_to_java_gamemode:
            return f'gamemode={negation}{bedrock_to_java_gamemode[m_value]}'
        else:
            # 保持原值
            return match.group(0)
    
    bedrock_converted = re.sub(m_pattern, replace_m_to_java, bedrock_converted)
    
    # 处理limit参数到c参数的转换（Java版到基岩版）
    limit_pattern = r'\blimit=([+-]?\d+)'
    def replace_limit_to_c(match):
        limit_value = match.group(1)
        java_reminders.append(f"Java版limit={limit_value}参数已转换为基岩版c={limit_value}")
        return f'c={limit_value}'
    
    java_converted = re.sub(limit_pattern, replace_limit_to_c, java_converted)
    
    # 处理c参数到limit参数的转换（基岩版到Java版）
    c_pattern = r'\bc=([+-]?\d+)'
    def replace_c_to_limit(match):
        c_value = match.group(1)
        # 处理负数c值，转换为绝对值并添加sort=furthest参数
        if c_value.startswith('-'):
            abs_c_val = c_value[1:]  # 移除负号
            bedrock_reminders.append(f"基岩版c={c_value}参数已转换为Java版limit={abs_c_val},sort=furthest")
            # 检查是否已有sort参数
            if not re.search(r'\bsort=', bedrock_converted):
                # 添加sort=furthest参数
                return f'limit={abs_c_val},sort=furthest'
            else:
                return f'limit={abs_c_val}'
        else:
            bedrock_reminders.append(f"基岩版c={c_value}参数已转换为Java版limit={c_value}")
            return f'limit={c_value}'
    
    bedrock_converted = re.sub(c_pattern, replace_c_to_limit, bedrock_converted)
    
    # 添加缺失的基岩版到Java版参数转换逻辑
    
    # 处理r/rm参数到distance参数的转换（基岩版到Java版）
    def convert_r_rm_to_distance(selector):
        """将基岩版的r和rm参数转换为Java版的distance参数"""
        nonlocal bedrock_reminders
        
        # 提取r和rm参数
        r_pattern = r'\br=([^,\]]+)'
        rm_pattern = r'\brm=([^,\]]+)'
        
        r_match = re.search(r_pattern, selector)
        rm_match = re.search(rm_pattern, selector)
        
        if not r_match and not rm_match:
            return selector
        
        r_value = r_match.group(1) if r_match else None
        rm_value = rm_match.group(1) if rm_match else None
        
        # 构建distance参数
        distance_value = ""
        if rm_value and r_value:
            # 两个参数都存在：rm..r
            distance_value = f"{rm_value}..{r_value}"
            bedrock_reminders.append(f"基岩版rm={rm_value},r={r_value}参数已转换为Java版distance={distance_value}")
        elif rm_value:
            # 只有rm参数：rm..
            distance_value = f"{rm_value}.."
            bedrock_reminders.append(f"基岩版rm={rm_value}参数已转换为Java版distance={distance_value}")
        elif r_value:
            # 只有r参数：..r
            distance_value = f"..{r_value}"
            bedrock_reminders.append(f"基岩版r={r_value}参数已转换为Java版distance={distance_value}")
        
        # 移除原有的r和rm参数
        result = re.sub(r_pattern, '', selector)
        result = re.sub(rm_pattern, '', result)
        
        # 添加distance参数
        if distance_value:
            # 检查是否已有其他参数
            if '[' in result and ']' in result:
                if result.endswith('['):
                    result = result[:-1] + f"distance={distance_value}]"
                elif result.endswith(']'):
                    result = result[:-1] + f",distance={distance_value}]"
                else:
                    result = result + f",distance={distance_value}]"
            else:
                result = f"[{result}distance={distance_value}]" if '[' in result else f"{result}[distance={distance_value}]"
        
        # 清理多余的逗号和空括号
        result = re.sub(r',,', ',', result)
        result = re.sub(r'\[,', '[', result)
        result = re.sub(r',\]', ']', result)
        result = re.sub(r'\[\]', '', result)
        
        return result
    
    # 处理rx/rxm参数到x_rotation参数的转换（基岩版到Java版）
    def convert_rx_rxm_to_x_rotation(selector):
        """将基岩版的rx和rxm参数转换为Java版的x_rotation参数"""
        nonlocal bedrock_reminders
        
        # 提取rx和rxm参数
        rx_pattern = r'\brx=([^,\]]+)'
        rxm_pattern = r'\brxm=([^,\]]+)'
        
        rx_match = re.search(rx_pattern, selector)
        rxm_match = re.search(rxm_pattern, selector)
        
        if not rx_match and not rxm_match:
            return selector
        
        rx_value = rx_match.group(1) if rx_match else None
        rxm_value = rxm_match.group(1) if rxm_match else None
        
        # 构建x_rotation参数
        x_rotation_value = ""
        if rxm_value and rx_value:
            # 两个参数都存在：rxm..rx
            x_rotation_value = f"{rxm_value}..{rx_value}"
            bedrock_reminders.append(f"基岩版rxm={rxm_value},rx={rx_value}参数已转换为Java版x_rotation={x_rotation_value}")
        elif rxm_value:
            # 只有rxm参数：rxm..
            x_rotation_value = f"{rxm_value}.."
            bedrock_reminders.append(f"基岩版rxm={rxm_value}参数已转换为Java版x_rotation={x_rotation_value}")
        elif rx_value:
            # 只有rx参数：..rx
            x_rotation_value = f"..{rx_value}"
            bedrock_reminders.append(f"基岩版rx={rx_value}参数已转换为Java版x_rotation={x_rotation_value}")
        
        # 移除原有的rx和rxm参数
        result = re.sub(rx_pattern, '', selector)
        result = re.sub(rxm_pattern, '', result)
        
        # 添加x_rotation参数
        if x_rotation_value:
            # 检查是否已有其他参数
            if '[' in result and ']' in result:
                if result.endswith('['):
                    result = result[:-1] + f"x_rotation={x_rotation_value}]"
                elif result.endswith(']'):
                    result = result[:-1] + f",x_rotation={x_rotation_value}]"
                else:
                    result = result + f",x_rotation={x_rotation_value}]"
            else:
                result = f"[{result}x_rotation={x_rotation_value}]" if '[' in result else f"{result}[x_rotation={x_rotation_value}]"
        
        # 清理多余的逗号和空括号
        result = re.sub(r',,', ',', result)
        result = re.sub(r'\[,', '[', result)
        result = re.sub(r',\]', ']', result)
        result = re.sub(r'\[\]', '', result)
        
        return result
    
    # 处理ry/rym参数到y_rotation参数的转换（基岩版到Java版）
    def convert_ry_rym_to_y_rotation(selector):
        """将基岩版的ry和rym参数转换为Java版的y_rotation参数"""
        nonlocal bedrock_reminders
        
        # 提取ry和rym参数
        ry_pattern = r'\bry=([^,\]]+)'
        rym_pattern = r'\brym=([^,\]]+)'
        
        ry_match = re.search(ry_pattern, selector)
        rym_match = re.search(rym_pattern, selector)
        
        if not ry_match and not rym_match:
            return selector
        
        ry_value = ry_match.group(1) if ry_match else None
        rym_value = rym_match.group(1) if rym_match else None
        
        # 构建y_rotation参数
        y_rotation_value = ""
        if rym_value and ry_value:
            # 两个参数都存在：rym..ry
            y_rotation_value = f"{rym_value}..{ry_value}"
            bedrock_reminders.append(f"基岩版rym={rym_value},ry={ry_value}参数已转换为Java版y_rotation={y_rotation_value}")
        elif rym_value:
            # 只有rym参数：rym..
            y_rotation_value = f"{rym_value}.."
            bedrock_reminders.append(f"基岩版rym={rym_value}参数已转换为Java版y_rotation={y_rotation_value}")
        elif ry_value:
            # 只有ry参数：..ry
            y_rotation_value = f"..{ry_value}"
            bedrock_reminders.append(f"基岩版ry={ry_value}参数已转换为Java版y_rotation={y_rotation_value}")
        
        # 移除原有的ry和rym参数
        result = re.sub(ry_pattern, '', selector)
        result = re.sub(rym_pattern, '', result)
        
        # 添加y_rotation参数
        if y_rotation_value:
            # 检查是否已有其他参数
            if '[' in result and ']' in result:
                if result.endswith('['):
                    result = result[:-1] + f"y_rotation={y_rotation_value}]"
                elif result.endswith(']'):
                    result = result[:-1] + f",y_rotation={y_rotation_value}]"
                else:
                    result = result + f",y_rotation={y_rotation_value}]"
            else:
                result = f"[{result}y_rotation={y_rotation_value}]" if '[' in result else f"{result}[y_rotation={y_rotation_value}]"
        
        # 清理多余的逗号和空括号
        result = re.sub(r',,', ',', result)
        result = re.sub(r'\[,', '[', result)
        result = re.sub(r',\]', ']', result)
        result = re.sub(r'\[\]', '', result)
        
        return result
    
    # 处理l/lm参数到level参数的转换（基岩版到Java版）
    def convert_l_lm_to_level(selector):
        """将基岩版的l和lm参数转换为Java版的level参数"""
        nonlocal bedrock_reminders
        
        # 提取l和lm参数
        l_pattern = r'\bl=([^,\]]+)'
        lm_pattern = r'\blm=([^,\]]+)'
        
        l_match = re.search(l_pattern, selector)
        lm_match = re.search(lm_pattern, selector)
        
        if not l_match and not lm_match:
            return selector
        
        l_value = l_match.group(1) if l_match else None
        lm_value = lm_match.group(1) if lm_match else None
        
        # 构建level参数
        level_value = ""
        if lm_value and l_value:
            # 两个参数都存在：lm..l
            level_value = f"{lm_value}..{l_value}"
            bedrock_reminders.append(f"基岩版lm={lm_value},l={l_value}参数已转换为Java版level={level_value}")
        elif lm_value:
            # 只有lm参数：lm..
            level_value = f"{lm_value}.."
            bedrock_reminders.append(f"基岩版lm={lm_value}参数已转换为Java版level={level_value}")
        elif l_value:
            # 只有l参数：..l
            level_value = f"..{l_value}"
            bedrock_reminders.append(f"基岩版l={l_value}参数已转换为Java版level={level_value}")
        
        # 移除原有的l和lm参数
        result = re.sub(l_pattern, '', selector)
        result = re.sub(lm_pattern, '', result)
        
        # 添加level参数
        if level_value:
            # 检查是否已有其他参数
            if '[' in result and ']' in result:
                if result.endswith('['):
                    result = result[:-1] + f"level={level_value}]"
                elif result.endswith(']'):
                    result = result[:-1] + f",level={level_value}]"
                else:
                    result = result + f",level={level_value}]"
            else:
                result = f"[{result}level={level_value}]" if '[' in result else f"{result}[level={level_value}]"
        
        # 清理多余的逗号和空括号
        result = re.sub(r',,', ',', result)
        result = re.sub(r'\[,', '[', result)
        result = re.sub(r',\]', ']', result)
        result = re.sub(r'\[\]', '', result)
        
        return result
    
    # 应用所有基岩版到Java版的参数转换
    bedrock_converted = convert_r_rm_to_distance(bedrock_converted)
    bedrock_converted = convert_rx_rxm_to_x_rotation(bedrock_converted)
    bedrock_converted = convert_ry_rym_to_y_rotation(bedrock_converted)
    bedrock_converted = convert_l_lm_to_level(bedrock_converted)
    
    # 调试：打印输出
    
    return java_converted, bedrock_converted, java_reminders, bedrock_reminders

def convert_nbt_to_hasitem(params_part):
    """
    尝试将Java版的nbt参数转换为基岩版的hasitem参数
    如果可以转换则转换，如果不能转换则保留原nbt参数
    """
    import re
    
    # 收集所有转换提醒
    all_reminders = []
    
    def replace_nbt(match):
        full_match = match.group(0)  # 完整匹配，如 nbt={...}
        nbt_content = match.group(1)  # 大括号内的内容
        
        # 先检查是否包含物品相关信息（SelectedItem、Item、Inventory）
        if not any(keyword in nbt_content for keyword in ['SelectedItem', 'Item', 'Inventory']):
            # 如果不包含物品信息，直接返回原匹配内容
            return full_match
        
        # 尝试解析nbt内容，看是否可以转换为hasitem
        hasitem_result, conversion_reminders = try_convert_nbt_content_to_hasitem(nbt_content)
        # 收集转换提醒
        all_reminders.extend(conversion_reminders)
        
        if hasitem_result:
            # 如果可以转换，返回hasitem参数
            all_reminders.append("nbt参数已转换为hasitem格式，可能无法完全保留原意")
            return f'hasitem={hasitem_result}'
        else:
            # 如果不能转换，返回原始nbt参数（保持完整格式）
            return full_match
    
    # 使用非递归正则表达式匹配nbt参数，处理嵌套的大括号结构
    try:
        nbt_pattern = r'nbt=(\{[^{}]*(?:\{[^{}]*\}[^{}]*)*\})'
        result = re.sub(nbt_pattern, replace_nbt, params_part, flags=re.DOTALL)
    except re.error:
        # 如果正则表达式失败，尝试更简单的模式
        try:
            # 简化的NBT匹配模式，只处理一层嵌套
            simple_nbt_pattern = r'nbt=(\{[^{}]*\})'
            result = re.sub(simple_nbt_pattern, replace_nbt, params_part)
        except re.error:
            # 如果仍然失败，返回原始参数，不进行转换
            result = params_part
            all_reminders.append("NBT参数解析失败，保留原始格式")
    
    return result, all_reminders

def try_convert_nbt_content_to_hasitem(nbt_content):
    """
    尝试将nbt内容转换为hasitem内容
    返回转换后的hasitem内容和提醒信息列表，如果不能转换则返回(None, [])
    """
    import re
    
    # 初始化提醒列表
    reminders = []
    
    # 检查nbt内容是否包含物品信息
    # 例如: {SelectedItem:{id:"minecraft:diamond_sword"}} 或 {Inventory:[{id:"minecraft:diamond",Count:3b}]}
    
    # 模式1: SelectedItem:{...id:"xxx"..., ...,Slot:0b} - 用于指定槽位
    selected_item_pattern = r'SelectedItem\s*:\s*\{([^}]*(?:\{[^}]*\}[^}]*)*)\}'
    match = re.search(selected_item_pattern, nbt_content)
    if match:
        item_data = match.group(1)
        # 提取id
        id_match = re.search(r'id\s*:\s*["\']([^"\']+)["\']', item_data)
        if id_match:
            item_id = id_match.group(1)
            # 移除minecraft:前缀（如果存在）
            if item_id.startswith('minecraft:'):
                item_id = item_id[10:]  # 移除 'minecraft:' 前缀
            # 提取Count（数量）
            count_match = re.search(r'Count\s*["\']?\s*:\s*(\d+)[bBfFdD]?', item_data)
            # 提取Slot信息
            slot_match = re.search(r'Slot\s*["\']?\s*:\s*(\d+)', item_data)
            if slot_match:
                slot_value = slot_match.group(1)
                slot_str = ""
                if slot_value == "0":  # 主手
                    slot_str = ",location=slot.weapon.mainhand"
                elif slot_value == "1":  # 副手
                    slot_str = ",location=slot.weapon.offhand"
                else:
                    # 对于其他槽位，可以根据需要扩展
                    slot_str = ",location=slot.inventory,slot=" + slot_value + ".." + slot_value
                
                if count_match:
                    count_value = count_match.group(1)
                    reminders.append("nbt参数转换为hasitem参数，可能无法完全保留原意")
                    return "{item=" + item_id + ",quantity=" + count_value + ".." + slot_str + "}", reminders
                else:
                    reminders.append("nbt参数转换为hasitem参数，可能无法完全保留原意")
                    return "{item=" + item_id + slot_str + "}", reminders
            else:
                # 如果没有显式槽位信息，SelectedItem默认是主手物品
                slot_str = ",location=slot.weapon.mainhand"
                if count_match:
                    count_value = count_match.group(1)
                    reminders.append("nbt参数转换为hasitem参数，可能无法完全保留原意")
                    return "{item=" + item_id + ",quantity=" + count_value + ".." + slot_str + "}", reminders
                else:
                    reminders.append("nbt参数转换为hasitem参数，可能无法完全保留原意")
                    return "{item=" + item_id + slot_str + "}", reminders
    
    # 模式2: Inventory:[{...id:"xxx"..., ...,Slot:0b, ...}, ...] - 指定具体槽位的物品
    inventory_pattern = r'Inventory\s*:\s*\[([^\]]*)\]'
    match = re.search(inventory_pattern, nbt_content)
    if match:
        inventory_content = match.group(1)
        # 解析所有物品
        items = []
        # 使用正则表达式匹配所有物品对象
        item_objects = re.findall(r'\{([^{}]*(?:\{[^{}]*\}[^{}]*)*)\}', inventory_content)
        
        for item_obj in item_objects:
            # 提取id
            id_match = re.search(r'id\s*:\s*["\']([^"\']+)["\']', item_obj)
            if not id_match:
                continue  # 跳过没有id的物品
                
            item_id = id_match.group(1)
            # 移除minecraft:前缀（如果存在）
            if item_id.startswith('minecraft:'):
                item_id = item_id[10:]  # 移除 'minecraft:' 前缀
                
            # 提取Count（数量）
            count_match = re.search(r'Count\s*["\']?\s*:\s*(\d+)[bBfFdD]?', item_obj)
            # 提取Slot信息
            slot_match = re.search(r'Slot\s*["\']?\s*:\s*(\d+)', item_obj)
            
            # 构建hasitem参数
            item_str = f'item={item_id}'
            if count_match:
                count_value = count_match.group(1)
                item_str += f',quantity={count_value}..'
            
            if slot_match:
                slot_value = slot_match.group(1)
                if slot_value == "0":  # 主手
                    item_str += ',location=slot.weapon.mainhand'
                elif slot_value == "1":  # 副手
                    item_str += ',location=slot.weapon.offhand'
                elif slot_value in ["2", "3", "4", "5", "6", "7", "8"]:  # 热键栏
                    item_str += f',location=slot.hotbar,slot={slot_value}..{slot_value}'
                else:
                    # 对于其他槽位
                    item_str += f',location=slot.inventory,slot={slot_value}..{slot_value}'
            
            items.append(item_str)
        
        if items:
            # 如果有多个物品，构建hasitem数组格式
            if len(items) == 1:
                reminders.append("nbt参数转换为hasitem参数，可能无法完全保留原意")
                return f'{{{items[0]}}}', reminders
            else:
                # 多个物品使用数组格式
                items_str = ','.join([f'{{{item}}}' for item in items])
                reminders.append("nbt参数转换为hasitem参数，可能无法完全保留原意")
                return f'[{items_str}]', reminders
    
    # 模式3: Item:{...id:"xxx"..., ...} (对于物品实体)
    item_pattern = r'Item\s*:\s*\{([^}]*(?:\{[^}]*\}[^}]*)*)\}'
    match = re.search(item_pattern, nbt_content)
    if match:
        item_data = match.group(1)
        # 提取id
        id_match = re.search(r'id\s*:\s*["\']([^"\']+)["\']', item_data)
        if id_match:
            item_id = id_match.group(1)
            # 移除minecraft:前缀（如果存在）
            if item_id.startswith('minecraft:'):
                item_id = item_id[10:]  # 移除 'minecraft:' 前缀
            # 提取Count（数量）
            count_match = re.search(r'Count\s*["\']?\s*:\s*(\d+)[bBfFdD]?', item_data)
            if count_match:
                count_value = count_match.group(1)
                reminders.append("nbt参数转换为hasitem参数，可能无法完全保留原意")
                return '{item=' + item_id + ',quantity=' + count_value + '..}', reminders
            else:
                reminders.append("nbt参数转换为hasitem参数，可能无法完全保留原意")
                return '{item=' + item_id + '}', reminders
    
    # 尝试其他可能的NBT模式
    # 模式4: 直接的物品ID匹配，如 {id:"minecraft:diamond"}
    direct_id_pattern = r'id\s*:\s*["\']([^"\']+)["\']'
    match = re.search(direct_id_pattern, nbt_content)
    if match:
        item_id = match.group(1)
        # 移除minecraft:前缀（如果存在）
        if item_id.startswith('minecraft:'):
            item_id = item_id[10:]  # 移除 'minecraft:' 前缀
        reminders.append("nbt参数转换为hasitem参数，仅保留物品ID信息")
        return f'hasitem={{item={item_id}}}', reminders
    
    # 模式5: Tags匹配，如 {Tags:["a","b"]}
    tags_pattern = r'Tags\s*:\s*\[([^\]]*)\]'
    match = re.search(tags_pattern, nbt_content)
    if match:
        tags_content = match.group(1)
        # 提取所有标签
        tag_pattern = r'["\']([^"\']+)["\']'
        tags = re.findall(tag_pattern, tags_content)
        
        if tags:
            # 将每个标签转换为单独的tag参数
            tag_params = []
            for tag in tags:
                tag_params.append(f'tag={tag}')
            
            # 返回第一个标签作为tag参数，因为hasitem不支持多个标签
            reminders.append("nbt参数转换为tag参数，仅保留第一个标签")
            return tag_params[0], reminders
    
    # 模式6: 实体类型匹配，如 {Type:"minecraft:zombie"}
    entity_type_pattern = r'Type\s*:\s*["\']([^"\']+)["\']'
    match = re.search(entity_type_pattern, nbt_content)
    if match:
        entity_type = match.group(1)
        # 移除minecraft:前缀（如果存在）
        if entity_type.startswith('minecraft:'):
            entity_type = entity_type[10:]  # 移除 'minecraft:' 前缀
        reminders.append("nbt参数转换为type参数，仅保留实体类型信息")
        return f'type={entity_type}', reminders
    
    # 如果没有找到可转换的模式，返回None和空提醒列表
    return None, reminders

def convert_limit_c_between_versions(java_selector, bedrock_selector):
    """
    在Java版和基岩版之间转换limit和c参数
    """
    import re
    
    # Java版: limit -> 基岩版: c
    java_converted = java_selector
    bedrock_converted = bedrock_selector
    java_to_bedrock_reminders = []
    bedrock_to_java_reminders = []
    
    # Java版的limit转换为基岩版的c (在处理基岩版命令时需要提醒)
    limit_pattern = r'limit=(!?)([^,\]]+)'
    def replace_limit_to_c(match):
        negation = match.group(1)  # ! 或空
        limit_value = match.group(2).strip()
        # 提醒信息：当只有limit=数字时，转换为c=数字，并给出提醒"limit只是限制数量，c当由近到远"
        java_to_bedrock_reminders.append(f"Java版limit={limit_value}参数已转换为基岩版c={limit_value}")
        java_to_bedrock_reminders.append("limit只是限制数量，c当由近到远")
        return f'c={negation}{limit_value}'
    
    bedrock_converted = re.sub(limit_pattern, replace_limit_to_c, bedrock_converted)
    
    # 基岩版的c转换为Java版的limit (在处理Java版命令时需要提醒)
    c_pattern = r'c=(!?)([^,\]]+)'
    def replace_c_to_limit(match):
        negation = match.group(1)  # ! 或空
        c_value = match.group(2).strip()
        # 当c=数字时，转换为limit=数字,sort=nearest
        # 当c=-数字时，转换为limit=数字,sort=furthest
        if not c_value.startswith('-'):
            bedrock_to_java_reminders.append(f"基岩版c={c_value}参数已转换为Java版limit={c_value},sort=nearest")
            return f'limit={negation}{c_value},sort=nearest'
        else:
            abs_c_val = c_value[1:]  # 移除负号
            bedrock_to_java_reminders.append(f"基岩版c={c_value}参数已转换为Java版limit={abs_c_val},sort=furthest")
            return f'limit={negation}{abs_c_val},sort=furthest'
    
    java_converted = re.sub(c_pattern, replace_c_to_limit, java_converted)
    
    return java_converted, bedrock_converted, java_to_bedrock_reminders, bedrock_to_java_reminders

def convert_distance_parameters(java_params_part, conversion_reminders):
    """
    将Java版的distance参数转换为基岩版的r/rm参数
    """
    import re
    
    # 处理distance参数
    distance_pattern = r'distance=([^,\]]+)'
    
    def replace_distance(match):
        full_match = match.group(0)  # 完整匹配，如 distance=5..10
        distance_value = match.group(1)  # 提取值部分，如 5..10
        
        # 检查是否为范围格式
        if '..' in distance_value:
            parts = distance_value.split('..')
            if parts[0] and parts[1]:
                # 有上下限：5..10 -> rm=5,r=10
                rm_val = parts[0]
                r_val = parts[1]
                conversion_reminders.append(f"Java版distance={distance_value}参数已转换为基岩版rm={rm_val},r={r_val}")
                return f'rm={rm_val},r={r_val}'
            elif parts[0]:
                # 只有下限：5.. -> rm=5
                rm_val = parts[0]
                conversion_reminders.append(f"Java版distance={distance_value}参数已转换为基岩版rm={rm_val}")
                return f'rm={rm_val}'
            elif parts[1]:
                # 只有上限：..10 -> r=10
                r_val = parts[1]
                conversion_reminders.append(f"Java版distance={distance_value}参数已转换为基岩版r={r_val}")
                return f'r={r_val}'
            else:
                # 无效格式
                return full_match
        else:
            # 单个值：10 -> rm=10,r=10（精确匹配）
            conversion_reminders.append(f"Java版distance={distance_value}参数已转换为基岩版rm={distance_value},r={distance_value}")
            return f'rm={distance_value},r={distance_value}'
    
    result = re.sub(distance_pattern, replace_distance, java_params_part)
    
    # 清理多余的逗号和空括号
    result = re.sub(r',,', ',', result)
    result = re.sub(r',\]', ']', result)
    
    return result


def convert_rotation_parameters(java_params_part, conversion_reminders):
    """
    将Java版的x_rotation/y_rotation参数转换为基岩版的rx/rxm和ry/rym参数
    """
    import re
    
    # 处理x_rotation参数
    x_rotation_pattern = r'x_rotation=([^,\]]+)'
    
    def replace_x_rotation(match):
        full_match = match.group(0)
        rotation_value = match.group(1)
        
        # 检查是否为范围格式
        if '..' in rotation_value:
            parts = rotation_value.split('..')
            if parts[0] and parts[1]:
                # 有上下限：-45..45 -> rxm=-45,rx=45
                rxm_val = parts[0]
                rx_val = parts[1]
                conversion_reminders.append(f"Java版x_rotation={rotation_value}参数已转换为基岩版rxm={rxm_val},rx={rx_val}")
                return f'rxm={rxm_val},rx={rx_val}'
            elif parts[0]:
                # 只有下限：-45.. -> rxm=-45
                rxm_val = parts[0]
                conversion_reminders.append(f"Java版x_rotation={rotation_value}参数已转换为基岩版rxm={rxm_val}")
                return f'rxm={rxm_val}'
            elif parts[1]:
                # 只有上限：..45 -> rx=45
                rx_val = parts[1]
                conversion_reminders.append(f"Java版x_rotation={rotation_value}参数已转换为基岩版rx={rx_val}")
                return f'rx={rx_val}'
            else:
                # 无效格式
                return full_match
        else:
            # 单个值：45 -> rxm=45,rx=45（精确匹配）
            conversion_reminders.append(f"Java版x_rotation={rotation_value}参数已转换为基岩版rxm={rotation_value},rx={rotation_value}")
            return f'rxm={rotation_value},rx={rotation_value}'

    # 先处理x_rotation参数
    result = re.sub(x_rotation_pattern, replace_x_rotation, java_params_part)
    
    # 处理y_rotation参数
    y_rotation_pattern = r'y_rotation=([^,\]]+)'
    
    def replace_y_rotation(match):
        full_match = match.group(0)
        rotation_value = match.group(1)
        
        # 检查是否为范围格式
        if '..' in rotation_value:
            parts = rotation_value.split('..')
            if parts[0] and parts[1]:
                # 有上下限：-45..45 -> rym=-45,ry=45
                rym_val = parts[0]
                ry_val = parts[1]
                conversion_reminders.append(f"Java版y_rotation={rotation_value}参数已转换为基岩版rym={rym_val},ry={ry_val}")
                return f'rym={rym_val},ry={ry_val}'
            elif parts[0]:
                # 只有下限：-45.. -> rym=-45
                rym_val = parts[0]
                conversion_reminders.append(f"Java版y_rotation={rotation_value}参数已转换为基岩版rym={rym_val}")
                return f'rym={rym_val}'
            elif parts[1]:
                # 只有上限：..45 -> ry=45
                ry_val = parts[1]
                conversion_reminders.append(f"Java版y_rotation={rotation_value}参数已转换为基岩版ry={ry_val}")
                return f'ry={ry_val}'
            else:
                # 无效格式
                return full_match
        else:
            # 单个值：90 -> rym=90,ry=90（精确匹配）
            conversion_reminders.append(f"Java版y_rotation={rotation_value}参数已转换为基岩版rym={rotation_value},ry={rotation_value}")
            return f'rym={rotation_value},ry={rotation_value}'
    
    # 再处理y_rotation参数
    result = re.sub(y_rotation_pattern, replace_y_rotation, result)
    
    # 清理多余的逗号和空括号
    result = re.sub(r',,', ',', result)
    result = re.sub(r',\]', ']', result)
    
    return result


def convert_level_parameters(java_params_part, conversion_reminders):
    """
    将Java版的level参数转换为基岩版的l/lm参数
    """
    import re
    
    # 处理level参数
    level_pattern = r'level=([^,\]]+)'
    
    def replace_level(match):
        full_match = match.group(0)  # 完整匹配，如 level=5..10
        level_value = match.group(1)  # 提取值部分，如 5..10
        
        # 检查是否为范围格式
        if '..' in level_value:
            parts = level_value.split('..')
            if parts[0] and parts[1]:
                # 有上下限：5..10 -> lm=5,l=10
                lm_val = parts[0]
                l_val = parts[1]
                conversion_reminders.append(f"Java版level={level_value}参数已转换为基岩版lm={lm_val},l={l_val}")
                return f'lm={lm_val},l={l_val}'
            elif parts[0]:
                # 只有下限：5.. -> lm=5
                lm_val = parts[0]
                conversion_reminders.append(f"Java版level={level_value}参数已转换为基岩版lm={lm_val}")
                return f'lm={lm_val}'
            elif parts[1]:
                # 只有上限：..10 -> l=10
                l_val = parts[1]
                conversion_reminders.append(f"Java版level={level_value}参数已转换为基岩版l={l_val}")
                return f'l={l_val}'
            else:
                # 无效格式
                return full_match
        else:
            # 单个值：10 -> lm=10,l=10
            conversion_reminders.append(f"Java版level={level_value}参数已转换为基岩版lm={level_value},l={level_value}")
            return f'lm={level_value},l={level_value}'
    
    result = re.sub(level_pattern, replace_level, java_params_part)
    
    # 清理多余的逗号和空括号
    result = re.sub(r',,', ',', result)
    result = re.sub(r',\]', ']', result)
    
    return result


def filter_selector_parameters(selector, target_version):
    """
    根据目标版本过滤选择器参数，对于可转换的参数进行转换，
    只有完全不支持的参数才被剔除
    """
    import re
    
    # Java版特有参数（完全不支持，无法转换）
    java_specific_params = [
        'predicate', 'advancements', 'team'
    ]
    
    # 基岩版特有参数（完全不支持，无法转换）
    bedrock_specific_params = [
        'haspermission', 'has_property', 'family'
    ]
    
    # 初始化提醒列表
    conversion_reminders = []
    
    # 如果没有参数部分，直接返回
    if '[' not in selector or ']' not in selector:
        return selector, conversion_reminders
    
    # 提取参数部分
    selector_var = selector.split('[')[0]
    params_part = selector[selector.find('[')+1:selector.rfind(']')]
    
    # 初始化nbt_conversion_reminders变量
    nbt_conversion_reminders = []
    
    # 处理hasitem到nbt的转换（基岩版到Java版）
    if target_version == 'java':
        params_part, hasitem_to_nbt_reminders = convert_hasitem_to_nbt_with_reminders(params_part)
        # 添加hasitem转换提醒
        conversion_reminders.extend(hasitem_to_nbt_reminders)
        
        # 处理m参数到gamemode的转换（基岩版到Java版）
        bedrock_to_java_gamemode = {
            'survival': 'survival',
            'creative': 'creative',
            'adventure': 'adventure',
            'default': 'survival',  # 默认模式 -> 生存模式
            's': 'survival',  # 缩写 -> 全称
            'c': 'creative',  # 缩写 -> 全称
            'a': 'adventure',  # 缩写 -> 全称
            'd': 'survival',  # 缩写 -> 生存模式
            '0': 'survival',  # 数字 -> 全称
            '1': 'creative',  # 数字 -> 全称
            '2': 'adventure',  # 数字 -> 全称
            '5': 'survival'  # 数字 -> 生存模式
        }
        
        m_pattern = r'\bm=(!?)([^,\]]+)'
        def replace_m_to_java(match):
            negation = match.group(1)  # ! 或空
            m_value = match.group(2).strip()
            
            # 如果是默认模式，需要提醒用户并转换为生存模式
            if m_value == 'default' or m_value == 'd' or m_value == '5':
                if negation:
                    conversion_reminders.append(f"基岩版反选默认模式(m=!{m_value})在Java版中不支持，已转换为反选生存模式")
                else:
                    conversion_reminders.append(f"基岩版默认模式(m={m_value})在Java版中不支持，已转换为生存模式")
                return f'gamemode={negation}survival'
            elif m_value in bedrock_to_java_gamemode:
                conversion_reminders.append(f"基岩版m={m_value}参数已转换为Java版gamemode={bedrock_to_java_gamemode[m_value]}")
                return f'gamemode={negation}{bedrock_to_java_gamemode[m_value]}'
            else:
                # 保持原值
                return match.group(0)
        
        params_part = re.sub(m_pattern, replace_m_to_java, params_part)
        
        # 特殊处理scores参数中的!=反选：在Java版输出中移除整个scores参数
        scores_pattern = r'scores=\{([^}]*)\}'
        
        def process_scores_negation(match):
            full_match = match.group(0)
            scores_content = match.group(1)
            
            # 检查是否有反选模式
            if '![' in scores_content or '!' in scores_content:
                # Java版不支持scores反选，直接移除整个scores参数
                conversion_reminders.append(f"基岩版scores反选参数{full_match}在Java版中不支持，已移除")
                return ''  # 返回空字符串，表示移除整个参数
            return full_match
        
        params_part = re.sub(scores_pattern, process_scores_negation, params_part)
        
    # 处理nbt到hasitem的转换（Java版到基岩版）
    elif target_version == 'bedrock':
        params_part, nbt_conversion_reminders_from_nbt = convert_nbt_to_hasitem(params_part)
        nbt_conversion_reminders = nbt_conversion_reminders_from_nbt
        
        # 处理scores参数中的level参数
        scores_pattern = r'scores=\{([^}]*)\}'
        
        def process_scores_level(match):
            full_match = match.group(0)
            scores_content = match.group(1)
            
            # 处理level参数
            level_pattern = r'level=([^,\}]+)'
            def replace_level_in_scores(match):
                level_value = match.group(1)
                conversion_reminders.append(f"Java版level={level_value}参数已转换为基岩版lm={level_value},l={level_value}")
                return f'lm={level_value},l={level_value}'
            
            new_scores_content = re.sub(level_pattern, replace_level_in_scores, scores_content)
            
            # 如果scores内容发生了变化，返回新的scores参数
            if new_scores_content != scores_content:
                return f'scores={{{new_scores_content}}}'
            return full_match
        
        params_part = re.sub(scores_pattern, process_scores_level, params_part)
    else:
        nbt_conversion_reminders = []
    
    # 根据目标版本进行参数转换
    if target_version == 'bedrock':
        # Java版到基岩版的参数转换
        params_part = convert_distance_parameters(params_part, conversion_reminders)
        params_part = convert_rotation_parameters(params_part, conversion_reminders)
        params_part = convert_level_parameters(params_part, conversion_reminders)
        
        # 处理gamemode到m的转换（Java版到基岩版）
        java_to_bedrock_gamemode = {
            'survival': 'survival',
            'creative': 'creative',
            'adventure': 'adventure',
            'spectator': 'survival'  # 基岩版没有旁观模式，转换为生存模式
        }
        
        gamemode_pattern = r'gamemode=(!?)([^,\]]+)'
        def replace_gamemode_to_bedrock(match):
            negation = match.group(1)  # ! 或空
            gamemode_value = match.group(2).strip()
            
            if gamemode_value == 'spectator':
                if negation:
                    conversion_reminders.append(f"Java版反选旁观模式(gamemode=!{gamemode_value})在基岩版中不支持，已转换为反选生存模式")
                else:
                    conversion_reminders.append(f"Java版旁观模式(gamemode={gamemode_value})在基岩版中不支持，已转换为生存模式")
                return f'm={negation}survival'
            elif gamemode_value in java_to_bedrock_gamemode:
                conversion_reminders.append(f"Java版gamemode={gamemode_value}参数已转换为基岩版m={java_to_bedrock_gamemode[gamemode_value]}")
                return f'm={negation}{java_to_bedrock_gamemode[gamemode_value]}'
            else:
                # 保持原值
                return match.group(0)
        
        params_part = re.sub(gamemode_pattern, replace_gamemode_to_bedrock, params_part)
        
        # 处理sort参数和limit参数的联合转换（Java版到基岩版）
        sort_pattern = r',?sort=([^,\]]+)'
        limit_pattern = r'limit=([+-]?\d+)'
        
        # 先查找sort和limit参数
        sort_match = re.search(sort_pattern, params_part)
        limit_match = re.search(limit_pattern, params_part)
        sort_value = sort_match.group(1) if sort_match else None
        limit_value = limit_match.group(1) if limit_match else None
        
        if sort_value:
            if sort_value == 'nearest':
                # 当limit=数字,sort=nearest时，基岩版转换为c=数字
                # 当只有sort=nearest，没有limit时，基岩版转换为c=9999
                c_value = limit_value if limit_value else '9999'
                params_part = re.sub(sort_pattern, '', params_part)
                if limit_value:
                    params_part = re.sub(limit_pattern, '', params_part)
                # 添加c参数
                if re.search(r'c=[+-]?\d+', params_part):
                    params_part = re.sub(r'c=[+-]?\d+', f'c={c_value}', params_part)
                else:
                    if params_part.endswith('['):
                        params_part = params_part[:-1] + f'c={c_value}]'
                    elif params_part.endswith(']'):
                        params_part = params_part[:-1] + f',c={c_value}]'
                    else:
                        params_part = params_part + f'c={c_value}'
                conversion_reminders.append(f"Java版sort=nearest已转换为基岩版c={c_value}")
            elif sort_value == 'furthest':
                # 当limit=数字,sort=furthest时，基岩版转换为c=-数字
                # 当只有sort=furthest，没有limit时，基岩版转换为c=-9999
                c_value = f"-{limit_value}" if limit_value else '-9999'
                params_part = re.sub(sort_pattern, '', params_part)
                if limit_value:
                    params_part = re.sub(limit_pattern, '', params_part)
                # 添加c参数
                if re.search(r'c=[+-]?\d+', params_part):
                    params_part = re.sub(r'c=[+-]?\d+', f'c={c_value}', params_part)
                else:
                    if params_part.endswith('['):
                        params_part = params_part[:-1] + f'c={c_value}]'
                    elif params_part.endswith(']'):
                        params_part = params_part[:-1] + f',c={c_value}]'
                    else:
                        params_part = params_part + f'c={c_value}'
                conversion_reminders.append(f"Java版sort=furthest已转换为基岩版c={c_value}")
            elif sort_value == 'arbitrary':
                params_part = re.sub(sort_pattern, '', params_part)
                conversion_reminders.append("Java版sort=arbitrary在基岩版中不支持，已移除")
            elif sort_value == 'random':
                # 当@a[limit=数字,sort=random]或@r[limit=数字,sort=random]时，转换为@r[c=数字]
                # 当只有@a[sort=random]或@r[sort=random]时，转换为基岩版的@r[c=9999]
                c_value = limit_value if limit_value else '9999'
                if selector_var in ['@a', '@r']:
                    params_part = re.sub(sort_pattern, '', params_part)
                    if limit_value:
                        params_part = re.sub(limit_pattern, '', params_part)
                    # 添加c参数
                    if re.search(r'c=[+-]?\d+', params_part):
                        params_part = re.sub(r'c=[+-]?\d+', f'c={c_value}', params_part)
                    else:
                        if params_part.endswith('['):
                            params_part = params_part[:-1] + f'c={c_value}]'
                        elif params_part.endswith(']'):
                            params_part = params_part[:-1] + f',c={c_value}]'
                        else:
                            params_part = params_part + f'c={c_value}'
                    conversion_reminders.append(f"Java版{selector_var}[sort=random]已转换为基岩版@r[c={c_value}]")
                else:
                    params_part = re.sub(sort_pattern, '', params_part)
                    if limit_value:
                        params_part = re.sub(limit_pattern, '', params_part)
                    # 添加c参数
                    if re.search(r'c=[+-]?\d+', params_part):
                        params_part = re.sub(r'c=[+-]?\d+', f'c={c_value}', params_part)
                    else:
                        if params_part.endswith('['):
                            params_part = params_part[:-1] + f'c={c_value}]'
                        elif params_part.endswith(']'):
                            params_part = params_part[:-1] + f',c={c_value}]'
                        else:
                            params_part = params_part + f'c={c_value}'
                    conversion_reminders.append(f"Java版sort=random已转换为基岩版c={c_value}")
            else:
                params_part = re.sub(sort_pattern, '', params_part)
                conversion_reminders.append(f"Java版sort={sort_value}在基岩版中不支持，已移除")
        else:
            # 没有sort参数，只转换limit
            if limit_value:
                conversion_reminders.append(f"Java版limit={limit_value}参数已转换为基岩版c={limit_value}")
                conversion_reminders.append("limit只是限制数量，c当由近到远")
                params_part = re.sub(limit_pattern, f'c={limit_value}', params_part)
    
    # 分割参数，但要处理包含大括号的参数
    # 使用更智能的方法来分割参数，避免在{}内部分割
    params = []
    current_param = ""
    brace_count = 0
    in_param_value = False
    
    for char in params_part:
        if char == '{':
            brace_count += 1
            in_param_value = True
        elif char == '}':
            brace_count -= 1
            if brace_count == 0:
                in_param_value = False
        
        if char == ',' and brace_count == 0:
            # 在最外层遇到逗号，分割参数
            if current_param.strip():
                params.append(current_param.strip())
            current_param = ""
        else:
            current_param += char
    
    # 添加最后一个参数
    if current_param.strip():
        params.append(current_param.strip())
    
    # 调试：打印分割后的参数
    
    # 过滤参数并收集提醒信息
    filtered_params = []
    removed_params = []
    
    for param in params:
        if '=' in param:
            param_name = param.split('=')[0].strip()
            # 特殊处理haspermission参数，它包含大括号
            if param_name == 'haspermission' and target_version == 'java':
                removed_params.append(param_name)
                nbt_conversion_reminders.append(f"警告：基岩版{param_name}参数在Java版中没有对应的功能，已移除")
                continue  # 跳过此参数，不添加到filtered_params中
            
            # 特殊处理nbt参数
            if param_name == 'nbt':
                if target_version == 'bedrock':
                    # Java版的nbt参数在基岩版中不支持
                    removed_params.append('nbt')
                    nbt_conversion_reminders.append("警告：Java版nbt参数在基岩版中不支持，已尝试转换为hasitem参数，如果转换失败则已移除")
                else:
                    # 保留Java版的nbt参数
                    filtered_params.append(param)
            # 根据目标版本过滤参数
            elif target_version == 'java' and param_name in bedrock_specific_params:
                removed_params.append(param_name)
                if param_name == 'family':
                    nbt_conversion_reminders.append(f"警告：基岩版{param_name}参数在Java版中没有直接对应的功能，已移除。建议使用type参数指定实体类型作为替代")
                elif param_name == 'haspermission':
                    nbt_conversion_reminders.append(f"警告：基岩版{param_name}参数在Java版中没有对应的功能，已移除")
                elif param_name == 'has_property':
                    nbt_conversion_reminders.append(f"警告：基岩版{param_name}参数在Java版中没有对应的功能，已移除")
                else:
                    nbt_conversion_reminders.append(f"警告：基岩版{param_name}参数在Java版中不支持，已移除")
                # 不将此参数添加到filtered_params中，即跳过此参数
                continue  # 跳过此参数，不添加到filtered_params中
            elif target_version == 'bedrock' and param_name in java_specific_params:
                removed_params.append(param_name)
                if param_name == 'team':
                    nbt_conversion_reminders.append(f"警告：Java版{param_name}参数在基岩版中不支持，已移除。基岩版中没有队伍系统的直接对应功能")
                elif param_name == 'predicate':
                    nbt_conversion_reminders.append(f"警告：Java版{param_name}参数在基岩版中不支持，已移除。基岩版中没有谓词系统")
                elif param_name == 'advancements':
                    nbt_conversion_reminders.append(f"警告：Java版{param_name}参数在基岩版中不支持，已移除。基岩版中没有进度系统")
                else:
                    nbt_conversion_reminders.append(f"警告：Java版{param_name}参数在基岩版中不支持，已移除")
                # 不将此参数添加到filtered_params中，即跳过此参数
            else:
                filtered_params.append(param)
        else:
            # 没有等号的参数（可能是一些特殊参数）
            filtered_params.append(param)
    
    # 重构选择器
    if filtered_params:
        new_params_part = '[' + ','.join(filtered_params) + ']'
        new_selector = selector_var + new_params_part
    else:
        # 如果过滤后没有参数，只返回选择器变量
        new_selector = selector_var
    
    # 调试：打印过滤后的选择器
    
    # 将nbt_conversion_reminders合并到conversion_reminders中
    conversion_reminders.extend(nbt_conversion_reminders)
    
    # 清理多余的逗号和空括号
    new_selector = re.sub(r',,', ',', new_selector)
    new_selector = re.sub(r',\]', ']', new_selector)
    new_selector = re.sub(r'\[,', '[', new_selector)
    
    return new_selector, conversion_reminders

def process_range_values(params_part):
    """
    处理参数中的范围数值，提取第一个数字用于不支持范围的参数
    """
    import re
    
    # Java版的nbt参数不支持范围选择，需要提取第一个数字
    # 例如：nbt={Inventory:[{id:"minecraft:diamond",Count:3..}]}
    # 需要转换为：nbt={Inventory:[{id:"minecraft:diamond",Count:3b}]}
    
    def replace_range_in_nbt(match):
        nbt_content = match.group(1)
        # 处理nbt中的范围数值，改进正则表达式以处理嵌套结构
        # 查找类似 Count:3.. 或 Count:5..6 的模式
        
        # 使用栈来处理嵌套的大括号，确保不会错误处理嵌套结构中的范围
        def process_nbt_content(content):
            # 处理Count范围
            def replace_count_range(count_match):
                count_value = count_match.group(1)
                # 如果是范围，提取第一个数字
                if '..' in count_value:
                    range_parts = count_value.split('..')
                    try:
                        first_num = int(range_parts[0]) if range_parts[0] else 0
                        return f'Count:{first_num}b'
                    except ValueError:
                        return count_match.group(0)
                return count_match.group(0)
            
            # 处理其他可能的数值范围字段
            def replace_general_range(field_match):
                field_name = field_match.group(1)
                field_value = field_match.group(2)
                # 如果是范围，提取第一个数字
                if '..' in field_value:
                    range_parts = field_value.split('..')
                    try:
                        first_num = int(range_parts[0]) if range_parts[0] else 0
                        # 根据字段名决定后缀
                        suffix = 'b' if field_name in ['Count', 'Damage'] else ''
                        return f'{field_name}:{first_num}{suffix}'
                    except ValueError:
                        return field_match.group(0)
                return field_match.group(0)
            
            # 处理Count范围
            content = re.sub(r'Count:(\d*\.\.\d*)b', replace_count_range, content)
            content = re.sub(r'Count:(\d*\.\.\d*)', replace_count_range, content)
            
            # 处理其他数值范围
            content = re.sub(r'(\w+):(\d*\.\.\d*)', replace_general_range, content)
            
            return content
        
        # 处理nbt内容
        processed_content = process_nbt_content(nbt_content)
        
        return f'nbt={processed_content}'
    
    # 使用非递归正则表达式处理nbt参数中的范围数值
    # 支持有限层级的嵌套结构，避免使用Python不支持的(?R)语法
    nbt_pattern = r'nbt=(\{[^{}]*(?:\{[^{}]*\}[^{}]*)*\})'
    try:
        params_part = re.sub(nbt_pattern, replace_range_in_nbt, params_part, flags=re.DOTALL)
    except re.error:
        # 如果正则表达式失败，尝试更简单的模式
        try:
            # 简化的NBT匹配模式，只处理一层嵌套
            simple_nbt_pattern = r'nbt=(\{[^{}]*\})'
            params_part = re.sub(simple_nbt_pattern, replace_range_in_nbt, params_part)
        except re.error:
            # 如果仍然失败，跳过NBT范围处理，保持原参数不变
            pass
    
    return params_part

def parse_hasitem_simple(hasitem_content):
    """
    解析简单的hasitem参数并转换为nbt参数
    例如：hasitem={item=diamond,quantity=3..} -> nbt={Inventory:[{id:"minecraft:diamond"}]}
    注意：Java版NBT不需要Count值，有了反而会让检测失效
    """
    import re
    
    # 初始化提醒列表
    reminders = []
    
    # 解析hasitem参数
    params = {}
    # 分割参数，但要小心处理值中的逗号（例如在[]或{}中）
    parts = re.split(r',(?![^{}]*\})', hasitem_content)
    
    for part in parts:
        if '=' in part:
            key, value = part.split('=', 1)
            params[key.strip()] = value.strip()
    
    # 提取物品信息
    item_name = params.get('item', '').strip('"')
    quantity = params.get('quantity', '1..')
    location = params.get('location', None)
    slot = params.get('slot', None)
    
    # 解析数量范围 - 仍然解析但不用于NBT
    count_value = 1
    has_quantity = 'quantity' in params
    range_reminders = []
    if quantity:
        if '..' in quantity:
            # 根据要求：hasitem如果有数量范围则取中间值(整数)
            range_parts = quantity.split('..')
            try:
                if range_parts[0] and range_parts[1]:
                    # 两个数字都存在：取中间值
                    start = int(range_parts[0])
                    end = int(range_parts[1])
                    count_value = round((start + end) / 2)
                    # 添加中间值提醒
                    range_reminders.append(f"hasitem数量范围{range_parts[0]}..{range_parts[1]}取中间值{count_value}")
                elif range_parts[0]:
                    # 只有下限：使用下限值
                    count_value = int(range_parts[0])
                elif range_parts[1]:
                    # 只有上限：使用上限值
                    count_value = int(range_parts[1])
                else:
                    # 都没有，使用默认值
                    count_value = 1
            except ValueError:
                count_value = 1
        else:
            try:
                count_value = int(quantity)
            except ValueError:
                count_value = 1
    reminders.extend(range_reminders)
    
    # 构建nbt参数
    if item_name:
        # 添加minecraft:前缀（如果需要）
        if not item_name.startswith('minecraft:'):
            item_name = 'minecraft:' + item_name
        
        # 根据location和slot信息确定具体的NBT格式
        nbt_items = []
        
        # 处理位置信息
        if location and slot:
            # 有具体位置信息，尝试映射到NBT的Slot字段
            slot_num = None
            if location == 'slot.weapon.mainhand':
                slot_num = 0
            elif location == 'slot.weapon.offhand':
                slot_num = 1
            elif location == 'slot.hotbar':
                # 解析slot范围，如"0..2"，取第一个值
                if '..' in slot:
                    slot_num = int(slot.split('..')[0])
                else:
                    slot_num = int(slot)
            elif location == 'slot.inventory':
                # 解析slot范围，如"9..35"，取第一个值
                if '..' in slot:
                    slot_num = int(slot.split('..')[0])
                else:
                    slot_num = int(slot)
            
            # 构建带Slot信息的NBT项，不包含Count字段
            if slot_num is not None:
                nbt_items.append(f'{{id:"{item_name}",Slot:{slot_num}b}}')
            else:
                # 无法确定槽位，使用通用格式
                nbt_items.append(f'{{id:"{item_name}"}}')
        else:
            # 没有具体位置信息，使用通用格式
            nbt_items.append(f'{{id:"{item_name}"}}')
        
        # 构建完整的NBT内容
        nbt_content = f'{{Inventory:[{",".join(nbt_items)}]}}'
        
        # 添加转换提醒
        reminders.append(f"hasitem参数已转换为nbt格式，可能无法完全保留原意")
        reminders.append(f"注意：Java版NBT不需要Count值，hasitem的quantity参数未转换为NBT的Count字段")
        
        if location and slot:
            reminders.append(f"hasitem参数位置信息已转换为NBT Slot字段")
        
        return f'nbt={nbt_content}', reminders
    else:
        # 如果没有物品信息，返回None表示转换失败
        # 重要：返回None而不是原始hasitem，这样主转换函数可以决定如何处理
        return None, []

def parse_hasitem_complex(hasitem_content):
    """
    解析复杂的hasitem参数并转换为nbt参数
    例如：hasitem=[{item=diamond,quantity=3..},{item=stick,quantity=2..}] 
    -> nbt={Inventory:[{id:"minecraft:diamond"},{id:"minecraft:stick"}]}
    注意：Java版NBT不需要Count值，有了反而会让检测失效
    """
    import re
    
    # 解析数组中的每个对象
    # 简单处理：分割每个对象
    objects = []
    brace_count = 0
    current_obj = ""
    
    for char in hasitem_content + ',':
        if char == '{':
            brace_count += 1
            if brace_count == 1:
                current_obj = '{'
                continue
        elif char == '}':
            brace_count -= 1
            current_obj += char
            if brace_count == 0 and current_obj:
                objects.append(current_obj[1:-1])  # 移除首尾的{}
                continue
        if brace_count > 0:
            current_obj += char
    
    # 构建nbt内容
    nbt_items = []
    reminders = []
    for obj in objects:
        params = {}
        # 分割参数
        parts = re.split(r',(?![^{}]*\})', obj)
        for part in parts:
            if '=' in part:
                key, value = part.split('=', 1)
                params[key.strip()] = value.strip()
        
        # 提取物品信息
        item_name = params.get('item', '').strip('"')
        quantity = params.get('quantity', '1..')
        location = params.get('location', None)
        slot = params.get('slot', None)
        
        # 解析数量范围 - 根据需求取中间值（整数）
        count_value = 1
        has_quantity = 'quantity' in params
        range_reminders = []
        if quantity:
            if '..' in quantity:
                # 根据要求：hasitem如果有数量范围则取中间值(整数)
                range_parts = quantity.split('..')
                try:
                    if range_parts[0] and range_parts[1]:
                        # 两个数字都存在：取中间值
                        start = int(range_parts[0])
                        end = int(range_parts[1])
                        count_value = round((start + end) / 2)
                        # 添加中间值提醒
                        range_reminders.append(f"hasitem数量范围{range_parts[0]}..{range_parts[1]}取中间值{count_value}")
                    elif range_parts[0]:
                        # 只有下限：使用下限值
                        count_value = int(range_parts[0])
                    elif range_parts[1]:
                        # 只有上限：使用上限值
                        count_value = int(range_parts[1])
                    else:
                        # 都没有，使用默认值
                        count_value = 1
                except ValueError:
                    count_value = 1
            elif quantity.startswith('!'):
                # 处理反选情况
                count_value = 1
            else:
                try:
                    count_value = int(quantity)
                except ValueError:
                    count_value = 1
        reminders.extend(range_reminders)
        
        if item_name:
            # 添加minecraft:前缀（如果需要）
            if not item_name.startswith('minecraft:'):
                item_name = 'minecraft:' + item_name
            
            # 处理位置信息
            slot_num = None
            if location and slot:
                # 有具体位置信息，尝试映射到NBT的Slot字段
                if location == 'slot.weapon.mainhand':
                    slot_num = 0
                elif location == 'slot.weapon.offhand':
                    slot_num = 1
                elif location == 'slot.hotbar':
                    # 解析slot范围，如"0..2"，取第一个值
                    if '..' in slot:
                        slot_num = int(slot.split('..')[0])
                    else:
                        slot_num = int(slot)
                elif location == 'slot.inventory':
                    # 解析slot范围，如"9..35"，取第一个值
                    if '..' in slot:
                        slot_num = int(slot.split('..')[0])
                    else:
                        slot_num = int(slot)
            
            # 构建带Slot信息的NBT项，不包含Count字段
            if slot_num is not None:
                nbt_items.append(f'{{id:"{item_name}",Slot:{slot_num}b}}')
            else:
                # 没有具体位置信息，使用通用格式
                nbt_items.append(f'{{id:"{item_name}"}}')
    
    if nbt_items:
        nbt_content = '{Inventory:[' + ','.join(nbt_items) + ']}'
        # 添加关于NBT格式的说明
        reminders.append("hasitem参数已转换为nbt格式，可能无法完全保留原意")
        reminders.append("注意：Java版NBT不需要Count值，hasitem的quantity参数未转换为NBT的Count字段")
        return f'nbt={nbt_content}', reminders
    else:
        # 如果没有物品信息，返回None表示转换失败
        return None, []

def convert_colors_to_bedrock(text):
    """将Java版颜色代码转换为基岩版"""
    import re
    result = text
    
    # 将Java版颜色名称转换为基岩版颜色代码
    for java_color, code in JAVA_COLORS.items():
        # 替换颜色名称格式
        result = result.replace(f'{{"color":"{java_color}"}}', f'{code}')
    
    # 将基岩版特有颜色代码转换为相似的Java版颜色代码
    for bedrock_code, replacement in BEDROCK_COLORS.items():
        result = result.replace(bedrock_code, replacement)
    
    # 处理Java版颜色代码到基岩版颜色代码的转换
    for java_code in JAVA_COLORS.values():
        # Java版颜色代码保持不变，因为基岩版也支持相同的颜色代码
        pass
    
    # 使用TEXT_COLOR_CODES映射处理所有颜色代码
    # 按长度降序排列，确保较长的代码先被处理
    sorted_codes = sorted(TEXT_COLOR_CODES.items(), key=lambda x: len(x[0]), reverse=True)
    for color_code, replacement in sorted_codes:
        result = result.replace(color_code, replacement)
    
    return result

def convert_text_to_java(text, m_n_handling="color"):
    """将文本转换为Java版tellraw格式，支持复杂颜色格式"""
    import re
    
    # 使用正则表达式解析颜色代码和文本
    # 匹配§+字符的模式，然后处理后续的文本
    result = parse_minecraft_formatting(text, m_n_handling)
    
    return result

def parse_minecraft_formatting(text, m_n_handling="color"):
    """解析Minecraft颜色和格式代码，按Java版逻辑合并相同格式的文本"""
    import re
    
    # 使用正则表达式找到所有格式代码和文本部分
    tokens = []
    i = 0
    while i < len(text):
        if text[i] == '§' and i + 1 < len(text):
            code = text[i:i+2]
            tokens.append(('format_code', code))
            i += 2
        else:
            # 收集非§开头的文本
            start = i
            while i < len(text) and text[i] != '§':
                i += 1
            if i > start:
                tokens.append(('text', text[start:i]))
    
    # 构建结果
    result = {"text": ""}
    current_format = {}
    extra_parts = []
    
    # 按Java版逻辑处理：相同颜色相同字体形式的文本放在一起处理
    # 特别处理空格：空格放在前面一坨处理，如果开头就是空格，与后面那一坨放在一起处理
    for token_type, token_value in tokens:
        if token_type == 'format_code':
            code = token_value
            # 颜色代码
            if code[1] in '0123456789abcdefg hijpqs tuv':
                # 颜色代码
                if code == '§0': 
                    current_format['color'] = 'black'
                elif code == '§1': 
                    current_format['color'] = 'dark_blue'
                elif code == '§2': 
                    current_format['color'] = 'dark_green'
                elif code == '§3': 
                    current_format['color'] = 'dark_aqua'
                elif code == '§4': 
                    current_format['color'] = 'dark_red'
                elif code == '§5': 
                    current_format['color'] = 'dark_purple'
                elif code == '§6': 
                    current_format['color'] = 'gold'
                elif code == '§7': 
                    current_format['color'] = 'gray'
                elif code == '§8': 
                    current_format['color'] = 'dark_gray'
                elif code == '§9': 
                    current_format['color'] = 'blue'
                elif code == '§a': 
                    current_format['color'] = 'green'
                elif code == '§b': 
                    current_format['color'] = 'aqua'
                elif code == '§c': 
                    current_format['color'] = 'red'
                elif code == '§d': 
                    current_format['color'] = 'light_purple'
                elif code == '§e': 
                    current_format['color'] = 'yellow'
                elif code == '§f': 
                    current_format['color'] = 'white'
                # 基岩版颜色代码
                elif code == '§g':
                    current_format['color'] = 'gold'  # minecoin_gold
                elif code == '§h':
                    current_format['color'] = 'white'  # material_quartz
                elif code == '§i':
                    current_format['color'] = 'gray'  # material_iron
                elif code == '§j':
                    current_format['color'] = 'dark_gray'  # material_netherite
                elif code == '§m':
                    if m_n_handling == "color":
                        current_format['color'] = 'dark_red'  # material_redstone
                    else:
                        # 在Java版中，§m是删除线格式
                        current_format['strikethrough'] = True
                elif code == '§n':
                    if m_n_handling == "color":
                        current_format['color'] = 'gold'  # material_copper
                    else:
                        # 在Java版中，§n是下划线格式
                        current_format['underlined'] = True
                elif code == '§p':
                    current_format['color'] = 'gold'  # material_gold
                elif code == '§q':
                    current_format['color'] = 'green'  # material_emerald
                elif code == '§s':
                    current_format['color'] = 'aqua'  # material_diamond
                elif code == '§t':
                    current_format['color'] = 'dark_blue'  # material_lapis
                elif code == '§u':
                    current_format['color'] = 'light_purple'  # material_amethyst
                elif code == '§v':
                    current_format['color'] = 'gold'  # material_resin
            # 格式代码
            elif code[1] in 'klmnor':
                if code == '§k':
                    current_format['obfuscated'] = True  # 随机字符（混淆）
                elif code == '§l':
                    current_format['bold'] = True  # 粗体
                elif code == '§m' and m_n_handling != "color":
                    current_format['strikethrough'] = True  # 删除线
                elif code == '§n' and m_n_handling != "color":
                    current_format['underlined'] = True  # 下划线
                elif code == '§o':
                    current_format['italic'] = True  # 斜体
                elif code == '§r':
                    # 重置所有格式
                    current_format = {}
        else:  # token_type == 'text'
            text_content = token_value
            
            # 按Java版逻辑：将相同颜色和格式的文本组合在一起
            # 检查是否可以与前一部分合并
            if extra_parts:
                # 检查最后一个部分的格式是否与当前格式相同
                last_part = extra_parts[-1]
                
                # 检查格式是否完全相同（包括所有格式属性）
                formats_match = True
                all_format_keys = set(current_format.keys()) | set({k: v for k, v in last_part.items() if k != 'text'}.keys())
                
                for key in all_format_keys:
                    if key == 'text':
                        continue
                    if current_format.get(key) != last_part.get(key):
                        formats_match = False
                        break
                        
                if formats_match:
                    # 格式相同，合并文本
                    extra_parts[-1]["text"] += text_content
                else:
                    # 格式不同，添加新部分
                    new_part = {"text": text_content}
                    new_part.update(current_format)
                    extra_parts.append(new_part)
            else:
                # 检查是否可以与主文本合并
                if result.get("text"):
                    # 检查主文本格式是否与当前格式相同
                    formats_match = True
                    all_format_keys = set(current_format.keys()) | set({k: v for k, v in result.items() if k != 'text'}.keys())
                    
                    for key in all_format_keys:
                        if key == 'text':
                            continue
                        if current_format.get(key) != result.get(key):
                            formats_match = False
                            break
                            
                    if formats_match:
                        # 格式相同，合并到主文本
                        result["text"] += text_content
                    else:
                        # 格式不同，添加到extra部分
                        new_part = {"text": text_content}
                        new_part.update(current_format)
                        extra_parts.append(new_part)
                else:
                    # 第一部分，设置为主文本
                    result["text"] = text_content
                    result.update(current_format)
    
    # 添加extra部分（如果有的话）
    if extra_parts:
        result["extra"] = extra_parts
    
    return result

def convert_text_to_bedrock(text, m_n_handling="color"):
    """将文本转换为基岩版tellraw格式"""
    # 首先处理所有基岩版特有颜色代码
    processed_text = text
    for bedrock_code, replacement in BEDROCK_COLORS.items():
        processed_text = processed_text.replace(bedrock_code, replacement)
    
    # 使用TEXT_COLOR_CODES处理所有颜色代码
    # 按长度降序排列，确保较长的代码先被处理
    sorted_codes = sorted(TEXT_COLOR_CODES.items(), key=lambda x: len(x[0]), reverse=True)
    for color_code, replacement in sorted_codes:
        processed_text = processed_text.replace(color_code, replacement)
    
    # 根据m_n_handling参数处理§m§n代码（如果需要）
    if m_n_handling == "font":
        # 基岩版使用默认颜色代码方式
        # 将基岩版特有颜色代码转换为相似的Java版颜色代码
        for bedrock_code in BEDROCK_COLORS:
            if bedrock_code in processed_text:
                processed_text = processed_text.replace(bedrock_code, BEDROCK_COLORS[bedrock_code])
    elif m_n_handling == "color":
        # 同样使用颜色代码方式
        for bedrock_code in BEDROCK_COLORS:
            if bedrock_code in processed_text:
                processed_text = processed_text.replace(bedrock_code, BEDROCK_COLORS[bedrock_code])
    
    # 返回rawtext格式
    return {"rawtext": [{"text": processed_text}]}

def generate_tellraw_commands(selector, message, m_n_handling="none"):
    """生成Java版和基岩版的tellraw命令"""
    # 检测选择器类型
    selector_type = detect_selector_type(selector)
    
    # 将基岩版选择器转换为Java版（如果需要）
    java_selector, was_converted, java_selector_reminders = convert_bedrock_selector_to_java(selector)
    
    # 转换gamemode/m参数并收集提醒
    java_selector_converted, bedrock_selector_converted, java_gamemode_reminders, bedrock_gamemode_reminders = convert_gamemode_parameters(java_selector, selector)
    
    # 处理参数转换 - 分别为Java版和基岩版创建不同的参数
    import re
    
    # 提取原始参数信息（从原始选择器提取，用于判断原始输入类型）
    sort_pattern = r'\bsort=([^,\]]+)'
    limit_pattern = r'\blimit=([+-]?\d+)'
    
    sort_match = re.search(sort_pattern, selector)
    limit_match = re.search(limit_pattern, selector)
    
    original_sort_value = sort_match.group(1) if sort_match else None
    original_limit_value = limit_match.group(1) if limit_match else None
    
    # 提取基岩版c参数
    c_pattern = r'\bc=([+-]?\d+)'
    c_match = re.search(c_pattern, selector)
    original_c_value = c_match.group(1) if c_match else None
    
    # 提取基岩版level相关参数 (l, lm) 
    l_pattern = r'\bl=([+-]?\d+)'  # level上限
    lm_pattern = r'\blm=([+-]?\d+)'  # level下限
    level_pattern = r'\blevel=([^,\]]+)'  # Java版level参数
    l_match = re.search(l_pattern, selector)
    lm_match = re.search(lm_pattern, selector)
    level_match = re.search(level_pattern, selector)
    original_l_value = l_match.group(1) if l_match else None
    original_lm_value = lm_match.group(1) if lm_match else None
    original_level_value = level_match.group(1) if level_match else None
    
    # 提取基岩版距离相关参数 (r, rm)
    r_pattern = r'\br=([^,\]]+)'  # r参数（最大距离）
    rm_pattern = r'\brm=([^,\]]+)'  # rm参数（最小距离）
    # Java版distance参数
    distance_pattern = r'\bdistance=([^,\]]+)'  # distance参数
    
    r_match = re.search(r_pattern, selector)
    rm_match = re.search(rm_pattern, selector)
    distance_match = re.search(distance_pattern, selector)
    
    original_r_value = r_match.group(1) if r_match else None
    original_rm_value = rm_match.group(1) if rm_match else None
    original_distance_value = distance_match.group(1) if distance_match else None
    
    # 提取旋转角度相关参数
    # Java版旋转角度参数
    x_rotation_pattern = r'\bx_rotation=([^,\]]+)'  # x_rotation参数（垂直旋转）
    y_rotation_pattern = r'\by_rotation=([^,\]]+)'  # y_rotation参数（水平旋转）
    # 基岩版旋转角度参数
    rx_pattern = r'\brx=([^,\]]+)'  # rx参数（垂直旋转最大值）
    rxm_pattern = r'\brxm=([^,\]]+)'  # rxm参数（垂直旋转最小值）
    ry_pattern = r'\bry=([^,\]]+)'  # ry参数（水平旋转最大值）
    rym_pattern = r'\brym=([^,\]]+)'  # rym参数（水平旋转最小值）
    
    x_rotation_match = re.search(x_rotation_pattern, selector)
    y_rotation_match = re.search(y_rotation_pattern, selector)
    rx_match = re.search(rx_pattern, selector)
    rxm_match = re.search(rxm_pattern, selector)
    ry_match = re.search(ry_pattern, selector)
    rym_match = re.search(rym_pattern, selector)
    
    original_x_rotation_value = x_rotation_match.group(1) if x_rotation_match else None
    original_y_rotation_value = y_rotation_match.group(1) if y_rotation_match else None
    original_rx_value = rx_match.group(1) if rx_match else None
    original_rxm_value = rxm_match.group(1) if rxm_match else None
    original_ry_value = ry_match.group(1) if ry_match else None
    original_rym_value = rym_match.group(1) if rym_match else None
    
    
    
    # 根据原始选择器类型选择转换后的选择器作为基础
    # 新逻辑：检测出的版本直接套用原始格式
    if selector_type == 'bedrock':
        # 对于基岩版输入：
        # 基岩版输出直接使用原始输入
        bedrock_selector_final = selector
        # Java版输出需要将基岩版参数转换为Java版
        # 使用convert_bedrock_selector_to_java的返回结果，确保选择器变量也被正确转换
        java_selector_final = java_selector if was_converted else bedrock_selector_converted
    else:
        # 对于Java版输入：
        # Java版输出直接使用原始输入
        java_selector_final = selector
        # 基岩版输出需要将Java版参数转换为基岩版
        # bedrock_selector_converted是将原始selector(Java版)中的m转为gamemode的结果（基本不变）
        # 所以：
        # Java版输出应该使用原始的Java版输入
        # 基岩版输出应该使用java_selector_converted（Java版参数转基岩版）
        java_selector_final = java_selector  # 保持原始Java版输入
        bedrock_selector_final = java_selector_converted  # 使用Java版转基岩版的结果
    
    # 处理Java版选择器参数转换
    java_reminders = []
    if '[' in java_selector_final and ']' in java_selector_final:
        selector_var = java_selector_final.split('[')[0]
        params_part = java_selector_final[java_selector_final.find('['):]
        
        # 如果原始选择器是基岩版输入（包含c参数、r/rm参数等），需要处理基岩版参数到Java版的转换
        # 但是，如果java_selector_final已经是bedrock_selector_converted（即已经转换过一次），则不需要再次转换
        is_bedrock_input = selector_type == 'bedrock'
        already_converted = (selector_type == 'bedrock' and java_selector_final == bedrock_selector_converted)
        
        if is_bedrock_input and not already_converted and (original_l_value or original_lm_value or original_r_value or original_rm_value or original_rx_value or original_rxm_value or original_ry_value or original_rym_value):
            # 注意：不在这里处理c参数转换，因为它已经在convert_limit_c_between_versions函数中处理了
            # 这样可以避免重复的提醒信息
            
            # 处理基岩版l/lm参数到Java版level参数的转换
            if original_l_value or original_lm_value:
                # 先检查是否已存在level参数
                existing_level_match = re.search(r'\blevel=([^,\]]+)', params_part)
                existing_level_value = existing_level_match.group(1) if existing_level_match else None
                
                # 移除基岩版的l和lm参数（这些应该已经在java_selector_converted中被转换为level参数了）
                # 但是我们仍然需要移除它们以确保不会保留原始参数
                if original_l_value:
                    params_part = re.sub(r'\bl=' + re.escape(original_l_value) + r'(?=[,\]])', '', params_part)
                if original_lm_value:
                    params_part = re.sub(r'\blm=' + re.escape(original_lm_value) + r'(?=[,\]])', '', params_part)
                
                # 构建Java版level参数
                level_param = None
                if original_l_value and original_lm_value:
                    # l=上限, lm=下限 -> level=下限..上限
                    if original_l_value == original_lm_value:
                        # 如果l和lm相等，使用单个数字而非范围
                        level_param = f'level={original_l_value}'
                        java_reminders.append(f"基岩版l/lm参数（相等值{original_l_value}）已转换为Java版level={original_l_value}")
                    else:
                        level_param = f'level={original_lm_value}..{original_l_value}'
                        java_reminders.append(f"基岩版l/lm参数已转换为Java版{level_param}")
                elif original_l_value:
                    # 只有l -> level=..上限
                    # 检查是否已存在level参数，如果存在且已经是..开头，则不需要修改
                    if existing_level_value and existing_level_value.startswith('..'):
                        # 已有合适的level参数，不需要修改
                        level_param = None
                    else:
                        level_param = f'level=..{original_l_value}'
                        java_reminders.append(f"基岩版l参数已转换为Java版{level_param}")
                elif original_lm_value:
                    # 只有lm -> level=下限..
                    # 检查是否已存在level参数，如果存在且已经是..结尾，则不需要修改
                    if existing_level_value and existing_level_value.endswith('..'):
                        # 已有合适的level参数，不需要修改
                        level_param = None
                    else:
                        level_param = f'level={original_lm_value}..'
                        java_reminders.append(f"基岩版lm参数已转换为Java版{level_param}")
                
                if level_param:
                    # 先移除已存在的level参数，避免重复
                    params_part = re.sub(r'\blevel=[^,\]]+', '', params_part)
                    
                    # 在方括号内添加level参数
                    if params_part.endswith('['):
                        params_part = params_part[:-1] + f'{level_param}]'
                    elif params_part.endswith(']'):
                        params_part = params_part[:-1] + f',{level_param}]'
                    else:
                        params_part = params_part + f',{level_param}'
            
            # 处理基岩版r/rm参数到Java版distance参数的转换
            if original_r_value or original_rm_value:
                # 先检查是否已存在distance参数
                existing_distance_match = re.search(r'\bdistance=([^,\]]+)', params_part)
                existing_distance_value = existing_distance_match.group(1) if existing_distance_match else None
                
                # 移除基岩版的r和rm参数
                if original_r_value:
                    params_part = re.sub(r'\br=' + re.escape(original_r_value) + r'(?=[,\]])', '', params_part)
                if original_rm_value:
                    params_part = re.sub(r'\brm=' + re.escape(original_rm_value) + r'(?=[,\]])', '', params_part)
                
                # 构建Java版distance参数
                distance_param = None
                if original_r_value and original_rm_value:
                    # r=最大距离, rm=最小距离 -> distance=最小距离..最大距离
                    if original_r_value == original_rm_value:
                        # 如果r和rm相等，使用单个数字而非范围
                        distance_param = f'distance={original_rm_value}'
                        java_reminders.append(f"基岩版r/rm参数（相等值{original_r_value}）已转换为Java版distance={original_r_value}")
                    else:
                        distance_param = f'distance={original_rm_value}..{original_r_value}'
                        java_reminders.append(f"基岩版r/rm参数已转换为Java版{distance_param}")
                elif original_r_value:
                    # 只有r -> distance=..最大距离
                    # 检查是否已存在distance参数，如果存在且已经是..开头，则不需要修改
                    if existing_distance_value and existing_distance_value.startswith('..'):
                        # 已有合适的distance参数，不需要修改
                        distance_param = None
                    else:
                        distance_param = f'distance=..{original_r_value}'
                        java_reminders.append(f"基岩版r参数已转换为Java版{distance_param}")
                elif original_rm_value:
                    # 只有rm -> distance=最小距离..
                    # 检查是否已存在distance参数，如果存在且已经是..结尾，则不需要修改
                    if existing_distance_value and existing_distance_value.endswith('..'):
                        # 已有合适的distance参数，不需要修改
                        distance_param = None
                    else:
                        distance_param = f'distance={original_rm_value}..'
                        java_reminders.append(f"基岩版rm参数已转换为Java版{distance_param}")
                
                if distance_param:
                    # 先移除已存在的distance参数，避免重复
                    params_part = re.sub(r'\bdistance=[^,\]]+', '', params_part)
                    
                    # 在方括号内添加distance参数
                    if params_part.endswith('['):
                        params_part = params_part[:-1] + f'{distance_param}]'
                    elif params_part.endswith(']'):
                        params_part = params_part[:-1] + f',{distance_param}]'
                    else:
                        params_part = params_part + f',{distance_param}'
            
            # 处理基岩版rx/rxm参数到Java版x_rotation参数的转换
            if original_rx_value or original_rxm_value:
                # 先检查是否已存在x_rotation参数
                existing_x_rotation_match = re.search(r'\bx_rotation=([^,\]]+)', params_part)
                existing_x_rotation_value = existing_x_rotation_match.group(1) if existing_x_rotation_match else None
                
                # 移除基岩版的rx和rxm参数
                if original_rx_value:
                    params_part = re.sub(r'\brx=' + re.escape(original_rx_value) + r'(?=[,\]])', '', params_part)
                if original_rxm_value:
                    params_part = re.sub(r'\brxm=' + re.escape(original_rxm_value) + r'(?=[,\]])', '', params_part)
                
                # 构建Java版x_rotation参数
                x_rotation_param = None
                if original_rx_value and original_rxm_value:
                    # rx=最大值, rxm=最小值 -> x_rotation=最小值..最大值
                    if original_rx_value == original_rxm_value:
                        # 如果rx和rxm相等，使用单个数字而非范围
                        x_rotation_param = f'x_rotation={original_rxm_value}'
                        java_reminders.append(f"基岩版rx/rxm参数（相等值{original_rx_value}）已转换为Java版x_rotation={original_rx_value}")
                    else:
                        x_rotation_param = f'x_rotation={original_rxm_value}..{original_rx_value}'
                        java_reminders.append(f"基岩版rx/rxm参数已转换为Java版{x_rotation_param}")
                elif original_rx_value:
                    # 只有rx -> x_rotation=..最大值
                    # 检查是否已存在x_rotation参数，如果存在且已经是..开头，则不需要修改
                    if existing_x_rotation_value and existing_x_rotation_value.startswith('..'):
                        # 已有合适的x_rotation参数，不需要修改
                        x_rotation_param = None
                    else:
                        x_rotation_param = f'x_rotation=..{original_rx_value}'
                        java_reminders.append(f"基岩版rx参数已转换为Java版{x_rotation_param}")
                elif original_rxm_value:
                    # 只有rxm -> x_rotation=最小值..
                    # 检查是否已存在x_rotation参数，如果存在且已经是..结尾，则不需要修改
                    if existing_x_rotation_value and existing_x_rotation_value.endswith('..'):
                        # 已有合适的x_rotation参数，不需要修改
                        x_rotation_param = None
                    else:
                        x_rotation_param = f'x_rotation={original_rxm_value}..'
                        java_reminders.append(f"基岩版rxm参数已转换为Java版{x_rotation_param}")
                
                if x_rotation_param:
                    # 先移除已存在的x_rotation参数，避免重复
                    params_part = re.sub(r'\bx_rotation=[^,\]]+', '', params_part)
                    
                    # 在方括号内添加x_rotation参数
                    if params_part.endswith('['):
                        params_part = params_part[:-1] + f'{x_rotation_param}]'
                    elif params_part.endswith(']'):
                        params_part = params_part[:-1] + f',{x_rotation_param}]'
                    else:
                        params_part = params_part + f',{x_rotation_param}'
            
            # 处理基岩版ry/rym参数到Java版y_rotation参数的转换
            if original_ry_value or original_rym_value:
                # 先检查是否已存在y_rotation参数
                existing_y_rotation_match = re.search(r'\by_rotation=([^,\]]+)', params_part)
                existing_y_rotation_value = existing_y_rotation_match.group(1) if existing_y_rotation_match else None
                
                # 移除基岩版的ry和rym参数
                if original_ry_value:
                    params_part = re.sub(r'\bry=' + re.escape(original_ry_value) + r'(?=[,\]])', '', params_part)
                if original_rym_value:
                    params_part = re.sub(r'\brym=' + re.escape(original_rym_value) + r'(?=[,\]])', '', params_part)
                
                # 构建Java版y_rotation参数
                y_rotation_param = None
                if original_ry_value and original_rym_value:
                    # ry=最大值, rym=最小值 -> y_rotation=最小值..最大值
                    if original_ry_value == original_rym_value:
                        # 如果ry和rym相等，使用单个数字而非范围
                        y_rotation_param = f'y_rotation={original_rym_value}'
                        java_reminders.append(f"基岩版ry/rym参数（相等值{original_ry_value}）已转换为Java版y_rotation={original_ry_value}")
                    else:
                        y_rotation_param = f'y_rotation={original_rym_value}..{original_ry_value}'
                        java_reminders.append(f"基岩版ry/rym参数已转换为Java版{y_rotation_param}")
                elif original_ry_value:
                    # 只有ry -> y_rotation=..最大值
                    # 检查是否已存在y_rotation参数，如果存在且已经是..开头，则不需要修改
                    if existing_y_rotation_value and existing_y_rotation_value.startswith('..'):
                        # 已有合适的y_rotation参数，不需要修改
                        y_rotation_param = None
                    else:
                        y_rotation_param = f'y_rotation=..{original_ry_value}'
                        java_reminders.append(f"基岩版ry参数已转换为Java版{y_rotation_param}")
                elif original_rym_value:
                    # 只有rym -> y_rotation=最小值..
                    # 检查是否已存在y_rotation参数，如果存在且已经是..结尾，则不需要修改
                    if existing_y_rotation_value and existing_y_rotation_value.endswith('..'):
                        # 已有合适的y_rotation参数，不需要修改
                        y_rotation_param = None
                    else:
                        y_rotation_param = f'y_rotation={original_rym_value}..'
                        java_reminders.append(f"基岩版rym参数已转换为Java版{y_rotation_param}")
                
                if y_rotation_param:
                    # 先移除已存在的y_rotation参数，避免重复
                    params_part = re.sub(r'\by_rotation=[^,\]]+', '', params_part)
                    
                    # 在方括号内添加y_rotation参数
                    if params_part.endswith('['):
                        params_part = params_part[:-1] + f'{y_rotation_param}]'
                    elif params_part.endswith(']'):
                        params_part = params_part[:-1] + f',{y_rotation_param}]'
                    else:
                        params_part = params_part + f',{y_rotation_param}'
            
            else:
                # 如果原始选择器是Java版输入，保留原始参数
                if original_sort_value:
                    # Java版保留sort参数，不需要对选择器进行转换
                    java_reminders.append(f"Java版sort={original_sort_value}保留")
        
        # 清理多余的逗号
        params_part = re.sub(r',+', ',', params_part)
        params_part = re.sub(r'\[,', '[', params_part)
        params_part = re.sub(r',\]', ']', params_part)
        params_part = re.sub(r'\[\]', '', params_part)  # 移除空方括号
        
        java_selector_final = selector_var + params_part
    else:
        java_selector_final = java_selector_converted
    
    # 处理基岩版选择器
    bedrock_reminders = []
    if '[' in bedrock_selector_final and ']' in bedrock_selector_final:
        # 调试：打印处理前的基岩版选择器
        selector_var = bedrock_selector_final.split('[')[0]
        params_part = bedrock_selector_final[bedrock_selector_final.find('['):]
        
        # 如果原始选择器是Java版（包含sort参数或其他Java版参数），需要处理sort参数到c参数的转换等
        # 检查是否需要处理Java版特有参数到基岩版参数的转换
        needs_java_to_bedrock_conversion = (
            original_distance_value or 
            original_x_rotation_value or original_y_rotation_value or 
            original_level_value or 
            original_limit_value or original_sort_value
        )
        
        if needs_java_to_bedrock_conversion:
            # 处理Java版distance参数到基岩版r/rm参数的转换
            if original_distance_value:
                # 先检查是否已存在r和rm参数
                existing_r_match = re.search(r'\br=([^,\]]+)', params_part)
                existing_r_value = existing_r_match.group(1) if existing_r_match else None
                existing_rm_match = re.search(r'\brm=([^,\]]+)', params_part)
                existing_rm_value = existing_rm_match.group(1) if existing_rm_match else None
                
                # 移除Java版distance参数
                params_part = re.sub(distance_pattern, '', params_part)
                
                # 解析distance值
                if '..' in original_distance_value:
                    parts = original_distance_value.split('..')
                    if parts[0] and parts[1]:
                        # 有上下限：5..10 -> rm=5,r=10
                        rm_val = parts[0]
                        r_val = parts[1]
                        # 检查是否已存在合适的r和rm参数
                        if existing_r_value and existing_rm_value:
                            # 已有r和rm参数，不需要修改
                            pass
                        else:
                            # 移除已存在的r和rm参数，避免重复
                            params_part = re.sub(r'\br=[^,\]]+', '', params_part)
                            params_part = re.sub(r'\brm=[^,\]]+', '', params_part)
                            
                            # 添加r和rm参数
                            if params_part.endswith('['):
                                params_part = params_part[:-1] + f'rm={rm_val},r={r_val}]'
                            elif params_part.endswith(']'):
                                params_part = params_part[:-1] + f',rm={rm_val},r={r_val}]'
                            else:
                                params_part = params_part + f',rm={rm_val},r={r_val}'
                            bedrock_reminders.append(f"Java版distance={original_distance_value}参数已转换为基岩版rm={rm_val},r={r_val}")
                    elif parts[0]:
                        # 只有下限：5.. -> rm=5
                        rm_val = parts[0]
                        # 检查是否已存在合适的rm参数
                        if existing_rm_value:
                            # 已有rm参数，不需要修改
                            pass
                        else:
                            # 移除已存在的rm参数，避免重复
                            params_part = re.sub(r'\brm=[^,\]]+', '', params_part)
                            
                            # 添加rm参数
                            if params_part.endswith('['):
                                params_part = params_part[:-1] + f'rm={rm_val}]'
                            elif params_part.endswith(']'):
                                params_part = params_part[:-1] + f',rm={rm_val}]'
                            else:
                                params_part = params_part + f',rm={rm_val}'
                            bedrock_reminders.append(f"Java版distance={original_distance_value}参数已转换为基岩版rm={rm_val}")
                    elif parts[1]:
                        # 只有上限：..10 -> r=10
                        r_val = parts[1]
                        # 检查是否已存在合适的r参数
                        if existing_r_value:
                            # 已有r参数，不需要修改
                            pass
                        else:
                            # 移除已存在的r参数，避免重复
                            params_part = re.sub(r'\br=[^,\]]+', '', params_part)
                            
                            # 添加r参数
                            if params_part.endswith('['):
                                params_part = params_part[:-1] + f'r={r_val}]'
                            elif params_part.endswith(']'):
                                params_part = params_part[:-1] + f',r={r_val}]'
                            else:
                                params_part = params_part + f',r={r_val}'
                            bedrock_reminders.append(f"Java版distance={original_distance_value}参数已转换为基岩版r={r_val}")
                else:
                                # 单个值：10 -> rm=10,r=10（精确匹配）
                                rm_val = original_distance_value
                                r_val = original_distance_value
                                # 检查是否已存在合适的r和rm参数
                                if existing_r_value and existing_rm_value:
                                    # 已有r和rm参数，不需要修改
                                    pass
                                else:
                                    # 移除已存在的r和rm参数，避免重复
                                    params_part = re.sub(r'\br=[^,\]]+', '', params_part)
                                    params_part = re.sub(r'\brm=[^,\]]+', '', params_part)
                                    
                                    # 添加r和rm参数
                                    if params_part.endswith('['):
                                        params_part = params_part[:-1] + f'rm={rm_val},r={r_val}]'
                                    elif params_part.endswith(']'):
                                        params_part = params_part[:-1] + f',rm={rm_val},r={r_val}]'
                                    else:
                                        params_part = params_part + f',rm={rm_val},r={r_val}'
                                    bedrock_reminders.append(f"Java版distance={original_distance_value}参数已转换为基岩版rm={rm_val},r={r_val}")            
            # 处理Java版x_rotation参数到基岩版rx/rxm参数的转换
            if original_x_rotation_value:
                # 先检查是否已存在rx和rxm参数
                existing_rx_match = re.search(r'\brx=([^,\]]+)', params_part)
                existing_rx_value = existing_rx_match.group(1) if existing_rx_match else None
                existing_rxm_match = re.search(r'\brxm=([^,\]]+)', params_part)
                existing_rxm_value = existing_rxm_match.group(1) if existing_rxm_match else None
                
                # 移除Java版x_rotation参数
                params_part = re.sub(x_rotation_pattern, '', params_part)
                
                # 解析x_rotation值
                if '..' in original_x_rotation_value:
                    parts = original_x_rotation_value.split('..')
                    if parts[0] and parts[1]:
                        # 有上下限：-45..45 -> rxm=-45,rx=45
                        rxm_val = parts[0]
                        rx_val = parts[1]
                        # 检查是否已存在合适的rx和rxm参数
                        if existing_rx_value and existing_rxm_value:
                            # 已有rx和rxm参数，不需要修改
                            pass
                        else:
                            # 移除已存在的rx和rxm参数，避免重复
                            params_part = re.sub(r'\brx=[^,\]]+', '', params_part)
                            params_part = re.sub(r'\brxm=[^,\]]+', '', params_part)
                            
                            # 添加rx和rxm参数
                            if params_part.endswith('['):
                                params_part = params_part[:-1] + f'rxm={rxm_val},rx={rx_val}]'
                            elif params_part.endswith(']'):
                                params_part = params_part[:-1] + f',rxm={rxm_val},rx={rx_val}]'
                            else:
                                params_part = params_part + f',rxm={rxm_val},rx={rx_val}'
                            bedrock_reminders.append(f"Java版x_rotation={original_x_rotation_value}参数已转换为基岩版rxm={rxm_val},rx={rx_val}")
                    elif parts[0]:
                        # 只有下限：-45.. -> rxm=-45
                        rxm_val = parts[0]
                        # 检查是否已存在合适的rxm参数
                        if existing_rxm_value:
                            # 已有rxm参数，不需要修改
                            pass
                        else:
                            # 移除已存在的rxm参数，避免重复
                            params_part = re.sub(r'\brxm=[^,\]]+', '', params_part)
                            
                            # 添加rxm参数
                            if params_part.endswith('['):
                                params_part = params_part[:-1] + f'rxm={rxm_val}]'
                            elif params_part.endswith(']'):
                                params_part = params_part[:-1] + f',rxm={rxm_val}]'
                            else:
                                params_part = params_part + f',rxm={rxm_val}'
                            bedrock_reminders.append(f"Java版x_rotation={original_x_rotation_value}参数已转换为基岩版rxm={rxm_val}")
                    elif parts[1]:
                        # 只有上限：..45 -> rx=45
                        rx_val = parts[1]
                        # 检查是否已存在合适的rx参数
                        if existing_rx_value:
                            # 已有rx参数，不需要修改
                            pass
                        else:
                            # 移除已存在的rx参数，避免重复
                            params_part = re.sub(r'\brx=[^,\]]+', '', params_part)
                            
                            # 添加rx参数
                            if params_part.endswith('['):
                                params_part = params_part[:-1] + f'rx={rx_val}]'
                            elif params_part.endswith(']'):
                                params_part = params_part[:-1] + f',rx={rx_val}]'
                            else:
                                params_part = params_part + f',rx={rx_val}'
                            bedrock_reminders.append(f"Java版x_rotation={original_x_rotation_value}参数已转换为基岩版rx={rx_val}")
                else:
                                # 单个值：45 -> rxm=45,rx=45（精确匹配）
                                rxm_val = original_x_rotation_value
                                rx_val = original_x_rotation_value
                                # 检查是否已存在合适的rx和rxm参数
                                if existing_rx_value and existing_rxm_value:
                                    # 已有rx和rxm参数，不需要修改
                                    pass
                                else:
                                    # 移除已存在的rx和rxm参数，避免重复
                                    params_part = re.sub(r'\brx=[^,\]]+', '', params_part)
                                    params_part = re.sub(r'\brxm=[^,\]]+', '', params_part)
                                    
                                    # 添加rx和rxm参数
                                    if params_part.endswith('['):
                                        params_part = params_part[:-1] + f'rxm={rxm_val},rx={rx_val}]'
                                    elif params_part.endswith(']'):
                                        params_part = params_part[:-1] + f',rxm={rxm_val},rx={rx_val}]'
                                    else:
                                        params_part = params_part + f',rxm={rxm_val},rx={rx_val}'
                                    bedrock_reminders.append(f"Java版x_rotation={original_x_rotation_value}参数已转换为基岩版rxm={rxm_val},rx={rx_val}")            
            # 处理Java版y_rotation参数到基岩版ry/rym参数的转换
            if original_y_rotation_value:
                # 先检查是否已存在ry和rym参数
                existing_ry_match = re.search(r'\bry=([^,\]]+)', params_part)
                existing_ry_value = existing_ry_match.group(1) if existing_ry_match else None
                existing_rym_match = re.search(r'\brym=([^,\]]+)', params_part)
                existing_rym_value = existing_rym_match.group(1) if existing_rym_match else None
                
                # 移除Java版y_rotation参数
                params_part = re.sub(y_rotation_pattern, '', params_part)
                
                # 解析y_rotation值
                if '..' in original_y_rotation_value:
                    parts = original_y_rotation_value.split('..')
                    if parts[0] and parts[1]:
                        # 有上下限：-45..45 -> rym=-45,ry=45
                        rym_val = parts[0]
                        ry_val = parts[1]
                        # 检查是否已存在合适的ry和rym参数
                        if existing_ry_value and existing_rym_value:
                            # 已有ry和rym参数，不需要修改
                            pass
                        else:
                            # 移除已存在的ry和rym参数，避免重复
                            params_part = re.sub(r'\bry=[^,\]]+', '', params_part)
                            params_part = re.sub(r'\brym=[^,\]]+', '', params_part)
                            
                            # 添加ry和rym参数
                            if params_part.endswith('['):
                                params_part = params_part[:-1] + f'rym={rym_val},ry={ry_val}]'
                            elif params_part.endswith(']'):
                                params_part = params_part[:-1] + f',rym={rym_val},ry={ry_val}]'
                            else:
                                params_part = params_part + f',rym={rym_val},ry={ry_val}'
                            bedrock_reminders.append(f"Java版y_rotation={original_y_rotation_value}参数已转换为基岩版rym={rym_val},ry={ry_val}")
                    elif parts[0]:
                        # 只有下限：-45.. -> rym=-45
                        rym_val = parts[0]
                        # 检查是否已存在合适的rym参数
                        if existing_rym_value:
                            # 已有rym参数，不需要修改
                            pass
                        else:
                            # 移除已存在的rym参数，避免重复
                            params_part = re.sub(r'\brym=[^,\]]+', '', params_part)
                            
                            # 添加rym参数
                            if params_part.endswith('['):
                                params_part = params_part[:-1] + f'rym={rym_val}]'
                            elif params_part.endswith(']'):
                                params_part = params_part[:-1] + f',rym={rym_val}]'
                            else:
                                params_part = params_part + f',rym={rym_val}'
                            bedrock_reminders.append(f"Java版y_rotation={original_y_rotation_value}参数已转换为基岩版rym={rym_val}")
                    elif parts[1]:
                        # 只有上限：..45 -> ry=45
                        ry_val = parts[1]
                        # 检查是否已存在合适的ry参数
                        if existing_ry_value:
                            # 已有ry参数，不需要修改
                            pass
                        else:
                            # 移除已存在的ry参数，避免重复
                            params_part = re.sub(r'\bry=[^,\]]+', '', params_part)
                            
                            # 添加ry参数
                            if params_part.endswith('['):
                                params_part = params_part[:-1] + f'ry={ry_val}]'
                            elif params_part.endswith(']'):
                                params_part = params_part[:-1] + f',ry={ry_val}]'
                            else:
                                params_part = params_part + f',ry={ry_val}'
                            bedrock_reminders.append(f"Java版y_rotation={original_y_rotation_value}参数已转换为基岩版ry={ry_val}")
                else:
                                # 单个值：90 -> rym=90,ry=90（精确匹配）
                                rym_val = original_y_rotation_value
                                ry_val = original_y_rotation_value
                                # 检查是否已存在合适的ry和rym参数
                                if existing_ry_value and existing_rym_value:
                                    # 已有ry和rym参数，不需要修改
                                    pass
                                else:
                                    # 移除已存在的ry和rym参数，避免重复
                                    params_part = re.sub(r'\bry=[^,\]]+', '', params_part)
                                    params_part = re.sub(r'\brym=[^,\]]+', '', params_part)
                                    
                                    # 添加ry和rym参数
                                    if params_part.endswith('['):
                                        params_part = params_part[:-1] + f'rym={rym_val},ry={ry_val}]'
                                    elif params_part.endswith(']'):
                                        params_part = params_part[:-1] + f',rym={rym_val},ry={ry_val}]'
                                    else:
                                        params_part = params_part + f',rym={rym_val},ry={ry_val}'
                                    bedrock_reminders.append(f"Java版y_rotation={original_y_rotation_value}参数已转换为基岩版rym={rym_val},ry={ry_val}")            
            # 处理Java版level参数到基岩版l/lm参数的转换
            if original_level_value:
                # 先检查是否已存在l和lm参数
                existing_l_match = re.search(r'\bl=([^,\]]+)', params_part)
                existing_l_value = existing_l_match.group(1) if existing_l_match else None
                existing_lm_match = re.search(r'\blm=([^,\]]+)', params_part)
                existing_lm_value = existing_lm_match.group(1) if existing_lm_match else None
                
                # 移除Java版level参数
                params_part = re.sub(level_pattern, '', params_part)
                
                # 解析level值
                if '..' in original_level_value:
                    parts = original_level_value.split('..')
                    if parts[0] and parts[1]:
                        # 有上下限：5..10 -> lm=5,l=10
                        lm_val = parts[0]
                        l_val = parts[1]
                        # 检查是否已存在合适的l和lm参数
                        if existing_l_value and existing_lm_value:
                            # 已有l和lm参数，不需要修改
                            pass
                        else:
                            # 移除已存在的l和lm参数，避免重复
                            params_part = re.sub(r'\bl=[^,\]]+', '', params_part)
                            params_part = re.sub(r'\blm=[^,\]]+', '', params_part)
                            
                            # 添加l和lm参数
                            if params_part.endswith('['):
                                params_part = params_part[:-1] + f'lm={lm_val},l={l_val}]'
                            elif params_part.endswith(']'):
                                params_part = params_part[:-1] + f',lm={lm_val},l={l_val}]'
                            else:
                                params_part = params_part + f',lm={lm_val},l={l_val}'
                            bedrock_reminders.append(f"Java版level={original_level_value}参数已转换为基岩版lm={lm_val},l={l_val}")
                    elif parts[0]:
                        # 只有下限：5.. -> lm=5
                        lm_val = parts[0]
                        # 检查是否已存在合适的lm参数
                        if existing_lm_value:
                            # 已有lm参数，不需要修改
                            pass
                        else:
                            # 移除已存在的lm参数，避免重复
                            params_part = re.sub(r'\blm=[^,\]]+', '', params_part)
                            
                            # 添加lm参数
                            if params_part.endswith('['):
                                params_part = params_part[:-1] + f'lm={lm_val}]'
                            elif params_part.endswith(']'):
                                params_part = params_part[:-1] + f',lm={lm_val}]'
                            else:
                                params_part = params_part + f',lm={lm_val}'
                            bedrock_reminders.append(f"Java版level={original_level_value}参数已转换为基岩版lm={lm_val}")
                    elif parts[1]:
                        # 只有上限：..10 -> l=10
                        l_val = parts[1]
                        # 检查是否已存在合适的l参数
                        if existing_l_value:
                            # 已有l参数，不需要修改
                            pass
                        else:
                            # 移除已存在的l参数，避免重复
                            params_part = re.sub(r'\bl=[^,\]]+', '', params_part)
                            
                            # 添加l参数
                            if params_part.endswith('['):
                                params_part = params_part[:-1] + f'l={l_val}]'
                            elif params_part.endswith(']'):
                                params_part = params_part[:-1] + f',l={l_val}]'
                            else:
                                params_part = params_part + f',l={l_val}'
                            bedrock_reminders.append(f"Java版level={original_level_value}参数已转换为基岩版l={l_val}")
                else:
                    # 单个值：10 -> l=10,lm=10
                    l_val = original_level_value
                    # 检查是否已存在合适的l和lm参数
                    if existing_l_value and existing_lm_value:
                        # 已有l和lm参数，不需要修改
                        pass
                    else:
                        # 移除已存在的l和lm参数，避免重复
                        params_part = re.sub(r'\bl=[^,\]]+', '', params_part)
                        params_part = re.sub(r'\blm=[^,\]]+', '', params_part)
                        
                        # 添加l和lm参数
                        if params_part.endswith('['):
                            params_part = params_part[:-1] + f'lm={l_val},l={l_val}]'
                        elif params_part.endswith(']'):
                            params_part = params_part[:-1] + f',lm={l_val},l={l_val}]'
                        else:
                            params_part = params_part + f',lm={l_val},l={l_val}'
                        bedrock_reminders.append(f"Java版level={original_level_value}参数已转换为基岩版lm={l_val},l={l_val}")
            
            # 处理Java版limit参数到基岩版c参数的转换（如果没有sort参数）
            # 注意：limit参数的转换已经在convert_limit_c_between_versions函数中处理了，这里不再添加重复提醒
            if original_limit_value and not original_sort_value:
                # 先移除已存在的c参数，避免重复
                params_part = re.sub(r'\bc=[^,\]]+', '', params_part)
                # 移除Java版limit参数
                params_part = re.sub(limit_pattern, '', params_part)
                
                # 添加c参数
                if params_part == '[]':
                    # 如果params_part是空的方括号，直接替换为[c=5]
                    params_part = f'[c={original_limit_value}]'
                elif params_part.endswith('['):
                    params_part = params_part[:-1] + f'c={original_limit_value}]'
                elif params_part.endswith(']'):
                    # 检查是否是空方括号
                    if params_part == '[]':
                        params_part = f'[c={original_limit_value}]'
                    else:
                        params_part = params_part[:-1] + f',c={original_limit_value}]'
                else:
                    params_part = params_part + f',c={original_limit_value}'
                # 不再添加重复的提醒信息，因为convert_limit_c_between_versions函数已经添加了
        
        # 处理sort参数
        if original_sort_value:
            if original_sort_value == 'nearest':
                # 根据sort.txt: sort=nearest时，只有大选择器是@a @p时基岩版才开始转换，并且选择器只能用@p
                # 当大选择器不是这几个时基岩版直接砍掉sort=nearest
                if selector_var in ['@a', '@p']:
                    # 只有当选择器是@p或@a时才进行转换
                    c_value = original_limit_value if original_limit_value else '9999'
                    # sort=nearest转换为c参数，并将选择器转换为@p
                    selector_var = '@p'
                    # 先移除已存在的c参数，避免重复
                    params_part = re.sub(r'\bc=[^,\]]+', '', params_part)
                    # 移除sort参数，移除limit参数（基岩版不需要），添加c参数
                    params_part = re.sub(sort_pattern, '', params_part)
                    params_part = re.sub(limit_pattern, '', params_part)
                    if params_part.endswith('['):
                        params_part = params_part[:-1] + f'c={c_value}]'
                    elif params_part.endswith(']'):
                        params_part = params_part[:-1] + f',c={c_value}]'
                    else:
                        params_part = params_part + f'c={c_value}'
                    bedrock_reminders.append(f"Java版sort=nearest已转换为基岩版c={c_value}")
                else:
                    # 当选择器不是@p或@a时，直接移除sort=nearest参数
                    params_part = re.sub(sort_pattern, '', params_part)
                    params_part = re.sub(limit_pattern, '', params_part)
                    bedrock_reminders.append("Java版非@p/@a选择器的sort=nearest参数在基岩版中不支持，已移除")
            elif original_sort_value == 'furthest':
                # sort=furthest转换为c参数
                abs_limit_val = original_limit_value if original_limit_value else '9999'
                c_value = f'-{abs_limit_val}'
                # 先移除已存在的c参数，避免重复
                params_part = re.sub(r'\bc=[^,\]]+', '', params_part)
                # 移除sort参数，移除limit参数（基岩版不需要），添加c参数
                params_part = re.sub(sort_pattern, '', params_part)
                params_part = re.sub(limit_pattern, '', params_part)
                if params_part.endswith('['):
                    params_part = params_part[:-1] + f'c={c_value}]'
                elif params_part.endswith(']'):
                    params_part = params_part[:-1] + f',c={c_value}]'
                else:
                    params_part = params_part + f'c={c_value}'
                bedrock_reminders.append(f"Java版sort=furthest已转换为基岩版c={c_value}")
            elif original_sort_value == 'arbitrary':
                # 移除sort参数，基岩版不支持sort=arbitrary
                params_part = re.sub(sort_pattern, '', params_part)
                bedrock_reminders.append("Java版sort=arbitrary在基岩版中不支持，已移除")
            elif original_sort_value == 'random':
                # 对于sort=random，基岩版中将选择器从@a改为@r，并添加c参数
                if selector_var == '@a':
                    selector_var = '@r'
                    c_value = original_limit_value if original_limit_value else '9999'
                    # 先移除已存在的c参数，避免重复
                    params_part = re.sub(r'\bc=[^,\]]+', '', params_part)
                    # 移除sort参数，移除limit参数（基岩版不需要），添加c参数
                    params_part = re.sub(sort_pattern, '', params_part)
                    params_part = re.sub(limit_pattern, '', params_part)
                    if params_part.endswith('['):
                        params_part = params_part[:-1] + f'c={c_value}]'
                    elif params_part.endswith(']'):
                        params_part = params_part[:-1] + f',c={c_value}]'
                    else:
                        params_part = params_part + f'c={c_value}'
                    bedrock_reminders.append("Java版@a[sort=random]已转换为基岩版@r")
                else:
                    # 对于其他选择器（如@p、@e），移除sort参数，保留已转换的c参数
                    params_part = re.sub(sort_pattern, '', params_part)
                    # 清理多余的逗号
                    params_part = re.sub(r',+', ',', params_part)
                    params_part = re.sub(r'\[,', '[', params_part)
                    params_part = re.sub(r',\]', ']', params_part)
                    bedrock_reminders.append(f"Java版sort=random已转换为基岩版c参数")
            else:
                # 移除sort参数
                params_part = re.sub(sort_pattern, '', params_part)
                # 移除limit参数（如果存在）
                params_part = re.sub(limit_pattern, '', params_part)
                bedrock_reminders.append(f"Java版sort={original_sort_value}被移除")
        else:
            # 如果原始选择器是Java版（只有limit参数，没有sort参数），需要处理limit参数到c参数的转换
            # 注意：如果已经处理过limit参数转换（在needs_java_to_bedrock_conversion中），则不再重复处理
            if original_limit_value and not original_sort_value and not needs_java_to_bedrock_conversion:
                # 检查params_part是否已经包含c参数
                if 'c=' not in params_part:
                    # 先移除已存在的c参数，避免重复
                    params_part = re.sub(r'\bc=[^,\]]+', '', params_part)
                    # 只有limit参数，没有sort参数，直接转换为c参数
                    params_part = re.sub(limit_pattern, f'c={original_limit_value}', params_part)
                    bedrock_reminders.append(f"Java版limit={original_limit_value}参数已转换为基岩版c={original_limit_value}")
                else:
                    # 如果已经包含c参数，不需要再次转换
                    pass
        
        # 清理多余的逗号
        params_part = re.sub(r',+', ',', params_part)
        
        # 特殊处理：如果params_part以逗号开头，移除开头的逗号
        if params_part.startswith(','):
            params_part = params_part[1:]
        
        params_part = re.sub(r'\[,', '[', params_part)
        params_part = re.sub(r',\]', ']', params_part)
        # 不移除空方括号，因为可能会误删内容
        
        bedrock_selector_final = selector_var + params_part
        # 调试：打印处理后的基岩版选择器
    else:
        bedrock_selector_final = bedrock_selector_converted
    
    # 过滤Java版参数，移除基岩版特有的参数（完全不支持）
    java_selector_filtered, java_removed_params = filter_selector_parameters(java_selector_final, 'java')
    
    # 过滤基岩版参数，移除Java版特有的参数（完全不支持）
    # 调试：打印过滤前的基岩版选择器
    bedrock_selector_filtered, bedrock_removed_params = filter_selector_parameters(bedrock_selector_final, 'bedrock')
    # 调试：打印过滤后的基岩版选择器
    
    # 额外处理：确保基岩版选择器中不包含l和lm参数（这些是基岩版参数，但在Java版中没有对应）
    # 但基岩版应该保留这些参数，因为它们是基岩版的参数
    # 实际上，l和lm是基岩版参数，应该保留在基岩版中，因为基岩版本身就支持这些参数
    
    # 合并所有提醒信息
    all_java_reminders = java_gamemode_reminders + java_reminders + java_selector_reminders
    all_bedrock_reminders = bedrock_gamemode_reminders + bedrock_reminders
    
    # 生成Java版命令
    java_json = convert_text_to_java(message, m_n_handling)
    java_command = f'tellraw {java_selector_filtered} {json.dumps(java_json, ensure_ascii=False)}'
    
    # 生成基岩版命令
    bedrock_json = convert_text_to_bedrock(message, m_n_handling)
    bedrock_command = f'tellraw {bedrock_selector_filtered} {json.dumps(bedrock_json, ensure_ascii=False)}'
    
    return java_command, bedrock_command, was_converted, java_selector if was_converted else None, java_removed_params, bedrock_removed_params, all_java_reminders, all_bedrock_reminders

def handle_m_n_codes(message):
    """
    处理§m§n代码，询问用户选择
    选项1: Java版使用字体方式，基岩版使用颜色代码方式
    选项2: Java版和基岩版都使用颜色代码方式
    """
    if '§m' in message or '§n' in message:
        print(PROMPTS["prompts"]["m_n_choice"])
        
        choice = input("请选择 (1/2): ").strip()
        if choice == '1':
            # Java版使用字体，基岩版使用颜色代码
            return message, "font"
        elif choice == '2':
            # 都使用颜色代码
            return message, "color"
        else:
            print("无效选择，默认使用选项1")
            return message, "font"
    
    return message, "none"

def get_user_input():
    """获取用户交互式输入"""
    print("=== Minecraft Tellraw 生成器 ===")
    
    # 输入目标选择器
    selector = input(PROMPTS["prompts"]["selector_input"] + " ").strip()
    
    # 检测选择器类型
    selector_type = detect_selector_type(selector)
    print(PROMPTS["prompts"]["selector_type"].format(selector_type))
    
    # 输入文本消息
    message = input(PROMPTS["prompts"]["message_input"] + " ").strip()
    
    # 处理§m§n代码
    message, m_n_option = handle_m_n_codes(message)
    
    return selector, message, m_n_option

def show_commands(java_cmd, bedrock_cmd, was_converted=False, original_selector=None, converted_selector=None, java_removed_params=None, bedrock_removed_params=None, java_gamemode_reminders=None, bedrock_gamemode_reminders=None):
    """显示生成的命令"""
    print("\n=== 生成的命令 ===")
    if was_converted:
        print(PROMPTS["prompts"]["selector_conversion_note"].format(original_selector, converted_selector))
    
    # 合并所有Java版提醒信息并去重
    all_java_reminders = []
    if java_removed_params:
        all_java_reminders.extend(java_removed_params)
    if java_gamemode_reminders:
        all_java_reminders.extend(java_gamemode_reminders)
    
    # 分类并去重处理Java版提醒
    java_non_reminders = [param for param in all_java_reminders if not param.startswith("Java版") and not param.startswith("基岩版") and not "nbt参数" in param and not "参数已转换为" in param and not "已转换为" in param]
    java_non_reminders = list(dict.fromkeys(java_non_reminders))
    
    java_specific_reminders = [param for param in all_java_reminders if "参数已转换为" in param or "nbt参数" in param or "已转换为" in param or (param.startswith("Java版") or param.startswith("基岩版"))]
    java_specific_reminders = list(dict.fromkeys(java_specific_reminders))
    
    # 显示Java版参数剔除提醒
    if java_non_reminders:
        print(f"注意: Java版不支持以下参数，已从Java版命令中移除: {', '.join(java_non_reminders)}")
    
    # 显示特殊提醒（包括以"基岩版"或"Java版"开头的提醒）
    for reminder in java_specific_reminders:
        print(f"注意: {reminder}")
    
    # 合并所有基岩版提醒信息并去重
    all_bedrock_reminders = []
    if bedrock_removed_params:
        all_bedrock_reminders.extend(bedrock_removed_params)
    if bedrock_gamemode_reminders:
        all_bedrock_reminders.extend(bedrock_gamemode_reminders)
    
    # 分类并去重处理基岩版提醒
    bedrock_non_reminders = [param for param in all_bedrock_reminders if not param.startswith("Java版") and not param.startswith("基岩版") and not "nbt参数" in param and not "参数已转换为" in param and not "已转换为" in param]
    bedrock_non_reminders = list(dict.fromkeys(bedrock_non_reminders))
    
    bedrock_specific_reminders = [param for param in all_bedrock_reminders if "参数已转换为" in param or "nbt参数" in param or "已转换为" in param or (param.startswith("Java版") or param.startswith("基岩版"))]
    bedrock_specific_reminders = list(dict.fromkeys(bedrock_specific_reminders))
    
    # 显示基岩版参数剔除提醒
    if bedrock_non_reminders:
        print(f"注意: 基岩版不支持以下参数，已从基岩版命令中移除: {', '.join(bedrock_non_reminders)}")
    
    # 显示特殊提醒（包括以"基岩版"或"Java版"开头的提醒）
    for reminder in bedrock_specific_reminders:
        print(f"注意: {reminder}")
    
    print(PROMPTS["prompts"]["java_command"].format(java_cmd))
    print(PROMPTS["prompts"]["bedrock_command"].format(bedrock_cmd))

def main():
    if len(sys.argv) == 3:
        # 命令行参数模式
        selector = sys.argv[1]
        message = sys.argv[2]
        
        # 检查是否包含§m§n代码
        if '§m' in message or '§n' in message:
            # 在命令行模式下，如果包含§m§n代码，默认使用颜色代码方式
            m_n_option = "color"
        else:
            m_n_option = "none"
        
        java_cmd, bedrock_cmd, was_converted, converted_selector, java_removed_params, bedrock_removed_params, java_gamemode_reminders, bedrock_gamemode_reminders = generate_tellraw_commands(selector, message, m_n_option)
        show_commands(java_cmd, bedrock_cmd, was_converted, selector, converted_selector, java_removed_params, bedrock_removed_params, java_gamemode_reminders, bedrock_gamemode_reminders)
    elif len(sys.argv) == 1:
        # 交互式模式
        selector, message, m_n_option = get_user_input()
        
        java_cmd, bedrock_cmd, was_converted, converted_selector, java_removed_params, bedrock_removed_params, java_gamemode_reminders, bedrock_gamemode_reminders = generate_tellraw_commands(selector, message, m_n_option)
        show_commands(java_cmd, bedrock_cmd, was_converted, selector, converted_selector, java_removed_params, bedrock_removed_params, java_gamemode_reminders, bedrock_gamemode_reminders)
    else:
        print(PROMPTS["prompts"]["usage"])
        sys.exit(1)

if __name__ == "__main__":
    main()
