package restaurant

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.undertow.Undertow
import io.undertow.util.HttpString
import restaurant.internal.RoutesAdder
import restaurant.internal.routes
import restaurant.internal.undertow.buildUndertow
import java.net.ServerSocket
import java.nio.ByteBuffer
import java.util.*

internal fun findFreePort(): Int = ServerSocket(0).use {
    it.reuseAddress = true
    it.localPort
}


data class ErrorReply(val status: Int, val body: String)

typealias ThrowableToErrorReply = (Throwable) -> ErrorReply

val defaultErrorHandler: ThrowableToErrorReply = {
    ErrorReply(500, "internal server error")
}

class Restaurant(
    val port: Int = findFreePort(),
    val errorHandler: ThrowableToErrorReply = defaultErrorHandler,
    objectMapper: ObjectMapper = jacksonObjectMapper(),
    serviceMapping: RoutingDSL.() -> Unit,
) : AutoCloseable {

    val routes = routes(RoutesAdder(objectMapper), serviceMapping)

    private val rootHandlers = routes.map { route ->

        Pair(RootHandler(route.wrappers, errorHandler, route.handler), route)
    }

    private val undertow: Undertow = buildUndertow(rootHandlers, port).apply { start() }

    override fun close() {
        undertow.stop()
    }

}


private fun path(service: RestService) =
    service::class.simpleName!!.toLowerCase(Locale.getDefault()).removeSuffix("service")

@RestDSL
interface RoutingDSL {
    fun post(path: String, service: SuspendingHandler)
    fun resources(service: RestService, path: String = path(service), function: ResourceDSL.() -> Unit = {})
    fun namespace(prefix: String, function: RoutingDSL.() -> Unit)
    fun get(path: String, service: SuspendingHandler)
    fun wrap(wrapper: Wrapper, function: RoutingDSL.() -> Unit)
}

interface Wrapper {
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

interface SuspendingHandler {
    suspend fun handle(exchange: Exchange, context: RequestContext): Response
}

class RootHandler(
    private val wrappers: List<Wrapper>,
    private val errorHandler: ThrowableToErrorReply,
    private val restHandler: SuspendingHandler
) : SuspendingHandler {
    override suspend fun handle(exchange: Exchange, context: RequestContext): Response {
        return try {
            // wrappers can add request constants, finish the request, or do nothing
            val requestContext = wrappers.fold(context) { wrapperContext, wrapper ->
                when (val wrapperResult = wrapper.invoke(exchange)) {
                    is AddRequestConstant<*> -> wrapperContext.apply { add(wrapperResult.key, wrapperResult.value) }
                    is FinishRequest -> return wrapperResult.response
                    null -> wrapperContext
                }
            }
            restHandler.handle(exchange, requestContext)
        } catch (e: Exception) {
            val result = errorHandler(e)
            response(result.status, result.body)
        }
    }

}

class RequestContext {
    private val map = mutableMapOf<Key<*>, Any>()
    fun <T : Any> add(key: Key<T>, value: Any) {
        map[key] = value
    }

    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(key: Key<T>): T = map[key] as T

}

internal class HttpServiceHandler(
    private val service: HttpService,
    private val readBody: Boolean,
    private val statusCode: Int
) :
    SuspendingHandler {
    override suspend fun handle(exchange: Exchange, context: RequestContext): Response {
        val body = if (readBody) exchange.readBody() else null
        val response = service.handle(body, exchange.queryParameters.mapValues { it.value.single() })
        return if (response == null) {
            response(204)
        } else
            response(statusCode, ByteBuffer.wrap(response))
    }

}


interface Exchange {
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

interface HttpService {
    suspend fun handle(requestBody: ByteArray?, pathVariables: Map<String, String>): ByteArray?
}

