package com.vortex.launcher.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Tune
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.vortex.launcher.ui.common.AppEntry
import com.vortex.launcher.ui.common.AppImage
import com.vortex.launcher.ui.common.ClockBlock
import com.vortex.launcher.ui.common.FocusAnim
import com.vortex.launcher.ui.common.Glass
import com.vortex.launcher.ui.common.GlassIconButton
import com.vortex.launcher.ui.common.GlassStrength
import com.vortex.launcher.ui.common.GlassSurface
import com.vortex.launcher.ui.common.Wallpaper
import com.vortex.launcher.ui.common.WallpaperBackground
import com.vortex.launcher.ui.common.WeatherUi
import com.vortex.launcher.ui.common.liquidGlass
import kotlinx.coroutines.delay

/**
 * Classic (Apple TV style) home: clock + weather on top, big rounded-rectangle liquid-glass dock
 * at the bottom (NOT a pill), with a large rounded-square "All Apps" button.
 */
@Composable
fun AppleHomeScreen(
    wallpaper: Wallpaper,
    pinnedApps: List<AppEntry>,
    weather: WeatherUi?,
    onLaunchApp: (AppEntry) -> Unit,
    onOpenAllApps: () -> Unit,
    onOpenControlCenter: () -> Unit,
    onSwitchToGaming: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundAnimated: Boolean = true,
) {
    val enter = remember { Animatable(0f) }
    val allAppsFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { enter.animateTo(1f, spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessLow)) }
    LaunchedEffect(Unit) { delay(120); runCatching { allAppsFocus.requestFocus() } }

    Box(modifier.fillMaxSize()) {
        WallpaperBackground(wallpaper, animated = backgroundAnimated, scrim = 0.1f)

        // top bar: clock left, weather + quick buttons right
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 56.dp, vertical = 40.dp)
                .graphicsLayer {
                    alpha = enter.value
                    translationY = -24.dp.toPx() * (1f - enter.value)
                },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            ClockBlock()
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                if (weather != null) WeatherChip(weather)
                GlassIconButton(Icons.Rounded.SportsEsports, onSwitchToGaming)
                GlassIconButton(Icons.Rounded.Tune, onOpenControlCenter)
            }
        }

        Dock(
            apps = pinnedApps,
            onLaunch = onLaunchApp,
            onAllApps = onOpenAllApps,
            allAppsFocus = allAppsFocus,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 44.dp)
                .graphicsLayer {
                    alpha = enter.value
                    translationY = 90.dp.toPx() * (1f - enter.value)
                },
        )
    }
}

@Composable
private fun WeatherChip(weather: WeatherUi) {
    Row(
        Modifier
            .liquidGlass(RoundedCornerShape(20.dp), GlassStrength.Regular)
            .padding(horizontal = 22.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(weather.icon, null, Modifier.size(30.dp), Glass.Icon)
        Column {
            Text(weather.temperature, color = Glass.Text, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
            Text(weather.condition, color = Glass.TextDim, fontSize = 14.sp)
        }
    }
}

@Composable
private fun Dock(
    apps: List<AppEntry>,
    onLaunch: (AppEntry) -> Unit,
    onAllApps: () -> Unit,
    allAppsFocus: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(44.dp)
    Row(
        modifier
            .fillMaxWidth(0.84f)
            .background(Color.Black.copy(alpha = 0.22f), shape) // contrast base so glass reads on bright wallpapers
            .liquidGlass(shape, GlassStrength.Heavy)
            .padding(horizontal = 32.dp, vertical = 26.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // "All Apps": large rounded square
        GlassSurface(
            onClick = onAllApps,
            shape = RoundedCornerShape(30.dp),
            modifier = Modifier.size(112.dp).focusRequester(allAppsFocus),
            strength = GlassStrength.Heavy,
            focusScale = 1.1f,
        ) {
            Icon(Icons.Rounded.GridView, "All apps", Modifier.align(Alignment.Center).size(52.dp), Glass.Icon)
        }

        Spacer(Modifier.width(28.dp))
        Box(
            Modifier
                .width(1.dp)
                .height(80.dp)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.White.copy(0.35f), Color.Transparent)))
        )

        LazyRow(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items(apps, key = { it.packageName }) { app -> DockItem(app, onClick = { onLaunch(app) }) }
        }
    }
}

@Composable
private fun DockItem(app: AppEntry, onClick: () -> Unit) {
    val focus: FocusAnim = remember { Animatable(0f) }
    Column(Modifier.width(104.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        GlassSurface(
            onClick = onClick,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.size(92.dp),
            strength = GlassStrength.Light,
            focusScale = 1.14f,
            focusProgress = focus,
        ) {
            AsyncImage(
                model = AppImage(app.packageName),
                contentDescription = app.label,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(16.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            app.label,
            color = Glass.Text,
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.graphicsLayer { alpha = focus.value.coerceIn(0f, 1f) },
        )
    }
}
