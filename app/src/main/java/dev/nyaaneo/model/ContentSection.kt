package dev.nyaaneo.model

enum class TorrentSource(val host: String, val label: String) {
    NYAA("nyaa.si", "Nyaa"), SUKEBEI("sukebei.nyaa.si", "Sukebei · +18");
    fun key(id: String): String = "$name:$id"
}

enum class ContentSection(val label: String, val description: String) {
    ANIME("Anime", "Series, películas y animación"),
    MANGA("Manga", "Literatura de Nyaa: manga, cómics y novelas"),
    ADULT("+18", "Catálogo separado de Sukebei"),
    ALL("Todo", "Todas las categorías de Nyaa");

    val categories: List<Category> get() = when (this) {
        ANIME -> Category.entries.filter { it.code.startsWith("1_") }
        MANGA -> Category.entries.filter { it.code.startsWith("3_") }
        else -> Category.entries
    }
    val defaultCategory: Category get() = when (this) {
        ANIME -> Category.ANIME
        MANGA -> Category.LITERATURE
        else -> Category.ALL
    }

    fun accepts(torrent: Torrent): Boolean = when (this) {
        ADULT -> torrent.source == TorrentSource.SUKEBEI
        ALL -> torrent.source == TorrentSource.NYAA
        ANIME -> torrent.source == TorrentSource.NYAA &&
            (if (torrent.categoryId.isNotBlank()) torrent.categoryId.startsWith("1_") else torrent.category.startsWith("Anime", true))
        MANGA -> torrent.source == TorrentSource.NYAA &&
            (if (torrent.categoryId.isNotBlank()) torrent.categoryId.startsWith("3_") else torrent.category.startsWith("Literature", true))
    }
}

enum class AdultCategory(val code: String, val label: String) {
    ANIME("1_1", "Anime +18"), MANGA("1_4", "Manga +18"), DOUJINSHI("1_2", "Doujinshi +18")
}

enum class ArtworkKind { ANIME, MANGA }
