package com.vortex.launcher.ui.transition

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.tv.material3.Text
import com.vortex.launcher.ui.common.LauncherMode
import kotlin.math.hypot
import kotlin.math.max

/*
 * Timeline (progress p in 0..1):
 *   0.00-0.14  dark hole opens in the centre (old screen dims + shrinks a little)
 *   0.10-0.36  Gaming: white joystick flies out of the hole      | Classic: "Vortex" text flies out
 *   0.30-0.46  Gaming: "Vortex Gaming" appears under the joystick
 *   0.52-0.70  hole expands and swallows the whole screen  -> content is swapped while fully covered
 *   0.70-1.00  overlay fades out, new desktop settles in (scale 1.06 -> 1)
 *
 * p is an Animatable that is READ ONLY in draw / graphicsLayer lambdas -> no recomposition while playing.
 */
private const val COVER = 0.70f
private val HoleRadius = 130.dp
private val Overshoot = Easing { t -> val s = 1.70158f; val x = t - 1f; x * x * ((s + 1f) * x + s) + 1f }
private val Accelerate = CubicBezierEasing(0.5f, 0f, 0.9f, 0.4f)

private fun seg(p: Float, from: Float, to: Float, easing: Easing = LinearEasing): Float =
    easing.transform(((p - from) / (to - from)).coerceIn(0f, 1f))

/**
 * Hosts both launcher modes. Change [targetMode] and the cinematic transition plays automatically.
 * First composition shows [targetMode] immediately without animation.
 */
@Composable
fun ModeTransitionHost(
    targetMode: LauncherMode,
    classic: @Composable () -> Unit,
    gaming: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    var shown by remember { mutableStateOf(targetMode) }
    var running by remember { mutableStateOf<LauncherMode?>(null) }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(targetMode) {
        if (targetMode == shown) {
            running = null
            progress.snapTo(0f)
            return@LaunchedEffect
        }
        running = targetMode
        progress.snapTo(0f)
        progress.animateTo(COVER, tween(2800, easing = LinearEasing))
        shown = targetMode // swap happens while the hole fully covers the screen
        progress.animateTo(1f, tween(1200, easing = LinearEasing))
        running = null
        progress.snapTo(0f)
    }

    val blocking = running != null
    BackHandler(enabled = blocking) { /* swallow Back during the animation */ }

    Box(modifier.fillMaxSize().onPreviewKeyEvent { blocking }) {
        Box(
            Modifier.fillMaxSize().graphicsLayer {
                if (running != null) {
                    val p = progress.value
                    val s = if (p < COVER) 1f - 0.05f * seg(p, 0f, 0.5f)
                    else 1.06f - 0.06f * seg(p, COVER, 1f, FastOutSlowInEasing)
                    scaleX = s
                    scaleY = s
                }
            }
        ) {
            key(shown) {
                when (shown) {
                    LauncherMode.Classic -> classic()
                    LauncherMode.Gaming -> gaming()
                }
            }
        }

        when (running) {
            LauncherMode.Gaming -> HoleOverlay(gaming = true, progress = progress)
            LauncherMode.Classic -> HoleOverlay(gaming = false, progress = progress)
            null -> Unit
        }
    }
}

@Composable
private fun HoleOverlay(gaming: Boolean, progress: Animatable<Float, *>) {
    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) { drawHole(gaming, progress.value) }

        if (gaming) {
            // white joystick flies out of the red/black hole
            JoystickGlyph(
                Modifier
                    .align(Alignment.Center)
                    .size(112.dp, 140.dp)
                    .graphicsLayer {
                        val p = progress.value
                        val fly = seg(p, 0.10f, 0.36f, Overshoot)
                        val out = seg(p, 0.50f, 0.60f)
                        val s = (0.15f + 0.85f * fly) * (1f + 0.15f * out)
                        scaleX = s
                        scaleY = s
                        translationY = lerp(20.dp.toPx(), -40.dp.toPx(), fly)
                        rotationZ = (1f - fly) * -35f
                        alpha = seg(p, 0.10f, 0.18f) * (1f - out)
                    },
            )
            Text(
                "Vortex Gaming",
                color = Color.White,
                fontSize = 40.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 6.sp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer {
                        val p = progress.value
                        val e = seg(p, 0.30f, 0.46f, FastOutSlowInEasing)
                        translationY = 64.dp.toPx() + (1f - e) * 24.dp.toPx()
                        alpha = e * (1f - seg(p, 0.50f, 0.60f))
                    },
            )
        } else {
            // text "Vortex" flies out of the black hole
            Text(
                "Vortex",
                color = Color.White,
                fontSize = 72.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 14.sp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer {
                        val p = progress.value
                        val fly = seg(p, 0.10f, 0.36f, Overshoot)
                        val out = seg(p, 0.50f, 0.60f)
                        val s = (0.1f + 0.9f * fly) * (1f + 0.12f * out)
                        scaleX = s
                        scaleY = s
                        alpha = seg(p, 0.10f, 0.20f) * (1f - out)
                    },
            )
        }
    }
}

