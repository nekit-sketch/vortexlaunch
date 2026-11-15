package com.vortex.launcher.ui.onboarding

import android.net.Uri
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.PhotoCamera
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.vortex.launcher.ui.common.AvatarImage
import com.vortex.launcher.ui.common.FlowingGradientBackground
import com.vortex.launcher.ui.common.Glass
import com.vortex.launcher.ui.common.GlassButton
import com.vortex.launcher.ui.common.GlassStrength
import com.vortex.launcher.ui.common.GlassSurface
import com.vortex.launcher.ui.common.StockWallpaper
import com.vortex.launcher.ui.common.Wallpaper
import com.vortex.launcher.ui.common.WallpaperBackground
import com.vortex.launcher.ui.common.WallpaperSaver
import com.vortex.launcher.ui.common.liquidGlass
import com.vortex.launcher.ui.common.rememberImagePicker
import com.vortex.launcher.ui.common.rememberImageRequest
import kotlinx.coroutines.delay

data class OnboardingResult(
    val wallpaper: Wallpaper,
    val profileName: String,
    val avatarUri: Uri?,
)

private const val ROUTE_WELCOME = "welcome"
private const val ROUTE_WALLPAPER = "wallpaper"
private const val ROUTE_PROFILE = "profile"

/**
 * Onboarding: Welcome -> Wallpaper -> Profile. The Wi-Fi step is GONE.
 * The chosen wallpaper is rendered live behind every step.
 * Persist the result (DataStore / Room) inside [onFinished].
 */
@Composable
fun OnboardingNavHost(
    onFinished: (OnboardingResult) -> Unit,
    modifier: Modifier = Modifier,
) {
    val nav = rememberNavController()
    var wallpaper by rememberSaveable(stateSaver = WallpaperSaver) { mutableStateOf<Wallpaper>(Wallpaper.Default) }
    var customUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var avatarUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var name by rememberSaveable { mutableStateOf("") }

    Box(modifier.fillMaxSize()) {
        WallpaperBackground(wallpaper, scrim = 0.2f)

        NavHost(
            navController = nav,
            startDestination = ROUTE_WELCOME,
            modifier = Modifier.fillMaxSize(),
            enterTransition = { fadeIn(tween(450)) + scaleIn(tween(450), initialScale = 0.94f) },
            exitTransition = { fadeOut(tween(250)) + scaleOut(tween(250), targetScale = 1.04f) },
            popEnterTransition = { fadeIn(tween(450)) + scaleIn(tween(450), initialScale = 1.04f) },
            popExitTransition = { fadeOut(tween(250)) + scaleOut(tween(250), targetScale = 0.94f) },
        ) {
            composable(ROUTE_WELCOME) {
                WelcomeStep(onNext = { nav.navigate(ROUTE_WALLPAPER) })
            }
            composable(ROUTE_WALLPAPER) {
                WallpaperStep(
                    selected = wallpaper,
                    customUri = customUri,
                    onSelect = { wallpaper = it },
                    onCustomPicked = { uri ->
                        customUri = uri
                        wallpaper = Wallpaper.Custom(uri)
                    },
                    onBack = { nav.popBackStack() },
                    onNext = { nav.navigate(ROUTE_PROFILE) },
                )
            }
            composable(ROUTE_PROFILE) {
                ProfileStep(
                    name = name,
                    onNameChange = { name = it },
                    avatarUri = avatarUri,
                    onAvatarPicked = { avatarUri = it },
                    onBack = { nav.popBackStack() },
                    onFinish = {
                        onFinished(OnboardingResult(wallpaper, name.trim().ifEmpty { "Guest" }, avatarUri))
                    },
                )
            }
        }
    }
}

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { delay(120); runCatching { focus.requestFocus() } }

    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Welcome to Vortex", color = Glass.Text, fontSize = 64.sp, fontWeight = FontWeight.Light)
        Spacer(Modifier.height(12.dp))
        Text("Let's set up your launcher in a minute.", color = Glass.TextDim, fontSize = 24.sp)
        Spacer(Modifier.height(48.dp))
        GlassButton(
            text = "Get started",
            onClick = onNext,
            icon = Icons.AutoMirrored.Rounded.ArrowForward,
            strength = GlassStrength.Heavy,
            modifier = Modifier.focusRequester(focus),
        )
    }
}

