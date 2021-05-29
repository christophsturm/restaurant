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
        val needsBody = route.method != Method.GET

        val restHandler = RestHandler(route.handler, needsBody, if (route.method == Method.POST) 201 else 200)
        Pair(RootHandler(route.wrappers, errorHandler, restHandler), route)
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
    fun post(path: String, service: HttpService)
    fun resources(service: RestService, path: String = path(service), function: ResourceDSL.() -> Unit = {})
    fun namespace(prefix: String, function: RoutingDSL.() -> Unit)
    fun get(path: String, service: HttpService)
    fun wrap(wrapper: Wrapper, function: RoutingDSL.() -> Unit)
}

interface Wrapper {
    suspend fun invoke(exchange: Exchange): Response?
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
    suspend fun handle(exchange: Exchange): Response
}

class RootHandler(
    private val wrappers: List<Wrapper>,
    private val errorHandler: ThrowableToErrorReply,
    private val restHandler: RestHandler
) : SuspendingHandler {
    override suspend fun handle(exchange: Exchange): Response {
        return try {
            wrappers.forEach {
                it.invoke(exchange)?.let { reply ->
                    return reply
                }
            }
            restHandler.handle(exchange)
        } catch (e: Exception) {
            val result = errorHandler(e)
            response(result.status, result.body)
        }
    }

}

class RestHandler(private val service: HttpService, private val readBody: Boolean, private val statusCode: Int) :
    SuspendingHandler {
    override suspend fun handle(exchange: Exchange): Response {
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

data class Route(val method: Method, val path: String, val handler: HttpService, val wrappers: List<Wrapper> = listOf())


interface RestService

interface HttpService {
    suspend fun handle(requestBody: ByteArray?, pathVariables: Map<String, String>): ByteArray?
}

