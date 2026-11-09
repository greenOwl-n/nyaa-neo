package dev.nyaaneo.data

import dev.nyaaneo.model.AnimeArtwork
import dev.nyaaneo.model.ReleaseInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Shared per application: deduplicates episodes, caps cache size and serializes catalog lookups. */
class AnimeArtworkRepository {
    private val client = OkHttpClient.Builder().callTimeout(15, TimeUnit.SECONDS).build()
    private val mutex = Mutex()
    private val cache = linkedMapOf<String, AnimeArtwork?>()
    private var nextRequestNanos = 0L

    suspend fun find(query: String): AnimeArtwork? = withContext(Dispatchers.IO) {
        mutex.withLock {
            val key = ReleaseInfo.normalize(query)
            if (cache.containsKey(key)) return@withLock cache[key]
            val waitMs = (nextRequestNanos - System.nanoTime()) / 1_000_000
            if (waitMs > 0) delay(waitMs)
            val url = "https://kitsu.io/api/edge/anime".toHttpUrl().newBuilder()
                .addQueryParameter("filter[text]", query).addQueryParameter("page[limit]", "5").build()
            val request = Request.Builder().url(url).header("Accept", "application/vnd.api+json")
                .header("User-Agent", "NyaaNeo/0.2 (Android client)").build()
            val artwork = client.newCall(request).awaitResponse().use { response ->
                nextRequestNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(1100)
                if (response.code == 429) {
                    nextRequestNanos += TimeUnit.SECONDS.toNanos(60)
                    throw IOException("El catálogo de carátulas está ocupado.")
                }
                if (!response.isSuccessful) throw IOException("Carátula no disponible (${response.code}).")
                AnimeArtworkParser.findBest(response.readLimitedText(), query)
            }
            if (cache.size >= 128) cache.remove(cache.keys.first())
            cache[key] = artwork
            artwork
        }
    }
}

object AnimeArtworkParser {
    fun findBest(json: String, query: String): AnimeArtwork? {
        val entries = JSONObject(json).optJSONArray("data") ?: return null
        val target = normalizedTitle(query)
        var bestScore = 0.79
        var best: AnimeArtwork? = null
        for (i in 0 until entries.length()) {
            val entry = entries.optJSONObject(i) ?: continue
            val attributes = entry.optJSONObject("attributes") ?: continue
            val title = attributes.optString("canonicalTitle")
            val aliases = mutableListOf(title)
            attributes.optJSONObject("titles")?.let { names ->
                names.keys().forEach { aliases += names.optString(it) }
            }
            attributes.optJSONArray("abbreviatedTitles")?.let { names ->
                for (index in 0 until names.length()) aliases += names.optString(index)
            }
            val score = aliases.maxOfOrNull { similarity(target, normalizedTitle(it)) } ?: 0.0
            val image = attributes.optJSONObject("posterImage")?.optString("medium")
                ?.toHttpUrlOrNull()?.takeIf { it.isHttps && it.host in setOf("media.kitsu.app", "media.kitsu.io") }
                ?: continue
            val id = entry.optString("id").takeIf { it.matches(Regex("\\d+")) } ?: continue
            if (score > bestScore && title.isNotBlank()) {
                bestScore = score
                best = AnimeArtwork(title, image.toString(), "https://kitsu.app/anime/$id")
            }
        }
        return best
    }

    private fun normalizedTitle(title: String) = ReleaseInfo.normalize(title)
        .replace("'", "").replace("’", "").replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim()

    private fun similarity(query: String, candidate: String): Double {
        if (query.isBlank() || candidate.isBlank()) return 0.0
        if (query == candidate) return 1.0
        val a = query.split(' ').toSet()
        val b = candidate.split(' ').toSet()
        return a.intersect(b).size.toDouble() / maxOf(a.size, b.size)
    }
}
