package com.example.gymlog.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

private val GlassFill = Color.White.copy(alpha = 0.34f)

/**
 * 液态玻璃卡片（拟玻璃方案 A）。
 *
 * 不使用 RenderEffect / backdrop blur，全版本可用：
 * - 半透明白底，透出页面背景色块
 * - 左上斜向高光层（液态感来源）
 * - 1px 渐变描边模拟玻璃边缘反光
 *
 * 内容文字需使用深色（见 [LiquidGlassColors]），玻璃上不要放白色正文。
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(GymLogRadii.panel),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .clip(shape)
            .background(GlassFill)
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
object LiquidGlassColors {
    /** 主文字（姓名/大数字/数值） */
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
