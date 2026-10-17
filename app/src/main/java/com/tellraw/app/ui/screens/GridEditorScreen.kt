package com.tellraw.app.ui.screens

import android.graphics.BitmapFactory
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.tellraw.app.model.CustomEntity
import com.tellraw.app.model.CustomEntityStore
import com.tellraw.app.model.EditableGridEntity
import com.tellraw.app.model.EntityRegistry
import com.tellraw.app.model.FeatureGate
import com.tellraw.app.model.GridEntityStore
import com.tellraw.app.model.SelectorEvaluator
import com.tellraw.app.model.VersionSpec
import com.tellraw.app.ui.viewmodel.TellrawViewModel
import com.tellraw.app.util.TextComponentHelper
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import kotlin.math.floor
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.math.sqrt

private const val GRID_BASE_BLOCK_DP = 16f
private const val LONG_PRESS_MS = 500L

private data class Viewport(val camX: Double = 0.0, val camZ: Double = 0.0, val zoom: Float = 1f)

private fun snap(v: Double): Double = round(v * 2.0) / 2.0

private fun entityColor(category: EntityRegistry.Category): Color = when (category) {
    EntityRegistry.Category.PLAYER -> Color(0xFF3949AB)
    EntityRegistry.Category.HOSTILE -> Color(0xFFD32F2F)
    EntityRegistry.Category.PASSIVE -> Color(0xFF2E7D32)
    EntityRegistry.Category.PROJECTILE -> Color(0xFFF57C00)
    EntityRegistry.Category.VEHICLE -> Color(0xFF6A1B9A)
    EntityRegistry.Category.OTHER -> Color(0xFF546E7A)
}

private fun displayNameOf(
    e: EditableGridEntity,
    customs: List<CustomEntity>
): String {
    val custom = e.customKey?.let { key -> customs.firstOrNull { it.id == key } }
    return e.name.ifBlank {
        custom?.displayName?.takeIf { it.isNotBlank() }
            ?: custom?.officialId?.takeIf { it.isNotBlank() }
            ?: EntityRegistry.displayName(e.id)
    }
}

