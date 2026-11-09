package dev.nyaaneo.data

import dev.nyaaneo.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ArtworkCatalogTest {
    @get:Rule val folder = TemporaryFolder()
    private fun fixture(name: String) = requireNotNull(javaClass.getResourceAsStream("/$name")).bufferedReader().use { it.readText() }

    @Test fun tvMazeProvidesRealAnimePosterAsFallback() {
        val cover = requireNotNull(AdditionalArtworkParser.tvMaze(fixture("tvmaze-anime.json"), "Naruto"))
        assertEquals("Naruto", cover.title)
        assertEquals("TVMaze", cover.provider)
        assertTrue(cover.imageUrl.startsWith("https://static.tvmaze.com/"))
    }
    @Test fun tvMazeDoesNotReturnLiveActionSeries() {
        assertNull(AdditionalArtworkParser.tvMaze(fixture("tvmaze-anime.json"), "Maruko"))
    }
    @Test fun mangaDexMatchesOriginalRatherThanSpinOff() {
        val cover = requireNotNull(AdditionalArtworkParser.mangaDex(fixture("mangadex-manga.json"), "One Piece"))
        assertEquals("One Piece", cover.title)
        assertEquals("MangaDex", cover.provider)
        assertTrue(cover.imageUrl.endsWith(".512.jpg"))
        assertTrue(cover.pageUrl.endsWith("a1c7c817-4e59-43b7-9365-09675a149a6f"))
    }
    @Test fun mangaDexRejectsExplicitResultsInGeneralCatalog() {
        assertNull(AdditionalArtworkParser.mangaDex(fixture("mangadex-manga.json").replace("\"safe\"", "\"pornographic\"").replace("\"suggestive\"", "\"pornographic\""), "One Piece"))
    }
    @Test fun kitsuSelectsHigherResolutionAndKeepsAlternateSizes() {
        val cover = requireNotNull(AnimeArtworkParser.findBest(fixture("kitsu-anime.json"), "Naruto"))
        assertEquals("https://media.kitsu.app/anime/11/poster_image/4512d403727b2a19a6eb7e7a959be0c3.jpg", cover.imageUrl)
        assertTrue(cover.alternateImages.isNotEmpty())
        assertNotEquals(cover.imageUrl, cover.alternateImages.first())
    }
    @Test fun kitsuMangaUsesMangaCatalogAndCorrectEntry() {
        val cover = requireNotNull(AnimeArtworkParser.findBest(fixture("kitsu-manga.json"), "Naruto", ArtworkKind.MANGA))
        assertEquals("NARUTO", cover.title)
        assertEquals("https://kitsu.app/manga/35", cover.pageUrl)
    }
    @Test fun animeCatalogResponseCannotBeUsedAsManga() {
        assertNull(AnimeArtworkParser.findBest(fixture("kitsu-anime.json"), "Naruto", ArtworkKind.MANGA))
    }
    @Test fun kitsuExcludesAdultCatalogResults() {
        val json = """{"data":[{"id":"1","attributes":{"canonicalTitle":"Test title","ageRating":"R18","posterImage":{"original":"https://media.kitsu.app/test.jpg"}}}]}"""
        assertNull(AnimeArtworkParser.findBest(json, "Test title"))
    }
    @Test fun chosenCoverMetadataSurvivesRepositoryRestartWithoutNetwork() = runBlocking {
        val search = requireNotNull(ArtworkSearch.create(ArtworkKind.ANIME, "[Group] Naruto - 01 [1080p]"))
        val cover = requireNotNull(AnimeArtworkParser.findBest(fixture("kitsu-anime.json"), "Naruto"))
        val file = folder.newFile("covers.json")
        file.writeText(ArtworkCacheCodec.encode(mapOf(search.key to CachedArtwork(System.currentTimeMillis(), cover))))
        assertEquals(cover, AnimeArtworkRepository(file).find(search))
        assertEquals(cover, AnimeArtworkRepository(file).find(search))
    }
    @Test fun cacheKeepsProvidersAndBannersAndSkipsMissingResults() {
        val cover = AnimeArtwork("Naruto", "https://media.kitsu.app/poster.jpg", "https://kitsu.app/anime/11",
            "Kitsu", "https://media.kitsu.app/banner.jpg", listOf("https://media.kitsu.app/small.jpg"))
        val values = mapOf("ANIME:naruto" to CachedArtwork(123L, cover), "ANIME:missing" to CachedArtwork(123L, null))
        val decoded = ArtworkCacheCodec.decode(ArtworkCacheCodec.encode(values))
        assertEquals(cover, decoded.getValue("ANIME:naruto").artwork)
        assertFalse(decoded.containsKey("ANIME:missing"))
    }
    @Test fun cacheRejectsUntrustedImageHosts() {
        val cover = AnimeArtwork("Test", "https://example.com/image.jpg", "https://kitsu.app/anime/1")
        assertTrue(ArtworkCacheCodec.decode(ArtworkCacheCodec.encode(mapOf("ANIME:test" to CachedArtwork(1L, cover)))).isEmpty())
    }
}
