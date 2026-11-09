package dev.nyaaneo.model

import java.text.Normalizer
import java.util.Locale

enum class ReleaseLanguage(val label: String, val tags: String) {
    SPANISH("Español", "spanish|espanol|esp|castellano|cast|latino|latam"),
    ENGLISH("Inglés", "english|eng"), FRENCH("Francés", "french|francais|fra|fre|vostfr"),
    PORTUGUESE("Portugués", "portuguese|portugues|por|pt[- ]?br"),
    GERMAN("Alemán", "german|deutsch|ger|deu"), ITALIAN("Italiano", "italian|italiano|ita"),
    JAPANESE("Japonés", "japanese|japones|jpn"), RUSSIAN("Ruso", "russian|rus"),
    ARABIC("Árabe", "arabic|ara"), CHINESE("Chino", "chinese|chs|cht"), KOREAN("Coreano", "korean|kor"),
}

enum class LanguageFilter(val label: String) {
    ALL("Todos"), SPANISH("Español"), ENGLISH("Inglés"), MULTI("Multi-sub / audio"), RAW("Raw"), OTHER("Otros / sin indicar");

    fun accepts(info: ReleaseInfo): Boolean = when (this) {
        ALL -> true
        SPANISH -> ReleaseLanguage.SPANISH in info.languages
        ENGLISH -> ReleaseLanguage.ENGLISH in info.languages || info.englishCategory
        MULTI -> info.multiSub || info.multiAudio
        RAW -> info.raw
        OTHER -> !SPANISH.accepts(info) && !ENGLISH.accepts(info) && !MULTI.accepts(info) && !info.raw
    }
}

