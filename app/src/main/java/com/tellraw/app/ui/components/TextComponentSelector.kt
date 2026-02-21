package com.tellraw.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tellraw.app.R
import com.tellraw.app.util.TextComponentHelper

/**
 * 文本组件选择器
 * 用于选择文本组件类型（text, translate等）
 */
@Composable
fun TextComponentSelector(
    selectedComponent: TextComponentHelper.ComponentType?,
    selectedSubComponent: TextComponentHelper.SubComponentType?,
    expandedSubComponents: Set<String> = emptySet(),
    onComponentSelected: (TextComponentHelper.ComponentType) -> Unit,
    onSubComponentToggle: (TextComponentHelper.ComponentType) -> Unit,
    onSubComponentSelected: (TextComponentHelper.SubComponentType) -> Unit = {},
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
            
            // 组件列表（独立滚动）
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(TextComponentHelper.ComponentType.values()) { component ->
                    ComponentItem(
                        component = component,
                        isSelected = selectedComponent == component,
                        isExpanded = expandedSubComponents.contains(component.key),
                        selectedSubComponent = selectedSubComponent,
                        onSelected = { onComponentSelected(component) },
                        onSubComponentToggle = { onSubComponentToggle(component) },
                        onSubComponentSelected = onSubComponentSelected
                    )
                }
            }
        }
    }
}

/**
 * 单个组件项
 */
@Composable
private fun ComponentItem(
    component: TextComponentHelper.ComponentType,
    isSelected: Boolean,
    isExpanded: Boolean,
    selectedSubComponent: TextComponentHelper.SubComponentType?,
    onSelected: () -> Unit,
    onSubComponentToggle: () -> Unit,
    onSubComponentSelected: (TextComponentHelper.SubComponentType) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // 主组件
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelected() }
                .background(
                    color = if (isSelected) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else 
                        MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(component.displayNameResId),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                ),
                color = if (isSelected) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            
            // 展开副组件的图标（如果有副组件）
            if (component.hasSubComponent) {
                IconButton(
                    onClick = {
                        onSubComponentToggle()
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) stringResource(R.string.component_collapse) else stringResource(R.string.component_expand),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        
        // 副组件（已展开时显示）
        if (component.hasSubComponent && isExpanded) {
            Spacer(modifier = Modifier.height(4.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp)
            ) {
                component.key?.let { componentKey ->
                    when (component) {
                        TextComponentHelper.ComponentType.TRANSLATE -> {
                            // with参数
                            SubComponentItem(
                                subComponent = TextComponentHelper.SubComponentType.WITH,
                                isSelected = selectedSubComponent == TextComponentHelper.SubComponentType.WITH,
                                parentComponent = component,
                                onSelected = { onSubComponentSelected(TextComponentHelper.SubComponentType.WITH) }
                            )
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}

/**
 * 副组件项
 */
@Composable
private fun SubComponentItem(
    subComponent: TextComponentHelper.SubComponentType,
    isSelected: Boolean,
    parentComponent: TextComponentHelper.ComponentType,
    onSelected: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelected() }
            .background(
                color = if (isSelected) 
                    MaterialTheme.colorScheme.secondaryContainer 
                else 
                    MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(4.dp)
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${stringResource(R.string.component_arrow)}${stringResource(subComponent.displayNameResId)}",
            style = MaterialTheme.typography.bodySmall,
            color = if (isSelected) 
                MaterialTheme.colorScheme.secondary 
            else 
                MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
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