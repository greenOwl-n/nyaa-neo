package dev.nyaaneo.data

import dev.nyaaneo.model.Torrent
import dev.nyaaneo.model.TorrentSource
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.io.Reader

/** Namespace-aware RSS parser: only accepts numeric Nyaa IDs and valid BTIH hashes. */
class RssParser {
    fun parse(reader: Reader, source: TorrentSource = TorrentSource.NYAA): List<Torrent> {
        val parser = XmlPullParserFactory.newInstance().apply { isNamespaceAware = true }.newPullParser()
        // DTD processing is off by default. Some providers reject setFeature(false),
        // so verify the default and reject DOCDECL tokens explicitly below.
        check(!parser.getFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL)) { "El parser no permite desactivar DTD" }
        parser.setInput(reader)
        val torrents = mutableListOf<Torrent>()
        var fields: MutableMap<String, String>? = null
        var channelSeen = false
        var rootClosed = false
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            // Nyaa RSS needs no DTD. Reject it explicitly, including internal entities.
            require(event != XmlPullParser.DOCDECL) { "El RSS contiene un DTD no permitido" }
            if (event == XmlPullParser.START_TAG) {
                if (parser.depth == 1 && parser.name != "rss") {
                    throw XmlPullParserException("La respuesta no es un documento RSS")
                }
                if (parser.name == "channel" && parser.depth == 2) channelSeen = true
                if (parser.name == "item" && parser.depth == 3 && channelSeen) {
                    fields = mutableMapOf()
                } else if (fields != null && parser.depth == 4 && parser.name in TAGS) {
                    val key = parser.name
                    fields[key] = parser.nextText().trim()
                }
            } else if (event == XmlPullParser.END_TAG) {
                if (parser.name == "item" && parser.depth == 3) {
                    fields?.toTorrent(source)?.let(torrents::add)
                    fields = null
                }
                if (parser.name == "rss" && parser.depth == 1) rootClosed = true
            }
            event = parser.nextToken()
        }
        if (!rootClosed || !channelSeen) throw XmlPullParserException("El documento RSS está incompleto")
        return torrents.distinctBy { it.key }
    }

    private fun Map<String, String>.toTorrent(source: TorrentSource): Torrent? {
        val idPattern = Regex("^https://${Regex.escape(source.host)}/view/(\\d+)(?:[/?#].*)?$")
        val id = listOf(get("guid"), get("link")).firstNotNullOfOrNull { value ->
            value?.let { idPattern.find(it)?.groupValues?.get(1) }
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
            get("categoryId") ?: "",
            source = source,
        )
    }

    private fun Map<String, String>.number(key: String) = get(key)?.toIntOrNull()?.coerceAtLeast(0) ?: 0

    companion object {
        private val TAGS = setOf("title", "link", "guid", "pubDate", "infoHash", "size", "category", "categoryId", "seeders", "leechers", "downloads", "trusted")
        private val HASH = Regex("(?:[a-fA-F0-9]{40}|[A-Z2-7a-z]{32})")
        private val MAGNET_HASH = Regex("urn:btih:([^&]+)", RegexOption.IGNORE_CASE)
    }
}
