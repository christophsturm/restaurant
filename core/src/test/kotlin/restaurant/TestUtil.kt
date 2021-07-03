package restaurant

import kotlinx.coroutines.future.await
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

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

}

