package restaurant.client

import java.net.URI
import java.util.ServiceLoader
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.Flow

data class HttpClientConfig(val baseUrl: String = "", val timeout: Duration = 5.seconds)

interface RestaurantHttpClientFactory {
    val name: String

    fun create(config: HttpClientConfig = HttpClientConfig()): RestaurantHttpClient
}

abstract class RestaurantHttpClient(config: HttpClientConfig = HttpClientConfig()) : AutoCloseable {
    protected val baseUrl = config.baseUrl
    protected val timeout = config.timeout

    sealed interface BodyHandlerType<BodyType> {
        data object AsString : BodyHandlerType<String>

        data object AsBytes : BodyHandlerType<ByteArray>

        data object AsFlow : BodyHandlerType<Flow<String>>
    }

    suspend fun send(path: String, config: RequestDSL.() -> Unit = {}): RestaurantResponse<String> =
        send(path, BodyHandlerType.AsString, config)

    suspend fun <BodyType> send(
        path: String,
        asType: BodyHandlerType<BodyType>,
        config: RequestDSL.() -> Unit = {}
    ): RestaurantResponse<BodyType> = sendInternal(buildRequestData(path, config), asType)

    @Deprecated("use send(... BodyHandlerType.AsFlow)")
    suspend fun sendStreaming(
        url: String,
        config: RequestDSL.() -> Unit = {}
    ): RestaurantResponse<Flow<String>> = send(url, BodyHandlerType.AsFlow, config)

    protected data class RequestData(
        val url: String,
        val method: String,
        val body: String?,
        val headers: List<Pair<String, String>>,
        val timeout: Duration
    )

    protected abstract suspend fun <BodyType> sendInternal(
        request: RequestData,
        bodyHandler: BodyHandlerType<BodyType>
    ): RestaurantResponse<BodyType>

    override fun close() {}

    protected fun buildRequestData(path: String, config: RequestDSL.() -> Unit): RequestData {
        val requestDsl = RequestDslImpl(timeout)
        requestDsl.config()
        return RequestData(
            url = resolveUrl(path),
            method = requestDsl.method,
            body = requestDsl.body,
            headers = requestDsl.headers,
            timeout = requestDsl.timeout)
    }

    private fun resolveUrl(path: String): String =
        when {
            path.startsWith("http://") -> path
            path.startsWith("https://") -> path
            else -> baseUrl + path
        }
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

class HttpClientException(message: String, cause: Throwable) : RuntimeException(message, cause)

data class RestaurantResponse<BodyType>(
    val statusCode: Int,
    val body: BodyType?,
    val headers: ResponseHeaders,
    val uri: URI?
) {
    val isOk = statusCode in 200..299

    fun statusCode(): Int = statusCode

    fun body(): BodyType? = body

    fun headers(): ResponseHeaders = headers

    override fun toString(): String =
        """HttpResponse(url: "$uri", status: $statusCode, body:"$body" headers: $headers)"""
}

class ResponseHeaders internal constructor(private val values: Map<String, List<String>>) {
    fun allValues(name: String): List<String> = values[name.lowercase()] ?: emptyList()

    override fun toString(): String = values.toString()

    companion object {
        fun of(values: Map<String, List<String>>): ResponseHeaders {
            val normalized = linkedMapOf<String, MutableList<String>>()
            values.forEach { (key, headerValues) ->
                normalized.getOrPut(key.lowercase()) { mutableListOf() }.addAll(headerValues)
            }
            return ResponseHeaders(normalized)
        }
    }
}

fun loadHttpClientFactory(): RestaurantHttpClientFactory {
    val factories = ServiceLoader.load(RestaurantHttpClientFactory::class.java).toList()
    if (factories.isEmpty()) {
        throw IllegalStateException(
            "no restaurant client implementation found. add restaurant-java11-client or " +
                "restaurant-okhttp-client")
    }
    if (factories.size > 1) {
        throw IllegalStateException(
            "multiple restaurant client implementations found: " +
                factories.joinToString { it.name } +
                ". choose a client explicitly")
    }
    return factories.single()
}

private class RequestDslImpl(defaultTimeout: Duration) : RequestDSL {
    var method: String = "GET"
    var body: String? = null
    val headers = mutableListOf<Pair<String, String>>()
    var timeout: Duration = defaultTimeout

    override fun post(body: String) {
        method = "POST"
        this.body = body
    }

    override fun post() {
        method = "POST"
        body = ""
    }

    override fun put(body: String) {
        method = "PUT"
        this.body = body
    }

    override fun put() {
        method = "PUT"
        body = ""
    }

    override fun delete() {
        method = "DELETE"
        body = null
    }

    override fun addHeader(key: String, value: String) {
        headers += key to value
    }

    override fun timeout(duration: Duration) {
        timeout = duration
    }
}
