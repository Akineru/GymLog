package com.example.gymlog.ui.settings

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.media.ExifInterface
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect as ComposeRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.gymlog.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal data class AvatarCropRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

internal data class AvatarCropTransform(
    val userScale: Float,
    val offsetX: Float,
    val offsetY: Float,
)

internal object AvatarCropMath {
    const val MIN_USER_SCALE = 1f
    const val MAX_USER_SCALE = 4f
    const val OUTPUT_SIZE = 512
    private const val MAX_SOURCE_EDGE = 2048

    fun sampleSize(width: Int, height: Int, maxEdge: Int = MAX_SOURCE_EDGE): Int {
        if (width <= 0 || height <= 0) return 1
        var sample = 1
        val longest = max(width, height)
        while (sample < 32 && longest > maxEdge * sample) {
            sample *= 2
        }
        return sample
    }

    fun baseScale(imageWidth: Int, imageHeight: Int, cropSizePx: Float): Float {
        return max(cropSizePx / imageWidth, cropSizePx / imageHeight)
    }

    fun clamp(
        imageWidth: Int,
        imageHeight: Int,
        cropSizePx: Float,
        userScale: Float,
        offsetX: Float,
        offsetY: Float,
    ): AvatarCropTransform {
        val scale = userScale.coerceIn(MIN_USER_SCALE, MAX_USER_SCALE)
        val total = baseScale(imageWidth, imageHeight, cropSizePx) * scale
        val maxX = ((imageWidth * total - cropSizePx) / 2f).coerceAtLeast(0f)
        val maxY = ((imageHeight * total - cropSizePx) / 2f).coerceAtLeast(0f)
        return AvatarCropTransform(
            userScale = scale,
            offsetX = offsetX.coerceIn(-maxX, maxX),
            offsetY = offsetY.coerceIn(-maxY, maxY),
        )
    }

    fun zoomAround(
        imageWidth: Int,
        imageHeight: Int,
        cropSizePx: Float,
        current: AvatarCropTransform,
        zoom: Float,
        panX: Float,
        panY: Float,
        focalXFromCenter: Float,
        focalYFromCenter: Float,
    ): AvatarCropTransform {
        val safeZoom = if (zoom.isFinite() && zoom > 0f) zoom else 1f
        val nextScale = (current.userScale * safeZoom).coerceIn(MIN_USER_SCALE, MAX_USER_SCALE)
        val ratio = nextScale / current.userScale
        return clamp(
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            cropSizePx = cropSizePx,
            userScale = nextScale,
            offsetX = focalXFromCenter * (1f - ratio) + current.offsetX * ratio + panX,
            offsetY = focalYFromCenter * (1f - ratio) + current.offsetY * ratio + panY,
        )
    }

    fun sourceRect(
        imageWidth: Int,
        imageHeight: Int,
        cropSizePx: Float,
        transform: AvatarCropTransform,
    ): AvatarCropRect {
        val clamped = clamp(
            imageWidth,
            imageHeight,
            cropSizePx,
            transform.userScale,
            transform.offsetX,
            transform.offsetY,
        )
        val total = baseScale(imageWidth, imageHeight, cropSizePx) * clamped.userScale
        val size = cropSizePx / total
        val left = imageWidth / 2f - (cropSizePx / 2f + clamped.offsetX) / total
        val top = imageHeight / 2f - (cropSizePx / 2f + clamped.offsetY) / total
        return AvatarCropRect(left, top, left + size, top + size)
    }

    fun render(source: Bitmap, rect: AvatarCropRect): Bitmap {
        val left = rect.left.coerceIn(0f, (source.width - 1).toFloat())
        val top = rect.top.coerceIn(0f, (source.height - 1).toFloat())
        val right = rect.right.coerceIn(left + 1f, source.width.toFloat())
        val bottom = rect.bottom.coerceIn(top + 1f, source.height.toFloat())
        val output = Bitmap.createBitmap(OUTPUT_SIZE, OUTPUT_SIZE, Bitmap.Config.ARGB_8888)
        Canvas(output).drawBitmap(
            source,
            Rect(
                left.toInt(),
                top.toInt(),
                right.toInt().coerceAtLeast(left.toInt() + 1),
                bottom.toInt().coerceAtLeast(top.toInt() + 1),
            ),
            RectF(0f, 0f, OUTPUT_SIZE.toFloat(), OUTPUT_SIZE.toFloat()),
            Paint(Paint.FILTER_BITMAP_FLAG),
        )
        return output
    }

    fun jpeg(bitmap: Bitmap): ByteArray {
        return ByteArrayOutputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, output)
            output.toByteArray()
        }
    }
}

