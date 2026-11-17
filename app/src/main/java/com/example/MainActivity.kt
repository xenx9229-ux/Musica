package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.AsyncImage
import com.example.core.configureAppCheck
import com.example.data.local.MusicaDatabase
import com.example.data.local.ThemePreferencesStore
import com.example.data.model.TrackItem
import com.example.data.repository.FirestoreSocialRepository
import com.example.data.repository.MusicCatalogRepository
import com.example.data.repository.MusicaAiRepository
import com.example.player.MusicaPlayerController
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DiscoverScreen
import com.example.ui.screens.DynamicIslandOverlay
import com.example.ui.screens.LibraryPlaylistsScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.screens.PosterStudioScreen
import com.example.ui.screens.ProfileThemeScreen
import com.example.ui.screens.SocialHubScreen
import com.example.ui.screens.attemptSilentGoogleSignIn
import com.example.ui.theme.IosCyan
import com.example.ui.theme.IosRose
import com.example.ui.theme.IosViolet
import com.example.ui.theme.LiquidGlassBackdrop
import com.example.ui.theme.MusicaTheme
import com.example.ui.viewmodel.CloudUiState
import com.example.ui.viewmodel.MusicaCoreViewModel
import com.example.ui.viewmodel.SocialCloudViewModel
import com.example.util.PosterRenderer
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

enum class MusicaTab(val label: String, val icon: ImageVector) {
    DISCOVER("Discover", Icons.Rounded.Explore),
    PLAYER("Player", Icons.Rounded.Album),
    LIBRARY("Library", Icons.Rounded.LibraryMusic),
    SOCIAL("Social", Icons.Rounded.Forum),
    PROFILE("Profil", Icons.Rounded.Person)
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth -> trySend(auth.currentUser) }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

class MainActivity : ComponentActivity() {
    private lateinit var playerController: MusicaPlayerController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // REQUIRED: Configure Firebase App Check with launch intent before Compose UI or ViewModels
        configureAppCheck(this, intent)

        playerController = MusicaPlayerController(this)

        setContent {
            MusicaRootApp(playerController = playerController)
        }
    }

    override fun onDestroy() {
        if (::playerController.isInitialized) {
            playerController.release()
        }
        super.onDestroy()
    }
}

