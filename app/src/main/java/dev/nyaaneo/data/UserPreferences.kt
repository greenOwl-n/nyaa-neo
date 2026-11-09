package dev.nyaaneo.data

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.nyaaneo.model.Torrent
import kotlinx.coroutines.flow.map

private val Context.neoDataStore by preferencesDataStore(
    name = "nyaa_neo",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

data class Preferences(val favorites: List<Torrent> = emptyList(), val compact: Boolean = false, val showArtwork: Boolean = true)

class UserPreferences(context: Context) {
    private val store = context.applicationContext.neoDataStore
    val preferences = store.data.map { values ->
        Preferences(FavoritesCodec.decode(values[FAVORITES] ?: "[]"), values[COMPACT] ?: false, values[ARTWORK] ?: true)
    }
    suspend fun toggleFavorite(torrent: Torrent) {
        store.edit { values ->
            val current = FavoritesCodec.decode(values[FAVORITES] ?: "[]")
            val next = if (current.any { it.id == torrent.id }) current.filterNot { it.id == torrent.id }
                else listOf(torrent) + current
            values[FAVORITES] = FavoritesCodec.encode(next)
        }
    }
    suspend fun setCompact(compact: Boolean) { store.edit { it[COMPACT] = compact } }
    suspend fun refreshFavorite(torrent: Torrent) {
        store.edit { values ->
            val current = FavoritesCodec.decode(values[FAVORITES] ?: "[]")
            if (current.any { it.id == torrent.id }) {
                values[FAVORITES] = FavoritesCodec.encode(current.map { if (it.id == torrent.id) torrent else it })
            }
        }
    }
    suspend fun setArtwork(enabled: Boolean) { store.edit { it[ARTWORK] = enabled } }

    companion object {
        private val FAVORITES = stringPreferencesKey("favorites_v1")
        private val COMPACT = booleanPreferencesKey("compact")
        private val ARTWORK = booleanPreferencesKey("artwork")
    }
}
