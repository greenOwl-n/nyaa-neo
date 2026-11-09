package dev.nyaaneo.data

import dev.nyaaneo.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Catalog metadata survives restarts; image bytes are cached separately by Coil. */
class AnimeArtworkRepository(private val cacheFile: File? = null) {
    private val client = OkHttpClient.Builder().connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS).callTimeout(8, TimeUnit.SECONDS).build()
    private val cacheLock = Mutex()
    private val permits = Semaphore(3)
    private val cache = linkedMapOf<String, CachedArtwork>()
    private var loaded = false
    private class Gate { val lock = Mutex(); var nextNanos = 0L }
    private val gates = mapOf("kitsu.io" to Gate(), "api.tvmaze.com" to Gate(), "api.mangadex.org" to Gate())

    suspend fun find(search: ArtworkSearch, overrideQuery: String? = null): AnimeArtwork? = withContext(Dispatchers.IO) {
        cacheLock.withLock {
            if (!loaded) {
                cache.putAll(runCatching { ArtworkCacheCodec.decode(cacheFile?.takeIf { it.exists() }?.readText() ?: "[]") }.getOrDefault(emptyMap()))
                loaded = true
            }
            if (overrideQuery == null) cache[search.key]?.let {
                val lifetime = if (it.artwork == null) 10 * 60_000L else 7 * 24 * 60 * 60_000L
                if (System.currentTimeMillis() - it.savedAt in 0 until lifetime) return@withContext it.artwork
            }
        }
        permits.withPermit {
            val queries = overrideQuery?.trim()?.takeIf { it.length in 2..160 }?.let(::listOf) ?: search.queries
            var artwork: AnimeArtwork? = null
            for (query in queries) {
                artwork = try {
                    val url = "https://kitsu.io/api/edge/${if (search.kind == ArtworkKind.ANIME) "anime" else "manga"}".toHttpUrl().newBuilder()
                        .addQueryParameter("filter[text]", query).addQueryParameter("page[limit]", "12").build()
                    AnimeArtworkParser.findBest(fetch(url), query, search.kind)
                } catch (_: IOException) { null } catch (_: org.json.JSONException) { null }
                if (artwork != null) break
            }
            if (artwork == null) for (query in queries.take(2)) {
                artwork = try {
                    if (search.kind == ArtworkKind.ANIME) {
                        val url = "https://api.tvmaze.com/search/shows".toHttpUrl().newBuilder().addQueryParameter("q", query).build()
                        AdditionalArtworkParser.tvMaze(fetch(url), query)
                    } else {
                        val url = "https://api.mangadex.org/manga".toHttpUrl().newBuilder()
                            .addQueryParameter("title", query).addQueryParameter("limit", "12")
                            .addQueryParameter("includes[]", "cover_art")
                            .addQueryParameter("contentRating[]", "safe").addQueryParameter("contentRating[]", "suggestive").build()
                        AdditionalArtworkParser.mangaDex(fetch(url), query)
                    }
                } catch (_: IOException) { null } catch (_: org.json.JSONException) { null }
                if (artwork != null) break
            }
            cacheLock.withLock {
                // An unsuccessful manual correction does not discard a previously chosen cover.
                if (artwork != null || overrideQuery == null) {
                    cache.remove(search.key)
                    cache[search.key] = CachedArtwork(System.currentTimeMillis(), artwork)
                    while (cache.size > 256) cache.remove(cache.keys.first())
                    if (artwork != null && cacheFile != null) runCatching {
                        cacheFile.parentFile?.mkdirs()
                        val temp = File(cacheFile.path + ".tmp")
                        temp.writeText(ArtworkCacheCodec.encode(cache))
                        if (!temp.renameTo(cacheFile)) temp.delete()
                    }
                }
            }
            artwork
        }
    }

    private suspend fun fetch(url: HttpUrl): String {
        val gate = gates.getValue(url.host)
        gate.lock.withLock {
            val wait = (gate.nextNanos - System.nanoTime()) / 1_000_000
            if (wait > 5_000) throw IOException("El catálogo está limitando las consultas.")
            if (wait > 0) delay(wait)
            gate.nextNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(1100)
        }
        val request = Request.Builder().url(url).header("Accept", "application/json")
            .header("User-Agent", "NyaaNeo/0.3 (Android catalog client)").build()
        return client.newCall(request).awaitResponse().use { response ->
            if (response.code == 429) gate.lock.withLock {
                val seconds = response.header("Retry-After")?.toLongOrNull()?.coerceIn(10, 300) ?: 60
                gate.nextNanos = maxOf(gate.nextNanos, System.nanoTime() + TimeUnit.SECONDS.toNanos(seconds))
            }
            if (!response.isSuccessful) throw IOException("Carátula no disponible (${response.code}).")
            response.readLimitedText()
        }
    }
}

internal fun trustedArtworkUrl(url: String?): String? = url?.toHttpUrlOrNull()?.takeIf {
    it.isHttps && it.host in setOf("media.kitsu.app", "media.kitsu.io", "static.tvmaze.com", "uploads.mangadex.org")
}?.toString()

