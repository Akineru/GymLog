package com.example.gymlog.ui.theme

import android.graphics.BlurMaskFilter
import android.graphics.Paint
import android.graphics.RadialGradient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

private val GlassTint = Color(0xFFF4F7FB).copy(alpha = 0.42f)

/** 页面背景流体色块（与 GlassBackdropBlobs 保持同一布局，供卡片内部磨砂场复用） */
internal data class BackdropBlob(
    val color: Color,
    val alpha: Float,
    val radiusDp: Float,
    val centerXDp: (screenW: Float, screenH: Float) -> Float,
    val centerYDp: (screenW: Float, screenH: Float) -> Float,
)

internal val BackdropBlobs = listOf(
    BackdropBlob(Color(0xFF5B96FF), 0.85f, 190f, { w, _ -> 130f }, { _, h -> 140f }),
    BackdropBlob(Color(0xFFFF8A50), 0.70f, 165f, { w, _ -> w - 115f }, { _, _ -> 195f }),
    BackdropBlob(Color(0xFF3FD9B4), 0.60f, 170f, { _, _ -> 110f }, { _, h -> h / 2f + 80f }),
    BackdropBlob(Color(0xFFB872FF), 0.55f, 160f, { w, _ -> w - 120f }, { _, h -> h - 190f }),
)

/**
 * 液态磨砂玻璃卡片（自研实现，无第三方依赖）。
 *
 * 原理：卡片知道自己在全屏中的位置后，在内部按同样的布局画一份
 * 背景流体色块，并用 BlurMaskFilter 做真实高斯模糊 —— 视觉上等价于
 * backdrop blur（磨砂折射），但不需要 RenderEffect、全版本可用。
 * 再叠加白色提亮、左上斜向高光和 1px 渐变描边。
 *
 * 内容文字需使用深色（见 [LiquidGlassColors]），玻璃上不要放白色正文。
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(GymLogRadii.panel),
    content: @Composable ColumnScope.() -> Unit,
) {
    val configuration = LocalConfiguration.current
    val screenWDp = configuration.screenWidthDp.toFloat()
    val screenHDp = configuration.screenHeightDp.toFloat()

    var cardOrigin by remember { mutableStateOf(Offset.Zero) }

    Column(
        modifier = modifier
            .onGloballyPositioned { cardOrigin = it.positionInRoot() }
            .clip(shape)
            .drawBehind {
                drawFrostedBackdrop(cardOrigin, screenWDp, screenHDp)
                // 白色提亮层
                drawRect(GlassTint)
                // 左上 45° 斜向高光
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.50f),
                            Color.White.copy(alpha = 0.10f),
                            Color.Transparent,
                        ),
                        start = Offset.Zero,
                        end = Offset(size.width * 0.55f, size.height * 0.55f),
                    ),
                )
            }
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.95f),
                        Color.White.copy(alpha = 0.25f),
                        Color.White.copy(alpha = 0.10f),
                        Color.White.copy(alpha = 0.60f),
                    ),
                ),
                shape = shape,
            ),
        content = content,
    )
}

/** 在卡片坐标下画整屏色块场的模糊版本（位置校正 = 色块窗口坐标 - 卡片原点） */
private fun DrawScope.drawFrostedBackdrop(cardOrigin: Offset, screenWDp: Float, screenHDp: Float) {
    val blurPx = 34f * density
    val paint = Paint().apply {
        isAntiAlias = true
        maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
    }
    for (blob in BackdropBlobs) {
        val cx = blob.centerXDp(screenWDp, screenHDp).dp.toPx() - cardOrigin.x
        val cy = blob.centerYDp(screenWDp, screenHDp).dp.toPx() - cardOrigin.y
        val r = blob.radiusDp.dp.toPx() + blurPx
        paint.shader = RadialGradient(
            cx, cy, r,
            intArrayOf(blob.color.copy(alpha = blob.alpha).toArgb(), Color.Transparent.toArgb()),
            floatArrayOf(0f, 1f),
            android.graphics.Shader.TileMode.CLAMP,
        )
        drawContext.canvas.nativeCanvas.drawCircle(cx, cy, r, paint)
    }
}

/** 页面背景流体色块：与卡片内部磨砂场使用同一份布局数据 */
@Composable
fun GlassBackdropBlobs(modifier: Modifier = Modifier) {
    val configuration = LocalConfiguration.current
    val w = configuration.screenWidthDp.toFloat()
    val h = configuration.screenHeightDp.toFloat()
    Box(modifier) {
        for (blob in BackdropBlobs) {
            val cx = blob.centerXDp(w, h)
            val cy = blob.centerYDp(w, h)
            val r = blob.radiusDp
            Box(
                modifier = Modifier
                    .offset(x = (cx - r).dp, y = (cy - r).dp)
                    .size((2 * r).dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                blob.color.copy(alpha = blob.alpha),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )
        }
    }
}

/** 玻璃卡片统一配色（深色文字保证对比度） */
object LiquidGlassColors {
    /** 主文字（姓名/大数字/数值） */
    val ink = Color(0xFF141C28)

    /** 次要文字（标签/单位） */
    val inkSoft = Color(0xFF4A5566)

    /** 中等文字（日期区间等） */
    val inkMid = Color(0xFF3F4C5F)

    /** 卡内分隔线 */
    val divider = Color(0x33788CAA)

    /** 玻璃半透明底 */
    val fill = GlassTint
}
