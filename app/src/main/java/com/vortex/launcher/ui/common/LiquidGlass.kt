package com.vortex.launcher.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import kotlinx.coroutines.launch

/** Monochrome palette. No emoji anywhere: only white / light-gray vector icons. */
object Glass {
    val Text = Color(0xFFF4F6FA)
    val TextDim = Color(0xFFB4BAC6)
    val Icon = Color(0xFFE9ECF2)
}

enum class GlassStrength(val fillTop: Float, val fillBottom: Float, val sheen: Float) {
    Light(0.12f, 0.04f, 0.18f),
    Regular(0.18f, 0.07f, 0.26f),
    Heavy(0.26f, 0.11f, 0.36f),
}

typealias FocusAnim = Animatable<Float, AnimationVector1D>

private fun Outline.toPath(): Path = when (this) {
    is Outline.Rectangle -> Path().apply { addRect(rect) }
    is Outline.Rounded -> Path().apply { addRoundRect(roundRect) }
    is Outline.Generic -> path
}

/**
 * "Liquid glass" WITHOUT any blur (cheap on SberBox): translucent white fill, top sheen,
 * diagonal specular glint, bottom rim light and a 1dp gradient white border.
 * [focus] is read only in the draw phase -> focus changes never trigger recomposition.
 */
fun Modifier.liquidGlass(
    shape: Shape,
    strength: GlassStrength = GlassStrength.Regular,
    focus: () -> Float = { 0f },
): Modifier = this.drawWithCache {
    val path = shape.createOutline(size, layoutDirection, this).toPath()
    val stroke = Stroke(1.dp.toPx())
    val sheenH = size.height * 0.55f

    val fill = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = strength.fillTop),
            Color.White.copy(alpha = strength.fillBottom),
        )
    )
    val sheen = Brush.verticalGradient(
        listOf(Color.White.copy(alpha = strength.sheen), Color.Transparent),
        startY = 0f,
        endY = sheenH,
    )
    val glint = Brush.linearGradient(
        listOf(Color.Transparent, Color.White.copy(alpha = 0.12f), Color.Transparent),
        start = Offset.Zero,
        end = Offset(size.width, size.height),
    )
    val rim = Brush.verticalGradient(
        listOf(Color.Transparent, Color.White.copy(alpha = 0.12f)),
        startY = size.height * 0.8f,
        endY = size.height,
    )
    val border = Brush.linearGradient(
        listOf(Color.White.copy(alpha = 0.85f), Color.White.copy(alpha = 0.18f), Color.White.copy(alpha = 0.5f)),
        start = Offset.Zero,
        end = Offset(size.width, size.height),
    )

    onDrawBehind {
        val f = focus().coerceIn(0f, 1f)
        drawPath(path, fill)
        if (f > 0f) drawPath(path, Color.White.copy(alpha = 0.12f * f))
        clipPath(path) {
            drawRect(sheen, size = Size(size.width, sheenH))
            drawRect(glint)
            drawRect(rim)
        }
        drawPath(path, border, alpha = 0.55f + 0.45f * f, style = stroke)
    }
}

/**
 * Focusable D-pad surface: liquid glass + spring scale via graphicsLayer (no recomposition).
 * [content] receives the focus progress (0..1) as a lambda; read it only in graphicsLayer / draw.
 */
@Composable
fun GlassSurface(
    onClick: () -> Unit,
    shape: Shape,
    modifier: Modifier = Modifier,
    strength: GlassStrength = GlassStrength.Regular,
    focusScale: Float = 1.08f,
    focusProgress: FocusAnim = remember { Animatable(0f) },
    onFocused: () -> Unit = {},
    content: @Composable BoxScope.(focus: () -> Float) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val interaction = remember { MutableInteractionSource() }
    val currentOnFocused by rememberUpdatedState(onFocused)
    val focusFn = remember(focusProgress) { { focusProgress.value } }

    Box(
        modifier = modifier
            .graphicsLayer {
                val s = 1f + (focusScale - 1f) * focusProgress.value
                scaleX = s
                scaleY = s
                zIndex = focusProgress.value
            }
            .liquidGlass(shape, strength, focusFn)
            .onFocusChanged { state ->
                scope.launch {
                    focusProgress.animateTo(
                        if (state.isFocused) 1f else 0f,
                        spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium),
                    )
                }
                if (state.isFocused) currentOnFocused()
            }
            .clip(shape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        content = { content(focusFn) },
    )
}

@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    strength: GlassStrength = GlassStrength.Regular,
) {
    GlassSurface(onClick, RoundedCornerShape(20.dp), modifier, strength) {
        Row(
            Modifier.padding(horizontal = 30.dp, vertical = 16.dp),
            Arrangement.spacedBy(12.dp),
            Alignment.CenterVertically,
        ) {
            if (icon != null) Icon(icon, null, Modifier.size(26.dp), Glass.Icon)
            Text(text, color = Glass.Text, fontSize = 22.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun GlassIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 64.dp,
) {
    GlassSurface(onClick, RoundedCornerShape(20.dp), modifier.size(size)) {
        Icon(icon, null, Modifier.align(Alignment.Center).size(size * 0.46f), Glass.Icon)
    }
}
