package restaurant.internal.undertow

import io.undertow.Undertow
import io.undertow.server.HttpServerExchange
import io.undertow.server.RoutingHandler
import io.undertow.server.handlers.error.SimpleErrorPageHandler
import io.undertow.util.HttpString
import io.undertow.util.Methods
import io.undertow.util.SameThreadExecutor
import java.lang.Runnable
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
            exchange.requestReceiver.receiveFullBytes { _, bytes ->
                // AsyncReceiver resumes inside a nested executeRootHandler call. Dispatching the
                // continuation keeps Undertow from ending the exchange before later suspensions.
                exchange.dispatch(SameThreadExecutor.INSTANCE, Runnable { it.resume(bytes) })
            }
        }
        requestWithBody = UndertowRequestWithBody(this, body)
        return requestWithBody!!
    }

    override val requestPath: String = exchange.requestPath

    override val queryString: String = exchange.queryString

    override val headers: HeaderMap = exchange.requestHeaders.toRestaurantHeaderMap()
    override val method: Method =
        when (val requestMethod = exchange.requestMethod) {
            Methods.GET -> Method.GET
            Methods.POST -> Method.POST
            Methods.PUT -> Method.PUT
            Methods.DELETE -> Method.DELETE
            else -> throw RestaurantException("unknown request method: $requestMethod")
        }

    override val queryParameters: Map<String, Collection<String>> = exchange.queryParameters

    override fun toString(): String =
        if (queryString.isEmpty()) "Request(method:$method, path:$requestPath)"
        else "Request(method:$method, path:$requestPath?$queryString)"
}

private fun io.undertow.util.HeaderMap.toRestaurantHeaderMap(): HeaderMap =
    HeaderMap(asSequence().associate { it.headerName.toString() to it.toList() })

class UndertowRequestWithBody(
    private val undertowRequest: UndertowRequest,
    override val body: ByteArray?
) : RequestWithBody, Request by undertowRequest {
    override suspend fun withBody(): RequestWithBody = this

    override fun toString(): String {
        val withoutBody = undertowRequest.toString()
        val requestBody = body?.let { String(it) }
        return if (requestBody != null) {
            withoutBody.dropLast(1) + ", body:" + requestBody + ")"
        } else {
            withoutBody
        }
    }
}

internal fun buildUndertow(
    rootHandlers: List<Pair<SuspendingHandler, Route>>,
    defaultHandler: SuspendingHandler,
    port: Int?,
    host: String,
    getPort: () -> Int = { findFreePort() }
): RunningRestaurantServer {
    val routingHandler =
        rootHandlers.fold(RoutingHandler()) { currentRoutingHandler, (handler, route) ->
            val httpHandler = CoroutinesHandler(handler)
            currentRoutingHandler.add(route.methodToHttpString(), route.path, httpHandler)
        }
    routingHandler.fallbackHandler = CoroutinesHandler(defaultHandler)

    val totalTries = 3
    val triedPorts = ArrayList<Int>(totalTries)
    val requestedPort = if (port == 0) null else port
    while (true) {
        val realPort = requestedPort ?: getPort()
        triedPorts.add(realPort)
        try {
            val undertow =
                Undertow.builder()
                    .addHttpListener(realPort, host)
                    .setHandler(SimpleErrorPageHandler(routingHandler))
                    .build()
                    .apply { start() }
            return RunningRestaurantServer(RestaurantServer { undertow.stop() }, realPort)
        } catch (e: RuntimeException) {
            if (e.cause is BindException ||
                e.cause is IllegalStateException ||
                e.cause is SocketException) {
                if (requestedPort != null) {
                    throw RestaurantException("could not start server on port $requestedPort")
                }
                if (triedPorts.size == totalTries) {
                    throw RestaurantException(
                        "could not start restaurant after trying $totalTries times." +
                            " ports tried: $triedPorts")
                }
                Thread.sleep(100)
                continue
            }
            throw e
        }
    }
}
