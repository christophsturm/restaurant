package restaurant

import io.undertow.Undertow
import java.net.ServerSocket
import restaurant.HttpStatus.INTERNAL_SERVER_ERROR_500
import restaurant.internal.Mapper
import restaurant.internal.routes
import restaurant.internal.undertow.buildUndertow

/** return an unused port for servers to listen on */
fun findFreePort(): Int =
    ServerSocket(0).use {
        it.reuseAddress = true
        it.localPort
    }

typealias ExceptionHandler = (Throwable) -> Response

private val defaultExceptionHandler: ExceptionHandler = {
    if (it is ResponseException) {
        it.response
    } else {
        response(500, "internal server error:" + it.stackTraceToString())
    }
}
private val defaultDefaultHandler = SuspendingHandler { _, _ -> response(404) }

@ConsistentCopyVisibility
data class Restaurant
internal constructor(
    val baseUrl: String,
    val routes: List<Route>,
    private val undertow: Undertow,
    @Suppress("unused") @Deprecated("use baseUrl") val port: Int
) : AutoCloseable {
    companion object {
        operator fun invoke(
            host: String = "127.0.0.1",
            port: Int? = null,
            exceptionHandler: ExceptionHandler = defaultExceptionHandler,
            defaultHandler: SuspendingHandler = defaultDefaultHandler,
            mapper: Mapper? = null,
            serviceMapping: RoutingDSL.() -> Unit
        ): Restaurant {
            val routes: List<Route> = routes(mapper, serviceMapping)
            val rootHandlers =
                routes.map { route ->
                    Pair(rootHandler(route.wrappers, exceptionHandler, route.handler), route)
                }
            val undertowAndPort = buildUndertow(rootHandlers, defaultHandler, port, host)
            val baseUrl = "http://$host:${undertowAndPort.port}"
            return Restaurant(baseUrl, routes, undertowAndPort.undertow, undertowAndPort.port)
        }

        private fun rootHandler(
            wrappers: List<Wrapper>,
            exceptionHandler: (Throwable) -> Response,
            handler: SuspendingHandler
        ): SuspendingHandler {
            val wrappedHandler =
                wrappers.reversed().fold(handler) { acc, wrapper -> wrapper.wrap(acc) }
            return SuspendingHandler { request, requestContext ->
                try {
                    wrappedHandler.handle(request, requestContext)
                } catch (e: Exception) {
                    try {
                        exceptionHandler(e)
                    } catch (e: Exception) {
                        response(
                            INTERNAL_SERVER_ERROR_500,
                            "error in error handler" + e.stackTraceToString())
                    }
                }
            }
        }
    }

    override fun close() {
        undertow.stop()
    }
}
