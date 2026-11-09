package dev.nyaaneo.data

import dev.nyaaneo.model.Category
import dev.nyaaneo.model.FeedFilter
import dev.nyaaneo.model.Torrent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class NyaaRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS).readTimeout(25, TimeUnit.SECONDS)
        .callTimeout(35, TimeUnit.SECONDS).build(),
    private val parser: RssParser = RssParser(),
) {
    suspend fun search(query: String, category: Category, filter: FeedFilter): List<Torrent> =
        withContext(Dispatchers.IO) {
            val request = Request.Builder().url(buildFeedUrl(query, category, filter))
                .header("User-Agent", "NyaaNeo/0.1 (Android RSS client)")
                .header("Accept", "application/rss+xml, application/xml, text/xml").build()
            val response = suspendCancellableCoroutine { continuation ->
                val call = client.newCall(request)
                continuation.invokeOnCancellation { call.cancel() }
                call.enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        if (!continuation.isCancelled) continuation.resumeWithException(e)
                    }
                    override fun onResponse(call: Call, response: Response) {
                        continuation.resume(response) { _, value, _ -> value.close() }
                    }
                })
            }
            response.use {
                if (!it.isSuccessful) throw IOException("Nyaa.si respondió HTTP ${it.code}. Inténtalo más tarde.")
                val body = it.body ?: throw IOException("Nyaa.si devolvió una respuesta vacía.")
                // Limit unexpected responses instead of retaining an unbounded document in memory.
                val xml = body.charStream().use { reader ->
                    val buffer = CharArray(2_000_001)
                    var count = 0
                    while (count < buffer.size) {
                        val read = reader.read(buffer, count, buffer.size - count)
                        if (read < 0) break
                        count += read
                    }
                    if (count == buffer.size) throw IOException("El RSS supera el tamaño permitido.")
                    String(buffer, 0, count)
                }
                if (!xml.contains("<rss", ignoreCase = true)) throw IOException("Nyaa.si no devolvió un RSS válido.")
                parser.parse(xml.reader())
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
