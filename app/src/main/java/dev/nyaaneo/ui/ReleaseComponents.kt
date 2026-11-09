@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package dev.nyaaneo.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
internal fun AnimeCover(artwork: AnimeArtwork?, modifier: Modifier = Modifier) {
    Surface(modifier, shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.25f))) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Movie, if (artwork == null) "Carátula no disponible" else null,
                Modifier.size(28.dp), tint = NeonCyan.copy(alpha = 0.5f))
            if (artwork != null) {
                AsyncImage(model = artwork.imageUrl, contentDescription = "Carátula orientativa de ${artwork.title}, Kitsu",
                    modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Text("Kitsu · aprox.", Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.8f)).padding(horizontal = 4.dp, vertical = 2.dp), color = Color.White, fontSize = 9.sp,
                    style = MaterialTheme.typography.labelSmall)
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
