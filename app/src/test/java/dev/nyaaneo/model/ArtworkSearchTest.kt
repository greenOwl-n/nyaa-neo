package dev.nyaaneo.model

import dev.nyaaneo.data.ArtworkMatching
import org.junit.Assert.*
import org.junit.Test

class ArtworkSearchTest {
    @Test fun cleansSceneNamesAndEpisodeNumbers() {
        val search = requireNotNull(ArtworkSearch.create(ArtworkKind.ANIME, "[Group] Naruto.Shippuden.Ep.01.1080p.WEB-DL.mkv"))
        assertEquals("Naruto Shippuden", search.queries.first())
    }
    @Test fun preservesAlternativeOriginalTitle() {
        val search = requireNotNull(ArtworkSearch.create(ArtworkKind.ANIME,
            "[Group] JoJos Bizarre Adventure S06E08 [1080p] (Steel Ball Run JoJo no Kimyou na Bouken)"))
        assertEquals("JoJos Bizarre Adventure", search.queries.first())
        assertTrue(search.queries.contains("Steel Ball Run JoJo no Kimyou na Bouken"))
    }
    @Test fun removesMangaVolumesAndDigitalTags() {
        val torrent = Torrent("1", "[Digital] One Piece v001-v103 (Digital) [CBZ]", "", categoryId = "3_1")
        val search = requireNotNull(torrent.artworkSearch)
        assertEquals(ArtworkKind.MANGA, search.kind)
        assertEquals("One Piece", search.queries.first())
        assertNull(torrent.releaseInfo.group)
    }
    @Test fun numericAnimeNamesCanHaveCovers() {
        assertEquals("86", ArtworkSearch.create(ArtworkKind.ANIME, "[Group] 86 - 01 [1080p]")?.queries?.first())
    }
    @Test fun keepsSeasonFirstAndProvidesFranchiseFallback() {
        val search = requireNotNull(ArtworkSearch.create(ArtworkKind.ANIME, "[Group] Vinland Saga Season 2 - 01 [1080p]"))
        assertEquals("Vinland Saga Season 2", search.queries.first())
        assertTrue("Vinland Saga" in search.queries)
    }
    @Test fun cachesAnimeAndMangaSeparately() {
        val anime = ArtworkSearch.create(ArtworkKind.ANIME, "[Group] Naruto - 01")!!
        val manga = ArtworkSearch.create(ArtworkKind.MANGA, "[Group] Naruto v01")!!
        assertNotEquals(anime.key, manga.key)
    }
    @Test fun supportsLegacyFavoritesWithoutCategoryId() {
        assertEquals(ArtworkKind.MANGA, Torrent("1", "Naruto v01", "", category = "Literature - English-translated").artworkSearch?.kind)
    }
    @Test fun softwareAndAmvsDoNotTriggerCatalogRequests() {
        assertNull(Torrent("1", "Some Software", "", categoryId = "6_1").artworkSearch)
        assertNull(Torrent("1", "Music Video", "", categoryId = "1_1").artworkSearch)
    }
    @Test fun matchesCommonRomanizationAndApostropheVariants() {
        assertTrue(ArtworkMatching.score("Naruto Shippuden", "Naruto: Shippuuden") >= 0.9)
        assertEquals(1.0, ArtworkMatching.score("JoJos Bizarre Adventure", "JoJo's Bizarre Adventure"), 0.001)
    }
    @Test fun rejectsOtherSeriesAndDifferentNumericSeasons() {
        assertTrue(ArtworkMatching.score("One Piece", "One Punch Man") < 0.8)
        assertEquals(0.0, ArtworkMatching.score("Vinland Saga Season 2", "Vinland Saga Season 3"), 0.001)
        assertTrue(ArtworkMatching.score("Naruto", "Naruto Shippuden") < 0.8)
    }
}
