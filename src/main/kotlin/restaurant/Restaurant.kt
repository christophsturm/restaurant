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

internal fun findFreePort(): Int = ServerSocket(0).use {
    it.reuseAddress = true
    it.localPort
}

private val logger = KotlinLogging.logger {}

data class ErrorReply(val status: Int, val body: String)

typealias ThrowableToErrorReply = (Throwable) -> ErrorReply

val defaultErrorHandler: ThrowableToErrorReply = { ErrorReply(500, "internal server error") }

class Restaurant(
    val port: Int = findFreePort(),
    val errorHandler: ThrowableToErrorReply = defaultErrorHandler,
    objectMapper: ObjectMapper = jacksonObjectMapper(),
    serviceMapping: RoutingDSL.() -> Unit,
) : AutoCloseable {

    val routes = routes(RoutesAdder(objectMapper), serviceMapping)


    private val undertow: Undertow = run {

        val routingHandler = routes.fold(RoutingHandler()) { routingHandler, route ->
            val httpHandler = if (route.method == Method.GET) NoBodyServiceHandler(
                route.handler,
                errorHandler
            ) else HttpServiceHandler(route.handler, if (route.method == Method.POST) 201 else 200, errorHandler)
            val wrappedHandler =
                route.wrappers.foldRight(httpHandler) { wrapper, handler -> WrapperHandler(handler, wrapper) }
            routingHandler.add(
                route.toHttpString(),
                route.path,
                wrappedHandler
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

class WrapperHandler(val next: HttpHandler, val wrapper: Wrapper) : HttpHandler {
    override fun handleRequest(exchange: HttpServerExchange?) {
        wrapper.invoke()
        next.handleRequest(exchange)
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

fun interface Wrapper {
    fun invoke()
}


@DslMarker
annotation class RestDSL

@Suppress("UNUSED_PARAMETER")
@RestDSL
class ResourceDSL(resolvedPath: String) {
    fun resources(service: RestService, function: ResourceDSL.() -> Unit = {}) {
    }
}

private fun callSuspend(
    exchange: HttpServerExchange,
    service: HttpService,
    requestBody: ByteArray?,
    errorHandler: ThrowableToErrorReply
) {
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
            try {
                val r = service.handle(requestBody, exchange.queryParameters.mapValues { it.value.single() })
                if (r == null) {
                    exchange.statusCode = 204
                    exchange.endExchange()
                } else
                    exchange.responseSender.send(ByteBuffer.wrap(r))
            } catch (e: Exception) {
                val result = errorHandler(e)
                exchange.statusCode = result.status
                exchange.responseSender.send(result.body)
            }
        }
    })
}

private class NoBodyServiceHandler(private val service: HttpService, private val errorHandler: ThrowableToErrorReply) :
    HttpHandler {
    override fun handleRequest(exchange: HttpServerExchange) = callSuspend(exchange, service, null, errorHandler)
}

private class HttpServiceHandler(
    private val service: HttpService,
    private val statusCode: Int,
    private val errorHandler: ThrowableToErrorReply
) : HttpHandler {
    override fun handleRequest(ex: HttpServerExchange) {
        ex.requestReceiver.receiveFullBytes { exchange, body ->
            exchange.statusCode = statusCode
            callSuspend(exchange, service, body, errorHandler)
        }
    }
}

interface RestService

interface HttpService {
    suspend fun handle(requestBody: ByteArray?, pathVariables: Map<String, String>): ByteArray?
}

