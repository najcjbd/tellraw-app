package com.tellraw.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.tellraw.app.R
import com.tellraw.app.util.CommandInputParser
import com.tellraw.app.util.SimpleTranslatePlanner
import com.tellraw.app.util.SimpleTranslatePlanner.Alias
import com.tellraw.app.util.SimpleTranslatePlanner.Row
import com.tellraw.app.util.SimpleTranslatePlanner.Table
import com.tellraw.app.util.VersionDiff
import com.tellraw.app.util.WithArgCodec
import com.tellraw.app.util.WithArgDraft

/**
 * Simpler translate：按收信人选择器参数替换消息里的 token。
 *
 * 表格只填参数（tag/scores 等）；基础选择器来自主界面。
 * 生成由 [SimpleTranslatePlanner] 完成，本对话框只负责编辑和还原。
 */
@Composable
fun SimpleTranslateGroupDialog(
    initialBlockJson: String? = null,
    onComplete: (blockJson: String, warnings: List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val initialTable = remember(initialBlockJson) { SimpleTranslatePlanner.decode(initialBlockJson) }
    val rows = remember {
        mutableStateListOf<Row>().apply {
            addAll(initialTable?.rows.orEmpty().ifEmpty { listOf(Row(id = "r0")) })
        }
    }
    val aliases = remember {
        mutableStateListOf<Alias>().apply { addAll(initialTable?.aliases.orEmpty()) }
    }
    var nextRow by remember { mutableStateOf(nextRowNumber(rows.toList())) }
    var expandedId by remember { mutableStateOf<String?>(null) }
    var expandedAliasIndex by remember { mutableStateOf<Int?>(null) }
    var restoreInput by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf("") }
    var bedrockFirstMatch by rememberSaveable {
        mutableStateOf(initialTable?.bedrockFirstMatch == true)
    }

    fun apply() {
        val problems = validate(rows.toList(), aliases.toList())
        if (problems.isNotEmpty()) {
            notice = problems.joinToString("\n")
            return
        }
        val table = Table(rows.toList(), aliases.toList(), bedrockFirstMatch)
        val notes = buildList {
            if (rows.size > 1 && rows.take(rows.size - 1).any { it.params.isBlank() }) {
                add("无参数行不是最后一行，后面的行会被生成器跳过")
            }
        }
        onComplete(SimpleTranslatePlanner.encode(table), notes)
        onDismiss()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp), tonalElevation = 6.dp) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .heightIn(max = 780.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Simpler translate", style = MaterialTheme.typography.titleMedium)
                Text(
                    "收信人选择器 = 主界面选择器 + 这里填的参数。每行只替换消息里 Simpler token 的位置，" +
                        "token 前后的文字会保留。Java 按行第一匹配。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text("收信人参数 → 替换内容", style = MaterialTheme.typography.titleSmall)
                rows.forEachIndexed { index, row ->
                    Surface(tonalElevation = 1.dp) {
                        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("#${index + 1}", style = MaterialTheme.typography.labelMedium)
                                if (row.params.isBlank()) {
                                    Text(
                                        "  回退行（匹配其余所有人）",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                                Spacer(Modifier.weight(1f))
                                TextButton(onClick = {
                                    rows.removeAt(index)
                                    if (expandedId == row.id) expandedId = null
                                }) { Text("删除") }
                            }
                            OutlinedTextField(
                                value = row.params,
                                onValueChange = { rows[index] = row.copy(params = it) },
                                label = { Text("参数，例如 tag=vip 或 scores={level=5..}") },
                                supportingText = { Text("不要写 @a；留空表示“其余所有人”") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Text(
                                contentSummary(row.content),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(onClick = {
                                expandedId = if (expandedId == row.id) null else row.id
                            }) {
                                Text(if (expandedId == row.id) "收起内容" else "编辑内容")
                            }
                            if (expandedId == row.id) {
                                MessageContentEditor(
                                    args = row.content,
                                    onArgsChange = { rows[index] = row.copy(content = it) }
                                )
                            }
                        }
                    }
                }
                OutlinedButton(onClick = {
                    val id = "r${nextRow++}"
                    rows.add(Row(id = id))
                    expandedId = id
                }) { Text("添加收信人参数") }

                HorizontalDivider()
                Text("别名表（只替换整段文字，且只解析一层）", style = MaterialTheme.typography.titleSmall)
                Text(
                    "内容里有一段文字完全等于左边时，整段换成右边；替换结果里的同名文字不会再次替换。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                aliases.forEachIndexed { index, alias ->
                    Surface(tonalElevation = 1.dp) {
                    Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            value = alias.from,
                            onValueChange = { aliases[index] = alias.copy(from = it) },
                            label = { Text("被替换的整段文字") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                contentSummary(alias.to),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(onClick = {
                                expandedAliasIndex = if (expandedAliasIndex == index) null else index
                            }) { Text("编辑别名内容") }
                            TextButton(onClick = {
                                aliases.removeAt(index)
                                expandedAliasIndex = expandedAliasIndex?.let { e ->
                                    when {
                                        e == index -> null
                                        e > index -> e - 1
                                        else -> e
                                    }
                                }
                            }) { Text("删除") }
                        }
                        if (expandedAliasIndex == index) {
                            MessageContentEditor(
                                args = alias.to,
                                onArgsChange = { aliases[index] = alias.copy(to = it) }
                            )
                        }
                    }
                    }
                }
                OutlinedButton(onClick = {
                    aliases.add(Alias(from = "", to = listOf(WithArgDraft.Text(""))))
                }) { Text("添加别名") }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = bedrockFirstMatch,
                        onCheckedChange = { bedrockFirstMatch = it }
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "基岩也用多条 tellraw 做第一匹配（不用单条条件 rawtext）",
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                HorizontalDivider()
                Text("从生成结果还原", style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(
                    value = restoreInput,
                    onValueChange = { restoreInput = it },
                    label = { Text("粘贴 tellraw / execute+tellraw 行，或基岩 conditional rawtext") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(onClick = {
                    val looksBedrock = restoreInput.contains("\"rawtext\"")
                    val javaRows = if (looksBedrock) emptyList()
                    else restoreRowsFromJava(restoreInput, context, VersionDiff.NbtSyntax.MODERN)
                    val bedrockRows = if (looksBedrock || javaRows.isEmpty()) {
                        restoreRowsFromBedrock(restoreInput)
                    } else emptyList()
                    val restored = javaRows.ifEmpty { bedrockRows }
                    if (restored.isEmpty()) {
                        notice = "没有识别到可还原的收信人内容"
                    } else {
                        rows.clear()
                        restored.forEachIndexed { i, row -> rows.add(row.copy(id = "r$i")) }
                        nextRow = restored.size
                        expandedId = null
                        notice = "已还原 ${restored.size} 条" + if (javaRows.isNotEmpty()) " Java 收信人内容" else "基岩条件内容"
                    }
                }) { Text("还原到参数表") }
                if (notice.isNotBlank()) {
                    Text(
                        notice,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (notice.startsWith("已还原")) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                    Button(onClick = { apply() }) { Text("完成") }
                }
            }
        }
    }
}

private fun nextRowNumber(rows: List<Row>): Int {
    val max = rows.mapNotNull { row ->
        row.id.takeIf { it.startsWith("r") }?.drop(1)?.toIntOrNull()
    }.maxOrNull() ?: -1
    return max + 1
}

private fun validate(rows: List<Row>, aliases: List<Alias>): List<String> {
    val problems = mutableListOf<String>()
    if (rows.isEmpty()) problems.add("至少添加一条收信人参数")
    val seenParams = mutableSetOf<String>()
    rows.forEachIndexed { index, row ->
        val p = row.params.trim()
        if (p.startsWith("@")) {
            problems.add("第 ${index + 1} 行不要写 @a/@p；只写方括号里的参数")
        }
        if (p.isNotEmpty() && !seenParams.add(p)) {
            problems.add("第 ${index + 1} 行的参数与前面重复；第一匹配下它永远不会命中")
        }
        if (!WithArgCodec.isComplete(row.content)) {
            problems.add("第 ${index + 1} 行有未填完的文本组件（例如空 translate key / score）")
        }
    }
    val seenAliases = mutableSetOf<String>()
    aliases.forEachIndexed { index, alias ->
        if (alias.from.isNotBlank() && !seenAliases.add(alias.from)) {
            problems.add("别名「${alias.from}」重复")
        }
        if (!WithArgCodec.isComplete(alias.to)) {
            problems.add("第 ${index + 1} 条别名的替换内容有未填完的文本组件")
        }
    }
    return problems
}

private fun contentSummary(args: List<WithArgDraft>): String =
    WithArgCodec.serialize(args).let { if (it.length <= 100) it else it.take(100) + "…" }

private fun restoreRowsFromJava(
    text: String,
    context: android.content.Context,
    nbtSyntax: VersionDiff.NbtSyntax
): List<Row> {
    val out = mutableListOf<Row>()
    var id = 0
    text.lines().map { it.trim() }.filter { it.isNotEmpty() }.forEach { line ->
        val parsed = runCatching { CommandInputParser.parse(line, context, nbtSyntax) }.getOrNull()
            ?: return@forEach
        val args = runCatching { parseJavaContent(parsed.javaJson) }.getOrNull()
            ?: return@forEach
        if (args.isEmpty()) return@forEach
        val execAs = Regex("\\bas\\s+(@[aepnrs](?:\\[[^\\]]*\\])?)")
            .find(parsed.executePrefix.orEmpty())?.groupValues?.getOrNull(1)
        val selector = execAs ?: parsed.selector ?: "@a"
        val params = if ('[' in selector && selector.endsWith("]")) {
            selector.substringAfter('[').dropLast(1)
        } else ""
        out.add(Row(id = "r${id++}", params = params, content = args))
    }
    return out
}

/** 把 Java 根组件 + extra 拆成平铺的 with 项；否则第一匹配还原会丢掉 extra 里的兄弟组件。 */
private fun parseJavaContent(json: String): List<WithArgDraft> {
    val root = JsonParser.parseString(json)
    if (root.isJsonArray) return WithArgCodec.parseArgs(root.toString())
    if (!root.isJsonObject) return WithArgCodec.parseArgs(json)
    val obj = root.asJsonObject
    val extra = obj.getAsJsonArray("extra") ?: return WithArgCodec.parseArgs(json)
    val flat = JsonArray()
    val own = obj.deepCopy()
    own.remove("extra")
    flat.add(own)
    extra.forEach { flat.add(it) }
    return WithArgCodec.parseArgs(flat.toString())
}

private fun restoreRowsFromBedrock(text: String): List<Row> {
    val start = text.indexOf('{')
    val end = text.lastIndexOf('}')
    if (start < 0 || end <= start) return emptyList()
    val root = runCatching { JsonParser.parseString(text.substring(start, end + 1)) }.getOrNull()
        ?: return emptyList()
    val rawtext = root.asJsonObject.getAsJsonArray("rawtext") ?: return emptyList()
    val out = mutableListOf<Row>()
    var id = 0
    rawtext.forEach { el ->
        val row = el.takeIf { it.isJsonObject }?.asJsonObject ?: return@forEach
        val with = row.getAsJsonObject("with") ?: return@forEach
        val args = with.getAsJsonArray("rawtext") ?: return@forEach
        if (args.size() < 2) return@forEach
        val selector = args[0].takeIf { it.isJsonObject }
            ?.asJsonObject?.get("selector")?.takeIf { it.isJsonPrimitive }?.asString
            ?: return@forEach
        val params = if ('[' in selector && selector.endsWith("]")) {
            selector.substringAfter('[').dropLast(1)
        } else ""
        val content = JsonArray()
        for (i in 1 until args.size()) {
            val itemEl = args[i]
            val nested = itemEl.takeIf { it.isJsonObject }
                ?.asJsonObject?.getAsJsonArray("rawtext")
            if (nested != null) nested.forEach { content.add(it) } else content.add(itemEl)
        }
        val drafts = runCatching { WithArgCodec.parseArgs(content.toString()) }.getOrNull()
            ?: return@forEach
        if (drafts.isNotEmpty()) out.add(Row(id = "b${id++}", params = params, content = drafts))
    }
    return out
}
