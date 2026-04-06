package restaurant

import java.io.InterruptedIOException
import java.net.ConnectException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient as SquareOkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import restaurant.client.HttpClientConfig
import restaurant.client.HttpClientException
import restaurant.client.ResponseHeaders
import restaurant.client.RestaurantHttpClient
import restaurant.client.RestaurantResponse

class RequestBodyReadSteps {
    val firstChunkWritten = CompletableDeferred<Unit>()
    val handlerReadyForBodyRemainder = CompletableDeferred<Unit>()
    val allowBodyRemainder = CompletableDeferred<Unit>()
    val bodyReadCompleted = CompletableDeferred<Unit>()
    val allowResponse = CompletableDeferred<Unit>()
}

class StepControlledOkHttpClient(
    config: HttpClientConfig,
    private val steps: RequestBodyReadSteps,
    private val splitAfterBytes: Int
) : RestaurantHttpClient(config) {
    private val httpClient =
        SquareOkHttpClient.Builder()
            .connectTimeout(timeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)
            .readTimeout(timeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)
            .writeTimeout(timeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)
            .build()

    override fun close() {
        httpClient.dispatcher.executorService.shutdown()
        httpClient.connectionPool.evictAll()
        httpClient.cache?.close()
    }

    override suspend fun <BodyType> sendInternal(
        request: RequestData,
        bodyHandler: BodyHandlerType<BodyType>
    ): RestaurantResponse<BodyType> {
        require(bodyHandler == BodyHandlerType.AsString) {
            "StepControlledOkHttpClient only supports string responses in tests"
        }
        val response =
            try {
                withContext(Dispatchers.IO) {
                    httpClient.newCall(request.toOkHttpRequest()).execute()
                }
            } catch (e: ConnectException) {
                throw HttpClientException("Error connecting to ${request.url}.", e)
            } catch (e: InterruptedIOException) {
                throw HttpClientException("Request Timeout for request ${request.url}.", e)
            }
        response.use { openResponse ->
            @Suppress("UNCHECKED_CAST")
            return RestaurantResponse(
                openResponse.code,
                openResponse.body?.string().orEmpty(),
                ResponseHeaders.of(openResponse.headers.toMultimap()),
                openResponse.request.url.toUri())
                as RestaurantResponse<BodyType>
        }
    }

    private fun RequestData.toOkHttpRequest(): Request {
        val builder = Request.Builder().url(url)
        headers.forEach { (key, value) -> builder.addHeader(key, value) }
        when (method) {
            "POST" ->
                builder.post(
                    StepControlledStringRequestBody(body.orEmpty(), steps, splitAfterBytes))
            "PUT" ->
                builder.put(StepControlledStringRequestBody(body.orEmpty(), steps, splitAfterBytes))
            else -> error("unsupported method $method")
        }
        return builder.build()
    }
}

private class StepControlledStringRequestBody(
    private val body: String,
    private val steps: RequestBodyReadSteps,
    private val splitAfterBytes: Int
) : RequestBody() {
    private val bytes = body.toByteArray()

    override fun contentType() = "text/plain; charset=utf-8".toMediaType()

    override fun contentLength(): Long = bytes.size.toLong()

    override fun writeTo(sink: BufferedSink) {
        val splitIndex = splitAfterBytes.coerceIn(0, bytes.size)
        sink.write(bytes, 0, splitIndex)
        sink.flush()
        steps.firstChunkWritten.complete(Unit)
        runBlocking { steps.allowBodyRemainder.await() }
        sink.write(bytes, splitIndex, bytes.size - splitIndex)
    }
}
