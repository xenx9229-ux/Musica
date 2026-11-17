package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.GlassMode
import com.example.ui.theme.GlassThemeConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Context.musicaDataStore: DataStore<Preferences> by preferencesDataStore(name = "musica_prefs")

data class LocalAppState(
    val themeConfig: GlassThemeConfig = GlassThemeConfig(),
    val isGuestSession: Boolean = false,
    val guestUsername: String = "guest_musica",
    val guestDisplayName: String = "Tamu Musica",
    val guestBio: String = "Exploring Liquid Glass soundwaves",
    val guestAvatarIndex: Int = 0,
    val guestBorderStyle: String = "streak_auto",
    val guestFrameStyle: String = "liquid_halo",
    val appStreakDays: Int = 1,
    val lastActiveDate: String = "",
    val favoriteTrackIds: Set<String> = emptySet(),
    val followedArtistIds: Set<String> = emptySet()
)

class ThemePreferencesStore(private val context: Context) {

    private object Keys {
        val GLASS_MODE = stringPreferencesKey("glass_mode")
        val FROST_OPACITY = floatPreferencesKey("frost_opacity")
        val CORNER_SOFTNESS = floatPreferencesKey("corner_softness")
        val ACCENT_HEX = stringPreferencesKey("accent_hex")
        val ADAPTIVE_ALBUM = booleanPreferencesKey("adaptive_album")
        val IS_GUEST = booleanPreferencesKey("is_guest")
        val GUEST_USERNAME = stringPreferencesKey("guest_username")
        val GUEST_DISPLAY_NAME = stringPreferencesKey("guest_display_name")
        val GUEST_BIO = stringPreferencesKey("guest_bio")
        val GUEST_AVATAR_INDEX = intPreferencesKey("guest_avatar_index")
        val GUEST_BORDER_STYLE = stringPreferencesKey("guest_border_style")
        val GUEST_FRAME_STYLE = stringPreferencesKey("guest_frame_style")
        val APP_STREAK_DAYS = intPreferencesKey("app_streak_days")
        val LAST_ACTIVE_DATE = stringPreferencesKey("last_active_date")
        val FAVORITE_TRACKS = stringSetPreferencesKey("favorite_tracks")
        val FOLLOWED_ARTISTS = stringSetPreferencesKey("followed_artists")
    }

    val appState: Flow<LocalAppState> = context.musicaDataStore.data.map { prefs ->
        val modeName = prefs[Keys.GLASS_MODE] ?: GlassMode.MIDNIGHT_NEON.name
        val mode = GlassMode.entries.firstOrNull { it.name == modeName } ?: GlassMode.MIDNIGHT_NEON
        LocalAppState(
            themeConfig = GlassThemeConfig(
                glassMode = mode,
                frostOpacity = prefs[Keys.FROST_OPACITY] ?: 0.24f,
                cornerSoftnessDp = prefs[Keys.CORNER_SOFTNESS] ?: 26f,
                accentHex = prefs[Keys.ACCENT_HEX] ?: "#38BDF8",
                adaptiveAlbumColor = prefs[Keys.ADAPTIVE_ALBUM] ?: true
            ),
            isGuestSession = prefs[Keys.IS_GUEST] ?: false,
            guestUsername = prefs[Keys.GUEST_USERNAME] ?: "guest_musica",
            guestDisplayName = prefs[Keys.GUEST_DISPLAY_NAME] ?: "Tamu Musica",
            guestBio = prefs[Keys.GUEST_BIO] ?: "Exploring Liquid Glass soundwaves",
            guestAvatarIndex = prefs[Keys.GUEST_AVATAR_INDEX] ?: 0,
            guestBorderStyle = prefs[Keys.GUEST_BORDER_STYLE] ?: "streak_auto",
            guestFrameStyle = prefs[Keys.GUEST_FRAME_STYLE] ?: "liquid_halo",
            appStreakDays = prefs[Keys.APP_STREAK_DAYS] ?: 1,
            lastActiveDate = prefs[Keys.LAST_ACTIVE_DATE] ?: "",
            favoriteTrackIds = prefs[Keys.FAVORITE_TRACKS] ?: emptySet(),
            followedArtistIds = prefs[Keys.FOLLOWED_ARTISTS] ?: emptySet()
        )
    }

    suspend fun updateThemeConfig(config: GlassThemeConfig) {
        context.musicaDataStore.edit { prefs ->
            prefs[Keys.GLASS_MODE] = config.glassMode.name
            prefs[Keys.FROST_OPACITY] = config.frostOpacity
            prefs[Keys.CORNER_SOFTNESS] = config.cornerSoftnessDp
            prefs[Keys.ACCENT_HEX] = config.accentHex
            prefs[Keys.ADAPTIVE_ALBUM] = config.adaptiveAlbumColor
        }
    }

    suspend fun setGuestSession(enabled: Boolean) {
        context.musicaDataStore.edit { prefs ->
            prefs[Keys.IS_GUEST] = enabled
        }
    }

    suspend fun updateGuestProfile(
        username: String,
        displayName: String,
        bio: String,
        avatarIndex: Int,
        borderStyle: String,
        frameStyle: String
    ) {
        context.musicaDataStore.edit { prefs ->
            prefs[Keys.GUEST_USERNAME] = username
            prefs[Keys.GUEST_DISPLAY_NAME] = displayName
            prefs[Keys.GUEST_BIO] = bio
            prefs[Keys.GUEST_AVATAR_INDEX] = avatarIndex
            prefs[Keys.GUEST_BORDER_STYLE] = borderStyle
            prefs[Keys.GUEST_FRAME_STYLE] = frameStyle
        }
    }

    suspend fun toggleFavorite(trackId: String): Boolean {
        var added = false
        context.musicaDataStore.edit { prefs ->
            val current = prefs[Keys.FAVORITE_TRACKS]?.toMutableSet() ?: mutableSetOf()
            if (current.contains(trackId)) {
                current.remove(trackId)
                added = false
            } else {
                current.add(trackId)
                added = true
            }
            prefs[Keys.FAVORITE_TRACKS] = current
        }
        return added
    }

    suspend fun toggleFollowArtist(artistId: String): Boolean {
        var followed = false
        context.musicaDataStore.edit { prefs ->
            val current = prefs[Keys.FOLLOWED_ARTISTS]?.toMutableSet() ?: mutableSetOf()
            if (current.contains(artistId)) {
                current.remove(artistId)
                followed = false
            } else {
                current.add(artistId)
                followed = true
            }
            prefs[Keys.FOLLOWED_ARTISTS] = current
        }
        return followed
    }

    suspend fun recordDailyCheckIn(): Int {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        var resultingDays = 1
        context.musicaDataStore.edit { prefs ->
            val lastDate = prefs[Keys.LAST_ACTIVE_DATE] ?: ""
            val currentDays = prefs[Keys.APP_STREAK_DAYS] ?: 0
            if (lastDate == today) {
                resultingDays = currentDays.coerceAtLeast(1)
            } else {
                resultingDays = currentDays + 1
                prefs[Keys.APP_STREAK_DAYS] = resultingDays
                prefs[Keys.LAST_ACTIVE_DATE] = today
            }
        }
        return resultingDays
    }

    suspend fun setDemoStreakDays(days: Int) {
        context.musicaDataStore.edit { prefs ->
            prefs[Keys.APP_STREAK_DAYS] = days.coerceAtLeast(1)
        }
    }
}
