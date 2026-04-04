package restaurant.internal.netty

import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.EventLoopGroup
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.DefaultFullHttpResponse
import io.netty.handler.codec.http.DefaultHttpContent
import io.netty.handler.codec.http.DefaultHttpResponse
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaders
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpResponse
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.codec.http.HttpUtil
import io.netty.handler.codec.http.HttpVersion
import io.netty.handler.codec.http.LastHttpContent
import io.netty.handler.codec.http.QueryStringDecoder
import java.net.BindException
import java.net.InetSocketAddress
import java.net.SocketException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import restaurant.*

internal fun buildNetty(
    rootHandlers: List<Pair<SuspendingHandler, Route>>,
    defaultHandler: SuspendingHandler,
    port: Int?,
    host: String
): RunningRestaurantServer {
    val bossGroup = NioEventLoopGroup(1)
    val workerGroup = NioEventLoopGroup()
    val requestedPort = port ?: 0
    try {
        val bootstrap =
            ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel::class.java)
                .option(ChannelOption.SO_REUSEADDR, true)
                .childHandler(
                    object : ChannelInitializer<SocketChannel>() {
                        override fun initChannel(channel: SocketChannel) {
                            channel.pipeline().addLast(HttpServerCodec())
                            channel.pipeline().addLast(HttpObjectAggregator(Int.MAX_VALUE))
                            channel
                                .pipeline()
                                .addLast(NettyRestaurantHandler(rootHandlers, defaultHandler))
                        }
                    })
        val bindFuture = bootstrap.bind(host, requestedPort).awaitUninterruptibly()
        bindFuture.cause()?.let { throw it }
        val channel = bindFuture.channel()
        val actualPort = (channel.localAddress() as InetSocketAddress).port
        return RunningRestaurantServer(
            RestaurantServer {
                channel.close().syncUninterruptibly()
                shutdown(bossGroup, workerGroup)
            },
            actualPort)
    } catch (e: Throwable) {
        shutdown(bossGroup, workerGroup)
        if (requestedPort != 0 && isPortBindingFailure(e)) {
            throw RestaurantException("could not start server on port $requestedPort", e)
        }
        throw e
    }
}

private fun shutdown(bossGroup: EventLoopGroup, workerGroup: EventLoopGroup) {
    workerGroup.shutdownGracefully().syncUninterruptibly()
    bossGroup.shutdownGracefully().syncUninterruptibly()
}

private fun isPortBindingFailure(error: Throwable): Boolean =
    error is BindException ||
        error is SocketException ||
        error.cause?.let(::isPortBindingFailure) == true

private class NettyRestaurantHandler(
    private val rootHandlers: List<Pair<SuspendingHandler, Route>>,
    private val defaultHandler: SuspendingHandler
) : SimpleChannelInboundHandler<FullHttpRequest>() {
    override fun channelRead0(ctx: ChannelHandlerContext, request: FullHttpRequest) {
        val requestMethod = request.method().toRestaurantMethod()
        val keepAlive = HttpUtil.isKeepAlive(request)
        val decoder = QueryStringDecoder(request.uri())
        val requestPath = decoder.path()
        val routeMatch = findRoute(rootHandlers, requestMethod, requestPath)
        val requestHeaders = request.headers().toRestaurantHeaderMap()
        val requestBody = request.content().toByteArrayOrNull()
        val queryParameters =
            decoder.parameters().entries.fold(mutableMapOf<String, Collection<String>>()) {
                parameters,
                (name, values) ->
                parameters[name] = values.toList()
                parameters
            }
        routeMatch?.pathParameters?.forEach { (name, value) ->
            queryParameters[name] = (queryParameters[name] ?: emptyList()) + value
        }
        val restaurantRequest =
            NettyRequest(
                requestPath = requestPath,
                queryString = request.uri().substringAfter('?', ""),
                headers = requestHeaders,
                method = requestMethod,
                queryParameters = queryParameters,
                body = requestBody)
        val handler = routeMatch?.handler ?: defaultHandler
        CoroutineScope(Dispatchers.Unconfined).launch {
            try {
                writeResponse(
                    ctx = ctx,
                    keepAlive = keepAlive,
                    response = handler.handle(restaurantRequest, MutableRequestContext()))
            } catch (e: Throwable) {
                ctx.fireExceptionCaught(e)
            }
        }
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        ctx.close()
    }
}

