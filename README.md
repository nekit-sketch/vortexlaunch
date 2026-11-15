# Vortex Launcher UI rewrite: setup

Package root: `com.vortex.launcher.ui` (folders match the sub-packages: common, splash, onboarding, home, apps, control, gaming, transition).

## Dependencies (app/build.gradle.kts)

```kotlin
implementation("androidx.tv:tv-material:1.0.0")                 // Text / Icon
implementation("androidx.compose.material:material-icons-extended")
implementation("androidx.navigation:navigation-compose:2.7.7")
implementation("androidx.activity:activity-compose:1.9.0")      // BackHandler, OpenDocument launcher
implementation("androidx.core:core-ktx:1.13.1")                 // Drawable.toBitmap
implementation("io.coil-kt:coil-compose:2.7.0")                 // Coil 2.x API is used
```

## Coil: register the loader (Application)

```kotlin
@HiltAndroidApp
class VortexApp : Application(), coil.ImageLoaderFactory {
    override fun newImageLoader() = VortexImageLoader.create(this)
}
```

This also registers the app icon / banner fetcher, so `AsyncImage(model = AppImage(pkg))` works and is memory-cached.

## Wiring

```kotlin
var mode by rememberSaveable { mutableStateOf(LauncherMode.Classic) }
var showAllApps by remember { mutableStateOf(false) }
var showControl by remember { mutableStateOf(false) }

Box(Modifier.fillMaxSize()) {
    ModeTransitionHost(
        targetMode = mode,
        classic = {
            AppleHomeScreen(wallpaper, pinned, weather, ::launch,
                onOpenAllApps = { showAllApps = true },
                onOpenControlCenter = { showControl = true },
                onSwitchToGaming = { mode = LauncherMode.Gaming })
        },
        gaming = {
            Ps5HomeScreen(games, ::launch,
                onOpenControlCenter = { showControl = true },
                onSwitchToClassic = { mode = LauncherMode.Classic })
        },
    )
    AnimatedVisibility(showAllApps, enter = fadeIn() + scaleIn(initialScale = 0.95f), exit = fadeOut()) {
        AllAppsScreen(allApps, wallpaper, ::launch, onBack = { showAllApps = false })
    }
    ControlCenterPanel(showControl, tiles, volume, onVolumeChange, onTileClick, onDismiss = { showControl = false })
}
```

Splash: `SplashScreen(onFinished = { /* go to onboarding or home */ })`.
Onboarding: `OnboardingNavHost(onFinished = { result -> /* save wallpaper (toStorage()), name, avatarUri */ })`.

## Notes

- `AppEntry`, `WeatherUi` in `common/Models.kt` are minimal UI models: map your Room entities onto them.
- Wallpaper is stored as a string: `wallpaper.toStorage()` / `Wallpaper.fromStorage(str)` (DataStore).
- Gallery uris are persisted via `takePersistableUriPermission` (OpenDocument). Many TV boxes ship without a document picker; the UI shows a message instead of crashing.
- Performance: no blur anywhere; animated values are read in draw / graphicsLayer only; previews are static frames; secondary screens use a static wallpaper.
