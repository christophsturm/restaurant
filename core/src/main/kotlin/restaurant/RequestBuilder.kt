package restaurant

import kotlinx.coroutines.future.await
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.temporal.ChronoUnit

val httpClient = HttpClient.newHttpClient()!!

suspend fun request(
    restaurant: Restaurant,
    path: String,
    config: RequestBuilder.() -> RequestBuilder = { this }
): HttpResponse<String> {
    val builder = RequestBuilder(
        HttpRequest.newBuilder(URI("http://localhost:${restaurant.port}$path")).timeout(Duration.ofSeconds(1))
    )
    val request = builder.config().delegate.build()
    return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).await()!!
}

class RequestBuilder(val delegate: HttpRequest.Builder) {
    fun post(body: String): RequestBuilder {
        delegate.POST(HttpRequest.BodyPublishers.ofString(body))
        return this
    }

    fun put(body: String): RequestBuilder {
        delegate.PUT(HttpRequest.BodyPublishers.ofString(body))
        return this
    }

    fun delete(): RequestBuilder {
        delegate.DELETE()
        return this
    }

    fun addHeader(key: String, value: String): RequestBuilder {
        delegate.header(key, value)
        return this
    }

    fun timeout(amount: Long, unit: ChronoUnit): RequestBuilder {
        delegate.timeout(Duration.of(amount, unit))
        return this
    }

}

