package restaurant.internal.undertow

import io.undertow.Undertow
import io.undertow.server.HttpServerExchange
import io.undertow.server.RoutingHandler
import io.undertow.server.handlers.error.SimpleErrorPageHandler
import io.undertow.util.HttpString
import io.undertow.util.Methods
import restaurant.*
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

fun Route.methodToHttpString(): HttpString = when (method) {
    Method.GET -> Methods.GET
    Method.PUT -> Methods.PUT
    Method.POST -> Methods.POST
    Method.DELETE -> Methods.DELETE
}

class UndertowExchange(private val exchange: HttpServerExchange) : Exchange {
    override suspend fun readBody(): ByteArray {
        return suspendCoroutine {
            exchange.requestReceiver.receiveFullBytes { _, body ->
                it.resume(body)
            }
        }
    }

    override val headers: HeaderMap = HeaderMap(exchange.requestHeaders)
    override val queryParameters: Map<String, Deque<String>> = exchange.queryParameters
}

fun buildUndertow(rootHandlers: List<Pair<RootHandler, Route>>, port: Int): Undertow {
    val routingHandler = rootHandlers.fold(RoutingHandler()) { routingHandler, (handler, route) ->
        val httpHandler = CoroutinesHandler(handler)
        routingHandler.add(route.methodToHttpString(), route.path, httpHandler)
    }

    return Undertow.builder()
        //            .setServerOption(UndertowOptions.ENABLE_HTTP2, true)
        .addHttpListener(port, "127.0.0.1")
        .setHandler(SimpleErrorPageHandler(routingHandler))
        .build()
}

