package restaurant

import kotlinx.coroutines.future.await
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.temporal.ChronoUnit
class Java11HttpClient {
    private val httpClient = HttpClient.newHttpClient()!!
    suspend fun send(
        path: String,
        config: RequestDSL.() -> Unit
    ): HttpResponse<String> = send(buildRequest(path, config))

    suspend fun send(request: HttpRequest) =
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).await()!!

    interface IRequestDSL {
        fun post(body: String)
        fun put(body: String)
        fun delete()
        fun addHeader(key: String, value: String)
        fun timeout(amount: Long, unit: ChronoUnit)
    }

    class RequestDSL(val delegate: HttpRequest.Builder) : IRequestDSL {
        override fun post(body: String) {
            delegate.POST(HttpRequest.BodyPublishers.ofString(body))
        }

        override fun put(body: String) {
            delegate.PUT(HttpRequest.BodyPublishers.ofString(body))
        }

        override fun delete() {
            delegate.DELETE()
        }

        override fun addHeader(key: String, value: String) {
            delegate.header(key, value)
        }

        override fun timeout(amount: Long, unit: ChronoUnit) {
            delegate.timeout(Duration.of(amount, unit))
        }
    }

    companion object {
        fun buildRequest(
            path: String,
            config: RequestDSL.() -> Unit
        ): HttpRequest {
            val builder = RequestDSL(HttpRequest.newBuilder(URI(path)).timeout(Duration.ofSeconds(1)))
            return builder.apply { config() }.delegate.build()
        }
    }
}

val httpClient = Java11HttpClient()

/**
 * make a http request to a restaurant instance.
 */
suspend fun Restaurant.request(
    path: String,
    config: Java11HttpClient.RequestDSL.() -> Unit = { }
): HttpResponse<String> {
    return httpClient.send("http://localhost:$port$path", config)
}



