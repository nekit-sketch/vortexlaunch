package com.vortex.launcher.ui.common

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Ticks once per minute, aligned to the minute boundary. */
@Composable
fun rememberNow(): State<LocalDateTime> = produceState(initialValue = LocalDateTime.now()) {
    while (true) {
        val now = LocalDateTime.now()
        value = now
        delay((60_000L - (now.second * 1_000L + now.nano / 1_000_000L)).coerceAtLeast(500L))
    }
}

@Composable
fun rememberTimeText(): String {
    val context = LocalContext.current
    val now by rememberNow()
    val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm"
    return remember(now, pattern) { now.format(DateTimeFormatter.ofPattern(pattern)) }
}

@Composable
fun rememberDateText(): String {
    val now by rememberNow()
    return remember(now) { now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")) }
}

@Composable
fun ClockBlock(modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(rememberTimeText(), color = Glass.Text, fontSize = 76.sp, fontWeight = FontWeight.Light)
        Text(rememberDateText(), color = Glass.TextDim, fontSize = 22.sp)
    }
}
