package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MusicaDao {
    @Query("SELECT * FROM downloaded_tracks ORDER BY downloadedAt DESC")
    fun observeDownloadedTracks(): Flow<List<DownloadedTrackEntity>>

    @Query("SELECT * FROM downloaded_tracks WHERE trackId = :trackId LIMIT 1")
    suspend fun getDownloadedTrack(trackId: String): DownloadedTrackEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDownloadedTrack(track: DownloadedTrackEntity)

    @Query("DELETE FROM downloaded_tracks WHERE trackId = :trackId")
    suspend fun deleteDownloadedTrack(trackId: String)

    @Query("SELECT * FROM local_playlists ORDER BY createdAt DESC")
    fun observePlaylists(): Flow<List<LocalPlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlaylist(playlist: LocalPlaylistEntity)

    @Query("UPDATE local_playlists SET isOfflineSaved = :offlineSaved WHERE playlistId = :playlistId")
    suspend fun markPlaylistOffline(playlistId: String, offlineSaved: Boolean)

    @Query("DELETE FROM local_playlists WHERE playlistId = :playlistId")
    suspend fun deletePlaylist(playlistId: String)

    @Query("DELETE FROM local_playlist_tracks WHERE playlistId = :playlistId")
    suspend fun deletePlaylistTracks(playlistId: String)

    @Query("SELECT * FROM local_playlist_tracks WHERE playlistId = :playlistId ORDER BY addedAt ASC")
    fun observePlaylistTracks(playlistId: String): Flow<List<LocalPlaylistTrackEntity>>

    @Query("SELECT * FROM local_playlist_tracks ORDER BY addedAt ASC")
    fun observeAllPlaylistTracks(): Flow<List<LocalPlaylistTrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlaylistTrack(track: LocalPlaylistTrackEntity)

    @Query("DELETE FROM local_playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String)
}
