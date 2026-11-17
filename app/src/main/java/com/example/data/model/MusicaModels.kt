package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

data class TrackItem(
    val id: String,
    val title: String,
    val artist: String,
    val artistId: String = "",
    val album: String = "Single",
    val coverUrl: String = "",
    val streamUrl: String = "",
    val durationMs: Long = 0L,
    val description: String = "",
    val listenerCount: Long = 0L,
    val playCount: Long = 0L,
    val genre: String = "Pop",
    val isRemix: Boolean = false,
    val isCover: Boolean = false,
    val isLocal: Boolean = false,
    val localFilePath: String? = null
)

data class AlbumItem(
    val id: String,
    val title: String,
    val artist: String,
    val artistId: String = "",
    val coverUrl: String = "",
    val year: String = "2026",
    val genre: String = "Electronic",
    val tracksCount: Int = 10,
    val description: String = ""
)

data class ArtistProfile(
    val id: String,
    val name: String,
    val handle: String,
    val avatarUrl: String = "",
    val followersCount: Long = 0L,
    val trackCount: Int = 0,
    val bio: String = ""
)

data class LyricLine(
    val timeMs: Long,
    val text: String,
    val translatedText: String = ""
)

data class PlaylistUi(
    val id: String,
    val name: String,
    val shareCode: String = "",
    val trackCount: Int = 0,
    val isOfflineReady: Boolean = false,
    val tracks: List<TrackItem> = emptyList()
)

data class PlaylistMatchResult(
    val friendPlaylistName: String,
    val myPlaylistName: String,
    val matchPercent: Int,
    val sharedArtists: List<String>,
    val sharedTrackTitles: List<String>,
    val recommendedBlendTracks: List<TrackItem>
)

enum class RepeatMode {
    OFF, ONE, ALL
}

enum class SleepTimerOption(val label: String, val minutes: Int) {
    OFF("Mati", 0),
    MIN_15("15 Menit", 15),
    MIN_30("30 Menit", 30),
    MIN_45("45 Menit", 45),
    MIN_60("60 Menit", 60),
    END_OF_TRACK("Akhir Lagu", -1)
}

@Keep
@IgnoreExtraProperties
data class UserPrivateDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val username: String = "",
    val displayName: String = "",
    val followingArtistIds: List<String> = emptyList(),
    val streakDays: Long = 0L,
    val lastActiveDate: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PublicProfileDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val displayName: String = "",
    val bio: String = "",
    val avatarIndex: Long = 0L,
    val borderStyle: String = "streak_auto",
    val frameStyle: String = "liquid_halo",
    val streakDays: Long = 0L,
    val followersCount: Long = 0L,
    val discoverable: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SharedPlaylistDoc(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    val name: String = "",
    val shareCode: String = "",
    val publicShared: Boolean = true,
    val trackTitles: List<String> = emptyList(),
    val trackArtists: List<String> = emptyList(),
    val trackIds: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StoryItemDoc(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorUsername: String = "",
    val mediaType: String = "TRACK", // PHOTO, LYRIC, STREAK_PAD, TRACK
    val caption: String = "",
    val trackId: String = "",
    val trackTitle: String = "",
    val trackArtist: String = "",
    val streakCount: Long = 0L,
    val accentColorHex: String = "#38BDF8",
    val visibility: String = "public",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SocialPostDoc(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorUsername: String = "",
    val trackId: String = "",
    val trackTitle: String = "",
    val trackArtist: String = "",
    val coverUrl: String = "",
    val caption: String = "",
    val likesCount: Long = 0L,
    val visibility: String = "public",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DirectChatDoc(
    @DocumentId val id: String = "",
    val participantIds: List<String> = emptyList(),
    val participantNames: List<String> = emptyList(),
    val lastMessage: String = "",
    val mutualStreak: Long = 0L,
    val lastStreakDate: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChatMessageDoc(
    @DocumentId val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val participantIds: List<String> = emptyList(),
    val text: String = "",
    val bubbleStyle: String = "frost_cyan",
    val edited: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MusicRoomDoc(
    @DocumentId val id: String = "",
    val hostId: String = "",
    val hostName: String = "",
    val roomName: String = "",
    val inviteCode: String = "",
    val publicRoom: Boolean = true,
    val memberUids: List<String> = emptyList(),
    val currentTrackId: String = "",
    val currentTrackTitle: String = "",
    val currentTrackArtist: String = "",
    val currentTrackCover: String = "",
    val currentTrackStreamUrl: String = "",
    val playing: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RoomMessageDoc(
    @DocumentId val id: String = "",
    val roomId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val publicRoom: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null
)

fun safeServerTimestamp() = FieldValue.serverTimestamp()
