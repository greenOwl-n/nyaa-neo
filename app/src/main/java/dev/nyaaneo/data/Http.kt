package dev.nyaaneo.data

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal suspend fun Call.awaitResponse(): Response = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            if (!continuation.isCancelled) continuation.resumeWithException(e)
        }
        override fun onResponse(call: Call, response: Response) {
            continuation.resume(response) { _, value, _ -> value.close() }
        }
    })
}

internal fun Response.readLimitedText(maxChars: Int = 2_000_000): String {
    val body = body ?: throw IOException("El servidor devolvió una respuesta vacía.")
    return body.charStream().use { reader ->
        val buffer = CharArray(maxChars + 1)
        var count = 0
        while (count < buffer.size) {
            val read = reader.read(buffer, count, buffer.size - count)
            if (read < 0) break
            count += read
        }
        if (count == buffer.size) throw IOException("La respuesta supera el tamaño permitido.")
        String(buffer, 0, count)
    }
}
