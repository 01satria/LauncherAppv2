package id.satria.launcher.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import id.satria.launcher.data.CategoryStyle
import id.satria.launcher.data.IconEffect
import id.satria.launcher.data.IconShape
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ─────────────────────────────────────────────────────────────────────────────
// StyledAppIcon
// Renders a bitmap icon dengan shape + effect sesuai CategoryStyle.
// Tidak mengalokasikan Bitmap tambahan — semua via Canvas (RenderThread-friendly).
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun StyledAppIcon(
    bitmap: ImageBitmap?,
    style: CategoryStyle,
    sizeDp: Dp,
    modifier: Modifier = Modifier,
) {
    val primaryColor   = Color(style.primaryColor)
    val secondaryColor = Color(style.secondaryColor)
    val borderColor    = Color(style.borderColor)
    val shape          = style.shapeEnum()
    val effect         = style.effectEnum()

    // Watercolor: animated ripple offset
    val infiniteTransition = rememberInfiniteTransition(label = "wcAnim")
    val wcOffset by infiniteTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = 360f,
        animationSpec = infiniteRepeatable(
            animation  = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wcOffset",
    )

    Canvas(
        modifier = modifier.size(sizeDp),
    ) {
        val canvasSize = size.minDimension
        val path       = buildShapePath(shape, canvasSize)

        clipPath(path) {
            // 1. Background fill
            when (effect) {
                IconEffect.GRADIENT -> {
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(primaryColor, secondaryColor),
                            start  = Offset(0f, 0f),
                            end    = Offset(canvasSize, canvasSize),
                        ),
                        size = Size(canvasSize, canvasSize),
                    )
                }
                IconEffect.WATERCOLOR -> {
                    drawRect(color = primaryColor.copy(alpha = 0.15f), size = Size(canvasSize, canvasSize))
                    drawWatercolor(primaryColor, secondaryColor, canvasSize, wcOffset)
                }
                IconEffect.GLASS -> {
                    drawRect(color = primaryColor.copy(alpha = 0.35f), size = Size(canvasSize, canvasSize))
                    drawGlassHighlight(canvasSize)
                }
                IconEffect.NONE -> {
                    // No background — icon fills the shape
                }
            }

            // 2. Draw icon bitmap
            if (bitmap != null) {
                val alpha = style.opacity.coerceIn(0f, 1f)
                drawImage(
                    image       = bitmap,
                    dstSize     = IntSize(canvasSize.toInt(), canvasSize.toInt()),
                    colorFilter = if (effect == IconEffect.WATERCOLOR)
                        ColorFilter.tint(primaryColor.copy(alpha = 0.18f), BlendMode.SrcOver)
                    else null,
                    alpha       = alpha,
                )
            }

            // 3. Glass shimmer overlay
            if (effect == IconEffect.GLASS) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.30f), Color.Transparent),
                        startY = 0f,
                        endY   = canvasSize * 0.55f,
                    ),
                    size = Size(canvasSize, canvasSize),
                )
            }
        }

        // 4. Border (drawn outside clip so it overlaps the edge)
        if (style.borderThickness > 0f) {
            drawPath(
                path  = path,
                color = borderColor,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = style.borderThickness * density,
                ),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// buildShapePath — build clip path for each IconShape
// ─────────────────────────────────────────────────────────────────────────────
private fun DrawScope.buildShapePath(shape: IconShape, size: Float): Path {
    val path = Path()
    val half = size / 2f

    when (shape) {
        IconShape.CIRCLE -> {
            path.addOval(Rect(0f, 0f, size, size))
        }
        IconShape.ROUNDED_SQUARE -> {
            val r = size * 0.22f
            path.addRoundRect(RoundRect(0f, 0f, size, size, r, r))
        }
        IconShape.SQUIRCLE -> {
            // Approximasi squircle dengan cubic beziers
            // Formula: |x/r|^4 + |y/r|^4 = 1  →  n=4 squircle
            val ctrl = half * 0.55f   // ~55% control point untuk squircle
            path.apply {
                moveTo(half, 0f)
                cubicTo(half + ctrl, 0f,         size,        half - ctrl, size, half)
                cubicTo(size,        half + ctrl, half + ctrl, size,        half, size)
                cubicTo(half - ctrl, size,        0f,          half + ctrl, 0f,   half)
                cubicTo(0f,          half - ctrl, half - ctrl, 0f,          half, 0f)
                close()
            }
        }
        IconShape.TEARDROP -> {
            // Teardrop: lingkaran di bawah + titik di atas
            val r = half * 0.78f
            val cx = half; val cy = half + r * 0.12f
            path.addOval(Rect(cx - r, cy - r, cx + r, cy + r))
            // Triangle top
            path.moveTo(half - half * 0.28f, cy - r * 0.72f)
            path.lineTo(half, 0f)
            path.lineTo(half + half * 0.28f, cy - r * 0.72f)
            path.close()
        }
        IconShape.NONE -> {
            // No clip — full square
            path.addRect(Rect(0f, 0f, size, size))
        }
    }
    return path
}

// ─────────────────────────────────────────────────────────────────────────────
// drawWatercolor — lukisan watercolor menggunakan banyak circle dengan alpha rendah
// Efek berjalan di Canvas thread — zero bitmap allocation
// ─────────────────────────────────────────────────────────────────────────────
private fun DrawScope.drawWatercolor(
    primary: Color,
    secondary: Color,
    size: Float,
    angleOffset: Float,
) {
    val half = size / 2f
    // 12 blob melingkar yang berputar perlahan
    repeat(12) { i ->
        val angle  = (i * 30f + angleOffset) * PI.toFloat() / 180f
        val radius = size * (0.25f + (i % 3) * 0.10f)
        val cx     = half + cos(angle) * radius * 0.35f
        val cy     = half + sin(angle) * radius * 0.35f
        val r      = size * 0.28f
        val color  = if (i % 2 == 0) primary else secondary
        drawCircle(
            brush  = Brush.radialGradient(
                colors  = listOf(color.copy(alpha = 0.22f), Color.Transparent),
                center  = Offset(cx, cy),
                radius  = r,
            ),
            radius = r,
            center = Offset(cx, cy),
        )
    }
    // Lapisan tengah yang lebih pekat
    drawCircle(
        brush  = Brush.radialGradient(
            colors  = listOf(primary.copy(alpha = 0.15f), Color.Transparent),
            center  = Offset(half, half),
            radius  = size * 0.4f,
        ),
        radius = size * 0.4f,
        center = Offset(half, half),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// drawGlassHighlight — frosted glass card effect
// ─────────────────────────────────────────────────────────────────────────────
private fun DrawScope.drawGlassHighlight(size: Float) {
    // Subtle diagonal shimmer
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.12f),
                Color.Transparent,
                Color.White.copy(alpha = 0.06f),
            ),
            start = Offset(0f, 0f),
            end   = Offset(size * 0.7f, size * 0.7f),
        ),
        size = Size(size, size),
    )
}
