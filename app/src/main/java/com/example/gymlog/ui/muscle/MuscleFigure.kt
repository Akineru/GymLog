package com.example.gymlog.ui.muscle

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gymlog.R
import com.example.gymlog.data.MuscleHighlight
import com.example.gymlog.data.MuscleRole
import kotlinx.coroutines.launch

private data class Callout(
    val regionId: String,
    val text: String,
    val onRight: Boolean,
)

private val frontCallouts = listOf(
    Callout("deltoid_anterior_l", "三角肌前束", onRight = false),
    Callout("deltoid_lateral_r", "三角肌中束", onRight = true),
    Callout("pectoralis_major_l", "胸大肌", onRight = false),
    Callout("biceps_brachii_l", "肱二头肌", onRight = false),
    Callout("rectus_abdominis", "腹直肌", onRight = false),
    Callout("external_oblique_r", "腹外斜肌", onRight = true),
    Callout("vastus_lateralis_l", "股外侧肌", onRight = false),
    Callout("tibialis_anterior_r", "胫骨前肌", onRight = true),
)

private val backCallouts = listOf(
    Callout("trapezius_upper", "斜方肌上部", onRight = false),
    Callout("trapezius_middle", "斜方肌中部", onRight = false),
    Callout("deltoid_posterior_r", "三角肌后束", onRight = true),
    Callout("latissimus_dorsi_l", "背阔肌", onRight = false),
    Callout("trapezius_lower", "斜方肌下部", onRight = true),
    Callout("triceps_brachii_r", "肱三头肌", onRight = true),
    Callout("gluteus_medius_l", "臀中肌", onRight = false),
    Callout("gluteus_maximus_r", "臀大肌", onRight = true),
    Callout("hamstrings_l", "腘绳肌", onRight = false),
    Callout("gastrocnemius_r", "腓肠肌", onRight = true),
    Callout("soleus_l", "比目鱼肌", onRight = false),
)

