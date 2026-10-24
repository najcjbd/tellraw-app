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
import com.tellraw.app.MainActivity
import com.tellraw.app.model.CustomEntity
import com.tellraw.app.model.CustomEntityStore
import com.tellraw.app.model.EditableGridEntity
import com.tellraw.app.model.EntityAvatars
import com.tellraw.app.model.EntityIdAliases
import com.tellraw.app.model.EntityRegistry
import com.tellraw.app.model.FeatureGate
import com.tellraw.app.model.GridConditionEvaluator
import com.tellraw.app.model.GridEntityStore
import com.tellraw.app.model.GridSettingsStore
import com.tellraw.app.model.GridUiSettings
import com.tellraw.app.model.GridItem
import com.tellraw.app.model.KnownNbt
import com.tellraw.app.model.SnbtLite
import com.tellraw.app.model.SelectorEvaluator
import com.tellraw.app.model.VersionSpec
import com.tellraw.app.ui.components.ImageFilePickerDialog
import com.tellraw.app.ui.viewmodel.TellrawViewModel
import com.google.gson.JsonParser
import com.tellraw.app.util.CommandInputParser
import com.tellraw.app.util.TextComponentHelper
import com.tellraw.app.util.TranslatePreview
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
    val settingsStore = remember { GridSettingsStore(File(context.filesDir, "grid_settings.json")) }
    val savedUi = remember { settingsStore.load() }
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

    var selectorText by rememberSaveable { mutableStateOf(savedUi.selector.ifBlank { selectorFromMain }) }
    var conditions by rememberSaveable { mutableStateOf(savedUi.conditions) }
    LaunchedEffect(selectorFromMain) {
        if (selectorText.isBlank()) selectorText = selectorFromMain
    }

    var viewport by remember {
        mutableStateOf(Viewport(savedUi.camX, savedUi.camZ, savedUi.zoom.coerceIn(0.25f, 4f)))
    }
    val viewportState = rememberUpdatedState(viewport)
    var jumpX by rememberSaveable { mutableStateOf("0") }
    var jumpZ by rememberSaveable { mutableStateOf("0") }

    // 执行者 / 原点（对应 execute 的 as / at / positioned）
    var executorUid by rememberSaveable { mutableStateOf<String?>(savedUi.executorUid) }
    var useExecutorOrigin by rememberSaveable { mutableStateOf(savedUi.useExecutorOrigin) }
    var manualOX by rememberSaveable { mutableStateOf(savedUi.manualOX) }
    var manualOY by rememberSaveable { mutableStateOf(savedUi.manualOY) }
    var manualOZ by rememberSaveable { mutableStateOf(savedUi.manualOZ) }
    var showExecutorPicker by remember { mutableStateOf(false) }
    var showWorldSettings by remember { mutableStateOf(false) }

    var worldDimension by rememberSaveable { mutableStateOf(savedUi.worldDimension) }
    var worldBiome by rememberSaveable { mutableStateOf(savedUi.worldBiome) }
    var worldLoaded by rememberSaveable { mutableStateOf(savedUi.worldLoaded) }
    var worldBlocks by rememberSaveable { mutableStateOf(savedUi.worldBlocks) }
    var worldFunctions by rememberSaveable { mutableStateOf(savedUi.worldFunctions) }
    var worldStopwatches by rememberSaveable { mutableStateOf(savedUi.worldStopwatches) }

    // 网格设置持久化（防抖；输入过程中不会每个字符都写盘）
    LaunchedEffect(
        selectorText, conditions, worldDimension, worldBiome, worldLoaded,
        worldBlocks, worldFunctions, worldStopwatches, executorUid,
        useExecutorOrigin, manualOX, manualOY, manualOZ, viewport
    ) {
        kotlinx.coroutines.delay(400)
        settingsStore.save(
            GridUiSettings(
                selector = selectorText,
                conditions = conditions,
                worldDimension = worldDimension,
                worldBiome = worldBiome,
                worldLoaded = worldLoaded,
                worldBlocks = worldBlocks,
                worldFunctions = worldFunctions,
                worldStopwatches = worldStopwatches,
                executorUid = executorUid,
                useExecutorOrigin = useExecutorOrigin,
                manualOX = manualOX,
                manualOY = manualOY,
                manualOZ = manualOZ,
                camX = viewport.camX,
                camZ = viewport.camZ,
                zoom = viewport.zoom
            )
        )
    }

    var showEditor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<EditableGridEntity?>(null) }
    var editingIsNew by remember { mutableStateOf(false) }
    var showCoordEditor by remember { mutableStateOf(false) }
    var showCustomManager by remember { mutableStateOf(false) }
    var showAvatarPicker by remember { mutableStateOf(false) }
    var pickedAvatarPath by remember { mutableStateOf<String?>(null) }
    val activity = context as? MainActivity

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

    // 允许直接粘贴整条 execute … run tellraw … / tellraw …；能解析时用命令里的选择器与前缀
    val parsedCommand = remember(selectorText) {
        val t = selectorText.trim()
        if (t.startsWith("execute") || t.startsWith("tellraw")) {
            CommandInputParser.parse(selectorText, context)
        } else null
    }
    val effectiveSelector = parsedCommand?.selector?.takeIf { it.isNotBlank() } ?: selectorText
    val effectiveConditions = listOfNotNull(
        parsedCommand?.executePrefix?.takeIf { it.isNotBlank() },
        conditions.takeIf { it.isNotBlank() }
    ).joinToString(" ")

    val executorEntity = entities.firstOrNull { it.uid == executorUid }?.toGridEntity()
    val baseOrigin: Triple<Double, Double, Double> = when {
        useExecutorOrigin && executorEntity != null ->
            Triple(executorEntity.x, executorEntity.y, executorEntity.z)
        useExecutorOrigin -> Triple(0.0, -60.0, 0.0)
        else -> Triple(
            manualOX.toDoubleOrNull() ?: 0.0,
            manualOY.toDoubleOrNull() ?: -60.0,
            manualOZ.toDoubleOrNull() ?: 0.0
        )
    }
    val allGridEntities = remember(entities.toList()) { entities.map { it.toGridEntity() } }
    val worldState = remember(
        worldDimension, worldBiome, worldLoaded, worldBlocks, worldFunctions, worldStopwatches
    ) {
        GridConditionEvaluator.GridWorld(
            dimension = worldDimension.ifBlank { "minecraft:overworld" },
            biome = worldBiome,
            loaded = worldLoaded,
            blocks = parseWorldBlocks(worldBlocks),
            functions = splitList(worldFunctions).toSet(),
            stopwatches = parseStopwatches(worldStopwatches)
        )
    }
    val resolvedOrigin = remember(effectiveConditions, allGridEntities, executorEntity, baseOrigin, targetSpec) {
        GridConditionEvaluator.resolveOrigin(
            effectiveConditions, allGridEntities, executorEntity, baseOrigin, targetSpec
        )
    }
    val evaluationOrigin = resolvedOrigin.origin
    val evaluation = remember(
        effectiveSelector, allGridEntities, targetSpec,
        executorUid, resolvedOrigin
    ) {
        SelectorEvaluator.evaluate(
            selector = effectiveSelector,
            entities = allGridEntities,
            executor = executorEntity,
            edition = if (targetSpec?.edition == VersionSpec.Edition.BEDROCK) "bedrock" else "java",
            origin = evaluationOrigin,
            target = targetSpec
        )
    }
    val conditionResults = remember(
        evaluation, effectiveConditions, allGridEntities, targetSpec, worldState, evaluationOrigin
    ) {
        evaluation.matched.map { ge ->
            GridConditionEvaluator.evaluate(
                effectiveConditions, ge, allGridEntities, targetSpec, worldState, evaluationOrigin
            ) to ge
        }
    }
    val filteredMatched = conditionResults.filter { it.first.ok }.map { it.second }
    val conditionNotes = (
        conditionResults.flatMap { it.first.notes } +
            resolvedOrigin.notes +
            (parsedCommand?.warnings ?: emptyList())
        ).distinct()
    val matchedUids = remember(filteredMatched) { filteredMatched.map { it.uid }.toSet() }

    // 自定义头像：只在用户填了本地头像路径时加载（不联网）
    // 内置实体头像（wiki EntitySprite），自定义头像优先
    val builtinAvatarMap = remember(entities.map { it.uid to it.id }) {
        entities.associate { e -> e.uid to EntityAvatars.load(context, e.id) }
    }
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

            // ---- 执行者 / 原点（as / at / positioned）----
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { showExecutorPicker = true }) {
                    Text(
                        "执行者：" + (
                            entities.firstOrNull { it.uid == executorUid }
                                ?.let { displayNameOf(it, customs) } ?: "无"
                        )
                    )
                }
                Spacer(Modifier.width(8.dp))
                Checkbox(
                    checked = useExecutorOrigin,
                    onCheckedChange = { useExecutorOrigin = it }
                )
                Text("at 执行者位置", style = MaterialTheme.typography.labelMedium)
            }
            if (!useExecutorOrigin) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(manualOX, { manualOX = it }, label = { Text("原点 x") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(6.dp))
                    OutlinedTextField(manualOY, { manualOY = it }, label = { Text("y") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(6.dp))
                    OutlinedTextField(manualOZ, { manualOZ = it }, label = { Text("z") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f))
                }
            }
            Text(
                "原点：(${"%.1f".format(evaluationOrigin.first)}, " +
                    "${"%.1f".format(evaluationOrigin.second)}, ${"%.1f".format(evaluationOrigin.third)})" +
                    (if (selectorText.contains(Regex("(^|[,[])x="))) "；选择器自带 x/y/z 会覆盖原点" else ""),
                style = MaterialTheme.typography.labelSmall
            )

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
                        val avatar = e.customKey?.let { avatarMap[it] } ?: builtinAvatarMap[e.uid]
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
                label = { Text("tellraw 目标选择器 / 整条 execute…run tellraw 命令") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    TextButton(onClick = { viewModel.updateSelector(selectorText) }) {
                        Text("写回主界面")
                    }
                }
            )
            OutlinedTextField(
                value = conditions,
                onValueChange = { conditions = it },
                label = { Text("附加条件（所有 if/unless 子条件）") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { showWorldSettings = true }) { Text("世界条件…") }
                Spacer(Modifier.width(8.dp))
                Text(
                    "维度=${worldState.dimension} 群系=${worldState.biome.ifBlank { "未设置" }} " +
                        "方块=${worldState.blocks.size} 函数=${worldState.functions.size}",
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Text(
                "将选中 ${filteredMatched.size} / ${entities.size} 个实体" +
                    (if (conditions.isNotBlank()) "（已按附加条件筛选）" else ""),
                style = MaterialTheme.typography.titleSmall
            )
            if (conditionNotes.isNotEmpty()) {
                conditionNotes.forEach { note ->
                    Text("• $note", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error)
                }
            }
            if (evaluation.notes.isNotEmpty()) {
                evaluation.notes.forEach { note ->
                    Text("• $note", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error)
                }
            }

            if (filteredMatched.isEmpty()) {
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
                    items(filteredMatched, key = { it.uid }) { ge ->
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

    if (showWorldSettings) {
        WorldSettingsDialog(
            dimension = worldDimension,
            biome = worldBiome,
            loaded = worldLoaded,
            blocks = worldBlocks,
            functions = worldFunctions,
            stopwatches = worldStopwatches,
            onChange = { d, b, l, bl, f, st ->
                worldDimension = d
                worldBiome = b
                worldLoaded = l
                worldBlocks = bl
                worldFunctions = f
                worldStopwatches = st
            },
            onDismiss = { showWorldSettings = false }
        )
    }

    if (showExecutorPicker) {
        Dialog(onDismissRequest = { showExecutorPicker = false }) {
            Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 6.dp) {
                Column(Modifier.padding(12.dp)) {
                    Text("选择执行者（@s / 默认原点）", style = MaterialTheme.typography.titleSmall)
                    LazyColumn(Modifier.heightIn(max = 360.dp)) {
                        item {
                            ListItem(
                                headlineContent = { Text("无（原点 0,-60,0）") },
                                modifier = Modifier.clickable {
                                    executorUid = null
                                    showExecutorPicker = false
                                }
                            )
                        }
                        items(entities, key = { it.uid }) { e ->
                            ListItem(
                                headlineContent = { Text(displayNameOf(e, customs)) },
                                supportingContent = { Text("(${fmt(e.x)}, ${fmt(e.y)}, ${fmt(e.z)})") },
                                modifier = Modifier.clickable {
                                    executorUid = e.uid
                                    showExecutorPicker = false
                                }
                            )
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

    if (showAvatarPicker) {
        ImageFilePickerDialog(
            onDismiss = { showAvatarPicker = false },
            onImageSelected = { path ->
                pickedAvatarPath = path
                showAvatarPicker = false
            }
        )
    }

    // ---- 自定义实体管理 ----
    if (showCustomManager) {
        CustomEntityManagerDialog(
            customs = customs.toList(),
            pickedAvatarPath = pickedAvatarPath,
            onPickAvatar = {
                activity?.checkAndRequestStoragePermission(
                    onGranted = { showAvatarPicker = true },
                    onDenied = { }
                ) ?: run { showAvatarPicker = true }
            },
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
            TextComponentHelper.ComponentType.TRANSLATE -> {
                val withText = c.subComponents
                    .find { it.type == TextComponentHelper.SubComponentType.WITH }?.content
                val args = if (withText.isNullOrEmpty()) emptyList()
                    else TextComponentHelper.parseTranslateArgs(withText)
                TranslatePreview.render(c.content, args)
            }
        }
    }.ifBlank { "（消息为空）" }
}

@Composable
private fun SmallAvatar(id: String, path: String?, sizeDp: Int = 24) {
    val ctx = LocalContext.current
    val bmp = remember(id, path) {
        path?.let { p -> runCatching { BitmapFactory.decodeFile(p)?.asImageBitmap() }.getOrNull() }
            ?: EntityAvatars.load(ctx, id)
    }
    if (bmp != null) {
        Image(
            bitmap = bmp,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(sizeDp.dp).clip(CircleShape)
        )
    } else {
        Box(
            modifier = Modifier
                .size(sizeDp.dp)
                .clip(CircleShape)
                .background(entityColor(EntityRegistry.categoryOf(id)))
        )
    }
}

@Composable
private fun EntityAvatar(
    entity: EditableGridEntity,
    customs: List<CustomEntity>,
    avatarMap: Map<String, androidx.compose.ui.graphics.ImageBitmap>
) {
    val ctx = LocalContext.current
    val builtin = remember(entity.id) { EntityAvatars.load(ctx, entity.id) }
    val avatar = entity.customKey?.let { avatarMap[it] } ?: builtin
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
    var nbt by remember(initial.uid) { mutableStateOf(initial.nbt) }
    var showQuickNbt by remember { mutableStateOf(false) }

    // 手动 NBT 里若写了 Tags/CustomName/Team，同步到上方字段（与 NBT 同一份数据）
    LaunchedEffect(nbt) {
        val ext = KnownNbt.readExternal(nbt) ?: return@LaunchedEffect
        ext.tags?.let { tags = it }
        ext.name?.let { name = it }
        ext.team?.let { team = it }
    }
    var properties by remember(initial.uid) { mutableStateOf(initial.properties) }
    var permissions by remember(initial.uid) { mutableStateOf(initial.permissions) }
    var predicates by remember(initial.uid) { mutableStateOf(initial.predicates) }
    var advancements by remember(initial.uid) { mutableStateOf(initial.advancements) }
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

                OutlinedTextField(name, {
                        name = it
                        nbt = KnownNbt.stripExternal(nbt) ?: nbt
                    }, label = { Text("名称（同 NBT CustomName，可空）") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(tags, {
                        tags = it
                        nbt = KnownNbt.stripExternal(nbt) ?: nbt
                    }, label = { Text("tags（同 NBT Tags，逗号分隔）") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(team, {
                        team = it
                        nbt = KnownNbt.stripExternal(nbt) ?: nbt
                    }, label = { Text("队伍（同 NBT Team，可空）") },
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
                    label = { Text("装备（head=...,mainhand=sword d=5）") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(inventory, { inventory = it },
                    label = { Text("物品栏（0-8 快捷栏，9+ 主栏；1=sword x1 {damage:5}）") },
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
                OutlinedTextField(nbt, { nbt = it },
                    label = { Text("手动 NBT（可多键，逗号分隔：Health:20.0f, Tags:[\"a\"]）") },
                    modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { showQuickNbt = true }) {
                        Text("快捷 NBT（血量 / 开关 / 标签…）")
                    }
                }
                OutlinedTextField(properties, { properties = it },
                    label = { Text("基岩属性（is_baby=1, fire）") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(permissions, { permissions = it },
                    label = { Text("基岩权限（camera=enabled）") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(predicates, { predicates = it },
                    label = { Text("Java 谓词 ID（逗号分隔）") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(advancements, { advancements = it },
                    label = { Text("Java 已完成进度（id 或 id|criterion）") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
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
                                nbt = nbt,
                                properties = properties,
                                permissions = permissions,
                                predicates = predicates,
                                advancements = advancements,
                                customKey = customKey
                            )
                        )
                    }) { Text("保存") }
                }
            }
        }
    }

    if (showQuickNbt) {
        QuickNbtDialog(
            entityId = id,
            nbtText = nbt,
            onDismiss = { showQuickNbt = false },
            onApply = { nbt = it; showQuickNbt = false }
        )
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

/**
 * 快捷 NBT：与手动 NBT 双向同步。
 * - 打开时从当前 NBT 文本回填已知键（Health/NoAI/…）；
 * - 修改任一快捷键会把它合并回 NBT（未知键原样保留）；
 * - 手动文本里改已知键，再打开时快捷字段就是新值。
 */
@Composable
private fun QuickNbtDialog(
    entityId: String,
    nbtText: String,
    onDismiss: () -> Unit,
    onApply: (String) -> Unit
) {
    var text by remember { mutableStateOf(nbtText) }
    var invalid by remember { mutableStateOf(false) }
    val quick = remember {
        mutableStateMapOf<String, String>().apply { KnownNbt.fillQuick(nbtText, this, entityId) }
    }
    var branchSpec by remember { mutableStateOf<KnownNbt.Spec?>(null) }

    fun rebuild() {
        val merged = KnownNbt.merge(text, quick, entityId)
        if (merged == null) {
            invalid = true
        } else {
            text = merged
            invalid = false
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 6.dp) {
            Column(
                Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("快捷 NBT", style = MaterialTheme.typography.titleMedium)
                Text(
                    "已知键会与手动 NBT 双向同步；未知键原样保留。" +
                        "Tags / CustomName / Team 用上方的 tags / 名称 / 队伍字段（同样会同步到 NBT）。",
                    style = MaterialTheme.typography.labelSmall
                )
                if (invalid) {
                    Text("手动 NBT 无法解析，请先修正下面的文本。",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall)
                }

                KnownNbt.specsFor(entityId).forEach { spec ->
                    if (spec.kind == KnownNbt.Kind.BOOL) {
                        val v = quick[spec.key].orEmpty()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(spec.label, modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall)
                            FilterChip(
                                selected = v == "true",
                                onClick = {
                                    quick[spec.key] = if (v == "true") "" else "true"
                                    rebuild()
                                },
                                label = { Text("开") }
                            )
                            Spacer(Modifier.width(4.dp))
                            FilterChip(
                                selected = v == "false",
                                onClick = {
                                    quick[spec.key] = if (v == "false") "" else "false"
                                    rebuild()
                                },
                                label = { Text("关") }
                            )
                        }
                    } else if (spec.kind == KnownNbt.Kind.LIST || spec.kind == KnownNbt.Kind.COMPOUND) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(spec.label, style = MaterialTheme.typography.bodySmall)
                                val preview = quick[spec.key].orEmpty()
                                Text(
                                    if (preview.isBlank()) "（未设置）" else preview.take(90),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedButton(onClick = { branchSpec = spec }) { Text("编辑…") }
                        }
                    } else {
                        OutlinedTextField(
                            value = quick[spec.key].orEmpty(),
                            onValueChange = {
                                quick[spec.key] = it
                                rebuild()
                            },
                            label = { Text(spec.label + if (spec.hint.isBlank()) "" else " · ${spec.hint}") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = when (spec.kind) {
                                    KnownNbt.Kind.INT, KnownNbt.Kind.BYTE -> KeyboardType.Number
                                    KnownNbt.Kind.FLOAT -> KeyboardType.Decimal
                                    else -> KeyboardType.Text
                                }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        val p = SnbtLite.parseCompoundBody(it)
                        if (p == null) {
                            invalid = true
                        } else {
                            invalid = false
                            quick.clear()
                            KnownNbt.fillQuick(it, quick, entityId)
                        }
                    },
                    label = { Text("手动 NBT（多个键用逗号分隔）") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Button(onClick = { onApply(text) }) { Text("应用") }
                }
            }
        }
    }

    branchSpec?.let { spec ->
        NbtBranchDialog(
            spec = spec,
            value = quick[spec.key].orEmpty(),
            onDismiss = { branchSpec = null },
            onApply = { raw ->
                quick[spec.key] = raw
                rebuild()
                branchSpec = null
            }
        )
    }
}

/** 列表/复合键的分支编辑器：直接编辑该键的 SNBT 值。 */
@Composable
private fun NbtBranchDialog(
    spec: KnownNbt.Spec,
    value: String,
    onDismiss: () -> Unit,
    onApply: (String) -> Unit
) {
    var text by remember { mutableStateOf(value) }
    val valid = text.isBlank() || SnbtLite.parseValue(text) != null
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 6.dp) {
            Column(
                Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("编辑 ${spec.label}", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (spec.kind == KnownNbt.Kind.LIST) "列表值（[...]）" else "复合值（{...}）",
                    style = MaterialTheme.typography.labelSmall
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("SNBT 值；留空=移除该键") },
                    minLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!valid) {
                    Text("SNBT 无法解析", color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Button(enabled = valid, onClick = { onApply(text.trim()) }) { Text("应用") }
                }
            }
        }
    }
}


/**
 * 方块表：
 *  - JSON：{"0,64,0":{"block":"minecraft:chest","items":[{"slot":0,"id":"minecraft:diamond","count":3}]}}
 *  - 简写：0,64,0=chest | 0:minecraft:diamond x3,1:minecraft:stone
 */
private fun parseWorldBlocks(text: String): Map<String, GridConditionEvaluator.GridBlock> {
    val t = text.trim()
    if (t.startsWith("{")) {
        try {
            val root = JsonParser.parseString(t).asJsonObject
            val out = linkedMapOf<String, GridConditionEvaluator.GridBlock>()
            for ((pos, el) in root.entrySet()) {
                if (!el.isJsonObject) continue
                val o = el.asJsonObject
                val id = o.get("block")?.asString ?: o.get("id")?.asString ?: continue
                val items = linkedMapOf<Int, GridItem>()
                o.getAsJsonArray("items")?.forEach { itemEl ->
                    if (!itemEl.isJsonObject) return@forEach
                    val io = itemEl.asJsonObject
                    val iid = io.get("id")?.asString ?: return@forEach
                    val slot = io.get("slot")?.asInt ?: 0
                    val count = io.get("count")?.asInt ?: 1
                    val damage = io.get("damage")?.asInt
                    val comps = io.get("components")?.let { compEl ->
                        (SnbtLite.parseValue(compEl.toString()) as? SnbtLite.Value.Compound)?.map
                            ?: SnbtLite.parseCompoundLenient(compEl.toString())
                    } ?: emptyMap()
                    items[slot] = GridItem(normItemId(iid), count, damage, comps)
                }
                out[pos.trim()] = GridConditionEvaluator.GridBlock(id, items)
            }
            return out
        } catch (e: Exception) {
            // 落到简写解析
        }
    }
    return text.split(';', '\n').mapNotNull { entry ->
        val i = entry.indexOf('=')
        if (i <= 0) return@mapNotNull null
        val pos = entry.substring(0, i).trim()
        val rest = entry.substring(i + 1).trim()
        val parts = rest.split('|', limit = 2)
        val id = parts[0].trim()
        val items = if (parts.size == 2) parseSimpleBlockItems(parts[1]) else emptyMap()
        pos to GridConditionEvaluator.GridBlock(id, items)
    }.toMap()
}

private fun normItemId(id: String): String = if (id.contains(':')) id else "minecraft:$id"

private fun parseSimpleBlockItems(text: String): Map<Int, GridItem> =
    text.split(',', ';').mapNotNull { entry ->
        val m = Regex("^\\s*(\\d+)\\s*:\\s*([^\\s,;]+)\\s*(?:x\\s*(\\d+))?\\s*$").find(entry)
            ?: return@mapNotNull null
        val slot = m.groupValues[1].toInt()
        val id = normItemId(m.groupValues[2])
        val count = m.groupValues[3].toIntOrNull() ?: 1
        slot to GridItem(id, count)
    }.toMap()

private fun parseStopwatches(text: String): Map<String, Double> =
    text.split(',', '，', ';', '\n').mapNotNull { entry ->
        val i = entry.indexOf('=')
        if (i <= 0) null
        else entry.substring(0, i).trim() to (entry.substring(i + 1).trim().toDoubleOrNull() ?: return@mapNotNull null)
    }.toMap()

private fun splitList(text: String): List<String> =
    text.split(',', '，', ';', '\n').map { it.trim() }.filter { it.isNotEmpty() }

@Composable
private fun WorldSettingsDialog(
    dimension: String,
    biome: String,
    loaded: Boolean,
    blocks: String,
    functions: String,
    stopwatches: String,
    onChange: (String, String, Boolean, String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var d by remember { mutableStateOf(dimension) }
    var b by remember { mutableStateOf(biome) }
    var l by remember { mutableStateOf(loaded) }
    var bl by remember { mutableStateOf(blocks) }
    var f by remember { mutableStateOf(functions) }
    var st by remember { mutableStateOf(stopwatches) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 6.dp) {
            Column(
                Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("世界条件", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(d, { d = it }, label = { Text("维度 dimension") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(b, { b = it }, label = { Text("生物群系 biome（可空）") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(l, { l = it })
                    Text("区块已加载 loaded")
                }
                OutlinedTextField(
                    bl, { bl = it },
                    label = { Text("方块表：JSON（含 items）或 x,y,z=block | slot:item xN; …") },
                    minLines = 3, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(f, { f = it },
                    label = { Text("返回 true 的函数（逗号分隔）") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(st, { st = it },
                    label = { Text("stopwatch：id=秒, …") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Button(onClick = {
                        onChange(d, b, l, bl, f, st)
                        onDismiss()
                    }) { Text("应用") }
                }
            }
        }
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
                                leadingContent = {
                                    SmallAvatar(c.builtinId ?: "minecraft:player", c.avatarPath)
                                },
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
                            leadingContent = { SmallAvatar(e.id, null) },
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
    pickedAvatarPath: String? = null,
    onPickAvatar: () -> Unit = {},
    onDismiss: () -> Unit,
    onChange: (List<CustomEntity>) -> Unit
) {
    var officialId by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var builtinId by remember { mutableStateOf("minecraft:player") }
    var avatarPath by remember { mutableStateOf("") }
    LaunchedEffect(pickedAvatarPath) {
        if (!pickedAvatarPath.isNullOrBlank()) avatarPath = pickedAvatarPath
    }
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
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        TextButton(onClick = onPickAvatar) { Text("选择图片") }
                    })
                if (avatarPath.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SmallAvatar(builtinId.ifBlank { "minecraft:player" }, avatarPath)
                        Spacer(Modifier.width(8.dp))
                        Text(avatarPath, style = MaterialTheme.typography.labelSmall)
                    }
                }
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

