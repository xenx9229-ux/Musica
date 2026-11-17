package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Leaderboard
import androidx.compose.material.icons.rounded.PersonSearch
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.AlbumItem
import com.example.data.model.ArtistProfile
import com.example.data.model.PublicProfileDoc
import com.example.data.model.TrackItem
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.LiquidGlassPill
import com.example.ui.theme.StreakAvatarFrame
import com.example.ui.viewmodel.DiscoverState

@Composable
fun DiscoverScreen(
    state: DiscoverState,
    favoriteTrackIds: Set<String>,
    followedArtistIds: Set<String>,
    discoverableUsers: List<PublicProfileDoc>,
    onSearch: (String) -> Unit,
    onSelectGenre: (String) -> Unit,
    onPlayTrack: (TrackItem, List<TrackItem>) -> Unit,
    onEnqueueTrack: (TrackItem) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onDownloadTrack: (TrackItem) -> Unit,
    onSelectArtist: (ArtistProfile) -> Unit,
    onClearArtist: () -> Unit,
    onToggleFollowArtist: (String, String) -> Unit,
    onOpenAiAssistant: () -> Unit,
    onOpenUserProfileChat: (PublicProfileDoc) -> Unit,
    modifier: Modifier = Modifier
) {
    val genres = remember { listOf("All", "Electronic", "Pop", "Hip-Hop/Rap", "Lo-Fi", "Rock", "Ambient") }
    var activeFilterTab by remember { mutableStateOf(0) } // 0=Trending, 1=Cover & Remix, 2=Leaderboard, 3=Cari User

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 74.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(166.dp)
                    .clip(RoundedCornerShape(28.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_liquid),
                    contentDescription = "Musica Liquid Glass Hero",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.20f), Color.Black.copy(alpha = 0.72f))
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column(
                        modifier = Modifier.align(Alignment.BottomStart),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "MUSICA • XENPROJECT",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF7DD3FC)
                        )
                        Text(
                            text = "Seluruh Musik Dunia, Penuh & Tanpa Batas 30 Detik",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    LiquidGlassPill(
                        onClick = onOpenAiAssistant,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .testTag("open_ai_pill")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = "Musica AI",
                                tint = Color(0xFF7DD3FC),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Tanya AI",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Global Search Bar
        item {
            LiquidGlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 10.dp) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = onSearch,
                    placeholder = { Text("Cari lagu, artis, album, remix, atau @username...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Cari") },
                    trailingIcon = {
                        if (state.searchQuery.isNotBlank()) {
                            IconButton(onClick = { onSearch("") }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Hapus pencarian")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("global_search_input")
                )
            }
        }

        // Sub-navigation Pills: Trending / Cover & Remix / Leaderboard / Cari @Username
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val tabs = listOf("Trending & Global", "Cover & Remix", "Leaderboard", "Pengguna (@)")
                itemsIndexed(tabs) { idx, label ->
                    LiquidGlassPill(
                        selected = activeFilterTab == idx,
                        onClick = { activeFilterTab = idx },
                        modifier = Modifier.testTag("discover_tab_$idx")
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Genre Chips
        if (activeFilterTab == 0) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(genres) { genre ->
                        LiquidGlassPill(
                            selected = state.selectedGenre == genre,
                            onClick = { onSelectGenre(genre) }
                        ) {
                            Text(genre, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        if (state.isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        if (!state.errorMessage.isNullOrBlank()) {
            item {
                LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(state.errorMessage, style = MaterialTheme.typography.bodyMedium)
                        Button(onClick = { onSelectGenre(state.selectedGenre) }) {
                            Text("Muat Ulang Katalog")
                        }
                    }
                }
            }
        }

        // Selected Artist Detail Overlay Card
        if (state.selectedArtist != null) {
            val artist = state.selectedArtist
            val isFollowed = followedArtistIds.contains(artist.id)
            item {
                LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = artist.avatarUrl.ifBlank { R.drawable.img_vinyl_art },
                                contentDescription = artist.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(artist.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text(
                                    "@${artist.handle} • ${artist.followersCount} pengikut",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = onClearArtist) {
                                Icon(Icons.Rounded.Close, contentDescription = "Tutup artis")
                            }
                        }
                        Text(artist.bio, style = MaterialTheme.typography.bodyMedium)
                        Button(
                            onClick = { onToggleFollowArtist(artist.id, artist.name) },
                            modifier = Modifier.testTag("follow_artist_btn")
                        ) {
                            Text(if (isFollowed) "Mengikuti ✓" else "Ikuti Artis / Band")
                        }
                        state.selectedArtistTracks.take(8).forEach { track ->
                            TrackRowCard(
                                track = track,
                                isFavorite = favoriteTrackIds.contains(track.id),
                                onPlay = { onPlayTrack(track, state.selectedArtistTracks) },
                                onEnqueue = { onEnqueueTrack(track) },
                                onToggleFavorite = { onToggleFavorite(track.id) },
                                onDownload = { onDownloadTrack(track) }
                            )
                        }
                    }
                }
            }
        }

        // Tab 3: Search Musica Users (@username)
        if (activeFilterTab == 3) {
            val matchedUsers = if (state.searchQuery.isBlank()) {
                discoverableUsers
            } else {
                val q = state.searchQuery.removePrefix("@").lowercase()
                discoverableUsers.filter {
                    it.username.lowercase().contains(q) || it.displayName.lowercase().contains(q)
                }
            }
            item {
                Text(
                    text = "Pengguna Musica (${matchedUsers.size})",
                    style = MaterialTheme.typography.titleLarge
                )
            }
            if (matchedUsers.isEmpty()) {
                item {
                    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.PersonSearch, contentDescription = null)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Belum ada pengguna ditemukan atau Anda sedang dalam mode Guest.")
                        }
                    }
                }
            } else {
                items(matchedUsers, key = { it.userId }) { user ->
                    LiquidGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onOpenUserProfileChat(user) }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StreakAvatarFrame(
                                streakDays = user.streakDays.toInt(),
                                borderStyle = user.borderStyle,
                                modifier = Modifier.size(52.dp)
                            ) {
                                Text(
                                    text = user.displayName.take(1).uppercase(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(user.displayName, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "@${user.username} • 🔥 ${user.streakDays} hari",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedButton(onClick = { onOpenUserProfileChat(user) }) {
                                Text("Chat / Streak")
                            }
                        }
                    }
                }
            }
        }

        // Artists Carousel
        if (state.artists.isNotEmpty() && activeFilterTab == 0) {
            item {
                Text("Artis & Band Dunia", style = MaterialTheme.typography.titleLarge)
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.artists, key = { it.id }) { artist ->
                        LiquidGlassCard(
                            modifier = Modifier.width(142.dp),
                            onClick = { onSelectArtist(artist) }
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                AsyncImage(
                                    model = artist.avatarUrl.ifBlank { R.drawable.img_vinyl_art },
                                    contentDescription = artist.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                )
                                Text(
                                    text = artist.name,
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (followedArtistIds.contains(artist.id)) "Diikuti ✓" else "Lihat Lagu",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Recommended Albums Carousel
        if (state.albums.isNotEmpty() && activeFilterTab == 0) {
            item {
                Text("Album Rekomendasi Global", style = MaterialTheme.typography.titleLarge)
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.albums, key = { it.id }) { album ->
                        AlbumSquareCard(
                            album = album,
                            onClick = { onSearch("${album.artist} ${album.title}") }
                        )
                    }
                }
            }
        }

        // Tracks List based on Tab / Search
        val pool = if (state.searchQuery.isNotBlank()) state.searchTracks else state.trendingTracks
        val displayedTracks = when (activeFilterTab) {
            1 -> {
                val filtered = pool.filter { it.isRemix || it.isCover }
                if (filtered.isNotEmpty()) filtered else pool
            }
            2 -> pool.sortedByDescending { it.listenerCount }
            3 -> emptyList()
            else -> pool
        }

        if (displayedTracks.isNotEmpty()) {
            item {
                val header = when (activeFilterTab) {
                    1 -> "Cover & Remix Pilihan"
                    2 -> "Leaderboard Musik Global"
                    else -> if (state.searchQuery.isNotBlank()) "Hasil Pencarian Lagu" else "Trending Sekarang"
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (activeFilterTab == 2) {
                        Icon(
                            Icons.Rounded.Leaderboard,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(header, style = MaterialTheme.typography.titleLarge)
                }
            }
            itemsIndexed(displayedTracks, key = { idx, t -> "${t.id}_$idx" }) { index, track ->
                TrackRowCard(
                    track = track,
                    rank = if (activeFilterTab == 2) index + 1 else null,
                    isFavorite = favoriteTrackIds.contains(track.id),
                    onPlay = { onPlayTrack(track, displayedTracks) },
                    onEnqueue = { onEnqueueTrack(track) },
                    onToggleFavorite = { onToggleFavorite(track.id) },
                    onDownload = { onDownloadTrack(track) }
                )
            }
        }
    }
}

@Composable
private fun AlbumSquareCard(album: AlbumItem, onClick: () -> Unit) {
    LiquidGlassCard(
        modifier = Modifier.width(152.dp),
        contentPadding = 10.dp,
        onClick = onClick
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (album.coverUrl.isNotBlank()) {
                AsyncImage(
                    model = album.coverUrl,
                    contentDescription = album.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(126.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(126.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Album, contentDescription = album.title, modifier = Modifier.size(42.dp))
                }
            }
            Text(
                text = album.title,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${album.artist} • ${album.year}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun TrackRowCard(
    track: TrackItem,
    isFavorite: Boolean,
    onPlay: () -> Unit,
    onEnqueue: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDownload: () -> Unit,
    rank: Int? = null
) {
    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = 12.dp,
        onClick = onPlay
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (rank != null) {
                Text(
                    text = "#$rank",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(36.dp)
                )
            }

            AsyncImage(
                model = track.coverUrl.ifBlank { R.drawable.img_vinyl_art },
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${track.artist} • ${track.genre}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Headphones,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "${formatCompactCount(track.listenerCount)} pendengar • ${formatDuration(track.durationMs)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "Favorit",
                    tint = if (isFavorite) Color(0xFFF43F5E) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onEnqueue) {
                Icon(Icons.Rounded.QueueMusic, contentDescription = "Tambah ke antrean")
            }
            IconButton(onClick = onDownload) {
                Icon(Icons.Rounded.Download, contentDescription = "Unduh offline")
            }
            IconButton(onClick = onPlay) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Putar lagu",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

fun formatDuration(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val min = totalSec / 60L
    val sec = totalSec % 60L
    return "%d:%02d".format(min, sec)
}

fun formatCompactCount(n: Long): String = when {
    n >= 1_000_000L -> "%.1fM".format(n / 1_000_000f)
    n >= 1_000L -> "%.1fK".format(n / 1_000f)
    else -> n.toString()
}
