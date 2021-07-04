package restaurant

import kotlinx.coroutines.future.await
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.temporal.ChronoUnit

val httpClient = HttpClient.newHttpClient()!!

/**
 * make a http request to a restaurant instance.
 */
suspend fun Restaurant.request(
    path: String,
    config: RequestDSL.() -> Unit = { }
): HttpResponse<String> {
    val builder = RequestDSL(
        HttpRequest.newBuilder(URI("http://localhost:$port$path")).timeout(Duration.ofSeconds(1))
    )
    val request = builder.apply { config() }.delegate.build()
    return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).await()!!
}

class RequestDSL(val delegate: HttpRequest.Builder) {
    fun post(body: String) {
        delegate.POST(HttpRequest.BodyPublishers.ofString(body))
    }

    fun put(body: String) {
        delegate.PUT(HttpRequest.BodyPublishers.ofString(body))
    }

    fun delete() {
        delegate.DELETE()
    }

    fun addHeader(key: String, value: String) {
        delegate.header(key, value)
    }

    fun timeout(amount: Long, unit: ChronoUnit) {
        delegate.timeout(Duration.of(amount, unit))
    }
}

