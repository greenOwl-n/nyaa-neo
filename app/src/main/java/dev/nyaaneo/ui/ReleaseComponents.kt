@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package dev.nyaaneo.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import dev.nyaaneo.model.AnimeArtwork
import dev.nyaaneo.model.ReleaseInfo
import java.text.NumberFormat
import java.util.Locale

private val SeedGreen = Color(0xFF65E5A5)
private val LeechAmber = Color(0xFFFFC078)

@Composable
internal fun AnimeCover(artwork: AnimeArtwork?, modifier: Modifier = Modifier, title: String = "", adult: Boolean = false, hero: Boolean = false) {
    val safeArtwork = artwork?.takeUnless { adult }
    val urls = remember(safeArtwork, hero) {
        if (safeArtwork == null) emptyList() else buildList {
            if (hero) safeArtwork.bannerUrl?.let(::add)
            add(safeArtwork.imageUrl)
            addAll(safeArtwork.alternateImages)
        }.distinct()
    }
    var imageIndex by remember(urls) { mutableIntStateOf(0) }
    var loaded by remember(urls) { mutableStateOf(false) }
    Surface(modifier, shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.25f))) {
        Box(Modifier.background(Brush.linearGradient(listOf(Color(0xFF12323D), Color(0xFF070C13), Color(0xFF103743)))), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(if (adult) Icons.Default.Lock else Icons.Default.Movie,
                    if (adult) "Miniatura neutra de la sección +18" else if (!loaded) "Portada pendiente o no disponible de $title" else null,
                    Modifier.size(if (adult) 36.dp else 28.dp), tint = NeonCyan.copy(alpha = 0.55f))
                if (adult) Text("+18", style = MaterialTheme.typography.headlineMedium, color = NeonCyan)
            }
            if (safeArtwork != null && urls.isNotEmpty()) {
                AsyncImage(model = urls[imageIndex], contentDescription = "Portada orientativa de ${safeArtwork.title}, ${safeArtwork.provider}",
                    modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop,
                    onSuccess = { loaded = true }, onError = {
                        loaded = false
                        if (imageIndex < urls.lastIndex) imageIndex++
                    })
                if (loaded) Text(safeArtwork.provider, Modifier.align(Alignment.TopStart)
                    .background(Color.Black.copy(alpha = 0.70f)).padding(horizontal = 6.dp, vertical = 3.dp), color = Color.White,
                    fontSize = 9.sp, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
internal fun ReleaseBadges(info: ReleaseInfo, compact: Boolean = false) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        MetaBadge(info.languageLabel, NeonCyan)
        if (info.multiSub && info.languages.isNotEmpty()) MetaBadge("Multi-sub", NeonCyan)
        if (info.multiAudio && (info.languages.isNotEmpty() || info.multiSub)) MetaBadge("Multi-audio", LeechAmber)
        if (!compact) {
            info.resolution?.let { MetaBadge(it, MaterialTheme.colorScheme.onSurfaceVariant) }
            info.source?.let { MetaBadge(it, MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun MetaBadge(text: String, color: Color) {
    Surface(shape = MaterialTheme.shapes.small, color = color.copy(alpha = 0.10f)) {
        Text(text, Modifier.padding(horizontal = 8.dp, vertical = 4.dp).widthIn(max = 280.dp),
            color = color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold,
            maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun SwarmStats(seeders: Int, leechers: Int, downloads: Int, compact: Boolean = false) {
    if (compact) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("↑ ${formatCount(seeders)} seeds", color = SeedGreen, style = MaterialTheme.typography.labelMedium)
            Text("↓ ${formatCount(leechers)} leechers", color = LeechAmber, style = MaterialTheme.typography.labelMedium)
            Text("${formatCount(downloads)} descargas", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        }
    } else {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SwarmStat("Seeds", seeders, SeedGreen, Modifier.weight(1f))
            SwarmStat("Leechers", leechers, LeechAmber, Modifier.weight(1f))
            SwarmStat("Descargas", downloads, NeonCyan, Modifier.weight(1f))
        }
    }
}

@Composable
private fun SwarmStat(label: String, count: Int, color: Color, modifier: Modifier) {
    Surface(modifier, shape = MaterialTheme.shapes.medium, color = color.copy(alpha = 0.07f)) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(formatCount(count), color = color, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatCount(count: Int): String = NumberFormat.getIntegerInstance(Locale.forLanguageTag("es")).format(count)
