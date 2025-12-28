package com.tellraw.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.tellraw.app.data.remote.GithubRelease

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateDialog(
    release: GithubRelease,
    onDismiss: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onDisableChecks: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "发现新版本",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 版本信息
                Text(
                    text = "新版本: ${release.name}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Text(
                    text = "版本号: ${release.tag_name}",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                // 发布日期
                Text(
                    text = "发布日期: ${formatDate(release.published_at)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // 下载链接
                SelectionContainer {
                    Text(
                        text = "下载地址:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = release.html_url,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline
                        )
                    )
                }
                
                // 更新说明（截取前200个字符）
                release.body?.let { body ->
                    if (body.isNotEmpty()) {
                        val truncatedBody = if (body.length > 200) {
                            body.take(200) + "..."
                        } else {
                            body
                        }
                        
                        Text(
                            text = "更新说明:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        SelectionContainer {
                            Text(
                                text = truncatedBody,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onOpenUrl(release.html_url) }) {
                Text("打开")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("稍后提醒")
            }
        },
        modifier = Modifier.padding(16.dp)
    )
}

@Composable
fun DisableCheckDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "禁用版本检查",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = "确定要禁用版本检查功能吗？\n\n禁用后将不再自动检查新版本，您可以在设置中重新启用。",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

private fun formatDate(dateString: String): String {
    return try {
        // 简单的日期格式化，只显示年月日
        val date = dateString.substringBefore('T')
        date
    } catch (e: Exception) {
        dateString
    }
}
