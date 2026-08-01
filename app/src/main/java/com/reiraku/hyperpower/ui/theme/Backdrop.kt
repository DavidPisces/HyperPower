package com.reiraku.hyperpower.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.shader.isRenderEffectSupported
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun rememberMiuixBlurBackdrop(): LayerBackdrop? {
    if (!isRenderEffectSupported()) return null
    val pageColor = MiuixTheme.colorScheme.surface
    return rememberLayerBackdrop {
        drawRect(pageColor)
        drawContent()
    }
}

@Composable
fun Modifier.miuixGaussianBlur(
    backdrop: LayerBackdrop?,
    shape: Shape = RectangleShape,
    blurRadius: Float = 25f,
    blendAlpha: Float = 0.78f,
): Modifier {
    if (backdrop == null) return this
    return then(
        Modifier.textureBlur(
            backdrop = backdrop,
            shape = shape,
            blurRadius = blurRadius,
            colors = BlurColors(
                blendColors = listOf(
                    BlendColorEntry(
                        color = MiuixTheme.colorScheme.surface.copy(alpha = blendAlpha),
                    ),
                ),
            ),
        ),
    )
}

@Composable
fun miuixBarColor(backdrop: LayerBackdrop?): Color =
    if (backdrop == null) MiuixTheme.colorScheme.surface else Color.Transparent
