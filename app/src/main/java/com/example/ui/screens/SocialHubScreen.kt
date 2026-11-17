package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Tag
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ChatMessageDoc
import com.example.data.model.MusicRoomDoc
import com.example.data.model.PublicProfileDoc
import com.example.data.model.StoryItemDoc
import com.example.data.model.TrackItem
import com.example.ui.theme.IosAmber
import com.example.ui.theme.IosCyan
import com.example.ui.theme.IosEmerald
import com.example.ui.theme.IosRose
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.LiquidGlassPill
import com.example.ui.theme.StreakAvatarFrame
import com.example.ui.theme.streakMilestoneColors
import com.example.ui.viewmodel.CloudUiState
import com.example.ui.viewmodel.SocialCloudViewModel
import com.example.util.PosterRenderer

@Composable
fun SocialHubScreen(
    socialVm: SocialCloudViewModel?,
    myUsername: String,
    appStreakDays: Int,
    currentTrack: TrackItem?,
    onPlayTrack: (TrackItem) -> Unit,
    onRequestGoogleSignIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (socialVm == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 90.dp),
            contentAlignment = Alignment.Center
        ) {
            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp)
                    )
                    Text(
                        "Fitur Sosial Cloud & Discord Rooms",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Anda sedang menggunakan Akun Guest. Masuk dengan Google untuk membuka Stories ala IG, Mutual Streak ala TikTok, Chat Bubble yang bisa diedit, dan Music Listening Room ala Discord.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = onRequestGoogleSignIn,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("social_upgrade_signin_btn")
                    ) {
                        Text("Hubungkan Google Sign-In")
                    }
                }
            }
        }
        return
    }

    val context = LocalContext.current
    val storiesState by socialVm.stories.collectAsStateWithLifecycle()
    val postsState by socialVm.feedPosts.collectAsStateWithLifecycle()
    val usersState by socialVm.discoverableUsers.collectAsStateWithLifecycle()
    val chatsState by socialVm.chats.collectAsStateWithLifecycle()
    val activeChat by socialVm.activeChat.collectAsStateWithLifecycle()
    val chatMessages by socialVm.activeChatMessages.collectAsStateWithLifecycle()
    val roomsState by socialVm.publicRooms.collectAsStateWithLifecycle()
    val activeRoomId by socialVm.activeRoomId.collectAsStateWithLifecycle()
    val roomMessages by socialVm.activeRoomMessages.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0=Stories/Feed, 1=Chat & Streak, 2=Discord Rooms
    var showCreateStoryDialog by remember { mutableStateOf(false) }
    var viewingStory by remember { mutableStateOf<StoryItemDoc?>(null) }
    var editingBubble by remember { mutableStateOf<ChatMessageDoc?>(null) }
    var showCreateRoomDialog by remember { mutableStateOf(false) }
    var showJoinRoomDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 74.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val tabs = listOf("Stories & Feed", "Chat & Streak", "Music Rooms")
                itemsIndexed(tabs) { idx, label ->
                    LiquidGlassPill(
                        selected = activeTab == idx,
                        onClick = { activeTab = idx },
                        modifier = Modifier.testTag("social_tab_$idx")
                    ) {
                        Text(label, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        when (activeTab) {
            0 -> {
                // Stories Tray
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Stories Musica", style = MaterialTheme.typography.titleLarge)
                        Button(
                            onClick = { showCreateStoryDialog = true },
                            modifier = Modifier.testTag("create_story_btn")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Buat Story")
                        }
                    }
                }

                item {
                    when (val st = storiesState) {
                        is CloudUiState.Loading -> CircularProgressIndicator()
                        is CloudUiState.Error -> Text(st.message, color = MaterialTheme.colorScheme.tertiary)
                        is CloudUiState.Success -> {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(st.data, key = { it.id }) { story ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .width(76.dp)
                                            .clickable { viewingStory = story }
                                    ) {
                                        StreakAvatarFrame(
                                            streakDays = story.streakCount.toInt(),
                                            modifier = Modifier.size(66.dp)
                                        ) {
                                            Text(
                                                text = story.authorUsername.take(2).uppercase(),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "@${story.authorUsername}",
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Text("Posting Ulang & Aktivitas", style = MaterialTheme.typography.titleLarge)
                }

                when (val ps = postsState) {
                    is CloudUiState.Loading -> item { CircularProgressIndicator() }
                    is CloudUiState.Error -> item { Text(ps.message, color = MaterialTheme.colorScheme.tertiary) }
                    is CloudUiState.Success -> {
                        if (ps.data.isEmpty()) {
                            item {
                                LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                                    Text("Belum ada repost lagu. Putar lagu di Player dan ketuk ikon Bagikan/Repost!")
                                }
                            }
                        } else {
                            items(ps.data, key = { it.id }) { post ->
                                LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                "@${post.authorUsername}",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text("Repost Musik", style = MaterialTheme.typography.labelSmall)
                                        }
                                        Text(post.caption, style = MaterialTheme.typography.bodyLarge)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                                .clickable {
                                                    onPlayTrack(
                                                        TrackItem(
                                                            id = post.trackId,
                                                            title = post.trackTitle,
                                                            artist = post.trackArtist,
                                                            coverUrl = post.coverUrl,
                                                            streamUrl = "https://api.audius.co/v1/tracks/${post.trackId}/stream?app_name=Musica"
                                                        )
                                                    )
                                                }
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Rounded.PlayArrow, contentDescription = "Putar")
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(post.trackTitle, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                                                Text(post.trackArtist, style = MaterialTheme.typography.labelSmall)
                                            }
                                            IconButton(onClick = { socialVm.likePost(post.id) }) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Rounded.Favorite, contentDescription = "Suka", tint = IosRose, modifier = Modifier.size(18.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("${post.likesCount}", style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                // Direct Messages + Mutual Streak + Editable Chat Bubbles
                val currentChat = activeChat
                if (currentChat == null) {
                    item {
                        Text("Chat & Streak Teman (Mirip TikTok)", style = MaterialTheme.typography.titleLarge)
                    }
                    item {
                        when (val us = usersState) {
                            is CloudUiState.Success -> {
                                val others = us.data.filter { it.userId != socialVm.currentUserId }
                                if (others.isNotEmpty()) {
                                    Text("Mulai Chat / Streak Baru:", style = MaterialTheme.typography.labelLarge)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(others, key = { it.userId }) { user ->
                                            LiquidGlassPill(
                                                onClick = { socialVm.startChatWithUser(myUsername, user) }
                                            ) {
                                                Text("@${user.username}")
                                            }
                                        }
                                    }
                                }
                            }
                            else -> Unit
                        }
                    }

                    when (val cs = chatsState) {
                        is CloudUiState.Loading -> item { CircularProgressIndicator() }
                        is CloudUiState.Error -> item { Text(cs.message, color = MaterialTheme.colorScheme.tertiary) }
                        is CloudUiState.Success -> {
                            if (cs.data.isEmpty()) {
                                item {
                                    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                                        Text("Belum ada obrolan. Pilih pengguna di atas atau cari @username di Discover untuk memulai Streak harian!")
                                    }
                                }
                            } else {
                                items(cs.data, key = { it.id }) { chat ->
                                    LiquidGlassCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = { socialVm.selectChat(chat) }
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Rounded.Forum, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(chat.participantNames.joinToString(" × "), style = MaterialTheme.typography.titleMedium)
                                                Text(chat.lastMessage, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .background(
                                                        Brush.horizontalGradient(streakMilestoneColors(chat.mutualStreak.toInt()))
                                                    )
                                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("${chat.mutualStreak}", style = MaterialTheme.typography.labelMedium, color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    item {
                        var msgText by remember { mutableStateOf("") }
                        var bubbleStyle by remember { mutableStateOf("frost_cyan") }
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            LiquidGlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 10.dp) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { socialVm.selectChat(null) }) {
                                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Kembali")
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(currentChat.participantNames.joinToString(" × "), style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            "🔥 Mutual Streak: ${currentChat.mutualStreak} hari",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            chatMessages.forEach { msg ->
                                val isMine = msg.senderId == socialVm.currentUserId
                                val bubbleColor = when (msg.bubbleStyle) {
                                    "neon_rose" -> IosRose.copy(alpha = 0.28f)
                                    "royal_gold" -> IosAmber.copy(alpha = 0.28f)
                                    "aurora_emerald" -> IosEmerald.copy(alpha = 0.28f)
                                    else -> IosCyan.copy(alpha = 0.26f)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(bubbleColor)
                                            .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                                            .padding(horizontal = 14.dp, vertical = 10.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(msg.text, style = MaterialTheme.typography.bodyLarge)
                                            if (isMine) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    imageVector = Icons.Rounded.Edit,
                                                    contentDescription = "Edit bubble",
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .clickable { editingBubble = msg }
                                                        .testTag("edit_bubble_${msg.id}")
                                                )
                                            }
                                        }
                                        if (msg.edited) {
                                            Text(
                                                text = "diedit",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                val styles = listOf(
                                    "frost_cyan" to "Cyan",
                                    "neon_rose" to "Rose",
                                    "royal_gold" to "Gold",
                                    "aurora_emerald" to "Emerald"
                                )
                                items(styles) { (key, label) ->
                                    LiquidGlassPill(
                                        selected = bubbleStyle == key,
                                        onClick = { bubbleStyle = key }
                                    ) {
                                        Text("Bubble $label", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = msgText,
                                    onValueChange = { msgText = it },
                                    placeholder = { Text("Kirim pesan untuk jaga streak...") },
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("chat_message_input")
                                )
                                Button(
                                    onClick = {
                                        socialVm.sendChatMessage(myUsername, msgText, bubbleStyle)
                                        msgText = ""
                                    },
                                    enabled = msgText.isNotBlank()
                                ) {
                                    Icon(Icons.Rounded.Send, contentDescription = "Kirim pesan")
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // Discord-style Listening Rooms
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Discord-Style Music Rooms", style = MaterialTheme.typography.titleLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick = { showJoinRoomDialog = true }) {
                                Icon(Icons.Rounded.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Gabung")
                            }
                            Button(
                                onClick = { showCreateRoomDialog = true },
                                modifier = Modifier.testTag("create_room_btn")
                            ) {
                                Text("+ Room")
                            }
                        }
                    }
                }

                when (val rs = roomsState) {
                    is CloudUiState.Loading -> item { CircularProgressIndicator() }
                    is CloudUiState.Error -> item { Text(rs.message, color = MaterialTheme.colorScheme.tertiary) }
                    is CloudUiState.Success -> {
                        val activeRoom = rs.data.firstOrNull { it.id == activeRoomId }
                        if (activeRoom == null) {
                            items(rs.data, key = { it.id }) { room ->
                                DiscordRoomSummaryCard(
                                    room = room,
                                    onEnter = { socialVm.selectRoom(room.id) }
                                )
                            }
                        } else {
                            item {
                                DiscordActiveRoomStage(
                                    room = activeRoom,
                                    messages = roomMessages,
                                    currentTrack = currentTrack,
                                    onBack = { socialVm.selectRoom(null) },
                                    onSyncMyTrack = {
                                        if (currentTrack != null) {
                                            socialVm.syncRoomTrack(activeRoom.id, currentTrack, true)
                                        }
                                    },
                                    onListenRoomTrack = {
                                        if (activeRoom.currentTrackId.isNotBlank()) {
                                            onPlayTrack(
                                                TrackItem(
                                                    id = activeRoom.currentTrackId,
                                                    title = activeRoom.currentTrackTitle,
                                                    artist = activeRoom.currentTrackArtist,
                                                    coverUrl = activeRoom.currentTrackCover,
                                                    streamUrl = activeRoom.currentTrackStreamUrl
                                                )
                                            )
                                        }
                                    },
                                    onInviteLink = {
                                        PosterRenderer.shareImageOrText(
                                            context,
                                            null,
                                            "Gabung ke Listening Room '${activeRoom.roomName}' di Musica! Kode/Link: https://musica.xenproject.app/room/${activeRoom.inviteCode}"
                                        )
                                    },
                                    onSendMessage = { txt -> socialVm.sendRoomMessage(activeRoom.id, myUsername, txt) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateStoryDialog) {
        CreateStoryDialog(
            currentTrack = currentTrack,
            streakDays = appStreakDays,
            onDismiss = { showCreateStoryDialog = false },
            onPublish = { mediaType, caption ->
                socialVm.publishStory(myUsername, mediaType, caption, currentTrack, appStreakDays, "#38BDF8")
                showCreateStoryDialog = false
            }
        )
    }

    if (viewingStory != null) {
        val s = viewingStory!!
        AlertDialog(
            onDismissRequest = { viewingStory = null },
            title = { Text("@${s.authorUsername} • ${s.mediaType}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(s.caption, style = MaterialTheme.typography.titleMedium)
                    if (s.trackTitle.isNotBlank()) {
                        Text("🎵 ${s.trackTitle} — ${s.trackArtist}", color = MaterialTheme.colorScheme.primary)
                    }
                    if (s.streakCount > 0) {
                        Text("🔥 Streak: ${s.streakCount} hari")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewingStory = null }) { Text("Tutup") }
            }
        )
    }

    if (editingBubble != null) {
        val b = editingBubble!!
        var newText by remember(b.id) { mutableStateOf(b.text) }
        var newStyle by remember(b.id) { mutableStateOf(b.bubbleStyle) }
        AlertDialog(
            onDismissRequest = { editingBubble = null },
            title = { Text("Edit Chat Bubble") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newText,
                        onValueChange = { newText = it },
                        label = { Text("Teks Pesan") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("frost_cyan" to "Cyan", "neon_rose" to "Rose", "royal_gold" to "Gold").forEach { (k, label) ->
                            LiquidGlassPill(selected = newStyle == k, onClick = { newStyle = k }) {
                                Text(label, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    socialVm.editChatBubble(b.id, newText, newStyle)
                    editingBubble = null
                }) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { editingBubble = null }) { Text("Batal") }
            }
        )
    }

    if (showCreateRoomDialog) {
        var rName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateRoomDialog = false },
            title = { Text("Buat Listening Room (Discord-Style)") },
            text = {
                OutlinedTextField(
                    value = rName,
                    onValueChange = { rName = it },
                    label = { Text("Nama Room / Channel") },
                    placeholder = { Text("contoh: #midnight-chill-stage") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    socialVm.createRoom(myUsername, rName, currentTrack)
                    showCreateRoomDialog = false
                }) { Text("Buat") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateRoomDialog = false }) { Text("Batal") }
            }
        )
    }

    if (showJoinRoomDialog) {
        var code by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showJoinRoomDialog = false },
            title = { Text("Gabung Room via Link / Kode") },
            text = {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Kode Undangan atau Link Room") },
                    placeholder = { Text("XEN-12345") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    socialVm.joinRoomByCode(code)
                    showJoinRoomDialog = false
                }) { Text("Gabung") }
            },
            dismissButton = {
                TextButton(onClick = { showJoinRoomDialog = false }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun CreateStoryDialog(
    currentTrack: TrackItem?,
    streakDays: Int,
    onDismiss: () -> Unit,
    onPublish: (mediaType: String, caption: String) -> Unit
) {
    var mediaType by remember { mutableStateOf("TRACK") }
    var caption by remember { mutableStateOf("") }
    var pickedPhotoUri by remember { mutableStateOf<String?>(null) }
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                pickedPhotoUri = uri.toString()
                mediaType = "PHOTO"
            }
        }
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Buat Story Musica") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("TRACK", "LYRIC", "STREAK_PAD", "PHOTO")) { t ->
                        LiquidGlassPill(selected = mediaType == t, onClick = { mediaType = t }) {
                            Text(t, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                OutlinedButton(
                    onClick = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Image, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (pickedPhotoUri != null) "Foto Dipilih ✓" else "Pilih Foto (Photo Picker)")
                }
                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    label = { Text("Pesan / Caption Story") },
                    placeholder = {
                        Text(
                            when (mediaType) {
                                "STREAK_PAD" -> "Merayakan Streak $streakDays hari!"
                                else -> currentTrack?.title ?: "Lagi dengerin lagu keren..."
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val finalCap = caption.ifBlank {
                    if (mediaType == "STREAK_PAD") "Streak Pad $streakDays Hari di Musica!"
                    else "Mendengarkan ${currentTrack?.title ?: "Musica"}"
                }
                onPublish(mediaType, finalCap)
            }) { Text("Posting") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
private fun DiscordRoomSummaryCard(room: MusicRoomDoc, onEnter: () -> Unit) {
    LiquidGlassCard(modifier = Modifier.fillMaxWidth(), onClick = onEnter) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Tag, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(room.roomName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Host: @${room.hostName} • ${room.memberUids.size} anggota • Kode: ${room.inviteCode}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(onClick = onEnter) { Text("Masuk") }
        }
    }
}

@Composable
private fun DiscordActiveRoomStage(
    room: MusicRoomDoc,
    messages: List<com.example.data.model.RoomMessageDoc>,
    currentTrack: TrackItem?,
    onBack: () -> Unit,
    onSyncMyTrack: () -> Unit,
    onListenRoomTrack: () -> Unit,
    onInviteLink: () -> Unit,
    onSendMessage: (String) -> Unit
) {
    var msgText by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Keluar tampilan room")
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("# ${room.roomName}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Kode Undangan: ${room.inviteCode}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onInviteLink) {
                        Icon(Icons.Rounded.Share, contentDescription = "Undang teman dengan link")
                    }
                }

                // Voice Stage Listener Avatars (Discord Lounge style)
                Text("Stage Pendengar (${room.memberUids.size})", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    itemsIndexed(room.memberUids) { idx, uid ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, IosEmerald, CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (idx == 0) Icons.Rounded.Mic else Icons.Rounded.Headphones,
                                    contentDescription = null,
                                    tint = IosEmerald
                                )
                            }
                            Text(if (idx == 0) "Host" else "Listener", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                // Now Playing in Room
                LiquidGlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 10.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = if (room.currentTrackTitle.isNotBlank()) {
                                "Sedang Diputar di Room: ${room.currentTrackTitle} — ${room.currentTrackArtist}"
                            } else "Belum ada lagu disinkronkan",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (room.currentTrackId.isNotBlank()) {
                                Button(onClick = onListenRoomTrack, modifier = Modifier.weight(1f)) {
                                    Text("Dengar Sinkron")
                                }
                            }
                            if (currentTrack != null) {
                                OutlinedButton(onClick = onSyncMyTrack, modifier = Modifier.weight(1f)) {
                                    Text("Putar Laguku di Room")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Room Text Chat
        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Obrolan Room", style = MaterialTheme.typography.titleMedium)
                if (messages.isEmpty()) {
                    Text("Belum ada pesan di channel ini.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    messages.takeLast(12).forEach { m ->
                        Text(
                            text = "@${m.senderName}: ${m.text}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = msgText,
                        onValueChange = { msgText = it },
                        placeholder = { Text("Kirim pesan ke #${room.roomName}...") },
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            onSendMessage(msgText)
                            msgText = ""
                        },
                        enabled = msgText.isNotBlank()
                    ) {
                        Icon(Icons.Rounded.Send, contentDescription = "Kirim")
                    }
                }
            }
        }
    }
}
