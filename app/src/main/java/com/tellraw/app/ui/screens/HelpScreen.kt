package com.tellraw.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("帮助") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HelpSection(
                title = "关于应用",
                content = """
                    Tellraw命令生成器是一个帮助Minecraft玩家生成tellraw命令的工具。
                    
                    支持Java版和基岩版的选择器转换，以及文本格式化功能。
                """.trimIndent()
            )

            HelpSection(
                title = "选择器输入",
                content = """
                    选择器用于指定命令的目标：
                    
                    • @a - 所有玩家
                    • @p - 最近的玩家
                    • @r - 随机玩家
                    • @e - 所有实体
                    • @s - 命令执行者
                    
                    支持参数过滤，如：
                    • @a[gamemode=survival] - 生存模式的所有玩家
                    • @e[type=cow] - 所有牛
                    • @p[distance=..10] - 10格内的最近玩家
                """.trimIndent()
            )

            HelpSection(
                title = "文本格式化",
                content = """
                    支持Minecraft颜色代码和格式代码：
                    
                    颜色代码：
                    • §0 - 黑色    §1 - 深蓝色   §2 - 深绿色
                    • §3 - 深青色   §4 - 深红色   §5 - 深紫色
                    • §6 - 金色    §7 - 灰色     §8 - 深灰色
                    • §9 - 蓝色    §a - 绿色     §b - 青色
                    • §c - 红色    §d - 粉色     §e - 黄色
                    • §f - 白色
                    
                    格式代码：
                    • §l - 粗体    §m - 删除线   §n - 下划线
                    • §o - 斜体    §k - 混乱字   §r - 重置
                """.trimIndent()
            )

            HelpSection(
                title = "版本差异",
                content = """
                    Java版和基岩版在选择器和格式化方面存在差异：
                    
                    选择器差异：
                    • Java版使用distance参数，基岩版使用r/rm参数
                    • Java版使用gamemode参数，基岩版使用m参数
                    • Java版支持nbt参数，基岩版支持hasitem参数
                    
                    格式化差异：
                    • §m§n代码在两个版本中的处理方式不同
                    • 某些颜色代码在基岩版中有特殊含义
                """.trimIndent()
            )

            HelpSection(
                title = "使用提示",
                content = """
                    1. 输入选择器和消息内容
                    2. 应用会自动检测选择器类型
                    3. 生成Java版和基岩版命令
                    4. 可以复制命令或分享给他人
                    
                    如果遇到§m§n代码，应用会询问处理方式：
                    • Java版字体方式：Java版保持原样，基岩版转换为颜色代码
                    • 颜色代码方式：两个版本都转换为颜色代码
                """.trimIndent()
            )
        }
    }
}

@Composable
private fun HelpSection(
    title: String,
    content: String
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}