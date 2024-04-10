package restaurant

import io.undertow.Undertow
import restaurant.HttpStatus.INTERNAL_SERVER_ERROR_500
import restaurant.internal.Mapper
import restaurant.internal.routes
import restaurant.internal.undertow.buildUndertow
import java.net.BindException
import java.net.ServerSocket

/** return an unused port for servers to listen on */
fun findFreePort(): Int = ServerSocket(0).use {
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

data class Restaurant internal constructor(
    val baseUrl: String,
    val routes: List<Route>,
    private val undertow: Undertow,
    @Suppress("unused")
    @Deprecated("use baseUrl")
    val port: Int
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
            val rootHandlers = routes.map { route ->
                Pair(RootHandler(route.wrappers, exceptionHandler, route.handler), route)
            }
            // retry undertow construction when listening on a random port and a bind exception occurs.
            var tries = 3
            while (true) {
                val realPort = port ?: findFreePort()
                val undertow: Undertow = buildUndertow(rootHandlers, defaultHandler, realPort, host)
                try {
                    undertow.start()
                } catch (e: RuntimeException) {
                    // it seems that undertow now wraps the bind exception in a runtime exception
                    if (e.cause is BindException) {
                        // if no port was specified, we retry
                        if (port != 0 || tries-- < 0)
                            throw e
                        continue
                    }
                    throw e
                } catch (e: BindException) {
                    // if no port was specified, we retry
                    if (port != 0 || tries-- < 0)
                        throw e
                    continue
                }
                val baseUrl = "http://$host:$realPort"
                return Restaurant(baseUrl, routes, undertow, realPort)
            }
        }

        private fun RootHandler(
            wrappers: List<Wrapper>,
            exceptionHandler: (Throwable) -> Response,
            handler: SuspendingHandler
        ): SuspendingHandler {
            val wrappedHandler = wrappers.reversed().fold(handler) { acc, wrapper ->
                wrapper.wrap(acc)
            }
            return SuspendingHandler { request, requestContext ->
                try {
                    wrappedHandler.handle(request, requestContext)
                } catch (e: Exception) {
                    try {
                        exceptionHandler(e)
                    } catch (e: Exception) {
                        response(INTERNAL_SERVER_ERROR_500, "error in error handler" + e.stackTraceToString())
                    }
                }
            }
        }
    }

    override fun close() {
        undertow.stop()
    }
}

@RestDSL
interface RoutingDSL {
    fun namespace(prefix: String, function: RoutingDSL.() -> Unit)
    fun wrap(wrapper: Wrapper, function: RoutingDSL.() -> Unit)
    fun route(method: Method, path: String, service: SuspendingHandler)
}
// fun resources(service: RestService, path: String = path(service), function: ResourceDSL.() -> Unit = {})

interface Key<T>

fun interface Wrapper {
    fun wrap(wrapped: SuspendingHandler): SuspendingHandler
}

@DslMarker
annotation class RestDSL

fun interface SuspendingHandler {
    suspend fun handle(request: Request, requestContext: MutableRequestContext): Response
}

interface RequestContext {
    operator fun <T> get(key: Key<T>): T
}

class MutableRequestContext : RequestContext {
    private val map = mutableMapOf<Key<*>, Any>()
    fun <T : Any> add(key: Key<T>, value: Any) {
        map[key] = value
    }

    @Suppress("UNCHECKED_CAST")
    override operator fun <T> get(key: Key<T>): T = map[key] as T
}

interface Request {

    /** The Request Path. Everything before the query string */
    val requestPath: String

    /** The Query String. Everything after the "?" */
    val queryString: String

    /** The Headers. */
    val headers: HeaderMap

    /** The Request Method. */
    val method: Method

    val queryParameters: Map<String, Collection<String>>

    /**
     * read the body of the request and return a request that has a body set. if the request body was already read this
     * returns this
     */
    suspend fun withBody(): RequestWithBody
}

interface RequestWithBody : Request {
    /** Body of the request. This is null when no request body was sent for example for get requests. */
    val body: ByteArray?
}

class HeaderMap(private val requestHeaders: io.undertow.util.HeaderMap) {
    operator fun get(header: String): List<String>? {
        return requestHeaders.get(header)
    }
}

enum class Method {
    GET, PUT, POST, DELETE
}

data class Route(
    val method: Method,
    val path: String,
    val handler: SuspendingHandler,
    val wrappers: List<Wrapper> = listOf()
)

open class RestaurantException(override val message: String, override val cause: Throwable? = null) :
    RuntimeException(message, cause)
