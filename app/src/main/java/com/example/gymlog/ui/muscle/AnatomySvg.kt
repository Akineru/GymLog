package com.example.gymlog.ui.muscle

import android.graphics.RectF
import android.graphics.Region
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.vector.PathParser

internal enum class AnatomyKind {
    SKIN,
    OUTLINE,
    MUSCLE,
    LINE,
}

internal data class AnatomyPath(
    val id: String,
    val kind: AnatomyKind,
    val path: Path,
    val hit: Region,
    val centerX: Float,
    val centerY: Float,
)

internal data class AnatomySheet(
    val width: Float,
    val height: Float,
    val paths: List<AnatomyPath>,
) {
    val muscles: List<AnatomyPath> = paths.filter { it.kind == AnatomyKind.MUSCLE }
}

internal object AnatomySvg {
    fun parse(svg: String): AnatomySheet {
        val box = viewBox.find(svg) ?: error("missing viewBox")
        val width = box.groupValues[3].toFloat()
        val height = box.groupValues[4].toFloat()
        val parsed = pathTag.findAll(svg).mapNotNull { match ->
            val attrs = attributes(match.groupValues[1])
            val id = attrs["id"] ?: return@mapNotNull null
            val data = attrs["d"] ?: return@mapNotNull null
            val kind = kindOf(id)
            val path = PathParser().parsePathString(data).toPath()
            val bounds = RectF()
            val native = path.asAndroidPath()
            native.computeBounds(bounds, true)
            val hit = Region()
            if (kind == AnatomyKind.MUSCLE) {
                hit.setPath(
                    native,
                    Region(
                        bounds.left.toInt() - 2,
                        bounds.top.toInt() - 2,
                        bounds.right.toInt() + 2,
                        bounds.bottom.toInt() + 2,
                    ),
                )
            }
            AnatomyPath(
                id = id,
                kind = kind,
                path = path,
                hit = hit,
                centerX = bounds.centerX(),
                centerY = bounds.centerY(),
            )
        }.toList()
        val body = parsed.first { it.id == "skin-body" }.hit.also { bodyHit ->
            val bounds = RectF()
            val native = parsed.first { it.id == "skin-body" }.path.asAndroidPath()
            native.computeBounds(bounds, true)
            bodyHit.setPath(
                native,
                Region(
                    bounds.left.toInt() - 2,
                    bounds.top.toInt() - 2,
                    bounds.right.toInt() + 2,
                    bounds.bottom.toInt() + 2,
                ),
            )
        }
        parsed.filter { it.kind == AnatomyKind.MUSCLE }.forEach { muscle ->
            muscle.hit.op(body, Region.Op.INTERSECT)
        }
        return AnatomySheet(width, height, parsed)
    }

    private fun kindOf(id: String): AnatomyKind {
        return when {
            id == "skin-outline" -> AnatomyKind.OUTLINE
            id.startsWith("skin-") -> AnatomyKind.SKIN
            id.startsWith("line-") -> AnatomyKind.LINE
            else -> AnatomyKind.MUSCLE
        }
    }

    private fun attributes(source: String): Map<String, String> {
        return attr.findAll(source).associate { it.groupValues[1] to it.groupValues[2] }
    }

    private val viewBox = Regex("""viewBox="([\d.]+) ([\d.]+) ([\d.]+) ([\d.]+)"""")
    private val pathTag = Regex("""<path\b([^>]*?)/?>""")
    private val attr = Regex("""([\w-]+)="([^"]*)"""")
}
