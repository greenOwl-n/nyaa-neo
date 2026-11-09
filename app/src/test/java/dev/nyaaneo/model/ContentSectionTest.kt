package dev.nyaaneo.model

import dev.nyaaneo.data.FavoritesCodec
import dev.nyaaneo.data.NyaaRepository
import dev.nyaaneo.data.RssParser
import org.junit.Assert.*
import org.junit.Test

class ContentSectionTest {
    private val magnet = "magnet:?xt=urn:btih:0123456789abcdef0123456789abcdef01234567"
    private val anime = Torrent("123", "Test release", magnet, categoryId = "1_2")
    private val manga = Torrent("456", "Test book", magnet, categoryId = "3_1")
    private val adult = anime.copy(categoryId = "1_4", category = "Art - Manga", source = TorrentSource.SUKEBEI)

    @Test fun separatesNormalAnimeMangaAndAdultSources() {
        assertTrue(ContentSection.ANIME.accepts(anime))
        assertFalse(ContentSection.ANIME.accepts(manga))
        assertTrue(ContentSection.MANGA.accepts(manga))
        assertFalse(ContentSection.MANGA.accepts(anime))
        assertTrue(ContentSection.ADULT.accepts(adult))
        for (section in listOf(ContentSection.ANIME, ContentSection.MANGA, ContentSection.ALL)) assertFalse(section.accepts(adult))
        assertFalse(ContentSection.ADULT.accepts(anime))
    }
    @Test fun sameNumericIdDoesNotMergeFavoritesAcrossSources() {
        assertNotEquals(anime.key, adult.key)
        val decoded = FavoritesCodec.decode(FavoritesCodec.encode(listOf(anime, adult)))
        assertEquals(2, decoded.size)
        assertEquals(setOf(TorrentSource.NYAA, TorrentSource.SUKEBEI), decoded.map { it.source }.toSet())
    }
    @Test fun linksAlwaysUseTheirOwnSource() {
        assertEquals("https://nyaa.si/view/123", anime.pageUrl)
        assertEquals("https://sukebei.nyaa.si/view/123", adult.pageUrl)
    }
    @Test fun adultCategoryCodesDoNotMeanRawOrEnglishNyaaCategories() {
        assertFalse(adult.releaseInfo.raw)
        assertFalse(adult.releaseInfo.englishCategory)
        assertFalse(adult.copy(categoryId = "1_2").releaseInfo.englishCategory)
        assertNull(adult.artworkSearch)
    }
    @Test fun buildsSeparateFeedsAndAdultCategoryCodes() {
        for (category in AdultCategory.entries) {
            val url = NyaaRepository.buildFeedUrl("test & query", Category.ALL, FeedFilter.ALL, TorrentSource.SUKEBEI, category)
            assertEquals("sukebei.nyaa.si", url.host)
            assertEquals(category.code, url.queryParameter("c"))
            assertEquals("test & query", url.queryParameter("q"))
        }
        val regular = NyaaRepository.buildFeedUrl("", Category.ANIME, FeedFilter.ALL)
        assertEquals("nyaa.si", regular.host)
        assertEquals("1_0", regular.queryParameter("c"))
    }
    @Test fun parserAcceptsAdultNamespaceButRejectsWrongDomain() {
        val xml = """<rss xmlns:nyaa="https://sukebei.nyaa.si/xmlns/nyaa"><channel><item>
            <title>Test release</title><guid>https://sukebei.nyaa.si/view/123</guid>
            <nyaa:infoHash>0123456789abcdef0123456789abcdef01234567</nyaa:infoHash>
            <nyaa:categoryId>1_4</nyaa:categoryId><nyaa:category>Art - Manga</nyaa:category>
            </item></channel></rss>"""
        val torrent = RssParser().parse(xml.reader(), TorrentSource.SUKEBEI).single()
        assertEquals(TorrentSource.SUKEBEI, torrent.source)
        assertEquals("1_4", torrent.categoryId)
        assertFalse(torrent.releaseInfo.raw)
        assertTrue(RssParser().parse(xml.reader()).isEmpty())
        assertTrue(RssParser().parse(xml.replace("https://sukebei.nyaa.si/view/", "https://nyaa.si/view/").reader(), TorrentSource.SUKEBEI).isEmpty())
    }
    @Test fun unknownSourcesAreNotSilentlyOpenedAsNyaa() {
        val encoded = FavoritesCodec.encode(listOf(anime)).replace("\"NYAA\"", "\"UNKNOWN\"")
        assertTrue(FavoritesCodec.decode(encoded).isEmpty())
    }
    @Test fun sectionCategoriesStayScoped() {
        assertTrue(ContentSection.ANIME.categories.all { it.code.startsWith("1_") })
        assertTrue(ContentSection.MANGA.categories.all { it.code.startsWith("3_") })
        assertEquals(Category.ANIME, ContentSection.ANIME.defaultCategory)
        assertEquals(Category.LITERATURE, ContentSection.MANGA.defaultCategory)
    }
}
