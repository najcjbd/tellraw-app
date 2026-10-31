package com.tellraw.app.ui.components

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
                    Text(stringResource(R.string.with_builder_args), style = MaterialTheme.typography.titleSmall)
                    ArgListSection(args = state.args, onArgsChange = onArgsChange)
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
                            (state.mode == WithBuilderMode.VISUAL || state.rawError == null)
                    ) {
                        Text(stringResource(R.string.with_builder_apply))
                    }
                }
            }
        }
    }
}

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
            onApply(original.copy(key = key, fallback = fallback.ifEmpty { null }, args = args))
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
        onApply = { onApply(original.copy(selector = selector, separator = separator.ifEmpty { null })) }
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
            onApply(
                original.copy(
                    path = path,
                    source = source,
                    interpret = interpret,
                    separator = separator.ifEmpty { null }
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
