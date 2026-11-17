package com.example.data.repository

import android.util.Log
import com.example.data.model.LyricLine
import com.example.data.model.TrackItem
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "MusicaAiRepo"

sealed interface AiUiState {
    data object Idle : AiUiState
    data object Loading : AiUiState
    data class Streaming(val text: String) : AiUiState
    data class Success(val text: String) : AiUiState
    data class Error(val message: String) : AiUiState
}

class MusicaAiRepository {
    private val chatModel by lazy {
        Firebase.ai.generativeModel("gemini-3.8-flash")
    }

    private val translationSchema = Schema.obj(
        properties = mapOf(
            "translations" to Schema.array(
                items = Schema.string(),
                description = "Translated lyric lines matching input order"
            )
        )
    )

    private val translationModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = translationSchema
            }
        )
    }

    fun askMusicaAiStream(
        question: String,
        currentTrack: TrackItem?
    ): Flow<AiUiState> = flow {
        emit(AiUiState.Loading)
        val trackContext = if (currentTrack != null) {
            "Lagu yang sedang diputar pengguna: '${currentTrack.title}' oleh '${currentTrack.artist}' (Genre: ${currentTrack.genre}). "
        } else ""
        val systemGuidance =
            "Kamu adalah Asisten AI resmi di aplikasi Musica (Creator by XenProject). " +
            "Jawab pertanyaan seputar musik, artis, makna lirik, rekomendasi lagu, teori musik, atau panduan fitur aplikasi Musica " +
            "(Dynamic Island, Liquid Glass 2 mode, geser kanan antrean / geser kiri playlist, Poster Studio, Discord Listening Room, Streak 3 hari & milestone). " +
            "Gunakan bahasa Indonesia yang ramah, ringkas, dan akurat.\n\n" +
            trackContext + "Pertanyaan pengguna: $question"

        val accumulated = StringBuilder()
        try {
            withTimeout(60_000L) {
                chatModel.generateContentStream(systemGuidance).collect { chunk ->
                    val chunkText = chunk.text.orEmpty()
                    if (chunkText.isNotEmpty()) {
                        accumulated.append(chunkText)
                        emit(AiUiState.Streaming(accumulated.toString()))
                    }
                }
            }
            if (accumulated.isNotBlank()) {
                emit(AiUiState.Success(accumulated.toString()))
            } else {
                emit(AiUiState.Error("Asisten AI tidak mengembalikan jawaban."))
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI stream timed out", e)
            emit(AiUiState.Error("Permintaan AI melebihi batas waktu 60 detik. Silakan coba lagi."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI error", e)
            emit(AiUiState.Error(e.localizedMessage ?: "Gagal menghubungi Musica AI."))
        }
    }

    suspend fun translateLyrics(
        lines: List<LyricLine>,
        targetLanguage: String
    ): Result<List<LyricLine>> {
        if (lines.isEmpty()) return Result.success(emptyList())
        return try {
            val joined = lines.mapIndexed { idx, l -> "${idx + 1}. ${l.text}" }.joinToString("\n")
            val prompt =
                "Terjemahkan setiap baris lirik berikut ke bahasa $targetLanguage secara puitis dan natural. " +
                "Kembalikan tepat ${lines.size} item dalam array JSON 'translations'.\n\n$joined"
            val response = withTimeout(60_000L) {
                translationModel.generateContent(prompt)
            }
            val jsonStr = response.text ?: throw IllegalStateException("Respons terjemahan kosong.")
            val json = JSONObject(jsonStr)
            val arr = json.getJSONArray("translations")
            val translated = lines.mapIndexed { index, line ->
                val tr = if (index < arr.length()) arr.optString(index, line.text) else line.text
                line.copy(translatedText = tr)
            }
            Result.success(translated)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Lyrics translation failed", e)
            Result.failure(e)
        }
    }
}
