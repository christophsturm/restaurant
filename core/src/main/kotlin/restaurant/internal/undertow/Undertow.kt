package restaurant.internal.undertow

import io.undertow.Undertow
import io.undertow.server.HttpServerExchange
import io.undertow.server.RoutingHandler
import io.undertow.server.handlers.error.SimpleErrorPageHandler
import io.undertow.util.HttpString
import io.undertow.util.Methods
import restaurant.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

fun Route.methodToHttpString(): HttpString = when (method) {
    Method.GET -> Methods.GET
    Method.PUT -> Methods.PUT
    Method.POST -> Methods.POST
    Method.DELETE -> Methods.DELETE
}

class UndertowRequest(private val exchange: HttpServerExchange) : Request {
    private var requestWithBody: RequestWithBody? = null
    override suspend fun withBody(): RequestWithBody {
        if (requestWithBody != null)
            return requestWithBody!!
        val body: ByteArray = suspendCoroutine {
            exchange.requestReceiver.receiveFullBytes { _, body ->
                it.resume(body)
            }
        }
        requestWithBody = UndertowRequestWithBody(this, body)
        return requestWithBody!!
    }

    override val requestPath: String = exchange.requestPath

    override val queryString: String = exchange.queryString

    override val headers: HeaderMap = HeaderMap(exchange.requestHeaders)
    override val method: Method = when (val method = exchange.requestMethod) {
        Methods.GET -> Method.GET
        Methods.POST -> Method.POST
        Methods.PUT -> Method.PUT
        Methods.DELETE -> Method.DELETE
        else -> throw RestaurantException("unknown request method: $method")
    }

    override val queryParameters: Map<String, Collection<String>> = exchange.queryParameters
    override fun toString(): String =
        if (queryString.isEmpty())
            "Request(method:$method, path:$requestPath)"
        else
            "Request(method:$method, path:$requestPath?$queryString)"
}

class UndertowRequestWithBody(private val undertowRequest: UndertowRequest, override val body: ByteArray?) :
    RequestWithBody, Request by undertowRequest {
    override suspend fun withBody(): RequestWithBody = this
    override fun toString(): String {
        val withoutBody = undertowRequest.toString()
        val body = body?.let { String(it) }
        return if (body != null)
            withoutBody.dropLast(1) + ", body:" + body + ")"
        else
            withoutBody
    }
}

internal fun buildUndertow(
    rootHandlers: List<Pair<SuspendingHandler, Route>>,
    defaultHandler: SuspendingHandler,
    port: Int,
    host: String
): Undertow {
    val routingHandler = rootHandlers.fold(RoutingHandler()) { routingHandler, (handler, route) ->
        val httpHandler = CoroutinesHandler(handler)
        routingHandler.add(route.methodToHttpString(), route.path, httpHandler)
    }
    routingHandler.fallbackHandler = CoroutinesHandler(defaultHandler)

    return Undertow.builder()
        //            .setServerOption(UndertowOptions.ENABLE_HTTP2, true)
        .addHttpListener(port, host).setHandler(SimpleErrorPageHandler(routingHandler)).build().apply { start() }
}
