package restaurant

import io.undertow.Undertow
import restaurant.HttpStatus.INTERNAL_SERVER_ERROR_500
import restaurant.internal.Mapper
import restaurant.internal.routes
import restaurant.internal.undertow.buildUndertow
import java.net.ServerSocket


/**
 * return an unused port for servers to listen on
 */
fun findFreePort(): Int = ServerSocket(0).use {
    it.reuseAddress = true
    it.localPort
}


typealias ExceptionHandler = (Throwable) -> Response

private val defaultExceptionHandler: ExceptionHandler = {
    if (it is ResponseException)
        it.response
    else {
        response(500, "internal server error:" + it.stackTraceToString())
    }
}
private val defaultDefaultHandler = SuspendingHandler { _, _ -> response(404) }

internal object NullMapper : Mapper {
    private const val errorMessage = "No Json mapping defined, please use a dependency that contains a json lib"

    override fun <T : Any> readValue(requestBody: ByteArray?, clazz: Class<T>): T {
        TODO(errorMessage)
    }

    override fun writeValueAsBytes(value: Any?): ByteArray {
        TODO(errorMessage)
    }

}

class Restaurant(
    host: String = "127.0.0.1",
    val port: Int = findFreePort(),
    private val exceptionHandler: ExceptionHandler = defaultExceptionHandler,
    mapper: Mapper = NullMapper,
    defaultHandler: SuspendingHandler = defaultDefaultHandler,
    serviceMapping: RoutingDSL.() -> Unit
) : AutoCloseable {

    val baseUrl = "http://localhost:$port"
    val routes = routes(serviceMapping)

    private val rootHandlers = routes.map { route ->
        Pair(RootHandler(route.wrappers, exceptionHandler, route.handler), route)
    }

    private val undertow: Undertow = buildUndertow(rootHandlers, defaultHandler, port, host).apply { start() }

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
//fun resources(service: RestService, path: String = path(service), function: ResourceDSL.() -> Unit = {})


sealed class WrapperResult
data class FinishRequest(val response: Response) : WrapperResult()
data class AddRequestConstant<T : Any>(val key: Key<T>, val value: T) : WrapperResult()

interface Key<T>

fun interface Wrapper {
    suspend fun invoke(request: Request): WrapperResult?
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
    suspend fun handle(request: Request, requestContext: RequestContext): Response
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
    override suspend fun handle(request: Request, requestContext: RequestContext): Response {
        return try {
            // wrappers can add request constants, finish the request, or do nothing
            @Suppress("NAME_SHADOWING") val requestContext =
                wrappers.fold(requestContext as MutableRequestContext) { wrapperContext, wrapper ->
                    when (val wrapperResult = wrapper.invoke(request)) {
                        is AddRequestConstant<*> -> wrapperContext.apply { add(wrapperResult.key, wrapperResult.value) }
                        is FinishRequest -> return wrapperResult.response
                        null -> wrapperContext
                    }
                }
            restHandler.handle(request, requestContext)
        } catch (e: Throwable) {
            return try {
                exceptionHandler(e)
            } catch (e: Exception) {
                response(INTERNAL_SERVER_ERROR_500, "error in error handler" + e.stackTraceToString())
            }
        }
    }
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

    /**
     * The Request Path. Everything before the query string
     */
    val requestPath: String

    /**
     * The Query String. Everything after the "?"
     */
    val queryString: String

    /**
     * The Headers.
     */
    val headers: HeaderMap

    /**
     * The Request Method.
     */
    val method: Method

    val queryParameters: Map<String, Collection<String>>

    /**
     * read the body of the request and return a request that has a body set.
     * if the request body was already read this returns this
     *
     */
    suspend fun withBody(): RequestWithBody
}



interface RequestWithBody : Request {
    /**
     * Body of the request. This is null when no body was sent for example for get requests.
     */
    val body: ByteArray?
}

class HeaderMap(private val requestHeaders: io.undertow.util.HeaderMap) {
    operator fun get(header: String): List<String>? {
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

