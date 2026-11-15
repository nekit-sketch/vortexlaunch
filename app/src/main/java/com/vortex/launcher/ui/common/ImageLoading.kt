package com.vortex.launcher.ui.common

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.decode.DataSource
import coil.disk.DiskCache
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.key.Keyer
import coil.memory.MemoryCache
import coil.request.ImageRequest
import coil.request.Options
import coil.size.Precision
import coil.size.Scale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Coil model for an installed app. banner=true -> TV banner (16:9) with icon fallback. */
data class AppImage(val packageName: String, val banner: Boolean = false)

object VortexImageLoader {
    /** Call from Application.newImageLoader() (Application implements coil.ImageLoaderFactory). */
    fun create(context: Context): ImageLoader = ImageLoader.Builder(context)
        .memoryCache { MemoryCache.Builder(context).maxSizePercent(0.12).build() }
        .diskCache {
            DiskCache.Builder()
                .directory(context.cacheDir.resolve("vortex_images"))
                .maxSizeBytes(96L * 1024 * 1024)
                .build()
        }
        .components {
            add(AppImageKeyer())
            add(AppImageFetcher.Factory(context))
        }
        .allowRgb565(true)
        .crossfade(180)
        .respectCacheHeaders(false)
        .build()
}

private class AppImageKeyer : Keyer<AppImage> {
    override fun key(data: AppImage, options: Options): String = "app:${data.packageName}:${data.banner}"
}

/**
 * Renders the PackageManager drawable into a small BitmapDrawable, so Coil can keep it in the
 * memory cache (plain Drawables from PackageManager are NOT cached by Coil).
 */
private class AppImageFetcher(
    private val context: Context,
    private val data: AppImage,
) : Fetcher {

    override suspend fun fetch(): FetchResult? = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val drawable = try {
            if (data.banner) {
                pm.getApplicationBanner(data.packageName) ?: pm.getApplicationIcon(data.packageName)
            } else {
                pm.getApplicationIcon(data.packageName)
            }
        } catch (e: PackageManager.NameNotFoundException) {
            return@withContext null
        }
        val (w, h) = if (data.banner) 320 to 180 else 192 to 192
        val bitmap = drawable.toBitmap(w, h, Bitmap.Config.ARGB_8888)
        DrawableResult(BitmapDrawable(context.resources, bitmap), false, DataSource.DISK)
    }

    class Factory(private val context: Context) : Fetcher.Factory<AppImage> {
        override fun create(data: AppImage, options: Options, imageLoader: ImageLoader): Fetcher =
            AppImageFetcher(context, data)
    }
}

/** Down-sampled request for wallpapers / avatars (gallery uris). Never decodes full-size photos. */
@Composable
fun rememberImageRequest(data: Any?, width: Int, height: Int, crossfadeMs: Int = 250): ImageRequest {
    val context = LocalContext.current
    return remember(data, width, height) {
        ImageRequest.Builder(context)
            .data(data)
            .size(width, height)
            .scale(Scale.FILL)
            .precision(Precision.INEXACT)
            .crossfade(crossfadeMs)
            .build()
    }
}

/**
 * Opens the system file/gallery picker. Many TV boxes (SberBox included) ship WITHOUT a picker,
 * so launch failures are reported through [onUnavailable] instead of crashing.
 */
@Composable
fun rememberImagePicker(onPicked: (Uri) -> Unit, onUnavailable: () -> Unit = {}): () -> Unit {
    val context = LocalContext.current
    val pickedState = rememberUpdatedState(onPicked)
    val unavailableState = rememberUpdatedState(onUnavailable)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            pickedState.value(uri)
        }
    }
    return remember(launcher) {
        {
            try {
                launcher.launch(arrayOf("image/*"))
            } catch (e: ActivityNotFoundException) {
                unavailableState.value()
            }
        }
    }
}
