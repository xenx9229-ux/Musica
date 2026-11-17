package com.example.player

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.model.RepeatMode
import com.example.data.model.SleepTimerOption
import com.example.data.model.TrackItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

private const val TAG = "MusicaPlayer"

data class PlayerPlaybackState(
    val currentTrack: TrackItem? = null,
    val queue: List<TrackItem> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val shuffleEnabled: Boolean = false,
    val sleepTimerOption: SleepTimerOption = SleepTimerOption.OFF,
    val sleepRemainingSec: Int = 0,
    val errorMessage: String? = null
)

class MusicaPlayerController(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(appContext)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build(),
            true
        )
        .setHandleAudioBecomingNoisy(true)
        .build()

    private val _state = MutableStateFlow(PlayerPlaybackState())
    val state: StateFlow<PlayerPlaybackState> = _state.asStateFlow()

    private var positionJob: Job? = null
    private var sleepJob: Job? = null
    private var onTrackChangedCallback: ((TrackItem) -> Unit)? = null

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.update { it.copy(isPlaying = isPlaying) }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val buffering = playbackState == Player.STATE_BUFFERING
                val duration = exoPlayer.duration.takeIf { it > 0 }
                    ?: _state.value.currentTrack?.durationMs
                    ?: 0L
                _state.update {
                    it.copy(isBuffering = buffering, durationMs = duration)
                }
                if (playbackState == Player.STATE_ENDED) {
                    handleTrackEnded()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "ExoPlayer error: ${error.errorCodeName}", error)
                _state.update {
                    it.copy(
                        isPlaying = false,
                        isBuffering = false,
                        errorMessage = "Gagal memutar audio: ${error.localizedMessage ?: error.errorCodeName}"
                    )
                }
            }
        })

        startPositionTicker()
    }

    fun setOnTrackChangedListener(listener: (TrackItem) -> Unit) {
        onTrackChangedCallback = listener
    }

    fun playTrack(track: TrackItem, queueContext: List<TrackItem> = emptyList()) {
        val effectiveQueue = if (queueContext.isNotEmpty()) {
            if (queueContext.any { it.id == track.id }) queueContext else listOf(track) + queueContext
        } else {
            val currentQ = _state.value.queue
            if (currentQ.any { it.id == track.id }) currentQ else listOf(track) + currentQ
        }
        val idx = effectiveQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        _state.update {
            it.copy(
                currentTrack = track,
                queue = effectiveQueue,
                currentIndex = idx,
                positionMs = 0L,
                durationMs = track.durationMs,
                errorMessage = null
            )
        }
        loadAndPlay(track)
        onTrackChangedCallback?.invoke(track)
    }

    fun enqueueTrack(track: TrackItem) {
        _state.update { current ->
            val updated = if (current.queue.any { it.id == track.id }) {
                current.queue
            } else {
                current.queue + track
            }
            current.copy(queue = updated)
        }
        if (_state.value.currentTrack == null) {
            playTrack(track)
        }
    }

    fun removeFromQueue(trackId: String) {
        _state.update { current ->
            val filtered = current.queue.filterNot { it.id == trackId }
            val newIndex = filtered.indexOfFirst { it.id == current.currentTrack?.id }
            current.copy(queue = filtered, currentIndex = newIndex)
        }
    }

    fun togglePlayPause() {
        val current = _state.value.currentTrack ?: return
        if (exoPlayer.playbackState == Player.STATE_IDLE) {
            loadAndPlay(current)
            return
        }
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            exoPlayer.volume = 1f
            exoPlayer.play()
        }
    }

    fun seekTo(positionMs: Long) {
        val target = positionMs.coerceAtLeast(0L)
        exoPlayer.seekTo(target)
        _state.update { it.copy(positionMs = target) }
    }

    fun skipNext() {
        val current = _state.value
        if (current.queue.isEmpty()) return
        val nextIndex = if (current.shuffleEnabled && current.queue.size > 1) {
            (current.queue.indices - current.currentIndex).random()
        } else {
            (current.currentIndex + 1) % current.queue.size
        }
        val nextTrack = current.queue.getOrNull(nextIndex) ?: return
        _state.update {
            it.copy(
                currentTrack = nextTrack,
                currentIndex = nextIndex,
                positionMs = 0L,
                durationMs = nextTrack.durationMs,
                errorMessage = null
            )
        }
        loadAndPlay(nextTrack)
        onTrackChangedCallback?.invoke(nextTrack)
    }

    fun skipPrevious() {
        val current = _state.value
        if (exoPlayer.currentPosition > 3500L) {
            seekTo(0L)
            return
        }
        if (current.queue.isEmpty()) return
        val prevIndex = if (current.currentIndex <= 0) current.queue.lastIndex else current.currentIndex - 1
        val prevTrack = current.queue.getOrNull(prevIndex) ?: return
        _state.update {
            it.copy(
                currentTrack = prevTrack,
                currentIndex = prevIndex,
                positionMs = 0L,
                durationMs = prevTrack.durationMs,
                errorMessage = null
            )
        }
        loadAndPlay(prevTrack)
        onTrackChangedCallback?.invoke(prevTrack)
    }

    fun cycleRepeatMode() {
        val next = when (_state.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        exoPlayer.repeatMode = when (next) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
        }
        _state.update { it.copy(repeatMode = next) }
    }

    fun toggleShuffle() {
        _state.update { it.copy(shuffleEnabled = !it.shuffleEnabled) }
    }

    fun setSleepTimer(option: SleepTimerOption) {
        sleepJob?.cancel()
        if (option == SleepTimerOption.OFF) {
            exoPlayer.volume = 1f
            _state.update { it.copy(sleepTimerOption = option, sleepRemainingSec = 0) }
            return
        }
        if (option == SleepTimerOption.END_OF_TRACK) {
            _state.update { it.copy(sleepTimerOption = option, sleepRemainingSec = 1) }
            return
        }
        val totalSeconds = option.minutes * 60
        _state.update { it.copy(sleepTimerOption = option, sleepRemainingSec = totalSeconds) }
        sleepJob = scope.launch {
            var remaining = totalSeconds
            while (remaining > 0) {
                delay(1000L)
                remaining--
                if (remaining <= 10 && remaining > 0) {
                    exoPlayer.volume = remaining / 10f
                }
                _state.update { it.copy(sleepRemainingSec = remaining) }
            }
            exoPlayer.pause()
            exoPlayer.volume = 1f
            _state.update {
                it.copy(sleepTimerOption = SleepTimerOption.OFF, sleepRemainingSec = 0)
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    private fun handleTrackEnded() {
        val current = _state.value
        if (current.sleepTimerOption == SleepTimerOption.END_OF_TRACK) {
            exoPlayer.pause()
            _state.update { it.copy(sleepTimerOption = SleepTimerOption.OFF, sleepRemainingSec = 0) }
            return
        }
        when (current.repeatMode) {
            RepeatMode.ONE -> {
                seekTo(0L)
                exoPlayer.play()
            }
            RepeatMode.ALL -> skipNext()
            RepeatMode.OFF -> {
                if (current.currentIndex in 0 until current.queue.lastIndex) {
                    skipNext()
                } else {
                    _state.update { it.copy(isPlaying = false, positionMs = 0L) }
                }
            }
        }
    }

    private fun loadAndPlay(track: TrackItem) {
        val mediaUri = when {
            !track.localFilePath.isNullOrBlank() && File(track.localFilePath).exists() ->
                Uri.fromFile(File(track.localFilePath))
            track.streamUrl.isNotBlank() -> Uri.parse(track.streamUrl)
            else -> {
                _state.update { it.copy(errorMessage = "URL audio tidak tersedia untuk lagu ini.") }
                return
            }
        }
        val item = MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(mediaUri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .setAlbumTitle(track.album)
                    .build()
            )
            .build()
        exoPlayer.volume = 1f
        exoPlayer.setMediaItem(item)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    private fun startPositionTicker() {
        positionJob?.cancel()
        positionJob = scope.launch {
            while (true) {
                if (exoPlayer.isPlaying) {
                    val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
                    val dur = exoPlayer.duration.takeIf { it > 0 }
                        ?: _state.value.currentTrack?.durationMs
                        ?: 0L
                    _state.update { it.copy(positionMs = pos, durationMs = dur) }
                }
                delay(180L)
            }
        }
    }

    fun release() {
        positionJob?.cancel()
        sleepJob?.cancel()
        exoPlayer.release()
    }
}
