package dev.nyaaneo.data

import dev.nyaaneo.model.TorrentPageDetails
import org.jsoup.Jsoup
import java.io.IOException

class NyaaPageParser {
    fun parse(html: String): TorrentPageDetails {
        val document = Jsoup.parse(html)
        if (document.selectFirst("#torrent-description") == null) throw IOException("Nyaa.si no devolvió el detalle del torrent.")
        val fields = document.select(".panel-body .row > div")
        fun value(label: String): String? = fields.firstOrNull { it.text().trim().equals(label, true) }
            ?.nextElementSibling()?.text()?.trim()?.takeIf { it.isNotBlank() }
        fun count(label: String): Int? = value(label)?.replace(",", "")?.replace(" ", "")?.toIntOrNull()?.takeIf { it >= 0 }
        return TorrentPageDetails(value("Submitter:")?.let { if (it.equals("Anonymous", true)) "Anónimo" else it },
            count("Seeders:"), count("Leechers:"), count("Completed:"))
    }
}
