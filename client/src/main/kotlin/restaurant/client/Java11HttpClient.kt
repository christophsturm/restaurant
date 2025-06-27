package restaurant.client

import java.net.ConnectException
import java.net.URI
import java.net.http.*
import java.util.stream.Stream
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.future.await
import kotlinx.coroutines.stream.consumeAsFlow

data class HttpClientConfig(val baseUrl: String = "", val timeout: Duration = 5.seconds)

class Java11HttpClient(config: HttpClientConfig = HttpClientConfig()) {
    private val baseUrl = config.baseUrl
    private val timeout = config.timeout.toJavaDuration()
    private val httpClient = HttpClient.newHttpClient()!!

    sealed interface BodyHandlerType<HandlerType, BodyType> {
        fun handler(): HttpResponse.BodyHandler<HandlerType>

        fun convert(body: HandlerType): BodyType

        data object AsString : BodyHandlerType<String, String> {
            override fun handler(): HttpResponse.BodyHandler<String> =
                HttpResponse.BodyHandlers.ofString()

            override fun convert(body: String): String = body
        }

        data object AsBytes : BodyHandlerType<ByteArray, ByteArray> {
            override fun handler(): HttpResponse.BodyHandler<ByteArray> =
                HttpResponse.BodyHandlers.ofByteArray()

            override fun convert(body: ByteArray): ByteArray = body
        }

        data object AsFlow : BodyHandlerType<Stream<String>, Flow<String>> {
            override fun handler(): HttpResponse.BodyHandler<Stream<String>> =
                HttpResponse.BodyHandlers.ofLines()

            override fun convert(body: Stream<String>): Flow<String> {
                return body.consumeAsFlow()
            }
        }
    }

    suspend fun send(path: String, config: RequestDSL.() -> Unit = {}): RestaurantResponse<String> =
        send(buildRequest(path, config), BodyHandlerType.AsString)

    suspend fun <HandlerType, ResponseType> send(
        path: String,
        asType: BodyHandlerType<HandlerType, ResponseType>,
        config: RequestDSL.() -> Unit = {}
    ): RestaurantResponse<ResponseType> = sendInternal(buildRequest(path, config), asType)

    suspend fun send(request: HttpRequest): RestaurantResponse<String> {
        return sendInternal(request, BodyHandlerType.AsString)
    }

    suspend fun <T> send(
        request: HttpRequest,
        asType: BodyHandlerType<T, T>
    ): RestaurantResponse<T> {
        return sendInternal(request, asType)
    }

    private suspend fun <HandlerType, ResponseType> sendInternal(
        request: HttpRequest,
        bodyHandler: BodyHandlerType<HandlerType, ResponseType>
    ): RestaurantResponse<ResponseType> {
        val response =
            try {
                httpClient.sendAsync(request, bodyHandler.handler()).await()
            } catch (e: ConnectException) {
                throw HttpClientException("Error connecting to $request.", e)
            } catch (e: HttpTimeoutException) {
                throw HttpClientException("Request Timeout for request $request.", e)
            }
        val body = response.body()
        val convertedBody = bodyHandler.convert(body)
        return RestaurantResponse(
            response.statusCode(), convertedBody, response.headers(), response.uri())
    }

    @Deprecated("use send(... BodyHandlerType.AsFlow)")
    suspend fun sendStreaming(
        url: String,
        config: RequestDSL.() -> Unit = {}
    ): RestaurantResponse<Flow<String>> = send(url, BodyHandlerType.AsFlow, config)

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

    fun buildRequest(path: String, config: RequestDSL.() -> Unit = {}): HttpRequest {
        val builder =
            J11ClientRequestDSL(HttpRequest.newBuilder(URI(baseUrl + path)).timeout(timeout))
        return builder.apply { config() }.delegate.build()
    }
}

class HttpClientException(message: String, cause: Exception) : RuntimeException(message, cause)

data class RestaurantResponse<BodyType>(
    val statusCode: Int,
    val body: BodyType?,
    val headers: HttpHeaders,
    val uri: URI?
) {
    val isOk = statusCode in 200..299

    fun statusCode(): Int = statusCode

    fun body(): BodyType? = body

    fun headers(): HttpHeaders = headers

    override fun toString(): String =
        """HttpResponse(url: "$uri", status: $statusCode, body:"$body" headers: $headers)"""
}
