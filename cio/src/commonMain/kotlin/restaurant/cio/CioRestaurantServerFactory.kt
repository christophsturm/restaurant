package restaurant.cio

import io.ktor.http.HttpMethod
import io.ktor.http.HttpProtocolVersion
import io.ktor.http.HttpStatusCode
import io.ktor.http.cio.ConnectionOptions
import io.ktor.http.cio.HttpHeadersMap
import io.ktor.http.cio.ParserException
import io.ktor.http.cio.RequestResponseBuilder
import io.ktor.http.cio.expectHttpBody
import io.ktor.http.cio.internals.parseDecLong
import io.ktor.http.cio.parseHttpBody
import io.ktor.http.cio.parseRequest
import io.ktor.http.decodeURLPart
import io.ktor.http.parseQueryString
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.ServerSocket
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.awaitClosed
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.network.sockets.port
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.cancel
import io.ktor.utils.io.readRemaining
import io.ktor.utils.io.writeFully
import io.ktor.utils.io.writePacket
import io.ktor.utils.io.writeStringUtf8
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.io.IOException
import kotlinx.io.readByteArray
import restaurant.ByteArrayFlowResponse
import restaurant.ByteArrayResponse
import restaurant.FlowResponse
import restaurant.HeaderMap
import restaurant.Method
import restaurant.MutableRequestContext
import restaurant.Request
import restaurant.RequestWithBody
import restaurant.Response
import restaurant.RestaurantException
import restaurant.RestaurantServer
import restaurant.RestaurantServerFactory
import restaurant.Route
import restaurant.RunningRestaurantServer
import restaurant.StatusResponse
import restaurant.StringResponse
import restaurant.SuspendingHandler
import restaurant.internal.findRoute
import restaurant.internal.withPathParameters

class CioRestaurantServerFactory : RestaurantServerFactory {
    override val name: String = "cio"

    override fun start(
        rootHandlers: List<Pair<SuspendingHandler, Route>>,
        defaultHandler: SuspendingHandler,
        port: Int?,
        host: String
    ): RunningRestaurantServer {
        val requestedPort = port ?: 0
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val selector = SelectorManager(scope.coroutineContext)
        val serverSocketDeferred = CompletableDeferred<ServerSocket>()
        val clientJobs = mutableListOf<kotlinx.coroutines.Job>()

        val acceptJob =
            scope.launch {
                val serverSocket =
                    aSocket(selector).tcp().bind(host, requestedPort) { reuseAddress = true }
                serverSocketDeferred.complete(serverSocket)

                try {
                    while (isActive) {
                        val client = serverSocket.accept()
                        clientJobs += launch {
                            handleConnection(client, rootHandlers, defaultHandler)
                        }
                    }
                } finally {
                    serverSocket.close()
                    serverSocket.awaitClosed()
                }
            }

        val serverSocket =
            try {
                runBlocking { serverSocketDeferred.await() }
            } catch (e: Exception) {
                scope.cancel()
                selector.close()
                if (requestedPort != 0) {
                    throw RestaurantException("could not start server on port $requestedPort", e)
                }
                throw e
            }

        val server = RestaurantServer {
            runBlocking {
                acceptJob.cancel()
                scope.coroutineContext.cancelChildren()
                serverSocket.close()
                serverSocket.awaitClosed()
                clientJobs.joinAll()
                scope.cancel()
                selector.close()
            }
        }
        return RunningRestaurantServer(server, serverSocket.port)
    }
}

