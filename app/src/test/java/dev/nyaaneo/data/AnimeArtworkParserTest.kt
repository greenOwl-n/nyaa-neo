package dev.nyaaneo.data

import org.junit.Assert.*
import org.junit.Test

class AnimeArtworkParserTest {
    private fun sample() = requireNotNull(javaClass.getResourceAsStream("/kitsu-anime.json")).bufferedReader().use { it.readText() }

    @Test fun findsExactSeriesInsteadOfFirstSearchResult() {
        val artwork = requireNotNull(AnimeArtworkParser.findBest(sample(), "Naruto"))
        assertEquals("Naruto", artwork.title)
        assertEquals("https://kitsu.app/anime/11", artwork.pageUrl)
        assertTrue(artwork.imageUrl.startsWith("https://media.kitsu.app/"))
    }
    @Test fun distinguishesSequelFromOriginal() {
        val artwork = requireNotNull(AnimeArtworkParser.findBest(sample(), "Naruto Shippuuden"))
        assertEquals("Naruto: Shippuuden", artwork.title)
    }
    @Test fun doesNotUseUnrelatedArtworkWhenNoMatchExists() {
        assertNull(AnimeArtworkParser.findBest(sample(), "One Punch Man"))
    }
    @Test fun noImageIsAnAcceptableResult() {
        assertNull(AnimeArtworkParser.findBest("""{"data":[{"id":"1","attributes":{"canonicalTitle":"Naruto","posterImage":null}}]}""", "Naruto"))
    }
    @Test fun rejectsInsecureOrUntrustedImageUrls() {
        assertNull(AnimeArtworkParser.findBest(sample().replace("https://media.kitsu.app/", "http://example.com/"), "Naruto"))
    }
}