@Composable
fun MuscleFigure(
    highlights: List<MuscleHighlight>,
    onRegionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    selectedRegionId: String? = null,
    boundedHeight: Dp? = null,
    showCallouts: Boolean = boundedHeight == null,
    lockFront: Boolean = false,
    forcedFront: Boolean? = null,
    showSwitch: Boolean = true,
    switchBelow: Boolean = false,
    onViewSideChange: (Boolean) -> Unit = {},
    onBackgroundClick: () -> Unit = {},
) {
    var showFront by remember(forcedFront) { mutableStateOf(forcedFront ?: true) }
    val palette = rememberMusclePalette()
    val context = LocalContext.current
    val assets by MuscleAssetCache.assets.collectAsStateWithLifecycle()
    val loading by MuscleAssetCache.loading.collectAsStateWithLifecycle()
    val loadErrors by MuscleAssetCache.errors.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val frontLabel = stringResource(R.string.muscle_front)
    val backLabel = stringResource(R.string.muscle_back)
    val frontShown = forcedFront ?: if (lockFront) true else showFront
    val displaySwitch = showSwitch && !lockFront && forcedFront == null
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val compact = maxWidth < 720.dp
        val switchVisible = displaySwitch && compact
        val switchSide: (Boolean) -> Unit = { front ->
            if (showFront != front) {
                showFront = front
                onViewSideChange(front)
            }
        }
        LaunchedEffect(context, frontShown, compact, lockFront) {
            if (compact || lockFront) {
                MuscleAssetCache.loadSide(context, frontShown)
            } else {
                MuscleAssetCache.loadSide(context, front = true)
                MuscleAssetCache.loadSide(context, front = false)
            }
        }
        val retryCurrent: () -> Unit = {
            scope.launch { MuscleAssetCache.retry(context, frontShown) }
        }
        Column(
            modifier = Modifier.align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (compact) {
                if (switchVisible && !switchBelow) {
                    ViewSwitch(
                        frontSelected = frontShown,
                        frontLabel = frontLabel,
                        backLabel = backLabel,
                        below = false,
                        onFront = { switchSide(true) },
                        onBack = { switchSide(false) },
                    )
                }
                val side = if (frontShown) assets.front else assets.back
                if (side == null) {
                    AssetPlaceholder(
                        loading = loading,
                        error = loadErrors[frontShown],
                        onRetry = retryCurrent,
                        modifier = figureSize(boundedHeight, showCallouts),
                    )
                } else {
                    FigureCanvas(
                        image = side.base,
                        front = frontShown,
                        masks = side.masks,
                        callouts = if (showCallouts) {
                            if (frontShown) frontCallouts else backCallouts
                        } else {
                            emptyList()
                        },
                        highlights = highlights,
                        palette = palette,
                        selectedRegionId = selectedRegionId,
                        showCallouts = showCallouts,
                        onRegionClick = onRegionClick,
                        onBackgroundClick = onBackgroundClick,
                        modifier = figureSize(boundedHeight, showCallouts),
                    )
                    if (side.masks == null) {
                        MaskLoadNotice(onRetry = retryCurrent)
                    }
                }
                if (switchVisible && switchBelow) {
                    ViewSwitch(
                        frontSelected = frontShown,
                        frontLabel = frontLabel,
                        backLabel = backLabel,
                        below = true,
                        onFront = { switchSide(true) },
                        onBack = { switchSide(false) },
                    )
                }
            } else if (lockFront) {
                val side = assets.front
                if (side == null) {
                    AssetPlaceholder(
                        loading = loading,
                        error = loadErrors[true],
                        onRetry = retryCurrent,
                        modifier = figureSize(boundedHeight, showCallouts),
                    )
                } else {
                    FigureCanvas(
                        image = side.base,
                        front = true,
                        masks = side.masks,
                        callouts = if (showCallouts) frontCallouts else emptyList(),
                        highlights = highlights,
                        palette = palette,
                        selectedRegionId = selectedRegionId,
                        showCallouts = showCallouts,
                        onRegionClick = onRegionClick,
                        onBackgroundClick = onBackgroundClick,
                        modifier = figureSize(boundedHeight, showCallouts),
                    )
                    if (side.masks == null) {
                        MaskLoadNotice(onRetry = retryCurrent)
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WideSide(
                        side = assets.front,
                        front = true,
                        callouts = if (showCallouts) frontCallouts else emptyList(),
                        highlights = highlights,
                        palette = palette,
                        selectedRegionId = selectedRegionId,
                        showCallouts = showCallouts,
                        onRegionClick = onRegionClick,
                        onBackgroundClick = onBackgroundClick,
                        modifier = Modifier.weight(1f).aspectRatio(canvasAspect(showCallouts)),
                        loading = loading,
                        error = loadErrors[true],
                        onRetry = { scope.launch { MuscleAssetCache.retry(context, front = true) } },
                    )
                    WideSide(
                        side = assets.back,
                        front = false,
                        callouts = if (showCallouts) backCallouts else emptyList(),
                        highlights = highlights,
                        palette = palette,
                        selectedRegionId = selectedRegionId,
                        showCallouts = showCallouts,
                        onRegionClick = onRegionClick,
                        onBackgroundClick = onBackgroundClick,
                        modifier = Modifier.weight(1f).aspectRatio(canvasAspect(showCallouts)),
                        loading = loading,
                        error = loadErrors[false],
                        onRetry = { scope.launch { MuscleAssetCache.retry(context, front = false) } },
                    )
                }
                if (assets.front?.masks == null || assets.back?.masks == null) {
                    MaskLoadNotice(
                        onRetry = {
                            scope.launch {
                                MuscleAssetCache.retry(context, front = true)
                                MuscleAssetCache.retry(context, front = false)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun WideSide(
    side: MuscleSideAssets?,
    front: Boolean,
    callouts: List<Callout>,
    highlights: List<MuscleHighlight>,
    palette: MusclePalette,
    selectedRegionId: String?,
    showCallouts: Boolean,
    onRegionClick: (String) -> Unit,
    onBackgroundClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    error: String? = null,
    onRetry: () -> Unit = {},
) {
    if (side == null) {
        AssetPlaceholder(
            loading = loading,
            error = error,
            onRetry = onRetry,
            modifier = modifier,
        )
    } else {
        FigureCanvas(
            image = side.base,
            front = front,
            masks = side.masks,
            callouts = callouts,
            highlights = highlights,
            palette = palette,
            selectedRegionId = selectedRegionId,
            showCallouts = showCallouts,
            onRegionClick = onRegionClick,
            onBackgroundClick = onBackgroundClick,
            modifier = modifier,
        )
    }
}

@Composable
private fun AssetPlaceholder(
    loading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    modifier: Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(24.dp),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (loading || error == null) {
                CircularProgressIndicator()
            } else {
                Text(
                    text = stringResource(R.string.muscle_figure_load_failed),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onRetry) {
                    Text(stringResource(R.string.muscle_figure_retry))
                }
            }
        }
    }
}

@Composable
private fun MaskLoadNotice(onRetry: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.muscle_masks_unavailable),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onRetry) {
            Text(stringResource(R.string.muscle_figure_retry))
        }
    }
}

private fun figureSize(boundedHeight: Dp?, callouts: Boolean): Modifier {
    val aspect = canvasAspect(callouts)
    return if (boundedHeight != null) {
        Modifier.height(boundedHeight).aspectRatio(aspect, matchHeightConstraintsFirst = true)
    } else {
        Modifier.fillMaxWidth().aspectRatio(aspect)
    }
}

@Composable
private fun ViewSwitch(
    frontSelected: Boolean,
    frontLabel: String,
    backLabel: String,
    below: Boolean,
    onFront: () -> Unit,
    onBack: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.padding(top = if (below) 8.dp else 0.dp, bottom = if (below) 0.dp else 8.dp),
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            SwitchSegment(frontLabel, frontSelected, onFront)
            SwitchSegment(backLabel, !frontSelected, onBack)
        }
    }
}

@Composable
private fun SwitchSegment(text: String, selected: Boolean, onClick: () -> Unit) {
    val background = if (selected) MaterialTheme.colorScheme.inverseSurface else Color.Transparent
    val foreground = if (selected) {
        MaterialTheme.colorScheme.inverseOnSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(onClick = onClick, shape = RoundedCornerShape(50), color = background) {
        Text(
            text = text,
            color = foreground,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun FigureCanvas(
    image: ImageBitmap,
    front: Boolean,
    masks: MuscleMaskStore?,
    callouts: List<Callout>,
    highlights: List<MuscleHighlight>,
    palette: MusclePalette,
    selectedRegionId: String?,
    showCallouts: Boolean,
    onRegionClick: (String) -> Unit,
    onBackgroundClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val labelSize = 11.sp
    val layers = remember(highlights) { MuscleMaskCatalog.layers(highlights) }
    val selectedId = remember(selectedRegionId) { selectedRegionId?.let(::muscleKey) }
    val preferredLayerIds = remember(layers, selectedId) {
        buildList {
            layers.forEach { add(it.muscleId) }
            if (selectedId != null && selectedId !in this) add(selectedId)
        }
    }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Surface(
            color = palette.backdrop,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            val interaction = if (masks != null) {
                Modifier.pointerInput(masks, front, showCallouts, callouts, preferredLayerIds) {
                    val textPx = labelSize.toPx()
                    detectTapGestures { offset ->
                        val frame = frameFor(size.width.toFloat(), size.height.toFloat(), showCallouts)
                        val labelHit = placeCallouts(
                            callouts,
                            front,
                            masks,
                            frame,
                            size.width.toFloat(),
                            textPx,
                        ).firstOrNull { offset.hits(it) }
                        if (labelHit != null) {
                            onRegionClick(labelHit.regionId)
                            return@detectTapGestures
                        }
                        val source = frame.canvasToImage(offset.x, offset.y)
                        val hit = masks.hit(
                            imageX = source.x.toInt(),
                            imageY = source.y.toInt(),
                            preferredLayerIds = preferredLayerIds,
                        )
                        if (hit != null) onRegionClick(hit) else onBackgroundClick()
                    }
                }
            } else {
                Modifier
            }
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .then(interaction),
            ) {
                val frame = frameFor(size.width, size.height, showCallouts)
                val dst = IntOffset(frame.left, frame.top)
                val dstSize = IntSize(frame.width, frame.height)
                drawImage(
                    image = image,
                    dstOffset = dst,
                    dstSize = dstSize,
                    filterQuality = FilterQuality.High,
                )
                if (masks != null) {
                    layers.forEach { layer ->
                        if (!MuscleMaskCatalog.visible(layer.muscleId, front)) return@forEach
                        drawMask(
                            masks = masks,
                            front = front,
                            muscleId = layer.muscleId,
                            color = if (layer.role == MuscleRole.PRIMARY) palette.primary else palette.secondary,
                            alpha = MuscleMaskCatalog.OVERLAY_ALPHA,
                            dst = dst,
                            dstSize = dstSize,
                        )
                    }
                    if (selectedId != null) {
                        drawMask(
                            masks = masks,
                            front = front,
                            muscleId = selectedId,
                            color = palette.selected,
                            alpha = SELECTED_ALPHA,
                            dst = dst,
                            dstSize = dstSize,
                        )
                    }
                    if (showCallouts) {
                        drawCallouts(callouts, front, masks, highlights, palette, selectedRegionId, frame, labelSize.toPx())
                    }
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMask(
    masks: MuscleMaskStore?,
    front: Boolean,
    muscleId: String,
    color: Color,
    alpha: Float,
    dst: IntOffset,
    dstSize: IntSize,
) {
    if (masks == null) return
    if (!MuscleMaskCatalog.visible(muscleId, front)) return
    val fileName = MuscleMaskCatalog.spec(muscleId)?.fileName ?: return
    val mask = masks.bitmap(fileName)
    if (mask.opaqueCount == 0) return
    drawImage(
        image = mask.image,
        dstOffset = dst,
        dstSize = dstSize,
        filterQuality = FilterQuality.High,
        colorFilter = ColorFilter.tint(color.copy(alpha = alpha), BlendMode.SrcIn),
        blendMode = BlendMode.SrcAtop,
    )
}

private data class PlacedCallout(
    val regionId: String,
    val text: String,
    val onRight: Boolean,
    val textLeft: Float,
    val textTop: Float,
    val textRight: Float,
    val textBottom: Float,
    val anchor: Offset,
    val labelY: Float,
)

private fun placeCallouts(
    callouts: List<Callout>,
    front: Boolean,
    masks: MuscleMaskStore,
    frame: FigureFrame,
    canvasWidth: Float,
    textSize: Float,
): List<PlacedCallout> {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.textSize = textSize }
    val minGap = textSize * 1.7f
    var nextLeft = textSize
    var nextRight = textSize
    val ordered = callouts.mapNotNull { callout ->
        val anchor = anchorFor(masks, front, callout.regionId) ?: return@mapNotNull null
        callout to frame.imageToCanvas(anchor.x, anchor.y)
    }.sortedBy { it.second.y }
    return ordered.map { (callout, muscle) ->
        var labelY = muscle.y
        if (callout.onRight) {
            if (labelY < nextRight) labelY = nextRight
            nextRight = labelY + minGap
        } else {
            if (labelY < nextLeft) labelY = nextLeft
            nextLeft = labelY + minGap
        }
        val textWidth = paint.measureText(callout.text)
        val margin = 6f
        val textX = if (callout.onRight) canvasWidth - margin - textWidth else margin
        PlacedCallout(
            regionId = callout.regionId,
            text = callout.text,
            onRight = callout.onRight,
            textLeft = textX,
            textTop = labelY - textSize,
            textRight = textX + textWidth,
            textBottom = labelY + textSize * 0.45f,
            anchor = muscle,
            labelY = labelY,
        )
    }
}

private fun Offset.hits(callout: PlacedCallout): Boolean {
    val pad = 18f
    return x in (callout.textLeft - pad)..(callout.textRight + pad) &&
        y in (callout.textTop - pad)..(callout.textBottom + pad)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCallouts(
    callouts: List<Callout>,
    front: Boolean,
    masks: MuscleMaskStore,
    highlights: List<MuscleHighlight>,
    palette: MusclePalette,
    selectedRegionId: String?,
    frame: FigureFrame,
    textSize: Float,
) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.textSize = textSize
        typeface = android.graphics.Typeface.DEFAULT
    }
    placeCallouts(callouts, front, masks, frame, size.width, textSize).forEach { callout ->
        val active = sameMuscle(callout.regionId, selectedRegionId)
        paint.color = (if (active) palette.selected else palette.label).toArgb()
        val lineX = if (callout.onRight) callout.textLeft - 8f else callout.textRight + 8f
        val dot = when {
            active -> palette.selected
            else -> when (MuscleMaskCatalog.roleFor(callout.regionId, highlights)) {
                MuscleRole.PRIMARY -> palette.primary
                MuscleRole.SECONDARY -> palette.secondary
                null -> palette.idle
            }
        }
        drawLine(palette.leader, Offset(lineX, callout.labelY), callout.anchor, strokeWidth = 1.6.dp.toPx())
        drawCircle(dot, radius = 3.dp.toPx(), center = callout.anchor)
        drawContext.canvas.nativeCanvas.drawText(
            callout.text,
            callout.textLeft,
            callout.labelY + textSize * 0.35f,
            paint,
        )
    }
}

private fun anchorFor(masks: MuscleMaskStore, front: Boolean, regionId: String): Offset? {
    val muscleId = regionId.removeSuffix("_l").removeSuffix("_r")
    val spec = MuscleMaskCatalog.spec(muscleId) ?: return null
    if (!MuscleMaskCatalog.visible(muscleId, front)) return null
    val mask = masks.bitmap(spec.fileName)
    if (mask.opaqueCount == 0) return null
    return when {
        regionId.endsWith("_l") -> {
            val x = mask.leftCenterX ?: return null
            val y = mask.leftCenterY ?: return null
            Offset(x, y)
        }
        regionId.endsWith("_r") -> {
            val x = mask.rightCenterX ?: return null
            val y = mask.rightCenterY ?: return null
            Offset(x, y)
        }
        else -> Offset(mask.centerX, mask.centerY)
    }
}

private fun sameMuscle(regionId: String, selectedRegionId: String?): Boolean {
    if (selectedRegionId == null) return false
    return muscleKey(regionId) == muscleKey(selectedRegionId)
}

private fun muscleKey(regionId: String): String {
    return regionId.removeSuffix("_l").removeSuffix("_r")
}

private const val SELECTED_ALPHA = 0.28f
