package restaurant.client

import java.net.ConnectException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpTimeoutException
import java.util.stream.Stream
import kotlin.time.toJavaDuration
import kotlinx.coroutines.future.await
import kotlinx.coroutines.stream.consumeAsFlow

class Java11HttpClient(config: HttpClientConfig = HttpClientConfig()) :
    RestaurantHttpClient(config) {
    private val httpClient = HttpClient.newHttpClient()

    suspend fun send(request: HttpRequest): RestaurantResponse<String> {
        return send(request, BodyHandlerType.AsString)
    }

    suspend fun <BodyType> send(
        request: HttpRequest,
        asType: BodyHandlerType<BodyType>
    ): RestaurantResponse<BodyType> {
        return sendInternal(request, asType)
    }

    fun buildRequest(path: String, config: RequestDSL.() -> Unit = {}): HttpRequest {
        return buildRequestData(path, config).toHttpRequest()
    }

    override suspend fun <BodyType> sendInternal(
        request: RequestData,
        bodyHandler: BodyHandlerType<BodyType>
    ): RestaurantResponse<BodyType> = sendInternal(request.toHttpRequest(), bodyHandler)

    private suspend fun <BodyType> sendInternal(
        request: HttpRequest,
        bodyHandler: BodyHandlerType<BodyType>
    ): RestaurantResponse<BodyType> {
        val response =
            try {
                httpClient.sendAsync(request, bodyHandler.handler()).await()
            } catch (e: ConnectException) {
                throw HttpClientException("Error connecting to $request.", e)
            } catch (e: HttpTimeoutException) {
                throw HttpClientException("Request Timeout for request $request.", e)
            }
        return RestaurantResponse(
            response.statusCode(),
            bodyHandler.convert(response.body()),
            ResponseHeaders.of(response.headers().map()),
            response.uri())
    }

    private fun RequestData.toHttpRequest(): HttpRequest {
        val builder = HttpRequest.newBuilder(URI(url)).timeout(timeout.toJavaDuration())
        headers.forEach { (key, value) -> builder.header(key, value) }
        when (method) {
            "GET" -> builder.GET()
            "POST" -> builder.POST(HttpRequest.BodyPublishers.ofString(body ?: ""))
            "PUT" -> builder.PUT(HttpRequest.BodyPublishers.ofString(body ?: ""))
            "DELETE" -> builder.DELETE()
            else -> error("unsupported method $method")
        }
        return builder.build()
    }

    private fun <BodyType> BodyHandlerType<BodyType>.handler(): HttpResponse.BodyHandler<*> =
        when (this) {
            BodyHandlerType.AsString -> HttpResponse.BodyHandlers.ofString()
            BodyHandlerType.AsBytes -> HttpResponse.BodyHandlers.ofByteArray()
            BodyHandlerType.AsFlow -> HttpResponse.BodyHandlers.ofLines()
        }

    @Suppress("UNCHECKED_CAST")
    private fun <BodyType> BodyHandlerType<BodyType>.convert(body: Any?): BodyType =
        when (this) {
            BodyHandlerType.AsString -> body as BodyType
            BodyHandlerType.AsBytes -> body as BodyType
            BodyHandlerType.AsFlow -> (body as Stream<String>).consumeAsFlow() as BodyType
        }
}

class Java11HttpClientFactory : RestaurantHttpClientFactory {
    override val name: String = "java11-client"

    override fun create(config: HttpClientConfig): RestaurantHttpClient = Java11HttpClient(config)
}
