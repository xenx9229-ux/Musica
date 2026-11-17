package com.example.data.remote

import android.net.Uri
import android.util.Log
import com.example.data.model.LyricLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private const val TAG = "LrclibApi"

class LrclibApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    private val lrcRegex = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})]\s*(.*)""")

    suspend fun fetchLyrics(title: String, artist: String, durationMs: Long): List<LyricLine> =
        withContext(Dispatchers.IO) {
            val cleanTitle = title.replace(Regex("""\(.*?\)|\[.*?]"""), "").trim()
            val durationSec = (durationMs / 1000L).coerceAtLeast(60L)

            // 1. Direct lookup
            val exactUrl =
                "https://lrclib.net/api/get?track_name=${Uri.encode(cleanTitle)}&artist_name=${Uri.encode(artist)}&duration=$durationSec"
            val exactBody = requestJson(exactUrl)
            if (exactBody != null) {
                val parsed = extractLyricsFromObject(JSONObject(exactBody), durationMs)
                if (parsed.isNotEmpty()) return@withContext parsed
            }

            // 2. Fallback search
            val searchUrl = "https://lrclib.net/api/search?q=${Uri.encode("$cleanTitle $artist")}"
            val searchBody = requestJson(searchUrl)
            if (searchBody != null) {
                val arr = JSONArray(searchBody)
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val parsed = extractLyricsFromObject(obj, durationMs)
                    if (parsed.isNotEmpty()) return@withContext parsed
                }
            }

            emptyList()
        }

    private fun requestJson(url: String): String? {
        return try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "Musica v1.0 (https://musica.xenproject.app)")
                .header("Accept", "application/json")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                resp.body?.string()
            }
        } catch (e: Exception) {
            Log.w(TAG, "LRCLIB request failed: ${e.message}")
            null
        }
    }

    private fun extractLyricsFromObject(obj: JSONObject, durationMs: Long): List<LyricLine> {
        val synced = obj.optString("syncedLyrics", "")
        if (synced.isNotBlank() && synced != "null") {
            val parsed = parseLrc(synced)
            if (parsed.isNotEmpty()) return parsed
        }
        val plain = obj.optString("plainLyrics", "")
        if (plain.isNotBlank() && plain != "null") {
            val lines = plain.lines().map { it.trim() }.filter { it.isNotEmpty() }
            if (lines.isNotEmpty()) {
                val step = (durationMs.coerceAtLeast(60_000L) / (lines.size + 1)).coerceAtLeast(2500L)
                return lines.mapIndexed { idx, line ->
                    LyricLine(timeMs = (idx + 1) * step, text = line)
                }
            }
        }
        return emptyList()
    }

    fun parseLrc(lrcText: String): List<LyricLine> {
        val out = mutableListOf<LyricLine>()
        for (rawLine in lrcText.lines()) {
            val match = lrcRegex.find(rawLine.trim()) ?: continue
            val min = match.groupValues[1].toLongOrNull() ?: 0L
            val sec = match.groupValues[2].toLongOrNull() ?: 0L
            val fracStr = match.groupValues[3]
            val millis = if (fracStr.length == 2) (fracStr.toLongOrNull() ?: 0L) * 10L else (fracStr.toLongOrNull() ?: 0L)
            val text = match.groupValues[4].trim()
            if (text.isNotEmpty()) {
                out.add(LyricLine(timeMs = (min * 60_000L) + (sec * 1000L) + millis, text = text))
            }
        }
        return out.sortedBy { it.timeMs }
    }
}
