@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.nyaaneo.ui

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.net.toUri
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import dev.nyaaneo.model.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class Destination(val route: String, val label: String, val icon: ImageVector)
private val destinations = listOf(
    Destination("explore", "Explorar", Icons.Default.Explore),
    Destination("favorites", "Favoritos", Icons.Default.Star),
    Destination("settings", "Ajustes", Icons.Default.Settings),
)

@Composable
fun NeoApp(model: NeoViewModel) {
    val state by model.state.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val showMessage: (String) -> Unit = { scope.launch { snackbar.showSnackbar(it) } }
    LaunchedEffect(model) { model.events.collect { snackbar.showSnackbar(it) } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (route in destinations.map { it.route }) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    destinations.forEach { destination ->
                        NavigationBarItem(
                            selected = route == destination.route,
                            onClick = {
                                nav.navigate(destination.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(destination.icon, null) },
                            label = { Text(destination.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = "explore", modifier = Modifier.padding(padding).consumeWindowInsets(padding)) {
            composable("explore") {
                ExploreScreen(state, model) { torrent ->
                    model.selectTorrent(torrent)
                    nav.navigate("detail/${torrent.id}")
                }
            }
            composable("favorites") {
                FavoritesScreen(state, model) { torrent ->
                    model.selectTorrent(torrent)
                    nav.navigate("detail/${torrent.id}")
                }
            }
            composable("settings") { SettingsScreen(state, model) }
            composable("detail/{id}") { backStack ->
                val id = backStack.arguments?.getString("id").orEmpty()
                val torrent = model.torrent(id)
                LaunchedEffect(id) { if (id.matches(Regex("\\d+"))) model.loadDetails(id) }
                LaunchedEffect(torrent?.id, state.showArtwork) { if (state.showArtwork) torrent?.let(model::loadArtwork) }
                DetailScreen(torrent, state.favorites.any { it.id == torrent?.id },
                    state = state,
                    onRefresh = { model.loadDetails(id, force = true) },
                    onBack = { nav.popBackStack() },
                    onFavorite = { torrent?.let(model::toggleFavorite) },
                    onMagnet = { torrent?.let { openExternal(context, it.magnet, showMessage) } },
                    onWebsite = { torrent?.let { openExternal(context, it.pageUrl, showMessage) } },
                    onMessage = showMessage,
                )
            }
        }
    }
}

@Composable
private fun PageTitle(title: String, subtitle: String, action: @Composable (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = NeonCyan)
            Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        action?.invoke()
    }
}

@Composable
private fun ViewToggle(compact: Boolean, onToggle: () -> Unit) {
    IconButton(onClick = onToggle) {
        Icon(if (compact) Icons.Default.GridView else Icons.AutoMirrored.Filled.ViewList,
            if (compact) "Mostrar tarjetas" else "Mostrar lista compacta", tint = NeonCyan)
    }
}

@Composable
private fun ExploreScreen(state: NeoState, model: NeoViewModel, onDetail: (Torrent) -> Unit) {
    val focus = LocalFocusManager.current
    var filtersOpen by rememberSaveable { mutableStateOf(false) }
    val visible = remember(state.torrents, state.language, state.sort) {
        state.torrents.filter { state.language.accepts(it.releaseInfo) }.sortedByOrder(state.sort)
    }
    Column(Modifier.fillMaxSize().widthIn(max = 960.dp)) {
        PageTitle("NYAA // NEO", "EXPLORA · ENCUENTRA · GUARDA") {
            ViewToggle(state.compact) { model.setCompact(!state.compact) }
        }
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = state.query, onValueChange = model::setQuery,
                modifier = Modifier.weight(1f), singleLine = true,
                label = { Text("Buscar en Nyaa.si") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (state.query.isNotEmpty()) IconButton(onClick = { model.setQuery("") }) { Icon(Icons.Default.Close, "Borrar búsqueda") }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focus.clearFocus(); model.search() }),
            )
            IconButton(onClick = { focus.clearFocus(); model.search() }) { Icon(Icons.AutoMirrored.Filled.ArrowForward, "Buscar", tint = NeonCyan) }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { filtersOpen = !filtersOpen }) {
                Icon(Icons.Default.Tune, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (filtersOpen) "Ocultar filtros" else "Filtros")
            }
            Text(state.category.label, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelMedium)
            IconButton(onClick = model::search, enabled = !state.loading) { Icon(Icons.Default.Refresh, "Actualizar RSS") }
        }
        if (filtersOpen) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoiceMenu("Categoría", state.category, Category.entries, { it.label }, model::setCategory, Modifier.weight(1f))
                    ChoiceMenu("Filtro", state.filter, FeedFilter.entries, { it.label }, model::setFilter, Modifier.weight(1f))
                }
                ChoiceMenu("Orden", state.sort, SortOrder.entries, { it.label }, model::setSort)
            }
        }
        Text("Orden local del RSS: ${state.sort.label}", Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ChoiceMenu("Idioma en este RSS", state.language, LanguageFilter.entries, { it.label }, model::setLanguage,
            Modifier.padding(horizontal = 16.dp))
        when {
            state.loading -> StatusPanel("Consultando Nyaa.si…", loading = true)
            state.error != null -> StatusPanel("No se pudo cargar el RSS", state.error, Icons.Default.CloudOff, "Reintentar", model::search)
            state.searched && state.torrents.isEmpty() -> StatusPanel("Sin resultados", "Prueba otra búsqueda o cambia los filtros.", Icons.Default.SearchOff)
            visible.isEmpty() && state.torrents.isNotEmpty() -> StatusPanel("Sin coincidencias de idioma en este RSS",
                "El filtro usa indicios del título y la categoría. Prueba añadir Spanish o ESP a tu búsqueda, o selecciona Todos.",
                Icons.Default.Translate, "Mostrar todos", { model.setLanguage(LanguageFilter.ALL) })
            else -> {
                Text("${visible.size} de ${state.torrents.size} resultados · ${state.resultsLabel}", Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    maxLines = 2, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TorrentList(visible, state, model::toggleFavorite, onDetail, model::loadArtwork)
            }
        }
    }
}

