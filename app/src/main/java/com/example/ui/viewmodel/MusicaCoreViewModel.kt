package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.LocalAppState
import com.example.data.local.ThemePreferencesStore
import com.example.data.model.AlbumItem
import com.example.data.model.ArtistProfile
import com.example.data.model.LyricLine
import com.example.data.model.PlaylistMatchResult
import com.example.data.model.PlaylistUi
import com.example.data.model.SharedPlaylistDoc
import com.example.data.model.SleepTimerOption
import com.example.data.model.TrackItem
import com.example.data.repository.AiUiState
import com.example.data.repository.MusicCatalogRepository
import com.example.data.repository.MusicaAiRepository
import com.example.player.MusicaPlayerController
import com.example.player.PlayerPlaybackState
import com.example.ui.theme.GlassMode
import com.example.ui.theme.GlassThemeConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DiscoverState(
    val isLoading: Boolean = true,
    val selectedGenre: String = "All",
    val searchQuery: String = "",
    val trendingTracks: List<TrackItem> = emptyList(),
    val searchTracks: List<TrackItem> = emptyList(),
    val albums: List<AlbumItem> = emptyList(),
    val artists: List<ArtistProfile> = emptyList(),
    val selectedArtistTracks: List<TrackItem> = emptyList(),
    val selectedArtist: ArtistProfile? = null,
    val errorMessage: String? = null
)

data class LyricsState(
    val trackId: String = "",
    val isLoading: Boolean = false,
    val isTranslating: Boolean = false,
    val targetLanguage: String = "Indonesia",
    val lines: List<LyricLine> = emptyList(),
    val translationError: String? = null
)

data class AiChatEntry(
    val isUser: Boolean,
    val text: String
)

