package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloaded_tracks")
data class DownloadedTrackEntity(
    @PrimaryKey val trackId: String,
    val title: String,
    val artist: String,
    val artistId: String,
    val album: String,
    val coverUrl: String,
    val streamUrl: String,
    val localFilePath: String,
    val durationMs: Long,
    val description: String,
    val listenerCount: Long,
    val genre: String,
    val isLocalSource: Boolean = false,
    val downloadedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "local_playlists")
data class LocalPlaylistEntity(
    @PrimaryKey val playlistId: String,
    val name: String,
    val shareCode: String,
    val isOfflineSaved: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "local_playlist_tracks", primaryKeys = ["playlistId", "trackId"])
data class LocalPlaylistTrackEntity(
    val playlistId: String,
    val trackId: String,
    val title: String,
    val artist: String,
    val artistId: String,
    val album: String,
    val coverUrl: String,
    val streamUrl: String,
    val durationMs: Long,
    val description: String,
    val listenerCount: Long,
    val genre: String,
    val addedAt: Long = System.currentTimeMillis()
)
