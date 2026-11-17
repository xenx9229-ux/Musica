package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ChatMessageDoc
import com.example.data.model.DirectChatDoc
import com.example.data.model.MusicRoomDoc
import com.example.data.model.PlaylistUi
import com.example.data.model.PublicProfileDoc
import com.example.data.model.RoomMessageDoc
import com.example.data.model.SharedPlaylistDoc
import com.example.data.model.SocialPostDoc
import com.example.data.model.StoryItemDoc
import com.example.data.model.TrackItem
import com.example.data.model.UserPrivateDoc
import com.example.data.repository.FirestoreSocialRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "SocialCloudVM"

sealed interface CloudUiState<out T> {
    data object Loading : CloudUiState<Nothing>
    data class Success<T>(val data: T) : CloudUiState<T>
    data class Error(val message: String) : CloudUiState<Nothing>
}

class SocialCloudViewModel(
    private val repo: FirestoreSocialRepository,
    val currentUserId: String
) : ViewModel() {

    val myProfile: StateFlow<UserPrivateDoc?> = repo.observeMyPrivateProfile(currentUserId)
        .catch { error ->
            val fe = unwrapFirestoreError(error)
            if (fe == null && error is CancellationException) throw error
            Log.w(TAG, "Profile observe error", fe ?: error)
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val discoverableUsers: StateFlow<CloudUiState<List<PublicProfileDoc>>> =
        repo.observeDiscoverableProfiles()
            .toCloudState("Gagal memuat daftar pengguna")

    val stories: StateFlow<CloudUiState<List<StoryItemDoc>>> =
        repo.observeStories()
            .toCloudState("Gagal memuat Stories")

    val feedPosts: StateFlow<CloudUiState<List<SocialPostDoc>>> =
        repo.observePosts()
            .toCloudState("Gagal memuat postingan ulang")

    val chats: StateFlow<CloudUiState<List<DirectChatDoc>>> =
        repo.observeMyChats(currentUserId)
            .toCloudState("Gagal memuat daftar chat")

    val publicRooms: StateFlow<CloudUiState<List<MusicRoomDoc>>> =
        repo.observePublicRooms()
            .toCloudState("Gagal memuat room musik")

    private val _activeChat = MutableStateFlow<DirectChatDoc?>(null)
    val activeChat: StateFlow<DirectChatDoc?> = _activeChat.asStateFlow()

    val activeChatMessages: StateFlow<List<ChatMessageDoc>> = _activeChat
        .flatMapLatest { chat ->
            if (chat == null) flowOf(emptyList()) else repo.observeChatMessages(chat.id)
        }
        .catch { error ->
            val fe = unwrapFirestoreError(error)
            if (fe == null && error is CancellationException) throw error
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _activeRoomId = MutableStateFlow<String?>(null)
    val activeRoomId: StateFlow<String?> = _activeRoomId.asStateFlow()

    val activeRoomMessages: StateFlow<List<RoomMessageDoc>> = _activeRoomId
        .flatMapLatest { roomId ->
            if (roomId == null) flowOf(emptyList()) else repo.observeRoomMessages(roomId)
        }
        .catch { error ->
            val fe = unwrapFirestoreError(error)
            if (fe == null && error is CancellationException) throw error
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _cloudFeedback = MutableStateFlow<String?>(null)
    val cloudFeedback: StateFlow<String?> = _cloudFeedback.asStateFlow()

    fun clearFeedback() {
        _cloudFeedback.value = null
    }

    fun selectChat(chat: DirectChatDoc?) {
        _activeChat.value = chat
    }

    fun selectRoom(roomId: String?) {
        _activeRoomId.value = roomId
    }

    fun ensureCloudProfileIfMissing(
        username: String,
        displayName: String,
        email: String,
        bio: String,
        avatarIndex: Int,
        borderStyle: String,
        frameStyle: String,
        streakDays: Int
    ) {
        viewModelScope.launch {
            repo.ensureUserProfileIfMissing(username, displayName, email, bio, avatarIndex, borderStyle, frameStyle, streakDays)
        }
    }

    fun saveCloudProfile(
        username: String,
        displayName: String,
        email: String,
        bio: String,
        avatarIndex: Int,
        borderStyle: String,
        frameStyle: String,
        streakDays: Int
    ) {
        viewModelScope.launch {
            repo.upsertUserProfile(username, displayName, email, bio, avatarIndex, borderStyle, frameStyle, streakDays)
                .onSuccess { _cloudFeedback.value = "Profil cloud tersimpan!" }
                .onFailure { _cloudFeedback.value = it.localizedMessage ?: "Gagal menyimpan profil" }
        }
    }

    fun publishStory(
        authorUsername: String,
        mediaType: String,
        caption: String,
        track: TrackItem?,
        streakCount: Int,
        accentHex: String
    ) {
        viewModelScope.launch {
            repo.publishStory(authorUsername, mediaType, caption, track, streakCount, accentHex)
                .onSuccess { _cloudFeedback.value = "Story berhasil diposting!" }
                .onFailure { _cloudFeedback.value = it.localizedMessage ?: "Gagal memposting story" }
        }
    }

    fun repostTrack(authorUsername: String, track: TrackItem, caption: String) {
        viewModelScope.launch {
            repo.repostTrack(authorUsername, track, caption)
                .onSuccess { _cloudFeedback.value = "Berhasil posting ulang ${track.title}!" }
                .onFailure { _cloudFeedback.value = it.localizedMessage ?: "Gagal posting ulang" }
        }
    }

    fun likePost(postId: String) {
        viewModelScope.launch {
            repo.likePost(postId)
        }
    }

    fun startChatWithUser(myUsername: String, otherUser: PublicProfileDoc) {
        viewModelScope.launch {
            repo.startOrGetChat(myUsername, otherUser)
                .onSuccess { chat -> _activeChat.value = chat }
                .onFailure { _cloudFeedback.value = it.localizedMessage ?: "Gagal membuka chat" }
        }
    }

    fun sendChatMessage(senderName: String, text: String, bubbleStyle: String) {
        val chat = _activeChat.value ?: return
        if (text.isBlank()) return
        viewModelScope.launch {
            repo.sendChatMessage(chat, senderName, text.trim(), bubbleStyle)
                .onFailure { _cloudFeedback.value = it.localizedMessage ?: "Gagal mengirim pesan" }
        }
    }

    fun editChatBubble(messageId: String, newText: String, newBubbleStyle: String) {
        val chat = _activeChat.value ?: return
        if (newText.isBlank()) return
        viewModelScope.launch {
            repo.editChatBubble(chat.id, messageId, newText.trim(), newBubbleStyle)
                .onSuccess { _cloudFeedback.value = "Chat bubble diperbarui" }
                .onFailure { _cloudFeedback.value = it.localizedMessage ?: "Gagal mengedit bubble" }
        }
    }

    fun createRoom(hostName: String, roomName: String, currentTrack: TrackItem?) {
        if (roomName.isBlank()) return
        viewModelScope.launch {
            repo.createListeningRoom(hostName, roomName.trim(), currentTrack)
                .onSuccess { room ->
                    _activeRoomId.value = room.id
                    _cloudFeedback.value = "Room dibuat! Kode undangan: ${room.inviteCode}"
                }
                .onFailure { _cloudFeedback.value = it.localizedMessage ?: "Gagal membuat room" }
        }
    }

    fun joinRoomByCode(codeOrLink: String) {
        if (codeOrLink.isBlank()) return
        viewModelScope.launch {
            repo.joinRoomByCodeOrId(codeOrLink)
                .onSuccess { room ->
                    _activeRoomId.value = room.id
                    _cloudFeedback.value = "Bergabung ke room '${room.roomName}'"
                }
                .onFailure { _cloudFeedback.value = it.localizedMessage ?: "Room tidak ditemukan" }
        }
    }

    fun syncRoomTrack(roomId: String, track: TrackItem, isPlaying: Boolean) {
        viewModelScope.launch {
            repo.syncRoomTrack(roomId, track, isPlaying)
                .onSuccess { _cloudFeedback.value = "Lagu disinkronkan ke room!" }
                .onFailure { _cloudFeedback.value = it.localizedMessage ?: "Gagal sinkronisasi room" }
        }
    }

    fun sendRoomMessage(roomId: String, senderName: String, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            repo.sendRoomMessage(roomId, senderName, text.trim())
        }
    }

    fun sharePlaylistLink(playlist: PlaylistUi, ownerName: String, onLinkReady: (String) -> Unit) {
        viewModelScope.launch {
            repo.publishSharedPlaylist(playlist, ownerName)
                .onSuccess { link -> onLinkReady(link) }
                .onFailure { _cloudFeedback.value = it.localizedMessage ?: "Gagal membuat tautan playlist" }
        }
    }

    fun fetchPlaylistByLink(linkOrCode: String, onFound: (SharedPlaylistDoc) -> Unit) {
        if (linkOrCode.isBlank()) return
        viewModelScope.launch {
            repo.fetchSharedPlaylistByLink(linkOrCode)
                .onSuccess { doc -> onFound(doc) }
                .onFailure { _cloudFeedback.value = it.localizedMessage ?: "Tautan playlist tidak ditemukan" }
        }
    }

    private fun <T> kotlinx.coroutines.flow.Flow<List<T>>.toCloudState(
        fallbackMsg: String
    ): StateFlow<CloudUiState<List<T>>> = this
        .map<List<T>, CloudUiState<List<T>>> { CloudUiState.Success(it) }
        .catch { error ->
            val firestoreError = unwrapFirestoreError(error)
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            Log.w(TAG, fallbackMsg, root)
            emit(CloudUiState.Error(root.message ?: fallbackMsg))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), CloudUiState.Loading)

    private fun unwrapFirestoreError(error: Throwable): FirebaseFirestoreException? =
        generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
}