private data class MatchedHandler(
    val handler: SuspendingHandler,
    val pathParameters: Map<String, String>
)

private data class MatchedRoute(
    val handler: SuspendingHandler,
    val pathParameters: Map<String, String>,
    val literalSegmentCount: Int
)

private data class PathMatch(val pathParameters: Map<String, String>, val literalSegmentCount: Int)

private fun findRoute(
    rootHandlers: List<Pair<SuspendingHandler, Route>>,
    requestMethod: Method,
    requestPath: String
): MatchedHandler? {
    val matchedRoute =
        rootHandlers
            .asSequence()
            .mapNotNull { (handler, route) ->
                if (route.method != requestMethod) return@mapNotNull null
                val pathMatch = matchPath(route.path, requestPath) ?: return@mapNotNull null
                MatchedRoute(handler, pathMatch.pathParameters, pathMatch.literalSegmentCount)
            }
            .maxByOrNull { it.literalSegmentCount } ?: return null
    return MatchedHandler(matchedRoute.handler, matchedRoute.pathParameters)
}

private fun matchPath(routePath: String, requestPath: String): PathMatch? {
    val routeSegments = normalizePath(routePath).splitIntoSegments()
    val requestSegments = normalizePath(requestPath).splitIntoSegments()
    if (routeSegments.size != requestSegments.size) return null
    val pathParameters = mutableMapOf<String, String>()
    var literalSegmentCount = 0
    routeSegments.zip(requestSegments).forEach { (routeSegment, requestSegment) ->
        if (routeSegment.isPathParameter()) {
            pathParameters[routeSegment.removeSurrounding("{", "}")] = requestSegment
        } else if (routeSegment != requestSegment) {
            return null
        } else {
            literalSegmentCount++
        }
    }
    return PathMatch(pathParameters, literalSegmentCount)
}

private fun String.isPathParameter(): Boolean = startsWith("{") && endsWith("}")

private fun normalizePath(path: String): String {
    val trimmed = path.trim()
    if (trimmed.isEmpty() || trimmed == "/") return "/"
    return "/" + trimmed.trim('/')
}

private fun String.splitIntoSegments(): List<String> =
    if (this == "/") emptyList() else trim('/').split('/')

private class NettyRequest(
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
        requestWithBody = NettyRequestWithBody(this, body)
        return requestWithBody!!
    }

    override fun toString(): String =
        if (queryString.isEmpty()) "Request(method:$method, path:$requestPath)"
        else "Request(method:$method, path:$requestPath?$queryString)"
}

private class NettyRequestWithBody(
    private val nettyRequest: NettyRequest,
    override val body: ByteArray?
) : RequestWithBody, Request by nettyRequest {
    override suspend fun withBody(): RequestWithBody = this

    override fun toString(): String {
        val withoutBody = nettyRequest.toString()
        val requestBody = body?.let { String(it) }
        return if (requestBody != null) {
            withoutBody.dropLast(1) + ", body:" + requestBody + ")"
        } else {
            withoutBody
        }
    }
}

private fun HttpMethod.toRestaurantMethod(): Method =
    when (this) {
        HttpMethod.GET -> Method.GET
        HttpMethod.PUT -> Method.PUT
        HttpMethod.POST -> Method.POST
        HttpMethod.DELETE -> Method.DELETE
        else -> throw RestaurantException("unknown request method: $this")
    }

private fun HttpHeaders.toRestaurantHeaderMap(): HeaderMap =
    HeaderMap(entries().groupBy({ it.key }) { it.value })

private fun ByteBuf.toByteArrayOrNull(): ByteArray? {
    if (!isReadable) return null
    val bytes = ByteArray(readableBytes())
    getBytes(readerIndex(), bytes)
    return bytes
}

