package com.vortex.launcher.ui.common

import androidx.compose.ui.graphics.vector.ImageVector

enum class LauncherMode { Classic, Gaming }

/** Minimal UI model. Map your existing Room / domain entity to it. */
data class AppEntry(
    val packageName: String,
    val label: String,
    val isGame: Boolean = false,
)

data class WeatherUi(
    val temperature: String,
    val condition: String,
    val icon: ImageVector,
)
