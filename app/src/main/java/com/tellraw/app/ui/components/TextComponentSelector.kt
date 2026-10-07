package com.tellraw.app.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tellraw.app.R
import com.tellraw.app.util.TextComponentHelper

/**
 * 文本组件选择器
 *
 * 布局：**横排**（与 § 颜色/格式快捷列表一致的 FilterChip 风格，配色也一致）。
 * 每个组件类型一个 chip；带副组件的（translate / selector）在 chip 尾部有一个展开图标，
 * 展开后把副组件（with / separator）作为相邻 chip 追加在其后；末尾是"纯文本单引号"chip。
 */
@Composable
fun TextComponentSelector(
    selectedComponent: TextComponentHelper.ComponentType?,
    selectedSubComponent: TextComponentHelper.SubComponentType?,
    expandedSubComponents: Set<String> = emptySet(),
    onComponentSelected: (TextComponentHelper.ComponentType) -> Unit,
    onSubComponentToggle: (TextComponentHelper.ComponentType) -> Unit,
    onSubComponentSelected: (TextComponentHelper.SubComponentType) -> Unit = {},
    onInsertPlainQuote: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.text_component_title),
                style = MaterialTheme.typography.titleSmall
            )

            // 横排（可左右滚动），配色沿用 FilterChip 默认（与 § 快捷列表一致）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextComponentHelper.ComponentType.values().forEach { component ->
                    val isSelected = selectedComponent == component
                    val isExpanded = expandedSubComponents.contains(component.key)
                    ComponentChip(
                        component = component,
                        isSelected = isSelected,
                        isExpanded = isExpanded,
                        onSelected = { onComponentSelected(component) },
                        onSubComponentToggle = { onSubComponentToggle(component) }
                    )
                    // 展开时：副组件紧跟其后（同一横排）
                    if (component.hasSubComponent && isExpanded) {
                        subComponentOf(component)?.let { sub ->
                            SubComponentChip(
                                subComponent = sub,
                                isSelected = selectedSubComponent == sub,
                                onSelected = { onSubComponentSelected(sub) }
                            )
                        }
                    }
                }
                // 纯文本单引号：点一下往输入框插一个 '
                FilterChip(
                    selected = false,
                    onClick = onInsertPlainQuote,
                    label = {
                        Text(
                            text = "'  " + stringResource(R.string.component_quote_hint),
                            maxLines = 1
                        )
                    }
                )
            }
        }
    }
}

/** 组件对应的副组件（没有则返回 null）。 */
private fun subComponentOf(
    component: TextComponentHelper.ComponentType
): TextComponentHelper.SubComponentType? = when (component) {
    TextComponentHelper.ComponentType.TRANSLATE -> TextComponentHelper.SubComponentType.WITH
    TextComponentHelper.ComponentType.SELECTOR -> TextComponentHelper.SubComponentType.SEPARATOR
    else -> null
}

/** 主组件 chip（带副组件的尾部带展开/收起图标）。 */
@Composable
private fun ComponentChip(
    component: TextComponentHelper.ComponentType,
    isSelected: Boolean,
    isExpanded: Boolean,
    onSelected: () -> Unit,
    onSubComponentToggle: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onSelected,
        label = {
            Text(stringResource(component.displayNameResId), maxLines = 1)
        },
        trailingIcon = if (component.hasSubComponent) {
            {
                IconButton(
                    onClick = onSubComponentToggle,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = stringResource(
                            if (isExpanded) R.string.component_collapse else R.string.component_expand
                        ),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        } else null
    )
}

/** 副组件 chip（with / separator）。 */
@Composable
private fun SubComponentChip(
    subComponent: TextComponentHelper.SubComponentType,
    isSelected: Boolean,
    onSelected: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onSelected,
        label = {
            Text(
                text = "${stringResource(R.string.component_arrow)}${stringResource(subComponent.displayNameResId)}",
                maxLines = 1
            )
        }
    )
}

/**
 * 当前选中的组件类型指示器
 */
@Composable
fun CurrentComponentIndicator(
    component: TextComponentHelper.ComponentType?,
    componentContent: String? = null,
    modifier: Modifier = Modifier
) {
    if (component != null) {
        Surface(
            modifier = modifier,
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.current_component),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(component.displayNameResId),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                if (componentContent != null && componentContent.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "(${componentContent})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
