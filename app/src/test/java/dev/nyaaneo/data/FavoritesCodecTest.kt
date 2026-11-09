package dev.nyaaneo.data

import dev.nyaaneo.model.Torrent
import org.junit.Assert.*
import org.junit.Test

class FavoritesCodecTest {
    @Test fun preservesOfflineMetadataAndUnicode() {
        val torrent = Torrent("123", "日本語 · Español & Neo", "magnet:?xt=urn:btih:0123456789abcdef0123456789abcdef01234567",
            "1 GiB", "Anime", "Fri, 01 May 2026 12:00:00 +0000", 42, 7, 100, true)
        assertEquals(listOf(torrent), FavoritesCodec.decode(FavoritesCodec.encode(listOf(torrent))))
    }
    @Test fun ignoresBrokenRowsWithoutDroppingValidRows() {
        val torrent = Torrent("1", "Neo", "magnet:?xt=urn:btih:0123456789abcdef0123456789abcdef01234567")
        val valid = FavoritesCodec.encode(listOf(torrent)).removePrefix("[")
        assertEquals(listOf(torrent), FavoritesCodec.decode("[null,{},$valid"))
    }
    @Test fun handlesEmptyFavorites() { assertEquals(emptyList<Torrent>(), FavoritesCodec.decode("[]")) }
}