class MusicaCoreViewModel(
    private val catalogRepo: MusicCatalogRepository,
    private val prefsStore: ThemePreferencesStore,
    private val playerController: MusicaPlayerController,
    private val aiRepo: MusicaAiRepository
) : ViewModel() {

    val playerState: StateFlow<PlayerPlaybackState> = playerController.state

    val localAppState: StateFlow<LocalAppState> = prefsStore.appState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = LocalAppState()
    )

    val downloadedTracks: StateFlow<List<TrackItem>> = catalogRepo.downloadedTracks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    val playlists: StateFlow<List<PlaylistUi>> = catalogRepo.playlists.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    private val _discoverState = MutableStateFlow(DiscoverState())
    val discoverState: StateFlow<DiscoverState> = _discoverState.asStateFlow()

    private val _lyricsState = MutableStateFlow(LyricsState())
    val lyricsState: StateFlow<LyricsState> = _lyricsState.asStateFlow()

    private val _aiChatHistory = MutableStateFlow<List<AiChatEntry>>(
        listOf(
            AiChatEntry(
                isUser = false,
                text = "Halo! Saya Musica AI (Creator by XenProject). Tanyakan apa saja tentang lagu yang sedang diputar, sejarah artis, rekomendasi mood, atau cara memakai fitur Musica!"
            )
        )
    )
    val aiChatHistory: StateFlow<List<AiChatEntry>> = _aiChatHistory.asStateFlow()

    private val _aiState = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val aiState: StateFlow<AiUiState> = _aiState.asStateFlow()

    private val _statusToast = MutableStateFlow<String?>(null)
    val statusToast: StateFlow<String?> = _statusToast.asStateFlow()

    private val _playlistMatchResult = MutableStateFlow<PlaylistMatchResult?>(null)
    val playlistMatchResult: StateFlow<PlaylistMatchResult?> = _playlistMatchResult.asStateFlow()

    init {
        playerController.setOnTrackChangedListener { track ->
            loadLyricsForTrack(track)
        }
        viewModelScope.launch {
            prefsStore.recordDailyCheckIn()
            loadInitialCatalog()
        }
    }

    fun clearToast() {
        _statusToast.value = null
    }

    fun showToast(message: String) {
        _statusToast.value = message
    }

    fun loadInitialCatalog(genre: String = "All") {
        viewModelScope.launch {
            _discoverState.update { it.copy(isLoading = true, selectedGenre = genre, errorMessage = null) }
            val tracks = catalogRepo.getTrending(genre)
            val albums = catalogRepo.getRecommendedAlbums()
            val artists = catalogRepo.searchArtists("electronic")
            _discoverState.update {
                it.copy(
                    isLoading = false,
                    trendingTracks = tracks,
                    albums = albums,
                    artists = artists,
                    errorMessage = if (tracks.isEmpty()) "Katalog online sedang sibuk. Cek koneksi atau putar lagu offline." else null
                )
            }
            if (playerController.state.value.currentTrack == null && tracks.isNotEmpty()) {
                // Pre-load queue without blasting audio on cold start until user taps or keep ready
                val first = tracks.first()
                loadLyricsForTrack(first)
            }
        }
    }

    fun performGlobalSearch(query: String) {
        _discoverState.update { it.copy(searchQuery = query) }
        if (query.isBlank()) {
            _discoverState.update { it.copy(searchTracks = emptyList(), selectedArtist = null) }
            return
        }
        viewModelScope.launch {
            _discoverState.update { it.copy(isLoading = true) }
            val tracks = catalogRepo.searchTracks(query)
            val artists = catalogRepo.searchArtists(query)
            val albums = catalogRepo.getRecommendedAlbums(query)
            _discoverState.update {
                it.copy(
                    isLoading = false,
                    searchTracks = tracks,
                    artists = artists,
                    albums = albums
                )
            }
        }
    }

    fun selectArtist(artist: ArtistProfile) {
        viewModelScope.launch {
            _discoverState.update { it.copy(selectedArtist = artist, isLoading = true) }
            val tracks = catalogRepo.getArtistTracks(artist.id)
            _discoverState.update { it.copy(selectedArtistTracks = tracks, isLoading = false) }
        }
    }

    fun clearSelectedArtist() {
        _discoverState.update { it.copy(selectedArtist = null, selectedArtistTracks = emptyList()) }
    }

    fun playTrack(track: TrackItem, queueContext: List<TrackItem> = emptyList()) {
        val contextList = if (queueContext.isNotEmpty()) {
            queueContext
        } else {
            (_discoverState.value.searchTracks + _discoverState.value.trendingTracks).distinctBy { it.id }
        }
        playerController.playTrack(track, contextList)
    }

    fun togglePlayPause() = playerController.togglePlayPause()
    fun seekTo(positionMs: Long) = playerController.seekTo(positionMs)
    fun skipNext() = playerController.skipNext()
    fun skipPrevious() = playerController.skipPrevious()
    fun cycleRepeatMode() = playerController.cycleRepeatMode()
    fun toggleShuffle() = playerController.toggleShuffle()

    fun enqueueTrackRightSwipe(track: TrackItem) {
        playerController.enqueueTrack(track)
        _statusToast.value = "Ditambahkan ke Antrean: ${track.title}"
    }

    fun removeFromQueue(trackId: String) = playerController.removeFromQueue(trackId)

    fun setSleepTimer(option: SleepTimerOption) {
        playerController.setSleepTimer(option)
        _statusToast.value = if (option == SleepTimerOption.OFF) {
            "Mode Sleep dimatikan"
        } else {
            "Mode Sleep aktif: ${option.label}"
        }
    }

    fun clearPlayerError() = playerController.clearError()

    fun loadLyricsForTrack(track: TrackItem) {
        viewModelScope.launch {
            _lyricsState.update {
                it.copy(
                    trackId = track.id,
                    isLoading = true,
                    translationError = null
                )
            }
            val lyrics = catalogRepo.getLyricsForTrack(track)
            _lyricsState.update {
                it.copy(
                    trackId = track.id,
                    isLoading = false,
                    lines = lyrics
                )
            }
        }
    }

    fun translateCurrentLyrics(language: String) {
        val currentLines = _lyricsState.value.lines
        if (currentLines.isEmpty()) return
        viewModelScope.launch {
            _lyricsState.update {
                it.copy(isTranslating = true, targetLanguage = language, translationError = null)
            }
            val result = aiRepo.translateLyrics(currentLines, language)
            result.onSuccess { translated ->
                _lyricsState.update { it.copy(isTranslating = false, lines = translated) }
            }.onFailure { err ->
                _lyricsState.update {
                    it.copy(
                        isTranslating = false,
                        translationError = err.localizedMessage ?: "Gagal menerjemahkan lirik."
                    )
                }
            }
        }
    }

    fun askMusicaAi(question: String) {
        if (question.isBlank()) return
        val currentTrack = playerController.state.value.currentTrack
        _aiChatHistory.update { it + AiChatEntry(isUser = true, text = question.trim()) }
        viewModelScope.launch {
            aiRepo.askMusicaAiStream(question.trim(), currentTrack).collectLatest { state ->
                _aiState.value = state
                when (state) {
                    is AiUiState.Success -> {
                        _aiChatHistory.update { it + AiChatEntry(isUser = false, text = state.text) }
                    }
                    is AiUiState.Error -> {
                        _aiChatHistory.update {
                            it + AiChatEntry(isUser = false, text = "⚠️ ${state.message}")
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    fun createPlaylist(name: String, initialTrack: TrackItem? = null) {
        viewModelScope.launch {
            val id = catalogRepo.createPlaylist(name)
            if (initialTrack != null) {
                catalogRepo.addTrackToPlaylist(id, initialTrack)
            }
            _statusToast.value = "Playlist '$name' dibuat"
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            catalogRepo.deletePlaylist(playlistId)
            _statusToast.value = "Playlist dihapus"
        }
    }

    fun addTrackToPlaylist(playlistId: String, playlistName: String, track: TrackItem) {
        viewModelScope.launch {
            catalogRepo.addTrackToPlaylist(playlistId, track)
            _statusToast.value = "${track.title} ditambahkan ke '$playlistName'"
        }
    }

    fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        viewModelScope.launch {
            catalogRepo.removeTrackFromPlaylist(playlistId, trackId)
        }
    }

    fun downloadTrack(track: TrackItem) {
        viewModelScope.launch {
            _statusToast.value = "Mengunduh ${track.title}..."
            catalogRepo.downloadTrackForOffline(track)
                .onSuccess { _statusToast.value = "Siap Offline: ${it.title}" }
                .onFailure { _statusToast.value = it.localizedMessage ?: "Gagal mengunduh lagu" }
        }
    }

    fun downloadPlaylist(playlist: PlaylistUi) {
        viewModelScope.launch {
            _statusToast.value = "Mengunduh playlist '${playlist.name}'..."
            catalogRepo.downloadEntirePlaylist(playlist)
                .onSuccess { count -> _statusToast.value = "$count lagu siap diputar offline!" }
                .onFailure { _statusToast.value = it.localizedMessage ?: "Gagal unduh playlist" }
        }
    }

    fun removeDownload(trackId: String) {
        viewModelScope.launch {
            catalogRepo.removeDownloadedTrack(trackId)
            _statusToast.value = "Unduhan dihapus"
        }
    }

    fun importLocalAudio(uri: Uri) {
        viewModelScope.launch {
            catalogRepo.importLocalAudioUri(uri)
                .onSuccess { track ->
                    _statusToast.value = "Audio lokal siap: ${track.title}"
                    playTrack(track, listOf(track))
                }
                .onFailure { _statusToast.value = "Gagal memuat file audio lokal" }
        }
    }

    fun importExternalPlaylist(rawLinkOrText: String) {
        if (rawLinkOrText.isBlank()) return
        viewModelScope.launch {
            _statusToast.value = "Mengimpor playlist..."
            catalogRepo.importExternalPlaylist(rawLinkOrText)
                .onSuccess { pl ->
                    _statusToast.value = "Berhasil impor '${pl.name}' (${pl.trackCount} lagu penuh)"
                }
                .onFailure {
                    _statusToast.value = it.localizedMessage ?: "Gagal mengimpor playlist"
                }
        }
    }

    fun matchWithSharedPlaylist(myPlaylist: PlaylistUi, sharedDoc: SharedPlaylistDoc) {
        val pool = (_discoverState.value.trendingTracks + _discoverState.value.searchTracks).distinctBy { it.id }
        val result = catalogRepo.computePlaylistMatch(
            myPlaylist = myPlaylist,
            friendName = sharedDoc.name,
            friendArtists = sharedDoc.trackArtists,
            friendTitles = sharedDoc.trackTitles,
            catalogPool = pool
        )
        _playlistMatchResult.value = result
    }

    fun clearPlaylistMatchResult() {
        _playlistMatchResult.value = null
    }

    fun toggleFavorite(trackId: String) {
        viewModelScope.launch {
            val added = prefsStore.toggleFavorite(trackId)
            _statusToast.value = if (added) "Disimpan ke Favorit ❤️" else "Dihapus dari Favorit"
        }
    }

    fun toggleFollowArtist(artistId: String, artistName: String) {
        viewModelScope.launch {
            val followed = prefsStore.toggleFollowArtist(artistId)
            _statusToast.value = if (followed) "Mengikuti $artistName" else "Berhenti mengikuti $artistName"
        }
    }

    fun updateThemeConfig(config: GlassThemeConfig) {
        viewModelScope.launch {
            prefsStore.updateThemeConfig(config)
        }
    }

    fun setGlassMode(mode: GlassMode) {
        val current = localAppState.value.themeConfig
        updateThemeConfig(current.copy(glassMode = mode))
    }

    fun setGuestMode(enabled: Boolean) {
        viewModelScope.launch {
            prefsStore.setGuestSession(enabled)
        }
    }

    fun updateGuestProfile(
        username: String,
        displayName: String,
        bio: String,
        avatarIndex: Int,
        borderStyle: String,
        frameStyle: String
    ) {
        viewModelScope.launch {
            prefsStore.updateGuestProfile(username, displayName, bio, avatarIndex, borderStyle, frameStyle)
            _statusToast.value = "Profil diperbarui"
        }
    }

    fun simulateStreakMilestone(days: Int) {
        viewModelScope.launch {
            prefsStore.setDemoStreakDays(days)
            _statusToast.value = "Streak diperbarui ke $days hari!"
        }
    }
}
