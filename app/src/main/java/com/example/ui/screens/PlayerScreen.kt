package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.PlaylistUi
import com.example.data.model.RepeatMode
import com.example.data.model.SleepTimerOption
import com.example.data.model.TrackItem
import com.example.player.PlayerPlaybackState
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.LiquidGlassPill
import com.example.ui.theme.LocalAlbumAccentColor
import com.example.ui.viewmodel.LyricsState
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun PlayerScreen(
    playerState: PlayerPlaybackState,
    lyricsState: LyricsState,
    catalogTracks: List<TrackItem>,
    playlists: List<PlaylistUi>,
    favoriteTrackIds: Set<String>,
    followedArtistIds: Set<String>,
    onSearchCatalog: (String) -> Unit,
    onPlayTrack: (TrackItem, List<TrackItem>) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleShuffle: () -> Unit,
    onEnqueueRightSwipe: (TrackItem) -> Unit,
    onAddToPlaylist: (playlistId: String, playlistName: String, TrackItem) -> Unit,
    onCreatePlaylistAndAdd: (playlistName: String, TrackItem) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onToggleFollowArtist: (String, String) -> Unit,
    onRepostTrack: (TrackItem) -> Unit,
    onDownloadTrack: (TrackItem) -> Unit,
    onTranslateLyrics: (String) -> Unit,
    onSetSleepTimer: (SleepTimerOption) -> Unit,
    onOpenPosterStudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    val track = playerState.currentTrack ?: catalogTracks.firstOrNull()
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    var showCurvedDrawer by remember { mutableStateOf(false) }
    var showSleepDialog by remember { mutableStateOf(false) }
    var playlistTargetTrack by remember { mutableStateOf<TrackItem?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 74.dp, bottom = 104.dp)
        ) {
            // Dedicated Sub-page Tabs: Piringan Vinyl | Lirik | Terjemahan
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val pageTitles = listOf("Piringan", "Lirik", "Terjemahan")
                pageTitles.forEachIndexed { idx, label ->
                    LiquidGlassPill(
                        selected = pagerState.currentPage == idx,
                        onClick = { scope.launch { pagerState.animateScrollToPage(idx) } },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("player_subpage_$idx")
                    ) {
                        Text(label, style = MaterialTheme.typography.labelLarge)
                    }
                }
                LiquidGlassPill(
                    onClick = onOpenPosterStudio,
                    modifier = Modifier.testTag("open_poster_from_player")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Image,
                        contentDescription = "Buat Poster",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                when (page) {
                    0 -> VinylStagePage(
                        track = track,
                        playerState = playerState,
                        isFavorite = track != null && favoriteTrackIds.contains(track.id),
                        isFollowed = track != null && followedArtistIds.contains(track.artistId),
                        onClickVinyl = { showCurvedDrawer = true },
                        onTogglePlayPause = onTogglePlayPause,
                        onSeekTo = onSeekTo,
                        onNext = onNext,
                        onPrevious = onPrevious,
                        onCycleRepeat = onCycleRepeat,
                        onToggleShuffle = onToggleShuffle,
                        onToggleFavorite = { track?.let { onToggleFavorite(it.id) } },
                        onToggleFollow = { track?.let { onToggleFollowArtist(it.artistId, it.artist) } },
                        onRepost = { track?.let(onRepostTrack) },
                        onDownload = { track?.let(onDownloadTrack) },
                        onOpenSleep = { showSleepDialog = true }
                    )

                    1 -> SyncedLyricsPage(
                        track = track,
                        lyricsState = lyricsState,
                        positionMs = playerState.positionMs,
                        showTranslation = false,
                        onSeekTo = onSeekTo,
                        onOpenPosterStudio = onOpenPosterStudio
                    )

                    2 -> SyncedLyricsPage(
                        track = track,
                        lyricsState = lyricsState,
                        positionMs = playerState.positionMs,
                        showTranslation = true,
                        onSeekTo = onSeekTo,
                        onTranslate = onTranslateLyrics,
                        onOpenPosterStudio = onOpenPosterStudio
                    )
                }
            }
        }

        // Curved Elongated Bottom Drawer on Vinyl Tap (Persegi Memanjang Melengkung dari Bawah)
        AnimatedVisibility(
            visible = showCurvedDrawer,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
            ),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(240)
            ),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            CurvedSearchSwipeDrawer(
                tracks = catalogTracks,
                onSearch = onSearchCatalog,
                onSelectTrack = { chosen ->
                    onPlayTrack(chosen, catalogTracks)
                    showCurvedDrawer = false
                },
                onSwipeRightQueue = onEnqueueRightSwipe,
                onSwipeLeftPlaylist = { chosen -> playlistTargetTrack = chosen },
                onClose = { showCurvedDrawer = false }
            )
        }
    }

    if (showSleepDialog) {
        AlertDialog(
            onDismissRequest = { showSleepDialog = false },
            title = { Text("Mode Sleep & Timer") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Audio akan mengecil lembut dan berhenti otomatis saat waktu habis.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    SleepTimerOption.entries.forEach { option ->
                        OutlinedButton(
                            onClick = {
                                onSetSleepTimer(option)
                                showSleepDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (playerState.sleepTimerOption == option) "${option.label} (Aktif)" else option.label
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSleepDialog = false }) { Text("Tutup") }
            }
        )
    }

    if (playlistTargetTrack != null) {
        PlaylistPickerDialog(
            track = playlistTargetTrack!!,
            playlists = playlists,
            onDismiss = { playlistTargetTrack = null },
            onSelectPlaylist = { pl ->
                onAddToPlaylist(pl.id, pl.name, playlistTargetTrack!!)
                playlistTargetTrack = null
            },
            onCreateAndAdd = { newName ->
                onCreatePlaylistAndAdd(newName, playlistTargetTrack!!)
                playlistTargetTrack = null
            }
        )
    }
}

