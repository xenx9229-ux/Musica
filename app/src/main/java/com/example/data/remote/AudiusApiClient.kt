package com.example.data.remote

import android.net.Uri
import android.util.Log
import com.example.data.model.AlbumItem
import com.example.data.model.ArtistProfile
import com.example.data.model.TrackItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private const val TAG = "AudiusApi"
private const val APP_NAME = "Musica"

class AudiusApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    @Volatile
    private var activeHost: String = "https://api.audius.co"

    suspend fun getTrendingTracks(genre: String? = null, limit: Int = 25): List<TrackItem> =
        withContext(Dispatchers.IO) {
            val host = resolveHealthyHost()
            val genreParam = if (!genre.isNullOrBlank() && genre != "All") {
                "&genre=${Uri.encode(genre)}"
            } else ""
            val url = "$host/v1/tracks/trending?limit=$limit$genreParam&app_name=$APP_NAME"
            fetchTrackList(url, host)
        }

    suspend fun searchTracks(query: String, limit: Int = 25): List<TrackItem> =
        withContext(Dispatchers.IO) {
            if (query.isBlank()) return@withContext emptyList()
            val host = resolveHealthyHost()
            val url = "$host/v1/tracks/search?query=${Uri.encode(query)}&limit=$limit&app_name=$APP_NAME"
            fetchTrackList(url, host)
        }

    suspend fun searchArtists(query: String, limit: Int = 15): List<ArtistProfile> =
        withContext(Dispatchers.IO) {
            if (query.isBlank()) return@withContext emptyList()
            val host = resolveHealthyHost()
            val url = "$host/v1/users/search?query=${Uri.encode(query)}&limit=$limit&app_name=$APP_NAME"
            val body = getString(url) ?: return@withContext emptyList()
            parseArtists(body)
        }

    suspend fun getArtistTracks(artistId: String, limit: Int = 20): List<TrackItem> =
        withContext(Dispatchers.IO) {
            val host = resolveHealthyHost()
            val url = "$host/v1/users/$artistId/tracks?limit=$limit&app_name=$APP_NAME"
            fetchTrackList(url, host)
        }

    suspend fun searchAlbumsFromItunes(query: String, limit: Int = 18): List<AlbumItem> =
        withContext(Dispatchers.IO) {
            val q = if (query.isBlank()) "top electronic pop" else query
            val url = "https://itunes.apple.com/search?term=${Uri.encode(q)}&entity=album&limit=$limit"
            val body = getString(url) ?: return@withContext emptyList()
            parseItunesAlbums(body)
        }

    suspend fun searchPlaylists(query: String, limit: Int = 12): List<AlbumItem> =
        withContext(Dispatchers.IO) {
            val host = resolveHealthyHost()
            val q = if (query.isBlank()) "chill" else query
            val url = "$host/v1/playlists/search?query=${Uri.encode(q)}&limit=$limit&app_name=$APP_NAME"
            val body = getString(url) ?: return@withContext emptyList()
            parseAudiusPlaylistsAsAlbums(body)
        }

    fun buildStreamUrl(trackId: String): String {
        return "$activeHost/v1/tracks/$trackId/stream?app_name=$APP_NAME"
    }

    private fun resolveHealthyHost(): String {
        return try {
            val body = getString("https://api.audius.co") ?: return activeHost
            val data = JSONObject(body).optJSONArray("data")
            if (data != null && data.length() > 0) {
                val chosen = data.getString(0).trimEnd('/')
                if (chosen.startsWith("http")) {
                    activeHost = chosen
                }
            }
            activeHost
        } catch (_: Exception) {
            activeHost
        }
    }

    private fun fetchTrackList(url: String, host: String): List<TrackItem> {
        val body = getString(url) ?: return emptyList()
        return parseTracks(body, host)
    }

    private fun getString(url: String): String? {
        return try {
            val req = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", "Musica/1.0 (Android)")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    Log.w(TAG, "HTTP ${resp.code} for $url")
                    return null
                }
                resp.body?.string()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Request failed for $url", e)
            null
        }
    }

    private fun parseTracks(jsonStr: String, host: String): List<TrackItem> {
        return try {
            val root = JSONObject(jsonStr)
            val data = root.optJSONArray("data") ?: JSONArray()
            val list = ArrayList<TrackItem>(data.length())
            for (i in 0 until data.length()) {
                val obj = data.optJSONObject(i) ?: continue
                val id = obj.optString("id")
                val title = obj.optString("title")
                if (id.isBlank() || title.isBlank()) continue
                val userObj = obj.optJSONObject("user")
                val artistName = userObj?.optString("name")?.takeIf { it.isNotBlank() } ?: "Unknown Artist"
                val artistId = userObj?.optString("id").orEmpty()
                val artworkObj = obj.optJSONObject("artwork")
                val coverUrl = artworkObj?.optString("480x480")
                    ?.takeIf { it.isNotBlank() }
                    ?: artworkObj?.optString("150x150").orEmpty()
                val durationSec = obj.optLong("duration", 180L).coerceAtLeast(30L)
                val playCount = obj.optLong("play_count", 0L)
                val favCount = obj.optLong("favorite_count", 0L)
                val genre = obj.optString("genre", "Electronic").ifBlank { "Electronic" }
                val desc = obj.optString("description", "").ifBlank {
                    "$genre • Dirilis oleh $artistName • Audio penuh berkualitas tinggi di Musica."
                }
                val lowerTitle = title.lowercase()
                val isRemix = lowerTitle.contains("remix") || lowerTitle.contains("flip") || lowerTitle.contains("edit")
                val isCover = lowerTitle.contains("cover") || lowerTitle.contains("acoustic")
                val isStreamable = obj.optBoolean("is_streamable", true)
                if (!isStreamable) continue

                list.add(
                    TrackItem(
                        id = id,
                        title = title,
                        artist = artistName,
                        artistId = artistId,
                        album = obj.optString("mood", genre).ifBlank { "Single" },
                        coverUrl = coverUrl,
                        streamUrl = "$host/v1/tracks/$id/stream?app_name=$APP_NAME",
                        durationMs = durationSec * 1000L,
                        description = desc,
                        listenerCount = (playCount / 4L).coerceAtLeast(favCount + 120L),
                        playCount = playCount.coerceAtLeast(1000L),
                        genre = genre,
                        isRemix = isRemix,
                        isCover = isCover
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Failed parsing tracks", e)
            emptyList()
        }
    }

    private fun parseArtists(jsonStr: String): List<ArtistProfile> {
        return try {
            val data = JSONObject(jsonStr).optJSONArray("data") ?: return emptyList()
            val list = ArrayList<ArtistProfile>(data.length())
            for (i in 0 until data.length()) {
                val obj = data.optJSONObject(i) ?: continue
                val id = obj.optString("id")
                val name = obj.optString("name")
                if (id.isBlank() || name.isBlank()) continue
                val picObj = obj.optJSONObject("profile_picture")
                val avatar = picObj?.optString("480x480")
                    ?.takeIf { it.isNotBlank() }
                    ?: picObj?.optString("150x150").orEmpty()
                list.add(
                    ArtistProfile(
                        id = id,
                        name = name,
                        handle = obj.optString("handle", name.lowercase().replace(" ", "_")),
                        avatarUrl = avatar,
                        followersCount = obj.optLong("follower_count", 0L),
                        trackCount = obj.optInt("track_count", 0),
                        bio = obj.optString("bio", "Artis resmi di katalog global Musica.")
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Failed parsing artists", e)
            emptyList()
        }
    }

    private fun parseItunesAlbums(jsonStr: String): List<AlbumItem> {
        return try {
            val results = JSONObject(jsonStr).optJSONArray("results") ?: return emptyList()
            val list = ArrayList<AlbumItem>(results.length())
            for (i in 0 until results.length()) {
                val obj = results.optJSONObject(i) ?: continue
                val id = obj.optLong("collectionId", i.toLong()).toString()
                val title = obj.optString("collectionName")
                val artist = obj.optString("artistName")
                if (title.isBlank() || artist.isBlank()) continue
                val art100 = obj.optString("artworkUrl100")
                val art600 = art100.replace("100x100bb", "600x600bb")
                val releaseYear = obj.optString("releaseDate", "2026").take(4)
                list.add(
                    AlbumItem(
                        id = "itunes_$id",
                        title = title,
                        artist = artist,
                        coverUrl = art600,
                        year = releaseYear,
                        genre = obj.optString("primaryGenreName", "Pop"),
                        tracksCount = obj.optInt("trackCount", 10),
                        description = "Album resmi oleh $artist ($releaseYear)"
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Failed parsing iTunes albums", e)
            emptyList()
        }
    }

    private fun parseAudiusPlaylistsAsAlbums(jsonStr: String): List<AlbumItem> {
        return try {
            val data = JSONObject(jsonStr).optJSONArray("data") ?: return emptyList()
            val list = ArrayList<AlbumItem>(data.length())
            for (i in 0 until data.length()) {
                val obj = data.optJSONObject(i) ?: continue
                val id = obj.optString("id")
                val title = obj.optString("playlist_name")
                if (id.isBlank() || title.isBlank()) continue
                val userObj = obj.optJSONObject("user")
                val artist = userObj?.optString("name").orEmpty().ifBlank { "Curated" }
                val artObj = obj.optJSONObject("artwork")
                val cover = artObj?.optString("480x480")?.takeIf { it.isNotBlank() }
                    ?: artObj?.optString("150x150").orEmpty()
                list.add(
                    AlbumItem(
                        id = id,
                        title = title,
                        artist = artist,
                        artistId = userObj?.optString("id").orEmpty(),
                        coverUrl = cover,
                        year = "2026",
                        genre = if (obj.optBoolean("is_album", false)) "Album" else "Playlist",
                        tracksCount = obj.optInt("track_count", 8),
                        description = obj.optString("description", "Koleksi pilihan oleh $artist")
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }
}
