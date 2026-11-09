package com.example.gymlog.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 全 app 的材质体系（2026-11-08 起）：
 *
 * 1. 页面背景：`Modifier.appMeshBackground()` —— 淡蓝灰底 + 四角环境色晕，只在 GymLogApp 根节点画一次。
 * 2. 主体卡片：`Modifier.ceramicSurface()` —— 不透明哑光陶瓷，靠釉面柔光 + 浅灰圈边定轮廓，不用透明度。
 * 3. 局部组件：磨砂玻璃让位给「淡色底 + 细描边」，只出现在分段器、标签、按钮这类小面积控件上
 *    （见 `ui/home/HomeTheme.kt` 的 frost / brandChip）。
 *
 * 旧的磨砂玻璃卡（LiquidGlassCard + GlassBackdropBlobs + 逐卡重绘模糊背景）已经移除：
 * 那套在滚动时每帧重绘模糊，且卡片贴在淡背景上会发灰。名字保留 `LiquidGlassCard` 只为不动调用点。
 */

// ---------------------------------------------------------------- 卡片文字色

private val LightInk = Color(0xFF202B40)
private val LightInkMid = Color(0xFF5D6A7D)
private val LightInkSoft = Color(0xFF7D8798)
private val LightDivider = Color(0x0F202B40)

private val DarkInk = Color(0xFFE7EDF7)
private val DarkInkMid = Color(0xFFC3CDDC)
private val DarkInkSoft = Color(0xFF9AA6B8)
private val DarkDivider = Color(0x1FFFFFFF)

/**
 * 卡片内通用文字色。改成 @Composable 读取，深浅色自动跟随主题
 * （旧实现是一组写死的浅色值，深色模式下同样会取到深色文字）。
 */
object LiquidGlassColors {
    /** 主文字：标题、大数字 */
    val ink: Color
        @Composable @ReadOnlyComposable get() =
            if (LocalGymLogDarkTheme.current) DarkInk else LightInk

    /** 中等文字：次级标题、日期区间 */
    val inkMid: Color
        @Composable @ReadOnlyComposable get() =
            if (LocalGymLogDarkTheme.current) DarkInkMid else LightInkMid

    /** 弱化文字：标签、单位、说明 */
    val inkSoft: Color
        @Composable @ReadOnlyComposable get() =
            if (LocalGymLogDarkTheme.current) DarkInkSoft else LightInkSoft

    /** 卡内分隔线 */
    val divider: Color
        @Composable @ReadOnlyComposable get() =
            if (LocalGymLogDarkTheme.current) DarkDivider else LightDivider

    /** 兼容旧签名，卡面已不再使用半透明填充 */
    val fill: Color
        @Composable @ReadOnlyComposable get() = Color.Transparent
}

// ---------------------------------------------------------------- 卡片材质

/**
 * 哑光陶瓷卡面：不透明渐变底 + 顶部釉面高光 + 1dp 浅灰圈边。
 * 默认投影只落在卡片下沿 5dp 内，不扩散，避免"悬浮"。
 *
 * [floatingElevation] 给真正需要浮在页面之上的卡片（如「锻炼中」悬浮条）用：
 * 传值后用该高度做投影，且暗色主题下也给投影、阴影色更重。
 */
@Composable
internal fun Modifier.ceramicSurface(
    shape: Shape = RoundedCornerShape(GymLogRadii.panel),
    floatingElevation: Dp? = null,
): Modifier {
    val dark = LocalGymLogDarkTheme.current
    val top = if (dark) Color(0xFF19222F) else Color(0xFFFFFFFF)
    val bottom = if (dark) Color(0xFF151D29) else Color(0xFFFAFBFD)
    val rim = if (dark) Color(0xFF232D3D) else Color(0xFFEDF0F6)
    val sheen = if (dark) Color(0x1FFFFFFF) else Color(0xE6FFFFFF)
    val shadowTint =
        if (floatingElevation != null) Color(0x4A1F2B3F) else Color(0x14202B40)
    return this
        .shadow(
            elevation = floatingElevation ?: if (dark) 0.dp else 5.dp,
            shape = shape,
            clip = false,
            ambientColor = shadowTint,
            spotColor = shadowTint,
        )
        .clip(shape)
        .background(Brush.linearGradient(listOf(top, bottom)))
        .background(
            Brush.verticalGradient(
                0f to sheen,
                0.30f to Color.Transparent,
            ),
        )
        .border(1.dp, rim, shape)
}

/**
 * 旧名字保留：现在渲染的是哑光陶瓷卡，不再是磨砂玻璃。
 * 全 app 的卡片都走这里，改一次全站生效。
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(GymLogRadii.panel),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.ceramicSurface(shape),
        content = content,
    )
}

// ---------------------------------------------------------------- 页面背景

/**
 * 页面背景：淡蓝灰底 + 四角大范围环境色晕 + 顶部天光。
 *
 * 四个色晕半径都是屏宽的 1.2–1.35 倍、圆心压在屏幕外侧，所以看不到"光斑"，
 * 只看到颜色从角落慢慢化开；中间档位先压到 45% 再化到透明，避免硬边和分层。
 */
@Composable
fun Modifier.appMeshBackground(): Modifier {
    val dark = LocalGymLogDarkTheme.current
    val pageTop = if (dark) Color(0xFF111A29) else Color(0xFFEDF2FC)
    val pageBottom = if (dark) Color(0xFF0B1220) else Color(0xFFEEF2FC)
    val topLeft = if (dark) Color(0x8C1D3153) else Color(0xF0DFEAFE)
    val topRight = if (dark) Color(0x8024203F) else Color(0xE0EEF0FF)
    val bottomRight = if (dark) Color(0x731C1B38) else Color(0xCCE8E5FA)
    val bottomLeft = if (dark) Color(0x66142B33) else Color(0xC2E4F2F7)
    val ambient = if (dark) Color(0x12FFFFFF) else Color(0x1CFFFFFF)
    return this
        .background(Brush.verticalGradient(listOf(pageTop, pageBottom)))
        .drawBehind {
            fun corner(color: Color, cx: Float, cy: Float, radiusFactor: Float) {
                drawRect(
                    Brush.radialGradient(
                        colorStops = arrayOf(
                            0f to color,
                            0.55f to color.copy(alpha = color.alpha * 0.45f),
                            1f to color.copy(alpha = 0f),
                        ),
                        center = Offset(size.width * cx, size.height * cy),
                        radius = size.width * radiusFactor,
                    ),
                )
            }
            corner(topLeft, -0.06f, -0.04f, 1.30f)
            corner(topRight, 1.04f, -0.02f, 1.20f)
            corner(bottomRight, 1.06f, 1.02f, 1.35f)
            corner(bottomLeft, -0.04f, 0.98f, 1.25f)
            drawRect(
                Brush.verticalGradient(
                    0f to ambient,
                    0.42f to ambient.copy(alpha = 0f),
                ),
            )
        }
}