object AnimeArtworkParser {
    fun findBest(json: String, query: String, kind: ArtworkKind = ArtworkKind.ANIME): AnimeArtwork? {
        val entries = JSONObject(json).optJSONArray("data") ?: return null
        var bestScore = 0.79
        var best: AnimeArtwork? = null
        for (i in 0 until entries.length()) {
            val entry = entries.optJSONObject(i) ?: continue
            val type = entry.optString("type")
            if (type.isNotEmpty() && type != kind.name.lowercase(java.util.Locale.ROOT)) continue
            val attributes = entry.optJSONObject("attributes") ?: continue
            if (attributes.optString("ageRating").equals("R18", true)) continue
            val title = attributes.optString("canonicalTitle")
            val aliases = mutableListOf(title)
            attributes.optJSONObject("titles")?.let { names -> names.keys().forEach { aliases += names.optString(it) } }
            attributes.optJSONArray("abbreviatedTitles")?.let { names ->
                for (index in 0 until names.length()) aliases += names.optString(index)
            }
            val score = aliases.maxOfOrNull { ArtworkMatching.score(query, it) } ?: 0.0
            fun images(field: String) = listOf("original", "large", "medium", "small", "tiny")
                .mapNotNull { trustedArtworkUrl(attributes.optJSONObject(field)?.optString(it)) }.distinct()
            val posters = images("posterImage")
            if (posters.isEmpty()) continue
            val id = entry.optString("id").takeIf { it.matches(Regex("\\d+")) } ?: continue
            if (score > bestScore && title.isNotBlank()) {
                bestScore = score
                best = AnimeArtwork(title, posters.first(), "https://kitsu.app/${if (kind == ArtworkKind.ANIME) "anime" else "manga"}/$id",
                    "Kitsu", images("coverImage").firstOrNull(), posters.drop(1))
            }
        }
        return best
    }
}

object AdditionalArtworkParser {
    fun tvMaze(json: String, query: String): AnimeArtwork? {
        val entries = JSONArray(json)
        var score = 0.79
        var best: AnimeArtwork? = null
        for (i in 0 until entries.length()) {
            val show = entries.optJSONObject(i)?.optJSONObject("show") ?: continue
            if (show.optString("type") != "Animation") continue
            val title = show.optString("name")
            val current = ArtworkMatching.score(query, title)
            val images = listOf("original", "medium").mapNotNull { trustedArtworkUrl(show.optJSONObject("image")?.optString(it)) }.distinct()
            val id = show.optInt("id", -1).takeIf { it > 0 } ?: continue
            if (current > score && images.isNotEmpty()) {
                score = current
                best = AnimeArtwork(title, images.first(), "https://www.tvmaze.com/shows/$id", "TVMaze", alternateImages = images.drop(1))
            }
        }
        return best
    }

    fun mangaDex(json: String, query: String): AnimeArtwork? {
        val entries = JSONObject(json).optJSONArray("data") ?: return null
        var score = 0.79
        var best: AnimeArtwork? = null
        for (i in 0 until entries.length()) {
            val item = entries.optJSONObject(i) ?: continue
            val attributes = item.optJSONObject("attributes") ?: continue
            if (attributes.optString("contentRating") !in setOf("safe", "suggestive")) continue
            val names = mutableListOf<String>()
            fun namesFrom(values: JSONObject?) { values?.let { v -> v.keys().forEach { names += v.optString(it) } } }
            namesFrom(attributes.optJSONObject("title"))
            attributes.optJSONArray("altTitles")?.let { values -> for (j in 0 until values.length()) namesFrom(values.optJSONObject(j)) }
            val current = names.maxOfOrNull { ArtworkMatching.score(query, it) } ?: continue
            val id = item.optString("id").takeIf { UUID.matches(it) } ?: continue
            val relations = item.optJSONArray("relationships") ?: continue
            var file: String? = null
            for (j in 0 until relations.length()) {
                val relation = relations.optJSONObject(j) ?: continue
                if (relation.optString("type") == "cover_art") file = relation.optJSONObject("attributes")?.optString("fileName")
            }
            val filename = file?.takeIf { Regex("[a-fA-F0-9-]+\\.(?:jpg|jpeg|png|webp)").matches(it) } ?: continue
            if (current > score && names.isNotEmpty()) {
                score = current
                val base = "https://uploads.mangadex.org/covers/$id/$filename"
                best = AnimeArtwork(names.first(), "$base.512.jpg", "https://mangadex.org/title/$id", "MangaDex",
                    alternateImages = listOf(base, "$base.256.jpg"))
            }
        }
        return best
    }

    private val UUID = Regex("[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}")
}

data class CachedArtwork(val savedAt: Long, val artwork: AnimeArtwork?)

object ArtworkCacheCodec {
    fun encode(values: Map<String, CachedArtwork>): String = JSONArray().apply {
        values.forEach { (key, entry) -> entry.artwork?.let { artwork ->
            put(JSONObject().put("key", key).put("savedAt", entry.savedAt)
                .put("title", artwork.title).put("image", artwork.imageUrl).put("page", artwork.pageUrl)
                .put("provider", artwork.provider).put("banner", artwork.bannerUrl ?: JSONObject.NULL)
                .put("alternates", JSONArray(artwork.alternateImages)))
        } }
    }.toString()

    fun decode(json: String): Map<String, CachedArtwork> {
        val array = JSONArray(json)
        val values = linkedMapOf<String, CachedArtwork>()
        for (i in 0 until array.length()) {
            val value = array.optJSONObject(i) ?: continue
            val image = trustedArtworkUrl(value.optString("image")) ?: continue
            val page = value.optString("page").toHttpUrlOrNull()?.takeIf {
                it.isHttps && it.host in setOf("kitsu.app", "www.tvmaze.com", "mangadex.org")
            } ?: continue
            val alternates = value.optJSONArray("alternates")
            values[value.optString("key")] = CachedArtwork(value.optLong("savedAt"), AnimeArtwork(value.optString("title"),
                image, page.toString(), value.optString("provider"), trustedArtworkUrl(value.optString("banner")),
                (0 until (alternates?.length() ?: 0)).mapNotNull { trustedArtworkUrl(alternates?.optString(it)) }))
        }
        return values.toList().takeLast(256).toMap()
    }
}
