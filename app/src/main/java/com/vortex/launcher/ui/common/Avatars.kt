package com.vortex.launcher.ui.common

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/** Minimalist black-and-white "3D" bust: lit sphere head + shoulders, drawn as vectors (no emoji). */
@Composable
fun DefaultAvatar(modifier: Modifier = Modifier) {
    Canvas(modifier.aspectRatio(1f).clip(CircleShape)) {
        val s = size.minDimension
        // studio backdrop
        drawRect(
            Brush.radialGradient(
                listOf(Color(0xFF3A3D44), Color(0xFF0B0C0F)),
                center = Offset(s * 0.35f, s * 0.25f),
                radius = s * 0.95f,
            )
        )
        // shoulders
        drawOval(
            Brush.verticalGradient(
                listOf(Color(0xFFF2F3F5), Color(0xFF6B7078), Color(0xFF1A1C20)),
                startY = s * 0.62f,
                endY = s * 1.2f,
            ),
            topLeft = Offset(s * 0.14f, s * 0.64f),
            size = Size(s * 0.72f, s * 0.66f),
        )
        // contact shadow under the head
        drawCircle(Color.Black.copy(alpha = 0.4f), radius = s * 0.17f, center = Offset(s * 0.5f, s * 0.66f))
        // head sphere
        drawCircle(
            Brush.radialGradient(
                listOf(Color.White, Color(0xFFB9BDC4), Color(0xFF3A3D44)),
                center = Offset(s * 0.43f, s * 0.34f),
                radius = s * 0.27f,
            ),
            radius = s * 0.2f,
            center = Offset(s * 0.5f, s * 0.42f),
        )
        // specular dot
        drawCircle(Color.White.copy(alpha = 0.75f), radius = s * 0.032f, center = Offset(s * 0.42f, s * 0.33f))
    }
}

/** Custom avatar via Coil (down-sampled to 256px, cached) or the default vector avatar. */
@Composable
fun AvatarImage(uri: Uri?, modifier: Modifier = Modifier) {
    val ring = Modifier.border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
    if (uri == null) {
        DefaultAvatar(modifier.then(ring))
    } else {
        AsyncImage(
            model = rememberImageRequest(uri, 256, 256),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.aspectRatio(1f).clip(CircleShape).then(ring),
        )
    }
}
