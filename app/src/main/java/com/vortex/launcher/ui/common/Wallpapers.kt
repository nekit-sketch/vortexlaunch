package com.vortex.launcher.ui.common

import android.net.Uri
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

data class FlowPalette(val base: Color, val a: Color, val b: Color, val c: Color)

enum class StockWallpaper(val title: String, val palette: FlowPalette) {
    Aurora("Aurora", FlowPalette(Color(0xFF050B1F), Color(0xFF2B59FF), Color(0xFF7A3CFF), Color(0xFF14C8E8))),
    Ember("Ember", FlowPalette(Color(0xFF14040A), Color(0xFFE0243C), Color(0xFFFF7A1A), Color(0xFF7A1030))),
    DeepOcean("Deep Ocean", FlowPalette(Color(0xFF020B14), Color(0xFF0369A1), Color(0xFF22D3EE), Color(0xFF1E3A8A))),
    Dusk("Dusk", FlowPalette(Color(0xFF0E0618), Color(0xFFC026D3), Color(0xFF6366F1), Color(0xFFF43F5E))),
    Emerald("Emerald", FlowPalette(Color(0xFF031512), Color(0xFF10B981), Color(0xFF0EA5E9), Color(0xFF065F46))),
    Graphite("Graphite", FlowPalette(Color(0xFF08090C), Color(0xFF5A6275), Color(0xFF8892A6), Color(0xFF2A2F3B))),
}

sealed interface Wallpaper {
    data class Stock(val style: StockWallpaper) : Wallpaper
    data class Custom(val uri: Uri) : Wallpaper

    fun toStorage(): String = when (this) {
        is Stock -> "stock:${style.name}"
        is Custom -> "custom:$uri"
    }

    companion object {
        val Default: Wallpaper = Stock(StockWallpaper.Aurora)

        /** Use for DataStore. */
        fun fromStorage(value: String?): Wallpaper = when {
            value == null -> Default
            value.startsWith("stock:") ->
                StockWallpaper.entries.firstOrNull { it.name == value.removePrefix("stock:") }
                    ?.let(::Stock) ?: Default
            value.startsWith("custom:") -> Custom(Uri.parse(value.removePrefix("custom:")))
            else -> Default
        }
    }
}

val WallpaperSaver = Saver<Wallpaper, String>(
    save = { it.toStorage() },
    restore = { Wallpaper.fromStorage(it) },
)

/**
 * Wallpaper layer. Stock = animated flowing gradient, Custom = Coil image (down-sampled, cached).
 * Pass animated=false on secondary screens (All Apps, previews) to save GPU time.
 */
@Composable
fun WallpaperBackground(
    wallpaper: Wallpaper,
    modifier: Modifier = Modifier,
    animated: Boolean = true,
    scrim: Float = 0f,
) {
    Box(modifier.fillMaxSize()) {
        Crossfade(wallpaper, animationSpec = tween(500), label = "wallpaper") { wp ->
            when (wp) {
                is Wallpaper.Stock -> FlowingGradientBackground({ wp.style.palette }, Modifier, animated)
                is Wallpaper.Custom -> Box(Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = rememberImageRequest(wp.uri, 1280, 720, crossfadeMs = 300),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    // readability gradient for clock / dock on arbitrary photos
                    Box(
                        Modifier.fillMaxSize().background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(0.35f), Color.Transparent, Color.Black.copy(0.5f))
                            )
                        )
                    )
                }
            }
        }
        if (scrim > 0f) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = scrim)))
    }
}

private const val TAU = (2.0 * PI).toFloat()

/**
 * PS5-style flowing background: 3 drifting radial blobs + 3 slow sine waves.
 * The phase State is read ONLY inside drawBehind => zero recompositions while animating.
 * [palette] is a lambda so callers can animate colors (read in draw as well).
 */
@Composable
fun FlowingGradientBackground(
    palette: () -> FlowPalette,
    modifier: Modifier = Modifier,
    animated: Boolean = true,
) {
    if (animated) {
        val transition = rememberInfiniteTransition(label = "flow")
        val phase = transition.animateFloat(
            initialValue = 0f,
            targetValue = TAU,
            animationSpec = infiniteRepeatable(tween(32_000, easing = LinearEasing)),
            label = "phase",
        )
        Box(modifier.fillMaxSize().drawBehind { drawFlow(palette(), phase.value) })
    } else {
        Box(modifier.fillMaxSize().drawBehind { drawFlow(palette(), 0.9f) })
    }
}

private fun DrawScope.drawFlow(p: FlowPalette, phase: Float) {
    val w = size.width
    val h = size.height
    drawRect(p.base)

    fun blob(color: Color, cx: Float, cy: Float, r: Float, alpha: Float) {
        val center = Offset(cx, cy)
        drawCircle(Brush.radialGradient(listOf(color.copy(alpha = alpha), Color.Transparent), center, r), r, center)
    }
    // all terms are integer multiples of phase -> seamless loop
    blob(p.a, w * (0.22f + 0.16f * sin(phase)), h * (0.30f + 0.14f * cos(phase)), w * 0.60f, 0.65f)
    blob(p.b, w * (0.78f + 0.14f * cos(phase)), h * (0.25f + 0.18f * sin(2 * phase + 1f)), w * 0.55f, 0.55f)
    blob(p.c, w * (0.50f + 0.22f * sin(2 * phase)), h * (0.85f + 0.10f * cos(phase)), w * 0.65f, 0.50f)

    wave(p.a, h * 0.62f, h * 0.07f, 1.0f, phase, 0.22f)
    wave(p.b, h * 0.74f, h * 0.06f, 2.0f, -phase, 0.18f)
    wave(p.c, h * 0.86f, h * 0.05f, 1.0f, 2 * phase, 0.16f)

    drawRect(
        Brush.radialGradient(
            listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f)),
            center = Offset(w / 2f, h / 2f),
            radius = max(w, h) * 0.75f,
        )
    )
}

private fun DrawScope.wave(color: Color, baseY: Float, amp: Float, cycles: Float, phase: Float, alpha: Float) {
    val w = size.width
    val h = size.height
    val steps = 32
    val path = Path().apply {
        moveTo(0f, h)
        for (i in 0..steps) {
            val t = i / steps.toFloat() * TAU * cycles
            lineTo(w * i / steps, baseY + amp * sin(t + phase) + amp * 0.4f * sin(2 * t - 2 * phase))
        }
        lineTo(w, h)
        close()
    }
    drawPath(
        path,
        Brush.verticalGradient(
            listOf(color.copy(alpha = alpha), Color.Transparent),
            startY = baseY - amp,
            endY = h,
        ),
    )
}