@Composable
private fun MusicaRootApp(playerController: MusicaPlayerController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val appContext = remember(context) { context.applicationContext }
    val coreViewModel: MusicaCoreViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val db = MusicaDatabase.getInstance(appContext)
                val catalogRepo = MusicCatalogRepository(appContext, db.musicaDao())
                val prefsStore = ThemePreferencesStore(appContext)
                val aiRepo = MusicaAiRepository()
                MusicaCoreViewModel(catalogRepo, prefsStore, playerController, aiRepo)
            }
        }
    )

    val localState by coreViewModel.localAppState.collectAsStateWithLifecycle()
    val playerState by coreViewModel.playerState.collectAsStateWithLifecycle()
    val currentUser by Firebase.auth.authStateFlow()
        .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    var pendingPreferredUsername by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        if (Firebase.auth.currentUser == null && !localState.isGuestSession) {
            attemptSilentGoogleSignIn(
                context = context,
                credentialManager = credentialManager,
                scope = scope,
                onSuccess = {},
                onUnauthenticated = {}
            )
        }
    }

    val albumAccent = remember(playerState.currentTrack?.genre) {
        when (playerState.currentTrack?.genre?.lowercase()) {
            "electronic", "ambient" -> IosCyan
            "pop", "r&b/soul" -> IosRose
            "hip-hop/rap" -> IosViolet
            else -> IosCyan
        }
    }

    MusicaTheme(
        config = localState.themeConfig,
        albumAccent = albumAccent
    ) {
        val user = currentUser
        when {
            user != null -> {
                key(user.uid) {
                    val socialViewModel: SocialCloudViewModel = viewModel(
                        key = user.uid,
                        factory = viewModelFactory {
                            initializer {
                                SocialCloudViewModel(
                                    repo = FirestoreSocialRepository(appContext),
                                    currentUserId = user.uid
                                )
                            }
                        }
                    )
                    LaunchedEffect(user.uid, pendingPreferredUsername) {
                        val desiredName = pendingPreferredUsername.ifBlank {
                            user.displayName?.lowercase()?.replace(" ", "_") ?: localState.guestUsername
                        }
                        if (pendingPreferredUsername.isNotBlank()) {
                            socialViewModel.saveCloudProfile(
                                username = desiredName,
                                displayName = user.displayName ?: desiredName,
                                email = user.email.orEmpty(),
                                bio = localState.guestBio,
                                avatarIndex = localState.guestAvatarIndex,
                                borderStyle = localState.guestBorderStyle,
                                frameStyle = localState.guestFrameStyle,
                                streakDays = localState.appStreakDays
                            )
                        } else {
                            socialViewModel.ensureCloudProfileIfMissing(
                                username = desiredName,
                                displayName = user.displayName ?: desiredName,
                                email = user.email.orEmpty(),
                                bio = localState.guestBio,
                                avatarIndex = localState.guestAvatarIndex,
                                borderStyle = localState.guestBorderStyle,
                                frameStyle = localState.guestFrameStyle,
                                streakDays = localState.appStreakDays
                            )
                        }
                    }
                    MusicaMainShell(
                        coreVm = coreViewModel,
                        socialVm = socialViewModel,
                        userEmail = user.email,
                        onExitToAuth = { coreViewModel.setGuestMode(false) }
                    )
                }
            }
            localState.isGuestSession -> {
                MusicaMainShell(
                    coreVm = coreViewModel,
                    socialVm = null,
                    userEmail = null,
                    onExitToAuth = { coreViewModel.setGuestMode(false) }
                )
            }
            else -> {
                AuthScreen(
                    onGoogleAuthSuccess = { preferred ->
                        pendingPreferredUsername = preferred
                        coreViewModel.setGuestMode(false)
                    },
                    onEnterGuest = { guestName ->
                        coreViewModel.updateGuestProfile(
                            username = guestName,
                            displayName = guestName,
                            bio = localState.guestBio,
                            avatarIndex = localState.guestAvatarIndex,
                            borderStyle = localState.guestBorderStyle,
                            frameStyle = localState.guestFrameStyle
                        )
                        coreViewModel.setGuestMode(true)
                    }
                )
            }
        }
    }
}

