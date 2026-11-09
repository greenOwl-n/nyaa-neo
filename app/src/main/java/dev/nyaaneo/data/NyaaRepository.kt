package dev.nyaaneo.data

import dev.nyaaneo.model.Category
import dev.nyaaneo.model.FeedFilter
import dev.nyaaneo.model.Torrent
import dev.nyaaneo.model.TorrentPageDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class NyaaRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS).readTimeout(25, TimeUnit.SECONDS)
        .callTimeout(35, TimeUnit.SECONDS).build(),
    private val parser: RssParser = RssParser(),
) {
    suspend fun search(query: String, category: Category, filter: FeedFilter): List<Torrent> =
        withContext(Dispatchers.IO) {
            val request = Request.Builder().url(buildFeedUrl(query, category, filter))
                .header("User-Agent", "NyaaNeo/0.2 (Android RSS client)")
                .header("Accept", "application/rss+xml, application/xml, text/xml").build()
            val response = client.newCall(request).awaitResponse()
            response.use {
                if (!it.isSuccessful) throw IOException("Nyaa.si respondió HTTP ${it.code}. Inténtalo más tarde.")
                val xml = it.readLimitedText()
                if (!xml.contains("<rss", ignoreCase = true)) throw IOException("Nyaa.si no devolvió un RSS válido.")
                parser.parse(xml.reader())
            }
        }

    suspend fun details(id: String): TorrentPageDetails = withContext(Dispatchers.IO) {
        require(id.matches(Regex("\\d+")))
        val request = Request.Builder().url("https://nyaa.si/view/$id")
            .header("User-Agent", "NyaaNeo/0.2 (Android client)").build()
        client.newCall(request).awaitResponse().use { response ->
            if (!response.isSuccessful) throw IOException("Nyaa.si respondió HTTP ${response.code}.")
            NyaaPageParser().parse(response.readLimitedText())
        }
    }

    companion object {
        fun buildFeedUrl(query: String, category: Category, filter: FeedFilter): HttpUrl =
            "https://nyaa.si/".toHttpUrl().newBuilder()
                .addQueryParameter("page", "rss")
                .addQueryParameter("q", query.trim())
                .addQueryParameter("c", category.code)
                .addQueryParameter("f", filter.code).build()
    }
}
