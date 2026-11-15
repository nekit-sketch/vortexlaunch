package com.vortex.launcher.ui.control

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.vortex.launcher.ui.common.Glass
import com.vortex.launcher.ui.common.GlassIconButton
import com.vortex.launcher.ui.common.GlassStrength
import com.vortex.launcher.ui.common.GlassSurface
import com.vortex.launcher.ui.common.liquidGlass
import com.vortex.launcher.ui.common.rememberTimeText
import kotlinx.coroutines.delay

data class ControlTileModel(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val active: Boolean = false,
    val subtitle: String? = null,
)

/** Example tile set; wire the booleans to your ViewModel / system services. */
fun defaultControlTiles(wifiOn: Boolean, bluetoothOn: Boolean, nightOn: Boolean): List<ControlTileModel> = listOf(
    ControlTileModel("wifi", "Wi-Fi", Icons.Rounded.Wifi, wifiOn, if (wifiOn) "On" else "Off"),
    ControlTileModel("bluetooth", "Bluetooth", Icons.Rounded.Bluetooth, bluetoothOn, if (bluetoothOn) "On" else "Off"),
    ControlTileModel("night", "Night mode", Icons.Rounded.NightsStay, nightOn, if (nightOn) "On" else "Off"),
    ControlTileModel("cast", "Screen cast", Icons.Rounded.Cast),
    ControlTileModel("gaming", "Gaming mode", Icons.Rounded.SportsEsports),
    ControlTileModel("settings", "Settings", Icons.Rounded.Settings),
    ControlTileModel("power", "Power", Icons.Rounded.PowerSettingsNew),
)

/**
 * Right-side sliding drawer (~36% of the width) instead of a full-screen overlay.
 * Compose it on top of the current screen; it handles its own enter / exit animation and Back.
 */
@Composable
fun ControlCenterPanel(
    visible: Boolean,
    tiles: List<ControlTileModel>,
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    onTileClick: (ControlTileModel) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = visible, onBack = onDismiss)

    Box(modifier.fillMaxSize()) {
        // soft dim on the right side only (cheap gradient, no blur)
        AnimatedVisibility(visible, enter = fadeIn(tween(250)), exit = fadeOut(tween(200))) {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.horizontalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)))
                )
            )
        }
        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().fillMaxWidth(0.36f),
            enter = slideInHorizontally(spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)) { it } +
                fadeIn(tween(200)),
            exit = slideOutHorizontally(tween(220)) { it } + fadeOut(tween(180)),
        ) {
            Panel(tiles, volume, onVolumeChange, onTileClick, onDismiss)
        }
    }
}

@Composable
private fun Panel(
    tiles: List<ControlTileModel>,
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    onTileClick: (ControlTileModel) -> Unit,
    onDismiss: () -> Unit,
) {
    val shape = RoundedCornerShape(36.dp)
    val firstTile = remember { FocusRequester() }
    LaunchedEffect(Unit) { delay(180); runCatching { firstTile.requestFocus() } }

    Column(
        Modifier
            .fillMaxSize()
            .padding(20.dp)
            .background(Color.Black.copy(alpha = 0.3f), shape)
            .liquidGlass(shape, GlassStrength.Heavy)
            .padding(24.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Control Center",
                color = Glass.Text,
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            GlassIconButton(Icons.Rounded.Close, onDismiss, size = 48.dp)
        }
        Spacer(Modifier.height(20.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            itemsIndexed(tiles, key = { _, t -> t.id }) { index, tile ->
                ControlTile(
                    tile = tile,
                    onClick = { onTileClick(tile) },
                    modifier = if (index == 0) Modifier.focusRequester(firstTile) else Modifier,
                )
            }
            item(key = "volume", span = { GridItemSpan(maxLineSpan) }) {
                VolumeSlider(volume, onVolumeChange)
            }
        }
    }
}

@Composable
private fun ControlTile(tile: ControlTileModel, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val chipColor by animateColorAsState(
        if (tile.active) Color.White.copy(alpha = 0.92f) else Color.White.copy(alpha = 0.14f),
        tween(250),
        label = "chip",
    )
    val iconColor by animateColorAsState(
        if (tile.active) Color(0xFF14161A) else Glass.Icon,
        tween(250),
        label = "icon",
    )

    GlassSurface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        modifier = modifier.fillMaxWidth().aspectRatio(1.2f),
        strength = if (tile.active) GlassStrength.Heavy else GlassStrength.Light,
        focusScale = 1.06f,
    ) {
        Column(
            Modifier.fillMaxSize().padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(Modifier.size(46.dp).background(chipColor, CircleShape), contentAlignment = Alignment.Center) {
                Icon(tile.icon, null, Modifier.size(26.dp), iconColor)
            }
            Column {
                Text(tile.label, color = Glass.Text, fontSize = 19.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                if (tile.subtitle != null) Text(tile.subtitle, color = Glass.TextDim, fontSize = 14.sp, maxLines = 1)
            }
        }
    }
}

/** D-pad slider: focus it, then LEFT / RIGHT change the value in 5% steps. */
@Composable
private fun VolumeSlider(volume: Float, onVolumeChange: (Float) -> Unit) {
    val animated = animateFloatAsState(volume.coerceIn(0f, 1f), spring(stiffness = Spring.StiffnessMedium), label = "vol")

    GlassSurface(
        onClick = {},
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .onKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (e.key) {
                    Key.DirectionLeft -> { onVolumeChange((volume - 0.05f).coerceAtLeast(0f)); true }
                    Key.DirectionRight -> { onVolumeChange((volume + 0.05f).coerceAtMost(1f)); true }
                    else -> false
                }
            },
        strength = GlassStrength.Light,
        focusScale = 1.03f,
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 22.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.VolumeUp, null, Modifier.size(28.dp), Glass.Icon)
            Box(
                Modifier
                    .weight(1f)
                    .height(10.dp)
                    .drawBehind {
                        val r = CornerRadius(size.height / 2f)
                        drawRoundRect(Color.White.copy(alpha = 0.2f), cornerRadius = r)
                        drawRoundRect(Color.White, size = Size(size.width * animated.value, size.height), cornerRadius = r)
                    }
            )
        }
    }
}

/**
 * Small liquid-glass "island" for the top-right corner of Gaming mode.
 * Shows the time and status icons; click opens the Control Center panel.
 */
@Composable
fun ControlCenterIsland(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val time = rememberTimeText()
    GlassSurface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        modifier = modifier,
        strength = GlassStrength.Heavy,
        focusScale = 1.1f,
    ) {
        Row(
            Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(time, color = Glass.Text, fontSize = 20.sp, fontWeight = FontWeight.Medium)
            Box(Modifier.width(1.dp).height(18.dp).background(Color.White.copy(alpha = 0.3f)))
            Icon(Icons.Rounded.Wifi, null, Modifier.size(22.dp), Glass.Icon)
            Icon(Icons.Rounded.Bluetooth, null, Modifier.size(22.dp), Glass.Icon)
            Icon(Icons.Rounded.Tune, null, Modifier.size(22.dp), Glass.Icon)
        }
    }
}
