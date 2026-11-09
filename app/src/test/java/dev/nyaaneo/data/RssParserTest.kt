package dev.nyaaneo.data

import dev.nyaaneo.model.*
import org.junit.Assert.*
import org.junit.Test

class RssParserTest {
    private val hash = "0123456789abcdef0123456789abcdef01234567"
    private fun feed(items: String) = """<?xml version="1.0"?><rss version="2.0" xmlns:nyaa="https://nyaa.si/xmlns/nyaa"><channel>$items</channel></rss>"""
    private fun item(id: String = "123", title: String = "Anime &amp; Neo", extra: String = "") = """
        <item><title>$title</title><guid isPermaLink="true">https://nyaa.si/view/$id</guid>
        <link>https://nyaa.si/download/$id.torrent</link><nyaa:infoHash>$hash</nyaa:infoHash>
        <nyaa:size>1.2 GiB</nyaa:size><nyaa:category>Anime - English-translated</nyaa:category>
        <nyaa:seeders>42</nyaa:seeders><nyaa:leechers>7</nyaa:leechers><nyaa:downloads>100</nyaa:downloads>
        <nyaa:trusted>Yes</nyaa:trusted>$extra</item>
    """.trimIndent()

    @Test fun parsesNamespacedNyaaRssAndBuildsMagnet() {
        val torrent = RssParser().parse(feed(item()).reader()).single()
        assertEquals("Anime & Neo", torrent.title)
        assertEquals("123", torrent.id)
        assertEquals(42, torrent.seeders)
        assertEquals(7, torrent.leechers)
        assertEquals(100, torrent.downloads)
        assertEquals("1.2 GiB", torrent.size)
        assertTrue(torrent.trusted)
        assertEquals("magnet:?xt=urn:btih:$hash&dn=Anime+%26+Neo", torrent.magnet)
        assertEquals("https://nyaa.si/view/123", torrent.pageUrl)
    }
    @Test fun supportsCdataAndMagnetLinkInsteadOfTorrentDownload() {
        val xml = """<item><title><![CDATA[[Group] A & B]]></title>
            <guid>https://nyaa.si/view/99</guid><link><![CDATA[magnet:?xt=urn:btih:$hash&amp;dn=name]]></link></item>"""
        assertEquals("[Group] A & B", RssParser().parse(feed(xml).reader()).single().title)
    }
    @Test fun skipsInvalidIdsAndHashesAndDeduplicates() {
        val xml = item() + item() + item("bad") + item("456").replace(hash, "invalid")
        assertEquals(listOf("123"), RssParser().parse(feed(xml).reader()).map { it.id })
    }
    @Test fun missingStatisticsUseDefaults() {
        val xml = "<item><title>T</title><link>https://nyaa.si/view/8</link><nyaa:infoHash>$hash</nyaa:infoHash><nyaa:seeders>-5</nyaa:seeders></item>"
        val torrent = RssParser().parse(feed(xml).reader()).single()
        assertEquals(0, torrent.seeders)
        assertEquals(0, torrent.downloads)
        assertFalse(torrent.trusted)
    }
    @Test fun emptyFeedHasNoResults() { assertTrue(RssParser().parse(feed("").reader()).isEmpty()) }
    @Test fun parsesCapturedLiveNyaaResponse() {
        val stream = requireNotNull(javaClass.getResourceAsStream("/nyaa-software.xml"))
        val torrent = stream.bufferedReader().use { RssParser().parse(it) }.single()
        assertEquals("96659", torrent.id)
        assertEquals("624.0 MiB", torrent.size)
        assertEquals("Software - Applications", torrent.category)
        assertTrue(torrent.magnet.contains("45008e48c8800b7d7643337b2e70a634e4c69f6a"))
    }
    @Test(expected = org.xmlpull.v1.XmlPullParserException::class) fun rejectsMalformedXml() { RssParser().parse("<rss><channel><item>".reader()) }
    @Test(expected = org.xmlpull.v1.XmlPullParserException::class) fun rejectsHtmlResponse() { RssParser().parse("<html><body>Unavailable</body></html>".reader()) }
    @Test(expected = org.xmlpull.v1.XmlPullParserException::class) fun rejectsTruncatedFeedAfterValidItems() {
        RssParser().parse(feed(item()).removeSuffix("</rss>").reader())
    }
    @Test fun rejectsDtdAndExternalEntities() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            RssParser().parse("""<!DOCTYPE rss [<!ENTITY x SYSTEM "file:///etc/passwd">]><rss><channel>&x;</channel></rss>""".reader())
        }
        assertTrue(error.message.orEmpty().contains("DTD"))
    }
    @Test fun encodesQueryAndFiltersWithoutQueryInjection() {
        val url = NyaaRepository.buildFeedUrl("  A & B + español?  ", Category.ANIME_OTHER, FeedFilter.TRUSTED)
        assertEquals("https", url.scheme)
        assertEquals("nyaa.si", url.host)
        assertEquals("rss", url.queryParameter("page"))
        assertEquals("A & B + español?", url.queryParameter("q"))
        assertEquals("1_3", url.queryParameter("c"))
        assertEquals("2", url.queryParameter("f"))
        assertEquals(4, url.querySize)
    }
    @Test fun sortsRecentByNumericIdAndSeedersByCount() {
        val torrents = listOf(Torrent("9", "Z", "", seeders = 5), Torrent("10", "A", "", seeders = 2))
        assertEquals("10", torrents.sortedByOrder(SortOrder.RECENT).first().id)
        assertEquals("9", torrents.sortedByOrder(SortOrder.SEEDERS).first().id)
        assertEquals("A", torrents.sortedByOrder(SortOrder.TITLE).first().title)
    }
}
