package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CompareArrows
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.PlaylistMatchResult
import com.example.data.model.PlaylistUi
import com.example.data.model.TrackItem
import com.example.data.repository.AiUiState
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.LiquidGlassPill
import com.example.ui.viewmodel.AiChatEntry
import com.example.util.PosterRenderer

@Composable
fun LibraryPlaylistsScreen(
    playlists: List<PlaylistUi>,
    downloadedTracks: List<TrackItem>,
    queue: List<TrackItem>,
    catalogTracks: List<TrackItem>,
    aiChatHistory: List<AiChatEntry>,
    aiState: AiUiState,
    matchResult: PlaylistMatchResult?,
    initialSubTab: Int = 0,
    onCreatePlaylist: (String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onAddTrackToPlaylist: (String, String, TrackItem) -> Unit,
    onRemoveTrackFromPlaylist: (String, String) -> Unit,
    onDownloadPlaylist: (PlaylistUi) -> Unit,
    onRemoveDownload: (String) -> Unit,
    onPlayTrack: (TrackItem, List<TrackItem>) -> Unit,
    onRemoveFromQueue: (String) -> Unit,
    onImportLocalAudio: (Uri) -> Unit,
    onImportExternalPlaylist: (String) -> Unit,
    onSharePlaylistLink: (PlaylistUi) -> Unit,
    onMatchPlaylistByLink: (PlaylistUi, String) -> Unit,
    onClearMatchResult: () -> Unit,
    onAskAi: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var activeTab by remember(initialSubTab) { mutableStateOf(initialSubTab) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var addTrackTargetPlaylist by remember { mutableStateOf<PlaylistUi?>(null) }
    var externalLinkInput by remember { mutableStateOf("") }
    var matchLinkInput by remember { mutableStateOf("") }
    var aiInput by remember { mutableStateOf("") }

    val localAudioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri -> uri?.let(onImportLocalAudio) }
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 74.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val tabs = listOf("Playlist", "Offline (${downloadedTracks.size})", "Antrean (${queue.size})", "Impor & Match", "Musica AI")
                itemsIndexed(tabs) { idx, label ->
                    LiquidGlassPill(
                        selected = activeTab == idx,
                        onClick = { activeTab = idx },
                        modifier = Modifier.testTag("library_tab_$idx")
                    ) {
                        Text(label, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        when (activeTab) {
            0 -> {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Playlist Anda", style = MaterialTheme.typography.titleLarge)
                        Button(
                            onClick = { showCreateDialog = true },
                            modifier = Modifier.testTag("create_playlist_btn")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Buat")
                        }
                    }
                }

                if (playlists.isEmpty()) {
                    item {
                        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text("Belum ada playlist. Tekan 'Buat' atau impor dari Spotify / YouTube Music.")
                        }
                    }
                } else {
                    items(playlists, key = { it.id }) { pl ->
                        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(pl.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text(
                                            "${pl.trackCount} lagu • Kode: ${pl.shareCode}" +
                                                if (pl.isOfflineReady) " • Offline ✓" else "",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    // "+" Plus button on playlist page to add songs!
                                    IconButton(
                                        onClick = { addTrackTargetPlaylist = pl },
                                        modifier = Modifier.testTag("playlist_add_song_${pl.id}")
                                    ) {
                                        Icon(Icons.Rounded.Add, contentDescription = "Tambah lagu ke playlist")
                                    }
                                    IconButton(onClick = { onDownloadPlaylist(pl) }) {
                                        Icon(Icons.Rounded.Download, contentDescription = "Download playlist offline")
                                    }
                                    IconButton(onClick = { onSharePlaylistLink(pl) }) {
                                        Icon(Icons.Rounded.Share, contentDescription = "Bagikan tautan playlist")
                                    }
                                    IconButton(onClick = { onDeletePlaylist(pl.id) }) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "Hapus playlist")
                                    }
                                }

                                pl.tracks.forEach { t ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${t.title} — ${t.artist}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(onClick = { onPlayTrack(t, pl.tracks) }) {
                                            Icon(Icons.Rounded.PlayArrow, contentDescription = "Putar lagu")
                                        }
                                        IconButton(onClick = { onRemoveTrackFromPlaylist(pl.id, t.id) }) {
                                            Icon(Icons.Rounded.Delete, contentDescription = "Keluarkan lagu")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Musik Offline & Perangkat", style = MaterialTheme.typography.titleLarge)
                        OutlinedButton(
                            onClick = { localAudioPicker.launch(arrayOf("audio/*")) },
                            modifier = Modifier.testTag("pick_local_audio_btn")
                        ) {
                            Icon(Icons.Rounded.AudioFile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Audio Lokal")
                        }
                    }
                }
                if (downloadedTracks.isEmpty()) {
                    item {
                        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text("Belum ada lagu yang diunduh. Unduh lagu atau playlist agar dapat diputar tanpa internet.")
                        }
                    }
                } else {
                    items(downloadedTracks, key = { it.id }) { track ->
                        LiquidGlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onPlayTrack(track, downloadedTracks) }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.DownloadDone,
                                    contentDescription = "Siap Offline",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(track.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                                    Text("${track.artist} • Offline Penuh", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { onPlayTrack(track, downloadedTracks) }) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = "Putar offline")
                                }
                                IconButton(onClick = { onRemoveDownload(track.id) }) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "Hapus unduhan")
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                item {
                    Text("Antrean Pemutaran (${queue.size})", style = MaterialTheme.typography.titleLarge)
                }
                if (queue.isEmpty()) {
                    item {
                        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text("Antrean kosong. Buka piringan pemutar lalu geser lagu ke kanan untuk menambahkan antrean.")
                        }
                    }
                } else {
                    items(queue, key = { it.id }) { track ->
                        LiquidGlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onPlayTrack(track, queue) }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(track.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                                    Text(track.artist, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { onRemoveFromQueue(track.id) }) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "Hapus dari antrean")
                                }
                            }
                        }
                    }
                }
            }

            3 -> {
                item {
                    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Link, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Impor Playlist Spotify / YouTube Music", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            OutlinedTextField(
                                value = externalLinkInput,
                                onValueChange = { externalLinkInput = it },
                                label = { Text("Tempel tautan Spotify / YT Music atau daftar lagu") },
                                placeholder = { Text("https://open.spotify.com/playlist/... atau Artis - Judul") },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("import_playlist_input")
                            )
                            Button(
                                onClick = {
                                    onImportExternalPlaylist(externalLinkInput)
                                    externalLinkInput = ""
                                },
                                enabled = externalLinkInput.isNotBlank(),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Impor & Cocokkan Audio Penuh")
                            }
                        }
                    }
                }

                item {
                    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.CompareArrows, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Pencocokan Playlist dengan Link", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            OutlinedTextField(
                                value = matchLinkInput,
                                onValueChange = { matchLinkInput = it },
                                label = { Text("Tempel tautan / kode playlist teman") },
                                placeholder = { Text("https://musica.xenproject.app/playlist/MSC-...") },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            val myFirst = playlists.firstOrNull()
                            Button(
                                onClick = {
                                    if (myFirst != null) {
                                        onMatchPlaylistByLink(myFirst, matchLinkInput)
                                    }
                                },
                                enabled = myFirst != null && matchLinkInput.isNotBlank(),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    if (myFirst != null) "Cocokkan dengan '${myFirst.name}'" else "Buat 1 playlist dulu untuk mencocokkan"
                                )
                            }
                            if (myFirst != null) {
                                OutlinedButton(
                                    onClick = {
                                        PosterRenderer.shareImageOrText(
                                            context,
                                            null,
                                            "Cocokkan playlist kamu denganku di Musica (XenProject)! Kode: ${myFirst.shareCode}"
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Rounded.Share, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Sebarkan Link Playlist Saya")
                                }
                            }
                        }
                    }
                }

                if (matchResult != null) {
                    item {
                        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "Skor Kecocokan: ${matchResult.matchPercent}%",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text("${matchResult.myPlaylistName} × ${matchResult.friendPlaylistName}")
                                if (matchResult.sharedArtists.isNotEmpty()) {
                                    Text("Artis sama: ${matchResult.sharedArtists.joinToString(", ")}")
                                }
                                TextButton(onClick = onClearMatchResult) { Text("Tutup Hasil") }
                            }
                        }
                    }
                }
            }

            4 -> {
                item {
                    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Musica AI Assistant", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Tanya tentang musik, lirik, artis, atau fitur aplikasi Musica.", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
                items(aiChatHistory) { msg ->
                    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text(
                                text = if (msg.isUser) "Anda" else "Musica AI",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(msg.text, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
                if (aiState is AiUiState.Streaming) {
                    item {
                        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text(aiState.text, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                } else if (aiState is AiUiState.Loading) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Musica AI sedang berpikir...")
                        }
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = aiInput,
                            onValueChange = { aiInput = it },
                            placeholder = { Text("Tanya soal lagu, makna lirik, atau fitur...") },
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ai_question_input")
                        )
                        Button(
                            onClick = {
                                onAskAi(aiInput)
                                aiInput = ""
                            },
                            enabled = aiInput.isNotBlank() && aiState !is AiUiState.Loading,
                            modifier = Modifier.testTag("ai_send_btn")
                        ) {
                            Icon(Icons.Rounded.Send, contentDescription = "Kirim ke AI")
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var plName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Buat Playlist Baru") },
            text = {
                OutlinedTextField(
                    value = plName,
                    onValueChange = { plName = it },
                    label = { Text("Nama Playlist") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (plName.isNotBlank()) {
                            onCreatePlaylist(plName)
                            showCreateDialog = false
                        }
                    }
                ) { Text("Buat") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Batal") }
            }
        )
    }

    if (addTrackTargetPlaylist != null) {
        val target = addTrackTargetPlaylist!!
        AlertDialog(
            onDismissRequest = { addTrackTargetPlaylist = null },
            title = { Text("Tambah Lagu ke '${target.name}'") },
            text = {
                LazyColumn(modifier = Modifier.height(280.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(catalogTracks.take(15), key = { it.id }) { t ->
                        OutlinedButton(
                            onClick = {
                                onAddTrackToPlaylist(target.id, target.name, t)
                                addTrackTargetPlaylist = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("${t.title} — ${t.artist}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { addTrackTargetPlaylist = null }) { Text("Tutup") }
            }
        )
    }
}