@Composable
private fun <T> ChoiceMenu(label: String, selected: T, options: List<T>, text: (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
            Text("$label: ${text(selected)}", modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Icon(Icons.Default.ExpandMore, null)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.heightIn(max = 360.dp)) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(text(option)) }, onClick = { open = false; onSelect(option) },
                    trailingIcon = { if (selected == option) Icon(Icons.Default.Check, null, tint = NeonCyan) })
            }
        }
    }
}

@Composable
private fun FavoritesScreen(state: NeoState, model: NeoViewModel, onDetail: (Torrent) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val torrents = state.favorites.filter { it.title.contains(query, ignoreCase = true) }.sortedByOrder(state.sort)
    Column(Modifier.fillMaxSize().widthIn(max = 960.dp)) {
        PageTitle("Favoritos", "${state.favorites.size} GUARDADOS · DISPONIBLES SIN CONEXIÓN") {
            ViewToggle(state.compact) { model.setCompact(!state.compact) }
        }
        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            label = { Text("Buscar en favoritos") }, singleLine = true, leadingIcon = { Icon(Icons.Default.Search, null) })
        Spacer(Modifier.height(12.dp))
        when {
            state.favorites.isEmpty() -> StatusPanel("Tu colección empieza aquí", "Toca la estrella de un torrent para guardarlo.", Icons.Default.StarOutline)
            torrents.isEmpty() -> StatusPanel("Sin coincidencias", "Prueba otro título.", Icons.Default.SearchOff)
            else -> TorrentList(torrents, state, model::toggleFavorite, onDetail, model::loadArtwork)
        }
    }
}

