package com.reiraku.hyperpower.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reiraku.hyperpower.ui.theme.miuixBarColor
import com.reiraku.hyperpower.ui.theme.miuixGaussianBlur
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

data class HyperBottomNavItem(
    val label: String,
    val icon: ImageVector,
)

@Composable
fun HyperBottomBar(
    items: List<HyperBottomNavItem>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    backdrop: LayerBackdrop? = null,
) {
    val navigationBarInset = WindowInsets.navigationBars
        .asPaddingValues()
        .calculateBottomPadding()
    val fallbackColor = miuixBarColor(backdrop)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp + navigationBarInset),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Row(
            modifier = Modifier
                .width(IntrinsicSize.Min)
                .shadow(
                    elevation = 10.dp,
                    shape = CircleShape,
                    ambientColor = Color.Black.copy(alpha = 0.12f),
                    spotColor = Color.Black.copy(alpha = 0.18f),
                )
                .miuixGaussianBlur(
                    backdrop = backdrop,
                    shape = CircleShape,
                    blendAlpha = 0.66f,
                )
                .background(fallbackColor, CircleShape)
                .height(64.dp)
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEachIndexed { index, item ->
                HyperBottomBarItem(
                    item = item,
                    selected = index == selectedIndex,
                    onClick = { onSelected(index) },
                )
            }
        }
    }
}

@Composable
private fun RowScope.HyperBottomBarItem(
    item: HyperBottomNavItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val indicatorColor = MiuixTheme.colorScheme.primary.copy(alpha = 0.15f)
    val contentColor = if (selected) {
        MiuixTheme.colorScheme.primary
    } else {
        MiuixTheme.colorScheme.onSurface
    }
    val animatedBackground by animateColorAsState(
        targetValue = if (selected) indicatorColor else Color.Transparent,
        animationSpec = spring(stiffness = 500f),
        label = "BottomBarIndicator",
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.04f else 1f,
        animationSpec = spring(stiffness = 500f),
        label = "BottomBarItemScale",
    )

    Column(
        modifier = Modifier
            .defaultMinSize(minWidth = 76.dp)
            .fillMaxHeight()
            .weight(1f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(animatedBackground)
            .clickable(
                role = Role.Tab,
                onClick = onClick,
            )
            .semantics { this.selected = selected },
        verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            tint = contentColor,
        )
        Text(
            text = item.label,
            color = contentColor,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Visible,
        )
    }
}
