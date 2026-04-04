package restaurant

import restaurant.internal.buildRoutes
import restaurant.internal.prepareRootHandlers

typealias ExceptionHandler = (Throwable) -> Response

private val defaultExceptionHandler: ExceptionHandler = {
    if (it is ResponseException) {
        it.response
    } else {
        response(500, "internal server error:" + (it.message ?: it.toString()))
    }
}

private val defaultDefaultHandler = SuspendingHandler { _, _ -> response(404) }

@ConsistentCopyVisibility
data class EmbeddedRestaurant
internal constructor(
    val baseUrl: String,
    val routes: List<Route>,
    private val server: RestaurantServer,
    @Suppress("unused") @Deprecated("use baseUrl") val port: Int
) {
    companion object {
        operator fun invoke(
            host: String = "127.0.0.1",
            port: Int? = null,
            exceptionHandler: ExceptionHandler = defaultExceptionHandler,
            defaultHandler: SuspendingHandler = defaultDefaultHandler,
            serverFactory: RestaurantServerFactory,
            serviceMapping: RoutingDSL.() -> Unit
        ): EmbeddedRestaurant {
            val routes = buildRoutes(serviceMapping)
            val rootHandlers = prepareRootHandlers(routes, exceptionHandler)
            val runningServer = serverFactory.start(rootHandlers, defaultHandler, port, host)
            val baseUrl = "http://$host:${runningServer.port}"
            return EmbeddedRestaurant(baseUrl, routes, runningServer.server, runningServer.port)
        }
    }

    fun close() {
        server.close()
    }
}
