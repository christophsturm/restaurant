package restaurant

@RestDSL
interface RoutingDSL {
    fun namespace(prefix: String, function: RoutingDSL.() -> Unit)

    fun wrap(wrapper: Wrapper, function: RoutingDSL.() -> Unit)

    fun route(method: Method, path: String, service: SuspendingHandler)
}

interface Key<T>

fun interface Wrapper {
    fun wrap(wrapped: SuspendingHandler): SuspendingHandler
}

@DslMarker annotation class RestDSL

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

    @Suppress("UNCHECKED_CAST") override operator fun <T> get(key: Key<T>): T = map[key] as T
}

class HeaderMap(requestHeaders: Map<String, List<String>> = emptyMap()) {
    private val normalizedHeaders: Map<String, List<String>> =
        requestHeaders.entries
            .fold(linkedMapOf<String, MutableList<String>>()) { headers, (name, values) ->
                headers.getOrPut(name.lowercase()) { mutableListOf() }.addAll(values)
                headers
            }
            .mapValues { (_, values) -> values.toList() }

    operator fun get(header: String): List<String>? = normalizedHeaders[header.lowercase()]
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
     * read the body of the request and return a request that has a body set. if the request body
     * was already read this returns this
     */
    suspend fun withBody(): RequestWithBody
}

interface RequestWithBody : Request {
    /**
     * Body of the request. This is null when no request body was sent for example for get requests.
     */
    val body: ByteArray?
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

open class RestaurantException(
    override val message: String,
    override val cause: Throwable? = null
) : RuntimeException(message, cause)