/**
 * 网格实体编辑器（D5）：
 *  - 俯视网格：双指缩放/平移、单指拖空白平移、拖实体移动；
 *  - 长按空白放置实体并配置，长按实体重新配置，点实体改坐标；
 *  - 顶部 x/z 跳转；默认 y=-60；
 *  - 下方列出当前 tellraw 会选中的实体，以及每个实体会显示什么。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GridEditorScreen(
    viewModel: TellrawViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val selectorFromMain by viewModel.selectorInput.collectAsState()
    val message by viewModel.messageInput.collectAsState()
    val targetVersion by viewModel.targetVersion.collectAsState()
    val targetSpec = remember(targetVersion) { VersionSpec.parse(targetVersion) }

    val entityStore = remember { GridEntityStore(File(context.filesDir, "grid_entities.json")) }
    val customStore = remember { CustomEntityStore(File(context.filesDir, "custom_entities.json")) }
    val entities: SnapshotStateList<EditableGridEntity> = remember {
        mutableStateListOf<EditableGridEntity>().apply { addAll(entityStore.load()) }
    }
    val customs: SnapshotStateList<CustomEntity> = remember {
        mutableStateListOf<CustomEntity>().apply { addAll(customStore.load()) }
    }

    fun persistEntities() = entityStore.save(entities.toList())
    fun persistCustoms() {
        customStore.save(customs.toList())
    }

    var selectorText by rememberSaveable { mutableStateOf(selectorFromMain) }
    LaunchedEffect(selectorFromMain) {
        if (selectorText.isBlank()) selectorText = selectorFromMain
    }

    var viewport by remember { mutableStateOf(Viewport()) }
    val viewportState = rememberUpdatedState(viewport)
    var jumpX by rememberSaveable { mutableStateOf("0") }
    var jumpZ by rememberSaveable { mutableStateOf("0") }

    var showEditor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<EditableGridEntity?>(null) }
    var editingIsNew by remember { mutableStateOf(false) }
    var showCoordEditor by remember { mutableStateOf(false) }
    var showCustomManager by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val baseBlockPx = with(density) { GRID_BASE_BLOCK_DP.dp.toPx() }
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    val canvasSizeState = rememberUpdatedState(canvasSize)
    val blockPx = { vp: Viewport -> baseBlockPx * vp.zoom }

    fun worldToScreen(x: Double, z: Double, vp: Viewport, size: Size): Offset {
        val b = blockPx(vp)
        return Offset(
            (size.width / 2f + ((x - vp.camX) * b).toFloat()),
            (size.height / 2f + ((z - vp.camZ) * b).toFloat())
        )
    }

    fun screenToWorld(p: Offset, vp: Viewport, size: Size): Pair<Double, Double> {
        val b = blockPx(vp)
        return Pair(
            vp.camX + (p.x - size.width / 2f) / b,
            vp.camZ + (p.y - size.height / 2f) / b
        )
    }

    fun hitTest(p: Offset, vp: Viewport, size: Size): EditableGridEntity? {
        val radius = with(density) { 18.dp.toPx() }
        return entities.lastOrNull { e ->
            val c = worldToScreen(e.x, e.z, vp, size)
            sqrt((c.x - p.x) * (c.x - p.x) + (c.y - p.y) * (c.y - p.y)) <= radius
        }
    }

    val evaluation = remember(selectorText, entities.toList(), targetSpec) {
        SelectorEvaluator.evaluate(
            selector = selectorText,
            entities = entities.map { it.toGridEntity() },
            edition = if (targetSpec?.edition == VersionSpec.Edition.BEDROCK) "bedrock" else "java",
            target = targetSpec
        )
    }
    val matchedUids = remember(evaluation) { evaluation.matched.map { it.uid }.toSet() }

    // 自定义头像：只在用户填了本地头像路径时加载（不联网）
    val avatarMap = remember(customs.toList()) {
        customs.mapNotNull { c ->
            val path = c.avatarPath ?: return@mapNotNull null
            val bmp = runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull()
                ?: return@mapNotNull null
            c.id to bmp
        }.toMap()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("网格实体编辑器") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showCustomManager = true }) {
                        Icon(Icons.Default.Person, contentDescription = "自定义实体")
                    }
                    IconButton(onClick = {
                        editing = EditableGridEntity(x = viewport.camX, z = viewport.camZ, y = -60.0)
                        editingIsNew = true
                        showEditor = true
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "添加实体")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "目标版本：" + (targetSpec?.let {
                    (if (it.edition == VersionSpec.Edition.JAVA) "Java " else "基岩 ") +
                        it.parts.joinToString(".")
                } ?: "-1（最新）"),
                style = MaterialTheme.typography.labelLarge
            )

            // ---- x/z 跳转 + 缩放 ----
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = jumpX,
                    onValueChange = { jumpX = it },
                    label = { Text("x") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = jumpZ,
                    onValueChange = { jumpZ = it },
                    label = { Text("z") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    val x = jumpX.toDoubleOrNull()
                    val z = jumpZ.toDoubleOrNull()
                    if (x != null && z != null) viewport = viewport.copy(camX = x, camZ = z)
                }) { Text("跳转") }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    viewport = viewport.copy(zoom = (viewport.zoom / 1.25f).coerceIn(0.25f, 4f))
                }) { Icon(Icons.Default.ZoomOut, contentDescription = "缩小") }
                Text("${(viewport.zoom * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
                IconButton(onClick = {
                    viewport = viewport.copy(zoom = (viewport.zoom * 1.25f).coerceIn(0.25f, 4f))
                }) { Icon(Icons.Default.ZoomIn, contentDescription = "放大") }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = { viewport = Viewport() }) { Text("居中 (0,0)") }
                Text(
                    "视角 x=${"%.1f".format(viewport.camX)} z=${"%.1f".format(viewport.camZ)}",
                    style = MaterialTheme.typography.labelSmall
                )
            }

            // ---- 画布 ----
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .onSizeChanged { canvasSize = Size(it.width.toFloat(), it.height.toFloat()) }
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val size = canvasSizeState.value
                            if (size == Size.Zero) {
                                down.consume()
                                return@awaitEachGesture
                            }
                            val vp0 = viewportState.value
                            var vp = vp0
                            val hit = hitTest(down.position, vp0, size)
                            var draggedUid: String? = hit?.uid
                            var moved = 0f
                            var longFired = false
                            var lastPos = down.position
                            down.consume()
                            while (true) {
                                val ev = withTimeoutOrNull(
                                    if (!longFired && moved < viewConfiguration.touchSlop) LONG_PRESS_MS
                                    else Long.MAX_VALUE
                                ) { awaitPointerEvent() }
                                if (ev == null) {
                                    if (!longFired) {
                                        longFired = true
                                        if (hit != null) {
                                            editing = hit
                                            editingIsNew = false
                                            showEditor = true
                                        } else {
                                            val (wx, wz) = screenToWorld(lastPos, viewportState.value, size)
                                            editing = EditableGridEntity(
                                                x = snap(wx), z = snap(wz), y = -60.0
                                            )
                                            editingIsNew = true
                                            showEditor = true
                                        }
                                    }
                                    continue
                                }
                                val pressed = ev.changes.filter { it.pressed }
                                if (pressed.size >= 2) {
                                    val zoomChange = ev.calculateZoom()
                                    val pan = ev.calculatePan()
                                    vp = viewportState.value
                                    val nz = (vp.zoom * zoomChange).coerceIn(0.25f, 4f)
                                    val b = baseBlockPx * nz
                                    viewport = Viewport(
                                        camX = vp.camX - pan.x / b,
                                        camZ = vp.camZ - pan.y / b,
                                        zoom = nz
                                    )
                                    draggedUid = null
                                    ev.changes.forEach { it.consume() }
                                } else if (pressed.size == 1) {
                                    val change = pressed[0]
                                    val delta = change.position - lastPos
                                    lastPos = change.position
                                    moved += delta.getDistance()
                                    if (moved > viewConfiguration.touchSlop) {
                                        vp = viewportState.value
                                        val b = baseBlockPx * vp.zoom
                                        val uid = draggedUid
                                        if (uid != null) {
                                            val i = entities.indexOfFirst { it.uid == uid }
                                            if (i >= 0) {
                                                val e = entities[i]
                                                entities[i] = e.copy(
                                                    x = e.x + delta.x / b,
                                                    z = e.z + delta.y / b
                                                )
                                            }
                                        } else {
                                            viewport = vp.copy(
                                                camX = vp.camX - delta.x / b,
                                                camZ = vp.camZ - delta.y / b
                                            )
                                        }
                                        change.consume()
                                    }
                                }
                                if (pressed.isEmpty()) break
                            }
                            if (!longFired && moved <= viewConfiguration.touchSlop) {
                                if (hit != null) {
                                    editing = hit
                                    editingIsNew = false
                                    showCoordEditor = true
                                }
                            } else if (draggedUid != null) {
                                persistEntities()
                            }
                        }
                    }
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val vp = viewport
                    val bpx = baseBlockPx * vp.zoom
                    val left = vp.camX - size.width / 2.0 / bpx
                    val right = vp.camX + size.width / 2.0 / bpx
                    val top = vp.camZ - size.height / 2.0 / bpx
                    val bottom = vp.camZ + size.height / 2.0 / bpx

                    val minor = Color.Gray.copy(alpha = 0.18f)
                    val major = Color.Gray.copy(alpha = 0.38f)
                    for (gx in floor(left).toInt()..kotlin.math.ceil(right).toInt()) {
                        val sx = (size.width / 2f + ((gx - vp.camX) * bpx).toFloat())
                        drawLine(
                            if (gx % 5 == 0) major else minor,
                            Offset(sx, 0f), Offset(sx, size.height),
                            strokeWidth = if (gx % 5 == 0) 1.5f else 0.7f
                        )
                    }
                    for (gz in floor(top).toInt()..kotlin.math.ceil(bottom).toInt()) {
                        val sy = (size.height / 2f + ((gz - vp.camZ) * bpx).toFloat())
                        drawLine(
                            if (gz % 5 == 0) major else minor,
                            Offset(0f, sy), Offset(size.width, sy),
                            strokeWidth = if (gz % 5 == 0) 1.5f else 0.7f
                        )
                    }

                    val paint = Paint().apply {
                        isAntiAlias = true
                        textAlign = Paint.Align.CENTER
                        color = android.graphics.Color.WHITE
                        textSize = 12.sp.toPx()
                    }
                    val labelPaint = Paint().apply {
                        isAntiAlias = true
                        textAlign = Paint.Align.CENTER
                        color = android.graphics.Color.DKGRAY
                        textSize = 10.sp.toPx()
                    }
                    for (e in entities) {
                        val c = Offset(
                            size.width / 2f + ((e.x - vp.camX) * bpx).toFloat(),
                            size.height / 2f + ((e.z - vp.camZ) * bpx).toFloat()
                        )
                        val radius = 14.dp.toPx()
                        val color = entityColor(EntityRegistry.categoryOf(e.id))
                        if (e.uid in matchedUids) {
                            drawCircle(Color(0xFFFFD54F), radius + 4.dp.toPx(), c, style = Stroke(3.dp.toPx()))
                        }
                        val avatar = e.customKey?.let { avatarMap[it] }
                        if (avatar != null) {
                            val r = radius.roundToInt()
                            clipPath(Path().apply {
                                addOval(
                                    androidx.compose.ui.geometry.Rect(
                                        c.x - radius, c.y - radius, c.x + radius, c.y + radius
                                    )
                                )
                            }) {
                                drawImage(
                                    avatar,
                                    dstOffset = IntOffset(
                                        (c.x - radius).roundToInt(), (c.y - radius).roundToInt()
                                    ),
                                    dstSize = IntSize(r * 2, r * 2)
                                )
                            }
                        } else {
                            drawCircle(color, radius, c)
                            val label = displayNameOf(e, customs).take(1)
                            drawContext.canvas.nativeCanvas.drawText(label, c.x, c.y + 4.dp.toPx(), paint)
                        }
                        drawContext.canvas.nativeCanvas.drawText(
                            displayNameOf(e, customs), c.x, c.y + radius + 13.dp.toPx(), labelPaint
                        )
                    }
                }
                Text(
                    "长按空白放置 · 长按实体重配 · 点实体改坐标 · 拖动移动 · 双指缩放",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(6.dp)
                )
            }

            // ---- 选择器与求值结果 ----
            OutlinedTextField(
                value = selectorText,
                onValueChange = { selectorText = it },
                label = { Text("tellraw 目标选择器") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    TextButton(onClick = { viewModel.updateSelector(selectorText) }) {
                        Text("写回主界面")
                    }
                }
            )
            Text(
                "将选中 ${evaluation.matched.size} / ${entities.size} 个实体",
                style = MaterialTheme.typography.titleSmall
            )
            if (evaluation.notes.isNotEmpty()) {
                evaluation.notes.forEach { note ->
                    Text("• $note", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error)
                }
            }

            if (evaluation.matched.isEmpty()) {
                Text("（没有实体会被选中）", style = MaterialTheme.typography.bodySmall)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            RoundedCornerShape(8.dp))
                        .padding(6.dp)
                ) {
                    items(evaluation.matched, key = { it.uid }) { ge ->
                        val editable = entities.firstOrNull { it.uid == ge.uid }
                        if (editable != null) {
                            val name = displayNameOf(editable, customs)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                EntityAvatar(editable, customs, avatarMap)
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text("$name · ${EntityRegistry.displayName(editable.id)}",
                                        style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "(${fmt(editable.x)}, ${fmt(editable.y)}, ${fmt(editable.z)})" +
                                            (if (editable.tags.isBlank()) "" else "  tags=${editable.tags}") +
                                            (if (editable.team.isBlank()) "" else "  team=${editable.team}"),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    Text(
                                        "显示：" + previewMessage(message, editable, name),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }

    // ---- 完整配置对话框 ----
    if (showEditor) {
        val current = editing
        if (current != null) {
            EntityEditorDialog(
                initial = current,
                isNew = editingIsNew,
                customs = customs.toList(),
                onDismiss = { showEditor = false; editing = null },
                onDelete = {
                    entities.removeAll { it.uid == current.uid }
                    persistEntities()
                    showEditor = false
                    editing = null
                },
                onConfirm = { updated ->
                    val i = entities.indexOfFirst { it.uid == updated.uid }
                    if (i >= 0) entities[i] = updated else entities.add(updated)
                    persistEntities()
                    showEditor = false
                    editing = null
                }
            )
        }
    }

    // ---- 点按：只改 x/y/z ----
    if (showCoordEditor) {
        val current = editing
        if (current != null) {
            CoordEditorDialog(
                entity = current,
                onDismiss = { showCoordEditor = false; editing = null },
                onConfirm = { updated ->
                    val i = entities.indexOfFirst { it.uid == updated.uid }
                    if (i >= 0) entities[i] = updated
                    persistEntities()
                    showCoordEditor = false
                    editing = null
                }
            )
        }
    }

    // ---- 自定义实体管理 ----
    if (showCustomManager) {
        CustomEntityManagerDialog(
            customs = customs.toList(),
            onDismiss = { showCustomManager = false },
            onChange = { list ->
                customs.clear()
                customs.addAll(list)
                persistCustoms()
            }
        )
    }
}

private fun fmt(v: Double): String =
    if (v == v.toLong().toDouble()) v.toLong().toString() else "%.2f".format(v)

/** 每个实体实际会看到的文本：selector 组件按该实体解析，score 组件取它的分数。 */
private fun previewMessage(
    message: String,
    entity: EditableGridEntity,
    displayName: String
): String {
    if (message.isBlank()) return "（消息为空）"
    val components = TextComponentHelper.parseTextComponents(message)
    if (components.none { it.type != TextComponentHelper.ComponentType.TEXT }) {
        return components.joinToString("") { it.content }.ifBlank { "（消息为空）" }
    }
    return components.joinToString("") { c ->
        when (c.type) {
            TextComponentHelper.ComponentType.TEXT -> c.content
            TextComponentHelper.ComponentType.SELECTOR -> {
                val sel = c.content.substringBefore(':').trim()
                if (sel == "@s" || sel == "@p" || sel == "@n") displayName else c.content
            }
            TextComponentHelper.ComponentType.SCORE -> {
                val objective = c.content.substringAfter(':', "").substringBefore(':').trim()
                entity.scoreMap()[objective]?.toString() ?: displayName
            }
            TextComponentHelper.ComponentType.TRANSLATE -> c.content
        }
    }.ifBlank { "（消息为空）" }
}

