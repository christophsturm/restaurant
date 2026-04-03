package restaurant.client

import java.io.InterruptedIOException
import java.net.ConnectException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient as SquareOkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class OkHttpClient(config: HttpClientConfig = HttpClientConfig()) : RestaurantHttpClient(config) {
    private val httpClient = SquareOkHttpClient.Builder().withTimeouts(timeout).build()

    override fun close() {
        httpClient.dispatcher.executorService.shutdown()
        httpClient.connectionPool.evictAll()
        httpClient.cache?.close()
    }

    override suspend fun <BodyType> sendInternal(
        request: RequestData,
        bodyHandler: BodyHandlerType<BodyType>
    ): RestaurantResponse<BodyType> =
        @Suppress("UNCHECKED_CAST")
        when (bodyHandler) {
            BodyHandlerType.AsString -> sendString(request) as RestaurantResponse<BodyType>
            BodyHandlerType.AsBytes -> sendBytes(request) as RestaurantResponse<BodyType>
            BodyHandlerType.AsFlow -> sendFlow(request) as RestaurantResponse<BodyType>
        }

    private suspend fun sendString(request: RequestData): RestaurantResponse<String> =
        execute(request) { response ->
            response.use { openResponse ->
                RestaurantResponse(
                    openResponse.code,
                    openResponse.body?.string().orEmpty(),
                    ResponseHeaders.of(openResponse.headers.toMultimap()),
                    openResponse.request.url.toUri())
            }
        }

    private suspend fun sendBytes(request: RequestData): RestaurantResponse<ByteArray> =
        execute(request) { response ->
            response.use { openResponse ->
                RestaurantResponse(
                    openResponse.code,
                    openResponse.body?.bytes() ?: byteArrayOf(),
                    ResponseHeaders.of(openResponse.headers.toMultimap()),
                    openResponse.request.url.toUri())
            }
        }

    private suspend fun sendFlow(
        request: RequestData
    ): RestaurantResponse<kotlinx.coroutines.flow.Flow<String>> =
        execute(request) { response ->
            RestaurantResponse(
                response.code,
                flow {
                        response.use { openResponse ->
                            val source = openResponse.body?.source() ?: return@use
                            while (true) {
                                val line = source.readUtf8Line() ?: break
                                emit(line)
                            }
                        }
                    }
                    .flowOn(Dispatchers.IO),
                ResponseHeaders.of(response.headers.toMultimap()),
                response.request.url.toUri())
        }

    private suspend fun <BodyType> execute(
        request: RequestData,
        mapResponse: (okhttp3.Response) -> RestaurantResponse<BodyType>
    ): RestaurantResponse<BodyType> =
        try {
            withContext(Dispatchers.IO) {
                clientFor(request).newCall(request.toOkHttpRequest()).execute().let(mapResponse)
            }
        } catch (e: ConnectException) {
            throw HttpClientException("Error connecting to ${request.url}.", e)
        } catch (e: InterruptedIOException) {
            throw HttpClientException("Request Timeout for request ${request.url}.", e)
        }

    private fun clientFor(request: RequestData): SquareOkHttpClient =
        if (request.timeout == timeout) {
            httpClient
        } else {
            httpClient.newBuilder().withTimeouts(request.timeout).build()
        }

    private fun RequestData.toOkHttpRequest(): Request {
        val builder = Request.Builder().url(url)
        headers.forEach { (key, value) -> builder.addHeader(key, value) }
        when (method) {
            "GET" -> builder.get()
            "POST" -> builder.post((body ?: "").toRequestBody())
            "PUT" -> builder.put((body ?: "").toRequestBody())
            "DELETE" -> builder.delete()
            else -> error("unsupported method $method")
        }
        return builder.build()
    }

    private fun SquareOkHttpClient.Builder.withTimeouts(
        timeout: kotlin.time.Duration
    ): SquareOkHttpClient.Builder =
        connectTimeout(timeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)
            .readTimeout(timeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)
            .writeTimeout(timeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)
}

class OkHttpClientFactory : RestaurantHttpClientFactory {
    override val name: String = "okhttp-client"

    override fun create(config: HttpClientConfig): RestaurantHttpClient = OkHttpClient(config)
}