private suspend fun handleConnection(
    client: Socket,
    rootHandlers: List<Pair<SuspendingHandler, Route>>,
    defaultHandler: SuspendingHandler
) {
    val input = client.openReadChannel()
    val output = client.openWriteChannel(autoFlush = false)

    try {
        while (true) {
            val request =
                try {
                    parseRequest(input) ?: break
                } catch (e: ParserException) {
                    writeBadRequest(output, e.message)
                    break
                } catch (_: IOException) {
                    break
                }

            try {
                val connectionOptions = ConnectionOptions.parse(request.headers["Connection"])
                val version = HttpProtocolVersion.parse(request.version)
                val body = readRequestBody(request, input, connectionOptions, version)
                val restaurantRequest = request.toRestaurantRequest(body)
                val routeMatch =
                    findRoute(rootHandlers, restaurantRequest.method, restaurantRequest.requestPath)
                val handler = routeMatch?.handler ?: defaultHandler
                val response =
                    handler.handle(
                        restaurantRequest.withPathParameters(
                            routeMatch?.pathParameters ?: emptyMap()),
                        MutableRequestContext())
                val keepAlive = !isLastHttpRequest(version, connectionOptions)
                writeResponse(output, response, keepAlive)
                if (!keepAlive) break
            } catch (e: ParserException) {
                writeBadRequest(output, e.message)
                break
            } catch (_: CancellationException) {
                break
            } finally {
                request.release()
            }
        }
    } finally {
        val connectionClosed = IOException("Connection closed")
        output.cancel(connectionClosed)
        input.cancel(connectionClosed)
        client.close()
    }
}

private suspend fun readRequestBody(
    request: io.ktor.http.cio.Request,
    input: ByteReadChannel,
    connectionOptions: ConnectionOptions?,
    version: HttpProtocolVersion
): ByteArray? {
    val contentLengthHeaders = request.headers.getAll("Content-Length").toList()
    if (contentLengthHeaders.size > 1) {
        throw ParserException("Duplicate Content-Length header")
    }
    val contentLength = contentLengthHeaders.singleOrNull()?.parseDecLong() ?: -1L
    val transferEncoding = request.headers["Transfer-Encoding"]
    val contentType = request.headers["Content-Type"]
    if (!expectHttpBody(
        request.method, contentLength, transferEncoding, connectionOptions, contentType)) {
        return null
    }

    val bodyChannel = ByteChannel(true)
    try {
        parseHttpBody(
            version, contentLength, transferEncoding, connectionOptions, input, bodyChannel)
    } finally {
        bodyChannel.close()
    }
    return bodyChannel.readRemaining().readByteArray()
}

private fun io.ktor.http.cio.Request.toRestaurantRequest(body: ByteArray?): Request {
    val uriString = uri.toString()
    val queryString = uriString.substringAfter('?', "")
    val requestPath = uriString.substringBefore('?').ifEmpty { "/" }.decodeURLPart()
    val queryParameters =
        parseQueryString(queryString).entries().associate { (name, values) ->
            name to values.toList()
        }
    return CioRequest(
        requestPath = requestPath,
        queryString = queryString,
        headers = headers.toRestaurantHeaderMap(),
        method = method.toRestaurantMethod(),
        queryParameters = queryParameters,
        body = body)
}

private fun HttpHeadersMap.toRestaurantHeaderMap(): HeaderMap {
    val requestHeaders = linkedMapOf<String, MutableList<String>>()
    offsets().forEach { offset ->
        val name = nameAtOffset(offset).toString()
        val value = valueAtOffset(offset).toString()
        requestHeaders.getOrPut(name) { mutableListOf() }.add(value)
    }
    return HeaderMap(requestHeaders.mapValues { (_, values) -> values.toList() })
}

private class CioRequest(
    override val requestPath: String,
    override val queryString: String,
    override val headers: HeaderMap,
    override val method: Method,
    override val queryParameters: Map<String, Collection<String>>,
    private val body: ByteArray?
) : Request {
    private var requestWithBody: RequestWithBody? = null

    override suspend fun withBody(): RequestWithBody {
        if (requestWithBody != null) return requestWithBody!!
        requestWithBody = CioRequestWithBody(this, body)
        return requestWithBody!!
    }

    override fun toString(): String =
        if (queryString.isEmpty()) "Request(method:$method, path:$requestPath)"
        else "Request(method:$method, path:$requestPath?$queryString)"
}

private class CioRequestWithBody(private val request: CioRequest, override val body: ByteArray?) :
    RequestWithBody, Request by request {
    override suspend fun withBody(): RequestWithBody = this

    override fun toString(): String {
        val withoutBody = request.toString()
        val requestBody = body?.decodeToString()
        return if (requestBody != null) {
            withoutBody.dropLast(1) + ", body:" + requestBody + ")"
        } else {
            withoutBody
        }
    }
}