@Composable
private fun WallpaperStep(
    selected: Wallpaper,
    customUri: Uri?,
    onSelect: (Wallpaper) -> Unit,
    onCustomPicked: (Uri) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    val first = remember { FocusRequester() }
    var pickerMissing by remember { mutableStateOf(false) }
    val pick = rememberImagePicker(onPicked = onCustomPicked, onUnavailable = { pickerMissing = true })
    LaunchedEffect(Unit) { delay(150); runCatching { first.requestFocus() } }

    Column(
        Modifier.fillMaxSize().padding(vertical = 56.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.padding(horizontal = 72.dp)) {
            Text("Choose your wallpaper", color = Glass.Text, fontSize = 44.sp, fontWeight = FontWeight.Light)
            Spacer(Modifier.height(8.dp))
            Text("Pick a flowing background or upload your own.", color = Glass.TextDim, fontSize = 22.sp)
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 72.dp, vertical = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            item(key = "upload") {
                WallpaperCard(
                    title = "Upload from gallery",
                    selected = false,
                    onClick = pick,
                    surfaceModifier = Modifier.focusRequester(first),
                ) {
                    Icon(
                        Icons.Rounded.AddPhotoAlternate, null,
                        Modifier.align(Alignment.Center).size(48.dp), Glass.Icon,
                    )
                }
            }
            if (customUri != null) {
                item(key = "custom") {
                    WallpaperCard("Your photo", selected is Wallpaper.Custom, { onSelect(Wallpaper.Custom(customUri)) }) {
                        AsyncImage(
                            model = rememberImageRequest(customUri, 400, 225),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
            items(StockWallpaper.entries, key = { it.name }) { style ->
                WallpaperCard(
                    title = style.title,
                    selected = (selected as? Wallpaper.Stock)?.style == style,
                    onClick = { onSelect(Wallpaper.Stock(style)) },
                ) {
                    // static frame in previews: only the full-screen background animates
                    FlowingGradientBackground({ style.palette }, Modifier, animated = false)
                }
            }
        }

        Row(
            Modifier.padding(horizontal = 72.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassButton("Back", onBack, icon = Icons.AutoMirrored.Rounded.ArrowBack)
            GlassButton("Continue", onNext, icon = Icons.AutoMirrored.Rounded.ArrowForward, strength = GlassStrength.Heavy)
            if (pickerMissing) {
                Text("No gallery app found on this device.", color = Glass.TextDim, fontSize = 18.sp)
            }
        }
    }
}

@Composable
private fun WallpaperCard(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    surfaceModifier: Modifier = Modifier,
    preview: @Composable BoxScope.() -> Unit,
) {
    Column(Modifier.width(280.dp)) {
        GlassSurface(
            onClick = onClick,
            shape = RoundedCornerShape(24.dp),
            modifier = surfaceModifier.fillMaxWidth().aspectRatio(16f / 9f),
            strength = GlassStrength.Light,
            focusScale = 1.06f,
        ) {
            Box(Modifier.fillMaxSize().padding(4.dp).clip(RoundedCornerShape(20.dp))) { preview() }
            if (selected) {
                Icon(
                    Icons.Rounded.CheckCircle, null,
                    Modifier.align(Alignment.TopEnd).padding(10.dp).size(28.dp), Color.White,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(title, color = Glass.Text, fontSize = 20.sp, maxLines = 1)
    }
}

@Composable
private fun ProfileStep(
    name: String,
    onNameChange: (String) -> Unit,
    avatarUri: Uri?,
    onAvatarPicked: (Uri) -> Unit,
    onBack: () -> Unit,
    onFinish: () -> Unit,
) {
    val first = remember { FocusRequester() }
    var pickerMissing by remember { mutableStateOf(false) }
    val pick = rememberImagePicker(onPicked = onAvatarPicked, onUnavailable = { pickerMissing = true })
    LaunchedEffect(Unit) { delay(150); runCatching { first.requestFocus() } }

    Row(
        Modifier.fillMaxSize().padding(horizontal = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(72.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassSurface(
            onClick = pick,
            shape = CircleShape,
            modifier = Modifier.size(240.dp).focusRequester(first),
            strength = GlassStrength.Heavy,
            focusScale = 1.06f,
        ) { focus ->
            AvatarImage(avatarUri, Modifier.fillMaxSize().padding(8.dp))
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 22.dp)
                    .graphicsLayer { alpha = focus() }
                    .liquidGlass(RoundedCornerShape(50), GlassStrength.Heavy)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.PhotoCamera, null, Modifier.size(20.dp), Glass.Icon)
                Text("Upload photo", color = Glass.Text, fontSize = 16.sp)
            }
        }

        Column(Modifier.weight(1f)) {
            Text("Create your profile", color = Glass.Text, fontSize = 44.sp, fontWeight = FontWeight.Light)
            Spacer(Modifier.height(8.dp))
            Text(
                if (pickerMissing) "No gallery app found. A default avatar will be used."
                else "Add a photo or keep the default avatar.",
                color = Glass.TextDim,
                fontSize = 22.sp,
            )
            Spacer(Modifier.height(32.dp))
            NameField(name) { if (it.length <= 20) onNameChange(it) }
            Spacer(Modifier.height(32.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                GlassButton("Back", onBack, icon = Icons.AutoMirrored.Rounded.ArrowBack)
                GlassButton("Finish", onFinish, icon = Icons.Rounded.Done, strength = GlassStrength.Heavy)
            }
        }
    }
}

@Composable
private fun NameField(value: String, onValueChange: (String) -> Unit) {
    val focusManager = LocalFocusManager.current
    var focused by remember { mutableStateOf(false) }
    Box(
        Modifier
            .fillMaxWidth()
            .liquidGlass(RoundedCornerShape(20.dp), GlassStrength.Regular) { if (focused) 1f else 0f }
            .padding(horizontal = 24.dp, vertical = 18.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = Glass.Text, fontSize = 26.sp),
            cursorBrush = SolidColor(Color.White),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
            decorationBox = { inner ->
                if (value.isEmpty()) Text("Your name", color = Glass.TextDim, fontSize = 26.sp)
                inner()
            },
        )
    }
}
