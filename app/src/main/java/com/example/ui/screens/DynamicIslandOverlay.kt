package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.LyricLine
import com.example.data.model.SleepTimerOption
import com.example.player.PlayerPlaybackState
import com.example.ui.theme.LocalAlbumAccentColor

@Composable
fun DynamicIslandOverlay(
    playerState: PlayerPlaybackState,
    lyrics: List<LyricLine>,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onOpenFullPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val track = playerState.currentTrack
    val accent = LocalAlbumAccentColor.current

    val pulseTransition = rememberInfiniteTransition(label = "islandPulse")
    val breathScale by pulseTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (playerState.isPlaying) 1.045f else 1.01f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (playerState.isPlaying) 750 else 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathScale"
    )
    val eqBar1 by pulseTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(420), RepeatMode.Reverse),
        label = "eq1"
    )
    val eqBar2 by pulseTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(520), RepeatMode.Reverse),
        label = "eq2"
    )

    val activeLyric = remember(playerState.positionMs, lyrics) {
        lyrics.lastOrNull { it.timeMs <= playerState.positionMs } ?: lyrics.firstOrNull()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 6.dp, start = 16.dp, end = 16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        val cornerRadius = if (expanded) 30.dp else 99.dp
        val shape = RoundedCornerShape(cornerRadius)
        val islandBg = Brush.linearGradient(
            colors = listOf(
                Color(0xFF070B14).copy(alpha = 0.94f),
                Color(0xFF10182C).copy(alpha = 0.92f),
                accent.copy(alpha = 0.24f)
            )
        )
        val borderBrush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.38f),
                accent.copy(alpha = 0.55f),
                Color.White.copy(alpha = 0.16f)
            )
        )

        Column(
            modifier = Modifier
                .widthIn(max = if (expanded) 440.dp else 285.dp)
                .graphicsLayer {
                    val s = if (!expanded) breathScale else 1f
                    scaleX = s
                    scaleY = s
                }
                .shadow(
                    elevation = 16.dp,
                    shape = shape,
                    ambientColor = accent.copy(alpha = 0.45f),
                    spotColor = accent.copy(alpha = 0.45f)
                )
                .clip(shape)
                .background(islandBg)
                .border(1.dp, borderBrush, shape)
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
                .clickable { expanded = !expanded }
                .padding(horizontal = if (expanded) 16.dp else 14.dp, vertical = if (expanded) 14.dp else 8.dp)
                .testTag("dynamic_island_pill")
        ) {
            if (!expanded) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (track != null && track.coverUrl.isNotBlank()) {
                        AsyncImage(
                            model = track.coverUrl,
                            contentDescription = track.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(accent.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MusicNote,
                                contentDescription = "Musica Island",
                                tint = accent,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Text(
                        text = track?.title ?: "Musica • Liquid Glass",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // Live Equalizer Bars
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val h1 = if (playerState.isPlaying) (14f * eqBar1).dp else 5.dp
                        val h2 = if (playerState.isPlaying) (16f * eqBar2).dp else 8.dp
                        val h3 = if (playerState.isPlaying) (12f * eqBar1).dp else 5.dp
                        Box(Modifier.width(3.dp).height(h1).clip(CircleShape).background(accent))
                        Box(Modifier.width(3.dp).height(h2).clip(CircleShape).background(Color.White))
                        Box(Modifier.width(3.dp).height(h3).clip(CircleShape).background(accent))
                    }
                }
            } else {
                // Expanded Dynamic Island Card with Cover, Lyrics, and Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (track != null && track.coverUrl.isNotBlank()) {
                        AsyncImage(
                            model = track.coverUrl,
                            contentDescription = track.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.img_vinyl_art),
                            contentDescription = "Default Album Cover",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track?.title ?: "Belum ada lagu diputar",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = track?.artist ?: "Pilih lagu dari Discover",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.72f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (playerState.sleepTimerOption != SleepTimerOption.OFF) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Bedtime,
                                    contentDescription = "Sleep timer",
                                    tint = accent,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Sleep: ${playerState.sleepTimerOption.label}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = accent
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = {
                            expanded = false
                            onOpenFullPlayer()
                        },
                        modifier = Modifier.testTag("island_open_player")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.OpenInFull,
                            contentDescription = "Buka Pemutar",
                            tint = Color.White
                        )
                    }
                    IconButton(
                        onClick = { expanded = false },
                        modifier = Modifier.testTag("island_collapse")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Perkecil Dynamic Island",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                AnimatedVisibility(visible = activeLyric != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = activeLyric?.text.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = accent,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!activeLyric?.translatedText.isNullOrBlank()) {
                            Text(
                                text = activeLyric?.translatedText.orEmpty(),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.72f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                val progress = if (playerState.durationMs > 0) {
                    (playerState.positionMs.toFloat() / playerState.durationMs.toFloat()).coerceIn(0f, 1f)
                } else 0f
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape),
                    color = accent,
                    trackColor = Color.White.copy(alpha = 0.16f)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onPrevious) {
                        Icon(
                            imageVector = Icons.Rounded.SkipPrevious,
                            contentDescription = "Sebelumnya",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = onTogglePlayPause) {
                        Icon(
                            imageVector = if (playerState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = "Putar atau Jeda",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    IconButton(onClick = onNext) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = "Berikutnya",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}
