package com.tellraw.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.tellraw.app.R
import com.tellraw.app.data.remote.GithubRelease
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateDialog(
    release: GithubRelease,
    onDismiss: () -> Unit,
    onOpenUrl: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.new_version_found),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 版本信息
                Text(
                    text = stringResource(R.string.new_version, release.name),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Text(
                    text = stringResource(R.string.version_number, release.tag_name),
                    style = MaterialTheme.typography.bodyMedium
                )
                
                // 发布日期
                Text(
                    text = stringResource(R.string.release_date, formatDate(release.published_at)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // 下载链接
                SelectionContainer {
                    Text(
                        text = stringResource(R.string.download_url),
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
                val body = release.body
                if (body.isNotEmpty()) {
                    val truncatedBody = if (body.length > 200) {
                        body.take(200) + "..."
                    } else {
                        body
                    }

                    Text(
                        text = stringResource(R.string.update_notes),
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
        },
        confirmButton = {
            TextButton(onClick = { onOpenUrl(release.html_url) }) {
                Text(stringResource(R.string.open))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.remind_later))
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
                text = stringResource(R.string.disable_version_check),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = stringResource(R.string.disable_version_check_message),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
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

@Composable
fun JavaBedrockMixedModeWarningDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var countdown by remember { mutableStateOf(2) }
    
    LaunchedEffect(countdown) {
        while (countdown > 0) {
            delay(1000L)
            countdown--
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.java_bedrock_mixed_mode_warning),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.java_bedrock_mixed_mode_not_mature),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.java_bedrock_mixed_mode_warning_message),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                if (countdown > 0) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.please_wait_seconds, countdown),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = countdown == 0
            ) {
                if (countdown > 0) {
                    Text("$countdown")
                } else {
                    Text(stringResource(R.string.ok))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
