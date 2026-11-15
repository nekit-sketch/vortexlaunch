package com.vortex.launcher.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.tv.material3.Text
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val Ice = Color(0xFFBFD8FF)
private const val PI_F = PI.toFloat()

/**
 * Liquid droplets spiral in, merge into a 3-arm glowing vortex, a pulse ring fires and
 * the "VORTEX" wordmark fades in. Everything is drawn in ONE Canvas; the animation values
 * are read only in the draw phase, so there is no recomposition during the splash.
 * Glow is faked with stacked translucent strokes (no blur / RenderEffect).
 */
@Composable
fun SplashScreen(onFinished: () -> Unit, modifier: Modifier = Modifier) {
    val form = remember { Animatable(0f) }
    val pulse = remember { Animatable(0f) }
    val label = remember { Animatable(0f) }
    val finish by rememberUpdatedState(onFinished)

    LaunchedEffect(Unit) {
        coroutineScope {
            launch { form.animateTo(1f, tween(2400, easing = CubicBezierEasing(0.25f, 0f, 0.15f, 1f))) }
            launch { delay(1800); pulse.animateTo(1f, tween(1000, easing = LinearOutSlowInEasing)) }
            launch { delay(1500); label.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
        }
        delay(450)
        finish()
    }

    Box(modifier.fillMaxSize().background(Color(0xFF020306))) {
        Canvas(Modifier.fillMaxSize()) { drawVortex(form.value, pulse.value) }
        Text(
            text = "VORTEX",
            color = Color.White,
            fontSize = 46.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = 14.sp,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 150.dp)
                .graphicsLayer {
                    alpha = label.value
                    translationY = (1f - label.value) * 20.dp.toPx()
                    val s = 1.08f - 0.08f * label.value
                    scaleX = s
                    scaleY = s
                },
        )
    }
}

private fun DrawScope.drawVortex(form: Float, pulse: Float) {
    val c = Offset(size.width / 2f, size.height / 2f - 20.dp.toPx())
    val radius = 110.dp.toPx()

    // ambient glow
    val glowR = radius * 3f
    drawCircle(Brush.radialGradient(listOf(Ice.copy(alpha = 0.22f * form), Color.Transparent), c, glowR), glowR, c)

    // liquid droplets swirling inwards
    val count = 20
    for (i in 0 until count) {
        val seed = i / count.toFloat()
        val t = ((form - seed * 0.45f) / 0.55f).coerceIn(0f, 1f)
        if (t <= 0f || t >= 1f) continue
        val e = t * t * (3f - 2f * t)
        val ang = i * 2.39996f + e * 2.2f * PI_F
        val dist = lerp(size.maxDimension * 0.6f, 0f, e)
        val pos = Offset(c.x + cos(ang) * dist, c.y + sin(ang) * dist)
        val r = lerp(11.dp.toPx(), 3.dp.toPx(), e)
        val g = r * 3f
        drawCircle(
            Brush.radialGradient(listOf(Color.White.copy(0.95f), Ice.copy(0.35f), Color.Transparent), pos, g),
            g,
            pos,
        )
    }

    // 3 spiral arms growing from the centre
    val growth = ((form - 0.15f) / 0.85f).coerceIn(0f, 1f)
    if (growth > 0f) {
        val spin = (1f - growth) * -PI_F
        val arms = 3
        val steps = 90
        val path = Path()
        for (arm in 0 until arms) {
            val off = arm * (2f * PI_F / arms) + spin
            for (k in 0..steps) {
                val u = growth * k / steps
                val a = off + u * 2.4f * PI_F
                val x = c.x + cos(a) * radius * u
                val y = c.y + sin(a) * radius * u
                if (k == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
        }
        val layers = listOf(
            34f to Ice.copy(alpha = 0.05f),
            22f to Ice.copy(alpha = 0.09f),
            12f to Ice.copy(alpha = 0.18f),
            5f to Color.White.copy(alpha = 0.95f),
        )
        for ((widthDp, color) in layers) {
            drawPath(path, color, style = Stroke(widthDp.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        // bright core
        val coreR = 26.dp.toPx() * growth
        if (coreR > 0f) {
            drawCircle(Brush.radialGradient(listOf(Color.White, Color.Transparent), c, coreR), coreR, c)
        }
    }

    // pulse ring once the emblem is formed
    if (pulse > 0f && pulse < 1f) {
        drawCircle(
            Ice.copy(alpha = 0.5f * (1f - pulse)),
            radius * (0.4f + 1.8f * pulse),
            c,
            style = Stroke(2.dp.toPx()),
        )
    }
}