@Composable
private fun VinylStagePage(
    track: TrackItem?,
    playerState: PlayerPlaybackState,
    isFavorite: Boolean,
    isFollowed: Boolean,
    onClickVinyl: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleFollow: () -> Unit,
    onRepost: () -> Unit,
    onDownload: () -> Unit,
    onOpenSleep: () -> Unit
) {
    val accent = LocalAlbumAccentColor.current
    val rotation = remember { Animatable(0f) }

    LaunchedEffect(playerState.isPlaying) {
        while (isActive && playerState.isPlaying) {
            rotation.animateTo(
                targetValue = rotation.value + 360f,
                animationSpec = tween(durationMillis = 8500, easing = LinearEasing)
            )
            if (rotation.value >= 360f) {
                rotation.snapTo(rotation.value % 360f)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // Rotating Vinyl Disc (Tap to open Curved Search & Swipe Drawer)
        Box(
            modifier = Modifier
                .size(236.dp)
                .shadow(20.dp, CircleShape, ambientColor = accent.copy(alpha = 0.45f))
                .clip(CircleShape)
                .clickable(onClick = onClickVinyl)
                .testTag("spinning_vinyl_disc"),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationZ = rotation.value },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF1F2937), Color(0xFF070A12), Color(0xFF111827))
                        )
                    )
                    for (r in listOf(0.88f, 0.78f, 0.68f, 0.58f)) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.08f),
                            radius = size.minDimension * r * 0.5f,
                            style = Stroke(width = 2f)
                        )
                    }
                    drawCircle(
                        color = accent.copy(alpha = 0.45f),
                        radius = size.minDimension * 0.48f,
                        style = Stroke(width = 3f)
                    )
                }

                AsyncImage(
                    model = track?.coverUrl?.takeIf { it.isNotBlank() } ?: R.drawable.img_vinyl_art,
                    contentDescription = track?.title ?: "Piringan Vinyl",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(104.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color.White.copy(alpha = 0.65f), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.background)
                        .border(2.dp, accent, CircleShape)
                )
            }
        }

        Text(
            text = "Ketuk piringan untuk cari lagu & geser antrean/playlist",
            style = MaterialTheme.typography.labelSmall,
            color = accent
        )

        if (track != null) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = track.artist,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                LiquidGlassPill(onClick = onToggleFollow) {
                    Text(
                        text = if (isFollowed) "Diikuti ✓" else "+ Ikuti",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            // Track Stats & Description Card
            LiquidGlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 12.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Headphones,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${formatCompactCount(track.listenerCount)} pendengar",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        Text(
                            text = "Pencipta: ${track.artist}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = track.description.ifBlank { "Audio penuh berkualitas studio di Musica • XenProject." },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Scrubber
        val duration = playerState.durationMs.coerceAtLeast(track?.durationMs ?: 180_000L).coerceAtLeast(1L)
        val sliderPos = (playerState.positionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        Column(modifier = Modifier.fillMaxWidth()) {
            Slider(
                value = sliderPos,
                onValueChange = { frac -> onSeekTo((frac * duration).toLong()) },
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatDuration(playerState.positionMs), style = MaterialTheme.typography.labelSmall)
                Text(formatDuration(duration), style = MaterialTheme.typography.labelSmall)
            }
        }

        // Playback Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggleShuffle) {
                Icon(
                    imageVector = Icons.Rounded.Shuffle,
                    contentDescription = "Acak",
                    tint = if (playerState.shuffleEnabled) accent else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onPrevious) {
                Icon(Icons.Rounded.SkipPrevious, contentDescription = "Sebelumnya", modifier = Modifier.size(34.dp))
            }
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(accent)
                    .clickable(onClick = onTogglePlayPause)
                    .testTag("player_play_pause_btn"),
                contentAlignment = Alignment.Center
            ) {
                if (playerState.isBuffering) {
                    CircularProgressIndicator(modifier = Modifier.size(26.dp), color = Color.White, strokeWidth = 2.5.dp)
                } else {
                    Icon(
                        imageVector = if (playerState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = "Putar / Jeda",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
            IconButton(onClick = onNext) {
                Icon(Icons.Rounded.SkipNext, contentDescription = "Berikutnya", modifier = Modifier.size(34.dp))
            }
            IconButton(onClick = onCycleRepeat) {
                Icon(
                    imageVector = if (playerState.repeatMode == RepeatMode.ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                    contentDescription = "Ulangi lagu",
                    tint = if (playerState.repeatMode != RepeatMode.OFF) accent else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Action Bar: Love, Repost, Download, Sleep Mode
        LiquidGlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 8.dp) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(onClick = onToggleFavorite, modifier = Modifier.testTag("player_love_btn")) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Love Favorit",
                        tint = if (isFavorite) Color(0xFFF43F5E) else MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = onRepost, modifier = Modifier.testTag("player_repost_btn")) {
                    Icon(Icons.Rounded.Share, contentDescription = "Posting Ulang")
                }
                IconButton(onClick = onDownload, modifier = Modifier.testTag("player_download_btn")) {
                    Icon(Icons.Rounded.Download, contentDescription = "Download Offline")
                }
                IconButton(onClick = onOpenSleep, modifier = Modifier.testTag("player_sleep_btn")) {
                    Icon(
                        imageVector = Icons.Rounded.Bedtime,
                        contentDescription = "Mode Sleep",
                        tint = if (playerState.sleepTimerOption != SleepTimerOption.OFF) accent else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SyncedLyricsPage(
    track: TrackItem?,
    lyricsState: LyricsState,
    positionMs: Long,
    showTranslation: Boolean,
    onSeekTo: (Long) -> Unit,
    onTranslate: ((String) -> Unit)? = null,
    onOpenPosterStudio: () -> Unit
) {
    val listState = rememberLazyListState()
    val activeIdx = remember(positionMs, lyricsState.lines) {
        lyricsState.lines.indexOfLast { it.timeMs <= positionMs }.coerceAtLeast(0)
    }

    LaunchedEffect(activeIdx) {
        if (lyricsState.lines.isNotEmpty() && activeIdx in lyricsState.lines.indices) {
            listState.animateScrollToItem((activeIdx - 2).coerceAtLeast(0))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        LiquidGlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 12.dp) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (showTranslation) "Terjemahan Lirik AI" else "Lirik Tersinkronisasi",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = track?.let { "${it.title} • ${it.artist}" } ?: "Pilih lagu",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (showTranslation && onTranslate != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Indonesia", "English", "Japanese").forEach { lang ->
                            LiquidGlassPill(
                                selected = lyricsState.targetLanguage == lang,
                                onClick = { onTranslate(lang) }
                            ) {
                                Text(lang.take(3).uppercase(), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                } else {
                    OutlinedButton(onClick = onOpenPosterStudio) {
                        Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Poster Lirik")
                    }
                }
            }
        }

        if (showTranslation && onTranslate != null && lyricsState.lines.none { it.translatedText.isNotBlank() }) {
            Button(
                onClick = { onTranslate(lyricsState.targetLanguage) },
                enabled = !lyricsState.isTranslating,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.Translate, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (lyricsState.isTranslating) "Menerjemahkan dengan AI..." else "Terjemahkan Lirik ke ${lyricsState.targetLanguage}")
            }
        }

        if (!lyricsState.translationError.isNullOrBlank()) {
            Text(
                text = lyricsState.translationError,
                color = MaterialTheme.colorScheme.tertiary,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (lyricsState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                itemsIndexed(lyricsState.lines) { idx, line ->
                    val isActive = idx == activeIdx
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSeekTo(line.timeMs) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = line.text,
                            style = if (isActive) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                            color = if (isActive) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.56f)
                            }
                        )
                        if (showTranslation && line.translatedText.isNotBlank()) {
                            Text(
                                text = line.translatedText,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isActive) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                                },
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CurvedSearchSwipeDrawer(
    tracks: List<TrackItem>,
    onSearch: (String) -> Unit,
    onSelectTrack: (TrackItem) -> Unit,
    onSwipeRightQueue: (TrackItem) -> Unit,
    onSwipeLeftPlaylist: (TrackItem) -> Unit,
    onClose: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val drawerShape = RoundedCornerShape(topStart = 42.dp, topEnd = 42.dp)
    val accent = LocalAlbumAccentColor.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(470.dp)
            .shadow(24.dp, drawerShape)
            .clip(drawerShape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                        MaterialTheme.colorScheme.background.copy(alpha = 0.98f)
                    )
                )
            )
            .border(1.5.dp, accent.copy(alpha = 0.45f), drawerShape)
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 14.dp)
            .testTag("curved_vinyl_drawer")
    ) {
        Box(
            modifier = Modifier
                .width(46.dp)
                .height(5.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.35f))
                .align(Alignment.CenterHorizontally)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Ganti Lagu & Gestur Cepat", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Geser kanan → Antrean  •  Geser kiri ← Playlist",
                    style = MaterialTheme.typography.labelSmall,
                    color = accent
                )
            }
            IconButton(onClick = onClose, modifier = Modifier.testTag("close_curved_drawer")) {
                Icon(Icons.Rounded.Close, contentDescription = "Tutup panel")
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                onSearch(it)
            },
            placeholder = { Text("Cari lagu untuk diganti atau digeser...") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .testTag("drawer_search_input")
        )

        val visibleTracks = if (query.isBlank()) {
            tracks
        } else {
            tracks.filter {
                it.title.contains(query, ignoreCase = true) ||
                    it.artist.contains(query, ignoreCase = true)
            }.ifEmpty { tracks }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(visibleTracks, key = { it.id }) { item ->
                SwipeableDrawerTrackRow(
                    track = item,
                    onClick = { onSelectTrack(item) },
                    onSwipeRightQueue = { onSwipeRightQueue(item) },
                    onSwipeLeftPlaylist = { onSwipeLeftPlaylist(item) }
                )
            }
        }
    }
}

@Composable
private fun SwipeableDrawerTrackRow(
    track: TrackItem,
    onClick: () -> Unit,
    onSwipeRightQueue: () -> Unit,
    onSwipeLeftPlaylist: () -> Unit
) {
    var rawOffsetX by remember { mutableFloatStateOf(0f) }
    val animatedOffsetX by animateFloatAsState(
        targetValue = rawOffsetX,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "swipeOffset"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                when {
                    rawOffsetX > 30f -> Color(0xFF0EA5E9).copy(alpha = 0.25f)
                    rawOffsetX < -30f -> Color(0xFF8B5CF6).copy(alpha = 0.25f)
                    else -> Color.Transparent
                }
            )
    ) {
        if (rawOffsetX > 20f) {
            Row(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.QueueMusic, contentDescription = null, tint = Color(0xFF38BDF8))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Antrean", style = MaterialTheme.typography.labelMedium)
            }
        } else if (rawOffsetX < -20f) {
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Playlist", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.Rounded.PlaylistAdd, contentDescription = null, tint = Color(0xFFA78BFA))
            }
        }

        LiquidGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
                .pointerInput(track.id) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (rawOffsetX > 140f) {
                                onSwipeRightQueue()
                            } else if (rawOffsetX < -140f) {
                                onSwipeLeftPlaylist()
                            }
                            rawOffsetX = 0f
                        },
                        onDragCancel = { rawOffsetX = 0f },
                        onHorizontalDrag = { _, dragAmount ->
                            rawOffsetX = (rawOffsetX + dragAmount).coerceIn(-240f, 240f)
                        }
                    )
                },
            contentPadding = 10.dp,
            onClick = onClick
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                AsyncImage(
                    model = track.coverUrl.ifBlank { R.drawable.img_vinyl_art },
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(track.title, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(track.artist, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                IconButton(onClick = onSwipeRightQueue) {
                    Icon(Icons.Rounded.QueueMusic, contentDescription = "Antrean")
                }
                IconButton(onClick = onSwipeLeftPlaylist) {
                    Icon(Icons.Rounded.PlaylistAdd, contentDescription = "Tambah ke Playlist")
                }
            }
        }
    }
}

@Composable
fun PlaylistPickerDialog(
    track: TrackItem,
    playlists: List<PlaylistUi>,
    onDismiss: () -> Unit,
    onSelectPlaylist: (PlaylistUi) -> Unit,
    onCreateAndAdd: (String) -> Unit
) {
    var newPlaylistName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Masukkan '${track.title}' ke Playlist") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                playlists.forEach { pl ->
                    OutlinedButton(
                        onClick = { onSelectPlaylist(pl) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("${pl.name} (${pl.trackCount} lagu)")
                    }
                }
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text("Atau buat playlist baru") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newPlaylistName.isNotBlank()) onCreateAndAdd(newPlaylistName)
                },
                enabled = newPlaylistName.isNotBlank()
            ) {
                Text("Buat & Tambah")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
