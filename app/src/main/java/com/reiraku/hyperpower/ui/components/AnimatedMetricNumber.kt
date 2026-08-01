package com.reiraku.hyperpower.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import top.yukonga.miuix.kmp.basic.Text
import kotlin.math.max

/**
 * AppOdex 编译卡片同款逐位数字过渡：
 * 只有发生变化的数字参与动画，并根据变化顺序做轻微错峰。
 */
@Composable
fun AnimatedMetricNumber(
    value: Int,
    style: TextStyle,
    color: Color,
    animationLabel: String,
    modifier: Modifier = Modifier,
    minimumDigits: Int = 1,
) {
    val targetValue = value.coerceAtLeast(0)
    val safeMinimumDigits = minimumDigits.coerceAtLeast(1)
    var numberTransition by remember {
        mutableStateOf(NumericValueTransition(targetValue, targetValue))
    }
    val progress = remember(animationLabel) { Animatable(1f) }

    LaunchedEffect(targetValue) {
        val previousTarget = numberTransition.to
        if (previousTarget == targetValue) return@LaunchedEffect

        progress.snapTo(0f)
        val transition = NumericValueTransition(previousTarget, targetValue)
        val duration = transition.toDigitTransition(safeMinimumDigits).totalDurationMillis
        numberTransition = transition
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = duration,
                easing = LinearEasing,
            ),
        )
        numberTransition = NumericValueTransition(targetValue, targetValue)
    }

    val digitTransition = remember(numberTransition, safeMinimumDigits) {
        numberTransition.toDigitTransition(safeMinimumDigits)
    }

    Layout(
        modifier = modifier.clearAndSetSemantics {
            text = AnnotatedString(targetValue.toString().padStart(safeMinimumDigits, '0'))
        },
        content = {
            repeat(digitTransition.slotCount) { index ->
                val changeOrder = digitTransition.changedIndices
                    .indexOf(index)
                    .coerceAtLeast(0)
                NumericDigitSlot(
                    oldDigit = digitTransition.oldDigits[index].takeUnless { it == ' ' },
                    newDigit = digitTransition.newDigits[index].takeUnless { it == ' ' },
                    animationProgress = progress,
                    delayMillis = changeOrder * NUMERIC_STAGGER_MS,
                    totalDurationMillis = digitTransition.totalDurationMillis,
                    countsUp = numberTransition.to >= numberTransition.from,
                    style = style,
                    color = color,
                )
            }
        },
    ) { measurables, constraints ->
        val childConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val placeables = measurables.map { it.measure(childConstraints) }
        val slotWidth = placeables.maxOfOrNull { it.width } ?: 0
        val contentHeight = placeables.maxOfOrNull { it.height } ?: 0
        val desiredWidth = slotWidth * digitTransition.slotCount
        val layoutWidth = desiredWidth.coerceIn(constraints.minWidth, constraints.maxWidth)
        val layoutHeight = contentHeight.coerceIn(constraints.minHeight, constraints.maxHeight)

        layout(layoutWidth, layoutHeight) {
            placeables.forEachIndexed { index, placeable ->
                val slotsFromRight = digitTransition.slotCount - index
                val x = layoutWidth - slotsFromRight * slotWidth +
                    (slotWidth - placeable.width) / 2
                val y = (layoutHeight - placeable.height) / 2
                placeable.placeRelative(x, y)
            }
        }
    }
}

private data class NumericValueTransition(
    val from: Int,
    val to: Int,
)

private data class NumericDigitTransition(
    val slotCount: Int,
    val oldDigits: String,
    val newDigits: String,
    val changedIndices: List<Int>,
    val totalDurationMillis: Int,
)

private fun NumericValueTransition.toDigitTransition(
    minimumDigits: Int,
): NumericDigitTransition {
    val fromText = from.toString().padStart(minimumDigits, '0')
    val toText = to.toString().padStart(minimumDigits, '0')
    val slotCount = max(fromText.length, toText.length)
    val oldDigits = fromText.padStart(slotCount, ' ')
    val newDigits = toText.padStart(slotCount, ' ')
    val changedIndices = oldDigits.indices.filter { oldDigits[it] != newDigits[it] }
    return NumericDigitTransition(
        slotCount = slotCount,
        oldDigits = oldDigits,
        newDigits = newDigits,
        changedIndices = changedIndices,
        totalDurationMillis = NUMERIC_DIGIT_DURATION_MS +
            (changedIndices.size - 1).coerceAtLeast(0) * NUMERIC_STAGGER_MS,
    )
}

@Composable
private fun NumericDigitSlot(
    oldDigit: Char?,
    newDigit: Char?,
    animationProgress: Animatable<Float, AnimationVector1D>,
    delayMillis: Int,
    totalDurationMillis: Int,
    countsUp: Boolean,
    style: TextStyle,
    color: Color,
) {
    Box(modifier = Modifier.clipToBounds()) {
        NumericDigit(
            digit = '0',
            style = style,
            color = color,
            modifier = Modifier.graphicsLayer { alpha = 0f },
        )

        if (oldDigit == newDigit) {
            newDigit?.let { digit ->
                NumericDigit(digit = digit, style = style, color = color)
            }
            return@Box
        }

        oldDigit?.let { digit ->
            NumericDigit(
                digit = digit,
                style = style,
                color = color,
                modifier = Modifier.numericGlyphLayer(
                    animationProgress = animationProgress,
                    delayMillis = delayMillis,
                    totalDurationMillis = totalDurationMillis,
                    incoming = false,
                    countsUp = countsUp,
                ),
            )
        }

        newDigit?.let { digit ->
            NumericDigit(
                digit = digit,
                style = style,
                color = color,
                modifier = Modifier.numericGlyphLayer(
                    animationProgress = animationProgress,
                    delayMillis = delayMillis,
                    totalDurationMillis = totalDurationMillis,
                    incoming = true,
                    countsUp = countsUp,
                ),
            )
        }
    }
}

@Composable
private fun NumericDigit(
    digit: Char,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = digit.toString(),
        modifier = modifier,
        style = style,
        fontWeight = FontWeight.Bold,
        color = color,
    )
}

private fun Modifier.numericGlyphLayer(
    animationProgress: Animatable<Float, AnimationVector1D>,
    delayMillis: Int,
    totalDurationMillis: Int,
    incoming: Boolean,
    countsUp: Boolean,
): Modifier = graphicsLayer {
    val elapsedMillis = animationProgress.value * totalDurationMillis
    val progress = ((elapsedMillis - delayMillis) / NUMERIC_DIGIT_DURATION_MS)
        .coerceIn(0f, 1f)
    val easedProgress = NumericMotionEasing.transform(progress)
    val rollDirection = if (countsUp) -1f else 1f

    translationY = if (incoming) {
        -rollDirection * size.height * (1f - easedProgress)
    } else {
        rollDirection * size.height * easedProgress
    }
    alpha = if (incoming) {
        lerpFloat(NUMERIC_EDGE_ALPHA, 1f, easedProgress)
    } else {
        lerpFloat(1f, NUMERIC_EDGE_ALPHA, easedProgress)
    }
}

private fun lerpFloat(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction.coerceIn(0f, 1f)

private const val NUMERIC_DIGIT_DURATION_MS = 440
private const val NUMERIC_STAGGER_MS = 24
private const val NUMERIC_EDGE_ALPHA = 0.72f

private val NumericMotionEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
