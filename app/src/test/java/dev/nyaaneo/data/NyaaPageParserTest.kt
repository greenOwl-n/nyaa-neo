package dev.nyaaneo.data

import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class NyaaPageParserTest {
    @Test fun parsesActualCapturedNyaaPage() {
        val html = requireNotNull(javaClass.getResourceAsStream("/nyaa-anime-page.html")).bufferedReader().use { it.readText() }
        val detail = NyaaPageParser().parse(html)
        assertEquals("Anónimo", detail.uploader)
        assertEquals(211, detail.seeders)
        assertEquals(49, detail.leechers)
        assertEquals(286, detail.downloads)
    }
    @Test fun readsUploaderAccountAndFormattedCounts() {
        val html = """<div class="panel-body"><div class="row">
            <div>Submitter:</div><div><a href="/user/NeoUploader">NeoUploader</a></div>
            <div>Seeders:</div><div>1,234</div><div>Leechers:</div><div>0</div>
            </div></div><div id="torrent-description"></div>"""
        val detail = NyaaPageParser().parse(html)
        assertEquals("NeoUploader", detail.uploader)
        assertEquals(1234, detail.seeders)
        assertEquals(0, detail.leechers)
        assertNull(detail.downloads)
    }
    @Test fun missingStatisticsAreNotFabricatedAsZero() {
        val detail = NyaaPageParser().parse("<div id='torrent-description'></div>")
        assertNull(detail.uploader)
        assertNull(detail.seeders)
        assertNull(detail.leechers)
    }
    @Test(expected = IOException::class) fun rejectsChallengeOrUnavailablePage() {
        NyaaPageParser().parse("<html><body>Checking your browser</body></html>")
    }
}