@Composable
private fun EntityAvatar(
    entity: EditableGridEntity,
    customs: List<CustomEntity>,
    avatarMap: Map<String, androidx.compose.ui.graphics.ImageBitmap>
) {
    val avatar = entity.customKey?.let { avatarMap[it] }
    if (avatar != null) {
        Image(
            bitmap = avatar,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
        )
    } else {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(entityColor(EntityRegistry.categoryOf(entity.id))),
            contentAlignment = Alignment.Center
        ) {
            Text(
                displayNameOf(entity, customs).take(1),
                color = Color.White,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun CoordEditorDialog(
    entity: EditableGridEntity,
    onDismiss: () -> Unit,
    onConfirm: (EditableGridEntity) -> Unit
) {
    var x by remember(entity.uid) { mutableStateOf(fmt(entity.x)) }
    var y by remember(entity.uid) { mutableStateOf(fmt(entity.y)) }
    var z by remember(entity.uid) { mutableStateOf(fmt(entity.z)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("修改坐标") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row {
                    OutlinedTextField(x, { x = it }, label = { Text("x") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(y, { y = it }, label = { Text("y") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(z, { z = it }, label = { Text("z") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(entity.copy(
                    x = x.toDoubleOrNull() ?: entity.x,
                    y = y.toDoubleOrNull() ?: entity.y,
                    z = z.toDoubleOrNull() ?: entity.z
                ))
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntityEditorDialog(
    initial: EditableGridEntity,
    isNew: Boolean,
    customs: List<CustomEntity>,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onConfirm: (EditableGridEntity) -> Unit
) {
    var id by remember(initial.uid) { mutableStateOf(initial.id) }
    var name by remember(initial.uid) { mutableStateOf(initial.name) }
    var tags by remember(initial.uid) { mutableStateOf(initial.tags) }
    var team by remember(initial.uid) { mutableStateOf(initial.team) }
    var x by remember(initial.uid) { mutableStateOf(fmt(initial.x)) }
    var y by remember(initial.uid) { mutableStateOf(fmt(initial.y)) }
    var z by remember(initial.uid) { mutableStateOf(fmt(initial.z)) }
    var scores by remember(initial.uid) { mutableStateOf(initial.scores) }
    var equipment by remember(initial.uid) { mutableStateOf(initial.equipment) }
    var inventory by remember(initial.uid) { mutableStateOf(initial.inventory) }
    var gamemode by remember(initial.uid) { mutableStateOf(initial.gamemode) }
    var level by remember(initial.uid) { mutableStateOf(initial.level) }
    var rotationX by remember(initial.uid) { mutableStateOf(initial.rotationX) }
    var rotationY by remember(initial.uid) { mutableStateOf(initial.rotationY) }
    var customKey by remember(initial.uid) { mutableStateOf(initial.customKey) }
    var showPicker by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth().heightIn(max = 640.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(if (isNew) "放置实体" else "配置实体",
                    style = MaterialTheme.typography.titleMedium)

                OutlinedTextField(
                    value = displayNameOf(
                        EditableGridEntity(id = id, name = name, customKey = customKey), customs
                    ) + "  ($id)",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("类型") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { showPicker = true }) {
                            Icon(Icons.Default.Search, contentDescription = "选择类型")
                        }
                    }
                )

                OutlinedTextField(name, { name = it }, label = { Text("名称（可空）") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(tags, { tags = it }, label = { Text("tags（逗号分隔）") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(team, { team = it }, label = { Text("队伍（可空）") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                Row {
                    OutlinedTextField(x, { x = it }, label = { Text("x") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(y, { y = it }, label = { Text("y（默认 -60）") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(z, { z = it }, label = { Text("z") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f))
                }
                OutlinedTextField(scores, { scores = it },
                    label = { Text("分数（kills=3,deaths=1）") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(equipment, { equipment = it },
                    label = { Text("装备（head=...,chest=...,mainhand=...,offhand=...）") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(inventory, { inventory = it },
                    label = { Text("物品栏（0=stone x64,hotbar.1=bread）") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                Row {
                    OutlinedTextField(gamemode, { gamemode = it },
                        label = { Text("游戏模式") },
                        singleLine = true, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(level, { level = it },
                        label = { Text("等级") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true, modifier = Modifier.weight(1f))
                }
                Row {
                    OutlinedTextField(rotationX, { rotationX = it },
                        label = { Text("垂直视角 -90..90") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(rotationY, { rotationY = it },
                        label = { Text("水平视角 -180..180") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true, modifier = Modifier.weight(1f))
                }
                if (customKey != null) {
                    Text("自定义条目：$customKey", style = MaterialTheme.typography.labelSmall)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (!isNew) {
                        TextButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Text("删除")
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Button(onClick = {
                        onConfirm(
                            initial.copy(
                                id = id,
                                name = name,
                                tags = tags,
                                team = team,
                                x = x.toDoubleOrNull() ?: initial.x,
                                y = y.toDoubleOrNull() ?: initial.y,
                                z = z.toDoubleOrNull() ?: initial.z,
                                scores = scores,
                                equipment = equipment,
                                inventory = inventory,
                                gamemode = gamemode,
                                level = level,
                                rotationX = rotationX,
                                rotationY = rotationY,
                                customKey = customKey
                            )
                        )
                    }) { Text("保存") }
                }
            }
        }
    }

    if (showPicker) {
        EntityPickerDialog(
            customs = customs,
            onDismiss = { showPicker = false },
            onPickBuiltin = { entity ->
                id = entity.id
                customKey = null
                showPicker = false
            },
            onPickCustom = { custom ->
                id = custom.builtinId ?: "minecraft:player"
                customKey = custom.id
                if (name.isBlank()) name = custom.displayName ?: custom.officialId ?: ""
                showPicker = false
            }
        )
    }
}

@Composable
private fun EntityPickerDialog(
    customs: List<CustomEntity>,
    onDismiss: () -> Unit,
    onPickBuiltin: (EntityRegistry.Entity) -> Unit,
    onPickCustom: (CustomEntity) -> Unit
) {
    var query by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 6.dp) {
            Column(Modifier.padding(12.dp)) {
                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    label = { Text("搜索实体（中文名 / id）") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    if (customs.isNotEmpty()) {
                        item { Text("自定义", style = MaterialTheme.typography.labelMedium) }
                        items(customs, key = { "c_" + it.id }) { c ->
                            ListItem(
                                headlineContent = {
                                    Text(c.displayName ?: c.officialId ?: c.id)
                                },
                                supportingContent = {
                                    Text("${c.builtinId ?: "minecraft:player"} · 自定义")
                                },
                                modifier = Modifier.clickable { onPickCustom(c) }
                            )
                        }
                    }
                    val filtered = EntityRegistry.ALL.filter {
                        query.isBlank() || it.zhName.contains(query, true) ||
                            it.id.contains(query, true)
                    }
                    item { Text("内置实体（${filtered.size}）", style = MaterialTheme.typography.labelMedium) }
                    items(filtered, key = { it.id }) { e ->
                        ListItem(
                            headlineContent = { Text(e.zhName) },
                            supportingContent = { Text(e.id) },
                            modifier = Modifier.clickable { onPickBuiltin(e) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomEntityManagerDialog(
    customs: List<CustomEntity>,
    onDismiss: () -> Unit,
    onChange: (List<CustomEntity>) -> Unit
) {
    var officialId by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var builtinId by remember { mutableStateOf("minecraft:player") }
    var avatarPath by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 6.dp) {
            Column(
                Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("自定义实体 / 玩家", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(officialId, { officialId = it },
                    label = { Text("正版 ID / 唯一标识（必填）") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(displayName, { displayName = it },
                    label = { Text("显示名（中文键）") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(builtinId, { builtinId = it },
                    label = { Text("外观实体 id（默认 minecraft:player）") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(avatarPath, { avatarPath = it },
                    label = { Text("头像路径（可空）") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())
                Button(
                    onClick = {
                        if (officialId.isNotBlank()) {
                            val c = CustomEntity(
                                id = officialId.trim(),
                                officialId = officialId.trim(),
                                displayName = displayName.ifBlank { null },
                                avatarPath = avatarPath.ifBlank { null },
                                builtinId = builtinId.ifBlank { "minecraft:player" }
                            )
                            onChange(customs.filterNot { it.id == c.id } + c)
                            officialId = ""
                            displayName = ""
                            avatarPath = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("添加 / 更新") }

                if (customs.isNotEmpty()) {
                    Text("已有条目", style = MaterialTheme.typography.labelMedium)
                    customs.forEach { c ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${c.displayName ?: c.officialId ?: c.id}  (${c.builtinId ?: "minecraft:player"})",
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { onChange(customs.filterNot { it.id == c.id }) }) {
                                Icon(Icons.Default.Delete, contentDescription = "删除")
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("关闭") }
                }
            }
        }
    }
}

