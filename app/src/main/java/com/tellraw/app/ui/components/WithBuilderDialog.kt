package com.tellraw.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.google.gson.JsonParser
import com.tellraw.app.R
import com.tellraw.app.ui.viewmodel.WithBuilderMode
import com.tellraw.app.ui.viewmodel.WithBuilderState
import com.tellraw.app.util.WithArgCodec
import com.tellraw.app.util.WithArgDraft

/**
 * translate 的 with 参数可视化构造器：可视化列表 + JSON 兜底，支持 translate 无限嵌套。
 */
@Composable
fun WithBuilderDialog(
    state: WithBuilderState,
    onDismiss: () -> Unit,
    onKeyChange: (String) -> Unit,
    onFallbackChange: (String) -> Unit,
    onArgsChange: (List<WithArgDraft>) -> Unit,
    onModeChange: (WithBuilderMode) -> Unit,
    onRawJsonChange: (String) -> Unit,
    onApply: () -> Unit
) {
    var visualRawError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(state.mode) {
        if (state.mode == WithBuilderMode.JSON) visualRawError = null
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .heightIn(max = 680.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    stringResource(R.string.with_builder_title),
                    style = MaterialTheme.typography.titleMedium
                )
                if (state.isNew) {
                    Text(
                        stringResource(R.string.with_builder_new_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedTextField(
                    value = state.key,
                    onValueChange = onKeyChange,
                    label = { Text(stringResource(R.string.with_builder_key)) },
                    singleLine = true,
                    isError = state.key.isBlank(),
                    supportingText = if (state.key.isBlank()) {
                        { Text(stringResource(R.string.with_builder_key_required)) }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.fallback,
                    onValueChange = onFallbackChange,
                    label = { Text(stringResource(R.string.with_builder_fallback)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    stringResource(R.string.with_builder_json_input_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                TabRow(selectedTabIndex = if (state.mode == WithBuilderMode.VISUAL) 0 else 1) {
                    Tab(
                        selected = state.mode == WithBuilderMode.VISUAL,
                        onClick = { onModeChange(WithBuilderMode.VISUAL) },
                        text = { Text(stringResource(R.string.with_builder_tab_visual)) }
                    )
                    Tab(
                        selected = state.mode == WithBuilderMode.JSON,
                        onClick = { onModeChange(WithBuilderMode.JSON) },
                        text = { Text(stringResource(R.string.with_builder_tab_json)) }
                    )
                }

                if (state.mode == WithBuilderMode.VISUAL) {
                    VisualTreeEditor(
                        args = state.args,
                        onArgsChange = onArgsChange,
                        onRawError = { visualRawError = it }
                    )
                } else {
                    JsonTab(state = state, onRawJsonChange = onRawJsonChange)
                }

                val parsedForNotes = remember(state.mode, state.args, state.rawJson) {
                    when (state.mode) {
                        WithBuilderMode.VISUAL -> state.args
                        WithBuilderMode.JSON -> runCatching { WithArgCodec.parseArgs(state.rawJson) }
                            .getOrNull() ?: emptyList()
                    }
                }
                BedrockNotes(parsedForNotes)
                JsonPreview(state)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = onApply,
                        enabled = state.key.isNotBlank() &&
                            if (state.mode == WithBuilderMode.VISUAL) {
                                visualRawError == null
                            } else {
                                state.rawError == null
                            }
                    ) {
                        Text(stringResource(R.string.with_builder_apply))
                    }
                }
            }
        }
    }
}

// ---------------- 树形大纲 + 检查器 ----------------

@Composable
private fun VisualTreeEditor(
    args: List<WithArgDraft>,
    onArgsChange: (List<WithArgDraft>) -> Unit,
    onRawError: (String?) -> Unit
) {
    var selectedRaw by rememberSaveable { mutableStateOf("") }
    var collapsedRaw by rememberSaveable { mutableStateOf("") }
    val selectedPath = parseArgPath(selectedRaw)
    val collapsed = collapsedRaw.split(';').filter { it.isNotBlank() }.toSet()
    val selected = argAt(args, selectedPath)

    fun select(path: List<Int>) {
        selectedRaw = argPathString(path)
    }

    fun toggle(path: List<Int>) {
        val key = argPathString(path)
        val next = collapsed.toMutableSet()
        if (!next.add(key)) next.remove(key)
        collapsedRaw = next.joinToString(";")
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.with_builder_args), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(R.string.with_builder_tree_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AddArgMenu(label = stringResource(R.string.with_builder_add)) { draft ->
                val next = args + draft
                onArgsChange(next)
                collapsedRaw = ""
                select(listOf(next.lastIndex))
            }
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .heightIn(max = 300.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(6.dp)
            ) {
                if (args.isEmpty()) {
                    Text(
                        stringResource(R.string.with_builder_args_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                ArgTree(
                    args = args,
                    parentPath = emptyList(),
                    depth = 0,
                    selectedPath = selectedPath,
                    collapsed = collapsed,
                    onSelect = ::select,
                    onToggle = ::toggle
                )
            }
        }

        HorizontalDivider()
        Text(stringResource(R.string.with_builder_inspector), style = MaterialTheme.typography.titleSmall)
        if (selected == null) {
            Text(
                stringResource(R.string.with_builder_inspector_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            InlineArgEditor(
                arg = selected,
                nodeKey = argPathString(selectedPath),
                onChanged = { updated -> onArgsChange(updateArgAt(args, selectedPath, updated)) },
                onRawError = onRawError
            )
            val parentPath = selectedPath.dropLast(1)
            val index = selectedPath.lastOrNull() ?: 0
            val siblings = listAtPath(args, parentPath)
            val siblingCount = siblings?.size ?: 0
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TextButton(
                    enabled = index > 0,
                    onClick = {
                        onArgsChange(moveArg(args, selectedPath, -1))
                        collapsedRaw = ""
                        select(parentPath + (index - 1))
                    }
                ) { Text(stringResource(R.string.with_builder_move_up)) }
                TextButton(
                    enabled = index < siblingCount - 1,
                    onClick = {
                        onArgsChange(moveArg(args, selectedPath, 1))
                        collapsedRaw = ""
                        select(parentPath + (index + 1))
                    }
                ) { Text(stringResource(R.string.with_builder_move_down)) }
                TextButton(onClick = {
                    val next = insertArgAt(args, parentPath, index + 1, selected)
                    onArgsChange(next)
                    collapsedRaw = ""
                    select(parentPath + (index + 1))
                }) { Text(stringResource(R.string.with_builder_duplicate)) }
                if (selected is WithArgDraft.Translate) {
                    AddArgMenu(label = stringResource(R.string.with_builder_add_child)) { draft ->
                        val childIndex = selected.args.size
                        onArgsChange(addChildArg(args, selectedPath, draft))
                        collapsedRaw = ""
                        select(selectedPath + childIndex)
                    }
                }
                TextButton(onClick = {
                    onArgsChange(removeArg(args, selectedPath))
                    collapsedRaw = ""
                    selectedRaw = ""
                }) { Text(stringResource(R.string.delete)) }
            }
        }
    }
}

@Composable
private fun ArgTree(
    args: List<WithArgDraft>,
    parentPath: List<Int>,
    depth: Int,
    selectedPath: List<Int>,
    collapsed: Set<String>,
    onSelect: (List<Int>) -> Unit,
    onToggle: (List<Int>) -> Unit
) {
    Column {
        args.forEachIndexed { index, arg ->
            val path = parentPath + index
            val pathKey = argPathString(path)
            val translate = arg as? WithArgDraft.Translate
            val expanded = translate != null && translate.args.isNotEmpty() && pathKey !in collapsed
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (path == selectedPath) MaterialTheme.colorScheme.secondaryContainer
                        else Color.Transparent
                    )
                    .clickable { onSelect(path) }
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.width((depth * 14).dp))
                if (translate != null && translate.args.isNotEmpty()) {
                    IconButton(
                        onClick = { onToggle(path) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                            contentDescription = null
                        )
                    }
                } else {
                    Spacer(Modifier.width(28.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        kindLabel(arg),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        summaryOf(arg),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (translate != null) {
                    Text(
                        stringResource(R.string.with_builder_child_count, translate.args.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (expanded) {
                ArgTree(
                    args = translate!!.args,
                    parentPath = path,
                    depth = depth + 1,
                    selectedPath = selectedPath,
                    collapsed = collapsed,
                    onSelect = onSelect,
                    onToggle = onToggle
                )
            }
        }
    }
}

@Composable
private fun AddArgMenu(label: String, onAdd: (WithArgDraft) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { open = true }) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text(label)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            AddKind.values().forEach { kind ->
                DropdownMenuItem(
                    text = { Text(stringResource(kind.labelRes)) },
                    onClick = {
                        open = false
                        onAdd(defaultDraft(kind))
                    }
                )
            }
        }
    }
}

// ---------------- 检查器内联编辑 ----------------

@Composable
private fun InlineArgEditor(
    arg: WithArgDraft,
    nodeKey: String,
    onChanged: (WithArgDraft) -> Unit,
    onRawError: (String?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(kindLabel(arg), style = MaterialTheme.typography.titleSmall)
        when (arg) {
            is WithArgDraft.Text -> InlineTextEditor(arg, nodeKey, onChanged)
            is WithArgDraft.Translate -> InlineTranslateEditor(arg, nodeKey, onChanged)
            is WithArgDraft.Selector -> InlineSelectorEditor(arg, nodeKey, onChanged)
            is WithArgDraft.Score -> InlineScoreEditor(arg, nodeKey, onChanged)
            is WithArgDraft.Keybind -> InlineKeybindEditor(arg, nodeKey, onChanged)
            is WithArgDraft.Nbt -> InlineNbtEditor(arg, nodeKey, onChanged)
            is WithArgDraft.Raw -> InlineRawEditor(arg, nodeKey, onChanged, onRawError)
        }
    }
}

@Composable
private fun InlineTextEditor(
    original: WithArgDraft.Text,
    nodeKey: String,
    onChanged: (WithArgDraft) -> Unit
) {
    var text by rememberSaveable(nodeKey) { mutableStateOf(original.text) }
    var color by rememberSaveable(nodeKey) { mutableStateOf(original.color.orEmpty()) }
    var bold by rememberSaveable(nodeKey) { mutableStateOf(original.bold) }
    var italic by rememberSaveable(nodeKey) { mutableStateOf(original.italic) }
    var underlined by rememberSaveable(nodeKey) { mutableStateOf(original.underlined) }
    var strikethrough by rememberSaveable(nodeKey) { mutableStateOf(original.strikethrough) }
    var obfuscated by rememberSaveable(nodeKey) { mutableStateOf(original.obfuscated) }

    fun emit() {
        onChanged(
            original.copy(
                text = text,
                color = color.trim().ifEmpty { null },
                bold = bold,
                italic = italic,
                underlined = underlined,
                strikethrough = strikethrough,
                obfuscated = obfuscated
            )
        )
    }

    OutlinedTextField(
        value = text,
        onValueChange = { text = it; emit() },
        label = { Text(stringResource(R.string.with_builder_text)) },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = color,
        onValueChange = { color = it; emit() },
        label = { Text(stringResource(R.string.with_builder_color)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf("red", "gold", "yellow", "green", "aqua", "blue", "light_purple", "white", "gray", "dark_gray", "black")
            .forEach { preset ->
                FilterChip(
                    selected = color == preset,
                    onClick = { color = preset; emit() },
                    label = { Text(preset) }
                )
            }
    }
    TriStateChip(R.string.with_builder_style_bold, bold) { bold = it; emit() }
    TriStateChip(R.string.with_builder_style_italic, italic) { italic = it; emit() }
    TriStateChip(R.string.with_builder_style_underlined, underlined) { underlined = it; emit() }
    TriStateChip(R.string.with_builder_style_strikethrough, strikethrough) { strikethrough = it; emit() }
    TriStateChip(R.string.with_builder_style_obfuscated, obfuscated) { obfuscated = it; emit() }
}

@Composable
private fun InlineTranslateEditor(
    original: WithArgDraft.Translate,
    nodeKey: String,
    onChanged: (WithArgDraft) -> Unit
) {
    var key by rememberSaveable(nodeKey) { mutableStateOf(original.key) }
    var fallback by rememberSaveable(nodeKey) { mutableStateOf(original.fallback.orEmpty()) }

    fun emit() {
        val fallbackOut = when {
            fallback == original.fallback.orEmpty() -> original.fallback
            fallback.isEmpty() -> null
            else -> fallback
        }
        onChanged(original.copy(key = key, fallback = fallbackOut, args = original.args))
    }

    OutlinedTextField(
        value = key,
        onValueChange = { key = it; emit() },
        label = { Text(stringResource(R.string.with_builder_key)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = fallback,
        onValueChange = { fallback = it; emit() },
        label = { Text(stringResource(R.string.with_builder_fallback)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Text(
        stringResource(R.string.with_builder_child_count, original.args.size),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun InlineSelectorEditor(
    original: WithArgDraft.Selector,
    nodeKey: String,
    onChanged: (WithArgDraft) -> Unit
) {
    var selector by rememberSaveable(nodeKey) { mutableStateOf(original.selector) }
    var separator by rememberSaveable(nodeKey) { mutableStateOf(original.separator.orEmpty()) }

    fun emit() {
        val separatorOut = when {
            separator == original.separator.orEmpty() -> original.separator
            separator.isEmpty() -> null
            else -> separator
        }
        onChanged(original.copy(selector = selector, separator = separatorOut))
    }

    OutlinedTextField(
        value = selector,
        onValueChange = { selector = it; emit() },
        label = { Text(stringResource(R.string.with_builder_selector)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = separator,
        onValueChange = { separator = it; emit() },
        label = { Text(stringResource(R.string.with_builder_separator)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun InlineScoreEditor(
    original: WithArgDraft.Score,
    nodeKey: String,
    onChanged: (WithArgDraft) -> Unit
) {
    var name by rememberSaveable(nodeKey) { mutableStateOf(original.name) }
    var objective by rememberSaveable(nodeKey) { mutableStateOf(original.objective) }

    fun emit() = onChanged(original.copy(name = name, objective = objective))

    OutlinedTextField(
        value = name,
        onValueChange = { name = it; emit() },
        label = { Text(stringResource(R.string.with_builder_score_name)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = objective,
        onValueChange = { objective = it; emit() },
        label = { Text(stringResource(R.string.with_builder_score_objective)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun InlineKeybindEditor(
    original: WithArgDraft.Keybind,
    nodeKey: String,
    onChanged: (WithArgDraft) -> Unit
) {
    var keybind by rememberSaveable(nodeKey) { mutableStateOf(original.keybind) }
    OutlinedTextField(
        value = keybind,
        onValueChange = { keybind = it; onChanged(original.copy(keybind = it)) },
        label = { Text(stringResource(R.string.with_builder_keybind)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun InlineNbtEditor(
    original: WithArgDraft.Nbt,
    nodeKey: String,
    onChanged: (WithArgDraft) -> Unit
) {
    var path by rememberSaveable(nodeKey) { mutableStateOf(original.path) }
    var source by rememberSaveable(nodeKey) { mutableStateOf(original.source) }
    var interpret by rememberSaveable(nodeKey) { mutableStateOf(original.interpret) }
    var separator by rememberSaveable(nodeKey) { mutableStateOf(original.separator.orEmpty()) }

    fun emit() {
        val separatorOut = when {
            separator == original.separator.orEmpty() -> original.separator
            separator.isEmpty() -> null
            else -> separator
        }
        onChanged(
            original.copy(
                path = path,
                source = source,
                interpret = interpret,
                separator = separatorOut
            )
        )
    }

    OutlinedTextField(
        value = path,
        onValueChange = { path = it; emit() },
        label = { Text(stringResource(R.string.with_builder_nbt_path)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Text(stringResource(R.string.with_builder_nbt_source), style = MaterialTheme.typography.labelMedium)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FilterChip(
            selected = source == null,
            onClick = { source = null; emit() },
            label = { Text(stringResource(R.string.with_builder_state_unset)) }
        )
        listOf("entity", "block", "storage").forEach { value ->
            FilterChip(
                selected = source == value,
                onClick = { source = value; emit() },
                label = { Text(value) }
            )
        }
    }
    TriStateChip(R.string.with_builder_nbt_interpret, interpret) { interpret = it; emit() }
    OutlinedTextField(
        value = separator,
        onValueChange = { separator = it; emit() },
        label = { Text(stringResource(R.string.with_builder_nbt_separator)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun InlineRawEditor(
    original: WithArgDraft.Raw,
    nodeKey: String,
    onChanged: (WithArgDraft) -> Unit,
    onRawError: (String?) -> Unit
) {
    var text by rememberSaveable(nodeKey) {
        mutableStateOf(WithArgCodec.prettyJson(original.element))
    }
    val parsed = remember(text) { runCatching { WithArgCodec.parseRaw(text) }.getOrNull() }
    LaunchedEffect(text) { onRawError(if (parsed == null) "invalid" else null) }
    DisposableEffect(Unit) { onDispose { onRawError(null) } }
    Text(
        stringResource(R.string.with_builder_raw_hint),
        style = MaterialTheme.typography.labelSmall
    )
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            runCatching { WithArgCodec.parseRaw(it) }.getOrNull()?.let { raw ->
                onChanged(WithArgDraft.Raw(raw))
            }
        },
        minLines = 6,
        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
        modifier = Modifier.fillMaxWidth()
    )
    if (parsed == null) {
        Text(
            stringResource(R.string.with_builder_raw_invalid),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

// ---------------- 路径/树操作 ----------------

private fun parseArgPath(raw: String): List<Int> =
    raw.split('.').mapNotNull { it.toIntOrNull() }

private fun argPathString(path: List<Int>): String = path.joinToString(".")

private fun argAt(args: List<WithArgDraft>, path: List<Int>): WithArgDraft? {
    if (path.isEmpty()) return null
    var list = args
    var current: WithArgDraft? = null
    for ((level, index) in path.withIndex()) {
        current = list.getOrNull(index) ?: return null
        if (level < path.lastIndex) {
            val translate = current as? WithArgDraft.Translate ?: return null
            list = translate.args
        }
    }
    return current
}

private fun listAtPath(args: List<WithArgDraft>, path: List<Int>): List<WithArgDraft>? {
    if (path.isEmpty()) return args
    return (argAt(args, path) as? WithArgDraft.Translate)?.args
}

private fun updateArgListAt(
    args: List<WithArgDraft>,
    parentPath: List<Int>,
    transform: (List<WithArgDraft>) -> List<WithArgDraft>
): List<WithArgDraft> {
    if (parentPath.isEmpty()) return transform(args)
    val index = parentPath.first()
    val node = args.getOrNull(index) as? WithArgDraft.Translate ?: return args
    val child = updateArgListAt(node.args, parentPath.drop(1), transform)
    if (child === node.args) return args
    return args.toMutableList().also { it[index] = node.copy(args = child) }
}

private fun updateArgAt(
    args: List<WithArgDraft>,
    path: List<Int>,
    updated: WithArgDraft
): List<WithArgDraft> {
    if (path.isEmpty()) return args
    val parentPath = path.dropLast(1)
    val index = path.last()
    return updateArgListAt(args, parentPath) { list ->
        list.toMutableList().also { if (index in it.indices) it[index] = updated }
    }
}

private fun moveArg(
    args: List<WithArgDraft>,
    path: List<Int>,
    delta: Int
): List<WithArgDraft> {
    if (path.isEmpty()) return args
    val parentPath = path.dropLast(1)
    val index = path.last()
    return updateArgListAt(args, parentPath) { list ->
        val target = index + delta
        if (index !in list.indices || target !in list.indices) list
        else list.toMutableList().also { it.add(target, it.removeAt(index)) }
    }
}

private fun insertArgAt(
    args: List<WithArgDraft>,
    parentPath: List<Int>,
    index: Int,
    draft: WithArgDraft
): List<WithArgDraft> = updateArgListAt(args, parentPath) { list ->
    list.toMutableList().also { it.add(index.coerceIn(0, it.size), draft) }
}

private fun removeArg(args: List<WithArgDraft>, path: List<Int>): List<WithArgDraft> {
    if (path.isEmpty()) return args
    val parentPath = path.dropLast(1)
    val index = path.last()
    return updateArgListAt(args, parentPath) { list ->
        list.filterIndexed { i, _ -> i != index }
    }
}

private fun addChildArg(
    args: List<WithArgDraft>,
    path: List<Int>,
    draft: WithArgDraft
): List<WithArgDraft> = updateArgListAt(args, path) { it + draft }

// ---------------- 参数列表 ----------------

@Composable
private fun ArgListSection(
    args: List<WithArgDraft>,
    onArgsChange: (List<WithArgDraft>) -> Unit
) {
    var editIndex by remember { mutableStateOf<Int?>(null) }
    var showAdd by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (args.isEmpty()) {
            Text(
                stringResource(R.string.with_builder_args_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        args.forEachIndexed { index, arg ->
            ArgRow(
                arg = arg,
                canMoveUp = index > 0,
                canMoveDown = index < args.lastIndex,
                onEdit = { editIndex = index },
                onMoveUp = {
                    if (index > 0) {
                        val list = args.toMutableList()
                        list.add(index - 1, list.removeAt(index))
                        onArgsChange(list)
                    }
                },
                onMoveDown = {
                    if (index < args.lastIndex) {
                        val list = args.toMutableList()
                        list.add(index + 1, list.removeAt(index))
                        onArgsChange(list)
                    }
                },
                onDelete = {
                    onArgsChange(args.filterIndexed { i, _ -> i != index })
                }
            )
        }
        Box {
            OutlinedButton(onClick = { showAdd = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.with_builder_add))
            }
            DropdownMenu(expanded = showAdd, onDismissRequest = { showAdd = false }) {
                AddKind.values().forEach { kind ->
                    DropdownMenuItem(
                        text = { Text(stringResource(kind.labelRes)) },
                        onClick = {
                            showAdd = false
                            onArgsChange(args + defaultDraft(kind))
                        }
                    )
                }
            }
        }
    }

    editIndex?.let { index ->
        val current = args.getOrNull(index) ?: return@let
        WithArgItemDialog(
            original = current,
            onDismiss = { editIndex = null },
            onApply = { updated ->
                onArgsChange(args.toMutableList().also { it[index] = updated })
                editIndex = null
            }
        )
    }
}

@Composable
private fun ArgRow(
    arg: WithArgDraft,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onEdit: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    kindLabel(arg),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    summaryOf(arg),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                Icon(
                    Icons.Default.KeyboardArrowUp,
                    contentDescription = stringResource(R.string.with_builder_move_up)
                )
            }
            IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.with_builder_move_down)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete)
                )
            }
        }
    }
}

// ---------------- 单项编辑 ----------------

@Composable
private fun WithArgItemDialog(
    original: WithArgDraft,
    onDismiss: () -> Unit,
    onApply: (WithArgDraft) -> Unit
) {
    when (original) {
        is WithArgDraft.Text -> TextItemDialog(original, onDismiss, onApply)
        is WithArgDraft.Translate -> TranslateItemDialog(original, onDismiss, onApply)
        is WithArgDraft.Selector -> SelectorItemDialog(original, onDismiss, onApply)
        is WithArgDraft.Score -> ScoreItemDialog(original, onDismiss, onApply)
        is WithArgDraft.Keybind -> KeybindItemDialog(original, onDismiss, onApply)
        is WithArgDraft.Nbt -> NbtItemDialog(original, onDismiss, onApply)
        is WithArgDraft.Raw -> RawItemDialog(original, onDismiss, onApply)
    }
}

@Composable
private fun TextItemDialog(
    original: WithArgDraft.Text,
    onDismiss: () -> Unit,
    onApply: (WithArgDraft) -> Unit
) {
    var text by remember(original) { mutableStateOf(original.text) }
    var color by remember(original) { mutableStateOf(original.color.orEmpty()) }
    var bold by remember(original) { mutableStateOf(original.bold) }
    var italic by remember(original) { mutableStateOf(original.italic) }
    var underlined by remember(original) { mutableStateOf(original.underlined) }
    var strikethrough by remember(original) { mutableStateOf(original.strikethrough) }
    var obfuscated by remember(original) { mutableStateOf(original.obfuscated) }

    ItemDialog(
        title = kindLabel(original),
        onDismiss = onDismiss,
        onApply = {
            onApply(
                original.copy(
                    text = text,
                    color = color.trim().ifEmpty { null },
                    bold = bold,
                    italic = italic,
                    underlined = underlined,
                    strikethrough = strikethrough,
                    obfuscated = obfuscated
                )
            )
        }
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text(stringResource(R.string.with_builder_text)) },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = color,
            onValueChange = { color = it },
            label = { Text(stringResource(R.string.with_builder_color)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        TriStateChip(R.string.with_builder_style_bold, bold) { bold = it }
        TriStateChip(R.string.with_builder_style_italic, italic) { italic = it }
        TriStateChip(R.string.with_builder_style_underlined, underlined) { underlined = it }
        TriStateChip(R.string.with_builder_style_strikethrough, strikethrough) { strikethrough = it }
        TriStateChip(R.string.with_builder_style_obfuscated, obfuscated) { obfuscated = it }
    }
}

@Composable
private fun TranslateItemDialog(
    original: WithArgDraft.Translate,
    onDismiss: () -> Unit,
    onApply: (WithArgDraft) -> Unit
) {
    var key by remember(original) { mutableStateOf(original.key) }
    var fallback by remember(original) { mutableStateOf(original.fallback.orEmpty()) }
    var args by remember(original) { mutableStateOf(original.args) }

    ItemDialog(
        title = kindLabel(original),
        onDismiss = onDismiss,
        onApply = {
            val fallbackOut = if (fallback == original.fallback.orEmpty()) {
                original.fallback
            } else {
                fallback.ifEmpty { null }
            }
            onApply(original.copy(key = key, fallback = fallbackOut, args = args))
        }
    ) {
        OutlinedTextField(
            value = key,
            onValueChange = { key = it },
            label = { Text(stringResource(R.string.with_builder_key)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = fallback,
            onValueChange = { fallback = it },
            label = { Text(stringResource(R.string.with_builder_fallback)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Text(stringResource(R.string.with_builder_args), style = MaterialTheme.typography.titleSmall)
        ArgListSection(args = args, onArgsChange = { args = it })
    }
}

@Composable
private fun SelectorItemDialog(
    original: WithArgDraft.Selector,
    onDismiss: () -> Unit,
    onApply: (WithArgDraft) -> Unit
) {
    var selector by remember(original) { mutableStateOf(original.selector) }
    var separator by remember(original) { mutableStateOf(original.separator.orEmpty()) }
    ItemDialog(
        title = kindLabel(original),
        onDismiss = onDismiss,
        onApply = {
            val separatorOut = if (separator == original.separator.orEmpty()) {
                original.separator
            } else {
                separator.ifEmpty { null }
            }
            onApply(original.copy(selector = selector, separator = separatorOut))
        }
    ) {
        OutlinedTextField(
            value = selector,
            onValueChange = { selector = it },
            label = { Text(stringResource(R.string.with_builder_selector)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = separator,
            onValueChange = { separator = it },
            label = { Text(stringResource(R.string.with_builder_separator)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ScoreItemDialog(
    original: WithArgDraft.Score,
    onDismiss: () -> Unit,
    onApply: (WithArgDraft) -> Unit
) {
    var name by remember(original) { mutableStateOf(original.name) }
    var objective by remember(original) { mutableStateOf(original.objective) }
    ItemDialog(
        title = kindLabel(original),
        onDismiss = onDismiss,
        onApply = { onApply(original.copy(name = name, objective = objective)) }
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.with_builder_score_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = objective,
            onValueChange = { objective = it },
            label = { Text(stringResource(R.string.with_builder_score_objective)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun KeybindItemDialog(
    original: WithArgDraft.Keybind,
    onDismiss: () -> Unit,
    onApply: (WithArgDraft) -> Unit
) {
    var keybind by remember(original) { mutableStateOf(original.keybind) }
    ItemDialog(
        title = kindLabel(original),
        onDismiss = onDismiss,
        onApply = { onApply(original.copy(keybind = keybind)) }
    ) {
        OutlinedTextField(
            value = keybind,
            onValueChange = { keybind = it },
            label = { Text(stringResource(R.string.with_builder_keybind)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun NbtItemDialog(
    original: WithArgDraft.Nbt,
    onDismiss: () -> Unit,
    onApply: (WithArgDraft) -> Unit
) {
    var path by remember(original) { mutableStateOf(original.path) }
    var source by remember(original) { mutableStateOf(original.source) }
    var interpret by remember(original) { mutableStateOf(original.interpret) }
    var separator by remember(original) { mutableStateOf(original.separator.orEmpty()) }
    ItemDialog(
        title = kindLabel(original),
        onDismiss = onDismiss,
        onApply = {
            val separatorOut = if (separator == original.separator.orEmpty()) {
                original.separator
            } else {
                separator.ifEmpty { null }
            }
            onApply(
                original.copy(
                    path = path,
                    source = source,
                    interpret = interpret,
                    separator = separatorOut
                )
            )
        }
    ) {
        OutlinedTextField(
            value = path,
            onValueChange = { path = it },
            label = { Text(stringResource(R.string.with_builder_nbt_path)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            stringResource(R.string.with_builder_nbt_source),
            style = MaterialTheme.typography.labelMedium
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FilterChip(
                selected = source == null,
                onClick = { source = null },
                label = { Text(stringResource(R.string.with_builder_state_unset)) }
            )
            listOf("entity", "block", "storage").forEach { value ->
                FilterChip(
                    selected = source == value,
                    onClick = { source = value },
                    label = { Text(value) }
                )
            }
        }
        TriStateChip(R.string.with_builder_nbt_interpret, interpret) { interpret = it }
        OutlinedTextField(
            value = separator,
            onValueChange = { separator = it },
            label = { Text(stringResource(R.string.with_builder_nbt_separator)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun RawItemDialog(
    original: WithArgDraft.Raw,
    onDismiss: () -> Unit,
    onApply: (WithArgDraft) -> Unit
) {
    var text by remember(original) { mutableStateOf(WithArgCodec.prettyJson(original.element)) }
    val parsed = remember(text) {
        runCatching { WithArgCodec.parseRaw(text) }.getOrNull()
    }
    ItemDialog(
        title = kindLabel(original),
        onDismiss = onDismiss,
        onApply = { parsed?.let { onApply(WithArgDraft.Raw(it)) } },
        applyEnabled = parsed != null
    ) {
        Text(
            stringResource(R.string.with_builder_raw_hint),
            style = MaterialTheme.typography.labelSmall
        )
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            minLines = 6,
            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            modifier = Modifier.fillMaxWidth()
        )
        if (parsed == null) {
            Text(
                stringResource(R.string.with_builder_raw_invalid),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

// ---------------- JSON / 预览 / 提示 ----------------

@Composable
private fun JsonTab(state: WithBuilderState, onRawJsonChange: (String) -> Unit) {
    Text(
        stringResource(R.string.with_builder_json_hint),
        style = MaterialTheme.typography.labelSmall
    )
    OutlinedTextField(
        value = state.rawJson,
        onValueChange = onRawJsonChange,
        minLines = 8,
        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
        modifier = Modifier.fillMaxWidth()
    )
    state.rawError?.let {
        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun JsonPreview(state: WithBuilderState) {
    val preview = remember(state.mode, state.args, state.rawJson) {
        when (state.mode) {
            WithBuilderMode.VISUAL -> WithArgCodec.serializePretty(state.args)
            WithBuilderMode.JSON -> runCatching {
                val normalized = WithArgCodec.normalizeArgsJson(state.rawJson)
                WithArgCodec.prettyJson(JsonParser.parseString(normalized))
            }.getOrNull() ?: state.rawJson
        }
    }
    Text(stringResource(R.string.with_builder_preview), style = MaterialTheme.typography.titleSmall)
    SelectionContainer {
        Text(
            preview,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            maxLines = 14,
            overflow = TextOverflow.Ellipsis
        )
    }
    if (state.args.any { it is WithArgDraft.Text && '§' in it.text }) {
        Text(
            stringResource(R.string.with_builder_section_plain_hint),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun BedrockNotes(args: List<WithArgDraft>) {
    val issues = remember(args) { WithArgCodec.bedrockIssues(args) }
    if (issues.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        issues.forEach { issue ->
            Text(
                stringResource(
                    when (issue) {
                        WithArgCodec.BedrockIssue.STYLES -> R.string.with_builder_bedrock_styles
                        WithArgCodec.BedrockIssue.SELECTOR_SEPARATOR -> R.string.with_builder_bedrock_separator
                        WithArgCodec.BedrockIssue.KEYBIND -> R.string.with_builder_bedrock_keybind
                        WithArgCodec.BedrockIssue.NBT -> R.string.with_builder_bedrock_nbt
                        WithArgCodec.BedrockIssue.FALLBACK -> R.string.with_builder_bedrock_fallback
                    }
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ---------------- 通用小组件 ----------------

@Composable
private fun ItemDialog(
    title: String,
    onDismiss: () -> Unit,
    onApply: () -> Unit,
    applyEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                content()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = onApply, enabled = applyEnabled) {
                        Text(stringResource(R.string.with_builder_apply))
                    }
                }
            }
        }
    }
}

@Composable
private fun TriStateChip(labelRes: Int, value: Boolean?, onChange: (Boolean?) -> Unit) {
    val stateText = stringResource(
        when (value) {
            null -> R.string.with_builder_state_unset
            true -> R.string.with_builder_state_on
            false -> R.string.with_builder_state_off
        }
    )
    AssistChip(
        onClick = {
            onChange(
                when (value) {
                    null -> true
                    true -> false
                    false -> null
                }
            )
        },
        label = { Text("${stringResource(labelRes)}：$stateText") }
    )
}

// ---------------- 类型映射 ----------------

private enum class AddKind {
    TEXT, TRANSLATE, SELECTOR, SCORE, KEYBIND, NBT, RAW
}

private val AddKind.labelRes: Int
    get() = when (this) {
        AddKind.TEXT -> R.string.with_builder_kind_text
        AddKind.TRANSLATE -> R.string.with_builder_kind_translate
        AddKind.SELECTOR -> R.string.with_builder_kind_selector
        AddKind.SCORE -> R.string.with_builder_kind_score
        AddKind.KEYBIND -> R.string.with_builder_kind_keybind
        AddKind.NBT -> R.string.with_builder_kind_nbt
        AddKind.RAW -> R.string.with_builder_kind_raw
    }

private fun defaultDraft(kind: AddKind): WithArgDraft = when (kind) {
    AddKind.TEXT -> WithArgDraft.Text("")
    AddKind.TRANSLATE -> WithArgDraft.Translate("")
    AddKind.SELECTOR -> WithArgDraft.Selector("")
    AddKind.SCORE -> WithArgDraft.Score("", "")
    AddKind.KEYBIND -> WithArgDraft.Keybind("")
    AddKind.NBT -> WithArgDraft.Nbt("")
    AddKind.RAW -> WithArgDraft.Raw(com.google.gson.JsonObject())
}

@Composable
private fun kindLabel(draft: WithArgDraft): String = stringResource(
    when (draft) {
        is WithArgDraft.Text -> R.string.with_builder_kind_text
        is WithArgDraft.Translate -> R.string.with_builder_kind_translate
        is WithArgDraft.Selector -> R.string.with_builder_kind_selector
        is WithArgDraft.Score -> R.string.with_builder_kind_score
        is WithArgDraft.Keybind -> R.string.with_builder_kind_keybind
        is WithArgDraft.Nbt -> R.string.with_builder_kind_nbt
        is WithArgDraft.Raw -> R.string.with_builder_kind_raw
    }
)

private fun summaryOf(draft: WithArgDraft): String = when (draft) {
    is WithArgDraft.Text -> buildString {
        append('"').append(draft.text).append('"')
        if (draft.color != null) append(" · ").append(draft.color)
        val flags = buildList {
            if (draft.bold == true) add("bold")
            if (draft.italic == true) add("italic")
            if (draft.underlined == true) add("underlined")
            if (draft.strikethrough == true) add("strikethrough")
            if (draft.obfuscated == true) add("obfuscated")
        }
        if (flags.isNotEmpty()) append(" · ").append(flags.joinToString("/"))
    }
    is WithArgDraft.Translate ->
        draft.key + if (draft.args.isEmpty()) "" else " (${draft.args.size})"
    is WithArgDraft.Selector -> draft.selector
    is WithArgDraft.Score -> "${draft.name}:${draft.objective}"
    is WithArgDraft.Keybind -> draft.keybind
    is WithArgDraft.Nbt -> draft.path
    is WithArgDraft.Raw -> draft.element.toString().take(80)
}
