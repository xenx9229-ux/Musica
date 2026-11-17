package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun LiquidGlassBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val config = LocalGlassThemeConfig.current
    val accent by animateColorAsState(
        targetValue = LocalAlbumAccentColor.current,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "accentAnim"
    )
    val transition = rememberInfiniteTransition(label = "liquidMesh")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift"
    )

    val isDark = config.glassMode == GlassMode.MIDNIGHT_NEON
    Box(
        modifier = modifier.drawBehind {
            val baseColors = if (isDark) {
                listOf(MidnightBackgroundTop, MidnightBackgroundMid, MidnightBackgroundBottom)
            } else {
                listOf(FrostBackgroundTop, FrostBackgroundMid, FrostBackgroundBottom)
            }
            drawRect(brush = Brush.verticalGradient(baseColors))

            // Specular fluid orbs reactive to album accent
            val orb1 = Offset(size.width * (0.2f + 0.55f * drift), size.height * 0.16f)
            val orb2 = Offset(size.width * (0.85f - 0.5f * drift), size.height * 0.55f)
            val orb3 = Offset(size.width * (0.35f + 0.3f * (1f - drift)), size.height * 0.88f)

            val alphaScale = if (isDark) 0.28f else 0.34f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = alphaScale), Color.Transparent),
                    center = orb1,
                    radius = size.minDimension * 0.85f
                ),
                center = orb1,
                radius = size.minDimension * 0.85f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(IosViolet.copy(alpha = alphaScale * 0.85f), Color.Transparent),
                    center = orb2,
                    radius = size.minDimension * 0.9f
                ),
                center = orb2,
                radius = size.minDimension * 0.9f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(IosRose.copy(alpha = alphaScale * 0.55f), Color.Transparent),
                    center = orb3,
                    radius = size.minDimension * 0.75f
                ),
                center = orb3,
                radius = size.minDimension * 0.75f
            )
        },
        content = content
    )
}

@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp? = null,
    onClick: (() -> Unit)? = null,
    highlightBorder: Boolean = true,
    contentPadding: Dp = 14.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val config = LocalGlassThemeConfig.current
    val accent = LocalAlbumAccentColor.current
    val isDark = config.glassMode == GlassMode.MIDNIGHT_NEON
    val effectiveRadius = cornerRadius ?: config.cornerSoftnessDp.dp
    val shape = RoundedCornerShape(effectiveRadius)

    val fillAlpha = if (isDark) {
        (config.frostOpacity * 0.75f).coerceIn(0.14f, 0.45f)
    } else {
        (config.frostOpacity + 0.38f).coerceIn(0.45f, 0.82f)
    }
    val glassFill = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = fillAlpha * 0.42f),
                MidnightSurface.copy(alpha = 0.72f),
                accent.copy(alpha = fillAlpha * 0.25f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = fillAlpha),
                Color.White.copy(alpha = (fillAlpha - 0.14f).coerceAtLeast(0.32f)),
                accent.copy(alpha = 0.12f)
            )
        )
    }

    val borderBrush = if (highlightBorder) {
        Brush.linearGradient(
            colors = if (isDark) {
                listOf(
                    Color.White.copy(alpha = 0.38f),
                    accent.copy(alpha = 0.34f),
                    Color.White.copy(alpha = 0.10f)
                )
            } else {
                listOf(
                    Color.White.copy(alpha = 0.95f),
                    accent.copy(alpha = 0.30f),
                    Color.White.copy(alpha = 0.50f)
                )
            }
        )
    } else {
        Brush.linearGradient(listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.08f)))
    }

    val baseModifier = modifier
        .shadow(
            elevation = if (isDark) 10.dp else 8.dp,
            shape = shape,
            ambientColor = accent.copy(alpha = 0.24f),
            spotColor = accent.copy(alpha = 0.28f)
        )
        .clip(shape)
        .background(glassFill)
        .border(width = 1.dp, brush = borderBrush, shape = shape)
        .let { m -> if (onClick != null) m.clickable(onClick = onClick) else m }

    Box(
        modifier = baseModifier.padding(contentPadding),
        content = content
    )
}

@Composable
fun LiquidGlassPill(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val config = LocalGlassThemeConfig.current
    val accent = LocalAlbumAccentColor.current
    val isDark = config.glassMode == GlassMode.MIDNIGHT_NEON
    val shape: Shape = CircleShape
    val bgColor = when {
        selected -> accent.copy(alpha = if (isDark) 0.30f else 0.22f)
        isDark -> Color.White.copy(alpha = 0.10f)
        else -> Color.White.copy(alpha = 0.65f)
    }
    val borderColor = if (selected) accent.copy(alpha = 0.85f) else MaterialTheme.colorScheme.outline

    Surface(
        modifier = modifier
            .clip(shape)
            .let { m -> if (onClick != null) m.clickable(onClick = onClick) else m },
        shape = shape,
        color = bgColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
            content = content
        )
    }
}

@Composable
fun StreakAvatarFrame(
    streakDays: Int,
    borderStyle: String = "streak_auto",
    modifier: Modifier = Modifier,
    sizeDp: Dp = 56.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = when (borderStyle) {
        "neon_cyan" -> listOf(IosCyan, IosCobalt, Color(0xFF67E8F9))
        "neon_rose" -> listOf(IosRose, IosViolet, IosAmber)
        "royal_gold" -> listOf(Streak100Gold, Streak3Flame, Color(0xFFFFF3B0))
        "aurora_emerald" -> listOf(Streak20Emerald, IosCyan, Streak300Aurora)
        else -> streakMilestoneColors(streakDays)
    }
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Brush.sweepGradient(colors))
            .padding(3.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .padding(2.dp),
            contentAlignment = Alignment.Center,
            content = content
        )
    }
}
