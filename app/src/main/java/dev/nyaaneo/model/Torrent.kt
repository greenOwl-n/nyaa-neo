package dev.nyaaneo.model

data class Torrent(
    val id: String,
    val title: String,
    val magnet: String,
    val size: String = "—",
    val category: String = "Sin categoría",
    val published: String = "",
    val seeders: Int = 0,
    val leechers: Int = 0,
    val downloads: Int = 0,
    val trusted: Boolean = false,
) {
    val pageUrl: String get() = "https://nyaa.si/view/$id"
}

enum class Category(val code: String, val label: String) {
    ALL("0_0", "Todas"), ANIME("1_0", "Anime"),
    ANIME_AMV("1_1", "Anime · AMV"), ANIME_EN("1_2", "Anime · Inglés"),
    ANIME_OTHER("1_3", "Anime · Otros idiomas"), ANIME_RAW("1_4", "Anime · Raw"),
    AUDIO("2_0", "Audio"), AUDIO_LOSSLESS("2_1", "Audio · Sin pérdida"),
    AUDIO_LOSSY("2_2", "Audio · Con pérdida"), LITERATURE("3_0", "Literatura"),
    LITERATURE_EN("3_1", "Literatura · Inglés"), LITERATURE_OTHER("3_2", "Literatura · Otros idiomas"),
    LITERATURE_RAW("3_3", "Literatura · Raw"), LIVE("4_0", "Live action"),
    LIVE_EN("4_1", "Live action · Inglés"), LIVE_IDOL("4_2", "Live action · Idol/PV"),
    LIVE_OTHER("4_3", "Live action · Otros idiomas"), LIVE_RAW("4_4", "Live action · Raw"),
    PICTURES("5_0", "Imágenes"), GRAPHICS("5_1", "Imágenes · Gráficos"),
    PHOTOS("5_2", "Imágenes · Fotos"), SOFTWARE("6_0", "Software"),
    APPS("6_1", "Software · Aplicaciones"), GAMES("6_2", "Software · Juegos")
}

enum class FeedFilter(val code: String, val label: String) {
    ALL("0", "Todos"), NO_REMAKES("1", "Sin remakes"), TRUSTED("2", "Solo confiables")
}

enum class SortOrder(val label: String) {
    RECENT("Más recientes"), SEEDERS("Más seeds"), DOWNLOADS("Más descargas"), TITLE("Título A–Z")
}

fun List<Torrent>.sortedByOrder(order: SortOrder): List<Torrent> = when (order) {
    SortOrder.RECENT -> sortedByDescending { it.id.toLongOrNull() ?: 0L }
    SortOrder.SEEDERS -> sortedWith(compareByDescending<Torrent> { it.seeders }.thenByDescending { it.id.toLongOrNull() })
    SortOrder.DOWNLOADS -> sortedByDescending { it.downloads }
    SortOrder.TITLE -> sortedBy { it.title.lowercase(java.util.Locale.ROOT) }
}
