package restaurant

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.undertow.Undertow
import io.undertow.util.HttpString
import restaurant.internal.RoutesAdder
import restaurant.internal.routes
import restaurant.internal.undertow.buildUndertow
import java.net.ServerSocket
import java.util.*

fun findFreePort(): Int = ServerSocket(0).use {
    it.reuseAddress = true
    it.localPort
}


typealias ExceptionHandler = (Throwable) -> Response

private val defaultExceptionHandler: ExceptionHandler = {
    response(500, "internal server error")
}
class Restaurant(
    val port: Int = findFreePort(),
    val exceptionHandler: ExceptionHandler = defaultExceptionHandler,
    objectMapper: ObjectMapper = jacksonObjectMapper(),
    defaultHandler: SuspendingHandler = SuspendingHandler { _, _ -> response(404) },
    host: String = "127.0.0.1",
    serviceMapping: RoutingDSL.() -> Unit
) : AutoCloseable {

    val routes = routes(RoutesAdder(objectMapper), serviceMapping)

    private val rootHandlers = routes.map { route ->

        Pair(RootHandler(route.wrappers, exceptionHandler, route.handler), route)
    }

    private val undertow: Undertow = buildUndertow(rootHandlers, defaultHandler, port, host).apply { start() }

    override fun close() {
        undertow.stop()
    }

}


private fun path(service: RestService) =
    service::class.simpleName!!.lowercase(Locale.getDefault()).removeSuffix("service")

@RestDSL
interface RoutingDSL {
    fun resources(service: RestService, path: String = path(service), function: ResourceDSL.() -> Unit = {})
    fun namespace(prefix: String, function: RoutingDSL.() -> Unit)
    fun wrap(wrapper: Wrapper, function: RoutingDSL.() -> Unit)
    fun route(method: Method, path: String, service: SuspendingHandler)
}

fun interface Wrapper {
    suspend fun invoke(exchange: Exchange): WrapperResult?
}


@DslMarker
annotation class RestDSL

@Suppress("UNUSED_PARAMETER")
@RestDSL
class ResourceDSL(resolvedPath: String) {
    fun resources(service: RestService, function: ResourceDSL.() -> Unit = {}) {
    }
}

fun interface SuspendingHandler {
    suspend fun handle(exchange: Exchange, requestContext: RequestContext): Response
}

/**
 * the Root Handler is the main request entry point after the coroutine context is created.
 * It executes the wrappers, and the real handler for this route. It also catches exception and
 * translates them to http replies via [ExceptionHandler]
 */
internal class RootHandler(
    private val wrappers: List<Wrapper>,
    private val exceptionHandler: ExceptionHandler,
    private val restHandler: SuspendingHandler
) : SuspendingHandler {
    override suspend fun handle(exchange: Exchange, requestContext: RequestContext): Response {
        return try {
            // wrappers can add request constants, finish the request, or do nothing
            @Suppress("NAME_SHADOWING") val requestContext =
                wrappers.fold(requestContext as MutableRequestContext) { wrapperContext, wrapper ->
                    when (val wrapperResult = wrapper.invoke(exchange)) {
                        is AddRequestConstant<*> -> wrapperContext.apply { add(wrapperResult.key, wrapperResult.value) }
                        is FinishRequest -> return wrapperResult.response
                        null -> wrapperContext
                    }
                }
            restHandler.handle(exchange, requestContext)
        } catch (e: Exception) {
            return exceptionHandler(e)
        }
    }
}

interface RequestContext {
    operator fun <T> get(key: Key<T>): T
}

internal class MutableRequestContext : RequestContext {
    private val map = mutableMapOf<Key<*>, Any>()
    fun <T : Any> add(key: Key<T>, value: Any) {
        map[key] = value
    }

    @Suppress("UNCHECKED_CAST")
    override operator fun <T> get(key: Key<T>): T = map[key] as T

}


interface Exchange {
    /**
     * The Request Path. Everything before the query string
     */
    val requestPath: String

    /**
     * The Query String. Everything after the ?
     */
    val queryString: String

    /**
     * The Headers.
     */
    val headers: HeaderMap
    val queryParameters: Map<String, Deque<String>>
    suspend fun readBody(): ByteArray
}

class HeaderMap(private val requestHeaders: io.undertow.util.HeaderMap) {
    operator fun get(header: String): List<String>? {
        return requestHeaders.get(header)
    }

    operator fun get(header: HttpString): List<String>? {
        return requestHeaders.get(header)
    }
}

enum class Method {
    GET,
    PUT,
    POST,
    DELETE
}

data class Route(
    val method: Method,
    val path: String,
    val handler: SuspendingHandler,
    val wrappers: List<Wrapper> = listOf()
)


interface RestService

