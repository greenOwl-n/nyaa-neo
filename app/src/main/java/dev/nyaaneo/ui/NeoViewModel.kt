package dev.nyaaneo.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.nyaaneo.data.FavoritesCodec
import dev.nyaaneo.NeoApplication
import dev.nyaaneo.model.Category
import dev.nyaaneo.model.FeedFilter
import dev.nyaaneo.model.SortOrder
import dev.nyaaneo.model.Torrent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

data class NeoState(
    val query: String = "",
    val category: Category = Category.ALL,
    val filter: FeedFilter = FeedFilter.ALL,
    val sort: SortOrder = SortOrder.RECENT,
    val torrents: List<Torrent> = emptyList(),
    val favorites: List<Torrent> = emptyList(),
    val compact: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
    val searched: Boolean = false,
    val resultsLabel: String = "Últimos torrents",
)

class NeoViewModel(private val app: NeoApplication, private val saved: SavedStateHandle) : ViewModel() {
    private val mutableState = MutableStateFlow(NeoState(
        query = saved["query"] ?: "",
        category = Category.entries.firstOrNull { it.name == saved.get<String>("category") } ?: Category.ALL,
        filter = FeedFilter.entries.firstOrNull { it.name == saved.get<String>("filter") } ?: FeedFilter.ALL,
        sort = SortOrder.entries.firstOrNull { it.name == saved.get<String>("sort") } ?: SortOrder.RECENT,
    ))
    val state = mutableState.asStateFlow()
    private val messages = Channel<String>(Channel.BUFFERED)
    val events = messages.receiveAsFlow()
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            try {
                app.preferences.preferences.collect { prefs ->
                    mutableState.update { it.copy(favorites = prefs.favorites, compact = prefs.compact) }
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { messages.send("No se pudieron leer tus preferencias. Reinicia la aplicación para reintentar.") }
        }
        search()
    }

    fun setQuery(query: String) {
        saved["query"] = query
        mutableState.update { it.copy(query = query) }
    }
    fun setCategory(category: Category) {
        saved["category"] = category.name
        mutableState.update { it.copy(category = category) }
        search()
    }
    fun setFilter(filter: FeedFilter) {
        saved["filter"] = filter.name
        mutableState.update { it.copy(filter = filter) }
        search()
    }
    fun setSort(sort: SortOrder) {
        saved["sort"] = sort.name
        mutableState.update { it.copy(sort = sort) }
    }
    fun search() {
        searchJob?.cancel()
        val request = state.value
        searchJob = viewModelScope.launch {
            mutableState.update { it.copy(loading = true, error = null, torrents = emptyList(), searched = true) }
            try {
                val torrents = app.repository.search(request.query, request.category, request.filter)
                mutableState.update { it.copy(loading = false, torrents = torrents,
                    resultsLabel = "${request.query.trim().ifEmpty { "Últimos torrents" }} · ${request.category.label} · ${request.filter.label}") }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                val message = if (e is IOException) e.message ?: "No se pudo conectar con Nyaa.si."
                    else "No se pudo interpretar el RSS de Nyaa.si."
                mutableState.update { it.copy(loading = false, error = message) }
            }
        }
    }
    fun toggleFavorite(torrent: Torrent) = persist { app.preferences.toggleFavorite(torrent) }
    fun setCompact(compact: Boolean) = persist { app.preferences.setCompact(compact) }
    private fun persist(action: suspend () -> Unit) {
        viewModelScope.launch {
            try { action() }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { messages.send("No se pudo guardar el cambio. Comprueba el almacenamiento disponible.") }
        }
    }
    fun selectTorrent(torrent: Torrent) { saved["detail"] = FavoritesCodec.encode(listOf(torrent)) }
    fun torrent(id: String): Torrent? = state.value.let { state ->
        state.torrents.firstOrNull { it.id == id } ?: state.favorites.firstOrNull { it.id == id }
            ?: saved.get<String>("detail")?.let(FavoritesCodec::decode)?.firstOrNull { it.id == id }
    }
}