@Composable
private fun TorrentList(torrents: List<Torrent>, state: NeoState, onFavorite: (Torrent) -> Unit, onDetail: (Torrent) -> Unit, onArtwork: (Torrent) -> Unit) {
    val favoriteIds = remember(state.favorites) { state.favorites.map { it.id }.toSet() }
    LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(if (state.compact) 2.dp else 12.dp)) {
        items(torrents, key = { it.id }) { torrent ->
            LaunchedEffect(torrent.id, state.showArtwork) { if (state.showArtwork) onArtwork(torrent) }
            TorrentItem(torrent, state.compact, torrent.id in favoriteIds, state.showArtwork,
                state.artworks[torrent.releaseInfo.animeQuery], { onFavorite(torrent) }, { onDetail(torrent) })
        }
        item { Text("Datos del RSS de Nyaa.si. Idiomas y grupo según título/categoría. Carátulas orientativas de Kitsu. Las estadísticas pueden cambiar.", Modifier.padding(vertical = 16.dp),
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun TorrentItem(t: Torrent, compact: Boolean, favorite: Boolean, showArtwork: Boolean, artwork: AnimeArtwork?, onFavorite: () -> Unit, onDetail: () -> Unit) {
    val info = t.releaseInfo
    OutlinedCard(onClick = onDetail, modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, if (compact) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))) {
        Column(Modifier.padding(if (compact) 10.dp else 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            if (showArtwork && info.animeQuery != null) AnimeCover(artwork, Modifier.width(if (compact) 48.dp else 88.dp).height(if (compact) 72.dp else 132.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Grupo: ${info.group ?: "Sin indicar"}", style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold, color = NeonCyan, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(t.title, maxLines = if (compact) 2 else 3, overflow = TextOverflow.Ellipsis,
                    style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleSmall)
                ReleaseBadges(info, compact)
                Text("${t.size}" + if (t.trusted) "  ·  ✓ Confiable" else "",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onFavorite) {
                Icon(if (favorite) Icons.Default.Star else Icons.Default.StarOutline,
                    if (favorite) "Quitar de favoritos" else "Guardar favorito", tint = if (favorite) NeonCyan else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        SwarmStats(t.seeders, t.leechers, t.downloads, compact)
        if (!compact) Text((if (t.seeders == 0) "Sin seeds reportados · " else "") + formatDate(t.published),
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StatusPanel(title: String, description: String = "", icon: ImageVector = Icons.Default.CloudDownload,
                        action: String? = null, onAction: () -> Unit = {}, loading: Boolean = false) {
    Column(Modifier.fillMaxWidth().padding(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (loading) CircularProgressIndicator() else Icon(icon, null, Modifier.size(40.dp), tint = NeonCyan)
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (description.isNotEmpty()) Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (action != null) OutlinedButton(onClick = onAction) { Text(action) }
    }
}

@Composable
private fun DetailScreen(torrent: Torrent?, favorite: Boolean, state: NeoState, onRefresh: () -> Unit, onBack: () -> Unit, onFavorite: () -> Unit,
                         onMagnet: () -> Unit, onWebsite: () -> Unit, onMessage: (String) -> Unit) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Detalle // NEO") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            actions = {
                if (torrent != null) IconButton(onClick = onFavorite) {
                    Icon(if (favorite) Icons.Default.Star else Icons.Default.StarOutline,
                        if (favorite) "Quitar de favoritos" else "Guardar favorito", tint = NeonCyan)
                }
            })
        if (torrent == null) {
            StatusPanel("Detalle no disponible", "Vuelve a buscar este torrent o abre uno de tus favoritos.", action = "Volver", onAction = onBack)
        } else {
            val info = torrent.releaseInfo
            val details = state.details[torrent.id]
            val artwork = state.artworks[info.animeQuery]
            val context = LocalContext.current
            Column(Modifier.widthIn(max = 960.dp).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(torrent.category.uppercase(Locale.ROOT), style = MaterialTheme.typography.labelMedium, color = NeonCyan)
                if (state.showArtwork && info.animeQuery != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        AnimeCover(artwork, Modifier.size(width = 104.dp, height = 156.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Fansub / grupo", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(info.group ?: "No indicado en el título", style = MaterialTheme.typography.titleLarge, color = NeonCyan)
                            artwork?.let {
                                Text("Carátula orientativa: ${it.title}", style = MaterialTheme.typography.bodySmall)
                                TextButton(onClick = { openExternal(context, it.pageUrl, onMessage) }) { Text("Ver ficha en Kitsu") }
                            }
                        }
                    }
                } else Text("Fansub / grupo: ${info.group ?: "No indicado en el título"}", color = NeonCyan, style = MaterialTheme.typography.titleMedium)
                Text(torrent.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                ReleaseBadges(info)
                Text(info.languageExplanation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Publicado en Nyaa por", style = MaterialTheme.typography.labelMedium)
                        Text(details?.uploader ?: torrent.uploader ?: if (torrent.id in state.detailsLoading) "Consultando…" else "No disponible",
                            style = MaterialTheme.typography.titleMedium, color = NeonCyan)
                        Text("El usuario que publica puede ser distinto del grupo indicado en el título.", style = MaterialTheme.typography.bodySmall)
                    }
                }
                state.detailsErrors[torrent.id]?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                if (torrent.trusted) AssistChip(onClick = {}, label = { Text("Publicador confiable en Nyaa.si") }, leadingIcon = { Icon(Icons.Default.Verified, null) })
                Text("#${torrent.id} · ${formatDate(torrent.published)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                SwarmStats(details?.seeders ?: torrent.seeders, details?.leechers ?: torrent.leechers, details?.downloads ?: torrent.downloads)
                Text(if (details != null) "Estadísticas consultadas en Nyaa al abrir o actualizar este detalle."
                    else "Estadísticas del RSS o de tu favorito guardado.", style = MaterialTheme.typography.labelSmall)
                Text("Seeds: comparten el archivo completo. Leechers: están descargando. Un número de seeds mayor que cero no garantiza disponibilidad continua.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = onRefresh, enabled = torrent.id !in state.detailsLoading) {
                    Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(8.dp))
                    Text(if (torrent.id in state.detailsLoading) "Actualizando…" else "Actualizar publicador y estadísticas")
                }
                Text("Tamaño: ${torrent.size}", style = MaterialTheme.typography.titleMedium)
                Button(onClick = onMagnet, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Download, null); Spacer(Modifier.width(8.dp)); Text("Abrir magnet")
                }
                OutlinedButton(onClick = {
                    scope.launch {
                        clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("Magnet", torrent.magnet)))
                        onMessage("Magnet copiado")
                    }
                }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.ContentCopy, null); Spacer(Modifier.width(8.dp)); Text("Copiar magnet")
                }
                OutlinedButton(onClick = onWebsite, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, null); Spacer(Modifier.width(8.dp)); Text("Ver en Nyaa.si")
                }
                Text("Necesitas una aplicación compatible con BitTorrent para abrir el magnet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                HorizontalDivider()
                Text("Datos del RSS", style = MaterialTheme.typography.titleSmall)
                Text("Nyaa Neo muestra los metadatos disponibles en el RSS. Consulta Nyaa.si para ver la descripción completa, los archivos y los comentarios. Las estadísticas guardadas en favoritos son una copia del momento en que los guardaste.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SettingsScreen(state: NeoState, model: NeoViewModel) {
    Column(Modifier.fillMaxSize().widthIn(max = 960.dp).verticalScroll(rememberScrollState())) {
        PageTitle("Ajustes", "TU ESPACIO · TU ESTILO")
        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Lista compacta", style = MaterialTheme.typography.titleMedium)
                Text("Se guarda entre sesiones", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = state.compact, onCheckedChange = model::setCompact)
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Carátulas de anime", style = MaterialTheme.typography.titleMedium)
                Text("Búsqueda orientativa en Kitsu. Envía el nombre del anime al catálogo y descarga su imagen.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = state.showArtwork, onCheckedChange = model::setArtwork)
        }
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ChoiceMenu("Orden de resultados", state.sort, SortOrder.entries, { it.label }, model::setSort)
            HorizontalDivider()
            Text("Nyaa Neo", style = MaterialTheme.typography.titleLarge, color = NeonCyan)
            Text("Versión 0.2.0 · Android nativo", style = MaterialTheme.typography.bodyMedium)
            Text("Tema AMOLED negro con acentos cian. Los favoritos se guardan en este dispositivo y se eliminan si borras los datos o desinstalas la aplicación.", style = MaterialTheme.typography.bodyMedium)
            Text("La búsqueda consulta el RSS de Nyaa.si. Cada respuesta contiene una cantidad limitada de torrents. El ordenamiento se aplica únicamente a los resultados recibidos; no hay paginación global del catálogo.", style = MaterialTheme.typography.bodyMedium)
            Text("Cliente no oficial, sin afiliación con Nyaa.si. Nyaa Neo abre los magnet en aplicaciones externas.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(20.dp))
        }
    }
}

private fun openExternal(context: Context, url: String, onMessage: (String) -> Unit) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()).addCategory(Intent.CATEGORY_BROWSABLE))
    } catch (_: ActivityNotFoundException) {
        onMessage(if (url.startsWith("magnet:")) "No hay una aplicación instalada para abrir magnets. Puedes copiar el enlace." else "No hay un navegador disponible.")
    } catch (_: SecurityException) { onMessage("Android no permitió abrir el enlace.") }
}

private fun formatDate(raw: String): String = runCatching {
    ZonedDateTime.parse(raw, DateTimeFormatter.RFC_1123_DATE_TIME)
        .format(DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm z", Locale.forLanguageTag("es")))
}.getOrDefault(raw.ifBlank { "Fecha no disponible" })
