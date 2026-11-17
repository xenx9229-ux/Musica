package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.data.local.LocalAppState
import com.example.ui.theme.GlassMode
import com.example.ui.theme.GlassThemeConfig
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.LiquidGlassPill
import com.example.ui.theme.StreakAvatarFrame
import com.example.ui.theme.parseColorHex
import com.example.ui.theme.streakMilestoneColors
import com.example.ui.theme.streakMilestoneLabel
import com.example.util.PosterRenderer

@Composable
fun ProfileThemeScreen(
    localState: LocalAppState,
    cloudEmail: String?,
    cloudUid: String?,
    onSaveProfile: (username: String, displayName: String, bio: String, avatarIdx: Int, border: String, frame: String) -> Unit,
    onUpdateTheme: (GlassThemeConfig) -> Unit,
    onPreviewMilestone: (Int) -> Unit,
    onOpenPosterStudio: () -> Unit,
    onSignOutOrSwitch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var username by remember(localState.guestUsername) { mutableStateOf(localState.guestUsername) }
    var displayName by remember(localState.guestDisplayName) { mutableStateOf(localState.guestDisplayName) }
    var bio by remember(localState.guestBio) { mutableStateOf(localState.guestBio) }
    var avatarIdx by remember(localState.guestAvatarIndex) { mutableIntStateOf(localState.guestAvatarIndex) }
    var borderStyle by remember(localState.guestBorderStyle) { mutableStateOf(localState.guestBorderStyle) }
    var frameStyle by remember(localState.guestFrameStyle) { mutableStateOf(localState.guestFrameStyle) }

    val avatarGlyphs = remember { listOf("🎧", "💿", "⚡", "🌙", "🔥", "🎹", "🚀", "💎") }
    val borderOptions = remember {
        listOf(
            "streak_auto" to "Streak Auto",
            "neon_cyan" to "Cyan Neon",
            "neon_rose" to "Rose Pulse",
            "royal_gold" to "Royal Gold",
            "aurora_emerald" to "Aurora"
        )
    }
    val frameOptions = remember {
        listOf(
            "liquid_halo" to "Liquid Halo",
            "vinyl_ring" to "Vinyl Ring",
            "prism_crest" to "Prism Crest",
            "minimal_glass" to "Minimal"
        )
    }
    val themeConfig = localState.themeConfig

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 74.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Profile Card with Avatar, Border, Frame
        item {
            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StreakAvatarFrame(
                        streakDays = localState.appStreakDays,
                        borderStyle = borderStyle,
                        modifier = Modifier.size(92.dp)
                    ) {
                        Text(
                            text = avatarGlyphs.getOrElse(avatarIdx) { "🎧" },
                            style = MaterialTheme.typography.displaySmall
                        )
                    }
                    Text(displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        text = "@$username • Frame: $frameStyle" +
                            if (!cloudEmail.isNullOrBlank()) "\n$cloudEmail" else " (Akun Guest)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(avatarGlyphs.indices.toList()) { idx ->
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (avatarIdx == idx) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                    )
                                    .clickable { avatarIdx = idx },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(avatarGlyphs[idx])
                            }
                        }
                    }

                    Text("Pilih Border & Bingkai Profil:", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(borderOptions) { (key, label) ->
                            LiquidGlassPill(selected = borderStyle == key, onClick = { borderStyle = key }) {
                                Text(label, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(frameOptions) { (key, label) ->
                            LiquidGlassPill(selected = frameStyle == key, onClick = { frameStyle = key }) {
                                Text(label, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Nama Tampilan") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = bio,
                        onValueChange = { bio = it },
                        label = { Text("Bio") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { onSaveProfile(username, displayName, bio, avatarIdx, borderStyle, frameStyle) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_profile_btn")
                    ) {
                        Text("Simpan Profil & Bingkai")
                    }
                }
            }
        }

        // Streak & Milestone Pad Card (3 days ignition + 10/20/50/100/200/300/400+)
        item {
            val streakColors = streakMilestoneColors(localState.appStreakDays)
            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(streakColors)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Streak Musica: ${localState.appStreakDays} Hari",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (localState.appStreakDays >= 3) {
                                    "Aktif (${streakMilestoneLabel(localState.appStreakDays)})"
                                } else {
                                    "Gunakan aplikasi 3 hari berturut-turut agar menyala!"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Text("Pratinjau Warna Milestone Streak (10–400+):", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf(3, 10, 20, 50, 100, 200, 300, 400)) { m ->
                            LiquidGlassPill(
                                selected = localState.appStreakDays >= m,
                                onClick = { onPreviewMilestone(m) }
                            ) {
                                Text("${m}d", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = onOpenPosterStudio, modifier = Modifier.weight(1f)) {
                            Text("Buka Streak Pad")
                        }
                        OutlinedButton(
                            onClick = {
                                PosterRenderer.shareImageOrText(
                                    context,
                                    null,
                                    "🔥 Streak Musica saya sudah ${localState.appStreakDays} hari (${streakMilestoneLabel(localState.appStreakDays)})! Musica by XenProject"
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Bagikan")
                        }
                    }
                }
            }
        }

        // 2-Mode iOS Liquid Glass Customizer
        item {
            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Atur Tema Liquid Glass (2 Mode)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        GlassMode.entries.forEach { mode ->
                            LiquidGlassPill(
                                selected = themeConfig.glassMode == mode,
                                onClick = { onUpdateTheme(themeConfig.copy(glassMode = mode)) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("glass_mode_${mode.name}")
                            ) {
                                Text(
                                    text = if (mode == GlassMode.IOS_FROST) "iOS Frost" else "Midnight Neon",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }

                    Text("Intensitas Transparansi Kaca (${(themeConfig.frostOpacity * 100).toInt()}%)")
                    Slider(
                        value = themeConfig.frostOpacity,
                        onValueChange = { onUpdateTheme(themeConfig.copy(frostOpacity = it)) },
                        valueRange = 0.14f..0.48f
                    )

                    Text("Kelengkungan Kaca (${themeConfig.cornerSoftnessDp.toInt()} dp)")
                    Slider(
                        value = themeConfig.cornerSoftnessDp,
                        onValueChange = { onUpdateTheme(themeConfig.copy(cornerSoftnessDp = it)) },
                        valueRange = 14f..34f
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Warna Adaptif Sampul Album")
                        Switch(
                            checked = themeConfig.adaptiveAlbumColor,
                            onCheckedChange = { onUpdateTheme(themeConfig.copy(adaptiveAlbumColor = it)) }
                        )
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(listOf("#38BDF8", "#8B5CF6", "#F43F5E", "#10B981", "#F59E0B", "#2563EB")) { hex ->
                            val c = parseColorHex(hex)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .border(
                                        width = if (themeConfig.accentHex == hex) 3.dp else 1.dp,
                                        color = Color.White,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        onUpdateTheme(themeConfig.copy(accentHex = hex, adaptiveAlbumColor = false))
                                    }
                            )
                        }
                    }
                }
            }
        }

        // Account & Creator Attribution
        item {
            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Musica • Creator by XenProject", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = {
                            if (cloudUid != null) {
                                performCloudSignOut(credentialManager, scope) {
                                    onSignOutOrSwitch()
                                }
                            } else {
                                onSignOutOrSwitch()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("signout_switch_btn")
                    ) {
                        Icon(Icons.Rounded.Logout, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (cloudUid != null) "Keluar Akun Google" else "Masuk / Ganti Akun")
                    }
                }
            }
        }
    }
}
