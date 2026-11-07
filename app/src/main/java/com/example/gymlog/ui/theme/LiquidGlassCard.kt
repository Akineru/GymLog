package com.example.gymlog.ui.theme

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
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

import com.chrisbanes.haze.HazeInput
import com.chrisbanes.haze.HazeState
import com.chrisbanes.haze.HazeTint
import com.chrisbanes.haze.hazeBlur
import com.chrisbanes.haze.hazeBlurStyle
import dev.chrisbanes.haze.materials.HazeMaterials

/** 页面级 Haze 状态：有值时玻璃卡启用真实 backdrop blur（磨砂），null 时退化为拟玻璃 */
val LocalGlassHazeState = compositionLocalOf<HazeState?> { null }

private val GlassFill = Color.White.copy(alpha = 0.34f)

/**
 * 液态玻璃卡片。
 *
 * - 页面提供了 [LocalGlassHazeState]（API 31+）：真实 backdrop blur 磨砂 + 白色提亮，透出滚动内容
 * - 未提供（或低版本自动降级）：半透明白底拟玻璃
 * - 共同点：左上斜向高光层 + 1px 渐变描边
 *
 * 内容文字需使用深色（见 [LiquidGlassColors]），玻璃上不要放白色正文。
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(GymLogRadii.panel),
    content: @Composable ColumnScope.() -> Unit,
) {
    val hazeState = LocalGlassHazeState.current
    Column(
        modifier = modifier
            .clip(shape)
            .then(
                if (hazeState != null) {
                    Modifier.hazeBlur(
                        input = HazeInput.Sources(hazeState),
                        style = HazeBlurStyle {
                            blurRadius(28.dp)
                            tints += HazeTint(Color(0xFFF4F7FB).copy(alpha = 0.55f))
                            // 磨砂颗粒：噪点是"砂"的关键，比模糊本身更能带来磨砂质感
                            noiseFactor(0.18f)
                        },
                    )
                } else {
                    Modifier.background(GlassFill)
                },
            )
            .drawBehind {
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

/** 玻璃卡片统一配色（深色文字保证对比度） */
object LiquidGlassColors {    /** 主文字（姓名/大数字/数值） */
    val ink = Color(0xFF141C28)

    /** 次要文字（标签/单位） */
    val inkSoft = Color(0xFF5A6676)

    /** 中等文字（日期区间等） */
    val inkMid = Color(0xFF3F4C5F)

    /** 卡内分隔线 */
    val divider = Color(0x33788CAA)

    /** 玻璃半透明底 */
    val fill = GlassFill
}

/** 低饱和径向色块，给玻璃卡片提供可透出的流体背景（radialGradient 软边，不依赖 RenderEffect，全版本一致） */
@Composable
fun GlassBackdropBlobs(modifier: Modifier = Modifier) {
    Box(modifier) {
        Blob(
            color = Color(0xFF7FB4FF),
            alpha = 0.45f,
            size = 300.dp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (-80).dp, y = (-60).dp),
        )
        Blob(
            color = Color(0xFFFF9D6C),
            alpha = 0.34f,
            size = 250.dp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 70.dp, y = 40.dp),
        )
        Blob(
            color = Color(0xFF69E0C3),
            alpha = 0.30f,
            size = 260.dp,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = (-70).dp, y = 90.dp),
        )
        Blob(
            color = Color(0xFFC78BFF),
            alpha = 0.26f,
            size = 240.dp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 60.dp, y = (-40).dp),
        )
    }
}

@Composable
private fun Blob(
    color: Color,
    alpha: Float,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(color.copy(alpha = alpha), Color.Transparent),
                ),
            ),
    )
}
