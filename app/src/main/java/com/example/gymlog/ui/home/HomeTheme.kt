package com.example.gymlog.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.example.gymlog.ui.theme.LocalGymLogDarkTheme

/**
 * 首页视觉体系（第二轮精修）：
 *
 * 第 1 层 页面背景 —— 极淡蓝紫 mesh，只做环境光，不出现单点光斑。
 * 第 2 层 主体卡片 —— 不透明哑光陶瓷，靠「细腻度 + 色温」区分三种白，不用透明度。
 * 第 3 层 局部组件 —— 磨砂玻璃，只贴分段器 / 日期切换 / 状态标签 / 底栏这种小面积部件。
 *
 * 所有颜色、圆角、阴影都从这里取，页面里不再写字面值。
 */
@Immutable
data class HomeColors(
    // 第 2 层：三种陶瓷
    val ceramicTop: Color,
    val ceramicBottom: Color,
    val ceramicFlatTop: Color,
    val ceramicFlatBottom: Color,
    val ceramicWarmTop: Color,
    val ceramicWarmBottom: Color,
    val rim: Color,
    // 文字
    val ink: Color,
    val inkSecondary: Color,
    val inkTertiary: Color,
    // 线、底
    val hair: Color,
    val tileBackground: Color,
    // 品牌
    val brand: Color,
    val onBrand: Color,
    val brandSoft: Color,
    /** 陶瓷釉面高光色（浅色用纯白，深色只能极淡，否则卡面会发灰） */
    val sheen: Color,
    // 第 3 层：磨砂
    val frostFill: Color,
    val frostFillSoft: Color,
    val frostBorder: Color,
    // 底部导航
    val barTop: Color,
    val barBottom: Color,
    val barDivider: Color,
)

internal val LightHomeColors = HomeColors(
    ceramicTop = Color(0xFFFFFFFF),
    ceramicBottom = Color(0xFFFAFBFD),
    ceramicFlatTop = Color(0xFFFFFFFF),
    ceramicFlatBottom = Color(0xFFFCFDFE),
    ceramicWarmTop = Color(0xFFFFFFFF),
    ceramicWarmBottom = Color(0xFFFDFCFA),
    rim = Color(0xFFEDF0F6),
    ink = Color(0xFF202B40),
    inkSecondary = Color(0xFF7D8798),
    inkTertiary = Color(0xFFA0A8B5),
    hair = Color(0x0F202B40),
    tileBackground = Color(0xFFF3F5F9),
    brand = Color(0xFF4380F5),
    onBrand = Color.White,
    brandSoft = Color(0xFFEAF1FF),
    sheen = Color.White,
    frostFill = Color(0xC6EEF2F9),
    frostFillSoft = Color(0xA6EDF1F8),
    frostBorder = Color(0xFFE7EBF2),
    barTop = Color(0xD9FCFDFF),
    barBottom = Color(0xF0FCFDFF),
    barDivider = Color(0x0F202B40),
)

internal val DarkHomeColors = HomeColors(
    ceramicTop = Color(0xFF19222F),
    ceramicBottom = Color(0xFF151D29),
    ceramicFlatTop = Color(0xFF171F2C),
    ceramicFlatBottom = Color(0xFF141C28),
    ceramicWarmTop = Color(0xFF1B232F),
    ceramicWarmBottom = Color(0xFF171E29),
    rim = Color(0xFF232D3D),
    ink = Color(0xFFE7EDF7),
    inkSecondary = Color(0xFF9AA6B8),
    inkTertiary = Color(0xFF76828F),
    hair = Color(0x1FFFFFFF),
    tileBackground = Color(0xFF1E2836),
    brand = Color(0xFF6E9DFF),
    onBrand = Color(0xFF0B1B33),
    brandSoft = Color(0xFF22314A),
    sheen = Color(0x1FFFFFFF),
    frostFill = Color(0x2EFFFFFF),
    frostFillSoft = Color(0x1FFFFFFF),
    frostBorder = Color(0x2EFFFFFF),
    barTop = Color(0xE6101826),
    barBottom = Color(0xF20C1320),
    barDivider = Color(0x1FFFFFFF),
)