private fun DrawScope.drawHole(gaming: Boolean, p: Float) {
    val open = seg(p, 0f, 0.14f, FastOutSlowInEasing)
    val expand = seg(p, 0.52f, COVER, Accelerate)
    val fade = 1f - seg(p, COVER, 1f)
    if (fade <= 0f) return

    val inner = if (gaming) Color(0xFF050001) else Color.Black
    val mid = if (gaming) Color(0xFF2A0309) else Color(0xFF0A0B0E)
    val rim = if (gaming) Color(0xFFFF2D3A) else Color(0xFFD7DCE6)

    val c = Offset(size.width / 2f, size.height / 2f)
    val openR = HoleRadius.toPx()
    val fullR = hypot(size.width, size.height) * 0.55f
    val r = lerp(openR * open, fullR, expand)
    if (r < 1f) return

    // dim the outgoing screen
    drawRect(Color.Black.copy(alpha = 0.6f * max(open, expand) * fade))

    // hole body
    drawCircle(
        Brush.radialGradient(0f to inner, 0.7f to inner, 1f to mid, center = c, radius = r),
        radius = r,
        center = c,
        alpha = fade,
    )

    // swirling accretion streak inside the hole
    if (expand < 0.9f) {
        rotate(degrees = p * 540f, pivot = c) {
            drawCircle(
                Brush.sweepGradient(
                    listOf(Color.Transparent, rim.copy(alpha = 0.55f), Color.Transparent, Color.Transparent),
                    center = c,
                ),
                radius = r * 0.9f,
                center = c,
                alpha = fade,
                style = Stroke(r * 0.12f),
            )
        }
        // glowing rim: stacked translucent strokes instead of blur
        val layers = listOf(36f to 0.10f, 22f to 0.18f, 10f to 0.35f, 3f to 0.95f)
        for ((w, a) in layers) {
            drawCircle(rim.copy(alpha = a * fade), r, c, style = Stroke(w.dp.toPx()))
        }
    }
}

/** Arcade joystick: base plate, dome, shaft and a lit ball, drawn in white vectors. */
@Composable
private fun JoystickGlyph(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        // soft red under-glow
        drawCircle(
            Brush.radialGradient(
                listOf(Color(0xFFFF2D3A).copy(alpha = 0.35f), Color.Transparent),
                center = Offset(w / 2f, h * 0.6f),
                radius = w * 0.9f,
            ),
            radius = w * 0.9f,
            center = Offset(w / 2f, h * 0.6f),
        )
        // base plate
        drawRoundRect(
            Brush.verticalGradient(listOf(Color.White, Color(0xFFB8BCC6)), startY = h * 0.78f, endY = h * 0.96f),
            topLeft = Offset(w * 0.08f, h * 0.78f),
            size = Size(w * 0.84f, h * 0.18f),
            cornerRadius = CornerRadius(h * 0.09f),
        )
        // dome
        drawOval(
            Brush.verticalGradient(listOf(Color(0xFFF2F3F5), Color(0xFF9AA0AC)), startY = h * 0.66f, endY = h * 0.84f),
            topLeft = Offset(w * 0.28f, h * 0.66f),
            size = Size(w * 0.44f, h * 0.18f),
        )
        // shaft
        drawRoundRect(
            Color.White,
            topLeft = Offset(w * 0.46f, h * 0.28f),
            size = Size(w * 0.08f, h * 0.46f),
            cornerRadius = CornerRadius(w * 0.04f),
        )
        // ball
        drawCircle(
            Brush.radialGradient(
                listOf(Color.White, Color(0xFFC9CDD6), Color(0xFF8C92A0)),
                center = Offset(w * 0.43f, h * 0.17f),
                radius = w * 0.3f,
            ),
            radius = w * 0.2f,
            center = Offset(w * 0.5f, h * 0.22f),
        )
    }
}