data class ReleaseInfo(
    val group: String?,
    val languages: Set<ReleaseLanguage>,
    val multiSub: Boolean,
    val multiAudio: Boolean,
    val raw: Boolean,
    val englishCategory: Boolean,
    val resolution: String?,
    val source: String?,
    val animeQuery: String?,
) {
    val languageLabel: String get() = when {
        languages.isNotEmpty() -> languages.joinToString(" · ") { it.label }
        multiSub -> "Varios subtítulos"
        multiAudio -> "Varios audios"
        raw -> "Raw · sin traducir"
        englishCategory -> "Inglés · categoría"
        else -> "Idioma sin indicar"
    }
    val languageExplanation: String get() = when {
        languages.isNotEmpty() || multiSub || multiAudio -> "Indicios del nombre del archivo. No confirman las pistas de audio ni los subtítulos incluidos."
        raw -> "La categoría de Nyaa indica una publicación sin traducir. No confirma el idioma del audio."
        englishCategory -> "Nyaa lo clasifica como traducido al inglés; puede ser audio o subtítulos."
        else -> "El RSS y el nombre no indican un idioma concreto. Consulta la descripción en Nyaa."
    }

    companion object {
        fun from(title: String, categoryId: String, category: String, source: TorrentSource = TorrentSource.NYAA): ReleaseInfo {
            val leading = Regex("^\\s*\\[([^]\\r\\n]+)]").find(title)?.groupValues?.get(1)?.trim()
            val group = leading?.takeUnless { isTechnicalTag(it) }?.takeIf { it.length in 2..70 }
            // Do not derive languages from a group's name (e.g. a group called SpanishSub).
            val rest = if (group != null) title.substringAfter(']') else title
            val hints = normalize(rest)
            val languages = ReleaseLanguage.entries.filterTo(linkedSetOf()) { language ->
                Regex("(?<![a-z0-9])(?:${language.tags})(?![a-z0-9])").containsMatchIn(hints)
            }
            val shortTags = Regex("\\[([^]]+)]").findAll(rest).joinToString(" ") { it.groupValues[1] }
            val tokens = normalize(shortTags).split(Regex("[^a-z0-9]+")).toSet()
            if ("es" in tokens) languages += ReleaseLanguage.SPANISH
            if ("en" in tokens) languages += ReleaseLanguage.ENGLISH
            if ("fr" in tokens) languages += ReleaseLanguage.FRENCH
            if ("pt" in tokens) languages += ReleaseLanguage.PORTUGUESE
            if ("de" in tokens) languages += ReleaseLanguage.GERMAN
            if ("it" in tokens) languages += ReleaseLanguage.ITALIAN
            if ("jp" in tokens || "ja" in tokens) languages += ReleaseLanguage.JAPANESE
            val raw = (source == TorrentSource.NYAA && categoryId in setOf("1_4", "3_3", "4_4")) || category.contains("Raw", true)
            val english = if (source != TorrentSource.NYAA) false else if (categoryId.isNotBlank()) categoryId in setOf("1_2", "3_1", "4_1")
                else category.contains("English-translated", true) && !category.contains("Non-English", true)
            val anime = categoryId.startsWith("1_") || category.startsWith("Anime", true)
            return ReleaseInfo(group, languages,
                Regex("multi[ _-]?(?:sub(?:title)?s?)", RegexOption.IGNORE_CASE).containsMatchIn(rest),
                Regex("(?:multi|dual)[ _-]?(?:audio|dub)", RegexOption.IGNORE_CASE).containsMatchIn(rest),
                raw, english,
                Regex("(?i)\\b(?:2160|1080|720|576|480)p\\b").find(rest)?.value?.lowercase(Locale.ROOT),
                Regex("(?i)\\b(?:WEB[ .-]?DL|WEBRip|Blu[ .-]?Ray|BDRip|BDMV|HDTV|DVD(?:Rip)?)\\b").find(title)?.value?.uppercase(Locale.ROOT),
                if (source == TorrentSource.NYAA && anime && categoryId != "1_1") animeSearchTitle(title) else null,
            )
        }

        private fun isTechnicalTag(tag: String): Boolean {
            val text = normalize(tag)
            if (text in setOf("es", "en", "fr", "pt", "de", "it", "ja", "jp", "dual audio", "dual-audio",
                "manga", "digital", "scan", "cbz", "cbr", "pdf", "epub", "light novel", "ln") ||
                Regex("\\d+(?:-\\d+)?").matches(text)) return true
            if (Regex("\\b(?:\\d{3,4}p|hevc|av1|x26[45]|h[ .]?26[45]|s\\d{1,2})\\b").containsMatchIn(text)) return true
            return Regex("(?:[a-f0-9]{8}|\\d{3,4}p|bdmv|bdrip|bluray|web[- ]?dl|webrip|hevc|av1|x26[45]|h[ .]?26[45]|raw|multi[- ]?(?:sub|audio)s?|\\d{4})")
                .matches(text) || ReleaseLanguage.entries.any { Regex("(?:${it.tags})").matches(text) }
        }

        fun animeSearchTitle(title: String): String? {
            val clean = title.replace(Regex("\\[[^]]*]"), " ")
                .replace(Regex("(?i)\\.(?:mkv|mp4|avi|torrent)$"), "")
                .replace(Regex("(?i)\\bS\\d{1,2}E\\d{1,4}\\b.*$"), "")
                .replace(Regex("\\s+-\\s+\\d{1,4}(?:v\\d+)?(?:\\s|$|[.(]).*$"), "")
                .replace(Regex("(?i)\\b(?:\\d{3,4}p|WEB[ .-]?DL|WEBRip|BluRay|BDRip|HEVC|x26[45]|multi[ -]?(?:sub|audio)s?|batch|complete)\\b.*$"), "")
                .substringBefore(" (").replace(Regex("\\s+"), " ").trim(' ', '-', '.')
            return clean.takeIf { it.length in 3..150 && it.any(Char::isLetter) }
        }

        fun normalize(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT)
    }
}

data class TorrentPageDetails(
    val uploader: String?,
    val seeders: Int?,
    val leechers: Int?,
    val downloads: Int?,
)

data class AnimeArtwork(
    val title: String, val imageUrl: String, val pageUrl: String,
    val provider: String = "Kitsu", val bannerUrl: String? = null,
    val alternateImages: List<String> = emptyList(),
)
