package dev.nyaaneo.data

import dev.nyaaneo.model.Torrent
import org.json.JSONArray
import org.json.JSONObject

/** Stores complete RSS snapshots so favorites and details also work offline. */
object FavoritesCodec {
    fun encode(torrents: List<Torrent>): String = JSONArray().apply {
        torrents.forEach { t -> put(JSONObject().apply {
            put("id", t.id); put("title", t.title); put("magnet", t.magnet)
            put("size", t.size); put("category", t.category); put("published", t.published)
            put("seeders", t.seeders); put("leechers", t.leechers); put("downloads", t.downloads)
            put("trusted", t.trusted)
        }) }
    }.toString()

    fun decode(value: String): List<Torrent> {
        val array = JSONArray(value)
        return (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            val id = item.optString("id")
            val title = item.optString("title")
            val magnet = item.optString("magnet")
            if (!id.matches(Regex("\\d+")) || title.isBlank() || !magnet.startsWith("magnet:?xt=urn:btih:")) return@mapNotNull null
            Torrent(id, title, magnet, item.optString("size", "—"), item.optString("category"),
                item.optString("published"), item.optInt("seeders"), item.optInt("leechers"),
                item.optInt("downloads"), item.optBoolean("trusted"))
        }.distinctBy { it.id }
    }
}