@Composable
private fun MusicaMainShell(
    coreVm: MusicaCoreViewModel,
    socialVm: SocialCloudViewModel?,
    userEmail: String?,
    onExitToAuth: () -> Unit
) {
    val context = LocalContext.current
    val discoverState by coreVm.discoverState.collectAsStateWithLifecycle()
    val playerState by coreVm.playerState.collectAsStateWithLifecycle()
    val lyricsState by coreVm.lyricsState.collectAsStateWithLifecycle()
    val localState by coreVm.localAppState.collectAsStateWithLifecycle()
    val downloadedTracks by coreVm.downloadedTracks.collectAsStateWithLifecycle()
    val playlists by coreVm.playlists.collectAsStateWithLifecycle()
    val aiHistory by coreVm.aiChatHistory.collectAsStateWithLifecycle()
    val aiState by coreVm.aiState.collectAsStateWithLifecycle()
    val matchResult by coreVm.playlistMatchResult.collectAsStateWithLifecycle()
    val toastMessage by coreVm.statusToast.collectAsStateWithLifecycle()

    val cloudProfile by socialVm?.myProfile?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(null) }
    val cloudUsersState by socialVm?.discoverableUsers?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf(CloudUiState.Success(emptyList())) }
    val cloudFeedback by socialVm?.cloudFeedback?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf(null) }

    var activeTab by remember { mutableStateOf(MusicaTab.DISCOVER) }
    var showPosterStudio by remember { mutableStateOf(false) }
    var libraryInitialSubTab by remember { mutableIntStateOf(0) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(toastMessage) {
        if (!toastMessage.isNullOrBlank()) {
            snackbarHostState.showSnackbar(toastMessage!!)
            coreVm.clearToast()
        }
    }
    LaunchedEffect(cloudFeedback) {
        if (!cloudFeedback.isNullOrBlank()) {
            snackbarHostState.showSnackbar(cloudFeedback!!)
            socialVm?.clearFeedback()
        }
    }
    LaunchedEffect(playerState.errorMessage) {
        if (!playerState.errorMessage.isNullOrBlank()) {
            snackbarHostState.showSnackbar(playerState.errorMessage!!)
            coreVm.clearPlayerError()
        }
    }

    BackHandler(enabled = showPosterStudio || activeTab != MusicaTab.DISCOVER) {
        if (showPosterStudio) {
            showPosterStudio = false
        } else {
            activeTab = MusicaTab.DISCOVER
        }
    }

    val effectiveUsername = cloudProfile?.username?.takeIf { it.isNotBlank() } ?: localState.guestUsername
    val allCatalog = remember(discoverState.trendingTracks, discoverState.searchTracks) {
        (discoverState.searchTracks + discoverState.trendingTracks).distinctBy { it.id }
    }

    LiquidGlassBackdrop(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.padding(bottom = 112.dp)
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding())
            ) {
                if (showPosterStudio) {
                    PosterStudioScreen(
                        track = playerState.currentTrack ?: allCatalog.firstOrNull(),
                        lyrics = lyricsState.lines,
                        streakDays = localState.appStreakDays,
                        onPostToStory = { mediaType, caption, track, streak ->
                            if (socialVm != null) {
                                socialVm.publishStory(effectiveUsername, mediaType, caption, track, streak, "#38BDF8")
                            } else {
                                coreVm.showToast("Hubungkan Google Sign-In untuk posting ke Stories Cloud!")
                            }
                            showPosterStudio = false
                            activeTab = MusicaTab.SOCIAL
                        },
                        onShowMessage = { coreVm.showToast(it) }
                    )
                } else {
                    when (activeTab) {
                        MusicaTab.DISCOVER -> {
                            val usersList = (cloudUsersState as? CloudUiState.Success)?.data.orEmpty()
                            DiscoverScreen(
                                state = discoverState,
                                favoriteTrackIds = localState.favoriteTrackIds,
                                followedArtistIds = localState.followedArtistIds,
                                discoverableUsers = usersList,
                                onSearch = coreVm::performGlobalSearch,
                                onSelectGenre = coreVm::loadInitialCatalog,
                                onPlayTrack = { track, ctx ->
                                    coreVm.playTrack(track, ctx)
                                    activeTab = MusicaTab.PLAYER
                                },
                                onEnqueueTrack = coreVm::enqueueTrackRightSwipe,
                                onToggleFavorite = coreVm::toggleFavorite,
                                onDownloadTrack = coreVm::downloadTrack,
                                onSelectArtist = coreVm::selectArtist,
                                onClearArtist = coreVm::clearSelectedArtist,
                                onToggleFollowArtist = coreVm::toggleFollowArtist,
                                onOpenAiAssistant = {
                                    libraryInitialSubTab = 4
                                    activeTab = MusicaTab.LIBRARY
                                },
                                onOpenUserProfileChat = { otherUser ->
                                    socialVm?.startChatWithUser(effectiveUsername, otherUser)
                                    activeTab = MusicaTab.SOCIAL
                                }
                            )
                        }

                        MusicaTab.PLAYER -> {
                            PlayerScreen(
                                playerState = playerState,
                                lyricsState = lyricsState,
                                catalogTracks = allCatalog,
                                playlists = playlists,
                                favoriteTrackIds = localState.favoriteTrackIds,
                                followedArtistIds = localState.followedArtistIds,
                                onSearchCatalog = coreVm::performGlobalSearch,
                                onPlayTrack = coreVm::playTrack,
                                onTogglePlayPause = coreVm::togglePlayPause,
                                onSeekTo = coreVm::seekTo,
                                onNext = coreVm::skipNext,
                                onPrevious = coreVm::skipPrevious,
                                onCycleRepeat = coreVm::cycleRepeatMode,
                                onToggleShuffle = coreVm::toggleShuffle,
                                onEnqueueRightSwipe = coreVm::enqueueTrackRightSwipe,
                                onAddToPlaylist = coreVm::addTrackToPlaylist,
                                onCreatePlaylistAndAdd = { name, t -> coreVm.createPlaylist(name, t) },
                                onToggleFavorite = coreVm::toggleFavorite,
                                onToggleFollowArtist = coreVm::toggleFollowArtist,
                                onRepostTrack = { t ->
                                    if (socialVm != null) {
                                        socialVm.repostTrack(effectiveUsername, t, "Lagi suka banget lagu ini di Musica!")
                                    } else {
                                        coreVm.showToast("Masuk dengan Google untuk posting ulang ke feed!")
                                    }
                                },
                                onDownloadTrack = coreVm::downloadTrack,
                                onTranslateLyrics = coreVm::translateCurrentLyrics,
                                onSetSleepTimer = coreVm::setSleepTimer,
                                onOpenPosterStudio = { showPosterStudio = true }
                            )
                        }

                        MusicaTab.LIBRARY -> {
                            LibraryPlaylistsScreen(
                                playlists = playlists,
                                downloadedTracks = downloadedTracks,
                                queue = playerState.queue,
                                catalogTracks = allCatalog,
                                aiChatHistory = aiHistory,
                                aiState = aiState,
                                matchResult = matchResult,
                                initialSubTab = libraryInitialSubTab,
                                onCreatePlaylist = { coreVm.createPlaylist(it) },
                                onDeletePlaylist = coreVm::deletePlaylist,
                                onAddTrackToPlaylist = coreVm::addTrackToPlaylist,
                                onRemoveTrackFromPlaylist = coreVm::removeTrackFromPlaylist,
                                onDownloadPlaylist = coreVm::downloadPlaylist,
                                onRemoveDownload = coreVm::removeDownload,
                                onPlayTrack = { track, ctx ->
                                    coreVm.playTrack(track, ctx)
                                    activeTab = MusicaTab.PLAYER
                                },
                                onRemoveFromQueue = coreVm::removeFromQueue,
                                onImportLocalAudio = coreVm::importLocalAudio,
                                onImportExternalPlaylist = coreVm::importExternalPlaylist,
                                onSharePlaylistLink = { pl ->
                                    if (socialVm != null) {
                                        socialVm.sharePlaylistLink(pl, effectiveUsername) { link ->
                                            PosterRenderer.shareImageOrText(
                                                context,
                                                null,
                                                "Dengar & cocokkan playlist '${pl.name}' di Musica: $link"
                                            )
                                        }
                                    } else {
                                        PosterRenderer.shareImageOrText(
                                            context,
                                            null,
                                            "Playlist Musica '${pl.name}' (Kode: ${pl.shareCode})"
                                        )
                                    }
                                },
                                onMatchPlaylistByLink = { myPl, link ->
                                    if (socialVm != null) {
                                        socialVm.fetchPlaylistByLink(link) { sharedDoc ->
                                            coreVm.matchWithSharedPlaylist(myPl, sharedDoc)
                                        }
                                    } else {
                                        coreVm.showToast("Hubungkan Google Sign-In untuk mencocokkan playlist cloud teman!")
                                    }
                                },
                                onClearMatchResult = coreVm::clearPlaylistMatchResult,
                                onAskAi = coreVm::askMusicaAi
                            )
                        }

                        MusicaTab.SOCIAL -> {
                            SocialHubScreen(
                                socialVm = socialVm,
                                myUsername = effectiveUsername,
                                appStreakDays = localState.appStreakDays,
                                currentTrack = playerState.currentTrack,
                                onPlayTrack = { track ->
                                    coreVm.playTrack(track, listOf(track))
                                    activeTab = MusicaTab.PLAYER
                                },
                                onRequestGoogleSignIn = onExitToAuth
                            )
                        }

                        MusicaTab.PROFILE -> {
                            ProfileThemeScreen(
                                localState = localState,
                                cloudEmail = userEmail,
                                cloudUid = socialVm?.currentUserId,
                                onSaveProfile = { uname, dname, bioText, avIdx, border, frame ->
                                    coreVm.updateGuestProfile(uname, dname, bioText, avIdx, border, frame)
                                    socialVm?.saveCloudProfile(
                                        username = uname,
                                        displayName = dname,
                                        email = userEmail.orEmpty(),
                                        bio = bioText,
                                        avatarIndex = avIdx,
                                        borderStyle = border,
                                        frameStyle = frame,
                                        streakDays = localState.appStreakDays
                                    )
                                },
                                onUpdateTheme = coreVm::updateThemeConfig,
                                onPreviewMilestone = coreVm::simulateStreakMilestone,
                                onOpenPosterStudio = { showPosterStudio = true },
                                onSignOutOrSwitch = onExitToAuth
                            )
                        }
                    }
                }

                // Persistent Dynamic Island Pill at Top Safe Area
                DynamicIslandOverlay(
                    playerState = playerState,
                    lyrics = lyricsState.lines,
                    onTogglePlayPause = coreVm::togglePlayPause,
                    onNext = coreVm::skipNext,
                    onPrevious = coreVm::skipPrevious,
                    onOpenFullPlayer = {
                        showPosterStudio = false
                        activeTab = MusicaTab.PLAYER
                    },
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                // Floating iOS Liquid Glass Mini Player + Navigation Bar
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val activeTrack = playerState.currentTrack
                    if (activeTrack != null && activeTab != MusicaTab.PLAYER && !showPosterStudio) {
                        FloatingLiquidMiniPlayer(
                            track = activeTrack,
                            isPlaying = playerState.isPlaying,
                            onClick = { activeTab = MusicaTab.PLAYER },
                            onTogglePlayPause = coreVm::togglePlayPause
                        )
                    }
                    FloatingLiquidNavBar(
                        activeTab = activeTab,
                        onSelectTab = { tab ->
                            showPosterStudio = false
                            if (tab == MusicaTab.LIBRARY) libraryInitialSubTab = 0
                            activeTab = tab
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingLiquidMiniPlayer(
    track: TrackItem,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onTogglePlayPause: () -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    Row(
        modifier = Modifier
            .widthIn(max = 560.dp)
            .fillMaxWidth()
            .shadow(12.dp, shape)
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.34f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("floating_mini_player"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = track.coverUrl.ifBlank { R.drawable.img_vinyl_art },
            contentDescription = track.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(track.title, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artist, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        IconButton(onClick = onTogglePlayPause) {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = "Putar atau jeda"
            )
        }
    }
}

@Composable
private fun FloatingLiquidNavBar(
    activeTab: MusicaTab,
    onSelectTab: (MusicaTab) -> Unit
) {
    val shape = RoundedCornerShape(28.dp)
    Row(
        modifier = Modifier
            .widthIn(max = 560.dp)
            .fillMaxWidth()
            .shadow(16.dp, shape)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.86f))
            .border(1.dp, Color.White.copy(alpha = 0.32f), shape)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        MusicaTab.entries.forEach { tab ->
            val selected = activeTab == tab
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                        else Color.Transparent
                    )
                    .clickable { onSelectTab(tab) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("nav_tab_${tab.name.lowercase()}"),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = tab.label,
                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = tab.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