internal fun decodeAvatarForCrop(context: Context, uri: Uri): Bitmap? {
    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
    if (bytes.isEmpty()) return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    val decoded = BitmapFactory.decodeByteArray(
        bytes,
        0,
        bytes.size,
        BitmapFactory.Options().apply {
            inSampleSize = AvatarCropMath.sampleSize(bounds.outWidth, bounds.outHeight)
        },
    ) ?: return null
    val orientation = runCatching {
        ExifInterface(ByteArrayInputStream(bytes)).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        )
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
    return orientBitmap(decoded, orientation)
}

private fun orientBitmap(bitmap: Bitmap, orientation: Int): Bitmap {
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.setScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            matrix.setRotate(90f)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            matrix.setRotate(270f)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(270f)
        else -> return bitmap
    }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AvatarCropDialog(
    bitmap: Bitmap,
    onDismiss: () -> Unit,
    onCropped: (ByteArray) -> Unit,
) {
    val transformState = remember { mutableStateOf(AvatarCropTransform(1f, 0f, 0f)) }
    val cropSizeState = remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }
    val image = remember(bitmap) { bitmap.asImageBitmap() }
    BackHandler(enabled = !saving, onBack = onDismiss)
    Dialog(
        onDismissRequest = { if (!saving) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text(stringResource(R.string.mine_crop_avatar)) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss, enabled = !saving) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.cancel),
                            )
                        }
                    },
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color.Black)
                        .onSizeChanged { size ->
                            val next = min(size.width, size.height) * 0.86f
                            cropSizeState.floatValue = next
                            val current = transformState.value
                            transformState.value = AvatarCropMath.clamp(
                                imageWidth = bitmap.width,
                                imageHeight = bitmap.height,
                                cropSizePx = next,
                                userScale = current.userScale,
                                offsetX = current.offsetX,
                                offsetY = current.offsetY,
                            )
                        }
                        .pointerInput(bitmap) {
                            detectTransformGestures { centroid, pan, zoom, _ ->
                                val cropSize = cropSizeState.floatValue
                                if (cropSize <= 0f || saving) return@detectTransformGestures
                                val current = transformState.value
                                transformState.value = AvatarCropMath.zoomAround(
                                    imageWidth = bitmap.width,
                                    imageHeight = bitmap.height,
                                    cropSizePx = cropSize,
                                    current = current,
                                    zoom = zoom,
                                    panX = pan.x,
                                    panY = pan.y,
                                    focalXFromCenter = centroid.x - size.width / 2f,
                                    focalYFromCenter = centroid.y - size.height / 2f,
                                )
                            }
                        },
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cropSize = cropSizeState.floatValue
                        if (cropSize <= 0f) return@Canvas
                        val transform = transformState.value
                        val total = AvatarCropMath.baseScale(bitmap.width, bitmap.height, cropSize) *
                            transform.userScale
                        val dispW = bitmap.width * total
                        val dispH = bitmap.height * total
                        val left = center.x + transform.offsetX - dispW / 2f
                        val top = center.y + transform.offsetY - dispH / 2f
                        drawImage(
                            image = image,
                            dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                            dstSize = IntSize(dispW.roundToInt().coerceAtLeast(1), dispH.roundToInt().coerceAtLeast(1)),
                            filterQuality = FilterQuality.Medium,
                        )
                        val radius = cropSize / 2f
                        val circle = Path().apply {
                            addOval(
                                ComposeRect(
                                    center = Offset(center.x, center.y),
                                    radius = radius,
                                ),
                            )
                        }
                        val full = Path().apply {
                            addRect(ComposeRect(Offset.Zero, Size(size.width, size.height)))
                        }
                        val dim = Path()
                        dim.op(full, circle, PathOperation.Difference)
                        drawPath(dim, Color.Black.copy(alpha = 0.62f))
                        drawCircle(
                            color = Color.White,
                            radius = radius,
                            center = center,
                            style = Stroke(width = 2.dp.toPx()),
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.mine_crop_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 20.dp, top = 16.dp, end = 20.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    TextButton(onClick = onDismiss, enabled = !saving) {
                        Text(stringResource(R.string.cancel))
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = {
                            if (saving) return@TextButton
                            val cropSize = cropSizeState.floatValue
                            if (cropSize <= 0f) return@TextButton
                            val transform = transformState.value
                            saving = true
                            scope.launch {
                                val bytes = withContext(Dispatchers.Default) {
                                    val rect = AvatarCropMath.sourceRect(
                                        bitmap.width,
                                        bitmap.height,
                                        cropSize,
                                        transform,
                                    )
                                    val cropped = AvatarCropMath.render(bitmap, rect)
                                    AvatarCropMath.jpeg(cropped)
                                }
                                onCropped(bytes)
                            }
                        },
                        enabled = !saving,
                    ) {
                        Text(stringResource(R.string.mine_crop_use))
                    }
                }
            }
        }
    }
}
