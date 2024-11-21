package restaurant.internal.undertow

import io.undertow.Undertow
import io.undertow.server.HttpServerExchange
import io.undertow.server.RoutingHandler
import io.undertow.server.handlers.error.SimpleErrorPageHandler
import io.undertow.util.HttpString
import io.undertow.util.Methods
import java.net.BindException
import java.net.SocketException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import restaurant.*

fun Route.methodToHttpString(): HttpString =
    when (method) {
        Method.GET -> Methods.GET
        Method.PUT -> Methods.PUT
        Method.POST -> Methods.POST
        Method.DELETE -> Methods.DELETE
    }

class UndertowRequest(private val exchange: HttpServerExchange) : Request {
    private var requestWithBody: RequestWithBody? = null

    override suspend fun withBody(): RequestWithBody {
        if (requestWithBody != null) return requestWithBody!!
        val body: ByteArray = suspendCoroutine {
            exchange.requestReceiver.receiveFullBytes { _, body -> it.resume(body) }
        }
        requestWithBody = UndertowRequestWithBody(this, body)
        return requestWithBody!!
    }

    override val requestPath: String = exchange.requestPath

    override val queryString: String = exchange.queryString

    override val headers: HeaderMap = HeaderMap(exchange.requestHeaders)
    override val method: Method =
        when (val method = exchange.requestMethod) {
            Methods.GET -> Method.GET
            Methods.POST -> Method.POST
            Methods.PUT -> Method.PUT
            Methods.DELETE -> Method.DELETE
            else -> throw RestaurantException("unknown request method: $method")
        }

    override val queryParameters: Map<String, Collection<String>> = exchange.queryParameters

    override fun toString(): String =
        if (queryString.isEmpty()) "Request(method:$method, path:$requestPath)"
        else "Request(method:$method, path:$requestPath?$queryString)"
}

class UndertowRequestWithBody(
    private val undertowRequest: UndertowRequest,
    override val body: ByteArray?
) : RequestWithBody, Request by undertowRequest {
    override suspend fun withBody(): RequestWithBody = this

    override fun toString(): String {
        val withoutBody = undertowRequest.toString()
        val body = body?.let { String(it) }
        return if (body != null) withoutBody.dropLast(1) + ", body:" + body + ")" else withoutBody
    }
}

internal fun buildUndertow(
    rootHandlers: List<Pair<SuspendingHandler, Route>>,
    defaultHandler: SuspendingHandler,
    port: Int?,
    host: String,
    getPort: () -> Int = { findFreePort() }
): UndertowAndPort {
    val routingHandler =
        rootHandlers.fold(RoutingHandler()) { routingHandler, (handler, route) ->
            val httpHandler = CoroutinesHandler(handler)
            routingHandler.add(route.methodToHttpString(), route.path, httpHandler)
        }
    routingHandler.fallbackHandler = CoroutinesHandler(defaultHandler)

    // retry undertow construction when listening on a random port and a bind exception occurs.
    val TOTAL_TRIES = 3
    val triedPorts = ArrayList<Int>(TOTAL_TRIES)
    while (true) {
        val realPort = port ?: getPort()
        triedPorts.add(realPort)
        try {
            return UndertowAndPort(
                Undertow.builder()
                    //            .setServerOption(UndertowOptions.ENABLE_HTTP2, true)
                    .addHttpListener(realPort, host)
                    .setHandler(SimpleErrorPageHandler(routingHandler))
                    .build()
                    .apply { start() },
                realPort)
        } catch (e: RuntimeException) {
            // it seems that undertow now wraps the bind exception in a runtime exception
            if (e.cause is BindException ||
                e.cause is IllegalStateException ||
                e.cause is SocketException) {
                if (port != null) throw RestaurantException("could not start server on port $port")
                if (triedPorts.size == TOTAL_TRIES)
                    throw RestaurantException(
                        "could not start restaurant after trying $TOTAL_TRIES times." +
                            " ports tried: $triedPorts")
                continue
            }
            throw e
        }
    }
}

data class UndertowAndPort(val undertow: Undertow, val port: Int)
