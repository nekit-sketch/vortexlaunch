package com.vortex.launcher.ui.apps

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.vortex.launcher.ui.common.AppEntry
import com.vortex.launcher.ui.common.AppImage
import com.vortex.launcher.ui.common.Glass
import com.vortex.launcher.ui.common.GlassIconButton
import com.vortex.launcher.ui.common.GlassStrength
import com.vortex.launcher.ui.common.GlassSurface
import com.vortex.launcher.ui.common.Wallpaper
import com.vortex.launcher.ui.common.WallpaperBackground
import kotlinx.coroutines.delay

/**
 * All Apps: classic Android-TV style 16:9 tiles in a LazyVerticalGrid.
 * Banners (or icons as fallback) are loaded and cached by Coil; every tile has a liquid-glass border.
 * Wrap the call site in AnimatedVisibility for the exit animation.
 */
@Composable
fun AllAppsScreen(
    apps: List<AppEntry>,
    wallpaper: Wallpaper,
    onLaunchApp: (AppEntry) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)

    val enter = remember { Animatable(0f) }
    val firstTile = remember { FocusRequester() }
    LaunchedEffect(Unit) { enter.animateTo(1f, spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)) }
    LaunchedEffect(apps.isNotEmpty()) { delay(150); runCatching { firstTile.requestFocus() } }

    Box(
        modifier
            .fillMaxSize()
            .graphicsLayer {
                val s = 0.95f + 0.05f * enter.value
                scaleX = s
                scaleY = s
                alpha = enter.value
            }
    ) {
        // static (non-animated) wallpaper + dark scrim: cheaper and keeps tiles readable
        WallpaperBackground(wallpaper, animated = false, scrim = 0.55f)

        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 56.dp, end = 56.dp, top = 40.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, onBack, size = 60.dp)
                Column {
                    Text("All Apps", color = Glass.Text, fontSize = 36.sp, fontWeight = FontWeight.Light)
                    Text("${apps.size} apps", color = Glass.TextDim, fontSize = 16.sp)
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 220.dp),
                contentPadding = PaddingValues(horizontal = 56.dp, vertical = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(26.dp),
                verticalArrangement = Arrangement.spacedBy(26.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                itemsIndexed(apps, key = { _, app -> app.packageName }) { index, app ->
                    AppTile(
                        app = app,
                        onClick = { onLaunchApp(app) },
                        modifier = if (index == 0) Modifier.focusRequester(firstTile) else Modifier,
                    )
                }
            }
        }
    }
}

@Composable
private fun AppTile(app: AppEntry, onClick: () -> Unit, modifier: Modifier = Modifier) {
    GlassSurface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.fillMaxWidth().aspectRatio(16f / 9f),
        strength = GlassStrength.Light,
        focusScale = 1.1f,
    ) { focus ->
        // TV banner (16:9); icon fallback is centred thanks to ContentScale.Fit
        AsyncImage(
            model = AppImage(app.packageName, banner = true),
            contentDescription = app.label,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().padding(4.dp),
        )
        // label appears on focus
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(52.dp)
                .graphicsLayer { alpha = focus().coerceIn(0f, 1f) }
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.BottomStart,
        ) {
            Text(app.label, color = Glass.Text, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
