package dev.nyaaneo.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.nyaaneo.NeoApplication
import dev.nyaaneo.data.FavoritesCodec
import dev.nyaaneo.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.IOException

data class NeoState(
    val query: String = "",
    val section: ContentSection = ContentSection.ANIME,
    val category: Category = Category.ANIME,
    val adultCategory: AdultCategory = AdultCategory.ANIME,
    val adultEnabled: Boolean = false,
    val filter: FeedFilter = FeedFilter.ALL,
    val sort: SortOrder = SortOrder.RECENT,
    val torrents: List<Torrent> = emptyList(),
    val favorites: List<Torrent> = emptyList(),
    val compact: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
    val searched: Boolean = false,
    val resultsLabel: String = "Últimos torrents",
    val language: LanguageFilter = LanguageFilter.ALL,
    val showArtwork: Boolean = false,
    val artworks: Map<String, AnimeArtwork?> = emptyMap(),
    val artworkLoading: Set<String> = emptySet(),
    val details: Map<String, TorrentPageDetails> = emptyMap(),
    val detailsLoading: Set<String> = emptySet(),
    val detailsErrors: Map<String, String> = emptyMap(),
)

class NeoViewModel(private val app: NeoApplication, private val saved: SavedStateHandle) : ViewModel() {
    private val mutableState = MutableStateFlow(restoredState(saved))
    val state = mutableState.asStateFlow()
    private val messages = Channel<String>(Channel.BUFFERED)
    val events = messages.receiveAsFlow()
    private var searchJob: Job? = null
    private val artworkJobs = mutableMapOf<String, Job>()