private suspend fun writeResponse(
    ctx: ChannelHandlerContext,
    keepAlive: Boolean,
    response: Response
) {
    when (response) {
        is StatusResponse -> {
            val nettyResponse =
                DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.valueOf(response.status),
                    Unpooled.EMPTY_BUFFER)
            response.headers.forEach { (name, value) -> nettyResponse.headers().set(name, value) }
            HttpUtil.setContentLength(nettyResponse, 0)
            applyConnectionHeaders(nettyResponse, keepAlive)
            writeAndMaybeClose(ctx, nettyResponse, keepAlive)
        }

        is StringResponse -> {
            val content = Unpooled.wrappedBuffer(response.body.toByteArray())
            val nettyResponse =
                DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(response.status), content)
            response.headers.forEach { (name, value) -> nettyResponse.headers().set(name, value) }
            HttpUtil.setContentLength(nettyResponse, content.readableBytes().toLong())
            applyConnectionHeaders(nettyResponse, keepAlive)
            writeAndMaybeClose(ctx, nettyResponse, keepAlive)
        }

        is ByteArrayResponse -> {
            val content = Unpooled.wrappedBuffer(response.body)
            val nettyResponse =
                DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(response.status), content)
            response.headers.forEach { (name, value) -> nettyResponse.headers().set(name, value) }
            HttpUtil.setContentLength(nettyResponse, content.readableBytes().toLong())
            applyConnectionHeaders(nettyResponse, keepAlive)
            writeAndMaybeClose(ctx, nettyResponse, keepAlive)
        }

        is FlowResponse -> {
            writeChunkedResponse(
                ctx = ctx,
                keepAlive = keepAlive,
                status = response.status,
                headers = response.headers) {
                    response.body.collect { chunk -> writeChunk(ctx, chunk.toByteArray()) }
                }
        }

        is ByteArrayFlowResponse -> {
            writeChunkedResponse(
                ctx = ctx,
                keepAlive = keepAlive,
                status = response.status,
                headers = response.headers) {
                    response.body.collect { chunk -> writeChunk(ctx, chunk) }
                }
        }
    }
}

private suspend fun writeChunkedResponse(
    ctx: ChannelHandlerContext,
    keepAlive: Boolean,
    status: Int,
    headers: Map<String, String>,
    writeBody: suspend () -> Unit
) {
    val nettyResponse =
        DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(status))
    headers.forEach { (name, value) -> nettyResponse.headers().set(name, value) }
    nettyResponse.headers().set(HttpHeaderNames.TRANSFER_ENCODING, "chunked")
    applyConnectionHeaders(nettyResponse, keepAlive)
    ctx.writeAndFlush(nettyResponse).awaitCompletion()
    writeBody()
    val lastContent = LastHttpContent.EMPTY_LAST_CONTENT
    val future = ctx.writeAndFlush(lastContent)
    if (!keepAlive) {
        future.addListener(ChannelFutureListener.CLOSE)
    }
    future.awaitCompletion()
}

private suspend fun writeChunk(ctx: ChannelHandlerContext, chunk: ByteArray) {
    ctx.writeAndFlush(DefaultHttpContent(Unpooled.wrappedBuffer(chunk))).awaitCompletion()
}

private fun applyConnectionHeaders(response: HttpResponse, keepAlive: Boolean) {
    HttpUtil.setKeepAlive(response, keepAlive)
}

private suspend fun writeAndMaybeClose(
    ctx: ChannelHandlerContext,
    response: HttpResponse,
    keepAlive: Boolean
) {
    val future = ctx.writeAndFlush(response)
    if (!keepAlive) {
        future.addListener(ChannelFutureListener.CLOSE)
    }
    future.awaitCompletion()
}

private suspend fun ChannelFuture.awaitCompletion() {
    suspendCoroutine { continuation ->
        addListener { future ->
            future.cause()?.let(continuation::resumeWithException) ?: continuation.resume(Unit)
        }
    }
}
