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

data class TranslateAlias(val from: String, val to: String)

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
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var prefix by rememberSaveable { mutableStateOf(initialPrefix) }
    val rows = remember { mutableStateListOf(TranslateParamRow("r0")) }
    val aliases = remember { mutableStateListOf<TranslateAlias>() }
    var nextRow by remember { mutableStateOf(1) }
    var nextAlias by remember { mutableStateOf(0) }
    var expandedId by remember { mutableStateOf<String?>(null) }
    var javaOutput by remember { mutableStateOf("") }
    var bedrockOutput by remember { mutableStateOf("") }
    var warnings by remember { mutableStateOf<List<String>>(emptyList()) }

    fun generate() {
        val result = buildSimpleTranslateGroups(
            context, prefix, rows.toList(), aliases.toList(), target, nbtSyntax,
            allowMultiCmdTag, chainAnchor
        )
        javaOutput = result.java
        bedrockOutput = result.bedrock
        warnings = result.warnings
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
                Text("简单 translate 收信人表", style = MaterialTheme.typography.titleMedium)
                Text(
                    "参数只写 tag=... / scores=...；程序会在 Java 补 @a[...]，在基岩条件 rawtext 里补 @s[...]。",
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
                                label = { Text("参数：tag=admin / scores={vip=1..}") },
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
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = alias.from,
                            onValueChange = { aliases[index] = alias.copy(from = it) },
                            label = { Text("输入") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Spacer(Modifier.width(4.dp))
                        OutlinedTextField(
                            value = alias.to,
                            onValueChange = { aliases[index] = alias.copy(to = it) },
                            label = { Text("别名/替换") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        TextButton(onClick = { aliases.removeAt(index) }) { Text("删除") }
                    }
                }
                OutlinedButton(onClick = { aliases.add(TranslateAlias("", "")) }) {
                    Text("添加别名")
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                    Button(onClick = { generate() }) { Text("生成") }
                }

                if (javaOutput.isNotBlank() || bedrockOutput.isNotBlank()) {
                    HorizontalDivider()
                    Text("输出", style = MaterialTheme.typography.titleSmall)
                    SelectionContainer { Text(javaOutput, style = MaterialTheme.typography.bodySmall) }
                    SelectionContainer { Text(bedrockOutput, style = MaterialTheme.typography.bodySmall) }
                }
                warnings.forEach {
                    Text("• $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

private fun contentSummary(args: List<WithArgDraft>): String =
    WithArgCodec.serialize(args).let { if (it.length <= 100) it else it.take(100) + "…" }

private data class SimpleTranslateOutput(val java: String, val bedrock: String, val warnings: List<String>)

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
    fun applyAlias(draft: WithArgDraft): WithArgDraft = when (draft) {
        is WithArgDraft.Text -> {
            val replacement = aliasMap[draft.text.trim()]
            if (replacement != null && draft.text.trim() == draft.text) draft.copy(text = replacement) else draft
        }
        is WithArgDraft.Translate -> draft.copy(args = draft.args.map(::applyAlias))
        else -> draft
    }

    val java = mutableListOf<String>()
    val bedrockRows = mutableListOf<Any>()

    rows.forEachIndexed { index, row ->
        val contents = row.content.map(::applyAlias)
        val contentJson = WithArgCodec.serialize(contents)
        if (contentJson == "[]") return@forEachIndexed
        val params = row.params.trim()
        if (params.startsWith("@")) {
            warnings.add("第 ${index + 1} 行参数不要写 @a；只写 tag/scores 等参数，程序会补选择器")
        }
        val javaTarget = if (params.isBlank()) "@a" else "@a[$params]"
        val jPlan = if (prefix.isNotBlank() && allowMultiCmdTag) {
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
            val rem = mutableListOf<String>()
            val converted = if (prefix.isBlank()) null else
                ExecuteConverter.convertExecutePrefix(prefix, ExecuteConverter.Direction.BEDROCK_TO_JAVA, rem, nbtSyntax)
            warnings.addAll(rem)
            val plain = "tellraw $javaTarget $contentJson"
            java.add(if (converted != null && converted.isNotBlank() && converted != "execute") "$converted run $plain" else plain)
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
    warnings.add("Java 每组一条 tellraw；基岩为一条 conditional rawtext。组间参数若重叠，Java 会重复发送。")
    return SimpleTranslateOutput(java.joinToString("\n"), bedrock, warnings.distinct())
}
