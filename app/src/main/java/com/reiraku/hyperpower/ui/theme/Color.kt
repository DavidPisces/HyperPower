package com.reiraku.hyperpower.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class HyperPalette(
    val background: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val outline: Color,
    val primary: Color,
    val secondary: Color,
    val amber: Color,
    val error: Color,
    val onSurface: Color,
    val onSurfaceMuted: Color,
)

internal val LocalHyperPalette = staticCompositionLocalOf {
    HyperPalette(
        background = Color(0xFF071012),
        surface = Color(0xFF101B1E),
        surfaceHigh = Color(0xFF172529),
        outline = Color(0xFF2A3A3E),
        primary = Color(0xFFB8F25A),
        secondary = Color(0xFF53D7F4),
        amber = Color(0xFFFFC56E),
        error = Color(0xFFFF7B78),
        onSurface = Color(0xFFF0F6F4),
        onSurfaceMuted = Color(0xFF9DADAA),
    )
}

val HyperBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalHyperPalette.current.background

val HyperSurface: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalHyperPalette.current.surface

val HyperSurfaceHigh: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalHyperPalette.current.surfaceHigh

val HyperOutline: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalHyperPalette.current.outline

val HyperGreen: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalHyperPalette.current.primary

val HyperCyan: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalHyperPalette.current.secondary

val HyperAmber: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalHyperPalette.current.amber

val HyperRed: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalHyperPalette.current.error

val HyperOnSurface: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalHyperPalette.current.onSurface

val HyperOnSurfaceMuted: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalHyperPalette.current.onSurfaceMuted
