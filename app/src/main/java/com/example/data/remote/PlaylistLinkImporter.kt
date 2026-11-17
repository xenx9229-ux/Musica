package com.example.data.remote

import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ParsedExternalPlaylist(
    val sourcePlatform: String,
    val playlistTitle: String,
    val candidateQueries: List<String>
)

class PlaylistLinkImporter {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun inspectPlaylistInput(rawInput: String): ParsedExternalPlaylist =
        withContext(Dispatchers.IO) {
            val trimmed = rawInput.trim()
            val platform = when {
                trimmed.contains("spotify.com", ignoreCase = true) -> "Spotify"
                trimmed.contains("music.youtube.com", ignoreCase = true) -> "YouTube Music"
                trimmed.contains("youtube.com", ignoreCase = true) || trimmed.contains("youtu.be", ignoreCase = true) -> "YouTube"
                else -> "Custom List"
            }

            if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                val oembedUrl = when (platform) {
                    "Spotify" -> "https://open.spotify.com/oembed?url=${Uri.encode(trimmed)}"
                    "YouTube Music", "YouTube" -> "https://www.youtube.com/oembed?url=${Uri.encode(trimmed)}&format=json"
                    else -> null
                }
                val titleFromOembed = oembedUrl?.let { fetchOembedTitle(it) }
                val htmlTitles = fetchHtmlTrackHints(trimmed)
                val queries = if (htmlTitles.isNotEmpty()) {
                    htmlTitles
                } else {
                    listOfNotNull(titleFromOembed?.takeIf { it.isNotBlank() })
                }
                return@withContext ParsedExternalPlaylist(
                    sourcePlatform = platform,
                    playlistTitle = titleFromOembed?.ifBlank { null } ?: "Impor $platform",
                    candidateQueries = queries.ifEmpty { listOf("electronic chill pop", "synthwave night", "lofi beats") }
                )
            }

            // Plain text lines pasted by user (e.g. "Artist - Song")
            val lines = trimmed.lines()
                .map { it.trim() }
                .filter { it.length >= 3 }
                .take(20)
            ParsedExternalPlaylist(
                sourcePlatform = platform,
                playlistTitle = "Impor Playlist (${lines.size} lagu)",
                candidateQueries = lines
            )
        }

    private fun fetchOembedTitle(url: String): String? {
        return try {
            val req = Request.Builder().url(url).header("User-Agent", "Musica/1.0").build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                JSONObject(body).optString("title", "").takeIf { it.isNotBlank() }
            }
        } catch (e: Exception) {
            Log.w("PlaylistImporter", "oEmbed lookup skipped: ${e.message}")
            null
        }
    }

    private fun fetchHtmlTrackHints(pageUrl: String): List<String> {
        return try {
            val req = Request.Builder()
                .url(pageUrl)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return emptyList()
                val html = resp.body?.string().orEmpty()
                val metaRegex = Regex("""<meta\s+(?:property|name)="(?:og:title|music:song|twitter:title)"\s+content="([^"]+)"""")
                metaRegex.findAll(html)
                    .map { it.groupValues[1].replace("&amp;", "&").trim() }
                    .filter { it.isNotBlank() && !it.equals("Spotify", true) && !it.equals("YouTube Music", true) }
                    .distinct()
                    .take(12)
                    .toList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
