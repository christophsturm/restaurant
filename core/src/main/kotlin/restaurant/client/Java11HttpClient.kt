package restaurant.client

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.future.await
import kotlinx.coroutines.stream.consumeAsFlow
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpHeaders
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.stream.Stream

class Java11HttpClient {
    private val httpClient = HttpClient.newHttpClient()!!
    suspend fun send(
        path: String,
        config: RequestDSL.() -> Unit = {}
    ) = send(buildRequest(path, config))

    suspend fun send(request: HttpRequest): RestaurantResponse<String> {
        val response = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).await()
        return RestaurantResponse(response.statusCode(), response.body(), response.headers(), response.uri())
    }


        suspend fun sendStreaming(url: String, config: RequestDSL.() -> Unit = {}): RestaurantResponse<Flow<String>> {
            val request = buildRequest(url, config)
            val response: HttpResponse<Stream<String>> = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofLines()).await()
            return RestaurantResponse(response.statusCode(), response.body().consumeAsFlow(), response.headers(), response.uri())
        }

    interface RequestDSL {
        fun post(body: String)
        fun put(body: String)
        fun delete()
        fun addHeader(key: String, value: String)
        fun timeout(amount: Long, unit: ChronoUnit)
        fun post()
    }

    class J11ClientRequestDSL(val delegate: HttpRequest.Builder) : RequestDSL {
        override fun post(body: String) {
            delegate.POST(HttpRequest.BodyPublishers.ofString(body))
        }

        override fun post() {
            delegate.POST(HttpRequest.BodyPublishers.noBody())
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
        // THE JDK11 http client defaults to no timeout. We set a 5 seconds timeout per default, which is pretty long
        // because we don't want a test to hang, but we also don't want a test to fail because of too much load on CI
        private const val DEFAULT_TIMEOUT_SECONDS = 5L

        fun buildRequest(
            path: String,
            config: RequestDSL.() -> Unit
        ): HttpRequest {
            val builder = J11ClientRequestDSL(
                HttpRequest.newBuilder(URI(path)).timeout(
                    Duration.ofSeconds(
                        DEFAULT_TIMEOUT_SECONDS
                    )
                )
            )
            return builder.apply { config() }.delegate.build()
        }
    }
}

data class RestaurantResponse<BodyType>(val statusCode: Int, val body: BodyType?, val headers: HttpHeaders, val uri: URI?) {
    fun statusCode(): Int = statusCode
    fun body(): BodyType? = body
    fun headers(): HttpHeaders = headers
    override fun toString(): String = """HttpResponse(url: "$uri", status: $statusCode, body:"$body" headers: $headers)"""
}
