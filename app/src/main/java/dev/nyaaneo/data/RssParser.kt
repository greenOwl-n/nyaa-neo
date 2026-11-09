package dev.nyaaneo.data

import dev.nyaaneo.model.Torrent
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.Reader

/** Namespace-aware RSS parser: only accepts numeric Nyaa IDs and valid BTIH hashes. */
class RssParser {
    fun parse(reader: Reader): List<Torrent> {
        val parser = XmlPullParserFactory.newInstance().apply { isNamespaceAware = true }.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, false)
        parser.setInput(reader)
        val torrents = mutableListOf<Torrent>()
        var fields: MutableMap<String, String>? = null
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            // Nyaa RSS needs no DTD. Reject it explicitly, including internal entities.
            require(event != XmlPullParser.DOCDECL) { "El RSS contiene un DTD no permitido" }
            if (event == XmlPullParser.START_TAG) {
                if (parser.name == "item") {
                    fields = mutableMapOf()
                } else if (fields != null && parser.name in TAGS) {
                    val key = parser.name
                    fields[key] = parser.nextText().trim()
                }
            } else if (event == XmlPullParser.END_TAG && parser.name == "item") {
                fields?.toTorrent()?.let(torrents::add)
                fields = null
            }
            event = parser.nextToken()
        }
        return torrents.distinctBy { it.id }
    }

    private fun Map<String, String>.toTorrent(): Torrent? {
        val id = listOf(get("guid"), get("link")).firstNotNullOfOrNull { value ->
            value?.let { ID.find(it)?.groupValues?.get(1) }
        } ?: return null
        val title = get("title")?.takeIf { it.isNotBlank() } ?: return null
        val hash = get("infoHash")?.takeIf { HASH.matches(it) }
            ?: get("link")?.let { MAGNET_HASH.find(it)?.groupValues?.get(1) }?.takeIf { HASH.matches(it) }
            ?: return null
        val magnet = "magnet:?xt=urn:btih:$hash&dn=" +
            java.net.URLEncoder.encode(title, "UTF-8")
        return Torrent(
            id, title, magnet, get("size") ?: "—", get("category") ?: "Sin categoría",
            get("pubDate") ?: "", number("seeders"), number("leechers"), number("downloads"),
            get("trusted").equals("yes", true) || get("trusted").equals("true", true),
        )
    }

    private fun Map<String, String>.number(key: String) = get(key)?.toIntOrNull()?.coerceAtLeast(0) ?: 0

    companion object {
        private val TAGS = setOf("title", "link", "guid", "pubDate", "infoHash", "size", "category", "seeders", "leechers", "downloads", "trusted")
        private val ID = Regex("https://nyaa\\.si/view/(\\d+)(?:[/?#].*)?$")
        private val HASH = Regex("(?:[a-fA-F0-9]{40}|[A-Z2-7a-z]{32})")
        private val MAGNET_HASH = Regex("urn:btih:([^&]+)", RegexOption.IGNORE_CASE)
    }
}
