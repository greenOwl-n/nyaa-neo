package dev.nyaaneo.model

import java.text.Normalizer

data class ArtworkSearch(val kind: ArtworkKind, val queries: List<String>) {
    val key: String get() = "${kind.name}:${ReleaseInfo.normalize(queries.first())}"

    companion object {
        fun from(torrent: Torrent): ArtworkSearch? {
            // Adult titles never enter the general artwork catalogs or their caches.
            if (torrent.source != TorrentSource.NYAA) return null
            val kind = when {
                torrent.categoryId.startsWith("1_") && torrent.categoryId != "1_1" -> ArtworkKind.ANIME
                torrent.categoryId.startsWith("3_") -> ArtworkKind.MANGA
                torrent.categoryId.isBlank() && torrent.category.startsWith("Anime", true) && !torrent.category.contains("AMV", true) -> ArtworkKind.ANIME
                torrent.categoryId.isBlank() && torrent.category.startsWith("Literature", true) -> ArtworkKind.MANGA
                else -> return null
            }
            return create(kind, torrent.title)
        }

        fun create(kind: ArtworkKind, releaseTitle: String): ArtworkSearch? {
            val primary = clean(releaseTitle, kind)
            if (primary.isBlank()) return null
            val candidates = mutableListOf(primary)
            Regex("\\(([^)]+)\\)").findAll(releaseTitle).forEach { match ->
                val alternative = clean(match.groupValues[1], kind)
                if (alternative.length >= 4 && !TECHNICAL.matches(alternative)) candidates += alternative
            }
            val base = primary.replace(Regex("(?i)\\s+(?:s\\d{1,2}|season\\s*\\d+|\\d+(?:st|nd|rd|th)\\s+season|part\\s*\\d+)\\b.*$"), "").trim()
            if (base != primary && base.length >= 2) candidates += base
            val numericEpisode = primary.replace(Regex("\\s+\\d{1,2}(?:v\\d+)?$"), "").trim()
            if (numericEpisode != primary && numericEpisode.length >= 3) candidates += numericEpisode
            return ArtworkSearch(kind, candidates.filter { it.length in 2..160 }
                .distinctBy(ReleaseInfo::normalize).take(3)).takeIf { it.queries.isNotEmpty() }
        }

        fun clean(title: String, kind: ArtworkKind): String {
            var value = Normalizer.normalize(title, Normalizer.Form.NFKC)
                .replace(Regex("\\[[^]]*]"), " ")
                .replace(Regex("(?i)\\.(?:mkv|mp4|avi|torrent|cbz|cbr|pdf|epub|zip|rar|7z)$"), "")
                .replace('_', ' ').replace(Regex("(?<=[\\p{L}\\p{N}])\\.(?=[\\p{L}\\p{N}])"), " ")
                .replace(Regex("(?i)\\b(?:s\\d{1,2}[ .-]*e\\d{1,4}|ep(?:isode)?[ .#-]*\\d{1,4})\\b.*$"), "")
                .replace(Regex("\\s*[-–]\\s*\\d{1,4}(?:v\\d+)?(?:\\s|$|[.(–-]).*$"), "")
                .replace(Regex("(?i)\\b(?:\\d{3,4}[pi]|web[ .-]?dl|webrip|blu[ .-]?ray|bdrip|bdmv|hevc|avc|x26[45]|h[ .]?26[45]|aac|flac)\\b.*$"), "")
            if (kind == ArtworkKind.MANGA) value = value.replace(
                Regex("(?i)\\b(?:vol(?:ume)?s?|v|chap(?:ter)?s?|ch|c)[ .#-]*\\d+(?:[.\\d-]*).*?$"), "")
            value = value.replace(Regex("(?i)\\s*\\((?:digital|scan|complete|batch|multi[- ]?subs?|dual[- ]?audio|v\\d+)[^)]*\\)"), " ")
                .replace(Regex("(?i)\\s+(?:complete|batch|digital|multi[- ]?subs?|multi[- ]?audio|dual[- ]?audio)$"), "")
                .substringBefore(" (").replace(Regex("\\s+"), " ").trim(' ', '-', '–', '.')
            return value.takeIf { it.length in 2..160 && it.any(Char::isLetterOrDigit) && !TECHNICAL.matches(it) } ?: ""
        }

        private val TECHNICAL = Regex("(?i)(?:digital|scan|complete|batch|multi[- ]?subs?|dual[- ]?audio|manga|web|raw|english|spanish|japanese|\\d{3,4}p|\\d{4}|v\\d+)")
    }
}
