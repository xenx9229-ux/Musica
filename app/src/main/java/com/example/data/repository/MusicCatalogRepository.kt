package com.example.data.repository

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.example.data.local.DownloadedTrackEntity
import com.example.data.local.LocalPlaylistEntity
import com.example.data.local.LocalPlaylistTrackEntity
import com.example.data.local.MusicaDao
import com.example.data.model.AlbumItem
import com.example.data.model.ArtistProfile
import com.example.data.model.LyricLine
import com.example.data.model.PlaylistMatchResult
import com.example.data.model.PlaylistUi
import com.example.data.model.TrackItem
import com.example.data.remote.AudiusApiClient
import com.example.data.remote.LrclibApiClient
import com.example.data.remote.PlaylistLinkImporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.TimeUnit

private const val TAG = "MusicCatalogRepo"

class MusicCatalogRepository(
    private val context: Context,
    private val dao: MusicaDao,
    private val audius: AudiusApiClient = AudiusApiClient(),
    private val lrclib: LrclibApiClient = LrclibApiClient(),
    private val linkImporter: PlaylistLinkImporter = PlaylistLinkImporter()
) {
    private val downloadClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    val downloadedTracks: Flow<List<TrackItem>> = dao.observeDownloadedTracks().map { list ->
        list.map { it.toTrackItem() }
    }

    val playlists: Flow<List<PlaylistUi>> = combine(
        dao.observePlaylists(),
        dao.observeAllPlaylistTracks()
    ) { playlistList, allTracks ->
        val byPlaylist = allTracks.groupBy { it.playlistId }
        playlistList.map { pl ->
            val items = byPlaylist[pl.playlistId].orEmpty().map { it.toTrackItem() }
            PlaylistUi(
                id = pl.playlistId,
                name = pl.name,
                shareCode = pl.shareCode,
                trackCount = items.size,
                isOfflineReady = pl.isOfflineSaved,
                tracks = items
            )
        }
    }

    suspend fun getTrending(genre: String? = null): List<TrackItem> = audius.getTrendingTracks(genre)

    suspend fun searchTracks(query: String): List<TrackItem> = audius.searchTracks(query)

    suspend fun searchArtists(query: String): List<ArtistProfile> = audius.searchArtists(query)

    suspend fun getArtistTracks(artistId: String): List<TrackItem> = audius.getArtistTracks(artistId)

    suspend fun getRecommendedAlbums(query: String = ""): List<AlbumItem> {
        val audiusPlaylists = audius.searchPlaylists(query.ifBlank { "lofi" }, limit = 8)
        val itunesAlbums = audius.searchAlbumsFromItunes(query.ifBlank { "electronic pop" }, limit = 12)
        return (audiusPlaylists + itunesAlbums).distinctBy { it.title.lowercase() }
    }

    suspend fun getLyricsForTrack(track: TrackItem): List<LyricLine> {
        val remote = lrclib.fetchLyrics(track.title, track.artist, track.durationMs)
        if (remote.isNotEmpty()) return remote

        // Contextual rhythmic fallback synced to track duration when LRCLIB has no entry
        val duration = track.durationMs.coerceAtLeast(120_000L)
        val lines = listOf(
            "♪ ${track.title} — ${track.artist}",
            "Mengalir dalam dimensi Liquid Glass",
            "Genre ${track.genre} • ${track.album}",
            "Nikmati audio penuh tanpa batas di Musica",
            "Detak irama berputar bersama piringan waktu",
            "Setiap nada membawa cerita baru malam ini",
            "Dengarkan bersama teman di Listening Room",
            "Musica • Creator by XenProject"
        )
        val step = duration / (lines.size + 1)
        return lines.mapIndexed { index, text ->
            LyricLine(timeMs = (index + 1) * step, text = text)
        }
    }

    suspend fun createPlaylist(name: String): String {
        val id = "pl_${UUID.randomUUID().toString().take(8)}"
        val shareCode = "MSC-${id.takeLast(5).uppercase()}"
        dao.upsertPlaylist(
            LocalPlaylistEntity(
                playlistId = id,
                name = name.trim().ifBlank { "Playlist Baru" },
                shareCode = shareCode
            )
        )
        return id
    }

    suspend fun deletePlaylist(playlistId: String) {
        dao.deletePlaylistTracks(playlistId)
        dao.deletePlaylist(playlistId)
    }

    suspend fun addTrackToPlaylist(playlistId: String, track: TrackItem) {
        dao.upsertPlaylistTrack(
            LocalPlaylistTrackEntity(
                playlistId = playlistId,
                trackId = track.id,
                title = track.title,
                artist = track.artist,
                artistId = track.artistId,
                album = track.album,
                coverUrl = track.coverUrl,
                streamUrl = track.streamUrl,
                durationMs = track.durationMs,
                description = track.description,
                listenerCount = track.listenerCount,
                genre = track.genre
            )
        )
    }

    suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        dao.removeTrackFromPlaylist(playlistId, trackId)
    }

    suspend fun downloadTrackForOffline(track: TrackItem): Result<TrackItem> = withContext(Dispatchers.IO) {
        try {
            val existing = dao.getDownloadedTrack(track.id)
            if (existing != null && File(existing.localFilePath).exists()) {
                return@withContext Result.success(existing.toTrackItem())
            }

            val dir = File(context.filesDir, "downloads").apply { mkdirs() }
            val safeId = track.id.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val targetFile = File(dir, "$safeId.mp3")

            if (track.isLocal && !track.localFilePath.isNullOrBlank()) {
                val uri = Uri.parse(track.localFilePath)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(targetFile).use { output -> input.copyTo(output) }
                } ?: throw IllegalStateException("Tidak dapat membaca file audio lokal.")
            } else {
                val url = track.streamUrl.ifBlank { audius.buildStreamUrl(track.id) }
                val req = Request.Builder().url(url).header("User-Agent", "Musica/1.0").build()
                downloadClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        throw IllegalStateException("Gagal mengunduh audio (HTTP ${resp.code})")
                    }
                    val bodyStream = resp.body?.byteStream()
                        ?: throw IllegalStateException("Stream audio kosong")
                    bodyStream.use { input ->
                        FileOutputStream(targetFile).use { output -> input.copyTo(output) }
                    }
                }
            }

            val entity = DownloadedTrackEntity(
                trackId = track.id,
                title = track.title,
                artist = track.artist,
                artistId = track.artistId,
                album = track.album,
                coverUrl = track.coverUrl,
                streamUrl = track.streamUrl,
                localFilePath = targetFile.absolutePath,
                durationMs = track.durationMs,
                description = track.description,
                listenerCount = track.listenerCount,
                genre = track.genre,
                isLocalSource = track.isLocal
            )
            dao.upsertDownloadedTrack(entity)
            Result.success(entity.toTrackItem())
        } catch (e: Exception) {
            Log.e(TAG, "Download failed for ${track.title}", e)
            Result.failure(e)
        }
    }

    suspend fun downloadEntirePlaylist(playlist: PlaylistUi): Result<Int> {
        var successCount = 0
        for (track in playlist.tracks) {
            if (downloadTrackForOffline(track).isSuccess) {
                successCount++
            }
        }
        if (successCount > 0) {
            dao.markPlaylistOffline(playlist.id, true)
        }
        return if (successCount > 0 || playlist.tracks.isEmpty()) {
            Result.success(successCount)
        } else {
            Result.failure(IllegalStateException("Gagal mengunduh lagu dari playlist ini."))
        }
    }

    suspend fun removeDownloadedTrack(trackId: String) = withContext(Dispatchers.IO) {
        val existing = dao.getDownloadedTrack(trackId)
        if (existing != null) {
            File(existing.localFilePath).delete()
        }
        dao.deleteDownloadedTrack(trackId)
    }

    suspend fun importLocalAudioUri(uri: Uri): Result<TrackItem> = withContext(Dispatchers.IO) {
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.takeIf { it.isNotBlank() } ?: "Audio Lokal"
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?.takeIf { it.isNotBlank() } ?: "Perangkat Saya"
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                ?.takeIf { it.isNotBlank() } ?: "File Perangkat"
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 180_000L
            retriever.release()

            val track = TrackItem(
                id = "local_${UUID.randomUUID().toString().take(8)}",
                title = title,
                artist = artist,
                album = album,
                streamUrl = uri.toString(),
                durationMs = durationMs,
                description = "File audio lokal penuh dari perangkat Anda.",
                listenerCount = 1L,
                genre = "Local Audio",
                isLocal = true,
                localFilePath = uri.toString()
            )
            val saved = downloadTrackForOffline(track).getOrElse { track }
            Result.success(saved)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importExternalPlaylist(
        rawLinkOrList: String,
        aiExtractedQueries: List<String> = emptyList()
    ): Result<PlaylistUi> {
        return try {
            val parsed = linkImporter.inspectPlaylistInput(rawLinkOrList)
            val queries = (aiExtractedQueries + parsed.candidateQueries)
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .take(12)

            val matchedTracks = mutableListOf<TrackItem>()
            for (q in queries) {
                val found = audius.searchTracks(q, limit = 2)
                if (found.isNotEmpty()) {
                    matchedTracks.add(found.first())
                }
            }
            if (matchedTracks.isEmpty()) {
                matchedTracks.addAll(audius.getTrendingTracks(limit = 6))
            }

            val finalName = "${parsed.playlistTitle} (${parsed.sourcePlatform})"
            val plId = createPlaylist(finalName)
            val uniqueTracks = matchedTracks.distinctBy { it.id }
            uniqueTracks.forEach { addTrackToPlaylist(plId, it) }

            Result.success(
                PlaylistUi(
                    id = plId,
                    name = finalName,
                    shareCode = "MSC-${plId.takeLast(5).uppercase()}",
                    trackCount = uniqueTracks.size,
                    tracks = uniqueTracks
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun computePlaylistMatch(
        myPlaylist: PlaylistUi,
        friendName: String,
        friendArtists: List<String>,
        friendTitles: List<String>,
        catalogPool: List<TrackItem>
    ): PlaylistMatchResult {
        val myArtists = myPlaylist.tracks.map { it.artist.lowercase() }.toSet()
        val myTitles = myPlaylist.tracks.map { it.title.lowercase() }.toSet()
        val sharedArtists = friendArtists.filter { myArtists.contains(it.lowercase()) }.distinct()
        val sharedTitles = friendTitles.filter { myTitles.contains(it.lowercase()) }.distinct()

        val denom = (friendArtists.size + friendTitles.size).coerceAtLeast(1)
        val rawScore = ((sharedArtists.size * 2 + sharedTitles.size * 3) * 100) / (denom * 2)
        val score = rawScore.coerceIn(38, 99)

        val blend = (myPlaylist.tracks + catalogPool.filter { t ->
            friendArtists.any { it.equals(t.artist, ignoreCase = true) }
        }).distinctBy { it.id }.take(12)

        return PlaylistMatchResult(
            friendPlaylistName = friendName,
            myPlaylistName = myPlaylist.name,
            matchPercent = score,
            sharedArtists = sharedArtists,
            sharedTrackTitles = sharedTitles,
            recommendedBlendTracks = blend
        )
    }

    private fun DownloadedTrackEntity.toTrackItem() = TrackItem(
        id = trackId,
        title = title,
        artist = artist,
        artistId = artistId,
        album = album,
        coverUrl = coverUrl,
        streamUrl = localFilePath,
        durationMs = durationMs,
        description = description,
        listenerCount = listenerCount,
        genre = genre,
        isLocal = isLocalSource,
        localFilePath = localFilePath
    )

    private fun LocalPlaylistTrackEntity.toTrackItem() = TrackItem(
        id = trackId,
        title = title,
        artist = artist,
        artistId = artistId,
        album = album,
        coverUrl = coverUrl,
        streamUrl = streamUrl,
        durationMs = durationMs,
        description = description,
        listenerCount = listenerCount,
        genre = genre
    )
}
