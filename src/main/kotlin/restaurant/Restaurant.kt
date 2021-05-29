package restaurant

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.undertow.Undertow
import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange
import io.undertow.server.RoutingHandler
import io.undertow.server.handlers.error.SimpleErrorPageHandler
import io.undertow.util.HttpString
import io.undertow.util.Methods
import io.undertow.util.SameThreadExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import mu.KotlinLogging
import restaurant.internal.Method
import restaurant.internal.Route
import restaurant.internal.RoutesAdder
import restaurant.internal.routes
import java.net.ServerSocket
import java.nio.ByteBuffer
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal fun findFreePort(): Int = ServerSocket(0).use {
    it.reuseAddress = true
    it.localPort
}

private val logger = KotlinLogging.logger {}

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


    private val undertow: Undertow = run {

        val routingHandler = routes.fold(RoutingHandler()) { routingHandler, route ->
            val needsBody = route.method != Method.GET

            val httpHandler: HttpHandler = CoroutinesHandler(
                HttpServiceHandler(
                    route.handler,
                    route.wrappers,
                    errorHandler,
                    needsBody,
                    if (route.method == Method.POST) 201 else 200
                )
            )
            routingHandler.add(
                route.toHttpString(),
                route.path,
                httpHandler
            )
        }

        Undertow.builder()
//            .setServerOption(UndertowOptions.ENABLE_HTTP2, true)
            .addHttpListener(port, "127.0.0.1")
            .setHandler(SimpleErrorPageHandler(routingHandler))
            .build()
    }


    init {
        undertow.start()
    }

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
    fun resource(service: RestService)
    fun get(path: String, service: HttpService)
    fun wrap(wrapper: Wrapper, function: RoutingDSL.() -> Unit)
}

fun Route.toHttpString(): HttpString = when (method) {
    Method.GET -> Methods.GET
    Method.PUT -> Methods.PUT
    Method.POST -> Methods.POST
    Method.DELETE -> Methods.DELETE
}

interface Wrapper {
    suspend fun invoke(exchange: ExchangeWrapper): RequestResult?
}

data class RequestResult(val status: Int, val result: String)


@DslMarker
annotation class RestDSL

@Suppress("UNUSED_PARAMETER")
@RestDSL
class ResourceDSL(resolvedPath: String) {
    fun resources(service: RestService, function: ResourceDSL.() -> Unit = {}) {
    }
}

private class CoroutinesHandler(val suspendHandler: SuspendingHandler) : HttpHandler {
    override fun handleRequest(exchange: HttpServerExchange) {
        val requestScope = CoroutineScope(Dispatchers.Unconfined)
        exchange.addExchangeCompleteListener { _, nextListener ->
            try {
                requestScope.cancel()
            } catch (e: Exception) {
                logger.error(e) { "error closing coroutine context" }
            }
            nextListener.proceed()
        }
        exchange.dispatch(SameThreadExecutor.INSTANCE, Runnable {
            requestScope.launch {
                suspendHandler.handle(ExchangeWrapper(exchange))
            }
        })
    }

}

interface SuspendingHandler {
    suspend fun handle(exchange: ExchangeWrapper)
}

class HttpServiceHandler(
    private val service: HttpService,
    private val wrappers: List<Wrapper>,
    val errorHandler: ThrowableToErrorReply,
    private val readBody: Boolean = false,
    private val statusCode: Int
) : SuspendingHandler {
    override suspend fun handle(exchange: ExchangeWrapper) {
        try {
            wrappers.forEach {
                it.invoke(exchange)?.let { reply ->
                    exchange.reply(reply.status, reply.result)
                    return
                }
            }
            val body = if (readBody) exchange.readBody() else null
            val response = service.handle(body, exchange.queryParameters.mapValues { it.value.single() })
            if (response == null) {
                exchange.reply(204)
            } else
                exchange.reply(statusCode, body = response)
        } catch (e: Exception) {
            val result = errorHandler(e)
            exchange.reply(result.status, result.body)
        }
    }
}

class ExchangeWrapper(private val exchange: HttpServerExchange) {
    fun reply(status: Int = 200, body: String) {
        exchange.statusCode = status
        exchange.responseSender.send(body)
    }

    fun reply(status: Int = 200, body: ByteArray) {
        exchange.statusCode = status
        exchange.responseSender.send(ByteBuffer.wrap(body))
    }

    fun reply(status: Int) {
        exchange.statusCode = status
        exchange.endExchange()
    }

    suspend fun readBody(): ByteArray {
        return suspendCoroutine {
            exchange.requestReceiver.receiveFullBytes { _, body ->
                it.resume(body)
            }
        }
    }

    val headers: HeaderMap = HeaderMap(exchange.requestHeaders)
    val queryParameters: Map<String, Deque<String>> = exchange.queryParameters
}

class HeaderMap(private val requestHeaders: io.undertow.util.HeaderMap) {
    operator fun get(header: String): List<String>? {
        return requestHeaders.get(header)
    }

    operator fun get(header: HttpString): List<String>? {
        return requestHeaders.get(header)
    }

}


interface RestService

interface HttpService {
    suspend fun handle(requestBody: ByteArray?, pathVariables: Map<String, String>): ByteArray?
}

