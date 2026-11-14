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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import com.google.gson.JsonParser
import com.tellraw.app.R
import com.tellraw.app.model.SelectorType
import com.tellraw.app.model.VersionSpec
import com.tellraw.app.util.CommandInputParser
import com.tellraw.app.util.ExecuteConverter
import com.tellraw.app.util.SelectorConverter
import com.tellraw.app.util.TextComponentHelper
import com.tellraw.app.util.VersionDiff
import com.tellraw.app.util.WithArgCodec
import com.tellraw.app.util.WithArgDraft

data class TranslateParamRow(
    val id: String,
    val params: String = "",
    val content: List<WithArgDraft> = listOf(WithArgDraft.Text(""))
)

data class TranslateAlias(
    val from: String,
    val to: List<WithArgDraft> = listOf(WithArgDraft.Text(""))
)

/**
 * Simpler translate 的收信人参数表：
 *  - 参数只写 tag=... / scores=...，程序补 @a / @s；
 *  - 每行内容可任意嵌套文本组件；
 *  - 别名只做一层文本替换；
 *  - Java 生成多条 tellraw，基岩生成一条 conditional rawtext。
 */
@Composable
fun SimpleTranslateGroupDialog(
    initialPrefix: String,
    target: VersionSpec?,
    nbtSyntax: VersionDiff.NbtSyntax,
    allowMultiCmdTag: Boolean,
    chainAnchor: Boolean,
    onComplete: (String, String, List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var prefix by rememberSaveable { mutableStateOf(initialPrefix) }
    val rows = remember { mutableStateListOf(TranslateParamRow("r0")) }
    val aliases = remember { mutableStateListOf<TranslateAlias>() }
    var nextRow by remember { mutableStateOf(1) }
    var nextAlias by remember { mutableStateOf(0) }
    var expandedId by remember { mutableStateOf<String?>(null) }
    var expandedAliasIndex by remember { mutableStateOf<Int?>(null) }
    var restoreInput by remember { mutableStateOf("") }
    var restoreNotice by remember { mutableStateOf("") }
    fun apply() {
        val result = buildSimpleTranslateGroups(
            context, prefix, rows.toList(), aliases.toList(), target, nbtSyntax,
            allowMultiCmdTag, chainAnchor
        )
        onComplete(result.java, result.bedrock, result.warnings)
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
                    "填选择器参数；收信人选择器由程序补。按行第一匹配优先，Java 会排除前面已命中的收信人。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = prefix,
                    onValueChange = { prefix = it },
                    label = { Text(stringResource(R.string.input_execute_prefix)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                Text("收信人参数 → 内容", style = MaterialTheme.typography.titleSmall)
                rows.forEachIndexed { index, row ->
                    Surface(tonalElevation = 1.dp) {
                        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("#${index + 1}", style = MaterialTheme.typography.labelMedium)
                                Spacer(Modifier.weight(1f))
                                TextButton(onClick = { rows.removeAt(index) }) { Text("删除") }
                            }
                            OutlinedTextField(
                                value = row.params,
                                onValueChange = { rows[index] = row.copy(params = it) },
                                label = { Text("填选择器参数") },
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
                    rows.add(TranslateParamRow(id))
                    expandedId = id
                }) { Text("添加收信人参数") }

                HorizontalDivider()
                Text("别名表（只解析一层）", style = MaterialTheme.typography.titleSmall)
                Text(
                    "左边输入的文字，右边是替换后的文字。a=b 只把 a 变成 b，不会继续 b→c。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                aliases.forEachIndexed { index, alias ->
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = alias.from,
                                onValueChange = { aliases[index] = alias.copy(from = it) },
                                label = { Text("输入文字") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            Spacer(Modifier.width(4.dp))
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
                                if (expandedAliasIndex == index) expandedAliasIndex = null
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
                OutlinedButton(onClick = {
                    aliases.add(TranslateAlias("", listOf(WithArgDraft.Text(""))))
                }) { Text("添加别名") }

                HorizontalDivider()
                Text("从 Java 生成结果还原", style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(
                    value = restoreInput,
                    onValueChange = { restoreInput = it },
                    label = { Text("粘贴 tellraw / execute+tellraw 行") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(onClick = {
                    val restored = restoreRowsFromJava(restoreInput, context, nbtSyntax)
                        .ifEmpty { restoreRowsFromBedrock(restoreInput) }
                    if (restored.isEmpty()) {
                        restoreNotice = "没有识别到可还原的 Java tellraw 行"
                    } else {
                        rows.clear()
                        rows.addAll(restored)
                        restoreNotice = "已还原 ${restored.size} 条 Java 收信人内容"
                    }
                }) { Text("还原到参数表") }
                if (restoreNotice.isNotBlank()) {
                    Text(restoreNotice, style = MaterialTheme.typography.labelSmall)
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                    Button(onClick = { apply() }) { Text("完成") }
                }

            }
        }
    }
}

private fun contentSummary(args: List<WithArgDraft>): String =
    WithArgCodec.serialize(args).let { if (it.length <= 100) it else it.take(100) + "…" }

private data class SimpleTranslateOutput(val java: String, val bedrock: String, val warnings: List<String>)

private fun restoreRowsFromJava(
    text: String,
    context: android.content.Context,
    nbtSyntax: VersionDiff.NbtSyntax
): List<TranslateParamRow> {
    val out = mutableListOf<TranslateParamRow>()
    var id = 0
    text.lines().map { it.trim() }.filter { it.isNotEmpty() }.forEach { line ->
        val parsed = runCatching { CommandInputParser.parse(line, context, nbtSyntax) }.getOrNull()
            ?: return@forEach
        val args = runCatching { WithArgCodec.parseArgs(parsed.javaJson) }.getOrNull()
            ?: return@forEach
        if (args.isEmpty()) return@forEach
        val execAs = Regex("\\bas\\s+(@a(?:\\[[^\\]]*\\])?)")
            .find(parsed.executePrefix.orEmpty())?.groupValues?.getOrNull(1)
        val selector = execAs ?: parsed.selector ?: "@a"
        val params = if (selector.startsWith("@a[") && selector.endsWith("]")) {
            selector.substring(3, selector.length - 1)
        } else ""
        out.add(TranslateParamRow("r${id++}", params, args))
    }
    return out
}

private fun restoreRowsFromBedrock(text: String): List<TranslateParamRow> {
    val start = text.indexOf('{')
    val end = text.lastIndexOf('}')
    if (start < 0 || end <= start) return emptyList()
    val root = runCatching { JsonParser.parseString(text.substring(start, end + 1)) }.getOrNull()
        ?: return emptyList()
    val rawtext = root.asJsonObject.getAsJsonArray("rawtext") ?: return emptyList()
    val out = mutableListOf<TranslateParamRow>()
    var id = 0
    rawtext.forEach { el ->
        val row = el.takeIf { it.isJsonObject }?.asJsonObject ?: return@forEach
        val with = row.getAsJsonObject("with") ?: return@forEach
        val args = with.getAsJsonArray("rawtext") ?: return@forEach
        if (args.size() < 2) return@forEach
        val selector = args[0].takeIf { it.isJsonObject }
            ?.asJsonObject?.get("selector")?.takeIf { it.isJsonPrimitive }?.asString
            ?: return@forEach
        val params = if (selector.startsWith("@s[") && selector.endsWith("]")) {
            selector.substring(3, selector.length - 1)
        } else ""
        val content = com.google.gson.JsonArray()
        for (i in 1 until args.size()) content.add(args[i])
        val drafts = runCatching { WithArgCodec.parseArgs(content.toString()) }.getOrNull()
            ?: return@forEach
        if (drafts.isNotEmpty()) out.add(TranslateParamRow("b${id++}", params, drafts))
    }
    return out
}

private fun buildSimpleTranslateGroups(
    context: android.content.Context,
    prefix: String,
    rows: List<TranslateParamRow>,
    aliases: List<TranslateAlias>,
    target: VersionSpec?,
    nbtSyntax: VersionDiff.NbtSyntax,
    allowMultiCmdTag: Boolean,
    chainAnchor: Boolean
): SimpleTranslateOutput {
    val warnings = mutableListOf<String>()
    val aliasMap = aliases
        .filter { it.from.isNotBlank() }
        .associate { it.from.trim() to it.to }
    // 只解析一层：命中别名时直接放入别名内容，不再继续解析别名内容里的别名。
    fun applyAliasList(args: List<WithArgDraft>): List<WithArgDraft> = args.flatMap { draft ->
        when (draft) {
            is WithArgDraft.Text -> {
                val replacement = if (draft.text.trim() == draft.text) aliasMap[draft.text.trim()] else null
                replacement ?: listOf(draft)
            }
            is WithArgDraft.Translate -> listOf(draft.copy(args = applyAliasList(draft.args)))
            else -> listOf(draft)
        }
    }

    val java = mutableListOf<String>()
    val bedrockRows = mutableListOf<Any>()

    rows.forEachIndexed { index, row ->
        val contents = applyAliasList(row.content)
        val contentJson = WithArgCodec.serialize(contents)
        if (contentJson == "[]") return@forEachIndexed
        val params = row.params.trim()
        if (params.startsWith("@")) {
            warnings.add("第 ${index + 1} 行参数不要写 @a；只写 tag/scores 等参数，程序会补选择器")
        }
        val earlierParams = rows.take(index)
            .map { it.params.trim() }
            .filter { it.isNotBlank() }
        if (rows.take(index).any { it.params.isBlank() }) {
            warnings.add("第 ${index + 1} 行前面存在无参数行；第一匹配优先时该行不会再有收信人")
            return@forEachIndexed
        }
        val javaTarget = if (params.isBlank()) "@a" else "@a[$params]"
        val exclusions = earlierParams.joinToString("") { " unless entity @s[$it]" }
        val jPlan = if (exclusions.isBlank() && prefix.isNotBlank() && allowMultiCmdTag) {
            ExecuteConverter.buildMultiCommandTagPlan(
                prefix, ExecuteConverter.Direction.BEDROCK_TO_JAVA,
                javaTarget, contentJson, nbtSyntax,
                tagSuffix = "sg${index}"
            )
        } else null
        if (jPlan != null) {
            java.add(if (chainAnchor) ExecuteConverter.anchorCommandChain(jPlan.commands).joinToString("\n")
            else jPlan.commands.joinToString("\n"))
            warnings.addAll(jPlan.warnings)
        } else {
            // Java 用第一匹配优先：后面的行排除前面已经命中的实体。
            val base = if (prefix.isBlank()) "execute" else prefix
            java.add("$base as $javaTarget$exclusions run tellraw @s $contentJson")
        }

        val parsed = runCatching { CommandInputParser.parse("tellraw @a $contentJson", context, nbtSyntax) }.getOrNull()
        if (parsed == null) {
            warnings.add("第 ${index + 1} 行内容无法转成基岩 rawtext，已跳过")
            return@forEachIndexed
        }
        val bJson = TextComponentHelper.convertToBedrockJson(parsed.components, context = context, warnings = warnings, nbtSyntax = nbtSyntax, target = target)
        val rawItems = runCatching {
            JsonParser.parseString(bJson).asJsonObject.getAsJsonArray("rawtext")
        }.getOrNull()
        if (rawItems == null) {
            warnings.add("第 ${index + 1} 行基岩 rawtext 解析失败，已跳过")
            return@forEachIndexed
        }
        if (earlierParams.isNotEmpty()) {
            warnings.add("基岩单条 conditional rawtext 无法表达第一匹配排除；第 ${index + 1} 行与前面参数重叠时会显示多条")
        }
        val args = com.google.gson.JsonArray()
        val condSel = if (params.isBlank()) "@s" else "@s[$params]"
        args.add(com.google.gson.JsonObject().apply { addProperty("selector", condSel) })
        if (rawItems.size() == 1) {
            args.add(rawItems[0])
        } else {
            args.add(com.google.gson.JsonObject().apply {
                add("rawtext", rawItems)
            })
        }
        val rowObj = com.google.gson.JsonObject().apply {
            addProperty("translate", "%%2")
            add("with", com.google.gson.JsonObject().apply { add("rawtext", args) })
        }
        bedrockRows.add(rowObj)
    }

    val bedrock = if (bedrockRows.isEmpty()) "" else {
        val raw = com.google.gson.JsonObject().apply {
            add("rawtext", com.google.gson.JsonArray().apply { bedrockRows.forEach { add(it as com.google.gson.JsonElement) } })
        }
        val rem = mutableListOf<String>()
        val converted = if (prefix.isBlank()) "execute as @a" else
            ExecuteConverter.convertExecutePrefix(prefix, ExecuteConverter.Direction.JAVA_TO_BEDROCK, rem, nbtSyntax) ?: prefix
        warnings.addAll(rem)
        "$converted run tellraw @s $raw"
    }
    warnings.add("Java 按行第一匹配优先，后面的行会排除前面命中的实体；基岩条件 rawtext 无法表达该排除，重叠时可能显示多条。")
    return SimpleTranslateOutput(java.joinToString("\n"), bedrock, warnings.distinct())
}