    init {
        viewModelScope.launch {
            try {
                app.preferences.preferences.collect { prefs ->
                    val wasEnabled = state.value.adultEnabled
                    mutableState.update { it.copy(favorites = prefs.favorites, compact = prefs.compact,
                        showArtwork = prefs.showArtwork, adultEnabled = prefs.adultEnabled) }
                    if (state.value.section == ContentSection.ADULT && prefs.adultEnabled && !wasEnabled) search()
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
    fun setSection(section: ContentSection) {
        saved["section"] = section.name
        saved["category"] = section.defaultCategory.name
        saved["language"] = LanguageFilter.ALL.name
        mutableState.update { it.copy(section = section, category = section.defaultCategory, language = LanguageFilter.ALL) }
        search()
    }
    fun setCategory(category: Category) {
        if (category !in state.value.section.categories) return
        saved["category"] = category.name
        mutableState.update { it.copy(category = category) }
        search()
    }
    fun setAdultCategory(category: AdultCategory) {
        saved["adultCategory"] = category.name
        mutableState.update { it.copy(adultCategory = category) }
        search()
    }
    fun setAdultEnabled(enabled: Boolean) {
        mutableState.update { it.copy(adultEnabled = enabled) }
        persist { app.preferences.setAdultEnabled(enabled) }
        if (!enabled && state.value.section == ContentSection.ADULT) setSection(ContentSection.ANIME)
        else if (enabled && state.value.section == ContentSection.ADULT) search()
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
    fun setLanguage(language: LanguageFilter) {
        saved["language"] = language.name
        mutableState.update { it.copy(language = language) }
    }
    fun loadArtwork(torrent: Torrent, overrideQuery: String? = null) {
        val search = torrent.artworkSearch ?: return
        val key = search.key
        if (!state.value.showArtwork) return
        if (overrideQuery == null && (state.value.artworks.containsKey(key) || artworkJobs.containsKey(key))) return
        if (overrideQuery != null && overrideQuery.trim().length !in 2..160) {
            viewModelScope.launch { messages.send("Escribe entre 2 y 160 caracteres para buscar la portada.") }
            return
        }
        artworkJobs.remove(key)?.cancel()
        mutableState.update { it.copy(artworkLoading = it.artworkLoading + key) }
        val job = viewModelScope.launch {
            val artwork = try { app.artwork.find(search, overrideQuery) }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { null }
            if (artwork != null || overrideQuery == null) mutableState.update {
                it.copy(artworks = (it.artworks + (key to artwork)).toList().takeLast(256).toMap())
            }
            if (overrideQuery != null && artwork == null) messages.send("No se encontró una portada fiable. Prueba el título original o un nombre alternativo.")
        }
        artworkJobs[key] = job
        job.invokeOnCompletion {
            if (artworkJobs[key] === job) {
                artworkJobs.remove(key)
                mutableState.update { it.copy(artworkLoading = it.artworkLoading - key) }
            }
        }
    }
    fun loadDetails(torrent: Torrent, force: Boolean = false) {
        val key = torrent.key
        if (torrent.source == TorrentSource.SUKEBEI && !state.value.adultEnabled) return
        if (key in state.value.detailsLoading || (!force && key in state.value.details)) return
        mutableState.update { it.copy(detailsLoading = it.detailsLoading + key, detailsErrors = it.detailsErrors - key) }
        viewModelScope.launch {
            try {
                val detail = app.repository.details(torrent.id, torrent.source)
                mutableState.update { it.copy(details = it.details + (key to detail), detailsLoading = it.detailsLoading - key) }
                val snapshot = withDetails(this@NeoViewModel.torrent(key) ?: torrent)
                val selected = saved.get<String>("detail")?.let(FavoritesCodec::decode)?.firstOrNull()
                if (selected?.key == key) saved["detail"] = FavoritesCodec.encode(listOf(snapshot))
                persist { app.preferences.refreshFavorite(snapshot) }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                mutableState.update { it.copy(detailsLoading = it.detailsLoading - key,
                    detailsErrors = it.detailsErrors + (key to "No se pudo consultar al publicador ni actualizar las estadísticas.")) }
            }
        }
    }
    private fun cancelArtwork() {
        val jobs = artworkJobs.values.toList()
        artworkJobs.clear()
        jobs.forEach { it.cancel() }
        mutableState.update { it.copy(artworkLoading = emptySet()) }
    }
    fun search() {
        searchJob?.cancel()
        cancelArtwork()
        val request = state.value
        if (request.section == ContentSection.ADULT && !request.adultEnabled) {
            mutableState.update { it.copy(loading = false, error = null, torrents = emptyList(), searched = false) }
            return
        }
        searchJob = viewModelScope.launch {
            mutableState.update { it.copy(loading = true, error = null, torrents = emptyList(), searched = true) }
            val source = if (request.section == ContentSection.ADULT) TorrentSource.SUKEBEI else TorrentSource.NYAA
            try {
                val torrents = app.repository.search(request.query, request.category, request.filter, source, request.adultCategory)
                val category = if (source == TorrentSource.SUKEBEI) request.adultCategory.label else request.category.label
                mutableState.update { it.copy(loading = false, torrents = torrents,
                    resultsLabel = "${request.query.trim().ifEmpty { "Últimos torrents" }} · $category · ${request.filter.label}") }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                val message = if (e is IOException) e.message ?: "No se pudo conectar con ${source.label}."
                    else "No se pudo interpretar el RSS de ${source.label}."
                mutableState.update { it.copy(loading = false, error = message) }
            }
        }
    }
    private fun withDetails(torrent: Torrent): Torrent {
        val detail = state.value.details[torrent.key] ?: return torrent
        return torrent.copy(uploader = detail.uploader ?: torrent.uploader,
            seeders = detail.seeders ?: torrent.seeders, leechers = detail.leechers ?: torrent.leechers,
            downloads = detail.downloads ?: torrent.downloads)
    }
    fun toggleFavorite(torrent: Torrent) = persist { app.preferences.toggleFavorite(withDetails(torrent)) }
    fun setCompact(compact: Boolean) = persist { app.preferences.setCompact(compact) }
    fun setArtwork(enabled: Boolean) {
        if (!enabled) cancelArtwork()
        mutableState.update { it.copy(showArtwork = enabled,
            artworks = if (enabled) it.artworks.filterValues { value -> value != null } else it.artworks) }
        persist { app.preferences.setArtwork(enabled) }
    }
    private fun persist(action: suspend () -> Unit) {
        viewModelScope.launch {
            try { action() }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { messages.send("No se pudo guardar el cambio. Comprueba el almacenamiento disponible.") }
        }
    }
    fun selectTorrent(torrent: Torrent) { saved["detail"] = FavoritesCodec.encode(listOf(withDetails(torrent))) }
    fun torrent(key: String): Torrent? = state.value.let { state ->
        state.torrents.firstOrNull { it.key == key } ?: state.favorites.firstOrNull { it.key == key }
            ?: saved.get<String>("detail")?.let(FavoritesCodec::decode)?.firstOrNull { it.key == key }
    }

    companion object {
        private fun restoredState(saved: SavedStateHandle): NeoState {
            val section = ContentSection.entries.firstOrNull { it.name == saved.get<String>("section") } ?: ContentSection.ANIME
            val category = Category.entries.firstOrNull { it.name == saved.get<String>("category") && it in section.categories } ?: section.defaultCategory
            return NeoState(query = saved["query"] ?: "", section = section, category = category,
                adultCategory = AdultCategory.entries.firstOrNull { it.name == saved.get<String>("adultCategory") } ?: AdultCategory.ANIME,
                filter = FeedFilter.entries.firstOrNull { it.name == saved.get<String>("filter") } ?: FeedFilter.ALL,
                sort = SortOrder.entries.firstOrNull { it.name == saved.get<String>("sort") } ?: SortOrder.RECENT,
                language = LanguageFilter.entries.firstOrNull { it.name == saved.get<String>("language") } ?: LanguageFilter.ALL)
        }
    }
}
