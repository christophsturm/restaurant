package restaurant.internal.undertow

import io.undertow.Undertow
import io.undertow.server.HttpServerExchange
import io.undertow.server.RoutingHandler
import io.undertow.server.handlers.error.SimpleErrorPageHandler
import io.undertow.util.HttpString
import io.undertow.util.Methods
import restaurant.HeaderMap
import restaurant.Method
import restaurant.Request
import restaurant.RootHandler
import restaurant.Route
import restaurant.SuspendingHandler
import restaurant.internal.RestaurantException
import java.util.Deque
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

fun Route.methodToHttpString(): HttpString = when (method) {
    Method.GET -> Methods.GET
    Method.PUT -> Methods.PUT
    Method.POST -> Methods.POST
    Method.DELETE -> Methods.DELETE
}

class UndertowRequest(private val exchange: HttpServerExchange) : Request {

    override suspend fun readBody(): ByteArray {
        return suspendCoroutine {
            exchange.requestReceiver.receiveFullBytes { _, body ->
                it.resume(body)
            }
        }
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

    override val queryParameters: Map<String, Deque<String>> = exchange.queryParameters
}

internal fun buildUndertow(
    rootHandlers: List<Pair<RootHandler, Route>>,
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
        .addHttpListener(port, host)
        .setHandler(SimpleErrorPageHandler(routingHandler))
        .build()
}