@Composable
@ReadOnlyComposable
internal fun homeColors(): HomeColors =
    if (LocalGymLogDarkTheme.current) DarkHomeColors else LightHomeColors

/** 圆角：大卡 20 / 小卡 11 / 控件 10–14 / 胶囊 999，层级固定，不再逐处微调。 */
internal object HomeDims {
    val cardRadius = 20.dp
    val tileRadius = 11.dp
    val controlRadius = 14.dp
    val chipRadius = 999.dp
    val cardPadding = 18.dp
    val cardGap = 10.dp
    val screenPadding = 16.dp
}

internal enum class CeramicStyle {
    /** 训练概览：最厚的哑光陶瓷白 */
    Overview,

    /** 训练足迹：更平整细腻，几乎没有渐变 */
    Flat,

    /** 今日训练：微弱暖白 */
    Warm,
}

/**
 * 哑光陶瓷卡面：不透明底 + 顶部柔光 + 1dp 浅灰圈边。
 * 投影只落在卡片下沿 7～13dp 内，不扩散，避免"悬浮"。
 */
internal fun Modifier.ceramic(
    colors: HomeColors,
    style: CeramicStyle,
    shape: Shape = RoundedCornerShape(HomeDims.cardRadius),
): Modifier {
    val top: Color
    val bottom: Color
    val sheenAlpha: Float
    val sheenEnd: Float
    val elevation: Dp
    val ambient: Color
    when (style) {
        CeramicStyle.Overview -> {
            top = colors.ceramicTop
            bottom = colors.ceramicBottom
            sheenAlpha = 0.85f
            sheenEnd = 0.30f
            elevation = 6.dp
            ambient = Color(0x14202B40)
        }
        CeramicStyle.Flat -> {
            top = colors.ceramicFlatTop
            bottom = colors.ceramicFlatBottom
            sheenAlpha = 0.55f
            sheenEnd = 0.22f
            elevation = 3.dp
            ambient = Color(0x0D202B40)
        }
        CeramicStyle.Warm -> {
            top = colors.ceramicWarmTop
            bottom = colors.ceramicWarmBottom
            sheenAlpha = 0.80f
            sheenEnd = 0.34f
            elevation = 4.dp
            ambient = Color(0x10202B40)
        }
    }
    val sheenColor = colors.sheen
    val sheen = Brush.verticalGradient(
        0f to sheenColor.copy(alpha = sheenColor.alpha * sheenAlpha),
        sheenEnd to Color.Transparent,
    )
    return this
        .shadow(
            elevation = elevation,
            shape = shape,
            clip = false,
            ambientColor = ambient,
            spotColor = ambient,
        )
        .clip(shape)
        .background(Brush.linearGradient(listOf(top, bottom)))
        .background(sheen)
        .border(1.dp, colors.rim, shape)
}

/** 局部磨砂玻璃：半透明填充 + 白色细边，只用于小面积控件。 */
internal fun Modifier.frost(
    colors: HomeColors,
    shape: Shape,
    strong: Boolean = false,
): Modifier {
    val fill = if (strong) colors.frostFill else colors.frostFillSoft
    return this
        .clip(shape)
        .background(
            Brush.linearGradient(
                listOf(fill, fill.copy(alpha = fill.alpha * 0.62f)),
            ),
        )
        .border(1.dp, colors.frostBorder, shape)
}

/** 浅蓝半透明状态标签（今天 / 选中提示这类） */
internal fun Modifier.brandChip(colors: HomeColors, shape: Shape): Modifier {
    return this
        .clip(shape)
        .background(colors.brandSoft.copy(alpha = 0.9f))
        .border(1.dp, Color.White.copy(alpha = 0.75f), shape)
}