private fun HttpMethod.toRestaurantMethod(): Method =
    when (this) {
        HttpMethod.Get -> Method.GET
        HttpMethod.Put -> Method.PUT
        HttpMethod.Post -> Method.POST
        HttpMethod.Delete -> Method.DELETE
        else -> throw RestaurantException("unknown request method: $this")
    }

private suspend fun writeResponse(
    output: ByteWriteChannel,
    response: Response,
    keepAlive: Boolean
) {
    when (response) {
        is StatusResponse ->
            writeFixedResponse(output, response.status, response.headers, ByteArray(0), keepAlive)
        is StringResponse ->
            writeFixedResponse(
                output,
                response.status,
                response.headers,
                response.body.encodeToByteArray(),
                keepAlive)
        is ByteArrayResponse ->
            writeFixedResponse(output, response.status, response.headers, response.body, keepAlive)
        is FlowResponse ->
            writeChunkedResponse(output, response.status, response.headers, keepAlive) {
                response.body.collect { chunk -> writeChunk(output, chunk.encodeToByteArray()) }
            }
        is ByteArrayFlowResponse ->
            writeChunkedResponse(output, response.status, response.headers, keepAlive) {
                response.body.collect { chunk -> writeChunk(output, chunk) }
            }
    }
}

private suspend fun writeFixedResponse(
    output: ByteWriteChannel,
    status: Int,
    headers: Map<String, String>,
    body: ByteArray,
    keepAlive: Boolean
) {
    writeHeaders(
        output = output,
        status = status,
        headers =
            headers +
                mapOf(
                    "Content-Length" to body.size.toString(),
                    "Connection" to connectionHeaderValue(keepAlive)))
    if (body.isNotEmpty()) {
        output.writeFully(body)
    }
    output.flush()
}

private suspend fun writeChunkedResponse(
    output: ByteWriteChannel,
    status: Int,
    headers: Map<String, String>,
    keepAlive: Boolean,
    writeBody: suspend () -> Unit
) {
    writeHeaders(
        output = output,
        status = status,
        headers =
            headers +
                mapOf(
                    "Transfer-Encoding" to "chunked",
                    "Connection" to connectionHeaderValue(keepAlive)))
    writeBody()
    output.writeStringUtf8("0\r\n\r\n")
    output.flush()
}

private suspend fun writeHeaders(
    output: ByteWriteChannel,
    status: Int,
    headers: Map<String, String>
) {
    val builder = RequestResponseBuilder()
    try {
        val statusCode = HttpStatusCode.fromValue(status)
        builder.responseLine("HTTP/1.1", status, statusCode.description)
        headers.forEach { (name, value) -> builder.headerLine(name, value) }
        builder.emptyLine()
        output.writePacket(builder.build())
    } finally {
        builder.release()
    }
}

private suspend fun writeChunk(output: ByteWriteChannel, chunk: ByteArray) {
    output.writeStringUtf8(chunk.size.toString(16))
    output.writeStringUtf8("\r\n")
    output.writeFully(chunk)
    output.writeStringUtf8("\r\n")
    output.flush()
}

private suspend fun writeBadRequest(output: ByteWriteChannel, message: String?) {
    val body = message?.encodeToByteArray() ?: ByteArray(0)
    writeFixedResponse(
        output = output,
        status = 400,
        headers =
            if (body.isEmpty()) {
                emptyMap()
            } else {
                mapOf("Content-Type" to "text/plain; charset=utf-8")
            },
        body = body,
        keepAlive = false)
}

private fun connectionHeaderValue(keepAlive: Boolean): String =
    if (keepAlive) "keep-alive" else "close"

private fun isLastHttpRequest(
    version: HttpProtocolVersion,
    connectionOptions: ConnectionOptions?
): Boolean =
    when {
        connectionOptions == null && version == HttpProtocolVersion.HTTP_1_0 -> true
        connectionOptions == null -> version != HttpProtocolVersion.HTTP_1_1
        connectionOptions.keepAlive -> false
        connectionOptions.close -> true
        else -> false
    }
