package restaurant.client

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.future.await
import kotlinx.coroutines.stream.consumeAsFlow
import java.net.ConnectException
import java.net.URI
import java.net.http.*
import java.util.stream.Stream
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

data class HttpClientConfig(val baseUrl: String = "", val timeout: Duration = 5.seconds)
class Java11HttpClient(config: HttpClientConfig = HttpClientConfig()) {
    private val baseUrl = config.baseUrl
    private val timeout = config.timeout.toJavaDuration()
    private val httpClient = HttpClient.newHttpClient()!!
    suspend fun send(
        path: String, config: RequestDSL.() -> Unit = {}
    ) = send(buildRequest(path, config))

    suspend fun send(request: HttpRequest): RestaurantResponse<String> {
        val response = try {
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).await()
        } catch (e: ConnectException) {
            throw HttpClientException("Error connecting to $request.", e)
        } catch (e: HttpTimeoutException) {
            throw HttpClientException("Request Timeout for request $request.", e)
        }
        return RestaurantResponse(response.statusCode(), response.body(), response.headers(), response.uri())
    }

    suspend fun sendStreaming(url: String, config: RequestDSL.() -> Unit = {}): RestaurantResponse<Flow<String>> {
        val request = buildRequest(url, config)
        val response: HttpResponse<Stream<String>> =
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofLines()).await()
        return RestaurantResponse(
            response.statusCode(), response.body().consumeAsFlow(), response.headers(), response.uri()
        )
    }

    interface RequestDSL {
        fun post(body: String)
        fun post()
        fun put(body: String)
        fun put()
        fun delete()
        fun addHeader(key: String, value: String)
        fun timeout(duration: Duration)
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

        override fun put() {
            delegate.PUT(HttpRequest.BodyPublishers.noBody())
        }

        override fun delete() {
            delegate.DELETE()
        }

        override fun addHeader(key: String, value: String) {
            delegate.header(key, value)
        }

        override fun timeout(duration: Duration) {
            delegate.timeout(duration.toJavaDuration())
        }
    }

    fun buildRequest(
        path: String, config: RequestDSL.() -> Unit = {}
    ): HttpRequest {
        val builder = J11ClientRequestDSL(HttpRequest.newBuilder(URI(baseUrl + path)).timeout(timeout))
        return builder.apply { config() }.delegate.build()
    }
}

class HttpClientException(message: String, cause: Exception) : RuntimeException(message, cause)

data class RestaurantResponse<BodyType>(
    val statusCode: Int, val body: BodyType?, val headers: HttpHeaders, val uri: URI?
) {
    val isOk = statusCode in 200..299
    fun statusCode(): Int = statusCode
    fun body(): BodyType? = body
    fun headers(): HttpHeaders = headers
    override fun toString(): String =
        """HttpResponse(url: "$uri", status: $statusCode, body:"$body" headers: $headers)"""
}
