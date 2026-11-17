package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.LyricLine
import com.example.data.model.TrackItem
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.LiquidGlassPill
import com.example.ui.theme.streakMilestoneColors
import com.example.ui.theme.streakMilestoneLabel
import com.example.util.PosterRenderer
import com.example.util.PosterStylePreset
import kotlinx.coroutines.launch

@Composable
fun PosterStudioScreen(
    track: TrackItem?,
    lyrics: List<LyricLine>,
    streakDays: Int,
    onPostToStory: (mediaType: String, caption: String, track: TrackItem?, streakCount: Int) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedPreset by remember { mutableStateOf(PosterStylePreset.LIQUID_CYAN) }
    var isStreakPadMode by remember { mutableStateOf(false) }
    var customQuote by remember(track?.id, lyrics) {
        mutableStateOf(lyrics.firstOrNull()?.text ?: "Rasakan musik penuh dalam balutan Liquid Glass.")
    }
    var customTitle by remember(track?.id) {
        mutableStateOf(track?.title ?: "Musica Special Mix")
    }
    var customArtist by remember(track?.id) {
        mutableStateOf(track?.artist ?: "XenProject")
    }
    var isSaving by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 74.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Studio Poster Lirik & Streak Pad", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }

        // Live Poster Preview Card
        item {
            val streakColors = streakMilestoneColors(streakDays)
            val cardBrush = if (isStreakPadMode && streakDays >= 3) {
                Brush.linearGradient(streakColors.map { it.copy(alpha = 0.85f) })
            } else {
                Brush.linearGradient(
                    listOf(Color(selectedPreset.startHex), Color(selectedPreset.endHex))
                )
            }
            val textColor = Color(selectedPreset.textHex)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(30.dp))
                    .background(cardBrush)
                    .border(1.5.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(30.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (isStreakPadMode) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, tint = textColor)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "STREAK PAD • ${streakMilestoneLabel(streakDays)}",
                                style = MaterialTheme.typography.labelLarge,
                                color = textColor
                            )
                        }
                        Text(
                            text = "$streakDays HARI",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    } else {
                        Text(
                            text = "MUSICA • NOW PLAYING",
                            style = MaterialTheme.typography.labelMedium,
                            color = textColor.copy(alpha = 0.8f)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.35f))
                            .border(2.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }

                    Text(
                        text = customTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = customArtist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "“$customQuote”",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Musica • Creator by XenProject",
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor.copy(alpha = 0.72f)
                    )
                }
            }
        }

        // Edit Fields & Presets
        item {
            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Mode Streak Pad (Warna Milestone)", style = MaterialTheme.typography.titleMedium)
                        Switch(
                            checked = isStreakPadMode,
                            onCheckedChange = { isStreakPadMode = it },
                            modifier = Modifier.testTag("streak_pad_switch")
                        )
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(PosterStylePreset.entries) { preset ->
                            LiquidGlassPill(
                                selected = selectedPreset == preset,
                                onClick = { selectedPreset = preset }
                            ) {
                                Text(preset.label, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }

                    if (lyrics.isNotEmpty()) {
                        Text("Pilih Baris Lirik:", style = MaterialTheme.typography.labelLarge)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(lyrics.take(10)) { line ->
                                LiquidGlassPill(
                                    selected = customQuote == line.text,
                                    onClick = { customQuote = line.text }
                                ) {
                                    Text(line.text.take(26), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = customTitle,
                        onValueChange = { customTitle = it },
                        label = { Text("Judul di Poster") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = customQuote,
                        onValueChange = { customQuote = it },
                        label = { Text("Teks Lirik / Kutipan Poster") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                scope.launch {
                                    isSaving = true
                                    val res = PosterRenderer.renderAndSavePoster(
                                        context = context,
                                        title = customTitle,
                                        artist = customArtist,
                                        lyricQuote = customQuote,
                                        preset = selectedPreset,
                                        isStreakPad = isStreakPadMode,
                                        streakDays = streakDays
                                    )
                                    isSaving = false
                                    res.onSuccess { onShowMessage("Poster tersimpan ke Galeri (Pictures/Musica)!") }
                                        .onFailure { onShowMessage(it.localizedMessage ?: "Gagal menyimpan poster") }
                                }
                            },
                            enabled = !isSaving,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("download_poster_btn")
                        ) {
                            Icon(Icons.Rounded.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Download")
                        }

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val res = PosterRenderer.renderAndSavePoster(
                                        context = context,
                                        title = customTitle,
                                        artist = customArtist,
                                        lyricQuote = customQuote,
                                        preset = selectedPreset,
                                        isStreakPad = isStreakPadMode,
                                        streakDays = streakDays
                                    )
                                    val uri = res.getOrNull()
                                    PosterRenderer.shareImageOrText(
                                        context = context,
                                        imageUri = uri,
                                        text = "$customTitle - $customArtist: \"$customQuote\" via Musica (XenProject)"
                                    )
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("share_poster_btn")
                        ) {
                            Icon(Icons.Rounded.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Bagikan")
                        }
                    }

                    Button(
                        onClick = {
                            onPostToStory(
                                if (isStreakPadMode) "STREAK_PAD" else "LYRIC",
                                customQuote,
                                track,
                                streakDays
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("post_poster_to_story_btn")
                    ) {
                        Icon(Icons.Rounded.AutoStories, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Post ke Stories Musica")
                    }
                }
            }
        }
    }
}
