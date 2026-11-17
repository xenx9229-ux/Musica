package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.OperationType
import com.example.core.handleFirestoreError
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class FirestoreSocialRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUid(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Silakan masuk dengan Google untuk menggunakan fitur cloud sosial.")
    }

    fun observeMyPrivateProfile(uid: String): Flow<UserPrivateDoc?> =
        db.collection("users").document(uid)
            .snapshots()
            .map { it.toObject(UserPrivateDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }

    fun observeDiscoverableProfiles(): Flow<List<PublicProfileDoc>> = flow {
        requireUid()
        val path = "public_profiles"
        emitAll(
            db.collection(path)
                .whereEqualTo("discoverable", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(PublicProfileDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    val fe = unwrapFirestoreError(error)
                    if (fe == null && error is CancellationException) throw error
                    if (auth.currentUser != null) handleFirestoreError(fe ?: error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeStories(): Flow<List<StoryItemDoc>> = flow {
        requireUid()
        val path = "stories"
        emitAll(
            db.collection(path)
                .whereEqualTo("visibility", "public")
                .snapshots()
                .map { snap ->
                    snap.toObjects(StoryItemDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val fe = unwrapFirestoreError(error)
                    if (fe == null && error is CancellationException) throw error
                    if (auth.currentUser != null) handleFirestoreError(fe ?: error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observePosts(): Flow<List<SocialPostDoc>> = flow {
        requireUid()
        val path = "posts"
        emitAll(
            db.collection(path)
                .whereEqualTo("visibility", "public")
                .snapshots()
                .map { snap ->
                    snap.toObjects(SocialPostDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val fe = unwrapFirestoreError(error)
                    if (fe == null && error is CancellationException) throw error
                    if (auth.currentUser != null) handleFirestoreError(fe ?: error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeMyChats(uid: String): Flow<List<DirectChatDoc>> = flow {
        val path = "chats"
        emitAll(
            db.collection(path)
                .whereArrayContains("participantIds", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(DirectChatDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val fe = unwrapFirestoreError(error)
                    if (fe == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) handleFirestoreError(fe ?: error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeChatMessages(chatId: String): Flow<List<ChatMessageDoc>> = flow {
        val uid = requireUid()
        val path = "chats/$chatId/messages"
        emitAll(
            db.collection("chats").document(chatId).collection("messages")
                .whereArrayContains("participantIds", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ChatMessageDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val fe = unwrapFirestoreError(error)
                    if (fe == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) handleFirestoreError(fe ?: error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observePublicRooms(): Flow<List<MusicRoomDoc>> = flow {
        requireUid()
        val path = "rooms"
        emitAll(
            db.collection(path)
                .whereEqualTo("publicRoom", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(MusicRoomDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val fe = unwrapFirestoreError(error)
                    if (fe == null && error is CancellationException) throw error
                    if (auth.currentUser != null) handleFirestoreError(fe ?: error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeRoomMessages(roomId: String): Flow<List<RoomMessageDoc>> = flow {
        requireUid()
        val path = "rooms/$roomId/messages"
        emitAll(
            db.collection("rooms").document(roomId).collection("messages")
                .whereEqualTo("publicRoom", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(RoomMessageDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val fe = unwrapFirestoreError(error)
                    if (fe == null && error is CancellationException) throw error
                    if (auth.currentUser != null) handleFirestoreError(fe ?: error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun ensureUserProfileIfMissing(
        username: String,
        displayName: String,
        email: String,
        bio: String,
        avatarIndex: Int,
        borderStyle: String,
        frameStyle: String,
        streakDays: Int
    ): Result<Unit> = runFirestoreWrite(OperationType.WRITE, "users") {
        val uid = requireUid()
        val rawUsername = username.trim().take(30).ifBlank { "user_${uid.take(6)}" }
        val cleanUsername = if (rawUsername.length < 2) "${rawUsername}_mx" else rawUsername
        val cleanDisplay = displayName.trim().take(50).ifBlank { cleanUsername }
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        val userRef = db.collection("users").document(uid)
        val existingPrivate = userRef.get().await()
        if (!existingPrivate.exists()) {
            userRef.set(
                mapOf(
                    "userId" to uid,
                    "email" to email.take(120),
                    "username" to cleanUsername,
                    "displayName" to cleanDisplay,
                    "followingArtistIds" to emptyList<String>(),
                    "streakDays" to streakDays.toLong(),
                    "lastActiveDate" to today,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

        val pubRef = db.collection("public_profiles").document(uid)
        val existingPub = pubRef.get().await()
        if (!existingPub.exists()) {
            pubRef.set(
                mapOf(
                    "userId" to uid,
                    "username" to cleanUsername,
                    "displayName" to cleanDisplay,
                    "bio" to bio.take(200),
                    "avatarIndex" to avatarIndex.toLong(),
                    "borderStyle" to borderStyle,
                    "frameStyle" to frameStyle,
                    "streakDays" to streakDays.toLong(),
                    "followersCount" to 1L,
                    "discoverable" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    suspend fun upsertUserProfile(
        username: String,
        displayName: String,
        email: String,
        bio: String,
        avatarIndex: Int,
        borderStyle: String,
        frameStyle: String,
        streakDays: Int
    ): Result<Unit> = runFirestoreWrite(OperationType.WRITE, "users") {
        val uid = requireUid()
        val rawUsername = username.trim().take(30).ifBlank { "user_${uid.take(6)}" }
        val cleanUsername = if (rawUsername.length < 2) "${rawUsername}_mx" else rawUsername
        val cleanDisplay = displayName.trim().take(50).ifBlank { cleanUsername }
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        val userRef = db.collection("users").document(uid)
        val existingPrivate = userRef.get().await()
        if (!existingPrivate.exists()) {
            userRef.set(
                mapOf(
                    "userId" to uid,
                    "email" to email.take(120),
                    "username" to cleanUsername,
                    "displayName" to cleanDisplay,
                    "followingArtistIds" to emptyList<String>(),
                    "streakDays" to streakDays.toLong(),
                    "lastActiveDate" to today,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            userRef.update(
                mapOf(
                    "username" to cleanUsername,
                    "displayName" to cleanDisplay,
                    "streakDays" to streakDays.toLong(),
                    "lastActiveDate" to today,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

        val pubRef = db.collection("public_profiles").document(uid)
        val existingPub = pubRef.get().await()
        if (!existingPub.exists()) {
            pubRef.set(
                mapOf(
                    "userId" to uid,
                    "username" to cleanUsername,
                    "displayName" to cleanDisplay,
                    "bio" to bio.take(200),
                    "avatarIndex" to avatarIndex.toLong(),
                    "borderStyle" to borderStyle,
                    "frameStyle" to frameStyle,
                    "streakDays" to streakDays.toLong(),
                    "followersCount" to 1L,
                    "discoverable" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            pubRef.update(
                mapOf(
                    "username" to cleanUsername,
                    "displayName" to cleanDisplay,
                    "bio" to bio.take(200),
                    "avatarIndex" to avatarIndex.toLong(),
                    "borderStyle" to borderStyle,
                    "frameStyle" to frameStyle,
                    "streakDays" to streakDays.toLong(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    suspend fun publishStory(
        authorUsername: String,
        mediaType: String,
        caption: String,
        track: TrackItem?,
        streakCount: Int,
        accentHex: String
    ): Result<Unit> = runFirestoreWrite(OperationType.CREATE, "stories") {
        val uid = requireUid()
        val storyId = "st_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val cleanAuthor = authorUsername.trim().take(30).let { if (it.length < 2) "musica_user" else it }
        val payload = mapOf(
            "authorId" to uid,
            "authorUsername" to cleanAuthor,
            "mediaType" to mediaType,
            "caption" to caption.take(300).ifBlank { "Vibe di Musica" },
            "trackId" to track?.id?.take(128),
            "trackTitle" to track?.title?.take(120),
            "trackArtist" to track?.artist?.take(100),
            "streakCount" to streakCount.toLong(),
            "accentColorHex" to accentHex.take(20),
            "visibility" to "public",
            "createdAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }
        db.collection("stories").document(storyId).set(payload).await()
    }

    suspend fun repostTrack(
        authorUsername: String,
        track: TrackItem,
        caption: String
    ): Result<Unit> = runFirestoreWrite(OperationType.CREATE, "posts") {
        val uid = requireUid()
        val postId = "post_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val cleanAuthor = authorUsername.trim().take(30).let { if (it.length < 2) "musica_user" else it }
        val payload = mapOf(
            "authorId" to uid,
            "authorUsername" to cleanAuthor,
            "trackId" to track.id.take(128).ifBlank { "trk_musica" },
            "trackTitle" to track.title.take(120).ifBlank { "Musica Track" },
            "trackArtist" to track.artist.take(100).ifBlank { "Musica Artist" },
            "coverUrl" to track.coverUrl.take(500),
            "caption" to caption.take(300),
            "likesCount" to 0L,
            "visibility" to "public",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        db.collection("posts").document(postId).set(payload).await()
    }

    suspend fun likePost(postId: String): Result<Unit> = runFirestoreWrite(OperationType.UPDATE, "posts/$postId") {
        requireUid()
        db.collection("posts").document(postId).update(
            mapOf(
                "likesCount" to FieldValue.increment(1L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun startOrGetChat(
        myUsername: String,
        otherUser: PublicProfileDoc
    ): Result<DirectChatDoc> {
        return try {
            val uid = requireUid()
            val sortedIds = listOf(uid, otherUser.userId).sorted()
            val chatId = "chat_${sortedIds[0].take(14)}_${sortedIds[1].take(14)}"
            val docRef = db.collection("chats").document(chatId)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                val names = if (sortedIds[0] == uid) {
                    listOf(myUsername, otherUser.username)
                } else {
                    listOf(otherUser.username, myUsername)
                }
                docRef.set(
                    mapOf(
                        "participantIds" to sortedIds,
                        "participantNames" to names,
                        "lastMessage" to "Mulai streak musik bersama!",
                        "mutualStreak" to 1L,
                        "lastStreakDate" to today,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                val created = docRef.get().await().toObject(DirectChatDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                Result.success(created ?: DirectChatDoc(id = chatId, participantIds = sortedIds, participantNames = names, mutualStreak = 1L))
            } else {
                val existing = snap.toObject(DirectChatDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: DirectChatDoc(id = chatId, participantIds = sortedIds)
                Result.success(existing)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "chats")
            Result.failure(e)
        }
    }

    suspend fun sendChatMessage(
        chat: DirectChatDoc,
        senderName: String,
        text: String,
        bubbleStyle: String
    ): Result<Unit> = runFirestoreWrite(OperationType.CREATE, "chats/${chat.id}/messages") {
        val uid = requireUid()
        val msgId = "msg_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val nextStreak = if (chat.lastStreakDate != today) chat.mutualStreak + 1L else chat.mutualStreak

        val safeText = text.trim().take(1000).ifBlank { "🎵" }
        db.collection("chats").document(chat.id).collection("messages").document(msgId).set(
            mapOf(
                "chatId" to chat.id,
                "senderId" to uid,
                "senderName" to senderName.take(50).ifBlank { "Musica User" },
                "participantIds" to chat.participantIds,
                "text" to safeText,
                "bubbleStyle" to bubbleStyle.take(40).ifBlank { "frost_cyan" },
                "edited" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()

        db.collection("chats").document(chat.id).update(
            mapOf(
                "lastMessage" to safeText.take(500),
                "mutualStreak" to nextStreak,
                "lastStreakDate" to today,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun editChatBubble(
        chatId: String,
        messageId: String,
        newText: String,
        newBubbleStyle: String
    ): Result<Unit> = runFirestoreWrite(OperationType.UPDATE, "chats/$chatId/messages/$messageId") {
        requireUid()
        val safeText = newText.trim().take(1000).ifBlank { "🎵" }
        db.collection("chats").document(chatId).collection("messages").document(messageId).update(
            mapOf(
                "text" to safeText,
                "bubbleStyle" to newBubbleStyle.take(40).ifBlank { "frost_cyan" },
                "edited" to true,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun createListeningRoom(
        hostName: String,
        roomName: String,
        currentTrack: TrackItem?
    ): Result<MusicRoomDoc> {
        return try {
            val uid = requireUid()
            val roomId = "room_${UUID.randomUUID().toString().replace("-", "").take(10)}"
            val inviteCode = "XEN-${UUID.randomUUID().toString().take(5).uppercase()}"
            val cleanHost = hostName.trim().take(50).ifBlank { "Musica Host" }
            val cleanRoom = roomName.trim().take(80).ifBlank { "Musica Room" }
            val payload = mapOf(
                "hostId" to uid,
                "hostName" to cleanHost,
                "roomName" to cleanRoom,
                "inviteCode" to inviteCode,
                "publicRoom" to true,
                "memberUids" to listOf(uid),
                "currentTrackId" to currentTrack?.id?.take(128),
                "currentTrackTitle" to currentTrack?.title?.take(120),
                "currentTrackArtist" to currentTrack?.artist?.take(100),
                "currentTrackCover" to currentTrack?.coverUrl?.take(500),
                "currentTrackStreamUrl" to currentTrack?.streamUrl?.take(500),
                "playing" to (currentTrack != null),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            db.collection("rooms").document(roomId).set(payload).await()
            val doc = db.collection("rooms").document(roomId).get().await()
                .toObject(MusicRoomDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            Result.success(doc ?: MusicRoomDoc(id = roomId, hostId = uid, hostName = cleanHost, roomName = cleanRoom, inviteCode = inviteCode, memberUids = listOf(uid)))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "rooms")
            Result.failure(e)
        }
    }

    suspend fun joinRoomByCodeOrId(codeOrLink: String): Result<MusicRoomDoc> {
        return try {
            val uid = requireUid()
            val cleanCode = codeOrLink.trim().substringAfterLast("/").uppercase()
            val rooms = db.collection("rooms").whereEqualTo("publicRoom", true).get().await()
                .toObjects(MusicRoomDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            val match = rooms.firstOrNull {
                it.inviteCode.equals(cleanCode, ignoreCase = true) ||
                    it.id.equals(cleanCode, ignoreCase = true)
            } ?: throw IllegalStateException("Room dengan kode '$cleanCode' tidak ditemukan.")

            val updatedMembers = if (!match.memberUids.contains(uid)) {
                if (match.memberUids.size >= 50) {
                    throw IllegalStateException("Room '${match.roomName}' sudah penuh (maks 50 pendengar).")
                }
                val next = match.memberUids + uid
                db.collection("rooms").document(match.id).update(
                    mapOf(
                        "memberUids" to next,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                next
            } else {
                match.memberUids
            }
            Result.success(match.copy(memberUids = updatedMembers))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "rooms")
            Result.failure(e)
        }
    }

    suspend fun syncRoomTrack(roomId: String, track: TrackItem, isPlaying: Boolean): Result<Unit> =
        runFirestoreWrite(OperationType.UPDATE, "rooms/$roomId") {
            requireUid()
            db.collection("rooms").document(roomId).update(
                mapOf(
                    "currentTrackId" to track.id.take(128),
                    "currentTrackTitle" to track.title.take(120),
                    "currentTrackArtist" to track.artist.take(100),
                    "currentTrackCover" to track.coverUrl.take(500),
                    "currentTrackStreamUrl" to track.streamUrl.take(500),
                    "playing" to isPlaying,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

    suspend fun sendRoomMessage(roomId: String, senderName: String, text: String): Result<Unit> =
        runFirestoreWrite(OperationType.CREATE, "rooms/$roomId/messages") {
            val uid = requireUid()
            val msgId = "rmsg_${UUID.randomUUID().toString().replace("-", "").take(12)}"
            db.collection("rooms").document(roomId).collection("messages").document(msgId).set(
                mapOf(
                    "roomId" to roomId,
                    "senderId" to uid,
                    "senderName" to senderName.take(50),
                    "text" to text.take(500),
                    "publicRoom" to true,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

    suspend fun publishSharedPlaylist(playlist: PlaylistUi, ownerName: String): Result<String> {
        return try {
            val uid = requireUid()
            val shareCode = playlist.shareCode.ifBlank { "MSC-${playlist.id.takeLast(5).uppercase()}" }
            val docId = "spl_${shareCode.replace("-", "_")}"
            val docRef = db.collection("shared_playlists").document(docId)
            val existing = docRef.get().await()
            val cleanOwner = ownerName.trim().take(50).ifBlank { "Musica Listener" }
            val cleanName = playlist.name.trim().take(80).ifBlank { "Musica Playlist" }
            if (!existing.exists()) {
                docRef.set(
                    mapOf(
                        "ownerId" to uid,
                        "ownerName" to cleanOwner,
                        "name" to cleanName,
                        "shareCode" to shareCode,
                        "publicShared" to true,
                        "trackTitles" to playlist.tracks.map { it.title }.take(100),
                        "trackArtists" to playlist.tracks.map { it.artist }.take(100),
                        "trackIds" to playlist.tracks.map { it.id }.take(100),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "ownerName" to cleanOwner,
                        "name" to cleanName,
                        "publicShared" to true,
                        "trackTitles" to playlist.tracks.map { it.title }.take(100),
                        "trackArtists" to playlist.tracks.map { it.artist }.take(100),
                        "trackIds" to playlist.tracks.map { it.id }.take(100),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success("https://musica.xenproject.app/playlist/$shareCode")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "shared_playlists")
            Result.failure(e)
        }
    }

    suspend fun fetchSharedPlaylistByLink(linkOrCode: String): Result<SharedPlaylistDoc> {
        return try {
            requireUid()
            val cleanCode = linkOrCode.trim().substringAfterLast("/").uppercase()
            val docs = db.collection("shared_playlists").whereEqualTo("publicShared", true).get().await()
                .toObjects(SharedPlaylistDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            val found = docs.firstOrNull {
                it.shareCode.equals(cleanCode, ignoreCase = true) ||
                    it.id.equals(cleanCode, ignoreCase = true)
            } ?: throw IllegalStateException("Playlist dengan tautan '$cleanCode' tidak ditemukan.")
            Result.success(found)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "shared_playlists")
            Result.failure(e)
        }
    }

    private suspend fun runFirestoreWrite(
        op: OperationType,
        path: String,
        block: suspend () -> Unit
    ): Result<Unit> {
        return try {
            block()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, op, path)
            Result.failure(e)
        }
    }

    private fun unwrapFirestoreError(error: Throwable): FirebaseFirestoreException? =
        generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
}
