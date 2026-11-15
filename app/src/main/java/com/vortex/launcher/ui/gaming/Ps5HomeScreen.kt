package com.vortex.launcher.ui.gaming

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.vortex.launcher.ui.common.AppEntry
import com.vortex.launcher.ui.common.AppImage
import com.vortex.launcher.ui.common.FlowPalette
import com.vortex.launcher.ui.common.FlowingGradientBackground
import com.vortex.launcher.ui.common.Glass
import com.vortex.launcher.ui.common.GlassButton
import com.vortex.launcher.ui.common.GlassIconButton
import com.vortex.launcher.ui.common.GlassStrength
import com.vortex.launcher.ui.common.GlassSurface
import com.vortex.launcher.ui.common.StockWallpaper
import com.vortex.launcher.ui.common.liquidGlass
import com.vortex.launcher.ui.control.ControlCenterIsland
import kotlinx.coroutines.delay

/**
 * Gaming desktop (PS5 style).
 *  - only the "Games" tab (Media removed)
 *  - NO achievements block
 *  - NO static wallpaper: animated flowing gradient whose colours morph to the focused game
 *  - liquid-glass mini-island for the Control Center in the top-right corner
 */
@Composable
fun Ps5HomeScreen(
    games: List<AppEntry>,
    onLaunchGame: (AppEntry) -> Unit,
    onOpenControlCenter: () -> Unit,
    onSwitchToClassic: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onSwitchToClassic)

    var focusedPkg by rememberSaveable { mutableStateOf(games.firstOrNull()?.packageName) }
    val focused = games.firstOrNull { it.packageName == focusedPkg } ?: games.firstOrNull()

    // colours morph smoothly between games; the State values are read only while drawing
    val target = remember(focused?.packageName) { paletteFor(focused?.packageName) }
    val spec = tween<Color>(900)
    val base = animateColorAsState(target.base, spec, label = "base")
    val a = animateColorAsState(target.a, spec, label = "a")
    val b = animateColorAsState(target.b, spec, label = "b")
    val c = animateColorAsState(target.c, spec, label = "c")

    val enter = remember { Animatable(0f) }
    val firstTile = remember { FocusRequester() }
    LaunchedEffect(Unit) { enter.animateTo(1f, spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessLow)) }
    LaunchedEffect(games.isNotEmpty()) { delay(150); runCatching { firstTile.requestFocus() } }

    Box(modifier.fillMaxSize()) {
        FlowingGradientBackground(palette = { FlowPalette(base.value, a.value, b.value, c.value) })
        // legibility gradient (top + bottom)
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color.Black.copy(0.25f), Color.Transparent, Color.Black.copy(0.55f)))
            )
        )

        // top bar: Games tab (only tab) + home on the left, island on the right
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 56.dp, vertical = 36.dp)
                .graphicsLayer {
                    alpha = enter.value
                    translationY = -20.dp.toPx() * (1f - enter.value)
                },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                GlassIconButton(Icons.Rounded.Home, onSwitchToClassic, size = 56.dp)
                GamesTab()
            }
            ControlCenterIsland(onClick = onOpenControlCenter)
        }

        if (focused == null) {
            Text(
                "No games installed",
                color = Glass.TextDim,
                fontSize = 26.sp,
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            // hero: title + Play (no achievements)
            Column(
                Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 72.dp, bottom = 120.dp)
                    .fillMaxWidth(0.45f)
                    .graphicsLayer {
                        alpha = enter.value
                        translationX = -40.dp.toPx() * (1f - enter.value)
                    },
            ) {
                AnimatedContent(
                    targetState = focused,
                    transitionSpec = { fadeIn(tween(350)) togetherWith fadeOut(tween(200)) },
                    label = "title",
                ) { game ->
                    Text(
                        game.label,
                        color = Color.White,
                        fontSize = 60.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(28.dp))
                GlassButton(
                    "Play",
                    onClick = { onLaunchGame(focused) },
                    icon = Icons.Rounded.PlayArrow,
                    strength = GlassStrength.Heavy,
                )
            }

            ArtCard(
                pkg = focused.packageName,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 72.dp, bottom = 120.dp)
                    .graphicsLayer {
                        alpha = enter.value
                        translationX = 60.dp.toPx() * (1f - enter.value)
                    },
            )

            LazyRow(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 44.dp)
                    .graphicsLayer {
                        alpha = enter.value
                        translationY = 80.dp.toPx() * (1f - enter.value)
                    },
                contentPadding = PaddingValues(horizontal = 72.dp, vertical = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                itemsIndexed(games, key = { _, g -> g.packageName }) { index, game ->
                    GameTile(
                        game = game,
                        onClick = { onLaunchGame(game) },
                        onFocused = { focusedPkg = game.packageName },
                        modifier = if (index == 0) Modifier.focusRequester(firstTile) else Modifier,
                    )
                }
            }
        }
    }
}

@Composable
private fun GamesTab() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.SportsEsports, null, Modifier.size(28.dp), Color.White)
            Text("Games", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(6.dp))
        Box(Modifier.width(96.dp).height(3.dp).background(Color.White, RoundedCornerShape(2.dp)))
    }
}

/** The focused game's own art in a floating glass card. Replaces the old static wallpaper + achievements. */
@Composable
private fun ArtCard(pkg: String, modifier: Modifier = Modifier) {
    val float = rememberInfiniteTransition(label = "float").animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "floatY",
    )
    val shape = RoundedCornerShape(30.dp)
    Box(
        modifier
            .width(440.dp)
            .aspectRatio(16f / 9f)
            .graphicsLayer { translationY = float.value * 6.dp.toPx() }
            .liquidGlass(shape, GlassStrength.Heavy)
            .padding(6.dp)
            .clip(RoundedCornerShape(24.dp)),
    ) {
        Crossfade(pkg, animationSpec = tween(400), label = "art") { p ->
            AsyncImage(
                model = AppImage(p, banner = true),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun GameTile(
    game: AppEntry,
    onClick: () -> Unit,
    onFocused: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassSurface(
        onClick = onClick,
        shape = RoundedCornerShape(26.dp),
        modifier = modifier.size(152.dp),
        strength = GlassStrength.Light,
        focusScale = 1.16f,
        onFocused = onFocused,
    ) {
        AsyncImage(
            model = AppImage(game.packageName),
            contentDescription = game.label,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().padding(22.dp),
        )
    }
}

/** Deterministic palette per game (cheap; no Palette-library bitmap analysis on a weak SoC). */
private fun paletteFor(pkg: String?): FlowPalette {
    val styles = StockWallpaper.entries
    val index = ((pkg?.hashCode() ?: 0) and Int.MAX_VALUE) % styles.size
    return styles[index].palette
}
